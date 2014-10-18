package edu.snu.bdcs.differentsc.mlpractice;

import com.microsoft.tang.annotations.Name;
import com.microsoft.tang.annotations.NamedParameter;

/**
 * Classes for named parameter
 */
public class Parameters {

  /**
   * Number of workers to process ML
   */
  @NamedParameter(doc = "Number of workers to process ML", default_value = "3", short_name = "workers")
  public static class WorkerNum implements Name<Integer> {
  }

  /**
   * Number of iterations given by users
   */
  @NamedParameter(doc = "Num", short_name = "iters", default_value = "10")
  public static class IterNum implements Name<Integer> {

  }

  /**
   * Number of iterations given by users
   */
  @NamedParameter(doc = "regularization value", short_name = "lambda", default_value = "0.001")
  public static class Lambda implements Name<Double> {
  }

  @NamedParameter(doc = "HDFS input file path", short_name = "input", default_value = "/input.csv")
  public static class InputFilePath implements Name<String> {
  }
}
