/**
 * 列生成算法。 / Column generation algorithm.
 *
 * 对应 BPP3D 的 `ColumnGenerationAlgorithm`：编排「初始列 → RMP(LP) 求对偶 → 定价生成候选列 → 按检验数过滤
 * → 加列」的迭代，最后用最终 MILP 选出具体板材与层。本文件只做编排，不硬编码任何领域子模型。
 * / Mirrors BPP3D `ColumnGenerationAlgorithm`: orchestrates the iteration of "initial columns → RMP (LP) duals →
 * pricing candidate columns → reduced-cost filtering → column addition", finishing with a final MILP that
 * selects concrete sheets and layers. This file only orchestrates and hard-codes no domain submodel.
*/
package fuookami.ospf.kotlin.framework.bpp2d.application.service

import kotlin.time.Duration
import kotlin.time.TimeSource
import kotlin.time.Duration.Companion.ZERO
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.Layer
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.Sheet
import fuookami.ospf.kotlin.framework.bpp2d.domain.layer_generation.Bpp2dLayerGenerationRequest
import fuookami.ospf.kotlin.framework.bpp2d.domain.layer_generation.Bpp2dLayerGenerationResult
import fuookami.ospf.kotlin.framework.bpp2d.domain.layer_generation.Bpp2dLayerGenerator
import fuookami.ospf.kotlin.framework.bpp2d.domain.layer_generation.DemandModeKey

/**
 * 列生成配置。 / Column generation configuration.
 *
 * @property iterationLimit 最大迭代次数 / maximum iteration count
 * @property timeLimit 时间限制 / time limit
 * @property maxColumnsPerIteration 每次迭代最大候选列数 / maximum candidate columns per iteration
 * @property finalMilpEnabled 是否启用最终 MILP / whether the final MILP is enabled
 * @property reducedCostTolerance 检验数容差，小于该值的列被视为无改进 / reduced-cost tolerance; columns below it
 * are treated as making no progress
*/
data class ColumnGenerationConfig(
    val iterationLimit: Int = 64,
    val timeLimit: Duration = ZERO,
    val maxColumnsPerIteration: Int = 32,
    val finalMilpEnabled: Boolean = true,
    val reducedCostTolerance: FltX = FltX(1e-6)
)

/**
 * 列生成状态。 / Column generation state.
 *
 * @property iteration 当前迭代 / current iteration
 * @property columns 当前列集合 / current column set
 * @property sheets 最终板材（可选） / final sheets (optional)
 * @property shadowPrices 影子价格映射 / shadow price map
 * @property additionalShadowPrices 扩展影子价格映射 / extension shadow price map
*/
data class ColumnGenerationState(
    val iteration: Int,
    val columns: List<Layer<FltX>>,
    val sheets: List<Sheet<FltX>> = emptyList(),
    val shadowPrices: Map<DemandModeKey, FltX> = emptyMap(),
    val additionalShadowPrices: Map<String, FltX> = emptyMap()
)

/**
 * LP 求解结果。 / LP solve result.
 *
 * @property shadowPrices 影子价格 / shadow prices
 * @property objective 目标值（可选） / objective value (optional)
 * @property info 附加信息 / additional info
 * @property additionalShadowPrices 扩展影子价格 / extension shadow prices
*/
data class ColumnGenerationLpResult(
    val shadowPrices: Map<DemandModeKey, FltX>,
    val objective: FltX? = null,
    val info: Map<String, String> = emptyMap(),
    val additionalShadowPrices: Map<String, FltX> = emptyMap()
)

/**
 * 最终 MILP 求解结果。 / Final MILP solve result.
 *
 * @property columns 最终列集合 / final column set
 * @property sheets 最终板材 / final sheets
 * @property objective 目标值（可选） / objective value (optional)
 * @property info 附加信息 / additional info
*/
data class ColumnGenerationFinalResult(
    val columns: List<Layer<FltX>>,
    val sheets: List<Sheet<FltX>> = emptyList(),
    val objective: FltX? = null,
    val info: Map<String, String> = emptyMap()
)

/**
 * 列生成完整结果。 / Complete column generation result.
 *
 * @property columns 最终列集合 / final column set
 * @property sheets 最终板材 / final sheets
 * @property iterationCount 迭代次数 / iteration count
 * @property terminatedByIterationLimit 是否因迭代上限终止 / whether terminated by the iteration limit
 * @property terminatedByTimeLimit 是否因时间上限终止 / whether terminated by the time limit
 * @property lpSolvedTimes LP 求解次数 / LP solve count
 * @property finalSolved 是否求解了最终 MILP / whether the final MILP was solved
 * @property lpObjectives 各次 LP 目标值 / LP objective per iteration
 * @property finalObjective 最终目标值（可选） / final objective (optional)
 * @property elapsed 耗时 / elapsed time
 * @property lpInfos 各次 LP 求解信息 / LP solve info per iteration
 * @property finalInfo 最终求解信息 / final solve info
*/
data class ColumnGenerationResult(
    val columns: List<Layer<FltX>>,
    val sheets: List<Sheet<FltX>>,
    val iterationCount: Int,
    val terminatedByIterationLimit: Boolean,
    val terminatedByTimeLimit: Boolean,
    val lpSolvedTimes: Int,
    val finalSolved: Boolean,
    val lpObjectives: List<FltX?>,
    val finalObjective: FltX?,
    val elapsed: Duration = ZERO,
    val lpInfos: List<Map<String, String>> = emptyList(),
    val finalInfo: Map<String, String> = emptyMap()
)

/**
 * RMP 求解器。 / RMP solver.
*/
fun interface ColumnGenerationRmpSolver {
    /**
     * 求解 RMP。 / Solve the RMP.
     *
     * @param state 列生成状态 / column generation state
     * @return LP 求解结果 / LP solve result
    */
    suspend fun solve(state: ColumnGenerationState): Ret<ColumnGenerationLpResult>
}

/**
 * 最终 MILP 求解器。 / Final MILP solver.
*/
fun interface ColumnGenerationFinalSolver {
    /**
     * 求解最终 MILP。 / Solve the final MILP.
     *
     * @param state 列生成状态 / column generation state
     * @return 最终求解结果 / final solve result
    */
    suspend fun solve(state: ColumnGenerationState): Ret<ColumnGenerationFinalResult>
}

/**
 * 解分析器。 / Solution analyzer.
*/
fun interface ColumnGenerationSolutionAnalyzer {
    /**
     * 分析当前状态。 / Analyze the current state.
     *
     * @param state 列生成状态 / column generation state
     * @return 操作结果 / operation result
    */
    suspend fun analyze(state: ColumnGenerationState): Try
}

/**
 * 失败阶段。 / Failure stage.
*/
enum class ColumnGenerationFailureStage {
    /** 初始列阶段 / initial columns */
    InitialColumns,

    /** RMP 阶段 / restricted master problem */
    RestrictedMasterProblem,

    /** 候选列过滤阶段 / candidate filter */
    CandidateFilter,

    /** 最终 MILP 阶段 / final MILP */
    FinalMilp,

    /** 解分析阶段 / solution analysis */
    SolutionAnalysis
}

/**
 * 失败上下文。 / Failure context.
 *
 * @property stage 失败阶段 / failure stage
 * @property state 失败时的状态 / state at failure
 * @property errors 原始错误 / original errors
*/
data class ColumnGenerationFailure(
    val stage: ColumnGenerationFailureStage,
    val state: ColumnGenerationState,
    val errors: List<Error<ErrorCode>>
)

/**
 * 失败分析器。 / Failure analyzer.
*/
fun interface ColumnGenerationFailureAnalyzer {
    /**
     * 分析失败状态。 / Analyze a failed state.
     *
     * @param failure 失败上下文 / failure context
     * @return 操作结果 / operation result
    */
    suspend fun analyze(failure: ColumnGenerationFailure): Try
}

/**
 * 心跳回调。 / Heartbeat callback.
*/
fun interface ColumnGenerationHeartbeat {
    /**
     * 心跳回调。 / Heartbeat callback.
     *
     * @param state 列生成状态 / column generation state
    */
    suspend fun invoke(state: ColumnGenerationState)
}

/**
 * 层生成请求构建器。 / Layer generation request builder.
*/
fun interface ColumnGenerationLayerRequestBuilder {
    /**
     * 构建层生成请求。 / Build the layer generation request.
     *
     * @param state 列生成状态 / column generation state
     * @param config 列生成配置 / column generation config
     * @return 层生成请求 / layer generation request
    */
    fun build(
        state: ColumnGenerationState,
        config: ColumnGenerationConfig
    ): Bpp2dLayerGenerationRequest<FltX>
}

/**
 * 列生成编排器。 / Column generation orchestrator.
 *
 * @property layerGenerator 层生成器 / layer generator
 * @property rmpSolver RMP 求解器（可选） / RMP solver (optional)
 * @property finalMilpSolver 最终 MILP 求解器（可选） / final MILP solver (optional)
 * @property solutionAnalyzer 解分析器（可选） / solution analyzer (optional)
 * @property heartbeat 心跳回调（可选） / heartbeat callback (optional)
 * @property layerRequestBuilder 层请求构建器（可选） / layer request builder (optional)
 * @property initialColumnsWithResult 可失败的初始列提供函数 / fallible initial columns provider
 * @property filterByReducedCostWithResult 可失败的列过滤函数 / fallible column filter
 * @property deduplicateColumns 列去重函数 / column deduplication function
 * @property failureAnalyzer 失败分析器（可选） / failure analyzer (optional)
*/
class ColumnGenerationAlgorithm(
    private val layerGenerator: Bpp2dLayerGenerator<FltX>,
    private val rmpSolver: ColumnGenerationRmpSolver? = null,
    private val finalMilpSolver: ColumnGenerationFinalSolver? = null,
    private val solutionAnalyzer: ColumnGenerationSolutionAnalyzer? = null,
    private val heartbeat: ColumnGenerationHeartbeat? = null,
    private val layerRequestBuilder: ColumnGenerationLayerRequestBuilder? = null,
    private val initialColumnsWithResult: (suspend () -> Ret<List<Layer<FltX>>>)? = null,
    private val filterByReducedCostWithResult: (suspend (ColumnGenerationState, List<Bpp2dLayerGenerationResult<FltX>>) -> Ret<List<Bpp2dLayerGenerationResult<FltX>>>)? = null,
    private val deduplicateColumns: (List<Layer<FltX>>) -> List<Layer<FltX>> = { it.distinct() },
    private val failureAnalyzer: ColumnGenerationFailureAnalyzer? = null
) {

    /**
     * 构造失败结果，并交由失败分析器处理。 / Build a failure result, delegating to the failure analyzer.
     *
     * @param T 结果类型 / result type
     * @param stage 失败阶段 / failure stage
     * @param state 失败时的状态 / state at failure
     * @param errors 原始错误 / original errors
     * @return 失败结果 / failure result
    */
    private suspend fun <T> failed(
        stage: ColumnGenerationFailureStage,
        state: ColumnGenerationState,
        errors: List<Error<ErrorCode>>
    ): Ret<T> {
        val analyzerResult = failureAnalyzer?.analyze(
            ColumnGenerationFailure(
                stage = stage,
                state = state,
                errors = errors
            )
        ) ?: ok
        return when (analyzerResult) {
            is Ok -> if (errors.size == 1) Failed(errors.first()) else Fatal(errors)
            is Failed -> Fatal(errors + analyzerResult.error)
            is Fatal -> Fatal(errors + analyzerResult.errors)
        }
    }

    /**
     * 执行列生成。 / Execute column generation.
     *
     * @param config 列生成配置 / column generation config
     * @return 列生成结果 / column generation result
    */
    suspend fun solve(
        config: ColumnGenerationConfig = ColumnGenerationConfig()
    ): Ret<ColumnGenerationResult> {
        val startedAt = TimeSource.Monotonic.markNow()
        val initialState = ColumnGenerationState(iteration = 0, columns = emptyList())

        var columns = when (val result = initialColumnsWithResult?.invoke() ?: Ok(emptyList())) {
            is Ok -> deduplicateColumns(result.value)
            is Failed -> return failed(
                stage = ColumnGenerationFailureStage.InitialColumns,
                state = initialState,
                errors = listOf(result.error)
            )

            is Fatal -> return failed(
                stage = ColumnGenerationFailureStage.InitialColumns,
                state = initialState,
                errors = result.errors
            )
        }

        var terminatedByIterationLimit = false
        var terminatedByTimeLimit = false
        var iterations = 0
        var lpSolvedTimes = 0
        var finalSolved = false
        var latestShadowPrices: Map<DemandModeKey, FltX> = emptyMap()
        var latestAdditionalShadowPrices: Map<String, FltX> = emptyMap()
        val lpObjectives = ArrayList<FltX?>()
        val lpInfos = ArrayList<Map<String, String>>()
        var finalObjective: FltX? = null
        var finalInfo: Map<String, String> = emptyMap()
        var sheets: List<Sheet<FltX>> = emptyList()

        while (iterations < config.iterationLimit) {
            if (config.timeLimit > ZERO && startedAt.elapsedNow() >= config.timeLimit) {
                terminatedByTimeLimit = true
                break
            }

            val rmpState = ColumnGenerationState(
                iteration = iterations,
                columns = columns,
                shadowPrices = latestShadowPrices,
                additionalShadowPrices = latestAdditionalShadowPrices
            )
            val solver = rmpSolver
            if (solver == null) {
                // 未配置 RMP 求解器时无法生成有意义的候选，直接结束迭代。
                // Without an RMP solver no meaningful pricing is possible, so the loop ends here.
                break
            }
            val lpResult = when (val result = solver.solve(rmpState)) {
                is Ok -> result.value
                is Failed -> return failed(
                    stage = ColumnGenerationFailureStage.RestrictedMasterProblem,
                    state = rmpState,
                    errors = listOf(result.error)
                )

                is Fatal -> return failed(
                    stage = ColumnGenerationFailureStage.RestrictedMasterProblem,
                    state = rmpState,
                    errors = result.errors
                )
            }
            latestShadowPrices = lpResult.shadowPrices
            latestAdditionalShadowPrices = lpResult.additionalShadowPrices
            lpObjectives.add(lpResult.objective)
            lpInfos.add(lpResult.info)
            lpSolvedTimes += 1

            val pricedState = ColumnGenerationState(
                iteration = iterations,
                columns = columns,
                shadowPrices = latestShadowPrices,
                additionalShadowPrices = latestAdditionalShadowPrices
            )
            val request = layerRequestBuilder?.build(pricedState, config)
                ?: Bpp2dLayerGenerationRequest(
                    iteration = iterations,
                    existingLayers = columns,
                    shadowPrices = latestShadowPrices,
                    maxCandidates = config.maxColumnsPerIteration
                )
            val candidates = layerGenerator.generate(request)
            val accepted = when (
                val result = filterByReducedCostWithResult?.invoke(pricedState, candidates)
                    ?: Ok(candidates)
            ) {
                is Ok -> result.value
                is Failed -> return failed(
                    stage = ColumnGenerationFailureStage.CandidateFilter,
                    state = pricedState,
                    errors = listOf(result.error)
                )

                is Fatal -> return failed(
                    stage = ColumnGenerationFailureStage.CandidateFilter,
                    state = pricedState,
                    errors = result.errors
                )
            }
            if (accepted.isEmpty()) {
                break
            }

            columns = deduplicateColumns(columns + accepted.map { it.layer })
            heartbeat?.invoke(
                ColumnGenerationState(
                    iteration = iterations,
                    columns = columns,
                    shadowPrices = latestShadowPrices,
                    additionalShadowPrices = latestAdditionalShadowPrices
                )
            )
            iterations += 1
        }

        if (iterations >= config.iterationLimit) {
            terminatedByIterationLimit = true
        }

        var finalState = ColumnGenerationState(
            iteration = iterations,
            columns = columns,
            shadowPrices = latestShadowPrices,
            additionalShadowPrices = latestAdditionalShadowPrices
        )
        if (config.finalMilpEnabled) {
            val solver = finalMilpSolver
            if (solver != null) {
                val finalResult = when (val result = solver.solve(finalState)) {
                    is Ok -> result.value
                    is Failed -> return failed(
                        stage = ColumnGenerationFailureStage.FinalMilp,
                        state = finalState,
                        errors = listOf(result.error)
                    )

                    is Fatal -> return failed(
                        stage = ColumnGenerationFailureStage.FinalMilp,
                        state = finalState,
                        errors = result.errors
                    )
                }
                columns = deduplicateColumns(finalResult.columns)
                sheets = finalResult.sheets
                finalObjective = finalResult.objective
                finalInfo = finalResult.info
                finalState = finalState.copy(columns = columns, sheets = sheets)
                finalSolved = true
            }
        }

        when (val result = solutionAnalyzer?.analyze(finalState) ?: ok) {
            is Ok -> {}
            is Failed -> return failed(
                stage = ColumnGenerationFailureStage.SolutionAnalysis,
                state = finalState,
                errors = listOf(result.error)
            )

            is Fatal -> return failed(
                stage = ColumnGenerationFailureStage.SolutionAnalysis,
                state = finalState,
                errors = result.errors
            )
        }

        return Ok(
            ColumnGenerationResult(
                columns = columns,
                sheets = sheets,
                iterationCount = iterations,
                terminatedByIterationLimit = terminatedByIterationLimit,
                terminatedByTimeLimit = terminatedByTimeLimit,
                lpSolvedTimes = lpSolvedTimes,
                finalSolved = finalSolved,
                lpObjectives = lpObjectives,
                finalObjective = finalObjective,
                elapsed = startedAt.elapsedNow(),
                lpInfos = lpInfos,
                finalInfo = finalInfo
            )
        )
    }
}