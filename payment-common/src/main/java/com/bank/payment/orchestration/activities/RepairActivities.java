package com.bank.payment.orchestration.activities;

import com.bank.payment.orchestration.domain.PaymentContext;
import com.bank.payment.orchestration.domain.RepairSubmission;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * NOTE: this interface changed shape during the microservices split. The monolith's
 * RepairActivitiesImpl used to call EnrichmentActivities/MopActivities directly as
 * in-process Java objects to "requalify" a payment — that only worked because
 * everything shared one JVM. Once activities are split across services, an activity
 * implementation has no way to call another activity's implementation directly; only a
 * *workflow* can invoke multiple activities (each via its own stub, potentially against
 * a different task queue). So the requalify step is now split: this interface offers
 * only the pure, local part (patch in the operator's corrected fields); the
 * re-enrichment + re-derive-MOP sequencing that used to happen inside requalify() now
 * happens in RepairWorkflowImpl, calling EnrichmentActivities and MopActivities as two
 * more ordinary activity stubs, same as everywhere else.
 */
@ActivityInterface
public interface RepairActivities {

    @ActivityMethod
    void notifyRepairQueue(PaymentContext context);

    /** Applies the operator's corrections to the payment fields. No cross-activity calls. */
    @ActivityMethod
    PaymentContext applyCorrections(PaymentContext context, RepairSubmission submission);
}
