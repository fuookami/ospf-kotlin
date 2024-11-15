package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.geometry.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.operator.*

interface AbstractCuboid {
    val width: Flt64
    val height: Flt64
    val depth: Flt64

    val weight: Flt64
    val volume: Flt64 get() = depth * height * width
    val actualVolume: Flt64 get() = volume
    val linearDensity: Flt64 get() = weight / depth
}

interface Cuboid<T : Cuboid<T>> : AbstractCuboid {
    val enabledOrientations: List<Orientation>

    fun enabledOrientationsAt(
        space: AbstractContainer2Shape<*>,
        withRotation: Boolean = true
    ): List<Orientation> {
        return enabledOrientations.filter {
            space.length geq space.plane.length(this, it)
                    && space.width geq space.plane.width(this, it)
                    && (withRotation || !it.rotated)
        }
    }

    fun enabledOrientationsAt(
        space: AbstractContainer3Shape,
        withRotation: Boolean = true
    ): List<Orientation> {
        return enabledOrientations.filter {
            space.width geq it.width(this)
                    && space.height geq it.height(this)
                    && space.depth geq it.depth(this)
                    && (withRotation || !it.rotated)
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun view(orientation: Orientation = Orientation.Upright): CuboidView<T>? {
        return CuboidView(this as T, orientation)
    }
}

data class BottomSupport(
    val area: Flt64,
    val weight: Flt64
) : Plus<BottomSupport, BottomSupport> {
    override fun plus(rhs: BottomSupport) = BottomSupport(
        area = area + rhs.area,
        weight = weight + rhs.weight
    )
}

open class CuboidView<T : Cuboid<T>>(
    val unit: T,
    val orientation: Orientation = Orientation.Upright
) : AbstractCuboid, Copyable<CuboidView<T>> {
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
        val placement = Placement2(PlaneProjection(this, Bottom), point2())
        val bottomPlacement = Placement2(PlaneProjection(bottomView, Bottom), point2())
        val intersect = placement.intersect(bottomPlacement)
        return if (intersect == null) {
            BottomSupport(
                area = Flt64.zero,
                weight = Flt64.zero
            )
        } else {
            BottomSupport(
                area = intersect.area,
                weight = intersect.area / bottomPlacement.projection.area * bottomPlacement.weight
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
        area = Flt64.zero,
        weight = Flt64.zero
    )

    val bottomPlacement = Placement2(unit, Bottom)
    for (fixedPlacement in bottomUnits) {
        if (fixedPlacement.maxY eq unit.y) {
            val thisBottomPlacement = Placement2(fixedPlacement, Bottom)
            val intersect = bottomPlacement.intersect(thisBottomPlacement)
            if (intersect != null) {
                val thisSupport = BottomSupport(
                    area = intersect.area,
                    weight = intersect.area / thisBottomPlacement.projection.area * thisBottomPlacement.weight
                )
                support += thisSupport
            }
        }
    }
    return support
}
