/**
 * 二维轴置换
 * 2D axis permutation
 *
 * 定义二维坐标轴的置换操作，支持对矩形和包围盒进行轴交换。
 * Defines 2D axis permutation operations, supporting axis swapping for rectangles and bounding boxes.
 */
package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber

/**
 * 二维轴置换为纯几何概念；仅 apply(...) 与数量模型绑定。
 * 2D axis permutation is pure geometry; only apply(...) binds to quantity models.
 *
 * @property widthAxis 宽度对应的轴 / Axis corresponding to width
 * @property heightAxis 高度对应的轴 / Axis corresponding to height
 */
data class QuantityAxisPermutation2(
    val widthAxis: Axis2,
    val heightAxis: Axis2
) {
    companion object {
        /** X-Y 置换 / X-Y permutation */
        val XY = QuantityAxisPermutation2(Axis2.X, Axis2.Y)
        /** Y-X 置换 / Y-X permutation */
        val YX = QuantityAxisPermutation2(Axis2.Y, Axis2.X)
    }

    /**
     * 对矩形应用轴置换
     * Apply axis permutation to a rectangle
     *
     * @param rectangle 待置换的矩形 / Rectangle to permute
     * @param V 数值类型 / Number type
     * @return 置换后的矩形 / Permuted rectangle
     */
    fun <V : FloatingNumber<V>> apply(rectangle: QuantityRectangle2<V>): QuantityRectangle2<V> {
        return QuantityRectangle2(
            width = rectangle.along(widthAxis),
            height = rectangle.along(heightAxis)
        )
    }

    /**
     * 对圆形应用轴置换（圆形无变化）
     * Apply axis permutation to a circle (no change for circles)
     *
     * @param circle 待置换的圆形 / Circle to permute
     * @param V 数值类型 / Number type
     * @return 原圆形（圆形置换不变）/ Original circle (unchanged)
     */
    fun <V : FloatingNumber<V>> apply(circle: QuantityCircle2<V>): QuantityCircle2<V> {
        return circle
    }
}
