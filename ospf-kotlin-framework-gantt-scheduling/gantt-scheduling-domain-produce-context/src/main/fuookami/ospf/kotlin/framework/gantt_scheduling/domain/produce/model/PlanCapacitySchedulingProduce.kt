/**
 * Plan 模式产能调度生产 / Plan-mode capacity scheduling produce
 *
 * 本文件定义 PlanCapacitySchedulingProduce 类，用于非列生成场景下的产品产量管理。
 * This file defines PlanCapacitySchedulingProduce class for product output management in non-column generation scenarios.
*/
@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.toSolverValue
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeSlot
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow

/**
 * Plan 模式的产能调度产品产量管理
 * Plan-mode produce management for Capacity Scheduling
 *
 * 用于非列生成场景，在构造时绑定 Capacity 编译对象
 * Used for non-column generation scenarios, binds to Capacity compilation object at construction
 *
 * @param products 产品列表及其需求 / Product list with demands
 * @param compilation Capacity 编译对象 / Capacity compilation object
*/
class PlanCapacitySchedulingProduce<
        A : ProductionAction,
        P : AbstractMaterial,
        C : AbstractMaterial,
        V
        >(
    products: List<Pair<P, MaterialDemand<V>?>>,
    private val compilation: Capacity<A>,
    actions: List<A>,
    slots: List<TimeSlot>,
    timeWindow: TimeWindow<V>
) : CapacitySchedulingProduce<A, P, C, V>(products, actions, slots, timeWindow)
        where V : RealNumber<V>, V : NumberField<V> {

    init {
        // 在构造时绑定编译对象
        // Bind compilation object at construction
        for ((product, _) in products) {
            for (action in actions) {
                val unitProduce = unitProduceMapOf<P, V>(action)?.get(product) ?: continue
                if (unitProduce neq unitProduce.constants.zero) {
                    for ((s, _) in slots.withIndex()) {
                        val actionIndex = actions.indexOf(action)
                        if (actionIndex >= 0) {
                            quantity[product].asMutable() += LinearMonomial(unitProduce.toSolverValue(), compilation.operationTime[actionIndex, s])
                        }
                    }
                }
            }
        }
    }

    override fun register(model: LinearMetaModel<Flt64>): Try {
        return addQuantityToModel(model)
    }
}
