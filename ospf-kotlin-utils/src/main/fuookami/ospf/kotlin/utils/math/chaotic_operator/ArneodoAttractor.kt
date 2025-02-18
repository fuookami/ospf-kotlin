package fuookami.ospf.kotlin.utils.math.chaotic_operator

import kotlin.random.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.geometry.*
import fuookami.ospf.kotlin.utils.functional.*

data class ArneodoAttractor(
    val alpha: Flt64 = Flt64(-5.5),
    val beta: Flt64 = Flt64(3.5),
    val delta: Flt64 = Flt64(-1.0),
    val h: Flt64 = Flt64(0.01)
) : Extractor<Point3, Point3> {
    override operator fun invoke(x: Point3): Point3 {
        val dx = x[1]
        val dy = x[2]
        val dz = -alpha * x[0] - beta * x[1] - x[2] + delta * x[0].cub()
        return point3(
            x[0] + h * dx,
            x[1] + h * dy,
            x[2] + h * dz
        )
    }
}

data class ArneodoAttractorGenerator(
    val arneodoAttractor: ArneodoAttractor = ArneodoAttractor(),
    private var _x: Point3 = point3(
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
    )
) : Generator<Point3> {
    companion object {
        operator fun invoke(
            alpha: Flt64 = Flt64(-5.5),
            beta: Flt64 = Flt64(3.5),
            delta: Flt64 = Flt64(-1.0),
            x: Point3 = point3(
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
            )
        ): ArneodoAttractorGenerator {
            return ArneodoAttractorGenerator(
                ArneodoAttractor(alpha, beta, delta),
                x
            )
        }
    }

    val x by ::_x

    override operator fun invoke(): Point3 {
        val x = _x
        _x = arneodoAttractor(x)
        return x
    }
}
