/**
 * 二维投影形状
 * 2D Projection Shape
 *
 * 定义二维投影形状的密封接口，支持圆形和矩形等投影形状。
 * Defines sealed interface for 2D projection shapes, supporting circles, rectangles, and other projection shapes.
*/
package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber

/**
 * 二维投影形状的密封接口，支持圆形和矩形。
 * Sealed interface for 2D projection shapes, supporting circles and rectangles.
 *
 * @param V 数值类型 / The numeric type
*/
sealed interface Projection2<V : FloatingNumber<V>>

/**
 * 二维形状，等同于 Projection2。
 * 2D shape, equivalent to Projection2.
 *
 * @param V 数值类型 / The numeric type
 * @see Projection2
*/
typealias Shape2<V> = Projection2<V>

/**
 * 二维圆形投影，由半径定义。
 * 2D circle projection defined by radius.
 *
 * @param V 数值类型 / The numeric type
 * @property radius 半径 / The radius
*/
data class Circle2<V : FloatingNumber<V>>(
    val radius: V
) : Projection2<V> {

    /** 直径 / The diameter */
    val diameter: V get() = quantityPlus(radius, radius)

    /**
     * 计算面积
     * Compute the area
     *
     * @param pi 圆周率值 / The pi value
     * @return 面积 / The area
    */
    fun area(pi: V): V = (radius * radius) * pi

    /**
     * Create a bounding box at the origin.
     * 在原点处创建包围盒。
     *
     * @return the bounding box at the origin / 原点处的包围盒
    */
    fun boundingBoxAtOrigin(): Box2<V> = Box2.atOrigin(this)
}

/**
 * 二维矩形投影，由宽度和高度定义。
 * 2D rectangle projection defined by width and height.
 *
 * @param V 数值类型 / The numeric type
 * @property width 宽度 / The width
 * @property height 高度 / The height
*/
data class Rectangle2<V : FloatingNumber<V>>(
    val width: V,
    val height: V
) : Projection2<V> {

    /** 面积 / Area */
    val area: V get() = width * height

    /**
     * 沿指定轴的尺寸
     * Dimension along the specified axis
     *
     * @param axis 目标轴 / The target axis
     * @return 沿该轴的尺寸 / The dimension along the axis
    */
    fun along(axis: Axis2): V {
        return when (axis) {
            Axis2.X -> width
            Axis2.Y -> height
        }
    }

    /**
     * 按轴置换宽高
     * Permute width and height by axes
     *
     * @param permutation 轴置换方案 / The axis permutation
     * @return 置换后的矩形 / The permuted rectangle
    */
    fun permute(permutation: AxisPermutation2): Rectangle2<V> {
        return permutation.apply(this)
    }

    /** 在原点处创建包围盒 / Create a bounding box at the origin
     * @return 原点处的包围盒 / The bounding box at the origin */
    fun atOrigin(): Box2<V> = Box2.atOrigin(this)
}
