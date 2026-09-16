package com.bank.payment.orchestration.activities;

import com.bank.payment.orchestration.domain.AccountData;
import com.bank.payment.orchestration.domain.CustomerData;
import com.bank.payment.orchestration.domain.PaymentContext;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface EnrichmentActivities {

    @ActivityMethod
    CustomerData enrichCustomer(PaymentContext context);

    @ActivityMethod
    AccountData enrichAccount(PaymentContext context);
}
