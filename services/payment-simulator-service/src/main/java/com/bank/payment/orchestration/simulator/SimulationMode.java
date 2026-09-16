package com.bank.payment.orchestration.simulator;

/**
 * Controls how the fake partner/clearing responders (FakePartnerResponseSimulator,
 * FakeClearingSimulator) behave.
 *
 * POSITIVE  - always approve/settle, short realistic latency.
 * NEGATIVE  - always reject/return, short realistic latency.
 * MIXED     - randomized approve/reject PLUS a randomized chance of a long delay (which
 *             may land inside or outside the workflow's response SLA) — this is what
 *             exercises both the ordinary reject-triggers-reversal path and the
 *             timeout-triggers-reversal path without writing a special case for either.
 */
public enum SimulationMode { POSITIVE, NEGATIVE, MIXED }
