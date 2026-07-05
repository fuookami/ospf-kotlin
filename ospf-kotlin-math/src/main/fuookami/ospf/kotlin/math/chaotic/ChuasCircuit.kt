/**
 * 蔡氏电路
 * Chua's Circuit
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
 * @property c 系统参数 c / System parameter c
 * @property d 系统参数 d / System parameter d
 * @property h 时间步长 / Time step size
 */
data class ChuasCircuit<V : FloatingNumber<V>>(val a: V, val b: V, val c: V, val d: V, val h: V) :
    Extractor<Point<Dim3, V>, Point<Dim3, V>> {
    override operator fun invoke(p: Point<Dim3, V>): Point<Dim3, V> {
        val x = p[0];
        val y = p[1];
        val z = p[2]
        val half = x.constants.half
        val f = c * x + half * (d - c) * ((x + x.constants.one).abs() + (x - x.constants.one).abs())
        val dx = a * (y - x - f)
        val dy = x - y + z
        val dz = -b * y
        return Point<Dim3, V>(listOf(x + h * dx, y + h * dy, z + h * dz), Dim3)
    }

    companion object {
        operator fun invoke(
            a: Flt64 = Flt64(15.6),
            b: Flt64 = Flt64(28.0),
            c: Flt64 = Flt64(-1.143),
            d: Flt64 = Flt64(-0.714),
            h: Flt64 = Flt64(0.01)
        ): ChuasCircuit<Flt64> = ChuasCircuit(a, b, c, d, h)
    }
}

/**
 * 蔡氏电路生成器
 * Chua's Circuit generator
 *
 * @property attractor 蔡氏电路吸引子 / Chua's Circuit attractor
 * @property x 当前状态点 / Current state point
 */
data class ChuasCircuitGenerator(
    val attractor: ChuasCircuit<Flt64> = ChuasCircuit(),
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
