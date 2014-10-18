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
  private final Broadcast.Receiver<ArrayList<Double>> vectorBroadcastReceiver;
  private final Reduce.Sender<ArrayList<Double>> sumVectorReduceSender;
  private final Reduce.Sender<ArrayList<Double>> averageVectorReduceSender;
  private final Reduce.Sender<Double> sumDoubleReduceSender;

  private final DataSet<LongWritable, Text> dataSet;
  private final int iterNum;
  private final double lambda;
  private int numberOfTrainingSets;
  private int dimension;
  private int m = 5;
  private double c = 0.0001;

  @Inject
  WorkerTask(final GroupCommClient groupCommClient,
      final DataSet<LongWritable, Text> dataSet,
      @Parameter(TaskConfigurationOptions.Identifier.class) final String identifier,
      @Parameter(Parameters.IterNum.class) final int iterNum,
      @Parameter(Parameters.Lambda.class) final double lambda) {

    this.communicationGroupClient = groupCommClient.getCommunicationGroup(MLGroupCommucation.class);
    this.vectorBroadcastReceiver = communicationGroupClient
        .getBroadcastReceiver(GroupCommunicationNames.VectorBroadcaster.class);
    this.sumVectorReduceSender = communicationGroupClient
        .getReduceSender(GroupCommunicationNames.SumVectorReducer.class);
    this.averageVectorReduceSender = communicationGroupClient
        .getReduceSender(GroupCommunicationNames.AverageVectorReducer.class);
    this.sumDoubleReduceSender = communicationGroupClient
        .getReduceSender(GroupCommunicationNames.SumDoubleReducer.class);
    this.dataSet = dataSet;
    this.iterNum = iterNum;
    this.numberOfTrainingSets = 0;
    this.lambda = lambda;
  }

  @Override
  public final byte[] call(final byte[] memento) throws Exception {

    final List<Pair<MyVector,Double>> trainingSets = new ArrayList<Pair<MyVector, Double>>();
    // Saves training set
    for(Pair<LongWritable,Text> entry: dataSet) {
      final String line = entry.second.toString();
      final String[] splitted = line.split("[\\s,;]+");
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
      System.out.println("Iteration " + i);
      // Get gradient for current weights
      final MyVector gradient = new MyVector();
      // Get wtx's in advance
      final List<Double> wtx = new ArrayList<Double>();
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
      sumVectorReduceSender.send(gradient.getArrayList());
      // Regularization
      globalGradient = new MyVector(vectorBroadcastReceiver.receive());
      globalGradient = MyVector.add(globalGradient, MyVector.constantMultiply(lambda, weights));
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
        final MyVector sVector = sVectorList.get(sVectorList.size()-1);
        final MyVector yVector = yVectorList.get(yVectorList.size()-1);
        final double sty = MyVector.scalarProduct(sVector, yVector);
        final double ysquare = MyVector.scalarProduct(yVector, yVector);
        for(int j = 0; j < dimension + 1; j++) {
          hDiagonal.add(sty/ysquare);
        }
      }
      final List<Double> alphaList = new ArrayList<Double>();
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

      // Compute currentError if it's invalid
      if (currentError < 0) {
        currentError = 0;
        for (int j = 0; j < numberOfTrainingSets; j++) {
          double error = MyVector.scalarProduct(weights, trainingSets.get(j).first) - trainingSets.get(j).second;
          currentError += error * error;
        }
        currentError += 1/2 * lambda * MyVector.scalarProduct(weights, weights);
      }
      // Determines learning rate by backtracking line search
      double learningRate = 1.;
      while(true) {
        double newError = 0;
        final MyVector newWeights = MyVector.add(weights, MyVector.constantMultiply(learningRate, p));
        for (int j = 0; j < numberOfTrainingSets; j++) {
          double error = MyVector.scalarProduct(newWeights, trainingSets.get(j).first) - trainingSets.get(j).second;
          newError += error * error;
        }
        newError += 1/2 * lambda * MyVector.scalarProduct(newWeights, newWeights);

        // Armijo-Goldstein condition
        if (newError > currentError + c * learningRate * MyVector.scalarProduct(globalGradient, p)) {
          // Try again with smaller learning rate
          learningRate *= 0.3;
          continue;
        }

        oldWeights = weights;
        oldGradient = globalGradient;
        weights = newWeights;

        // Re-use current calculated error
        currentError = newError;
        break;
      }

      if(yVectorList.size() == m) {
        yVectorList.remove(0);
        sVectorList.remove(0);
        rhoList.remove(0);
      }
      System.out.println("Learning Rate: " + learningRate);
      System.out.println("Weights :" + weights.getArrayList().toString() + "\n");

      // Send local weights to Controller
      averageVectorReduceSender.send(weights.getArrayList());
      final MyVector globalWeights = new MyVector(vectorBroadcastReceiver.receive());
      // One of the workers has converged
      if (globalWeights.containsNaN())
        break;
      sumDoubleReduceSender.send(currentError);
    }

    return null;
  }
}