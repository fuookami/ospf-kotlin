/**
 * 主问题建模：图案使用量、需求覆盖、物料可用量、产能与目标。 / Master problem modelling: pattern usage, demand
 * coverage, material availability, capacity and objectives.
 *
 * 这是 csp2d 的**建模边界**：与 bpp2d 的 `layer-assignment` 同级，允许出现 `FltX`；领域其余部分保持泛型。
 * / This is the **modelling boundary** of csp2d: on the same level as bpp2d `layer-assignment`, it may use `FltX` while
 * the rest of the domain stays generic.
 *
 * 与一维的关键差异：一维方案是「切片列表」，主问题按切片宽度累加；二维方案是「一块料上的图案」，
 * 主问题按图案使用次数累加，覆盖量来自图案里的件数，消耗量来自图案料域的面积或段高。
 * / The key difference from the 1D case: a 1D plan is a slice list, so the master problem accumulates slice widths,
 * while a 2D plan is a pattern on one piece of material, so the master problem accumulates pattern usage counts, with
 * coverage coming from the piece counts inside the pattern and consumption coming from the region area or segment
 * height.
*/
package fuookami.ospf.kotlin.framework.csp2d.domain.produce

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.plusAssign
import fuookami.ospf.kotlin.multiarray.Shape1
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.symbol.LinearExpressionSymbol
import fuookami.ospf.kotlin.core.symbol.LinearIntermediateSymbols1
import fuookami.ospf.kotlin.core.variable.UIntVariable1
import fuookami.ospf.kotlin.framework.csp2d.domain.material.model.CuttingPlan2
import fuookami.ospf.kotlin.framework.csp2d.domain.material.model.DemandMode2
import fuookami.ospf.kotlin.framework.csp2d.domain.material.model.DemandValue2
import fuookami.ospf.kotlin.framework.csp2d.domain.material.model.Machine2
import fuookami.ospf.kotlin.framework.csp2d.domain.material.model.Material2
import fuookami.ospf.kotlin.framework.csp2d.domain.material.model.ProductDemand2
import fuookami.ospf.kotlin.framework.csp2d.infrastructure.Csp2dValidationError

/**
 * 构造非空线性多项式。 / Build a non-empty linear polynomial.
 *
 * `LinearPolynomial` 在空单项式列表上不可用，而裁剪后的目标或约束项数可能为零，因此统一走这里兜底为常数零。
 * / `LinearPolynomial` cannot be built from an empty monomial list, while a trimmed objective or constraint may end up
 * with no term, so everything goes through this helper and falls back to the zero constant.
 *
 * @param monomials 单项式列表 / monomial list
 * @return 线性多项式 / linear polynomial
*/
fun safeCsp2dPolynomial(monomials: List<LinearMonomial<FltX>>): LinearPolynomial<FltX> {
    return if (monomials.isEmpty()) {
        LinearPolynomial(emptyList(), FltX.zero)
    } else {
        LinearPolynomial(monomials, FltX.zero)
    }
}

/**
 * 图案使用量变量集合。 / The pattern usage variable set.
 *
 * @property plans 候选方案 / candidate plans
*/
class Csp2dPatternAssignment(
    val plans: List<CuttingPlan2<FltX>>
) {

    /** 每个方案的使用次数变量 / usage variable of each plan */
    lateinit var usage: UIntVariable1

    /** 成品占用面积表达式 / expression of the area occupied by finished parts */
    lateinit var usedArea: LinearExpressionSymbol<FltX>

    /** 余料面积表达式 / expression of the remnant area */
    lateinit var wasteArea: LinearExpressionSymbol<FltX>

    /** 消耗料域面积表达式 / expression of the consumed region area */
    lateinit var consumedArea: LinearExpressionSymbol<FltX>

    /** 消耗段高（卷材模式）表达式 / expression of the consumed segment height (coil mode) */
    lateinit var consumedHeight: LinearExpressionSymbol<FltX>

    /**
     * 注册变量与聚合表达式。 / Register the variables and the aggregate expressions.
     *
     * 使用次数变量上界按「需求件数上界」收紧：单方案重复次数不会超过全部需求件数之和，避免无意义的无限上界。
     * / The usage variable bound is tightened by the demand piece count: a single plan cannot be repeated more times
     * than the total demanded piece count, which avoids a meaningless unbounded range.
     *
     * @param model 线性元模型 / linear meta model
     * @param demandUpperBound 使用次数上界 / usage upper bound
     * @return 注册结果 / registration result
    */
    fun register(model: LinearMetaModel<FltX>, demandUpperBound: UInt64): Try {
        if (plans.isEmpty()) {
            return Failed(
                Csp2dValidationError(detail = "主问题没有候选方案 / The master problem has no candidate plan")
            )
        }
        if (!::usage.isInitialized) {
            usage = UIntVariable1("usage", Shape1(plans.size))
            val bound = if (demandUpperBound == UInt64.zero) UInt64.one else demandUpperBound
            for (index in plans.indices) {
                usage[index].range.leq(bound)
            }
        }
        when (val result = model.add(usage)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        if (!::usedArea.isInitialized) {
            usedArea = LinearExpressionSymbol(FltX.zero, name = "used_area")
        }
        when (val result = model.add(usedArea)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        usedArea.flush()
        usedArea.asMutable() += safeCsp2dPolynomial(
            plans.mapIndexed { index, plan ->
                LinearMonomial(plan.usedArea.value, usage[index])
            }
        )

        if (!::wasteArea.isInitialized) {
            wasteArea = LinearExpressionSymbol(FltX.zero, name = "waste_area")
        }
        when (val result = model.add(wasteArea)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        wasteArea.flush()
        wasteArea.asMutable() += safeCsp2dPolynomial(
            plans.mapIndexed { index, plan ->
                LinearMonomial(plan.wasteArea.value, usage[index])
            }
        )

        if (!::consumedArea.isInitialized) {
            consumedArea = LinearExpressionSymbol(FltX.zero, name = "consumed_area")
        }
        when (val result = model.add(consumedArea)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        consumedArea.flush()
        consumedArea.asMutable() += safeCsp2dPolynomial(
            plans.mapIndexed { index, plan ->
                LinearMonomial(plan.pattern.region.area.value, usage[index])
            }
        )

        if (!::consumedHeight.isInitialized) {
            consumedHeight = LinearExpressionSymbol(FltX.zero, name = "consumed_height")
        }
        when (val result = model.add(consumedHeight)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        consumedHeight.flush()
        consumedHeight.asMutable() += safeCsp2dPolynomial(
            plans.mapIndexed { index, plan ->
                LinearMonomial(plan.pattern.region.height.value, usage[index])
            }
        )
        return ok
    }

}

/**
 * 需求覆盖约束：`Σ_j 覆盖_ij · y_j ≥ 需求_i`。 / Demand coverage constraint `Σ_j coverage_ij · y_j ≥ demand_i`.
 *
 * 覆盖量的口径随需求口径变化：件数口径按件数、面积口径按成品面积、重量口径按单件重量。三者都归一到同一个数值域，
 * 因为主问题只有一套变量。
 * / The coverage unit follows the demand unit: counts for the amount unit, finished part area for the area unit and per
 * piece weight for the weight unit. All three map into one numeric domain because the master problem has a single
 * variable set.
 *
 * @property plans 候选方案 / candidate plans
 * @property assignment 图案使用量变量 / pattern usage variables
 * @property demands 产品需求 / product demands
 * @property name 约束名称 / constraint name
*/
class Csp2dDemandConstraint(
    private val plans: List<CuttingPlan2<FltX>>,
    private val assignment: Csp2dPatternAssignment,
    private val demands: List<ProductDemand2<FltX>>,
    val name: String = "csp2d_demand"
) {

    /**
     * 注册需求覆盖约束。 / Register the demand coverage constraints.
     *
     * @param model 线性元模型 / linear meta model
     * @return 注册结果 / registration result
    */
    fun invoke(model: LinearMetaModel<FltX>): Try {
        for ((demandIndex, demand) in demands.withIndex()) {
            val required = requiredValue(demand)
            val monomials = ArrayList<LinearMonomial<FltX>>()
            for ((planIndex, plan) in plans.withIndex()) {
                val coverage = coverageOf(plan, demand)
                if (coverage == FltX.zero) {
                    continue
                }
                monomials.add(LinearMonomial(coverage, assignment.usage[planIndex]))
            }
            val relation = LinearInequality(
                safeCsp2dPolynomial(monomials),
                LinearPolynomial(emptyList(), required),
                Comparison.GE
            )
            when (val result = model.addConstraint(relation = relation, name = "${name}_$demandIndex")) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        return ok
    }

    /**
     * 读取需求的目标值。 / Read the target value of a demand.
     *
     * @param demand 产品需求 / product demand
     * @return 目标数值 / target value
    */
    private fun requiredValue(demand: ProductDemand2<FltX>): FltX {
        return when (val value = demand.demand) {
            is DemandValue2.Amount -> FltX(value.count.toInt().toDouble())
            is DemandValue2.Area -> value.area.value
            is DemandValue2.Weight -> value.weight.value
        }
    }

    /**
     * 计算单个方案对某个需求的覆盖量。 / Compute the coverage of one plan for one demand.
     *
     * @param plan 方案 / plan
     * @param demand 需求 / demand
     * @return 覆盖量 / coverage
    */
    private fun coverageOf(plan: CuttingPlan2<FltX>, demand: ProductDemand2<FltX>): FltX {
        var amount = UInt64.zero
        for (contribution in plan.contributions) {
            if (contribution.product.id.value != demand.product.id.value) {
                continue
            }
            amount = contribution.amount
            break
        }
        if (amount == UInt64.zero) {
            return FltX.zero
        }
        val count = FltX(amount.toInt().toDouble())
        return when (demand.mode) {
            DemandMode2.Amount -> count
            DemandMode2.Area -> count * demand.product.area.value
            DemandMode2.Weight -> {
                val unitWeight = demand.product.unitWeight
                if (unitWeight == null) FltX.zero else count * unitWeight.value
            }
        }
    }
}

/**
 * 物料可用量约束：`Σ_j y_j ≤ 可用卷数/张数`。 / Material availability constraint `Σ_j y_j ≤ available count`.
 *
 * 同一物料的方案共享一个可用量；不同物料各有一条约束。
 * / Plans of the same material share one availability, so each material gets one constraint.
 *
 * @property plans 候选方案 / candidate plans
 * @property assignment 图案使用量变量 / pattern usage variables
 * @property materials 物料目录 / material catalogue
 * @property name 约束名称 / constraint name
*/
class Csp2dMaterialAvailabilityConstraint(
    private val plans: List<CuttingPlan2<FltX>>,
    private val assignment: Csp2dPatternAssignment,
    private val materials: List<Material2<FltX>>,
    val name: String = "csp2d_material"
) {

    /**
     * 注册物料可用量约束。 / Register the material availability constraints.
     *
     * @param model 线性元模型 / linear meta model
     * @return 注册结果 / registration result
    */
    fun invoke(model: LinearMetaModel<FltX>): Try {
        for ((materialIndex, material) in materials.withIndex()) {
            if (material.availableBatches == UInt64.maximum) {
                // 不限量时不注册约束，避免给出无意义的巨大上界。 / Unlimited material gets no constraint, avoiding a
                // meaningless huge bound.
                continue
            }
            val monomials = ArrayList<LinearMonomial<FltX>>()
            for ((planIndex, plan) in plans.withIndex()) {
                if (plan.material.id.value != material.id.value) {
                    continue
                }
                monomials.add(LinearMonomial(FltX.one, assignment.usage[planIndex]))
            }
            if (monomials.isEmpty()) {
                continue
            }
            val relation = LinearInequality(
                safeCsp2dPolynomial(monomials),
                LinearPolynomial(emptyList(), FltX(material.availableBatches.toInt().toDouble())),
                Comparison.LE
            )
            when (
                val result = model.addConstraint(relation = relation, name = "${name}_$materialIndex")
            ) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        return ok
    }
}

/**
 * 设备产能约束：`Σ_j 产能消耗_j · y_j ≤ 产能`。 / Machine capacity constraint `Σ_j consumption_j · y_j ≤ capacity`.
 *
 * @property plans 候选方案 / candidate plans
 * @property assignment 图案使用量变量 / pattern usage variables
 * @property machines 设备目录 / machine catalogue
 * @property name 约束名称 / constraint name
*/
class Csp2dMachineCapacityConstraint(
    private val plans: List<CuttingPlan2<FltX>>,
    private val assignment: Csp2dPatternAssignment,
    private val machines: List<Machine2<FltX>>,
    val name: String = "csp2d_capacity"
) {

    /**
     * 注册设备产能约束。 / Register the machine capacity constraints.
     *
     * 未配置产能的设备不注册约束；未配置产能消耗的方案不计入。
     * / Machines without a capacity get no constraint, and plans without a consumption do not contribute.
     *
     * @param model 线性元模型 / linear meta model
     * @return 注册结果 / registration result
    */
    fun invoke(model: LinearMetaModel<FltX>): Try {
        for ((machineIndex, machine) in machines.withIndex()) {
            val capacity = machine.capacity ?: continue
            val monomials = ArrayList<LinearMonomial<FltX>>()
            for ((planIndex, plan) in plans.withIndex()) {
                val consumption = plan.capacityConsumption ?: continue
                if (!sameMachine(plan, machine)) {
                    continue
                }
                monomials.add(LinearMonomial(consumption.value, assignment.usage[planIndex]))
            }
            if (monomials.isEmpty()) {
                continue
            }
            val relation = LinearInequality(
                safeCsp2dPolynomial(monomials),
                LinearPolynomial(emptyList(), capacity.value),
                Comparison.LE
            )
            when (
                val result = model.addConstraint(relation = relation, name = "${name}_$machineIndex")
            ) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        return ok
    }

    /**
     * 判断方案是否属于指定设备。 / Decide whether a plan belongs to the given machine.
     *
     * @param plan 方案 / plan
     * @param machine 设备 / machine
     * @return 是否属于 / whether it belongs
    */
    private fun sameMachine(plan: CuttingPlan2<FltX>, machine: Machine2<FltX>): Boolean {
        val planMachine = plan.machineId ?: return false
        return planMachine.value == machine.id.value
    }
}

/**
 * 方案使用量目标：`min Σ_j 系数_j · y_j`。 / Plan usage objective `min Σ_j coefficient_j · y_j`.
 *
 * 系数由调用方给出（板材张数、消耗面积、段高、余料），因此同一个目标类可以表达多种口径，
 * 而不必为每种口径各写一个目标。
 * / The coefficient is supplied by the caller (sheet count, consumed area, segment height, waste), so a single objective
 * class expresses many measures instead of one class per measure.
 *
 * @property plans 候选方案 / candidate plans
 * @property assignment 图案使用量变量 / pattern usage variables
 * @property coefficient 方案到系数 / plan to coefficient
 * @property name 目标名称 / objective name
*/
class Csp2dUsageObjective(
    private val plans: List<CuttingPlan2<FltX>>,
    private val assignment: Csp2dPatternAssignment,
    private val coefficient: (CuttingPlan2<FltX>) -> FltX,
    val name: String = "csp2d_usage"
) {

    /**
     * 注册目标。 / Register the objective.
     *
     * @param model 线性元模型 / linear meta model
     * @return 注册结果 / registration result
    */
    fun invoke(model: LinearMetaModel<FltX>): Try {
        val monomials = plans.mapIndexedNotNull { index, plan ->
            val value = coefficient(plan)
            if (value == FltX.zero) {
                null
            } else {
                LinearMonomial(value, assignment.usage[index])
            }
        }
        if (monomials.isEmpty()) {
            return ok
        }
        when (val result = model.minimize(safeCsp2dPolynomial(monomials), name = name)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        return ok
    }
}

/**
 * 主问题就绪状态：变量、需求、物料、产能与目标的注册结果。 / Master problem readiness: the registration results of
 * variables, demand, material, capacity and objectives.
 *
 * @property model 线性元模型 / linear meta model
 * @property assignment 图案使用量变量 / pattern usage variables
 * @property demandRegistered 需求约束注册结果 / demand constraint registration result
 * @property materialRegistered 物料约束注册结果 / material constraint registration result
 * @property capacityRegistered 产能约束注册结果 / capacity constraint registration result
 * @property objectiveRegistered 目标注册结果 / objective registration result
*/
data class Csp2dMasterProblem(
    val model: LinearMetaModel<FltX>,
    val assignment: Csp2dPatternAssignment,
    val demandRegistered: Try,
    val materialRegistered: Try,
    val capacityRegistered: Try,
    val objectiveRegistered: Try
) {

    /**
     * 全部注册是否成功。 / Whether every registration succeeded.
     *
     * @return 是否全部成功 / whether everything succeeded
    */
    fun allSucceeded(): Boolean {
        return demandRegistered is Ok
                && materialRegistered is Ok
                && capacityRegistered is Ok
                && objectiveRegistered is Ok
    }
}
