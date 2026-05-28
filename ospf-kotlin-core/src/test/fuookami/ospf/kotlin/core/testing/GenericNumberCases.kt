package fuookami.ospf.kotlin.core.testing

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue

class GenericNumberCase<V>(
    val name: String,
    zeroProvider: () -> V,
    oneProvider: () -> V,
    twoProvider: () -> V,
    fiveProvider: () -> V,
    tenProvider: () -> V,
    val converter: IntoValue<V>
) where V : RealNumber<V>, V : NumberField<V> {
    val zero: V = zeroProvider()
    val one: V = oneProvider()
    val two: V = twoProvider()
    val five: V = fiveProvider()
    val ten: V = tenProvider()
}

object GenericNumberCases {
    val flt64 = GenericNumberCase(
        name = "Flt64",
        zeroProvider = { Flt64.zero },
        oneProvider = { Flt64.one },
        twoProvider = { Flt64.two },
        fiveProvider = { Flt64.five },
        tenProvider = { Flt64.ten },
        converter = IntoValue.fromConverter(Flt64)
    )

    val rtn64 = GenericNumberCase(
        name = "Rtn64",
        zeroProvider = { Rtn64.zero },
        oneProvider = { Rtn64.one },
        twoProvider = { Rtn64(Flt64.two.toInt64(), Flt64.one.toInt64()) },
        fiveProvider = { Rtn64(Flt64.five.toInt64(), Flt64.one.toInt64()) },
        tenProvider = { Rtn64(Flt64.ten.toInt64(), Flt64.one.toInt64()) },
        converter = IntoValue.fromConverter(Rtn64)
    )

    val fltX = GenericNumberCase(
        name = "FltX",
        zeroProvider = { FltX.zero },
        oneProvider = { FltX.one },
        twoProvider = { FltX.two },
        fiveProvider = { FltX.five },
        tenProvider = { FltX.ten },
        converter = IntoValue.fromConverter(FltX)
    )

    val rtnX = GenericNumberCase(
        name = "RtnX",
        zeroProvider = { RtnX.zero },
        oneProvider = { RtnX.one },
        twoProvider = { RtnX(2, 1) },
        fiveProvider = { RtnX(5, 1) },
        tenProvider = { RtnX(10, 1) },
        converter = IntoValue.fromConverter(RtnX)
    )
}
