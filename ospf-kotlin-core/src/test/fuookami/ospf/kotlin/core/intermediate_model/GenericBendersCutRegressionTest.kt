package fuookami.ospf.kotlin.core.intermediate_model

import fuookami.ospf.kotlin.core.testing.GenericNumberCase
import fuookami.ospf.kotlin.core.testing.GenericNumberCases
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.core.model.mechanism.LinearRelationImpl
import fuookami.ospf.kotlin.core.model.mechanism.QuadraticRelationImpl
import fuookami.ospf.kotlin.core.model.mechanism.flattenData
import fuookami.ospf.kotlin.core.model.mechanism.toLinearFlattenData
import fuookami.ospf.kotlin.core.model.mechanism.toQuadraticFlattenData
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequalityOf
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.utils.functional.Ok
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GenericBendersCutRegressionTest {
    @Test
    fun linearCutByIdShouldMatchDirectCallForFourNumberTypes() {
        runLinearCase(GenericNumberCases.flt64)
        runLinearCase(GenericNumberCases.fltX)
        runLinearCase(GenericNumberCases.rtn64)
        runLinearCase(GenericNumberCases.rtnX)
    }

    @Test
    fun quadraticCutByIdShouldMatchDirectCallForFourNumberTypes() {
        runQuadraticCase(GenericNumberCases.flt64)
        runQuadraticCase(GenericNumberCases.fltX)
        runQuadraticCase(GenericNumberCases.rtn64)
        runQuadraticCase(GenericNumberCases.rtnX)
    }

    @Suppress("DEPRECATION")
    private fun <V> runLinearCase(numberCase: GenericNumberCase<V>)
            where V : RealNumber<V>, V : NumberField<V> {
        val x = RealVar("${numberCase.name.lowercase()}_benders_lin_x")
        val theta = RealVar("${numberCase.name.lowercase()}_benders_lin_theta")

        val tokens = AutoTokenTable<V>(Linear, false)
        assertTrue(tokens.add(x) is Ok, "${numberCase.name}: add variable should succeed")

        val relation = LinearInequality(
            lhs = LinearPolynomial(
                monomials = listOf(LinearMonomial(numberCase.two, x)),
                constant = numberCase.zero
            ),
            rhs = LinearPolynomial(emptyList(), numberCase.five),
            comparison = Comparison.LE
        )
        val constraint = LinearConstraintImpl(
            relation = LinearRelationImpl(relation.toLinearFlattenData().getOrThrow(), relation.comparison),
            tokens = tokens,
            converter = numberCase.converter,
            name = "lin_cut_${numberCase.name.lowercase()}"
        )
        val mechanismModel = LinearMechanismModel(
            parent = LinearMetaModel<V>(
                name = "generic-cut-linear-${numberCase.name.lowercase()}",
                converter = numberCase.converter
            ),
            name = "generic-cut-linear-model-${numberCase.name.lowercase()}",
            constraints = listOf(constraint),
            objectFunction = SingleObject(ObjectCategory.Minimum, emptyList<LinearSubObject<V>>()),
            tokens = tokens
        )

        try {
            val fixedVars: Map<AbstractVariableItem<*, *>, V> = mapOf(x to numberCase.two)
            @Suppress("UNCHECKED_CAST")
            val dualSolution: kotlin.collections.Map<Constraint<Flt64, MechanismLinear>, Flt64> =
                mapOf(constraint as Constraint<Flt64, MechanismLinear> to Flt64.one)
            val dualById = mapOf(constraint.name to Flt64.one)

            val direct = mechanismModel.generateOptimalCut(theta, fixedVars, dualSolution)
            val byId = mechanismModel.generateOptimalCutById(theta, fixedVars, dualById)
            assertEquals(1, direct.size, "${numberCase.name}: direct cut size should be one")
            assertEquals(direct.size, byId.size, "${numberCase.name}: by-id cut size mismatch")
            assertLinearInequalityEquals(direct.first(), byId.first(), numberCase.name)
            assertTrue(
                direct.first().flattenData.getOrThrow().monomials.any { it.symbol == theta },
                "${numberCase.name}: optimal cut should contain theta"
            )

            @Suppress("UNCHECKED_CAST")
            val farkasDual: kotlin.collections.Map<Constraint<Flt64, MechanismLinear>, Flt64> =
                mapOf(constraint as Constraint<Flt64, MechanismLinear> to Flt64.one)
            val farkasById = mapOf(constraint.name to Flt64.one)

            val directFeasible = mechanismModel.generateFeasibleCut(fixedVars, farkasDual)
            val byIdFeasible = mechanismModel.generateFeasibleCutById(fixedVars, farkasById)
            assertEquals(1, directFeasible.size, "${numberCase.name}: direct feasible cut size should be one")
            assertEquals(directFeasible.size, byIdFeasible.size, "${numberCase.name}: by-id feasible cut size mismatch")
            assertLinearInequalityEquals(directFeasible.first(), byIdFeasible.first(), numberCase.name)
        } finally {
            mechanismModel.close()
        }
    }

    @Suppress("DEPRECATION")
    private fun <V> runQuadraticCase(numberCase: GenericNumberCase<V>)
            where V : RealNumber<V>, V : NumberField<V> {
        val x = RealVar("${numberCase.name.lowercase()}_benders_quad_x")
        val y = RealVar("${numberCase.name.lowercase()}_benders_quad_y")
        val theta = RealVar("${numberCase.name.lowercase()}_benders_quad_theta")

        val tokens = AutoTokenTable<V>(Quadratic, false)
        assertTrue(tokens.add(listOf(x, y)) is Ok, "${numberCase.name}: add variables should succeed")

        val relation = QuadraticInequalityOf(
            lhs = QuadraticPolynomial(
                monomials = listOf(
                    QuadraticMonomial.quadratic(numberCase.one, x, y),
                    QuadraticMonomial.linear(numberCase.one, x)
                ),
                constant = numberCase.zero
            ),
            rhs = QuadraticPolynomial(emptyList(), numberCase.five),
            comparison = Comparison.LE
        )
        val constraint = QuadraticConstraintImpl(
            relation = QuadraticRelationImpl(relation.toQuadraticFlattenData(), relation.comparison),
            tokens = tokens,
            converter = numberCase.converter,
            name = "quad_cut_${numberCase.name.lowercase()}"
        )
        val mechanismModel = QuadraticMechanismModel(
            parent = QuadraticMetaModel<V>(
                name = "generic-cut-quadratic-${numberCase.name.lowercase()}",
                converter = numberCase.converter
            ),
            name = "generic-cut-quadratic-model-${numberCase.name.lowercase()}",
            constraints = listOf(constraint),
            objectFunction = SingleObject(ObjectCategory.Minimum, emptyList<QuadraticSubObject<V>>()),
            tokens = tokens
        )

        try {
            val fixedVars: Map<AbstractVariableItem<*, *>, V> = mapOf(
                x to numberCase.one,
                y to numberCase.two
            )
            @Suppress("UNCHECKED_CAST")
            val dualSolution: kotlin.collections.Map<Constraint<Flt64, MechanismQuadratic>, Flt64> =
                mapOf(constraint as Constraint<Flt64, MechanismQuadratic> to Flt64.one)
            val dualById = mapOf(constraint.name to Flt64.one)

            val direct = mechanismModel.generateOptimalCut(Flt64.zero, theta, fixedVars, dualSolution)
            val byId = mechanismModel.generateOptimalCutById(Flt64.zero, theta, fixedVars, dualById)
            assertTrue(direct is Ok, "${numberCase.name}: direct quadratic optimal cut should succeed")
            assertTrue(byId is Ok, "${numberCase.name}: by-id quadratic optimal cut should succeed")
            assertEquals(direct.value.size, byId.value.size, "${numberCase.name}: by-id quadratic optimal cut size mismatch")
            for (i in direct.value.indices) {
                assertCutEquals(direct.value[i], byId.value[i], numberCase.name)
            }

            @Suppress("UNCHECKED_CAST")
            val farkasDual: kotlin.collections.Map<Constraint<Flt64, MechanismQuadratic>, Flt64> =
                mapOf(constraint as Constraint<Flt64, MechanismQuadratic> to Flt64.one)
            val farkasById = mapOf(constraint.name to Flt64.one)

            val directFeasible = mechanismModel.generateFeasibleCut(fixedVars, farkasDual)
            val byIdFeasible = mechanismModel.generateFeasibleCutById(fixedVars, farkasById)
            assertTrue(directFeasible is Ok, "${numberCase.name}: direct quadratic feasible cut should succeed")
            assertTrue(byIdFeasible is Ok, "${numberCase.name}: by-id quadratic feasible cut should succeed")
            assertEquals(directFeasible.value.size, byIdFeasible.value.size, "${numberCase.name}: by-id quadratic feasible cut size mismatch")
            for (i in directFeasible.value.indices) {
                assertCutEquals(directFeasible.value[i], byIdFeasible.value[i], numberCase.name)
            }
        } finally {
            mechanismModel.close()
        }
    }

    private fun assertLinearInequalityEquals(
        expected: LinearInequality<Flt64>,
        actual: LinearInequality<Flt64>,
        caseName: String
    ) {
        val eFlat = expected.flattenData.getOrThrow()
        val aFlat = actual.flattenData.getOrThrow()
        assertEquals(expected.comparison, actual.comparison, "$caseName: comparison mismatch")
        assertTrue(eFlat.constant eq aFlat.constant, "$caseName: constant mismatch")
        assertEquals(eFlat.monomials.size, aFlat.monomials.size, "$caseName: monomial count mismatch")
        for (em in eFlat.monomials) {
            val am = aFlat.monomials.firstOrNull { it.symbol == em.symbol }
            assertTrue(am != null && am.coefficient eq em.coefficient,
                "$caseName: monomial mismatch for ${em.symbol}")
        }
    }

    private fun assertQuadraticInequalityEquals(
        expected: QuadraticInequalityOf<Flt64>,
        actual: QuadraticInequalityOf<Flt64>,
        caseName: String
    ) {
        val eFlat = expected.flattenData
        val aFlat = actual.flattenData
        assertEquals(expected.comparison, actual.comparison, "$caseName: comparison mismatch")
        assertTrue(eFlat.constant eq aFlat.constant, "$caseName: constant mismatch")
        assertEquals(eFlat.monomials.size, aFlat.monomials.size, "$caseName: monomial count mismatch")
        for (em in eFlat.monomials) {
            val am = aFlat.monomials.firstOrNull {
                (it.symbol1 == em.symbol1 && it.symbol2 == em.symbol2) ||
                    (it.symbol1 == em.symbol2 && it.symbol2 == em.symbol1)
            }
            assertTrue(am != null && am.coefficient eq em.coefficient,
                "$caseName: quadratic monomial mismatch for (${em.symbol1}, ${em.symbol2})")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun assertCutEquals(expected: Any, actual: Any, caseName: String) {
        val expectedClassName = expected::class.qualifiedName
        val actualClassName = actual::class.qualifiedName
        assertEquals(expectedClassName, actualClassName, "$caseName: cut type mismatch")
        when (expectedClassName) {
            LinearInequality::class.qualifiedName ->
                assertLinearInequalityEquals(
                    expected as LinearInequality<Flt64>,
                    actual as LinearInequality<Flt64>,
                    caseName
                )
            QuadraticInequalityOf::class.qualifiedName ->
                assertQuadraticInequalityEquals(
                    expected as QuadraticInequalityOf<Flt64>,
                    actual as QuadraticInequalityOf<Flt64>,
                    caseName
                )
            else -> assertTrue(false, "$caseName: unexpected cut type: $expectedClassName")
        }
    }
}
