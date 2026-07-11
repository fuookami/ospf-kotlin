/**
 * 装箱几何守卫。
 * Packing geometry guard.
*/
package fuookami.ospf.kotlin.framework.bpp3d.domain.packing.service

import kotlin.math.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.PackedBin
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.geometry.Axis3
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

private const val PackingGeometryOverlapTolerance = 1e-7

/**
 * PackingGeometry data class.
 * PackingGeometry数据类。
*/
private data class PackingGeometry(
    val shape: PackingShape3<FltX>,
    val position: QuantityPoint3<FltX>
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

/**
 * intervalOverlaps.
 * intervalOverlaps。
 * @param lhsMin left-hand interval lower bound / 左侧区间下界
 * @param lhsMax left-hand interval upper bound / 左侧区间上界
 * @param rhsMin right-hand interval lower bound / 右侧区间下界
 * @param rhsMax right-hand interval upper bound / 右侧区间上界
 * @return whether the two intervals overlap / 两个区间是否重叠
*/
private fun intervalOverlaps(lhsMin: Double, lhsMax: Double, rhsMin: Double, rhsMax: Double): Boolean {
    return min(lhsMax, rhsMax) - max(lhsMin, rhsMin) > PackingGeometryOverlapTolerance
}

/**
 * distanceToInterval.
 * distanceToInterval。
 * @param point value to check / 待检查的值
 * @param min interval lower bound / 区间下界
 * @param max interval upper bound / 区间上界
 * @return distance from point to interval / 点到区间的距离
*/
private fun distanceToInterval(point: Double, min: Double, max: Double): Double {
    return when {
        point < min -> min - point
        point > max -> point - max
        else -> 0.0
    }
}

/**
 * PackingGeometry.
 * PackingGeometry。
 * @param axis coordinate axis / 坐标轴
 * @return center coordinate along the axis / 沿该轴的中心坐标
*/
private fun PackingGeometry.center(axis: Axis3): Double {
    return when (axis) {
        Axis3.X -> (minX + maxX) / 2.0
        Axis3.Y -> (minY + maxY) / 2.0
        Axis3.Z -> (minZ + maxZ) / 2.0
    }
}

/**
 * PackingGeometry.
 * PackingGeometry。
 * @param axis coordinate axis / 坐标轴
 * @return minimum coordinate along the axis / 沿该轴的最小坐标
*/
private fun PackingGeometry.min(axis: Axis3): Double {
    return when (axis) {
        Axis3.X -> minX
        Axis3.Y -> minY
        Axis3.Z -> minZ
    }
}

/**
 * PackingGeometry.
 * PackingGeometry。
 * @param axis coordinate axis / 坐标轴
 * @return maximum coordinate along the axis / 沿该轴的最大坐标
*/
private fun PackingGeometry.max(axis: Axis3): Double {
    return when (axis) {
        Axis3.X -> maxX
        Axis3.Y -> maxY
        Axis3.Z -> maxZ
    }
}

/**
 * axesExcept.
 * axesExcept。
 * @param axis axis to exclude / 要排除的轴
 * @return the other two axes / 其余两个轴
*/
private fun axesExcept(axis: Axis3): List<Axis3> {
    return Axis3.entries.filter { it != axis }
}

/**
 * PackingGeometry.
 * PackingGeometry。
 * @return horizontal cylinder support geometry / 水平圆柱支撑几何
*/
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

/**
 * Checks if has horizontalCylinderSupportCoverage.
 * 检查是否具有HorizontalCylinderSupportCoverage。
 * @param geometry packing geometry of the cylinder to check / 待检查的圆柱装载几何
 * @param index index in the sequence / 序列中的索引
 * @param geometries all packing geometries in the bin / 箱内所有装载几何列表
 * @return whether the cylinder has sufficient support / 圆柱是否有足够支撑
*/
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

/**
 * 校验水平放置的圆柱体是否获得足够的下方支撑覆盖。无支撑时返回失败。
 * Validate that a horizontally placed cylinder has sufficient support coverage from below. Returns failure when unsupported.
 *
 * @param geometry 待校验的几何体 / geometry to validate
 * @param index 该几何体在列表中的索引 / index of this geometry in the list
 * @param geometries 箱内所有几何体列表 / list of all geometries in the bin
 * @param binName 箱子名称，用于错误信息 / bin name, used in error messages
 * @param source 调用来源标识 / caller source identifier
 * @return 校验结果，支撑不足时失败 / validation result, fails when support is insufficient
*/
private fun requireHorizontalCylinderSupport(
    geometry: PackingGeometry,
    index: Int,
    geometries: List<PackingGeometry>,
    binName: String,
    source: String
): Try {
    if (!hasHorizontalCylinderSupportCoverage(
            geometry = geometry,
            index = index,
            geometries = geometries
        )
    ) {
        return Failed(
            ErrorCode.IllegalArgument,
            unsupportedHorizontalCylinderSupportMessage(
                source = source,
                binName = binName,
                itemIndex = index,
                diagnostic = geometry.diagnostic
            )
        )
    }
    return ok
}

/**
 * boxBoxOverlaps.
 * boxBoxOverlaps。
 * @param lhs left-hand geometry / 左侧几何
 * @param rhs right-hand operand / 右操作数
 * @return whether the two boxes overlap / 两个长方体是否重叠
*/
private fun boxBoxOverlaps(lhs: PackingGeometry, rhs: PackingGeometry): Boolean {
    return intervalOverlaps(lhs.minX, lhs.maxX, rhs.minX, rhs.maxX)
            && intervalOverlaps(lhs.minY, lhs.maxY, rhs.minY, rhs.maxY)
            && intervalOverlaps(lhs.minZ, lhs.maxZ, rhs.minZ, rhs.maxZ)
}

/**
 * cylinderBoxOverlaps.
 * cylinderBoxOverlaps。
 * @param cylinder cylinder geometry / 圆柱几何
 * @param box box geometry / 长方体几何
 * @return whether the cylinder and box overlap / 圆柱与长方体是否重叠
*/
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

/**
 * sameAxisCylinderOverlaps.
 * sameAxisCylinderOverlaps。
 * @param lhs left-hand geometry / 左侧几何
 * @param rhs right-hand operand / 右操作数
 * @return whether the two same-axis cylinders overlap / 两个同轴圆柱是否重叠
*/
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

/**
 * differentAxisCylinderOverlaps.
 * differentAxisCylinderOverlaps。
 * @param lhs left-hand geometry / 左侧几何
 * @param rhs right-hand operand / 右操作数
 * @return whether the two different-axis cylinders overlap / 两个异轴圆柱是否重叠
*/
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

/**
 * cylinderCylinderOverlaps.
 * cylinderCylinderOverlaps。
 * @param lhs left-hand cylinder geometry / 左侧圆柱几何
 * @param rhs right-hand operand / 右操作数
 * @return whether the two cylinders overlap / 两个圆柱是否重叠
*/
private fun cylinderCylinderOverlaps(lhs: PackingGeometry, rhs: PackingGeometry): Boolean {
    val lhsShape = lhs.shape as CylinderPackingShape3
    val rhsShape = rhs.shape as CylinderPackingShape3
    return if (lhsShape.axis == rhsShape.axis) {
        sameAxisCylinderOverlaps(lhs, rhs)
    } else {
        differentAxisCylinderOverlaps(lhs, rhs)
    }
}

/**
 * PackingGeometry.
 * PackingGeometry。
 * @param rhs right-hand operand / 右操作数
 * @return whether this geometry overlaps the other / 此几何是否与另一几何重叠
*/
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

/**
 * 校验已装箱物品的几何约束：形状不超出箱体边界、水平圆柱体有足够支撑、物品之间无重叠。
 * Validate geometric constraints of packed items: shapes do not exceed bin boundaries, horizontal cylinders have sufficient support, and no overlaps between items.
 *
 * @param bin 已装箱的箱子 / packed bin to validate
 * @param source 调用来源标识 / caller source identifier
 * @return 校验结果，几何约束违反时失败 / validation result, fails when geometric constraints are violated
*/
internal fun requirePackedBinShapeGeometry(
    bin: PackedBin,
    source: String
): Try {
    val geometries = bin.items.mapIndexed { index, packed ->
        val placement = packed.placement
        val shape = placement.resolvedPackingShape()
        if (!bin.type.asContainer3Shape().enabled(shape, placement.absolutePosition)) {
            val geometry = PackingGeometry(
                shape = shape,
                position = placement.absolutePosition
            )
            return Failed(
                ErrorCode.IllegalArgument,
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
        when (val support = requireHorizontalCylinderSupport(
            geometry = geometry,
            index = index,
            geometries = geometries,
            binName = bin.name,
            source = source
        )) {
            is Ok -> {}
            is Failed -> return Failed(support.error)
            is Fatal -> return Fatal(support.errors)
        }
    }

    for (lhsIndex in geometries.indices) {
        for (rhsIndex in (lhsIndex + 1) until geometries.size) {
            if (geometries[lhsIndex].overlaps(geometries[rhsIndex])) {
                return Failed(
                    ErrorCode.IllegalArgument,
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
    return ok
}
