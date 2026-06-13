/**
 * 投影面几何桥接。
 * Projective plane geometry bridge.
 */
package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.geometry.QuantityRectangle2 as GeometryRectangle2
import fuookami.ospf.kotlin.quantities.quantity.Quantity

fun ProjectivePlane.toPlaneFrame3(): QuantityPlaneFrame3 {
    return when (this) {
        Bottom -> QuantityPlaneFrame3.ZX
        Side -> QuantityPlaneFrame3.XY
        Front -> QuantityPlaneFrame3.YZ
    }
}

fun <V : FloatingNumber<V>> ProjectivePlane.distanceByGeometry(point: QuantityPoint3<V>): Quantity<V> {
    return toPlaneFrame3().distance(point.toGeometryPoint3())
}

fun <V : FloatingNumber<V>> ProjectivePlane.point2ByGeometry(point: QuantityPoint3<V>): QuantityPoint2<V> {
    return toPlaneFrame3().point2(point.toGeometryPoint3()).toDomainPoint2()
}

fun <V : FloatingNumber<V>> ProjectivePlane.point3ByGeometry(
    point: QuantityPoint2<V>,
    distance: Quantity<V>
): QuantityPoint3<V> {
    return toPlaneFrame3().point3(point.toGeometryPoint2(), distance).toDomainPoint3()
}

fun <V : FloatingNumber<V>> ProjectivePlane.vectorByGeometry(distance: Quantity<V>): QuantityVector3<V> {
    return toPlaneFrame3().vector(distance).toDomainVector3()
}

fun ProjectivePlane.footprintByGeometry(cuboid: QuantityCuboid3<FltX>): GeometryRectangle2<FltX> {
    return toPlaneFrame3().footprint(cuboid)
}

fun ProjectivePlane.footprintByGeometry(view: QuantityCuboid3View<FltX>): GeometryRectangle2<FltX> {
    return footprintByGeometry(view.cuboid)
}

private fun <V : FloatingNumber<V>> QuantityPoint2<V>.toGeometryPoint2(): QuantityPlanePoint2<V> {
    return QuantityPlanePoint2(x = x, y = y)
}

private fun <V : FloatingNumber<V>> QuantityPoint3<V>.toGeometryPoint3(): QuantityPlanePoint3<V> {
    return QuantityPlanePoint3(x = x, y = y, z = z)
}

private fun <V : FloatingNumber<V>> QuantityPlanePoint2<V>.toDomainPoint2(): QuantityPoint2<V> {
    return QuantityPoint2(x = x, y = y)
}

private fun <V : FloatingNumber<V>> QuantityPlanePoint3<V>.toDomainPoint3(): QuantityPoint3<V> {
    return QuantityPoint3(x = x, y = y, z = z)
}

private fun <V : FloatingNumber<V>> QuantityPlaneVector3<V>.toDomainVector3(): QuantityVector3<V> {
    return QuantityVector3(x = x, y = y, z = z)
}
