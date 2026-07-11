/**
 * 圆柱体基础设施。
 * Cylinder infrastructure.
*/
package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity

/**
 * Abstract cylinder interface defining basic dimensions (radius, height, axis) and weight.
 * 抽象圆柱体接口，定义基本尺寸（半径、高度、轴向）和重量属性。
*/
interface AbstractCylinder<V : FloatingNumber<V>> {

    /** Radius of the cylinder / 圆柱体半径 */
    val radius: Quantity<V>

    /** Height of the cylinder / 圆柱体高度 */
    val height: Quantity<V>

    /** Primary axis of the cylinder / 圆柱体主轴方向 */
    val axis: Axis3

    /** Weight of the cylinder / 圆柱体重量 */
    val weight: Quantity<V>
}

/**
 * Cylinder interface supporting axis selection and geometry views.
 * 圆柱体接口，支持轴向选择和几何视图。
*/
interface Cylinder<T : Cylinder<T>> : AbstractCylinder<FltX> {

    /** Self-reference for fluent API / 自引用，用于流式 API */
    val self: T

    /** List of allowed axis orientations / 允许的轴向方向列表 */
    val enabledAxes: List<Axis3>

    /**
     * Returns the geometry representation for the given axis.
     * 返回给定轴向的几何表示。
     *
     * @param axis The cylinder axis, defaults to the current axis.
     *             中文：圆柱轴向，默认为当前轴向。
     * @return The 3D quantity cylinder geometry.
     *         中文：三维带量圆柱体几何。
    */
    fun geometry(axis: Axis3 = this.axis): QuantityCylinder3<FltX> {
        return QuantityCylinder3(
            radius = radius,
            height = height,
            axis = axis
        )
    }
}
