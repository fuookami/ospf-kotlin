package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model

import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.quantities.quantity.Quantity

/** 物料数量物理量 / Material quantity */
typealias MaterialQuantity<V> = Quantity<V>

/** 物料数量范围物理量 / Material quantity range */
typealias MaterialQuantityRange<V> = Quantity<ValueRange<V>>
