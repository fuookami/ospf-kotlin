package fuookami.ospf.kotlin.framework.csp1d.application.service

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.MetaDualSolution
import fuookami.ospf.kotlin.core.model.mechanism.Constraint
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.canonicalKey
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.CuttingPlanCanonicalKey
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlan
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Machine
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Material
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ProductDemand
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Csp1dShadowPriceKey
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ShadowPriceMap
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.ProduceAggregation
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.ProduceInput
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.Csp1dIterativeContext
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.Csp1dModelingContext
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.Csp1dModelingExtension
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.Csp1dModelingMode
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.Csp1dPipelineList
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.Csp1dObjectivePolicy
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.SimpleDomainCalculationContext
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.CuttingPlanUsage
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.MaterialUsage
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.MachineCapacityUsage
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.Produce
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.service.pipeline.DemandConstraintPipeline
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.service.pipeline.MaterialConstraintPipeline
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.service.pipeline.MachineConstraintPipeline
import fuookami.ospf.kotlin.framework.csp1d.domain.yield.YieldAggregation
import fuookami.ospf.kotlin.framework.csp1d.domain.yield.model.YieldModelingConfig
import fuookami.ospf.kotlin.framework.csp1d.domain.yield.model.YieldModelingResult
import fuookami.ospf.kotlin.framework.csp1d.domain.yield.service.pipeline.YieldConstraintPipeline
import fuookami.ospf.kotlin.framework.csp1d.domain.yield.service.pipeline.YieldObjectivePipeline
import fuookami.ospf.kotlin.framework.csp1d.domain.wasting_minimization.WasteAggregation
import fuookami.ospf.kotlin.framework.csp1d.domain.wasting_minimization.service.pipeline.WasteObjectivePipeline
import fuookami.ospf.kotlin.framework.csp1d.domain.wasting_minimization.service.pipeline.OverProductionAreaMeasure as DomainOverProductionAreaMeasure
import fuookami.ospf.kotlin.framework.csp1d.domain.wasting_minimization.service.pipeline.RestMaterialMeasure as DomainRestMaterialMeasure
import fuookami.ospf.kotlin.framework.csp1d.domain.length_assignment.LengthAggregation
import fuookami.ospf.kotlin.framework.csp1d.domain.length_assignment.model.LengthAssignmentModelingConfig
import fuookami.ospf.kotlin.framework.csp1d.domain.length_assignment.model.LengthAssignmentModelingResult
import fuookami.ospf.kotlin.framework.csp1d.domain.length_assignment.service.pipeline.LengthConstraintPipeline
import fuookami.ospf.kotlin.framework.csp1d.domain.length_assignment.service.pipeline.LengthObjectivePipeline
import fuookami.ospf.kotlin.framework.model.Pipeline
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import kotlin.math.roundToLong

/**
 * CSP1D 产出模型上下文 / CSP1D produce model context
 *
 * 组合所有 Aggregation 和 Pipeline，实现 Csp1dIterativeContext 接口。
 * 这是 CSP1D 建模注册模式的默认实现，同时支持 MILP 和 LP 两种模式。
 *
 * @param V 数值类型 / Numeric value type
 */
class Csp1dProduceContext<V : RealNumber<V>>(
    override val produce: ProduceAggregation<V>,
    val yield: YieldAggregation<V>?,
    val waste: WasteAggregation<V>?,
    val length: LengthAggregation<V>?,
    val constraintPipelines: Csp1dPipelineList,
    private val yieldObjective: YieldObjectivePipeline<V>?,
    private val wasteObjective: WasteObjectivePipeline<V>?,
    private val lengthObjective: LengthObjectivePipeline<V>?,
    private val extraPipelines: Csp1dPipelineList = emptyList(),
    override val mode: Csp1dModelingMode = Csp1dModelingMode.MILP,
    private val shadowPriceKeys: MutableMap<String, Csp1dShadowPriceKey>? = null,
    private val warmStartPlanUsages: List<CuttingPlanUsage<V>> = emptyList(),
    private val wasteOverProductionAreaMeasure: OverProductionAreaMeasure = OverProductionAreaMeasure.ProductMaxWidthProxy,
    private val wasteRestMaterialMeasure: RestMaterialMeasure = RestMaterialMeasure.RestWidthByMaterialLengthProxy,
    private val objectivePolicies: List<Csp1dObjectivePolicy<V>> = emptyList(),
    override val vSample: V,
    override val isFinalMilp: Boolean = false
) : Csp1dIterativeContext<V>, Csp1dModelingContext<V> {

    override val demands: List<ProductDemand<V>> get() = produce.demands
    override val materials: List<Material<V>> get() = produce.materials
    override val machines: List<Machine<V>> get() = produce.machines

    override fun flt64ToV(value: Flt64): V = solverValueLike(vSample, value)

    override fun register(model: LinearMetaModel<Flt64>): Try {
        // 1. 注册变量
        when (val result = produce.register(model)) {
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
            is Ok -> {}
        }

        // LP 模式不加 yield/length slack 变量
        if (mode == Csp1dModelingMode.MILP) {
            yield?.let { agg ->
                when (val result = agg.register(model)) {
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                    is Ok -> {}
                }
            }
            length?.let { agg ->
                when (val result = agg.register(model)) {
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                    is Ok -> {}
                }
            }
        }

        // 2. 注册约束管线
        for (pipeline in constraintPipelines) {
            pipeline.register(model)
            when (val result = pipeline(model)) {
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
                is Ok -> {}
            }
        }

        // 3. 注册扩展约束管线
        for (pipeline in extraPipelines) {
            pipeline.register(model)
            when (val result = pipeline(model)) {
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
                is Ok -> {}
            }
        }

        // 4. 组装目标函数
        setObjective(model)

        // 5. 应用 warm start 初始解（仅 MILP 模式）
        if (mode == Csp1dModelingMode.MILP && warmStartPlanUsages.isNotEmpty()) {
            applyWarmStart(model)
        }

        return ok
    }

    private fun setObjective(model: LinearMetaModel<Flt64>) {
        val monomials = ArrayList<LinearMonomial<Flt64>>()

        // 基础目标: 最小化批次 / Base objective: minimize batches
        // 允许 objective policy 修正每个方案的 batch coefficient
        // Allow objective policy to modify batch coefficient for each plan
        val baseBatchCoefficient = lengthObjective?.batchCoefficient() ?: Flt64.one
        for (index in 0 until produce.planCount) {
            val plan = produce.cuttingPlans[index]
            val batchCoefficient = if (objectivePolicies.isNotEmpty()) {
                val ctx = SimpleDomainCalculationContext(
                    plan = plan,
                    planIndex = index,
                    vSample = vSample
                )
                objectivePolicies.fold(baseBatchCoefficient) { coeff, policy ->
                    policy.modifyBatchCoefficient(ctx, coeff)
                }
            } else {
                baseBatchCoefficient
            }
            monomials.add(LinearMonomial(batchCoefficient, produce[index]))
        }

        // LP 模式不加 yield/waste/length 目标项
        if (mode == Csp1dModelingMode.MILP) {
            monomials.addAll(yieldObjective?.objectiveMonomials() ?: emptyList())
            monomials.addAll(wasteObjective?.objectiveMonomials() ?: emptyList())
            monomials.addAll(lengthObjective?.objectiveMonomials() ?: emptyList())
        }

        val objective = LinearPolynomial(monomials, Flt64.zero)
        model.minimize(polynomial = objective, name = "csp1d_objective")
    }

    /**
     * 应用 warm start 初始解 / Apply warm start initial solution
     */
    private fun applyWarmStart(model: LinearMetaModel<Flt64>) {
        val usageByKey = warmStartUsageByKey()
        if (usageByKey.isEmpty()) return

        val initialSolution = LinkedHashMap<AbstractVariableItem<*, *>, Flt64>()
        for ((index, plan) in produce.cuttingPlans.withIndex()) {
            val usage = usageByKey[plan.canonicalKey()] ?: continue
            if (usage > UInt64.zero) {
                initialSolution[produce[index]] = usage.toFlt64()
            }
        }
        if (initialSolution.isNotEmpty()) {
            model.setSolution(initialSolution)
        }
    }

    private fun warmStartUsageByKey(): Map<CuttingPlanCanonicalKey, UInt64> {
        val result = LinkedHashMap<CuttingPlanCanonicalKey, UInt64>()
        for (usage in warmStartPlanUsages) {
            if (usage.amount <= UInt64.zero) continue
            val key = usage.plan.canonicalKey()
            result[key] = (result[key] ?: UInt64.zero) + usage.amount
        }
        return result
    }

    override fun extractSolution(model: AbstractLinearMetaModel<Flt64>): Ret<Produce<V>> {
        val selectedPlans = ArrayList<CuttingPlanUsage<V>>()
        val materialUsageMap = LinkedHashMap<String, UInt64>()

        for ((index, plan) in produce.cuttingPlans.withIndex()) {
            val doubleValue = model.tokens.find(produce[index])?.doubleResult ?: continue
            if (doubleValue <= 0.0) continue
            val amount = UInt64(doubleValue.roundToLong().coerceAtLeast(0).toULong())
            selectedPlans.add(CuttingPlanUsage(plan, amount))

            val currentMaterialUsage = materialUsageMap[plan.material.id] ?: UInt64.ZERO
            materialUsageMap[plan.material.id] = currentMaterialUsage + amount
        }

        val materialUsages = produce.materials.mapNotNull { material ->
            val amount = materialUsageMap[material.id] ?: return@mapNotNull null
            MaterialUsage(material, amount)
        }

        val machineUsages = extractMachineUsages(selectedPlans)

        val unmetDemands = produce.demands.filter { demand ->
            val supplied = selectedPlans.fold(Flt64.zero) { acc, usage ->
                val contribution = usage.plan.demandContributions.find {
                    it.product.id == demand.product.id && it.quantity.unit == demand.quantity.unit
                } ?: return@fold acc
                acc + contribution.quantity.value.toFlt64() * usage.amount.toFlt64()
            }
            supplied < demand.quantity.value.toFlt64()
        }

        return Ok(Produce(selectedPlans, materialUsages, machineUsages, unmetDemands))
    }

    /**
     * 提取设备使用量 / Extract machine usages
     */
    private fun extractMachineUsages(
        selectedPlans: List<CuttingPlanUsage<V>>
    ): List<MachineCapacityUsage<V>> {
        return produce.machines.mapNotNull { machine ->
            val machineCapacity = machine.capacity
            val used = selectedPlans
                .filter { usage -> usage.plan.machineId == machine.id }
                .fold(null as Quantity<V>?) { acc, usage ->
                    val consumption = usage.plan.capacityConsumption ?: return@fold acc
                    if (machineCapacity != null && consumption.unit != machineCapacity.unit) {
                        return@fold acc
                    }
                    if (acc != null && acc.unit != consumption.unit) {
                        return@fold acc
                    }
                    val contribution = Quantity(
                        consumption.value * solverValueLike(
                            sample = consumption.value,
                            value = usage.amount.toFlt64()
                        ),
                        consumption.unit
                    )
                    if (acc == null) contribution else Quantity(acc.value + contribution.value, acc.unit)
                }
            if (used != null) {
                MachineCapacityUsage(machine = machine, used = used)
            } else {
                null
            }
        }
    }

    /**
     * 提取 yield 建模结果 / Extract yield modeling result
     */
    fun extractYieldResult(model: AbstractLinearMetaModel<Flt64>): YieldModelingResult<V>? {
        return yield?.extractResult(model)
    }

    /**
     * 提取 length 建模结果 / Extract length modeling result
     */
    fun extractLengthResult(model: AbstractLinearMetaModel<Flt64>): LengthAssignmentModelingResult<V>? {
        return length?.extractResult(model)
    }

    /**
     * 提取 waste 建模结果 / Extract waste modeling result
     */
    fun extractWasteResult(model: AbstractLinearMetaModel<Flt64>): WasteMinimizationResult<V>? {
        val wasteAgg = waste ?: return null
        if (!wasteAgg.hasAnyPenalty) return null

        // 计算总余宽 / Calculate total trim width
        var totalTrimWidth: V? = null
        if (wasteAgg.trimWidthPenalty != null) {
            var sum: V? = null
            for ((index, plan) in produce.cuttingPlans.withIndex()) {
                val batchCount = solutionAmount(model, index)
                if (batchCount > UInt64.zero) {
                    val restWidthValue = plan.restWidth?.value ?: continue
                    val contribution = restWidthValue * solverValueLike(restWidthValue, batchCount.toFlt64())
                    sum = if (sum != null) sum + contribution else contribution
                }
            }
            totalTrimWidth = sum
        }

        // 计算总余料面积代理 / Calculate total rest material area proxy
        var totalRestMaterial: V? = null
        if (wasteAgg.restMaterialPenalty != null) {
            var sum: V? = null
            for ((index, plan) in produce.cuttingPlans.withIndex()) {
                val batchCount = solutionAmount(model, index)
                if (batchCount > UInt64.zero) {
                    val restMaterialValue = restMaterialValue(plan, when (wasteRestMaterialMeasure) {
                        RestMaterialMeasure.RestWidthByMaterialLengthProxy -> DomainRestMaterialMeasure.RestWidthByMaterialLengthProxy
                    }) ?: continue
                    val contribution = restMaterialValue * solverValueLike(restMaterialValue, batchCount.toFlt64())
                    sum = if (sum != null) sum + contribution else contribution
                }
            }
            totalRestMaterial = sum
        }

        // 计算物料成本 / Calculate material costs
        val materialCosts = ArrayList<ModeledMaterialCost<V>>()
        if (wasteAgg.materialCostPenalty.isNotEmpty()) {
            val costByMaterial = HashMap<String, V>()
            for ((index, plan) in produce.cuttingPlans.withIndex()) {
                val batchCount = solutionAmount(model, index)
                if (batchCount > UInt64.zero) {
                    val costPenalty = wasteAgg.materialCostPenalty[plan.material.id] ?: continue
                    val cost = costPenalty * solverValueLike(costPenalty, batchCount.toFlt64())
                    val existing = costByMaterial[plan.material.id]
                    costByMaterial[plan.material.id] = if (existing != null) existing + cost else cost
                }
            }
            for ((materialId, cost) in costByMaterial) {
                materialCosts.add(ModeledMaterialCost(materialId, cost))
            }
        }

        // 计算超产面积代理 / Calculate over-production area proxy
        var overProductionArea: V? = null
        val overAreaPenalty = wasteAgg.overProductionAreaPenalty
        if (overAreaPenalty != null && yield != null) {
            var areaSum: V? = null
            for ((demandIndex, demand) in produce.demands.withIndex()) {
                val overVar = yield.overProduction.getOrNull(demandIndex) ?: continue
                val overDouble = model.tokens.find(overVar)?.doubleResult ?: continue
                if (overDouble > 0.0) {
                    val productWidthValue = overProductionAreaWidthValue(
                        demand = demand,
                        measure = when (wasteOverProductionAreaMeasure) {
                            OverProductionAreaMeasure.ProductMaxWidthProxy -> DomainOverProductionAreaMeasure.ProductMaxWidthProxy
                        }
                    ) ?: continue
                    val area = productWidthValue * solverValueLike(productWidthValue, Flt64(overDouble))
                    areaSum = if (areaSum != null) areaSum + area else area
                }
            }
            overProductionArea = areaSum
        }

        return WasteMinimizationResult(
            totalTrimWidth = totalTrimWidth,
            totalRestMaterial = totalRestMaterial,
            materialCosts = materialCosts,
            overProductionArea = overProductionArea,
            overProductionAreaMeasure = wasteOverProductionAreaMeasure,
            restMaterialMeasure = wasteRestMaterialMeasure
        )
    }

    // ===== Csp1dIterativeContext 实现 =====

    override suspend fun addColumns(
        iteration: UInt64,
        newPlans: List<CuttingPlan<V>>,
        model: AbstractLinearMetaModel<Flt64>
    ): Ret<List<CuttingPlan<V>>> {
        // 去重 / Deduplicate
        val existingIds = produce.cuttingPlans.map { it.id }.toSet()
        val existingKeys = produce.cuttingPlans.map { it.canonicalKey() }.toSet()
        val addedPlans = newPlans.filter { candidate ->
            candidate.id !in existingIds && candidate.canonicalKey() !in existingKeys
        }
        if (addedPlans.isEmpty()) return Ok(emptyList())

        // Rebuild-compatible lifecycle:
        //
        // 当前 AbstractLinearMetaModel 不支持原地追加变量并刷新约束表达式，
        // 因此 addColumns 仅返回去重后的新方案列表，不修改已有模型。
        // 列生成主循环应使用 rebuild 模式：将新增方案加入 plan pool 后，
        // 下一轮迭代通过 Csp1dMilpSolver.solveLP() 重新构建完整模型。
        //
        // 后续若底层变量容器支持原地扩展，可在此方法中：
        // 1. 为每个新增方案注册 plan usage 变量 x[j]
        // 2. 更新需求、物料、设备约束表达式中的新列系数
        // 3. 更新目标函数中的新列系数
        //
        // Current AbstractLinearMetaModel does not support in-place variable addition
        // with constraint expression refresh, so addColumns only returns deduplicated
        // new plans without modifying the existing model.
        // The CG main loop should use rebuild mode: add new plans to the pool,
        // then the next iteration rebuilds the full model via Csp1dMilpSolver.solveLP().
        //
        // When the underlying variable container supports in-place extension, this method
        // should: 1) register plan usage variables for each new plan,
        // 2) update demand/material/machine constraint expressions with new column coefficients,
        // 3) update objective function with new column coefficients.

        return Ok(addedPlans)
    }

    override fun extractShadowPrice(
        model: AbstractLinearMetaModel<Flt64>,
        shadowPrices: MetaDualSolution,
        shadowPriceKeys: MutableMap<String, Csp1dShadowPriceKey>
    ): Try {
        // MetaDualSolution 格式适配：从 constraints 映射提取 shadow price
        // MathConstraint 的具体子类有 name 属性
        // LP 对偶值是 Flt64，需要通过 solverValueLike 显式转换为 V
        // LP dual values are Flt64; explicit conversion to V via solverValueLike is required
        val prices = HashMap<Csp1dShadowPriceKey, V>()
        for ((constraint, dualValue) in shadowPrices.constraints) {
            val constraintName = when (constraint) {
                is fuookami.ospf.kotlin.core.model.mechanism.LinearInequalityConstraint<*> -> constraint.name
                is fuookami.ospf.kotlin.core.model.mechanism.QuadraticInequalityConstraint<*> -> constraint.name
                else -> continue
            }
            val key = shadowPriceKeys[constraintName] ?: continue
            val vDual = solverValueLike(vSample, dualValue)
            val existingValue = prices[key]
            prices[key] = if (existingValue != null) existingValue + vDual else vDual
        }
        return ok
    }

    /**
     * 从 LP 对偶解提取影子价格映射 / Extract shadow price map from LP dual solution
     */
    fun extractShadowPriceMap(
        dualSolution: Map<Constraint<Flt64, Linear>, Flt64>,
        shadowPriceKeys: Map<String, Csp1dShadowPriceKey>
    ): ShadowPriceMap<V> {
        // LP 对偶值是 Flt64，需要通过 solverValueLike 显式转换为 V
        // LP dual values are Flt64; explicit conversion to V via solverValueLike is required
        val shadowPrices = HashMap<Csp1dShadowPriceKey, V>()
        for ((constraint, dualValue) in dualSolution) {
            val key = shadowPriceKeys[constraint.name] ?: continue
            val vDual = solverValueLike(vSample, dualValue)
            val existingValue = shadowPrices[key]
            shadowPrices[key] = if (existingValue != null) existingValue + vDual else vDual
        }
        return ShadowPriceMap(shadowPrices)
    }

    // ===== 辅助方法 =====

    private fun solutionAmount(
        model: AbstractLinearMetaModel<Flt64>,
        index: Int
    ): UInt64 {
        val value = model.tokens.find(produce[index])?.doubleResult ?: return UInt64.zero
        if (value <= 0.0) return UInt64.zero
        return UInt64(value.roundToLong().coerceAtLeast(0).toULong())
    }

    private fun restMaterialValue(
        plan: CuttingPlan<V>,
        measure: DomainRestMaterialMeasure
    ): V? {
        val restWidth = plan.restWidth?.value ?: return null
        return when (measure) {
            DomainRestMaterialMeasure.RestWidthByMaterialLengthProxy -> {
                val materialLength = plan.material.length?.value ?: return null
                if (restWidth <= restWidth.constants.zero || materialLength <= materialLength.constants.zero) return null
                restWidth * materialLength
            }
        }
    }

    private fun overProductionAreaWidthValue(
        demand: ProductDemand<V>,
        measure: DomainOverProductionAreaMeasure
    ): V? {
        return when (measure) {
            DomainOverProductionAreaMeasure.ProductMaxWidthProxy -> demand.product.maxWidth()?.value
        }
    }

    companion object {
        private val UInt64.Companion.ZERO: UInt64 get() = UInt64(0UL)
    }
}

@Suppress("UNCHECKED_CAST")
private fun <V : RealNumber<V>> solverValueLike(sample: V, value: Flt64): V {
    return when (sample) {
        is Flt64 -> value as V
        is FltX -> value.toFltX() as V
        else -> throw IllegalArgumentException("Unsupported RealNumber type: ${sample::class}")
    }
}

/**
 * Csp1dProduceContext 构建器 / Csp1dProduceContext builder
 */
class Csp1dProduceContextBuilder<V : RealNumber<V>>(
    private val input: ProduceInput<V>
) {
    private var _yieldConfig: YieldModelingConfig<V>? = null
    private var _wasteConfig: WasteMinimizationConfig<V>? = null
    private var _lengthConfig: LengthAssignmentModelingConfig<V>? = null
    private var _shadowPriceKeys: MutableMap<String, Csp1dShadowPriceKey>? = null
    private var _mode: Csp1dModelingMode = Csp1dModelingMode.MILP
    private var _isFinalMilp: Boolean = false
    private val _extraPipelines = ArrayList<Pipeline<LinearMetaModel<Flt64>>>()
    private val _objectivePolicies = ArrayList<Csp1dObjectivePolicy<V>>()
    private val _extensions = ArrayList<Csp1dModelingExtension<V>>()

    fun yieldConfig(config: YieldModelingConfig<V>): Csp1dProduceContextBuilder<V> {
        this._yieldConfig = config
        return this
    }

    fun wasteConfig(config: WasteMinimizationConfig<V>): Csp1dProduceContextBuilder<V> {
        this._wasteConfig = config
        return this
    }

    fun lengthConfig(config: LengthAssignmentModelingConfig<V>): Csp1dProduceContextBuilder<V> {
        this._lengthConfig = config
        return this
    }

    fun shadowPriceKeys(keys: MutableMap<String, Csp1dShadowPriceKey>): Csp1dProduceContextBuilder<V> {
        this._shadowPriceKeys = keys
        return this
    }

    fun mode(mode: Csp1dModelingMode): Csp1dProduceContextBuilder<V> {
        this._mode = mode
        return this
    }

    fun isFinalMilp(isFinalMilp: Boolean): Csp1dProduceContextBuilder<V> {
        this._isFinalMilp = isFinalMilp
        return this
    }

    fun extraPipeline(pipeline: Pipeline<LinearMetaModel<Flt64>>): Csp1dProduceContextBuilder<V> {
        _extraPipelines.add(pipeline)
        return this
    }

    fun objectivePolicy(policy: Csp1dObjectivePolicy<V>): Csp1dProduceContextBuilder<V> {
        _objectivePolicies.add(policy)
        return this
    }

    /**
     * 追加建模扩展，build 时自动解析 context-aware pipeline / Add a modeling extension
     *
     * @param extension 建模扩展 / Modeling extension
     */
    fun extension(extension: Csp1dModelingExtension<V>): Csp1dProduceContextBuilder<V> {
        _extensions.add(extension)
        return this
    }

    fun build(): Csp1dProduceContext<V> {
        val yieldCfg = _yieldConfig
        val wasteCfg = _wasteConfig
        val lengthCfg = _lengthConfig

        val produce = ProduceAggregation(
            cuttingPlans = input.cuttingPlans,
            demands = input.demands,
            materials = input.materials,
            machines = input.machines
        )

        // 为 context-aware 扩展构建只读建模上下文
        // Build read-only modeling context for context-aware extension resolution
        val vSample = resolveVSample(input)
        val modelingContext = object : Csp1dModelingContext<V> {
            override val mode = _mode
            override val isFinalMilp = _isFinalMilp
            override val produce = produce
            override val demands: List<ProductDemand<V>> get() = produce.demands
            override val materials: List<Material<V>> get() = produce.materials
            override val machines: List<Machine<V>> get() = produce.machines
            override val vSample = vSample
            override fun flt64ToV(value: Flt64): V = solverValueLike(vSample, value)
        }

        // 解析 context-aware 扩展管线
        // Resolve context-aware extension pipelines
        for (ext in _extensions) {
            if (ext.mode.matches(_mode, _isFinalMilp)) {
                _extraPipelines.add(ext.resolvePipeline(modelingContext))
            }
        }

        // LP 模式不加 yield/length slack
        val yieldAgg = if (_mode == Csp1dModelingMode.MILP) {
            yieldCfg?.let { cfg ->
                YieldAggregation(
                    config = cfg,
                    demands = input.demands,
                    needsOverSlackForOverArea = wasteCfg?.overProductionAreaPenalty != null
                )
            }
        } else {
            null
        }

        val wasteAgg = if (_mode == Csp1dModelingMode.MILP) {
            wasteCfg?.let { cfg ->
                WasteAggregation(
                    cuttingPlans = input.cuttingPlans,
                    trimWidthPenalty = cfg.trimWidthPenalty,
                    materialCostPenalty = cfg.materialCostPenalty,
                    overProductionAreaPenalty = cfg.overProductionAreaPenalty,
                    restMaterialPenalty = cfg.restMaterialPenalty
                )
            }
        } else {
            null
        }

        val lengthAgg = if (_mode == Csp1dModelingMode.MILP) {
            lengthCfg?.let { cfg ->
                LengthAggregation(config = cfg, demands = input.demands)
            }
        } else {
            null
        }

        val constraintPipelines = mutableListOf<Pipeline<LinearMetaModel<Flt64>>>()
        constraintPipelines.add(DemandConstraintPipeline(
            produce = produce,
            demands = input.demands,
            yieldUnderVars = yieldAgg?.underProduction ?: emptyList(),
            yieldOverVars = yieldAgg?.overProduction ?: emptyList(),
            shadowPriceKeys = _shadowPriceKeys
        ))
        constraintPipelines.add(MaterialConstraintPipeline(
            produce = produce,
            materials = input.materials,
            shadowPriceKeys = _shadowPriceKeys
        ))
        constraintPipelines.add(MachineConstraintPipeline(
            produce = produce,
            machines = input.machines,
            shadowPriceKeys = _shadowPriceKeys
        ))
        if (yieldAgg != null && yieldCfg != null) {
            constraintPipelines.add(YieldConstraintPipeline(
                yield = yieldAgg,
                config = yieldCfg,
                demands = input.demands
            ))
        }
        if (lengthAgg != null && lengthCfg != null) {
            constraintPipelines.add(LengthConstraintPipeline(
                length = lengthAgg,
                config = lengthCfg,
                demands = input.demands
            ))
        }

        val yieldObjective = yieldAgg?.let { agg ->
            yieldCfg?.let { cfg ->
                YieldObjectivePipeline(yield = agg, config = cfg, demands = input.demands)
            }
        }
        val wasteObjective = wasteAgg?.let { agg ->
            val domainOverAreaMeasure = when (wasteCfg?.overProductionAreaMeasure) {
                OverProductionAreaMeasure.ProductMaxWidthProxy -> DomainOverProductionAreaMeasure.ProductMaxWidthProxy
                null -> DomainOverProductionAreaMeasure.ProductMaxWidthProxy
            }
            val domainRestMaterialMeasure = when (wasteCfg?.restMaterialMeasure) {
                RestMaterialMeasure.RestWidthByMaterialLengthProxy -> DomainRestMaterialMeasure.RestWidthByMaterialLengthProxy
                null -> DomainRestMaterialMeasure.RestWidthByMaterialLengthProxy
            }
            WasteObjectivePipeline(
                produce = produce,
                waste = agg,
                demands = input.demands,
                overProductionVars = yieldAgg?.overProduction ?: emptyList(),
                overProductionAreaMeasure = domainOverAreaMeasure,
                restMaterialMeasure = domainRestMaterialMeasure
            )
        }
        val lengthObjective = lengthAgg?.let { agg ->
            lengthCfg?.let { cfg ->
                LengthObjectivePipeline(produce = produce, length = agg, config = cfg)
            }
        }

        return Csp1dProduceContext(
            produce = produce,
            yield = yieldAgg,
            waste = wasteAgg,
            length = lengthAgg,
            constraintPipelines = constraintPipelines,
            yieldObjective = yieldObjective,
            wasteObjective = wasteObjective,
            lengthObjective = lengthObjective,
            extraPipelines = _extraPipelines,
            mode = _mode,
            shadowPriceKeys = _shadowPriceKeys,
            warmStartPlanUsages = input.warmStartPlanUsages,
            wasteOverProductionAreaMeasure = wasteCfg?.overProductionAreaMeasure ?: OverProductionAreaMeasure.ProductMaxWidthProxy,
            wasteRestMaterialMeasure = wasteCfg?.restMaterialMeasure ?: RestMaterialMeasure.RestWidthByMaterialLengthProxy,
            objectivePolicies = _objectivePolicies,
            vSample = resolveVSample(input),
            isFinalMilp = _isFinalMilp
        )
    }

    /**
     * 从输入数据推导 V 的样本值，用于 Flt64 → V 显式转换
     * Derive a sample V value from input data, used for explicit Flt64 → V conversion
     */
    private fun resolveVSample(input: ProduceInput<V>): V {
        // 优先从 demand 的 quantity 获取 / Prefer demand quantity
        input.demands.firstOrNull()?.quantity?.value?.let { return it }
        // 其次从 material 的 widthRange 获取 / Fallback to material width range
        input.materials.firstOrNull()?.widthRange?.lowerBound?.value?.let { return it }
        // 再次从 cutting plan 的 restWidth 获取 / Fallback to plan restWidth
        input.cuttingPlans.firstOrNull()?.restWidth?.value?.let { return it }
        // 最后从 yield config 的 penalty 获取 / Fallback to yield config penalty
        _yieldConfig?.underProductionPenalty?.values?.firstOrNull()?.let { return it }
        _wasteConfig?.trimWidthPenalty?.let { return it }
        _lengthConfig?.batchMinPenalty?.let { return it }
        throw IllegalArgumentException(
            "Cannot derive V sample from ProduceInput; at least one demand, material, or config with V value is required"
        )
    }
}
