/**
 * 二维投影形状
 * 2D projection shapes
 *
 * 定义二维投影形状（圆形、矩形），支持面积计算和轴置换。
 * Defines 2D projection shapes (circle, rectangle), supporting area calculation and axis permutation.
 */
package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.quantities.quantity.*

/**
 * 二维投影形状接口
 * 2D projection shape interface
 *
 * 所有二维投影形状（圆形、矩形）的公共密封接口。
 * Common sealed interface for all 2D projection shapes (circle, rectangle).
 *
 * @param V 数值类型 / Number type
 */
sealed interface QuantityProjection2<V : FloatingNumber<V>>

/**
 * 二维形状类型别名
 * Type alias for 2D shape
 */
typealias QuantityShape2<V> = QuantityProjection2<V>

/**
 * 二维圆形投影
 * 2D circle projection
 *
 * @property radius 半径 / Radius
 * @param V 数值类型 / Number type
 */
data class QuantityCircle2<V : FloatingNumber<V>>(
    val radius: Quantity<V>
) : QuantityProjection2<V> {
    /** 直径 / Diameter */
    val diameter: Quantity<V> get() = Quantity(radius.value + radius.value, radius.unit)

    /**
     * 计算面积
     * Compute the area
     *
     * @param pi 圆周率值 / Pi value
     * @return 面积 / Area
     */
    fun area(pi: V): Quantity<V> = quantityProduct(quantityProduct(radius, radius), pi)

    /**
     * 在原点创建包围盒
     * Create a bounding box at the origin
     *
     * @return 原点处的包围盒 / Bounding box at the origin
     */
    fun boundingBoxAtOrigin(): QuantityBox2<V> = QuantityBox2.atOrigin(this)
}

/**
 * 二维矩形投影
 * 2D rectangle projection
 *
 * @property width 宽度 / Width
 * @property height 高度 / Height
 * @param V 数值类型 / Number type
 */
data class QuantityRectangle2<V : FloatingNumber<V>>(
    val width: Quantity<V>,
    val height: Quantity<V>
) : QuantityProjection2<V> {
    /** 面积 / Area */
    val area: Quantity<V> get() = quantityProduct(width, height)

    /**
     * 获取沿指定轴的尺寸
     * Get the dimension along a specified axis
     *
     * @param axis 目标轴 / Target axis
     * @return 沿该轴的尺寸 / Dimension along the axis
     */
    fun along(axis: Axis2): Quantity<V> {
        return when (axis) {
            Axis2.X -> width
            Axis2.Y -> height
        }
    }

    /**
     * 对矩形应用轴置换
     * Apply axis permutation to the rectangle
     *
     * @param permutation 轴置换 / Axis permutation
     * @return 置换后的矩形 / Permuted rectangle
     */
    fun permute(permutation: QuantityAxisPermutation2): QuantityRectangle2<V> {
        return permutation.apply(this)
    }

    /**
     * 在原点创建包围盒
     * Create a bounding box at the origin
     *
     * @return 原点处的包围盒 / Bounding box at the origin
     */
    fun atOrigin(): QuantityBox2<V> = QuantityBox2.atOrigin(this)
}
