/**
 * 双摆系统
 * Double Pendulum System
 *
 * 双摆系统是由两个相连的摆组成的经典力学系统，是展示混沌运动的典型范例。
 * 即使初始条件有微小差异，系统轨迹也会产生截然不同的演化，展现了混沌的敏感性。
 * 常用于混沌动力学教学、非线性力学研究和物理演示。
 *
 * The double pendulum system is a classical mechanical system composed of two connected pendulums, serving as a typical example demonstrating chaotic motion.
 * Even with slight differences in initial conditions, the system trajectories evolve distinctly, demonstrating the sensitivity of chaos.
 * Commonly used for chaos dynamics education, nonlinear mechanics research, and physics demonstrations.
 */
package fuookami.ospf.kotlin.math.chaotic_operator

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

import fuookami.ospf.kotlin.utils.functional.Generator
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.geometry.point2
import fuookami.ospf.kotlin.math.geometry.Point
import fuookami.ospf.kotlin.math.geometry.Dim2
import fuookami.ospf.kotlin.math.nextFlt64
import kotlin.random.Random

/**
 * 双摆系统
 * Double Pendulum System
 */
data class DoublePendulumSystem(
    val m: Flt64 = Flt64(10.0),
    val l: Flt64 = Flt64(1.0),
    val g: Flt64 = Flt64(9.80665),
    val h: Flt64 = Flt64(0.01)
) {
    operator fun invoke(x: Point<Dim2, Flt64>, y: Point<Dim2, Flt64>): Pair<Point<Dim2, Flt64>, Point<Dim2, Flt64>> {
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

/**
 * 双摆系统生成器
 * Double Pendulum System Generator
 */
data class DoublePendulumSystemGenerator(
    val doublePendulumSystem: DoublePendulumSystem = DoublePendulumSystem(),
    private var _x: Point<Dim2, Flt64> = point2(
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.pi / Flt64.two),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.pi / Flt64.two)
    ),
    private var _y: Point<Dim2, Flt64> = point2(
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.pi / Flt64.two),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.pi / Flt64.two)
    )
) : Generator<Pair<Point<Dim2, Flt64>, Point<Dim2, Flt64>>> {
    companion object {
        operator fun invoke(
            m: Flt64,
            l: Flt64,
            g: Flt64,
            h: Flt64,
            x: Point<Dim2, Flt64> = point2(
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.pi / Flt64.two),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.pi / Flt64.two)
            ),
            y: Point<Dim2, Flt64> = point2(
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.pi / Flt64.two),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.pi / Flt64.two)
            )
        ): DoublePendulumSystemGenerator {
            return DoublePendulumSystemGenerator(
                DoublePendulumSystem(
                    m = m,
                    l = l,
                    g = g,
                    h = h
                ),
                x,
                y
            )
        }
    }

    val x by ::_x
    val y by ::_y

    override operator fun invoke(): Pair<Point<Dim2, Flt64>, Point<Dim2, Flt64>> {
        val x = _x.copy()
        val y = _y.copy()
        val (x1, y1) = doublePendulumSystem(x, y)
        _x = x1
        _y = y1
        return x to y
    }
}






