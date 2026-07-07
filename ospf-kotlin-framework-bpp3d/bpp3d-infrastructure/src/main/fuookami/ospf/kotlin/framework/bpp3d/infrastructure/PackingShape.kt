/**
 * 三维装载形状基础抽象。
 * Core abstractions for 3D packing shapes.
 */
package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import kotlin.math.PI
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.geometry.Axis3
import fuookami.ospf.kotlin.quantities.quantity.*

/**
 * Enumeration of packing shape types.
 * 装载形状类型枚举。
 */
enum class PackingShapeType {
    Cuboid,
    Cylinder
}

/**
 * Enumeration of algorithm-specific packing shape types, distinguishing cylinder orientations.
 * 算法侧装载形状类型枚举，区分圆柱体方向。
 */
enum class PackingAlgorithmShapeType {
    Cuboid,
    VerticalCylinder,
    HorizontalCylinderX,
    HorizontalCylinderZ
}

/**
 * Enumeration of packing axes in 3D space.
 * 三维装载轴向枚举。
 */
enum class PackingAxis3 {
    X,
    Y,
    Z
}

/**
 * 3D shape bounding box with width, height, and depth.
 * 三维形状包围盒，包含宽度、高度和深度。
 *
 * @property width The bounding width.
 *                 中文：包围宽度。
 * @property height The bounding height.
 *                  中文：包围高度。
 * @property depth The bounding depth.
 *                 中文：包围深度。
 */
data class ShapeBoundingBox3<V : FloatingNumber<V>>(
    val width: Quantity<V>,
    val height: Quantity<V>,
    val depth: Quantity<V>
)

/**
 * Sealed interface for 2D shape footprints, either rectangular or circular.
 * 二维形状占位密封接口，可以是矩形或圆形。
 */
sealed interface ShapeFootprint2<V : FloatingNumber<V>> {
    /**
     * Rectangular footprint with width and depth.
     * 矩形占位，包含宽度和深度。
     *
     * @property width The footprint width.
     *                 中文：占位宽度。
     * @property depth The footprint depth.
     *                 中文：占位深度。
     */
    data class Rectangle<V : FloatingNumber<V>>(
        val width: Quantity<V>,
        val depth: Quantity<V>
    ) : ShapeFootprint2<V>

    /**
     * Circular footprint with radius.
     * 圆形占位，包含半径。
     *
     * @property radius The footprint radius.
     *                   中文：占位半径。
     */
    data class Circle<V : FloatingNumber<V>>(
        val radius: Quantity<V>
    ) : ShapeFootprint2<V>
}

/**
 * Interface for 3D packing shapes, providing bounding dimensions, weight, and footprint.
 * 三维装载形状接口，提供包围尺寸、重量和占位信息。
 */
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
    val cylinder: AbstractCylinder<FltX>
) : PackingShape3<FltX> {
    override val shapeType: PackingShapeType = PackingShapeType.Cylinder
    override val algorithmShapeType: PackingAlgorithmShapeType
        get() = when (cylinder.axis) {
            Axis3.X -> PackingAlgorithmShapeType.HorizontalCylinderX
            Axis3.Y -> PackingAlgorithmShapeType.VerticalCylinder
            Axis3.Z -> PackingAlgorithmShapeType.HorizontalCylinderZ
        }
    override val weight: Quantity<FltX> by cylinder::weight
    override val axis: Axis3 by cylinder::axis

    val radius: Quantity<FltX> by cylinder::radius
    val diameter: Quantity<FltX> get() = radius + radius

    override val boundingWidth: Quantity<FltX>
        get() = when (cylinder.axis) {
            Axis3.X -> cylinder.height
            Axis3.Y, Axis3.Z -> diameter
        }
    override val boundingHeight: Quantity<FltX>
        get() = when (cylinder.axis) {
            Axis3.X, Axis3.Z -> diameter
            Axis3.Y -> cylinder.height
        }
    override val boundingDepth: Quantity<FltX>
        get() = when (cylinder.axis) {
            Axis3.Z -> cylinder.height
            Axis3.X, Axis3.Y -> diameter
        }
    override val actualVolume: Quantity<FltX>
        get() = FltX(PI) * radius * radius * cylinder.height

    override fun footprint(): ShapeFootprint2<FltX> {
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

fun AbstractCylinder<FltX>.asPackingShape3(): PackingShape3<FltX> {
    return CylinderPackingShape3(this)
}

fun Axis3.asPackingAxis3(): PackingAxis3 {
    return when (this) {
        Axis3.X -> PackingAxis3.X
        Axis3.Y -> PackingAxis3.Y
        Axis3.Z -> PackingAxis3.Z
    }
}
