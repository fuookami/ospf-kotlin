/**
 * 二维装载形状抽象。 / 2D packing shape abstractions.
 *
 * 对应 BPP3D 的 `PackingShape3`（Cuboid + Cylinder），二维侧提供矩形与圆形两种形状。
 * 面积口径：`area` 是**实际占用面积**（圆为 `πr²`，矩形为 `w·h`），`boundingArea` 是**外接矩形面积**。
 * / Mirrors BPP3D `PackingShape3` (Cuboid + Cylinder) with rectangle and circle shapes.
 * `area` is the actual occupied area (`πr²` for circles, `w·h` for rectangles) while `boundingArea`
 * is the enclosing rectangle area.
*/
package fuookami.ospf.kotlin.framework.bpp2d.infrastructure

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.geometry.QuantityCircle2
import fuookami.ospf.kotlin.math.geometry.QuantityProjection2
import fuookami.ospf.kotlin.math.geometry.QuantityRectangle2
import fuookami.ospf.kotlin.quantities.quantity.Quantity

/**
 * 装载形状类型。 / Packing shape type.
*/
enum class PackingShape2Type {
    /** 矩形件 / rectangle item */
    Rectangle,

    /** 圆形件 / circular item */
    Circle
}

/**
 * 二维装载形状接口，提供包围尺寸、实际面积与外接面积。 / 2D packing shape providing bounding size, actual area and bounding area.
 *
 * 声明为 sealed，使调用方可以穷尽分支而不需要 `else`。 / Sealed so callers can exhaust the branches
 * without an `else`.
 *
 * @param V 数值类型 / numeric type
*/
sealed interface PackingShape2<V : FloatingNumber<V>> {

    /** 形状类型 / shape type */
    val shapeType: PackingShape2Type

    /** 包围宽度 / bounding width */
    val boundingWidth: Quantity<V>

    /** 包围高度 / bounding height */
    val boundingHeight: Quantity<V>

    /** 实际占用面积 / actual occupied area */
    val area: Quantity<V>

    /** 外接矩形面积 / enclosing rectangle area */
    val boundingArea: Quantity<V>

    /**
     * 转换为基础二维几何投影。 / Convert to the base 2D geometry projection.
     *
     * @return 二维几何投影 / 2D geometry projection
    */
    fun geometry(): QuantityProjection2<V>

    /**
     * 生成旋转 90 度后的形状。圆形件旋转后不变。 / Produce the shape rotated by 90 degrees. Circle shapes are unchanged.
     *
     * @return 旋转后的形状 / rotated shape
    */
    fun rotated(): PackingShape2<V>
}

/**
 * 矩形装载形状。 / Rectangle packing shape.
 *
 * @param V 数值类型 / numeric type
 * @property width 宽度 / width
 * @property height 高度 / height
*/
data class RectanglePackingShape2<V : FloatingNumber<V>>(
    val width: Quantity<V>,
    val height: Quantity<V>
) : PackingShape2<V> {
    override val shapeType: PackingShape2Type = PackingShape2Type.Rectangle
    override val boundingWidth: Quantity<V> get() = width
    override val boundingHeight: Quantity<V> get() = height

    override val area: Quantity<V> = width * height
    override val boundingArea: Quantity<V> get() = area

    override fun geometry(): QuantityProjection2<V> {
        return QuantityRectangle2(
            width = width,
            height = height
        )
    }

    override fun rotated(): PackingShape2<V> {
        return RectanglePackingShape2(
            width = height,
            height = width
        )
    }

    /**
     * 判断矩形旋转 90 度是否得到不同形状。 / Check whether a 90-degree rotation produces a different shape.
     *
     * @return 宽高是否不等 / whether width and height differ
    */
    fun isRotatable(): Boolean {
        return !quantityEqual(width, height)
    }
}

/**
 * 圆形装载形状。 / Circle packing shape.
 *
 * @param V 数值类型 / numeric type
 * @property radius 半径 / radius
*/
data class CirclePackingShape2<V : FloatingNumber<V>>(
    val radius: Quantity<V>
) : PackingShape2<V> {
    override val shapeType: PackingShape2Type = PackingShape2Type.Circle

    /** 直径 / diameter */
    val diameter: Quantity<V> = radius * radius.value.constants.two

    override val boundingWidth: Quantity<V> get() = diameter
    override val boundingHeight: Quantity<V> get() = diameter

    override val area: Quantity<V> = (radius * radius) * radius.value.constants.pi
    override val boundingArea: Quantity<V> = diameter * diameter

    override fun geometry(): QuantityProjection2<V> {
        return QuantityCircle2(radius = radius)
    }

    override fun rotated(): PackingShape2<V> = this
}

/**
 * 判断形状是否为圆形件。 / Check whether a shape is a circle.
 *
 * @param V 数值类型 / numeric type
 * @return 是否圆形件 / whether it is a circle
*/
fun <V : FloatingNumber<V>> PackingShape2<V>.isCircle(): Boolean {
    return shapeType == PackingShape2Type.Circle
}

/**
 * 判断形状是否为矩形件。 / Check whether a shape is a rectangle.
 *
 * @param V 数值类型 / numeric type
 * @return 是否矩形件 / whether it is a rectangle
*/
fun <V : FloatingNumber<V>> PackingShape2<V>.isRectangle(): Boolean {
    return shapeType == PackingShape2Type.Rectangle
}

/**
 * 带旋转标记的候选形状。 / Candidate shape carrying its rotation flag.
 *
 * @param V 数值类型 / numeric type
 * @property shape 候选形状 / candidate shape
 * @property rotated 是否旋转 90 度 / whether rotated by 90 degrees
*/
data class Shape2Candidate<V : FloatingNumber<V>>(
    val shape: PackingShape2<V>,
    val rotated: Boolean
)

/**
 * 按允许旋转与否返回候选形状列表。圆形件与不可旋转矩形只返回自身。 / Candidate shapes honouring the rotation allowance.
 * Circle shapes and non-rotatable rectangles only return themselves.
 *
 * @param V 数值类型 / numeric type
 * @param allowRotate 是否允许旋转 / whether rotation is allowed
 * @return 候选形状列表 / candidate shape list
*/
fun <V : FloatingNumber<V>> PackingShape2<V>.candidateShapes(allowRotate: Boolean): List<Shape2Candidate<V>> {
    val upright = Shape2Candidate(
        shape = this,
        rotated = false
    )
    if (!allowRotate) {
        return listOf(upright)
    }
    val rectangle = this as? RectanglePackingShape2 ?: return listOf(upright)
    if (!rectangle.isRotatable()) {
        return listOf(upright)
    }
    return listOf(
        upright,
        Shape2Candidate(
            shape = rectangle.rotated(),
            rotated = true
        )
    )
}

/**
 * 生成矩形形状，宽高必须为正。 / Build a rectangle shape; width and height must be positive.
 *
 * @param V 数值类型 / numeric type
 * @param width 宽度 / width
 * @param height 高度 / height
 * @return 矩形形状或失败 / rectangle shape, or failure
*/
fun <V : FloatingNumber<V>> rectangleShape2(
    width: Quantity<V>,
    height: Quantity<V>
): Ret<RectanglePackingShape2<V>> {
    if (!quantityIsPositive(width) || !quantityIsPositive(height)) {
        return Failed(
            ErrorCode.IllegalArgument,
            "宽高必须为正：$width × $height / Width and height must be positive: $width × $height"
        )
    }
    return ok(RectanglePackingShape2(width = width, height = height))
}

/**
 * 生成圆形形状，半径必须为正。 / Build a circle shape; the radius must be positive.
 *
 * @param V 数值类型 / numeric type
 * @param radius 半径 / radius
 * @return 圆形形状或失败 / circle shape, or failure
*/
fun <V : FloatingNumber<V>> circleShape2(radius: Quantity<V>): Ret<CirclePackingShape2<V>> {
    if (!quantityIsPositive(radius)) {
        return Failed(
            ErrorCode.IllegalArgument,
            "半径必须为正：$radius / Radius must be positive: $radius"
        )
    }
    return ok(CirclePackingShape2(radius = radius))
}
