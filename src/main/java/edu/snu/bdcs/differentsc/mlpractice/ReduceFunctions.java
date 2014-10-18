package edu.snu.bdcs.differentsc.mlpractice;

import com.microsoft.reef.io.network.group.operators.Reduce;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by Gyewon on 2014. 10. 18..
 */
public class ReduceFunctions {

  public static class AddVectorReduceFunc implements Reduce.ReduceFunction<ArrayList<Double>> {
    @Inject
    AddVectorReduceFunc() {
    }
    @Override
    public ArrayList<Double> apply(final Iterable<ArrayList<Double>> vectorIterable) {
      final Iterator<ArrayList<Double>> vectorIterator = vectorIterable.iterator();
      final ArrayList<Double> results = vectorIterator.next();
      while(vectorIterator.hasNext()) {
        final ArrayList<Double> vector = vectorIterator.next();
        for(int i = 0; i < results.size(); i++) {
          results.set(i, results.get(i) + vector.get(i));
        }
      }
      return results;
    }
  }

  public static class AverageVectorReduceFunc implements Reduce.ReduceFunction <ArrayList<Double>> {

    @Inject
    public AverageVectorReduceFunc() {
    }

    @Override
    public ArrayList<Double> apply(Iterable<ArrayList<Double>> iterable) {
      final Iterator<ArrayList<Double>> iterator = iterable.iterator();
      int count = 1;
      final ArrayList<Double> averageWeights = iterator.next();

      while(iterator.hasNext()) {
        final ArrayList<Double> weight = iterator.next();
        for (int i = 0; i < averageWeights.size(); i++)
          averageWeights.set(i, averageWeights.get(i) + weight.get(i));
        count++;
      }

      for(int i = 0; i < averageWeights.size(); i++)
        averageWeights.set(i, averageWeights.get(i) / (double) count);
      return averageWeights;
    }

  }

  public static class AddDoubleReduceFunc implements Reduce.ReduceFunction<Double> {
    @Inject
    public AddDoubleReduceFunc() {
    }

    @Override
    public Double apply(Iterable<Double> iterable) {
      final Iterator<Double> iterator = iterable.iterator();
      double sumWeight = iterator.next();

      while(iterator.hasNext()) {
        sumWeight += iterator.next();
      }

      return sumWeight;
    }
  }

}
