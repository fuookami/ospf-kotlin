package fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure

import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*

sealed class SolveMode {
    data object Milp : SolveMode()
    data class Benders(val config: EffectiveBendersAdaptiveConfig) : SolveMode()
}

data class EffectiveBendersAdaptiveConfig(
    val minBinaryVariables: Int,
    val maxIterations: Int,
    val tolerance: Double,
    val maxStallIterations: Int? = null,
    val objectiveStallIterations: Int? = null
)

data class BendersQualityGuardConfig(
    val weakGapMultiplier: Double = 20.0,
    val weakGapFloor: Double = 1e-5,
    val iterationPressurePercent: Int = 90,
    val cutDensityMinIterations: Int = 8,
    val cutDensityThreshold: Double = 0.25,
    val trajectoryMinSnapshots: Int = 6,
    val trajectoryStepMultiplier: Double = 20.0,
    val trajectoryStepFloor: Double = 1e-6,
    val timeGuardMinMs: Long = 500,
    val scoreGapWeight: Double = 0.35,
    val scoreTimeWeight: Double = 0.2,
    val scoreIterationWeight: Double = 0.2,
    val scoreCutDensityWeight: Double = 0.15,
    val scoreTrajectoryWeight: Double = 0.1
)

object BendersStrategy {
    private const val BENDERS_QUALITY_REASON_GAP_GUARD_EXCEEDED = "gap_guard_exceeded"
    private const val BENDERS_QUALITY_REASON_TIME_GUARD_EXCEEDED = "time_guard_exceeded"
    private const val BENDERS_QUALITY_REASON_PROGRESS_GUARD_TRIGGERED = "progress_guard_triggered"
    private const val BENDERS_QUALITY_REASON_CUT_EFFICIENCY_LOW = "cut_efficiency_low"
    private const val BENDERS_QUALITY_REASON_TRAJECTORY_WEAK = "trajectory_weak"

    fun supportedAircraft(aircraftType: AircraftTypeInput): Boolean {
        return aircraftType == AircraftTypeInput.B737 || aircraftType == AircraftTypeInput.B757
    }

    fun tuneAdaptiveConfig(configured: BendersAdaptiveConfig, binaryVariables: Int): EffectiveBendersAdaptiveConfig {
        if (configured.maxIterations == 0) {
            return EffectiveBendersAdaptiveConfig(
                minBinaryVariables = configured.minBinaryVariables,
                maxIterations = configured.maxIterations,
                tolerance = configured.tolerance
            )
        }

        val iterationBoost = when {
            binaryVariables >= 400 -> 64
            binaryVariables >= 200 -> 32
            binaryVariables >= 100 -> 16
            binaryVariables >= 60 -> 8
            else -> 0
        }
        val baseTolerance = if (configured.tolerance > 0.0) configured.tolerance else 1e-6
        val tunedTolerance = when {
            binaryVariables >= 400 -> maxOf(baseTolerance, 5e-5)
            binaryVariables >= 200 -> maxOf(baseTolerance, 2e-5)
            binaryVariables >= 100 -> maxOf(baseTolerance, 1e-5)
            binaryVariables >= 60 -> maxOf(baseTolerance, 5e-6)
            else -> baseTolerance
        }
        val tunedMaxIterations = configured.maxIterations + iterationBoost
        val stallWindowBase = when {
            binaryVariables >= 400 -> 24
            binaryVariables >= 200 -> 16
            binaryVariables >= 100 -> 12
            binaryVariables >= 60 -> 8
            else -> 6
        }
        val objectiveStallWindowBase = when {
            binaryVariables >= 400 -> 6
            binaryVariables >= 200 -> 5
            binaryVariables >= 100 -> 4
            binaryVariables >= 60 -> 3
            else -> 2
        }

        return EffectiveBendersAdaptiveConfig(
            minBinaryVariables = configured.minBinaryVariables,
            maxIterations = tunedMaxIterations,
            tolerance = tunedTolerance,
            maxStallIterations = minOf(stallWindowBase, tunedMaxIterations.coerceAtLeast(1)),
            objectiveStallIterations = minOf(objectiveStallWindowBase, tunedMaxIterations.coerceAtLeast(1))
        )
    }

    fun resolveSolveMode(request: RequestDTO, notes: MutableList<String>): SolveMode {
        val binaryVariables = request.cargos.size * request.positions.size
        val tunedAdaptive = tuneAdaptiveConfig(request.bendersAdaptive, binaryVariables)
        val qualityGuard = resolveQualityGuardConfig(request.bendersQualityOverrides)

        if (request.solvePolicy.preferBenders) {
            if (binaryVariables < tunedAdaptive.minBinaryVariables) {
                notes.add("Benders requested but skipped: binary_variables=$binaryVariables < threshold=${tunedAdaptive.minBinaryVariables}")
                return SolveMode.Milp
            }
            notes.add("Benders requested; adaptive Benders path enabled in application layer")
            notes.add("benders_adaptive=min_binary_variables=${request.bendersAdaptive.minBinaryVariables},max_iterations=${request.bendersAdaptive.maxIterations},tolerance=${String.format("%.6f", request.bendersAdaptive.tolerance)}")
            notes.add("benders_adaptive_effective=min_binary_variables=${tunedAdaptive.minBinaryVariables},max_iterations=${tunedAdaptive.maxIterations},tolerance=${String.format("%.6f", tunedAdaptive.tolerance)},max_stall_iterations=${tunedAdaptive.maxStallIterations ?: "none"},objective_stall_iterations=${tunedAdaptive.objectiveStallIterations ?: "none"}")
            Diagnostics.pushGroupedNote(
                notes, Diagnostics.LEVEL_DIAGNOSTIC, Diagnostics.GROUP_SOLVER,
                Diagnostics.CODE_BENDERS_ADAPTIVE_EFFECTIVE,
                "effective min_binary_variables=${tunedAdaptive.minBinaryVariables},max_iterations=${tunedAdaptive.maxIterations},tolerance=${String.format("%.6f", tunedAdaptive.tolerance)},max_stall_iterations=${tunedAdaptive.maxStallIterations ?: "none"},objective_stall_iterations=${tunedAdaptive.objectiveStallIterations ?: "none"}"
            )
            notes.add("benders_problem_size_binary_variables=$binaryVariables")
            Diagnostics.pushGroupedNote(
                notes, Diagnostics.LEVEL_DIAGNOSTIC, Diagnostics.GROUP_SOLVER,
                Diagnostics.CODE_BENDERS_PROBLEM_SIZE_BINARY_VARIABLES,
                "binary_variables=$binaryVariables"
            )
            return SolveMode.Benders(tunedAdaptive)
        }
        return SolveMode.Milp
    }

    fun resolveQualityGuardConfig(override: BendersQualityOverrideConfig?): BendersQualityGuardConfig {
        val default = BendersQualityGuardConfig()
        if (override == null) return default
        val rawWeights = listOf(
            override.scoreGapWeight ?: default.scoreGapWeight,
            override.scoreTimeWeight ?: default.scoreTimeWeight,
            override.scoreIterationWeight ?: default.scoreIterationWeight,
            override.scoreCutDensityWeight ?: default.scoreCutDensityWeight,
            override.scoreTrajectoryWeight ?: default.scoreTrajectoryWeight
        )
        val weightSum = rawWeights.sum()
        val normalizedWeights = if (weightSum <= 1e-12) {
            listOf(0.35, 0.2, 0.2, 0.15, 0.1)
        } else {
            rawWeights.map { it / weightSum }
        }
        return BendersQualityGuardConfig(
            weakGapMultiplier = (override.weakGapMultiplier ?: default.weakGapMultiplier).coerceAtLeast(1.0),
            weakGapFloor = (override.weakGapFloor ?: default.weakGapFloor).coerceAtLeast(1e-12),
            iterationPressurePercent = (override.iterationPressurePercent ?: default.iterationPressurePercent).coerceIn(1, 100),
            cutDensityMinIterations = (override.cutDensityMinIterations ?: default.cutDensityMinIterations).coerceAtLeast(1),
            cutDensityThreshold = (override.cutDensityThreshold ?: default.cutDensityThreshold).coerceAtLeast(0.0),
            trajectoryMinSnapshots = (override.trajectoryMinSnapshots ?: default.trajectoryMinSnapshots).coerceAtLeast(2),
            trajectoryStepMultiplier = (override.trajectoryStepMultiplier ?: default.trajectoryStepMultiplier).coerceAtLeast(1.0),
            trajectoryStepFloor = (override.trajectoryStepFloor ?: default.trajectoryStepFloor).coerceAtLeast(1e-12),
            timeGuardMinMs = (override.timeGuardMinMs ?: default.timeGuardMinMs).coerceAtLeast(1),
            scoreGapWeight = normalizedWeights[0],
            scoreTimeWeight = normalizedWeights[1],
            scoreIterationWeight = normalizedWeights[2],
            scoreCutDensityWeight = normalizedWeights[3],
            scoreTrajectoryWeight = normalizedWeights[4]
        )
    }

    fun resolveGapGuard(adaptive: EffectiveBendersAdaptiveConfig): Double {
        return (adaptive.tolerance * 100.0).coerceIn(1e-4, 0.2)
    }

    fun resolveTimeGuardMs(adaptive: EffectiveBendersAdaptiveConfig, qualityGuard: BendersQualityGuardConfig): Long {
        val perIterationMs: Long = when {
            adaptive.tolerance >= 1e-4 -> 40
            adaptive.tolerance >= 1e-5 -> 60
            else -> 80
        }
        val stallBonus = (adaptive.maxStallIterations ?: 0).toLong() * 30
        return (adaptive.maxIterations.coerceAtLeast(1).toLong() * perIterationMs + stallBonus)
            .coerceAtLeast(qualityGuard.timeGuardMinMs)
    }

    fun resolveQualityReason(
        adaptive: EffectiveBendersAdaptiveConfig,
        qualityGuard: BendersQualityGuardConfig,
        bendersIterations: Int,
        bendersGap: Double,
        bendersTimeMs: Long,
        executedIterations: Int? = null,
        totalCuts: Int? = null,
        iterationSnapshots: List<Double>? = null
    ): String? {
        val gapGuard = resolveGapGuard(adaptive)
        if (bendersGap > gapGuard + 1e-12) return BENDERS_QUALITY_REASON_GAP_GUARD_EXCEEDED

        val timeGuardMs = resolveTimeGuardMs(adaptive, qualityGuard)
        val weakGap = bendersGap > (adaptive.tolerance * qualityGuard.weakGapMultiplier).coerceAtLeast(qualityGuard.weakGapFloor)
        if (weakGap && bendersTimeMs > timeGuardMs) return BENDERS_QUALITY_REASON_TIME_GUARD_EXCEEDED

        val effectiveIterations = executedIterations ?: bendersIterations
        val effectiveCuts = totalCuts ?: 0
        val iterationPressure = adaptive.maxIterations > 0 &&
            effectiveIterations * 100 >= adaptive.maxIterations * qualityGuard.iterationPressurePercent
        if (iterationPressure && weakGap) return BENDERS_QUALITY_REASON_PROGRESS_GUARD_TRIGGERED

        if (weakGap) {
            if (iterationSnapshots != null && iterationSnapshots.size >= qualityGuard.trajectoryMinSnapshots) {
                var absStepSum = 0.0
                for (i in 1 until iterationSnapshots.size) {
                    absStepSum += kotlin.math.abs(iterationSnapshots[i] - iterationSnapshots[i - 1])
                }
                val avgStepImprovement = absStepSum / (iterationSnapshots.size - 1)
                val stepThreshold = (adaptive.tolerance * qualityGuard.trajectoryStepMultiplier).coerceAtLeast(qualityGuard.trajectoryStepFloor)
                if (avgStepImprovement < stepThreshold) return BENDERS_QUALITY_REASON_TRAJECTORY_WEAK
            }
        }

        if (weakGap && effectiveIterations >= qualityGuard.cutDensityMinIterations) {
            val cutDensity = effectiveCuts.toDouble() / effectiveIterations
            if (cutDensity < qualityGuard.cutDensityThreshold) return BENDERS_QUALITY_REASON_CUT_EFFICIENCY_LOW
        }

        return null
    }

    fun resolveQualityScore(
        adaptive: EffectiveBendersAdaptiveConfig,
        qualityGuard: BendersQualityGuardConfig,
        bendersIterations: Int,
        bendersGap: Double,
        bendersTimeMs: Long,
        executedIterations: Int? = null,
        totalCuts: Int? = null,
        iterationSnapshots: List<Double>? = null
    ): Double {
        val gapGuard = resolveGapGuard(adaptive)
        val gapRisk = if (gapGuard > 0.0) (bendersGap / gapGuard).coerceIn(0.0, 1.0) else 0.0

        val timeGuardMs = resolveTimeGuardMs(adaptive, qualityGuard)
        val timeRisk = if (timeGuardMs > 0) (bendersTimeMs.toDouble() / timeGuardMs).coerceIn(0.0, 1.0) else 0.0

        val effectiveIterations = executedIterations ?: bendersIterations
        val effectiveCuts = totalCuts ?: 0
        val iterationRisk = if (adaptive.maxIterations > 0) {
            (effectiveIterations.toDouble() / adaptive.maxIterations).coerceIn(0.0, 1.0)
        } else 0.0

        val cutDensityRisk = if (effectiveIterations >= qualityGuard.cutDensityMinIterations) {
            val cutDensity = effectiveCuts.toDouble() / effectiveIterations
            if (qualityGuard.cutDensityThreshold <= 1e-12) 0.0
            else (1.0 - cutDensity / qualityGuard.cutDensityThreshold).coerceIn(0.0, 1.0)
        } else 0.0

        val trajectoryRisk = if (iterationSnapshots != null && iterationSnapshots.size >= qualityGuard.trajectoryMinSnapshots) {
            var absStepSum = 0.0
            for (i in 1 until iterationSnapshots.size) {
                absStepSum += kotlin.math.abs(iterationSnapshots[i] - iterationSnapshots[i - 1])
            }
            val avgStepImprovement = absStepSum / (iterationSnapshots.size - 1)
            val stepThreshold = (adaptive.tolerance * qualityGuard.trajectoryStepMultiplier).coerceAtLeast(qualityGuard.trajectoryStepFloor)
            if (avgStepImprovement <= 1e-12) 1.0
            else (stepThreshold / avgStepImprovement).coerceIn(0.0, 1.0)
        } else 0.0

        val score = gapRisk * qualityGuard.scoreGapWeight +
            timeRisk * qualityGuard.scoreTimeWeight +
            iterationRisk * qualityGuard.scoreIterationWeight +
            cutDensityRisk * qualityGuard.scoreCutDensityWeight +
            trajectoryRisk * qualityGuard.scoreTrajectoryWeight
        return (score * 100.0).coerceIn(0.0, 100.0)
    }
}
