/**
 * 圆柱体基础设施。
 * Cylinder infrastructure.
 */
package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity

interface AbstractCylinder<V : FloatingNumber<V>> {
    val radius: Quantity<V>
    val height: Quantity<V>
    val axis: Axis3

    val weight: Quantity<V>
}

interface Cylinder<T : Cylinder<T>> : AbstractCylinder<FltX> {
    val self: T
    val enabledAxes: List<Axis3>

    fun geometry(axis: Axis3 = this.axis): QuantityCylinder3<FltX> {
        return QuantityCylinder3(
            radius = radius,
            height = height,
            axis = axis
        )
    }
}
