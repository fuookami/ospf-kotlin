/**
 * 二维装载放置。 / 2D layer placement.
 *
 * 对应 BPP3D 的 `QuantityPlacement2`：一个装载形状落在板材坐标系中的位置。所有几何判定都返回
 * [Ret]，不使用非空断言。
 * / Mirrors BPP3D `QuantityPlacement2`: a packing shape positioned on a sheet.
 * All geometry checks return [Ret] with no non-null assertions.
*/
package fuookami.ospf.kotlin.framework.bpp2d.infrastructure

import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity

/**
 * 二维装载放置。 / 2D layer placement.
 *
 * @param V 数值类型 / numeric type
 * @property shape 装载形状 / packing shape
 * @property position 左下角坐标 / lower-left position
 * @property rotated 是否旋转 90 度 / whether rotated by 90 degrees
 * @property itemId 物品标识（纯文本），层内件数统计依赖此字段 / plain item identifier used for layer amount statistics
*/
data class LayerPlacement2<V : FloatingNumber<V>>(
    val shape: PackingShape2<V>,
    val position: QuantityPoint2<V>,
    val rotated: Boolean = false,
    val itemId: String? = null
) : Copyable<LayerPlacement2<V>> {

    /** x 坐标 / x coordinate */
    val x: Quantity<V> get() = position.x

    /** y 坐标 / y coordinate */
    val y: Quantity<V> get() = position.y

    /** 包围宽度 / bounding width */
    val width: Quantity<V> get() = shape.boundingWidth

    /** 包围高度 / bounding height */
    val height: Quantity<V> get() = shape.boundingHeight

    /** 实际占用面积 / actual occupied area */
    val area: Quantity<V> get() = shape.area

    /**
     * 右上角 x 坐标。 / Top-right x coordinate.
     *
     * @return x 坐标或失败 / x coordinate, or failure
    */
    fun maxX(): Ret<Quantity<V>> {
        return quantityPlusSafe(x, width)
    }

    /**
     * 右上角 y 坐标。 / Top-right y coordinate.
     *
     * @return y 坐标或失败 / y coordinate, or failure
    */
    fun maxY(): Ret<Quantity<V>> {
        return quantityPlusSafe(y, height)
    }

    /**
     * 生成放置对应的包围盒。 / Build the bounding box of this placement.
     *
     * @return 包围盒 / bounding box
    */
    fun box(): Box2Geometry<V> {
        return Box2Geometry(
            x = x,
            y = y,
            shape = shape.geometry()
        )
    }

    /**
     * 判断与另一放置是否重叠。 / Check overlap with another placement.
     *
     * @param rhs 另一放置 / other placement
     * @return 是否重叠或失败 / whether they overlap, or failure
    */
    fun overlapped(rhs: LayerPlacement2<V>): Ret<Boolean> {
        return box().overlapped(rhs.box())
    }

    /**
     * 判断本放置是否完全位于给定板材内。 / Check whether this placement lies entirely inside the given sheet.
     *
     * @param sheet 板材形状 / sheet shape
     * @return 是否位于内部或失败 / whether inside, or failure
    */
    fun insideSheet(sheet: Sheet2Geometry<V>): Ret<Boolean> {
        return box().inside(sheet.originBox())
    }

    /**
     * 平移放置。 / Translate the placement.
     *
     * @param offset 偏移向量 / offset vector
     * @return 平移后的放置 / translated placement
    */
    fun translate(offset: QuantityVector2<V>): LayerPlacement2<V> {
        return copy(position = position + offset)
    }

    override fun copy(): LayerPlacement2<V> {
        return LayerPlacement2(
            shape = shape,
            position = QuantityPoint2(x = position.x, y = position.y),
            rotated = rotated,
            itemId = itemId
        )
    }
}
