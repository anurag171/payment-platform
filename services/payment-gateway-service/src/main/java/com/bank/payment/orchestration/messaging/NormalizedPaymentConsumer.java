package com.bank.payment.orchestration.messaging;

import com.bank.payment.orchestration.domain.PaymentEnvelope;
import com.bank.payment.orchestration.workflow.InboundNormalizationWorkflow;
import com.bank.payment.orchestration.workflow.TaskQueues;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * The bootstrap point: consumes the normalized Kafka topic and starts one
 * InboundNormalizationWorkflow execution per payment, keyed by UETR. This is the only
 * place a "new payment" turns into a Temporal workflow — everything downstream is child
 * workflows chained off this root.
 *
 * Deployed inside payment-gateway-service, which is a Temporal CLIENT only (no Worker
 * of its own) — so unlike the monolith, there's no local "task-queue" config property
 * for this service to read. It targets inbound-normalization-workflow-service's queue
 * directly via the shared TaskQueues constant instead, same as every cross-service
 * workflow/activity stub elsewhere in the codebase.
 */
@Component
public class NormalizedPaymentConsumer {

    private final WorkflowClient workflowClient;

    public NormalizedPaymentConsumer(WorkflowClient workflowClient) {
        this.workflowClient = workflowClient;
    }

    @KafkaListener(topics = "${payment-orchestration.kafka-topics.normalized-payments}")
    public void onNormalizedPayment(PaymentEnvelope envelope) {
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setTaskQueue(TaskQueues.INBOUND_NORMALIZATION)
                .setWorkflowId(envelope.getUetr())
                // Duplicate delivery of the same UETR is rejected by Temporal at start
                // time (WorkflowExecutionAlreadyStarted) rather than spawning a second
                // execution — this is the "free idempotency" benefit of UETR = workflow id.
                .build();

        InboundNormalizationWorkflow workflow =
                workflowClient.newWorkflowStub(InboundNormalizationWorkflow.class, options);

        WorkflowClient.start(workflow::normalize, envelope);
    }
}

