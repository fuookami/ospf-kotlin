package fuookami.ospf.kotlin.framework.csp1d.domain.produce

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlan
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Machine
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ProductDemand
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.CuttingPlanUsage
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.MachineCapacityUsage
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.MaterialUsage
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.Produce

/**
 * 主问题输入 / Master problem input
 *
 * @param V 数值类型 / Numeric value type
 * @property candidatePlans 候选切割方案 / Candidate cutting plans
 * @property demands 产品需求 / Product demands
 * @property machines 设备列表 / Machines
 */
data class ProduceInput<V : RealNumber<V>>(
    val candidatePlans: List<CuttingPlan<V>>,
    val demands: List<ProductDemand<V>>,
    val machines: List<Machine<V>> = emptyList()
)

/**
 * 主问题抽象求解器 / Master problem abstract solver
 *
 * @param V 数值类型 / Numeric value type
 */
fun interface ProduceSolver<V : RealNumber<V>> {
    /**
     * 求解主问题 / Solve master problem
     *
     * @param input 主问题输入 / Master problem input
     * @return 主问题产出 / Master problem output
     */
    fun solve(input: ProduceInput<V>): Produce<V>
}

/**
 * 最小可用主问题求解器 / Minimal master problem solver
 *
 * @param V 数值类型 / Numeric value type
 */
class SimpleProduceSolver<V : RealNumber<V>> : ProduceSolver<V> {
    override fun solve(input: ProduceInput<V>): Produce<V> {
        val demandProductIds = input.demands.map { it.product.id }.toSet()
        val selectedPlans = input.candidatePlans.filter { plan ->
            plan.demandContributions.any { demandProductIds.contains(it.product.id) }
        }
        val usages = selectedPlans.map { plan ->
            CuttingPlanUsage(
                plan = plan,
                amount = UInt64.one
            )
        }
        val materialUsage = selectedPlans
            .groupBy { it.material.id }
            .values
            .map { plans ->
                MaterialUsage(
                    material = plans.first().material,
                    amount = UInt64(plans.size)
                )
            }
        val machineUsages = input.machines.map { machine ->
            MachineCapacityUsage(
                machine = machine,
                used = machine.capacity
            )
        }
        val producedProductIds = selectedPlans
            .flatMap { it.demandContributions.map { contribution -> contribution.product.id } }
            .toSet()
        val unmetDemands = input.demands.filterNot { producedProductIds.contains(it.product.id) }
        return Produce(
            cuttingPlans = usages,
            materialUsages = materialUsage,
            machineUsages = machineUsages,
            unmetDemands = unmetDemands
        )
    }
}
