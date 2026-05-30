@file:Suppress("DEPRECATION")

/**
 * 影子价格映射模型。
 * Shadow price map model.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraInfinity
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraNegativeInfinity
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraOne
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraOne
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraZero
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*






typealias ShadowPriceScalar = InfraNumber

open class BPP3DShadowPriceArguments(
    override val cuboid: Item
) : AbstractBPP3DShadowPriceArguments<Item>

typealias BPP3DShadowPriceMap = AbstractBPP3DShadowPriceMap<BPP3DShadowPriceArguments, Item>;

fun BPP3DShadowPriceMap.reducedCost(cuboid: Cuboid<*>): ShadowPriceScalar {
    fun shadowPriceOf(item: Item): ShadowPriceScalar {
        return ShadowPriceScalar(this(BPP3DShadowPriceArguments(item)).toDouble())
    }

    return when (cuboid) {
        is Container3<*> -> cuboid.units.fold(ShadowPriceScalar.zero) { acc, placement ->
            acc + when (val unit = placement.unit) {
                is Container3<*> -> reducedCost(unit)
                is Item -> unit.volume.value - shadowPriceOf(unit)
                else -> ShadowPriceScalar.zero
            }
        }

        is Item -> cuboid.volume.value - shadowPriceOf(cuboid)
        else -> ShadowPriceScalar.zero
    }
}

fun BPP3DShadowPriceMap.reducedCost(
    cuboid: Cuboid<*>,
    demandEntries: Iterable<Pair<Bpp3dDemandMode, Bpp3dDemandKey>>
): ShadowPriceScalar {
    return reducedCost(
        cuboid = cuboid,
        demandEntries = demandEntries,
        shadowPriceOf = { mode, key ->
            val price = this.map.entries.firstOrNull { entry ->
                val thisKey = entry.key
                runCatching { thisKey::class.members.firstOrNull { it.name == "mode" }?.call(thisKey) == mode }.getOrDefault(false) &&
                        runCatching { thisKey::class.members.firstOrNull { it.name == "key" }?.call(thisKey) == key }.getOrDefault(false)
            }?.value?.price
            if (price != null) {
                ShadowPriceScalar(price.toDouble())
            } else {
                ShadowPriceScalar.zero
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

typealias BPP3DShadowPriceExtractor = AbstractBPP3DShadowPriceExtractor<BPP3DShadowPriceArguments, Item>;
typealias BPP3DCGPipeline = AbstractBPP3DCGPipeline<BPP3DShadowPriceArguments, Item>;
typealias BPP3DCGPipelineList = AbstractBPP3DCGPipelineList<BPP3DShadowPriceArguments, Item>;



