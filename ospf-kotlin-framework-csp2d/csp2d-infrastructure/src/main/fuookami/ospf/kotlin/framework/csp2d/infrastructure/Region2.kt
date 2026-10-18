/**
 * 二维裁切区域。 / Two-dimensional cutting region.
 *
 * 裁切与装箱的差别之一是「料」的表示：装箱关心物品放在板材的哪个位置，套裁还关心**每一刀把料切成哪两块**，
 * 因此需要一个可递归切分的矩形区域，而不是只有「板」与「件」。本类型是刀缝与断头台切分树的几何载体。
 * / One difference between cutting stock and bin packing is how material is represented: packing cares about where an
 * item sits on a sheet, while cutting stock also cares about **which two pieces each cut produces**. That needs a
 * recursively splittable rectangular region rather than only "sheet" and "piece". This type is the geometric carrier
 * of the kerf and of the guillotine cut tree.
*/
package fuookami.ospf.kotlin.framework.csp2d.infrastructure

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.geometry2d.*

/**
 * 二维裁切区域，以左下角与宽高表示。 / A 2D cutting region, expressed by its lower-left corner and size.
 *
 * @property V 数值类型 / numeric type
 * @property x 左下角 x 坐标 / lower-left x coordinate
 * @property y 左下角 y 坐标 / lower-left y coordinate
 * @property width 宽度 / width
 * @property height 高度 / height
*/
data class Region2<V : FloatingNumber<V>>(
    val x: Quantity<V>,
    val y: Quantity<V>,
    val width: Quantity<V>,
    val height: Quantity<V>
) {

    /** 面积 / area */
    val area: Quantity<V> get() = width * height

    /**
     * 右边界 x 坐标。 / Right edge x coordinate.
     *
     * @return 右边界 / right edge
    */
    fun maxX(): Quantity<V> = x + width

    /**
     * 上边界 y 坐标。 / Top edge y coordinate.
     *
     * @return 上边界 / top edge
    */
    fun maxY(): Quantity<V> = y + height

    /**
     * 是否退化（宽或高非正）。 / Whether the region is degenerate (non-positive width or height).
     *
     * @return 是否退化 / whether degenerate
    */
    fun isDegenerate(): Boolean {
        return !quantityIsPositive(width) || !quantityIsPositive(height)
    }

    /**
     * 是否完全包含另一个区域。 / Whether this region fully contains another region.
     *
     * @param rhs 另一个区域 / the other region
     * @return 是否包含 / whether contained
    */
    fun contains(rhs: Region2<V>): Boolean {
        return quantityLessOrEqual(x, rhs.x)
                && quantityLessOrEqual(y, rhs.y)
                && quantityGreaterOrEqual(maxX(), rhs.maxX())
                && quantityGreaterOrEqual(maxY(), rhs.maxY())
    }

    /**
     * 转换为几何包围盒，便于复用共享几何模块的重叠判定。 / Convert into a geometric box so that the overlap tests of
     * the shared geometry module can be reused.
     *
     * @return 矩形包围盒 / rectangle box
    */
    fun toBox(): Box2Geometry<V> {
        return Box2Geometry.of(
            x = x,
            y = y,
            width = width,
            height = height
        )
    }

    /**
     * 按刀线把区域切成两块，并扣除刀缝。 / Split the region into two by a cut line, removing the kerf.
     *
     * 刀缝是一条被切掉的窄带：竖刀在 `[position, position + kerf)`，横刀同理。因此右侧（或上侧）区域的起点是
     * `position + kerf` 而不是 `position`。
     * / The kerf is a removed narrow strip: a vertical cut occupies `[position, position + kerf)`, and likewise for a
     * horizontal cut. The start of the right (or upper) region is therefore `position + kerf`, not `position`.
     *
     * @param orientation 刀线方向 / cut line orientation
     * @param position 刀缝起始坐标 / the coordinate where the kerf strip starts
     * @param kerf 刀缝宽度 / kerf width
     * @return 切出的两个区域，顺序为「左/下」与「右/上」；无法切分时失败 /
     * the two resulting regions, ordered as left/bottom then right/top; failure when the cut is invalid
    */
    fun cut(
        orientation: CutOrientation2,
        position: Quantity<V>,
        kerf: Quantity<V>
    ): Ret<Pair<Region2<V>, Region2<V>>> {
        val effectiveKerf = if (quantityIsPositive(kerf)) kerf else quantityZeroOf(width)
        return when (orientation) {
            CutOrientation2.Vertical -> {
                val lowerStart = x
                val lowerEnd = position
                val upperStart = position + effectiveKerf
                val upperEnd = maxX()
                if (quantityLessOrEqual(lowerEnd, lowerStart) || quantityLessOrEqual(upperEnd, upperStart)) {
                    return Failed(
                        Csp2dValidationError(
                            detail = "竖刀位置非法：position=$position 无法把区域 $this 切成两块非退化部分 / " +
                                    "Invalid vertical cut position"
                        )
                    )
                }
                ok(
                    Region2(
                        x = lowerStart,
                        y = y,
                        width = lowerEnd - lowerStart,
                        height = height
                    ) to Region2(
                        x = upperStart,
                        y = y,
                        width = upperEnd - upperStart,
                        height = height
                    )
                )
            }

            CutOrientation2.Horizontal -> {
                val lowerStart = y
                val lowerEnd = position
                val upperStart = position + effectiveKerf
                val upperEnd = maxY()
                if (quantityLessOrEqual(lowerEnd, lowerStart) || quantityLessOrEqual(upperEnd, upperStart)) {
                    return Failed(
                        Csp2dValidationError(
                            detail = "横刀位置非法：position=$position 无法把区域 $this 切成两块非退化部分 / " +
                                    "Invalid horizontal cut position"
                        )
                    )
                }
                ok(
                    Region2(
                        x = x,
                        y = lowerStart,
                        width = width,
                        height = lowerEnd - lowerStart
                    ) to Region2(
                        x = x,
                        y = upperStart,
                        width = width,
                        height = upperEnd - upperStart
                    )
                )
            }
        }
    }
}

/**
 * 由坐标与尺寸构造裁切区域。 / Build a cutting region from a corner and a size.
 *
 * @param V 数值类型 / numeric type
 * @param x 左下角 x 坐标 / lower-left x coordinate
 * @param y 左下角 y 坐标 / lower-left y coordinate
 * @param width 宽度 / width
 * @param height 高度 / height
 * @return 裁切区域 / cutting region
*/
fun <V : FloatingNumber<V>> region2Of(
    x: Quantity<V>,
    y: Quantity<V>,
    width: Quantity<V>,
    height: Quantity<V>
): Region2<V> {
    return Region2(
        x = x,
        y = y,
        width = width,
        height = height
    )
}