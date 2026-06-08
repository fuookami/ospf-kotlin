@file:Suppress("DEPRECATION")

/**
 * 影子价格映射模型。
 * Shadow price map model.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*

typealias ShadowPriceNumber = InfraNumber

open class BPP3DShadowPriceArguments(
    override val cuboid: Item
) : AbstractBPP3DShadowPriceArguments<Item>

typealias BPP3DShadowPriceMap = AbstractBPP3DShadowPriceMap<BPP3DShadowPriceArguments, Item>

fun BPP3DShadowPriceMap.reducedCost(unit: Any): ShadowPriceNumber {
    fun shadowPriceOf(item: Item): ShadowPriceNumber {
        return ShadowPriceNumber(this(BPP3DShadowPriceArguments(item)).toDouble())
    }

    return when (unit) {
        is Container3<*> -> unit.units.fold(ShadowPriceNumber.zero) { acc, placement ->
            acc + when (val child = placement.unit) {
                is Container3<*> -> reducedCost(child)
                is Item -> child.shapeVolume.value - shadowPriceOf(child)
                else -> ShadowPriceNumber.zero
            }
        }

        is Item -> unit.shapeVolume.value - shadowPriceOf(unit)
        else -> ShadowPriceNumber.zero
    }
}

fun BPP3DShadowPriceMap.reducedCost(
    unit: Any,
    demandEntries: Iterable<Pair<Bpp3dDemandMode, Bpp3dDemandKey>>
): ShadowPriceNumber {
    return reducedCost(
        unit = unit,
        demandEntries = demandEntries,
        shadowPriceOf = { mode, key ->
            val price = this.map.entries.firstOrNull { entry ->
                val thisKey = entry.key
                runCatching { thisKey::class.members.firstOrNull { it.name == "mode" }?.call(thisKey) == mode }.getOrDefault(false) &&
                        runCatching { thisKey::class.members.firstOrNull { it.name == "key" }?.call(thisKey) == key }.getOrDefault(false)
            }?.value?.price
            if (price != null) {
                ShadowPriceNumber(price.toDouble())
            } else {
                ShadowPriceNumber.zero
            }
        },
        demandValueToScalar = { demand ->
            when (demand) {
                is Bpp3dDemandValue.Amount -> InfraNumber(demand.value.toULong().toDouble())
                is Bpp3dDemandValue.Weight -> demand.value.value
            }
        }
    )
}

