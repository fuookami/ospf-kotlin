/**
 * 圆映射
 * Circle Map
 *
 * 圆映射是描述周期驱动的非线性振子动力学的一维混沌映射。
 * 该映射在锁频和混沌之间展现出复杂的动力学行为，Arnold 舌结构是其重要特征。
 * 常用于锁相环分析、同步动力学研究和非线性振子建模。
 *
 * The circle map is a one-dimensional chaotic map describing the dynamics of periodically driven nonlinear oscillators.
 * This map exhibits complex dynamical behavior between frequency locking and chaos, with Arnold tongues being its important feature.
 * Commonly used for phase-locked loop analysis, synchronization dynamics research, and nonlinear oscillator modeling.
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

data class CircleMap(
    val alpha: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
    val beta: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, pi2)
) : Extractor<Flt64, Flt64> {
    companion object {
        val pi2 = Flt64.pi * Flt64.two
    }

    override fun invoke(x: Flt64): Flt64 {
        return (x + alpha - beta * (x * pi2).sin() / pi2) mod Flt64.one
    }
}

data class CircleMapGenerator(
    val circleMap: CircleMap = CircleMap(),
    private var _x: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
) : Generator<Flt64> {
    companion object {
        operator fun invoke(
            alpha: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
            beta: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, CircleMap.pi2),
            x: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
        ): CircleMapGenerator {
            return CircleMapGenerator(
                CircleMap(alpha, beta),
                x
            )
        }
    }

    val x by ::_x

    override fun invoke(): Flt64 {
        val x = _x.copy()
        _x = circleMap(x)
        return x
    }
}







