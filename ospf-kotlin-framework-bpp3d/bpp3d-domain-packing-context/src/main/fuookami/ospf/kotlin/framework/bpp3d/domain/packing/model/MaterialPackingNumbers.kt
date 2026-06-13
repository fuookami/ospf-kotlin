package fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model

import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.fltXOne
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.fltX
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.fltXZero

/**
 * 物料包装标量零值。
 * Zero value for material packing scalar.
 */
fun materialPackingZero(): FltX = fltXZero()

/**
 * 物料包装标量一值。
 * One value for material packing scalar.
 */
fun materialPackingOne(): FltX = fltXOne()

/**
 * 以 double 构造物料包装标量。
 * Build material packing scalar from double.
 */
fun materialPackingScalar(value: Double): FltX = fltX(value)
