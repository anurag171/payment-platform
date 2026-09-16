package com.bank.payment.orchestration.activities;

import com.bank.payment.orchestration.domain.PaymentContext;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface AuditActivities {

    @ActivityMethod
    void recordStage(String uetr, String workflowType, String stage, PaymentContext context);

    @ActivityMethod
    void savePayment(PaymentContext context);
}
