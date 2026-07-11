/**
 * Hopfractional 混沌映射
 * Hopfractional Chaotic Map
 *
 * Hopfractional 映射是一种基于分数阶的混沌映射，
 * 通过控制参数产生混沌序列，常用于混沌加密和伪随机数生成。
 *
 * The Hopfractional map is a fractional-order-based chaotic map
 * that generates chaotic sequences through control parameters,
 * commonly used for chaotic encryption and pseudo-random number generation.
*/
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * Hopfractional 混沌映射
 * Hopfractional Chaotic Map
 *
 * @property p 控制参数 p / Control parameter p
 * @property q 控制参数 q / Control parameter q
 * @property k 控制参数 k / Control parameter k
 * @property s 控制参数 s / Control parameter s
*/
data class HopfractionalMap<V : FloatingNumber<V>>(
    val p: V,
    val q: V,
    val k: V,
    val s: V
) : Extractor<V, V> {
    @Suppress("UNCHECKED_CAST")
    override operator fun invoke(x: V): V {
        val one = x.constants.one
        val numerator = one + (k * x).pow(q) as V
        val denominator = one + (s * x).pow(q) as V
        return p * x * numerator / denominator
    }

    companion object {
        operator fun invoke(
            p: Flt64 = Flt64(0.5),
            q: Flt64 = Flt64(2.0),
            k: Flt64 = Flt64(1.0),
            s: Flt64 = Flt64(1.0)
        ): HopfractionalMap<Flt64> = HopfractionalMap(p, q, k, s)
    }
}

/**
 * Hopfractional 混沌映射生成器
 * Hopfractional Chaotic Map Generator
 *
 * @property map Hopfractional 映射实例 / Hopfractional map instance
 * @property iterations 迭代次数 / Number of iterations
*/
data class HopfractionalMapGenerator(
    val map: HopfractionalMap<Flt64> = HopfractionalMap(),
    val iterations: UInt64 = UInt64(1000UL),
    private var _x: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
) : Generator<Flt64> {
    val x by ::_x
    override operator fun invoke(): Flt64? {
        val x = _x.copy(); _x = map(x); return x
    }
}
