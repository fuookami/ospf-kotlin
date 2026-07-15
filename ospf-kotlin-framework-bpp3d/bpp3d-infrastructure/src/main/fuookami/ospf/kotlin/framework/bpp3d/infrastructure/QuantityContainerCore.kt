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

    /** Width of the container / 容器宽度 */
    val width: Quantity<V>

    /** Height of the container / 容器高度 */
    val height: Quantity<V>

    /** Depth of the container / 容器深度 */
    val depth: Quantity<V>

    /** Total volume computed as width * height * depth / 总体积，计算为宽度 * 高度 * 深度 */
    val volume: Quantity<V>
        get() = quantityTimesByValue(quantityTimesByValue(width, height), depth)

/**
 * enabled.
 * enabled。
 * @param unit cuboid to check fit for / 待检查适配的长方体
 * @param orientation placement orientation / 放置姿态
 * @return whether the cuboid fits within this container / 该长方体是否可放入此容器
*/
    fun enabled(
        unit: AbstractCuboid<V>,
        orientation: Orientation = Orientation.Upright
    ): Boolean {
        return (width geq orientation.width(unit)) == true
                && (height geq orientation.height(unit)) == true
                && (depth geq orientation.depth(unit)) == true
    }

/**
 * enabled.
 * enabled。
 * @param unit placement to check fit for / 待检查适配的放置物
 * @return whether the placement fits within this container / 该放置物是否可放入此容器
*/
    fun enabled(unit: QuantityPlacement3<*, V>): Boolean {
        return (width geq unit.maxX) == true
                && (height geq unit.maxY) == true
                && (depth geq unit.maxZ) == true
    }

/**
 * enabled.
 * enabled。
 * @param units placements to check fit for / 待检查适配的放置物列表
 * @return whether all placements fit within this container's dimensions / 所有放置物是否在此容器的尺寸范围内
*/
    fun enabled(units: List<QuantityPlacement3<*, V>>): Boolean {
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

/**
 * restSpace.
 * restSpace。
 * @param offset offset point / 偏移点
 * @return remaining container shape / 剩余容器形状
*/
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

    /** Length of the container along the primary axis / 容器沿主轴方向的长度 */
    val length: Quantity<V>

    /** Width of the container / 容器宽度 */
    val width: Quantity<V>

    /** Projective plane defining the 2D orientation / 投影平面，定义二维方向 */
    val plane: P

/**
 * restSpace.
 * restSpace。
 * @param offset offset point / 偏移点
 * @return remaining container shape / 剩余容器形状
*/
    fun restSpace(offset: QuantityPoint2<V>): QuantityContainer2Shape<P, V> {
        return QuantityContainer2Shape(
            length = quantityMinusByValue(length, offset.x),
            width = quantityMinusByValue(width, offset.y),
            plane = plane
        )
    }

/**
 * restSpace.
 * restSpace。
 * @param offset offset point / 偏移点
 * @return remaining container shape / 剩余容器形状
*/
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
