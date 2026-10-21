/**
 * 圆形件网格生成器。 / Circle grid generators.
 *
 * 与 BPP3D 的 circle-packing 策略对应：圆形件不做通用密排优化，而是生成两种规则网格候选——
 * 直角网格（按直径铺格）与六角网格（行距约 `√3/2·d`、奇偶行错半格）。生成后仍用底层几何逐对复核，
 * 复核不通过的候选直接丢弃，不做近似。
 * / Mirrors the BPP3D circle-packing strategy: circle items are not optimised by a general dense-packing
 * solver; instead two regular grid candidates are generated — a square grid (cells of one diameter) and a
 * hexagonal grid (row pitch about `√3/2·d` with alternate rows offset by half a cell). Every candidate is
 * then re-validated pairwise against the underlying geometry, and failures are discarded rather than
 * approximated.
*/
package fuookami.ospf.kotlin.framework.bpp2d.domain.layer_generation.service

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.geometry2d.*
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.ActualItem
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.isCircleItem
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.SheetType
import fuookami.ospf.kotlin.framework.bpp2d.infrastructure.*

/**
 * 圆形网格布局模式。 / Circle grid layout mode.
*/
enum class CircleGridMode {
    /** 直角网格，按直径铺格 / square grid using one diameter per cell */
    SquareGrid,

    /** 六角网格，行距约 `√3/2·d` 且错行 / hexagonal grid with a row pitch of about `√3/2·d` and offset rows */
    HexGrid
}

/**
 * 圆形件网格布局结果。 / Circle grid layout result.
 *
 * @param V 数值类型 / numeric type
 * @property placements 放置列表 / placement list
 * @property mode 布局模式 / layout mode
 * @property itemId 物品编号 / item identifier
*/
data class CircleGridLayout<V : FloatingNumber<V>>(
    val placements: List<Placement2<V>>,
    val mode: CircleGridMode,
    val itemId: String
)

/**
 * 圆形件网格生成器。 / Circle grid generator.
 *
 * 六角行距取 `26/30·d`，略大于 `√3/2 ≈ 0.866025`。取偏大的行距是**保守**的：行与行之间比理论值更远，
 * 因此不会产生重叠，同时仍比直角网格更密。这样既避免了对泛型数值类型求平方根，也保持了正确性。
 * / The hexagonal row pitch uses `26/30·d`, slightly larger than `√3/2 ≈ 0.866025`. Choosing a larger pitch
 * is conservative: rows are farther apart than the theoretical value, so overlap cannot occur while density
 * still beats the square grid. This avoids square roots on a generic numeric type without losing
 * correctness.
*/
class CircleGridGenerator {

    /**
     * 为单个圆形件生成全部网格候选。 / Generate every grid candidate for a single circle item.
     *
     * @param V 数值类型 / numeric type
     * @param item 圆形件 / circle item
     * @param sheet 板材类型 / sheet type
     * @return 网格布局列表，按件数降序 / grid layout list, ordered by descending amount
    */
    fun <V : FloatingNumber<V>> invoke(
        item: ActualItem<V>,
        sheet: SheetType<V>,
        kerf: Quantity<V>? = null
    ): List<CircleGridLayout<V>> {
        if (!item.isCircleItem()) {
            return emptyList()
        }
        val circle = item.shape as? CirclePackingShape2<V> ?: return emptyList()
        val diameter = circle.diameter
        if (!quantityIsPositive(diameter)) {
            return emptyList()
        }
        // 圆形件的刀缝同样通过节距体现：节距 = 直径 + kerf。
        // For circles the kerf is also expressed through the pitch: pitch = diameter + kerf.
        val spacing = if (kerf != null && quantityIsPositive(kerf)) kerf else quantityZeroOf(diameter)
        val layouts = ArrayList<CircleGridLayout<V>>()
        squareGrid(item, circle, diameter, spacing, sheet)?.let { layouts.add(it) }
        hexGrid(item, circle, diameter, spacing, sheet)?.let { layouts.add(it) }
        return layouts.sortedByDescending { layout -> layout.placements.size }
    }

    /**
     * 生成直角网格布局。 / Generate the square grid layout.
     *
     * @param V 数值类型 / numeric type
     * @param item 圆形件 / circle item
     * @param circle 圆形形状 / circle shape
     * @param diameter 直径 / diameter
     * @param sheet 板材类型 / sheet type
     * @return 网格布局，无可行放置时返回 null / grid layout, or null when nothing fits
    */
    private fun <V : FloatingNumber<V>> squareGrid(
        item: ActualItem<V>,
        circle: CirclePackingShape2<V>,
        diameter: Quantity<V>,
        spacing: Quantity<V>,
        sheet: SheetType<V>
    ): CircleGridLayout<V>? {
        val pitch = diameter + spacing
        if (!quantityIsPositive(pitch)) {
            return null
        }
        val cols = countFitting(sheet.width, pitch)
        val rows = countFitting(sheet.height, pitch)
        if (cols <= 0 || rows <= 0) {
            return null
        }
        val placements = ArrayList<Placement2<V>>(cols * rows)
        for (row in 0 until rows) {
            val y = scaledQuantity(pitch, row)
            for (col in 0 until cols) {
                val x = scaledQuantity(pitch, col)
                placements.add(
                    Placement2(
                        shape = circle,
                        position = QuantityPoint2(x = x, y = y),
                        rotated = false,
                        itemId = item.id.value
                    )
                )
            }
        }
        return CircleGridLayout(
            placements = placements,
            mode = CircleGridMode.SquareGrid,
            itemId = item.id.value
        )
    }

    /**
     * 生成六角网格布局。 / Generate the hexagonal grid layout.
     *
     * @param V 数值类型 / numeric type
     * @param item 圆形件 / circle item
     * @param circle 圆形形状 / circle shape
     * @param diameter 直径 / diameter
     * @param sheet 板材类型 / sheet type
     * @return 网格布局，无可行放置时返回 null / grid layout, or null when nothing fits
    */
    private fun <V : FloatingNumber<V>> hexGrid(
        item: ActualItem<V>,
        circle: CirclePackingShape2<V>,
        diameter: Quantity<V>,
        spacing: Quantity<V>,
        sheet: SheetType<V>
    ): CircleGridLayout<V>? {
        val pitch = diameter + spacing
        if (!quantityIsPositive(pitch)) {
            return null
        }
        // 六角网格的行距与列距都按「直径 + kerf」推导，保证任意相邻件的真实间隙不小于 kerf。
        // Both the row and column pitch of the hexagonal grid derive from diameter plus kerf, so every pair of
        // neighbours keeps a real gap of at least the kerf.
        val rowPitch = hexRowPitch(pitch)
        if (!quantityIsPositive(rowPitch)) {
            return null
        }
        val halfPitch = pitch / pitch.value.constants.two

        val placements = ArrayList<Placement2<V>>()
        var row = 0
        while (row < MAX_HEX_ROWS) {
            val y = scaledQuantity(rowPitch, row)
            if (quantityGreater(y + pitch, sheet.height)) {
                break
            }
            val rowOffset = if (row % 2 == 0) quantityZeroOf(pitch) else halfPitch
            val availableWidth = sheet.width - rowOffset
            val cols = countFitting(availableWidth, pitch)
            if (cols > 0) {
                for (col in 0 until cols) {
                    val x = rowOffset + scaledQuantity(pitch, col)
                    placements.add(
                        Placement2(
                            shape = circle,
                            position = QuantityPoint2(x = x, y = y),
                            rotated = false,
                            itemId = item.id.value
                        )
                    )
                }
            }
            row += 1
        }
        if (placements.isEmpty()) {
            return null
        }
        return CircleGridLayout(
            placements = placements,
            mode = CircleGridMode.HexGrid,
            itemId = item.id.value
        )
    }

    /**
     * 计算六角网格行距 `26/30·d`。 / Compute the hexagonal row pitch `26/30·d`.
     *
     * 必须用**标量比** `26/30` 去缩放直径量，而不是「26d / 30d」：后者得到一个无量纲的 0.8667，
     * 只在 `d = 1` 时碰巧正确。修正前在 10×8 板材、直径 2 的圆形件上，相邻圆心距仅 1.32（小于直径 2），
     * 会实际产出重叠的圆。
     * / The diameter must be scaled by the **scalar ratio** `26/30`, not written as "26d / 30d": the latter
     * yields the dimensionless 0.8667 and is only accidentally correct when `d = 1`. Before the fix, on a
     * 10×8 sheet with diameter 2, adjacent centres were only 1.32 apart (below the diameter 2), which actually
     * produced overlapping circles.
     *
     * @param V 数值类型 / numeric type
     * @param diameter 直径 / diameter
     * @return 行距 / row pitch
    */
    private fun <V : FloatingNumber<V>> hexRowPitch(diameter: Quantity<V>): Quantity<V> {
        if (!quantityIsPositive(diameter)) {
            return quantityZeroOf(diameter)
        }
        // 用标量比 26/30 缩放直径量：结果仍是长度量，等于 0.8667·d。
        // Scale the diameter by the scalar ratio 26/30: the result is still a length and equals 0.8667·d.
        val numerator = integerScalarOf(diameter.value, HEX_PITCH_NUMERATOR)
        val denominator = integerScalarOf(diameter.value, HEX_PITCH_DENOMINATOR)
        if (!quantityIsPositive(Quantity(denominator, diameter.unit))) {
            return quantityZeroOf(diameter)
        }
        // `Quantity * 标量` 保留单位；标量比 26/30 因此把直径量缩放为 0.8667·d。
        // `Quantity * scalar` keeps the unit, so the scalar ratio 26/30 scales the diameter to 0.8667·d.
        return (diameter * (numerator / denominator)) ?: quantityZeroOf(diameter)
    }

    /**
     * 由样本数值构造整数标量。 / Build an integer scalar from the sample value.
     *
     * @param V 数值类型 / numeric type
     * @param sample 样本数值 / sample value
     * @param times 倍数 / multiplier
     * @return 标量 / scalar
    */
    private fun <V : FloatingNumber<V>> integerScalarOf(sample: V, times: Int): V {
        var acc = sample.constants.zero
        for (index in 0 until times) {
            acc += sample.constants.one
        }
        return acc
    }

    companion object {
        /** 六角网格的最大行数，避免病态输入造成的长时间循环。 / Maximum hexagonal grid rows, guarding pathological inputs. */
        private const val MAX_HEX_ROWS = 10000

        /** 六角行距分子：26 / Hexagonal pitch numerator: 26. */
        private const val HEX_PITCH_NUMERATOR = 26

        /** 六角行距分母：30（26/30 略大于 √3/2，保守且无重叠）/ Hexagonal pitch denominator: 30 (26/30 slightly exceeds √3/2, conservative and overlap-free). */
        private const val HEX_PITCH_DENOMINATOR = 30
    }
}