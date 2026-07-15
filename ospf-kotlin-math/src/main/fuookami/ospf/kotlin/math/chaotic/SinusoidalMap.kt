/**
 * 正弦平方映射
 * Sinusoidal Map
*/
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * @property mu 系统参数 mu / System parameter mu
*/
data class SinusoidalMap<V : FloatingNumber<V>>(val mu: V) : Extractor<V, V> {
    @Suppress("UNCHECKED_CAST")
    override operator fun invoke(x: V): V = mu * x * x * (x.constants.pi * x).sin() as V

    companion object {
        operator fun invoke(mu: Flt64 = Flt64(2.3)): SinusoidalMap<Flt64> = SinusoidalMap(mu)
    }
}

/**
 * 正弦平方映射生成器
 * Sinusoidal Map Generator
 *
 * @property map 正弦平方映射实例 / Sinusoidal map instance
*/
data class SinusoidalMapGenerator(
    val map: SinusoidalMap<Flt64> = SinusoidalMap(),
    private var _x: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
) : Generator<Flt64> {
    val x by ::_x;
    override operator fun invoke(): Flt64 {
        val x = _x.copy(); _x = map(x); return x
    }
}
