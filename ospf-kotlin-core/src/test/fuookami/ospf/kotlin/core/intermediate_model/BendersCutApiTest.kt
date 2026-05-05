package fuookami.ospf.kotlin.core.intermediate_model

import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.core.model.mechanism.LinearRelationImpl
import fuookami.ospf.kotlin.core.model.mechanism.QuadraticRelationImpl
import fuookami.ospf.kotlin.core.model.mechanism.flattenData
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.eq
import fuookami.ospf.kotlin.math.symbol.inequality.le
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import fuookami.ospf.kotlin.core.model.mechanism.Constraint
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality

private val flt64Converter = object : IntoValue<Flt64> {
        override fun intoValue(value: Flt64) = value
        override val zero get() = Flt64.zero
        override val one get() = Flt64.one
        override fun fromValue(value: Flt64) = value
    }

class BendersCutApiTest {

    // ── Linear by_id ──────────────────────────────────────────────

    @Test
    fun linearOptimalCutByIdShouldMatchDirectCall() {
        val x = RealVar("x")
        val theta = RealVar("theta")

        val tokens = AutoTokenTable<Flt64>(Linear, false)
        assertTrue(tokens.add(x) is Ok)

        val relation = LinearInequality<Flt64>(
            lhs = LinearPolynomial(listOf(LinearMonomial(Flt64(2.0), x)), Flt64.zero),
            rhs = LinearPolynomial(emptyList(), Flt64(6.0)),
            comparison = Comparison.LE
        )
        val constraint = LinearConstraintImpl(
            relation = LinearRelationImpl(relation.flattenData, relation.comparison),
            tokens = tokens,
            name = "lc-opt"
        )
        val mechanismModel = LinearMechanismModel<Flt64>(
            parent = LinearMetaModel<Flt64>(name = "cut-parent-lin-opt", converter = flt64Converter),
            name = "cut-model-lin-opt",
            constraints = listOf(constraint),
            objectFunction = SingleObject(ObjectCategory.Minimum, emptyList<LinearSubObject<Flt64>>()),
            tokens = tokens
        )

        val fixedVars: Map<AbstractVariableItem<*, *>, Flt64> = mapOf(x to Flt64(3.0))
        val dualSolution: kotlin.collections.Map<Constraint<Flt64, MechanismLinear>, Flt64> = mapOf(constraint as Constraint<Flt64, MechanismLinear> to Flt64.one)
        val dualById = mapOf(constraint.name to Flt64.one)

        val direct = mechanismModel.generateOptimalCut(theta, fixedVars, dualSolution)
        val byId = mechanismModel.generateOptimalCutById(theta, fixedVars, dualById)

        assertEquals(direct.size, byId.size)
        for (i in direct.indices) {
            assertLinearInequalityEquals(direct[i], byId[i])
        }

        mechanismModel.close()
    }

    @Test
    fun linearFeasibleCutByIdShouldMatchDirectCall() {
        val x = RealVar("x")

        val tokens = AutoTokenTable<Flt64>(Linear, false)
        assertTrue(tokens.add(x) is Ok)

        val relation = LinearInequality<Flt64>(
            lhs = LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.zero),
            rhs = LinearPolynomial(emptyList(), Flt64(5.0)),
            comparison = Comparison.LE
        )
        val constraint = LinearConstraintImpl(
            relation = LinearRelationImpl(relation.flattenData, relation.comparison),
            tokens = tokens,
            name = "lc-feas"
        )
        val mechanismModel = LinearMechanismModel<Flt64>(
            parent = LinearMetaModel<Flt64>(name = "cut-parent-lin-feas", converter = flt64Converter),
            name = "cut-model-lin-feas",
            constraints = listOf(constraint),
            objectFunction = SingleObject(ObjectCategory.Minimum, emptyList<LinearSubObject<Flt64>>()),
            tokens = tokens
        )

        val fixedVars: Map<AbstractVariableItem<*, *>, Flt64> = mapOf(x to Flt64(2.0))
        val farkasDual: kotlin.collections.Map<Constraint<Flt64, MechanismLinear>, Flt64> = mapOf(constraint as Constraint<Flt64, MechanismLinear> to Flt64.one)
        val farkasDualById = mapOf(constraint.name to Flt64.one)

        val direct = mechanismModel.generateFeasibleCut(fixedVars, farkasDual)
        val byId = mechanismModel.generateFeasibleCutById(fixedVars, farkasDualById)

        assertEquals(direct.size, byId.size)
        for (i in direct.indices) {
            assertLinearInequalityEquals(direct[i], byId[i])
        }

        mechanismModel.close()
    }

    // ── Quadratic by_id ───────────────────────────────────────────

    @Test
    fun quadraticOptimalCutByIdShouldMatchDirectCall() {
        val x = RealVar("x")
        val y = RealVar("y")
        val theta = RealVar("theta")

        val tokens = AutoTokenTable<Flt64>(Quadratic, false)
        assertTrue(tokens.add(listOf(x, y)) is Ok)

        val relation = QuadraticInequality(
            lhs = QuadraticPolynomial(
                monomials = listOf(
                    QuadraticMonomial.quadratic(Flt64.one, x, y),
                    QuadraticMonomial.linear(Flt64.one, x)
                ),
                constant = Flt64.zero
            ),
            rhs = QuadraticPolynomial(emptyList(), Flt64(6.0)),
            comparison = Comparison.LE
        )
        val constraint = QuadraticConstraintImpl(
            relation = QuadraticRelationImpl(relation.flattenData, relation.comparison),
            tokens = tokens,
            name = "qc-opt"
        )
        val mechanismModel = QuadraticMechanismModel<Flt64>(
            parent = QuadraticMetaModel<Flt64>(name = "cut-parent-qc-opt", converter = flt64Converter),
            name = "cut-model-qc-opt",
            constraints = listOf(constraint),
            objectFunction = SingleObject(ObjectCategory.Minimum, emptyList<QuadraticSubObject<Flt64>>()),
            tokens = tokens
        )

        val fixedVars: Map<AbstractVariableItem<*, *>, Flt64> = mapOf(x to Flt64.one, y to Flt64(2.0))
        val dualSolution: kotlin.collections.Map<Constraint<Flt64, MechanismQuadratic>, Flt64> = mapOf(constraint as Constraint<Flt64, MechanismQuadratic> to Flt64(2.0))
        val dualById = mapOf(constraint.name to Flt64(2.0))

        val direct = mechanismModel.generateOptimalCut(Flt64.zero, theta, fixedVars, dualSolution)
        val byId = mechanismModel.generateOptimalCutById(Flt64.zero, theta, fixedVars, dualById)

        assertTrue(direct is Ok)
        assertTrue(byId is Ok)
        assertEquals(direct.value.size, byId.value.size)
        for (i in direct.value.indices) {
            assertCutEquals(direct.value[i], byId.value[i])
        }

        mechanismModel.close()
    }

    @Test
    fun quadraticFeasibleCutByIdShouldMatchDirectCall() {
        val x = RealVar("x")
        val y = RealVar("y")

        val tokens = AutoTokenTable<Flt64>(Quadratic, false)
        assertTrue(tokens.add(listOf(x, y)) is Ok)

        val relation = QuadraticInequality(
            lhs = QuadraticPolynomial(
                monomials = listOf(QuadraticMonomial.quadratic(Flt64.one, x, y)),
                constant = Flt64.zero
            ),
            rhs = QuadraticPolynomial(emptyList(), Flt64.one),
            comparison = Comparison.LE
        )
        val constraint = QuadraticConstraintImpl(
            relation = QuadraticRelationImpl(relation.flattenData, relation.comparison),
            tokens = tokens,
            name = "qc-feas"
        )
        val mechanismModel = QuadraticMechanismModel<Flt64>(
            parent = QuadraticMetaModel<Flt64>(name = "cut-parent-qc-feas", converter = flt64Converter),
            name = "cut-model-qc-feas",
            constraints = listOf(constraint),
            objectFunction = SingleObject(ObjectCategory.Minimum, emptyList<QuadraticSubObject<Flt64>>()),
            tokens = tokens
        )

        val fixedVars: Map<AbstractVariableItem<*, *>, Flt64> = mapOf(x to Flt64(2.0), y to Flt64(3.0))
        val farkasDual: kotlin.collections.Map<Constraint<Flt64, MechanismQuadratic>, Flt64> = mapOf(constraint as Constraint<Flt64, MechanismQuadratic> to Flt64.one)
        val farkasDualById = mapOf(constraint.name to Flt64.one)

        val direct = mechanismModel.generateFeasibleCut(fixedVars, farkasDual)
        val byId = mechanismModel.generateFeasibleCutById(fixedVars, farkasDualById)

        assertTrue(direct is Ok)
        assertTrue(byId is Ok)
        assertEquals(direct.value.size, byId.value.size)
        for (i in direct.value.indices) {
            assertCutEquals(direct.value[i], byId.value[i])
        }

        mechanismModel.close()
    }

    // ── Linear from_output ────────────────────────────────────────

    @Test
    fun linearOptimalCutFromOutputShouldMatchDirectCall() {
        val x = RealVar("x")
        val theta = RealVar("theta")

        val tokens = AutoTokenTable<Flt64>(Linear, false)
        assertTrue(tokens.add(x) is Ok)

        val relation = LinearInequality<Flt64>(
            lhs = LinearPolynomial(listOf(LinearMonomial(Flt64(2.0), x)), Flt64.zero),
            rhs = LinearPolynomial(emptyList(), Flt64(6.0)),
            comparison = Comparison.LE
        )
        val constraint = LinearConstraintImpl(
            relation = LinearRelationImpl(relation.flattenData, relation.comparison),
            tokens = tokens,
            name = "lc-opt-out"
        )
        val mechanismModel = LinearMechanismModel<Flt64>(
            parent = LinearMetaModel<Flt64>(name = "cut-parent-lin-opt-out", converter = flt64Converter),
            name = "cut-model-lin-opt-out",
            constraints = listOf(constraint),
            objectFunction = SingleObject(ObjectCategory.Minimum, emptyList<LinearSubObject<Flt64>>()),
            tokens = tokens
        )

        val fixedVars: Map<AbstractVariableItem<*, *>, Flt64> = mapOf(x to Flt64(3.0))
        val dualSolution: kotlin.collections.Map<Constraint<Flt64, MechanismLinear>, Flt64> = mapOf(constraint as Constraint<Flt64, MechanismLinear> to Flt64.one)
        val dualValues: List<Flt64> = listOf(Flt64.one)

        val triadModel = RecordingLinearTriadModelView(dualSolution)

        val direct = mechanismModel.generateOptimalCut(theta, fixedVars, dualSolution)
        val fromOutput = mechanismModel.generateOptimalCutFromOutput(theta, fixedVars, dualValues, triadModel)

        // Verify the stub received the exact dualValues we passed
        assertEquals(dualValues, triadModel.lastSolution, "from_output must pass dualValues to tidyDualSolution")

        assertEquals(direct.size, fromOutput.size)
        for (i in direct.indices) {
            assertLinearInequalityEquals(direct[i], fromOutput[i])
        }

        mechanismModel.close()
    }

    @Test
    fun linearFeasibleCutFromOutputShouldMatchDirectCall() {
        val x = RealVar("x")

        val tokens = AutoTokenTable<Flt64>(Linear, false)
        assertTrue(tokens.add(x) is Ok)

        val relation = LinearInequality<Flt64>(
            lhs = LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.zero),
            rhs = LinearPolynomial(emptyList(), Flt64(5.0)),
            comparison = Comparison.LE
        )
        val constraint = LinearConstraintImpl(
            relation = LinearRelationImpl(relation.flattenData, relation.comparison),
            tokens = tokens,
            name = "lc-feas-out"
        )
        val mechanismModel = LinearMechanismModel<Flt64>(
            parent = LinearMetaModel<Flt64>(name = "cut-parent-lin-feas-out", converter = flt64Converter),
            name = "cut-model-lin-feas-out",
            constraints = listOf(constraint),
            objectFunction = SingleObject(ObjectCategory.Minimum, emptyList<LinearSubObject<Flt64>>()),
            tokens = tokens
        )

        val fixedVars: Map<AbstractVariableItem<*, *>, Flt64> = mapOf(x to Flt64(2.0))
        val farkasDual: kotlin.collections.Map<Constraint<Flt64, MechanismLinear>, Flt64> = mapOf(constraint as Constraint<Flt64, MechanismLinear> to Flt64.one)
        val farkasDualValues: List<Flt64> = listOf(Flt64.one)

        val triadModel = RecordingLinearTriadModelView(farkasDual)

        val direct = mechanismModel.generateFeasibleCut(fixedVars, farkasDual)
        val fromOutput = mechanismModel.generateFeasibleCutFromOutput(fixedVars, farkasDualValues, triadModel)

        // Verify the stub received the exact farkasDualValues we passed
        assertEquals(farkasDualValues, triadModel.lastSolution, "from_output must pass farkasDualValues to tidyDualSolution")

        assertEquals(direct.size, fromOutput.size)
        for (i in direct.indices) {
            assertLinearInequalityEquals(direct[i], fromOutput[i])
        }

        mechanismModel.close()
    }

    // ── Quadratic from_output ─────────────────────────────────────

    @Test
    fun quadraticOptimalCutFromOutputShouldMatchDirectCall() {
        val x = RealVar("x")
        val y = RealVar("y")
        val theta = RealVar("theta")

        val tokens = AutoTokenTable<Flt64>(Quadratic, false)
        assertTrue(tokens.add(listOf(x, y)) is Ok)

        val relation = QuadraticInequality(
            lhs = QuadraticPolynomial(
                monomials = listOf(
                    QuadraticMonomial.quadratic(Flt64.one, x, y),
                    QuadraticMonomial.linear(Flt64.one, x)
                ),
                constant = Flt64.zero
            ),
            rhs = QuadraticPolynomial(emptyList(), Flt64(6.0)),
            comparison = Comparison.LE
        )
        val constraint = QuadraticConstraintImpl(
            relation = QuadraticRelationImpl(relation.flattenData, relation.comparison),
            tokens = tokens,
            name = "qc-opt-out"
        )
        val mechanismModel = QuadraticMechanismModel<Flt64>(
            parent = QuadraticMetaModel<Flt64>(name = "cut-parent-qc-opt-out", converter = flt64Converter),
            name = "cut-model-qc-opt-out",
            constraints = listOf(constraint),
            objectFunction = SingleObject(ObjectCategory.Minimum, emptyList<QuadraticSubObject<Flt64>>()),
            tokens = tokens
        )

        val fixedVars: Map<AbstractVariableItem<*, *>, Flt64> = mapOf(x to Flt64.one, y to Flt64(2.0))
        val dualSolution: kotlin.collections.Map<Constraint<Flt64, MechanismQuadratic>, Flt64> = mapOf(constraint as Constraint<Flt64, MechanismQuadratic> to Flt64(2.0))
        val dualValues: List<Flt64> = listOf(Flt64(2.0))

        val tetradModel = RecordingQuadraticTetradModelView(dualSolution)

        val direct = mechanismModel.generateOptimalCut(Flt64.zero, theta, fixedVars, dualSolution)
        val fromOutput = mechanismModel.generateOptimalCutFromOutput(Flt64.zero, theta, fixedVars, dualValues, tetradModel)

        // Verify the stub received the exact dualValues we passed
        assertEquals(dualValues, tetradModel.lastSolution, "from_output must pass dualValues to tidyDualSolution")

        assertTrue(direct is Ok)
        assertTrue(fromOutput is Ok)
        assertEquals(direct.value.size, fromOutput.value.size)
        for (i in direct.value.indices) {
            assertCutEquals(direct.value[i], fromOutput.value[i])
        }

        mechanismModel.close()
    }

    @Test
    fun quadraticFeasibleCutFromOutputShouldMatchDirectCall() {
        val x = RealVar("x")
        val y = RealVar("y")

        val tokens = AutoTokenTable<Flt64>(Quadratic, false)
        assertTrue(tokens.add(listOf(x, y)) is Ok)

        val relation = QuadraticInequality(
            lhs = QuadraticPolynomial(
                monomials = listOf(QuadraticMonomial.quadratic(Flt64.one, x, y)),
                constant = Flt64.zero
            ),
            rhs = QuadraticPolynomial(emptyList(), Flt64.one),
            comparison = Comparison.LE
        )
        val constraint = QuadraticConstraintImpl(
            relation = QuadraticRelationImpl(relation.flattenData, relation.comparison),
            tokens = tokens,
            name = "qc-feas-out"
        )
        val mechanismModel = QuadraticMechanismModel<Flt64>(
            parent = QuadraticMetaModel<Flt64>(name = "cut-parent-qc-feas-out", converter = flt64Converter),
            name = "cut-model-qc-feas-out",
            constraints = listOf(constraint),
            objectFunction = SingleObject(ObjectCategory.Minimum, emptyList<QuadraticSubObject<Flt64>>()),
            tokens = tokens
        )

        val fixedVars: Map<AbstractVariableItem<*, *>, Flt64> = mapOf(x to Flt64(2.0), y to Flt64(3.0))
        val farkasDual: kotlin.collections.Map<Constraint<Flt64, MechanismQuadratic>, Flt64> = mapOf(constraint as Constraint<Flt64, MechanismQuadratic> to Flt64.one)
        val farkasDualValues: List<Flt64> = listOf(Flt64.one)

        val tetradModel = RecordingQuadraticTetradModelView(farkasDual)

        val direct = mechanismModel.generateFeasibleCut(fixedVars, farkasDual)
        val fromOutput = mechanismModel.generateFeasibleCutFromOutput(fixedVars, farkasDualValues, tetradModel)

        // Verify the stub received the exact farkasDualValues we passed
        assertEquals(farkasDualValues, tetradModel.lastSolution, "from_output must pass farkasDualValues to tidyDualSolution")

        assertTrue(direct is Ok)
        assertTrue(fromOutput is Ok)
        assertEquals(direct.value.size, fromOutput.value.size)
        for (i in direct.value.indices) {
            assertCutEquals(direct.value[i], fromOutput.value[i])
        }

        mechanismModel.close()
    }

    // ── Helpers ───────────────────────────────────────────────────

    private fun assertLinearInequalityEquals(
        expected: LinearInequality<Flt64>,
        actual: LinearInequality<Flt64>
    ) {
        val eFlat = expected.flattenData
        val aFlat = actual.flattenData
        assertEquals(expected.comparison, actual.comparison, "comparison mismatch")
        assertTrue(eFlat.constant eq aFlat.constant,
            "constant mismatch: expected ${eFlat.constant}, actual ${aFlat.constant}")
        assertEquals(eFlat.monomials.size, aFlat.monomials.size, "monomial count mismatch")
        for (em in eFlat.monomials) {
            val am = aFlat.monomials.firstOrNull { it.symbol == em.symbol }
            assertTrue(am != null && am.coefficient eq em.coefficient,
                "monomial mismatch for ${em.symbol}: expected ${em.coefficient}, actual ${am?.coefficient}")
        }
    }

    private fun assertQuadraticInequalityEquals(
        expected: QuadraticInequality,
        actual: QuadraticInequality
    ) {
        val eFlat = expected.flattenData
        val aFlat = actual.flattenData
        assertEquals(expected.comparison, actual.comparison, "comparison mismatch")
        assertTrue(eFlat.constant eq aFlat.constant,
            "constant mismatch: expected ${eFlat.constant}, actual ${aFlat.constant}")
        assertEquals(eFlat.monomials.size, aFlat.monomials.size, "monomial count mismatch")
        for (em in eFlat.monomials) {
            val am = aFlat.monomials.firstOrNull {
                (it.symbol1 == em.symbol1 && it.symbol2 == em.symbol2) ||
                    (it.symbol1 == em.symbol2 && it.symbol2 == em.symbol1)
            }
            assertTrue(am != null && am.coefficient eq em.coefficient,
                "monomial mismatch for (${em.symbol1}, ${em.symbol2}): expected ${em.coefficient}, actual ${am?.coefficient}")
        }
    }

    /** Dispatch to the correct assertion based on cut type (linear or quadratic). */
    private fun assertCutEquals(expected: Any, actual: Any) {
        val expectedClassName = expected::class.qualifiedName
        val actualClassName = actual::class.qualifiedName
        assertEquals(expectedClassName, actualClassName, "cut type mismatch")
        when (expectedClassName) {
            LinearInequality::class.qualifiedName ->
                assertLinearInequalityEquals(expected as LinearInequality<Flt64>, actual as LinearInequality<Flt64>)
            QuadraticInequality::class.qualifiedName ->
                assertQuadraticInequalityEquals(expected as QuadraticInequality, actual as QuadraticInequality)
            else -> assertTrue(false, "unexpected cut type: $expectedClassName")
        }
    }
}

/** Stub [LinearTriadModelView] that records the [Solution] passed to [tidyDualSolution] and returns a fixed result. */
private class RecordingLinearTriadModelView(
    private val fixedDualSolution: kotlin.collections.Map<Constraint<Flt64, MechanismLinear>, Flt64>
) : LinearTriadModelView {
    var lastSolution: List<Flt64> = emptyList()
        private set

    override fun tidyDualSolution(solution: List<Flt64>): kotlin.collections.Map<Constraint<Flt64, MechanismLinear>, Flt64> {
        lastSolution = solution
        return fixedDualSolution
    }
    override val name: String = "stub-triad"
    override val dual: Boolean = false
    override val constraints: LinearConstraintBatch get() = throw UnsupportedOperationException()
    override val variables: List<Variable> get() = throw UnsupportedOperationException()
    override val objective: LinearObjective get() = throw UnsupportedOperationException()
    override fun linearRelax(): LinearTriadModelView = throw UnsupportedOperationException()
    override fun linearRelaxed(): LinearTriadModelView = throw UnsupportedOperationException()
    override suspend fun farkasDual(): LinearTriadModelView = throw UnsupportedOperationException()
    override fun feasibility(): LinearTriadModelView = throw UnsupportedOperationException()
    override fun elastic(minmaxSlack: Boolean, minSlackAmount: Pair<UInt64, Flt64>?): LinearTriadModelView = throw UnsupportedOperationException()
    override fun exportLP(writer: java.io.OutputStreamWriter): Try = throw UnsupportedOperationException()
    override fun close() {}
}

/** Stub [QuadraticTetradModelView] that records the [Solution] passed to [tidyDualSolution] and returns a fixed result. */
private class RecordingQuadraticTetradModelView(
    private val fixedDualSolution: kotlin.collections.Map<Constraint<Flt64, MechanismQuadratic>, Flt64>
) : QuadraticTetradModelView {
    var lastSolution: List<Flt64> = emptyList()
        private set

    override fun tidyDualSolution(solution: List<Flt64>): kotlin.collections.Map<Constraint<Flt64, MechanismQuadratic>, Flt64> {
        lastSolution = solution
        return fixedDualSolution
    }
    override suspend fun dual(): QuadraticTetradModel = throw UnsupportedOperationException()
    override suspend fun farkasDual(): QuadraticTetradModel = throw UnsupportedOperationException()
    override val name: String = "stub-tetrad"
    override val dual: Boolean = false
    override val constraints: QuadraticConstraintBatch get() = throw UnsupportedOperationException()
    override val variables: List<Variable> get() = throw UnsupportedOperationException()
    override val objective: QuadraticObjective get() = throw UnsupportedOperationException()
    override fun linearRelax(): QuadraticTetradModelView = throw UnsupportedOperationException()
    override fun linearRelaxed(): QuadraticTetradModelView = throw UnsupportedOperationException()
    override fun feasibility(): QuadraticTetradModelView = throw UnsupportedOperationException()
    override fun elastic(): QuadraticTetradModelView = throw UnsupportedOperationException()
    override fun exportLP(writer: java.io.OutputStreamWriter): Try = throw UnsupportedOperationException()
    override fun close() {}
}
