package com.bank.payment.orchestration.activities;

import com.bank.payment.orchestration.domain.PaymentContext;
import com.bank.payment.orchestration.domain.RepairSubmission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Pure, local activity: patches operator-supplied corrections into the payment fields.
 * Deliberately does NOT call EnrichmentActivities/MopActivities itself — see the note on
 * RepairActivities for why (that orchestration now lives in RepairWorkflowImpl).
 */
@Component
public class RepairActivitiesImpl implements RepairActivities {

    private static final Logger log = LoggerFactory.getLogger(RepairActivitiesImpl.class);

    @Override
    public void notifyRepairQueue(PaymentContext context) {
        // TODO: push to the real ops worklist (e.g. an internal repair-queue REST API or
        // a dedicated Mongo "repair_worklist" collection an operator UI polls).
        log.info("Payment {} routed to manual repair (attempt {})", context.getUetr(), context.getRepairAttempts());
    }

    @Override
    public PaymentContext applyCorrections(PaymentContext context, RepairSubmission submission) {
        if (submission.getCorrectedFields() != null) {
            submission.getCorrectedFields().forEach((field, value) -> applyCorrection(context, field, value));
        }
        return context;
    }

    private void applyCorrection(PaymentContext context, String field, String value) {
        switch (field) {
            case "currency" -> context.setCurrency(value);
            case "creditorAccount" -> context.setCreditorAccount(value);
            case "debtorAccount" -> context.setDebtorAccount(value);
            default -> log.warn("Unrecognized repair field '{}' for payment {} — ignored", field, context.getUetr());
        }
    }
}
