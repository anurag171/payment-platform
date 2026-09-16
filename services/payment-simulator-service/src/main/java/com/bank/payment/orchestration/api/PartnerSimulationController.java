package com.bank.payment.orchestration.api;

import com.bank.payment.orchestration.domain.ClearingAck;
import com.bank.payment.orchestration.domain.PartnerResponse;
import com.bank.payment.orchestration.domain.PartnerType;
import com.bank.payment.orchestration.simulator.SimulationMode;
import com.bank.payment.orchestration.simulator.SimulationModeRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Two things live here now:
 *  1. Manual simulation endpoints (unchanged) — publish one exact response/ack yourself
 *     for reproducing a specific scenario.
 *  2. Mode controls for the automatic FakePartnerResponseSimulator/FakeClearingSimulator,
 *     which listen on the request/submit topics and answer on their own. Manual calls
 *     and the automatic responders don't conflict: PartnerCorrelationService's pending-id
 *     check means whichever answers first "wins" and the second is logged and ignored.
 *
 * Replaces the Rapidoid/Aeromock-based simulator called for in the original spec — see
 * README.
 */
@Tag(name = "Partner & clearing simulation")
@RestController
@RequestMapping("/api/simulate")
public class PartnerSimulationController {

    private final KafkaTemplate<String, PartnerResponse> partnerKafkaTemplate;
    private final KafkaTemplate<String, ClearingAck> clearingKafkaTemplate;
    private final SimulationModeRegistry modeRegistry;

    public PartnerSimulationController(KafkaTemplate<String, PartnerResponse> partnerKafkaTemplate,
                                        KafkaTemplate<String, ClearingAck> clearingKafkaTemplate,
                                        SimulationModeRegistry modeRegistry) {
        this.partnerKafkaTemplate = partnerKafkaTemplate;
        this.clearingKafkaTemplate = clearingKafkaTemplate;
        this.modeRegistry = modeRegistry;
    }

    public record SimulatePartnerResponseRequest(
            String correlationId, String uetr, PartnerType partnerType, boolean approved, String reasonCode) {}

    @Operation(summary = "Simulate an async partner response (sanctions/fraud/fx/booking)")
    @PostMapping("/partner-response")
    public ResponseEntity<Void> simulatePartnerResponse(@RequestBody SimulatePartnerResponseRequest request) {
        PartnerResponse response = new PartnerResponse(
                request.correlationId(), request.uetr(), request.partnerType(),
                request.approved(), request.reasonCode(), Instant.now());
        String topic = "partner." + request.partnerType().name().toLowerCase() + ".response";
        partnerKafkaTemplate.send(topic, request.correlationId(), response);
        return ResponseEntity.accepted().build();
    }

    public record SimulateClearingAckRequest(
            String correlationId, String uetr, String rail, boolean settled, String rejectReason) {}

    @Operation(summary = "Simulate a clearing-rail settlement acknowledgment")
    @PostMapping("/clearing-ack")
    public ResponseEntity<Void> simulateClearingAck(@RequestBody SimulateClearingAckRequest request) {
        ClearingAck ack = new ClearingAck(
                request.correlationId(), request.uetr(), request.settled(), request.rejectReason());
        String topic = "clearing." + request.rail().toLowerCase() + ".ack";
        clearingKafkaTemplate.send(topic, request.correlationId(), ack);
        return ResponseEntity.accepted().build();
    }

    public record ModeRequest(SimulationMode mode) {}

    public record ModeStatus(SimulationMode globalMode, Map<PartnerType, SimulationMode> overrides) {}

    @Operation(summary = "Get the current global simulation mode and any per-partner overrides")
    @GetMapping("/mode")
    public ModeStatus getMode() {
        return new ModeStatus(modeRegistry.getGlobalMode(), modeRegistry.getOverrides());
    }

    @Operation(summary = "Set the global simulation mode (POSITIVE, NEGATIVE, or MIXED) for all auto-generated partner/clearing responses")
    @PostMapping("/mode")
    public ModeStatus setGlobalMode(@RequestBody ModeRequest request) {
        modeRegistry.setGlobalMode(request.mode());
        return getMode();
    }

    @Operation(summary = "Override the mode for one partner type only (e.g. force FRAUD to always reject)")
    @PostMapping("/mode/{partnerType}")
    public ModeStatus setPartnerOverride(@PathVariable PartnerType partnerType, @RequestBody ModeRequest request) {
        modeRegistry.setOverride(partnerType, request.mode());
        return getMode();
    }

    @Operation(summary = "Clear a partner-specific mode override, falling back to the global mode")
    @DeleteMapping("/mode/{partnerType}")
    public ModeStatus clearPartnerOverride(@PathVariable PartnerType partnerType) {
        modeRegistry.clearOverride(partnerType);
        return getMode();
    }
}
