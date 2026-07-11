package fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure

import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*

/**
 * Sealed class representing the solver mode selection between MILP and Benders decomposition.
 * 密封类，表示在 MILP 和 Benders 分解之间的求解模式选择。
*/
sealed class SolveMode {

    /**
     * Direct MILP solve mode.
     * 直接 MILP 求解模式。
    */
    data object Milp : SolveMode()

    /**
     * Benders decomposition solve mode with adaptive configuration.
     * 带自适应配置的 Benders 分解求解模式。
     *
     * @property config The effective Benders adaptive configuration. / 有效的 Benders 自适应配置
    */
    data class Benders(val config: EffectiveBendersAdaptiveConfig) : SolveMode()
}

/**
 * Effective (tuned) Benders adaptive configuration after applying problem-size-based adjustments.
 * 应用基于问题规模调整后的有效（调优）Benders 自适应配置。
 *
 * @property minBinaryVariables Minimum number of binary variables required to enable Benders. / 启用 Benders 所需的最小二值变量数
 * @property maxIterations Maximum number of Benders iterations. / Benders 最大迭代次数
 * @property tolerance Convergence tolerance for the gap between master and sub objectives. / 主子问题目标间隙的收敛容差
 * @property maxStallIterations Maximum number of consecutive stalled iterations before termination. / 终止前允许的最大连续停滞迭代数
 * @property objectiveStallIterations Maximum number of consecutive objective-stalled iterations. / 最大连续目标停滞迭代数
*/
data class EffectiveBendersAdaptiveConfig(
    val minBinaryVariables: Int,
    val maxIterations: Int,
    val tolerance: Double,
    val maxStallIterations: Int? = null,
    val objectiveStallIterations: Int? = null
)

/**
 * Configuration for Benders quality guard checks, controlling gap, time, iteration, cut density, and trajectory thresholds.
 * Benders 质量守卫检查配置，控制间隙、时间、迭代、割密度和轨迹阈值。
 *
 * @property weakGapMultiplier Multiplier applied to tolerance to determine weak gap threshold. / 应用于容差的乘数，用于确定弱间隙阈值
 * @property weakGapFloor Floor value for the weak gap threshold. / 弱间隙阈值的下限值
 * @property iterationPressurePercent Percentage of max iterations at which iteration pressure triggers. / 触发迭代压力的最大迭代百分比
 * @property cutDensityMinIterations Minimum iterations before cut density check is applied. / 应用割密度检查前的最小迭代次数
 * @property cutDensityThreshold Threshold below which cut density is considered low. / 低于此阈值的割密度被视为低
 * @property trajectoryMinSnapshots Minimum snapshots required for trajectory analysis. / 轨迹分析所需的最小快照数
 * @property trajectoryStepMultiplier Multiplier applied to tolerance for trajectory step threshold. / 应用于容差的轨迹步长阈值乘数
 * @property trajectoryStepFloor Floor value for the trajectory step threshold. / 轨迹步长阈值的下限值
 * @property timeGuardMinMs Minimum time in milliseconds before time guard can trigger. / 时间守卫触发前的最小毫秒数
 * @property scoreGapWeight Weight of gap risk in quality score. / 间隙风险在质量评分中的权重
 * @property scoreTimeWeight Weight of time risk in quality score. / 时间风险在质量评分中的权重
 * @property scoreIterationWeight Weight of iteration risk in quality score. / 迭代风险在质量评分中的权重
 * @property scoreCutDensityWeight Weight of cut density risk in quality score. / 割密度风险在质量评分中的权重
 * @property scoreTrajectoryWeight Weight of trajectory risk in quality score. / 轨迹风险在质量评分中的权重
*/
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

/**
 * Strategy object for Benders decomposition configuration, solve mode resolution, and quality assessment.
 * Benders 分解配置、求解模式解析和质量评估的策略对象。
*/
object BendersStrategy {
    private const val BENDERS_QUALITY_REASON_GAP_GUARD_EXCEEDED = "gap_guard_exceeded"
    private const val BENDERS_QUALITY_REASON_TIME_GUARD_EXCEEDED = "time_guard_exceeded"
    private const val BENDERS_QUALITY_REASON_PROGRESS_GUARD_TRIGGERED = "progress_guard_triggered"
    private const val BENDERS_QUALITY_REASON_CUT_EFFICIENCY_LOW = "cut_efficiency_low"
    private const val BENDERS_QUALITY_REASON_TRAJECTORY_WEAK = "trajectory_weak"

    /**
     * Checks whether the given aircraft type is supported for Benders decomposition.
     * 检查给定的航空器类型是否支持 Benders 分解。
     *
     * @param aircraftType The aircraft type input to check. / 要检查的航空器类型输入
     * @return True if the aircraft type is supported, false otherwise. / 如果航空器类型受支持则返回 true，否则返回 false
    */
    fun supportedAircraft(aircraftType: AircraftTypeInput): Boolean {
        return aircraftType == AircraftTypeInput.B737 || aircraftType == AircraftTypeInput.B757
    }

    /**
     * Tunes the Benders adaptive configuration based on the number of binary variables, adjusting iterations, tolerance, and stall windows.
     * 根据二值变量数量调优 Benders 自适应配置，调整迭代次数、容差和停滞窗口。
     *
     * @param configured The user-configured Benders adaptive config. / 用户配置的 Benders 自适应配置
     * @param binaryVariables The number of binary variables in the problem. / 问题中的二值变量数量
     * @return The tuned effective Benders adaptive configuration. / 调优后的有效 Benders 自适应配置
    */
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

    /**
     * Resolves the solve mode (MILP or Benders) based on the request configuration and problem size.
     * 根据请求配置和问题规模解析求解模式（MILP 或 Benders）。
     *
     * @param request The request DTO containing solve policy and problem data. / 包含求解策略和问题数据的请求 DTO
     * @param notes Mutable list for collecting diagnostic notes. / 收集诊断信息的可变列表
     * @return The resolved solve mode. / 解析后的求解模式
    */
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

    /**
     * Resolves the effective quality guard configuration by merging user overrides with defaults and normalizing weights.
     * 通过合并用户覆盖与默认值并归一化权重，解析有效的质量守卫配置。
     *
     * @param override User-provided quality override configuration, or null for defaults. / 用户提供的质量覆盖配置，null 表示使用默认值
     * @return The resolved quality guard configuration with normalized weights. / 归一化权重后的质量守卫配置
    */
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

    /**
     * Resolves the gap guard threshold based on the adaptive configuration tolerance.
     * 根据自适应配置容差解析间隙守卫阈值。
     *
     * @param adaptive The effective Benders adaptive configuration. / 有效的 Benders 自适应配置
     * @return The gap guard threshold value. / 间隙守卫阈值
    */
    fun resolveGapGuard(adaptive: EffectiveBendersAdaptiveConfig): Double {
        return (adaptive.tolerance * 100.0).coerceIn(1e-4, 0.2)
    }

    /**
     * Resolves the time guard threshold in milliseconds based on adaptive config and quality guard settings.
     * 根据自适应配置和质量守卫设置解析时间守卫阈值（毫秒）。
     *
     * @param adaptive The effective Benders adaptive configuration. / 有效的 Benders 自适应配置
     * @param qualityGuard The quality guard configuration. / 质量守卫配置
     * @return The time guard threshold in milliseconds. / 时间守卫阈值（毫秒）
    */
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

    /**
     * Evaluates Benders solve quality and returns a reason string if quality is insufficient, or null if quality is acceptable.
     * 评估 Benders 求解质量，如果质量不足则返回原因字符串，否则返回 null。
     *
     * @param adaptive The effective Benders adaptive configuration. / 有效的 Benders 自适应配置
     * @param qualityGuard The quality guard configuration. / 质量守卫配置
     * @param bendersIterations The number of Benders iterations performed. / 执行的 Benders 迭代次数
     * @param bendersGap The final Benders gap. / 最终 Benders 间隙
     * @param bendersTimeMs The total Benders solve time in milliseconds. / Benders 总求解时间（毫秒）
     * @param executedIterations The actual number of iterations executed, if different from bendersIterations. / 实际执行的迭代次数，如果与 bendersIterations 不同
     * @param totalCuts The total number of cuts generated. / 生成的割总数
     * @param iterationSnapshots List of master objective values per iteration for trajectory analysis. / 每次迭代的主问题目标值列表，用于轨迹分析
     * @return A quality reason string if quality is insufficient, or null if acceptable. / 质量不足时的原因字符串，可接受时返回 null
    */
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

    /**
     * Computes a composite quality score (0-100) for the Benders solve based on gap, time, iteration, cut density, and trajectory risks.
     * 基于间隙、时间、迭代、割密度和轨迹风险计算 Benders 求解的综合质量评分（0-100）。
     *
     * @param adaptive The effective Benders adaptive configuration. / 有效的 Benders 自适应配置
     * @param qualityGuard The quality guard configuration. / 质量守卫配置
     * @param bendersIterations The number of Benders iterations performed. / 执行的 Benders 迭代次数
     * @param bendersGap The final Benders gap. / 最终 Benders 间隙
     * @param bendersTimeMs The total Benders solve time in milliseconds. / Benders 总求解时间（毫秒）
     * @param executedIterations The actual number of iterations executed, if different from bendersIterations. / 实际执行的迭代次数
     * @param totalCuts The total number of cuts generated. / 生成的割总数
     * @param iterationSnapshots List of master objective values per iteration for trajectory analysis. / 每次迭代的主问题目标值列表
     * @return The composite quality score between 0 and 100. / 0 到 100 之间的综合质量评分
    */
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
