/**
 * Singer 映射
 * Singer Map
 */
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * Singer map, a one-dimensional chaotic map based on a quartic polynomial.
 * Singer 映射，基于四次多项式的一维混沌映射。
 *
 * @property mu the bifurcation parameter controlling chaotic behavior / 控制混沌行为的分岔参数
 * @property c786 constant 7.86 / 常量 7.86
 * @property c2323 constant 23.23 / 常量 23.23
 * @property c2875 constant 28.75 / 常量 28.75
 * @property c1330 constant 13.30 / 常量 13.30
 */
data class SingerMap<V : FloatingNumber<V>>(val mu: V, val c786: V, val c2323: V, val c2875: V, val c1330: V) :
    Extractor<V, V> {
    override operator fun invoke(x: V): V {
        val x2 = x * x;
        val x3 = x2 * x;
        val x4 = x3 * x
        return mu * (c786 * x - c2323 * x2 + c2875 * x3 - c1330 * x4)
    }

    companion object {
        operator fun invoke(mu: Flt64 = Flt64(1.0)): SingerMap<Flt64> =
            SingerMap(mu, Flt64(7.86), Flt64(23.23), Flt64(28.75), Flt64(13.30))
    }
}

/**
 * Singer map generator that iteratively produces chaotic sequences.
 * Singer 映射生成器，通过迭代产生混沌序列。
 *
 * @property map the Singer map instance / Singer 映射实例
 * @property _x the internal state variable for iteration / 迭代用的内部状态变量
 */
data class SingerMapGenerator(
    val map: SingerMap<Flt64> = SingerMap(),
    private var _x: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
) : Generator<Flt64> {
    val x by ::_x;
    override operator fun invoke(): Flt64 {
        val x = _x.copy(); _x = map(x); return x
    }
}
