/**
 * 圆映尌
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

/**
 * 圆映尌
 * Circle Map
 */
data class CircleMap<V : FloatingImpl<V>>(
    val alpha: V,
    val beta: V
) : Extractor<V, V> {
    companion object {
        operator fun invoke(
            alpha: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
            beta: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.pi * Flt64.two)
        ): CircleMap<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
            return CircleMap(alpha, beta)
        }
    }

    override fun invoke(x: V): V {
        val v = alpha
        val pi2 = v.constants.pi * v.constants.two
        val sinVal = (x * pi2).sin() as V
        val raw = x + alpha - beta * sinVal / pi2
        return raw - (raw / v.constants.one).floor() * v.constants.one
    }
}

/**
 * 圆映射生成器
 * Circle Map Generator
 */
data class CircleMapGenerator(
    val circleMap: CircleMap<fuookami.ospf.kotlin.math.algebra.number.Flt64> = CircleMap(),
    private var _x: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
) : Generator<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
    companion object {
        operator fun invoke(
            alpha: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
            beta: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.pi * Flt64.two),
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