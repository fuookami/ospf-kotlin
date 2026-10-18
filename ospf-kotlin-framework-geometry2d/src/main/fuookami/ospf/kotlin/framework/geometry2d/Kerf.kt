/**
 * 刀缝与间距。 / Kerf and spacing.
 *
 * 排样中的 kerf（刀缝/锯缝）要求相邻件之间保留最小间隙。实现方式是**放大参与重叠判定的形状**：把一件的
 * 覆盖形状按 kerf 膨胀后与另一件的真实形状比较，若仍相交则说明真实间隙小于 kerf。这样无需在每个放置上
 * 记录额外偏移，坐标仍是真实坐标。
 * / Kerf (saw blade width) requires a minimum gap between neighbouring pieces. This is implemented by
 * **inflating the shape used in the overlap test**: inflating one piece's footprint by the kerf and comparing it
 * with the other piece's real shape reports intersection exactly when the real gap is smaller than the kerf.
 * Placement coordinates therefore stay physical and no extra offset has to be stored.
*/
package fuookami.ospf.kotlin.framework.geometry2d

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity

/**
 * 按间距膨胀装载形状。 / Inflate a packing shape by a spacing.
 *
 * 矩形件宽高各加 `spacing`；圆形件直径加 `spacing`，即半径加 `spacing / 2`。
 * / A rectangle grows by `spacing` in both width and height; a circle grows by `spacing` in diameter, i.e.
 * `spacing / 2` in radius.
 *
 * @param V 数值类型 / numeric type
 * @param spacing 间距，必须非负 / spacing, must be non-negative
 * @return 膨胀后的形状 / inflated shape
*/
fun <V : FloatingNumber<V>> PackingShape2<V>.inflatedBy(spacing: Quantity<V>): PackingShape2<V> {
    if (!quantityIsPositive(spacing)) {
        return this
    }
    return when (val concrete = this) {
        is RectanglePackingShape2 -> RectanglePackingShape2(
            width = concrete.width + spacing,
            height = concrete.height + spacing
        )

        is CirclePackingShape2 -> CirclePackingShape2(
            radius = concrete.radius + (spacing / spacing.value.constants.two)
        )
    }
}

/**
 * 按间距膨胀放置的形状，位置保持不变。 / Inflate the shape of a placement, keeping its position.
 *
 * @param V 数值类型 / numeric type
 * @param spacing 间距 / spacing
 * @return 膨胀后的放置 / inflated placement
*/
fun <V : FloatingNumber<V>> Placement2<V>.inflatedBy(spacing: Quantity<V>): Placement2<V> {
    return copy(shape = shape.inflatedBy(spacing))
}

/**
 * 计算考虑刀缝后的节距。 / Compute the pitch including the kerf.
 *
 * 规则网格按节距排布时，节距等于包围尺寸加 kerf 即可让真实间隙恰为 kerf。
 * / When a regular grid uses a pitch equal to the bounding size plus the kerf, the real gap becomes exactly the
 * kerf.
 *
 * @param V 数值类型 / numeric type
 * @param shape 装载形状 / packing shape
 * @param spacing 间距 / spacing
 * @return 水平与垂直节距 / horizontal and vertical pitch
*/
fun <V : FloatingNumber<V>> pitchOf(
    shape: PackingShape2<V>,
    spacing: Quantity<V>
): QuantityVector2<V> {
    val effective = if (quantityIsPositive(spacing)) spacing else quantityZeroOf(shape.boundingWidth)
    return QuantityVector2(
        x = shape.boundingWidth + effective,
        y = shape.boundingHeight + effective
    )
}

/**
 * 由样本构造零间距。 / Build a zero spacing from a sample.
 *
 * @param V 数值类型 / numeric type
 * @param sample 样本量值 / sample quantity
 * @return 零间距 / zero spacing
*/
fun <V : FloatingNumber<V>> zeroSpacingOf(sample: Quantity<V>): Quantity<V> {
    return quantityZeroOf(sample)
}
