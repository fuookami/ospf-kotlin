@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.framework.model.AbstractShadowPriceMap
import fuookami.ospf.kotlin.framework.model.CGPipeline
import fuookami.ospf.kotlin.framework.model.ShadowPriceExtractor
import fuookami.ospf.kotlin.math.algebra.number.Flt64

interface AbstractBPP3DShadowPriceArguments<
        out T : Cuboid<@UnsafeVariance T>
        > {
    val cuboid: T
}

open class AbstractBPP3DShadowPriceMap<
        out Args : AbstractBPP3DShadowPriceArguments<T>,
        out T : Cuboid<@UnsafeVariance T>
        > : AbstractShadowPriceMap<
        @UnsafeVariance Args, AbstractBPP3DShadowPriceMap<@UnsafeVariance Args, @UnsafeVariance T>
        >()

typealias AbstractBPP3DShadowPriceExtractor<Args, T> = ShadowPriceExtractor<
        Args, AbstractBPP3DShadowPriceMap<Args, T>
        >

typealias AbstractBPP3DCGPipeline<Args, T> = CGPipeline<
        Args, AbstractLinearMetaModel<Flt64>, AbstractBPP3DShadowPriceMap<Args, T>
        >

typealias AbstractBPP3DCGPipelineList<Args, T> = List<
        CGPipeline<Args, AbstractLinearMetaModel<Flt64>, AbstractBPP3DShadowPriceMap<Args, T>>
        >
