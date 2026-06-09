package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.quantities.quantity.Quantity

/** 物料数量物理量 / Material quantity */
typealias MaterialQuantity<V> = Quantity<V>

/** 物料数量范围物理量 / Material quantity range */
typealias MaterialQuantityRange<V> = Quantity<ValueRange<V>>

/** Flt64 物料数量物理量兼容类型 / Flt64 material quantity compatibility type */
@Deprecated(
    message = "Use MaterialQuantity<Flt64> directly",
    replaceWith = ReplaceWith("MaterialQuantity<Flt64>")
)
typealias Flt64MaterialQuantity = MaterialQuantity<Flt64>

/** Flt64 物料数量范围物理量兼容类型 / Flt64 material quantity range compatibility type */
@Deprecated(
    message = "Use MaterialQuantityRange<Flt64> directly",
    replaceWith = ReplaceWith("MaterialQuantityRange<Flt64>")
)
typealias Flt64MaterialQuantityRange = MaterialQuantityRange<Flt64>
