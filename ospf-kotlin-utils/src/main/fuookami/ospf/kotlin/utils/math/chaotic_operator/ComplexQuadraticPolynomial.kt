package fuookami.ospf.kotlin.utils.math.chaotic_operator

import kotlin.random.*
import org.kotlinmath.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.geometry.*
import fuookami.ospf.kotlin.utils.functional.*

data class ComplexQuadraticPolynomial(
    val c: Point2 = point2(
        Random.nextFlt64(-Flt64.one, Flt64.one),
        Random.nextFlt64(-Flt64.one, Flt64.one)
    ),
    val d: Flt64 = Flt64.two
) : Extractor<Point2, Point2> {
    override operator fun invoke(x: Point2): Point2 {
        val complexNumber = pow(complex(x[0].value, x[1].value), complex(d.value, 0.0)) + complex(c[0].value, c[1].value)
        return point2(
            Flt64(complexNumber.re),
            Flt64(complexNumber.im)
        )
    }
}

data class ComplexQuadraticPolynomialGenerator(
    val complexQuadraticPolynomial: ComplexQuadraticPolynomial = ComplexQuadraticPolynomial(),
    private var _x: Point2 = point2(
        Random.nextFlt64(-Flt64.one, Flt64.one),
        Random.nextFlt64(-Flt64.one, Flt64.one)
    )
) : Generator<Point2> {
    companion object {
        operator fun invoke(
            c: Point2 = point2(
                Random.nextFlt64(-Flt64.one, Flt64.one),
                Random.nextFlt64(-Flt64.one, Flt64.one)
            ),
            d: Flt64 = Flt64.two,
            x: Point2 = point2(
                Random.nextFlt64(-Flt64.one, Flt64.one),
                Random.nextFlt64(-Flt64.one, Flt64.one)
            )
        ): ComplexQuadraticPolynomialGenerator {
            return ComplexQuadraticPolynomialGenerator(
                ComplexQuadraticPolynomial(c, d),
                x
            )
        }
    }

    val x by ::_x

    override operator fun invoke(): Point2 {
        val x = _x.copy()
        _x = complexQuadraticPolynomial(x)
        return x
    }
}
