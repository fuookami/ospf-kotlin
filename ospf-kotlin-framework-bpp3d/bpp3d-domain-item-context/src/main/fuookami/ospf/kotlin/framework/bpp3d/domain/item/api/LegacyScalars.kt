@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.bpp3d.domain.item.api

import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.AbstractCuboid
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraInfinity
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraNegativeInfinity
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraOne
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraScalar
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraZero
import fuookami.ospf.kotlin.math.algebra.number.UInt64





typealias LegacyCuboid = AbstractCuboid<InfraNumber>

fun legacyZero(): InfraNumber = infraZero()
fun legacyOne(): InfraNumber = infraOne()
fun legacyTwo(): InfraNumber = infraOne() + infraOne()
fun legacyInfinity(): InfraNumber = infraInfinity()
fun legacyNegativeInfinity(): InfraNumber = infraNegativeInfinity()

fun legacyScalar(value: UInt64): InfraNumber = infraScalar(value)
fun legacyScalar(value: Int): InfraNumber = infraScalar(value.toDouble())
fun legacyScalar(value: Long): InfraNumber = infraScalar(value.toDouble())
fun legacyScalar(value: Double): InfraNumber = infraScalar(value)
