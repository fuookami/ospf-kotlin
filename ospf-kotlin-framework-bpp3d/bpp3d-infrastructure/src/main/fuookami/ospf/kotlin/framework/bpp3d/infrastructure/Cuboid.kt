@file:Suppress("DEPRECATION")

/**
 * 立方体基础设施。
 * Cuboid infrastructure.
 */
package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.math.geometry.QuantityBox3
import fuookami.ospf.kotlin.math.geometry.QuantityCuboid3
import fuookami.ospf.kotlin.math.geometry.QuantityCuboid3View
import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.operator.Plus
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.div
import fuookami.ospf.kotlin.quantities.quantity.eq
import fuookami.ospf.kotlin.quantities.quantity.geq
import fuookami.ospf.kotlin.quantities.quantity.plus
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Meter

interface AbstractCuboid<V : FloatingNumber<V>> {
    val width: Quantity<V>
    val height: Quantity<V>
    val depth: Quantity<V>

    val weight: Quantity<V>
    val volume: Quantity<V> get() = depth * height * width
    val actualVolume: Quantity<V> get() = volume
    val linearDensity: Quantity<V> get() = weight / depth
}

interface Cuboid<T : Cuboid<T>> : AbstractCuboid<InfraNumber> {
    val self: T
    val enabledOrientations: List<Orientation>

    fun geometryView(orientation: Orientation = Orientation.Upright): QuantityCuboid3View<InfraNumber> {
        return self.asGenericCuboid().geometryView(orientation)
    }

    fun geometry(orientation: Orientation = Orientation.Upright): QuantityCuboid3<InfraNumber> {
        return self.asGenericCuboid().geometry(orientation)
    }

    fun enabledOrientationsAt(
        space: AbstractContainer2Shape<*>,
        withRotation: Boolean = true
    ): List<Orientation> {
        return enabledOrientations.filter {
            (space.length geq space.plane.length(this, it)) == true
                    && (space.width geq space.plane.width(this, it)) == true
                    && (withRotation || !it.rotated)
        }
    }

    fun enabledOrientationsAt(
        space: AbstractContainer3Shape,
        withRotation: Boolean = true
    ): List<Orientation> {
        return self.asGenericCuboid().enabledOrientationsAt(space.asGenericContainer3Shape(), withRotation)
    }

    fun view(orientation: Orientation = Orientation.Upright): CuboidView<T>? {
        return CuboidView(self, orientation)
    }
}

data class BottomSupport(
    val area: Quantity<InfraNumber>,
    val weight: Quantity<InfraNumber>
) : Plus<BottomSupport, BottomSupport> {
    override fun plus(rhs: BottomSupport) = BottomSupport(
        area = area + rhs.area,
        weight = weight + rhs.weight
    )
}

open class CuboidView<T : Cuboid<T>>(
    val unit: T,
    val orientation: Orientation = Orientation.Upright
) : AbstractCuboid<InfraNumber>, Copyable<CuboidView<T>> {
    private val geometryView: QuantityCuboid3View<InfraNumber> by lazy { unit.geometryView(orientation) }

    override val width get() = geometryView.width
    override val height get() = geometryView.height
    override val depth get() = geometryView.depth
    override val weight by unit::weight

    val rotatedOrientation by orientation::rotation

    open val rotation: CuboidView<T>?
        get() {
            return if (unit.enabledOrientations.contains(rotatedOrientation)) {
                unit.view(rotatedOrientation)
            } else {
                null
            }
        }

    open fun rotationAt(space: AbstractContainer2Shape<*>): CuboidView<T>? {
        return if (unit.enabledOrientationsAt(space).contains(rotatedOrientation)) {
            unit.view(rotatedOrientation)
        } else {
            null
        }
    }

    open fun rotationAt(space: AbstractContainer3Shape): CuboidView<T>? {
        return if (unit.enabledOrientationsAt(space).contains(rotatedOrientation)) {
            unit.view(rotatedOrientation)
        } else {
            null
        }
    }

    fun bottomSupport(bottomView: CuboidView<*>): BottomSupport {
        val placement = QuantityPlacement2(
            projection = PlaneProjection(this, Bottom),
            position = QuantityPoint2(infraZero() * Meter, infraZero() * Meter)
        )
        val bottomPlacement = QuantityPlacement2(
            projection = PlaneProjection(bottomView, Bottom),
            position = QuantityPoint2(infraZero() * Meter, infraZero() * Meter)
        )
        val intersect = placement.intersect(bottomPlacement)
        return if (intersect == null) {
            BottomSupport(
                area = bottomPlacement.projection.area * infraZero(),
                weight = bottomPlacement.weight * infraZero()
            )
        } else {
            BottomSupport(
                area = intersect.area,
                weight = (intersect.area / bottomPlacement.projection.area).value * bottomPlacement.weight
            )
        }
    }

    override fun copy() = CuboidView(
        unit = unit,
        orientation = orientation
    )

    fun toGeometryCuboid3View(): QuantityCuboid3View<InfraNumber> = geometryView

    fun toGeometryCuboid3(): QuantityCuboid3<InfraNumber> = geometryView.cuboid

    fun toGeometryBox3AtOrigin(): QuantityBox3<InfraNumber> = QuantityBox3.atOrigin(geometryView.cuboid)

    override fun hashCode(): Int {
        var result = unit.hashCode()
        result = 31 * result + orientation.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CuboidView<*>) return false

        if (unit != other.unit) return false
        if (orientation != other.orientation) return false

        return true
    }

    override fun toString() = "$unit $orientation"
}

fun bottomSupport(
    unit: QuantityPlacement3<*>,
    bottomUnits: List<QuantityPlacement3<*>>
): BottomSupport {
    var support = BottomSupport(
        area = unit.depth * unit.width * infraZero(),
        weight = unit.weight * infraZero()
    )

    val bottomPlacement = QuantityPlacement2(unit, Bottom)
    for (fixedPlacement in bottomUnits) {
        if (fixedPlacement.maxY eq unit.y) {
            val thisBottomPlacement = QuantityPlacement2(fixedPlacement, Bottom)
            val intersect = bottomPlacement.intersect(thisBottomPlacement)
            if (intersect != null) {
                val thisSupport = BottomSupport(
                    area = intersect.area,
                    weight = (intersect.area / thisBottomPlacement.projection.area).value * thisBottomPlacement.weight
                )
                support += thisSupport
            }
        }
    }
    return support
}
