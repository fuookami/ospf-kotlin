package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model

import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.compat.LayerAssignmentCompatScalar
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.compat.layerAssignmentCompatOne
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.compat.layerAssignmentCompatProvider
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.compat.layerAssignmentCompatZero

typealias LayerAssignmentScalar = LayerAssignmentCompatScalar

fun layerAssignmentOne(): LayerAssignmentScalar = layerAssignmentCompatOne()
fun layerAssignmentZero(): LayerAssignmentScalar = layerAssignmentCompatZero()
fun layerAssignmentScalarProvider() = layerAssignmentCompatProvider()
