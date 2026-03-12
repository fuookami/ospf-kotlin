package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.*

/**
 * 支持 ProductionAction 的产量/消耗接口
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
 * 从 CapacityColumn 计算产量
 * Calculate produce from CapacityColumn
 *
 * @param product 产品 / Product
 * @return 产量 / Produce amount
 */
fun <E : Executor, A : ProductionAction, P : AbstractMaterial> 
    CapacityColumn<E, A>.produce(product: P): Flt64 {
    return allocations.mapNotNull { (action, amount) ->
        when (action) {
            is CapacityActionProduce<*, *> -> {
                (action as CapacityActionProduce<P, *>).produce[product]?.let { 
                    it * Flt64(amount.toDouble()) 
                }
            }
            else -> null
        }
    }.sumOf { it }
}

/**
 * 从 CapacityColumn 计算消耗
 * Calculate consumption from CapacityColumn
 *
 * @param material 原料 / Material
 * @return 消耗量 / Consumption amount
 */
fun <E : Executor, A : ProductionAction, C : AbstractMaterial> 
    CapacityColumn<E, A>.consumption(material: C): Flt64 {
    return allocations.mapNotNull { (action, amount) ->
        when (action) {
            is CapacityActionProduce<*, *> -> {
                (action as CapacityActionProduce<*, C>).consumption[material]?.let { 
                    it * Flt64(amount.toDouble()) 
                }
            }
            else -> null
        }
    }.sumOf { it }
}