/** 资源数量类型别名：物理量与成本量类型定义 / Resource quantity type aliases: physical quantity and cost quantity type definitions */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model

import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.quantities.quantity.*

/**
 * 资源数量物理量类型别名 / Resource quantity typealias
 *
 * 用于表示资源的数量物理量 / Used to represent resource quantity as a physical quantity
*/
typealias ResourceQuantity<V> = Quantity<V>

/**
 * 资源数量范围物理量类型别名 / Resource quantity range typealias
 *
 * 用于表示资源的数量范围物理量 / Used to represent resource quantity range as a physical quantity
*/
typealias ResourceQuantityRange<V> = Quantity<ValueRange<V>>

/**
 * 资源成本物理量类型别名 / Resource cost quantity typealias
 *
 * 用于表示资源的成本物理量 / Used to represent resource cost as a physical quantity
*/
typealias ResourceCostQuantity<V> = Quantity<V>
