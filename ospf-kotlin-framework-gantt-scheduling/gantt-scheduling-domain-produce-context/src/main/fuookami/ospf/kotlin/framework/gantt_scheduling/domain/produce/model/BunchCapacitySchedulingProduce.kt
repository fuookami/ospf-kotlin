@file:Suppress("DEPRECATION")

@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model

import fuookami.ospf.kotlin.core.expression.polynomial.times
import fuookami.ospf.kotlin.core.intermediate_model.LinearMetaModel
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.CapacityColumn
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.IterativeCapacityCompilation
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.ProductionAction
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeSlot
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64

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
     * �?IterativeCapacityCompilation 添加列贡�?
     * Add column contribution from IterativeCapacityCompilation
     *
     * 用于列生成场景，在每次迭代中添加新列的产量贡�?
     * Used for column generation, adds produce contribution from new columns in each iteration
     *
     * @param iteration 当前迭代 / Current iteration
     * @param columns 产能列列�?/ Capacity columns
     * @param compilation 迭代编译对象 / Iterative compilation object
     * @return 成功与否 / Success or failure
     */
    @Suppress("UNUSED_PARAMETER")
    suspend fun addColumns(
        iteration: UInt64,
        columns: List<CapacityColumn<E, A>>,
        compilation: IterativeCapacityCompilation<E, A>
    ): Try {
        // Rebuild from operationTime to keep consistency when iterative x variables are reshaped.
        // 基于 operationTime 重建，避免迭代扩容后 x 变量重建导致的表达式引用失配�?
        for ((product, _) in products) {
            quantity[product].asMutable().let {
                it.monomials.clear()
                it.constant = Flt64.zero
            }
            for ((actionIndex, action) in actions.withIndex()) {
                if (action !is CapacityActionProduce<*, *>) {
                    continue
                }
                @Suppress("UNCHECKED_CAST")
                val unitProduce = (action as CapacityActionProduce<P, *>).produce[product] ?: Flt64.zero
                if (unitProduce eq Flt64.zero) {
                    continue
                }
                for ((slotIndex, _) in slots.withIndex()) {
                    quantity[product].asMutable() += unitProduce * compilation.operationTime[actionIndex, slotIndex].toLinearPolynomial()
                }
            }
        }
        return ok
    }
}



