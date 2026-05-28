package fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model.compat

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity

/**
 * 物料包装主路径标量别名（兼容边界）。
 * Material packing main-path scalar alias (compatibility boundary).
 */
typealias MaterialPackingScalar = Flt64

/**
 * 物料包装主路径数量别名（兼容边界）。
 * Material packing main-path quantity alias (compatibility boundary).
 */
typealias MaterialPackingQuantity = Quantity<MaterialPackingScalar>

/**
 * 物料包装标量零值。
 * Zero value for material packing scalar.
 */
fun materialPackingZero(): MaterialPackingScalar = Flt64.zero

/**
 * 物料包装标量一值。
 * One value for material packing scalar.
 */
fun materialPackingOne(): MaterialPackingScalar = Flt64.one

/**
 * 从 double 构造物料包装标量。
 * Build material packing scalar from double.
 */
fun materialPackingScalar(value: Double): MaterialPackingScalar = Flt64(value)
