/**
 * 三维平面坐标框架
 * 3D Plane Coordinate Frame
 *
 * 定义三维空间中的平面坐标框架及相关类型（平面点、平面矩形等）。
 * Defines plane coordinate frame and related types (plane point, plane rectangle, etc.) in 3D space.
 */
package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret

/**
 * 二维平面点，用于平面坐标框架中的二维坐标。
 * 2D plane point for coordinates within a plane coordinate frame.
 *
 * @param V 数值类型 / The numeric type
 * @property x X 坐标 / X coordinate
 * @property y Y 坐标 / Y coordinate
 */
data class PlanePoint2<V : FloatingNumber<V>>(
    val x: V,
    val y: V
)

/**
 * 三维空间点，用于平面坐标框架中的三维坐标。
 * 3D space point for coordinates within a plane coordinate frame.
 *
 * @param V 数值类型 / The numeric type
 * @property x X 坐标 / X coordinate
 * @property y Y 坐标 / Y coordinate
 * @property z Z 坐标 / Z coordinate
 */
data class PlanePoint3<V : FloatingNumber<V>>(
    val x: V,
    val y: V,
    val z: V
) {
    /**
     * 沿指定轴的坐标值
     * Coordinate value along the specified axis
     *
     * @param axis 目标轴（X、Y 或 Z） / Target axis (X, Y, or Z)
     * @return 该轴上的坐标分量 / The coordinate component along the specified axis
     */
    fun along(axis: Axis3): V {
        return when (axis) {
            Axis3.X -> x
            Axis3.Y -> y
            Axis3.Z -> z
        }
    }
}

/**
 * 三维平面法向量，表示平面坐标框架中的法向偏移。
 * 3D plane normal vector representing the normal offset in a plane coordinate frame.
 *
 * @param V 数值类型 / The numeric type
 * @property x X 分量 / X component
 * @property y Y 分量 / Y component
 * @property z Z 分量 / Z component
 */
data class PlaneVector3<V : FloatingNumber<V>>(
    val x: V,
    val y: V,
    val z: V
)

/**
 * 平面坐标框架是纯几何能力；BPP3D 的 Bottom/Side/Front 映射由桥接层负责。
 * Plane frame is pure geometry; BPP3D Bottom/Side/Front mapping stays in bridge layer.
 *
 * @property firstAxis 平面第一轴 / The first axis of the plane
 * @property secondAxis 平面第二轴 / The second axis of the plane
 */
data class PlaneFrame3(
    val firstAxis: Axis3,
    val secondAxis: Axis3
) {
    init {
        require(firstAxis != secondAxis) { "firstAxis and secondAxis must be different." }
    }

    /** 法向轴，非法坐标框架返回 null / The normal axis, or null for an invalid frame */
    val normalAxisOrNull: Axis3?
        get() {
            return when {
                (firstAxis == Axis3.X && secondAxis == Axis3.Y) || (firstAxis == Axis3.Y && secondAxis == Axis3.X) -> Axis3.Z
                (firstAxis == Axis3.X && secondAxis == Axis3.Z) || (firstAxis == Axis3.Z && secondAxis == Axis3.X) -> Axis3.Y
                (firstAxis == Axis3.Y && secondAxis == Axis3.Z) || (firstAxis == Axis3.Z && secondAxis == Axis3.Y) -> Axis3.X
                else -> null
            }
        }

    /**
     * 获取法向轴
     * Get the normal axis.
     *
     * @return 法向轴或失败原因 / The normal axis or failure reason
     */
    fun normalAxis(): Ret<Axis3> {
        return normalAxisOrNull
            ?.let { Ok(it) }
            ?: Failed(ErrorCode.IllegalArgument, "Unsupported axis frame: $firstAxis, $secondAxis")
    }

    /**
     * 计算点到平面的距离
     * Compute the distance from a point to the plane
     *
     * @param V 数值类型 / The numeric type
     * @param point 三维空间点 / The 3D space point
     * @return 点到平面的距离或失败原因 / The distance from the point to the plane or failure reason
     */
    fun <V : FloatingNumber<V>> distance(point: PlanePoint3<V>): Ret<V> {
        return when (val axis = normalAxis()) {
            is Ok -> Ok(point.along(axis.value))
            is Failed -> Failed(axis.error)
            is Fatal -> Fatal(axis.errors)
        }
    }

    /**
     * 将三维点投影到平面二维坐标
     * Project a 3D point to 2D plane coordinates
     *
     * @param V 数值类型 / The numeric type
     * @param point 三维空间点 / The 3D space point
     * @return 平面二维坐标 / The 2D plane coordinates
     */
    fun <V : FloatingNumber<V>> point2(point: PlanePoint3<V>): PlanePoint2<V> {
        return PlanePoint2(
            x = point.along(firstAxis),
            y = point.along(secondAxis)
        )
    }

    /**
     * 将平面二维坐标还原为三维点
     * Convert 2D plane coordinates back to a 3D point
     *
     * @param V 数值类型 / The numeric type
     * @param point 平面二维坐标 / The 2D plane coordinates
     * @param distance 法向轴距离值 / The distance along the normal axis
     * @return 三维空间点 / The 3D space point
     */
    fun <V : FloatingNumber<V>> point3(point: PlanePoint2<V>, distance: V): PlanePoint3<V> {
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
        return PlanePoint3(
            x = x,
            y = y,
            z = z
        )
    }

    /**
     * 根据距离值创建法向量
     * Create a normal vector from a distance value
     *
     * @param V 数值类型 / The numeric type
     * @param distance 法向距离值 / The normal distance value
     * @return 法向量或失败原因 / The normal vector or failure reason
     */
    fun <V : FloatingNumber<V>> vector(distance: V): Ret<PlaneVector3<V>> {
        val zero = quantityZeroOf(distance)
        return when (val axis = normalAxis()) {
            is Ok -> when (axis.value) {
                Axis3.X -> Ok(PlaneVector3(x = distance, y = zero, z = zero))
                Axis3.Y -> Ok(PlaneVector3(x = zero, y = distance, z = zero))
                Axis3.Z -> Ok(PlaneVector3(x = zero, y = zero, z = distance))
            }
            is Failed -> Failed(axis.error)
            is Fatal -> Fatal(axis.errors)
        }
    }

    /**
     * 计算长方体在平面上的投影矩形
     * Compute the footprint rectangle of a cuboid on the plane
     *
     * @param V 数值类型 / The numeric type
     * @param cuboid 长方体 / The cuboid
     * @return 投影矩形 / The footprint rectangle
     */
    fun <V : FloatingNumber<V>> footprint(cuboid: Cuboid3<V>): Rectangle2<V> {
        return Rectangle2(
            width = cuboid.along(firstAxis),
            height = cuboid.along(secondAxis)
        )
    }

    companion object {
        /** X-Y 平面 / X-Y plane */
        val XY = PlaneFrame3(firstAxis = Axis3.X, secondAxis = Axis3.Y)
        /** Y-X 平面 / Y-X plane */
        val YX = PlaneFrame3(firstAxis = Axis3.Y, secondAxis = Axis3.X)
        /** X-Z 平面 / X-Z plane */
        val XZ = PlaneFrame3(firstAxis = Axis3.X, secondAxis = Axis3.Z)
        /** Z-X 平面 / Z-X plane */
        val ZX = PlaneFrame3(firstAxis = Axis3.Z, secondAxis = Axis3.X)
        /** Y-Z 平面 / Y-Z plane */
        val YZ = PlaneFrame3(firstAxis = Axis3.Y, secondAxis = Axis3.Z)
        /** Z-Y 平面 / Z-Y plane */
        val ZY = PlaneFrame3(firstAxis = Axis3.Z, secondAxis = Axis3.Y)
    }
}
