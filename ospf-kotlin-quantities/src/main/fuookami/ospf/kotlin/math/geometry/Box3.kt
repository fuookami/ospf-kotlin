/**
 * 三维包围盒
 * 3D bounding box
 *
 * 由位置坐标和长方体形状定义的三维包围盒，支持包含测试、重叠检测和求交运算。
 * A 3D bounding box defined by position coordinates and a cuboid shape, supporting containment tests, overlap detection, and intersection.
 */
package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.utils.functional.Order

/**
 * 三维包围盒
 * 3D bounding box
 *
 * 由原点坐标和长方体形状定义的三维包围盒，支持包含测试、重叠检测和求交运算。
 * A 3D bounding box defined by origin coordinates and a cuboid shape, supporting containment tests, overlap detection, and intersection.
 *
 * @property x 原点的 x 坐标 / x coordinate of the origin
 * @property y 原点的 y 坐标 / y coordinate of the origin
 * @property z 原点的 z 坐标 / z coordinate of the origin
 * @property cuboid 包围盒的长方体形状 / Cuboid shape of the bounding box
 * @param V 数值类型 / Number type
 */
data class QuantityBox3<V : FloatingNumber<V>>(
    val x: Quantity<V>,
    val y: Quantity<V>,
    val z: Quantity<V>,
    val cuboid: QuantityCuboid3<V>
) {
    companion object {
        /**
         * 在原点创建包围盒
         * Create a bounding box at the origin
         *
         * @param cuboid 长方体形状 / Cuboid shape
         * @param V 数值类型 / Number type
         * @return 原点处的包围盒 / Bounding box at the origin
         */
        fun <V : FloatingNumber<V>> atOrigin(cuboid: QuantityCuboid3<V>): QuantityBox3<V> {
            return QuantityBox3(
                x = quantityZeroOf(cuboid.width),
                y = quantityZeroOf(cuboid.height),
                z = quantityZeroOf(cuboid.depth),
                cuboid = cuboid
            )
        }
    }

    /** 包围盒宽度 / Bounding box width */
    val width get() = cuboid.width
    /** 包围盒高度 / Bounding box height */
    val height get() = cuboid.height
    /** 包围盒深度 / Bounding box depth */
    val depth get() = cuboid.depth

    /** x 方向最大值 / Maximum x value */
    val maxX: Quantity<V> get() = quantityPlus(x, width)
    /** y 方向最大值 / Maximum y value */
    val maxY: Quantity<V> get() = quantityPlus(y, height)
    /** z 方向最大值 / Maximum z value */
    val maxZ: Quantity<V> get() = quantityPlus(z, depth)

    /**
     * 判断点是否在包围盒内
     * Check if a point is inside the bounding box
     *
     * @param x 点的 x 坐标 / x coordinate of the point
     * @param y 点的 y 坐标 / y coordinate of the point
     * @param z 点的 z 坐标 / z coordinate of the point
     * @param withLowerBound 是否包含下界 / Whether to include lower bound
     * @param withUpperBound 是否包含上界 / Whether to include upper bound
     * @param withBorder 是否包含边界 / Whether to include border
     * @return 点是否在包围盒内 / Whether the point is inside
     */
    fun contains(
        x: Quantity<V>,
        y: Quantity<V>,
        z: Quantity<V>,
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
     * Check if two bounding boxes overlap
     *
     * @param rhs 另一个包围盒 / Another bounding box
     * @return 是否重叠 / Whether they overlap
     */
    fun overlapped(rhs: QuantityBox3<V>): Boolean {
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
     * 计算两个包围盒的交集
     * Compute the intersection of two bounding boxes
     *
     * @param rhs 另一个包围盒 / Another bounding box
     * @return 交集包围盒，如果不相交则返回 null / Intersection box, or null if they don't intersect
     */
    fun intersect(rhs: QuantityBox3<V>): QuantityBox3<V>? {
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
        return QuantityBox3(
            x = minX,
            y = minY,
            z = minZ,
            cuboid = QuantityCuboid3(
                width = quantityMinus(maxX, minX),
                height = quantityMinus(maxY, minY),
                depth = quantityMinus(maxZ, minZ)
            )
        )
    }
}

/**
 * 三维轴对齐包围盒别名
 * Type alias for 3D axis-aligned bounding box
 */
typealias QuantityAxisAlignedBox3<V> = QuantityBox3<V>
