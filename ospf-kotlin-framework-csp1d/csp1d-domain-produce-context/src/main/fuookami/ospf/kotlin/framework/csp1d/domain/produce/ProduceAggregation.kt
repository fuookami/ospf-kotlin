package fuookami.ospf.kotlin.framework.csp1d.domain.produce

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.plusAssign
import fuookami.ospf.kotlin.multiarray.Shape1
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.symbol.LinearExpressionSymbol
import fuookami.ospf.kotlin.core.symbol.LinearExpressionSymbols1
import fuookami.ospf.kotlin.core.symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.variable.UIntVariable1
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlan
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Machine
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Material
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ProductDemand
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.Csp1dAggregation
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.canonicalKey
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.CuttingPlanCanonicalKey

/**
 * 切割方案迭代聚合 / Cutting plan iterative aggregation
 *
 * 管理列生成迭代过程中的方案集合。与 BPP3D LayerAggregation 对齐，
 * 支持按迭代添加新方案并去重。
 *
 * Manages cutting plan sets during column generation iteration.
 * Aligned with BPP3D LayerAggregation, supporting per-iteration
 * addition of new plans with deduplication.
 *
 * @param V 数值类型 / Numeric value type
 */
class CuttingPlanAggregation<V : RealNumber<V>> {
    /** 每次迭代新增的方案 / Plans added per iteration */
    val plansIteration: MutableList<List<CuttingPlan<V>>> = ArrayList()

    /** 当前所有方案 / All current plans */
    val plans: MutableList<CuttingPlan<V>> = ArrayList()

    /** 已注册的方案 ID 集合 / Registered plan ID set */
    private val registeredIds: MutableSet<String> = HashSet()

    /** 已注册的方案 canonical key 集合 / Registered plan canonical key set */
    private val registeredKeys: MutableSet<CuttingPlanCanonicalKey> = HashSet()

    /**
     * 最近一次迭代的方案 / Plans from the last iteration
     */
    val lastIterationPlans: List<CuttingPlan<V>> get() = plansIteration.lastOrNull { it.isNotEmpty() } ?: emptyList()

    /**
     * 添加新列（去重后） / Add new columns (after deduplication)
     *
     * 通过 id 和 canonicalKey 去重。
     *
     * @param newPlans 新方案列表 / New plan list
     * @return 去重后实际新增的方案 / Deduplicated newly added plans
     */
    fun addColumns(newPlans: List<CuttingPlan<V>>): List<CuttingPlan<V>> {
        val unduplicatedPlans = newPlans.filter { candidate ->
            candidate.id !in registeredIds && candidate.canonicalKey() !in registeredKeys
        }
        plansIteration.add(unduplicatedPlans)
        for (plan in unduplicatedPlans) {
            plans.add(plan)
            registeredIds.add(plan.id)
            registeredKeys.add(plan.canonicalKey())
        }
        return unduplicatedPlans
    }

    /**
     * 添加初始方案（注册时调用）/ Add initial plans (called during registration)
     */
    fun addInitialPlans(initialPlans: List<CuttingPlan<V>>) {
        for (plan in initialPlans) {
            plans.add(plan)
            registeredIds.add(plan.id)
            registeredKeys.add(plan.canonicalKey())
        }
    }
}

/**
 * 产出聚合根 / Produce aggregation root
 *
 * 管理 CSP1D 主问题的核心变量 x[0..n-1]（每个切割方案的使用车次），
 * 以及约束中间符号（demandQuantity / materialQuantity / machineBatchQuantity / machineCapacityQuantity）。
 *
 * 支持 MILP 注册和列生成迭代模式（addColumns 原地增量）。
 * 参考 BPP3D ImpreciseAssignment 和 POIT CuttingPlanIterativeProduce 的模式：
 * - 初始注册时创建 x 变量组和中间符号
 * - addColumns 时创建新的 x_$iteration 变量组和 batch_$iteration 中间符号，
 *   并对已有中间符号执行 flush + asMutable 追加新列系数
 *
 * Manage the core variables x[0..n-1] (batch count per cutting plan) of the CSP1D master problem,
 * and constraint intermediate symbols (demandQuantity / materialQuantity / machineBatchQuantity / machineCapacityQuantity).
 *
 * Support MILP registration and column generation iterative mode (addColumns in-place increment).
 * Following the pattern from BPP3D ImpreciseAssignment and POIT CuttingPlanIterativeProduce:
 * - Initial registration creates x variable group and intermediate symbols
 * - addColumns creates new x_$iteration variable group and batch_$iteration intermediate symbols,
 *   and performs flush + asMutable on existing intermediate symbols to append new column coefficients
 *
 * @param V 数值类型 / Numeric value type
 * @property cuttingPlans 切割方案列表 / Cutting plan list
 * @property demands 需求列表 / Demand list
 * @property materials 物料列表 / Material list
 * @property machines 设备列表 / Machine list
 */
class ProduceAggregation<V : RealNumber<V>>(
    override val cuttingPlans: List<CuttingPlan<V>>,
    val demands: List<ProductDemand<V>>,
    val materials: List<Material<V>>,
    val machines: List<Machine<V>>
) : Csp1dAggregation<V> {

    /** 切割方案迭代聚合 / Cutting plan iterative aggregation */
    private val aggregation = CuttingPlanAggregation<V>()

    /** 每迭代一组 x 变量 / Per-iteration x variable group */
    private val _x = ArrayList<UIntVariable1>()

    /** 每迭代一组 batch 中间符号 / Per-iteration batch intermediate symbol group */
    private val _batch = ArrayList<LinearExpressionSymbols1<Flt64>>()

    /**
     * 需求贡献中间符号 / Demand contribution intermediate symbols
     *
     * 每个 demand 一个中间符号，表示 sum(contribution_j * x_j)，
     * 约束管线引用此中间符号而非直接引用 x 变量。
     *
     * One intermediate symbol per demand, representing sum(contribution_j * x_j).
     * Constraint pipelines reference this symbol instead of x variables directly.
     */
    lateinit var demandQuantity: LinearExpressionSymbols1<Flt64>

    /**
     * 物料用量中间符号 / Material usage intermediate symbols
     *
     * 每个物料一个中间符号，表示 sum(x_j where plan_j.material == material_i)。
     *
     * One intermediate symbol per material, representing sum(x_j where plan_j.material == material_i).
     */
    lateinit var materialQuantity: LinearExpressionSymbols1<Flt64>

    /**
     * 设备批次数中间符号 / Machine batch count intermediate symbols
     *
     * 每个设备一个中间符号，表示 sum(x_j where plan_j.machineId == machine_i)。
     *
     * One intermediate symbol per machine, representing sum(x_j where plan_j.machineId == machine_i).
     */
    lateinit var machineBatchQuantity: LinearExpressionSymbols1<Flt64>

    /**
     * 设备产能消耗中间符号 / Machine capacity consumption intermediate symbols
     *
     * 每个设备一个中间符号，表示 sum(consumption_j * x_j where plan_j.machineId == machine_i)。
     *
     * One intermediate symbol per machine, representing sum(consumption_j * x_j where plan_j.machineId == machine_i).
     */
    lateinit var machineCapacityQuantity: LinearExpressionSymbols1<Flt64>

    /** 方案数量 / Plan count */
    val planCount: Int get() = cuttingPlans.size

    /**
     * 获取指定索引的变量（初始 x 组）/ Get variable at specified index (from initial x group)
     */
    operator fun get(index: Int) = _x.firstOrNull()?.get(index)
        ?: throw IllegalStateException("ProduceAggregation not registered yet")

    /**
     * 获取所有 x 变量组 / Get all x variable groups
     */
    val xGroups: List<UIntVariable1> get() = _x

    /**
     * 获取所有 batch 中间符号组 / Get all batch intermediate symbol groups
     */
    val batchGroups: List<LinearExpressionSymbols1<Flt64>> get() = _batch

    /**
     * 注册到元模型 / Register to meta model
     *
     * 将初始 x 变量、batch 中间符号和约束中间符号注册到元模型。
     * Register the initial x variables, batch intermediate symbols and constraint intermediate symbols.
     */
    override fun register(model: LinearMetaModel<Flt64>): Try {
        // 初始化迭代聚合 / Initialize iterative aggregation
        aggregation.addInitialPlans(cuttingPlans)

        // 1. 创建初始 x 变量组 / Create initial x variable group
        val x0 = UIntVariable1("x_0", Shape1(cuttingPlans.size))
        for ((i, plan) in cuttingPlans.withIndex()) {
            x0[i].name = "x_0_$i"
        }
        _x.add(x0)
        when (val result = model.add(x0)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        // 2. 创建初始 batch 中间符号组 / Create initial batch intermediate symbol group
        val batch0 = LinearExpressionSymbols1(
            name = "batch_0",
            shape = Shape1(cuttingPlans.size)
        ) { i, _ ->
            LinearExpressionSymbol(
                monomial = LinearMonomial(Flt64.one, x0[i]),
                name = "batch_0_$i"
            )
        }
        _batch.add(batch0)
        when (val result = model.add(batch0)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        // 3. 创建需求贡献中间符号 / Create demand contribution intermediate symbols
        demandQuantity = LinearExpressionSymbols1(
            name = "demand_quantity",
            shape = Shape1(demands.size)
        ) { demandIndex, _ ->
            val demand = demands[demandIndex]
            val monomials = cuttingPlans.mapIndexedNotNull { planIndex, plan ->
                val contribution = plan.demandContributions.find {
                    it.product.id == demand.product.id && it.quantity.unit == demand.quantity.unit
                } ?: return@mapIndexedNotNull null
                LinearMonomial(contribution.quantity.value.toFlt64(), batch0[planIndex])
            }
            if (monomials.isNotEmpty()) {
                LinearExpressionSymbol(
                    LinearPolynomial(monomials, Flt64.zero),
                    name = "demand_quantity_$demandIndex"
                )
            } else {
                LinearExpressionSymbol(
                    Flt64.zero,
                    name = "demand_quantity_$demandIndex"
                )
            }
        }
        when (val result = model.add(demandQuantity)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        // 4. 创建物料用量中间符号 / Create material usage intermediate symbols
        materialQuantity = LinearExpressionSymbols1(
            name = "material_quantity",
            shape = Shape1(materials.size)
        ) { materialIndex, _ ->
            val material = materials[materialIndex]
            val monomials = cuttingPlans.mapIndexedNotNull { planIndex, plan ->
                if (plan.material.id != material.id) return@mapIndexedNotNull null
                LinearMonomial(Flt64.one, batch0[planIndex])
            }
            if (monomials.isNotEmpty()) {
                LinearExpressionSymbol(
                    LinearPolynomial(monomials, Flt64.zero),
                    name = "material_quantity_$materialIndex"
                )
            } else {
                LinearExpressionSymbol(
                    Flt64.zero,
                    name = "material_quantity_$materialIndex"
                )
            }
        }
        when (val result = model.add(materialQuantity)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        // 5. 创建设备批次数中间符号 / Create machine batch count intermediate symbols
        machineBatchQuantity = LinearExpressionSymbols1(
            name = "machine_batch_quantity",
            shape = Shape1(machines.size)
        ) { machineIndex, _ ->
            val machine = machines[machineIndex]
            val monomials = cuttingPlans.mapIndexedNotNull { planIndex, plan ->
                if (plan.machineId != machine.id) return@mapIndexedNotNull null
                LinearMonomial(Flt64.one, batch0[planIndex])
            }
            if (monomials.isNotEmpty()) {
                LinearExpressionSymbol(
                    LinearPolynomial(monomials, Flt64.zero),
                    name = "machine_batch_quantity_$machineIndex"
                )
            } else {
                LinearExpressionSymbol(
                    Flt64.zero,
                    name = "machine_batch_quantity_$machineIndex"
                )
            }
        }
        when (val result = model.add(machineBatchQuantity)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        // 6. 创建设备产能消耗中间符号 / Create machine capacity consumption intermediate symbols
        machineCapacityQuantity = LinearExpressionSymbols1(
            name = "machine_capacity_quantity",
            shape = Shape1(machines.size)
        ) { machineIndex, _ ->
            val machine = machines[machineIndex]
            val monomials = cuttingPlans.mapIndexedNotNull { planIndex, plan ->
                if (plan.machineId != machine.id) return@mapIndexedNotNull null
                val capacity = machine.capacity ?: return@mapIndexedNotNull null
                val consumption = plan.capacityConsumption ?: return@mapIndexedNotNull null
                if (consumption.unit != capacity.unit) return@mapIndexedNotNull null
                if (consumption.value leq consumption.value.constants.zero) return@mapIndexedNotNull null
                LinearMonomial(consumption.value.toFlt64(), batch0[planIndex])
            }
            if (monomials.isNotEmpty()) {
                LinearExpressionSymbol(
                    LinearPolynomial(monomials, Flt64.zero),
                    name = "machine_capacity_quantity_$machineIndex"
                )
            } else {
                LinearExpressionSymbol(
                    Flt64.zero,
                    name = "machine_capacity_quantity_$machineIndex"
                )
            }
        }
        when (val result = model.add(machineCapacityQuantity)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        return ok
    }

    /**
     * 添加新列到模型 / Add new columns to model
     *
     * 为新增切割方案创建 x_$iteration 变量组和 batch_$iteration 中间符号，
     * 并对已有约束中间符号执行 flush + asMutable 追加新列系数。
     *
     * Create x_$iteration variable group and batch_$iteration intermediate symbol group
     * for new cutting plans, and perform flush + asMutable on existing constraint
     * intermediate symbols to append new column coefficients.
     *
     * @param iteration 迭代号 / Iteration number
     * @param newPlans 新方案列表 / New plan list
     * @param model 线性元模型 / Linear meta model
     * @return 去重后新增的方案 / Deduplicated newly added plans
     */
    suspend fun addColumns(
        iteration: UInt64,
        newPlans: List<CuttingPlan<V>>,
        model: AbstractLinearMetaModel<Flt64>
    ): Ret<List<CuttingPlan<V>>> {
        // 去重通过 aggregation.addColumns / Dedup via aggregation.addColumns
        val unduplicatedPlans = aggregation.addColumns(newPlans)
        if (unduplicatedPlans.isEmpty()) return Ok(emptyList())

        // 1. 创建新 x_$iteration 变量组 / Create new x_$iteration variable group
        val xi = UIntVariable1("x_$iteration", Shape1(unduplicatedPlans.size))
        for ((i, plan) in unduplicatedPlans.withIndex()) {
            xi[i].name = "x_${iteration}_$i"
        }
        _x.add(xi)
        when (val result = model.add(xi)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        // 2. 创建新 batch_$iteration 中间符号组 / Create new batch_$iteration intermediate symbol group
        val batchI = LinearExpressionSymbols1(
            name = "batch_$iteration",
            shape = Shape1(unduplicatedPlans.size)
        ) { i, _ ->
            LinearExpressionSymbol(
                monomial = LinearMonomial(Flt64.one, xi[i]),
                name = "batch_${iteration}_$i"
            )
        }
        _batch.add(batchI)
        when (val result = model.add(batchI)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        // 3. 刷新需求贡献中间符号 / Refresh demand contribution intermediate symbols
        for ((demandIndex, demand) in demands.withIndex()) {
            val newMonomials = unduplicatedPlans.mapIndexedNotNull { planIndex, plan ->
                val contribution = plan.demandContributions.find {
                    it.product.id == demand.product.id && it.quantity.unit == demand.quantity.unit
                } ?: return@mapIndexedNotNull null
                LinearMonomial(contribution.quantity.value.toFlt64(), batchI[planIndex])
            }
            if (newMonomials.isNotEmpty()) {
                demandQuantity[demandIndex].flush()
                demandQuantity[demandIndex].asMutable() += LinearPolynomial(newMonomials, Flt64.zero)
            }
        }

        // 4. 刷新物料用量中间符号 / Refresh material usage intermediate symbols
        for ((materialIndex, material) in materials.withIndex()) {
            val newMonomials = unduplicatedPlans.mapIndexedNotNull { planIndex, plan ->
                if (plan.material.id != material.id) return@mapIndexedNotNull null
                LinearMonomial(Flt64.one, batchI[planIndex])
            }
            if (newMonomials.isNotEmpty()) {
                materialQuantity[materialIndex].flush()
                materialQuantity[materialIndex].asMutable() += LinearPolynomial(newMonomials, Flt64.zero)
            }
        }

        // 5. 刷新设备批次数中间符号 / Refresh machine batch count intermediate symbols
        for ((machineIndex, machine) in machines.withIndex()) {
            val newMonomials = unduplicatedPlans.mapIndexedNotNull { planIndex, plan ->
                if (plan.machineId != machine.id) return@mapIndexedNotNull null
                LinearMonomial(Flt64.one, batchI[planIndex])
            }
            if (newMonomials.isNotEmpty()) {
                machineBatchQuantity[machineIndex].flush()
                machineBatchQuantity[machineIndex].asMutable() += LinearPolynomial(newMonomials, Flt64.zero)
            }
        }

        // 6. 刷新设备产能消耗中间符号 / Refresh machine capacity consumption intermediate symbols
        for ((machineIndex, machine) in machines.withIndex()) {
            val newMonomials = unduplicatedPlans.mapIndexedNotNull { planIndex, plan ->
                if (plan.machineId != machine.id) return@mapIndexedNotNull null
                val capacity = machine.capacity ?: return@mapIndexedNotNull null
                val consumption = plan.capacityConsumption ?: return@mapIndexedNotNull null
                if (consumption.unit != capacity.unit) return@mapIndexedNotNull null
                if (consumption.value leq consumption.value.constants.zero) return@mapIndexedNotNull null
                LinearMonomial(consumption.value.toFlt64(), batchI[planIndex])
            }
            if (newMonomials.isNotEmpty()) {
                machineCapacityQuantity[machineIndex].flush()
                machineCapacityQuantity[machineIndex].asMutable() += LinearPolynomial(newMonomials, Flt64.zero)
            }
        }

        return Ok(unduplicatedPlans)
    }

    /**
     * 获取所有已注册的方案（含迭代新增）/ Get all registered plans (including iteratively added)
     */
    val allPlans: List<CuttingPlan<V>> get() = aggregation.plans
}
