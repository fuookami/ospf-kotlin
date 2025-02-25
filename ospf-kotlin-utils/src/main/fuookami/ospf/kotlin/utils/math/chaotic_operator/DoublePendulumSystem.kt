package fuookami.ospf.kotlin.utils.math.chaotic_operator

import kotlin.random.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.geometry.*
import fuookami.ospf.kotlin.utils.functional.*

data class DoublePendulumSystem(
    val m: Flt64 = Flt64(10.0),
    val l: Flt64 = Flt64(1.0),
    val g: Flt64 = Flt64(9.80665),
    val h: Flt64 = Flt64(0.01)
) {
    operator fun invoke(x: Point2, y: Point2): Pair<Point2, Point2> {
        val theta1 = x[0]
        val omega1 = x[1]
        val theta2 = y[0]
        val omega2 = y[1]
        val sinTemp = (theta1 - theta2).sin()
        val cosTemp = (theta1 - theta2).cos()
        val dThetaDenominator = (m * l.sqr()) * (Flt64(16) - Flt64(9) * cosTemp.sqr())
        val dTheta1 = Flt64(6) * (Flt64(2) * omega1 - Flt64(3) * cosTemp * omega2) / dThetaDenominator
        val dTheta2 = Flt64(6) * (Flt64(8) * omega2 - Flt64(3) * cosTemp * omega1) / dThetaDenominator
        val dOmegaCoefficient = -m * l.sqr() / Flt64.two
        val dOmega1 = dOmegaCoefficient * (dTheta1 * dTheta2 * sinTemp + Flt64(3) * g / l * theta1.sin())
        val dOmega2 = dOmegaCoefficient * (-dTheta1 * dTheta2 * sinTemp + g / l * theta2.sin())
        return point2(
            theta1 + h * dTheta1,
            omega1 + h * dOmega1
        ) to point2(
            omega2 + h * dTheta2,
            omega2 + h * dOmega2
        )
    }
}

data class DoublePendulumSystemGenerator(
    val doublePendulumSystem: DoublePendulumSystem = DoublePendulumSystem(),
    private var _x: Point2 = point2(
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.pi / Flt64.two),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.pi / Flt64.two)
    ),
    private var _y: Point2 = point2(
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.pi / Flt64.two),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.pi / Flt64.two)
    )
) : Generator<Pair<Point2, Point2>> {
    companion object {
        operator fun invoke(
            m: Flt64,
            l: Flt64,
            g: Flt64,
            h: Flt64,
            x: Point2 = point2(
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.pi / Flt64.two),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.pi / Flt64.two)
            ),
            y: Point2 = point2(
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.pi / Flt64.two),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.pi / Flt64.two)
            )
        ): DoublePendulumSystemGenerator {
            return DoublePendulumSystemGenerator(
                DoublePendulumSystem(m, l, g, h),
                x,
                y
            )
        }
    }

    val x by ::_x
    val y by ::_y

    override operator fun invoke(): Pair<Point2, Point2> {
        val x = _x.copy()
        val y = _y.copy()
        val (x1, y1) = doublePendulumSystem(x, y)
        _x = x1
        _y = y1
        return x to y
    }
}
