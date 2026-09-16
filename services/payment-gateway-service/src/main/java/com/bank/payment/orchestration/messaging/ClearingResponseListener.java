package com.bank.payment.orchestration.messaging;

import com.bank.payment.orchestration.correlation.PartnerCorrelationService;
import com.bank.payment.orchestration.domain.ClearingAck;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Same self-correlating pattern as PartnerResponseListener, for clearing-rail settlement acks. */
@Component
public class ClearingResponseListener {

    private static final Logger log = LoggerFactory.getLogger(ClearingResponseListener.class);

    private final WorkflowClient workflowClient;
    private final PartnerCorrelationService correlationService;

    public ClearingResponseListener(WorkflowClient workflowClient, PartnerCorrelationService correlationService) {
        this.workflowClient = workflowClient;
        this.correlationService = correlationService;
    }

    @KafkaListener(topicPattern = "clearing\\.(target2|eba|chaps|swift)\\.ack")
    public void onClearingAck(ClearingAck ack) {
        if (!correlationService.markResolvedIfPending(ack.getCorrelationId())) {
            log.warn("Ignoring clearing ack for correlationId={} — not pending (duplicate or expired)",
                    ack.getCorrelationId());
            return;
        }
        String workflowId = extractWorkflowId(ack.getCorrelationId());
        WorkflowStub stub = workflowClient.newUntypedWorkflowStub(workflowId);
        stub.signal("clearingAck", ack);
    }

    private String extractWorkflowId(String correlationId) {
        int idx = correlationId.indexOf("::");
        return idx >= 0 ? correlationId.substring(0, idx) : correlationId;
    }
}
