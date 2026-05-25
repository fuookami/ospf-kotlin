package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity

data class QuantityCuboid3View<V : FloatingNumber<V>>(
    val origin: QuantityCuboid3<V>,
    val permutation: QuantityAxisPermutation3 = QuantityAxisPermutation3.XYZ
) {
    val cuboid: QuantityCuboid3<V> get() = permutation.apply(origin)

    val width: Quantity<V> get() = cuboid.width
    val height: Quantity<V> get() = cuboid.height
    val depth: Quantity<V> get() = cuboid.depth
}

