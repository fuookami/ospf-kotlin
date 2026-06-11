package fuookami.ospf.kotlin.framework.csp1d.application.service

import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.solver.output.FeasibleSolverOutput
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.framework.solver.ColumnGenerationSolver
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Csp1dShadowPriceKey
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ShadowPriceMap
import fuookami.ospf.kotlin.framework.csp1d.domain.length_assignment.model.LengthAssignmentModelingConfig
import fuookami.ospf.kotlin.framework.csp1d.domain.length_assignment.model.LengthAssignmentModelingResult
import fuookami.ospf.kotlin.framework.csp1d.domain.yield.model.YieldModelingConfig
import fuookami.ospf.kotlin.framework.csp1d.domain.yield.model.YieldModelingResult
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.ProduceInput
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.Produce
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.Csp1dModelingMode
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.Csp1dModelingExtension
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.Csp1dExtensionMode

/**
 * CSP1D MILP/LP 求解器 / CSP1D MILP/LP solver
 *
 * 通过 Csp1dProduceContext 注册建模逻辑，自身只负责模型创建、context 注册、求解调用和异常安全。
 *
 * Register modeling logic through Csp1dProduceContext; this solver is only responsible for
 * model creation, context registration, solve invocation, and exception safety.
 */
class Csp1dMilpSolver(
    private val solver: ColumnGenerationSolver
) {
    data class MilpResult<V : RealNumber<V>>(
        val produce: Produce<V>,
        val yieldResult: YieldModelingResult<V>? = null,
        val wasteResult: WasteMinimizationResult<V>? = null,
        val lengthResult: LengthAssignmentModelingResult<V>? = null,
        val model: LinearMetaModel<Flt64>,
        val output: FeasibleSolverOutput<Flt64>
    )

    data class LpResult<V : RealNumber<V>>(
        val shadowPrices: ShadowPriceMap<V>,
        val model: LinearMetaModel<Flt64>,
        val lpOutput: ColumnGenerationSolver.LPResult
    )

    suspend fun <V : RealNumber<V>> solve(
        input: ProduceInput<V>,
        yieldConfig: YieldModelingConfig<V>? = null,
        wasteConfig: WasteMinimizationConfig<V>? = null,
        lengthConfig: LengthAssignmentModelingConfig<V>? = null,
        extensions: List<Csp1dModelingExtension<V>> = emptyList(),
        isFinalMilp: Boolean = false
    ): MilpResult<V>? {
        return try {
            solveInternal(input, yieldConfig, wasteConfig, lengthConfig, extensions, isFinalMilp)
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun <V : RealNumber<V>> solveInternal(
        input: ProduceInput<V>,
        yieldConfig: YieldModelingConfig<V>?,
        wasteConfig: WasteMinimizationConfig<V>?,
        lengthConfig: LengthAssignmentModelingConfig<V>?,
        extensions: List<Csp1dModelingExtension<V>>,
        isFinalMilp: Boolean
    ): MilpResult<V>? {
        if (input.cuttingPlans.isEmpty()) {
            return null
        }

        val model = LinearMetaModel(
            name = "csp1d_produce",
            converter = IntoValue.Identity
        )

        // 解决缺省长度边界 / Resolve default length bounds
        val resolvedLengthConfig = resolveDefaultLengthBounds(input, lengthConfig)

        // 通过 Csp1dProduceContext 注册建模逻辑 / Register modeling logic through Csp1dProduceContext
        val context = Csp1dProduceContextBuilder(input)
            .apply {
                yieldConfig?.let { yieldConfig(it) }
                wasteConfig?.let { wasteConfig(it) }
                resolvedLengthConfig?.let { lengthConfig(it) }
                mode(Csp1dModelingMode.MILP)
                // 注入适用于 MILP / FINAL_MILP / ALL 模式的扩展管线
                // Inject extensions applicable to MILP / FINAL_MILP / ALL modes
                for (ext in extensions) {
                    if (ext.mode.matches(Csp1dModelingMode.MILP, isFinalMilp)) {
                        extraPipeline(ext.pipeline)
                    }
                }
            }
            .build()

        when (val result = context.register(model)) {
            is Ok -> {}
            is fuookami.ospf.kotlin.utils.functional.Failed -> throw IllegalStateException("register context failed: ${result.error}")
            is fuookami.ospf.kotlin.utils.functional.Fatal -> throw IllegalStateException("register context fatal: ${result.errors}")
        }

        val output = ensureRet(
            result = solver.solveMILP(
                name = "csp1d-produce",
                metaModel = model
            ),
            stage = "solve CSP1D produce MILP"
        )
        model.setSolution(output.solution)

        val produce = when (val result = context.extractSolution(model)) {
            is Ok -> result.value
            is fuookami.ospf.kotlin.utils.functional.Failed -> throw IllegalStateException("extract solution failed: ${result.error}")
            is fuookami.ospf.kotlin.utils.functional.Fatal -> throw IllegalStateException("extract solution fatal: ${result.errors}")
        }
        val yieldResult = context.extractYieldResult(model)
        val wasteResult = context.extractWasteResult(model)
        val lengthResult = context.extractLengthResult(model)

        return MilpResult(
            produce = produce,
            yieldResult = yieldResult,
            wasteResult = wasteResult,
            lengthResult = lengthResult,
            model = model,
            output = output
        )
    }

    suspend fun <V : RealNumber<V>> solveLP(
        input: ProduceInput<V>,
        extensions: List<Csp1dModelingExtension<V>> = emptyList()
    ): LpResult<V>? {
        return try {
            solveLPInternal(input, extensions)
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun <V : RealNumber<V>> solveLPInternal(
        input: ProduceInput<V>,
        extensions: List<Csp1dModelingExtension<V>>
    ): LpResult<V>? {
        if (input.cuttingPlans.isEmpty()) {
            return null
        }

        val model = LinearMetaModel(
            name = "csp1d_produce_lp",
            converter = IntoValue.Identity
        )
        val shadowPriceKeys = java.util.LinkedHashMap<String, Csp1dShadowPriceKey>()

        // 通过 Csp1dProduceContext 注册 LP 模式建模逻辑 / Register LP mode modeling logic through Csp1dProduceContext
        val context = Csp1dProduceContextBuilder(input)
            .shadowPriceKeys(shadowPriceKeys)
            .mode(Csp1dModelingMode.LP)
            .apply {
                // 注入适用于 LP / ALL 模式的扩展管线
                // Inject extensions applicable to LP / ALL modes
                for (ext in extensions) {
                    if (ext.mode.matches(Csp1dModelingMode.LP)) {
                        extraPipeline(ext.pipeline)
                    }
                }
            }
            .build()

        when (val result = context.register(model)) {
            is Ok -> {}
            is fuookami.ospf.kotlin.utils.functional.Failed -> throw IllegalStateException("register context failed: ${result.error}")
            is fuookami.ospf.kotlin.utils.functional.Fatal -> throw IllegalStateException("register context fatal: ${result.errors}")
        }

        val lpResult = ensureRet(
            result = solver.solveLP(
                name = "csp1d-produce-lp",
                metaModel = model
            ),
            stage = "solve CSP1D produce LP"
        )

        val shadowPrices = context.extractShadowPriceMap(
            dualSolution = lpResult.dualSolution,
            shadowPriceKeys = shadowPriceKeys
        )
        return LpResult(
            shadowPrices = shadowPrices,
            model = model,
            lpOutput = lpResult
        )
    }

    /**
     * 为缺省的 assignedLengthLowerBound/UpperBound 提供默认推导 / Provide default derivation for missing assignedLengthLowerBound/UpperBound
     */
    private fun <V : RealNumber<V>> resolveDefaultLengthBounds(
        input: ProduceInput<V>,
        lengthConfig: LengthAssignmentModelingConfig<V>?
    ): LengthAssignmentModelingConfig<V>? {
        if (lengthConfig == null) return null
        if (lengthConfig.dynamicProductIds.isEmpty()) return lengthConfig

        val needsDerivation = lengthConfig.dynamicProductIds.any { pid ->
            pid !in lengthConfig.assignedLengthLowerBound.keys ||
            pid !in lengthConfig.assignedLengthUpperBound.keys
        }
        if (!needsDerivation) return lengthConfig

        val derivedLowerBounds = lengthConfig.assignedLengthLowerBound.toMutableMap()
        val derivedUpperBounds = lengthConfig.assignedLengthUpperBound.toMutableMap()

        for (productId in lengthConfig.dynamicProductIds) {
            val demand = input.demands.find { it.product.id == productId }
            val contribution = input.cuttingPlans
                .firstOrNull { plan -> plan.demandContributions.any { it.product.id == productId } }
                ?.demandContributions?.firstOrNull { it.product.id == productId }
            val product = demand?.product ?: contribution?.product
            val zero = demand?.quantity?.value?.constants?.zero
                ?: contribution?.quantity?.value?.constants?.zero
                ?: lengthConfig.overLengthPenalty.values.firstOrNull()?.constants?.zero
                ?: lengthConfig.totalLengthPenalty?.constants?.zero
                ?: lengthConfig.batchMinPenalty?.constants?.zero

            if (productId !in derivedLowerBounds && zero != null) {
                derivedLowerBounds[productId] = zero
            }

            if (productId !in derivedUpperBounds) {
                val maxOverLength = product?.maxOverProduceLength
                if (maxOverLength != null) {
                    derivedUpperBounds[productId] = maxOverLength.value
                }
            }
        }

        return lengthConfig.copy(
            assignedLengthLowerBound = derivedLowerBounds,
            assignedLengthUpperBound = derivedUpperBounds
        )
    }

    private fun <T> ensureRet(result: Ret<T>, stage: String): T {
        return when (result) {
            is Ok -> result.value
            is fuookami.ospf.kotlin.utils.functional.Failed -> throw IllegalStateException("$stage failed: ${result.error}")
            is fuookami.ospf.kotlin.utils.functional.Fatal -> throw IllegalStateException("$stage fatal: ${result.errors}")
        }
    }
}
