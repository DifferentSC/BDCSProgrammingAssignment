package edu.snu.bdcs.differentsc.mlpractice;

import com.microsoft.tang.annotations.Name;
import com.microsoft.tang.annotations.NamedParameter;

/**
 * Created by Gyewon on 2014. 10. 18..
 */
public class Parameters {

  /**
   * Number of workers to process ML
   */
  @NamedParameter(doc = "Number of workers to process ML", short_name = "workers")
  public static class WorkerNum implements Name<Integer> {
  }

  /**
   * Number of iterations given by users
   */
  @NamedParameter(doc = "Num", short_name = "iters")
  public static class IterNum implements Name<Integer> {

  }

  /**
   * Number of iterations given by users
   */
  @NamedParameter(doc = "regularization value", short_name = "lambda")
  public static class Lambda implements Name<Double> {
  }
}
