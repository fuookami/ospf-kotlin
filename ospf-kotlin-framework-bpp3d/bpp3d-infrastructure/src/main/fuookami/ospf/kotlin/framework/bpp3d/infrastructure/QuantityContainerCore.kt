@file:Suppress("DEPRECATION")
/**
 * 容器几何核心。
 * Container geometry core.
 */
package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.utils.functional.Eq
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.quantities.quantity.*

/**
 * 泛型 3D 容器形状接口。
 * Generic 3D container shape interface.
 *
 * @param V 数值类型 / numeric scalar type
 */
interface Container3Geometry<V : FloatingNumber<V>> : Eq<Container3Geometry<V>> {
    val width: Quantity<V>
    val height: Quantity<V>
    val depth: Quantity<V>
    val volume: Quantity<V>
        get() = quantityTimesByValue(quantityTimesByValue(width, height), depth)

    fun enabled(
        unit: AbstractCuboid<V>,
        orientation: Orientation = Orientation.Upright
    ): Boolean {
        return (width geq orientation.width(unit)) == true
                && (height geq orientation.height(unit)) == true
                && (depth geq orientation.depth(unit)) == true
    }

    fun enabled(unit: Placement3<*, V>): Boolean {
        return (width geq unit.maxX) == true
                && (height geq unit.maxY) == true
                && (depth geq unit.maxZ) == true
    }

    fun enabled(units: List<Placement3<*, V>>): Boolean {
        val maxX = maxQuantityByValue(units.map { it.maxX })
        if (maxX != null && (width geq maxX) != true) {
            return false
        }
        val maxY = maxQuantityByValue(units.map { it.maxY })
        if (maxY != null && (height geq maxY) != true) {
            return false
        }
        val maxZ = maxQuantityByValue(units.map { it.maxZ })
        if (maxZ != null && (depth geq maxZ) != true) {
            return false
        }
        return true
    }

    fun restSpace(offset: QuantityPoint3<V>): QuantityContainer3Shape<V> {
        return QuantityContainer3Shape(
            width = quantityMinusByValue(width, offset.x),
            height = quantityMinusByValue(height, offset.y),
            depth = quantityMinusByValue(depth, offset.z)
        )
    }

    override fun partialEq(rhs: Container3Geometry<V>): Boolean? {
        return width eq rhs.width && height eq rhs.height && depth eq rhs.depth
    }
}

/**
 * 泛型 3D 容器形状实现。
 * Generic 3D container shape implementation.
 *
 * @param V 数值类型 / numeric scalar type
 * @property width 宽度 / width
 * @property height 高度 / height
 * @property depth 深度 / depth
 */
data class QuantityContainer3Shape<V : FloatingNumber<V>>(
    override val width: Quantity<V>,
    override val height: Quantity<V>,
    override val depth: Quantity<V>
) : Container3Geometry<V>

/**
 * 泛型 2D 容器形状接口。
 * Generic 2D container shape interface.
 *
 * @param P 投影平面 / projective plane
 * @param V 数值类型 / numeric scalar type
 */
interface Container2Geometry<P : ProjectivePlane, V : FloatingNumber<V>> {
    val length: Quantity<V>
    val width: Quantity<V>
    val plane: P

    fun restSpace(offset: QuantityPoint2<V>): QuantityContainer2Shape<P, V> {
        return QuantityContainer2Shape(
            length = quantityMinusByValue(length, offset.x),
            width = quantityMinusByValue(width, offset.y),
            plane = plane
        )
    }

    fun restSpace(offset: QuantityVector2<V>): QuantityContainer2Shape<P, V> {
        return QuantityContainer2Shape(
            length = quantityMinusByValue(length, offset.x),
            width = quantityMinusByValue(width, offset.y),
            plane = plane
        )
    }
}

/**
 * 泛型 2D 容器形状实现。
 * Generic 2D container shape implementation.
 *
 * @param P 投影平面 / projective plane
 * @param V 数值类型 / numeric scalar type
 * @property length 长度 / length
 * @property width 宽度 / width
 * @property plane 投影平面 / projective plane
 */
data class QuantityContainer2Shape<P : ProjectivePlane, V : FloatingNumber<V>>(
    override val length: Quantity<V>,
    override val width: Quantity<V>,
    override val plane: P
) : Container2Geometry<P, V>

private fun <V : FloatingNumber<V>> maxQuantityByValue(values: Iterable<Quantity<V>>): Quantity<V>? {
    var maximum: Quantity<V>? = null
    for (value in values) {
        maximum = if (maximum == null || quantityOrd(value, maximum, "max") is fuookami.ospf.kotlin.utils.functional.Order.Greater) {
            value
        } else {
            maximum
        }
    }
    return maximum
}
