@file:Suppress("DEPRECATION")

/**
 * 基础设施数值类型别名。
 * Infrastructure numeric type aliases.
 */
package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.math.algebra.number.FltX as InfraBaseFloating
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity

typealias InfraNumber = InfraBaseFloating
typealias InfraQuantity = Quantity<InfraNumber>
typealias InfraCuboid = AbstractCuboid<InfraNumber>

fun infraZero(): InfraNumber = InfraNumber.zero
fun infraOne(): InfraNumber = InfraNumber.one
fun infraInfinity(): InfraNumber = InfraNumber.maximum
fun infraNegativeInfinity(): InfraNumber = InfraNumber.minimum
fun infraEpsilon(): InfraNumber = InfraNumber.epsilon

fun infraScalar(value: UInt64): InfraNumber = InfraNumber(value.toULong().toDouble())
fun infraScalar(value: Double): InfraNumber = InfraNumber(value)
