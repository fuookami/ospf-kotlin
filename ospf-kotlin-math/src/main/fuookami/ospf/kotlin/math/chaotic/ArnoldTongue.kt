/**
 * Arnold 舌
 * Arnold Tongue
 *
 * Arnold 舌是描述圆映射中锁频现象的重要数学概念，甌Vladimir Arnold 提出。
 * 在参数空间中，Arnold 舌呈现出锁频区域的结构，是研究非线性振子同步现象的重要工具。
 * 常用于锁相环分析、同步动力学研究和非线性动力学教学。
 *
 * Arnold tongue is an important mathematical concept describing frequency locking phenomena in circle maps, proposed by Vladimir Arnold.
 * In parameter space, Arnold tongues exhibit the structure of frequency locking regions, serving as an important tool for studying synchronization phenomena in nonlinear oscillators.
 * Commonly used for phase-locked loop analysis, synchronization dynamics research, and nonlinear dynamics education.
 */
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * Arnold 舌
 * Arnold Tongue
 *
 * @property omega 频率参数 / Frequency parameter
 * @property kappa 耦合强度参数 / Coupling strength parameter
 */
data class ArnoldTongue<V : FloatingNumber<V>>(
    val omega: V,
    val kappa: V
) : Extractor<V, V> {
    override operator fun invoke(x: V): V {
        val v = omega
        val pi2 = v.constants.pi * v.constants.two
        val sinValue = castToV((pi2 * x).sin())
        return x + omega - kappa / pi2 * sinValue
    }

    @Suppress("UNCHECKED_CAST")
    private fun castToV(value: Any): V {
        // 安全不变量：V 实现 FloatingNumber<V>，且三角函数运算返回与输入同一数值族的实例。
        // Safety invariant: V implements FloatingNumber<V>, and trigonometric operations return values from the same numeric family as the input.
        return value as V
    }

    companion object {
        operator fun invoke(
            omega: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
            kappa: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.pi * Flt64.two)
        ): ArnoldTongue<Flt64> {
            return ArnoldTongue(omega, kappa)
        }
    }
}

/**
 * Arnold 舌生成器
 * Arnold Tongue Generator
 */
data class ArnoldTongueGenerator(
    val arnoldTongue: ArnoldTongue<Flt64> = ArnoldTongue(),
    private var _x: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
) : Generator<Flt64> {
    companion object {
        operator fun invoke(
            omega: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
            kappa: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.pi * Flt64.two),
            x: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
        ): ArnoldTongueGenerator {
            return ArnoldTongueGenerator(
                ArnoldTongue(omega, kappa),
                x
            )
        }
    }

    val x by ::_x

    override operator fun invoke(): Flt64 {
        val x = _x.copy()
        _x = arnoldTongue(x)
        return x
    }
}
