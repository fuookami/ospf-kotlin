package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.math.geometry.QuantityCuboid3
import fuookami.ospf.kotlin.math.geometry.QuantityCuboid3View
import fuookami.ospf.kotlin.math.geometry.QuantityPlaneFrame3
import fuookami.ospf.kotlin.math.geometry.QuantityPlanePoint2
import fuookami.ospf.kotlin.math.geometry.QuantityPlanePoint3
import fuookami.ospf.kotlin.math.geometry.QuantityPlaneVector3
import fuookami.ospf.kotlin.math.geometry.QuantityRectangle2
import fuookami.ospf.kotlin.quantities.quantity.Quantity

fun ProjectivePlane.toPlaneFrame3(): QuantityPlaneFrame3 {
    return when (this) {
        Bottom -> QuantityPlaneFrame3.ZX
        Side -> QuantityPlaneFrame3.XY
        Front -> QuantityPlaneFrame3.YZ
    }
}

fun ProjectivePlane.distanceByGeometry(point: QuantityPoint3): Quantity<InfraScalar> {
    return toPlaneFrame3().distance(point.toGeometryPoint3())
}

fun ProjectivePlane.point2ByGeometry(point: QuantityPoint3): QuantityPoint2 {
    return toPlaneFrame3().point2(point.toGeometryPoint3()).toCompat()
}

fun ProjectivePlane.point3ByGeometry(
    point: QuantityPoint2,
    distance: Quantity<InfraScalar>
): QuantityPoint3 {
    return toPlaneFrame3().point3(point.toGeometryPoint2(), distance).toCompat()
}

fun ProjectivePlane.vectorByGeometry(distance: Quantity<InfraScalar>): QuantityVector3 {
    return toPlaneFrame3().vector(distance).toCompat()
}

fun ProjectivePlane.footprintByGeometry(cuboid: QuantityCuboid3<InfraScalar>): QuantityRectangle2<InfraScalar> {
    return toPlaneFrame3().footprint(cuboid)
}

fun ProjectivePlane.footprintByGeometry(view: QuantityCuboid3View<InfraScalar>): QuantityRectangle2<InfraScalar> {
    return footprintByGeometry(view.cuboid)
}

private fun QuantityPoint2.toGeometryPoint2(): QuantityPlanePoint2<InfraScalar> {
    return QuantityPlanePoint2(x = x, y = y)
}

private fun QuantityPoint3.toGeometryPoint3(): QuantityPlanePoint3<InfraScalar> {
    return QuantityPlanePoint3(x = x, y = y, z = z)
}

private fun QuantityPlanePoint2<InfraScalar>.toCompat(): QuantityPoint2 {
    return QuantityPoint2(x = x, y = y)
}

private fun QuantityPlanePoint3<InfraScalar>.toCompat(): QuantityPoint3 {
    return QuantityPoint3(x = x, y = y, z = z)
}

private fun QuantityPlaneVector3<InfraScalar>.toCompat(): QuantityVector3 {
    return QuantityVector3(x = x, y = y, z = z)
}
