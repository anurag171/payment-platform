package com.bank.payment.orchestration.activities;

import com.bank.payment.orchestration.correlation.PartnerCorrelationService;
import com.bank.payment.orchestration.domain.PaymentContext;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class ClearingActivitiesImpl implements ClearingActivities {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final PartnerCorrelationService correlationService;

    public ClearingActivitiesImpl(KafkaTemplate<String, String> kafkaTemplate,
                                   PartnerCorrelationService correlationService) {
        this.kafkaTemplate = kafkaTemplate;
        this.correlationService = correlationService;
    }

    @Override
    public String buildClearingMessage(PaymentContext context) {
        // TODO: replace with a real rail-specific message builder (TARGET2 MX, EBA,
        // CHAPS, or SWIFT MT/gpi cover) — this is a structural placeholder.
        return """
                <ClearingSubmission>
                  <Rail>%s</Rail>
                  <UETR>%s</UETR>
                  <Payload><![CDATA[%s]]></Payload>
                </ClearingSubmission>
                """.formatted(context.getClearingRail(), context.getUetr(), context.getCanonicalPsd2Message());
    }

    @Override
    public String submitToClearing(PaymentContext context, String clearingMessage, String targetWorkflowId) {
        String correlationId = correlationService.buildCorrelationId(targetWorkflowId, "clearing");
        correlationService.registerPending(correlationId);

        String topic = "clearing." + context.getClearingRail().name().toLowerCase() + ".submit";
        kafkaTemplate.send(topic, correlationId, clearingMessage);
        return correlationId;
    }
}
