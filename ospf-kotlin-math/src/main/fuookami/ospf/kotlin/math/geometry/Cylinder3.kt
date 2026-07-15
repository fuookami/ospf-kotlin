/**
 * 三维圆柱体与轴对齐线段
 * 3D Cylinder and Axis-Aligned Line Segment
 *
 * 定义三维几何空间中的圆柱体和轴对齐线段数据结构。
 * Defines cylinder and axis-aligned line segment data structures in 3D geometric space.
*/
package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 三维轴对齐线段，沿某一轴从 from 到 to。
 * 3D axis-aligned line segment along an axis from `from` to `to`.
 *
 * @param V 数值类型 / The numeric type
 * @property axis 对齐的轴 / The aligned axis
 * @property from 起始值 / Start value
 * @property to 终止值 / End value
*/
data class AxisLine3<V : FloatingNumber<V>>(
    val axis: Axis3,
    val from: V,
    val to: V
)

/**
 * 三维圆柱体，由半径、高度和对齐轴定义。
 * 3D cylinder defined by radius, height, and alignment axis.
 *
 * @param V 数值类型 / The numeric type
 * @property radius 半径 / The radius
 * @property height 高度 / The height
 * @property axis 对齐轴 / The alignment axis
*/
data class Cylinder3<V : FloatingNumber<V>>(
    val radius: V,
    val height: V,
    val axis: Axis3
) : Shape3<V> {

    /** 直径 / The diameter */
    val diameter: V get() = quantityPlus(radius, radius)

    override val boundingCuboid: Cuboid3<V>
        get() {
            return when (axis) {
                Axis3.X -> Cuboid3(width = height, height = diameter, depth = diameter)
                Axis3.Y -> Cuboid3(width = diameter, height = height, depth = diameter)
                Axis3.Z -> Cuboid3(width = diameter, height = diameter, depth = height)
            }
        }

    /**
     * 沿指定轴的尺寸（轴向为高度，其余为直径）
     * Dimension along the specified axis (axial=height, others=diameter)
     *
     * @param axis 目标轴 / The target axis
     * @return 沿该轴的尺寸 / The dimension along the axis
    */
    fun along(axis: Axis3): V {
        return if (axis == this.axis) {
            height
        } else {
            diameter
        }
    }

    /** 原点处的轴线段 / The axis line segment at the origin */
    val axisLineAtOrigin: AxisLine3<V>
        get() = AxisLine3(
            axis = axis,
            from = quantityZeroOf(height),
            to = height
        )

    /**
     * 在指定平面上的投影形状
     * The projection shape on the specified plane
     *
     * @param plane 目标平面 / The target plane
     * @return 投影形状 / The projection shape
    */
    fun projectionOn(plane: AxisPlane3): Projection2<V> {
        return if (plane.contains(axis)) {
            Rectangle2(
                width = along(plane.firstAxis),
                height = along(plane.secondAxis)
            )
        } else {
            Circle2(radius)
        }
    }

    /**
     * 计算底面积
     * Compute the base area
     *
     * @param pi 圆周率值 / The pi value
     * @return 底面积 / The base area
    */
    fun baseArea(pi: V): V = (radius * radius) * pi

    /**
     * 计算体积
     * Compute the volume
     *
     * @param pi 圆周率值 / The pi value
     * @return 体积 / The volume
    */
    fun volume(pi: V): V = baseArea(pi) * height

    /**
     * 按轴置换
     * Permute by axes
     *
     * @param permutation 轴置换方案 / The axis permutation
     * @return 置换后的圆柱结果 / The permuted cylinder result
    */
    fun permute(permutation: AxisPermutation3): Ret<Cylinder3<V>> = permutation.apply(this)

    /**
     * 在原点处创建包围盒
     * Create a bounding box at the origin
     *
     * @return 原点处的包围盒 / The bounding box at the origin
    */
    fun boundingBoxAtOrigin(): Box3<V> = Box3.atOrigin(boundingCuboid)

    /**
     * 在指定位置创建包围盒
     * Create a bounding box at the specified position
     *
     * @param x X 坐标 / X coordinate
     * @param y Y 坐标 / Y coordinate
     * @param z Z 坐标 / Z coordinate
     * @return 指定位置的包围盒 / The bounding box at the specified position
    */
    fun toBoundingBox(
        x: V,
        y: V,
        z: V
    ): Box3<V> {
        return Box3(
            x = x,
            y = y,
            z = z,
            cuboid = boundingCuboid
        )
    }
}

/**
 * 三维轴对齐圆柱体，等同于 Cylinder3。
 * 3D axis-aligned cylinder, equivalent to Cylinder3.
 *
 * @param V 数值类型 / The numeric type
 * @see Cylinder3
*/
typealias AxisAlignedCylinder3<V> = Cylinder3<V>
