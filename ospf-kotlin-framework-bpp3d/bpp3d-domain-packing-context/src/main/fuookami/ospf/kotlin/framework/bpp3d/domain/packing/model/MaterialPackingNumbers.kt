/**
 * Material packing numbers.
 * 物料装箱编号。
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model

import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*

/**
 * Returns the zero value for material packing scalar.
 * 返回物料包装标量的零值。
 *
 * @return The zero FltX value.
 * 零值 FltX。
 */
fun materialPackingZero(): FltX = FltX.zero

/**
 * Returns the one value for material packing scalar.
 * 返回物料包装标量的一值。
 *
 * @return The one FltX value.
 * 一值 FltX。
 */
fun materialPackingOne(): FltX = FltX.one

/**
 * Creates a material packing scalar from a double value.
 * 从双精度浮点数值构造物料包装标量。
 *
 * @param value The double value to convert.
 * 要转换的双精度浮点数值。
 * @return The FltX scalar value.
 * 构造的 FltX 标量值。
 */
fun materialPackingScalar(value: Double): FltX = FltX(value)
