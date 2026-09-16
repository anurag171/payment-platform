package com.bank.payment.orchestration.workflow;

import com.bank.payment.orchestration.domain.PaymentContext;
import com.bank.payment.orchestration.domain.RepairSubmission;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * Child workflow id: {uetr}-repair-{attempt}.
 * Parks the payment on a manual repair worklist and waits (signal, bounded by an SLA
 * timer) for an operator to submit a correction. On receipt, requalifies and re-derives
 * MOP, then chains into PaymentProcessingWorkflow. Exceeding the configured max repair
 * attempts marks the payment FAILED instead of looping indefinitely.
 */
@WorkflowInterface
public interface RepairWorkflow {

    @WorkflowMethod
    PaymentContext repair(PaymentContext context);

    @SignalMethod
    void submitRepair(RepairSubmission submission);
}
