/**
 * 高斯迭代映射
 * Gauss Iterated Map
 */
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * 高斯迭代映射
 * Gauss Iterated Map
 *
 * @property a 系统参数 a / System parameter a
 * @property b 系统参数 b / System parameter b
 */
/**
 * 高斯迭代映射
 * Gauss Iterated Map
 *
 * @property a 系统参数 a / System parameter a
 * @property b 系统参数 b / System parameter b
 */
data class GaussIteratedMap<V : FloatingNumber<V>>(val a: V, val b: V) : Extractor<V, V> {
    @Suppress("UNCHECKED_CAST")
    override operator fun invoke(x: V): V = (-a * x * x).exp() as V + b

    companion object {
        operator fun invoke(
            a: Flt64 = Flt64(4.9),
            b: Flt64 = Random.nextFlt64(-Flt64.one, Flt64.one)
        ): GaussIteratedMap<Flt64> = GaussIteratedMap(a, b)
    }
}

data class GaussIteratedMapGenerator(
    val map: GaussIteratedMap<Flt64> = GaussIteratedMap(),
    private var _x: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
) : Generator<Flt64> {
    val x by ::_x;
    override operator fun invoke(): Flt64 {
        val x = _x.copy(); _x = map(x); return x
    }
}
