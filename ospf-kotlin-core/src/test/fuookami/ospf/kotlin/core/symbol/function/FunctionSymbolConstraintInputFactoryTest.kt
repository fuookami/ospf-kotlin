package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.core.model.basic.ConstraintRelation
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.testing.*
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.functional.*
import kotlin.test.*
import kotlinx.coroutines.runBlocking

class FunctionSymbolConstraintInputFactoryTest {
    @Test
    fun ifAndIfThenFactoryShouldRegisterConstraintsForFourNumberTypes() {
        runCase(GenericNumberCases.flt64)
        runCase(GenericNumberCases.fltX)
        runCase(GenericNumberCases.rtn64)
        runCase(GenericNumberCases.rtnX)
    }

    private fun <V> runCase(numberCase: GenericNumberCase<V>)
            where V : RealNumber<V>, V : NumberField<V> {
        val x = RealVar("${numberCase.name.lowercase()}_if_input_x")
        val y = RealVar("${numberCase.name.lowercase()}_if_input_y")

        val model = LinearMetaModel<V>(
            name = "generic-if-input-${numberCase.name.lowercase()}",
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

            val lhsRange = ValueRange(
                lb = numberCase.converter.intoValue(Flt64(-1000.0)),
                ub = numberCase.converter.intoValue(Flt64(1000.0)),
                lbInterval = Interval.Closed,
                ubInterval = Interval.Closed,
                constants = numberCase.zero.constants
            ).value!!
            val input = LinearConstraintInput.from(
                relation = LinearInequality(xPoly, onePoly, Comparison.LE, "${numberCase.name.lowercase()}_if_input_ineq"),
                lhsRange = lhsRange,
                rhsConstant = numberCase.one
            )

            val ifFunc = IfFunction.from(
                inequality = input,
                converter = numberCase.converter,
                bigM = numberCase.ten,
                tolerance = numberCase.one,
                strictBoundary = numberCase.one,
                name = "if_input_${numberCase.name.lowercase()}"
            )
            val ifThenFunc = IfThenFunction.from(
                inequality = input,
                converter = numberCase.converter,
                thenPoly = yPoly,
                bigM = numberCase.ten,
                tolerance = numberCase.one,
                strictBoundary = numberCase.one,
                name = "ifthen_input_${numberCase.name.lowercase()}"
            )

            assertTrue(ifFunc.registerAuxiliaryTokens(model.tokens) is Ok, "${numberCase.name}: IfFunction auxiliary tokens should succeed")
            assertTrue(ifThenFunc.registerAuxiliaryTokens(model.tokens) is Ok, "${numberCase.name}: IfThenFunction auxiliary tokens should succeed")
            assertTrue(model.minimize(xPoly) is Ok, "${numberCase.name}: add objective should succeed")
            val mechanismResult = runBlocking {
                LinearMechanismModel.invoke<V>(metaModel = model, concurrent = false)
            }
            assertTrue(mechanismResult is Ok, "${numberCase.name}: dump mechanism model should succeed")
            val mechanismModel = mechanismResult.value

            assertConstraintRegistration(
                numberCase = numberCase,
                mechanismModel = mechanismModel,
                label = "if",
                register = { ifFunc.registerConstraints(mechanismModel) },
                expectedInputTokenNames = setOf(x.name)
            )
            assertConstraintRegistration(
                numberCase = numberCase,
                mechanismModel = mechanismModel,
                label = "ifThen",
                register = { ifThenFunc.registerConstraints(mechanismModel) },
                expectedInputTokenNames = setOf(x.name, y.name)
            )
        } finally {
            model.close()
        }
    }

    private fun <V> assertConstraintRegistration(
        numberCase: GenericNumberCase<V>,
        mechanismModel: LinearMechanismModel<V>,
        label: String,
        register: () -> Try,
        expectedInputTokenNames: Set<String>
    ) where V : RealNumber<V>, V : NumberField<V> {
        val before = mechanismModel.constraints.size
        assertTrue(register() is Ok, "${numberCase.name}: $label registerConstraints should succeed")
        val after = mechanismModel.constraints.size
        assertTrue(after > before, "${numberCase.name}: $label should append constraints")

        @Suppress("UNCHECKED_CAST")
        val appended = mechanismModel.constraints.subList(before, after).map { it as LinearConstraintImpl<V> }
        val coefficients = appended.flatMap { constraint -> constraint.lhs.map { cell -> cell.coefficient } }
        assertTrue(coefficients.isNotEmpty(), "${numberCase.name}: $label appended constraints should contain coefficients")
        assertTrue(
            coefficients.all { it::class == numberCase.one::class },
            "${numberCase.name}: $label coefficient type should stay V instead of leaking Flt64"
        )
        assertTrue(
            appended.all { it.rhs::class == numberCase.one::class },
            "${numberCase.name}: $label rhs type should stay V"
        )
        assertTrue(
            appended.any { constraint ->
                constraint.lhs.any { cell -> cell.token.variable.name in expectedInputTokenNames }
            },
            "${numberCase.name}: $label should reference input token(s) ${expectedInputTokenNames.joinToString()}"
        )
        assertTrue(
            appended.any {
                it.sign == ConstraintRelation.LessEqual ||
                    it.sign == ConstraintRelation.GreaterEqual ||
                    it.sign == ConstraintRelation.Equal
            },
            "${numberCase.name}: $label should produce comparable sign"
        )
    }
}
