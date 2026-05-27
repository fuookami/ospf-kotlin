/**
 * 三维圆柱体
 * 3D cylinder
 *
 * 由半径、高度和轴方向定义的圆柱体，支持投影、体积计算和轴置换。
 * A cylinder defined by radius, height, and axis direction, supporting projection, volume calculation, and axis permutation.
 */
package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.times

/**
 * 三维轴线段
 * 3D axis-aligned line segment
 *
 * 沿某一坐标轴方向的线段。
 * A line segment along a coordinate axis direction.
 *
 * @property axis 所在轴 / Axis along which the segment lies
 * @property from 起点坐标 / Start coordinate
 * @property to 终点坐标 / End coordinate
 * @param V 数值类型 / Number type
 */
data class QuantityAxisLine3<V : FloatingNumber<V>>(
    val axis: Axis3,
    val from: Quantity<V>,
    val to: Quantity<V>
)

/**
 * 三维圆柱体形状
 * 3D cylinder shape
 *
 * 由半径、高度和轴方向定义的圆柱体，实现 QuantityShape3 接口。
 * A cylinder defined by radius, height, and axis direction, implementing the QuantityShape3 interface.
 *
 * @property radius 半径 / Radius
 * @property height 高度 / Height
 * @property axis 轴方向 / Axis direction
 * @param V 数值类型 / Number type
 */
data class QuantityCylinder3<V : FloatingNumber<V>>(
    val radius: Quantity<V>,
    val height: Quantity<V>,
    val axis: Axis3
) : QuantityShape3<V> {
    /** 直径 / Diameter */
    val diameter: Quantity<V> get() = quantityPlus(radius, radius)

    /** 最小包围长方体 / Minimum bounding cuboid */
    override val boundingCuboid: QuantityCuboid3<V>
        get() {
            return when (axis) {
                Axis3.X -> QuantityCuboid3(width = height, height = diameter, depth = diameter)
                Axis3.Y -> QuantityCuboid3(width = diameter, height = height, depth = diameter)
                Axis3.Z -> QuantityCuboid3(width = diameter, height = diameter, depth = height)
            }
        }

    /**
     * 获取沿指定轴的尺寸
     * Get the dimension along a specified axis
     *
     * @param axis 目标轴 / Target axis
     * @return 沿该轴的尺寸（轴方向为高度，其余为直径）/ Dimension along the axis (height along axis, diameter otherwise)
     */
    fun along(axis: Axis3): Quantity<V> {
        return if (axis == this.axis) {
            height
        } else {
            diameter
        }
    }

    /** 原点处的轴线段 / Axis line segment at the origin */
    val axisLineAtOrigin: QuantityAxisLine3<V>
        get() = QuantityAxisLine3(
            axis = axis,
            from = quantityZeroOf(height),
            to = height
        )

    /**
     * 在指定平面上的投影
     * Projection onto a specified plane
     *
     * @param plane 目标平面 / Target plane
     * @return 投影形状（矩形或圆形）/ Projection shape (rectangle or circle)
     */
    fun projectionOn(plane: AxisPlane3): QuantityProjection2<V> {
        return if (plane.contains(axis)) {
            QuantityRectangle2(
                width = along(plane.firstAxis),
                height = along(plane.secondAxis)
            )
        } else {
            QuantityCircle2(radius)
        }
    }

    /**
     * 计算底面积
     * Compute the base area
     *
     * @param pi 圆周率值 / Pi value
     * @return 底面积 / Base area
     */
    fun baseArea(pi: V): Quantity<V> = (radius * radius) * pi

    /**
     * 计算体积
     * Compute the volume
     *
     * @param pi 圆周率值 / Pi value
     * @return 体积 / Volume
     */
    fun volume(pi: V): Quantity<V> = baseArea(pi) * height

    /**
     * 对圆柱体应用轴置换
     * Apply axis permutation to the cylinder
     *
     * @param permutation 轴置换 / Axis permutation
     * @return 置换后的圆柱体 / Permuted cylinder
     */
    fun permute(permutation: QuantityAxisPermutation3): QuantityCylinder3<V> = permutation.apply(this)

    /**
     * 在原点创建包围盒
     * Create a bounding box at the origin
     *
     * @return 原点处的包围盒 / Bounding box at the origin
     */
    fun boundingBoxAtOrigin(): QuantityBox3<V> = QuantityBox3.atOrigin(boundingCuboid)

    /**
     * 在指定位置创建包围盒
     * Create a bounding box at the specified position
     *
     * @param x x 坐标 / x coordinate
     * @param y y 坐标 / y coordinate
     * @param z z 坐标 / z coordinate
     * @return 指定位置的包围盒 / Bounding box at the specified position
     */
    fun toBoundingBox(
        x: Quantity<V>,
        y: Quantity<V>,
        z: Quantity<V>
    ): QuantityBox3<V> {
        return QuantityBox3(
            x = x,
            y = y,
            z = z,
            cuboid = boundingCuboid
        )
    }
}

/**
 * 三维轴对齐圆柱体别名
 * Type alias for 3D axis-aligned cylinder
 */
typealias QuantityAxisAlignedCylinder3<V> = QuantityCylinder3<V>
