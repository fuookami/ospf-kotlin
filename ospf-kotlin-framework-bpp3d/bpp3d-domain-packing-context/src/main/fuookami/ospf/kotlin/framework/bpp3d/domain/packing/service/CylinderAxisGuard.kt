@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.bpp3d.domain.packing.service

import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.CylinderPackingShape3
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackingShape3
import fuookami.ospf.kotlin.math.geometry.Axis3

internal fun requireVerticalCylinderAxis(
    shape: PackingShape3<InfraNumber>,
    source: String
) {
    if (shape is CylinderPackingShape3 && shape.axis != Axis3.Y) {
        throw IllegalArgumentException(
            "Unsupported cylinder axis in $source: only Axis3.Y is allowed, but got ${shape.axis}."
        )
    }
}
