/**
 * Copyright (C) 2014 Microsoft Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.snu.bdcs.differentsc.mlpractice;

import com.microsoft.reef.client.DriverConfiguration;
import com.microsoft.reef.client.DriverLauncher;
import com.microsoft.reef.client.LauncherStatus;
import com.microsoft.reef.driver.evaluator.EvaluatorRequest;
import com.microsoft.reef.io.data.loading.api.DataLoadingRequestBuilder;
import com.microsoft.reef.io.network.nggroup.impl.driver.GroupCommService;
import com.microsoft.reef.runtime.local.client.LocalRuntimeConfiguration;
import com.microsoft.reef.util.EnvironmentUtils;
import com.microsoft.tang.Configuration;
import com.microsoft.tang.ConfigurationBuilder;
import com.microsoft.tang.JavaConfigurationBuilder;
import com.microsoft.tang.Tang;
import com.microsoft.tang.exceptions.BindException;
import com.microsoft.tang.exceptions.InjectionException;
import org.apache.hadoop.mapred.TextInputFormat;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The Client for MLPractice.
 */
public final class MLPracticeClient {

  private static final Logger LOG = Logger.getLogger(MLPracticeClient.class.getName());

  /**
   * Number of milliseconds to wait for the job to complete.
   */
  private static final int JOB_TIMEOUT = 10000; // 10 sec.
  private static int NUM_WORKERS = 3;
  private static int NUM_ITERS = 5;

  /**
   * @return the configuration of the MLPractice driver.
   */
  public static Configuration getDriverConfiguration() {

    final EvaluatorRequest controllerRequest = EvaluatorRequest.newBuilder()
        .setNumber(1)
        .setMemory(128)
        .build();

    final Configuration dataLoadConfiguration = new DataLoadingRequestBuilder()
        .setComputeRequest(controllerRequest)
        .setMemoryMB(128)
        .setInputFormatClass(TextInputFormat.class)
        .setInputPath("/input.txt")
        .setNumberOfDesiredSplits(NUM_WORKERS)
        .setDriverConfigurationModule(DriverConfiguration.CONF
            .set(DriverConfiguration.DRIVER_IDENTIFIER, "MLDriver")
            .set(DriverConfiguration.GLOBAL_LIBRARIES, EnvironmentUtils.getClassLocation(MLPracticeClient.class))
            .set(DriverConfiguration.ON_CONTEXT_ACTIVE, MLPracticeDriver.ActiveContextHandler.class)
            .set(DriverConfiguration.ON_TASK_COMPLETED, MLPracticeDriver.CompleteTaskHandler.class))
        .build();

    JavaConfigurationBuilder cb = Tang.Factory.getTang().newConfigurationBuilder(dataLoadConfiguration);
    cb.bindNamedParameter(WorkerNum.class, Integer.toString(NUM_WORKERS));
    cb.bindNamedParameter(IterNum.class, Integer.toString(NUM_ITERS));

    Configuration driverConfiguration = Tang.Factory.getTang().newConfigurationBuilder(cb.build(),
        GroupCommService.getConfiguration()).build();

    return driverConfiguration;
  }

  public static LauncherStatus runMLPractice(final Configuration runtimeConf, final int timeOut)
    throws BindException, InjectionException {
    final Configuration driverConf = getDriverConfiguration();

    // DriverLauncher launches Driver to run the application.
    return DriverLauncher.getLauncher(runtimeConf).run(driverConf, timeOut);
  }

  public static void main(final String[] args) throws BindException, InjectionException {
    final Configuration runtimeConfiguration = LocalRuntimeConfiguration.CONF
        .set(LocalRuntimeConfiguration.NUMBER_OF_THREADS, NUM_WORKERS + 1)
        .build();

    final LauncherStatus status = runMLPractice(runtimeConfiguration, JOB_TIMEOUT);
    LOG.log(Level.INFO, "REEF job completed: {0}", status);
  }
}