@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.geometry.Dim2
import fuookami.ospf.kotlin.math.geometry.Dim3
import fuookami.ospf.kotlin.math.geometry.Point
import fuookami.ospf.kotlin.utils.functional.Ord
import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.quantities.quantity.eq
import fuookami.ospf.kotlin.quantities.quantity.geq
import fuookami.ospf.kotlin.quantities.quantity.gr
import fuookami.ospf.kotlin.quantities.quantity.leq
import fuookami.ospf.kotlin.quantities.quantity.ls
import fuookami.ospf.kotlin.quantities.quantity.minus
import fuookami.ospf.kotlin.quantities.quantity.partialOrd
import fuookami.ospf.kotlin.quantities.quantity.plus
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Meter

private fun quantityOrd(lhs: QuantityFlt64, rhs: QuantityFlt64, axis: String): Order {
    return lhs.partialOrd(rhs)
        ?: throw IllegalArgumentException("Incomparable quantity on axis $axis: ${lhs.unit} vs ${rhs.unit}")
}

private fun quantityMax(lhs: QuantityFlt64, rhs: QuantityFlt64, axis: String): QuantityFlt64 {
    return when (quantityOrd(lhs, rhs, axis)) {
        is Order.Greater, Order.Equal -> lhs
        is Order.Less -> rhs
    }
}

private fun quantityMin(lhs: QuantityFlt64, rhs: QuantityFlt64, axis: String): QuantityFlt64 {
    return when (quantityOrd(lhs, rhs, axis)) {
        is Order.Greater -> rhs
        is Order.Equal, is Order.Less -> lhs
    }
}

private fun containsInRange(
    value: QuantityFlt64,
    lb: QuantityFlt64,
    ub: QuantityFlt64,
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

data class Placement2<
        T : Cuboid<T>,
        P : ProjectivePlane
        >(
    val projection: Projection<T, P>,
    val position: QuantityPoint2
) : Copyable<Placement2<T, P>> {
    constructor(
        projection: Projection<T, P>,
        position: Point<Dim2, Flt64>
    ) : this(projection, point2(position))

    constructor(placement3: Placement3<T>, plane: P) : this(
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

    val maxX = x + length
    val maxY = y + width
    val maxPosition = QuantityPoint2(maxX, maxY)

    fun contains(
        point: QuantityPoint2,
        withLowerBound: Boolean = true,
        withUpperBound: Boolean = true,
        withBorder: Boolean = true
    ): Boolean {
        val includeLower = withBorder && withLowerBound
        val includeUpper = withBorder && withUpperBound
        return containsInRange(point.x, x, maxX, includeLower, includeUpper)
                && containsInRange(point.y, y, maxY, includeLower, includeUpper)
    }

    fun overlapped(rhs: Placement2<*, P>): Boolean {
        if ((maxX leq rhs.x) == true || (x geq rhs.maxX) == true) {
            return false
        }
        if ((maxY leq rhs.y) == true || (y geq rhs.maxY) == true) {
            return false
        }
        return true
    }

    fun intersect(rhs: Placement2<*, P>): QuantityRectangle2? {
        val minX = quantityMax(x, rhs.x, "x")
        val maxX = quantityMin(this.maxX, rhs.maxX, "x")
        val minY = quantityMax(y, rhs.y, "y")
        val maxY = quantityMin(this.maxY, rhs.maxY, "y")
        return if ((minX ls maxX) == true && (minY ls maxY) == true) {
            QuantityRectangle2(
                minX = minX,
                minY = minY,
                maxX = maxX,
                maxY = maxY
            )
        } else {
            null
        }
    }

    fun toPlacement3(): List<Placement3<T>> {
        return when (projection) {
            is PlaneProjection<*, *> -> {
                listOf(Placement3(view, plane.point3(position)))
            }

            is PileProjection<*, *> -> {
                val depth = projection.view.depth
                var z = Flt64.zero * depth.unit
                val units = ArrayList<Placement3<T>>()
                for (i in 0 until projection.layer.toInt()) {
                    units.add(Placement3(projection.view, projection.plane.point3(position, distance = z)))
                    z += depth
                }
                units
            }

            is MultiPileProjection<*, *> -> {
                var z = Flt64.zero * Meter
                val units = ArrayList<Placement3<T>>()
                for (view in projection.views) {
                    units.add(Placement3(view as CuboidView<T>, projection.plane.point3(position, distance = z)))
                    z += view.depth
                }
                units
            }
        }
    }

    override fun copy() = Placement2(projection.copy(), position)
}

data class Placement3<T : Cuboid<T>>(
    val view: CuboidView<T>,
    val position: QuantityPoint3
) : Copyable<Placement3<T>>, Ord<Placement3<T>> {
    constructor(
        view: CuboidView<T>,
        position: Point<Dim3, Flt64>
    ) : this(view, point3(position))

    private var _parent: Placement3<*>? = null

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
    val absoluteX: QuantityFlt64 get() = x + (parent?.absoluteX ?: (Flt64.zero * x.unit))
    val absoluteY: QuantityFlt64 get() = y + (parent?.absoluteY ?: (Flt64.zero * y.unit))
    val absoluteZ: QuantityFlt64 get() = z + (parent?.absoluteZ ?: (Flt64.zero * z.unit))

    val absolutePlacement get() = Placement3(view, absolutePosition)

    val width by view::width
    val height by view::height
    val depth by view::depth

    val maxX: QuantityFlt64 = x + width
    val maxY: QuantityFlt64 = y + height
    val maxZ: QuantityFlt64 = z + depth
    val maxPosition: QuantityPoint3 = position + QuantityVector3(x = width, y = height, z = depth)

    val maxAbsoluteX: QuantityFlt64 get() = absoluteX + width
    val maxAbsoluteY: QuantityFlt64 get() = absoluteY + height
    val maxAbsoluteZ: QuantityFlt64 get() = absoluteZ + depth
    val maxAbsolutePosition: QuantityPoint3 get() = absolutePosition + QuantityVector3(x = width, y = height, z = depth)

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
        val includeLower = withBorder && withLowerBound
        val includeUpper = withBorder && withUpperBound
        return containsInRange(point.x, absoluteX, maxAbsoluteX, includeLower, includeUpper)
                && containsInRange(point.y, absoluteY, maxAbsoluteY, includeLower, includeUpper)
                && containsInRange(point.z, absoluteZ, maxAbsoluteZ, includeLower, includeUpper)
    }

    infix fun overlapped(rhs: Placement3<*>): Boolean {
        if ((maxAbsoluteX leq rhs.absoluteX) == true || (absoluteX geq rhs.maxAbsoluteX) == true) {
            return false
        }
        if ((maxAbsoluteY leq rhs.absoluteY) == true || (absoluteY geq rhs.maxAbsoluteY) == true) {
            return false
        }
        if ((maxAbsoluteZ leq rhs.absoluteZ) == true || (absoluteZ geq rhs.maxAbsoluteZ) == true) {
            return false
        }
        return true
    }

    override fun copy() = Placement3(view.copy(), position)

    override fun partialOrd(rhs: Placement3<T>): Order {
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

        other as Placement3<*>

        if (view != other.view) return false
        if (position != other.position) return false

        return true
    }
}

fun topPlacements(placements: List<Placement3<*>>): List<Placement3<*>> {
    val topPlacements = ArrayList<Placement3<*>>()
    for (placement1 in placements) {
        val bottom1 = Placement2(placement1, Bottom)
        var flag = true
        for (placement2 in placements) {
            val bottom2 = Placement2(placement2, Bottom)
            if (bottom1.overlapped(bottom2)
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

fun bottomPlacements(placements: List<Placement3<*>>): List<Placement3<*>> {
    val bottomPlacements = ArrayList<Placement3<*>>()
    for (placement1 in placements) {
        val bottom1 = Placement2(placement1, Bottom)
        var flag = true
        for (placement2 in placements) {
            val bottom2 = Placement2(placement2, Bottom)
            if (bottom1.overlapped(bottom2)
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
