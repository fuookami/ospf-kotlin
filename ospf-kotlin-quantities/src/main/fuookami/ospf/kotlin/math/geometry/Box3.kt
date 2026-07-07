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
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.utils.functional.Ret

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

    /** x 方向最大值，失败时返回 null / Maximum x value, or null on failure */
    val maxXOrNull: Quantity<V>? get() = maxX().value
    /** y 方向最大值，失败时返回 null / Maximum y value, or null on failure */
    val maxYOrNull: Quantity<V>? get() = maxY().value
    /** z 方向最大值，失败时返回 null / Maximum z value, or null on failure */
    val maxZOrNull: Quantity<V>? get() = maxZ().value

    /**
     * 获取 x 方向最大值
     * Get the maximum x value
     *
     * @return x 方向最大值 / Maximum x value
     */
    fun maxX(): Ret<Quantity<V>> = quantityPlusSafe(x, width)

    /**
     * 获取 y 方向最大值
     * Get the maximum y value
     *
     * @return y 方向最大值 / Maximum y value
     */
    fun maxY(): Ret<Quantity<V>> = quantityPlusSafe(y, height)

    /**
     * 获取 z 方向最大值
     * Get the maximum z value
     *
     * @return z 方向最大值 / Maximum z value
     */
    fun maxZ(): Ret<Quantity<V>> = quantityPlusSafe(z, depth)

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
    ): Ret<Boolean> {
        val includeLower = withBorder && withLowerBound
        val includeUpper = withBorder && withUpperBound
        val maxX = when (val result = maxX()) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val maxY = when (val result = maxY()) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val maxZ = when (val result = maxZ()) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val xIn = when (val result = quantityContainsInRangeSafe(x, this.x, maxX, includeLower, includeUpper, "x")) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        if (!xIn) {
            return ok(false)
        }
        val yIn = when (val result = quantityContainsInRangeSafe(y, this.y, maxY, includeLower, includeUpper, "y")) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        if (!yIn) {
            return ok(false)
        }
        return quantityContainsInRangeSafe(z, this.z, maxZ, includeLower, includeUpper, "z")
    }

    /**
     * 判断两个包围盒是否重叠
     * Check if two bounding boxes overlap
     *
     * @param rhs 另一个包围盒 / Another bounding box
     * @return 是否重叠 / Whether they overlap
     */
    fun overlapped(rhs: QuantityBox3<V>): Ret<Boolean> {
        val thisMaxX = when (val result = maxX()) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val thisMaxY = when (val result = maxY()) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val thisMaxZ = when (val result = maxZ()) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val rhsMaxX = when (val result = rhs.maxX()) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val rhsMaxY = when (val result = rhs.maxY()) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val rhsMaxZ = when (val result = rhs.maxZ()) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val maxXOrd = when (val result = quantityOrdSafe(thisMaxX, rhs.x, "x")) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        if (maxXOrd !is Order.Greater) {
            return ok(false)
        }
        val xOrd = when (val result = quantityOrdSafe(x, rhsMaxX, "x")) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        if (xOrd !is Order.Less) {
            return ok(false)
        }
        val maxYOrd = when (val result = quantityOrdSafe(thisMaxY, rhs.y, "y")) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        if (maxYOrd !is Order.Greater) {
            return ok(false)
        }
        val yOrd = when (val result = quantityOrdSafe(y, rhsMaxY, "y")) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        if (yOrd !is Order.Less) {
            return ok(false)
        }
        val maxZOrd = when (val result = quantityOrdSafe(thisMaxZ, rhs.z, "z")) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        if (maxZOrd !is Order.Greater) {
            return ok(false)
        }
        val zOrd = when (val result = quantityOrdSafe(z, rhsMaxZ, "z")) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        if (zOrd !is Order.Less) {
            return ok(false)
        }
        return ok(true)
    }

    /**
     * 计算两个包围盒的交集
     * Compute the intersection of two bounding boxes
     *
     * @param rhs 另一个包围盒 / Another bounding box
     * @return 交集包围盒，如果不相交则返回 null / Intersection box, or null if they don't intersect
     */
    fun intersect(rhs: QuantityBox3<V>): Ret<QuantityBox3<V>?> {
        val thisMaxX = when (val result = maxX()) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val thisMaxY = when (val result = maxY()) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val thisMaxZ = when (val result = maxZ()) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val rhsMaxX = when (val result = rhs.maxX()) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val rhsMaxY = when (val result = rhs.maxY()) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val rhsMaxZ = when (val result = rhs.maxZ()) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val minX = when (val result = quantityMaxSafe(x, rhs.x, "x")) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val maxX = when (val result = quantityMinSafe(thisMaxX, rhsMaxX, "x")) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val minY = when (val result = quantityMaxSafe(y, rhs.y, "y")) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val maxY = when (val result = quantityMinSafe(thisMaxY, rhsMaxY, "y")) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val minZ = when (val result = quantityMaxSafe(z, rhs.z, "z")) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val maxZ = when (val result = quantityMinSafe(thisMaxZ, rhsMaxZ, "z")) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val xOrd = when (val result = quantityOrdSafe(minX, maxX, "x")) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        if (xOrd !is Order.Less) {
            return ok<QuantityBox3<V>?>(null)
        }
        val yOrd = when (val result = quantityOrdSafe(minY, maxY, "y")) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        if (yOrd !is Order.Less) {
            return ok<QuantityBox3<V>?>(null)
        }
        val zOrd = when (val result = quantityOrdSafe(minZ, maxZ, "z")) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        if (zOrd !is Order.Less) {
            return ok<QuantityBox3<V>?>(null)
        }
        val width = when (val result = quantityMinusSafe(maxX, minX)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val height = when (val result = quantityMinusSafe(maxY, minY)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val depth = when (val result = quantityMinusSafe(maxZ, minZ)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        return ok(QuantityBox3(
            x = minX,
            y = minY,
            z = minZ,
            cuboid = QuantityCuboid3(
                width = width,
                height = height,
                depth = depth
            )
        ))
    }
}

/**
 * 三维轴对齐包围盒别名
 * Type alias for 3D axis-aligned bounding box
 */
typealias QuantityAxisAlignedBox3<V> = QuantityBox3<V>
