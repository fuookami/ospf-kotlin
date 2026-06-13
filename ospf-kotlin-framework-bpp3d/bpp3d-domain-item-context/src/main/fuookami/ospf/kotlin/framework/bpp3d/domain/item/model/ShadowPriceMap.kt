@file:Suppress("DEPRECATION")

/**
 * 影子价格映射模型。
 * Shadow price map model.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*

open class BPP3DShadowPriceArguments(
    override val cuboid: Item
) : AbstractBPP3DShadowPriceArguments<FltX, Item>

typealias BPP3DShadowPriceMap = AbstractBPP3DShadowPriceMap<BPP3DShadowPriceArguments, FltX, Item>

fun BPP3DShadowPriceMap.reducedCost(unit: Any): FltX {
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

fun BPP3DShadowPriceMap.reducedCost(
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
