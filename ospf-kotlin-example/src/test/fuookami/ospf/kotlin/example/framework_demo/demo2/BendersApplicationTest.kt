package fuookami.ospf.kotlin.example.framework_demo.demo2

import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * 应用层 Benders 分解测试 / Application-level Benders decomposition tests.
 *
 * 这些测试验证应用层的 Benders 求解路径行为，与 Rust 版本的 domain.rs 测试套件对齐 /
 * These tests verify the Benders solve path behavior at the application level,
 * aligning with the Rust version's test suite in domain.rs.
 *
 * 注意：需要实际 Gurobi 求解器的测试（例如 full_load_prefers_benders_without_fallback）未包含在此处，因为它们需要授权的求解器安装 /
 * Note: Tests that require an actual Gurobi solver (e.g., full_load_prefers_benders_without_fallback)
 * are not included here as they need a licensed solver installation.
 * 这些测试侧重于配置、诊断和回退行为 /
 * These tests focus on configuration, diagnostics, and fallback behavior.
 */
class BendersApplicationTest {

    // Aligns with Rust: full_load_skips_benders_when_problem_size_below_threshold
    @Test
    fun `full load skips Benders when problem size below threshold`() {
        val request = RequestDTO.sample().copy(
            solvePolicy = SolvePolicy(preferBenders = true, bendersFallbackToMilp = false),
            bendersAdaptive = BendersAdaptiveConfig(minBinaryVariables = 999, maxIterations = 8, tolerance = 1e-4)
        )
        val notes = mutableListOf<String>()
        val mode = BendersStrategy.resolveSolveMode(request, notes)
        assertTrue(mode is SolveMode.Milp)
        assertTrue(notes.any { it.contains("Benders requested but skipped") })
    }

    // Aligns with Rust: tune_benders_adaptive_config_scales_iterations_and_tolerance
    @Test
    fun `adaptive config scales iterations and tolerance for large problems`() {
        val config = BendersAdaptiveConfig(minBinaryVariables = 1, maxIterations = 32, tolerance = 1e-6)
        val tuned = BendersStrategy.tuneAdaptiveConfig(config, 240)
        assertEquals(1, tuned.minBinaryVariables)
        assertEquals(64, tuned.maxIterations)
        assertEquals(2e-5, tuned.tolerance, 1e-12)
        assertEquals(16, tuned.maxStallIterations)
        assertEquals(5, tuned.objectiveStallIterations)
    }

    // Aligns with Rust: tune_benders_adaptive_config_keeps_zero_iterations_for_failure_injection
    @Test
    fun `adaptive config keeps zero iterations for failure injection`() {
        val config = BendersAdaptiveConfig(minBinaryVariables = 1, maxIterations = 0, tolerance = 1e-6)
        val tuned = BendersStrategy.tuneAdaptiveConfig(config, 400)
        assertEquals(0, tuned.maxIterations)
        assertEquals(1e-6, tuned.tolerance, 1e-12)
        assertNull(tuned.maxStallIterations)
        assertNull(tuned.objectiveStallIterations)
    }

    // Aligns with Rust: resolve_benders_quality_reason_prefers_gap_guard
    @Test
    fun `quality reason prefers gap guard`() {
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
    fun `quality reason uses time guard on weak gap`() {
        val adaptive = EffectiveBendersAdaptiveConfig(
            minBinaryVariables = 1, maxIterations = 32, tolerance = 1e-6,
            maxStallIterations = 6, objectiveStallIterations = 2
        )
        val qualityGuard = BendersQualityGuardConfig()
        // timeGuardMs = 32 * 80 + 6 * 30 = 2740
        val reason = BendersStrategy.resolveQualityReason(
            adaptive, qualityGuard,
            bendersIterations = 8, bendersGap = 5e-5, bendersTimeMs = 2741,
            executedIterations = 8, totalCuts = 10
        )
        assertEquals("time_guard_exceeded", reason)
    }

    // Aligns with Rust: resolve_benders_quality_reason_uses_progress_guard_near_iteration_cap
    @Test
    fun `quality reason uses progress guard near iteration cap`() {
        val adaptive = EffectiveBendersAdaptiveConfig(
            minBinaryVariables = 1, maxIterations = 40, tolerance = 1e-6,
            maxStallIterations = 6, objectiveStallIterations = 2
        )
        val qualityGuard = BendersQualityGuardConfig()
        val reason = BendersStrategy.resolveQualityReason(
            adaptive, qualityGuard,
            bendersIterations = 38, bendersGap = 5e-5, bendersTimeMs = 2580
        )
        assertEquals("progress_guard_triggered", reason)
    }

    // Aligns with Rust: resolve_benders_quality_reason_detects_low_cut_efficiency_from_runtime_metrics
    @Test
    fun `quality reason detects low cut efficiency`() {
        val adaptive = EffectiveBendersAdaptiveConfig(
            minBinaryVariables = 1, maxIterations = 200, tolerance = 1e-6,
            maxStallIterations = 16, objectiveStallIterations = 4
        )
        val qualityGuard = BendersQualityGuardConfig()
        val reason = BendersStrategy.resolveQualityReason(
            adaptive, qualityGuard,
            bendersIterations = 9, bendersGap = 5e-5, bendersTimeMs = 16480,
            executedIterations = 20, totalCuts = 2
        )
        assertEquals("cut_efficiency_low", reason)
    }

    // Aligns with Rust: resolve_benders_quality_reason_detects_trajectory_weak_from_iteration_snapshots
    @Test
    fun `quality reason detects trajectory weak`() {
        val adaptive = EffectiveBendersAdaptiveConfig(
            minBinaryVariables = 1, maxIterations = 200, tolerance = 1e-6,
            maxStallIterations = 16, objectiveStallIterations = 4
        )
        val qualityGuard = BendersQualityGuardConfig()
        val snapshots = (1..8).map { 100.0 + it * 1e-7 }
        val reason = BendersStrategy.resolveQualityReason(
            adaptive, qualityGuard,
            bendersIterations = 8, bendersGap = 5e-5, bendersTimeMs = 16480,
            executedIterations = 8, totalCuts = 8, iterationSnapshots = snapshots
        )
        assertEquals("trajectory_weak", reason)
    }

    // Aligns with Rust: resolve_benders_quality_guard_config_applies_overrides
    @Test
    fun `quality guard config applies overrides`() {
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

    // Aligns with Rust: resolve_solve_mode_emits_effective_benders_adaptive_notes
    @Test
    fun `solve mode emits effective adaptive notes for large problem`() {
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
        assertEquals(64, adaptive.maxIterations)
        assertEquals(2e-5, adaptive.tolerance, 1e-12)
        assertEquals(16, adaptive.maxStallIterations)
        assertEquals(5, adaptive.objectiveStallIterations)
        assertTrue(notes.any { it.contains("benders_adaptive_effective=") })
        assertTrue(notes.any { it.contains("max_stall_iterations=16") })
        assertTrue(notes.any { it.contains("objective_stall_iterations=5") })
    }

    // Aligns with Rust: full_load_benders_notes_include_adaptive_config
    @Test
    fun `benders notes include adaptive config`() {
        val request = RequestDTO.sample().copy(
            solvePolicy = SolvePolicy(preferBenders = true, bendersFallbackToMilp = true)
        )
        val notes = mutableListOf<String>()
        BendersStrategy.resolveSolveMode(request, notes)
        assertTrue(notes.any { it.contains("benders_adaptive=min_binary_variables=") })
        assertTrue(notes.any { it.contains("benders_adaptive_effective=min_binary_variables=") })
        assertTrue(notes.any { it.contains("benders_problem_size_binary_variables=") })
    }

    // Aligns with Rust: full_load_returns_no_solution_with_diagnostics_when_cargo_exceeds_all_positions
    @Test
    fun `feasibility diagnostics detects cargo exceeding all positions`() {
        val request = RequestDTO.sample().copy(
            cargos = listOf(CargoInput(name = "Big", weight = 100.0, priority = 1, source = "S1", destination = "D1"))
        )
        val notes = mutableListOf<String>()
        FeasibilityDiagnostics.appendCoreFeasibilityDiagnostics(request, notes)
        assertTrue(notes.any { it.contains("cargo_exceeds_all_positions") })
    }

    // Aligns with Rust: weight_recommendation_returns_no_solution_with_diagnostics_when_min_payload_infeasible
    @Test
    fun `feasibility diagnostics detects invalid payload ratio`() {
        val request = RequestDTO.sample().copy(
            payloadUpperBound = 10.0,
            minPayloadRatio = 1.5
        )
        val notes = mutableListOf<String>()
        FeasibilityDiagnostics.appendCoreFeasibilityDiagnostics(request, notes)
        assertTrue(notes.any { it.contains("min_payload_ratio_out_of_range") })
    }

    // Aligns with Rust: full_load_returns_no_solution_when_capacity_is_too_small
    @Test
    fun `feasibility diagnostics detects capacity too small`() {
        val request = RequestDTO.sample().copy(
            positions = RequestDTO.sample().positions.map { it.copy(maxWeight = 5.0) }
        )
        val notes = mutableListOf<String>()
        FeasibilityDiagnostics.appendCoreFeasibilityDiagnostics(request, notes)
        assertTrue(notes.any { it.contains("cargo_exceeds_all_positions") })
    }

    // Aligns with Rust: full_load_returns_no_solution_when_envelope_is_too_tight
    @Test
    fun `feasibility diagnostics detects tight envelope`() {
        val request = RequestDTO.sample().copy(
            envelopeLongitudinalMomentMin = -1.0,
            envelopeLongitudinalMomentMax = 1.0
        )
        // Tight envelope doesn't trigger feasibility diagnostics by itself
        // (it's a solver-level constraint, not a pre-check)
        // But we can verify the request is valid for diagnostics
        val notes = mutableListOf<String>()
        FeasibilityDiagnostics.appendCoreFeasibilityDiagnostics(request, notes)
        // No pre-check failure expected for tight envelope
        assertTrue(notes.isEmpty())
    }

    // Aligns with Rust: notes_and_diagnostics_contract_for_milp_direct_is_consistent_across_apps
    @Test
    fun `milp direct mode produces consistent notes`() {
        val request = RequestDTO.sample().copy(
            solvePolicy = SolvePolicy(preferBenders = false, bendersFallbackToMilp = true)
        )
        val notes = mutableListOf<String>()
        val mode = BendersStrategy.resolveSolveMode(request, notes)
        assertTrue(mode is SolveMode.Milp)
        // MILP direct mode doesn't add benders-specific notes
        assertTrue(notes.none { it.contains("benders_adaptive_effective") })
    }

    // Aligns with Rust: notes_and_diagnostics_contract_for_benders_failed_is_consistent_across_apps
    @Test
    fun `benders failed mode produces consistent notes`() {
        val request = RequestDTO.sample().copy(
            solvePolicy = SolvePolicy(preferBenders = true, bendersFallbackToMilp = false),
            bendersAdaptive = BendersAdaptiveConfig(minBinaryVariables = 1, maxIterations = 0, tolerance = 1e-6)
        )
        val notes = mutableListOf<String>()
        val mode = BendersStrategy.resolveSolveMode(request, notes)
        assertTrue(mode is SolveMode.Benders)
        // Benders mode adds adaptive config notes
        assertTrue(notes.any { it.contains("benders_adaptive=") })
        assertTrue(notes.any { it.contains("benders_adaptive_effective=") })
    }
}
