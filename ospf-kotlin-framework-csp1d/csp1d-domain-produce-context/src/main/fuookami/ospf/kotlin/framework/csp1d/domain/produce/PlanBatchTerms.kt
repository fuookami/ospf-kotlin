package fuookami.ospf.kotlin.framework.csp1d.domain.produce

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.*

/** 单批次按需求、物料、设备分发的项，保持 plan 顺序和需求首匹配。 / Batch terms dispatched by demand/material/machine, preserving plan order and first demand matches. */
internal class PlanBatchTerms<V : RealNumber<V>>(
    private val plans: List<CuttingPlan<V>>,
    private val batch: LinearExpressionSymbols1<Flt64>,
    demands: List<ProductDemand<V>>
) {
    val demandTerms = LinkedHashMap<Pair<ProductId, PhysicalUnit>, MutableList<LinearMonomial<Flt64>>>()
    val materialTerms = LinkedHashMap<MaterialId, MutableList<LinearMonomial<Flt64>>>()
    val machineTerms = LinkedHashMap<MachineId?, MutableList<LinearMonomial<Flt64>>>()
    private val machinePlans = LinkedHashMap<MachineId?, MutableList<Int>>()

    init {
        val wanted = demands.map { it.product.id to it.quantity.unit }.toSet()
        for ((index, plan) in plans.withIndex()) {
            materialTerms.getOrPut(plan.material.id) { ArrayList() }.add(Flt64.one * batch[index])
            machineTerms.getOrPut(plan.machineId) { ArrayList() }.add(Flt64.one * batch[index])
            machinePlans.getOrPut(plan.machineId) { ArrayList() }.add(index)
            val seen = HashSet<Pair<ProductId, PhysicalUnit>>()
            for (contribution in plan.demandContributions) {
                val key = contribution.product.id to contribution.quantity.unit
                if (key in wanted && seen.add(key)) {
                    demandTerms.getOrPut(key) { ArrayList() }.add(contribution.quantity.value.toFlt64() * batch[index])
                }
            }
        }
    }

    fun capacityTerms(machine: Machine<V>): List<LinearMonomial<Flt64>> {
        val capacity = machine.capacity ?: return emptyList()
        return machinePlans[machine.id].orEmpty().mapNotNull { index ->
            val consumption = plans[index].capacityConsumption ?: return@mapNotNull null
            if (consumption.unit != capacity.unit || consumption.value leq consumption.value.constants.zero) {
                return@mapNotNull null
            }
            consumption.value.toFlt64() * batch[index]
        }
    }
}
