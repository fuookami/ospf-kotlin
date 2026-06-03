@file:Suppress("DEPRECATION")

/**
 * 放置基础设施。
 * Placement infrastructure.
 */
package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.math.geometry.QuantityCuboid3
import fuookami.ospf.kotlin.math.geometry.QuantityCircle2
import fuookami.ospf.kotlin.math.geometry.QuantityPlacement2 as GeometryPlacement2
import fuookami.ospf.kotlin.math.geometry.QuantityPlacement3 as GeometryPlacement3
import fuookami.ospf.kotlin.math.geometry.QuantityProjection2
import fuookami.ospf.kotlin.math.geometry.QuantityRectangle2
import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.geometry.Dim2
import fuookami.ospf.kotlin.math.geometry.Dim3
import fuookami.ospf.kotlin.math.geometry.Point
import fuookami.ospf.kotlin.utils.functional.Ord
import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.eq
import fuookami.ospf.kotlin.quantities.quantity.gr
import fuookami.ospf.kotlin.quantities.quantity.ls
import fuookami.ospf.kotlin.quantities.quantity.minus
import fuookami.ospf.kotlin.quantities.quantity.partialOrd
import fuookami.ospf.kotlin.quantities.quantity.plus
import fuookami.ospf.kotlin.quantities.quantity.times
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private fun <V : FloatingNumber<V>> quantityOrd(lhs: Quantity<V>, rhs: Quantity<V>, axis: String): Order {
    return lhs.partialOrd(rhs)
        ?: throw IllegalArgumentException("Incomparable quantity on axis $axis: ${lhs.unit} vs ${rhs.unit}")
}

private fun <V : FloatingNumber<V>> quantityMax(lhs: Quantity<V>, rhs: Quantity<V>, axis: String): Quantity<V> {
    return when (quantityOrd(lhs, rhs, axis)) {
        is Order.Greater, Order.Equal -> lhs
        is Order.Less -> rhs
    }
}

private fun <V : FloatingNumber<V>> quantityMin(lhs: Quantity<V>, rhs: Quantity<V>, axis: String): Quantity<V> {
    return when (quantityOrd(lhs, rhs, axis)) {
        is Order.Greater -> rhs
        is Order.Equal, is Order.Less -> lhs
    }
}

private fun <V : FloatingNumber<V>> containsInRange(
    value: Quantity<V>,
    lb: Quantity<V>,
    ub: Quantity<V>,
    withLowerBound: Boolean,
    withUpperBound: Boolean
): Boolean {
    val lower = quantityOrd(value, lb, "range")
    val upper = quantityOrd(value, ub, "range")
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
    return lowerOk && upperOk
}

data class QuantityPlacement2<
        T : Cuboid<T>,
        P : ProjectivePlane
        >(
    val projection: Projection<T, P>,
    val position: QuantityPoint2
) : Copyable<QuantityPlacement2<T, P>> {
    constructor(
        projection: Projection<T, P>,
        position: Point<Dim2, InfraNumber>
    ) : this(projection, point2(position))

    constructor(QuantityPlacement3: QuantityPlacement3<T>, plane: P) : this(
        projection = PlaneProjection(QuantityPlacement3.view, plane),
        position = plane.point2(QuantityPlacement3.position)
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

    val maxX = x + length
    val maxY = y + width
    val maxPosition = QuantityPoint2(maxX, maxY)

    private fun toGeometryPlacement(): GeometryPlacement2<InfraNumber> {
        return GeometryPlacement2(
            x = x,
            y = y,
            shape = QuantityRectangle2(
                width = length,
                height = width
            )
        )
    }

    fun contains(
        point: QuantityPoint2,
        withLowerBound: Boolean = true,
        withUpperBound: Boolean = true,
        withBorder: Boolean = true
    ): Boolean {
        return asGenericPlacement2().contains(
            point = QuantityPoint2G(
                x = point.x,
                y = point.y
            ),
            withLowerBound = withLowerBound,
            withUpperBound = withUpperBound,
            withBorder = withBorder
        )
    }

    fun overlapped(rhs: QuantityPlacement2<*, P>): Boolean {
        return asGenericPlacement2().overlapped(rhs.asGenericPlacement2())
    }

    fun intersect(rhs: QuantityPlacement2<*, P>): QuantityRectangle2<InfraNumber>? {
        return asGenericPlacement2().intersect(rhs.asGenericPlacement2())
    }

    fun toPlacement3(): List<QuantityPlacement3<T>> {
        return projection.toPlacement3At(position)
    }

    override fun copy() = QuantityPlacement2(projection.copy(), position)
}

data class QuantityPlacement3<T : Cuboid<T>>(
    val view: CuboidView<T>,
    val position: QuantityPoint3
) : Copyable<QuantityPlacement3<T>>, Ord<QuantityPlacement3<T>> {
    constructor(
        view: CuboidView<T>,
        position: Point<Dim3, InfraNumber>
    ) : this(view, point3(position))

    private var _parent: QuantityPlacement3<*>? = null

    val unit by view::unit
    val orientation by view::orientation
    val parent by this::_parent
    val weight by unit::weight
    val volume by unit::volume

    val x by position::x
    val y by position::y
    val z by position::z

    val absolutePosition: QuantityPoint3
        get() = if (parent == null) {
            position
        } else {
            QuantityPoint3(
                x = x + parent!!.absoluteX,
                y = y + parent!!.absoluteY,
                z = z + parent!!.absoluteZ
            )
        }
    val absoluteX: Quantity<InfraNumber> get() = x + (parent?.absoluteX ?: (infraZero() * x.unit))
    val absoluteY: Quantity<InfraNumber> get() = y + (parent?.absoluteY ?: (infraZero() * y.unit))
    val absoluteZ: Quantity<InfraNumber> get() = z + (parent?.absoluteZ ?: (infraZero() * z.unit))

    val absolutePlacement get() = QuantityPlacement3(view, absolutePosition)

    val width by view::width
    val height by view::height
    val depth by view::depth

    val maxX: Quantity<InfraNumber> = x + width
    val maxY: Quantity<InfraNumber> = y + height
    val maxZ: Quantity<InfraNumber> = z + depth
    val maxPosition: QuantityPoint3 = position + QuantityVector3(x = width, y = height, z = depth)

    val maxAbsoluteX: Quantity<InfraNumber> get() = absoluteX + width
    val maxAbsoluteY: Quantity<InfraNumber> get() = absoluteY + height
    val maxAbsoluteZ: Quantity<InfraNumber> get() = absoluteZ + depth
    val maxAbsolutePosition: QuantityPoint3 get() = absolutePosition + QuantityVector3(x = width, y = height, z = depth)

    private fun toGeometryPlacement(): GeometryPlacement3<InfraNumber> {
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
        if (unit is Container3<*>) {
            for (placement in (unit as Container3<*>).units) {
                placement._parent = this
            }
        }
    }

    fun contains(
        point: QuantityPoint3,
        withLowerBound: Boolean = true,
        withUpperBound: Boolean = true,
        withBorder: Boolean = true
    ): Boolean {
        return asGenericPlacement3().contains(
            point = QuantityPoint3G(
                x = point.x,
                y = point.y,
                z = point.z
            ),
            withLowerBound = withLowerBound,
            withUpperBound = withUpperBound,
            withBorder = withBorder
        )
    }

    infix fun overlapped(rhs: QuantityPlacement3<*>): Boolean {
        return asGenericPlacement3().overlapped(rhs.asGenericPlacement3())
    }

    override fun copy() = QuantityPlacement3(view.copy(), position)

    override fun partialOrd(rhs: QuantityPlacement3<T>): Order {
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

        other as QuantityPlacement3<*>

        if (view != other.view) return false
        if (position != other.position) return false

        return true
    }
}

data class ShapePlacement3(
    val shape: PackingShape3<InfraNumber>,
    val position: QuantityPoint3
) : Copyable<ShapePlacement3> {
    private data class CircleFootprint(
        val centerX: Quantity<InfraNumber>,
        val centerZ: Quantity<InfraNumber>,
        val radius: Quantity<InfraNumber>
    )

    private data class RectangleFootprint(
        val minX: Quantity<InfraNumber>,
        val maxX: Quantity<InfraNumber>,
        val minZ: Quantity<InfraNumber>,
        val maxZ: Quantity<InfraNumber>
    )

    val x by position::x
    val y by position::y
    val z by position::z

    val boundingWidth by shape::boundingWidth
    val boundingHeight by shape::boundingHeight
    val boundingDepth by shape::boundingDepth

    val maxX: Quantity<InfraNumber> get() = x + boundingWidth
    val maxY: Quantity<InfraNumber> get() = y + boundingHeight
    val maxZ: Quantity<InfraNumber> get() = z + boundingDepth

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

    private fun zeroArea(unit: Quantity<InfraNumber>): Quantity<InfraNumber> {
        return unit * unit * infraZero()
    }

    private fun rectangleRectangleOverlapArea(
        lhs: RectangleFootprint,
        rhs: RectangleFootprint
    ): Quantity<InfraNumber> {
        val overlapX = quantityMin(lhs.maxX, rhs.maxX, "x") - quantityMax(lhs.minX, rhs.minX, "x")
        val overlapZ = quantityMin(lhs.maxZ, rhs.maxZ, "z") - quantityMax(lhs.minZ, rhs.minZ, "z")
        if ((overlapX gr (infraZero() * overlapX.unit)) != true || (overlapZ gr (infraZero() * overlapZ.unit)) != true) {
            return zeroArea(overlapX)
        }
        return overlapX * overlapZ
    }

    private fun circleCircleOverlapArea(
        lhs: CircleFootprint,
        rhs: CircleFootprint
    ): Quantity<InfraNumber> {
        val r1 = lhs.radius.toDouble()
        val r2 = rhs.radius.toDouble()
        val dx = (lhs.centerX - rhs.centerX).toDouble()
        val dz = (lhs.centerZ - rhs.centerZ).toDouble()
        val d = sqrt(dx * dx + dz * dz)
        val areaUnit = (lhs.radius * lhs.radius).unit
        if (d >= r1 + r2) {
            return infraZero() * areaUnit
        }
        if (d <= kotlin.math.abs(r1 - r2)) {
            val minRadius = min(r1, r2)
            return infraScalar(PI * minRadius * minRadius) * areaUnit
        }
        val dSafe = if (d == 0.0) 1e-12 else d
        val cosA = ((d * d + r1 * r1 - r2 * r2) / (2.0 * dSafe * r1)).coerceIn(-1.0, 1.0)
        val cosB = ((d * d + r2 * r2 - r1 * r1) / (2.0 * dSafe * r2)).coerceIn(-1.0, 1.0)
        val alpha = 2.0 * acos(cosA)
        val beta = 2.0 * acos(cosB)
        val area = 0.5 * r1 * r1 * (alpha - sin(alpha)) + 0.5 * r2 * r2 * (beta - sin(beta))
        return infraScalar(area) * areaUnit
    }

    private fun circleRectangleOverlapArea(
        circle: CircleFootprint,
        rectangle: RectangleFootprint
    ): Quantity<InfraNumber> {
        val radius = circle.radius.toDouble()
        val centerX = circle.centerX.toDouble()
        val centerZ = circle.centerZ.toDouble()
        val left = rectangle.minX.toDouble()
        val right = rectangle.maxX.toDouble()
        val front = rectangle.minZ.toDouble()
        val back = rectangle.maxZ.toDouble()
        val integrationLb = max(left, centerX - radius)
        val integrationUb = min(right, centerX + radius)
        val areaUnit = (circle.radius * circle.radius).unit
        if (left <= centerX - radius
            && right >= centerX + radius
            && front <= centerZ - radius
            && back >= centerZ + radius
        ) {
            return infraScalar(PI * radius * radius) * areaUnit
        }
        if (integrationLb >= integrationUb) {
            return infraZero() * areaUnit
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
        return infraScalar(area) * areaUnit
    }

    fun footprintOverlapArea(rhs: ShapePlacement3): Quantity<InfraNumber> {
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

    private val footprintPlacement: GeometryPlacement2<InfraNumber>
        get() {
            val footprintShape: QuantityProjection2<InfraNumber> = when (val footprint = shape.footprint()) {
                is ShapeFootprint2.Circle -> QuantityCircle2(footprint.radius)
                is ShapeFootprint2.Rectangle -> QuantityRectangle2(
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
        point: QuantityPoint3,
        withLowerBound: Boolean = true,
        withUpperBound: Boolean = true,
        withBorder: Boolean = true
    ): Boolean {
        val heightContains = containsInRange(
            value = point.y,
            lb = y,
            ub = maxY,
            withLowerBound = withLowerBound,
            withUpperBound = withUpperBound
        )
        if (!heightContains) {
            return false
        }
        return footprintPlacement.contains(
            x = point.x,
            y = point.z,
            withLowerBound = withLowerBound,
            withUpperBound = withUpperBound,
            withBorder = withBorder
        )
    }

    infix fun overlapped(rhs: ShapePlacement3): Boolean {
        if (!verticalOverlapped(rhs)) {
            return false
        }
        val overlapArea = footprintOverlapArea(rhs)
        return (overlapArea gr (infraZero() * overlapArea.unit)) == true || footprintPlacement.overlapped(rhs.footprintPlacement)
    }

    override fun copy(): ShapePlacement3 {
        return ShapePlacement3(
            shape = shape,
            position = position
        )
    }
}

fun QuantityPlacement3<*>.asShapePlacement3(): ShapePlacement3 {
    return asShapePlacement3 { placement ->
        placement.view.asPackingShape3()
    }
}

fun QuantityPlacement3<*>.asShapePlacement3(
    shapeResolver: (QuantityPlacement3<*>) -> PackingShape3<InfraNumber>
): ShapePlacement3 {
    return ShapePlacement3(
        shape = shapeResolver(this),
        position = absolutePosition
    )
}

fun topPlacements(placements: List<QuantityPlacement3<*>>): List<QuantityPlacement3<*>> {
    val genericBottomPlacements = placements.associateWith { placement ->
        GenericQuantityPlacement2(
            projection = GenericPlaneProjection(
                view = placement.unit.asGenericCuboid().view(placement.orientation),
                plane = Bottom
            ),
            position = QuantityPoint2G(
                x = Bottom.point2(placement.position).x,
                y = Bottom.point2(placement.position).y
            )
        )
    }
    val topPlacements = ArrayList<QuantityPlacement3<*>>()
    for (placement1 in placements) {
        val genericBottom1 = genericBottomPlacements[placement1]!!
        var flag = true
        for (placement2 in placements) {
            val genericBottom2 = genericBottomPlacements[placement2]!!
            if (genericBottom1.overlapped(genericBottom2)
                && (placement1.maxY ls placement2.maxY) == true
            ) {
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

fun bottomPlacements(placements: List<QuantityPlacement3<*>>): List<QuantityPlacement3<*>> {
    val genericBottomPlacements = placements.associateWith { placement ->
        GenericQuantityPlacement2(
            projection = GenericPlaneProjection(
                view = placement.unit.asGenericCuboid().view(placement.orientation),
                plane = Bottom
            ),
            position = QuantityPoint2G(
                x = Bottom.point2(placement.position).x,
                y = Bottom.point2(placement.position).y
            )
        )
    }
    val bottomPlacements = ArrayList<QuantityPlacement3<*>>()
    for (placement1 in placements) {
        val genericBottom1 = genericBottomPlacements[placement1]!!
        var flag = true
        for (placement2 in placements) {
            val genericBottom2 = genericBottomPlacements[placement2]!!
            if (genericBottom1.overlapped(genericBottom2)
                && (placement1.y gr placement2.y) == true
            ) {
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
