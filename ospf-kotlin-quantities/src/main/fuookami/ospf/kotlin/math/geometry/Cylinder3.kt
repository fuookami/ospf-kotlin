package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.times

data class QuantityAxisLine3<V : FloatingNumber<V>>(
    val axis: Axis3,
    val from: Quantity<V>,
    val to: Quantity<V>
)

data class QuantityCylinder3<V : FloatingNumber<V>>(
    val radius: Quantity<V>,
    val height: Quantity<V>,
    val axis: Axis3
) : QuantityShape3<V> {
    val diameter: Quantity<V> get() = quantityPlus(radius, radius)

    override val boundingCuboid: QuantityCuboid3<V>
        get() {
            return when (axis) {
                Axis3.X -> QuantityCuboid3(width = height, height = diameter, depth = diameter)
                Axis3.Y -> QuantityCuboid3(width = diameter, height = height, depth = diameter)
                Axis3.Z -> QuantityCuboid3(width = diameter, height = diameter, depth = height)
            }
        }

    fun along(axis: Axis3): Quantity<V> {
        return if (axis == this.axis) {
            height
        } else {
            diameter
        }
    }

    val axisLineAtOrigin: QuantityAxisLine3<V>
        get() = QuantityAxisLine3(
            axis = axis,
            from = quantityZeroOf(height),
            to = height
        )

    fun projectionOn(plane: AxisPlane3): QuantityProjection2<V> {
        return if (plane.contains(axis)) {
            QuantityRectangle2(
                width = along(plane.firstAxis),
                height = along(plane.secondAxis)
            )
        } else {
            QuantityCircle2(radius)
        }
    }

    fun baseArea(pi: V): Quantity<V> = (radius * radius) * pi

    fun volume(pi: V): Quantity<V> = baseArea(pi) * height

    fun permute(permutation: QuantityAxisPermutation3): QuantityCylinder3<V> = permutation.apply(this)

    fun boundingBoxAtOrigin(): QuantityBox3<V> = QuantityBox3.atOrigin(boundingCuboid)

    fun toBoundingBox(
        x: Quantity<V>,
        y: Quantity<V>,
        z: Quantity<V>
    ): QuantityBox3<V> {
        return QuantityBox3(
            x = x,
            y = y,
            z = z,
            cuboid = boundingCuboid
        )
    }
}

typealias QuantityAxisAlignedCylinder3<V> = QuantityCylinder3<V>

