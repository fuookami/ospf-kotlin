package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.intermediate_model.LinearMechanismModel
import fuookami.ospf.kotlin.core.intermediate_model.LinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.LinearConstraintImpl
import fuookami.ospf.kotlin.core.model.mechanism.QuadraticConstraintImpl
import fuookami.ospf.kotlin.core.model.mechanism.QuadraticMechanismModel
import fuookami.ospf.kotlin.core.model.mechanism.QuadraticMetaModel
import fuookami.ospf.kotlin.core.testing.GenericNumberCase
import fuookami.ospf.kotlin.core.testing.GenericNumberCases
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.utils.functional.Ok
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

class FunctionSymbolGenericRegistrationTest {
    @Test
    fun absAndAndAndIfAndOrAndNotAndXorShouldRegisterConstraintsForFourNumberTypes() {
        runFunctionCase(GenericNumberCases.flt64)
        runFunctionCase(GenericNumberCases.fltX)
        runFunctionCase(GenericNumberCases.rtn64)
        runFunctionCase(GenericNumberCases.rtnX)
    }

    @Test
    fun andFunctionDefaultToleranceShouldWorkForRtn64AfterRationalFix() {
        val x = RealVar("rtn64_default_tol_x")
        val xPoly = LinearPolynomial(
            monomials = listOf(LinearMonomial(GenericNumberCases.rtn64.one, x)),
            constant = GenericNumberCases.rtn64.zero
        )
        AndFunction(
            polynomials = listOf(xPoly),
            converter = GenericNumberCases.rtn64.converter,
            name = "and_rtn64_default_tol"
        )
    }

    @Test
    fun productShouldRegisterConstraintsForFourNumberTypesOnQuadraticMechanismModel() {
        runProductCase(GenericNumberCases.flt64)
        runProductCase(GenericNumberCases.fltX)
        runProductCase(GenericNumberCases.rtn64)
        runProductCase(GenericNumberCases.rtnX)
    }

    @Test
    fun quadraticLinearShouldRegisterConstraintsForFourNumberTypesOnQuadraticMechanismModel() {
        runQuadraticLinearCase(GenericNumberCases.flt64)
        runQuadraticLinearCase(GenericNumberCases.fltX)
        runQuadraticLinearCase(GenericNumberCases.rtn64)
        runQuadraticLinearCase(GenericNumberCases.rtnX)
    }

    private fun <V> runFunctionCase(numberCase: GenericNumberCase<V>)
            where V : RealNumber<V>, V : NumberField<V> {
        val x = RealVar("${numberCase.name.lowercase()}_fn_x")
        val y = RealVar("${numberCase.name.lowercase()}_fn_y")

        val model = LinearMetaModel<V>(
            name = "generic-function-${numberCase.name.lowercase()}",
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

            val abs = AbsFunction(
                polynomial = xPoly,
                converter = numberCase.converter,
                name = "abs_${numberCase.name.lowercase()}"
            )
            val and = AndFunction(
                polynomials = listOf(xPoly, yPoly),
                converter = numberCase.converter,
                bigM = numberCase.ten,
                tolerance = numberCase.one,
                strictBoundary = numberCase.one,
                name = "and_${numberCase.name.lowercase()}"
            )
            val ifFunc = IfFunction(
                condition = xPoly,
                converter = numberCase.converter,
                bigM = numberCase.ten,
                tolerance = numberCase.one,
                strictBoundary = numberCase.one,
                name = "if_${numberCase.name.lowercase()}"
            )
            val or = OrFunction(
                polynomials = listOf(xPoly, yPoly),
                converter = numberCase.converter,
                bigM = numberCase.ten,
                tolerance = numberCase.one,
                strictBoundary = numberCase.one,
                name = "or_${numberCase.name.lowercase()}"
            )
            val not = NotFunction(
                polynomial = xPoly,
                converter = numberCase.converter,
                bigM = numberCase.ten,
                tolerance = numberCase.one,
                strictBoundary = numberCase.one,
                name = "not_${numberCase.name.lowercase()}"
            )
            val xor = XorFunction(
                polynomials = listOf(xPoly, yPoly),
                converter = numberCase.converter,
                bigM = numberCase.ten,
                tolerance = numberCase.one,
                strictBoundary = numberCase.one,
                name = "xor_${numberCase.name.lowercase()}"
            )
            assertTrue(abs.registerAuxiliaryTokens(model.tokens) is Ok, "${numberCase.name}: abs auxiliary tokens should succeed")
            assertTrue(and.registerAuxiliaryTokens(model.tokens) is Ok, "${numberCase.name}: and auxiliary tokens should succeed")
            assertTrue(ifFunc.registerAuxiliaryTokens(model.tokens) is Ok, "${numberCase.name}: if auxiliary tokens should succeed")
            assertTrue(or.registerAuxiliaryTokens(model.tokens) is Ok, "${numberCase.name}: or auxiliary tokens should succeed")
            assertTrue(not.registerAuxiliaryTokens(model.tokens) is Ok, "${numberCase.name}: not auxiliary tokens should succeed")
            assertTrue(xor.registerAuxiliaryTokens(model.tokens) is Ok, "${numberCase.name}: xor auxiliary tokens should succeed")

            assertTrue(model.minimize(xPoly) is Ok, "${numberCase.name}: add objective should succeed")

            @Suppress("DEPRECATION")
            val mechanismResult = runBlocking {
                LinearMechanismModel.invoke<V>(metaModel = model, concurrent = false)
            }
            assertTrue(mechanismResult is Ok, "${numberCase.name}: dump mechanism model should succeed")
            val mechanismModel = mechanismResult.value

            val before = mechanismModel.constraints.size
            assertTrue(abs.registerConstraints(mechanismModel) is Ok, "${numberCase.name}: abs registerConstraints should succeed")
            assertTrue(and.registerConstraints(mechanismModel) is Ok, "${numberCase.name}: and registerConstraints should succeed")
            assertTrue(ifFunc.registerConstraints(mechanismModel) is Ok, "${numberCase.name}: if registerConstraints should succeed")
            assertTrue(or.registerConstraints(mechanismModel) is Ok, "${numberCase.name}: or registerConstraints should succeed")
            assertTrue(not.registerConstraints(mechanismModel) is Ok, "${numberCase.name}: not registerConstraints should succeed")
            assertTrue(xor.registerConstraints(mechanismModel) is Ok, "${numberCase.name}: xor registerConstraints should succeed")
            val after = mechanismModel.constraints.size

            assertTrue(after > before, "${numberCase.name}: function constraints should be appended")

            val newConstraint = mechanismModel.constraints.last() as LinearConstraintImpl<V>
            val firstCell = newConstraint.lhs.first()
            val coefficient = firstCell.coefficient
            assertTrue(coefficient::class == numberCase.one::class,
                "${numberCase.name}: constraint coefficient type should stay V instead of leaking Flt64")
        } finally {
            model.close()
        }
    }

    private fun <V> runProductCase(numberCase: GenericNumberCase<V>)
            where V : RealNumber<V>, V : NumberField<V> {
        val x = RealVar("${numberCase.name.lowercase()}_prod_x")
        val y = RealVar("${numberCase.name.lowercase()}_prod_y")

        val model = QuadraticMetaModel<V>(
            name = "generic-product-${numberCase.name.lowercase()}",
            converter = numberCase.converter
        )

        try {
            assertTrue(model.add(listOf(x, y)) is Ok, "${numberCase.name}: product add variables should succeed")

            val xPoly = LinearPolynomial(
                monomials = listOf(LinearMonomial(numberCase.one, x)),
                constant = numberCase.zero
            )
            val yPoly = LinearPolynomial(
                monomials = listOf(LinearMonomial(numberCase.one, y)),
                constant = numberCase.zero
            )
            val objective = QuadraticPolynomial(
                monomials = listOf(QuadraticMonomial.quadratic(numberCase.one, x, y)),
                constant = numberCase.zero
            )
            assertTrue(model.minimize(objective) is Ok, "${numberCase.name}: product add objective should succeed")

            val product = ProductFunction(
                left = xPoly,
                right = yPoly,
                converter = numberCase.converter,
                name = "product_${numberCase.name.lowercase()}"
            )
            assertTrue(product.registerAuxiliaryTokens(model.tokens) is Ok, "${numberCase.name}: product auxiliary tokens should succeed")

            @Suppress("DEPRECATION")
            val mechanismResult = runBlocking {
                QuadraticMechanismModel.invoke<V>(metaModel = model, concurrent = false)
            }
            assertTrue(mechanismResult is Ok, "${numberCase.name}: product dump quadratic mechanism model should succeed")
            val mechanismModel = mechanismResult.value

            val before = mechanismModel.constraints.size
            assertTrue(product.registerConstraints(mechanismModel) is Ok, "${numberCase.name}: product registerConstraints should succeed")
            val after = mechanismModel.constraints.size
            assertTrue(after > before, "${numberCase.name}: product constraints should be appended")

            val newConstraint = mechanismModel.constraints.last() as QuadraticConstraintImpl<V>
            val firstCell = newConstraint.lhs.first()
            val coefficient = firstCell.coefficient
            assertTrue(coefficient::class == numberCase.one::class,
                "${numberCase.name}: product constraint coefficient type should stay V instead of leaking Flt64")
        } finally {
            model.close()
        }
    }

    private fun <V> runQuadraticLinearCase(numberCase: GenericNumberCase<V>)
            where V : RealNumber<V>, V : NumberField<V> {
        val x = RealVar("${numberCase.name.lowercase()}_qlin_x")
        val y = RealVar("${numberCase.name.lowercase()}_qlin_y")

        val model = QuadraticMetaModel<V>(
            name = "generic-quadratic-linear-${numberCase.name.lowercase()}",
            converter = numberCase.converter
        )

        try {
            assertTrue(model.add(listOf(x, y)) is Ok, "${numberCase.name}: qlinear add variables should succeed")

            val objective = QuadraticPolynomial(
                monomials = listOf(QuadraticMonomial.quadratic(numberCase.one, x, y)),
                constant = numberCase.zero
            )
            assertTrue(model.minimize(objective) is Ok, "${numberCase.name}: qlinear add objective should succeed")

            val polynomial = QuadraticPolynomial(
                monomials = listOf(
                    QuadraticMonomial.quadratic(numberCase.one, x, y),
                    QuadraticMonomial.linear(numberCase.one, x)
                ),
                constant = numberCase.zero
            )
            val function = QuadraticLinearFunction(
                polynomial = polynomial,
                converter = numberCase.converter,
                name = "qlinear_${numberCase.name.lowercase()}"
            )
            assertTrue(function.registerAuxiliaryTokens(model.tokens) is Ok, "${numberCase.name}: qlinear auxiliary tokens should succeed")

            @Suppress("DEPRECATION")
            val mechanismResult = runBlocking {
                QuadraticMechanismModel.invoke<V>(metaModel = model, concurrent = false)
            }
            assertTrue(mechanismResult is Ok, "${numberCase.name}: qlinear dump quadratic mechanism model should succeed")
            val mechanismModel = mechanismResult.value

            val before = mechanismModel.constraints.size
            assertTrue(function.registerConstraints(mechanismModel) is Ok, "${numberCase.name}: qlinear registerConstraints should succeed")
            val after = mechanismModel.constraints.size
            assertTrue(after > before, "${numberCase.name}: qlinear constraints should be appended")

            val newConstraint = mechanismModel.constraints.last() as QuadraticConstraintImpl<V>
            val firstCell = newConstraint.lhs.first()
            val coefficient = firstCell.coefficient
            assertTrue(coefficient::class == numberCase.one::class,
                "${numberCase.name}: qlinear constraint coefficient type should stay V instead of leaking Flt64")
        } finally {
            model.close()
        }
    }
}
