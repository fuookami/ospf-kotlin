/**
 * 影子价格映射基础设施。
 * Shadow price map infrastructure.
 */
package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.framework.model.AbstractShadowPriceMap

/**
 * BPP3D 影子价格参数抽象接口，定义获取 cuboid 的协议。
 * Abstract interface for BPP3D shadow price arguments, defining the protocol for obtaining a cuboid.
 */
interface AbstractBPP3DShadowPriceArguments<
        V : FloatingNumber<V>,
        out T : Cuboid<@UnsafeVariance T, @UnsafeVariance V>
        > {
    val cuboid: T
}

/**
 * BPP3D 影子价格映射抽象基类，继承自 AbstractShadowPriceMap，提供类型安全的泛型参数约束。
 * Abstract base class for BPP3D shadow price map, extending AbstractShadowPriceMap with type-safe generic parameter constraints.
 */
open class AbstractBPP3DShadowPriceMap<
        out Args : AbstractBPP3DShadowPriceArguments<V, T>,
        V : FloatingNumber<V>,
        out T : Cuboid<@UnsafeVariance T, @UnsafeVariance V>
        > : AbstractShadowPriceMap<
        @UnsafeVariance Args, AbstractBPP3DShadowPriceMap<@UnsafeVariance Args, @UnsafeVariance V, @UnsafeVariance T>
        >()
