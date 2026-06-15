package fuookami.ospf.kotlin.framework.csp1d.application.service

import kotlin.math.roundToLong
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.CuttingPlanGenerationStatistics
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.*
import fuookami.ospf.kotlin.framework.csp1d.domain.length_assignment.LengthAggregation
import fuookami.ospf.kotlin.framework.csp1d.domain.length_assignment.model.*
import fuookami.ospf.kotlin.framework.csp1d.domain.length_assignment.service.pipeline.*
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.*
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.*
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.*
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.service.pipeline.*
import fuookami.ospf.kotlin.framework.csp1d.domain.wasting_minimization.WasteAggregation
import fuookami.ospf.kotlin.framework.csp1d.domain.wasting_minimization.service.pipeline.*
import fuookami.ospf.kotlin.framework.csp1d.domain.yield.YieldAggregation
import fuookami.ospf.kotlin.framework.csp1d.domain.yield.model.*
import fuookami.ospf.kotlin.framework.csp1d.domain.yield.service.pipeline.*
import fuookami.ospf.kotlin.framework.model.*

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
    val constraintPipelines: List<Pipeline<LinearMetaModel<Flt64>>>,
    val cgPipelines: List<CGPipeline<AbstractCsp1dShadowPriceArguments, AbstractLinearMetaModel<Flt64>, AbstractCsp1dShadowPriceMap<AbstractCsp1dShadowPriceArguments>>>,
    private val yieldObjective: YieldObjectivePipeline<V>?,
    private val wasteObjective: WasteObjectivePipeline<V>?,
    private val lengthObjective: LengthObjectivePipeline<V>?,
    private val extraPipelines: List<Pipeline<LinearMetaModel<Flt64>>> = emptyList(),
    override val mode: Csp1dModelingMode = Csp1dModelingMode.MILP,
    private val warmStartPlanUsages: List<CuttingPlanUsage<V>> = emptyList(),
    private val wasteOverProductionAreaMeasure: OverProductionAreaMeasure = OverProductionAreaMeasure.ProductMaxWidthProxy,
    private val wasteRestMaterialMeasure: RestMaterialMeasure = RestMaterialMeasure.RestWidthByMaterialLengthProxy,
    private val objectivePolicies: List<Csp1dObjectivePolicy<V>> = emptyList(),
    override val domainValueSample: V,
    override val isFinalMilp: Boolean = false
) : Csp1dIterativeContext<V>, Csp1dModelingContext<V> {

    override val demands: List<ProductDemand<V>> get() = produce.demands
    override val materials: List<Material<V>> get() = produce.materials
    override val machines: List<Machine<V>> get() = produce.machines

    override fun toDomainValue(value: Flt64): V = convertSolverValue(domainValueSample, value)

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

        // 2. 注册 CG 约束管线（含 group 注册）
        for (pipeline in cgPipelines) {
            pipeline.register(model)
            when (val result = pipeline(model)) {
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
                is Ok -> {}
            }
        }

        // 3. 注册普通约束管线（length-assignment 例外）
        //    Register plain constraint pipelines (length-assignment exception)
        for (pipeline in constraintPipelines) {
            pipeline.register(model)
            when (val result = pipeline(model)) {
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
                is Ok -> {}
            }
        }

        // 4. 注册扩展约束管线
        for (pipeline in extraPipelines) {
            pipeline.register(model)
            when (val result = pipeline(model)) {
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
                is Ok -> {}
            }
        }

        // 5. 组装目标函数
        setObjective(model)

        // 6. 应用 warm start 初始解（仅 MILP 模式）
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
                    domainValueSample = domainValueSample
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
                        consumption.value * convertSolverValue(
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
                    val contribution = restWidthValue * convertSolverValue(restWidthValue, batchCount.toFlt64())
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
                    val contribution = restMaterialValue * convertSolverValue(restMaterialValue, batchCount.toFlt64())
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
                    val cost = costPenalty * convertSolverValue(costPenalty, batchCount.toFlt64())
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
                    val area = productWidthValue * convertSolverValue(productWidthValue, Flt64(overDouble))
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
        val planCountBefore = produce.planCount

        // 1. 委托 ProduceAggregation.addColumns() 完成变量、batch 和约束中间符号的原地增量
        //    Delegate to ProduceAggregation.addColumns() for in-place variable, batch and
        //    constraint intermediate symbol increment
        var addedPlans = when (val result = produce.addColumns(iteration, newPlans, model)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        if (addedPlans.isEmpty()) return Ok(emptyList())

        // 2. 追加新列目标项到模型 / Append new column objective terms to model
        //    为每个新增方案追加 minimize(batchCoefficient * x_j) 到目标函数
        //    Append minimize(batchCoefficient * x_j) for each new plan to the objective
        val baseBatchCoefficient = lengthObjective?.batchCoefficient() ?: Flt64.one
        val latestBatch = produce.batchGroups.lastOrNull()
        if (latestBatch != null) {
            for ((planIndex, plan) in addedPlans.withIndex()) {
                if (planIndex >= latestBatch.shape[0]) break
                val batchCoefficient = if (objectivePolicies.isNotEmpty()) {
                    val ctx = SimpleDomainCalculationContext(
                        plan = plan,
                        planIndex = planCountBefore + planIndex,
                        domainValueSample = domainValueSample
                    )
                    objectivePolicies.fold(baseBatchCoefficient) { coeff, policy ->
                        policy.modifyBatchCoefficient(ctx, coeff)
                    }
                } else {
                    baseBatchCoefficient
                }
                when (val result = model.minimize(
                    LinearMonomial(batchCoefficient, latestBatch[planIndex]),
                    name = "csp1d_objective_${iteration}_$planIndex"
                )) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            }
        }

        // 3. 刷新支持增量加列的扩展管线 / Refresh extension pipelines that support incremental addColumns
        val incrementalContext = this
        for (pipeline in extraPipelines) {
            @Suppress("UNCHECKED_CAST")
            val incrementalPipeline = pipeline as? Csp1dIncrementalPipeline<V> ?: continue
            addedPlans = when (val result = incrementalPipeline.addColumns(
                context = incrementalContext,
                iteration = iteration,
                newPlans = addedPlans,
                model = model
            )) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            if (addedPlans.isEmpty()) return Ok(emptyList())
        }

        return Ok(addedPlans)
    }

    override fun extractShadowPrice(
        model: AbstractLinearMetaModel<Flt64>,
        shadowPrices: MetaDualSolution
    ): Try {
        // 通过 CGPipeline refresh 机制自动提取影子价格到 AbstractCsp1dShadowPriceMap
        // Extract shadow prices automatically via CGPipeline refresh mechanism
        val frameworkMap = Csp1dDefaultShadowPriceMap()
        for (pipeline in cgPipelines) {
            when (val result = pipeline.refresh(frameworkMap, model, shadowPrices)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            val extractor = pipeline.extractor() ?: continue
            frameworkMap.put(extractor)
        }
        return ok
    }

    /**
     * 从 LP 对偶解提取影子价格映射 / Extract shadow price map from LP dual solution
     *
     * 使用 CGPipeline extractor 机制从 AbstractCsp1dShadowPriceMap 转换为
     * pricing 可消费的轻量级 ShadowPriceMap<V>。
     *
     * 注意：此方法不传 model，因此无法执行 CGPipeline refresh（需要 model.constraintsOfGroup）。
     * 仅适用于已经把 shadow price key 写入 constraint.args 的对偶解。推荐使用 Csp1dShadowPriceLifecycle.extractFromDualSolution(model, dualSolution)
     * 走 CGPipeline 主路径。
     *
     * Use CGPipeline extractor mechanism to convert from AbstractCsp1dShadowPriceMap
     * to lightweight ShadowPriceMap<V> for pricing consumption.
     *
     * Note: This method does not receive model, so CGPipeline refresh (which needs model.constraintsOfGroup)
     * cannot be executed. It only works for dual solutions whose constraints already carry shadow price keys
     * in constraint.args. Recommend using
     * Csp1dShadowPriceLifecycle.extractFromDualSolution(model, dualSolution) for the CGPipeline primary path.
     */
    fun extractShadowPriceMap(
        dualSolution: Map<fuookami.ospf.kotlin.core.model.mechanism.Constraint<Flt64, Linear>, Flt64>
    ): ShadowPriceMap<V> {
        val frameworkMap = Csp1dDefaultShadowPriceMap()
        // 无 model 时无法执行 CGPipeline refresh，直接从 constraint.origin.args 提取
        // Without model, cannot execute CGPipeline refresh; extract from constraint.origin.args
        val prices = HashMap<Csp1dShadowPriceKey, V>()
        for ((constraint, dualValue) in dualSolution) {
            val args = constraint.origin?.args as? Csp1dShadowPriceKey ?: continue
            val vDual = convertSolverValue(domainValueSample, dualValue)
            frameworkMap.put(fuookami.ospf.kotlin.framework.model.ShadowPrice(args, dualValue))
            val existingValue = prices[args]
            prices[args] = if (existingValue != null) existingValue + vDual else vDual
        }
        return ShadowPriceMap(prices)
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

/**
 * Csp1dProduceContext 构建器 / Csp1dProduceContext builder
 */
class Csp1dProduceContextBuilder<V : RealNumber<V>>(
    private val input: ProduceInput<V>
) {
    private var _yieldConfig: YieldModelingConfig<V>? = null
    private var _wasteConfig: WasteMinimizationConfig<V>? = null
    private var _lengthConfig: LengthAssignmentModelingConfig<V>? = null
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
        val domainValueSample = resolveDomainValueSample(input)
        val modelingContext = object : Csp1dModelingContext<V> {
            override val mode = _mode
            override val isFinalMilp = _isFinalMilp
            override val produce = produce
            override val demands: List<ProductDemand<V>> get() = produce.demands
            override val materials: List<Material<V>> get() = produce.materials
            override val machines: List<Machine<V>> get() = produce.machines
            override val domainValueSample = domainValueSample
            override fun toDomainValue(value: Flt64): V = convertSolverValue(domainValueSample, value)
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

        // 构建 CG 约束管线（demand/material/machine，MILP 下包含 yield）
        // Build CG constraint pipelines (demand/material/machine, plus yield in MILP)
        val cgPipelines = mutableListOf<CGPipeline<AbstractCsp1dShadowPriceArguments, AbstractLinearMetaModel<Flt64>, AbstractCsp1dShadowPriceMap<AbstractCsp1dShadowPriceArguments>>>()
        cgPipelines.add(DemandConstraintPipeline(
            produce = produce,
            demands = input.demands,
            yieldUnderVars = yieldAgg?.underProduction ?: emptyList(),
            yieldOverVars = yieldAgg?.overProduction ?: emptyList()
        ))
        cgPipelines.add(MaterialConstraintPipeline(
            produce = produce,
            materials = input.materials
        ))
        cgPipelines.add(MachineConstraintPipeline(
            produce = produce,
            machines = input.machines
        ))
        if (yieldAgg != null && yieldCfg != null) {
            cgPipelines.add(YieldConstraintPipeline(
                yield = yieldAgg,
                config = yieldCfg,
                demands = input.demands
            ))
        }

        // 普通约束管线只保留 length-assignment 例外
        // Plain constraint pipelines keep only the length-assignment exception
        val constraintPipelines = mutableListOf<Pipeline<LinearMetaModel<Flt64>>>()
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
            cgPipelines = cgPipelines,
            yieldObjective = yieldObjective,
            wasteObjective = wasteObjective,
            lengthObjective = lengthObjective,
            extraPipelines = _extraPipelines,
            mode = _mode,
            warmStartPlanUsages = input.warmStartPlanUsages,
            wasteOverProductionAreaMeasure = wasteCfg?.overProductionAreaMeasure ?: OverProductionAreaMeasure.ProductMaxWidthProxy,
            wasteRestMaterialMeasure = wasteCfg?.restMaterialMeasure ?: RestMaterialMeasure.RestWidthByMaterialLengthProxy,
            objectivePolicies = _objectivePolicies,
            domainValueSample = resolveDomainValueSample(input),
            isFinalMilp = _isFinalMilp
        )
    }

    /**
     * 从输入数据推导领域数值样本，用于 solver 值显式转换
     * Derive a domain value sample from input data for explicit solver value conversion
     */
    private fun resolveDomainValueSample(input: ProduceInput<V>): V {
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
            "Cannot derive domain value sample from ProduceInput; at least one demand, material, or config with domain value is required"
        )
    }
}
