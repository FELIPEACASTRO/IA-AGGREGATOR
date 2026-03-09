package com.ia.aggregator.domain.execution;

/**
 * Agent autonomy levels (L0-L4).
 */
public enum AutonomyLevel {
    /** L0 — Assistive: only suggests, user does everything */
    L0_ASSISTIVE,
    /** L1 — Suggestive: suggests actions, user approves each */
    L1_SUGGESTIVE,
    /** L2 — Confirmative: executes with explicit confirmation */
    L2_CONFIRMATIVE,
    /** L3 — Policy-bound: executes within defined policy constraints */
    L3_POLICY_BOUND,
    /** L4 — Autonomous: operates continuously within budget */
    L4_AUTONOMOUS
}
