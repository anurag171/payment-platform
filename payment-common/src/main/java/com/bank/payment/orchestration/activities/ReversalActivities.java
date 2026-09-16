package com.bank.payment.orchestration.activities;

import com.bank.payment.orchestration.domain.PaymentContext;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Compensating actions for PaymentProcessingWorkflow's saga. Only covers steps that
 * create real external/ledger state (FX deal, ledger booking) — Sanctions/Fraud are pure
 * screens with nothing to undo, so they're deliberately not represented here. If your
 * real sanctions/fraud systems open a case or hold that needs closing on reversal, add a
 * matching method following the same pattern.
 */
@ActivityInterface
public interface ReversalActivities {

    @ActivityMethod
    void reverseFxDeal(PaymentContext context);

    @ActivityMethod
    void reverseBooking(PaymentContext context);

    @ActivityMethod
    void notifyReversal(PaymentContext context, String reason);
}
