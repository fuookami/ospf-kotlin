/**
 * Lorenz 84 模型
 * Lorenz 84 Model
 */
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * Lorenz 84 模型
 * Lorenz 84 Model
 *
 * @property a 系统参数 a / System parameter a
 * @property b 系统参数 b / System parameter b
 * @property f 系统参数 f / System parameter f
 * @property g 系统参数 g / System parameter g
 * @property h 时间步长 / Time step size
 */
data class Lorenz84Model<V : FloatingNumber<V>>(val a: V, val b: V, val f: V, val g: V, val h: V) :
    Extractor<Point<Dim3, V>, Point<Dim3, V>> {
    override operator fun invoke(p: Point<Dim3, V>): Point<Dim3, V> {
        val x = p[0];
        val y = p[1];
        val z = p[2]
        val dx = -y * y - z * z - a * x + a * f
        val dy = x * y - b * x * z - y + g
        val dz = b * x * y + x * z - z
        return Point<Dim3, V>(listOf(x + h * dx, y + h * dy, z + h * dz), Dim3)
    }

    companion object {
        operator fun invoke(
            a: Flt64 = Flt64(0.25),
            b: Flt64 = Flt64(4.0),
            f: Flt64 = Flt64(8.0),
            g: Flt64 = Flt64(1.0),
            h: Flt64 = Flt64(0.01)
        ): Lorenz84Model<Flt64> = Lorenz84Model(a, b, f, g, h)
    }
}

data class Lorenz84ModelGenerator(
    val attractor: Lorenz84Model<Flt64> = Lorenz84Model(),
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
