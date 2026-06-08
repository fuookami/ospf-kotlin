@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.bpp3d.domain.packing.service

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.resolvedPackingShape
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.PackedBin
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.CylinderPackingShape3
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.HorizontalCylinderSupportGeometry
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackingShape3
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.QuantityPoint3
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.horizontalCylinderCuboidSupportCoverage
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.toDouble
import fuookami.ospf.kotlin.math.geometry.Axis3

private const val PackingGeometryOverlapTolerance = 1e-7

private data class PackingGeometry(
    val shape: PackingShape3<InfraNumber>,
    val position: QuantityPoint3
) {
    val minX = position.x.toDouble()
    val minY = position.y.toDouble()
    val minZ = position.z.toDouble()
    val maxX = minX + shape.boundingWidth.toDouble()
    val maxY = minY + shape.boundingHeight.toDouble()
    val maxZ = minZ + shape.boundingDepth.toDouble()

    val diagnostic: String
        get() = "shape=${shape.algorithmShapeType}, axis=${shape.axis ?: "none"}"
}

private fun intervalOverlaps(lhsMin: Double, lhsMax: Double, rhsMin: Double, rhsMax: Double): Boolean {
    return min(lhsMax, rhsMax) - max(lhsMin, rhsMin) > PackingGeometryOverlapTolerance
}

private fun distanceToInterval(point: Double, min: Double, max: Double): Double {
    return when {
        point < min -> min - point
        point > max -> point - max
        else -> 0.0
    }
}

private fun PackingGeometry.center(axis: Axis3): Double {
    return when (axis) {
        Axis3.X -> (minX + maxX) / 2.0
        Axis3.Y -> (minY + maxY) / 2.0
        Axis3.Z -> (minZ + maxZ) / 2.0
    }
}

private fun PackingGeometry.min(axis: Axis3): Double {
    return when (axis) {
        Axis3.X -> minX
        Axis3.Y -> minY
        Axis3.Z -> minZ
    }
}

private fun PackingGeometry.max(axis: Axis3): Double {
    return when (axis) {
        Axis3.X -> maxX
        Axis3.Y -> maxY
        Axis3.Z -> maxZ
    }
}

private fun axesExcept(axis: Axis3): List<Axis3> {
    return Axis3.entries.filter { it != axis }
}

private fun PackingGeometry.toHorizontalCylinderSupportGeometry(): HorizontalCylinderSupportGeometry {
    return HorizontalCylinderSupportGeometry(
        minX = minX,
        maxX = maxX,
        minY = minY,
        maxY = maxY,
        minZ = minZ,
        maxZ = maxZ,
        isCylinder = shape is CylinderPackingShape3
    )
}

private fun hasHorizontalCylinderSupportCoverage(
    geometry: PackingGeometry,
    index: Int,
    geometries: List<PackingGeometry>
): Boolean {
    val cylinder = geometry.shape as? CylinderPackingShape3 ?: return true
    val axis = cylinder.axis
    if (axis == Axis3.Y || abs(geometry.minY) <= PackingGeometryOverlapTolerance) {
        return true
    }

    return horizontalCylinderCuboidSupportCoverage(
        cylinder = geometry.toHorizontalCylinderSupportGeometry(),
        axis = axis,
        supports = geometries
            .filterIndexed { candidateIndex, _ -> candidateIndex != index }
            .map { candidate -> candidate.toHorizontalCylinderSupportGeometry() },
        tolerance = PackingGeometryOverlapTolerance
    )
}

private fun requireHorizontalCylinderSupport(
    geometry: PackingGeometry,
    index: Int,
    geometries: List<PackingGeometry>,
    binName: String,
    source: String
) {
    if (!hasHorizontalCylinderSupportCoverage(
            geometry = geometry,
            index = index,
            geometries = geometries
        )
    ) {
        throw IllegalArgumentException(
            unsupportedHorizontalCylinderSupportMessage(
                source = source,
                binName = binName,
                itemIndex = index,
                diagnostic = geometry.diagnostic
            )
        )
    }
}

private fun boxBoxOverlaps(lhs: PackingGeometry, rhs: PackingGeometry): Boolean {
    return intervalOverlaps(lhs.minX, lhs.maxX, rhs.minX, rhs.maxX)
            && intervalOverlaps(lhs.minY, lhs.maxY, rhs.minY, rhs.maxY)
            && intervalOverlaps(lhs.minZ, lhs.maxZ, rhs.minZ, rhs.maxZ)
}

private fun cylinderBoxOverlaps(cylinder: PackingGeometry, box: PackingGeometry): Boolean {
    val cylinderShape = cylinder.shape as CylinderPackingShape3
    val axis = cylinderShape.axis
    if (!intervalOverlaps(cylinder.min(axis), cylinder.max(axis), box.min(axis), box.max(axis))) {
        return false
    }
    val radialAxes = axesExcept(axis)
    val nearest0 = cylinder.center(radialAxes[0]).coerceIn(box.min(radialAxes[0]), box.max(radialAxes[0]))
    val nearest1 = cylinder.center(radialAxes[1]).coerceIn(box.min(radialAxes[1]), box.max(radialAxes[1]))
    val delta0 = cylinder.center(radialAxes[0]) - nearest0
    val delta1 = cylinder.center(radialAxes[1]) - nearest1
    val distance = sqrt(delta0 * delta0 + delta1 * delta1)
    return cylinderShape.radius.toDouble() - distance > PackingGeometryOverlapTolerance
}

private fun sameAxisCylinderOverlaps(lhs: PackingGeometry, rhs: PackingGeometry): Boolean {
    val lhsShape = lhs.shape as CylinderPackingShape3
    val rhsShape = rhs.shape as CylinderPackingShape3
    val axis = lhsShape.axis
    if (!intervalOverlaps(lhs.min(axis), lhs.max(axis), rhs.min(axis), rhs.max(axis))) {
        return false
    }
    val radialAxes = axesExcept(axis)
    val delta0 = lhs.center(radialAxes[0]) - rhs.center(radialAxes[0])
    val delta1 = lhs.center(radialAxes[1]) - rhs.center(radialAxes[1])
    val distance = sqrt(delta0 * delta0 + delta1 * delta1)
    return lhsShape.radius.toDouble() + rhsShape.radius.toDouble() - distance > PackingGeometryOverlapTolerance
}

private fun differentAxisCylinderOverlaps(lhs: PackingGeometry, rhs: PackingGeometry): Boolean {
    val lhsShape = lhs.shape as CylinderPackingShape3
    val rhsShape = rhs.shape as CylinderPackingShape3
    val lhsAxis = lhsShape.axis
    val rhsAxis = rhsShape.axis
    val sharedAxis = Axis3.entries.first { it != lhsAxis && it != rhsAxis }
    val lhsAxisDistance = distanceToInterval(
        point = rhs.center(lhsAxis),
        min = lhs.min(lhsAxis),
        max = lhs.max(lhsAxis)
    )
    val rhsAxisDistance = distanceToInterval(
        point = lhs.center(rhsAxis),
        min = rhs.min(rhsAxis),
        max = rhs.max(rhsAxis)
    )
    val lhsRadius = lhsShape.radius.toDouble()
    val rhsRadius = rhsShape.radius.toDouble()
    val lhsSharedRadiusSquared = lhsRadius * lhsRadius - rhsAxisDistance * rhsAxisDistance
    val rhsSharedRadiusSquared = rhsRadius * rhsRadius - lhsAxisDistance * lhsAxisDistance
    if (lhsSharedRadiusSquared <= PackingGeometryOverlapTolerance || rhsSharedRadiusSquared <= PackingGeometryOverlapTolerance) {
        return false
    }
    val lhsSharedRadius = sqrt(lhsSharedRadiusSquared)
    val rhsSharedRadius = sqrt(rhsSharedRadiusSquared)
    return intervalOverlaps(
        lhsMin = lhs.center(sharedAxis) - lhsSharedRadius,
        lhsMax = lhs.center(sharedAxis) + lhsSharedRadius,
        rhsMin = rhs.center(sharedAxis) - rhsSharedRadius,
        rhsMax = rhs.center(sharedAxis) + rhsSharedRadius
    )
}

private fun cylinderCylinderOverlaps(lhs: PackingGeometry, rhs: PackingGeometry): Boolean {
    val lhsShape = lhs.shape as CylinderPackingShape3
    val rhsShape = rhs.shape as CylinderPackingShape3
    return if (lhsShape.axis == rhsShape.axis) {
        sameAxisCylinderOverlaps(lhs, rhs)
    } else {
        differentAxisCylinderOverlaps(lhs, rhs)
    }
}

private fun PackingGeometry.overlaps(rhs: PackingGeometry): Boolean {
    val lhsCylinder = shape is CylinderPackingShape3
    val rhsCylinder = rhs.shape is CylinderPackingShape3
    return when {
        lhsCylinder && rhsCylinder -> cylinderCylinderOverlaps(this, rhs)
        lhsCylinder -> cylinderBoxOverlaps(this, rhs)
        rhsCylinder -> cylinderBoxOverlaps(rhs, this)
        else -> boxBoxOverlaps(this, rhs)
    }
}

internal fun requirePackedBinShapeGeometry(
    bin: PackedBin,
    source: String
) {
    val geometries = bin.items.mapIndexed { index, packed ->
        val placement = packed.placement
        val shape = placement.resolvedPackingShape()
        if (!bin.type.enabled(shape, placement.absolutePosition)) {
            val geometry = PackingGeometry(
                shape = shape,
                position = placement.absolutePosition
            )
            throw IllegalArgumentException(
                unsupportedOutsideBinGeometryMessage(
                    source = source,
                    binName = bin.name,
                    itemIndex = index,
                    diagnostic = geometry.diagnostic
                )
            )
        }
        PackingGeometry(
            shape = shape,
            position = placement.absolutePosition
        )
    }

    geometries.forEachIndexed { index, geometry ->
        requireHorizontalCylinderSupport(
            geometry = geometry,
            index = index,
            geometries = geometries,
            binName = bin.name,
            source = source
        )
    }

    for (lhsIndex in geometries.indices) {
        for (rhsIndex in (lhsIndex + 1) until geometries.size) {
            if (geometries[lhsIndex].overlaps(geometries[rhsIndex])) {
                throw IllegalArgumentException(
                    unsupportedPlacementOverlapMessage(
                        source = source,
                        binName = bin.name,
                        lhsIndex = lhsIndex,
                        lhsDiagnostic = geometries[lhsIndex].diagnostic,
                        rhsIndex = rhsIndex,
                        rhsDiagnostic = geometries[rhsIndex].diagnostic
                    )
                )
            }
        }
    }
}
