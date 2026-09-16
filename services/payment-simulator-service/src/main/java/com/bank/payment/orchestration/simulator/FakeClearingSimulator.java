package com.bank.payment.orchestration.simulator;

import com.bank.payment.orchestration.domain.ClearingAck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stands in for the four clearing rails (TARGET2/EBA/CHAPS/SWIFT): listens on every
 * clearing.*.submit topic and auto-publishes a settlement ack on the matching
 * clearing.*.ack topic. Same SimulationMode-driven behavior as FakePartnerResponseSimulator.
 *
 * DISABLE (payment-orchestration.simulator.enabled: false) once real clearing
 * connectivity exists.
 */
@Component
@ConditionalOnProperty(prefix = "payment-orchestration.simulator", name = "enabled", havingValue = "true", matchIfMissing = true)
public class FakeClearingSimulator {

    private static final Logger log = LoggerFactory.getLogger(FakeClearingSimulator.class);
    private static final Pattern UETR_PATTERN = Pattern.compile("<UETR>([^<]+)</UETR>");
    private static final Pattern RAIL_TOPIC_PATTERN = Pattern.compile("clearing\\.([a-z0-9]+)\\.submit");

    private final SimulationModeRegistry modeRegistry;
    private final OutcomeGenerator outcomeGenerator;
    private final KafkaTemplate<String, ClearingAck> kafkaTemplate;
    private final Executor delayExecutor;

    public FakeClearingSimulator(SimulationModeRegistry modeRegistry,
                                  OutcomeGenerator outcomeGenerator,
                                  KafkaTemplate<String, ClearingAck> kafkaTemplate,
                                  @Qualifier("simulatorDelayExecutor") Executor delayExecutor) {
        this.modeRegistry = modeRegistry;
        this.outcomeGenerator = outcomeGenerator;
        this.kafkaTemplate = kafkaTemplate;
        this.delayExecutor = delayExecutor;
    }

    @KafkaListener(topicPattern = "clearing\\.(target2|eba|chaps|swift)\\.submit")
    public void onClearingSubmission(@Payload String clearingMessage,
                                      @Header(KafkaHeaders.RECEIVED_KEY) String correlationId,
                                      @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        String rail = extractRail(topic);
        String uetr = extractUetr(clearingMessage, correlationId);
        SimulatedOutcome outcome = outcomeGenerator.forClearing(modeRegistry.getGlobalMode());

        log.info("Simulator: {} will {} correlationId={} after {}",
                rail, outcome.approved() ? "SETTLE" : "REJECT/RETURN", correlationId, outcome.delay());

        CompletableFuture.runAsync(
                () -> publish(correlationId, uetr, rail, outcome),
                CompletableFuture.delayedExecutor(outcome.delay().toMillis(), TimeUnit.MILLISECONDS, delayExecutor));
    }

    private void publish(String correlationId, String uetr, String rail, SimulatedOutcome outcome) {
        ClearingAck ack = new ClearingAck(correlationId, uetr, outcome.approved(), outcome.reasonCode());
        kafkaTemplate.send("clearing." + rail + ".ack", correlationId, ack);
    }

    private String extractRail(String topic) {
        Matcher matcher = RAIL_TOPIC_PATTERN.matcher(topic);
        return matcher.find() ? matcher.group(1) : "unknown";
    }

    private String extractUetr(String clearingMessage, String correlationId) {
        Matcher matcher = UETR_PATTERN.matcher(clearingMessage == null ? "" : clearingMessage);
        if (matcher.find()) {
            return matcher.group(1);
        }
        // Fallback: correlationId is "{uetr}-processing::clearing" — strip the known suffix.
        int idx = correlationId.indexOf("-processing::clearing");
        return idx >= 0 ? correlationId.substring(0, idx) : correlationId;
    }
}
