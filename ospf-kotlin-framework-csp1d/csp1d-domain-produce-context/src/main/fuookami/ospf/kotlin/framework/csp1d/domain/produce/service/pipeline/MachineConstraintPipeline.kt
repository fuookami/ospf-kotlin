package fuookami.ospf.kotlin.framework.csp1d.domain.produce.service.pipeline

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.MetaDualSolution
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.framework.model.CGPipeline
import fuookami.ospf.kotlin.framework.model.ShadowPriceExtractor
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.*
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.ProduceAggregation

/**
 * 设备约束管线 / Machine constraint pipeline
 *
 * 为每个设备添加两类约束：
 * - 批次数约束：machineBatchQuantity[i] <= maxBatchCount
 * - 产能约束：machineCapacityQuantity[i] <= capacity
 *
 * machineBatchQuantity[i] 和 machineCapacityQuantity[i] 是中间符号，
 * 由 ProduceAggregation 管理，约束管线不再直接引用 x 变量。
 *
 * 实现 CGPipeline 接口，通过 constraint.args = MachineShadowPriceKey
 * 关联影子价格，替代 constraint-name registry。
 *
 * Add two types of constraints for each machine:
 * - Batch count constraint: machineBatchQuantity[i] <= maxBatchCount
 * - Capacity constraint: machineCapacityQuantity[i] <= capacity
 *
 * machineBatchQuantity[i] and machineCapacityQuantity[i] are intermediate symbols
 * managed by ProduceAggregation. Constraint pipelines no longer reference x variables directly.
 *
 * Implements CGPipeline interface, associating shadow prices via
 * constraint.args = MachineShadowPriceKey, replacing constraint-name registry.
 *
 * @param V 数值类型 / Numeric value type
 * @property produce 产出聚合 / Produce aggregation
 * @property machines 设备列表 / Machine list
 */
class MachineConstraintPipeline<V : RealNumber<V>>(
    private val produce: ProduceAggregation<V>,
    private val machines: List<Machine<V>>
) : Csp1dCGPipeline {

    override val name: String = "machine_constraint"

    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        for ((machineIndex, machine) in machines.withIndex()) {
            addMachineBatchConstraint(model, machineIndex, machine)
            addMachineCapacityConstraint(model, machineIndex, machine)
        }
        return ok
    }

    private fun addMachineBatchConstraint(
        model: AbstractLinearMetaModel<Flt64>,
        machineIndex: Int,
        machine: Machine<V>
    ) {
        val maxBatchCount = machine.maxBatchCount ?: return

        // 跳过零值中间符号（没有方案分配到该设备）
        // Skip zero-valued intermediate symbols (no plans assigned to this machine)
        val symbol = produce.machineBatchQuantity[machineIndex]
        if (symbol.polynomial.monomials.isEmpty()) return

        val constraintName = "machine_batch_$machineIndex"
        val priceKey = MachineBatchShadowPriceKey(machine.id)

        val lhs = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, symbol)),
            constant = Flt64.zero
        )
        model.addConstraint(
            relation = LinearInequality(
                lhs = lhs,
                rhs = DemandConstraintPipeline.constantPolynomial(maxBatchCount.toFlt64()),
                comparison = Comparison.LE
            ),
            group = this,
            name = constraintName,
            args = priceKey
        )
    }

    private fun addMachineCapacityConstraint(
        model: AbstractLinearMetaModel<Flt64>,
        machineIndex: Int,
        machine: Machine<V>
    ) {
        val capacity = machine.capacity ?: return

        // 跳过零值中间符号（没有方案产能消耗匹配该设备）
        // Skip zero-valued intermediate symbols (no plan capacity consumption matches this machine)
        val symbol = produce.machineCapacityQuantity[machineIndex]
        if (symbol.polynomial.monomials.isEmpty()) return

        val constraintName = "machine_capacity_$machineIndex"
        val priceKey = MachineCapacityShadowPriceKey(machine.id)

        val lhs = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, symbol)),
            constant = Flt64.zero
        )
        model.addConstraint(
            relation = LinearInequality(
                lhs = lhs,
                rhs = DemandConstraintPipeline.constantPolynomial(capacity.value.toFlt64()),
                comparison = Comparison.LE
            ),
            group = this,
            name = constraintName,
            args = priceKey
        )
    }

    override fun refresh(
        shadowPriceMap: Csp1dShadowPriceMap,
        model: AbstractLinearMetaModel<Flt64>,
        shadowPrices: MetaDualSolution
    ): Try {
        return CGPipeline.refreshByKeyAsArgs(this, shadowPriceMap, model, shadowPrices)
    }

    override fun extractor(): Csp1dShadowPriceExtractor? {
        if (machines.isEmpty()) return null
        return { map, args ->
            if (args is Csp1dCuttingPlanShadowPriceArguments<*>) {
                val machineId = args.plan.machineId
                if (machineId == null) {
                    Flt64.zero
                } else {
                    var price = Flt64.zero
                    val batchKey = MachineBatchShadowPriceKey(machineId)
                    map[batchKey]?.price?.let { price += it }
                    val capacityKey = MachineCapacityShadowPriceKey(machineId)
                    val capacityPrice = map[capacityKey]?.price
                    if (capacityPrice != null) {
                        val consumption = args.plan.capacityConsumption?.value?.toFlt64()
                        if (consumption != null) {
                            price += capacityPrice * consumption
                        }
                    }
                    price
                }
            } else {
                Flt64.zero
            }
        }
    }
}
