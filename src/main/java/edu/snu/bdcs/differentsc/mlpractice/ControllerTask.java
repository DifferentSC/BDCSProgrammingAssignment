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

import com.microsoft.reef.driver.task.TaskConfigurationOptions;
import com.microsoft.reef.io.network.group.operators.Broadcast;
import com.microsoft.reef.io.network.group.operators.Reduce;
import com.microsoft.reef.io.network.nggroup.api.task.CommunicationGroupClient;
import com.microsoft.reef.io.network.nggroup.api.task.GroupCommClient;
import com.microsoft.reef.task.Task;
import com.microsoft.tang.annotations.Parameter;

import javax.inject.Inject;
import java.util.ArrayList;

public final class ControllerTask implements Task {

  private final CommunicationGroupClient communicationGroupClient;
  private final Broadcast.Sender<ArrayList<Double>> broadCastSender;
  private final Reduce.Receiver<ArrayList<Double>> initialParameterReceiver;
  private final Reduce.Receiver<ArrayList<Double>> globalGradientReceiver;

  @Inject
  ControllerTask(final GroupCommClient groupCommClient,
      @Parameter(TaskConfigurationOptions.Identifier.class) final String identifier,
      @Parameter(IterNum.class) final int iterNum) {

    this.communicationGroupClient = groupCommClient.getCommunicationGroup(MLGroupCommucation.class);
    this.broadCastSender = communicationGroupClient.getBroadcastSender(BroadCastVector.class);
    this.initialParameterReceiver = communicationGroupClient.getReduceReceiver(ComputeGlobalGradient.class);
    this.globalGradientReceiver = communicationGroupClient.getReduceReceiver(ComputeInitialParameter.class);
  }

  @Override
  public final byte[] call(final byte[] memento) throws Exception {
    return null;
  }
}