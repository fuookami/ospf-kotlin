/**
 * 放置基础设施。
 * Placement infrastructure.
*/
package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import kotlin.math.*
import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.geometry.QuantityPlacement2 as GeometryPlacement2
import fuookami.ospf.kotlin.math.geometry.QuantityPlacement3 as GeometryPlacement3
import fuookami.ospf.kotlin.math.geometry.QuantityRectangle2 as GeometryRectangle2
import fuookami.ospf.kotlin.quantities.quantity.*

/** Check if a value is within the specified range bounds.
 * 检查值是否在指定范围内。
 *
 * @param V the floating number type / 浮点数类型
 * @param value the value to check / 要检查的值
 * @param lb the lower bound / 下界
 * @param ub the upper bound / 上界
 * @param withLowerBound whether to include the lower bound / 是否包含下界
 * @param withUpperBound whether to include the upper bound / 是否包含上界
 * @return whether the value is within the range / 值是否在范围内
*/
private fun <V : FloatingNumber<V>> containsInRange(
    value: Quantity<V>,
    lb: Quantity<V>,
    ub: Quantity<V>,
    withLowerBound: Boolean,
    withUpperBound: Boolean
): Ret<Boolean> {
    val lower = when (val result = quantityOrdSafe(value, lb, "range-lb")) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    val upper = when (val result = quantityOrdSafe(value, ub, "range-ub")) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    val lowerOk = if (withLowerBound) {
        lower is Order.Equal || lower is Order.Greater
    } else {
        lower is Order.Greater
    }
    val upperOk = if (withUpperBound) {
        upper is Order.Equal || upper is Order.Less
    } else {
        upper is Order.Less
    }
    return ok(lowerOk && upperOk)
}

/** A 2D quantity-based placement with projection and position.
 * 基于量的二维放置，包含投影和位置。
 *
 * @param T the cuboid type / 长方体类型
 * @param V the floating number type / 浮点数类型
 * @param P the projective plane type / 投影平面类型
 * @property projection the projection from 3D to 2D / 从三维到二维的投影
 * @property position the 2D position / 二维位置
*/
data class QuantityPlacement2<
        T : Cuboid<T, V>,
        V : FloatingNumber<V>,
        P : ProjectivePlane
        >(
    val projection: Projection<T, V, P>,
    val position: QuantityPoint2<V>
) : Copyable<QuantityPlacement2<T, V, P>> {

    /** Secondary constructor from a FltX-based point.
     * 基于 FltX 点的辅助构造函数。
     *
     * @param projection the projection / 投影
     * @param position the 2D point with FltX values / 基于 FltX 的二维点
    */
    @Suppress("UNCHECKED_CAST")
    constructor(
        projection: Projection<T, V, P>,
        position: Point<Dim2, FltX>
    ) : this(projection, point2FltX(position) as QuantityPoint2<V>)

    /** Secondary constructor from a 3D placement and a projective plane.
     * 从三维放置和投影平面构造的辅助构造函数。
     *
     * @param placement3 the 3D placement / 三维放置
     * @param plane the projective plane / 投影平面
    */
    constructor(placement3: QuantityPlacement3<T, V>, plane: P) : this(
        projection = PlaneProjection(placement3.view, plane),
        position = plane.point2(placement3.position)
    )

    /** The unit (cuboid) being placed.
     * 被放置的单元（长方体）。
    */
    val unit by projection::unit

    /** The orientation of the placed unit.
     * 被放置单元的朝向。
    */
    val orientation by projection::orientation

    /** The cuboid view used for projection.
     * 用于投影的长方体视图。
    */
    val view by projection::view

    /** The projective plane.
     * 投影平面。
    */
    val plane by projection::plane

    /** The weight of the placed unit.
     * 被放置单元的重量。
    */
    val weight by projection::weight

    /** The x-coordinate of the position.
     * 位置的 x 坐标。
    */
    val x by position::x

    /** The y-coordinate of the position.
     * 位置的 y 坐标。
    */
    val y by position::y

    /** The length (projected dimension along x).
     * 长度（沿 x 方向的投影尺寸）。
    */
    val length by projection::length

    /** The width (projected dimension along y).
     * 宽度（沿 y 方向的投影尺寸）。
    */
    val width by projection::width

    /** The maximum x-coordinate (x + length).
     * 最大 x 坐标（x + 长度）。
    */
    val maxX = quantityPlusByValue(x, length)

    /** The maximum y-coordinate (y + width).
     * 最大 y 坐标（y + 宽度）。
    */
    val maxY = quantityPlusByValue(y, width)

    /** The maximum position point.
     * 最大位置点。
    */
    val maxPosition = QuantityPoint2<V>(maxX, maxY)

    /** Convert this placement to a 2D geometry placement.
     * 将此放置转换为二维几何放置。
     *
     * @return the 2D geometry placement / 二维几何放置
    */
    private fun toGeometryPlacement(): GeometryPlacement2<V> {
        return GeometryPlacement2(
            x = x,
            y = y,
            shape = GeometryRectangle2(
                width = length,
                height = width
            )
        )
    }

    /** Check if a 2D point is contained within this placement.
     * 检查二维点是否包含在此放置内。
     *
     * @param point the point to check / 要检查的点
     * @param withLowerBound whether to include the lower bound / 是否包含下界
     * @param withUpperBound whether to include the upper bound / 是否包含上界
     * @param withBorder whether to include the border / 是否包含边界
     * @return whether the point is contained / 点是否被包含
    */
    fun contains(
        point: QuantityPoint2<V>,
        withLowerBound: Boolean = true,
        withUpperBound: Boolean = true,
        withBorder: Boolean = true
    ): Ret<Boolean> {
        return toGeometryPlacement().contains(
            x = point.x,
            y = point.y,
            withLowerBound = withLowerBound,
            withUpperBound = withUpperBound,
            withBorder = withBorder
        )
    }

    /** Check if overlapped with another 2D placement.
     * 检查是否与另一个二维放置重叠。
     *
     * @param rhs the other placement / 另一个放置
     * @return whether the two placements overlap / 两个放置是否重叠
    */
    fun overlapped(rhs: QuantityPlacement2<*, V, P>): Ret<Boolean> {
        return toGeometryPlacement().overlapped(rhs.toGeometryPlacement())
    }

    /** Compute the intersection with another 2D placement.
     * 计算与另一个二维放置的交集。
     *
     * @param rhs the other placement / 另一个放置
     * @return the intersection rectangle, or null if no intersection / 交集矩形，若无交集则返回 null
    */
    fun intersect(rhs: QuantityPlacement2<*, V, P>): Ret<GeometryRectangle2<V>?> {
        val intersection = when (val result = toGeometryPlacement().intersect(rhs.toGeometryPlacement())) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        } ?: return ok(null)
        return ok(GeometryRectangle2(
            width = intersection.width,
            height = intersection.height
        ))
    }

    /** Convert this 2D placement back to a list of 3D placements.
     * 将此二维放置转换回三维放置列表。
     *
     * @return the list of 3D placements / 三维放置列表
    */
    fun toPlacement3(): List<QuantityPlacement3<T, V>> {
        return projection.toPlacement3At(position)
    }

    /** Create a copy of this 2D placement.
     * 创建此二维放置的副本。
     *
     * @return a new QuantityPlacement2 with copied projection and position / 具有复制的投影和位置的新 QuantityPlacement2
    */
    override fun copy() = QuantityPlacement2(projection.copy(), position)
}

/** A 3D quantity-based placement with cuboid view and position.
 * 基于量的三维放置，包含长方体视图和位置。
 *
 * @param T the cuboid type / 长方体类型
 * @param V the floating number type / 浮点数类型
 * @property view the cuboid view / 长方体视图
 * @property position the 3D position / 三维位置
*/
data class QuantityPlacement3<
        T : Cuboid<T, V>,
        V : FloatingNumber<V>
        >(
    val view: CuboidView<T, V>,
    val position: QuantityPoint3<V>
) : Copyable<QuantityPlacement3<T, V>>, Ord<QuantityPlacement3<T, V>> {

    /** Secondary constructor from a FltX-based point.
     * 基于 FltX 点的辅助构造函数。
     *
     * @param view the cuboid view / 长方体视图
     * @param position the 3D point with FltX values / 基于 FltX 的三维点
    */
    @Suppress("UNCHECKED_CAST")
    constructor(
        view: CuboidView<T, V>,
        position: Point<Dim3, FltX>
    ) : this(view, point3FltX(position) as QuantityPoint3<V>)

    /** The parent placement that contains this placement.
     * 包含此放置的父放置。
    */
    private var _parent: QuantityPlacement3<*, V>? = null

    /** The unit (cuboid) being placed.
     * 被放置的单元（长方体）。
    */
    val unit by view::unit

    /** The orientation of the placed unit.
     * 被放置单元的朝向。
    */
    val orientation by view::orientation

    /** The parent placement, if any.
     * 父放置（如有）。
    */
    val parent by this::_parent

    /** The weight of the placed unit.
     * 被放置单元的重量。
    */
    val weight by unit::weight

    /** The volume of the placed unit.
     * 被放置单元的体积。
    */
    val volume by unit::volume

    /** The x-coordinate of the position.
     * 位置的 x 坐标。
    */
    val x by position::x

    /** The y-coordinate of the position.
     * 位置的 y 坐标。
    */
    val y by position::y

    /** The z-coordinate of the position.
     * 位置的 z 坐标。
    */
    val z by position::z

    /** The absolute position in world coordinates.
     * 世界坐标系中的绝对位置。
    */
    val absolutePosition: QuantityPoint3<V>
        get() = if (parent == null) {
            position
        } else {
            QuantityPoint3(
                x = quantityPlusByValue(x, parent!!.absoluteX),
                y = quantityPlusByValue(y, parent!!.absoluteY),
                z = quantityPlusByValue(z, parent!!.absoluteZ)
            )
        }

    /** The absolute x-coordinate in world coordinates.
     * 世界坐标系中的绝对 x 坐标。
    */
    val absoluteX: Quantity<V> get() = quantityPlusByValue(x, parent?.absoluteX ?: quantityZeroByValue(x))

    /** The absolute y-coordinate in world coordinates.
     * 世界坐标系中的绝对 y 坐标。
    */
    val absoluteY: Quantity<V> get() = quantityPlusByValue(y, parent?.absoluteY ?: quantityZeroByValue(y))

    /** The absolute z-coordinate in world coordinates.
     * 世界坐标系中的绝对 z 坐标。
    */
    val absoluteZ: Quantity<V> get() = quantityPlusByValue(z, parent?.absoluteZ ?: quantityZeroByValue(z))

    /** The absolute placement using world coordinates.
     * 使用世界坐标的绝对放置。
    */
    val absolutePlacement get() = QuantityPlacement3(view, absolutePosition)

    /** The width of the placed unit.
     * 被放置单元的宽度。
    */
    val width by view::width

    /** The height of the placed unit.
     * 被放置单元的高度。
    */
    val height by view::height

    /** The depth of the placed unit.
     * 被放置单元的深度。
    */
    val depth by view::depth

    /** The maximum x-coordinate (x + width).
     * 最大 x 坐标（x + 宽度）。
    */
    val maxX: Quantity<V> = quantityPlusByValue(x, width)

    /** The maximum y-coordinate (y + height).
     * 最大 y 坐标（y + 高度）。
    */
    val maxY: Quantity<V> = quantityPlusByValue(y, height)

    /** The maximum z-coordinate (z + depth).
     * 最大 z 坐标（z + 深度）。
    */
    val maxZ: Quantity<V> = quantityPlusByValue(z, depth)

    /** The maximum position point.
     * 最大位置点。
    */
    val maxPosition: QuantityPoint3<V> = position + QuantityVector3<V>(x = width, y = height, z = depth)

    /** The maximum absolute x-coordinate.
     * 最大绝对 x 坐标。
    */
    val maxAbsoluteX: Quantity<V> get() = quantityPlusByValue(absoluteX, width)

    /** The maximum absolute y-coordinate.
     * 最大绝对 y 坐标。
    */
    val maxAbsoluteY: Quantity<V> get() = quantityPlusByValue(absoluteY, height)

    /** The maximum absolute z-coordinate.
     * 最大绝对 z 坐标。
    */
    val maxAbsoluteZ: Quantity<V> get() = quantityPlusByValue(absoluteZ, depth)

    /** The maximum absolute position point.
     * 最大绝对位置点。
    */
    val maxAbsolutePosition: QuantityPoint3<V> get() = absolutePosition + QuantityVector3<V>(x = width, y = height, z = depth)

    /** Convert this placement to a 3D geometry placement using absolute coordinates.
     * 使用绝对坐标将此放置转换为三维几何放置。
     *
     * @return the 3D geometry placement / 三维几何放置
    */
    private fun toGeometryPlacement(): GeometryPlacement3<V> {
        return GeometryPlacement3(
            x = absoluteX,
            y = absoluteY,
            z = absoluteZ,
            shape = QuantityCuboid3(
                width = width,
                height = height,
                depth = depth
            )
        )
    }

    /** Initialize the placement, setting parent references for child units in a container.
     * 初始化放置，为容器中的子单元设置父引用。
    */
    init {
        if (unit is Container3<*, *>) {
            @Suppress("UNCHECKED_CAST")
            for (placement in (unit as Container3<*, V>).units) {
                placement._parent = this
            }
        }
    }

    /** Check if a 3D point is contained within this placement.
     * 检查三维点是否包含在此放置内。
     *
     * @param point the point to check / 要检查的点
     * @param withLowerBound whether to include the lower bound / 是否包含下界
     * @param withUpperBound whether to include the upper bound / 是否包含上界
     * @param withBorder whether to include the border / 是否包含边界
     * @return whether the point is contained / 点是否被包含
    */
    fun contains(
        point: QuantityPoint3<V>,
        withLowerBound: Boolean = true,
        withUpperBound: Boolean = true,
        withBorder: Boolean = true
    ): Ret<Boolean> {
        return toGeometryPlacement().contains(
            x = point.x,
            y = point.y,
            z = point.z,
            withLowerBound = withLowerBound,
            withUpperBound = withUpperBound,
            withBorder = withBorder
        )
    }

    /** Check if overlapped with another 3D placement.
     * 检查是否与另一个三维放置重叠。
     *
     * @param rhs the other placement / 另一个放置
     * @return whether the two placements overlap / 两个放置是否重叠
    */
    infix fun overlapped(rhs: QuantityPlacement3<*, V>): Ret<Boolean> {
        return toGeometryPlacement().overlapped(rhs.toGeometryPlacement())
    }

    /** Create a copy of this 3D placement.
     * 创建此三维放置的副本。
     *
     * @return a new QuantityPlacement3 with copied view and position / 具有复制的视图和位置的新 QuantityPlacement3
    */
    override fun copy() = QuantityPlacement3(view.copy(), position)

    /** Compare this placement with another by z, then y, then x order.
     * 按 z、y、x 顺序比较此放置与另一个放置。
     *
     * @param rhs the other placement to compare / 要比较的另一个放置
     * @return the comparison order / 比较结果
    */
    override fun partialOrd(rhs: QuantityPlacement3<T, V>): Order {
        when (val value = quantityOrd(z, rhs.z, "z")) {
            Order.Equal -> {}

            else -> {
                return value
            }
        }
        when (val value = quantityOrd(y, rhs.y, "y")) {
            Order.Equal -> {}

            else -> {
                return value
            }
        }
        return quantityOrd(x, rhs.x, "x")
    }

    /** Compute the hash code based on view and position.
     * 基于视图和位置计算哈希码。
     *
     * @return the hash code value / 哈希码值
    */
    override fun hashCode(): Int {
        return view.hashCode() or position.hashCode()
    }

    /** Check equality with another object based on view and position.
     * 基于视图和位置检查与另一个对象的相等性。
     *
     * @param other the object to compare / 要比较的对象
     * @return whether the objects are equal / 对象是否相等
    */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as QuantityPlacement3<*, *>

        if (view != other.view) return false
        if (position != other.position) return false

        return true
    }
}

/** A 3D shape-based placement with position and packing shape.
 * 基于形状的三维放置，包含位置和包装形状。
 *
 * @property shape the packing shape / 包装形状
 * @property position the 3D position / 三维位置
*/
data class ShapePlacement3(
    val shape: PackingShape3<FltX>,
    val position: QuantityPoint3<FltX>
) : Copyable<ShapePlacement3> {

    /** A circle footprint defined by center coordinates and radius.
     * 由中心坐标和半径定义的圆形底面。
     *
     * @property centerX the x-coordinate of the center / 中心的 x 坐标
     * @property centerZ the z-coordinate of the center / 中心的 z 坐标
     * @property radius the radius / 半径
    */
    private data class CircleFootprint(
        val centerX: Quantity<FltX>,
        val centerZ: Quantity<FltX>,
        val radius: Quantity<FltX>
    )

    /** A rectangle footprint defined by min and max coordinates.
     * 由最小和最大坐标定义的矩形底面。
     *
     * @property minX the minimum x-coordinate / 最小 x 坐标
     * @property maxX the maximum x-coordinate / 最大 x 坐标
     * @property minZ the minimum z-coordinate / 最小 z 坐标
     * @property maxZ the maximum z-coordinate / 最大 z 坐标
    */
    private data class RectangleFootprint(
        val minX: Quantity<FltX>,
        val maxX: Quantity<FltX>,
        val minZ: Quantity<FltX>,
        val maxZ: Quantity<FltX>
    )

    /** The x-coordinate of the position.
     * 位置的 x 坐标。
    */
    val x by position::x

    /** The y-coordinate of the position.
     * 位置的 y 坐标。
    */
    val y by position::y

    /** The z-coordinate of the position.
     * 位置的 z 坐标。
    */
    val z by position::z

    /** The bounding width of the shape.
     * 形状的包围宽度。
    */
    val boundingWidth by shape::boundingWidth

    /** The bounding height of the shape.
     * 形状的包围高度。
    */
    val boundingHeight by shape::boundingHeight

    /** The bounding depth of the shape.
     * 形状的包围深度。
    */
    val boundingDepth by shape::boundingDepth

    /** The maximum x-coordinate (x + boundingWidth).
     * 最大 x 坐标（x + 包围宽度）。
    */
    val maxX: Quantity<FltX> get() = x + boundingWidth

    /** The maximum y-coordinate (y + boundingHeight).
     * 最大 y 坐标（y + 包围高度）。
    */
    val maxY: Quantity<FltX> get() = y + boundingHeight

    /** The maximum z-coordinate (z + boundingDepth).
     * 最大 z 坐标（z + 包围深度）。
    */
    val maxZ: Quantity<FltX> get() = z + boundingDepth

    /** Convert this shape placement to a circle footprint, if applicable.
     * 将此形状放置转换为圆形底面（如适用）。
     *
     * @return the circle footprint, or null if not circular / 圆形底面，若非圆形则返回 null
    */
    private fun toCircleFootprint(): CircleFootprint? {
        val footprint = shape.footprint()
        return if (footprint is ShapeFootprint2.Circle) {
            CircleFootprint(
                centerX = x + footprint.radius,
                centerZ = z + footprint.radius,
                radius = footprint.radius
            )
        } else {
            null
        }
    }

    /** Convert this shape placement to a rectangle footprint, if applicable.
     * 将此形状放置转换为矩形底面（如适用）。
     *
     * @return the rectangle footprint, or null if not rectangular / 矩形底面，若非矩形则返回 null
    */
    private fun toRectangleFootprint(): RectangleFootprint? {
        val footprint = shape.footprint()
        return if (footprint is ShapeFootprint2.Rectangle) {
            RectangleFootprint(
                minX = x,
                maxX = x + footprint.width,
                minZ = z,
                maxZ = z + footprint.depth
            )
        } else {
            null
        }
    }

    /** Create a zero-area quantity with the given unit.
     * 使用给定单位创建零面积量。
     *
     * @param unit the unit quantity / 单位量
     * @return a zero-area quantity / 零面积量
    */
    private fun zeroArea(unit: Quantity<FltX>): Quantity<FltX> {
        return unit * unit * FltX.zero
    }

    /** Compute the overlap area between two rectangle footprints.
     * 计算两个矩形底面的重叠面积。
     *
     * @param lhs the first rectangle footprint / 第一个矩形底面
     * @param rhs the second rectangle footprint / 第二个矩形底面
     * @return the overlap area / 重叠面积
    */
    private fun rectangleRectangleOverlapArea(
        lhs: RectangleFootprint,
        rhs: RectangleFootprint
    ): Quantity<FltX> {
        val overlapX = quantityMin(lhs.maxX, rhs.maxX, "x") - quantityMax(lhs.minX, rhs.minX, "x")
        val overlapZ = quantityMin(lhs.maxZ, rhs.maxZ, "z") - quantityMax(lhs.minZ, rhs.minZ, "z")
        if ((overlapX gr (FltX.zero * overlapX.unit)) != true || (overlapZ gr (FltX.zero * overlapZ.unit)) != true) {
            return zeroArea(overlapX)
        }
        return overlapX * overlapZ
    }

    /** Compute the overlap area between two circle footprints.
     * 计算两个圆形底面的重叠面积。
     *
     * @param lhs the first circle footprint / 第一个圆形底面
     * @param rhs the second circle footprint / 第二个圆形底面
     * @return the overlap area / 重叠面积
    */
    private fun circleCircleOverlapArea(
        lhs: CircleFootprint,
        rhs: CircleFootprint
    ): Quantity<FltX> {
        val r1 = lhs.radius.value.toDouble()
        val r2 = rhs.radius.value.toDouble()
        val dx = (lhs.centerX - rhs.centerX).value.toDouble()
        val dz = (lhs.centerZ - rhs.centerZ).value.toDouble()
        val d = sqrt(dx * dx + dz * dz)
        val areaUnit = (lhs.radius * lhs.radius).unit
        if (d >= r1 + r2) {
            return FltX.zero * areaUnit
        }
        if (d <= kotlin.math.abs(r1 - r2)) {
            val minRadius = min(r1, r2)
            return FltX(PI * minRadius * minRadius) * areaUnit
        }
        val dSafe = if (d == 0.0) 1e-12 else d
        val cosA = ((d * d + r1 * r1 - r2 * r2) / (2.0 * dSafe * r1)).coerceIn(-1.0, 1.0)
        val cosB = ((d * d + r2 * r2 - r1 * r1) / (2.0 * dSafe * r2)).coerceIn(-1.0, 1.0)
        val alpha = 2.0 * acos(cosA)
        val beta = 2.0 * acos(cosB)
        val area = 0.5 * r1 * r1 * (alpha - sin(alpha)) + 0.5 * r2 * r2 * (beta - sin(beta))
        return FltX(area) * areaUnit
    }

    /** Compute the overlap area between a circle and a rectangle footprint.
     * 计算圆形与矩形底面的重叠面积。
     *
     * @param circle the circle footprint / 圆形底面
     * @param rectangle the rectangle footprint / 矩形底面
     * @return the overlap area / 重叠面积
    */
    private fun circleRectangleOverlapArea(
        circle: CircleFootprint,
        rectangle: RectangleFootprint
    ): Quantity<FltX> {
        val radius = circle.radius.value.toDouble()
        val centerX = circle.centerX.value.toDouble()
        val centerZ = circle.centerZ.value.toDouble()
        val left = rectangle.minX.value.toDouble()
        val right = rectangle.maxX.value.toDouble()
        val front = rectangle.minZ.value.toDouble()
        val back = rectangle.maxZ.value.toDouble()
        val integrationLb = max(left, centerX - radius)
        val integrationUb = min(right, centerX + radius)
        val areaUnit = (circle.radius * circle.radius).unit
        if (left <= centerX - radius
            && right >= centerX + radius
            && front <= centerZ - radius
            && back >= centerZ + radius
        ) {
            return FltX(PI * radius * radius) * areaUnit
        }
        if (integrationLb >= integrationUb) {
            return FltX.zero * areaUnit
        }

        /** Compute the vertical length of the circle at a given x-coordinate.
         * 计算圆在给定 x 坐标处的垂直长度。
         *
         * @param xValue the x-coordinate / x 坐标
         * @return the vertical length / 垂直长度
        */
        fun verticalLengthAt(xValue: Double): Double {
            val dx = xValue - centerX
            val remainder = radius * radius - dx * dx
            if (remainder <= 0.0) {
                return 0.0
            }
            val delta = sqrt(remainder)
            val low = max(front, centerZ - delta)
            val high = min(back, centerZ + delta)
            return max(0.0, high - low)
        }

        /** Compute the Simpson's rule approximation for the integral over [lb, ub].
         * 计算 [lb, ub] 区间上的辛普森法则近似积分。
         *
         * @param lb the lower bound of integration / 积分下界
         * @param ub the upper bound of integration / 积分上界
         * @return the Simpson's rule approximation / 辛普森法则近似值
        */
        fun simpson(lb: Double, ub: Double): Double {
            val mid = (lb + ub) / 2.0
            return (ub - lb) * (verticalLengthAt(lb) + 4.0 * verticalLengthAt(mid) + verticalLengthAt(ub)) / 6.0
        }

        /** Compute the adaptive Simpson's rule approximation with error control.
         * 计算带误差控制的自适应辛普森法则近似积分。
         *
         * @param lb the lower bound of integration / 积分下界
         * @param ub the upper bound of integration / 积分上界
         * @param whole the whole interval Simpson value / 整个区间的辛普森值
         * @param epsilon the error tolerance / 误差容限
         * @param depth the remaining recursion depth / 剩余递归深度
         * @return the adaptive Simpson's rule approximation / 自适应辛普森法则近似值
        */
        fun adaptiveSimpson(lb: Double, ub: Double, whole: Double, epsilon: Double, depth: Int): Double {
            val mid = (lb + ub) / 2.0
            val leftValue = simpson(lb, mid)
            val rightValue = simpson(mid, ub)
            val delta = leftValue + rightValue - whole
            return if (depth <= 0 || kotlin.math.abs(delta) <= 15.0 * epsilon) {
                leftValue + rightValue + delta / 15.0
            } else {
                adaptiveSimpson(lb, mid, leftValue, epsilon / 2.0, depth - 1) +
                        adaptiveSimpson(mid, ub, rightValue, epsilon / 2.0, depth - 1)
            }
        }

        val whole = simpson(integrationLb, integrationUb)
        val area = adaptiveSimpson(
            lb = integrationLb,
            ub = integrationUb,
            whole = whole,
            epsilon = 1e-7,
            depth = 12
        )
        return FltX(area) * areaUnit
    }

    /** Compute the overlap area of footprints between this and another shape placement.
     * 计算此形状放置与另一个形状放置的底面重叠面积。
     *
     * @param rhs the other shape placement / 另一个形状放置
     * @return the overlap area / 重叠面积
    */
    fun footprintOverlapArea(rhs: ShapePlacement3): Quantity<FltX> {
        val lhsCircle = toCircleFootprint()
        val rhsCircle = rhs.toCircleFootprint()
        val lhsRectangle = toRectangleFootprint()
        val rhsRectangle = rhs.toRectangleFootprint()
        return when {
            lhsCircle != null && rhsCircle != null -> circleCircleOverlapArea(lhsCircle, rhsCircle)
            lhsRectangle != null && rhsRectangle != null -> rectangleRectangleOverlapArea(lhsRectangle, rhsRectangle)
            lhsCircle != null && rhsRectangle != null -> circleRectangleOverlapArea(lhsCircle, rhsRectangle)
            lhsRectangle != null && rhsCircle != null -> circleRectangleOverlapArea(rhsCircle, lhsRectangle)
            else -> zeroArea(boundingWidth)
        }
    }

    /** The 2D geometry placement of the footprint projection.
     * 底面投影的二维几何放置。
    */
    private val footprintPlacement: GeometryPlacement2<FltX>
        get() {
            val footprintShape: QuantityProjection2<FltX> = when (val footprint = shape.footprint()) {
                is ShapeFootprint2.Circle -> QuantityCircle2(footprint.radius)
                is ShapeFootprint2.Rectangle -> GeometryRectangle2(
                    width = footprint.width,
                    height = footprint.depth
                )
            }
            return GeometryPlacement2(
                x = x,
                y = z,
                shape = footprintShape
            )
        }

    /** Check if vertically overlapped with another shape placement.
     * 检查是否与另一个形状放置在垂直方向上重叠。
     *
     * @param rhs the other shape placement / 另一个形状放置
     * @return whether the two placements overlap vertically / 两个放置在垂直方向上是否重叠
    */
    private fun verticalOverlapped(rhs: ShapePlacement3): Boolean {
        return (maxY gr rhs.y) == true && (y ls rhs.maxY) == true
    }

    /** Check if a 3D point is contained within this shape placement.
     * 检查三维点是否包含在此形状放置内。
     *
     * @param point the point to check / 要检查的点
     * @param withLowerBound whether to include the lower bound / 是否包含下界
     * @param withUpperBound whether to include the upper bound / 是否包含上界
     * @param withBorder whether to include the border / 是否包含边界
     * @return whether the point is contained / 点是否被包含
    */
    fun contains(
        point: QuantityPoint3<FltX>,
        withLowerBound: Boolean = true,
        withUpperBound: Boolean = true,
        withBorder: Boolean = true
    ): Ret<Boolean> {
        val heightContains = when (val result = containsInRange(
            value = point.y,
            lb = y,
            ub = maxY,
            withLowerBound = withLowerBound,
            withUpperBound = withUpperBound
        )) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        if (!heightContains) {
            return ok(false)
        }
        return footprintPlacement.contains(
            x = point.x,
            y = point.z,
            withLowerBound = withLowerBound,
            withUpperBound = withUpperBound,
            withBorder = withBorder
        )
    }

    /** Check if overlapped with another shape placement.
     * 检查是否与另一个形状放置重叠。
     *
     * @param rhs the other shape placement / 另一个形状放置
     * @return whether the two placements overlap / 两个放置是否重叠
    */
    infix fun overlapped(rhs: ShapePlacement3): Ret<Boolean> {
        if (!verticalOverlapped(rhs)) {
            return ok(false)
        }
        val overlapArea = footprintOverlapArea(rhs)
        return if ((overlapArea gr (FltX.zero * overlapArea.unit)) == true) {
            ok(true)
        } else {
            footprintPlacement.overlapped(rhs.footprintPlacement)
        }
    }

    /** Create a copy of this shape placement.
     * 创建此形状放置的副本。
     *
     * @return a new ShapePlacement3 with the same shape and position / 具有相同形状和位置的新 ShapePlacement3
    */
    override fun copy(): ShapePlacement3 {
        return ShapePlacement3(
            shape = shape,
            position = position
        )
    }
}

/** 二维放置主类型名。Primary 2D placement type name. */
typealias Placement2<T, V, P> = QuantityPlacement2<T, V, P>

/** 三维放置主类型名。Primary 3D placement type name. */
typealias Placement3<T, V> = QuantityPlacement3<T, V>

/**
 * 将三维放置转换为形状放置，使用默认形状解析器。
 * Convert 3D placement to shape placement using default shape resolver.
 *
 * @return 形状放置 / shape placement
*/
fun QuantityPlacement3<*, FltX>.asShapePlacement3(): ShapePlacement3 {
    return asShapePlacement3 { placement ->
        placement.view.asPackingShape3()
    }
}

/**
 * 将三维放置转换为形状放置，使用自定义形状解析器。
 * Convert 3D placement to shape placement using custom shape resolver.
 *
 * @param shapeResolver 形状解析器 / shape resolver
 * @return 形状放置 / shape placement
*/
fun QuantityPlacement3<*, FltX>.asShapePlacement3(
    shapeResolver: (QuantityPlacement3<*, FltX>) -> PackingShape3<FltX>
): ShapePlacement3 {
    return ShapePlacement3(
        shape = shapeResolver(this),
        position = absolutePosition
    )
}

/**
 * 获取顶层放置列表，即没有被其他放置覆盖的放置。
 * Get top placements, i.e., placements not covered by other placements.
 *
 * @param placements 放置列表 / placement list
 * @return 顶层放置列表 / top placement list
*/
fun topPlacements(placements: List<QuantityPlacement3<*, FltX>>): List<QuantityPlacement3<*, FltX>> {
    val bottomFootprintPlacements = placements.associateWith { placement ->
        QuantityPlacement2(
            projection = PlaneProjection(
                view = placement.view,
                plane = Bottom
            ),
            position = QuantityPoint2<FltX>(
                x = Bottom.point2(placement.position).x,
                y = Bottom.point2(placement.position).y
            )
        )
    }
    val topPlacements = ArrayList<QuantityPlacement3<*, FltX>>()
    for (placement1 in placements) {
        val bottomFootprint1 = bottomFootprintPlacements[placement1]!!
        var flag = true
        for (placement2 in placements) {
            val bottomFootprint2 = bottomFootprintPlacements[placement2]!!
            if (bottomFootprint1.overlapped(bottomFootprint2).value == true && (placement1.maxY ls placement2.maxY)) {
                flag = false
                break
            }
        }
        if (flag) {
            topPlacements.add(placement1)
        }
    }
    return topPlacements
}

/**
 * 获取底层放置列表，即没有放置在其下方的放置。
 * Get bottom placements, i.e., placements with no placement below them.
 *
 * @param placements 放置列表 / placement list
 * @return 底层放置列表 / bottom placement list
*/
fun bottomPlacements(placements: List<QuantityPlacement3<*, FltX>>): List<QuantityPlacement3<*, FltX>> {
    val bottomFootprintPlacements = placements.associateWith { placement ->
        QuantityPlacement2(
            projection = PlaneProjection(
                view = placement.view,
                plane = Bottom
            ),
            position = QuantityPoint2<FltX>(
                x = Bottom.point2(placement.position).x,
                y = Bottom.point2(placement.position).y
            )
        )
    }
    val bottomPlacements = ArrayList<QuantityPlacement3<*, FltX>>()
    for (placement1 in placements) {
        val bottomFootprint1 = bottomFootprintPlacements[placement1]!!
        var flag = true
        for (placement2 in placements) {
            val bottomFootprint2 = bottomFootprintPlacements[placement2]!!
            if (bottomFootprint1.overlapped(bottomFootprint2).value == true && (placement1.y gr placement2.y)) {
                flag = false
                break
            }
        }
        if (flag) {
            bottomPlacements.add(placement1)
        }
    }
    return bottomPlacements
}
