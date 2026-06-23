package fuookami.ospf.kotlin.framework.csp1d.application.service

import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.output.*
import fuookami.ospf.kotlin.core.solver.value.*
import fuookami.ospf.kotlin.framework.csp1d.domain.length_assignment.model.*
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.*
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.*
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.*
import fuookami.ospf.kotlin.framework.csp1d.domain.yield.model.*
import fuookami.ospf.kotlin.framework.solver.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

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
        val lpOutput: ColumnGenerationSolver.LPResult,
        val frameworkShadowPriceMap: AbstractCsp1dShadowPriceMap<AbstractCsp1dShadowPriceArguments>? = null
    )

    suspend fun <V : RealNumber<V>> solve(
        input: ProduceInput<V>,
        yieldConfig: YieldModelingConfig<V>? = null,
        wasteConfig: WasteMinimizationConfig<V>? = null,
        lengthConfig: LengthAssignmentModelingConfig<V>? = null,
        extensions: List<Csp1dModelingExtension<V>> = emptyList(),
        objectivePolicies: List<Csp1dObjectivePolicy<V>> = emptyList(),
        isFinalMilp: Boolean = false
    ): Ret<MilpResult<V>?> {
        return solveInternal(input, yieldConfig, wasteConfig, lengthConfig, extensions, objectivePolicies, isFinalMilp)
    }

    private suspend fun <V : RealNumber<V>> solveInternal(
        input: ProduceInput<V>,
        yieldConfig: YieldModelingConfig<V>?,
        wasteConfig: WasteMinimizationConfig<V>?,
        lengthConfig: LengthAssignmentModelingConfig<V>?,
        extensions: List<Csp1dModelingExtension<V>>,
        objectivePolicies: List<Csp1dObjectivePolicy<V>>,
        isFinalMilp: Boolean
    ): Ret<MilpResult<V>?> {
        if (input.cuttingPlans.isEmpty()) {
            return Ok(null)
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
                isFinalMilp(isFinalMilp)
                // 注入建模扩展（context-aware pipeline 在 build 时解析）
                // Inject modeling extensions (context-aware pipelines resolved at build time)
                for (ext in extensions) {
                    extension(ext)
                }
                // 注入目标策略 / Inject objective policies
                for (policy in objectivePolicies) {
                    objectivePolicy(policy)
                }
            }
            .build()

        when (val result = context.register(model)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        val output = when (val result = solver.solveMILP(
            name = "csp1d-produce",
            metaModel = model
        )) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        model.setSolution(output.solution)

        val produce = when (val result = context.extractSolution(model)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val yieldResult = context.extractYieldResult(model)
        val wasteResult = context.extractWasteResult(model)
        val lengthResult = context.extractLengthResult(model)

        return Ok(MilpResult(
            produce = produce,
            yieldResult = yieldResult,
            wasteResult = wasteResult,
            lengthResult = lengthResult,
            model = model,
            output = output
        ))
    }

    suspend fun <V : RealNumber<V>> solveLP(
        input: ProduceInput<V>,
        extensions: List<Csp1dModelingExtension<V>> = emptyList()
    ): Ret<LpResult<V>?> {
        return solveLPInternal(input, extensions)
    }

    private suspend fun <V : RealNumber<V>> solveLPInternal(
        input: ProduceInput<V>,
        extensions: List<Csp1dModelingExtension<V>>
    ): Ret<LpResult<V>?> {
        if (input.cuttingPlans.isEmpty()) {
            return Ok(null)
        }

        val model = LinearMetaModel(
            name = "csp1d_produce_lp",
            converter = IntoValue.Identity
        )

        // 通过 Csp1dProduceContext 注册 LP 模式建模逻辑 / Register LP mode modeling logic through Csp1dProduceContext
        val context = Csp1dProduceContextBuilder(input)
            .mode(Csp1dModelingMode.LP)
            .apply {
                // 注入建模扩展（context-aware pipeline 在 build 时解析）
                // Inject modeling extensions (context-aware pipelines resolved at build time)
                for (ext in extensions) {
                    extension(ext)
                }
            }
            .build()

        when (val result = context.register(model)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        val lpResult = when (val result = solver.solveLP(
            name = "csp1d-produce-lp",
            metaModel = model
        )) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        // 从 Csp1dProduceContext 获取 CG 管线列表，传入 lifecycle
        // Get CG pipeline list from Csp1dProduceContext, pass to lifecycle
        val domainValueSample = when (val result = resolveDomainValueSampleForLP(input)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val lifecycle = Csp1dShadowPriceLifecycle<V>(domainValueSample, context.cgPipelines)

        // 通过 lifecycle 统一提取影子价格（CGPipeline refresh / extractor）
        // Extract shadow prices through lifecycle (CGPipeline refresh / extractor)
        val shadowPrices = when (val result = lifecycle.extractFromDualSolution(model, lpResult.dualSolution)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        return Ok(LpResult(
            shadowPrices = shadowPrices,
            model = model,
            lpOutput = lpResult,
            frameworkShadowPriceMap = lifecycle.frameworkShadowPriceMap
        ))
    }

    /**
     * 从 ProduceInput 推导 V 样本值，用于影子价格 Flt64 → V 转换
     * Derive V sample value from ProduceInput for shadow price Flt64 → V conversion
     */
    private fun <V : RealNumber<V>> resolveDomainValueSampleForLP(input: ProduceInput<V>): Ret<V> {
        input.demands.firstOrNull()?.quantity?.value?.let { return Ok(it) }
        input.materials.firstOrNull()?.widthRange?.lowerBound?.value?.let { return Ok(it) }
        input.cuttingPlans.firstOrNull()?.restWidth?.value?.let { return Ok(it) }
        return Failed(
            ErrorCode.IllegalArgument,
            "Cannot derive V sample from ProduceInput for shadow price extraction"
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
}
