package fuookami.ospf.kotlin.core.testing

import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.Rtn64
import fuookami.ospf.kotlin.math.algebra.number.RtnX

data class GenericNumberCase<V>(
    val name: String,
    private val zeroProvider: () -> V,
    private val oneProvider: () -> V,
    private val twoProvider: () -> V,
    private val fiveProvider: () -> V,
    private val tenProvider: () -> V,
    val converter: IntoValue<V>
) where V : RealNumber<V>, V : NumberField<V> {
    val zero: V get() = zeroProvider()
    val one: V get() = oneProvider()
    val two: V get() = twoProvider()
    val five: V get() = fiveProvider()
    val ten: V get() = tenProvider()
}

object GenericNumberCases {
    val flt64 = GenericNumberCase(
        name = "Flt64",
        zeroProvider = { Flt64.zero },
        oneProvider = { Flt64.one },
        twoProvider = { Flt64.two },
        fiveProvider = { Flt64.five },
        tenProvider = { Flt64.ten },
        converter = IntoValue.Identity
    )

    val rtn64 = GenericNumberCase(
        name = "Rtn64",
        zeroProvider = { Rtn64(Flt64.zero.toInt64(), Flt64.one.toInt64()) },
        oneProvider = { Rtn64(Flt64.one.toInt64(), Flt64.one.toInt64()) },
        twoProvider = { Rtn64(Flt64.two.toInt64(), Flt64.one.toInt64()) },
        fiveProvider = { Rtn64(Flt64.five.toInt64(), Flt64.one.toInt64()) },
        tenProvider = { Rtn64(Flt64.ten.toInt64(), Flt64.one.toInt64()) },
        converter = object : IntoValue<Rtn64> {
            override fun intoValue(value: Flt64): Rtn64 = value.toRtn64()
            override val zero: Rtn64 get() = Rtn64(Flt64.zero.toInt64(), Flt64.one.toInt64())
            override val one: Rtn64 get() = Rtn64(Flt64.one.toInt64(), Flt64.one.toInt64())
            override fun fromValue(value: Rtn64): Flt64 = value.toFlt64()
        }
    )

    val fltX = GenericNumberCase(
        name = "FltX",
        zeroProvider = { FltX.zero },
        oneProvider = { FltX.one },
        twoProvider = { FltX.two },
        fiveProvider = { FltX.five },
        tenProvider = { FltX.ten },
        converter = object : IntoValue<FltX> {
            override fun intoValue(value: Flt64): FltX = value.toFltX()
            override val zero: FltX get() = FltX.zero
            override val one: FltX get() = FltX.one
            override fun fromValue(value: FltX): Flt64 = value.toFlt64()
        }
    )

    val rtnX = GenericNumberCase(
        name = "RtnX",
        zeroProvider = { RtnX(0, 1) },
        oneProvider = { RtnX(1, 1) },
        twoProvider = { RtnX(2, 1) },
        fiveProvider = { RtnX(5, 1) },
        tenProvider = { RtnX(10, 1) },
        converter = object : IntoValue<RtnX> {
            override fun intoValue(value: Flt64): RtnX = value.toRtnX()
            override val zero: RtnX get() = RtnX(0, 1)
            override val one: RtnX get() = RtnX(1, 1)
            override fun fromValue(value: RtnX): Flt64 = value.toFlt64()
        }
    )
}
