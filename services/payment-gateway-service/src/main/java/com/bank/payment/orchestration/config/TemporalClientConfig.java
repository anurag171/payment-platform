package com.bank.payment.orchestration.config;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Client-only Temporal wiring — this service starts/signals workflows but hosts no Worker. */
@Configuration
public class TemporalClientConfig {

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
}
