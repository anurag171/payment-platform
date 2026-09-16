package com.bank.payment.orchestration.workflow.impl;

import com.bank.payment.orchestration.activities.AuditActivities;
import com.bank.payment.orchestration.activities.TranslationActivities;
import com.bank.payment.orchestration.domain.PaymentContext;
import com.bank.payment.orchestration.domain.PaymentEnvelope;
import com.bank.payment.orchestration.domain.PaymentStatus;
import com.bank.payment.orchestration.workflow.InboundNormalizationWorkflow;
import com.bank.payment.orchestration.workflow.QualificationWorkflow;
import com.bank.payment.orchestration.workflow.TaskQueues;
import com.bank.payment.orchestration.workflow.WorkflowSupport;
import io.temporal.workflow.ChildWorkflowOptions;
import io.temporal.workflow.Workflow;

/** Deployed as inbound-normalization-workflow-service, task queue INBOUND_NORMALIZATION_TQ. */
public class InboundNormalizationWorkflowImpl implements InboundNormalizationWorkflow {

    private final TranslationActivities translationActivities = Workflow.newActivityStub(
            TranslationActivities.class, WorkflowSupport.activityOptions(TaskQueues.TRANSLATION_ACTIVITY));
    private final AuditActivities auditActivities = Workflow.newActivityStub(
            AuditActivities.class, WorkflowSupport.activityOptions(TaskQueues.AUDIT_ACTIVITY));

    @Override
    public PaymentContext normalize(PaymentEnvelope envelope) {
        PaymentContext context = new PaymentContext();
        context.setUetr(envelope.getUetr());
        context.setSenderReference(envelope.getSenderReference());
        context.setSourceFormat(envelope.getSourceFormat());
        context.setRawMessage(envelope.getRawMessage());
        context.setStatus(PaymentStatus.RECEIVED);

        String canonical = translationActivities.translateToPsd2(envelope);
        translationActivities.validateCanonical(canonical);
        context.setCanonicalPsd2Message(canonical);
        context.setStatus(PaymentStatus.TRANSLATED);

        auditActivities.recordStage(context.getUetr(), "InboundNormalizationWorkflow", "translated", context);

        QualificationWorkflow qualificationWorkflow = Workflow.newChildWorkflowStub(
                QualificationWorkflow.class,
                ChildWorkflowOptions.newBuilder()
                        .setWorkflowId(context.getUetr() + "-qualification")
                        .setTaskQueue(TaskQueues.QUALIFICATION)
                        .build());

        return qualificationWorkflow.qualify(context);
    }
}
