/**
 * Bunch 模式产能调度生产 / Bunch-mode capacity scheduling produce
 *
 * 本文件定义 BunchCapacitySchedulingProduce 类，用于列生成场景下的产品产量管理。
 * This file defines BunchCapacitySchedulingProduce class for product output management in column generation scenarios.
 */
@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeSlot
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow

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
        C : AbstractMaterial,
        V
        >(
    products: List<Pair<P, MaterialDemand<V>?>>,
    actions: List<A>,
    slots: List<TimeSlot>,
    timeWindow: TimeWindow<V>
) : CapacitySchedulingProduce<A, P, C, V>(products, actions, slots, timeWindow)
        where V : RealNumber<V>, V : NumberField<V> {

    override fun register(model: LinearMetaModel<Flt64>): Try {
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
    @Suppress("UNUSED_PARAMETER")
    suspend fun addColumns(
        iteration: UInt64,
        columns: List<CapacityColumn<E, A, V>>,
        compilation: IterativeCapacityCompilation<V, E, A>
    ): Try {
        // Rebuild from operationTime to keep consistency when iterative x variables are reshaped.
        // 基于 operationTime 重建，避免迭代扩容后 x 变量重建导致的表达式引用失配。
        for ((product, _) in products) {
            quantity[product].asMutable().let {
                it.clear()
                it.setConstant(Flt64.zero)
            }
            for ((actionIndex, action) in actions.withIndex()) {
                val unitProduce = unitProduceMapOf<P, V>(action)?.get(product) ?: continue
                if (unitProduce eq unitProduce.constants.zero) {
                    continue
                }
                for ((slotIndex, _) in slots.withIndex()) {
                    quantity[product].asMutable() += LinearMonomial(unitProduce.toSolverValue(), compilation.operationTime[actionIndex, slotIndex])
                }
            }
        }
        return ok
    }
}
