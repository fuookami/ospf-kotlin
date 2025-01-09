package fuookami.ospf.kotlin.utils.math.fractal_operator

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.geometry.*
import fuookami.ospf.kotlin.utils.functional.*

class MandelbrotSet(
    val c: Point2 = Point2(Flt64.one, Flt64.one)
) {
    companion object {
        operator fun invoke(real: Flt64, imag: Flt64): MandelbrotSet {
            return MandelbrotSet(Point2(real, imag))
        }
    }

    operator fun invoke(z: Point2): Point2 {
        val real = z[0].pow(2) - z[1].pow(2) + c[0]
        val imag = Flt64.two * z[0] * z[1] + c[1]
        return Point2(real, imag)
    }
}

data class MandelbrotSetGenerator(
    val mandelbrotSet: MandelbrotSet = MandelbrotSet(),
    private var _z: Point2 = point2()
): Generator<Point2> {
    companion object {
        operator fun invoke(
            real: Flt64,
            imag: Flt64,
            z: Point2 = point2()
        ): MandelbrotSetGenerator {
            return MandelbrotSetGenerator(MandelbrotSet(Point2(real, imag)), z)
        }
    }

    val z by ::_z

    override fun invoke(): Point2 {
        val z = _z.copy()
        _z = mandelbrotSet(_z)
        return z
    }
}
