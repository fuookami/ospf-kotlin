package fuookami.ospf.kotlin.utils.math.chaotic_operator

import kotlin.random.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.geometry.*
import fuookami.ospf.kotlin.utils.functional.*

data class CoulletAttractor(
    val alpha: Flt64 = Flt64(0.8),
    val beta: Flt64 = Flt64(-1.1),
    val delta: Flt64 = Flt64(-1.0),
    val zeta: Flt64 = Flt64(-0.45),
    val h: Flt64 = Flt64(0.01)
) : Extractor<Point3, Point3> {
    override operator fun invoke(x: Point3): Point3 {
        val dx = x[1]
        val dy = x[2]
        val dz = alpha * x[0] + beta * x[1] + delta * x[2] + delta * x[0].cub()
        return point3(
            x[0] + h * dx,
            x[1] + h * dy,
            x[2] + h * dz
        )
    }
}

data class CoulletAttractorGenerator(
    val coulletAttractor: CoulletAttractor = CoulletAttractor(),
    private var _x: Point3 = point3(
        Random.nextFlt64(Flt64.zero, Flt64.one),
        Random.nextFlt64(Flt64.zero, Flt64.one),
        Random.nextFlt64(Flt64.zero, Flt64.one)
    )
) : Generator<Point3> {
    companion object {
        operator fun invoke(
            alpha: Flt64 = Flt64(0.8),
            beta: Flt64 = Flt64(-1.1),
            delta: Flt64 = Flt64(-1.0),
            zeta: Flt64 = Flt64(-0.45),
            h: Flt64 = Flt64(0.01),
            x: Point3 = point3(
                Random.nextFlt64(Flt64.zero, Flt64.one),
                Random.nextFlt64(Flt64.zero, Flt64.one),
                Random.nextFlt64(Flt64.zero, Flt64.one)
            )
        ): CoulletAttractorGenerator {
            return CoulletAttractorGenerator(
                CoulletAttractor(alpha, beta, delta, zeta, h),
                x
            )
        }
    }

    val x by ::_x

    override operator fun invoke(): Point3 {
        val x = _x.copy()
        _x = coulletAttractor(x)
        return x
    }
}
