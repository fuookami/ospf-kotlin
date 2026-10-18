/**
 * 物品领域上下文。 / Item domain context.
 *
 * 对应 BPP3D 的 `ItemContext`：作为应用层的建模入口，暴露物品目录、板材类型与聚合能力。
 * / Mirrors BPP3D `ItemContext`: the modelling entry point for the application layer, exposing the item
 * catalogue, sheet types and aggregation.
*/
package fuookami.ospf.kotlin.framework.bpp2d.domain.item

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.framework.geometry2d.*
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.*
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.service.*
import fuookami.ospf.kotlin.framework.bpp2d.infrastructure.*

/**
 * 物品领域上下文。 / Item domain context.
 *
 * @param V 数值类型 / numeric type
 * @property items 物品目录 / item catalogue
 * @property sheetTypes 板材类型列表 / sheet type list
 * @property itemDemands 物品件数需求 / item amount demands
 * @property merger 物品合并服务 / item merger service
 * @property loadingOrderCalculator 装载顺序计算器 / loading order calculator
*/
class ItemContext<V : FloatingNumber<V>>(
    val items: Map<ItemId, ActualItem<V>> = emptyMap(),
    val sheetTypes: List<SheetType<V>> = emptyList(),
    val itemDemands: Map<ItemId, UInt64> = emptyMap(),
    val merger: ItemMerger<V> = ItemMerger(),
    val loadingOrderCalculator: LoadingOrderCalculator<V> = LoadingOrderCalculator()
) {

    /**
     * 构建聚合。 / Build the aggregation.
     *
     * @param sheetLimits 板材类型到可用数量，null 表示不限 / sheet type to available amount, null meaning unlimited
     * @return 聚合 / aggregation
    */
    fun aggregate(sheetLimits: Map<SheetType<V>, UInt64?> = emptyMap()): Aggregation<V> {
        return Aggregation(
            itemDemands = itemDemands,
            sheetTypes = if (sheetLimits.isEmpty()) {
                sheetTypes.associateWith { null }
            } else {
                sheetLimits
            }
        )
    }

    /**
     * 主板材类型，未配置时返回 null。 / Main sheet type, or null when none is configured.
     *
     * @return 主板材类型 / main sheet type
    */
    fun mainSheetType(): SheetType<V>? {
        return sheetTypes.firstOrNull { it.isMain }
            ?: sheetTypes.maxByQuantityOrNull { it.area }
    }
}