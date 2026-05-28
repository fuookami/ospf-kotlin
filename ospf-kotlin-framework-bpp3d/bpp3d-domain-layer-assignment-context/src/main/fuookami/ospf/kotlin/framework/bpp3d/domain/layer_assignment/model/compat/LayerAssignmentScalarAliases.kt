package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.compat

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.LegacyScalar
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.legacyOne
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.legacyZero
import fuookami.ospf.kotlin.math.algebra.number.Flt64

typealias LayerAssignmentCompatScalar = LegacyScalar

fun layerAssignmentCompatOne(): LayerAssignmentCompatScalar = legacyOne()
fun layerAssignmentCompatZero(): LayerAssignmentCompatScalar = legacyZero()
fun layerAssignmentCompatProvider() = Flt64
