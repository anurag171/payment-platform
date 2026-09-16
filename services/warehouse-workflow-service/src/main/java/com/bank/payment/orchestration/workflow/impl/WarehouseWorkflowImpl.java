package com.bank.payment.orchestration.workflow.impl;

import com.bank.payment.orchestration.activities.AuditActivities;
import com.bank.payment.orchestration.domain.PaymentContext;
import com.bank.payment.orchestration.workflow.MopDerivationWorkflow;
import com.bank.payment.orchestration.workflow.TaskQueues;
import com.bank.payment.orchestration.workflow.WarehouseWorkflow;
import com.bank.payment.orchestration.workflow.WorkflowSupport;
import io.temporal.workflow.ChildWorkflowOptions;
import io.temporal.workflow.Workflow;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

/** Deployed as warehouse-workflow-service, task queue WAREHOUSE_TQ. */
public class WarehouseWorkflowImpl implements WarehouseWorkflow {

    private final AuditActivities auditActivities =
            Workflow.newActivityStub(AuditActivities.class, WorkflowSupport.activityOptions(TaskQueues.AUDIT_ACTIVITY));

    @Override
    public PaymentContext holdUntilValueDate(PaymentContext context) {
        Instant target = context.getValueDate().atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant now = Instant.ofEpochMilli(Workflow.currentTimeMillis());
        Duration sleepDuration = Duration.between(now, target);

        if (!sleepDuration.isNegative()) {
            Workflow.sleep(sleepDuration);
        }

        auditActivities.recordStage(context.getUetr(), "WarehouseWorkflow", "value-date-reached", context);

        MopDerivationWorkflow mopWorkflow = Workflow.newChildWorkflowStub(
                MopDerivationWorkflow.class,
                ChildWorkflowOptions.newBuilder()
                        .setWorkflowId(context.getUetr() + "-mop")
                        .setTaskQueue(TaskQueues.MOP_DERIVATION)
                        .build());
        return mopWorkflow.deriveAndRoute(context);
    }
}
