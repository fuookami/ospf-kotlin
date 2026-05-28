@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity

typealias InfraNumber = Flt64
typealias InfraQuantity = Quantity<InfraNumber>
typealias InfraCuboid = AbstractCuboid<InfraNumber>

fun infraZero(): InfraNumber = Flt64.zero
fun infraOne(): InfraNumber = Flt64.one
fun infraInfinity(): InfraNumber = Flt64.infinity
fun infraNegativeInfinity(): InfraNumber = Flt64.negativeInfinity
fun infraEpsilon(): InfraNumber = Flt64.epsilon

fun infraScalar(value: UInt64): InfraNumber = Flt64(value.toULong().toDouble())
fun infraScalar(value: Double): InfraNumber = Flt64(value)
