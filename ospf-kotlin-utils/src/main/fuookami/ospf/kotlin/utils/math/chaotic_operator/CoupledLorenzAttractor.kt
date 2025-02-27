package fuookami.ospf.kotlin.utils.math.chaotic_operator

import kotlin.random.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.geometry.*
import fuookami.ospf.kotlin.utils.functional.*

data class CoupledLorenzAttractor(
    val beta: Flt64 = Flt64(8.0 / 3.0),
    val gamma1: Flt64 = Flt64(35.0),
    val gamma2: Flt64 = Flt64(1.15),
    val epsilon: Flt64 = Flt64(2.85),
    val omicron: Flt64 = Flt64(2.85),
    val h: Flt64 = Flt64(0.01)
) : Extractor<Pair<Point3, Point3>, Pair<Point3, Point3>> {
    override operator fun invoke(x: Pair<Point3, Point3>): Pair<Point3, Point3> {
        val (x1, x2) = x
        val dx1 = omicron * (x1[1] - x1[0])
        val dy1 = dy(gamma1, x1)
        val dz1 = dz(x1)
        val dx2 = omicron * (x2[1] - x2[0]) + epsilon * (x1[0] - x2[0])
        val dy2 = dy(gamma2, x2)
        val dz2 = dz(x2)
        return point3(
            x1[0] + h * dx1,
            x1[1] + h * dy1,
            x1[2] + h * dz1
        ) to point3(
            x2[0] + h * dx2,
            x2[1] + h * dy2,
            x2[2] + h * dz2
        )
    }

    private fun dy(gamma: Flt64, x: Point3): Flt64 {
        return gamma * x[0] - x[1] - x[0] * x[2]
    }

    private fun dz(x: Point3): Flt64 {
        return beta * x[2] + x[0] * x[1]
    }
}

data class CoupledLorenzAttractorGenerator(
    val coupledLorenzAttractor: CoupledLorenzAttractor = CoupledLorenzAttractor(),
    private var _x: Point3 = point3(
        Random.nextFlt64(Flt64.zero, Flt64.one),
        Random.nextFlt64(Flt64.zero, Flt64.one),
        Random.nextFlt64(Flt64.zero, Flt64.one)
    ),
    private var _y: Point3 = point3(
        Random.nextFlt64(Flt64.zero, Flt64.one),
        Random.nextFlt64(Flt64.zero, Flt64.one),
        Random.nextFlt64(Flt64.zero, Flt64.one)
    )
) : Generator<Pair<Point3, Point3>> {
    companion object {
        operator fun invoke(
            beta: Flt64 = Flt64(8.0 / 3.0),
            gamma1: Flt64 = Flt64(35.0),
            gamma2: Flt64 = Flt64(1.15),
            epsilon: Flt64 = Flt64(2.85),
            omicron: Flt64 = Flt64(2.85),
            h: Flt64 = Flt64(0.01),
            x: Point3 = point3(
                Random.nextFlt64(Flt64.zero, Flt64.one),
                Random.nextFlt64(Flt64.zero, Flt64.one),
                Random.nextFlt64(Flt64.zero, Flt64.one)
            ),
            y: Point3 = point3(
                Random.nextFlt64(Flt64.zero, Flt64.one),
                Random.nextFlt64(Flt64.zero, Flt64.one),
                Random.nextFlt64(Flt64.zero, Flt64.one)
            )
        ) : CoupledLorenzAttractorGenerator {
            return CoupledLorenzAttractorGenerator(
                CoupledLorenzAttractor(beta, gamma1, gamma2, epsilon, omicron, h),
                x,
                y
            )
        }
    }

    val x by ::_x
    val y by ::_y

    override fun invoke(): Pair<Point3, Point3> {
        val x = _x.copy()
        val y = _y.copy()
        val ret = coupledLorenzAttractor(x to y)
        _x = ret.first
        _y = ret.second
        return x to y
    }
}
