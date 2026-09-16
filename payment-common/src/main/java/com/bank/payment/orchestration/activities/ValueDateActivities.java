package com.bank.payment.orchestration.activities;

import com.bank.payment.orchestration.domain.PaymentContext;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface ValueDateActivities {

    /** Mutates context.valueDate and context.warehoused in place, then returns it. */
    @ActivityMethod
    PaymentContext deriveValueDate(PaymentContext context);
}
