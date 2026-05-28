@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.geq
import fuookami.ospf.kotlin.quantities.quantity.gr
import fuookami.ospf.kotlin.quantities.quantity.plus
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter

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

data class ProjectionShapeG<V : FloatingNumber<V>>(
    val length: Quantity<V>,
    val width: Quantity<V>
) {
    companion object {
        operator fun <V : FloatingNumber<V>> invoke(length: Quantity<V>, width: Quantity<V>): ProjectionShapeG<V> {
            return if ((length geq width) == true) {
                ProjectionShapeG(length, width)
            } else {
                ProjectionShapeG(width, length)
            }
        }
    }

    val area: Quantity<V> = length * width
}

typealias ProjectionShape = ProjectionShapeG<InfraNumber>

private fun <T : Cuboid<T>> GenericQuantityPlacement3<LegacyCuboidGenericAdapter<T>, InfraNumber>.toLegacyPlacement3(): QuantityPlacement3<T> {
    val legacyUnit = view.unit.cuboid
    val legacyView = legacyUnit.view(orientation)
        ?: throw IllegalStateException("Legacy cuboid view is unavailable for orientation $orientation")
    return QuantityPlacement3(
        view = legacyView,
        position = QuantityPoint3(
            x = position.x,
            y = position.y,
            z = position.z
        )
    )
}

sealed class ProjectivePlane {
    abstract fun <V : FloatingNumber<V>> length(unit: AbstractCuboid<V>, orientation: Orientation = Orientation.Upright): Quantity<V>
    abstract fun <V : FloatingNumber<V>> width(unit: AbstractCuboid<V>, orientation: Orientation = Orientation.Upright): Quantity<V>
    abstract fun <V : FloatingNumber<V>> height(unit: AbstractCuboid<V>, orientation: Orientation = Orientation.Upright): Quantity<V>

    abstract fun length(space: AbstractContainer3Shape): Quantity<InfraNumber>
    abstract fun width(space: AbstractContainer3Shape): Quantity<InfraNumber>
    abstract fun height(space: AbstractContainer3Shape): Quantity<InfraNumber>

    open fun <V : FloatingNumber<V>> length(space: GenericContainer3Shape<V>): Quantity<V> {
        return when (this) {
            Bottom -> space.depth
            Side -> space.width
            Front -> space.depth
        }
    }

    open fun <V : FloatingNumber<V>> width(space: GenericContainer3Shape<V>): Quantity<V> {
        return when (this) {
            Bottom -> space.width
            Side -> space.height
            Front -> space.height
        }
    }

    open fun <V : FloatingNumber<V>> height(space: GenericContainer3Shape<V>): Quantity<V> {
        return when (this) {
            Bottom -> space.height
            Side -> space.depth
            Front -> space.width
        }
    }

    fun <V : FloatingNumber<V>> shape(unit: AbstractCuboid<V>, orientation: Orientation = Orientation.Upright) = ProjectionShapeG.invoke(
        length = this.length(unit, orientation),
        width = this.width(unit, orientation)
    )

    fun shape(space: AbstractContainer3Shape): ProjectionShape {
        val genericSpace = space.asGenericContainer3Shape()
        return ProjectionShape.invoke(
            length = this.length(genericSpace),
            width = this.width(genericSpace)
        )
    }

    abstract fun distance(point: QuantityPoint3): Quantity<InfraNumber>
    abstract fun point2(point: QuantityPoint3): QuantityPoint2
    abstract fun point3(point: QuantityPoint2, distance: Quantity<InfraNumber> = infraZero() * Meter): QuantityPoint3
    abstract fun vector(distance: Quantity<InfraNumber> = infraOne() * Meter): QuantityVector3

    open fun <V : FloatingNumber<V>> distance(point: QuantityPoint3G<V>): Quantity<V> {
        return distanceByGeometry(point)
    }

    open fun <V : FloatingNumber<V>> point2(point: QuantityPoint3G<V>): QuantityPoint2G<V> {
        return point2ByGeometry(point)
    }

    open fun <V : FloatingNumber<V>> point3(point: QuantityPoint2G<V>, distance: Quantity<V>): QuantityPoint3G<V> {
        return point3ByGeometry(point, distance)
    }

    open fun <V : FloatingNumber<V>> vector(distance: Quantity<V>): QuantityVector3G<V> {
        return vectorByGeometry(distance)
    }
}

typealias Direction = ProjectivePlane

/**
 * ZOX
 */
object Bottom : ProjectivePlane() {
    override fun <V : FloatingNumber<V>> length(unit: AbstractCuboid<V>, orientation: Orientation) = orientation.depth(unit)
    override fun <V : FloatingNumber<V>> width(unit: AbstractCuboid<V>, orientation: Orientation) = orientation.width(unit)
    override fun <V : FloatingNumber<V>> height(unit: AbstractCuboid<V>, orientation: Orientation) = orientation.height(unit)

    override fun length(space: AbstractContainer3Shape) = length(space.asGenericContainer3Shape())
    override fun width(space: AbstractContainer3Shape) = width(space.asGenericContainer3Shape())
    override fun height(space: AbstractContainer3Shape) = height(space.asGenericContainer3Shape())

    override fun distance(point: QuantityPoint3) = distanceByGeometry(point)
    override fun point2(point: QuantityPoint3) = point2ByGeometry(point)
    override fun point3(point: QuantityPoint2, distance: Quantity<InfraNumber>) = point3ByGeometry(point, distance)
    override fun vector(distance: Quantity<InfraNumber>) = vectorByGeometry(distance)

    override fun toString(): String {
        return "Bottom-ZOX"
    }
}

typealias ZOX = Bottom

/**
 * XOY
 */
object Side : ProjectivePlane() {
    override fun <V : FloatingNumber<V>> length(unit: AbstractCuboid<V>, orientation: Orientation) = orientation.width(unit)
    override fun <V : FloatingNumber<V>> width(unit: AbstractCuboid<V>, orientation: Orientation) = orientation.height(unit)
    override fun <V : FloatingNumber<V>> height(unit: AbstractCuboid<V>, orientation: Orientation) = orientation.depth(unit)

    override fun length(space: AbstractContainer3Shape) = length(space.asGenericContainer3Shape())
    override fun width(space: AbstractContainer3Shape) = width(space.asGenericContainer3Shape())
    override fun height(space: AbstractContainer3Shape) = height(space.asGenericContainer3Shape())

    override fun distance(point: QuantityPoint3) = distanceByGeometry(point)
    override fun point2(point: QuantityPoint3) = point2ByGeometry(point)
    override fun point3(point: QuantityPoint2, distance: Quantity<InfraNumber>) = point3ByGeometry(point, distance)
    override fun vector(distance: Quantity<InfraNumber>) = vectorByGeometry(distance)

    override fun toString(): String {
        return "Side-XOY"
    }
}

typealias XOY = Side

/**
 * ZOY
 */
object Front : ProjectivePlane() {
    override fun <V : FloatingNumber<V>> length(unit: AbstractCuboid<V>, orientation: Orientation) = orientation.depth(unit)
    override fun <V : FloatingNumber<V>> width(unit: AbstractCuboid<V>, orientation: Orientation) = orientation.height(unit)
    override fun <V : FloatingNumber<V>> height(unit: AbstractCuboid<V>, orientation: Orientation) = orientation.width(unit)

    override fun length(space: AbstractContainer3Shape) = length(space.asGenericContainer3Shape())
    override fun width(space: AbstractContainer3Shape) = width(space.asGenericContainer3Shape())
    override fun height(space: AbstractContainer3Shape) = height(space.asGenericContainer3Shape())

    override fun distance(point: QuantityPoint3) = distanceByGeometry(point)
    override fun point2(point: QuantityPoint3) = point2ByGeometry(point)
    override fun point3(point: QuantityPoint2, distance: Quantity<InfraNumber>) = point3ByGeometry(point, distance)
    override fun vector(distance: Quantity<InfraNumber>) = vectorByGeometry(distance)

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
    val length: Quantity<InfraNumber> get() = asGenericProjection().length
    val width: Quantity<InfraNumber> get() = asGenericProjection().width
    val height: Quantity<InfraNumber> get() = asGenericProjection().height
    val area: Quantity<InfraNumber> get() = asGenericProjection().area
    val weight: Quantity<InfraNumber> get() = asGenericProjection().weight

    fun amount(unit: AbstractCuboid<*>): UInt64
    fun toPlacement3At(position: QuantityPoint2): List<QuantityPlacement3<T>>
}

data class PlaneProjection<
        T : Cuboid<T>,
        P : ProjectivePlane
        >(
    override val view: CuboidView<T>,
    override val plane: P
) : Projection<T, P> {
    private val genericProjection: GenericPlaneProjection<LegacyCuboidGenericAdapter<T>, InfraNumber, P> by lazy {
        GenericPlaneProjection(
            view = unit.asGenericCuboid().view(orientation),
            plane = plane
        )
    }

    override fun amount(unit: AbstractCuboid<*>): UInt64 {
        return if (unit == this.unit) {
            UInt64.one
        } else {
            UInt64.zero
        }
    }

    override fun toPlacement3At(position: QuantityPoint2): List<QuantityPlacement3<T>> {
        return genericProjection.toPlacement3At(
            position = QuantityPoint2G(
                x = position.x,
                y = position.y
            )
        ).map { it.toLegacyPlacement3() }
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
    private val genericProjection: GenericPileProjection<LegacyCuboidGenericAdapter<T>, InfraNumber, P> by lazy {
        GenericPileProjection(
            view = unit.asGenericCuboid().view(orientation),
            plane = plane,
            layer = layer
        )
    }

    override val height: Quantity<InfraNumber>
        get() = genericProjection.height
    override val weight: Quantity<InfraNumber>
        get() = genericProjection.weight

    constructor(plane: PlaneProjection<T, P>, layer: UInt64 = UInt64.one) : this(plane.view, plane.plane, layer)

    override fun amount(unit: AbstractCuboid<*>): UInt64 {
        return if (unit == this.unit) {
            layer
        } else {
            UInt64.zero
        }
    }

    override fun toPlacement3At(position: QuantityPoint2): List<QuantityPlacement3<T>> {
        return genericProjection.toPlacement3At(
            position = QuantityPoint2G(
                x = position.x,
                y = position.y
            )
        ).map { it.toLegacyPlacement3() }
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

    private val genericProjection: GenericMultiPileProjection<LegacyCuboidGenericAdapter<T>, InfraNumber, P> by lazy {
        GenericMultiPileProjection(
            views = views.map { it.unit.asGenericCuboid().view(it.orientation) },
            plane = plane
        )
    }

    override val length: Quantity<InfraNumber>
        get() = genericProjection.length
    override val width: Quantity<InfraNumber>
        get() = genericProjection.width
    override val height: Quantity<InfraNumber>
        get() = genericProjection.height
    override val weight: Quantity<InfraNumber>
        get() = genericProjection.weight

    override fun amount(unit: AbstractCuboid<*>) = UInt64(views.count { it.unit == unit })

    override fun toPlacement3At(position: QuantityPoint2): List<QuantityPlacement3<T>> {
        return genericProjection.toPlacement3At(
            position = QuantityPoint2G(
                x = position.x,
                y = position.y
            )
        ).map { it.toLegacyPlacement3() }
    }

    override fun copy() = MultiPileProjection(views.map { it.copy() }, plane)
}
