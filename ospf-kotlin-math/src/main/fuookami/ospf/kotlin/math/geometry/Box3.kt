/**
 * 三维包围盒
 * 3D Bounding Box
 *
 * 定义三维几何空间中的包围盒，由位置 (x, y, z) 和长方体形状定义。
 * Defines bounding box in 3D geometric space, defined by position (x, y, z) and cuboid shape.
 */
package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber

/**
 * 三维包围盒，由位置 (x, y, z) 和长方体形状定义。
 * 3D bounding box defined by position (x, y, z) and cuboid shape.
 *
 * @param V 数值类型 / The numeric type
 * @property x X 坐标 / X coordinate
 * @property y Y 坐标 / Y coordinate
 * @property z Z 坐标 / Z coordinate
 * @property cuboid 包围的长方体 / The enclosed cuboid
 */
data class Box3<V : FloatingNumber<V>>(
    val x: V,
    val y: V,
    val z: V,
    val cuboid: Cuboid3<V>
) {
    companion object {
        /**
         * 在原点处创建包围盒
         * Create a bounding box at the origin
         *
         * @param V 数值类型 / The numeric type
         * @param cuboid 长方体 / The cuboid
         * @return 原点处的包围盒 / The bounding box at the origin
         */
        fun <V : FloatingNumber<V>> atOrigin(cuboid: Cuboid3<V>): Box3<V> {
            return Box3(
                x = quantityZeroOf(cuboid.width),
                y = quantityZeroOf(cuboid.height),
                z = quantityZeroOf(cuboid.depth),
                cuboid = cuboid
            )
        }
    }

    /** 宽度 / Width */
    val width get() = cuboid.width
    /** 高度 / Height */
    val height get() = cuboid.height
    /** 深度 / Depth */
    val depth get() = cuboid.depth

    /** X 轴最大值 / Maximum X value */
    val maxX: V get() = quantityPlus(x, width)
    /** Y 轴最大值 / Maximum Y value */
    val maxY: V get() = quantityPlus(y, height)
    /** Z 轴最大值 / Maximum Z value */
    val maxZ: V get() = quantityPlus(z, depth)

    /**
     * 判断指定点是否在包围盒内
     * Check whether a point is inside the bounding box
     *
     * @param x X 坐标 / X coordinate
     * @param y Y 坐标 / Y coordinate
     * @param z Z 坐标 / Z coordinate
     * @param withLowerBound 是否包含下界 / Whether to include the lower bound
     * @param withUpperBound 是否包含上界 / Whether to include the upper bound
     * @param withBorder 是否包含边界 / Whether to include the border
     * @return 点是否在包围盒内 / Whether the point is inside the bounding box
     */
    fun contains(
        x: V,
        y: V,
        z: V,
        withLowerBound: Boolean = true,
        withUpperBound: Boolean = true,
        withBorder: Boolean = true
    ): Boolean {
        val includeLower = withBorder && withLowerBound
        val includeUpper = withBorder && withUpperBound
        return quantityContainsInRange(x, this.x, maxX, includeLower, includeUpper, "x")
                && quantityContainsInRange(y, this.y, maxY, includeLower, includeUpper, "y")
                && quantityContainsInRange(z, this.z, maxZ, includeLower, includeUpper, "z")
    }

    /**
     * 判断两个包围盒是否重叠
     * Check whether two bounding boxes overlap
     *
     * @param rhs 另一个包围盒 / The other bounding box
     * @return 是否重叠 / Whether they overlap
     */
    fun overlapped(rhs: Box3<V>): Boolean {
        if (quantityOrd(maxX, rhs.x, "x") !is Order.Greater) {
            return false
        }
        if (quantityOrd(x, rhs.maxX, "x") !is Order.Less) {
            return false
        }
        if (quantityOrd(maxY, rhs.y, "y") !is Order.Greater) {
            return false
        }
        if (quantityOrd(y, rhs.maxY, "y") !is Order.Less) {
            return false
        }
        if (quantityOrd(maxZ, rhs.z, "z") !is Order.Greater) {
            return false
        }
        if (quantityOrd(z, rhs.maxZ, "z") !is Order.Less) {
            return false
        }
        return true
    }

    /**
     * 计算两个包围盒的交集，无交集返回 null
     * Compute intersection of two boxes, returns null if no overlap
     *
     * @param rhs 另一个包围盒 / The other bounding box
     * @return 交集包围盒，无交集返回 null / The intersection box, or null if no overlap
     */
    fun intersect(rhs: Box3<V>): Box3<V>? {
        val minX = quantityMax(x, rhs.x, "x")
        val maxX = quantityMin(this.maxX, rhs.maxX, "x")
        val minY = quantityMax(y, rhs.y, "y")
        val maxY = quantityMin(this.maxY, rhs.maxY, "y")
        val minZ = quantityMax(z, rhs.z, "z")
        val maxZ = quantityMin(this.maxZ, rhs.maxZ, "z")
        if (quantityOrd(minX, maxX, "x") !is Order.Less) {
            return null
        }
        if (quantityOrd(minY, maxY, "y") !is Order.Less) {
            return null
        }
        if (quantityOrd(minZ, maxZ, "z") !is Order.Less) {
            return null
        }
        return Box3(
            x = minX,
            y = minY,
            z = minZ,
            cuboid = Cuboid3(
                width = quantityMinus(maxX, minX),
                height = quantityMinus(maxY, minY),
                depth = quantityMinus(maxZ, minZ)
            )
        )
    }
}

/**
 * 三维轴对齐包围盒，等同于 Box3。
 * 3D axis-aligned bounding box, equivalent to Box3.
 *
 * @param V 数值类型 / The numeric type
 * @see Box3
 */
typealias AxisAlignedBox3<V> = Box3<V>
