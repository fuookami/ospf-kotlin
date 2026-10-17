/**
 * 物品聚合。 / Item aggregation.
 *
 * 对应 BPP3D 的 `Aggregation`：跟踪已使用与剩余的物品、板材需求，供列生成迭代读取。
 * / Mirrors BPP3D `Aggregation`: tracks used and remaining items and sheet demands for the column
 * generation iterations.
*/
package fuookami.ospf.kotlin.framework.bpp2d.domain.item

import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.*
import fuookami.ospf.kotlin.framework.bpp2d.infrastructure.*

/**
 * 物品与板材需求聚合。 / Item and sheet demand aggregation.
 *
 * @param V 数值类型 / numeric type
 * @property itemDemands 物品件数需求 / item amount demands
 * @property sheetTypes 板材类型与可用数量，数量为 null 表示不限 / sheet types with available amounts, null meaning unlimited
*/
class Aggregation<V : FloatingNumber<V>>(
    val itemDemands: Map<ItemId, UInt64>,
    val sheetTypes: Map<SheetType<V>, UInt64?> = emptyMap()
) {

    /** 已使用的物品件数 / used item amounts */
    private val usedItems: MutableMap<ItemId, UInt64> = itemDemands.keys
        .associateWith { UInt64.zero }
        .toMutableMap()

    /** 已使用的板材数量 / used sheet amounts */
    private val usedSheets: MutableMap<SheetType<V>, UInt64> = sheetTypes.keys
        .associateWith { UInt64.zero }
        .toMutableMap()

    /** 主板材：显式标记优先，否则取面积最大者。 / Main sheet: the explicitly flagged one, otherwise the largest by area. */
    val mainSheet: SheetType<V>? = sheetTypes.keys.firstOrNull { it.isMain }
        ?: sheetTypes.keys.maxByQuantityOrNull { it.area }

    /** 剩余物品需求 / remaining item demands */
    val restItems: Map<ItemId, UInt64>
        get() = itemDemands
            .mapValues { (id, demand) ->
                val used = usedItems[id] ?: UInt64.zero
                if (used >= demand) UInt64.zero else demand - used
            }
            .filterValues { it > UInt64.zero }

    /** 剩余板材数量 / remaining sheet amounts */
    val restSheets: Map<SheetType<V>, UInt64>
        get() = sheetTypes
            .mapNotNull { (type, limit) ->
                val used = usedSheets[type] ?: UInt64.zero
                val rest = if (limit == null) {
                    UInt64.maximum
                } else if (used >= limit) {
                    UInt64.zero
                } else {
                    limit - used
                }
                if (rest > UInt64.zero) type to rest else null
            }
            .toMap()

    /**
     * 标记物品已使用。 / Mark items as used.
     *
     * @param items 物品编号到件数 / item identifier to amount
    */
    fun useItems(items: Map<ItemId, UInt64>) {
        for ((id, amount) in items) {
            usedItems[id] = (usedItems[id] ?: UInt64.zero) + amount
        }
    }

    /**
     * 标记板材已使用。 / Mark sheets as used.
     *
     * @param sheets 板材类型到数量 / sheet type to amount
    */
    fun useSheets(sheets: Map<SheetType<V>, UInt64>) {
        for ((type, amount) in sheets) {
            usedSheets[type] = (usedSheets[type] ?: UInt64.zero) + amount
        }
    }

    /**
     * 释放已用板材。 / Release used sheets.
     *
     * @param sheets 板材类型到数量 / sheet type to amount
    */
    fun releaseSheets(sheets: Map<SheetType<V>, UInt64>) {
        for ((type, amount) in sheets) {
            val current = usedSheets[type] ?: UInt64.zero
            usedSheets[type] = if (current <= amount) UInt64.zero else current - amount
        }
    }

    /**
     * 汇总需求面积：按板材类型给出满足全部物品需求所需的最小总面积。 / Summarise the demanded area: the minimum
     * total area needed per sheet type to satisfy all item demands.
     *
     * @param items 物品目录 / item catalogue
     * @return 需求总面积，目录缺失物品时返回 null / demanded total area, or null when an item is missing
    */
    fun demandArea(items: Map<ItemId, AbstractItem<V>>): Quantity<V>? {
        val areas = ArrayList<Quantity<V>>(itemDemands.size)
        for ((id, amount) in itemDemands) {
            val item = items[id] ?: return null
            areas.add(item.boundingArea * amountScaler(item.boundingArea, amount))
        }
        return quantitySum(areas)
    }

    /**
     * 把无符号整数换算为量值数值类型。 / Convert an unsigned integer to the quantity numeric type.
     *
     * @param sample 样本量值 / sample quantity
     * @param amount 件数 / amount
     * @return 数值类型的量 / value as the numeric type
    */
    private fun amountScaler(sample: Quantity<V>, amount: UInt64): V {
        var acc = sample.value.constants.zero
        var remaining = amount
        while (remaining > UInt64.zero) {
            acc += sample.value.constants.one
            remaining -= UInt64.one
        }
        return acc
    }

    /**
     * 按板材装载率需求估算板材数量。 / Estimate sheet amounts from a target loading rate.
     *
     * @param totalArea 需求总面积 / demanded total area
     * @param type 板材类型 / sheet type
     * @param estimatedLoadingRate 预估装载率 / estimated loading rate
     * @return 估算的板材数量 / estimated sheet amount
    */
    fun estimateSheetAmount(
        totalArea: Quantity<V>,
        type: SheetType<V>,
        estimatedLoadingRate: V
    ): V {
        val zero = type.area.value.constants.zero
        if (!quantityIsPositive(type.area) || !(estimatedLoadingRate > zero)) {
            return zero
        }
        return (totalArea.value / type.area.value) / estimatedLoadingRate
    }
}
