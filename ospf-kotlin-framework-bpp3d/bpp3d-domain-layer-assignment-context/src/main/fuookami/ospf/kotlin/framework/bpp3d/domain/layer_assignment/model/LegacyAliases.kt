package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.LegacyScalar
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.legacyOne
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.legacyZero

typealias LayerAssignmentScalar = LegacyScalar

fun layerAssignmentOne(): LayerAssignmentScalar = legacyOne()
fun layerAssignmentZero(): LayerAssignmentScalar = legacyZero()
