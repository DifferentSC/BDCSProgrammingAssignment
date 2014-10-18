/**
 * Copyright (C) 2014 Microsoft Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.snu.bdcs.differentsc.mlpractice;

import com.microsoft.reef.driver.task.TaskConfigurationOptions;
import com.microsoft.reef.io.data.loading.api.DataSet;
import com.microsoft.reef.io.network.group.operators.Broadcast;
import com.microsoft.reef.io.network.group.operators.Reduce;
import com.microsoft.reef.io.network.nggroup.api.task.CommunicationGroupClient;
import com.microsoft.reef.io.network.nggroup.api.task.GroupCommClient;
import com.microsoft.reef.io.network.util.Pair;
import com.microsoft.reef.task.Task;
import com.microsoft.tang.annotations.Parameter;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public final class WorkerTask implements Task {

  private final CommunicationGroupClient communicationGroupClient;
  private final Broadcast.Receiver<ArrayList<Double>> broadCastReceiver;
  private final Reduce.Sender<ArrayList<Double>> initialParameterSender;
  private final Reduce.Sender<ArrayList<Double>> globalGradientSender;

  private final DataSet<LongWritable, Text> dataSet;
  private final int iterNum;
  private final double lambda;
  private int numberOfTrainingSets;
  private int dimension;
  private int m = 5;
  private double c1 = 0.0001;
  private double c2 = 0.9;

  @Inject
  WorkerTask(final GroupCommClient groupCommClient,
      final DataSet<LongWritable, Text> dataSet,
      @Parameter(TaskConfigurationOptions.Identifier.class) final String identifier,
      @Parameter(IterNum.class) final int iterNum,
      @Parameter(Lambda.class) final double lambda) {

    this.communicationGroupClient = groupCommClient.getCommunicationGroup(MLGroupCommucation.class);
    this.broadCastReceiver = communicationGroupClient.getBroadcastReceiver(BroadCastVector.class);
    this.initialParameterSender = communicationGroupClient.getReduceSender(ComputeInitialParameter.class);
    this.globalGradientSender = communicationGroupClient.getReduceSender(ComputeGlobalGradient.class);
    this.dataSet = dataSet;
    this.iterNum = iterNum;
    this.numberOfTrainingSets = 0;
    this.lambda = lambda;
  }

  @Override
  public final byte[] call(final byte[] memento) throws Exception {

    List<Pair<MyVector,Double>> trainingSets = new ArrayList<Pair<MyVector, Double>>();
    // Saves training set
    for(Pair<LongWritable,Text> entry: dataSet) {
      String line = entry.second.toString();
      String[] splitted = line.split("[\\s,;]+");
      dimension = splitted.length - 1;
      Pair<MyVector,Double> newSet = new Pair<MyVector,Double>(new MyVector(), Double.valueOf(splitted[dimension]));
      // Add 1 to specify constant parameter!
      newSet.first.add(1.);
      for(int i = 0; i < dimension; i++) {
        newSet.first.add(Double.valueOf(splitted[i]));
      }
      trainingSets.add(newSet);
      numberOfTrainingSets++;
    }

    System.out.println("Training Set #0: " + trainingSets.get(0).first.getArrayList().toString() + ", " +
        trainingSets.get(0).second);

    // Initializes weights vector to zero
    MyVector weights = new MyVector();
    MyVector oldWeights = new MyVector();
    for(int i = 0; i < dimension + 1; i++)
      weights.add(0.);

    List<MyVector> sVectorList = new ArrayList<MyVector>();
    List<MyVector> yVectorList = new ArrayList<MyVector>();
    List<Double> rhoList = new ArrayList<Double>();

    MyVector globalGradient;
    MyVector oldGradient = new MyVector();
    double currentError = -1;

    for(int i = 0; i < iterNum; i++) {
      // Get gradient for current weights
      MyVector gradient = new MyVector();
      // Get wtx's in advance
      List<Double> wtx = new ArrayList<Double>();
      for(int j = 0; j < numberOfTrainingSets; j++) {
        MyVector xj = trainingSets.get(j).first;
        // Gets inner product
        double result = MyVector.scalarProduct(weights, xj);
        wtx.add(result);
      }
      // Compute gradient
      for(int j = 0; j < dimension + 1; j++) {
        double result = 0.;
        for(int k = 0; k < numberOfTrainingSets; k++) {
          double xkj = trainingSets.get(k).first.get(j);
          result += 2 * (wtx.get(k) - trainingSets.get(k).second) * xkj;
        }
        gradient.add(result);
      }
      globalGradientSender.send(gradient.getArrayList());
      // Regularization
      globalGradient = new MyVector(broadCastReceiver.receive());
      globalGradient = MyVector.add(globalGradient, MyVector.constantMultiply(lambda, weights));
      System.out.println("Global Gradient: " + globalGradient.getArrayList().toString());
      // Caluclate Sk-1, Yk-1, rhok-1
      if (oldGradient.isExist() && oldWeights.isExist()) {
        MyVector yVector = MyVector.subtract(globalGradient, oldGradient);
        MyVector sVector = MyVector.subtract(weights, oldWeights);
        double rho_inverse = MyVector.scalarProduct(yVector, sVector);
        yVectorList.add(yVector);
        sVectorList.add(sVector);
        rhoList.add(1./rho_inverse);
      }
      // Start L-BFGS step
      MyVector hDiagonal = new MyVector();
      // Initially, H is a multiple of unit matrix
      if (i == 0) {
        for(int j = 0; j < dimension + 1; j++)
          hDiagonal.add(1);
      }
      else {
        MyVector sVector = sVectorList.get(sVectorList.size()-1);
        MyVector yVector = yVectorList.get(yVectorList.size()-1);
        double sty = MyVector.scalarProduct(sVector, yVector);
        double ysquare = MyVector.scalarProduct(yVector, yVector);
        System.out.println("SVector: " + sVector.getArrayList().toString());
        System.out.println("STY : " + sty);
        System.out.println("YSqaure: " + ysquare);
        for(int j = 0; j < dimension + 1; j++) {
          hDiagonal.add(sty/ysquare);
        }
      }
      System.out.println("hDiagonal: " + hDiagonal.getArrayList().toString());
      List<Double> alphaList = new ArrayList<Double>();
      // Copy globalGradient to q
      MyVector q = new MyVector(globalGradient.getArrayList());
      for (int j = sVectorList.size() - 1; j >= 0; j--) {
        double alpha = rhoList.get(j) * MyVector.scalarProduct(sVectorList.get(j), q);
        alphaList.add(0, alpha);
        q = MyVector.subtract(q, MyVector.constantMultiply(alpha, yVectorList.get(j)));
      }
      MyVector z = MyVector.simpleProduct(hDiagonal, q);
      for (int j = 0; i < sVectorList.size(); j++) {
        double beta = rhoList.get(j) * MyVector.scalarProduct(yVectorList.get(j), z);
        z = MyVector.add(z, MyVector.constantMultiply(alphaList.get(j) - beta, sVectorList.get(j)));
      }
      MyVector p = MyVector.constantMultiply(-1, z);

      System.out.println("z: " + z.getArrayList().toString());
      // Compute currentError if it's invalid
      if (currentError < 0) {
        currentError = 0;
        for (int j = 0; j < numberOfTrainingSets; j++) {
          double error = MyVector.scalarProduct(weights, trainingSets.get(j).first) - trainingSets.get(j).second;
          currentError += error * error;
        }
        currentError += 1/2 * lambda * MyVector.scalarProduct(weights, weights);
      }
      // Determines learning rate by backtracking search
      double learningRate = 1.;
      while(true) {
        double newError = 0;
        MyVector newWeights = MyVector.add(weights, MyVector.constantMultiply(learningRate, p));
        for (int j = 0; j < numberOfTrainingSets; j++) {
          double error = MyVector.scalarProduct(newWeights, trainingSets.get(j).first) - trainingSets.get(j).second;
          newError += error * error;
        }
        newError += 1/2 * lambda * MyVector.scalarProduct(newWeights, newWeights);

        if (newError > currentError + c1 * learningRate * MyVector.scalarProduct(globalGradient, p)) {
          learningRate *= 0.3;
          continue;
        }

        // Wolfe's second rule should be performed on global gradient, but it could not be in distributed system so we
        // use local gradient instead

        /*
        MyVector newGradient = new MyVector();
        List<Double> newWtx = new ArrayList<Double>();
        for(int j = 0; j < numberOfTrainingSets; j++) {
          MyVector xj = trainingSets.get(j).first;
          // Gets inner product
          double result = MyVector.scalarProduct(newWeights, xj);
          newWtx.add(result);
        }
        // Compute gradient
        for(int j = 0; j < dimension + 1; j++) {
          double result = 0.;
          for(int k = 0; k < numberOfTrainingSets; k++) {
            double xkj = trainingSets.get(k).first.get(j);
            result += 2 * newWtx.get(k) * xkj;
          }
          newGradient.add(result);
        }
        if (MyVector.scalarProduct(newGradient, p) < c2 * MyVector.scalarProduct())
        */

        oldWeights = weights;
        oldGradient = globalGradient;
        weights = newWeights;

        currentError = newError;
        break;
      }

      if(yVectorList.size() == m) {
        yVectorList.remove(0);
        sVectorList.remove(0);
        rhoList.remove(0);
      }
      System.out.println("Learning Rate: " + learningRate);
      System.out.println("Weights :" + weights.getArrayList().toString());
    }
    return null;
  }
}