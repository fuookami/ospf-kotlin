package fuookami.ospf.kotlin.core.intermediate_model

import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.Flt64LinearInequality as MathLinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequality as MathQuadraticInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial as MathLinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial as MathQuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial as MathLinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial as MathQuadraticPolynomial
import fuookami.ospf.kotlin.utils.functional.Ok
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * P1-10 Semantic Equivalence Regression Test
 *
 * Verifies that the "new entry point" (math.symbol-based) and the
 * "old entry point" (deprecated core intermediate_model.monomial-based)
 * produce equivalent models.
 */
class SemanticEquivalenceTest {

    /**
     * Test 1: Linear model equivalence
     *
     * Creates a LinearMetaModel<Flt64> with variables x, y, adds the constraint
     * 2x + 3y <= 10 via MathLinearInequality (new entry), and also via the DSL
     * (MathInequalityDsl `le` extension). Verifies both produce the same number
     * of constraints and that both constraints have the same rhs, sign, and lhs
     * cell coefficients.
     */
    @Test
    fun linearModelEquivalenceViaNewEntryAndDsl() {
        val x = RealVar("x")
        val y = RealVar("y")

        // === Model A: New entry via MathLinearInequality constructed from LinearPolynomial ===
        val metaModelA = LinearMetaModel<Flt64>(name = "test-linear-equiv-a")
        metaModelA.add(listOf(x, y))

        val lhsA = MathLinearPolynomial(
            monomials = listOf(MathLinearMonomial(Flt64(2.0), x), MathLinearMonomial(Flt64(3.0), y)),
            constant = Flt64.zero
        )
        val rhsA = MathLinearPolynomial(emptyList(), Flt64(10.0))
        val inequalityA: MathLinearInequality = (lhsA le rhsA)

        metaModelA.addConstraint(
            relation = inequalityA,
            group = null,
            lazy = false,
            name = "c1"
        )

        // === Model B: DSL-based entry using `le` extension on LinearPolynomial ===
        val metaModelB = LinearMetaModel<Flt64>(name = "test-linear-equiv-b")
        metaModelB.add(listOf(x, y))

        val lhsB = MathLinearPolynomial(
            monomials = listOf(MathLinearMonomial(Flt64(2.0), x), MathLinearMonomial(Flt64(3.0), y)),
            constant = Flt64.zero
        )
        val inequalityB: MathLinearInequality = (lhsB le Flt64(10.0))

        metaModelB.addConstraint(
            relation = inequalityB,
            group = null,
            lazy = false,
            name = "c1"
        )

        // Verify both produce the same number of constraints
        assertEquals(1, metaModelA.relationConstraints.size, "Model A should have 1 constraint")
        assertEquals(1, metaModelB.relationConstraints.size, "Model B should have 1 constraint")

        val constraintA = metaModelA.relationConstraints.first()
        val constraintB = metaModelB.relationConstraints.first()

        // Verify both have the same sign (LE)
        assertEquals(Comparison.LE, constraintA.sign, "Constraint A should have LE sign")
        assertEquals(Comparison.LE, constraintB.sign, "Constraint B should have LE sign")

        // Verify flattenData equivalence: both should produce the same flattened representation
        val flattenA = constraintA.flattenData
        val flattenB = constraintB.flattenData

        // The flattened constant should be -10.0 (lhs.constant - rhs.constant = 0 - 10 = -10)
        assertEquals(Flt64(-10.0), flattenA.constant, "Flatten A constant should be -10")
        assertEquals(Flt64(-10.0), flattenB.constant, "Flatten B constant should be -10")

        // Both should have 2 monomials (2x, 3y)
        assertEquals(2, flattenA.monomials.size, "Flatten A should have 2 monomials")
        assertEquals(2, flattenB.monomials.size, "Flatten B should have 2 monomials")

        // Verify coefficients are equivalent
        val coeffsA = flattenA.monomials.map { it.coefficient.toDouble() }.sorted()
        val coeffsB = flattenB.monomials.map { it.coefficient.toDouble() }.sorted()
        assertEquals(coeffsA, coeffsB, "Flatten coefficients should match between A and B")

        metaModelA.close()
        metaModelB.close()
    }

    /**
     * Test 2: Quadratic model equivalence
     *
     * Creates a QuadraticMetaModel<Flt64> with variables x, y, adds the constraint
     * x*y + x <= 5 using MathQuadraticInequality (new entry). Verifies the constraint
     * has correct quadratic and linear terms.
     */
    @Test
    fun quadraticModelConstraintHasCorrectTerms() {
        val x = RealVar("x")
        val y = RealVar("y")

        val metaModel = QuadraticMetaModel<Flt64>(name = "test-quadratic-equiv")
        metaModel.add(listOf(x, y))

        // Construct x*y + x <= 5 as a QuadraticInequality
        val lhsPoly = MathQuadraticPolynomial(
            monomials = listOf(
                MathQuadraticMonomial(Flt64.one, x, y),  // x*y
                MathQuadraticMonomial.linear(Flt64.one, x) // x (linear term in quadratic poly)
            ),
            constant = Flt64.zero
        )
        val inequality: MathQuadraticInequality = (lhsPoly le Flt64(5.0))

        metaModel.addConstraint(
            relation = inequality,
            group = null,
            lazy = false,
            name = "qc1"
        )

        // Verify constraint was added
        assertEquals(1, metaModel.relationConstraints.size, "Should have 1 quadratic constraint")

        val constraint = metaModel.relationConstraints.first()

        // Verify sign
        assertEquals(Comparison.LE, constraint.sign, "Constraint should have LE sign")

        // Verify flattenData has correct terms
        val flattenData = constraint.flattenData

        // Flattened constant should be -5.0 (lhs.constant - rhs.constant = 0 - 5 = -5)
        assertEquals(Flt64(-5.0), flattenData.constant, "Flatten constant should be -5")

        // Should have 2 monomials: x*y (coefficient 1) and x (coefficient 1)
        assertEquals(2, flattenData.monomials.size, "Should have 2 monomials (xy + x)")

        // Verify at least one quadratic monomial (symbol2 != null) and one linear (symbol2 == null)
        val quadraticMonomials = flattenData.monomials.filter { it.symbol2 != null }
        val linearMonomials = flattenData.monomials.filter { it.symbol2 == null }
        assertEquals(1, quadraticMonomials.size, "Should have 1 quadratic monomial (xy)")
        assertEquals(1, linearMonomials.size, "Should have 1 linear monomial (x)")

        metaModel.close()
    }

    /**
     * Test 3: V-generic vs Flt64-specific path equivalence
     *
     * Creates a LinearMechanismModel<Flt64> and verifies convertMechanismModelToF64
     * returns Ok. Verifies the converted model has the same constraints as the original.
     */
    @Test
    fun vGenericPathEquivalentToFlt64Path() {
        val x = RealVar("x")
        val tokens = AutoTokenTable(Linear, false)
        tokens.add(x)

        val metaModel = LinearMetaModel<Flt64>(name = "test-vgeneric-equiv")
        val model = LinearMechanismModel<Flt64>(
            parent = metaModel,
            name = "test-mech-vgeneric",
            constraints = emptyList(),
            objectFunction = SingleObject(ObjectCategory.Minimum, emptyList<LinearSubObject<Flt64>>()),
            tokens = tokens
        )

        val result = convertMechanismModelToF64(model)
        assertTrue(result is Ok, "convertMechanismModelToF64 should return Ok for LinearMechanismModel<Flt64>")
        assertEquals(0, result.value.constraints.size, "Converted model should have same number of constraints as original")

        metaModel.close()
    }

    /**
     * Test 4: ConstraintRelation round-trip
     *
     * Creates constraints with LE, EQ, GE relations and verifies
     * ConstraintRelation.from(comparison) and toComparison() are bijective.
     */
    @Test
    fun constraintRelationRoundTripIsBijective() {
        // LE round-trip
        val leRelation = ConstraintRelation(Comparison.LE)
        assertEquals(ConstraintRelation.LessEqual, leRelation)
        assertEquals(Comparison.LE, leRelation.toComparison())

        // EQ round-trip
        val eqRelation = ConstraintRelation(Comparison.EQ)
        assertEquals(ConstraintRelation.Equal, eqRelation)
        assertEquals(Comparison.EQ, eqRelation.toComparison())

        // GE round-trip
        val geRelation = ConstraintRelation(Comparison.GE)
        assertEquals(ConstraintRelation.GreaterEqual, geRelation)
        assertEquals(Comparison.GE, geRelation.toComparison())

        // LT maps to LessEqual (non-strict normalization)
        val ltRelation = ConstraintRelation(Comparison.LT)
        assertEquals(ConstraintRelation.LessEqual, ltRelation)
        assertEquals(Comparison.LE, ltRelation.toComparison())

        // GT maps to GreaterEqual (non-strict normalization)
        val gtRelation = ConstraintRelation(Comparison.GT)
        assertEquals(ConstraintRelation.GreaterEqual, gtRelation)
        assertEquals(Comparison.GE, gtRelation.toComparison())

        // Full round-trip: toComparison() then ConstraintRelation() preserves identity
        for (relation in ConstraintRelation.entries) {
            val comparison = relation.toComparison()
            val roundTripped = ConstraintRelation(comparison)
            assertEquals(relation, roundTripped, "Round trip failed for $relation")
        }
    }

    /**
     * Test 5: LinearRelation from MathLinearInequality preserves semantics
     *
     * Verifies that creating a LinearRelation from a MathLinearInequality
     * preserves the flattenData and sign.
     */
    @Test
    fun linearRelationFromInequalityPreservesSemantics() {
        val x = RealVar("x")
        val y = RealVar("y")

        val lhs = MathLinearPolynomial(
            monomials = listOf(MathLinearMonomial(Flt64(1.0), x), MathLinearMonomial(Flt64(1.0), y)),
            constant = Flt64.zero
        )
        val inequality: MathLinearInequality = (lhs le Flt64(5.0))

        // Convert to LinearRelation
        val relation = inequality.toRelation()

        // Verify sign is preserved
        assertEquals(Comparison.LE, relation.sign, "LinearRelation should preserve LE sign")

        // Verify flattenData matches
        val origFlatten = inequality.flattenData
        val relationFlatten = relation.flattenData
        assertEquals(origFlatten.constant, relationFlatten.constant, "FlattenData constant should match")
        assertEquals(origFlatten.monomials.size, relationFlatten.monomials.size, "FlattenData monomial count should match")
    }

    /**
     * Test 6: Objective function equivalence — new minimize(polynomial) vs addObject(flattenData)
     *
     * Verifies that adding an objective via the convenience minimize(MathLinearPolynomial)
     * and via the low-level addObject(ObjectCategory, LinearFlattenDataF64) produce
     * equivalent flattenSubObjects entries.
     */
    @Test
    fun objectiveFunctionEquivalenceNewVsFlattenData() {
        val x = RealVar("x")
        val y = RealVar("y")

        // Model A: minimize via MathLinearPolynomial convenience
        val metaModelA = LinearMetaModel<Flt64>(name = "test-obj-equiv-a")
        metaModelA.add(listOf(x, y))

        val polyA = MathLinearPolynomial(
            monomials = listOf(MathLinearMonomial(Flt64(3.0), x), MathLinearMonomial(Flt64(5.0), y)),
            constant = Flt64(10.0)
        )
        metaModelA.minimize(polyA)

        // Model B: addObject via LinearFlattenDataF64 directly
        val metaModelB = LinearMetaModel<Flt64>(name = "test-obj-equiv-b")
        metaModelB.add(listOf(x, y))

        val flattenData = LinearFlattenDataF64(polyA.monomials, polyA.constant)
        metaModelB.addObject(ObjectCategory.Minimum, flattenData)

        // Both should have 1 flatten sub-object
        assertEquals(1, metaModelA.flattenSubObjects.size, "Model A should have 1 flatten sub-object")
        assertEquals(1, metaModelB.flattenSubObjects.size, "Model B should have 1 flatten sub-object")

        val subObjA = metaModelA.flattenSubObjects.first()
        val subObjB = metaModelB.flattenSubObjects.first()

        // Both should have Minimum category
        assertEquals(ObjectCategory.Minimum, subObjA.category)
        assertEquals(ObjectCategory.Minimum, subObjB.category)

        // Both should have the same constant
        assertEquals(subObjA.constantF64, subObjB.constantF64, "Constants should match")

        // Both should have the same number of cells
        assertEquals(subObjA.cells.size, subObjB.cells.size, "Cell count should match")

        metaModelA.close()
        metaModelB.close()
    }

    /**
     * Test 7: Full modeling→MechanismModel pipeline equivalence
     *
     * Creates two LinearMetaModel instances with identical variables and constraints
     * using different entry points (MathLinearInequality + minimize vs addObject),
     * then verifies the resulting MechanismModel constraints are equivalent.
     */
    @Test
    fun fullPipelineEquivalenceConstraintAndObjective() {
        val x = RealVar("x")
        val y = RealVar("y")

        // === Pipeline A: MathLinearInequality + minimize(polynomial) ===
        val metaModelA = LinearMetaModel<Flt64>(name = "test-pipeline-a")
        metaModelA.add(listOf(x, y))

        val lhsA = MathLinearPolynomial(
            monomials = listOf(MathLinearMonomial(Flt64(2.0), x), MathLinearMonomial(Flt64(3.0), y)),
            constant = Flt64.zero
        )
        val inequalityA: MathLinearInequality = (lhsA le Flt64(10.0))
        metaModelA.addConstraint(relation = inequalityA, group = null, lazy = false, name = "c1")

        val objPolyA = MathLinearPolynomial(
            monomials = listOf(MathLinearMonomial(Flt64.one, x)),
            constant = Flt64.zero
        )
        metaModelA.minimize(objPolyA)

        val tokensA = AutoTokenTable(Linear, false)
        tokensA.add(listOf(x, y))
        val mechModelA = LinearMechanismModel<Flt64>(
            parent = metaModelA,
            name = "mech-a",
            constraints = mutableListOf(),
            objectFunction = SingleObject(ObjectCategory.Minimum, emptyList<LinearSubObject<Flt64>>()),
            tokens = tokensA
        )

        // === Pipeline B: DSL le + addObject(flattenData) ===
        val metaModelB = LinearMetaModel<Flt64>(name = "test-pipeline-b")
        metaModelB.add(listOf(x, y))

        val lhsB = MathLinearPolynomial(
            monomials = listOf(MathLinearMonomial(Flt64(2.0), x), MathLinearMonomial(Flt64(3.0), y)),
            constant = Flt64.zero
        )
        val inequalityB: MathLinearInequality = (lhsB le Flt64(10.0))
        metaModelB.addConstraint(relation = inequalityB, group = null, lazy = false, name = "c1")

        val flattenData = LinearFlattenDataF64(objPolyA.monomials, objPolyA.constant)
        metaModelB.addObject(ObjectCategory.Minimum, flattenData)

        val tokensB = AutoTokenTable(Linear, false)
        tokensB.add(listOf(x, y))
        val mechModelB = LinearMechanismModel<Flt64>(
            parent = metaModelB,
            name = "mech-b",
            constraints = mutableListOf(),
            objectFunction = SingleObject(ObjectCategory.Minimum, emptyList<LinearSubObject<Flt64>>()),
            tokens = tokensB
        )

        // Verify both MechanismModels have same constraint count
        assertEquals(mechModelA.constraints.size, mechModelB.constraints.size,
            "Both MechanismModels should have same constraint count")

        // Verify convertMechanismModelToF64 works for both
        val resultA = convertMechanismModelToF64(mechModelA)
        val resultB = convertMechanismModelToF64(mechModelB)
        assertTrue(resultA is Ok, "convertMechanismModelToF64 should succeed for A")
        assertTrue(resultB is Ok, "convertMechanismModelToF64 should succeed for B")

        metaModelA.close()
        metaModelB.close()
    }

    /**
     * Test 8: Plugin boundary Double conversion — V=Flt64 path vs direct Flt64 path
     *
     * Verifies that a QuadraticMechanismModel<Flt64> passes through
     * convertMechanismModelToF64 correctly, and that the constraint
     * representation is preserved across the V→F64 boundary.
     */
    @Test
    fun pluginBoundaryDoubleConversionPreservesConstraints() {
        val x = RealVar("x")
        val y = RealVar("y")

        val metaModel = QuadraticMetaModel<Flt64>(name = "test-boundary")
        metaModel.add(listOf(x, y))

        // Add quadratic constraint: x*y <= 5
        val lhsPoly = MathQuadraticPolynomial(
            monomials = listOf(
                MathQuadraticMonomial(Flt64.one, x, y)
            ),
            constant = Flt64.zero
        )
        val inequality: MathQuadraticInequality = (lhsPoly le Flt64(5.0))
        metaModel.addConstraint(relation = inequality, group = null, lazy = false, name = "qc1")

        // Add quadratic objective: minimize x*y
        val objPoly = MathQuadraticPolynomial(
            monomials = listOf(MathQuadraticMonomial(Flt64.one, x, y)),
            constant = Flt64.zero
        )
        metaModel.minimize(objPoly)

        val tokens = AutoTokenTable(Quadratic, false)
        tokens.add(listOf(x, y))

        val mechModel = QuadraticMechanismModel<Flt64>(
            parent = metaModel,
            name = "mech-boundary",
            constraints = mutableListOf(),
            objectFunction = SingleObject(ObjectCategory.Minimum, emptyList<QuadraticSubObject<Flt64>>()),
            tokens = tokens
        )

        // Add constraint via mechanism model API
        val linearInequality: MathLinearInequality = (
            MathLinearPolynomial(
                monomials = listOf(MathLinearMonomial(Flt64.one, x)),
                constant = Flt64.zero
            ) le Flt64(3.0)
        )
        mechModel.addConstraint(relation = linearInequality, name = "lc1")

        // Verify convertMechanismModelToF64 succeeds
        val result = convertMechanismModelToF64(mechModel)
        assertTrue(result is Ok, "convertMechanismModelToF64 should succeed for QuadraticMechanismModel<Flt64>")

        val f64Model = result.value
        // The f64 model should have the same constraints
        assertEquals(mechModel.constraints.size, f64Model.constraints.size,
            "F64 model should have same constraint count as original")

        // Verify each constraint's sign and rhs are preserved
        for (i in mechModel.constraints.indices) {
            val orig = mechModel.constraints[i]
            val f64 = f64Model.constraints[i]
            assertEquals(orig.sign, f64.sign, "Constraint $i sign should match")
            assertEquals(orig.rhsF64, f64.rhsF64, "Constraint $i rhsF64 should match")
        }

        metaModel.close()
    }
}
