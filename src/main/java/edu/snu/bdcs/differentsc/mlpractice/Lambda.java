package edu.snu.bdcs.differentsc.mlpractice;

import com.microsoft.tang.annotations.Name;
import com.microsoft.tang.annotations.NamedParameter;

/**
 * Number of iterations given by users
 */
@NamedParameter(doc = "regularization value", short_name = "lambda")
public class Lambda implements Name<Double> {
}
