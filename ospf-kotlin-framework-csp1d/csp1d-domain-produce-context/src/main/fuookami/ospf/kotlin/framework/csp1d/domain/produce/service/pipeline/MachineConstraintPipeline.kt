package fuookami.ospf.kotlin.framework.csp1d.domain.produce.service.pipeline

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.framework.model.Pipeline
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Machine
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.MachineBatchShadowPriceKey
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.MachineCapacityShadowPriceKey
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Csp1dShadowPriceKey
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.ProduceAggregation

/**
 * 设备约束管线 / Machine constraint pipeline
 *
 * 为每个设备添加两类约束：
 * - 批次数约束：sum(x_j where plan_j.machineId == machine.id) <= maxBatchCount
 * - 产能约束：sum(consumption_j * x_j where plan_j.machineId == machine.id) <= capacity
 *
 * Add two types of constraints for each machine:
 * - Batch count constraint: sum(x_j where plan_j.machineId == machine.id) <= maxBatchCount
 * - Capacity constraint: sum(consumption_j * x_j where plan_j.machineId == machine.id) <= capacity
 *
 * @param V 数值类型 / Numeric value type
 * @property produce 产出聚合 / Produce aggregation
 * @property machines 设备列表 / Machine list
 * @property shadowPriceKeys 约束名到影子价格键的映射 / Constraint name to shadow price key mapping
 */
class MachineConstraintPipeline<V : RealNumber<V>>(
    private val produce: ProduceAggregation<V>,
    private val machines: List<Machine<V>>,
    private val shadowPriceKeys: MutableMap<String, Csp1dShadowPriceKey>? = null
) : Pipeline<LinearMetaModel<Flt64>> {

    override val name: String = "machine_constraint"

    override fun invoke(model: LinearMetaModel<Flt64>): Try {
        for ((machineIndex, machine) in machines.withIndex()) {
            addMachineBatchConstraint(model, machineIndex, machine)
            addMachineCapacityConstraint(model, machineIndex, machine)
        }
        return ok
    }

    private fun addMachineBatchConstraint(
        model: LinearMetaModel<Flt64>,
        machineIndex: Int,
        machine: Machine<V>
    ) {
        val maxBatchCount = machine.maxBatchCount ?: return
        val lhs = LinearPolynomial(
            monomials = produce.cuttingPlans.mapIndexedNotNull { index, plan ->
                if (plan.machineId != machine.id) return@mapIndexedNotNull null
                LinearMonomial(Flt64.one, produce[index])
            },
            constant = Flt64.zero
        )
        if (lhs.monomials.isEmpty()) return

        val constraintName = "machine_batch_$machineIndex"
        shadowPriceKeys?.set(
            constraintName,
            MachineBatchShadowPriceKey(machine.id)
        )
        model.addConstraint(
            relation = LinearInequality(
                lhs = lhs,
                rhs = DemandConstraintPipeline.constantPolynomial(maxBatchCount.toFlt64()),
                comparison = Comparison.LE
            ),
            name = constraintName
        )
    }

    private fun addMachineCapacityConstraint(
        model: LinearMetaModel<Flt64>,
        machineIndex: Int,
        machine: Machine<V>
    ) {
        val capacity = machine.capacity ?: return
        val lhs = LinearPolynomial(
            monomials = produce.cuttingPlans.mapIndexedNotNull { index, plan ->
                if (plan.machineId != machine.id) return@mapIndexedNotNull null
                val consumption = machineCapacityConsumptionValue(plan, machine) ?: return@mapIndexedNotNull null
                LinearMonomial(consumption.toFlt64(), produce[index])
            },
            constant = Flt64.zero
        )
        if (lhs.monomials.isEmpty()) return

        val constraintName = "machine_capacity_$machineIndex"
        shadowPriceKeys?.set(
            constraintName,
            MachineCapacityShadowPriceKey(machine.id)
        )
        model.addConstraint(
            relation = LinearInequality(
                lhs = lhs,
                rhs = DemandConstraintPipeline.constantPolynomial(capacity.value.toFlt64()),
                comparison = Comparison.LE
            ),
            name = constraintName
        )
    }

    private fun machineCapacityConsumptionValue(
        plan: fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlan<V>,
        machine: Machine<V>
    ): V? {
        val capacity = machine.capacity ?: return null
        val consumption = plan.capacityConsumption ?: return null
        if (consumption.unit != capacity.unit) return null
        if (consumption.value <= consumption.value.constants.zero) return null
        return consumption.value
    }
}
