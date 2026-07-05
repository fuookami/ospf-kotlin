/**
 * 物料装箱编号。
 * Material packing numbers.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model

import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*

/**
 * 物料包装标量零值。
 * Zero value for material packing scalar.
 * @return 零值物料包装标量
 */
fun materialPackingZero(): FltX = FltX.zero

/**
 * 物料包装标量一值。
 * One value for material packing scalar.
 * @return 一值物料包装标量
 */
fun materialPackingOne(): FltX = FltX.one

/**
 * 以 double 构造物料包装标量。
 * Build material packing scalar from double.
 * @param value 双精度浮点数值
 * @return 构造的物料包装标量
 */
fun materialPackingScalar(value: Double): FltX = FltX(value)
