/**
 * 三维放置
 * 3D Placement
 *
 * 定义三维几何空间中的放置操作，将形状放置在指定 (x, y, z) 位置。
 * Defines placement operation in 3D geometric space, placing a shape at a specified (x, y, z) position.
 */
package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber

/**
 * 三维放置，将一个三维形状放置在 (x, y, z) 位置。
 * 3D placement: places a 3D shape at position (x, y, z).
 *
 * @param V 数值类型 / The numeric type
 * @property x X 坐标 / X coordinate
 * @property y Y 坐标 / Y coordinate
 * @property z Z 坐标 / Z coordinate
 * @property shape 放置的三维形状 / The 3D shape being placed
 */
data class Placement3<V : FloatingNumber<V>>(
    val x: V,
    val y: V,
    val z: V,
    val shape: Shape3<V>
) {
    /** 包围盒 / Bounding box */
    val box: Box3<V>
        get() = Box3(
            x = x,
            y = y,
            z = z,
            cuboid = shape.boundingCuboid
        )

    /** 宽度 / Width */
    val width: V get() = box.width
    /** 高度 / Height */
    val height: V get() = box.height
    /** 深度 / Depth */
    val depth: V get() = box.depth
    /** X 轴最大值 / Maximum X value */
    val maxX: V get() = box.maxX
    /** Y 轴最大值 / Maximum Y value */
    val maxY: V get() = box.maxY
    /** Z 轴最大值 / Maximum Z value */
    val maxZ: V get() = box.maxZ

    /**
     * 判断指定点是否在放置区域内
     * Check whether a point is inside the placement region
     *
     * @param x X 坐标 / X coordinate
     * @param y Y 坐标 / Y coordinate
     * @param z Z 坐标 / Z coordinate
     * @param withLowerBound 是否包含下界 / Whether to include the lower bound
     * @param withUpperBound 是否包含上界 / Whether to include the upper bound
     * @param withBorder 是否包含边界 / Whether to include the border
     * @return 点是否在放置区域内 / Whether the point is inside the placement region
     */
    fun contains(
        x: V,
        y: V,
        z: V,
        withLowerBound: Boolean = true,
        withUpperBound: Boolean = true,
        withBorder: Boolean = true
    ): Boolean {
        return box.contains(
            x = x,
            y = y,
            z = z,
            withLowerBound = withLowerBound,
            withUpperBound = withUpperBound,
            withBorder = withBorder
        )
    }

    /**
     * 判断两个放置是否重叠
     * Check whether two placements overlap
     *
     * @param rhs 另一个放置 / The other placement
     * @return 是否重叠 / Whether they overlap
     */
    fun overlapped(rhs: Placement3<V>): Boolean = box.overlapped(rhs.box)

    /**
     * 计算两个放置的交集，无交集返回 null
     * Compute intersection of two placements, returns null if no overlap
     *
     * @param rhs 另一个放置 / The other placement
     * @return 交集放置，无交集返回 null / The intersection placement, or null if no overlap
     */
    fun intersect(rhs: Placement3<V>): Placement3<V>? {
        val intersected = box.intersect(rhs.box) ?: return null
        return Placement3(
            x = intersected.x,
            y = intersected.y,
            z = intersected.z,
            shape = intersected.cuboid
        )
    }
}
