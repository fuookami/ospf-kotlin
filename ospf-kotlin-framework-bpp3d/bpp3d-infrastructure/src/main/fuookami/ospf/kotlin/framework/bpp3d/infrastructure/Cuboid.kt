@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
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

interface Cuboid<T : Cuboid<T>> : AbstractCuboid<Flt64> {
    val enabledOrientations: List<Orientation>

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
        return enabledOrientations.filter {
            (space.width geq it.width(this)) == true
                    && (space.height geq it.height(this)) == true
                    && (space.depth geq it.depth(this)) == true
                    && (withRotation || !it.rotated)
        }
    }
    fun view(orientation: Orientation = Orientation.Upright): CuboidView<T>? {
        return CuboidView(this as T, orientation)
    }
}

data class BottomSupport(
    val area: Quantity<Flt64>,
    val weight: Quantity<Flt64>
) : Plus<BottomSupport, BottomSupport> {
    override fun plus(rhs: BottomSupport) = BottomSupport(
        area = area + rhs.area,
        weight = weight + rhs.weight
    )
}

open class CuboidView<T : Cuboid<T>>(
    val unit: T,
    val orientation: Orientation = Orientation.Upright
) : AbstractCuboid<Flt64>, Copyable<CuboidView<T>> {
    // inherited from Cuboid<T>
    override val width = orientation.width(unit)
    override val height = orientation.height(unit)
    override val depth = orientation.depth(unit)
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
        val placement = Placement2(
            projection = PlaneProjection(this, Bottom),
            position = QuantityPoint2(Flt64.zero * Meter, Flt64.zero * Meter)
        )
        val bottomPlacement = Placement2(
            projection = PlaneProjection(bottomView, Bottom),
            position = QuantityPoint2(Flt64.zero * Meter, Flt64.zero * Meter)
        )
        val intersect = placement.intersect(bottomPlacement)
        return if (intersect == null) {
            BottomSupport(
                area = bottomPlacement.projection.area * Flt64.zero,
                weight = bottomPlacement.weight * Flt64.zero
            )
        } else {
            BottomSupport(
                area = intersect.area,
                weight = (intersect.area / bottomPlacement.projection.area).asScalarF64() * bottomPlacement.weight
            )
        }
    }

    override fun copy() = CuboidView(
        unit = unit,
        orientation = orientation
    )

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
    unit: Placement3<*>,
    bottomUnits: List<Placement3<*>>
): BottomSupport {
    var support = BottomSupport(
        area = unit.depth * unit.width * Flt64.zero,
        weight = unit.weight * Flt64.zero
    )

    val bottomPlacement = Placement2(unit, Bottom)
    for (fixedPlacement in bottomUnits) {
        if (fixedPlacement.maxY eq unit.y) {
            val thisBottomPlacement = Placement2(fixedPlacement, Bottom)
            val intersect = bottomPlacement.intersect(thisBottomPlacement)
            if (intersect != null) {
                val thisSupport = BottomSupport(
                    area = intersect.area,
                    weight = (intersect.area / thisBottomPlacement.projection.area).asScalarF64() * thisBottomPlacement.weight
                )
                support += thisSupport
            }
        }
    }
    return support
}



