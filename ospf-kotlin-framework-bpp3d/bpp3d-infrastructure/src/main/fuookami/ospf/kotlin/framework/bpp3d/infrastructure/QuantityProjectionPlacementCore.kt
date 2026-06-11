@file:Suppress("DEPRECATION")

/**
 * 多态投影放置核心实现。
 * Quantity projection placement core implementation.
 */
package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.math.geometry.QuantityCuboid3
import fuookami.ospf.kotlin.math.geometry.QuantityPlacement2 as GeometryPlacement2
import fuookami.ospf.kotlin.math.geometry.QuantityPlacement3 as GeometryPlacement3
import fuookami.ospf.kotlin.math.geometry.QuantityRectangle2
import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.utils.functional.Ord
import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.partialOrd
import fuookami.ospf.kotlin.quantities.quantity.minus
import fuookami.ospf.kotlin.quantities.quantity.plus
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Kilogram

private fun <V : FloatingNumber<V>> projectionQuantityPlus(
    lhs: Quantity<V>,
    rhs: Quantity<V>
): Quantity<V> {
    @Suppress("UNCHECKED_CAST")
    return when (lhs.value) {
        is InfraNumber -> ((lhs as Quantity<InfraNumber>) + (rhs as Quantity<InfraNumber>)) as Quantity<V>
        is FltX -> ((lhs as Quantity<FltX>) + (rhs as Quantity<FltX>)) as Quantity<V>
        else -> throw IllegalArgumentException("Unsupported quantity scalar type: ${lhs.value::class.simpleName}")
    }
}

private fun <V : FloatingNumber<V>> projectionQuantityMinus(
    lhs: Quantity<V>,
    rhs: Quantity<V>
): Quantity<V> {
    @Suppress("UNCHECKED_CAST")
    return when (lhs.value) {
        is InfraNumber -> ((lhs as Quantity<InfraNumber>) - (rhs as Quantity<InfraNumber>)) as Quantity<V>
        is FltX -> ((lhs as Quantity<FltX>) - (rhs as Quantity<FltX>)) as Quantity<V>
        else -> throw IllegalArgumentException("Unsupported quantity scalar type: ${lhs.value::class.simpleName}")
    }
}

private fun <V : FloatingNumber<V>> projectionQuantityTimes(
    lhs: Quantity<V>,
    rhs: Quantity<V>
): Quantity<V> {
    @Suppress("UNCHECKED_CAST")
    return when (lhs.value) {
        is InfraNumber -> ((lhs as Quantity<InfraNumber>) * (rhs as Quantity<InfraNumber>)) as Quantity<V>
        is FltX -> ((lhs as Quantity<FltX>) * (rhs as Quantity<FltX>)) as Quantity<V>
        else -> throw IllegalArgumentException("Unsupported quantity scalar type: ${lhs.value::class.simpleName}")
    }
}

private fun <V : FloatingNumber<V>> projectionQuantityZero(sample: Quantity<V>): Quantity<V> {
    return projectionQuantityMinus(sample, sample)
}

private fun <V : FloatingNumber<V>> quantityOrd(lhs: Quantity<V>, rhs: Quantity<V>, axis: String): Order {
    return lhs.partialOrd(rhs)
        ?: throw IllegalArgumentException("Incomparable quantity on axis $axis: ${lhs.unit} vs ${rhs.unit}")
}

private fun <V : FloatingNumber<V>> maxQuantity(values: Iterable<Quantity<V>>): Quantity<V>? {
    var maximum: Quantity<V>? = null
    for (value in values) {
        maximum = if (maximum == null || quantityOrd(value, maximum, "max") is Order.Greater) {
            value
        } else {
            maximum
        }
    }
    return maximum
}

private fun <V : FloatingNumber<V>> repeatedQuantitySum(sample: Quantity<V>, times: UInt64): Quantity<V> {
    var acc = projectionQuantityZero(sample)
    var i = UInt64.zero
    while (i < times) {
        acc = projectionQuantityPlus(acc, sample)
        i += UInt64.one
    }
    return acc
}

sealed interface QuantityProjection<
        T : QuantityCuboid<T, V>,
        V : FloatingNumber<V>,
        P : ProjectivePlane
        > : Copyable<QuantityProjection<T, V, P>> {
    val view: QuantityCuboidView<T, V>
    val plane: P
    val unit: T
        get() = view.unit
    val orientation: Orientation
        get() = view.orientation
    val length: Quantity<V>
        get() = plane.length(view)
    val width: Quantity<V>
        get() = plane.width(view)
    val height: Quantity<V>
        get() = plane.height(view)
    val area: Quantity<V>
        get() = projectionQuantityTimes(length, width)
    val weight: Quantity<V>
        get() = unit.weight

    fun amount(unit: AbstractCuboid<*>): UInt64
    fun toPlacement3At(position: QuantityPoint2G<V>): List<QuantityCuboidPlacement3<T, V>>
}

data class QuantityPlaneProjection<
        T : QuantityCuboid<T, V>,
        V : FloatingNumber<V>,
        P : ProjectivePlane
        >(
    override val view: QuantityCuboidView<T, V>,
    override val plane: P
) : QuantityProjection<T, V, P> {
    override fun amount(unit: AbstractCuboid<*>): UInt64 {
        return if (unit == this.unit) {
            UInt64.one
        } else {
            UInt64.zero
        }
    }

    override fun toPlacement3At(position: QuantityPoint2G<V>): List<QuantityCuboidPlacement3<T, V>> {
        val zeroDistance = projectionQuantityZero(view.depth)
        return listOf(QuantityCuboidPlacement3(view, plane.point3(position, zeroDistance)))
    }

    override fun copy(): QuantityProjection<T, V, P> {
        return QuantityPlaneProjection(view.copy(), plane)
    }
}

data class QuantityPileProjection<
        T : QuantityCuboid<T, V>,
        V : FloatingNumber<V>,
        P : ProjectivePlane
        >(
    override val view: QuantityCuboidView<T, V>,
    override val plane: P,
    val layer: UInt64
) : QuantityProjection<T, V, P> {
    override val height: Quantity<V> = repeatedQuantitySum(plane.height(view), layer)
    override val weight: Quantity<V> = repeatedQuantitySum(unit.weight, layer)

    constructor(planeProjection: QuantityPlaneProjection<T, V, P>, layer: UInt64 = UInt64.one) : this(
        view = planeProjection.view,
        plane = planeProjection.plane,
        layer = layer
    )

    override fun amount(unit: AbstractCuboid<*>): UInt64 {
        return if (unit == this.unit) {
            layer
        } else {
            UInt64.zero
        }
    }

    override fun toPlacement3At(position: QuantityPoint2G<V>): List<QuantityCuboidPlacement3<T, V>> {
        val units = ArrayList<QuantityCuboidPlacement3<T, V>>()
        val depth = view.depth
        var z = projectionQuantityZero(depth)
        var i = UInt64.zero
        while (i < layer) {
            units.add(QuantityCuboidPlacement3(view, plane.point3(position, z)))
            z = projectionQuantityPlus(z, depth)
            i += UInt64.one
        }
        return units
    }

    override fun copy(): QuantityProjection<T, V, P> {
        return QuantityPileProjection(view.copy(), plane, layer)
    }
}

data class QuantityMultiPileProjection<
        T : QuantityCuboid<T, V>,
        V : FloatingNumber<V>,
        P : ProjectivePlane
        >(
    val views: List<QuantityCuboidView<T, V>>,
    override val plane: P
) : QuantityProjection<T, V, P> {
    override val view: QuantityCuboidView<T, V> = views.first()

    override val length: Quantity<V> = maxQuantity(views.map { plane.length(it) }) ?: projectionQuantityZero(view.depth)
    override val width: Quantity<V> = maxQuantity(views.map { plane.width(it) }) ?: projectionQuantityZero(view.width)
    override val height: Quantity<V> = views.asSequence().fold(projectionQuantityZero(plane.height(view))) { acc, item ->
        projectionQuantityPlus(acc, plane.height(item))
    }
    override val weight: Quantity<V> = views.asSequence().fold(projectionQuantityZero(unit.weight)) { acc, item ->
        projectionQuantityPlus(acc, item.weight)
    }

    override fun amount(unit: AbstractCuboid<*>): UInt64 {
        return UInt64(views.count { it.unit == unit })
    }

    override fun toPlacement3At(position: QuantityPoint2G<V>): List<QuantityCuboidPlacement3<T, V>> {
        if (views.isEmpty()) {
            return emptyList()
        }
        val units = ArrayList<QuantityCuboidPlacement3<T, V>>(views.size)
        var z = projectionQuantityZero(views.first().depth)
        for (itemView in views) {
            units.add(QuantityCuboidPlacement3(itemView, plane.point3(position, z)))
            z = projectionQuantityPlus(z, itemView.depth)
        }
        return units
    }

    override fun copy(): QuantityProjection<T, V, P> {
        return QuantityMultiPileProjection(views.map { it.copy() }, plane)
    }
}

data class QuantityProjectionPlacement2<
        T : QuantityCuboid<T, V>,
        V : FloatingNumber<V>,
        P : ProjectivePlane
        >(
    val projection: QuantityProjection<T, V, P>,
    val position: QuantityPoint2G<V>
) : Copyable<QuantityProjectionPlacement2<T, V, P>> {
    val unit by projection::unit
    val orientation by projection::orientation
    val view by projection::view
    val plane by projection::plane
    val weight by projection::weight

    val x by position::x
    val y by position::y
    val length by projection::length
    val width by projection::width

    val maxX: Quantity<V> = projectionQuantityPlus(x, length)
    val maxY: Quantity<V> = projectionQuantityPlus(y, width)
    val maxPosition: QuantityPoint2G<V> = QuantityPoint2G(x = maxX, y = maxY)

    private fun toGeometryPlacement(): GeometryPlacement2<V> {
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
        point: QuantityPoint2G<V>,
        withLowerBound: Boolean = true,
        withUpperBound: Boolean = true,
        withBorder: Boolean = true
    ): Boolean {
        return toGeometryPlacement().contains(
            x = point.x,
            y = point.y,
            withLowerBound = withLowerBound,
            withUpperBound = withUpperBound,
            withBorder = withBorder
        )
    }

    fun overlapped(rhs: QuantityProjectionPlacement2<*, V, P>): Boolean {
        return toGeometryPlacement().overlapped(rhs.toGeometryPlacement())
    }

    fun intersect(rhs: QuantityProjectionPlacement2<*, V, P>): QuantityRectangle2<V>? {
        val intersection = toGeometryPlacement().intersect(rhs.toGeometryPlacement()) ?: return null
        return QuantityRectangle2(
            width = intersection.width,
            height = intersection.height
        )
    }

    fun toPlacement3(): List<QuantityCuboidPlacement3<T, V>> {
        return projection.toPlacement3At(position)
    }

    override fun copy(): QuantityProjectionPlacement2<T, V, P> {
        return QuantityProjectionPlacement2(projection.copy(), position)
    }
}

data class QuantityCuboidPlacement3<
        T : QuantityCuboid<T, V>,
        V : FloatingNumber<V>
        >(
    val view: QuantityCuboidView<T, V>,
    val position: QuantityPoint3G<V>
) : Copyable<QuantityCuboidPlacement3<T, V>>, Ord<QuantityCuboidPlacement3<T, V>> {
    val unit by view::unit
    val orientation by view::orientation
    val weight by unit::weight
    val volume by unit::volume

    val x by position::x
    val y by position::y
    val z by position::z

    val width by view::width
    val height by view::height
    val depth by view::depth

    val maxX: Quantity<V> = projectionQuantityPlus(x, width)
    val maxY: Quantity<V> = projectionQuantityPlus(y, height)
    val maxZ: Quantity<V> = projectionQuantityPlus(z, depth)
    val maxPosition: QuantityPoint3G<V> = QuantityPoint3G(x = maxX, y = maxY, z = maxZ)

    private fun toGeometryPlacement(): GeometryPlacement3<V> {
        return GeometryPlacement3(
            x = x,
            y = y,
            z = z,
            shape = QuantityCuboid3(
                width = width,
                height = height,
                depth = depth
            )
        )
    }

    fun contains(
        point: QuantityPoint3G<V>,
        withLowerBound: Boolean = true,
        withUpperBound: Boolean = true,
        withBorder: Boolean = true
    ): Boolean {
        return toGeometryPlacement().contains(
            x = point.x,
            y = point.y,
            z = point.z,
            withLowerBound = withLowerBound,
            withUpperBound = withUpperBound,
            withBorder = withBorder
        )
    }

    infix fun overlapped(rhs: QuantityCuboidPlacement3<*, V>): Boolean {
        return toGeometryPlacement().overlapped(rhs.toGeometryPlacement())
    }

    override fun copy(): QuantityCuboidPlacement3<T, V> {
        return QuantityCuboidPlacement3(view.copy(), position)
    }

    override fun partialOrd(rhs: QuantityCuboidPlacement3<T, V>): Order {
        when (val value = quantityOrd(z, rhs.z, "z")) {
            Order.Equal -> {}
            else -> return value
        }
        when (val value = quantityOrd(y, rhs.y, "y")) {
            Order.Equal -> {}
            else -> return value
        }
        return quantityOrd(x, rhs.x, "x")
    }
}

fun <T : QuantityCuboid<T, V>, V : FloatingNumber<V>> topPlacements(
    placements: List<QuantityCuboidPlacement3<T, V>>
): List<QuantityCuboidPlacement3<T, V>> {
    val topPlacements = ArrayList<QuantityCuboidPlacement3<T, V>>()
    for (placement1 in placements) {
        var flag = true
        for (placement2 in placements) {
            if (placement1 overlapped placement2
                && quantityOrd(placement1.maxY, placement2.maxY, "y") is Order.Less
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

fun <T : QuantityCuboid<T, V>, V : FloatingNumber<V>> bottomPlacements(
    placements: List<QuantityCuboidPlacement3<T, V>>
): List<QuantityCuboidPlacement3<T, V>> {
    val bottomPlacements = ArrayList<QuantityCuboidPlacement3<T, V>>()
    for (placement1 in placements) {
        var flag = true
        for (placement2 in placements) {
            if (placement1 overlapped placement2
                && quantityOrd(placement1.y, placement2.y, "y") is Order.Greater
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

fun <T : Cuboid<T>> QuantityPlacement3<T>.asQuantityCuboidPlacement3(): QuantityCuboidPlacement3<ModelCuboidAdapter<T>, InfraNumber> {
    return QuantityCuboidPlacement3(
        view = unit.asQuantityCuboid().view(orientation),
        position = QuantityPoint3G(
            x = position.x,
            y = position.y,
            z = position.z
        )
    )
}

fun <T : Cuboid<T>, P : ProjectivePlane> QuantityPlacement2<T, P>.asQuantityProjectionPlacement2(): QuantityProjectionPlacement2<ModelCuboidAdapter<T>, InfraNumber, P> {
    return QuantityProjectionPlacement2(
        projection = QuantityPlaneProjection(
            view = unit.asQuantityCuboid().view(orientation),
            plane = plane
        ),
        position = QuantityPoint2G(
            x = position.x,
            y = position.y
        )
    )
}

fun <T : Cuboid<T>, P : ProjectivePlane> Projection<T, P>.asQuantityProjection(): QuantityProjection<ModelCuboidAdapter<T>, InfraNumber, P> {
    return when (this) {
        is PlaneProjection -> QuantityPlaneProjection(
            view = unit.asQuantityCuboid().view(orientation),
            plane = plane
        )

        is PileProjection -> QuantityPileProjection(
            view = unit.asQuantityCuboid().view(orientation),
            plane = plane,
            layer = layer
        )

        is MultiPileProjection -> QuantityMultiPileProjection(
            views = views.map { it.unit.asQuantityCuboid().view(it.orientation) },
            plane = plane
        )
    }
}

