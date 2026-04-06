/**
 * Arnold 舌
 * Arnold Tongue
 *
 * Arnold 舌是描述圆映射中锁频现象的重要数学概念，由 Vladimir Arnold 提出。
 * 在参数空间中，Arnold 舌呈现出锁频区域的结构，是研究非线性振子同步现象的重要工具。
 * 常用于锁相环分析、同步动力学研究和非线性动力学教学。
 *
 * Arnold tongue is an important mathematical concept describing frequency locking phenomena in circle maps, proposed by Vladimir Arnold.
 * In parameter space, Arnold tongues exhibit the structure of frequency locking regions, serving as an important tool for studying synchronization phenomena in nonlinear oscillators.
 * Commonly used for phase-locked loop analysis, synchronization dynamics research, and nonlinear dynamics education.
 */
package fuookami.ospf.kotlin.math.chaotic_operator

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

import fuookami.ospf.kotlin.utils.functional.Extractor
import fuookami.ospf.kotlin.utils.functional.Generator
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.nextFlt64
import kotlin.random.Random

data class ArnoldTongue(
    val omega: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
    val kappa: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.pi * Flt64.two),
) : Extractor<Flt64, Flt64> {
    override operator fun invoke(x: Flt64): Flt64 {
        return x + omega - kappa / (Flt64.pi * Flt64.two) * (Flt64.pi * Flt64.two * x).sin()
    }
}

data class ArnoldTongueGenerator(
    val arnoldTongue: ArnoldTongue = ArnoldTongue(),
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







