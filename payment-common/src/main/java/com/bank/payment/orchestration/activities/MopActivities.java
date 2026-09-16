package com.bank.payment.orchestration.activities;

import com.bank.payment.orchestration.domain.PaymentContext;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface MopActivities {

    /** Sets context.mop; returns true if derivation succeeded and is unambiguous. */
    @ActivityMethod
    boolean deriveMop(PaymentContext context);

    /** Sets context.clearingRail based on the derived MOP, currency and corridor. */
    @ActivityMethod
    void selectClearingRail(PaymentContext context);
}
