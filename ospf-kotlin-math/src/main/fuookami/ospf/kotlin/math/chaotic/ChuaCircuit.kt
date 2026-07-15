/**
 * 蔡氏电路
 * Chua's Circuit
 *
 * 蔡氏电路是由 Leon O. Chua 二1983 年设计的简单电子电路，是第一个被实验验证的混沌电路。
 * 该电路包含线性电阻、电容、电感和一个非线性元件（蔡氏二极管），能产生丰富的混沌动力学行为。
 * 常用于混沌电路实验、非线性电路设计和混沌动力学教学。
 *
 * Chua's circuit is a simple electronic circuit designed by Leon O. Chua in 1983, being the first experimentally verified chaotic circuit.
 * This circuit contains linear resistors, capacitors, inductors, and a nonlinear element (Chua's diode), capable of generating rich chaotic dynamical behavior.
 * Commonly used for chaotic circuit experiments, nonlinear circuit design, and chaos dynamics education.
*/
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * 蔡氏电路
 * Chua's Circuit
 *
 * @property a 系统参数 a / System parameter a
 * @property b 系统参数 b / System parameter b
 * @property c 分段线性参数 c / Piecewise linear parameter c
 * @property d 分段线性参数 d / Piecewise linear parameter d
 * @property h 时间步长 / Time step size
*/
data class ChuaCircuit<V : FloatingNumber<V>>(
    val a: V,
    val b: V,
    val c: V,
    val d: V,
    val h: V
) : Extractor<Point<Dim3, V>, Point<Dim3, V>> {
    override operator fun invoke(x: Point<Dim3, V>): Point<Dim3, V> {
        val v = a
        val half = v.constants.one / v.constants.two
        val f = c * x[0] + half * (d - c) * ((x[0] + v.constants.one).abs() - (x[0] - v.constants.one).abs())
        val dx = a * (x[1] - x[0] - f)
        val dy = x[0] - x[1] + x[2]
        val dz = -b * x[1]
        return Point<Dim3, V>(listOf(x[0] + h * dx, x[1] + h * dy, x[2] + h * dz), Dim3)
    }

    companion object {
        operator fun invoke(
            a: Flt64 = Flt64(15.6),
            b: Flt64 = Flt64(28.0),
            c: Flt64 = Flt64(-0.71),
            d: Flt64 = Flt64(-1.14),
            h: Flt64 = Flt64(0.01)
        ): ChuaCircuit<Flt64> {
            return ChuaCircuit(a, b, c, d, h)
        }
    }
}

/**
 * 蔡氏电路生成器
 * Chua's Circuit Generator
*/
data class ChuaCircuitGenerator(
    val chuaCircuit: ChuaCircuit<Flt64> = ChuaCircuit(),
    private var _x: Point<Dim3, Flt64> = point3(
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
    )
) : Generator<Point<Dim3, Flt64>> {
    companion object {
        operator fun invoke(
            a: Flt64 = Flt64(15.6),
            b: Flt64 = Flt64(28.0),
            c: Flt64 = Flt64(-0.71),
            d: Flt64 = Flt64(-1.14),
            h: Flt64 = Flt64(0.01),
            x: Point<Dim3, Flt64> = point3(
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
            )
        ): ChuaCircuitGenerator {
            return ChuaCircuitGenerator(
                ChuaCircuit(a, b, c, d, h),
                x
            )
        }
    }

    val x by ::_x

    override operator fun invoke(): Point<Dim3, Flt64> {
        val x = _x.copy()
        _x = chuaCircuit(x)
        return x
    }
}
