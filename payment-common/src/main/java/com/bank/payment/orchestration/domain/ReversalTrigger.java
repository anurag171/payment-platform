package com.bank.payment.orchestration.domain;

import java.util.Set;

/**
 * Classifies *why* PaymentProcessingWorkflow is compensating a payment. Workflow-internal
 * only — never crosses an Activity/Temporal serialization boundary (its describe() output,
 * a plain String, is what gets passed to ReversalActivities.notifyReversal), so it doesn't
 * need Jackson polymorphic-type annotations.
 *
 * A sealed interface + record patterns (Java 21) here buys real exhaustiveness checking:
 * if someone adds a fifth reversal cause later, the switch in describe() won't compile
 * until it's handled, instead of silently falling through a default branch.
 */
public sealed interface ReversalTrigger {

    /** An activity exhausted retries or threw a non-retryable error partway through processing. */
    record TechnicalFailure(String stage, String errorMessage) implements ReversalTrigger {}

    /** A partner (sanctions/fraud/fx/booking) explicitly rejected the payment. */
    record PartnerRejected(PartnerType partnerType, String reasonCode) implements ReversalTrigger {}

    /** One or more required partners never responded within SLA. */
    record PartnerTimeout(Set<PartnerType> missingPartners) implements ReversalTrigger {}

    /** The clearing rail rejected the submission, or returned it after initial acceptance. */
    record ClearingRejected(ClearingRail rail, String reasonCode) implements ReversalTrigger {}

    default String describe() {
        return switch (this) {
            case TechnicalFailure(String stage, String error) ->
                    "Technical failure at '%s': %s".formatted(stage, error == null ? "unknown error" : error);
            case PartnerRejected(PartnerType partner, String reason) ->
                    "%s rejected the payment (%s)".formatted(partner, reason == null ? "no reason given" : reason);
            case PartnerTimeout(Set<PartnerType> missing) ->
                    "Timed out waiting for partner response(s): %s".formatted(missing);
            case ClearingRejected(ClearingRail rail, String reason) ->
                    "%s rejected/returned the payment (%s)".formatted(rail, reason == null ? "no reason given" : reason);
        };
    }
}
