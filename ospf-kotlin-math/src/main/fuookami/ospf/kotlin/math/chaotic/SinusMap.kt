/**
 * Sinus 映射
 * Sinus Map
 *
 * 公式 / Formula: x_{n+1} = 2.3 * x^(2*sin(pi*x))
 */
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * @property c23 常量 2.3 / Constant 2.3
 * @property c2 常量 2 / Constant 2
 */
data class SinusMap<V : FloatingNumber<V>>(val c23: V, val c2: V) : Extractor<V, V> {
    @Suppress("UNCHECKED_CAST")
    override operator fun invoke(x: V): V {
        val sinVal = (x.constants.pi * x).sin() as V
        val exponent = c2 * sinVal
        return c23 * Flt64(Math.pow((x as Flt64).value, (exponent as Flt64).value)) as V
    }

    companion object {
        operator fun invoke(): SinusMap<Flt64> = SinusMap(Flt64(2.3), Flt64.two)
    }
}

data class SinusMapGenerator(
    val map: SinusMap<Flt64> = SinusMap(),
    private var _x: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
) : Generator<Flt64> {
    val x by ::_x;
    override operator fun invoke(): Flt64 {
        val x = _x.copy(); _x = map(x); return x
    }
}
