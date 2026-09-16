package com.bank.payment.orchestration.workflow;

import com.bank.payment.orchestration.domain.PaymentContext;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * Child workflow id: {uetr}-qualification.
 * Enriches from customer/account services, derives the value date, and decides
 * warehouse vs immediate. Routes to WarehouseWorkflow or straight to MopDerivationWorkflow.
 */
@WorkflowInterface
public interface QualificationWorkflow {

    @WorkflowMethod
    PaymentContext qualify(PaymentContext context);
}
