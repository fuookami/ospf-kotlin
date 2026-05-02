package fuookami.ospf.kotlin.example.framework_demo.demo2

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*

class BendersStrategyTest {
    @Test
    fun `supportedAircraft returns true for B737 and B757`() {
        assertTrue(BendersStrategy.supportedAircraft(AircraftTypeInput.B737))
        assertTrue(BendersStrategy.supportedAircraft(AircraftTypeInput.B757))
    }

    @Test
    fun `supportedAircraft returns false for B767 and B747`() {
        assertFalse(BendersStrategy.supportedAircraft(AircraftTypeInput.B767))
        assertFalse(BendersStrategy.supportedAircraft(AircraftTypeInput.B747))
        assertFalse(BendersStrategy.supportedAircraft(AircraftTypeInput.Unknown))
    }

    @Test
    fun `tuneAdaptiveConfig boosts iterations for large problems`() {
        val config = BendersAdaptiveConfig(minBinaryVariables = 4, maxIterations = 64, tolerance = 1e-6)
        val tuned = BendersStrategy.tuneAdaptiveConfig(config, 200)
        assertTrue(tuned.maxIterations > 64)
        assertTrue(tuned.tolerance > 1e-6)
    }

    @Test
    fun `tuneAdaptiveConfig keeps small config for small problems`() {
        val config = BendersAdaptiveConfig(minBinaryVariables = 4, maxIterations = 64, tolerance = 1e-6)
        val tuned = BendersStrategy.tuneAdaptiveConfig(config, 10)
        assertEquals(64, tuned.maxIterations)
        assertEquals(1e-6, tuned.tolerance, 1e-12)
    }

    @Test
    fun `resolveSolveMode returns Milp when preferBenders is false`() {
        val request = RequestDTO.sample()
        val notes = mutableListOf<String>()
        val mode = BendersStrategy.resolveSolveMode(request, notes)
        assertTrue(mode is SolveMode.Milp)
    }

    @Test
    fun `resolveSolveMode returns Benders when preferBenders is true and enough variables`() {
        val request = RequestDTO.sample().copy(
            solvePolicy = SolvePolicy(preferBenders = true, bendersFallbackToMilp = true),
            bendersAdaptive = BendersAdaptiveConfig(minBinaryVariables = 2, maxIterations = 64, tolerance = 1e-6)
        )
        val notes = mutableListOf<String>()
        val mode = BendersStrategy.resolveSolveMode(request, notes)
        assertTrue(mode is SolveMode.Benders)
        assertTrue(notes.any { it.contains("Benders requested") })
    }

    @Test
    fun `resolveSolveMode falls back to Milp when not enough binary variables`() {
        val request = RequestDTO.sample().copy(
            solvePolicy = SolvePolicy(preferBenders = true, bendersFallbackToMilp = true),
            bendersAdaptive = BendersAdaptiveConfig(minBinaryVariables = 100, maxIterations = 64, tolerance = 1e-6)
        )
        val notes = mutableListOf<String>()
        val mode = BendersStrategy.resolveSolveMode(request, notes)
        assertTrue(mode is SolveMode.Milp)
        assertTrue(notes.any { it.contains("skipped") })
    }

    @Test
    fun `resolveQualityGuardConfig uses defaults when no override`() {
        val config = BendersStrategy.resolveQualityGuardConfig(null)
        assertEquals(20.0, config.weakGapMultiplier, 0.01)
        assertEquals(90, config.iterationPressurePercent)
    }

    @Test
    fun `resolveQualityGuardConfig applies overrides`() {
        val override = BendersQualityOverrideConfig(weakGapMultiplier = 30.0, iterationPressurePercent = 80)
        val config = BendersStrategy.resolveQualityGuardConfig(override)
        assertEquals(30.0, config.weakGapMultiplier, 0.01)
        assertEquals(80, config.iterationPressurePercent)
    }

    @Test
    fun `resolveQualityReason returns null for good convergence`() {
        val adaptive = EffectiveBendersAdaptiveConfig(
            minBinaryVariables = 4, maxIterations = 64, tolerance = 1e-6,
            maxStallIterations = 10, objectiveStallIterations = 5
        )
        val qualityGuard = BendersQualityGuardConfig()
        val reason = BendersStrategy.resolveQualityReason(
            adaptive, qualityGuard,
            bendersIterations = 5, bendersGap = 1e-7, bendersTimeMs = 100
        )
        assertNull(reason)
    }

    @Test
    fun `resolveQualityReason returns gap_guard_exceeded for large gap`() {
        val adaptive = EffectiveBendersAdaptiveConfig(
            minBinaryVariables = 4, maxIterations = 64, tolerance = 1e-6,
            maxStallIterations = 10, objectiveStallIterations = 5
        )
        val qualityGuard = BendersQualityGuardConfig()
        val reason = BendersStrategy.resolveQualityReason(
            adaptive, qualityGuard,
            bendersIterations = 5, bendersGap = 0.5, bendersTimeMs = 100
        )
        assertEquals("gap_guard_exceeded", reason)
    }

    @Test
    fun `resolveQualityScore returns low score for good convergence`() {
        val adaptive = EffectiveBendersAdaptiveConfig(
            minBinaryVariables = 4, maxIterations = 64, tolerance = 1e-6,
            maxStallIterations = 10, objectiveStallIterations = 5
        )
        val qualityGuard = BendersQualityGuardConfig()
        val score = BendersStrategy.resolveQualityScore(
            adaptive, qualityGuard,
            bendersIterations = 5, bendersGap = 1e-7, bendersTimeMs = 100
        )
        assertTrue(score < 20.0)
    }

    @Test
    fun `resolveQualityScore returns high score for poor convergence`() {
        val adaptive = EffectiveBendersAdaptiveConfig(
            minBinaryVariables = 4, maxIterations = 64, tolerance = 1e-6,
            maxStallIterations = 10, objectiveStallIterations = 5
        )
        val qualityGuard = BendersQualityGuardConfig()
        val score = BendersStrategy.resolveQualityScore(
            adaptive, qualityGuard,
            bendersIterations = 60, bendersGap = 0.1, bendersTimeMs = 10000
        )
        assertTrue(score > 50.0)
    }
}
