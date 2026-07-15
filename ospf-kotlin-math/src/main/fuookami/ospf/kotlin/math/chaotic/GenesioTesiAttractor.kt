/**
 * Genesio-Tesi 吸引子
 * Genesio-Tesi Attractor
*/
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * Genesio-Tesi 吸引子
 * Genesio-Tesi Attractor
 *
 * @property alpha 系统参数 alpha / System parameter alpha
 * @property beta 系统参数 beta / System parameter beta
 * @property delta 系统参数 delta / System parameter delta
 * @property h 时间步长 / Time step size
*/
data class GenesioTesiAttractor<V : FloatingNumber<V>>(val alpha: V, val beta: V, val delta: V, val h: V) :
    Extractor<Point<Dim3, V>, Point<Dim3, V>> {
    override operator fun invoke(p: Point<Dim3, V>): Point<Dim3, V> {
        val x = p[0];
        val y = p[1];
        val z = p[2]
        val dx = y
        val dy = z
        val dz = -delta * x - beta * y - alpha * z + x * x
        return Point<Dim3, V>(listOf(x + h * dx, y + h * dy, z + h * dz), Dim3)
    }

    companion object {
        operator fun invoke(
            alpha: Flt64 = Flt64(0.44),
            beta: Flt64 = Flt64(1.1),
            delta: Flt64 = Flt64(1.0),
            h: Flt64 = Flt64(0.01)
        ): GenesioTesiAttractor<Flt64> = GenesioTesiAttractor(alpha, beta, delta, h)
    }
}

/**
 * Genesio-Tesi 吸引子生成器
 * Genesio-Tesi Attractor Generator
 *
 * @property attractor Genesio-Tesi 吸引子实例 / Genesio-Tesi attractor instance
 * @property _x 当前三维状态点 / Current 3D state point
*/
data class GenesioTesiAttractorGenerator(
    val attractor: GenesioTesiAttractor<Flt64> = GenesioTesiAttractor(),
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
