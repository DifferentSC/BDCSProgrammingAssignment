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
package edu.snu.cms.reef.tutorial;

import com.microsoft.reef.client.DriverConfiguration;
import com.microsoft.reef.client.DriverLauncher;
import com.microsoft.reef.client.LauncherStatus;
import com.microsoft.reef.runtime.local.client.LocalRuntimeConfiguration;
import com.microsoft.reef.util.EnvironmentUtils;
import com.microsoft.tang.Configuration;
import com.microsoft.tang.JavaConfigurationBuilder;
import com.microsoft.tang.Tang;
import com.microsoft.tang.exceptions.BindException;
import com.microsoft.tang.exceptions.InjectionException;

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

  /**
   * @return the configuration of the MLPractice driver.
   */
  public static Configuration getDriverConfiguration() {
    final Configuration driverConfiguration = DriverConfiguration.CONF
        .set(DriverConfiguration.DRIVER_IDENTIFIER, "MLPractice!")
        .set(DriverConfiguration.GLOBAL_LIBRARIES, EnvironmentUtils.getClassLocation(MLPracticeDriver.class))
        .build();
    JavaConfigurationBuilder cb = Tang.Factory.getTang().newConfigurationBuilder();

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
        .set(LocalRuntimeConfiguration.NUMBER_OF_THREADS, 1)
        .build();

    final LauncherStatus status = runMLPractice(runtimeConfiguration, JOB_TIMEOUT);
    LOG.log(Level.INFO, "REEF job completed: {0}", status);
  }
}