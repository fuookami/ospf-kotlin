/**
 * 二维空间区域。 / 2D space region.
 *
 * 对应 BPP3D 的 `Space`，裁剪为二维：一个矩形空区域，可按「沿 X 切分」或「沿 Y 切分」递归拆分为
 * 剩余空间，供块生成与空间切分搜索使用。
 * / Mirrors BPP3D `Space` reduced to 2D: a rectangular free region that can be recursively split along X
 * or Y into remainder spaces for block generation and space-splitting search.
*/
package fuookami.ospf.kotlin.framework.bpp2d.domain.block_loading.model

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.geometry2d.*
import fuookami.ospf.kotlin.framework.bpp2d.infrastructure.*

/**
 * 二维空间区域。 / 2D space region.
 *
 * @param V 数值类型 / numeric type
 * @property x 左下角 x 坐标 / lower-left x coordinate
 * @property y 左下角 y 坐标 / lower-left y coordinate
 * @property width 宽度 / width
 * @property height 高度 / height
*/
data class Space2<V : FloatingNumber<V>>(
    val x: Quantity<V>,
    val y: Quantity<V>,
    val width: Quantity<V>,
    val height: Quantity<V>
) {

    /** 区域面积 / region area */
    val area: Quantity<V> get() = width * height

    /**
     * 判断给定包围尺寸能否放入本区域。 / Check whether a bounding size fits the region.
     *
     * @param boxWidth 包围宽度 / bounding width
     * @param boxHeight 包围高度 / bounding height
     * @return 是否可容纳 / whether it fits
    */
    fun enabled(boxWidth: Quantity<V>, boxHeight: Quantity<V>): Boolean {
        return quantityLessOrEqual(boxWidth, width) && quantityLessOrEqual(boxHeight, height)
    }

    /**
     * 判断给定装载形状能否放入本区域。 / Check whether a packing shape fits the region.
     *
     * @param shape 装载形状 / packing shape
     * @return 是否可容纳 / whether it fits
    */
    fun enabled(shape: PackingShape2<V>): Boolean {
        return enabled(shape.boundingWidth, shape.boundingHeight)
    }

    /**
     * 沿 X 方向切出左侧一块，返回左侧块与剩余区域。 / Cut a left-hand piece along X, returning the piece and
     * the remainder.
     *
     * @param cutWidth 切出宽度 / cut width
     * @return 切出区域与剩余区域 / the cut region and the remainder
    */
    fun splitX(cutWidth: Quantity<V>): Pair<Space2<V>, Space2<V>> {
        val left = Space2(
            x = x,
            y = y,
            width = cutWidth,
            height = height
        )
        val right = Space2(
            x = x + cutWidth,
            y = y,
            width = width - cutWidth,
            height = height
        )
        return left to right
    }

    /**
     * 沿 Y 方向切出底部一块，返回底部块与剩余区域。 / Cut a bottom piece along Y, returning the piece and the
     * remainder.
     *
     * @param cutHeight 切出高度 / cut height
     * @return 切出区域与剩余区域 / the cut region and the remainder
    */
    fun splitY(cutHeight: Quantity<V>): Pair<Space2<V>, Space2<V>> {
        val bottom = Space2(
            x = x,
            y = y,
            width = width,
            height = cutHeight
        )
        val top = Space2(
            x = x,
            y = y + cutHeight,
            width = width,
            height = height - cutHeight
        )
        return bottom to top
    }

    /**
     * 判断区域是否已退化（宽或高非正）。 / Check whether the region is degenerate (non-positive width or height).
     *
     * @return 是否退化 / whether degenerate
    */
    fun isDegenerate(): Boolean {
        return quantityIsNonPositive(width) || quantityIsNonPositive(height)
    }
}

/**
 * 块：一组放置连同其包围区域。 / Block: a group of placements together with their bounding region.
 *
 * @param V 数值类型 / numeric type
 * @property space 块的包围区域 / bounding region of the block
 * @property placements 块内放置 / placements inside the block
 * @property source 生成来源 / generation source
*/
data class Block2<V : FloatingNumber<V>>(
    val space: Space2<V>,
    val placements: List<Placement2<V>>,
    val source: String
) {

    /** 块内件数 / amount inside the block */
    val amount: Int get() = placements.size

    /** 块内实际占用面积 / actually occupied area inside the block */
    val area: Quantity<V> = quantitySum(placements.map { it.shape.area })
        ?: quantityZeroOf(space.width)

    /** 块的利用率 / block utilisation */
    val utilization: V
        get() {
            if (!quantityIsPositive(space.area)) {
                return space.width.value.constants.zero
            }
            return area.value / space.area.value
        }

    /** 块内件数映射 / item amount map inside the block */
    val itemAmounts: Map<String, Int>
        get() = placements
            .mapNotNull { it.itemId }
            .groupingBy { it }
            .eachCount()
}