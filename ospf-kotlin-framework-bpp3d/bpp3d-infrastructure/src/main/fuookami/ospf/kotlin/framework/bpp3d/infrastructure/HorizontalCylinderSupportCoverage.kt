/**
 * 水平圆柱支撑覆盖区域。
 * Horizontal cylinder support coverage.
*/
package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import kotlin.math.*
import fuookami.ospf.kotlin.math.geometry.Axis3

/** 横向圆柱支撑覆盖默认容差。Default tolerance for horizontal-cylinder support coverage. */
const val HorizontalCylinderSupportCoverageTolerance: Double = 1e-7

/**
 * 横向圆柱支撑覆盖使用的轴对齐几何盒。
 * Axis-aligned geometry box used by horizontal-cylinder support coverage.
 *
 * @property minX X 轴最小坐标 / minimum X coordinate
 * @property maxX X 轴最大坐标 / maximum X coordinate
 * @property minY Y 轴最小坐标 / minimum Y coordinate
 * @property maxY Y 轴最大坐标 / maximum Y coordinate
 * @property minZ Z 轴最小坐标 / minimum Z coordinate
 * @property maxZ Z 轴最大坐标 / maximum Z coordinate
 * @property isCylinder 是否为圆柱 / whether the geometry is a cylinder
*/
data class HorizontalCylinderSupportGeometry(
    val minX: Double,
    val maxX: Double,
    val minY: Double,
    val maxY: Double,
    val minZ: Double,
    val maxZ: Double,
    val isCylinder: Boolean = false
) {

    /**
     * 获取指定轴向最小坐标。
     * Get minimum coordinate on the given axis.
     *
     * @param axis 轴向 / axis
     * @return 最小坐标 / minimum coordinate
    */
    fun min(axis: Axis3): Double {
        return when (axis) {
            Axis3.X -> minX
            Axis3.Y -> minY
            Axis3.Z -> minZ
        }
    }

    /**
     * 获取指定轴向最大坐标。
     * Get maximum coordinate on the given axis.
     *
     * @param axis 轴向 / axis
     * @return 最大坐标 / maximum coordinate
    */
    fun max(axis: Axis3): Double {
        return when (axis) {
            Axis3.X -> maxX
            Axis3.Y -> maxY
            Axis3.Z -> maxZ
        }
    }

    /**
     * 获取指定轴向中心坐标。
     * Get center coordinate on the given axis.
     *
     * @param axis 轴向 / axis
     * @return 中心坐标 / center coordinate
    */
    fun center(axis: Axis3): Double {
        return (min(axis) + max(axis)) / 2.0
    }
}

/**
 * 获取横向圆柱底部支撑线所在的径向轴。
 * Get the radial axis that contains the bottom support line of a horizontal cylinder.
 *
 * @param axis 圆柱轴向 / cylinder axis
 * @return 支撑线径向轴 / radial axis for the support line
*/
fun horizontalCylinderSupportRadialAxis(axis: Axis3): Axis3 {
    return when (axis) {
        Axis3.X -> Axis3.Z
        Axis3.Y -> Axis3.Y
        Axis3.Z -> Axis3.X
    }
}

/**
 * 判断若干区间是否完整覆盖目标区间。
 * Return whether intervals fully cover the target span.
 *
 * @param targetMin 目标最小坐标 / target minimum coordinate
 * @param targetMax 目标最大坐标 / target maximum coordinate
 * @param intervals 候选覆盖区间 / candidate coverage intervals
 * @param tolerance 容差 / tolerance
 * @return 是否完整覆盖 / whether the target span is fully covered
*/
fun intervalsCoverSpan(
    targetMin: Double,
    targetMax: Double,
    intervals: List<Pair<Double, Double>>,
    tolerance: Double = HorizontalCylinderSupportCoverageTolerance
): Boolean {
    var coveredMax = targetMin
    val sortedIntervals = intervals.sortedWith { lhs, rhs ->
        lhs.first.compareTo(rhs.first)
    }
    for ((intervalMin, intervalMax) in sortedIntervals) {
        if (intervalMax <= coveredMax + tolerance) {
            continue
        }
        if (intervalMin - coveredMax > tolerance) {
            return false
        }
        coveredMax = max(coveredMax, intervalMax)
        if (coveredMax >= targetMax - tolerance) {
            return true
        }
    }
    return coveredMax >= targetMax - tolerance
}

/**
 * 校验横向圆柱是否由贴地或长方体支撑区间完整覆盖。
 * Check whether a horizontal cylinder is on the floor or fully covered by cuboid support intervals.
 *
 * @param cylinder 横向圆柱几何 / horizontal cylinder geometry
 * @param axis 圆柱轴向 / cylinder axis
 * @param supports 候选支撑几何 / candidate support geometries
 * @param tolerance 容差 / tolerance
 * @return 是否具备完整支撑覆盖 / whether full support coverage exists
*/
fun horizontalCylinderCuboidSupportCoverage(
    cylinder: HorizontalCylinderSupportGeometry,
    axis: Axis3,
    supports: List<HorizontalCylinderSupportGeometry>,
    tolerance: Double = HorizontalCylinderSupportCoverageTolerance
): Boolean {
    if (axis == Axis3.Y || abs(cylinder.minY) <= tolerance) {
        return true
    }

    val radialAxis = horizontalCylinderSupportRadialAxis(axis)
    val bottomLineCoordinate = cylinder.center(radialAxis)
    val supportIntervals = supports.mapNotNull { support ->
        if (support.isCylinder) {
            return@mapNotNull null
        }
        if (abs(support.maxY - cylinder.minY) > tolerance) {
            return@mapNotNull null
        }
        if (bottomLineCoordinate < support.min(radialAxis) - tolerance
            || bottomLineCoordinate > support.max(radialAxis) + tolerance
        ) {
            return@mapNotNull null
        }
        val intervalMin = max(support.min(axis), cylinder.min(axis))
        val intervalMax = min(support.max(axis), cylinder.max(axis))
        if (intervalMax - intervalMin <= tolerance) {
            null
        } else {
            Pair(intervalMin, intervalMax)
        }
    }

    return intervalsCoverSpan(
        targetMin = cylinder.min(axis),
        targetMax = cylinder.max(axis),
        intervals = supportIntervals,
        tolerance = tolerance
    )
}
