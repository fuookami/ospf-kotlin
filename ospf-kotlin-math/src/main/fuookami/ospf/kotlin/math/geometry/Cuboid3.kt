/**
 * 三维长方体
 * 3D Cuboid
 *
 * 定义三维几何空间中的长方体形状，由宽、高、深三个维度定义。
 * Defines a cuboid shape in 3D geometric space, defined by width, height, and depth dimensions.
 */
package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber

/**
 * 三维长方体形状，由宽、高、深定义。
 * 3D cuboid shape defined by width, height, and depth.
 *
 * @param V 数值类型 / The numeric type
 * @property width 宽度（X 轴） / Width (X axis)
 * @property height 高度（Y 轴） / Height (Y axis)
 * @property depth 深度（Z 轴） / Depth (Z axis)
 */
data class Cuboid3<V : FloatingNumber<V>>(
    val width: V,
    val height: V,
    val depth: V
) : Shape3<V> {
    override val boundingCuboid: Cuboid3<V> get() = this

    /** 体积 / Volume */
    val volume: V get() = width * height * depth

    /** 在原点处创建包围盒 / Create a bounding box at the origin */
    fun atOrigin(): Box3<V> = Box3.atOrigin(this)

    /**
     * 在指定位置创建包围盒
     * Create a bounding box at the specified position
     *
     * @param x X 坐标 / X coordinate
     * @param y Y 坐标 / Y coordinate
     * @param z Z 坐标 / Z coordinate
     * @return 指定位置的包围盒 / The bounding box at the specified position
     */
    fun at(
        x: V,
        y: V,
        z: V
    ): Box3<V> = Box3(x = x, y = y, z = z, cuboid = this)

    /**
     * 沿指定轴的尺寸
     * Dimension along the specified axis
     *
     * @param axis 目标轴 / The target axis
     * @return 沿该轴的尺寸 / The dimension along the axis
     */
    fun along(axis: Axis3): V {
        return when (axis) {
            Axis3.X -> width
            Axis3.Y -> height
            Axis3.Z -> depth
        }
    }
}
