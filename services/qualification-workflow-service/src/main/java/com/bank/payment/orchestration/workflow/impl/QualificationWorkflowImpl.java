package com.bank.payment.orchestration.workflow.impl;

import com.bank.payment.orchestration.activities.AuditActivities;
import com.bank.payment.orchestration.activities.EnrichmentActivities;
import com.bank.payment.orchestration.activities.ValueDateActivities;
import com.bank.payment.orchestration.domain.PaymentContext;
import com.bank.payment.orchestration.domain.PaymentStatus;
import com.bank.payment.orchestration.workflow.MopDerivationWorkflow;
import com.bank.payment.orchestration.workflow.QualificationWorkflow;
import com.bank.payment.orchestration.workflow.TaskQueues;
import com.bank.payment.orchestration.workflow.WarehouseWorkflow;
import com.bank.payment.orchestration.workflow.WorkflowSupport;
import io.temporal.workflow.ChildWorkflowOptions;
import io.temporal.workflow.Workflow;

/** Deployed as qualification-workflow-service, task queue QUALIFICATION_TQ. */
public class QualificationWorkflowImpl implements QualificationWorkflow {

    private final EnrichmentActivities enrichmentActivities =
            Workflow.newActivityStub(EnrichmentActivities.class, WorkflowSupport.activityOptions(TaskQueues.ENRICHMENT_ACTIVITY));
    private final ValueDateActivities valueDateActivities =
            Workflow.newActivityStub(ValueDateActivities.class, WorkflowSupport.activityOptions(TaskQueues.VALUE_DATE_ACTIVITY));
    private final AuditActivities auditActivities =
            Workflow.newActivityStub(AuditActivities.class, WorkflowSupport.activityOptions(TaskQueues.AUDIT_ACTIVITY));

    @Override
    public PaymentContext qualify(PaymentContext context) {
        context.setStatus(PaymentStatus.QUALIFYING);
        context.setCustomerData(enrichmentActivities.enrichCustomer(context));
        context.setAccountData(enrichmentActivities.enrichAccount(context));
        context.setStatus(PaymentStatus.ENRICHED);

        valueDateActivities.deriveValueDate(context);
        auditActivities.recordStage(context.getUetr(), "QualificationWorkflow", "enriched", context);

        String uetr = context.getUetr();

        if (context.isWarehoused()) {
            context.setStatus(PaymentStatus.WAREHOUSED);
            auditActivities.recordStage(uetr, "QualificationWorkflow", "warehousing", context);

            WarehouseWorkflow warehouseWorkflow = Workflow.newChildWorkflowStub(
                    WarehouseWorkflow.class,
                    ChildWorkflowOptions.newBuilder()
                            .setWorkflowId(uetr + "-warehouse")
                            .setTaskQueue(TaskQueues.WAREHOUSE)
                            .build());
            return warehouseWorkflow.holdUntilValueDate(context);
        }

        MopDerivationWorkflow mopWorkflow = Workflow.newChildWorkflowStub(
                MopDerivationWorkflow.class,
                ChildWorkflowOptions.newBuilder()
                        .setWorkflowId(uetr + "-mop")
                        .setTaskQueue(TaskQueues.MOP_DERIVATION)
                        .build());
        return mopWorkflow.deriveAndRoute(context);
    }
}
