/**
 * 蔡氏吸引子
 * Chua's Attractor
 *
 * 蔡氏吸引子是由 Leon O. Chua 提出的著名混沌系统，是蔡氏电路的数学模型。
 * 该系统通过分段线性非线性函数产生混沌行为，是最早被实验观测到的混沌系统之一。
 * 常用于混沌电路研究、混沌加密和电子工程教学。
 *
 * Chua's attractor is a famous chaotic system proposed by Leon O. Chua, serving as the mathematical model for Chua's circuit.
 * This system generates chaotic behavior through piecewise linear nonlinear functions and is one of the first chaotic systems experimentally observed.
 * Commonly used for chaotic circuit research, chaos encryption, and electronic engineering education.
 */
package fuookami.ospf.kotlin.math.chaotic_operator

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

import fuookami.ospf.kotlin.utils.functional.Extractor
import fuookami.ospf.kotlin.utils.functional.Generator
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.geometry.Point3
import fuookami.ospf.kotlin.math.geometry.point3
import fuookami.ospf.kotlin.math.nextFlt64
import kotlin.random.Random

data class ChuaAttractor(
    val alpha: Flt64 = Flt64(15.6),
    val beta: Flt64 = Flt64(1.0),
    val delta: Flt64 = Flt64(-1.0),
    val epsilon: Flt64 = Flt64(0.0),
    val zeta: Flt64 = Flt64(25.58),
    val h: Flt64 = Flt64(0.01)
) : Extractor<Point3, Point3> {
    override operator fun invoke(x: Point3): Point3 {
        val g = epsilon * x[0] + (delta - epsilon) * ((x[0] + Flt64.one).abs() - (x[0] - Flt64.one).abs())
        val dx = alpha * (x[1] - x[0] - g)
        val dy = beta * (x[0] - x[1] + x[2])
        val dz = -zeta * x[1]
        return point3(
            x[0] + h * dx,
            x[1] + h * dy,
            x[2] + h * dz
        )
    }
}

data class ChuaAttractorGenerator(
    val chuaAttractor: ChuaAttractor = ChuaAttractor(),
    private var _x: Point3 = point3(
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
    )
) : Generator<Point3> {
    companion object {
        operator fun invoke(
            alpha: Flt64 = Flt64(15.6),
            beta: Flt64 = Flt64(1.0),
            delta: Flt64 = Flt64(-1.0),
            epsilon: Flt64 = Flt64(0.0),
            zeta: Flt64 = Flt64(25.58),
            h: Flt64 = Flt64(0.01),
            x: Point3 = point3(
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
            )
        ): ChuaAttractorGenerator {
            return ChuaAttractorGenerator(
                ChuaAttractor(
                    alpha = alpha,
                    beta = beta,
                    delta = delta,
                    epsilon = epsilon,
                    zeta = zeta,
                    h = h
                ),
                x
            )
        }
    }

    val x by ::_x

    override operator fun invoke(): Point3 {
        val x = _x.copy()
        _x = chuaAttractor(x)
        return _x
    }
}






