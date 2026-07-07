/**
 * 二维包围盒
 * 2D bounding box
 *
 * 由位置坐标和投影形状定义的二维包围盒，支持包含测试、重叠检测和求交运算。
 * A 2D bounding box defined by position coordinates and a projection shape, supporting containment tests, overlap detection, and intersection.
 */
package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.utils.functional.Ret

/**
 * 二维包围盒
 * 2D bounding box
 *
 * 由原点坐标和形状定义的二维包围盒，支持包含测试、重叠检测和求交运算。
 * A 2D bounding box defined by origin coordinates and a shape, supporting containment tests, overlap detection, and intersection.
 *
 * @property x 原点的 x 坐标 / x coordinate of the origin
 * @property y 原点的 y 坐标 / y coordinate of the origin
 * @property shape 包围盒的形状（矩形或圆形）/ Shape of the bounding box (rectangle or circle)
 * @param V 数值类型 / Number type
 */
data class QuantityBox2<V : FloatingNumber<V>>(
    val x: Quantity<V>,
    val y: Quantity<V>,
    val shape: QuantityShape2<V>
) {
    companion object {
        /**
         * 在原点创建包围盒
         * Create a bounding box at the origin
         *
         * @param shape 形状 / Shape
         * @param V 数值类型 / Number type
         * @return 原点处的包围盒 / Bounding box at the origin
         */
        fun <V : FloatingNumber<V>> atOrigin(shape: QuantityShape2<V>): QuantityBox2<V> {
            return when (shape) {
                is QuantityRectangle2 -> QuantityBox2(
                    x = quantityZeroOf(shape.width),
                    y = quantityZeroOf(shape.height),
                    shape = shape
                )

                is QuantityCircle2 -> {
                    val zero = quantityZeroOf(shape.radius)
                    QuantityBox2(
                        x = zero,
                        y = zero,
                        shape = shape
                    )
                }
            }
        }
    }

    /** 包围盒宽度 / Bounding box width */
    val width: Quantity<V>
        get() = when (shape) {
            is QuantityRectangle2 -> shape.width
            is QuantityCircle2 -> shape.diameter
        }

    /** 包围盒高度 / Bounding box height */
    val height: Quantity<V>
        get() = when (shape) {
            is QuantityRectangle2 -> shape.height
            is QuantityCircle2 -> shape.diameter
        }

    /** x 方向最大值，失败时返回 null / Maximum x value, or null on failure */
    val maxXOrNull: Quantity<V>? get() = maxX().value
    /** y 方向最大值，失败时返回 null / Maximum y value, or null on failure */
    val maxYOrNull: Quantity<V>? get() = maxY().value

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

    private val centerXOrNull: Quantity<V>? get() = centerX().value
    private val centerYOrNull: Quantity<V>? get() = centerY().value

    /** 计算包围盒中心 X 坐标 / Compute bounding box center X coordinate */
    private fun centerX(): Ret<Quantity<V>> {
        return when (val s = shape) {
            is QuantityRectangle2 -> ok(x)
            is QuantityCircle2 -> quantityPlusSafe(x, s.radius)
        }
    }

    /** 计算包围盒中心 Y 坐标 / Compute bounding box center Y coordinate */
    private fun centerY(): Ret<Quantity<V>> {
        return when (val s = shape) {
            is QuantityRectangle2 -> ok(y)
            is QuantityCircle2 -> quantityPlusSafe(y, s.radius)
        }
    }

    /**
     * 判断点是否在包围盒内
     * Check if a point is inside the bounding box
     *
     * @param x 点的 x 坐标 / x coordinate of the point
     * @param y 点的 y 坐标 / y coordinate of the point
     * @param withLowerBound 是否包含下界 / Whether to include lower bound
     * @param withUpperBound 是否包含上界 / Whether to include upper bound
     * @param withBorder 是否包含边界 / Whether to include border
     * @return 点是否在包围盒内 / Whether the point is inside
     */
    fun contains(
        x: Quantity<V>,
        y: Quantity<V>,
        withLowerBound: Boolean = true,
        withUpperBound: Boolean = true,
        withBorder: Boolean = true
    ): Ret<Boolean> {
        val includeLower = withBorder && withLowerBound
        val includeUpper = withBorder && withUpperBound
        return when (val s = shape) {
            is QuantityRectangle2 -> {
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
                val xIn = when (val result = quantityContainsInRangeSafe(x, this.x, maxX, includeLower, includeUpper, "x")) {
                    is Ok -> result.value
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
                if (!xIn) {
                    return ok(false)
                }
                quantityContainsInRangeSafe(y, this.y, maxY, includeLower, includeUpper, "y")
            }

            is QuantityCircle2 -> {
                val centerX = when (val result = centerX()) {
                    is Ok -> result.value
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
                val centerY = when (val result = centerY()) {
                    is Ok -> result.value
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
                val dx = when (val result = quantityMinusSafe(x, centerX)) {
                    is Ok -> result.value
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
                val dy = when (val result = quantityMinusSafe(y, centerY)) {
                    is Ok -> result.value
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
                val distance2 = when (val result = quantityPlusSafe(quantityProduct(dx, dx), quantityProduct(dy, dy))) {
                    is Ok -> result.value
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
                val radius2 = quantityProduct(s.radius, s.radius)
                val ord = when (val result = quantityOrdSafe(distance2, radius2, "circle-contains")) {
                    is Ok -> result.value
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
                ok(if (withBorder) {
                    ord is Order.Less || ord is Order.Equal
                } else {
                    ord is Order.Less
                })
            }
        }
    }

    /**
     * 判断两个包围盒是否重叠
     * Check if two bounding boxes overlap
     *
     * @param rhs 另一个包围盒 / Another bounding box
     * @return 是否重叠 / Whether they overlap
     */
    fun overlapped(rhs: QuantityBox2<V>): Ret<Boolean> {
        return when (val lhsShape = shape) {
            is QuantityRectangle2 -> when (val rhsShape = rhs.shape) {
                is QuantityRectangle2 -> rectangleOverlapped(rhs)
                is QuantityCircle2 -> rectCircleOverlapped(rhs, rhsShape)
            }

            is QuantityCircle2 -> when (val rhsShape = rhs.shape) {
                is QuantityRectangle2 -> rhs.rectCircleOverlapped(this, lhsShape)
                is QuantityCircle2 -> circleOverlapped(rhs, lhsShape, rhsShape)
            }
        }
    }

    /**
     * 计算两个包围盒的交集
     * Compute the intersection of two bounding boxes
     *
     * @param rhs 另一个包围盒 / Another bounding box
     * @return 交集包围盒，如果不相交则返回 null / Intersection box, or null if they don't intersect
     */
    fun intersect(rhs: QuantityBox2<V>): Ret<QuantityBox2<V>?> {
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
        val xOrd = when (val result = quantityOrdSafe(minX, maxX, "x")) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        if (xOrd !is Order.Less) {
            return ok<QuantityBox2<V>?>(null)
        }
        val yOrd = when (val result = quantityOrdSafe(minY, maxY, "y")) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        if (yOrd !is Order.Less) {
            return ok<QuantityBox2<V>?>(null)
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
        return ok(QuantityBox2(
            x = minX,
            y = minY,
            shape = QuantityRectangle2(
                width = width,
                height = height
            )
        ))
    }

    /** 判断矩形是否重叠 / Check if rectangles overlap */
    private fun rectangleOverlapped(rhs: QuantityBox2<V>): Ret<Boolean> {
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
        return ok(true)
    }

    /** 判断矩形与圆形是否重叠 / Check if rectangle and circle overlap */
    private fun rectCircleOverlapped(circleBox: QuantityBox2<V>, circle: QuantityCircle2<V>): Ret<Boolean> {
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
        val circleCenterX = when (val result = quantityPlusSafe(circleBox.x, circle.radius)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val circleCenterY = when (val result = quantityPlusSafe(circleBox.y, circle.radius)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val closestX = when (val result = quantityClampSafe(circleCenterX, x, maxX, "x")) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val closestY = when (val result = quantityClampSafe(circleCenterY, y, maxY, "y")) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val dx = when (val result = quantityMinusSafe(circleCenterX, closestX)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val dy = when (val result = quantityMinusSafe(circleCenterY, closestY)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val distance2 = when (val result = quantityPlusSafe(quantityProduct(dx, dx), quantityProduct(dy, dy))) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val radius2 = quantityProduct(circle.radius, circle.radius)
        val ord = when (val result = quantityOrdSafe(distance2, radius2, "rect-circle-overlap")) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        return ok(ord is Order.Less || ord is Order.Equal)
    }

    /** 判断两个圆形是否重叠 / Check if two circles overlap */
    private fun circleOverlapped(rhs: QuantityBox2<V>, lhs: QuantityCircle2<V>, rhsCircle: QuantityCircle2<V>): Ret<Boolean> {
        val centerX = when (val result = centerX()) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val centerY = when (val result = centerY()) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val rhsCenterX = when (val result = rhs.centerX()) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val rhsCenterY = when (val result = rhs.centerY()) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val dx = when (val result = quantityMinusSafe(centerX, rhsCenterX)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val dy = when (val result = quantityMinusSafe(centerY, rhsCenterY)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val distance2 = when (val result = quantityPlusSafe(quantityProduct(dx, dx), quantityProduct(dy, dy))) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val reach = when (val result = quantityPlusSafe(lhs.radius, rhsCircle.radius)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val reach2 = quantityProduct(reach, reach)
        val ord = when (val result = quantityOrdSafe(distance2, reach2, "circle-circle-overlap")) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        return ok(ord is Order.Less || ord is Order.Equal)
    }
}

/**
 * 二维轴对齐包围盒别名
 * Type alias for 2D axis-aligned bounding box
 */
typealias QuantityAxisAlignedBox2<V> = QuantityBox2<V>
