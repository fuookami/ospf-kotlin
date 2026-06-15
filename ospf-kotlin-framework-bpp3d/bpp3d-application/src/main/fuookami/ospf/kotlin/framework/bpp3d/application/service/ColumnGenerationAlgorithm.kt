/**
 * 列生成算法。
 * Column generation algorithm.
 */
package fuookami.ospf.kotlin.framework.bpp3d.application.service

import kotlin.time.*
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation.*

/**
 * 列生成配置。
 * Column generation configuration.
 *
 * @property iterationLimit 最大迭代次数 / maximum iteration count
 * @property timeLimit 时间限制 / time limit
 * @property maxColumnsPerIteration 每次迭代最大列数 / max columns per iteration
 * @property finalMilpEnabled 是否启用最终 MILP 求解 / whether to enable final MILP solving
 */
data class ColumnGenerationConfig(
    val iterationLimit: Int = 128,
    val timeLimit: Duration = Duration.INFINITE,
    val maxColumnsPerIteration: Int = 64,
    val finalMilpEnabled: Boolean = true
)

/**
 * 列生成状态。
 * Column generation state.
 *
 * @param V 数值类型 / numeric type
 * @property iteration 当前迭代 / current iteration
 * @property columns 当前列集合 / current column set
 * @property bins 最终箱子（可选） / final bins (optional)
 * @property shadowPrices 影子价格映射 / shadow price map
 * @property continuousRadiusSolverPrototypes 连续半径 solver 变量原型 / continuous-radius solver variable prototypes
 */
data class ColumnGenerationState<V>(
    val iteration: Int,
    val columns: List<BinLayer>,
    val bins: List<Bin<BinLayer, FltX>> = emptyList(),
    val shadowPrices: Map<DemandModeKey, V> = emptyMap(),
    val continuousRadiusSolverPrototypes: List<ContinuousCylinderRadiusSolverPrototype> = emptyList(),
    val continuousRadiusSolverResults: Map<String, FltX> = emptyMap(),
    // PWL results stored as opaque Map for public API - internal types remain internal
    val pwlContinuousRadiusResults: Map<String, Map<String, FltX>> = emptyMap()
)

/**
 * 从货物列表抽取连续半径 solver 变量原型。
 * Extract continuous-radius solver variable prototypes from item list.
 *
 * @param items 货物列表 / item list
 * @return 连续半径 solver 变量原型 / continuous-radius solver variable prototypes
 */
fun continuousRadiusSolverPrototypesFromItems(
    items: List<Item>
): List<ContinuousCylinderRadiusSolverPrototype> {
    val prototypes = LinkedHashMap<String, ContinuousCylinderRadiusSolverPrototype>()
    for (item in items) {
        val spec = item.packageShape.shapeSpec as? PackageShapeSpec.VerticalCylinder ?: continue
        val prototype = spec.continuousRadiusSolverPrototype(
            source = continuousCylinderRadiusSolverSource(item)
        ) ?: continue
        prototypes.putIfAbsent(prototype.variableName, prototype)
    }
    return prototypes.values.toList()
}

/**
 * 从 info 映射中提取连续半径 solver 选出结果。
 * Extract continuous-radius solver-selected results from info map.
 *
 * @param info 信息映射 / info map
 * @return solver 选出半径映射 / solver-selected radius map
 */
fun extractContinuousRadiusSolverResultsFromInfo(
    info: Map<String, String>
): Map<String, FltX> {
    val prefix = "continuous_radius_solver_selected_"
    val results = LinkedHashMap<String, FltX>()
    for ((key, value) in info) {
        if (key.startsWith(prefix)) {
            val variableName = key.removePrefix(prefix)
            results[variableName] = FltX(value.toDouble())
        }
    }
    return results
}

/**
 * 列生成 LP 求解结果。
 * Column generation LP solve result.
 *
 * @param V 数值类型 / numeric type
 * @property shadowPrices 影子价格 / shadow prices
 * @property objective 目标值（可选） / objective value (optional)
 * @property info 附加信息 / additional info
 */
data class ColumnGenerationLpResult<V>(
    val shadowPrices: Map<DemandModeKey, V>,
    val objective: V? = null,
    val info: Map<String, String> = emptyMap()
)

/**
 * 列生成最终 MILP 求解结果。
 * Column generation final MILP solve result.
 *
 * @param V 数值类型 / numeric type
 * @property columns 最终列集合 / final column set
 * @property bins 最终箱子 / final bins
 * @property objective 目标值（可选） / objective value (optional)
 * @property info 附加信息 / additional info
 */
data class ColumnGenerationFinalResult<V>(
    val columns: List<BinLayer>,
    val bins: List<Bin<BinLayer, FltX>> = emptyList(),
    val objective: V? = null,
    val info: Map<String, String> = emptyMap(),
    // PWL results stored as opaque Map for public API - internal types remain internal
    val pwlContinuousRadiusResults: Map<String, Map<String, FltX>> = emptyMap()
)

/**
 * 列生成完整结果。
 * Column generation complete result.
 *
 * @param V 数值类型 / numeric type
 * @property columns 最终列集合 / final column set
 * @property iterationCount 迭代次数 / iteration count
 * @property terminatedByIterationLimit 是否因迭代限制终止 / terminated by iteration limit
 * @property terminatedByTimeLimit 是否因时间限制终止 / terminated by time limit
 * @property lpSolvedTimes LP 求解次数 / LP solve count
 * @property finalSolved 是否求解了最终 MILP / whether final MILP was solved
 * @property lpObjectives LP 目标值列表 / LP objective list
 * @property finalObjective 最终目标值（可选） / final objective (optional)
 * @property elapsed 耗时 / elapsed time
 * @property lpInfos LP 求解信息列表 / LP info list
 * @property finalInfo 最终求解信息 / final solve info
 */
data class ColumnGenerationResult<V>(
    val columns: List<BinLayer>,
    val iterationCount: Int,
    val terminatedByIterationLimit: Boolean,
    val terminatedByTimeLimit: Boolean,
    val lpSolvedTimes: Int,
    val finalSolved: Boolean,
    val lpObjectives: List<V?>,
    val finalObjective: V?,
    val elapsed: Duration = Duration.ZERO,
    val lpInfos: List<Map<String, String>> = emptyList(),
    val finalInfo: Map<String, String> = emptyMap(),
    val continuousRadiusSolverResults: Map<String, FltX> = emptyMap(),
    // PWL results stored as opaque Map for public API - internal types remain internal
    val pwlContinuousRadiusResults: Map<String, Map<String, FltX>> = emptyMap()
)

/**
 * 列生成 RMP 求解器。
 * Column generation RMP solver.
 *
 * @param V 数值类型 / numeric type
 */
fun interface ColumnGenerationRmpSolver<V> {
    /**
     * 求解 RMP。
     * Solve RMP.
     *
     * @param state 列生成状态 / column generation state
     * @return LP 求解结果 / LP solve result
     */
    suspend fun solve(state: ColumnGenerationState<V>): ColumnGenerationLpResult<V>
}

/**
 * 列生成最终 MILP 求解器。
 * Column generation final MILP solver.
 *
 * @param V 数值类型 / numeric type
 */
fun interface ColumnGenerationFinalSolver<V> {
    /**
     * 求解最终 MILP。
     * Solve final MILP.
     *
     * @param state 列生成状态 / column generation state
     * @return 最终求解结果 / final solve result
     */
    suspend fun solve(state: ColumnGenerationState<V>): ColumnGenerationFinalResult<V>
}

/**
 * 列生成解分析器。
 * Column generation solution analyzer.
 *
 * @param V 数值类型 / numeric type
 */
fun interface ColumnGenerationSolutionAnalyzer<V> {
    /**
     * 分析当前状态。
     * Analyze current state.
     *
     * @param state 列生成状态 / column generation state
     */
    suspend fun analyze(state: ColumnGenerationState<V>)
}

/**
 * 列生成心跳回调。
 * Column generation heartbeat callback.
 *
 * @param V 数值类型 / numeric type
 */
fun interface ColumnGenerationHeartbeat<V> {
    /**
     * 心跳回调。
     * Heartbeat callback.
     *
     * @param state 列生成状态 / column generation state
     */
    suspend fun invoke(state: ColumnGenerationState<V>)
}

/**
 * 列生成层请求构建器。
 * Column generation layer request builder.
 *
 * @param V 数值类型 / numeric type
 */
fun interface ColumnGenerationLayerRequestBuilder<V> {
    /**
     * 构建层生成请求。
     * Build layer generation request.
     *
     * @param state 列生成状态 / column generation state
     * @param items 货物列表 / item list
     * @param config 列生成配置 / column generation config
     * @return 层生成请求 / layer generation request
     */
    suspend fun build(
        state: ColumnGenerationState<V>,
        items: List<Item>,
        config: ColumnGenerationConfig
    ): Bpp3dLayerGenerationRequest<V>
}

/**
 * 列生成编排器，协调 RMP 求解、列生成和最终 MILP 求解。
 * Column generation orchestrator, coordinates RMP solving, column generation and final MILP solving.
 *
 * @param V 数值类型 / numeric type
 * @property layerGenerator 层生成器 / layer generator
 * @property rmpSolver RMP 求解器（可选） / RMP solver (optional)
 * @property finalMilpSolver 最终 MILP 求解器（可选） / final MILP solver (optional)
 * @property solutionAnalyzer 解分析器（可选） / solution analyzer (optional)
 * @property heartbeat 心跳回调（可选） / heartbeat callback (optional)
 * @property layerRequestBuilder 层请求构建器（可选） / layer request builder (optional)
 */
class ColumnGenerationAlgorithm<V>(
    private val layerGenerator: Bpp3dLayerGenerator<V>,
    private val rmpSolver: ColumnGenerationRmpSolver<V>? = null,
    private val finalMilpSolver: ColumnGenerationFinalSolver<V>? = null,
    private val solutionAnalyzer: ColumnGenerationSolutionAnalyzer<V>? = null,
    private val heartbeat: ColumnGenerationHeartbeat<V>? = null,
    private val layerRequestBuilder: ColumnGenerationLayerRequestBuilder<V>? = null,
    private val initialColumns: suspend () -> List<BinLayer> = { emptyList() },
    private val solveRmpAndExtractShadowPrice: suspend (ColumnGenerationState<V>) -> Map<DemandModeKey, V> = { emptyMap() },
    private val solveRmpWithResult: (suspend (ColumnGenerationState<V>) -> ColumnGenerationLpResult<V>)? = null,
    private val filterByReducedCost: suspend (ColumnGenerationState<V>, List<Bpp3dLayerGenerationResult<V>>) -> List<Bpp3dLayerGenerationResult<V>> = { _, layers -> layers },
    private val deduplicateColumns: (List<BinLayer>) -> List<BinLayer> = { it.distinct() },
    private val solveFinalMilp: suspend (ColumnGenerationState<V>) -> Unit = {},
    private val solveFinalMilpWithResult: (suspend (ColumnGenerationState<V>) -> ColumnGenerationFinalResult<V>)? = null,
    private val analyzeSolution: suspend (ColumnGenerationState<V>) -> Unit = {},
    private val onIterationHeartbeat: suspend (ColumnGenerationState<V>) -> Unit = {}
) {
    /**
     * 执行列生成求解。
     * Execute column generation solving.
     *
     * @param items 货物列表 / item list
     * @param config 列生成配置 / column generation config
     * @return 列生成结果 / column generation result
     */
    suspend fun solve(
        items: List<Item>,
        config: ColumnGenerationConfig = ColumnGenerationConfig()
    ): ColumnGenerationResult<V> {
        val startedAt = TimeSource.Monotonic.markNow()
        var columns = deduplicateColumns(initialColumns.invoke())
        var terminatedByIterationLimit = false
        var terminatedByTimeLimit = false
        var iterations = 0
        var lpSolvedTimes = 0
        var finalSolved = false
        var latestShadowPrices: Map<DemandModeKey, V> = emptyMap()
        val lpObjectives = ArrayList<V?>()
        val lpInfos = ArrayList<Map<String, String>>()
        var finalObjective: V? = null
        var finalInfo: Map<String, String> = emptyMap()
        val continuousRadiusSolverPrototypes = continuousRadiusSolverPrototypesFromItems(items)

        while (iterations < config.iterationLimit) {
            if (startedAt.elapsedNow() >= config.timeLimit) {
                terminatedByTimeLimit = true
                break
            }

            val state = ColumnGenerationState<V>(
                iteration = iterations,
                columns = columns,
                bins = emptyList(),
                shadowPrices = latestShadowPrices,
                continuousRadiusSolverPrototypes = continuousRadiusSolverPrototypes
            )
            val lpResult = when {
                rmpSolver != null -> rmpSolver.solve(state)
                solveRmpWithResult != null -> solveRmpWithResult.invoke(state)
                else -> ColumnGenerationLpResult(
                    shadowPrices = solveRmpAndExtractShadowPrice(state)
                )
            }
            val shadowPrices = lpResult.shadowPrices
            latestShadowPrices = shadowPrices
            lpObjectives.add(lpResult.objective)
            lpInfos.add(lpResult.info)
            lpSolvedTimes += 1

            val shadowPriceRefreshedState = ColumnGenerationState(
                iteration = iterations,
                columns = columns,
                bins = emptyList(),
                shadowPrices = shadowPrices,
                continuousRadiusSolverPrototypes = continuousRadiusSolverPrototypes
            )
            val request = layerRequestBuilder?.build(
                shadowPriceRefreshedState,
                items,
                config
            ) ?: bpp3dLayerGenerationRequest(
                iteration = iterations,
                items = items,
                existingLayers = columns,
                shadowPrices = shadowPrices,
                maxCandidates = config.maxColumnsPerIteration
            )
            val candidates = layerGenerator.generate(request)
            val accepted = filterByReducedCost(
                shadowPriceRefreshedState,
                candidates
            )
            if (accepted.isEmpty()) {
                break
            }

            columns = deduplicateColumns(columns + accepted.map { it.layer })
            onIterationHeartbeat(
                ColumnGenerationState<V>(
                    iteration = iterations,
                    columns = columns,
                    bins = emptyList(),
                    shadowPrices = shadowPrices,
                    continuousRadiusSolverPrototypes = continuousRadiusSolverPrototypes
                )
            )
            heartbeat?.invoke(
                ColumnGenerationState<V>(
                    iteration = iterations,
                    columns = columns,
                    bins = emptyList(),
                    shadowPrices = shadowPrices,
                    continuousRadiusSolverPrototypes = continuousRadiusSolverPrototypes
                )
            )
            iterations += 1
        }

        if (iterations >= config.iterationLimit) {
            terminatedByIterationLimit = true
        }

        var finalState = ColumnGenerationState<V>(
            iteration = iterations,
            columns = columns,
            bins = emptyList(),
            shadowPrices = latestShadowPrices,
            continuousRadiusSolverPrototypes = continuousRadiusSolverPrototypes
        )
        if (config.finalMilpEnabled) {
            val finalResult = when {
                finalMilpSolver != null -> finalMilpSolver.solve(finalState)
                solveFinalMilpWithResult != null -> solveFinalMilpWithResult.invoke(finalState)
                else -> null
            }
            if (finalResult != null) {
                columns = deduplicateColumns(finalResult.columns)
                finalObjective = finalResult.objective
                finalInfo = finalResult.info
                val solverResults = extractContinuousRadiusSolverResultsFromInfo(finalResult.info)
                finalState = finalState.copy(
                    columns = columns,
                    bins = finalResult.bins,
                    continuousRadiusSolverResults = solverResults,
                    pwlContinuousRadiusResults = finalResult.pwlContinuousRadiusResults
                )
            } else {
                solveFinalMilp(finalState)
            }
            finalSolved = true
        }
        analyzeSolution(finalState)
        solutionAnalyzer?.analyze(finalState)

        return ColumnGenerationResult<V>(
            columns = columns,
            iterationCount = iterations,
            terminatedByIterationLimit = terminatedByIterationLimit,
            terminatedByTimeLimit = terminatedByTimeLimit,
            lpSolvedTimes = lpSolvedTimes,
            finalSolved = finalSolved,
            lpObjectives = lpObjectives,
            finalObjective = finalObjective,
            elapsed = startedAt.elapsedNow(),
            lpInfos = lpInfos,
            finalInfo = finalInfo,
            continuousRadiusSolverResults = finalState.continuousRadiusSolverResults,
            pwlContinuousRadiusResults = finalState.pwlContinuousRadiusResults
        )
    }
}

/**
 * 使用量纲货物列表执行列生成求解。
 * Execute column generation solving with quantity item list.
 *
 * @param T 量纲数值类型 / quantity numeric type
 * @param items 量纲货物列表 / quantity item list
 * @param config 列生成配置 / column generation config
 * @param materialCache 物料缓存 / material cache
 * @param itemCache 货物缓存 / item cache
 * @return 列生成结果 / column generation result
 */
suspend fun <V, T : FloatingNumber<T>> ColumnGenerationAlgorithm<V>.solveQuantity(
    items: List<QuantityItem<T>>,
    config: ColumnGenerationConfig = ColumnGenerationConfig(),
    materialCache: MutableMap<QuantityMaterial<T>, Material<FltX>> = LinkedHashMap(),
    itemCache: MutableMap<QuantityItem<T>, ActualItem> = LinkedHashMap()
): ColumnGenerationResult<V> {
    return solve(
        items = items.map { it.toModel(materialCache, itemCache) },
        config = config
    )
}
