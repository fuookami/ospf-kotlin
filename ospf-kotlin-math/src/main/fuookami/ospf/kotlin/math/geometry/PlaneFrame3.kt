package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber

data class PlanePoint2<V : FloatingNumber<V>>(
    val x: V,
    val y: V
)

data class PlanePoint3<V : FloatingNumber<V>>(
    val x: V,
    val y: V,
    val z: V
) {
    fun along(axis: Axis3): V {
        return when (axis) {
            Axis3.X -> x
            Axis3.Y -> y
            Axis3.Z -> z
        }
    }
}

data class PlaneVector3<V : FloatingNumber<V>>(
    val x: V,
    val y: V,
    val z: V
)

/**
 * 平面坐标框架是纯几何能力；BPP3D 的 Bottom/Side/Front 映射由桥接层负责。
 * Plane frame is pure geometry; BPP3D Bottom/Side/Front mapping stays in bridge layer.
 */
data class PlaneFrame3(
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

    fun <V : FloatingNumber<V>> distance(point: PlanePoint3<V>): V = point.along(normalAxis)

    fun <V : FloatingNumber<V>> point2(point: PlanePoint3<V>): PlanePoint2<V> {
        return PlanePoint2(
            x = point.along(firstAxis),
            y = point.along(secondAxis)
        )
    }

    fun <V : FloatingNumber<V>> point3(point: PlanePoint2<V>, distance: V): PlanePoint3<V> {
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
        return PlanePoint3(
            x = x,
            y = y,
            z = z
        )
    }

    fun <V : FloatingNumber<V>> vector(distance: V): PlaneVector3<V> {
        val zero = quantityZeroOf(distance)
        return when (normalAxis) {
            Axis3.X -> PlaneVector3(x = distance, y = zero, z = zero)
            Axis3.Y -> PlaneVector3(x = zero, y = distance, z = zero)
            Axis3.Z -> PlaneVector3(x = zero, y = zero, z = distance)
        }
    }

    fun <V : FloatingNumber<V>> footprint(cuboid: Cuboid3<V>): Rectangle2<V> {
        return Rectangle2(
            width = cuboid.along(firstAxis),
            height = cuboid.along(secondAxis)
        )
    }

    companion object {
        val XY = PlaneFrame3(firstAxis = Axis3.X, secondAxis = Axis3.Y)
        val YX = PlaneFrame3(firstAxis = Axis3.Y, secondAxis = Axis3.X)
        val XZ = PlaneFrame3(firstAxis = Axis3.X, secondAxis = Axis3.Z)
        val ZX = PlaneFrame3(firstAxis = Axis3.Z, secondAxis = Axis3.X)
        val YZ = PlaneFrame3(firstAxis = Axis3.Y, secondAxis = Axis3.Z)
        val ZY = PlaneFrame3(firstAxis = Axis3.Z, secondAxis = Axis3.Y)
    }
}

