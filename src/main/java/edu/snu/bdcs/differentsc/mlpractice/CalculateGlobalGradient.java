package edu.snu.bdcs.differentsc.mlpractice;

import com.microsoft.reef.io.network.group.operators.Reduce;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by Gyewon on 2014. 10. 17..
 */
public class CalculateGlobalGradient implements Reduce.ReduceFunction<ArrayList<Double>> {
  @Inject
  CalculateGlobalGradient() {
  }
  @Override
  public ArrayList<Double> apply(final Iterable<ArrayList<Double>> vectorIterable) {
    Iterator<ArrayList<Double>> vectorIterator = vectorIterable.iterator();
    ArrayList<Double> results = vectorIterator.next();
    while(vectorIterator.hasNext()) {
      ArrayList<Double> vector = vectorIterator.next();
      for(int i = 0; i < results.size(); i++) {
        results.set(i, results.get(i) + vector.get(i));
      }
    }
    return results;
  }
}
