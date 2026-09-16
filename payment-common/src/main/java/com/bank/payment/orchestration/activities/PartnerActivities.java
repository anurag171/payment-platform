package com.bank.payment.orchestration.activities;

import com.bank.payment.orchestration.domain.PartnerResponse;
import com.bank.payment.orchestration.domain.PartnerType;
import com.bank.payment.orchestration.domain.PaymentContext;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import java.util.Map;

@ActivityInterface
public interface PartnerActivities {

    /** Publishes the request and registers the correlation id in Redis; returns that correlation id. */
    @ActivityMethod
    String sendPartnerRequest(PaymentContext context, PartnerType partnerType, String targetWorkflowId);

    @ActivityMethod
    boolean aggregatePartnerResults(PaymentContext context, Map<PartnerType, PartnerResponse> responses);
}
