@file:Suppress("DEPRECATION")
/**
 * 立方体基础设施。
 * Cuboid infrastructure.
 */
package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.operator.Plus
import fuookami.ospf.kotlin.quantities.quantity.*

interface AbstractCuboid<V : FloatingNumber<V>> {
    val width: Quantity<V>
    val height: Quantity<V>
    val depth: Quantity<V>

    val weight: Quantity<V>
    val volume: Quantity<V> get() = depth * height * width
    val actualVolume: Quantity<V> get() = volume
    val linearDensity: Quantity<V> get() = weight / depth
}

interface Cuboid<T : Cuboid<T, V>, V : FloatingNumber<V>> : AbstractCuboid<V> {
    val self: T
    val enabledOrientations: List<Orientation>

    fun geometryView(orientation: Orientation = Orientation.Upright): QuantityCuboid3View<V> {
        return QuantityCuboid3View(
            origin = QuantityCuboid3(
                width = width,
                height = height,
                depth = depth
            ),
            permutation = orientation.toAxisPermutation3()
        )
    }

    fun geometry(orientation: Orientation = Orientation.Upright): QuantityCuboid3<V> {
        return geometryView(orientation).cuboid
    }

    fun enabledOrientationsAt(
        space: Container2Geometry<*, V>,
        withRotation: Boolean = true
    ): List<Orientation> {
        return enabledOrientations.filter {
            (space.length geq space.plane.length(this, it)) == true
                    && (space.width geq space.plane.width(this, it)) == true
                    && (withRotation || !it.rotated)
        }
    }

    fun enabledOrientationsAt(
        space: Container3Geometry<V>,
        withRotation: Boolean = true
    ): List<Orientation> {
        return enabledOrientations.filter {
            (space.width geq it.width(this)) == true
                    && (space.height geq it.height(this)) == true
                    && (space.depth geq it.depth(this)) == true
                    && (withRotation || !it.rotated)
        }
    }

    fun view(orientation: Orientation = Orientation.Upright): CuboidView<T, V>? {
        return CuboidView(self, orientation)
    }
}

data class BottomSupport(
    val area: Quantity<FltX>,
    val weight: Quantity<FltX>
) : Plus<BottomSupport, BottomSupport> {
    override fun plus(rhs: BottomSupport) = BottomSupport(
        area = area + rhs.area,
        weight = weight + rhs.weight
    )
}

open class CuboidView<T : Cuboid<T, V>, V : FloatingNumber<V>>(
    val unit: T,
    val orientation: Orientation = Orientation.Upright
) : AbstractCuboid<V>, Copyable<CuboidView<T, V>> {
    private val geometryView: QuantityCuboid3View<V> by lazy { unit.geometryView(orientation) }

    override val width get() = geometryView.width
    override val height get() = geometryView.height
    override val depth get() = geometryView.depth
    override val weight by unit::weight

    val rotatedOrientation by orientation::rotation

    open val rotation: CuboidView<T, V>?
        get() {
            return if (unit.enabledOrientations.contains(rotatedOrientation)) {
                unit.view(rotatedOrientation)
            } else {
                null
            }
        }

    open fun rotationAt(space: Container2Geometry<*, V>): CuboidView<T, V>? {
        return if (unit.enabledOrientationsAt(space).contains(rotatedOrientation)) {
            unit.view(rotatedOrientation)
        } else {
            null
        }
    }

    open fun rotationAt(space: Container3Geometry<V>): CuboidView<T, V>? {
        return if (unit.enabledOrientationsAt(space).contains(rotatedOrientation)) {
            unit.view(rotatedOrientation)
        } else {
            null
        }
    }

    override fun copy() = CuboidView(
        unit = unit,
        orientation = orientation
    )

    fun toGeometryCuboid3View(): QuantityCuboid3View<V> = geometryView

    fun toGeometryCuboid3(): QuantityCuboid3<V> = geometryView.cuboid

    fun toGeometryBox3AtOrigin(): QuantityBox3<V> = QuantityBox3.atOrigin(geometryView.cuboid)

    override fun hashCode(): Int {
        var result = unit.hashCode()
        result = 31 * result + orientation.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CuboidView<*, *>) return false

        if (unit != other.unit) return false
        if (orientation != other.orientation) return false

        return true
    }

    override fun toString() = "$unit $orientation"
}

fun CuboidView<*, FltX>.bottomSupport(bottomView: CuboidView<*, FltX>): BottomSupport {
    val shapePlacement = ShapePlacement3(
        shape = this.asPackingShape3(),
        position = point3FltX()
    )
    val bottomShapePlacement = ShapePlacement3(
        shape = bottomView.asPackingShape3(),
        position = point3FltX()
    )
    val supportArea = shapePlacement.footprintOverlapArea(bottomShapePlacement)
    val bottomArea = bottomShapePlacement.footprintOverlapArea(bottomShapePlacement)
    return if ((bottomArea eq (fltXZero() * bottomArea.unit)) == true) {
        BottomSupport(
            area = supportArea,
            weight = bottomView.weight * fltXZero()
        )
    } else {
        BottomSupport(
            area = supportArea,
            weight = (supportArea / bottomArea).value * bottomView.weight
        )
    }
}

fun bottomSupport(
    unit: QuantityPlacement3<*, FltX>,
    bottomUnits: List<QuantityPlacement3<*, FltX>>,
    shapeResolver: (QuantityPlacement3<*, FltX>) -> PackingShape3<FltX> = { placement ->
        placement.view.asPackingShape3()
    }
): BottomSupport {
    val unitShapePlacement = unit.asShapePlacement3(shapeResolver)
    var support = BottomSupport(
        area = unit.depth * unit.width * fltXZero(),
        weight = unit.weight * fltXZero()
    )

    for (fixedPlacement in bottomUnits) {
        if (fixedPlacement.maxY eq unit.y) {
            val bottomShapePlacement = fixedPlacement.asShapePlacement3(shapeResolver)
            val overlapArea = unitShapePlacement.footprintOverlapArea(bottomShapePlacement)
            if ((overlapArea gr (fltXZero() * overlapArea.unit)) == true) {
                val bottomArea = bottomShapePlacement.footprintOverlapArea(bottomShapePlacement)
                if ((bottomArea gr (fltXZero() * bottomArea.unit)) == true) {
                    val thisSupport = BottomSupport(
                        area = overlapArea,
                        weight = (overlapArea / bottomArea).value * fixedPlacement.weight
                    )
                    support += thisSupport
                }
            }
        }
    }
    return support
}
