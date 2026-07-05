/**
 * 立方体基础设施。
 * Cuboid infrastructure.
 */
package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.operator.Plus
import fuookami.ospf.kotlin.quantities.quantity.*

/**
 * 抽象立方体，定义立方体的基本尺寸（宽度、高度、深度）和重量属性。
 * Abstract cuboid defining basic dimensions (width, height, depth) and weight properties.
 */
interface AbstractCuboid<V : FloatingNumber<V>> {
    val width: Quantity<V>
    val height: Quantity<V>
    val depth: Quantity<V>

    val weight: Quantity<V>
    val volume: Quantity<V> get() = depth * height * width
    val actualVolume: Quantity<V> get() = volume
    val linearDensity: Quantity<V> get() = weight / depth
}

/**
 * 立方体，支持方向变换和几何视图。
 * Cuboid supporting orientation transformations and geometry views.
 */
interface Cuboid<T : Cuboid<T, V>, V : FloatingNumber<V>> : AbstractCuboid<V> {
    val self: T
    val enabledOrientations: List<Orientation>

    /**
     * 获取指定方向的几何视图。
     * Get the geometry view for the specified orientation.
     *
     * @param orientation 方向，默认为正放 / The orientation, defaults to Upright
     * @return 三维长方体视图 / A 3D cuboid view
     */
    fun geometryView(orientation: Orientation = Orientation.Upright): QuantityCuboid3View<V> {
        return QuantityCuboid3View(
            origin = QuantityCuboid3(
                width = width,
                height = height,
                depth = depth
            ),
            permutation = orientation.toAxisPermutation3()
        )
    }

    /**
     * 获取指定方向的几何体。
     * Get the geometry for the specified orientation.
     *
     * @param orientation 方向，默认为正放 / The orientation, defaults to Upright
     * @return 三维长方体 / A 3D cuboid
     */
    fun geometry(orientation: Orientation = Orientation.Upright): QuantityCuboid3<V> {
        return geometryView(orientation).cuboid
    }

    /**
     * 获取在二维容器几何中可用的方向。
     * Get the enabled orientations within a 2D container geometry.
     *
     * @param space 二维容器几何 / The 2D container geometry
     * @param withRotation 是否允许旋转，默认为 true / Whether rotation is allowed, defaults to true
     * @return 可用的方向列表 / The list of enabled orientations
     */
    fun enabledOrientationsAt(
        space: Container2Geometry<*, V>,
        withRotation: Boolean = true
    ): List<Orientation> {
        return enabledOrientations.filter {
            (space.length geq space.plane.length(this, it)) == true
                    && (space.width geq space.plane.width(this, it)) == true
                    && (withRotation || !it.rotated)
        }
    }

    /**
     * 获取在三维容器几何中可用的方向。
     * Get the enabled orientations within a 3D container geometry.
     *
     * @param space 三维容器几何 / The 3D container geometry
     * @param withRotation 是否允许旋转，默认为 true / Whether rotation is allowed, defaults to true
     * @return 可用的方向列表 / The list of enabled orientations
     */
    fun enabledOrientationsAt(
        space: Container3Geometry<V>,
        withRotation: Boolean = true
    ): List<Orientation> {
        return enabledOrientations.filter {
            (space.width geq it.width(this)) == true
                    && (space.height geq it.height(this)) == true
                    && (space.depth geq it.depth(this)) == true
                    && (withRotation || !it.rotated)
        }
    }

    /**
     * 获取指定方向的视图。
     * Get the view for the specified orientation.
     *
     * @param orientation 方向，默认为正放 / The orientation, defaults to Upright
     * @return 立方体视图，如果方向无效则返回 null / The cuboid view, or null if the orientation is invalid
     */
    fun view(orientation: Orientation = Orientation.Upright): CuboidView<T, V>? {
        return CuboidView(self, orientation)
    }
}

/**
 * 底部支撑信息，包含支撑面积和重量。
 * Bottom support information, including support area and weight.
 *
 * @property area 支撑面积 / The support area
 * @property weight 支撑重量 / The support weight
 */
data class BottomSupport(
    val area: Quantity<FltX>,
    val weight: Quantity<FltX>
) : Plus<BottomSupport, BottomSupport> {
    override fun plus(rhs: BottomSupport) = BottomSupport(
        area = area + rhs.area,
        weight = weight + rhs.weight
    )
}

/**
 * 立方体视图，表示具有特定方向的立方体。
 * Cuboid view representing a cuboid with a specific orientation.
 *
 * @property unit 基础立方体单元 / The base cuboid unit
 * @property orientation 方向 / The orientation
 */
open class CuboidView<T : Cuboid<T, V>, V : FloatingNumber<V>>(
    val unit: T,
    val orientation: Orientation = Orientation.Upright
) : AbstractCuboid<V>, Copyable<CuboidView<T, V>> {
    private val geometryView: QuantityCuboid3View<V> by lazy { unit.geometryView(orientation) }

    override val width get() = geometryView.width
    override val height get() = geometryView.height
    override val depth get() = geometryView.depth
    override val weight by unit::weight

    /**
     * 旋转后的方向。
     * The rotated orientation.
     */
    val rotatedOrientation by orientation::rotation

    /**
     * 旋转后的视图。
     * The rotated view.
     */
    open val rotation: CuboidView<T, V>?
        get() {
            return if (unit.enabledOrientations.contains(rotatedOrientation)) {
                unit.view(rotatedOrientation)
            } else {
                null
            }
        }

    /**
     * 在二维容器几何中获取旋转后的视图。
     * Get the rotated view in a 2D container geometry.
     *
     * @param space 二维容器几何 / The 2D container geometry
     * @return 旋转后的视图，如果旋转不可用则返回 null / The rotated view, or null if rotation is not available
     */
    open fun rotationAt(space: Container2Geometry<*, V>): CuboidView<T, V>? {
        return if (unit.enabledOrientationsAt(space).contains(rotatedOrientation)) {
            unit.view(rotatedOrientation)
        } else {
            null
        }
    }

    /**
     * 在三维容器几何中获取旋转后的视图。
     * Get the rotated view in a 3D container geometry.
     *
     * @param space 三维容器几何 / The 3D container geometry
     * @return 旋转后的视图，如果旋转不可用则返回 null / The rotated view, or null if rotation is not available
     */
    open fun rotationAt(space: Container3Geometry<V>): CuboidView<T, V>? {
        return if (unit.enabledOrientationsAt(space).contains(rotatedOrientation)) {
            unit.view(rotatedOrientation)
        } else {
            null
        }
    }

    override fun copy() = CuboidView(
        unit = unit,
        orientation = orientation
    )

    /**
     * 获取几何视图。
     * Gets the geometry view.
     *
     * @return 三维长方体视图 / A 3D cuboid view
     */
    fun toGeometryCuboid3View(): QuantityCuboid3View<V> = geometryView

    /**
     * 将几何视图转换为三维长方体。
     * Converts the geometry view to a 3D cuboid.
     *
     * @return 三维长方体 / A 3D cuboid
     */
    fun toGeometryCuboid3(): QuantityCuboid3<V> = geometryView.cuboid

    /**
     * 将几何视图转换为原点处的三维盒子。
     * Converts the geometry view to a 3D box at the origin.
     *
     * @return 原点处的三维盒子 / A 3D box at the origin
     */
    fun toGeometryBox3AtOrigin(): QuantityBox3<V> = QuantityBox3.atOrigin(geometryView.cuboid)

    override fun hashCode(): Int {
        var result = unit.hashCode()
        result = 31 * result + orientation.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CuboidView<*, *>) return false

        if (unit != other.unit) return false
        if (orientation != other.orientation) return false

        return true
    }

    override fun toString() = "$unit $orientation"
}

/**
 * 计算当前视图在底部视图上的支撑。
 * Calculate the support of this view on the bottom view.
 *
 * @param bottomView 底部视图 / The bottom view
 * @return 底部支撑信息 / The bottom support information
 */
fun CuboidView<*, FltX>.bottomSupport(bottomView: CuboidView<*, FltX>): BottomSupport {
    val shapePlacement = ShapePlacement3(
        shape = this.asPackingShape3(),
        position = point3FltX()
    )
    val bottomShapePlacement = ShapePlacement3(
        shape = bottomView.asPackingShape3(),
        position = point3FltX()
    )
    val supportArea = shapePlacement.footprintOverlapArea(bottomShapePlacement)
    val bottomArea = bottomShapePlacement.footprintOverlapArea(bottomShapePlacement)
    return if ((bottomArea eq (FltX.zero * bottomArea.unit)) == true) {
        BottomSupport(
            area = supportArea,
            weight = bottomView.weight * FltX.zero
        )
    } else {
        BottomSupport(
            area = supportArea,
            weight = (supportArea / bottomArea).value * bottomView.weight
        )
    }
}

/**
 * 计算底部支撑。
 * Calculate bottom support.
 *
 * @param unit 需要计算支撑的放置单元 / The placement unit to calculate support for
 * @param bottomUnits 底部已放置的单元列表 / The list of already-placed bottom units
 * @param shapeResolver 形状解析函数 / The shape resolver function
 * @return 底部支撑信息 / The bottom support information
 */
fun bottomSupport(
    unit: QuantityPlacement3<*, FltX>,
    bottomUnits: List<QuantityPlacement3<*, FltX>>,
    shapeResolver: (QuantityPlacement3<*, FltX>) -> PackingShape3<FltX> = { placement ->
        placement.view.asPackingShape3()
    }
): BottomSupport {
    val unitShapePlacement = unit.asShapePlacement3(shapeResolver)
    var support = BottomSupport(
        area = unit.depth * unit.width * FltX.zero,
        weight = unit.weight * FltX.zero
    )

    for (fixedPlacement in bottomUnits) {
        if (fixedPlacement.maxY eq unit.y) {
            val bottomShapePlacement = fixedPlacement.asShapePlacement3(shapeResolver)
            val overlapArea = unitShapePlacement.footprintOverlapArea(bottomShapePlacement)
            if ((overlapArea gr (FltX.zero * overlapArea.unit)) == true) {
                val bottomArea = bottomShapePlacement.footprintOverlapArea(bottomShapePlacement)
                if ((bottomArea gr (FltX.zero * bottomArea.unit)) == true) {
                    val thisSupport = BottomSupport(
                        area = overlapArea,
                        weight = (overlapArea / bottomArea).value * fixedPlacement.weight
                    )
                    support += thisSupport
                }
            }
        }
    }
    return support
}
