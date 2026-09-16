package com.bank.payment.orchestration.workflow.impl;

import com.bank.payment.orchestration.activities.AuditActivities;
import com.bank.payment.orchestration.activities.ClearingActivities;
import com.bank.payment.orchestration.activities.PartnerActivities;
import com.bank.payment.orchestration.activities.ReversalActivities;
import com.bank.payment.orchestration.domain.ClearingAck;
import com.bank.payment.orchestration.domain.PartnerResponse;
import com.bank.payment.orchestration.domain.PartnerType;
import com.bank.payment.orchestration.domain.PaymentContext;
import com.bank.payment.orchestration.domain.PaymentStatus;
import com.bank.payment.orchestration.domain.ReversalTrigger;
import com.bank.payment.orchestration.workflow.PaymentProcessingWorkflow;
import com.bank.payment.orchestration.workflow.TaskQueues;
import com.bank.payment.orchestration.workflow.WorkflowSupport;
import io.temporal.workflow.Async;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Saga;
import io.temporal.workflow.Workflow;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Deployed as payment-processing-workflow-service, task queue PAYMENT_PROCESSING_TQ.
 * See payment-platform README §Saga for the reversal design — unchanged by the
 * microservices split except that every activity stub now names its target service's
 * task queue explicitly.
 */
public class PaymentProcessingWorkflowImpl implements PaymentProcessingWorkflow {

    private static final Duration PARTNER_RESPONSE_SLA = Duration.ofSeconds(30);
    private static final Duration CLEARING_ACK_SLA = Duration.ofMinutes(5);

    private final PartnerActivities partnerActivities =
            Workflow.newActivityStub(PartnerActivities.class, WorkflowSupport.activityOptions(TaskQueues.PARTNER_ACTIVITY));
    private final ClearingActivities clearingActivities =
            Workflow.newActivityStub(ClearingActivities.class, WorkflowSupport.activityOptions(TaskQueues.CLEARING_ACTIVITY));
    private final AuditActivities auditActivities =
            Workflow.newActivityStub(AuditActivities.class, WorkflowSupport.activityOptions(TaskQueues.AUDIT_ACTIVITY));
    private final ReversalActivities reversalActivities =
            Workflow.newActivityStub(ReversalActivities.class, WorkflowSupport.activityOptions(TaskQueues.REVERSAL_ACTIVITY));

    private final Map<PartnerType, PartnerResponse> partnerResponses = new HashMap<>();
    private ClearingAck clearingAck;

    private Saga saga;
    private PaymentContext context;
    private boolean fxCompensationRegistered = false;
    private boolean bookingCompensationRegistered = false;

    @Override
    public PaymentContext process(PaymentContext incoming) {
        this.context = incoming;
        this.saga = new Saga(new Saga.Options.Builder()
                .setParallelCompensation(false)
                .setContinueWithError(true)
                .build());

        context.setStatus(PaymentStatus.PROCESSING);
        String workflowId = Workflow.getInfo().getWorkflowId();
        String uetr = context.getUetr();

        try {
            List<Promise<?>> sendPromises = new ArrayList<>();
            for (PartnerType partnerType : context.getRequiredPartners()) {
                sendPromises.add(Async.function(
                        () -> partnerActivities.sendPartnerRequest(context, partnerType, workflowId)));
            }
            Promise.allOf(sendPromises).get();
            auditActivities.recordStage(uetr, "PaymentProcessingWorkflow", "partner-requests-sent", context);

            boolean allResponded = Workflow.await(PARTNER_RESPONSE_SLA,
                    () -> partnerResponses.keySet().containsAll(context.getRequiredPartners()));

            if (!allResponded) {
                Set<PartnerType> missing = new HashSet<>(context.getRequiredPartners());
                missing.removeAll(partnerResponses.keySet());
                return compensateAndFail(new ReversalTrigger.PartnerTimeout(missing));
            }

            boolean approved = partnerActivities.aggregatePartnerResults(context, partnerResponses);
            if (!approved) {
                PartnerResponse rejected = partnerResponses.values().stream()
                        .filter(r -> !r.isApproved())
                        .findFirst()
                        .orElseThrow();
                return compensateAndFail(
                        new ReversalTrigger.PartnerRejected(rejected.getPartnerType(), rejected.getReasonCode()));
            }

            context.setStatus(PaymentStatus.PARTNER_APPROVED);
            auditActivities.recordStage(uetr, "PaymentProcessingWorkflow", "partner-approved", context);

            String clearingMessage = clearingActivities.buildClearingMessage(context);
            clearingActivities.submitToClearing(context, clearingMessage, workflowId);
            context.setStatus(PaymentStatus.CLEARING_SUBMITTED);
            auditActivities.recordStage(uetr, "PaymentProcessingWorkflow", "clearing-submitted", context);

            boolean acked = Workflow.await(CLEARING_ACK_SLA, () -> clearingAck != null);
            if (!acked) {
                return compensateAndFail(
                        new ReversalTrigger.ClearingRejected(context.getClearingRail(), "settlement ack SLA breached"));
            }
            if (!clearingAck.isSettled()) {
                return compensateAndFail(
                        new ReversalTrigger.ClearingRejected(context.getClearingRail(), clearingAck.getRejectReason()));
            }

            context.setStatus(PaymentStatus.COMPLETED);
            auditActivities.recordStage(uetr, "PaymentProcessingWorkflow", "finalized", context);
            return context;

        } catch (Exception e) {
            return compensateAndFail(new ReversalTrigger.TechnicalFailure("PaymentProcessingWorkflow", e.getMessage()));
        }
    }

    @Override
    public void onPartnerResponse(PartnerResponse response) {
        partnerResponses.put(response.getPartnerType(), response);
        if (!response.isApproved()) {
            return;
        }
        if (response.getPartnerType() == PartnerType.FX && !fxCompensationRegistered && saga != null) {
            saga.addCompensation(reversalActivities::reverseFxDeal, context);
            fxCompensationRegistered = true;
        } else if (response.getPartnerType() == PartnerType.BOOKING && !bookingCompensationRegistered && saga != null) {
            saga.addCompensation(reversalActivities::reverseBooking, context);
            bookingCompensationRegistered = true;
        }
    }

    @Override
    public void onClearingAck(ClearingAck ack) {
        this.clearingAck = ack;
    }

    private PaymentContext compensateAndFail(ReversalTrigger trigger) {
        String uetr = context.getUetr();
        String reason = trigger.describe();
        context.setFailureReason(reason);
        context.setStatus(PaymentStatus.REVERSAL_IN_PROGRESS);
        auditActivities.recordStage(uetr, "PaymentProcessingWorkflow", "reversal-initiated", context);

        try {
            saga.compensate();
            context.setStatus(PaymentStatus.REVERSED);
            auditActivities.recordStage(uetr, "PaymentProcessingWorkflow", "reversed", context);
        } catch (Exception compensationError) {
            context.setStatus(PaymentStatus.REVERSAL_FAILED);
            context.setFailureReason(reason + " | compensation also failed: " + compensationError.getMessage());
            auditActivities.recordStage(uetr, "PaymentProcessingWorkflow", "reversal-failed", context);
        }

        reversalActivities.notifyReversal(context, reason);
        return context;
    }
}
