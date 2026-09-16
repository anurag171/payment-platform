package com.bank.payment.orchestration.workflow.impl;

import com.bank.payment.orchestration.activities.AuditActivities;
import com.bank.payment.orchestration.activities.MopActivities;
import com.bank.payment.orchestration.domain.PaymentContext;
import com.bank.payment.orchestration.domain.PaymentStatus;
import com.bank.payment.orchestration.workflow.MopDerivationWorkflow;
import com.bank.payment.orchestration.workflow.PaymentProcessingWorkflow;
import com.bank.payment.orchestration.workflow.RepairWorkflow;
import com.bank.payment.orchestration.workflow.TaskQueues;
import com.bank.payment.orchestration.workflow.WorkflowSupport;
import io.temporal.workflow.ChildWorkflowOptions;
import io.temporal.workflow.Workflow;

/** Deployed as mop-derivation-workflow-service, task queue MOP_DERIVATION_TQ. */
public class MopDerivationWorkflowImpl implements MopDerivationWorkflow {

    private final MopActivities mopActivities =
            Workflow.newActivityStub(MopActivities.class, WorkflowSupport.activityOptions(TaskQueues.MOP_ACTIVITY));
    private final AuditActivities auditActivities =
            Workflow.newActivityStub(AuditActivities.class, WorkflowSupport.activityOptions(TaskQueues.AUDIT_ACTIVITY));

    @Override
    public PaymentContext deriveAndRoute(PaymentContext context) {
        boolean mopResolved = mopActivities.deriveMop(context);
        String uetr = context.getUetr();

        if (!mopResolved) {
            context.setStatus(PaymentStatus.IN_REPAIR);
            context.setFailureReason("MOP derivation could not resolve a method of payment");
            auditActivities.recordStage(uetr, "MopDerivationWorkflow", "mop-failed", context);

            RepairWorkflow repairWorkflow = Workflow.newChildWorkflowStub(
                    RepairWorkflow.class,
                    ChildWorkflowOptions.newBuilder()
                            .setWorkflowId(uetr + "-repair-" + (context.getRepairAttempts() + 1))
                            .setTaskQueue(TaskQueues.REPAIR_WORKFLOW)
                            .build());
            return repairWorkflow.repair(context);
        }

        mopActivities.selectClearingRail(context);
        context.setStatus(PaymentStatus.MOP_DERIVED);
        auditActivities.recordStage(uetr, "MopDerivationWorkflow", "mop-derived", context);

        PaymentProcessingWorkflow processingWorkflow = Workflow.newChildWorkflowStub(
                PaymentProcessingWorkflow.class,
                ChildWorkflowOptions.newBuilder()
                        .setWorkflowId(uetr + "-processing")
                        .setTaskQueue(TaskQueues.PAYMENT_PROCESSING)
                        .build());
        return processingWorkflow.process(context);
    }
}
