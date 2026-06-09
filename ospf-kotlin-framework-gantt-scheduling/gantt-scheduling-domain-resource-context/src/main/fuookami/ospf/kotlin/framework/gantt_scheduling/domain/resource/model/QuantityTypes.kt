package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.quantities.quantity.Quantity

/** 资源数量物理量 / Resource quantity */
typealias ResourceQuantity<V> = Quantity<V>

/** 资源数量范围物理量 / Resource quantity range */
typealias ResourceQuantityRange<V> = Quantity<ValueRange<V>>

/** 资源成本物理量 / Resource cost quantity */
typealias ResourceCostQuantity<V> = Quantity<V>

/** Flt64 资源数量物理量兼容类型 / Flt64 resource quantity compatibility type */
@Deprecated(
    message = "Use ResourceQuantity<Flt64> directly",
    replaceWith = ReplaceWith("ResourceQuantity<Flt64>")
)
typealias Flt64ResourceQuantity = ResourceQuantity<Flt64>

/** Flt64 资源数量范围物理量兼容类型 / Flt64 resource quantity range compatibility type */
@Deprecated(
    message = "Use ResourceQuantityRange<Flt64> directly",
    replaceWith = ReplaceWith("ResourceQuantityRange<Flt64>")
)
typealias Flt64ResourceQuantityRange = ResourceQuantityRange<Flt64>

/** Flt64 资源成本物理量兼容类型 / Flt64 resource cost quantity compatibility type */
@Deprecated(
    message = "Use ResourceCostQuantity<Flt64> directly",
    replaceWith = ReplaceWith("ResourceCostQuantity<Flt64>")
)
typealias Flt64ResourceCostQuantity = ResourceCostQuantity<Flt64>
