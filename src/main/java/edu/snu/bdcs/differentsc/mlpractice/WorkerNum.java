package edu.snu.bdcs.differentsc.mlpractice;

import com.microsoft.tang.annotations.Name;
import com.microsoft.tang.annotations.NamedParameter;

/**
 * Number of workers to process ML
 */

@NamedParameter(doc = "Number of workers to process ML", short_name = "workers")
public class WorkerNum implements Name<Integer>{
}
