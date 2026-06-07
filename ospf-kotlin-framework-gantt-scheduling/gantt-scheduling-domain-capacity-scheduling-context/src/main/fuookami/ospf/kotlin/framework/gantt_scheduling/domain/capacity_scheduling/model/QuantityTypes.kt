package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity

/** 产能物理量 / Capacity quantity */
typealias CapacityQuantity<V> = Quantity<V>

/** 产能成本物理量 / Capacity cost quantity */
typealias CapacityCostQuantity<V> = Quantity<V>

/** Flt64 产能物理量兼容类型 / Flt64 capacity quantity compatibility type */
typealias Flt64CapacityQuantity = CapacityQuantity<Flt64>

/** Flt64 产能成本物理量兼容类型 / Flt64 capacity cost quantity compatibility type */
typealias Flt64CapacityCostQuantity = CapacityCostQuantity<Flt64>
