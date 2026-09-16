package com.bank.payment.orchestration.simulator;

import com.bank.payment.orchestration.domain.PartnerType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * In-memory mode registry: a global default plus optional per-PartnerType overrides
 * (e.g. "everything POSITIVE except FRAUD, which is NEGATIVE today"). Clearing always
 * uses the global default — there's only one clearing leg per payment, so a per-rail
 * override wasn't worth the extra surface area.
 *
 * Deliberately in-memory, not Redis — this is test/sandbox tooling, not something that
 * needs to survive a restart or be consistent across replicas.
 */
@Component
public class SimulationModeRegistry {

    private final AtomicReference<SimulationMode> globalMode;
    private final Map<PartnerType, SimulationMode> overrides = new EnumMap<>(PartnerType.class);

    public SimulationModeRegistry(
            @Value("${payment-orchestration.simulator.default-mode:MIXED}") SimulationMode defaultMode) {
        this.globalMode = new AtomicReference<>(defaultMode);
    }

    public SimulationMode getGlobalMode() {
        return globalMode.get();
    }

    public void setGlobalMode(SimulationMode mode) {
        globalMode.set(mode);
    }

    public void setOverride(PartnerType partnerType, SimulationMode mode) {
        overrides.put(partnerType, mode);
    }

    public void clearOverride(PartnerType partnerType) {
        overrides.remove(partnerType);
    }

    public Map<PartnerType, SimulationMode> getOverrides() {
        return Map.copyOf(overrides);
    }

    public SimulationMode effectiveModeFor(PartnerType partnerType) {
        return overrides.getOrDefault(partnerType, globalMode.get());
    }
}
