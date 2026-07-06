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

data class QuantityPlacement2<
        T : Cuboid<T, V>,
        V : FloatingNumber<V>,
        P : ProjectivePlane
        >(
    val projection: Projection<T, V, P>,
    val position: QuantityPoint2<V>
) : Copyable<QuantityPlacement2<T, V, P>> {
    @Suppress("UNCHECKED_CAST")
    constructor(
        projection: Projection<T, V, P>,
        position: Point<Dim2, FltX>
    ) : this(projection, point2FltX(position) as QuantityPoint2<V>)

    constructor(placement3: QuantityPlacement3<T, V>, plane: P) : this(
        projection = PlaneProjection(placement3.view, plane),
        position = plane.point2(placement3.position)
    )

    val unit by projection::unit
    val orientation by projection::orientation
    val view by projection::view
    val plane by projection::plane
    val weight by projection::weight

    val x by position::x
    val y by position::y

    val length by projection::length
    val width by projection::width

    val maxX = quantityPlusByValue(x, length)
    val maxY = quantityPlusByValue(y, width)
    val maxPosition = QuantityPoint2<V>(maxX, maxY)

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

    /** 判断是否重叠 / Check if overlapped */
    fun overlapped(rhs: QuantityPlacement2<*, V, P>): Ret<Boolean> {
        return toGeometryPlacement().overlapped(rhs.toGeometryPlacement())
    }

    /** 计算交集 / Compute intersection */
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

    fun toPlacement3(): List<QuantityPlacement3<T, V>> {
        return projection.toPlacement3At(position)
    }

    override fun copy() = QuantityPlacement2(projection.copy(), position)
}

data class QuantityPlacement3<
        T : Cuboid<T, V>,
        V : FloatingNumber<V>
        >(
    val view: CuboidView<T, V>,
    val position: QuantityPoint3<V>
) : Copyable<QuantityPlacement3<T, V>>, Ord<QuantityPlacement3<T, V>> {
    @Suppress("UNCHECKED_CAST")
    constructor(
        view: CuboidView<T, V>,
        position: Point<Dim3, FltX>
    ) : this(view, point3FltX(position) as QuantityPoint3<V>)

    private var _parent: QuantityPlacement3<*, V>? = null

    val unit by view::unit
    val orientation by view::orientation
    val parent by this::_parent
    val weight by unit::weight
    val volume by unit::volume

    val x by position::x
    val y by position::y
    val z by position::z

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
    val absoluteX: Quantity<V> get() = quantityPlusByValue(x, parent?.absoluteX ?: quantityZeroByValue(x))
    val absoluteY: Quantity<V> get() = quantityPlusByValue(y, parent?.absoluteY ?: quantityZeroByValue(y))
    val absoluteZ: Quantity<V> get() = quantityPlusByValue(z, parent?.absoluteZ ?: quantityZeroByValue(z))

    val absolutePlacement get() = QuantityPlacement3(view, absolutePosition)

    val width by view::width
    val height by view::height
    val depth by view::depth

    val maxX: Quantity<V> = quantityPlusByValue(x, width)
    val maxY: Quantity<V> = quantityPlusByValue(y, height)
    val maxZ: Quantity<V> = quantityPlusByValue(z, depth)
    val maxPosition: QuantityPoint3<V> = position + QuantityVector3<V>(x = width, y = height, z = depth)

    val maxAbsoluteX: Quantity<V> get() = quantityPlusByValue(absoluteX, width)
    val maxAbsoluteY: Quantity<V> get() = quantityPlusByValue(absoluteY, height)
    val maxAbsoluteZ: Quantity<V> get() = quantityPlusByValue(absoluteZ, depth)
    val maxAbsolutePosition: QuantityPoint3<V> get() = absolutePosition + QuantityVector3<V>(x = width, y = height, z = depth)

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

    init {
        if (unit is Container3<*, *>) {
            @Suppress("UNCHECKED_CAST")
            for (placement in (unit as Container3<*, V>).units) {
                placement._parent = this
            }
        }
    }

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

    /** 判断是否重叠 / Check if overlapped */
    infix fun overlapped(rhs: QuantityPlacement3<*, V>): Ret<Boolean> {
        return toGeometryPlacement().overlapped(rhs.toGeometryPlacement())
    }

    override fun copy() = QuantityPlacement3(view.copy(), position)

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

    override fun hashCode(): Int {
        return view.hashCode() or position.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as QuantityPlacement3<*, *>

        if (view != other.view) return false
        if (position != other.position) return false

        return true
    }
}

data class ShapePlacement3(
    val shape: PackingShape3<FltX>,
    val position: QuantityPoint3<FltX>
) : Copyable<ShapePlacement3> {
    private data class CircleFootprint(
        val centerX: Quantity<FltX>,
        val centerZ: Quantity<FltX>,
        val radius: Quantity<FltX>
    )

    private data class RectangleFootprint(
        val minX: Quantity<FltX>,
        val maxX: Quantity<FltX>,
        val minZ: Quantity<FltX>,
        val maxZ: Quantity<FltX>
    )

    val x by position::x
    val y by position::y
    val z by position::z

    val boundingWidth by shape::boundingWidth
    val boundingHeight by shape::boundingHeight
    val boundingDepth by shape::boundingDepth

    val maxX: Quantity<FltX> get() = x + boundingWidth
    val maxY: Quantity<FltX> get() = y + boundingHeight
    val maxZ: Quantity<FltX> get() = z + boundingDepth

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

    private fun zeroArea(unit: Quantity<FltX>): Quantity<FltX> {
        return unit * unit * FltX.zero
    }

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

        fun simpson(lb: Double, ub: Double): Double {
            val mid = (lb + ub) / 2.0
            return (ub - lb) * (verticalLengthAt(lb) + 4.0 * verticalLengthAt(mid) + verticalLengthAt(ub)) / 6.0
        }

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

    private fun verticalOverlapped(rhs: ShapePlacement3): Boolean {
        return (maxY gr rhs.y) == true && (y ls rhs.maxY) == true
    }

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

    /** 判断是否重叠 / Check if overlapped */
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

