@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.math.geometry.QuantityCuboid3
import fuookami.ospf.kotlin.math.geometry.QuantityPlacement2 as GeometryPlacement2
import fuookami.ospf.kotlin.math.geometry.QuantityPlacement3 as GeometryPlacement3
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
