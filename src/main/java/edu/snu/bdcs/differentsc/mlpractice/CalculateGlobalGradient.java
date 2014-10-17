package edu.snu.bdcs.differentsc.mlpractice;

import com.microsoft.reef.io.network.group.operators.Reduce;

import javax.inject.Inject;
import java.util.ArrayList;

/**
 * Created by Gyewon on 2014. 10. 17..
 */
public class CalculateGlobalGradient implements Reduce.ReduceFunction<ArrayList<Double>> {
  @Inject
  CalculateGlobalGradient() {
  }
  @Override
  public ArrayList<Double> apply(final Iterable<ArrayList<Double>> vectorIterable) {
    return new ArrayList<Double>();
  }
}
