/**
 * Hindmarsh-Rose 神经元模型
 * Hindmarsh-Rose Neuron Model
 */
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * Hindmarsh-Rose 神经元模型
 * Hindmarsh-Rose Neuron Model
 *
 * @property a 系统参数 a / System parameter a
 * @property b 系统参数 b / System parameter b
 * @property c 系统参数 c / System parameter c
 * @property d 系统参数 d / System parameter d
 * @property s 系统参数 s / System parameter s
 * @property r 系统参数 r / System parameter r
 * @property xr 静息电位 / Resting potential
 * @property i 外部输入电流 / External input current
 * @property h 时间步长 / Time step size
 */
data class HindmarshRoseModel<V : FloatingNumber<V>>(
    val a: V, val b: V, val c: V, val d: V, val s: V, val r: V, val xr: V, val i: V, val h: V
) : Extractor<Point<Dim3, V>, Point<Dim3, V>> {
    override operator fun invoke(p: Point<Dim3, V>): Point<Dim3, V> {
        val x = p[0];
        val y = p[1];
        val z = p[2]
        val phi = -a * x * x * x + b * x * x
        val psi = c - d * x * x
        val dx = y + phi - z + i
        val dy = psi - y
        val dz = r * (s * (x - xr) - z)
        return Point<Dim3, V>(listOf(x + h * dx, y + h * dy, z + h * dz), Dim3)
    }

    companion object {
        operator fun invoke(
            a: Flt64 = Flt64(1.0),
            b: Flt64 = Flt64(1.0),
            c: Flt64 = Flt64(1.0),
            d: Flt64 = Flt64(1.0),
            s: Flt64 = Flt64(1.0),
            r: Flt64 = Flt64(1.0),
            xr: Flt64 = Flt64(1.0),
            i: Flt64 = Flt64(1.0),
            h: Flt64 = Flt64(0.01)
        ): HindmarshRoseModel<Flt64> = HindmarshRoseModel(a, b, c, d, s, r, xr, i, h)
    }
}

data class HindmarshRoseModelGenerator(
    val attractor: HindmarshRoseModel<Flt64> = HindmarshRoseModel(),
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
