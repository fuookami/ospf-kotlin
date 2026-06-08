package fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model

import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraOne
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraScalar
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraZero

/**
 * 物料包装标量零值。
 * Zero value for material packing scalar.
 */
fun materialPackingZero(): InfraNumber = infraZero()

/**
 * 物料包装标量一值。
 * One value for material packing scalar.
 */
fun materialPackingOne(): InfraNumber = infraOne()

/**
 * 以 double 构造物料包装标量。
 * Build material packing scalar from double.
 */
fun materialPackingScalar(value: Double): InfraNumber = infraScalar(value)
