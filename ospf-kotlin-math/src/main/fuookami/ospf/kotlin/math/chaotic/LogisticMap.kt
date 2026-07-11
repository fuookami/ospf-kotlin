/**
 * 逻辑斯蒂映射
 * Logistic Map
 *
 * 逻辑斯蒂映射是最经典的一维混沌映射，由 Robert May 于 1976 年推广。
 * 该映射通过简单的二次函数产生复杂的混沌行为，是混沌理论的入门模型。
 * 常用于混沌理论教学、种群动力学建模和伪随机数生成。
 *
 * The logistic map is the most classic one-dimensional chaotic map, popularized by Robert May in 1976.
 * This map generates complex chaotic behavior through a simple quadratic function, serving as an introductory model for chaos theory.
 * Commonly used for chaos theory education, population dynamics modeling, and pseudo-random number generation.
*/
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * 逻辑斯蒂映射
 * Logistic Map
 *
 * 公式 / Formula: x_{n+1} = a * x * (1 - x)
 *
 * @property a 系统参数 a，取值范围 (0, 4] / System parameter a, range (0, 4]
*/
data class LogisticMap<V : FloatingNumber<V>>(
    val a: V
) : Extractor<V, V> {
    override operator fun invoke(x: V): V {
        return a * x * (x.constants.one - x)
    }

    companion object {
        operator fun invoke(
            a: Flt64 = Random.nextFlt64(Flt64.one, Flt64(4.0))
        ): LogisticMap<Flt64> {
            return LogisticMap(a)
        }
    }
}

/**
 * 逻辑斯蒂映射生成器
 * Logistic Map Generator
*/
data class LogisticMapGenerator(
    val logisticMap: LogisticMap<Flt64> = LogisticMap(),
    private var _x: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
) : Generator<Flt64> {
    companion object {
        operator fun invoke(
            a: Flt64,
            x: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
        ): LogisticMapGenerator {
            return LogisticMapGenerator(
                LogisticMap(a),
                x
            )
        }
    }

    val x by ::_x

    override operator fun invoke(): Flt64 {
        val x = _x.copy()
        _x = logisticMap(x)
        return x
    }
}
