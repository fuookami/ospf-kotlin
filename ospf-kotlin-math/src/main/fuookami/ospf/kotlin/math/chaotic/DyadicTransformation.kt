/**
 * 二进制变换
 * Dyadic Transformation
 */
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * 公式 / Formula: x_{n+1} = 2*x mod 1
 */
/**
 * 二进制变换
 * Dyadic Transformation
 *
 * @property two 常量 2 / Constant 2
 * @property one 常量 1 / Constant 1
 */
/**
 * 二进制变换
 * Dyadic Transformation
 *
 * @property two 常量 2 / Constant 2
 * @property one 常量 1 / Constant 1
 */
data class DyadicTransformation<V : FloatingNumber<V>>(val two: V, val one: V) : Extractor<V, V> {
    override operator fun invoke(x: V): V = (two * x) mod one

    companion object {
        operator fun invoke(): DyadicTransformation<Flt64> = DyadicTransformation(Flt64.two, Flt64.one)
    }
}

data class DyadicTransformationGenerator(
    val map: DyadicTransformation<Flt64> = DyadicTransformation(),
    private var _x: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
) : Generator<Flt64> {
    val x by ::_x;
    override operator fun invoke(): Flt64 {
        val x = _x.copy(); _x = map(x); return x
    }
}
