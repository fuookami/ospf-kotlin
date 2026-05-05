@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model

import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.CapacityColumn
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.ProductionAction
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * 支持 ProductionAction 的产�?消耗接�?
 * Produce/Consumption interface that supports ProductionAction
 *
 * 此接口定义了生产动作与产品产量、原料消耗之间的关系
 * This interface defines the relationship between production actions and product output/material consumption
 */
interface CapacityActionProduce<
        P : AbstractMaterial,
        C : AbstractMaterial
        > {
    /**
     * 生产动作对应的产品产量（单位操作时间的产量）
     * Product produce per unit operation time
     */
    val produce: Map<P, Flt64>

    /**
     * 生产动作对应的原料消耗（单位操作时间的消耗）
     * Material consumption per unit operation time
     */
    val consumption: Map<C, Flt64>
}

/**
 * �?CapacityColumn 计算产量
 * Calculate produce from CapacityColumn
 *
 * @param product 产品 / Product
 * @return 产量 / Produce amount
 */
fun <E : Executor, A : ProductionAction, P : AbstractMaterial>
        CapacityColumn<E, A>.produce(product: P): Flt64 {
    var result = Flt64.zero
    for ((action, amount) in allocations) {
        if (action is CapacityActionProduce<*, *>) {
            val unitProduce = (action as CapacityActionProduce<P, *>).produce[product] ?: Flt64.zero
            result += unitProduce * amount.toFlt64()
        }
    }
    return result
}

/**
 * �?CapacityColumn 计算消�?
 * Calculate consumption from CapacityColumn
 *
 * @param material 原料 / Material
 * @return 消耗量 / Consumption amount
 */
fun <E : Executor, A : ProductionAction, C : AbstractMaterial>
        CapacityColumn<E, A>.consumption(material: C): Flt64 {
    var result = Flt64.zero
    for ((action, amount) in allocations) {
        if (action is CapacityActionProduce<*, *>) {
            val unitConsumption = (action as CapacityActionProduce<*, C>).consumption[material] ?: Flt64.zero
            result += unitConsumption * amount.toFlt64()
        }
    }
    return result
}



