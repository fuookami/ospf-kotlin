/**
 * Lorenz 96 模型
 * Lorenz 96 Model
 *
 * Lorenz 96 模型是 Edward Lorenz 于 1996 年提出的 N 维混沌系统。
 * 该模型用于研究大气可预报性，是气象学中常用的混沌测试模型。
 * 常用于天气预报、数据同化和混沌理论研究。
 *
 * The Lorenz 96 model is an N-dimensional chaotic system proposed by Edward Lorenz in 1996.
 * This model is used for studying atmospheric predictability, serving as a common chaotic test model in meteorology.
 * Commonly used for weather forecasting, data assimilation, and chaos theory research.
 */
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * 公式 / Formula: dx_i/dt = (x_{i+1} - x_{i-2}) * x_{i-1} - x_i + a
 * （周期性边界条件 / Periodic boundary conditions）
 *
 * @property a 强迫参数 / Forcing parameter
 * @property h 时间步长 / Time step size
 */
data class Lorenz96Model(
    val a: Flt64 = Flt64(8.0),
    val h: Flt64 = Flt64(0.01)
) : Extractor<List<Flt64>, List<Flt64>> {
    override operator fun invoke(state: List<Flt64>): List<Flt64> {
        val n = state.size
        return List(n) { i ->
            val xip1 = state[(i + 1) % n]
            val xi = state[i]
            val xim1 = state[(i - 1 + n) % n]
            val xim2 = state[(i - 2 + n) % n]
            val dx = (xip1 - xim2) * xim1 - xi + a
            xi + h * dx
        }
    }
}

/**
 * Lorenz 96 模型生成器
 * Lorenz 96 Model Generator
 *
 * 基于 Lorenz96Model 的状态生成器，每次调用时推进一个时间步并返回前一步的状态。
 * A state generator based on Lorenz96Model that advances one time step per invocation and returns the previous state.
 *
 * @property model 底层 Lorenz 96 模型实例 / Underlying Lorenz 96 model instance
 * @property n 系统维数 / System dimension
 */
data class Lorenz96ModelGenerator(
    val model: Lorenz96Model = Lorenz96Model(),
    val n: Int = 40,
    private var _state: List<Flt64> = List(n) { Random.nextFlt64(Flt64.decimalPrecision, Flt64.one) }
) : Generator<List<Flt64>> {
    companion object {
        operator fun invoke(
            a: Flt64,
            h: Flt64,
            n: Int = 40,
            state: List<Flt64> = List(n) { Random.nextFlt64(Flt64.decimalPrecision, Flt64.one) }
        ): Lorenz96ModelGenerator = Lorenz96ModelGenerator(Lorenz96Model(a, h), n, state)
    }

    val state by ::_state
    override operator fun invoke(): List<Flt64> {
        val s = _state.toList(); _state = model(_state); return s
    }
}
