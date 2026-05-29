@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.bpp3d.domain.item.api

import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.ItemCuboid as ModelItemCuboid
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraInfinity
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraNegativeInfinity
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraOne
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraScalar
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraZero
import fuookami.ospf.kotlin.math.algebra.number.UInt64





typealias ItemCuboid = ModelItemCuboid

fun itemZero(): InfraNumber = infraZero()
fun itemOne(): InfraNumber = infraOne()
fun itemTwo(): InfraNumber = infraOne() + infraOne()
fun itemInfinity(): InfraNumber = infraInfinity()
fun itemNegativeInfinity(): InfraNumber = infraNegativeInfinity()

fun itemScalar(value: UInt64): InfraNumber = infraScalar(value)
fun itemScalar(value: Int): InfraNumber = infraScalar(value.toDouble())
fun itemScalar(value: Long): InfraNumber = infraScalar(value.toDouble())
fun itemScalar(value: Double): InfraNumber = infraScalar(value)

