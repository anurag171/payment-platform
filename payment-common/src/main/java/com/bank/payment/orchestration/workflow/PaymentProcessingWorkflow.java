package com.bank.payment.orchestration.workflow;

import com.bank.payment.orchestration.domain.ClearingAck;
import com.bank.payment.orchestration.domain.PaymentContext;
import com.bank.payment.orchestration.domain.PartnerResponse;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * Child workflow id: {uetr}-processing.
 * Fans out async partner checks (sanctions/fraud/fx/booking) in parallel, aggregates
 * results, and — if approved — submits to the selected clearing rail and waits for
 * settlement. One signal method per response family; each partner's response is
 * distinguished by the correlation id / payload's partnerType field.
 */
@WorkflowInterface
public interface PaymentProcessingWorkflow {

    @WorkflowMethod
    PaymentContext process(PaymentContext context);

    @SignalMethod
    void onPartnerResponse(PartnerResponse response);

    @SignalMethod
    void onClearingAck(ClearingAck ack);
}
