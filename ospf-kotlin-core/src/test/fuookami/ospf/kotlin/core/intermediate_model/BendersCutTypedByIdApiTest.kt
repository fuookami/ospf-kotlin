package fuookami.ospf.kotlin.core.intermediate_model

import fuookami.ospf.kotlin.core.model.mechanism.Constraint
import fuookami.ospf.kotlin.core.model.mechanism.LinearRelationImpl
import fuookami.ospf.kotlin.core.model.mechanism.QuadraticRelationImpl
import fuookami.ospf.kotlin.core.model.mechanism.flattenData
import fuookami.ospf.kotlin.core.model.mechanism.toLinearFlattenData
import fuookami.ospf.kotlin.core.model.mechanism.toQuadraticFlattenData
import fuookami.ospf.kotlin.core.testing.GenericNumberCase
import fuookami.ospf.kotlin.core.testing.GenericNumberCases
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.math.symbol.adapter.flt64.normalize
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

class BendersCutTypedByIdApiTest {
    @Test
    fun linearTypedByIdShouldMatchLegacyByIdForFourNumberTypes() {
        runLinearCase(GenericNumberCases.flt64)
        runLinearCase(GenericNumberCases.fltX)
        runLinearCase(GenericNumberCases.rtn64)
        runLinearCase(GenericNumberCases.rtnX)
    }

    @Test
    fun quadraticTypedByIdShouldMatchLegacyByIdForFourNumberTypes() {
        runQuadraticCase(GenericNumberCases.flt64)
        runQuadraticCase(GenericNumberCases.fltX)
        runQuadraticCase(GenericNumberCases.rtn64)
        runQuadraticCase(GenericNumberCases.rtnX)
    }

    @Suppress("DEPRECATION")
    private fun <V> runLinearCase(numberCase: GenericNumberCase<V>)
            where V : RealNumber<V>, V : NumberField<V> {
        val x = RealVar("${numberCase.name.lowercase()}_lin_x")
        val theta = RealVar("${numberCase.name.lowercase()}_lin_theta")

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
                name = "typed-by-id-linear-${numberCase.name.lowercase()}",
                converter = numberCase.converter
            ),
            name = "typed-by-id-linear-model-${numberCase.name.lowercase()}",
            constraints = listOf(constraint),
            objectFunction = SingleObject(ObjectCategory.Minimum, emptyList<LinearSubObject<V>>()),
            tokens = tokens
        )

        try {
            val fixedVars: Map<AbstractVariableItem<*, *>, V> = mapOf(x to numberCase.two)
            val typedDualById = mapOf(constraint.name to numberCase.one)
            val legacyDualById = mapOf(constraint.name to Flt64.one)

            val typedOptimal = mechanismModel.generateOptimalCutByIdV(theta, fixedVars, typedDualById)
            val legacyOptimal = mechanismModel.generateOptimalCutById(theta, fixedVars, legacyDualById)
            assertEquals(legacyOptimal.size, typedOptimal.size, "${numberCase.name}: optimal cut size mismatch")
            for (i in legacyOptimal.indices) {
                assertLinearInequalityEquals(
                    legacyOptimal[i],
                    toFlt64LinearInequality(typedOptimal[i], numberCase),
                    "${numberCase.name}: optimal[$i]"
                )
            }

            val typedFeasible = mechanismModel.generateFeasibleCutByIdV(fixedVars, typedDualById)
            val legacyFeasible = mechanismModel.generateFeasibleCutById(fixedVars, legacyDualById)
            assertEquals(legacyFeasible.size, typedFeasible.size, "${numberCase.name}: feasible cut size mismatch")
            for (i in legacyFeasible.indices) {
                assertLinearInequalityEquals(
                    legacyFeasible[i],
                    toFlt64LinearInequality(typedFeasible[i], numberCase),
                    "${numberCase.name}: feasible[$i]"
                )
            }
        } finally {
            mechanismModel.close()
        }
    }

    @Suppress("DEPRECATION")
    private fun <V> runQuadraticCase(numberCase: GenericNumberCase<V>)
            where V : RealNumber<V>, V : NumberField<V> {
        val x = RealVar("${numberCase.name.lowercase()}_quad_x")
        val y = RealVar("${numberCase.name.lowercase()}_quad_y")
        val theta = RealVar("${numberCase.name.lowercase()}_quad_theta")

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
                name = "typed-by-id-quadratic-${numberCase.name.lowercase()}",
                converter = numberCase.converter
            ),
            name = "typed-by-id-quadratic-model-${numberCase.name.lowercase()}",
            constraints = listOf(constraint),
            objectFunction = SingleObject(ObjectCategory.Minimum, emptyList<QuadraticSubObject<V>>()),
            tokens = tokens
        )

        try {
            val fixedVars: Map<AbstractVariableItem<*, *>, V> = mapOf(
                x to numberCase.one,
                y to numberCase.two
            )
            val typedDualById = mapOf(constraint.name to numberCase.one)
            val legacyDualById = mapOf(constraint.name to Flt64.one)

            val typedOptimal = mechanismModel.generateOptimalCutByIdV(numberCase.zero, theta, fixedVars, typedDualById)
            val legacyOptimal = mechanismModel.generateOptimalCutById(Flt64.zero, theta, fixedVars, legacyDualById)
            assertTrue(legacyOptimal is Ok, "${numberCase.name}: legacy optimal cut should succeed")
            assertEquals(legacyOptimal.value.size, typedOptimal.size, "${numberCase.name}: optimal cut size mismatch")
            for (i in typedOptimal.indices) {
                assertCutEquals(
                    legacyOptimal.value[i],
                    toFlt64Cut(typedOptimal[i], numberCase),
                    "${numberCase.name}: optimal[$i]"
                )
            }

            val typedFeasible = mechanismModel.generateFeasibleCutByIdV(fixedVars, typedDualById)
            val legacyFeasible = mechanismModel.generateFeasibleCutById(fixedVars, legacyDualById)
            assertTrue(legacyFeasible is Ok, "${numberCase.name}: legacy feasible cut should succeed")
            assertEquals(legacyFeasible.value.size, typedFeasible.size, "${numberCase.name}: feasible cut size mismatch")
            for (i in typedFeasible.indices) {
                assertCutEquals(
                    legacyFeasible.value[i],
                    toFlt64Cut(typedFeasible[i], numberCase),
                    "${numberCase.name}: feasible[$i]"
                )
            }
        } finally {
            mechanismModel.close()
        }
    }

    private fun <V> toFlt64LinearInequality(
        inequality: LinearInequality<V>,
        numberCase: GenericNumberCase<V>
    ): LinearInequality<Flt64> where V : RealNumber<V>, V : NumberField<V> {
        return LinearInequality(
            lhs = LinearPolynomial(
                monomials = inequality.lhs.monomials.map { LinearMonomial(numberCase.converter.fromValue(it.coefficient), it.symbol) },
                constant = numberCase.converter.fromValue(inequality.lhs.constant)
            ),
            rhs = LinearPolynomial(
                monomials = inequality.rhs.monomials.map { LinearMonomial(numberCase.converter.fromValue(it.coefficient), it.symbol) },
                constant = numberCase.converter.fromValue(inequality.rhs.constant)
            ),
            comparison = inequality.comparison,
            name = inequality.name,
            displayName = inequality.displayName
        ).normalize()
    }

    private fun <V> toFlt64QuadraticInequality(
        inequality: QuadraticInequalityOf<V>,
        numberCase: GenericNumberCase<V>
    ): QuadraticInequalityOf<Flt64> where V : RealNumber<V>, V : NumberField<V> {
        return QuadraticInequalityOf(
            lhs = QuadraticPolynomial(
                monomials = inequality.lhs.monomials.map {
                    QuadraticMonomial(numberCase.converter.fromValue(it.coefficient), it.symbol1, it.symbol2)
                },
                constant = numberCase.converter.fromValue(inequality.lhs.constant)
            ),
            rhs = QuadraticPolynomial(
                monomials = inequality.rhs.monomials.map {
                    QuadraticMonomial(numberCase.converter.fromValue(it.coefficient), it.symbol1, it.symbol2)
                },
                constant = numberCase.converter.fromValue(inequality.rhs.constant)
            ),
            comparison = inequality.comparison,
            name = inequality.name,
            displayName = inequality.displayName
        ).normalize()
    }

    @Suppress("UNCHECKED_CAST")
    private fun <V> toFlt64Cut(
        cut: Any,
        numberCase: GenericNumberCase<V>
    ): Any where V : RealNumber<V>, V : NumberField<V> {
        return when (cut) {
            is LinearInequality<*> -> toFlt64LinearInequality(cut as LinearInequality<V>, numberCase)
            is QuadraticInequalityOf<*> -> toFlt64QuadraticInequality(cut as QuadraticInequalityOf<V>, numberCase)
            else -> cut
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
            assertTrue(am != null && am.coefficient eq em.coefficient, "$caseName: monomial mismatch for ${em.symbol}")
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
    private fun assertCutEquals(
        expected: Any,
        actual: Any,
        caseName: String
    ) {
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
