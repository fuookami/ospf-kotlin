@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.compat

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.number.toFlt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.toFlt64

/**
 * Scalar compatibility adapter at facade boundary.
 * 仅用于兼容 facade / solver 边界，不应用于业务主链路。
 */
fun <V : RealNumber<V>> Quantity<V>.asScalarF64(): Quantity<Flt64> = this.toFlt64()

fun Quantity<Flt64>.asScalarF64(): Flt64 = this.value

fun UInt64.asScalarF64(): Flt64 = this.toFlt64()
