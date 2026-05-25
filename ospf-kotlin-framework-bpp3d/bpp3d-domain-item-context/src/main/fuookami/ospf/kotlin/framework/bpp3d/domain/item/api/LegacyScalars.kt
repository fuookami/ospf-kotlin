@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.bpp3d.domain.item.api

import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.AbstractCuboid
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity

typealias LegacyScalar = Flt64
typealias LegacyQuantity = Quantity<LegacyScalar>
typealias LegacyCuboid = AbstractCuboid<LegacyScalar>

fun legacyZero(): LegacyScalar = Flt64.zero
fun legacyOne(): LegacyScalar = Flt64.one
fun legacyTwo(): LegacyScalar = Flt64.two
fun legacyInfinity(): LegacyScalar = Flt64.infinity
fun legacyNegativeInfinity(): LegacyScalar = Flt64.negativeInfinity

fun legacyScalar(value: UInt64): LegacyScalar = Flt64(value.toULong().toDouble())
fun legacyScalar(value: Int): LegacyScalar = Flt64(value.toDouble())
fun legacyScalar(value: Long): LegacyScalar = Flt64(value.toDouble())
fun legacyScalar(value: Double): LegacyScalar = Flt64(value)
