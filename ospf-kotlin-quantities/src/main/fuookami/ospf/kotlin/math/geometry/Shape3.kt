package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber

interface QuantityShape3<V : FloatingNumber<V>> {
    val boundingCuboid: QuantityCuboid3<V>
}

