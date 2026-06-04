/**
 * 三维装载形状基础抽象。
 * Core abstractions for 3D packing shapes.
 */
package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import kotlin.math.PI
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.geometry.Axis3
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.plus
import fuookami.ospf.kotlin.quantities.quantity.times

enum class PackingShapeType {
    Cuboid,
    Cylinder
}

enum class PackingAlgorithmShapeType {
    Cuboid,
    VerticalCylinder,
    HorizontalCylinderX,
    HorizontalCylinderZ,
    BoundingCuboid
}

enum class PackingAxis3 {
    X,
    Y,
    Z
}

data class ShapeBoundingBox3<V : FloatingNumber<V>>(
    val width: Quantity<V>,
    val height: Quantity<V>,
    val depth: Quantity<V>
)

sealed interface ShapeFootprint2<V : FloatingNumber<V>> {
    data class Rectangle<V : FloatingNumber<V>>(
        val width: Quantity<V>,
        val depth: Quantity<V>
    ) : ShapeFootprint2<V>

    data class Circle<V : FloatingNumber<V>>(
        val radius: Quantity<V>
    ) : ShapeFootprint2<V>
}

interface PackingShape3<V : FloatingNumber<V>> {
    val shapeType: PackingShapeType
    val algorithmShapeType: PackingAlgorithmShapeType
    val weight: Quantity<V>
    val boundingWidth: Quantity<V>
    val boundingHeight: Quantity<V>
    val boundingDepth: Quantity<V>
    val actualVolume: Quantity<V>
    val axis: Axis3?

    val boundingBox: ShapeBoundingBox3<V>
        get() = ShapeBoundingBox3(
            width = boundingWidth,
            height = boundingHeight,
            depth = boundingDepth
        )

    fun footprint(): ShapeFootprint2<V>
}

data class CuboidPackingShape3<V : FloatingNumber<V>>(
    val cuboid: AbstractCuboid<V>
) : PackingShape3<V> {
    override val shapeType: PackingShapeType = PackingShapeType.Cuboid
    override val algorithmShapeType: PackingAlgorithmShapeType = PackingAlgorithmShapeType.Cuboid
    override val weight: Quantity<V> by cuboid::weight
    override val boundingWidth: Quantity<V> by cuboid::width
    override val boundingHeight: Quantity<V> by cuboid::height
    override val boundingDepth: Quantity<V> by cuboid::depth
    override val actualVolume: Quantity<V> by cuboid::actualVolume
    override val axis: Axis3? = null

    override fun footprint(): ShapeFootprint2<V> {
        return ShapeFootprint2.Rectangle(
            width = cuboid.width,
            depth = cuboid.depth
        )
    }
}

data class CylinderPackingShape3(
    val cylinder: AbstractCylinder<InfraNumber>
) : PackingShape3<InfraNumber> {
    override val shapeType: PackingShapeType = PackingShapeType.Cylinder
    override val algorithmShapeType: PackingAlgorithmShapeType
        get() = when (cylinder.axis) {
            Axis3.X -> PackingAlgorithmShapeType.HorizontalCylinderX
            Axis3.Y -> PackingAlgorithmShapeType.VerticalCylinder
            Axis3.Z -> PackingAlgorithmShapeType.HorizontalCylinderZ
        }
    override val weight: Quantity<InfraNumber> by cylinder::weight
    override val axis: Axis3 by cylinder::axis

    val radius: Quantity<InfraNumber> by cylinder::radius
    val diameter: Quantity<InfraNumber> get() = radius + radius

    override val boundingWidth: Quantity<InfraNumber>
        get() = when (cylinder.axis) {
            Axis3.X -> cylinder.height
            Axis3.Y, Axis3.Z -> diameter
        }
    override val boundingHeight: Quantity<InfraNumber>
        get() = when (cylinder.axis) {
            Axis3.X, Axis3.Z -> diameter
            Axis3.Y -> cylinder.height
        }
    override val boundingDepth: Quantity<InfraNumber>
        get() = when (cylinder.axis) {
            Axis3.Z -> cylinder.height
            Axis3.X, Axis3.Y -> diameter
        }
    override val actualVolume: Quantity<InfraNumber>
        get() = infraScalar(PI) * radius * radius * cylinder.height

    override fun footprint(): ShapeFootprint2<InfraNumber> {
        return when (cylinder.axis) {
            Axis3.Y -> ShapeFootprint2.Circle(radius)
            Axis3.X, Axis3.Z -> ShapeFootprint2.Rectangle(
                width = boundingWidth,
                depth = boundingDepth
            )
        }
    }
}

fun <V : FloatingNumber<V>> AbstractCuboid<V>.asPackingShape3(): PackingShape3<V> {
    return CuboidPackingShape3(this)
}

fun AbstractCylinder<InfraNumber>.asPackingShape3(): PackingShape3<InfraNumber> {
    return CylinderPackingShape3(this)
}

fun Axis3.asPackingAxis3(): PackingAxis3 {
    return when (this) {
        Axis3.X -> PackingAxis3.X
        Axis3.Y -> PackingAxis3.Y
        Axis3.Z -> PackingAxis3.Z
    }
}
