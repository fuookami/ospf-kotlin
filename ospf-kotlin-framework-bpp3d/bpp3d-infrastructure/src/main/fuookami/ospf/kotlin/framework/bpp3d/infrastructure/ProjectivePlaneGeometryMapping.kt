/**
 * 投影面几何桥接。
 * Projective plane geometry bridge.
 */
package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.math.geometry.QuantityCuboid3
import fuookami.ospf.kotlin.math.geometry.QuantityCuboid3View
import fuookami.ospf.kotlin.math.geometry.QuantityPlaneFrame3
import fuookami.ospf.kotlin.math.geometry.QuantityPlanePoint2
import fuookami.ospf.kotlin.math.geometry.QuantityPlanePoint3
import fuookami.ospf.kotlin.math.geometry.QuantityPlaneVector3
import fuookami.ospf.kotlin.math.geometry.QuantityRectangle2
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity

fun ProjectivePlane.toPlaneFrame3(): QuantityPlaneFrame3 {
    return when (this) {
        Bottom -> QuantityPlaneFrame3.ZX
        Side -> QuantityPlaneFrame3.XY
        Front -> QuantityPlaneFrame3.YZ
    }
}

fun ProjectivePlane.distanceByGeometry(point: QuantityPoint3): Quantity<InfraNumber> {
    return toPlaneFrame3().distance(point.toGeometryPoint3())
}

fun <V : FloatingNumber<V>> ProjectivePlane.distanceByGeometry(point: QuantityPoint3G<V>): Quantity<V> {
    return toPlaneFrame3().distance(point.toGeometryPoint3())
}

fun ProjectivePlane.point2ByGeometry(point: QuantityPoint3): QuantityPoint2 {
    return toPlaneFrame3().point2(point.toGeometryPoint3()).toCompat()
}

fun <V : FloatingNumber<V>> ProjectivePlane.point2ByGeometry(point: QuantityPoint3G<V>): QuantityPoint2G<V> {
    return toPlaneFrame3().point2(point.toGeometryPoint3()).toCompat()
}

fun ProjectivePlane.point3ByGeometry(
    point: QuantityPoint2,
    distance: Quantity<InfraNumber>
): QuantityPoint3 {
    return toPlaneFrame3().point3(point.toGeometryPoint2(), distance).toCompat()
}

fun <V : FloatingNumber<V>> ProjectivePlane.point3ByGeometry(
    point: QuantityPoint2G<V>,
    distance: Quantity<V>
): QuantityPoint3G<V> {
    return toPlaneFrame3().point3(point.toGeometryPoint2(), distance).toCompat()
}

fun ProjectivePlane.vectorByGeometry(distance: Quantity<InfraNumber>): QuantityVector3 {
    return toPlaneFrame3().vector(distance).toCompat()
}

fun <V : FloatingNumber<V>> ProjectivePlane.vectorByGeometry(distance: Quantity<V>): QuantityVector3G<V> {
    return toPlaneFrame3().vector(distance).toCompat()
}

fun ProjectivePlane.footprintByGeometry(cuboid: QuantityCuboid3<InfraNumber>): QuantityRectangle2<InfraNumber> {
    return toPlaneFrame3().footprint(cuboid)
}

fun ProjectivePlane.footprintByGeometry(view: QuantityCuboid3View<InfraNumber>): QuantityRectangle2<InfraNumber> {
    return footprintByGeometry(view.cuboid)
}

private fun QuantityPoint2.toGeometryPoint2(): QuantityPlanePoint2<InfraNumber> {
    return QuantityPlanePoint2(x = x, y = y)
}

private fun QuantityPoint3.toGeometryPoint3(): QuantityPlanePoint3<InfraNumber> {
    return QuantityPlanePoint3(x = x, y = y, z = z)
}

private fun QuantityPlanePoint2<InfraNumber>.toCompat(): QuantityPoint2 {
    return QuantityPoint2(x = x, y = y)
}

private fun QuantityPlanePoint3<InfraNumber>.toCompat(): QuantityPoint3 {
    return QuantityPoint3(x = x, y = y, z = z)
}

private fun QuantityPlaneVector3<InfraNumber>.toCompat(): QuantityVector3 {
    return QuantityVector3(x = x, y = y, z = z)
}

private fun <V : FloatingNumber<V>> QuantityPoint2G<V>.toGeometryPoint2(): QuantityPlanePoint2<V> {
    return QuantityPlanePoint2(x = x, y = y)
}

private fun <V : FloatingNumber<V>> QuantityPoint3G<V>.toGeometryPoint3(): QuantityPlanePoint3<V> {
    return QuantityPlanePoint3(x = x, y = y, z = z)
}

private fun <V : FloatingNumber<V>> QuantityPlanePoint2<V>.toCompat(): QuantityPoint2G<V> {
    return QuantityPoint2G(x = x, y = y)
}

private fun <V : FloatingNumber<V>> QuantityPlanePoint3<V>.toCompat(): QuantityPoint3G<V> {
    return QuantityPoint3G(x = x, y = y, z = z)
}

private fun <V : FloatingNumber<V>> QuantityPlaneVector3<V>.toCompat(): QuantityVector3G<V> {
    return QuantityVector3G(x = x, y = y, z = z)
}
