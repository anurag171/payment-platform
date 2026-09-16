package com.bank.payment.orchestration.config;

import com.bank.payment.orchestration.workflow.impl.RepairWorkflowImpl;
import com.bank.payment.orchestration.workflow.TaskQueues;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * This service registers a SINGLE workflow or activity implementation against a SINGLE
 * task queue (TaskQueues.REPAIR_WORKFLOW) — the whole point of the decomposition. Every
 * cross-service call (child-workflow stub, activity stub) elsewhere in the codebase
 * targets this queue explicitly via ChildWorkflowOptions/ActivityOptions.setTaskQueue(),
 * never relying on a shared default.
 */
@Configuration
public class TemporalConfig {

    @Bean
    public WorkflowServiceStubs workflowServiceStubs(
            @Value("${payment-orchestration.temporal.target}") String target) {
        return WorkflowServiceStubs.newServiceStubs(
                WorkflowServiceStubsOptions.newBuilder().setTarget(target).build());
    }

    @Bean
    public WorkflowClient workflowClient(
            WorkflowServiceStubs serviceStubs,
            @Value("${payment-orchestration.temporal.namespace}") String namespace) {
        return WorkflowClient.newInstance(serviceStubs,
                WorkflowClientOptions.newBuilder().setNamespace(namespace).build());
    }

    @Bean
    public WorkerFactory workerFactory(WorkflowClient client) {
        WorkerFactory factory = WorkerFactory.newInstance(client);
        Worker worker = factory.newWorker(TaskQueues.REPAIR_WORKFLOW);
        worker.registerWorkflowImplementationTypes(RepairWorkflowImpl.class);
        
        return factory;
    }

    @Bean
    public SmartLifecycle temporalWorkerLifecycle(WorkerFactory workerFactory) {
        return new SmartLifecycle() {
            private volatile boolean running = false;

            @Override
            public void start() {
                workerFactory.start();
                running = true;
            }

            @Override
            public void stop() {
                workerFactory.shutdown();
                running = false;
            }

            @Override
            public boolean isRunning() {
                return running;
            }

            @Override
            public int getPhase() {
                return Integer.MAX_VALUE - 1;
            }
        };
    }
}
