/**
 * Windmi 混沌吸引子 / Windmi chaotic attractor
*/
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * Windmi 混沌吸引子 / Windmi chaotic attractor
 *
 * 由三个常微分方程组成的混沌系统，采用前向欧拉法推进一步。 / A chaotic system of three ordinary differential equations, advanced by one forward Euler step.
 *
 * 公式 / Formula:
 * x' = a * (y - x) + d * x * z
 * y' = b * x - x * z + f * y
 * z' = c * z + x * y - e * x * x
 *
 * @property a 系统参数 a / System parameter a
 * @property b 系统参数 b / System parameter b
 * @property c 系统参数 c / System parameter c
 * @property d 系统参数 d / System parameter d
 * @property e 系统参数 e / System parameter e
 * @property f 系统参数 f / System parameter f
 * @property g 保留参数 g（当前方程未使用）/ Reserved parameter g (unused by the current equations)
 * @property h 时间步长 / Time step size
*/
data class WindmiAttractor<V : FloatingNumber<V>>(
    val a: V,
    val b: V,
    val c: V,
    val d: V,
    val e: V,
    val f: V,
    val g: V,
    val h: V
) : Extractor<Point<Dim3, V>, Point<Dim3, V>> {

    override operator fun invoke(state: Point<Dim3, V>): Point<Dim3, V> {
        val x = state[0]
        val y = state[1]
        val z = state[2]

        val dx = a * (y - x) + d * x * z
        val dy = b * x - x * z + f * y
        val dz = c * z + x * y - e * x * x

        return Point(listOf(x + h * dx, y + h * dy, z + h * dz), Dim3)
    }

    companion object {
        /**
         * 以 Flt64 默认参数创建 Windmi 吸引子 / Create a Windmi attractor with Flt64 defaults
         *
         * @param a 系统参数 a / System parameter a
         * @param b 系统参数 b / System parameter b
         * @param c 系统参数 c / System parameter c
         * @param d 系统参数 d / System parameter d
         * @param e 系统参数 e / System parameter e
         * @param f 系统参数 f / System parameter f
         * @param g 保留参数 g / Reserved parameter g
         * @param h 时间步长 / Time step size
         * @return Windmi 吸引子实例 / Windmi attractor instance
        */
        operator fun invoke(
            a: Flt64 = Flt64(0.2),
            b: Flt64 = Flt64(0.2),
            c: Flt64 = Flt64(2.2),
            d: Flt64 = Flt64(0.5),
            e: Flt64 = Flt64(0.2),
            f: Flt64 = Flt64(5.7),
            g: Flt64 = Flt64(0.5),
            h: Flt64 = Flt64(0.01)
        ): WindmiAttractor<Flt64> {
            return WindmiAttractor(a, b, c, d, e, f, g, h)
        }
    }
}

/**
 * Windmi 吸引子序列生成器 / Windmi attractor sequence generator
 *
 * @property attractor Windmi 吸引子实例 / Windmi attractor instance
 * @property _state 当前状态（内部可变，通过 state 属性只读暴露）/ Current state (mutable internally, exposed read-only through the state property)
*/
data class WindmiAttractorGenerator(
    val attractor: WindmiAttractor<Flt64> = WindmiAttractor(),
    private var _state: Point<Dim3, Flt64> = point3(
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
    )
) : Generator<Point<Dim3, Flt64>> {
    val state by ::_state

    override operator fun invoke(): Point<Dim3, Flt64> {
        val current = _state.copy()
        _state = attractor(current)
        return current
    }
}
