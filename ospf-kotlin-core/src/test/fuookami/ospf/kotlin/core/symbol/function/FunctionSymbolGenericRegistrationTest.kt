package fuookami.ospf.kotlin.core.symbol.function

import kotlin.test.*
import kotlinx.coroutines.runBlocking
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.basic.ConstraintRelation
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.testing.*
import fuookami.ospf.kotlin.core.variable.*

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

    @Test
    fun quadraticMinShouldRegisterConstraintsForFourNumberTypesOnQuadraticMechanismModel() {
        runQuadraticMinCase(GenericNumberCases.flt64)
        runQuadraticMinCase(GenericNumberCases.fltX)
        runQuadraticMinCase(GenericNumberCases.rtn64)
        runQuadraticMinCase(GenericNumberCases.rtnX)
    }

    @Test
    fun quadraticMaskingRangeShouldRegisterConstraintsForFourNumberTypesOnQuadraticMechanismModel() {
        runQuadraticMaskingRangeCase(GenericNumberCases.flt64)
        runQuadraticMaskingRangeCase(GenericNumberCases.fltX)
        runQuadraticMaskingRangeCase(GenericNumberCases.rtn64)
        runQuadraticMaskingRangeCase(GenericNumberCases.rtnX)
    }

    @Test
    fun quadraticInStepRangeShouldRegisterConstraintsForFourNumberTypesOnQuadraticMechanismModel() {
        runQuadraticInStepRangeCase(GenericNumberCases.flt64)
        runQuadraticInStepRangeCase(GenericNumberCases.fltX)
        runQuadraticInStepRangeCase(GenericNumberCases.rtn64)
        runQuadraticInStepRangeCase(GenericNumberCases.rtnX)
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
            val mechanismResult = runBlocking {
                LinearMechanismModel.invoke<V>(metaModel = model, concurrent = false)
            }
            assertTrue(mechanismResult is Ok, "${numberCase.name}: dump mechanism model should succeed")
            val mechanismModel = mechanismResult.value

            assertLinearConstraintRegistration(
                numberCase = numberCase,
                mechanismModel = mechanismModel,
                label = "abs",
                register = { abs.registerConstraints(mechanismModel) },
                expectedInputTokenNames = setOf(x.name)
            )
            assertLinearConstraintRegistration(
                numberCase = numberCase,
                mechanismModel = mechanismModel,
                label = "and",
                register = { and.registerConstraints(mechanismModel) },
                expectedInputTokenNames = setOf(x.name, y.name)
            )
            assertLinearConstraintRegistration(
                numberCase = numberCase,
                mechanismModel = mechanismModel,
                label = "if",
                register = { ifFunc.registerConstraints(mechanismModel) },
                expectedInputTokenNames = setOf(x.name)
            )
            assertLinearConstraintRegistration(
                numberCase = numberCase,
                mechanismModel = mechanismModel,
                label = "or",
                register = { or.registerConstraints(mechanismModel) },
                expectedInputTokenNames = setOf(x.name, y.name)
            )
            assertLinearConstraintRegistration(
                numberCase = numberCase,
                mechanismModel = mechanismModel,
                label = "not",
                register = { not.registerConstraints(mechanismModel) },
                expectedInputTokenNames = setOf(x.name)
            )
            assertLinearConstraintRegistration(
                numberCase = numberCase,
                mechanismModel = mechanismModel,
                label = "xor",
                register = { xor.registerConstraints(mechanismModel) },
                expectedInputTokenNames = setOf(x.name, y.name)
            )
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
            val mechanismResult = runBlocking {
                QuadraticMechanismModel.invoke<V>(metaModel = model, concurrent = false)
            }
            assertTrue(mechanismResult is Ok, "${numberCase.name}: product dump quadratic mechanism model should succeed")
            val mechanismModel = mechanismResult.value

            assertQuadraticConstraintRegistration(
                numberCase = numberCase,
                mechanismModel = mechanismModel,
                label = "product",
                register = { product.registerConstraints(mechanismModel) },
                expectedInputTokenNames = setOf(x.name, y.name)
            )
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
            val mechanismResult = runBlocking {
                QuadraticMechanismModel.invoke<V>(metaModel = model, concurrent = false)
            }
            assertTrue(mechanismResult is Ok, "${numberCase.name}: qlinear dump quadratic mechanism model should succeed")
            val mechanismModel = mechanismResult.value

            assertQuadraticConstraintRegistration(
                numberCase = numberCase,
                mechanismModel = mechanismModel,
                label = "qlinear",
                register = { function.registerConstraints(mechanismModel) },
                expectedInputTokenNames = setOf(x.name, y.name)
            )
        } finally {
            model.close()
        }
    }

    private fun <V> runQuadraticMinCase(numberCase: GenericNumberCase<V>)
            where V : RealNumber<V>, V : NumberField<V> {
        val x = RealVar("${numberCase.name.lowercase()}_qmin_x")
        val y = RealVar("${numberCase.name.lowercase()}_qmin_y")

        val model = QuadraticMetaModel<V>(
            name = "generic-quadratic-min-${numberCase.name.lowercase()}",
            converter = numberCase.converter
        )

        try {
            assertTrue(model.add(listOf(x, y)) is Ok, "${numberCase.name}: qmin add variables should succeed")

            val objective = QuadraticPolynomial(
                monomials = listOf(QuadraticMonomial.quadratic(numberCase.one, x, y)),
                constant = numberCase.zero
            )
            assertTrue(model.minimize(objective) is Ok, "${numberCase.name}: qmin add objective should succeed")

            val p1 = QuadraticPolynomial(
                monomials = listOf(QuadraticMonomial.quadratic(numberCase.one, x, y)),
                constant = numberCase.zero
            )
            val p2 = QuadraticPolynomial(
                monomials = listOf(QuadraticMonomial.linear(numberCase.one, x)),
                constant = numberCase.one
            )
            val function = QuadraticMinFunction(
                polynomials = listOf(p1, p2),
                exact = true,
                bigM = numberCase.ten,
                converter = numberCase.converter,
                name = "qmin_${numberCase.name.lowercase()}"
            )
            assertTrue(function.registerAuxiliaryTokens(model.tokens) is Ok, "${numberCase.name}: qmin auxiliary tokens should succeed")
            val mechanismResult = runBlocking {
                QuadraticMechanismModel.invoke<V>(metaModel = model, concurrent = false)
            }
            assertTrue(mechanismResult is Ok, "${numberCase.name}: qmin dump quadratic mechanism model should succeed")
            val mechanismModel = mechanismResult.value

            assertQuadraticConstraintRegistration(
                numberCase = numberCase,
                mechanismModel = mechanismModel,
                label = "qmin",
                register = { function.registerConstraints(mechanismModel) },
                expectedInputTokenNames = setOf(x.name, y.name)
            )
        } finally {
            model.close()
        }
    }

    private fun <V> runQuadraticMaskingRangeCase(numberCase: GenericNumberCase<V>)
            where V : RealNumber<V>, V : NumberField<V> {
        val x = RealVar("${numberCase.name.lowercase()}_qmask_x")
        val y = RealVar("${numberCase.name.lowercase()}_qmask_y")
        val z = BinVar("${numberCase.name.lowercase()}_qmask_z")

        val model = QuadraticMetaModel<V>(
            name = "generic-quadratic-mask-${numberCase.name.lowercase()}",
            converter = numberCase.converter
        )

        try {
            assertTrue(model.add(listOf(x, y, z)) is Ok, "${numberCase.name}: qmask add variables should succeed")

            val objective = QuadraticPolynomial(
                monomials = listOf(QuadraticMonomial.quadratic(numberCase.one, x, y)),
                constant = numberCase.zero
            )
            assertTrue(model.minimize(objective) is Ok, "${numberCase.name}: qmask add objective should succeed")

            val polynomial = QuadraticPolynomial(
                monomials = listOf(
                    QuadraticMonomial.quadratic(numberCase.one, x, y),
                    QuadraticMonomial.linear(numberCase.one, x)
                ),
                constant = numberCase.zero
            )
            val function = QuadraticMaskingRangeFunction(
                polynomial = polynomial,
                z = z,
                bigM = numberCase.ten,
                converter = numberCase.converter,
                name = "qmask_${numberCase.name.lowercase()}"
            )
            assertTrue(function.registerAuxiliaryTokens(model.tokens) is Ok, "${numberCase.name}: qmask auxiliary tokens should succeed")
            val mechanismResult = runBlocking {
                QuadraticMechanismModel.invoke<V>(metaModel = model, concurrent = false)
            }
            assertTrue(mechanismResult is Ok, "${numberCase.name}: qmask dump quadratic mechanism model should succeed")
            val mechanismModel = mechanismResult.value

            assertQuadraticConstraintRegistration(
                numberCase = numberCase,
                mechanismModel = mechanismModel,
                label = "qmask",
                register = { function.registerConstraints(mechanismModel) },
                expectedInputTokenNames = setOf(x.name, y.name, z.name)
            )
        } finally {
            model.close()
        }
    }

    private fun <V> runQuadraticInStepRangeCase(numberCase: GenericNumberCase<V>)
            where V : RealNumber<V>, V : NumberField<V> {
        val x = RealVar("${numberCase.name.lowercase()}_qstep_x")
        val y = RealVar("${numberCase.name.lowercase()}_qstep_y")

        val model = QuadraticMetaModel<V>(
            name = "generic-quadratic-step-${numberCase.name.lowercase()}",
            converter = numberCase.converter
        )

        try {
            assertTrue(model.add(listOf(x, y)) is Ok, "${numberCase.name}: qstep add variables should succeed")

            val objective = QuadraticPolynomial(
                monomials = listOf(QuadraticMonomial.quadratic(numberCase.one, x, y)),
                constant = numberCase.zero
            )
            assertTrue(model.minimize(objective) is Ok, "${numberCase.name}: qstep add objective should succeed")

            val input = QuadraticPolynomial(
                monomials = listOf(
                    QuadraticMonomial.quadratic(numberCase.one, x, y),
                    QuadraticMonomial.linear(numberCase.one, x)
                ),
                constant = numberCase.zero
            )
            val function = QuadraticInStepRangeFunction(
                x = input,
                lower = -numberCase.one,
                upper = numberCase.two,
                bigM = numberCase.ten,
                converter = numberCase.converter,
                name = "qstep_${numberCase.name.lowercase()}"
            )
            assertTrue(function.registerAuxiliaryTokens(model.tokens) is Ok, "${numberCase.name}: qstep auxiliary tokens should succeed")
            val mechanismResult = runBlocking {
                QuadraticMechanismModel.invoke<V>(metaModel = model, concurrent = false)
            }
            assertTrue(mechanismResult is Ok, "${numberCase.name}: qstep dump quadratic mechanism model should succeed")
            val mechanismModel = mechanismResult.value

            assertQuadraticConstraintRegistration(
                numberCase = numberCase,
                mechanismModel = mechanismModel,
                label = "qstep",
                register = { function.registerConstraints(mechanismModel) },
                expectedInputTokenNames = setOf(x.name, y.name)
            )
        } finally {
            model.close()
        }
    }

    private fun <V> assertLinearConstraintRegistration(
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

        val appended = mechanismModel.constraints.subList(before, after)
        assertTrue(
            appended.all { it is LinearConstraintImpl<*> },
            "${numberCase.name}: $label should append linear constraints"
        )
        @Suppress("UNCHECKED_CAST")
        val appendedLinear = appended.map { it as LinearConstraintImpl<V> }
        val coefficients = appendedLinear.flatMap { constraint -> constraint.lhs.map { cell -> cell.coefficient } }
        assertTrue(coefficients.isNotEmpty(), "${numberCase.name}: $label appended constraints should contain coefficients")
        assertTrue(
            coefficients.all { it::class == numberCase.one::class },
            "${numberCase.name}: $label coefficients should stay V instead of leaking Flt64"
        )
        assertTrue(
            appendedLinear.all { it.rhs::class == numberCase.one::class },
            "${numberCase.name}: $label rhs type should stay V"
        )
        assertTrue(
            appendedLinear.any { constraint ->
                constraint.lhs.any { cell -> cell.token.variable.name in expectedInputTokenNames }
            },
            "${numberCase.name}: $label should reference input token(s) ${expectedInputTokenNames.joinToString()}"
        )
        assertTrue(
            appendedLinear.any { it.sign == ConstraintRelation.LessEqual || it.sign == ConstraintRelation.GreaterEqual || it.sign == ConstraintRelation.Equal },
            "${numberCase.name}: $label should produce comparable sign"
        )
    }

    private fun <V> assertQuadraticConstraintRegistration(
        numberCase: GenericNumberCase<V>,
        mechanismModel: QuadraticMechanismModel<V>,
        label: String,
        register: () -> Try,
        expectedInputTokenNames: Set<String>
    ) where V : RealNumber<V>, V : NumberField<V> {
        val before = mechanismModel.constraints.size
        assertTrue(register() is Ok, "${numberCase.name}: $label registerConstraints should succeed")
        val after = mechanismModel.constraints.size
        assertTrue(after > before, "${numberCase.name}: $label should append constraints")

        val appended = mechanismModel.constraints.subList(before, after)
        assertTrue(
            appended.all { it is QuadraticConstraintImpl<*> },
            "${numberCase.name}: $label should append quadratic constraints"
        )
        @Suppress("UNCHECKED_CAST")
        val appendedQuadratic = appended.map { it as QuadraticConstraintImpl<V> }
        val coefficients = appendedQuadratic.flatMap { constraint -> constraint.lhs.map { cell -> cell.coefficient } }
        assertTrue(coefficients.isNotEmpty(), "${numberCase.name}: $label appended constraints should contain coefficients")
        assertTrue(
            coefficients.all { it::class == numberCase.one::class },
            "${numberCase.name}: $label coefficients should stay V instead of leaking Flt64"
        )
        assertTrue(
            appendedQuadratic.all { it.rhs::class == numberCase.one::class },
            "${numberCase.name}: $label rhs type should stay V"
        )
        assertTrue(
            appendedQuadratic.any { constraint ->
                constraint.lhs.any { cell ->
                    cell.token1.variable.name in expectedInputTokenNames ||
                        cell.token2?.variable?.name in expectedInputTokenNames
                }
            },
            "${numberCase.name}: $label should reference input token(s) ${expectedInputTokenNames.joinToString()}"
        )
        assertTrue(
            appendedQuadratic.any { it.sign == ConstraintRelation.LessEqual || it.sign == ConstraintRelation.GreaterEqual || it.sign == ConstraintRelation.Equal },
            "${numberCase.name}: $label should produce comparable sign"
        )
    }
}
