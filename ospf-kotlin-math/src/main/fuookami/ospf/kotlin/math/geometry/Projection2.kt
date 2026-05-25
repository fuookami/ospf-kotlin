package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber

sealed interface Projection2<V : FloatingNumber<V>>
typealias Shape2<V> = Projection2<V>

data class Circle2<V : FloatingNumber<V>>(
    val radius: V
) : Projection2<V> {
    val diameter: V get() = quantityPlus(radius, radius)

    fun area(pi: V): V = (radius * radius) * pi

    fun boundingBoxAtOrigin(): Box2<V> = Box2.atOrigin(this)
}

data class Rectangle2<V : FloatingNumber<V>>(
    val width: V,
    val height: V
) : Projection2<V> {
    val area: V get() = width * height

    fun along(axis: Axis2): V {
        return when (axis) {
            Axis2.X -> width
            Axis2.Y -> height
        }
    }

    fun permute(permutation: AxisPermutation2): Rectangle2<V> {
        return permutation.apply(this)
    }

    fun atOrigin(): Box2<V> = Box2.atOrigin(this)
}

