package com.bank.payment.orchestration.simulator;

import com.bank.payment.orchestration.domain.PartnerRequest;
import com.bank.payment.orchestration.domain.PartnerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Stands in for the four real partner systems (sanctions/fraud/fx/booking): listens on
 * every partner.*.request topic and auto-publishes a response on the matching
 * partner.*.response topic, timed and outcome-decided by the current SimulationMode.
 *
 * DISABLE this in any environment where real partner integrations exist
 * (payment-orchestration.simulator.enabled: false) — it will otherwise race a real
 * partner system to answer every request.
 */
@Component
@ConditionalOnProperty(prefix = "payment-orchestration.simulator", name = "enabled", havingValue = "true", matchIfMissing = true)
public class FakePartnerResponseSimulator {

    private static final Logger log = LoggerFactory.getLogger(FakePartnerResponseSimulator.class);

    private final SimulationModeRegistry modeRegistry;
    private final OutcomeGenerator outcomeGenerator;
    private final KafkaTemplate<String, PartnerResponse> kafkaTemplate;
    private final Executor delayExecutor;

    public FakePartnerResponseSimulator(SimulationModeRegistry modeRegistry,
                                         OutcomeGenerator outcomeGenerator,
                                         KafkaTemplate<String, PartnerResponse> kafkaTemplate,
                                         @Qualifier("simulatorDelayExecutor") Executor delayExecutor) {
        this.modeRegistry = modeRegistry;
        this.outcomeGenerator = outcomeGenerator;
        this.kafkaTemplate = kafkaTemplate;
        this.delayExecutor = delayExecutor;
    }

    @KafkaListener(topicPattern = "partner\\.(sanctions|fraud|fx|booking)\\.request")
    public void onPartnerRequest(PartnerRequest request) {
        SimulatedOutcome outcome = outcomeGenerator.forPartner(
                modeRegistry.effectiveModeFor(request.getPartnerType()), request.getPartnerType());

        log.info("Simulator: {} will {} correlationId={} after {}",
                request.getPartnerType(), outcome.approved() ? "APPROVE" : "REJECT",
                request.getCorrelationId(), outcome.delay());

        CompletableFuture.runAsync(() -> publish(request, outcome),
                CompletableFuture.delayedExecutor(outcome.delay().toMillis(), TimeUnit.MILLISECONDS, delayExecutor));
    }

    private void publish(PartnerRequest request, SimulatedOutcome outcome) {
        PartnerResponse response = new PartnerResponse(
                request.getCorrelationId(), request.getUetr(), request.getPartnerType(),
                outcome.approved(), outcome.reasonCode(), Instant.now());
        String topic = "partner." + request.getPartnerType().name().toLowerCase() + ".response";
        kafkaTemplate.send(topic, request.getCorrelationId(), response);
    }
}
