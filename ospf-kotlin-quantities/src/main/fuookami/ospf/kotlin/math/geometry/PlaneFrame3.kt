/**
 * 三维平面框架
 * 3D plane frame
 *
 * 定义三维空间中的平面坐标框架，支持点投影、法向量计算和长方体底面积投影。
 * Defines plane coordinate frames in 3D space, supporting point projection, normal vector calculation, and cuboid footprint projection.
 */
package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret

/**
 * 二维平面上的点
 * 2D point on a plane
 *
 * @property x x 坐标 / x coordinate
 * @property y y 坐标 / y coordinate
 * @param V 数值类型 / Number type
 */
data class QuantityPlanePoint2<V : FloatingNumber<V>>(
    val x: Quantity<V>,
    val y: Quantity<V>
)

/**
 * 三维空间中的点
 * 3D point in space
 *
 * @property x x 坐标 / x coordinate
 * @property y y 坐标 / y coordinate
 * @property z z 坐标 / z coordinate
 * @param V 数值类型 / Number type
 */
data class QuantityPlanePoint3<V : FloatingNumber<V>>(
    val x: Quantity<V>,
    val y: Quantity<V>,
    val z: Quantity<V>
) {
    /**
     * 获取沿指定轴的坐标
     * Get the coordinate along a specified axis
     *
     * @param axis 目标轴 / Target axis
     * @return 沿该轴的坐标 / Coordinate along the axis
     */
    fun along(axis: Axis3): Quantity<V> {
        return when (axis) {
            Axis3.X -> x
            Axis3.Y -> y
            Axis3.Z -> z
        }
    }
}

/**
 * 三维平面法向量
 * 3D plane normal vector
 *
 * @property x x 分量 / x component
 * @property y y 分量 / y component
 * @property z z 分量 / z component
 * @param V 数值类型 / Number type
 */
data class QuantityPlaneVector3<V : FloatingNumber<V>>(
    val x: Quantity<V>,
    val y: Quantity<V>,
    val z: Quantity<V>
)

/**
 * 平面坐标框架是纯几何能力；BPP3D 的 Bottom/Side/Front 映射由桥接层负责。
 * Plane frame is pure geometry; BPP3D Bottom/Side/Front mapping stays in bridge layer.
 *
 * @property firstAxis 第一轴 / First axis
 * @property secondAxis 第二轴 / Second axis
 */
class QuantityPlaneFrame3 private constructor(
    val firstAxis: Axis3,
    val secondAxis: Axis3,
    private val normalAxisValue: Axis3
) {
    /** 可空法向轴（垂直于平面的轴）/ Nullable normal axis (perpendicular to the plane) */
    val normalAxisOrNull: Axis3? get() = normalAxisValue

    /** 获取法向轴（垂直于平面的轴）/ Get normal axis (perpendicular to the plane) */
    fun normalAxis(): Ret<Axis3> {
        return normalAxisOrNull?.let { Ok(it) }
            ?: Failed(
                ErrorCode.IllegalArgument,
                "无效平面坐标轴：$firstAxis, $secondAxis。 / Invalid plane axes: $firstAxis, $secondAxis."
            )
    }

    /**
     * 计算点到平面的距离
     * Compute the distance from a point to the plane
     *
     * @param point 三维点 / 3D point
     * @param V 数值类型 / Number type
     * @return 到平面的距离 / Distance to the plane
     */
    fun <V : FloatingNumber<V>> distance(point: QuantityPlanePoint3<V>): Quantity<V> = point.along(normalAxisValue)

    /**
     * 将三维点投影到二维平面坐标
     * Project a 3D point to 2D plane coordinates
     *
     * @param point 三维点 / 3D point
     * @param V 数值类型 / Number type
     * @return 二维平面坐标 / 2D plane coordinates
     */
    fun <V : FloatingNumber<V>> point2(point: QuantityPlanePoint3<V>): QuantityPlanePoint2<V> {
        return QuantityPlanePoint2(
            x = point.along(firstAxis),
            y = point.along(secondAxis)
        )
    }

    /**
     * 从二维平面坐标和距离恢复三维点
     * Restore a 3D point from 2D plane coordinates and distance
     *
     * @param point 二维平面坐标 / 2D plane coordinates
     * @param distance 到平面的距离 / Distance to the plane
     * @param V 数值类型 / Number type
     * @return 三维点 / 3D point
     */
    fun <V : FloatingNumber<V>> point3(point: QuantityPlanePoint2<V>, distance: Quantity<V>): QuantityPlanePoint3<V> {
        val x = if (firstAxis == Axis3.X) {
            point.x
        } else if (secondAxis == Axis3.X) {
            point.y
        } else {
            distance
        }
        val y = if (firstAxis == Axis3.Y) {
            point.x
        } else if (secondAxis == Axis3.Y) {
            point.y
        } else {
            distance
        }
        val z = if (firstAxis == Axis3.Z) {
            point.x
        } else if (secondAxis == Axis3.Z) {
            point.y
        } else {
            distance
        }
        return QuantityPlanePoint3(
            x = x,
            y = y,
            z = z
        )
    }

    /**
     * 根据距离生成法向量
     * Generate a normal vector from a distance value
     *
     * @param distance 距离值 / Distance value
     * @param V 数值类型 / Number type
     * @return 法向量 / Normal vector
     */
    fun <V : FloatingNumber<V>> vector(distance: Quantity<V>): QuantityPlaneVector3<V> {
        val zero = quantityZeroOf(distance)
        return when (normalAxisValue) {
            Axis3.X -> QuantityPlaneVector3(x = distance, y = zero, z = zero)
            Axis3.Y -> QuantityPlaneVector3(x = zero, y = distance, z = zero)
            Axis3.Z -> QuantityPlaneVector3(x = zero, y = zero, z = distance)
        }
    }

    /**
     * 计算长方体在平面上的投影（底面积）
     * Compute the footprint of a cuboid on the plane
     *
     * @param cuboid 长方体 / Cuboid
     * @param V 数值类型 / Number type
     * @return 投影矩形 / Footprint rectangle
     */
    fun <V : FloatingNumber<V>> footprint(cuboid: QuantityCuboid3<V>): QuantityRectangle2<V> {
        return QuantityRectangle2(
            width = cuboid.along(firstAxis),
            height = cuboid.along(secondAxis)
        )
    }

    /** 判断相等性 / Check equality */
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is QuantityPlaneFrame3) {
            return false
        }
        return firstAxis == other.firstAxis && secondAxis == other.secondAxis
    }

    /** 计算哈希码 / Compute hash code */
    override fun hashCode(): Int {
        var result = firstAxis.hashCode()
        result = 31 * result + secondAxis.hashCode()
        return result
    }

    /** 转换为字符串表示 / Convert to string representation */
    override fun toString(): String {
        return "QuantityPlaneFrame3(firstAxis=$firstAxis, secondAxis=$secondAxis)"
    }

    /** 工厂方法 / Factory methods */
    companion object {
        /** 创建平面框架，非法轴组合返回失败 / Create a plane frame, returning failure for invalid axis combinations */
        fun of(firstAxis: Axis3, secondAxis: Axis3): Ret<QuantityPlaneFrame3> {
            return ofOrNull(firstAxis, secondAxis)?.let { Ok(it) }
                ?: Failed(
                    ErrorCode.IllegalArgument,
                    "无效平面坐标轴：$firstAxis, $secondAxis。 / Invalid plane axes: $firstAxis, $secondAxis."
                )
        }

        /** 创建平面框架，非法轴组合返回 null / Create a plane frame, returning null for invalid axis combinations */
        fun ofOrNull(firstAxis: Axis3, secondAxis: Axis3): QuantityPlaneFrame3? {
            val normalAxis = normalAxisOf(firstAxis, secondAxis) ?: return null
            return QuantityPlaneFrame3(
                firstAxis = firstAxis,
                secondAxis = secondAxis,
                normalAxisValue = normalAxis
            )
        }

        private fun normalAxisOf(firstAxis: Axis3, secondAxis: Axis3): Axis3? {
            return when {
                (firstAxis == Axis3.X && secondAxis == Axis3.Y) || (firstAxis == Axis3.Y && secondAxis == Axis3.X) -> Axis3.Z
                (firstAxis == Axis3.X && secondAxis == Axis3.Z) || (firstAxis == Axis3.Z && secondAxis == Axis3.X) -> Axis3.Y
                (firstAxis == Axis3.Y && secondAxis == Axis3.Z) || (firstAxis == Axis3.Z && secondAxis == Axis3.Y) -> Axis3.X
                else -> null
            }
        }

        /** X-Y 平面框架 / X-Y plane frame */
        val XY = QuantityPlaneFrame3(firstAxis = Axis3.X, secondAxis = Axis3.Y, normalAxisValue = Axis3.Z)
        /** Y-X 平面框架 / Y-X plane frame */
        val YX = QuantityPlaneFrame3(firstAxis = Axis3.Y, secondAxis = Axis3.X, normalAxisValue = Axis3.Z)
        /** X-Z 平面框架 / X-Z plane frame */
        val XZ = QuantityPlaneFrame3(firstAxis = Axis3.X, secondAxis = Axis3.Z, normalAxisValue = Axis3.Y)
        /** Z-X 平面框架 / Z-X plane frame */
        val ZX = QuantityPlaneFrame3(firstAxis = Axis3.Z, secondAxis = Axis3.X, normalAxisValue = Axis3.Y)
        /** Y-Z 平面框架 / Y-Z plane frame */
        val YZ = QuantityPlaneFrame3(firstAxis = Axis3.Y, secondAxis = Axis3.Z, normalAxisValue = Axis3.X)
        /** Z-Y 平面框架 / Z-Y plane frame */
        val ZY = QuantityPlaneFrame3(firstAxis = Axis3.Z, secondAxis = Axis3.Y, normalAxisValue = Axis3.X)
    }
}
