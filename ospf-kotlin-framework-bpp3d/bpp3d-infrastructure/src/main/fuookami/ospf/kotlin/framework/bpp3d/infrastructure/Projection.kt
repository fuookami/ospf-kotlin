@file:Suppress("DEPRECATION")

/**
 * 投影基础设施。
 * Projection infrastructure.
 */
package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.geq
import fuookami.ospf.kotlin.quantities.quantity.gr
import fuookami.ospf.kotlin.quantities.quantity.times

private fun <V : FloatingNumber<V>> projectionQuantityZero(sample: Quantity<V>): Quantity<V> {
    return quantityZeroByValue(sample)
}

private fun <V : FloatingNumber<V>> repeatedQuantitySum(sample: Quantity<V>, times: UInt64): Quantity<V> {
    return repeatedQuantitySumByValue(sample, times)
}

private fun <V : FloatingNumber<V>> maxQuantity(values: Iterable<Quantity<V>>): Quantity<V>? {
    var maximum: Quantity<V>? = null
    for (value in values) {
        maximum = if (maximum == null || (value gr maximum) == true) {
            value
        } else {
            maximum
        }
    }
    return maximum
}

data class ProjectionShape<V : FloatingNumber<V>>(
    val length: Quantity<V>,
    val width: Quantity<V>
) {
    companion object {
        operator fun <V : FloatingNumber<V>> invoke(length: Quantity<V>, width: Quantity<V>): ProjectionShape<V> {
            return if ((length geq width) == true) {
                ProjectionShape(length, width)
            } else {
                ProjectionShape(width, length)
            }
        }
    }

    val area: Quantity<V> = length * width
}

sealed class ProjectivePlane {
    abstract fun <V : FloatingNumber<V>> length(unit: AbstractCuboid<V>, orientation: Orientation = Orientation.Upright): Quantity<V>
    abstract fun <V : FloatingNumber<V>> width(unit: AbstractCuboid<V>, orientation: Orientation = Orientation.Upright): Quantity<V>
    abstract fun <V : FloatingNumber<V>> height(unit: AbstractCuboid<V>, orientation: Orientation = Orientation.Upright): Quantity<V>

    abstract fun length(space: AbstractContainer3Shape): Quantity<FltX>
    abstract fun width(space: AbstractContainer3Shape): Quantity<FltX>
    abstract fun height(space: AbstractContainer3Shape): Quantity<FltX>

    open fun <V : FloatingNumber<V>> length(space: Container3Geometry<V>): Quantity<V> {
        return when (this) {
            Bottom -> space.depth
            Side -> space.width
            Front -> space.depth
        }
    }

    open fun <V : FloatingNumber<V>> width(space: Container3Geometry<V>): Quantity<V> {
        return when (this) {
            Bottom -> space.width
            Side -> space.height
            Front -> space.height
        }
    }

    open fun <V : FloatingNumber<V>> height(space: Container3Geometry<V>): Quantity<V> {
        return when (this) {
            Bottom -> space.height
            Side -> space.depth
            Front -> space.width
        }
    }

    fun <V : FloatingNumber<V>> shape(unit: AbstractCuboid<V>, orientation: Orientation = Orientation.Upright) = ProjectionShape.invoke(
        length = this.length(unit, orientation),
        width = this.width(unit, orientation)
    )

    fun shape(space: AbstractContainer3Shape): ProjectionShape<FltX> {
        return ProjectionShape(
            length = this.length(space),
            width = this.width(space)
        )
    }

    open fun <V : FloatingNumber<V>> distance(point: QuantityPoint3<V>): Quantity<V> {
        return distanceByGeometry(point)
    }

    open fun <V : FloatingNumber<V>> point2(point: QuantityPoint3<V>): QuantityPoint2<V> {
        return point2ByGeometry(point)
    }

    open fun <V : FloatingNumber<V>> point3(point: QuantityPoint2<V>, distance: Quantity<V>): QuantityPoint3<V> {
        return point3ByGeometry(point, distance)
    }

    open fun <V : FloatingNumber<V>> vector(distance: Quantity<V>): QuantityVector3<V> {
        return vectorByGeometry(distance)
    }
}

/**
 * ZOX
 */
object Bottom : ProjectivePlane() {
    override fun <V : FloatingNumber<V>> length(unit: AbstractCuboid<V>, orientation: Orientation) = orientation.depth(unit)
    override fun <V : FloatingNumber<V>> width(unit: AbstractCuboid<V>, orientation: Orientation) = orientation.width(unit)
    override fun <V : FloatingNumber<V>> height(unit: AbstractCuboid<V>, orientation: Orientation) = orientation.height(unit)

    override fun length(space: AbstractContainer3Shape): Quantity<FltX> = length(space as Container3Geometry<FltX>)
    override fun width(space: AbstractContainer3Shape): Quantity<FltX> = width(space as Container3Geometry<FltX>)
    override fun height(space: AbstractContainer3Shape): Quantity<FltX> = height(space as Container3Geometry<FltX>)

    override fun toString(): String {
        return "Bottom-ZOX"
    }
}

/**
 * XOY
 */
object Side : ProjectivePlane() {
    override fun <V : FloatingNumber<V>> length(unit: AbstractCuboid<V>, orientation: Orientation) = orientation.width(unit)
    override fun <V : FloatingNumber<V>> width(unit: AbstractCuboid<V>, orientation: Orientation) = orientation.height(unit)
    override fun <V : FloatingNumber<V>> height(unit: AbstractCuboid<V>, orientation: Orientation) = orientation.depth(unit)

    override fun length(space: AbstractContainer3Shape): Quantity<FltX> = length(space as Container3Geometry<FltX>)
    override fun width(space: AbstractContainer3Shape): Quantity<FltX> = width(space as Container3Geometry<FltX>)
    override fun height(space: AbstractContainer3Shape): Quantity<FltX> = height(space as Container3Geometry<FltX>)

    override fun toString(): String {
        return "Side-XOY"
    }
}

/**
 * ZOY
 */
object Front : ProjectivePlane() {
    override fun <V : FloatingNumber<V>> length(unit: AbstractCuboid<V>, orientation: Orientation) = orientation.depth(unit)
    override fun <V : FloatingNumber<V>> width(unit: AbstractCuboid<V>, orientation: Orientation) = orientation.height(unit)
    override fun <V : FloatingNumber<V>> height(unit: AbstractCuboid<V>, orientation: Orientation) = orientation.width(unit)

    override fun length(space: AbstractContainer3Shape): Quantity<FltX> = length(space as Container3Geometry<FltX>)
    override fun width(space: AbstractContainer3Shape): Quantity<FltX> = width(space as Container3Geometry<FltX>)
    override fun height(space: AbstractContainer3Shape): Quantity<FltX> = height(space as Container3Geometry<FltX>)

    override fun toString(): String {
        return "Front-ZOY"
    }
}

sealed interface Projection<
        T : Cuboid<T, V>,
        V : FloatingNumber<V>,
        P : ProjectivePlane
        > : Copyable<Projection<T, V, P>> {
    val view: CuboidView<T, V>
    val plane: P
    val unit: T get() = view.unit
    val orientation: Orientation get() = view.orientation
    val length: Quantity<V> get() = plane.length(view)
    val width: Quantity<V> get() = plane.width(view)
    val height: Quantity<V> get() = plane.height(view)
    val area: Quantity<V> get() = length * width
    val weight: Quantity<V> get() = unit.weight

    fun amount(unit: AbstractCuboid<*>): UInt64
    fun toPlacement3At(position: QuantityPoint2<V>): List<QuantityPlacement3<T, V>>
}

data class PlaneProjection<
        T : Cuboid<T, V>,
        V : FloatingNumber<V>,
        P : ProjectivePlane
        >(
    override val view: CuboidView<T, V>,
    override val plane: P
) : Projection<T, V, P> {
    override fun amount(unit: AbstractCuboid<*>): UInt64 {
        return if (unit == this.unit) {
            UInt64.one
        } else {
            UInt64.zero
        }
    }

    override fun toPlacement3At(position: QuantityPoint2<V>): List<QuantityPlacement3<T, V>> {
        return listOf(QuantityPlacement3(view, plane.point3(position, projectionQuantityZero(view.depth))))
    }

    override fun copy() = PlaneProjection(view.copy(), plane)
}

data class PileProjection<
        T : Cuboid<T, V>,
        V : FloatingNumber<V>,
        P : ProjectivePlane
        >(
    override val view: CuboidView<T, V>,
    override val plane: P,
    val layer: UInt64,
) : Projection<T, V, P> {
    override val height: Quantity<V> = repeatedQuantitySum(plane.height(view), layer)
    override val weight: Quantity<V> = repeatedQuantitySum(unit.weight, layer)

    constructor(plane: PlaneProjection<T, V, P>, layer: UInt64 = UInt64.one) : this(plane.view, plane.plane, layer)

    override fun amount(unit: AbstractCuboid<*>): UInt64 {
        return if (unit == this.unit) {
            layer
        } else {
            UInt64.zero
        }
    }

    override fun toPlacement3At(position: QuantityPoint2<V>): List<QuantityPlacement3<T, V>> {
        val placements = ArrayList<QuantityPlacement3<T, V>>()
        var distance = projectionQuantityZero(view.depth)
        var i = UInt64.zero
        while (i < layer) {
            placements.add(QuantityPlacement3(view, plane.point3(position, distance)))
            distance = quantityPlusByValue(distance, view.depth)
            i += UInt64.one
        }
        return placements
    }

    override fun copy() = PileProjection(view.copy(), plane, layer)
}

data class MultiPileProjection<
        T : Cuboid<T, V>,
        V : FloatingNumber<V>,
        P : ProjectivePlane
        >(
    val views: List<CuboidView<T, V>>,
    override val plane: P,
) : Projection<T, V, P> {
    override val view = views.first()

    override val length: Quantity<V> = maxQuantity(views.map { plane.length(it) }) ?: projectionQuantityZero(view.depth)
    override val width: Quantity<V> = maxQuantity(views.map { plane.width(it) }) ?: projectionQuantityZero(view.width)
    override val height: Quantity<V> = views.fold(projectionQuantityZero(plane.height(view))) { acc, item ->
        quantityPlusByValue(acc, plane.height(item))
    }
    override val weight: Quantity<V> = views.fold(projectionQuantityZero(unit.weight)) { acc, item ->
        quantityPlusByValue(acc, item.weight)
    }

    override fun amount(unit: AbstractCuboid<*>) = UInt64(views.count { it.unit == unit })

    override fun toPlacement3At(position: QuantityPoint2<V>): List<QuantityPlacement3<T, V>> {
        val placements = ArrayList<QuantityPlacement3<T, V>>(views.size)
        var distance = projectionQuantityZero(views.first().depth)
        for (itemView in views) {
            placements.add(QuantityPlacement3(itemView, plane.point3(position, distance)))
            distance = quantityPlusByValue(distance, itemView.depth)
        }
        return placements
    }

    override fun copy() = MultiPileProjection(views.map { it.copy() }, plane)
}
