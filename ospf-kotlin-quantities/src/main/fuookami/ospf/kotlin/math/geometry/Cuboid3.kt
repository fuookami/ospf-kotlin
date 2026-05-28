/**
 * 三维长方体形状
 * 3D cuboid shape
 *
 * 由宽度、高度和深度定义的三维长方体，支持体积计算和轴置换。
 * A 3D cuboid defined by width, height, and depth, supporting volume calculation and axis permutation.
 */
package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.quantities.quantity.*

/**
 * 三维长方体形状
 * 3D cuboid shape
 *
 * 由宽度、高度和深度定义的三维长方体，实现 QuantityShape3 接口。
 * A 3D cuboid defined by width, height, and depth, implementing the QuantityShape3 interface.
 *
 * @property width 宽度（x 方向）/ Width (x direction)
 * @property height 高度（y 方向）/ Height (y direction)
 * @property depth 深度（z 方向）/ Depth (z direction)
 * @param V 数值类型 / Number type
 */
data class QuantityCuboid3<V : FloatingNumber<V>>(
    val width: Quantity<V>,
    val height: Quantity<V>,
    val depth: Quantity<V>
) : QuantityShape3<V> {
    /** 最小包围长方体（自身）/ Minimum bounding cuboid (self) */
    override val boundingCuboid: QuantityCuboid3<V> get() = this

    /** 体积 / Volume */
    val volume: Quantity<V> get() = width * height * depth

    /**
     * 在原点创建包围盒
     * Create a bounding box at the origin
     *
     * @return 原点处的包围盒 / Bounding box at the origin
     */
    fun atOrigin(): QuantityBox3<V> = QuantityBox3.atOrigin(this)

    /**
     * 在指定位置创建包围盒
     * Create a bounding box at the specified position
     *
     * @param x x 坐标 / x coordinate
     * @param y y 坐标 / y coordinate
     * @param z z 坐标 / z coordinate
     * @return 指定位置的包围盒 / Bounding box at the specified position
     */
    fun at(
        x: Quantity<V>,
        y: Quantity<V>,
        z: Quantity<V>
    ): QuantityBox3<V> = QuantityBox3(x = x, y = y, z = z, cuboid = this)

    /**
     * 获取沿指定轴的尺寸
     * Get the dimension along a specified axis
     *
     * @param axis 目标轴 / Target axis
     * @return 沿该轴的尺寸 / Dimension along the axis
     */
    fun along(axis: Axis3): Quantity<V> {
        return when (axis) {
            Axis3.X -> width
            Axis3.Y -> height
            Axis3.Z -> depth
        }
    }
}
