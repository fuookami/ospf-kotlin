/**
 * 四涡卷超混沌吸引子
 * Four-Scroll Hyper-Chaotic Attractor
*/
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * 四涡卷超混沌吸引子
 * Four-Scroll Hyper-Chaotic Attractor
 *
 * @property a 系统参数 a / System parameter a
 * @property b 系统参数 b / System parameter b
 * @property c 系统参数 c / System parameter c
 * @property d 系统参数 d / System parameter d
 * @property h 时间步长 / Time step size
*/
data class FourScrollHyperChaoticAttractor<V : FloatingNumber<V>>(val a: V, val b: V, val c: V, val d: V, val h: V) :
    Extractor<Point<Dim4, V>, Point<Dim4, V>> {
    override operator fun invoke(p: Point<Dim4, V>): Point<Dim4, V> {
        val x = p[0];
        val y = p[1];
        val z = p[2];
        val u = p[3]
        val dx = -a * x + y * z + z
        val dy = b * y - x * z
        val dz = -c * z + x * y + u
        val du = y - d * u
        return Point<Dim4, V>(listOf(x + h * dx, y + h * dy, z + h * dz, u + h * du), Dim4)
    }

    companion object {
        operator fun invoke(
            a: Flt64 = Flt64(0.5),
            b: Flt64 = Flt64(0.5),
            c: Flt64 = Flt64(0.5),
            d: Flt64 = Flt64(0.5),
            h: Flt64 = Flt64(0.01)
        ): FourScrollHyperChaoticAttractor<Flt64> = FourScrollHyperChaoticAttractor(a, b, c, d, h)
    }
}

/**
 * 四涡卷超混沌吸引子生成器
 * Four-Scroll Hyper-Chaotic Attractor Generator
*/
data class FourScrollHyperChaoticAttractorGenerator(
    val attractor: FourScrollHyperChaoticAttractor<Flt64> = FourScrollHyperChaoticAttractor(),
    private var _x: Point<Dim4, Flt64> = point4(
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
    )
) : Generator<Point<Dim4, Flt64>> {
    val x by ::_x
    override operator fun invoke(): Point<Dim4, Flt64> {
        val x = _x.copy(); _x = attractor(x); return x
    }
}
