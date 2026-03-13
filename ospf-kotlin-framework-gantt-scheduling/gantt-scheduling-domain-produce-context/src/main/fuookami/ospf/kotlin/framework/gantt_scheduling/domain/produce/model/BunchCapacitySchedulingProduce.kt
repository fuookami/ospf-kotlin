package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.*

/**
 * Bunch 模式的产能调度产品产量管理（支持列生成）
 * Bunch-mode produce management for Capacity Scheduling (with column generation)
 *
 * 用于列生成场景，通过 CapacityColumn 追加产量贡献
 * Used for column generation scenarios, adds produce contribution through CapacityColumn
 */
class BunchCapacitySchedulingProduce<
    E : Executor,
    A : ProductionAction,
    P : AbstractMaterial,
    C : AbstractMaterial
>(
    products: List<Pair<P, MaterialDemand?>>,
    actions: List<A>,
    slots: List<TimeSlot>,
    timeWindow: TimeWindow
) : CapacitySchedulingProduce<A, P, C>(products, actions, slots, timeWindow) {

    override fun register(model: LinearMetaModel): Try {
        return addQuantityToModel(model)
    }

    /**
     * 从 IterativeCapacityCompilation 添加列贡献
     * Add column contribution from IterativeCapacityCompilation
     *
     * 用于列生成场景，在每次迭代中添加新列的产量贡献
     * Used for column generation, adds produce contribution from new columns in each iteration
     *
     * @param iteration 当前迭代 / Current iteration
     * @param columns 产能列列表 / Capacity columns
     * @param compilation 迭代编译对象 / Iterative compilation object
     * @return 成功与否 / Success or failure
     */
    suspend fun addColumns(
        iteration: UInt64,
        columns: List<CapacityColumn<E, A>>,
        compilation: IterativeCapacityCompilation<A>
    ): Try {
        for ((product, _) in products) {
            for (column in columns) {
                for ((action, amount) in column.allocations) {
                    if (action is CapacityActionProduce<*, *>) {
                        @Suppress("UNCHECKED_CAST")
                        val unitProduce = (action as CapacityActionProduce<P, *>).produce[product] ?: Flt64.zero
                        if (unitProduce neq Flt64.zero) {
                            val actionIndex = actions.indexOf(action)
                            if (actionIndex >= 0) {
                                val produceAmount = unitProduce * amount.toFlt64()
                                quantity[product].asMutable() += produceAmount * compilation.x[actionIndex, column.slotIndex]
                            }
                        }
                    }
                }
            }
        }
        return ok
    }
}
