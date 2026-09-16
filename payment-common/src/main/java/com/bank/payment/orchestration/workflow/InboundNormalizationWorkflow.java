package com.bank.payment.orchestration.workflow;

import com.bank.payment.orchestration.domain.PaymentContext;
import com.bank.payment.orchestration.domain.PaymentEnvelope;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * Root workflow. Workflow id = UETR (generated at ingestion if the source lacked one).
 * Translates the raw message to canonical PSD2, then chains into QualificationWorkflow
 * as a child and returns its eventual result — so the whole payment lifecycle lives
 * under this one workflow id in Temporal's UI/history, even though later stages run as
 * separate child workflow executions.
 */
@WorkflowInterface
public interface InboundNormalizationWorkflow {

    @WorkflowMethod
    PaymentContext normalize(PaymentEnvelope envelope);
}
