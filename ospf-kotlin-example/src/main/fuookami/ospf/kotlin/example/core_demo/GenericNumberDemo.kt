package fuookami.ospf.kotlin.example.core_demo

import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.mechanism.LinearMechanismModel
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.QuadraticMechanismModel
import fuookami.ospf.kotlin.core.model.mechanism.QuadraticMetaModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.Rtn64
import fuookami.ospf.kotlin.math.algebra.number.RtnX
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequalityOf
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.utils.functional.Ok
import kotlinx.coroutines.runBlocking

private data class GenericNumberCase<V>(
    val name: String,
    val converter: IntoValue<V>
) where V : RealNumber<V>, V : NumberField<V>

private object GenericNumberCases {
    val flt64 = GenericNumberCase(
        name = "Flt64",
        converter = IntoValue.Identity
    )

    val rtn64 = GenericNumberCase(
        name = "Rtn64",
        converter = object : IntoValue<Rtn64> {
            override fun intoValue(value: Flt64): Rtn64 = value.toRtn64()
            override val zero: Rtn64 get() = Rtn64(Flt64.zero.toInt64(), Flt64.one.toInt64())
            override val one: Rtn64 get() = Rtn64(Flt64.one.toInt64(), Flt64.one.toInt64())
            override fun fromValue(value: Rtn64): Flt64 = value.toFlt64()
        }
    )

    val fltX = GenericNumberCase(
        name = "FltX",
        converter = object : IntoValue<FltX> {
            override fun intoValue(value: Flt64): FltX = value.toFltX()
            override val zero: FltX get() = FltX.zero
            override val one: FltX get() = FltX.one
            override fun fromValue(value: FltX): Flt64 = value.toFlt64()
        }
    )

    val rtnX = GenericNumberCase(
        name = "RtnX",
        converter = object : IntoValue<RtnX> {
            override fun intoValue(value: Flt64): RtnX = value.toRtnX()
            override val zero: RtnX get() = RtnX(0, 1)
            override val one: RtnX get() = RtnX(1, 1)
            override fun fromValue(value: RtnX): Flt64 = value.toFlt64()
        }
    )
}

data object GenericNumberDemo {
    fun runBuildAndDump() {
        runBlocking {
            buildLinear(GenericNumberCases.flt64)
            buildLinear(GenericNumberCases.rtn64)
            buildLinear(GenericNumberCases.fltX)
            buildLinear(GenericNumberCases.rtnX)

            buildQuadratic(GenericNumberCases.flt64)
            buildQuadratic(GenericNumberCases.rtn64)
            buildQuadratic(GenericNumberCases.fltX)
            buildQuadratic(GenericNumberCases.rtnX)
        }
    }

    private suspend fun <V> buildLinear(numberCase: GenericNumberCase<V>)
            where V : RealNumber<V>, V : NumberField<V> {
        val x = RealVar("${numberCase.name.lowercase()}_demo_linear_x")
        val y = RealVar("${numberCase.name.lowercase()}_demo_linear_y")
        val model = LinearMetaModel<V>(
            name = "generic-number-demo-linear-${numberCase.name.lowercase()}",
            objectCategory = ObjectCategory.Minimum,
            converter = numberCase.converter
        )

        try {
            check(model.add(listOf(x, y)) is Ok)

            val lhs = LinearPolynomial(
                monomials = listOf(
                    LinearMonomial(numberCase.converter.one, x),
                    LinearMonomial(numberCase.converter.intoValue(Flt64.two), y)
                ),
                constant = numberCase.converter.zero
            )
            val rhs = LinearPolynomial<V>(emptyList(), numberCase.converter.zero)
            val relation = LinearInequality(lhs = lhs, rhs = rhs, comparison = Comparison.LE)
            check(model.addConstraint(relation = relation, name = "c_${numberCase.name.lowercase()}") is Ok)

            val objective = LinearPolynomial(
                monomials = listOf(
                    LinearMonomial(numberCase.converter.intoValue(Flt64.two), x),
                    LinearMonomial(numberCase.converter.one, y)
                ),
                constant = numberCase.converter.zero
            )
            check(model.minimize(objective) is Ok)

            @Suppress("DEPRECATION")
            val mechanismResult = LinearMechanismModel.invoke<V>(metaModel = model, concurrent = false)
            check(mechanismResult is Ok)
        } finally {
            model.close()
        }
    }

    private suspend fun <V> buildQuadratic(numberCase: GenericNumberCase<V>)
            where V : RealNumber<V>, V : NumberField<V> {
        val x = RealVar("${numberCase.name.lowercase()}_demo_quad_x")
        val y = RealVar("${numberCase.name.lowercase()}_demo_quad_y")
        val model = QuadraticMetaModel<V>(
            name = "generic-number-demo-quadratic-${numberCase.name.lowercase()}",
            objectCategory = ObjectCategory.Minimum,
            converter = numberCase.converter
        )

        try {
            check(model.add(listOf(x, y)) is Ok)

            val lhs = QuadraticPolynomial(
                monomials = listOf(QuadraticMonomial.quadratic(numberCase.converter.one, x, y)),
                constant = numberCase.converter.zero
            )
            val rhs = QuadraticPolynomial<V>(emptyList(), numberCase.converter.zero)
            val relation = QuadraticInequalityOf(lhs = lhs, rhs = rhs, comparison = Comparison.LE)
            check(model.addConstraint(relation = relation, name = "qc_${numberCase.name.lowercase()}") is Ok)

            val objective = QuadraticPolynomial(
                monomials = listOf(
                    QuadraticMonomial.quadratic(numberCase.converter.one, x, y),
                    QuadraticMonomial.linear(numberCase.converter.one, x)
                ),
                constant = numberCase.converter.zero
            )
            check(model.minimize(objective) is Ok)

            @Suppress("DEPRECATION")
            val mechanismResult = QuadraticMechanismModel.invoke<V>(metaModel = model, concurrent = false)
            check(mechanismResult is Ok)
        } finally {
            model.close()
        }
    }
}
