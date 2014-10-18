package edu.snu.bdcs.differentsc.mlpractice;

import com.microsoft.tang.annotations.Name;
import com.microsoft.tang.annotations.NamedParameter;

/**
 * Simple name for group communication operators
 */
public class GroupCommunicationNames {

  @NamedParameter
  public static class AverageVectorReducer implements Name<String> {
  }

  @NamedParameter
  public static class SumDoubleReducer implements Name<String> {
  }

  @NamedParameter()
  public static class SumVectorReducer implements Name<String> {
  }

  @NamedParameter()
  public static class VectorBroadcaster implements Name<String> {
  }
}
