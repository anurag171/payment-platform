package com.bank.payment.orchestration.workflow;

import com.bank.payment.orchestration.domain.PaymentContext;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * Child workflow id: {uetr}-warehouse.
 * Sleeps (durable Temporal timer, not a polling loop) until the value date, then hands
 * off to MopDerivationWorkflow.
 *
 * Design note: the original spec says the warehouse workflow "triggers Payment
 * Processing Workflow" directly on the due date. This implementation instead routes
 * warehoused payments through MopDerivationWorkflow on wake (same as immediate
 * payments), so a warehoused payment still gets MOP derivation and repair coverage
 * rather than skipping straight to partner checks with a stale/undetermined MOP. Adjust
 * WarehouseWorkflowImpl if you specifically want the literal wording instead.
 */
@WorkflowInterface
public interface WarehouseWorkflow {

    @WorkflowMethod
    PaymentContext holdUntilValueDate(PaymentContext context);
}
