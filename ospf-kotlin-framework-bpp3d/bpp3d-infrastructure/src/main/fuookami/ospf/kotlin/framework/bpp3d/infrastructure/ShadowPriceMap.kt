@file:Suppress("DEPRECATION")

/**
 * 影子价格映射基础设施。
 * Shadow price map infrastructure.
 */
package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.framework.model.AbstractShadowPriceMap
import fuookami.ospf.kotlin.framework.model.CGPipeline
import fuookami.ospf.kotlin.framework.model.ShadowPriceExtractor
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber

interface AbstractBPP3DShadowPriceArguments<
        out T : Cuboid<@UnsafeVariance T>
        > {
    val cuboid: T
}

interface GenericBPP3DShadowPriceArguments<
        V : FloatingNumber<V>,
        out T : AbstractCuboid<@UnsafeVariance V>
        > {
    val cuboid: T
}

open class AbstractBPP3DShadowPriceMap<
        out Args : AbstractBPP3DShadowPriceArguments<T>,
        out T : Cuboid<@UnsafeVariance T>
        > : AbstractShadowPriceMap<
        @UnsafeVariance Args, AbstractBPP3DShadowPriceMap<@UnsafeVariance Args, @UnsafeVariance T>
        >()

open class GenericBPP3DShadowPriceMap<
        out Args : GenericBPP3DShadowPriceArguments<V, T>,
        V : FloatingNumber<V>,
        out T : AbstractCuboid<@UnsafeVariance V>
        > : AbstractShadowPriceMap<
        @UnsafeVariance Args, GenericBPP3DShadowPriceMap<@UnsafeVariance Args, @UnsafeVariance V, @UnsafeVariance T>
        >()

typealias AbstractBPP3DShadowPriceExtractor<Args, T> = ShadowPriceExtractor<
        Args, AbstractBPP3DShadowPriceMap<Args, T>
        >

typealias AbstractBPP3DCGPipeline<Args, T> = CGPipeline<
        Args, AbstractLinearMetaModel<InfraNumber>, AbstractBPP3DShadowPriceMap<Args, T>
        >
