package edu.snu.bdcs.differentsc.mlpractice;

import com.microsoft.tang.annotations.Name;
import com.microsoft.tang.annotations.NamedParameter;

/**
 * Number of iterations given by users
 */
@NamedParameter(doc = "Num", short_name = "iters")
public class IterNum implements Name<Integer> {
}
