@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.LegacyCuboid
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.LegacyQuantity
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.LegacyScalar
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.legacyInfinity
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.legacyNegativeInfinity
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.legacyOne
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.legacyScalar
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.legacyTwo
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.legacyZero

import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*

typealias ShadowPriceScalar = LegacyScalar

open class BPP3DShadowPriceArguments(
    override val cuboid: Item
) : AbstractBPP3DShadowPriceArguments<Item>

typealias BPP3DShadowPriceMap = AbstractBPP3DShadowPriceMap<BPP3DShadowPriceArguments, Item>;

fun BPP3DShadowPriceMap.reducedCost(cuboid: Cuboid<*>): ShadowPriceScalar {
    return when (cuboid) {
        is Container3<*> -> cuboid.units.fold(ShadowPriceScalar.zero) { acc, placement ->
            acc + when (val unit = placement.unit) {
                is Container3<*> -> reducedCost(unit)
                is Item -> unit.volume.value - this(BPP3DShadowPriceArguments(unit))
                else -> ShadowPriceScalar.zero
            }
        }

        is Item -> cuboid.volume.value - this(BPP3DShadowPriceArguments(cuboid))
        else -> ShadowPriceScalar.zero
    }
}

typealias BPP3DShadowPriceExtractor = AbstractBPP3DShadowPriceExtractor<BPP3DShadowPriceArguments, Item>;
typealias BPP3DCGPipeline = AbstractBPP3DCGPipeline<BPP3DShadowPriceArguments, Item>;
typealias BPP3DCGPipelineList = AbstractBPP3DCGPipelineList<BPP3DShadowPriceArguments, Item>;
