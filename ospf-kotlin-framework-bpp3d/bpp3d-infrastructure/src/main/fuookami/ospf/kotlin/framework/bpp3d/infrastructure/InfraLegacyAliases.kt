@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity

typealias InfraScalar = Flt64
typealias InfraQuantity = Quantity<InfraScalar>
typealias InfraCuboid = AbstractCuboid<InfraScalar>

fun infraZero(): InfraScalar = Flt64.zero
fun infraOne(): InfraScalar = Flt64.one
fun infraInfinity(): InfraScalar = Flt64.infinity
fun infraNegativeInfinity(): InfraScalar = Flt64.negativeInfinity
fun infraEpsilon(): InfraScalar = Flt64.epsilon

fun infraScalar(value: UInt64): InfraScalar = Flt64(value.toULong().toDouble())
fun infraScalar(value: Double): InfraScalar = Flt64(value)
