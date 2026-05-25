package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber

interface Shape3<V : FloatingNumber<V>> {
    val boundingCuboid: Cuboid3<V>
}

