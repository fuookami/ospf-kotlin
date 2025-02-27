package fuookami.ospf.kotlin.utils.math.chaotic_operator

import kotlin.random.*
import org.kotlinmath.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.geometry.*
import fuookami.ospf.kotlin.utils.functional.*

data object ComplexSquaringMap: Extractor<Point2, Point2> {
    override operator fun invoke(x: Point2): Point2 {
        val complexNumber = pow(complex(x[0].value, x[1].value), complex(2.0, 0.0))
        return Point2(
            Flt64(complexNumber.re),
            Flt64(complexNumber.im)
        )
    }
}

data class ComplexSquaringMapGenerator(
    val complexSquaringMap: ComplexSquaringMap = ComplexSquaringMap,
    private var _x: Point2 = point2(
        Random.nextFlt64(-Flt64.one, Flt64.one),
        Random.nextFlt64(-Flt64.one, Flt64.one)
    )
) : Generator<Point2> {
    companion object {
        operator fun invoke(
            complexSquaringMap: ComplexSquaringMap = ComplexSquaringMap,
            x: Point2 = point2(
                Random.nextFlt64(-Flt64.one, Flt64.one),
                Random.nextFlt64(-Flt64.one, Flt64.one)
            )
        ): ComplexSquaringMapGenerator {
            return ComplexSquaringMapGenerator(
                complexSquaringMap,
                x
            )
        }
    }

    val x by ::_x

    override operator fun invoke(): Point2 {
        val x = _x.copy()
        _x = ComplexSquaringMap(x)
        return x
    }
}
