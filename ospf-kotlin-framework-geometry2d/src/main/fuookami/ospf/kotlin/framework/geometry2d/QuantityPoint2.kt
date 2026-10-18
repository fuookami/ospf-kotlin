/**
 * 二维量纲点与向量。 / 2D quantity point and vector.
 *
 * `ospf-kotlin-quantities` 的几何层只提供矩形、圆与包围盒，没有点位类型；二维装箱的落点计算需要点位与向量，
 * 因此在基础设施层补充。 / The quantity geometry layer only ships rectangles, circles and boxes; 2D packing
 * placement needs points and vectors, so they are provided here.
*/
package fuookami.ospf.kotlin.framework.geometry2d

import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity

/**
 * 二维量纲向量。 / 2D quantity vector.
 *
 * @param V 数值类型 / numeric type
 * @property x x 分量 / x component
 * @property y y 分量 / y component
*/
data class QuantityVector2<V : FloatingNumber<V>>(
    val x: Quantity<V>,
    val y: Quantity<V>
) {

    /**
     * 向量加法。 / Vector addition.
     *
     * @param rhs 右操作数 / right-hand operand
     * @return 加法结果 / addition result
    */
    operator fun plus(rhs: QuantityVector2<V>): QuantityVector2<V> {
        return QuantityVector2(
            x = x + rhs.x,
            y = y + rhs.y
        )
    }

    /**
     * 向量减法。 / Vector subtraction.
     *
     * @param rhs 右操作数 / right-hand operand
     * @return 减法结果 / subtraction result
    */
    operator fun minus(rhs: QuantityVector2<V>): QuantityVector2<V> {
        return QuantityVector2(
            x = x - rhs.x,
            y = y - rhs.y
        )
    }
}

/**
 * 二维量纲点。 / 2D quantity point.
 *
 * @param V 数值类型 / numeric type
 * @property x x 坐标 / x coordinate
 * @property y y 坐标 / y coordinate
*/
data class QuantityPoint2<V : FloatingNumber<V>>(
    val x: Quantity<V>,
    val y: Quantity<V>
) {

    /**
     * 加上向量偏移。 / Add a vector offset.
     *
     * @param offset 偏移向量 / offset vector
     * @return 新点位 / the new point
    */
    operator fun plus(offset: QuantityVector2<V>): QuantityPoint2<V> {
        return QuantityPoint2(
            x = x + offset.x,
            y = y + offset.y
        )
    }

    /**
     * 减去向量偏移。 / Subtract a vector offset.
     *
     * @param offset 偏移向量 / offset vector
     * @return 新点位 / the new point
    */
    operator fun minus(offset: QuantityVector2<V>): QuantityPoint2<V> {
        return QuantityPoint2(
            x = x - offset.x,
            y = y - offset.y
        )
    }

    /**
     * 比较两点排序：先比较 y，再比较 x；不可比较时返回 null。 / Compare two points by y then x, returning null when incomparable.
     *
     * @param rhs 右侧点位 / right-hand point
     * @return 比较结果或 null / comparison order, or null
    */
    fun ordOrNull(rhs: QuantityPoint2<V>): Order? {
        when (val yOrder = quantityOrdOrNull(y, rhs.y) ?: return null) {
            Order.Equal -> {}
            else -> return yOrder
        }
        return quantityOrdOrNull(x, rhs.x)
    }
}

/**
 * 构造二维量纲点。 / Build a 2D quantity point.
 *
 * @param V 数值类型 / numeric type
 * @param x x 坐标 / x coordinate
 * @param y y 坐标 / y coordinate
 * @return 二维量纲点 / 2D quantity point
*/
fun <V : FloatingNumber<V>> point2Of(x: Quantity<V>, y: Quantity<V>): QuantityPoint2<V> {
    return QuantityPoint2(x = x, y = y)
}

/**
 * 构造二维量纲向量。 / Build a 2D quantity vector.
 *
 * @param V 数值类型 / numeric type
 * @param x x 分量 / x component
 * @param y y 分量 / y component
 * @return 二维量纲向量 / 2D quantity vector
*/
fun <V : FloatingNumber<V>> vector2Of(x: Quantity<V>, y: Quantity<V>): QuantityVector2<V> {
    return QuantityVector2(x = x, y = y)
}
