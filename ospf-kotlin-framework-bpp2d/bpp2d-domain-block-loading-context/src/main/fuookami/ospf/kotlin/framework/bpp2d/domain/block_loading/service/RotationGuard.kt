/**
 * 旋转守卫。 / Rotation guard.
 *
 * 与 BPP3D 的能力守卫对应：生成旋转候选前必须确认物品允许旋转，否则返回失败而不是默默生成非法候选。
 * 圆形件旋转不改变形状，视为始终允许。
 * / Mirrors the BPP3D capability guard: before generating a rotated candidate the item must allow rotation,
 * otherwise the call fails instead of silently producing an illegal candidate. Circle items are unchanged by
 * rotation and are always allowed.
*/
package fuookami.ospf.kotlin.framework.bpp2d.domain.block_loading.service

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.AbstractItem
import fuookami.ospf.kotlin.framework.geometry2d.isCircle

/**
 * 校验物品是否允许进入旋转候选生成。 / Validate that an item may enter rotated candidate generation.
 *
 * @param V 数值类型 / numeric type
 * @param item 物品 / item
 * @return 允许时成功，否则失败 / success when allowed, otherwise failure
*/
fun <V : FloatingNumber<V>> requireRotationAllowed(item: AbstractItem<V>): Try {
    if (item.shape.isCircle()) {
        return ok
    }
    if (!item.allowRotate) {
        return Failed(
            ErrorCode.IllegalArgument,
            "物品 ${item.id} 未允许旋转，不能生成旋转候选 / Item ${item.id} does not allow rotation; rotated candidates are rejected"
        )
    }
    return ok
}