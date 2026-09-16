package com.bank.payment.orchestration.messaging;

import com.bank.payment.orchestration.correlation.PartnerCorrelationService;
import com.bank.payment.orchestration.domain.PartnerResponse;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Generic listener across all partner response topics. Extracts the target workflow id
 * from the correlation id (format "{workflowId}::{partnerCode}") and signals that
 * workflow directly — no correlation-id-to-workflow lookup table needed, only a Redis
 * check to avoid double-delivery.
 */
@Component
public class PartnerResponseListener {

    private static final Logger log = LoggerFactory.getLogger(PartnerResponseListener.class);
    private static final String SIGNAL_NAME_PREFIX = "partnerResponse::";

    private final WorkflowClient workflowClient;
    private final PartnerCorrelationService correlationService;

    public PartnerResponseListener(WorkflowClient workflowClient, PartnerCorrelationService correlationService) {
        this.workflowClient = workflowClient;
        this.correlationService = correlationService;
    }

    @KafkaListener(topicPattern = "partner\\.(sanctions|fraud|fx|booking)\\.response")
    public void onPartnerResponse(PartnerResponse response) {
        if (!correlationService.markResolvedIfPending(response.getCorrelationId())) {
            log.warn("Ignoring partner response for correlationId={} — not pending (duplicate or expired)",
                    response.getCorrelationId());
            return;
        }

        String workflowId = extractWorkflowId(response.getCorrelationId());
        String signalName = SIGNAL_NAME_PREFIX + response.getPartnerType().name();

        WorkflowStub stub = workflowClient.newUntypedWorkflowStub(workflowId);
        stub.signal(signalName, response);
    }

    private String extractWorkflowId(String correlationId) {
        int idx = correlationId.indexOf("::");
        return idx >= 0 ? correlationId.substring(0, idx) : correlationId;
    }
}
