package com.bank.payment.orchestration.simulator;

import com.bank.payment.orchestration.domain.PartnerType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Decides the (approved, reasonCode, delay) triple for a simulated partner/clearing
 * response, given a mode. Kept separate from the Kafka listeners so the decision logic
 * is unit-testable without spinning up Kafka.
 */
@Component
public class OutcomeGenerator {

    private static final Map<PartnerType, List<String>> PARTNER_REJECT_REASONS = Map.of(
            PartnerType.SANCTIONS, List.of("OFAC_MATCH", "SANCTIONS_LIST_HIT"),
            PartnerType.FRAUD, List.of("SUSPECTED_FRAUD", "VELOCITY_CHECK_FAILED"),
            PartnerType.FX, List.of("RATE_EXPIRED", "FX_LIMIT_EXCEEDED"),
            PartnerType.BOOKING, List.of("INSUFFICIENT_FUNDS", "ACCOUNT_BLOCKED"));

    private static final List<String> CLEARING_REJECT_REASONS =
            List.of("INVALID_BENEFICIARY_ACCOUNT", "ACCOUNT_CLOSED", "AML_HOLD", "RAIL_CUTOFF_MISSED");

    private final double mixedRejectProbability;
    private final double mixedDelayProbability;
    private final Duration shortDelayMin;
    private final Duration shortDelayMax;
    private final Duration longDelayMin;
    private final Duration longDelayMax;

    public OutcomeGenerator(
            @Value("${payment-orchestration.simulator.mixed.reject-probability:0.3}") double mixedRejectProbability,
            @Value("${payment-orchestration.simulator.mixed.delay-probability:0.4}") double mixedDelayProbability,
            @Value("${payment-orchestration.simulator.short-delay-min-seconds:0}") long shortDelayMinSeconds,
            @Value("${payment-orchestration.simulator.short-delay-max-seconds:2}") long shortDelayMaxSeconds,
            @Value("${payment-orchestration.simulator.mixed.long-delay-min-seconds:5}") long longDelayMinSeconds,
            @Value("${payment-orchestration.simulator.mixed.long-delay-max-seconds:45}") long longDelayMaxSeconds) {
        this.mixedRejectProbability = mixedRejectProbability;
        this.mixedDelayProbability = mixedDelayProbability;
        this.shortDelayMin = Duration.ofSeconds(shortDelayMinSeconds);
        this.shortDelayMax = Duration.ofSeconds(shortDelayMaxSeconds);
        this.longDelayMin = Duration.ofSeconds(longDelayMinSeconds);
        this.longDelayMax = Duration.ofSeconds(longDelayMaxSeconds);
    }

    public SimulatedOutcome forPartner(SimulationMode mode, PartnerType partnerType) {
        return switch (mode) {
            case POSITIVE -> new SimulatedOutcome(true, null, randomBetween(shortDelayMin, shortDelayMax));
            case NEGATIVE -> new SimulatedOutcome(false, randomReasonFor(partnerType), randomBetween(shortDelayMin, shortDelayMax));
            case MIXED -> {
                boolean approved = ThreadLocalRandom.current().nextDouble() >= mixedRejectProbability;
                Duration delay = pickMixedDelay();
                yield new SimulatedOutcome(approved, approved ? null : randomReasonFor(partnerType), delay);
            }
        };
    }

    public SimulatedOutcome forClearing(SimulationMode mode) {
        return switch (mode) {
            case POSITIVE -> new SimulatedOutcome(true, null, randomBetween(shortDelayMin, shortDelayMax));
            case NEGATIVE -> new SimulatedOutcome(false, randomFrom(CLEARING_REJECT_REASONS), randomBetween(shortDelayMin, shortDelayMax));
            case MIXED -> {
                boolean settled = ThreadLocalRandom.current().nextDouble() >= mixedRejectProbability;
                Duration delay = pickMixedDelay();
                yield new SimulatedOutcome(settled, settled ? null : randomFrom(CLEARING_REJECT_REASONS), delay);
            }
        };
    }

    private Duration pickMixedDelay() {
        boolean longDelay = ThreadLocalRandom.current().nextDouble() < mixedDelayProbability;
        return longDelay ? randomBetween(longDelayMin, longDelayMax) : randomBetween(shortDelayMin, shortDelayMax);
    }

    private String randomReasonFor(PartnerType partnerType) {
        return randomFrom(PARTNER_REJECT_REASONS.getOrDefault(partnerType, List.of("REJECTED")));
    }

    private String randomFrom(List<String> options) {
        return options.get(ThreadLocalRandom.current().nextInt(options.size()));
    }

    private Duration randomBetween(Duration min, Duration max) {
        long minMillis = min.toMillis();
        long maxMillis = Math.max(minMillis, max.toMillis());
        long chosen = minMillis == maxMillis
                ? minMillis
                : ThreadLocalRandom.current().nextLong(minMillis, maxMillis + 1);
        return Duration.ofMillis(chosen);
    }
}
