/**
 * Thomas 循环对称吸引子
 * Thomas Cyclically Symmetric Attractor
*/
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * Thomas 循环对称吸引子
 * Thomas Cyclically Symmetric Attractor
 *
 * @property b 系统参数 b / System parameter b
 * @property h 时间步长 / Time step size
*/
data class ThomasCyclicallySymmetricAttractor<V : FloatingNumber<V>>(val b: V, val h: V) :
    Extractor<Point<Dim3, V>, Point<Dim3, V>> {
    @Suppress("UNCHECKED_CAST")
    override operator fun invoke(p: Point<Dim3, V>): Point<Dim3, V> {
        val x = p[0];
        val y = p[1];
        val z = p[2]
        val dx = y.sin() as V - b * x
        val dy = z.sin() as V - b * y
        val dz = x.sin() as V - b * z
        return Point<Dim3, V>(listOf(x + h * dx, y + h * dy, z + h * dz), Dim3)
    }

    companion object {
        operator fun invoke(
            b: Flt64 = Flt64(0.208186),
            h: Flt64 = Flt64(0.01)
        ): ThomasCyclicallySymmetricAttractor<Flt64> = ThomasCyclicallySymmetricAttractor(b, h)
    }
}

/**
 * Thomas 循环对称吸引子生成器
 * Thomas Cyclically Symmetric Attractor Generator
*/
data class ThomasCyclicallySymmetricAttractorGenerator(
    val attractor: ThomasCyclicallySymmetricAttractor<Flt64> = ThomasCyclicallySymmetricAttractor(),
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
