/**
 * Shadow price map model.
 * 影子价格映射模型。
*/
package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*

/**
 * BPP3D 影子价格参数。 / BPP3D shadow price arguments.
*/
open class BPP3DShadowPriceArguments(
    override val cuboid: Item
) : AbstractBPP3DShadowPriceArguments<FltX, Item>

/**
 * Computes the reduced cost for a packing unit against this shadow price map.
 * 根据此影子价格映射计算装箱单元的缩减成本。
 *
 * @param unit 待评估的装箱单元（货物或三维容器） / packing unit to evaluate (Item or Container3)
 * @return 缩减成本：货物为体积减影子价格，容器为递归求和 / reduced cost: volume minus shadow price for items, or recursive sum for containers
*/
fun AbstractBPP3DShadowPriceMap<BPP3DShadowPriceArguments, FltX, Item>.reducedCost(unit: Any): FltX {

/**
 * Looks up the shadow price for a specific item.
 * 查找特定货物的影子价格。
 *
 * @param item 待评估的货物 / item to evaluate
 * @return 该货物的影子价格 / shadow price of the item
*/
    fun shadowPriceOf(item: Item): FltX {
        return FltX(this(BPP3DShadowPriceArguments(item)).toDouble())
    }

    return when (unit) {
        is Container3<*, *> -> {
            @Suppress("UNCHECKED_CAST")
            (unit as Container3<*, FltX>).units.fold(FltX.zero) { acc, placement ->
                acc + when (val child = placement.unit) {
                    is Container3<*, *> -> reducedCost(child)
                    is Item -> child.shapeVolume.value - shadowPriceOf(child)
                    else -> FltX.zero
                }
            }
        }

        is Item -> unit.shapeVolume.value - shadowPriceOf(unit)
        else -> FltX.zero
    }
}

/**
 * AbstractBPP3DShadowPriceMap.
 * AbstractBPP3DShadowPriceMap。
 * @param unit 待检查的装箱单元 / packing unit to check
 * @param demandEntries 活跃需求条目 / active demand entries
 * @return 缩减成本值 / reduced cost value
*/
fun AbstractBPP3DShadowPriceMap<BPP3DShadowPriceArguments, FltX, Item>.reducedCost(
    unit: Any,
    demandEntries: Iterable<Pair<Bpp3dDemandMode, Bpp3dDemandKey>>
): FltX {
    return reducedCost(
        unit = unit,
        demandEntries = demandEntries,
        shadowPriceOf = { mode: Bpp3dDemandMode, key: Bpp3dDemandKey ->
            val price = this.map.entries.firstOrNull { entry ->
                val thisKey = entry.key
                runCatching { thisKey::class.members.firstOrNull { it.name == "mode" }?.call(thisKey) == mode }.getOrDefault(false) &&
                        runCatching { thisKey::class.members.firstOrNull { it.name == "key" }?.call(thisKey) == key }.getOrDefault(false)
            }?.value?.price
            if (price != null) {
                FltX(price.toDouble())
            } else {
                FltX.zero
            }
        },
        demandValueToScalar = { demand: Bpp3dDemandValue ->
            when (demand) {
                is Bpp3dDemandValue.Amount -> FltX(demand.value.toULong().toDouble())
                is Bpp3dDemandValue.Weight -> demand.value.value
            }
        }
    )
}
