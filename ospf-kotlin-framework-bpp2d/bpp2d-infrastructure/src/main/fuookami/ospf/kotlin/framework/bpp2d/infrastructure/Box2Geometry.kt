/**
 * 二维包围盒几何。 / 2D bounding box geometry.
 *
 * 在 `QuantityBox2` 之上提供装箱所需的角点、包含与相交能力。圆形件以外接正方形表示，
 * 圆心位于该正方形中心，因此圆×圆、圆×矩形、矩形×矩形的重叠判定都可以直接委托给
 * `QuantityBox2.overlapped`，无需另写相交算法。
 * / Adds corners, containment and intersection on top of `QuantityBox2`. A circle is represented by its
 * enclosing square with the circle centre at its centre, so circle-circle, circle-rectangle and
 * rectangle-rectangle overlap tests all delegate to `QuantityBox2.overlapped`.
*/
package fuookami.ospf.kotlin.framework.bpp2d.infrastructure

import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.geometry.QuantityBox2
import fuookami.ospf.kotlin.math.geometry.QuantityCircle2
import fuookami.ospf.kotlin.math.geometry.QuantityProjection2
import fuookami.ospf.kotlin.math.geometry.QuantityRectangle2
import fuookami.ospf.kotlin.quantities.quantity.Quantity

/**
 * 二维包围盒，用于装载形状之间的重叠与包含判定。 / 2D bounding box used for overlap and containment tests.
 *
 * @param V 数值类型 / numeric type
 * @property x 左下角 x 坐标 / lower-left x coordinate
 * @property y 左下角 y 坐标 / lower-left y coordinate
 * @property shape 形状投影 / shape projection
*/
data class Box2Geometry<V : FloatingNumber<V>>(
    val x: Quantity<V>,
    val y: Quantity<V>,
    val shape: QuantityProjection2<V>
) : Copyable<Box2Geometry<V>> {
    companion object {

        /**
         * 由角点与尺寸构造矩形包围盒。 / Build a rectangle box from an origin and a size.
         *
         * @param V 数值类型 / numeric type
         * @param x 左下角 x 坐标 / lower-left x coordinate
         * @param y 左下角 y 坐标 / lower-left y coordinate
         * @param width 宽度 / width
         * @param height 高度 / height
         * @return 矩形包围盒 / rectangle box
        */
        fun <V : FloatingNumber<V>> of(
            x: Quantity<V>,
            y: Quantity<V>,
            width: Quantity<V>,
            height: Quantity<V>
        ): Box2Geometry<V> {
            return Box2Geometry(
                x = x,
                y = y,
                shape = QuantityRectangle2(
                    width = width,
                    height = height
                )
            )
        }
    }

    /** 宽度 / width */
    val width: Quantity<V>
        get() = when (val concrete = shape) {
            is QuantityRectangle2 -> concrete.width
            is QuantityCircle2 -> concrete.diameter
        }

    /** 高度 / height */
    val height: Quantity<V>
        get() = when (val concrete = shape) {
            is QuantityRectangle2 -> concrete.height
            is QuantityCircle2 -> concrete.diameter
        }

    /** 中心 x 坐标，圆形件为圆心 x。 / center x; the circle centre for circle shapes. */
    val centerX: Quantity<V>
        get() = when (val concrete = shape) {
            is QuantityRectangle2 -> x
            is QuantityCircle2 -> x + concrete.radius
        }

    /** 中心 y 坐标，圆形件为圆心 y。 / center y; the circle centre for circle shapes. */
    val centerY: Quantity<V>
        get() = when (val concrete = shape) {
            is QuantityRectangle2 -> y
            is QuantityCircle2 -> y + concrete.radius
        }

    /**
     * 右上角 x 坐标。 / Top-right x coordinate.
     *
     * @return x 坐标或失败 / x coordinate, or failure
    */
    fun maxX(): Ret<Quantity<V>> {
        return quantityPlusSafe(x, width)
    }

    /**
     * 右上角 y 坐标。 / Top-right y coordinate.
     *
     * @return y 坐标或失败 / y coordinate, or failure
    */
    fun maxY(): Ret<Quantity<V>> {
        return quantityPlusSafe(y, height)
    }

    /**
     * 判断与另一包围盒是否重叠。 / Check overlap with another box.
     *
     * 语义统一为「内部重叠」：**相切不算重叠**。矩形贴边、圆与圆相切、圆与矩形边界相切都视为不重叠。
     * 这与网格化排样的前提一致——相切是排样中最常见的相邻关系，若把相切判为重叠，任何规则网格都无法成立。
     * / The semantics are uniformly "interior overlap": **tangency is not an overlap**. Edge contact between
     * rectangles, tangent circles, and a circle tangent to a rectangle edge are all disjoint. This matches the
     * premise of grid packing: tangency is the most common adjacency, and treating it as overlap would make
     * every regular grid invalid.
     *
     * @param rhs 另一包围盒 / other box
     * @return 是否重叠或失败 / whether they overlap, or failure
    */
    fun overlapped(rhs: Box2Geometry<V>): Ret<Boolean> {
        val lhsCircle = shape as? QuantityCircle2
        val rhsCircle = rhs.shape as? QuantityCircle2
        return when {
            lhsCircle != null && rhsCircle != null -> ok(
                circleCircleOverlapped(this, rhs)
            )

            lhsCircle != null -> ok(
                circleRectangleOverlapped(this, rhs)
            )

            rhsCircle != null -> ok(
                circleRectangleOverlapped(rhs, this)
            )

            else -> ok(rectangleOverlapped(rhs))
        }
    }

    /**
     * 圆与圆的内部重叠判定：圆心距严格小于半径之和时重叠，相切不算。 / Interior overlap between two circles:
     * overlapping when the centre distance is strictly less than the sum of the radii; tangency is excluded.
     *
     * @param circleBox 左圆外接盒 / enclosing box of the left circle
     * @param rhs 右圆外接盒 / enclosing box of the right circle
     * @return 是否重叠 / whether they overlap
    */
    private fun circleCircleOverlapped(
        circleBox: Box2Geometry<V>,
        rhs: Box2Geometry<V>
    ): Boolean {
        val lhsCircle = circleBox.shape as? QuantityCircle2<V> ?: return false
        val rhsCircle = rhs.shape as? QuantityCircle2<V> ?: return false
        // 圆心 = 外接正方形左下角 + 半径。 / Centre = enclosing square lower-left corner + radius.
        val lhsCenterX = circleBox.x + lhsCircle.radius
        val lhsCenterY = circleBox.y + lhsCircle.radius
        val rhsCenterX = rhs.x + rhsCircle.radius
        val rhsCenterY = rhs.y + rhsCircle.radius
        val dx = lhsCenterX - rhsCenterX
        val dy = lhsCenterY - rhsCenterY
        val distanceSquared = (dx * dx) + (dy * dy)
        val reach = lhsCircle.radius + rhsCircle.radius
        val reachSquared = reach * reach
        return quantityLess(distanceSquared, reachSquared)
    }

    /**
     * 圆与矩形的内部重叠判定：圆心到矩形最近点的距离严格小于半径时重叠，相切不算。 / Interior overlap between a
     * circle and a rectangle: overlapping when the distance from the centre to the closest point on the
     * rectangle is strictly less than the radius; tangency is excluded.
     *
     * @param circleBox 圆外接盒 / enclosing box of the circle
     * @param rectangle 矩形外接盒 / enclosing box of the rectangle
     * @return 是否重叠 / whether they overlap
    */
    private fun circleRectangleOverlapped(
        circleBox: Box2Geometry<V>,
        rectangle: Box2Geometry<V>
    ): Boolean {
        val circle = circleBox.shape as? QuantityCircle2<V> ?: return false
        val rectangleMaxX = when (val result = rectangle.maxX()) {
            is Ok -> result.value
            is Failed -> return false
            is Fatal -> return false
        }
        val rectangleMaxY = when (val result = rectangle.maxY()) {
            is Ok -> result.value
            is Failed -> return false
            is Fatal -> return false
        }
        val circleCenterX = circleBox.x + circle.radius
        val circleCenterY = circleBox.y + circle.radius
        val closestX = clamp(circleCenterX, rectangle.x, rectangleMaxX)
        val closestY = clamp(circleCenterY, rectangle.y, rectangleMaxY)
        val dx = circleCenterX - closestX
        val dy = circleCenterY - closestY
        val distanceSquared = (dx * dx) + (dy * dy)
        val radiusSquared = circle.radius * circle.radius
        return quantityLess(distanceSquared, radiusSquared)
    }

    /**
     * 把量值限制在闭区间内。 / Clamp a quantity into a closed range.
     *
     * @param value 待限制量值 / value to clamp
     * @param lower 下界 / lower bound
     * @param upper 上界 / upper bound
     * @return 限制后的量值 / clamped value
    */
    private fun clamp(value: Quantity<V>, lower: Quantity<V>, upper: Quantity<V>): Quantity<V> {
        if (quantityLess(value, lower)) {
            return lower
        }
        if (quantityGreater(value, upper)) {
            return upper
        }
        return value
    }

    /**
     * 矩形件的精确重叠判定，含贴边不算重叠的语义。 / Exact rectangle overlap, treating edge contact as disjoint.
     *
     * @param rhs 另一矩形包围盒 / other rectangle box
     * @return 是否重叠 / whether they overlap
    */
    private fun rectangleOverlapped(rhs: Box2Geometry<V>): Boolean {
        val rhsMaxX = when (val result = rhs.maxX()) {
            is Ok -> result.value
            is Failed -> return false
            is Fatal -> return false
        }
        val rhsMaxY = when (val result = rhs.maxY()) {
            is Ok -> result.value
            is Failed -> return false
            is Fatal -> return false
        }
        val ownMaxX = when (val result = maxX()) {
            is Ok -> result.value
            is Failed -> return false
            is Fatal -> return false
        }
        val ownMaxY = when (val result = maxY()) {
            is Ok -> result.value
            is Failed -> return false
            is Fatal -> return false
        }
        return quantityGreater(ownMaxX, rhs.x)
                && quantityGreater(rhsMaxX, x)
                && quantityGreater(ownMaxY, rhs.y)
                && quantityGreater(rhsMaxY, y)
    }

    /**
     * 判断本包围盒是否完全位于另一包围盒内部（允许贴边）。 / Check whether this box lies entirely inside another box, touching allowed.
     *
     * @param outer 外部包围盒 / outer box
     * @return 是否位于内部或失败 / whether inside, or failure
    */
    fun inside(outer: Box2Geometry<V>): Ret<Boolean> {
        val ownMaxX = when (val result = maxX()) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val ownMaxY = when (val result = maxY()) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val outerMaxX = when (val result = outer.maxX()) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val outerMaxY = when (val result = outer.maxY()) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        return ok(
            quantityGreaterOrEqual(x, outer.x)
                    && quantityGreaterOrEqual(y, outer.y)
                    && quantityLessOrEqual(ownMaxX, outerMaxX)
                    && quantityLessOrEqual(ownMaxY, outerMaxY)
        )
    }

    /**
     * 计算与另一矩形包围盒的交集面积。 / Compute the intersection area with another rectangle box.
     *
     * @param rhs 另一矩形包围盒 / other rectangle box
     * @return 交集面积，无交集时为零 / intersection area, zero when disjoint
    */
    fun overlapArea(rhs: Box2Geometry<V>): Quantity<V> {
        val zero = quantityZeroOf(x)
        val intersection = when (val result = toQuantityBox().intersect(rhs.toQuantityBox())) {
            is Ok -> result.value
            is Failed -> null
            is Fatal -> null
        } ?: return zero * zero
        return intersection.width * intersection.height
    }

    /**
     * 构造本包围盒对应的 `QuantityBox2`。 / Build the underlying `QuantityBox2`.
     *
     * @return 底层包围盒 / underlying box
    */
    private fun toQuantityBox(): QuantityBox2<V> {
        return QuantityBox2(
            x = x,
            y = y,
            shape = shape
        )
    }

    /**
     * 判断包围盒是否只有一个点（退化面积为零）。 / Check whether the box has a degenerate zero extent.
     *
     * @return 是否退化 / whether degenerate
    */
    fun isDegenerate(): Boolean {
        val zero = quantityZeroOf(x)
        return quantityLessOrEqual(width, zero) || quantityLessOrEqual(height, zero)
    }

    override fun copy(): Box2Geometry<V> {
        return Box2Geometry(
            x = x,
            y = y,
            shape = shape
        )
    }
}

/**
 * 构造零值矩形的错误。 / Error raised when a degenerate box is requested.
 *
 * @param message 错误消息 / error message
 * @return 失败结果 / failure
*/
internal fun <V : FloatingNumber<V>> degenerateBoxError(message: String): Ret<Box2Geometry<V>> {
    return Failed(ErrorCode.IllegalArgument, message)
}

/**
 * 判断比较结果是否表示严格重叠轴。 / Check whether an axis comparison indicates a strict overlap.
 *
 * @param order 比较结果 / comparison order
 * @return 是否严格大于 / whether strictly greater
*/
internal fun isStrictlyGreater(order: Order): Boolean {
    return order is Order.Greater
}
