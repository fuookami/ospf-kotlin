/**
 * 二维板材形状与容器几何。 / 2D sheet shape and container geometry.
 *
 * 板材是二维装箱的容器，对应 BPP3D 的 `Container3Shape`；二维容器只需宽、高与可用面积。
 * / A sheet is the 2D packing container mirroring BPP3D `Container3Shape`; it only needs width, height
 * and the usable area.
*/
package fuookami.ospf.kotlin.framework.bpp2d.infrastructure

import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity

/**
 * 二维板材几何接口。 / 2D sheet geometry interface.
 *
 * @param V 数值类型 / numeric type
*/
interface Sheet2Geometry<V : FloatingNumber<V>> {

    /** 板材宽度 / sheet width */
    val width: Quantity<V>

    /** 板材高度 / sheet height */
    val height: Quantity<V>

    /** 板材面积 / sheet area */
    val area: Quantity<V>
        get() = width * height

    /**
     * 判断尺寸为 `boxWidth × boxHeight` 的包围盒能否放入板材。 / Check whether a `boxWidth × boxHeight` bounding box fits the sheet.
     *
     * @param boxWidth 包围盒宽度 / bounding-box width
     * @param boxHeight 包围盒高度 / bounding-box height
     * @return 是否可容纳或失败 / whether it fits, or failure
    */
    fun enabled(
        boxWidth: Quantity<V>,
        boxHeight: Quantity<V>
    ): Ret<Boolean> {
        return ok(
            quantityLessOrEqual(boxWidth, width)
                    && quantityLessOrEqual(boxHeight, height)
        )
    }

    /**
     * 判断给定装载形状能否放入板材（按包围尺寸）。 / Check whether a packing shape fits the sheet by bounding size.
     *
     * @param shape 装载形状 / packing shape
     * @return 是否可容纳或失败 / whether it fits, or failure
    */
    fun enabled(shape: PackingShape2<V>): Ret<Boolean> {
        return enabled(
            boxWidth = shape.boundingWidth,
            boxHeight = shape.boundingHeight
        )
    }

    /**
     * 生成以原点为左下角的板材包围盒。 / Build the sheet bounding box anchored at the origin.
     *
     * @return 板材包围盒 / sheet bounding box
    */
    fun originBox(): Box2Geometry<V> {
        return Box2Geometry(
            x = quantityZeroOf(width),
            y = quantityZeroOf(height),
            shape = fuookami.ospf.kotlin.math.geometry.QuantityRectangle2(
                width = width,
                height = height
            )
        )
    }
}

/**
 * 二维板材形状实现。 / 2D sheet shape implementation.
 *
 * @param V 数值类型 / numeric type
 * @property width 板材宽度 / sheet width
 * @property height 板材高度 / sheet height
*/
data class Sheet2Shape<V : FloatingNumber<V>>(
    override val width: Quantity<V>,
    override val height: Quantity<V>
) : Sheet2Geometry<V>, Copyable<Sheet2Shape<V>> {

    override fun copy(): Sheet2Shape<V> {
        return Sheet2Shape(width = width, height = height)
    }
}

/**
 * 由宽高构造板材形状，宽高必须为正。 / Build a sheet shape; width and height must be positive.
 *
 * @param V 数值类型 / numeric type
 * @param width 板材宽度 / sheet width
 * @param height 板材高度 / sheet height
 * @return 板材形状或失败 / sheet shape, or failure
*/
fun <V : FloatingNumber<V>> sheet2Shape(
    width: Quantity<V>,
    height: Quantity<V>
): Ret<Sheet2Shape<V>> {
    if (!quantityIsPositive(width) || !quantityIsPositive(height)) {
        return Failed(
            ErrorCode.IllegalArgument,
            "板材宽高必须为正：$width × $height / Sheet width and height must be positive: $width × $height"
        )
    }
    return ok(Sheet2Shape(width = width, height = height))
}
