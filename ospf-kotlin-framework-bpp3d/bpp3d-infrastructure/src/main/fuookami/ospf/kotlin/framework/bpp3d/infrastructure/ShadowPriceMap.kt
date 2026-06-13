@file:Suppress("DEPRECATION")

/**
 * 影子价格映射基础设施。
 * Shadow price map infrastructure.
 */
package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.framework.model.AbstractShadowPriceMap
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber

interface AbstractBPP3DShadowPriceArguments<
        V : FloatingNumber<V>,
        out T : Cuboid<@UnsafeVariance T, @UnsafeVariance V>
        > {
    val cuboid: T
}

open class AbstractBPP3DShadowPriceMap<
        out Args : AbstractBPP3DShadowPriceArguments<V, T>,
        V : FloatingNumber<V>,
        out T : Cuboid<@UnsafeVariance T, @UnsafeVariance V>
        > : AbstractShadowPriceMap<
        @UnsafeVariance Args, AbstractBPP3DShadowPriceMap<@UnsafeVariance Args, @UnsafeVariance V, @UnsafeVariance T>
        >()
