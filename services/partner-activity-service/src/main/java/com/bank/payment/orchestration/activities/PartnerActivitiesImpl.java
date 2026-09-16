package com.bank.payment.orchestration.activities;

import com.bank.payment.orchestration.correlation.PartnerCorrelationService;
import com.bank.payment.orchestration.domain.PartnerRequest;
import com.bank.payment.orchestration.domain.PartnerResponse;
import com.bank.payment.orchestration.domain.PartnerType;
import com.bank.payment.orchestration.domain.PaymentContext;
import com.bank.payment.orchestration.messaging.PartnerRequestPublisher;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
public class PartnerActivitiesImpl implements PartnerActivities {

    private final PartnerRequestPublisher publisher;
    private final PartnerCorrelationService correlationService;

    public PartnerActivitiesImpl(PartnerRequestPublisher publisher, PartnerCorrelationService correlationService) {
        this.publisher = publisher;
        this.correlationService = correlationService;
    }

    @Override
    public String sendPartnerRequest(PaymentContext context, PartnerType partnerType, String targetWorkflowId) {
        String correlationId = correlationService.buildCorrelationId(targetWorkflowId, partnerType.name());
        correlationService.registerPending(correlationId);

        PartnerRequest request = new PartnerRequest(
                correlationId, context.getUetr(), partnerType,
                context.getCanonicalPsd2Message(), Instant.now());
        publisher.send(request);
        return correlationId;
    }

    @Override
    public boolean aggregatePartnerResults(PaymentContext context, Map<PartnerType, PartnerResponse> responses) {
        return context.getRequiredPartners().stream()
                .allMatch(type -> {
                    PartnerResponse response = responses.get(type);
                    return response != null && response.isApproved();
                });
    }
}
