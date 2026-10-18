/**
 * 放置工厂。 / Placement factory.
 *
 * 统一二维放置的构造：把物品形状与旋转标记、落点组合为 `Placement2`，处理旋转 90° 时的宽高互换，
 * 并保留物品编号以支持层内件数统计。
 * / Centralises 2D placement construction: combines an item shape, rotation flag and position into a
 * `Placement2`, swapping width/height on a 90-degree rotation and keeping the item identifier for
 * layer amount statistics.
*/
package fuookami.ospf.kotlin.framework.bpp2d.domain.item.model

import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.geometry2d.*
import fuookami.ospf.kotlin.framework.bpp2d.infrastructure.*

/**
 * 由物品与落点构造放置。 / Build a placement from an item and a position.
 *
 * @param V 数值类型 / numeric type
 * @param item 物品 / item
 * @param position 左下角落点 / lower-left position
 * @param rotated 是否旋转 90 度 / whether rotated by 90 degrees
 * @return 放置或失败（旋转未获许可时失败） / placement, or failure when rotation is not allowed
*/
fun <V : FloatingNumber<V>> itemPlacement2Of(
    item: AbstractItem<V>,
    position: QuantityPoint2<V>,
    rotated: Boolean = false
): Ret<Placement2<V>> {
    if (rotated && !item.allowRotate) {
        return Failed(
            Bpp2dValidationError("物品 ${item.id} 未允许旋转 / Item ${item.id} does not allow rotation")
        )
    }
    val shape = if (rotated) item.shape.rotated() else item.shape
    return ok(
        Placement2(
            shape = shape,
            position = position,
            rotated = rotated,
            itemId = item.id.value
        )
    )
}

/**
 * 由装载形状与落点构造放置。 / Build a placement from a packing shape and a position.
 *
 * @param V 数值类型 / numeric type
 * @param shape 装载形状 / packing shape
 * @param position 左下角落点 / lower-left position
 * @param rotated 是否旋转 90 度 / whether rotated by 90 degrees
 * @param itemId 物品编号 / item identifier
 * @return 放置 / placement
*/
fun <V : FloatingNumber<V>> shapePlacement2Of(
    shape: PackingShape2<V>,
    position: QuantityPoint2<V>,
    rotated: Boolean = false,
    itemId: ItemId? = null
): Placement2<V> {
    return Placement2(
        shape = shape,
        position = position,
        rotated = rotated,
        itemId = itemId?.value
    )
}

/**
 * 构造与放置同形状、同旋转状态的图样放置。 / Build a pattern placement sharing the shape and rotation state.
 *
 * @param V 数值类型 / numeric type
 * @param template 模板放置 / template placement
 * @param position 新的左下角落点 / new lower-left position
 * @return 放置 / placement
*/
fun <V : FloatingNumber<V>> Placement2<V>.placedAt(position: QuantityPoint2<V>): Placement2<V> {
    return copy(position = position)
}

/**
 * 以零点为原点构造落点。 / Build a position anchored at the origin.
 *
 * @param V 数值类型 / numeric type
 * @param sample 提供单位与数值类型的样本量 / sample quantity providing unit and numeric type
 * @return 原点落点 / origin position
*/
fun <V : FloatingNumber<V>> originPoint2Of(sample: Quantity<V>): QuantityPoint2<V> {
    val zero = quantityZeroOf(sample)
    return QuantityPoint2(x = zero, y = zero)
}