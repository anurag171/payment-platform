package com.bank.payment.orchestration.workflow.impl;

import com.bank.payment.orchestration.activities.AuditActivities;
import com.bank.payment.orchestration.activities.EnrichmentActivities;
import com.bank.payment.orchestration.activities.MopActivities;
import com.bank.payment.orchestration.activities.RepairActivities;
import com.bank.payment.orchestration.domain.MopType;
import com.bank.payment.orchestration.domain.PaymentContext;
import com.bank.payment.orchestration.domain.PaymentStatus;
import com.bank.payment.orchestration.domain.RepairSubmission;
import com.bank.payment.orchestration.workflow.PaymentProcessingWorkflow;
import com.bank.payment.orchestration.workflow.RepairWorkflow;
import com.bank.payment.orchestration.workflow.TaskQueues;
import com.bank.payment.orchestration.workflow.WorkflowSupport;
import io.temporal.workflow.ChildWorkflowOptions;
import io.temporal.workflow.Workflow;

import java.time.Duration;

/**
 * Deployed as repair-workflow-service, task queue REPAIR_WORKFLOW_TQ.
 *
 * NOTE on the microservices split: the monolith's RepairActivities.requalify() used to
 * call EnrichmentActivities/MopActivities directly as in-process objects. That doesn't
 * work once activities live in separate services — an activity can't call another
 * activity's implementation. So this workflow now does that orchestration itself,
 * calling EnrichmentActivities (-> enrichment-activity-service) and MopActivities
 * (-> mop-activity-service) as two more ordinary cross-service activity stubs, exactly
 * like QualificationWorkflowImpl and MopDerivationWorkflowImpl already do. This is
 * arguably the more correct design even outside a microservices context — an activity
 * silently calling another activity's Java implementation blurred a boundary that
 * should have been explicit in the workflow all along.
 */
public class RepairWorkflowImpl implements RepairWorkflow {

    private static final int MAX_REPAIR_ATTEMPTS = 3;
    private static final Duration REPAIR_SLA = Duration.ofHours(4);

    private final RepairActivities repairActivities =
            Workflow.newActivityStub(RepairActivities.class, WorkflowSupport.activityOptions(TaskQueues.REPAIR_ACTIVITY));
    private final EnrichmentActivities enrichmentActivities =
            Workflow.newActivityStub(EnrichmentActivities.class, WorkflowSupport.activityOptions(TaskQueues.ENRICHMENT_ACTIVITY));
    private final MopActivities mopActivities =
            Workflow.newActivityStub(MopActivities.class, WorkflowSupport.activityOptions(TaskQueues.MOP_ACTIVITY));
    private final AuditActivities auditActivities =
            Workflow.newActivityStub(AuditActivities.class, WorkflowSupport.activityOptions(TaskQueues.AUDIT_ACTIVITY));

    private RepairSubmission receivedSubmission;

    @Override
    public PaymentContext repair(PaymentContext context) {
        context.setRepairAttempts(context.getRepairAttempts() + 1);
        String uetr = context.getUetr();

        if (context.getRepairAttempts() > MAX_REPAIR_ATTEMPTS) {
            context.setStatus(PaymentStatus.FAILED);
            context.setFailureReason("Exceeded max repair attempts (" + MAX_REPAIR_ATTEMPTS + ")");
            auditActivities.recordStage(uetr, "RepairWorkflow", "repair-attempts-exhausted", context);
            return context;
        }

        repairActivities.notifyRepairQueue(context);
        auditActivities.recordStage(uetr, "RepairWorkflow", "awaiting-operator", context);

        boolean submitted = Workflow.await(REPAIR_SLA, () -> receivedSubmission != null);

        if (!submitted) {
            context.setStatus(PaymentStatus.FAILED);
            context.setFailureReason("Repair SLA breached — no operator submission within " + REPAIR_SLA);
            auditActivities.recordStage(uetr, "RepairWorkflow", "sla-breached", context);
            return context;
        }

        // The three-step "requalify" sequence, now explicit in the workflow instead of
        // hidden inside one activity: patch corrections, re-enrich, re-derive MOP.
        PaymentContext corrected = repairActivities.applyCorrections(context, receivedSubmission);
        corrected.setCustomerData(enrichmentActivities.enrichCustomer(corrected));
        corrected.setAccountData(enrichmentActivities.enrichAccount(corrected));
        boolean mopResolved = mopActivities.deriveMop(corrected);
        if (mopResolved) {
            mopActivities.selectClearingRail(corrected);
        }

        if (corrected.getMop() == MopType.UNRESOLVED || !mopResolved) {
            auditActivities.recordStage(uetr, "RepairWorkflow", "requalify-still-unresolved", corrected);
            RepairWorkflow nextAttempt = Workflow.newChildWorkflowStub(
                    RepairWorkflow.class,
                    ChildWorkflowOptions.newBuilder()
                            .setWorkflowId(uetr + "-repair-" + (corrected.getRepairAttempts() + 1))
                            .setTaskQueue(TaskQueues.REPAIR_WORKFLOW)
                            .build());
            return nextAttempt.repair(corrected);
        }

        corrected.setStatus(PaymentStatus.MOP_DERIVED);
        auditActivities.recordStage(uetr, "RepairWorkflow", "requalified", corrected);

        PaymentProcessingWorkflow processingWorkflow = Workflow.newChildWorkflowStub(
                PaymentProcessingWorkflow.class,
                ChildWorkflowOptions.newBuilder()
                        .setWorkflowId(uetr + "-processing")
                        .setTaskQueue(TaskQueues.PAYMENT_PROCESSING)
                        .build());
        return processingWorkflow.process(corrected);
    }

    @Override
    public void submitRepair(RepairSubmission submission) {
        this.receivedSubmission = submission;
    }
}
