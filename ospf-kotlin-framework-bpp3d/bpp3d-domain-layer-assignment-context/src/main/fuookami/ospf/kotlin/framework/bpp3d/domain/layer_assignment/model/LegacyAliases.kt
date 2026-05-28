package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model

import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraOne
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraZero

fun layerAssignmentOne(): InfraNumber = infraOne()
fun layerAssignmentZero(): InfraNumber = infraZero()
fun layerAssignmentScalarProvider() = InfraNumber
