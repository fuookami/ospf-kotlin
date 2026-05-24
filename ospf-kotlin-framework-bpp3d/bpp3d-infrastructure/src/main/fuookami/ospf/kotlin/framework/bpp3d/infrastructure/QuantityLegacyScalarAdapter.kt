@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.number.toFlt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.toFlt64

/**
 * 标量兼容适配器。
 * Scalar compatibility adapter.
 *
 * 将 Quantity/UInt64 到 Flt64 的转换集中在兼容层，避免业务主链路直接调用 toFlt64()。
 * Centralizes Quantity/UInt64 to Flt64 conversions in the compatibility layer,
 * so business-domain main paths do not call toFlt64() directly.
 */
fun <V : RealNumber<V>> Quantity<V>.asScalarF64(): Quantity<Flt64> = this.toFlt64()

fun Quantity<Flt64>.asScalarF64(): Flt64 = this.value

fun UInt64.asScalarF64(): Flt64 = this.toFlt64()
