package fuookami.ospf.kotlin.example.framework_demo.demo2

import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/** Tests the Benders decomposition strategy logic: aircraft support, adaptive config tuning, solve-mode resolution, and quality guards. */
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

    // Aligns with Rust: tune_benders_adaptive_config_scales_iterations_and_tolerance
    @Test
    fun `tuneAdaptiveConfig scales iterations and tolerance for 240 binary variables`() {
        val config = BendersAdaptiveConfig(minBinaryVariables = 1, maxIterations = 32, tolerance = 1e-6)
        val tuned = BendersStrategy.tuneAdaptiveConfig(config, 240)
        assertEquals(1, tuned.minBinaryVariables)
        assertEquals(64, tuned.maxIterations) // 32 + 32 boost
        assertEquals(2e-5, tuned.tolerance, 1e-12)
        assertEquals(16, tuned.maxStallIterations)
        assertEquals(5, tuned.objectiveStallIterations)
    }

    // Aligns with Rust: tune_benders_adaptive_config_keeps_zero_iterations_for_failure_injection
    @Test
    fun `tuneAdaptiveConfig keeps zero iterations for failure injection`() {
        val config = BendersAdaptiveConfig(minBinaryVariables = 1, maxIterations = 0, tolerance = 1e-6)
        val tuned = BendersStrategy.tuneAdaptiveConfig(config, 400)
        assertEquals(0, tuned.maxIterations)
        assertEquals(1e-6, tuned.tolerance, 1e-12)
        assertNull(tuned.maxStallIterations)
        assertNull(tuned.objectiveStallIterations)
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

    // Aligns with Rust: resolve_solve_mode_emits_effective_benders_adaptive_notes
    @Test
    fun `resolveSolveMode emits effective adaptive notes for large problem`() {
        val cargos = (0 until 20).map { i ->
            CargoInput(name = "C$i", weight = 1.0, priority = 1, source = "S", destination = "D")
        }
        val positions = (0 until 10).map { i ->
            PositionInput(name = "P$i", maxWeight = 100.0, longitudinalArm = i.toDouble(), lateralArm = 0.0)
        }
        val request = RequestDTO.sample().copy(
            cargos = cargos,
            positions = positions,
            solvePolicy = SolvePolicy(preferBenders = true, bendersFallbackToMilp = false),
            bendersAdaptive = BendersAdaptiveConfig(minBinaryVariables = 1, maxIterations = 32, tolerance = 1e-6)
        )
        val notes = mutableListOf<String>()
        val mode = BendersStrategy.resolveSolveMode(request, notes)
        assertTrue(mode is SolveMode.Benders)
        val adaptive = (mode as SolveMode.Benders).config
        assertEquals(64, adaptive.maxIterations) // 32 + 32 boost for >=200
        assertEquals(2e-5, adaptive.tolerance, 1e-12)
        assertEquals(16, adaptive.maxStallIterations)
        assertEquals(5, adaptive.objectiveStallIterations)
        assertTrue(notes.any { it.contains("benders_adaptive_effective=") })
        assertTrue(notes.any { it.contains("max_stall_iterations=16") })
        assertTrue(notes.any { it.contains("objective_stall_iterations=5") })
    }

    @Test
    fun `resolveQualityGuardConfig uses defaults when no override`() {
        val config = BendersStrategy.resolveQualityGuardConfig(null)
        assertEquals(20.0, config.weakGapMultiplier, 0.01)
        assertEquals(90, config.iterationPressurePercent)
    }

    // Aligns with Rust: resolve_benders_quality_guard_config_applies_overrides
    @Test
    fun `resolveQualityGuardConfig applies overrides`() {
        val override = BendersQualityOverrideConfig(
            weakGapMultiplier = 30.0,
            weakGapFloor = 2e-5,
            iterationPressurePercent = 80,
            cutDensityMinIterations = 10,
            cutDensityThreshold = 0.4,
            trajectoryMinSnapshots = 8,
            trajectoryStepMultiplier = 25.0,
            trajectoryStepFloor = 2e-6,
            timeGuardMinMs = 1200,
            scoreGapWeight = 0.4,
            scoreTimeWeight = 0.3,
            scoreIterationWeight = 0.1,
            scoreCutDensityWeight = 0.1,
            scoreTrajectoryWeight = 0.1
        )
        val config = BendersStrategy.resolveQualityGuardConfig(override)
        assertEquals(30.0, config.weakGapMultiplier, 1e-12)
        assertEquals(2e-5, config.weakGapFloor, 1e-12)
        assertEquals(80, config.iterationPressurePercent)
        assertEquals(10, config.cutDensityMinIterations)
        assertEquals(0.4, config.cutDensityThreshold, 1e-12)
        assertEquals(8, config.trajectoryMinSnapshots)
        assertEquals(25.0, config.trajectoryStepMultiplier, 1e-12)
        assertEquals(2e-6, config.trajectoryStepFloor, 1e-12)
        assertEquals(1200L, config.timeGuardMinMs)
        assertEquals(0.4, config.scoreGapWeight, 1e-12)
        assertEquals(0.3, config.scoreTimeWeight, 1e-12)
        assertEquals(0.1, config.scoreIterationWeight, 1e-12)
        assertEquals(0.1, config.scoreCutDensityWeight, 1e-12)
        assertEquals(0.1, config.scoreTrajectoryWeight, 1e-12)
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

    // Aligns with Rust: resolve_benders_quality_reason_prefers_gap_guard
    @Test
    fun `resolveQualityReason returns gap_guard_exceeded for large gap`() {
        val adaptive = EffectiveBendersAdaptiveConfig(
            minBinaryVariables = 1, maxIterations = 64, tolerance = 1e-6,
            maxStallIterations = 8, objectiveStallIterations = 3
        )
        val qualityGuard = BendersQualityGuardConfig()
        val reason = BendersStrategy.resolveQualityReason(
            adaptive, qualityGuard,
            bendersIterations = 10, bendersGap = 0.3, bendersTimeMs = 400
        )
        assertEquals("gap_guard_exceeded", reason)
    }

    // Aligns with Rust: resolve_benders_quality_reason_uses_time_guard_on_weak_gap
    @Test
    fun `resolveQualityReason returns time_guard_exceeded for weak gap and long time`() {
        val adaptive = EffectiveBendersAdaptiveConfig(
            minBinaryVariables = 1, maxIterations = 32, tolerance = 1e-6,
            maxStallIterations = 6, objectiveStallIterations = 2
        )
        val qualityGuard = BendersQualityGuardConfig()
        // timeGuardMs = 32 * 60 + 6 * 30 = 2100
        val reason = BendersStrategy.resolveQualityReason(
            adaptive, qualityGuard,
            bendersIterations = 8, bendersGap = 5e-5, bendersTimeMs = 2101
        )
        assertEquals("time_guard_exceeded", reason)
    }

    // Aligns with Rust: resolve_benders_quality_reason_uses_progress_guard_near_iteration_cap
    @Test
    fun `resolveQualityReason returns progress_guard_triggered near iteration cap`() {
        val adaptive = EffectiveBendersAdaptiveConfig(
            minBinaryVariables = 1, maxIterations = 40, tolerance = 1e-6,
            maxStallIterations = 6, objectiveStallIterations = 2
        )
        val qualityGuard = BendersQualityGuardConfig()
        // timeGuardMs = 40 * 60 + 6 * 30 = 2580
        val reason = BendersStrategy.resolveQualityReason(
            adaptive, qualityGuard,
            bendersIterations = 38, bendersGap = 5e-5, bendersTimeMs = 2580
        )
        assertEquals("progress_guard_triggered", reason)
    }

    // Aligns with Rust: resolve_benders_quality_reason_detects_low_cut_efficiency_from_runtime_metrics
    @Test
    fun `resolveQualityReason detects cut_efficiency_low from runtime metrics`() {
        val adaptive = EffectiveBendersAdaptiveConfig(
            minBinaryVariables = 1, maxIterations = 200, tolerance = 1e-6,
            maxStallIterations = 16, objectiveStallIterations = 4
        )
        val qualityGuard = BendersQualityGuardConfig()
        // timeGuardMs = 200 * 80 + 16 * 30 = 16480
        val reason = BendersStrategy.resolveQualityReason(
            adaptive, qualityGuard,
            bendersIterations = 9, bendersGap = 5e-5, bendersTimeMs = 16480,
            executedIterations = 20, totalCuts = 2
        )
        assertEquals("cut_efficiency_low", reason)
    }

    // Aligns with Rust: resolve_benders_quality_reason_detects_trajectory_weak_from_iteration_snapshots
    @Test
    fun `resolveQualityReason detects trajectory_weak from iteration snapshots`() {
        val adaptive = EffectiveBendersAdaptiveConfig(
            minBinaryVariables = 1, maxIterations = 200, tolerance = 1e-6,
            maxStallIterations = 16, objectiveStallIterations = 4
        )
        val qualityGuard = BendersQualityGuardConfig()
        // 8 snapshots with tiny improvement (1e-7 per step)
        val snapshots = (1..8).map { 100.0 + it * 1e-7 }
        // timeGuardMs = 200 * 80 + 16 * 30 = 16480
        val reason = BendersStrategy.resolveQualityReason(
            adaptive, qualityGuard,
            bendersIterations = 8, bendersGap = 5e-5, bendersTimeMs = 16480,
            executedIterations = 8, totalCuts = 8, iterationSnapshots = snapshots
        )
        assertEquals("trajectory_weak", reason)
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
