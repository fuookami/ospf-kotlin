/**
 * 物料数量类型定义 / Material quantity type definitions
 *
 * 本文件定义物料数量相关的类型别名，用于表示物理量及范围。
 * This file defines type aliases for material quantities, representing physical quantities and ranges.
 */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model

import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.quantities.quantity.Quantity

/** 物料数量物理量 / Material quantity */
typealias MaterialQuantity<V> = Quantity<V>

/** 物料数量范围物理量 / Material quantity range */
typealias MaterialQuantityRange<V> = Quantity<ValueRange<V>>
