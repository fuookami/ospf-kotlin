/**
 * 二维轴置换
 * 2D Axis Permutation
 *
 * 定义二维几何空间中的轴置换，为纯几何概念，支持宽高轴的交换与映射。
 * Defines axis permutation in 2D geometric space, a pure geometry concept supporting width/height axis swapping and mapping.
*/
package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber

/**
 * 二维轴置换为纯几何概念；仅 apply(...) 与数量模型绑定。
 * 2D axis permutation is pure geometry; only apply(...) binds to quantity models.
 *
 * @property widthAxis 宽度对应的轴 / The axis corresponding to width
 * @property heightAxis 高度对应的轴 / The axis corresponding to height
*/
data class AxisPermutation2(
    val widthAxis: Axis2,
    val heightAxis: Axis2
) {
    companion object {
        /** 宽度轴=X，高度轴=Y / Width axis=X, height axis=Y */
        val XY = AxisPermutation2(Axis2.X, Axis2.Y)

        /** 宽度轴=Y，高度轴=X / Width axis=Y, height axis=X */
        val YX = AxisPermutation2(Axis2.Y, Axis2.X)
    }

    /**
     * 按轴置换矩形的宽度和高度
     * Permute the width and height of a rectangle by axes
     *
     * @param V 数值类型 / The numeric type
     * @param rectangle 待置换的矩形 / The rectangle to permute
     * @return 置换后的矩形 / The permuted rectangle
    */
    fun <V : FloatingNumber<V>> apply(rectangle: Rectangle2<V>): Rectangle2<V> {
        return Rectangle2(
            width = rectangle.along(widthAxis),
            height = rectangle.along(heightAxis)
        )
    }

    /**
     * 对圆形应用轴置换（圆形无方向性，原样返回）
     * Apply axis permutation to a circle (circle is direction-agnostic, returns as-is)
     *
     * @param V 数值类型 / The numeric type
     * @param circle 待置换的圆形 / The circle to permute
     * @return 置换后的圆形（原样返回） / The permuted circle (returned as-is)
    */
    fun <V : FloatingNumber<V>> apply(circle: Circle2<V>): Circle2<V> {
        return circle
    }
}
