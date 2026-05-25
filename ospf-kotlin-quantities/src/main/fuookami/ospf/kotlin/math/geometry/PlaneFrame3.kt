package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity

data class QuantityPlanePoint2<V : FloatingNumber<V>>(
    val x: Quantity<V>,
    val y: Quantity<V>
)

data class QuantityPlanePoint3<V : FloatingNumber<V>>(
    val x: Quantity<V>,
    val y: Quantity<V>,
    val z: Quantity<V>
) {
    fun along(axis: Axis3): Quantity<V> {
        return when (axis) {
            Axis3.X -> x
            Axis3.Y -> y
            Axis3.Z -> z
        }
    }
}

data class QuantityPlaneVector3<V : FloatingNumber<V>>(
    val x: Quantity<V>,
    val y: Quantity<V>,
    val z: Quantity<V>
)

/**
 * 平面坐标框架是纯几何能力；BPP3D 的 Bottom/Side/Front 映射由桥接层负责。
 * Plane frame is pure geometry; BPP3D Bottom/Side/Front mapping stays in bridge layer.
 */
data class QuantityPlaneFrame3(
    val firstAxis: Axis3,
    val secondAxis: Axis3
) {
    init {
        require(firstAxis != secondAxis) { "firstAxis and secondAxis must be different." }
    }

    val normalAxis: Axis3
        get() {
            return when {
                (firstAxis == Axis3.X && secondAxis == Axis3.Y) || (firstAxis == Axis3.Y && secondAxis == Axis3.X) -> Axis3.Z
                (firstAxis == Axis3.X && secondAxis == Axis3.Z) || (firstAxis == Axis3.Z && secondAxis == Axis3.X) -> Axis3.Y
                (firstAxis == Axis3.Y && secondAxis == Axis3.Z) || (firstAxis == Axis3.Z && secondAxis == Axis3.Y) -> Axis3.X
                else -> throw IllegalArgumentException("Unsupported axis frame: $firstAxis, $secondAxis")
            }
        }

    fun <V : FloatingNumber<V>> distance(point: QuantityPlanePoint3<V>): Quantity<V> = point.along(normalAxis)

    fun <V : FloatingNumber<V>> point2(point: QuantityPlanePoint3<V>): QuantityPlanePoint2<V> {
        return QuantityPlanePoint2(
            x = point.along(firstAxis),
            y = point.along(secondAxis)
        )
    }

    fun <V : FloatingNumber<V>> point3(point: QuantityPlanePoint2<V>, distance: Quantity<V>): QuantityPlanePoint3<V> {
        val x = if (firstAxis == Axis3.X) {
            point.x
        } else if (secondAxis == Axis3.X) {
            point.y
        } else {
            distance
        }
        val y = if (firstAxis == Axis3.Y) {
            point.x
        } else if (secondAxis == Axis3.Y) {
            point.y
        } else {
            distance
        }
        val z = if (firstAxis == Axis3.Z) {
            point.x
        } else if (secondAxis == Axis3.Z) {
            point.y
        } else {
            distance
        }
        return QuantityPlanePoint3(
            x = x,
            y = y,
            z = z
        )
    }

    fun <V : FloatingNumber<V>> vector(distance: Quantity<V>): QuantityPlaneVector3<V> {
        val zero = quantityZeroOf(distance)
        return when (normalAxis) {
            Axis3.X -> QuantityPlaneVector3(x = distance, y = zero, z = zero)
            Axis3.Y -> QuantityPlaneVector3(x = zero, y = distance, z = zero)
            Axis3.Z -> QuantityPlaneVector3(x = zero, y = zero, z = distance)
        }
    }

    fun <V : FloatingNumber<V>> footprint(cuboid: QuantityCuboid3<V>): QuantityRectangle2<V> {
        return QuantityRectangle2(
            width = cuboid.along(firstAxis),
            height = cuboid.along(secondAxis)
        )
    }

    companion object {
        val XY = QuantityPlaneFrame3(firstAxis = Axis3.X, secondAxis = Axis3.Y)
        val YX = QuantityPlaneFrame3(firstAxis = Axis3.Y, secondAxis = Axis3.X)
        val XZ = QuantityPlaneFrame3(firstAxis = Axis3.X, secondAxis = Axis3.Z)
        val ZX = QuantityPlaneFrame3(firstAxis = Axis3.Z, secondAxis = Axis3.X)
        val YZ = QuantityPlaneFrame3(firstAxis = Axis3.Y, secondAxis = Axis3.Z)
        val ZY = QuantityPlaneFrame3(firstAxis = Axis3.Z, secondAxis = Axis3.Y)
    }
}

