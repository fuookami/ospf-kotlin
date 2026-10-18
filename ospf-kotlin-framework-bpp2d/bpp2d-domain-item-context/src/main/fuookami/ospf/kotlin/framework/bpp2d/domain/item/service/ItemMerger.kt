/**
 * 物品合并服务。 / Item merger service.
 *
 * 对应 BPP3D 的 `ItemMerger`：把形状、旋转许可与批次都相同的物品合并为一条需求，减少候选列规模。
 * 二维侧以形状（宽高或半径）与旋转许可作为合并判据。
 * / Mirrors BPP3D `ItemMerger`: items with identical shape, rotation allowance and batch are merged into a
 * single demand, shrinking the candidate column space. 2D uses the shape (width/height or radius) and the
 * rotation allowance as the merge criteria.
*/
package fuookami.ospf.kotlin.framework.bpp2d.domain.item.service

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.framework.geometry2d.*
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.*
import fuookami.ospf.kotlin.framework.bpp2d.infrastructure.*

/**
 * 物品去重键：形状、旋转许可与批次的组合。 / Item deduplication key: shape, rotation allowance and batch.
 *
 * @param V 数值类型 / numeric type
 * @property shape 装载形状 / packing shape
 * @property allowRotate 是否允许旋转 90 度 / whether rotation is allowed
 * @property batchNo 批次号 / batch number
*/
data class ItemMergeKey<V : FloatingNumber<V>>(
    val shape: PackingShape2<V>,
    val allowRotate: Boolean,
    val batchNo: BatchNo?
)

/**
 * 物品合并服务。 / Item merger service.
 *
 * @param V 数值类型 / numeric type
*/
class ItemMerger<V : FloatingNumber<V>> {

    /**
     * 合并同形状物品的件数需求。 / Merge amount demands of identically shaped items.
     *
     * 合并键相同的物品会被累加件数；代表物品取第一个出现的实例。 / Items sharing a merge key accumulate
     * their amounts; the representative is the first occurrence.
     *
     * @param items 物品与件数 / items and their amounts
     * @return 合并后的物品与件数 / merged items and amounts
    */
    fun merge(items: List<Pair<ActualItem<V>, UInt64>>): List<Pair<ActualItem<V>, UInt64>> {
        val merged = LinkedHashMap<ItemMergeKey<V>, Pair<ActualItem<V>, UInt64>>()
        for ((item, amount) in items) {
            val key = ItemMergeKey(
                shape = item.shape,
                allowRotate = item.allowRotate,
                batchNo = item.batchNo
            )
            val existing = merged[key]
            merged[key] = if (existing == null) {
                item to amount
            } else {
                existing.first to (existing.second + amount)
            }
        }
        return merged.values.toList()
    }

    /**
     * 按形状分组物品，保持首次出现顺序。 / Group items by shape, preserving first-occurrence order.
     *
     * @param items 物品列表 / item list
     * @return 形状分组 / shape groups
    */
    fun groupByShape(items: List<ActualItem<V>>): List<List<ActualItem<V>>> {
        val groups = LinkedHashMap<ItemMergeKey<V>, MutableList<ActualItem<V>>>()
        for (item in items) {
            val key = ItemMergeKey(
                shape = item.shape,
                allowRotate = item.allowRotate,
                batchNo = item.batchNo
            )
            groups.getOrPut(key) { ArrayList() }.add(item)
        }
        return groups.values.map { it.toList() }
    }
}