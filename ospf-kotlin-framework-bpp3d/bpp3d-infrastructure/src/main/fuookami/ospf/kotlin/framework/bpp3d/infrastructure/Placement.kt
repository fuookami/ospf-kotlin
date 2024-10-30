package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.geometry.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*

data class Placement2<
    T : CuboidUnit<T>,
    P : ProjectivePlane
>(
    val projection: Projection<*, T, P>,
    val position: Point2
) : Copyable<Placement2<T, P>> {
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
    val maxPosition = Point2(maxX, maxY)

    fun contains(
        point: Point2,
        withLowerBound: Boolean = true,
        withUpperBound: Boolean = true,
        withBorder: Boolean = true
    ): Boolean {
        val lowerInterval = if (withBorder && withLowerBound) {
            Interval.Closed
        } else {
            Interval.Open
        }
        val upperInterval = if (withBorder && withUpperBound) {
            Interval.Closed
        } else {
            Interval.Open
        }
        val xRange = ValueRange(x, maxX, lowerInterval, upperInterval, Flt64).value!!
        val yRange = ValueRange(y, maxY, lowerInterval, upperInterval, Flt64).value!!
        return xRange.contains(point.x) && yRange.contains(point.y)
    }

    fun overlapped(rhs: Placement2<*, P>): Boolean {
        if (maxX leq rhs.x || x geq rhs.maxX) {
            return false
        }
        if (maxY leq rhs.y || y geq rhs.maxY) {
            return false
        }
        return true
    }

    fun intersect(rhs: Placement2<*, P>): Rectangle2? {
        val minX = max(x, rhs.x)
        val maxX = min(maxX, rhs.maxX)
        val minY = max(y, rhs.y)
        val maxY = min(maxY, rhs.maxY)
        return if (minX ls maxX && minY ls maxY) {
            Rectangle2(point2(x = minX, y = minY), point2(x = minX, y = maxY), point2(x = maxX, y = maxY), point2(x = maxX, y = minY))
        } else {
            null
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun toPlacement3(): List<Placement3<T>> {
        return when (projection) {
            is PlaneProjection<*, *> -> {
                listOf(Placement3(view, plane.point3(position)))
            }

            is PileProjection<*, *> -> {
                val depth = projection.view.depth
                var z = Flt64.zero
                val units = ArrayList<Placement3<T>>()
                for (i in 0 until projection.layer.toInt()) {
                    units.add(Placement3(projection.view, projection.plane.point3(position, distance = z)))
                    z += depth
                }
                units
            }

            is MultiPileProjection<*, *> -> {
                var z = Flt64.zero
                val units = ArrayList<Placement3<T>>()
                for (view in projection.views) {
                    units.add(Placement3(view as CuboidView<T>, projection.plane.point3(position, distance = z)))
                    z += view.depth
                }
                units
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun copy() = Placement2(projection.copy() as Projection<*, T, P>, position)
}

data class Placement3<T : CuboidUnit<T>>(
    val view: CuboidView<T>,
    val position: Point3
) : Copyable<Placement3<T>>, Ord<Placement3<T>> {
    private var _parent: Placement3<*>? = null

    val unit by view::unit
    val orientation by view::orientation
    val parent by this::_parent
    val weight by unit::weight
    val volume by unit::volume

    val x by position::x
    val y by position::y
    val z by position::z

    val absolutePosition: Point3 get() = position + (parent?.absolutePosition ?: point3())
    val absoluteX: Flt64 get() = x + (parent?.absoluteX ?: Flt64.zero)
    val absoluteY: Flt64 get() = y + (parent?.absoluteY ?: Flt64.zero)
    val absoluteZ: Flt64 get() = z + (parent?.absoluteZ ?: Flt64.zero)

    val absolutePlacement get() = Placement3(view, absolutePosition)

    val width by view::width
    val height by view::height
    val depth by view::depth

    val maxX: Flt64 = x + width
    val maxY: Flt64 = y + height
    val maxZ: Flt64 = z + depth
    val maxPosition: Point3 = position + vector3(x = width, y = height, z = depth)

    val maxAbsoluteX: Flt64 get() = absoluteX + width
    val maxAbsoluteY: Flt64 get() = absoluteY + height
    val maxAbsoluteZ: Flt64 get() = absoluteZ + depth
    val maxAbsolutePosition: Point3 get() = absolutePosition + vector3(x = width, y = height, z = depth)

    init {
        if (unit is Container3<*>) {
            for (placement in (unit as Container3<*>).units) {
                placement._parent = this
            }
        }
    }

    fun contains(
        point: Point3,
        withLowerBound: Boolean = true,
        withUpperBound: Boolean = true,
        withBorder: Boolean = true
    ): Boolean {
        val lowerInterval = if (withBorder && withLowerBound) {
            Interval.Closed
        } else {
            Interval.Open
        }
        val upperInterval = if (withBorder && withUpperBound) {
            Interval.Closed
        } else {
            Interval.Open
        }
        val xRange = ValueRange(absoluteX, maxAbsoluteX, lowerInterval, upperInterval, Flt64).value!!
        val yRange = ValueRange(absoluteY, maxAbsoluteY, lowerInterval, upperInterval, Flt64).value!!
        val zRange = ValueRange(absoluteZ, maxAbsoluteZ, lowerInterval, upperInterval, Flt64).value!!
        return xRange.contains(point.x) && yRange.contains(point.y) && zRange.contains(point.z)
    }

    infix fun overlapped(rhs: Placement3<*>): Boolean {
        if (maxAbsoluteX leq rhs.absoluteX || absoluteX geq rhs.maxAbsoluteX) {
            return false
        }
        if (maxAbsoluteY leq rhs.absoluteY || absoluteY geq rhs.maxAbsoluteY) {
            return false
        }
        if (maxAbsoluteZ leq rhs.absoluteZ || absoluteZ geq rhs.maxAbsoluteZ) {
            return false
        }
        return true
    }

    override fun copy() = Placement3(view.copy(), position)

    override fun partialOrd(rhs: Placement3<T>): Order {
        when (val value = z ord rhs.z) {
            Order.Equal -> {}

            else -> {
                return value
            }
        }
        when (val value = y ord rhs.y) {
            Order.Equal -> {}

            else -> {
                return value
            }
        }
        return x ord rhs.x
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
            val bottom2 = Placement2(placement1, Bottom)
            if (bottom1.overlapped(bottom2)
                && placement1.maxY ls placement2.maxY
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
            val bottom2 = Placement2(placement1, Bottom)
            if (bottom1.overlapped(bottom2)
                && placement1.y gr placement2.y
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
