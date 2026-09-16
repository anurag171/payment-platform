package com.bank.payment.orchestration.simulator;

import java.time.Duration;

/** approved/reasonCode/delay decided for one simulated response. */
public record SimulatedOutcome(boolean approved, String reasonCode, Duration delay) {}
