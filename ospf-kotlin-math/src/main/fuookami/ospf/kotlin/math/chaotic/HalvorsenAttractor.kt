/**
 * Halvorsen 吸引子
 * Halvorsen Attractor
 */
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * Halvorsen 吸引子是一种三维混沌系统，由 Halvorsen 提出，具有对称的周期性轨道结构。
 * 该系统通过非线性耦合项产生混沌行为，其动力学方程在三维空间中展现出独特的折叠和拉伸运动。
 * 常用于混沌通信、随机数生成和非线性动力学理论研究。
 *
 * The Halvorsen attractor is a three-dimensional chaotic system proposed by Halvorsen, characterized by symmetric periodic orbital structures.
 * This system generates chaotic behavior through nonlinear coupling terms, with its dynamic equations exhibiting unique folding and stretching motions in 3D space.
 * Commonly used in chaotic communication, random number generation, and nonlinear dynamics theory research.
 *
 * @property alpha 系统参数，控制吸引子的混沌行为 / System parameter controlling the chaotic behavior of the attractor
 * @property h 时间步长 / Time step size
 */
data class HalvorsenAttractor<V : FloatingNumber<V>>(val alpha: V, val h: V) :
    Extractor<Point<Dim3, V>, Point<Dim3, V>> {
    override operator fun invoke(p: Point<Dim3, V>): Point<Dim3, V> {
        val x = p[0];
        val y = p[1];
        val z = p[2]
        val four = x.constants.two * x.constants.two
        val dx = -alpha * x - four * y - four * z - y * y
        val dy = -alpha * y - four * z - four * x - z * z
        val dz = -alpha * z - four * x - four * y - x * x
        return Point<Dim3, V>(listOf(x + h * dx, y + h * dy, z + h * dz), Dim3)
    }

    companion object {
        operator fun invoke(alpha: Flt64 = Flt64(1.4), h: Flt64 = Flt64(0.01)): HalvorsenAttractor<Flt64> =
            HalvorsenAttractor(alpha, h)
    }
}

/**
 * Halvorsen 吸引子生成器，用于迭代生成吸引子轨迹点序列。
 * 初始状态随机生成，每次调用迭代一步，返回当前状态并更新到下一状态。
 * Halvorsen Attractor Generator for iteratively generating attractor trajectory point sequences.
 * The initial state is randomly generated; each invocation advances one step, returns the current state, and updates to the next state.
 *
 * @property attractor Halvorsen 吸引子实例 / Halvorsen attractor instance
 */
data class HalvorsenAttractorGenerator(
    val attractor: HalvorsenAttractor<Flt64> = HalvorsenAttractor(),
    private var _x: Point<Dim3, Flt64> = point3(
        Random.nextFlt64(
            Flt64.decimalPrecision,
            Flt64.one
        ),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
    )
) : Generator<Point<Dim3, Flt64>> {
    val x by ::_x;
    override operator fun invoke(): Point<Dim3, Flt64> {
        val x = _x.copy(); _x = attractor(x); return x
    }
}
