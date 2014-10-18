package edu.snu.bdcs.differentsc.mlpractice;

import com.microsoft.reef.driver.context.ActiveContext;
import com.microsoft.reef.driver.evaluator.EvaluatorRequestor;
import com.microsoft.reef.driver.task.CompletedTask;
import com.microsoft.reef.driver.task.TaskConfiguration;
import com.microsoft.reef.evaluator.context.parameters.ContextIdentifier;
import com.microsoft.reef.io.data.loading.api.DataLoadingService;
import com.microsoft.reef.io.network.nggroup.api.driver.CommunicationGroupDriver;
import com.microsoft.reef.io.network.nggroup.api.driver.GroupCommDriver;
import com.microsoft.reef.io.network.nggroup.impl.config.BroadcastOperatorSpec;
import com.microsoft.reef.io.network.nggroup.impl.config.ReduceOperatorSpec;
import com.microsoft.reef.io.serialization.SerializableCodec;
import com.microsoft.tang.Configuration;
import com.microsoft.tang.Injector;
import com.microsoft.tang.JavaConfigurationBuilder;
import com.microsoft.tang.Tang;
import com.microsoft.tang.annotations.Parameter;
import com.microsoft.tang.annotations.Unit;
import com.microsoft.tang.exceptions.InjectionException;
import com.microsoft.wake.EventHandler;

import javax.inject.Inject;
import java.util.logging.Level;
import java.util.logging.Logger;

@Unit
public final class MLPracticeDriver {

  private static final Logger LOG = Logger.getLogger(MLPracticeDriver.class.getName());

  private final EvaluatorRequestor requestor;
  private final DataLoadingService dataLoadingService;
  private final GroupCommDriver groupCommDriver;
  private final CommunicationGroupDriver MLCommGroup;
  private final int workerNum;
  private final int iterNum;
  private final double lambda;
  private int submittedWorkerTask;
  private String groupCommConfiguredMasterId;

  @Inject
  public MLPracticeDriver(final EvaluatorRequestor requestor,
    final GroupCommDriver groupCommDriver,
    final DataLoadingService dataLoadingService,
    @Parameter(Parameters.WorkerNum.class) int workerNum,
    @Parameter(Parameters.IterNum.class) int iterNum,
    @Parameter(Parameters.Lambda.class) double lambda) {
    LOG.log(Level.FINE, "Instantiated Driver");
    this.dataLoadingService = dataLoadingService;
    this.groupCommDriver = groupCommDriver;
    this.MLCommGroup = groupCommDriver.newCommunicationGroup(MLGroupCommucation.class, workerNum + 1);
    this.MLCommGroup.addBroadcast(GroupCommunicationNames.VectorBroadcaster.class, BroadcastOperatorSpec.newBuilder()
        .setSenderId("ControllerTask")
        .setDataCodecClass(SerializableCodec.class)
        .build()).addReduce(GroupCommunicationNames.SumVectorReducer.class, ReduceOperatorSpec.newBuilder()
        .setReceiverId("ControllerTask")
        .setDataCodecClass(SerializableCodec.class)
        .setReduceFunctionClass(ReduceFunctions.AddVectorReduceFunc.class)
        .build()).addReduce(GroupCommunicationNames.AverageVectorReducer.class, ReduceOperatorSpec.newBuilder()
        .setReceiverId("ControllerTask")
        .setDataCodecClass(SerializableCodec.class)
        .setReduceFunctionClass(ReduceFunctions.AverageVectorReduceFunc.class)
        .build()).addReduce(GroupCommunicationNames.SumDoubleReducer.class, ReduceOperatorSpec.newBuilder()
        .setReceiverId("ControllerTask")
        .setDataCodecClass(SerializableCodec.class)
        .setReduceFunctionClass(ReduceFunctions.AddDoubleReduceFunc.class).build())
        .finalise();

    this.requestor = requestor;
    this.workerNum = workerNum;
    this.iterNum = iterNum;
    this.lambda = lambda;
    this.submittedWorkerTask = 0;
  }

  public class ActiveContextHandler implements EventHandler<ActiveContext> {
    @Override
    public void onNext(final ActiveContext activeContext) {
      String contextId = activeContext.getId();

      // Context is for WorkerTask, not added task for group communication yet.
      if(dataLoadingService.isDataLoadedContext(activeContext)) {
        final Configuration contextConf = groupCommDriver.getContextConfiguration();
        final Configuration serviceConf = groupCommDriver.getServiceConfiguration();

        activeContext.submitContextAndService(contextConf, serviceConf);
      }
      // Context is ready for task with group communication conf
      else if (groupCommDriver.isConfigured(activeContext)) {
        final Configuration basicTaskConf;
        if (contextId.equals(groupCommConfiguredMasterId)) {
          basicTaskConf = TaskConfiguration.CONF
              .set(TaskConfiguration.IDENTIFIER, "ControllerTask")
              .set(TaskConfiguration.TASK, ControllerTask.class)
              .build();
        }
        else {
          basicTaskConf = TaskConfiguration.CONF
              .set(TaskConfiguration.IDENTIFIER, "WorkerTask_" + (submittedWorkerTask++))
              .set(TaskConfiguration.TASK, WorkerTask.class)
              .build();
        }
        final JavaConfigurationBuilder partialTaskConfigurationBuilder = Tang.Factory.getTang()
            .newConfigurationBuilder();
        partialTaskConfigurationBuilder.addConfiguration(basicTaskConf);
        partialTaskConfigurationBuilder.bindNamedParameter(Parameters.IterNum.class, Integer.toString(iterNum));
        partialTaskConfigurationBuilder.bindNamedParameter(Parameters.Lambda.class, Double.toString(lambda));
        final Configuration partialTaskConfiguration = partialTaskConfigurationBuilder.build();
        MLCommGroup.addTask(partialTaskConfiguration);

        final Configuration taskConfiguration = groupCommDriver.getTaskConfiguration(partialTaskConfiguration);

        activeContext.submitTask(taskConfiguration);
      }
      // Context is for ControllerTask, not added task for group communication yet. Only called once.
      else {
        final Configuration contextConf = groupCommDriver.getContextConfiguration();
        final Configuration serviceConf = groupCommDriver.getServiceConfiguration();
        groupCommConfiguredMasterId = contextId(contextConf);

        activeContext.submitContextAndService(contextConf, serviceConf);
      }
    }

    private String contextId(final Configuration contextConf) {
      try {
        final Injector injector = Tang.Factory.getTang().newInjector(contextConf);
        return injector.getNamedInstance(ContextIdentifier.class);
      } catch (final InjectionException e) {
        throw new RuntimeException("Unable to inject context identifier from context conf", e);
      }
    }
  }

  public class CompleteTaskHandler implements EventHandler<CompletedTask> {
    @Override
    public void onNext(final CompletedTask completedTask) {
      completedTask.getActiveContext().close();
    }
  }


}