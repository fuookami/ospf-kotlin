/**
 * 排样程序模型。 / Packing program model.
 *
 * 对应 BPP3D 的 `Pattern`：一个排样程序描述在给定板材上重复某种形状组合的方式，作为候选列生成的
 * 图样来源。二维侧按「每行可放数量 × 行数」描述。
 * / Mirrors BPP3D `Pattern`: a packing program describes how a shape combination repeats on a sheet and
 * feeds pattern-based candidate generation. The 2D form is described by "per-row count × row count".
*/
package fuookami.ospf.kotlin.framework.bpp2d.domain.item.model

import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64

/**
 * 排样程序：描述一种在板材上重复排布的形状组合。 / Packing program: a repeated shape combination on a sheet.
 *
 * @param V 数值类型 / numeric type
 * @property id 程序编号 / program identifier
 * @property item 参与排布的物品 / item taking part in the layout
 * @property perRowAmount 每行件数 / amount per row
 * @property rowAmount 行数 / row count
 * @property rotated 是否旋转 90 度排布 / whether the layout is rotated by 90 degrees
*/
data class PackingProgram<V : FloatingNumber<V>>(
    val id: String,
    val item: AbstractItem<V>,
    val perRowAmount: UInt64,
    val rowAmount: UInt64,
    val rotated: Boolean = false
) : Copyable<PackingProgram<V>> {

    /** 程序总件数 / total amount of the program */
    val amount: UInt64 = perRowAmount * rowAmount

    override fun copy(): PackingProgram<V> {
        return copy()
    }

    override fun toString(): String = "$id: ${perRowAmount}×${rowAmount}"
}

/**
 * 由物品生成基础排样程序。 / Build the base packing programs of an item.
 *
 * 对允许旋转的矩形件，同时给出不旋转与旋转两种程序，供候选生成对比。 / Rotatable rectangles produce both
 * upright and rotated programs so candidate generation can compare them.
 *
 * @param V 数值类型 / numeric type
 * @param item 物品 / item
 * @param perRowAmount 每行件数 / amount per row
 * @param rowAmount 行数 / row count
 * @return 排样程序列表 / packing program list
*/
fun <V : FloatingNumber<V>> packingProgramsOf(
    item: AbstractItem<V>,
    perRowAmount: UInt64,
    rowAmount: UInt64
): List<PackingProgram<V>> {
    val upright = PackingProgram(
        id = "${item.id}-upright",
        item = item,
        perRowAmount = perRowAmount,
        rowAmount = rowAmount,
        rotated = false
    )
    if (!item.allowRotate) {
        return listOf(upright)
    }
    return listOf(
        upright,
        PackingProgram(
            id = "${item.id}-rotated",
            item = item,
            perRowAmount = perRowAmount,
            rowAmount = rowAmount,
            rotated = true
        )
    )
}
