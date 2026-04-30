@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model

import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModelFlt64
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.Capacity
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.ProductionAction
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeSlot
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * Plan 模式的产能调度产品产量管�?
 * Plan-mode produce management for Capacity Scheduling
 *
 * 用于非列生成场景，在构造时绑定 Capacity 编译对象
 * Used for non-column generation scenarios, binds to Capacity compilation object at construction
 *
 * @param products 产品列表及其需�?/ Product list with demands
 * @param compilation Capacity 编译对象 / Capacity compilation object
 */
class PlanCapacitySchedulingProduce<
        A : ProductionAction,
        P : AbstractMaterial,
        C : AbstractMaterial
        >(
    products: List<Pair<P, MaterialDemand?>>,
    private val compilation: Capacity<A>,
    actions: List<A>,
    slots: List<TimeSlot>,
    timeWindow: TimeWindow
) : CapacitySchedulingProduce<A, P, C>(products, actions, slots, timeWindow) {

    init {
        // 在构造时绑定编译对象
        // Bind compilation object at construction
        for ((product, _) in products) {
            for (action in actions) {
                if (action is CapacityActionProduce<*, *>) {
                    @Suppress("UNCHECKED_CAST")
                    val unitProduce = (action as CapacityActionProduce<P, *>).produce[product] ?: Flt64.zero
                    if (unitProduce neq Flt64.zero) {
                        for ((s, _) in slots.withIndex()) {
                            val actionIndex = actions.indexOf(action)
                            if (actionIndex >= 0) {
                                quantity[product].asMutable() += LinearMonomial(unitProduce, compilation.operationTime[actionIndex, s])
                            }
                        }
                    }
                }
            }
        }
    }

    override fun register(model: LinearMetaModelFlt64): Try {
        return addQuantityToModel(model)
    }
}



