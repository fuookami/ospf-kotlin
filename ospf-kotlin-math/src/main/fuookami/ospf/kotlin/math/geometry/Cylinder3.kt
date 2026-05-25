package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber

data class AxisLine3<V : FloatingNumber<V>>(
    val axis: Axis3,
    val from: V,
    val to: V
)

data class Cylinder3<V : FloatingNumber<V>>(
    val radius: V,
    val height: V,
    val axis: Axis3
) : Shape3<V> {
    val diameter: V get() = quantityPlus(radius, radius)

    override val boundingCuboid: Cuboid3<V>
        get() {
            return when (axis) {
                Axis3.X -> Cuboid3(width = height, height = diameter, depth = diameter)
                Axis3.Y -> Cuboid3(width = diameter, height = height, depth = diameter)
                Axis3.Z -> Cuboid3(width = diameter, height = diameter, depth = height)
            }
        }

    fun along(axis: Axis3): V {
        return if (axis == this.axis) {
            height
        } else {
            diameter
        }
    }

    val axisLineAtOrigin: AxisLine3<V>
        get() = AxisLine3(
            axis = axis,
            from = quantityZeroOf(height),
            to = height
        )

    fun projectionOn(plane: AxisPlane3): Projection2<V> {
        return if (plane.contains(axis)) {
            Rectangle2(
                width = along(plane.firstAxis),
                height = along(plane.secondAxis)
            )
        } else {
            Circle2(radius)
        }
    }

    fun baseArea(pi: V): V = (radius * radius) * pi

    fun volume(pi: V): V = baseArea(pi) * height

    fun permute(permutation: AxisPermutation3): Cylinder3<V> = permutation.apply(this)

    fun boundingBoxAtOrigin(): Box3<V> = Box3.atOrigin(boundingCuboid)

    fun toBoundingBox(
        x: V,
        y: V,
        z: V
    ): Box3<V> {
        return Box3(
            x = x,
            y = y,
            z = z,
            cuboid = boundingCuboid
        )
    }
}

typealias AxisAlignedCylinder3<V> = Cylinder3<V>

