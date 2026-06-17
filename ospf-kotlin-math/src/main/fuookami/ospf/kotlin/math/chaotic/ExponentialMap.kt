/**
 * 指数映射
 * Exponential Map
 *
 * 指数映射是基于指数函数的复动力学混沌映射。
 * 该映射在复平面上定义，通过指数运算产生混沌行为。
 * 常用于复动力系统研究和分形图形生成。
 *
 * The exponential map is a complex dynamics chaotic map based on the exponential function.
 * This map is defined on the complex plane, generating chaotic behavior through exponential operations.
 * Commonly used for complex dynamics research and fractal graphics generation.
 */
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * 指数映射
 * Exponential Map
 *
 * 公式 / Formula: z_{n+1} = exp(z) + c
 *
 * @property c 系统参数 c / System parameter c
 */
data class ExponentialMap<V : FloatingNumber<V>>(
    val c: V
) : Extractor<V, V> {
    @Suppress("UNCHECKED_CAST")
    override operator fun invoke(z: V): V {
        return z.exp() as V + c
    }

    companion object {
        operator fun invoke(
            c: Flt64 = Random.nextFlt64(Flt64.zero, Flt64.one)
        ): ExponentialMap<Flt64> {
            return ExponentialMap(c)
        }
    }
}

/**
 * 指数映射生成器
 * Exponential Map Generator
 */
data class ExponentialMapGenerator(
    val exponentialMap: ExponentialMap<Flt64> = ExponentialMap(),
    private var _z: Flt64 = Random.nextFlt64(Flt64.zero, Flt64.one)
) : Generator<Flt64> {
    companion object {
        operator fun invoke(
            c: Flt64,
            z: Flt64 = Random.nextFlt64(Flt64.zero, Flt64.one)
        ): ExponentialMapGenerator {
            return ExponentialMapGenerator(
                ExponentialMap(c),
                z
            )
        }
    }

    val z by ::_z

    override operator fun invoke(): Flt64 {
        val z = _z.copy()
        _z = exponentialMap(z)
        return z
    }
}
