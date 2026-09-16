package com.bank.payment.orchestration.activities;

import com.bank.payment.orchestration.domain.PaymentContext;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface ClearingActivities {

    @ActivityMethod
    String buildClearingMessage(PaymentContext context);

    /** Submits to the rail and registers the correlation id in Redis; returns that correlation id. */
    @ActivityMethod
    String submitToClearing(PaymentContext context, String clearingMessage, String targetWorkflowId);
}
