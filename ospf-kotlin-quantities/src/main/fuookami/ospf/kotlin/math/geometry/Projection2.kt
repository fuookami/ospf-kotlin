package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.times

sealed interface QuantityProjection2<V : FloatingNumber<V>>
typealias QuantityShape2<V> = QuantityProjection2<V>

data class QuantityCircle2<V : FloatingNumber<V>>(
    val radius: Quantity<V>
) : QuantityProjection2<V> {
    val diameter: Quantity<V> get() = quantityPlus(radius, radius)

    fun area(pi: V): Quantity<V> = (radius * radius) * pi

    fun boundingBoxAtOrigin(): QuantityBox2<V> = QuantityBox2.atOrigin(this)
}

data class QuantityRectangle2<V : FloatingNumber<V>>(
    val width: Quantity<V>,
    val height: Quantity<V>
) : QuantityProjection2<V> {
    val area: Quantity<V> get() = width * height

    fun along(axis: Axis2): Quantity<V> {
        return when (axis) {
            Axis2.X -> width
            Axis2.Y -> height
        }
    }

    fun permute(permutation: QuantityAxisPermutation2): QuantityRectangle2<V> {
        return permutation.apply(this)
    }

    fun atOrigin(): QuantityBox2<V> = QuantityBox2.atOrigin(this)
}

