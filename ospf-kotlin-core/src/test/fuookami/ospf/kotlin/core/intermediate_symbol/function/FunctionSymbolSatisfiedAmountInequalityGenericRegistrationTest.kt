package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.intermediate_model.LinearMechanismModel
import fuookami.ospf.kotlin.core.intermediate_model.LinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.LinearConstraintImpl
import fuookami.ospf.kotlin.core.model.mechanism.LinearConstraintInputV
import fuookami.ospf.kotlin.core.testing.GenericNumberCase
import fuookami.ospf.kotlin.core.testing.GenericNumberCases
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.value_range.Interval
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.functional.Ok
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

class FunctionSymbolSatisfiedAmountInequalityGenericRegistrationTest {
    @Test
    fun anyAndAllShouldRegisterConstraintsForFourNumberTypes() {
        runSatisfiedAmountCase(GenericNumberCases.flt64)
        runSatisfiedAmountCase(GenericNumberCases.fltX)
        runSatisfiedAmountCase(GenericNumberCases.rtn64)
        runSatisfiedAmountCase(GenericNumberCases.rtnX)
    }

    private fun <V> runSatisfiedAmountCase(numberCase: GenericNumberCase<V>)
            where V : RealNumber<V>, V : NumberField<V> {
        val x = RealVar("${numberCase.name.lowercase()}_satineq_x")
        val y = RealVar("${numberCase.name.lowercase()}_satineq_y")

        val model = LinearMetaModel<V>(
            name = "generic-satineq-${numberCase.name.lowercase()}",
            converter = numberCase.converter
        )

        try {
            assertTrue(model.add(listOf(x, y)) is Ok, "${numberCase.name}: add variables should succeed")

            val xPoly = LinearPolynomial(
                monomials = listOf(LinearMonomial(numberCase.one, x)),
                constant = numberCase.zero
            )
            val yPoly = LinearPolynomial(
                monomials = listOf(LinearMonomial(numberCase.one, y)),
                constant = numberCase.zero
            )
            val onePoly = LinearPolynomial<V>(emptyList(), numberCase.one)
            val zeroPoly = LinearPolynomial<V>(emptyList(), numberCase.zero)

            val lhsRange = ValueRange(
                lb = numberCase.converter.intoValue(Flt64(-1000.0)),
                ub = numberCase.converter.intoValue(Flt64(1000.0)),
                lbInterval = Interval.Closed,
                ubInterval = Interval.Closed,
                constants = numberCase.zero.constants
            ).value!!
            val inputs = listOf(
                LinearConstraintInputV.from(
                    relation = LinearInequality(xPoly, onePoly, Comparison.LE, "${numberCase.name.lowercase()}_ineq_x_le_1"),
                    lhsRange = lhsRange,
                    rhsConstant = numberCase.one
                ),
                LinearConstraintInputV.from(
                    relation = LinearInequality(yPoly, zeroPoly, Comparison.GE, "${numberCase.name.lowercase()}_ineq_y_ge_0"),
                    lhsRange = lhsRange,
                    rhsConstant = numberCase.zero
                )
            )

            val any = AnyFunction.typed(
                inputs = inputs,
                converter = numberCase.converter,
                name = "any_${numberCase.name.lowercase()}"
            )
            val all = AllFunction.typed(
                inputs = inputs,
                converter = numberCase.converter,
                name = "all_${numberCase.name.lowercase()}"
            )

            assertTrue(any.registerAuxiliaryTokens(model.tokens) is Ok, "${numberCase.name}: AnyFunction auxiliary tokens should succeed")
            assertTrue(all.registerAuxiliaryTokens(model.tokens) is Ok, "${numberCase.name}: AllFunction auxiliary tokens should succeed")
            assertTrue(model.minimize(xPoly) is Ok, "${numberCase.name}: add objective should succeed")

            @Suppress("DEPRECATION")
            val mechanismResult = runBlocking {
                LinearMechanismModel.invoke<V>(metaModel = model, concurrent = false)
            }
            assertTrue(mechanismResult is Ok, "${numberCase.name}: dump mechanism model should succeed")
            val mechanismModel = mechanismResult.value

            val before = mechanismModel.constraints.size
            assertTrue(any.registerConstraints(mechanismModel) is Ok, "${numberCase.name}: AnyFunction registerConstraints should succeed")
            assertTrue(all.registerConstraints(mechanismModel) is Ok, "${numberCase.name}: AllFunction registerConstraints should succeed")
            val after = mechanismModel.constraints.size

            assertTrue(after > before, "${numberCase.name}: satisfied-amount-inequality constraints should be appended")

            val newConstraint = mechanismModel.constraints.last() as LinearConstraintImpl<V>
            val firstCell = newConstraint.lhs.first()
            val coefficient = firstCell.coefficient
            assertTrue(
                coefficient::class == numberCase.one::class,
                "${numberCase.name}: satisfied-amount-inequality coefficient type should stay V instead of leaking Flt64"
            )
        } finally {
            model.close()
        }
    }
}
