package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber

data class Cuboid3View<V : FloatingNumber<V>>(
    val origin: Cuboid3<V>,
    val permutation: AxisPermutation3 = AxisPermutation3.XYZ
) {
    val cuboid: Cuboid3<V> get() = permutation.apply(origin)

    val width: V get() = cuboid.width
    val height: V get() = cuboid.height
    val depth: V get() = cuboid.depth
}

