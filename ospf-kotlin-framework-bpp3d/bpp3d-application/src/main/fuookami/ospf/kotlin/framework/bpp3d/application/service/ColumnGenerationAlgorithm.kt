@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.bpp3d.application.service

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Item
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation.Bpp3dLayerGenerationRequest
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation.Bpp3dLayerGenerationResult
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation.Bpp3dLayerGenerator
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation.DemandModeKey
import kotlin.time.Duration
import kotlin.time.TimeSource

data class ColumnGenerationConfig(
    val iterationLimit: Int = 128,
    val timeLimit: Duration = Duration.INFINITE,
    val maxColumnsPerIteration: Int = 64,
    val finalMilpEnabled: Boolean = true
)

data class ColumnGenerationState<V>(
    val iteration: Int,
    val columns: List<BinLayer>,
    val shadowPrices: Map<DemandModeKey, V> = emptyMap()
)

data class ColumnGenerationLpResult<V>(
    val shadowPrices: Map<DemandModeKey, V>,
    val objective: V? = null,
    val info: Map<String, String> = emptyMap()
)

data class ColumnGenerationFinalResult<V>(
    val columns: List<BinLayer>,
    val objective: V? = null,
    val info: Map<String, String> = emptyMap()
)

data class ColumnGenerationResult<V>(
    val columns: List<BinLayer>,
    val iterationCount: Int,
    val terminatedByIterationLimit: Boolean,
    val terminatedByTimeLimit: Boolean,
    val lpSolvedTimes: Int,
    val finalSolved: Boolean,
    val lpObjectives: List<V?>,
    val finalObjective: V?
)

fun interface ColumnGenerationRmpSolver<V> {
    suspend fun solve(state: ColumnGenerationState<V>): ColumnGenerationLpResult<V>
}

fun interface ColumnGenerationFinalSolver<V> {
    suspend fun solve(state: ColumnGenerationState<V>): ColumnGenerationFinalResult<V>
}

fun interface ColumnGenerationSolutionAnalyzer<V> {
    suspend fun analyze(state: ColumnGenerationState<V>)
}

fun interface ColumnGenerationHeartbeat<V> {
    suspend fun invoke(state: ColumnGenerationState<V>)
}

fun interface ColumnGenerationLayerRequestBuilder<V> {
    suspend fun build(
        state: ColumnGenerationState<V>,
        items: List<Item>,
        config: ColumnGenerationConfig
    ): Bpp3dLayerGenerationRequest<V>
}

/**
 * Column generation orchestrator in application layer.
 * application 层列生成编排器。
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
        var finalObjective: V? = null

        while (iterations < config.iterationLimit) {
            if (startedAt.elapsedNow() >= config.timeLimit) {
                terminatedByTimeLimit = true
                break
            }

            val state = ColumnGenerationState<V>(
                iteration = iterations,
                columns = columns,
                shadowPrices = latestShadowPrices
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
            lpSolvedTimes += 1

            val shadowPriceRefreshedState = ColumnGenerationState(
                iteration = iterations,
                columns = columns,
                shadowPrices = shadowPrices
            )
            val request = layerRequestBuilder?.build(
                shadowPriceRefreshedState,
                items,
                config
            ) ?: Bpp3dLayerGenerationRequest<V>(
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
                    shadowPrices = shadowPrices
                )
            )
            heartbeat?.invoke(
                ColumnGenerationState<V>(
                    iteration = iterations,
                    columns = columns,
                    shadowPrices = shadowPrices
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
            shadowPrices = latestShadowPrices
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
                finalState = finalState.copy(columns = columns)
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
            finalObjective = finalObjective
        )
    }
}
