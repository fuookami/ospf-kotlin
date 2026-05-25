@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.bpp3d.infrastructure.api

import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Orientation
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.QuantityPoint2G
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.QuantityPoint3G
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity

interface Cuboid<T : Cuboid<T, V>, V : FloatingNumber<V>> {
    val self: T
    val width: Quantity<V>
    val height: Quantity<V>
    val depth: Quantity<V>
    val weight: Quantity<V>
    val volume: Quantity<V>
    val actualVolume: Quantity<V>
    val linearDensity: Quantity<V>
    val enabledOrientations: List<Orientation>

    fun view(orientation: Orientation = Orientation.Upright): CuboidView<T, V>? {
        return if (enabledOrientations.contains(orientation)) {
            CuboidView(self, orientation)
        } else {
            null
        }
    }
}

data class BottomSupport<V : FloatingNumber<V>>(
    val area: Quantity<V>,
    val weight: Quantity<V>
)

open class CuboidView<T : Cuboid<T, V>, V : FloatingNumber<V>>(
    val unit: T,
    val orientation: Orientation = Orientation.Upright
) {
    open val width by unit::width
    open val height by unit::height
    open val depth by unit::depth
    open val weight by unit::weight
}

interface Projection<T : Cuboid<T, V>, V : FloatingNumber<V>> {
    val view: CuboidView<T, V>
    val unit: T get() = view.unit
    val orientation: Orientation get() = view.orientation
    val length: Quantity<V>
    val width: Quantity<V>
    val height: Quantity<V>
    val area: Quantity<V>
    val weight: Quantity<V>

    fun amount(unit: Cuboid<*, V>): UInt64
}

data class PlaneProjection<T : Cuboid<T, V>, V : FloatingNumber<V>>(
    override val view: CuboidView<T, V>,
    override val length: Quantity<V>,
    override val width: Quantity<V>,
    override val height: Quantity<V>,
    override val area: Quantity<V>,
    override val weight: Quantity<V>,
) : Projection<T, V> {
    override fun amount(unit: Cuboid<*, V>): UInt64 {
        return if (unit == this.unit) {
            UInt64.one
        } else {
            UInt64.zero
        }
    }
}

data class PileProjection<T : Cuboid<T, V>, V : FloatingNumber<V>>(
    override val view: CuboidView<T, V>,
    val layer: UInt64,
    override val length: Quantity<V>,
    override val width: Quantity<V>,
    override val height: Quantity<V>,
    override val area: Quantity<V>,
    override val weight: Quantity<V>,
) : Projection<T, V> {
    override fun amount(unit: Cuboid<*, V>): UInt64 {
        return if (unit == this.unit) {
            layer
        } else {
            UInt64.zero
        }
    }
}

data class MultiPileProjection<T : Cuboid<T, V>, V : FloatingNumber<V>>(
    override val view: CuboidView<T, V>,
    val views: List<CuboidView<T, V>>,
    override val length: Quantity<V>,
    override val width: Quantity<V>,
    override val height: Quantity<V>,
    override val area: Quantity<V>,
    override val weight: Quantity<V>,
) : Projection<T, V> {
    override fun amount(unit: Cuboid<*, V>) = UInt64(views.count { it.unit == unit })
}

data class QuantityPlacement2<T : Cuboid<T, V>, V : FloatingNumber<V>>(
    val projection: Projection<T, V>,
    val position: QuantityPoint2G<V>,
    val maxX: Quantity<V>,
    val maxY: Quantity<V>
) {
    val unit by projection::unit
    val orientation by projection::orientation
    val view by projection::view
    val weight by projection::weight

    val x by position::x
    val y by position::y

    val length by projection::length
    val width by projection::width
}

data class QuantityPlacement3<T : Cuboid<T, V>, V : FloatingNumber<V>>(
    val view: CuboidView<T, V>,
    val position: QuantityPoint3G<V>,
    val maxX: Quantity<V>,
    val maxY: Quantity<V>,
    val maxZ: Quantity<V>
) {
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
}

data class Container2<V : FloatingNumber<V>>(
    val length: Quantity<V>,
    val width: Quantity<V>,
    val units: List<QuantityPlacement2<*, V>>,
    val amounts: Map<Cuboid<*, V>, UInt64>
)

data class Container3<V : FloatingNumber<V>>(
    val width: Quantity<V>,
    val height: Quantity<V>,
    val depth: Quantity<V>,
    val units: List<QuantityPlacement3<*, V>>,
    val volume: Quantity<V>,
    val actualVolume: Quantity<V>,
    val weight: Quantity<V>,
    val linearDensity: Quantity<V>,
    val amounts: Map<Cuboid<*, V>, UInt64>
)
