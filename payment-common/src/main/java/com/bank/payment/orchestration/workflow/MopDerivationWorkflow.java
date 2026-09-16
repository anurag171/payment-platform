package com.bank.payment.orchestration.workflow;

import com.bank.payment.orchestration.domain.PaymentContext;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * Child workflow id: {uetr}-mop.
 * Derives method of payment and selects a clearing rail. Branches to RepairWorkflow if
 * derivation fails or is ambiguous, otherwise to PaymentProcessingWorkflow.
 */
@WorkflowInterface
public interface MopDerivationWorkflow {

    @WorkflowMethod
    PaymentContext deriveAndRoute(PaymentContext context);
}
