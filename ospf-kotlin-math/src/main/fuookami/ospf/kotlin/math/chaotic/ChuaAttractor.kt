/**
 * 蔡氏吸引孌
 * Chua's Attractor
 *
 * 蔡氏吸引子是甌Leon O. Chua 提出的著名混沌系统，是蔡氏电路的数学模型。
 * 该系统通过分段线性非线性函数产生混沌行为，是最早被实验观测到的混沌系统之一。
 * 常用于混沌电路研究、混沌加密和电子工程教学。
 *
 * Chua's attractor is a famous chaotic system proposed by Leon O. Chua, serving as the mathematical model for Chua's circuit.
 * This system generates chaotic behavior through piecewise linear nonlinear functions and is one of the first chaotic systems experimentally observed.
 * Commonly used for chaotic circuit research, chaos encryption, and electronic engineering education.
*/
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * 蔡氏吸引子
 * Chua's Attractor
 *
 * @property alpha 系统参数 alpha / System parameter alpha
 * @property beta 系统参数 beta / System parameter beta
 * @property delta 分段线性参数 delta / Piecewise linear parameter delta
 * @property epsilon 分段线性参数 epsilon / Piecewise linear parameter epsilon
 * @property zeta 系统参数 zeta / System parameter zeta
 * @property h 时间步长 / Time step size
*/
data class ChuaAttractor<V : FloatingNumber<V>>(
    val alpha: V,
    val beta: V,
    val delta: V,
    val epsilon: V,
    val zeta: V,
    val h: V
) : Extractor<Point<Dim3, V>, Point<Dim3, V>> {
    override operator fun invoke(x: Point<Dim3, V>): Point<Dim3, V> {
        val v = alpha
        val g = epsilon * x[0] + (delta - epsilon) * ((x[0] + v.constants.one).abs() - (x[0] - v.constants.one).abs())
        val dx = alpha * (x[1] - x[0] - g)
        val dy = beta * (x[0] - x[1] + x[2])
        val dz = -zeta * x[1]
        return Point<Dim3, V>(listOf(x[0] + h * dx, x[1] + h * dy, x[2] + h * dz), Dim3)
    }

    companion object {
        operator fun invoke(
            alpha: Flt64 = Flt64(15.6),
            beta: Flt64 = Flt64(1.0),
            delta: Flt64 = Flt64(-1.0),
            epsilon: Flt64 = Flt64(0.0),
            zeta: Flt64 = Flt64(25.58),
            h: Flt64 = Flt64(0.01)
        ): ChuaAttractor<Flt64> {
            return ChuaAttractor(alpha, beta, delta, epsilon, zeta, h)
        }
    }
}

/**
 * 蔡氏吸引子生成器
 * Chua's Attractor Generator
*/
data class ChuaAttractorGenerator(
    val chuaAttractor: ChuaAttractor<Flt64> = ChuaAttractor(),
    private var _x: Point<Dim3, Flt64> = point3(
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
    )
) : Generator<Point<Dim3, Flt64>> {
    companion object {
        operator fun invoke(
            alpha: Flt64 = Flt64(15.6),
            beta: Flt64 = Flt64(1.0),
            delta: Flt64 = Flt64(-1.0),
            epsilon: Flt64 = Flt64(0.0),
            zeta: Flt64 = Flt64(25.58),
            h: Flt64 = Flt64(0.01),
            x: Point<Dim3, Flt64> = point3(
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
            )
        ): ChuaAttractorGenerator {
            return ChuaAttractorGenerator(
                ChuaAttractor(alpha, beta, delta, epsilon, zeta, h),
                x
            )
        }
    }

    val x by ::_x

    override operator fun invoke(): Point<Dim3, Flt64> {
        val x = _x.copy()
        _x = chuaAttractor(x)
        return _x
    }
}
