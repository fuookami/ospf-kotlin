package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber

/**
 * 二维轴置换为纯几何概念；仅 apply(...) 与数量模型绑定。
 * 2D axis permutation is pure geometry; only apply(...) binds to quantity models.
 */
data class QuantityAxisPermutation2(
    val widthAxis: Axis2,
    val heightAxis: Axis2
) {
    companion object {
        val XY = QuantityAxisPermutation2(Axis2.X, Axis2.Y)
        val YX = QuantityAxisPermutation2(Axis2.Y, Axis2.X)
    }

    fun <V : FloatingNumber<V>> apply(rectangle: QuantityRectangle2<V>): QuantityRectangle2<V> {
        return QuantityRectangle2(
            width = rectangle.along(widthAxis),
            height = rectangle.along(heightAxis)
        )
    }

    fun <V : FloatingNumber<V>> apply(circle: QuantityCircle2<V>): QuantityCircle2<V> {
        return circle
    }
}

