@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.math.functional.sumOf
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.eq
import fuookami.ospf.kotlin.quantities.quantity.geq
import fuookami.ospf.kotlin.quantities.quantity.gr
import fuookami.ospf.kotlin.quantities.quantity.plus
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter

private fun maxQuantity(values: Iterable<QuantityFlt64>): QuantityFlt64? {
    var maximum: QuantityFlt64? = null
    for (value in values) {
        maximum = if (maximum == null || (value gr maximum) == true) {
            value
        } else {
            maximum
        }
    }
    return maximum
}

data class ProjectionShape(
    val length: QuantityFlt64,
    val width: QuantityFlt64
) {
    companion object {
        operator fun invoke(length: QuantityFlt64, width: QuantityFlt64): ProjectionShape {
            return if ((length geq width) == true) {
                ProjectionShape(length, width)
            } else {
                ProjectionShape(width, length)
            }
        }
    }

    val area: QuantityFlt64 = length * width
}

sealed class ProjectivePlane {
    abstract fun length(unit: AbstractCuboid, orientation: Orientation = Orientation.Upright): QuantityFlt64
    abstract fun width(unit: AbstractCuboid, orientation: Orientation = Orientation.Upright): QuantityFlt64
    abstract fun height(unit: AbstractCuboid, orientation: Orientation = Orientation.Upright): QuantityFlt64

    abstract fun length(space: AbstractContainer3Shape): QuantityFlt64
    abstract fun width(space: AbstractContainer3Shape): QuantityFlt64
    abstract fun height(space: AbstractContainer3Shape): QuantityFlt64

    fun shape(unit: AbstractCuboid, orientation: Orientation = Orientation.Upright) = ProjectionShape.invoke(
        length = this.length(unit, orientation),
        width = this.width(unit, orientation)
    )

    fun shape(space: AbstractContainer3Shape) = ProjectionShape.invoke(
        length = this.length(space),
        width = this.width(space)
    )

    abstract fun distance(point: QuantityPoint3): QuantityFlt64
    abstract fun point2(point: QuantityPoint3): QuantityPoint2
    abstract fun point3(point: QuantityPoint2, distance: QuantityFlt64 = Flt64.zero * Meter): QuantityPoint3
    abstract fun vector(distance: QuantityFlt64 = Flt64.one * Meter): QuantityVector3
}

typealias Direction = ProjectivePlane

/**
 * ZOX
 */
object Bottom : ProjectivePlane() {
    override fun length(unit: AbstractCuboid, orientation: Orientation) = orientation.depth(unit)
    override fun width(unit: AbstractCuboid, orientation: Orientation) = orientation.width(unit)
    override fun height(unit: AbstractCuboid, orientation: Orientation) = orientation.height(unit)

    override fun length(space: AbstractContainer3Shape) = space.depth
    override fun width(space: AbstractContainer3Shape) = space.width
    override fun height(space: AbstractContainer3Shape) = space.height

    override fun distance(point: QuantityPoint3) = point.y
    override fun point2(point: QuantityPoint3) = QuantityPoint2(x = point.z, y = point.x)
    override fun point3(point: QuantityPoint2, distance: QuantityFlt64) = QuantityPoint3(x = point.y, y = distance, z = point.x)
    override fun vector(distance: QuantityFlt64) = QuantityVector3(y = distance, x = Flt64.zero * distance.unit, z = Flt64.zero * distance.unit)

    override fun toString(): String {
        return "Bottom-ZOX"
    }
}

typealias ZOX = Bottom

/**
 * XOY
 */
object Side : ProjectivePlane() {
    override fun length(unit: AbstractCuboid, orientation: Orientation) = orientation.width(unit)
    override fun width(unit: AbstractCuboid, orientation: Orientation) = orientation.height(unit)
    override fun height(unit: AbstractCuboid, orientation: Orientation) = orientation.depth(unit)

    override fun length(space: AbstractContainer3Shape) = space.width
    override fun width(space: AbstractContainer3Shape) = space.height
    override fun height(space: AbstractContainer3Shape) = space.depth

    override fun distance(point: QuantityPoint3) = point.z
    override fun point2(point: QuantityPoint3) = QuantityPoint2(x = point.x, y = point.y)
    override fun point3(point: QuantityPoint2, distance: QuantityFlt64) = QuantityPoint3(x = point.x, y = point.y, z = distance)
    override fun vector(distance: QuantityFlt64) = QuantityVector3(z = distance, x = Flt64.zero * distance.unit, y = Flt64.zero * distance.unit)

    override fun toString(): String {
        return "Side-XOY"
    }
}

typealias XOY = Side

/**
 * ZOY
 */
object Front : ProjectivePlane() {
    override fun length(unit: AbstractCuboid, orientation: Orientation) = orientation.depth(unit)
    override fun width(unit: AbstractCuboid, orientation: Orientation) = orientation.height(unit)
    override fun height(unit: AbstractCuboid, orientation: Orientation) = orientation.width(unit)

    override fun length(space: AbstractContainer3Shape) = space.depth
    override fun width(space: AbstractContainer3Shape) = space.height
    override fun height(space: AbstractContainer3Shape) = space.width

    override fun distance(point: QuantityPoint3) = point.x
    override fun point2(point: QuantityPoint3) = QuantityPoint2(x = point.y, y = point.z)
    override fun point3(point: QuantityPoint2, distance: QuantityFlt64) = QuantityPoint3(x = distance, y = point.x, z = point.y)
    override fun vector(distance: QuantityFlt64) = QuantityVector3(x = distance, y = Flt64.zero * distance.unit, z = Flt64.zero * distance.unit)

    override fun toString(): String {
        return "Front-ZOY"
    }
}

typealias ZOY = Front

sealed interface Projection<
        T : Cuboid<T>,
        P : ProjectivePlane
        > : Copyable<Projection<T, P>> {
    val view: CuboidView<T>
    val plane: P
    val unit: T get() = view.unit
    val orientation: Orientation get() = view.orientation
    val length: QuantityFlt64 get() = plane.length(view)
    val width: QuantityFlt64 get() = plane.width(view)
    val height: QuantityFlt64 get() = plane.height(view)
    val area: QuantityFlt64 get() = length * width
    val weight: QuantityFlt64 get() = unit.weight

    fun amount(unit: AbstractCuboid): UInt64
}

data class PlaneProjection<
        T : Cuboid<T>,
        P : ProjectivePlane
        >(
    override val view: CuboidView<T>,
    override val plane: P
) : Projection<T, P> {
    override fun amount(unit: AbstractCuboid): UInt64 {
        return if (unit == this.unit) {
            UInt64.one
        } else {
            UInt64.zero
        }
    }

    override fun copy() = PlaneProjection(view.copy(), plane)
}

data class PileProjection<
        T : Cuboid<T>,
        P : ProjectivePlane
        >(
    override val view: CuboidView<T>,
    override val plane: P,
    val layer: UInt64,
) : Projection<T, P> {
    override val height = plane.height(view) * layer.toFlt64()
    override val weight = unit.weight * layer.toFlt64()

    constructor(plane: PlaneProjection<T, P>, layer: UInt64 = UInt64.one) : this(plane.view, plane.plane, layer)

    override fun amount(unit: AbstractCuboid): UInt64 {
        return if (unit == this.unit) {
            layer
        } else {
            UInt64.zero
        }
    }

    override fun copy() = PileProjection(view.copy(), plane, layer)
}

data class MultiPileProjection<
        T : Cuboid<T>,
        P : ProjectivePlane
        >(
    val views: List<CuboidView<T>>,
    override val plane: P,
) : Projection<T, P> {
    override val view = views.first()

    override val length = maxQuantity(views.map { plane.length(it) }) ?: (Flt64.zero * Meter)
    override val width = maxQuantity(views.map { plane.width(it) }) ?: (Flt64.zero * Meter)
    override val height = views.asSequence().fold(Flt64.zero * length.unit) { acc, item ->
        acc + plane.height(item)
    }
    override val weight = views.asSequence().fold(Flt64.zero * Kilogram) { acc, item ->
        acc + item.weight
    }

    override fun amount(unit: AbstractCuboid) = UInt64(views.count { it.unit == unit })

    override fun copy() = MultiPileProjection(views.map { it.copy() }, plane)
}
