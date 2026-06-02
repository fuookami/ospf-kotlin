@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model

import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.CapacityColumn
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.ProductionAction
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
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
        C : AbstractMaterial,
        V : RealNumber<V>
        > {
    /**
     * 生产动作对应的产品产量（单位操作时间的产量）
     * Product produce per unit operation time
     */
    val produce: Map<P, V>

    /**
     * 生产动作对应的原料消耗（单位操作时间的消耗）
     * Material consumption per unit operation time
     */
    val consumption: Map<C, V>
}

/** Flt64 生产动作产出消耗类型别名 / Flt64 production action produce-consumption type alias */
typealias Flt64CapacityActionProduce<P, C> = CapacityActionProduce<P, C, Flt64>

/**
 * 按产品方向读取生产动作的单位产量映射。
 * 生产动作与产出物料类型由对应 column/produce 构造路径绑定。
 *
 * Reads the unit-produce map from a production action for product-side lookup.
 * Production action material types are bound by the corresponding column/produce construction path.
 */
@Suppress("UNCHECKED_CAST")
fun <P : AbstractMaterial, V : RealNumber<V>> unitProduceMapOf(
    action: ProductionAction
): Map<P, V>? {
    val produceAction = action as? CapacityActionProduce<*, *, *> ?: return null
    return produceAction.produce as Map<P, V>
}

/**
 * 按消耗方向读取生产动作的单位消耗映射。
 * 生产动作与消耗物料类型由对应 column/produce 构造路径绑定。
 *
 * Reads the unit-consumption map from a production action for material-side lookup.
 * Production action material types are bound by the corresponding column/produce construction path.
 */
@Suppress("UNCHECKED_CAST")
fun <C : AbstractMaterial, V : RealNumber<V>> unitConsumptionMapOf(
    action: ProductionAction
): Map<C, V>? {
    val produceAction = action as? CapacityActionProduce<*, *, *> ?: return null
    return produceAction.consumption as Map<C, V>
}

/**
 * �?CapacityColumn 计算产量
 * Calculate produce from CapacityColumn
 *
 * @param product 产品 / Product
 * @return 产量 / Produce amount
 */
fun <E : Executor, A : ProductionAction, P : AbstractMaterial>
        CapacityColumn<E, A, Flt64>.produce(product: P): Flt64 {
    var result = Flt64.zero
    for ((action, amount) in allocations) {
        val unitProduce = unitProduceMapOf<P, Flt64>(action)?.get(product) ?: Flt64.zero
        result += unitProduce * amount.toFlt64()
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
        CapacityColumn<E, A, Flt64>.consumption(material: C): Flt64 {
    var result = Flt64.zero
    for ((action, amount) in allocations) {
        val unitConsumption = unitConsumptionMapOf<C, Flt64>(action)?.get(material) ?: Flt64.zero
        result += unitConsumption * amount.toFlt64()
    }
    return result
}
