package fuookami.ospf.kotlin.core.intermediate_model

import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.eq
import fuookami.ospf.kotlin.math.symbol.inequality.Flt64LinearInequality as MathLinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequality as MathQuadraticInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial as MathLinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial as MathQuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial as MathLinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial as MathQuadraticPolynomial
import fuookami.ospf.kotlin.utils.functional.Ok
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * P1-10 Semantic Equivalence Regression Test
 *
 * Verifies that the math.symbol-based API produces correct models
 * and that the V-generic and Flt64-specific paths are equivalent.
 */
class SemanticEquivalenceTest {

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
     * Creates a LinearMetaModel<Flt64> with real constraints (2x + 3y <= 10) and
     * objective (min x), then builds a LinearMechanismModel via invoke() and
     * verifies convertMechanismModelToF64 preserves the constraints and objective.
     */
    @Test
    fun vGenericPathEquivalentToFlt64Path() = runBlocking {
        val x = RealVar("x")
        val y = RealVar("y")

        val metaModel = LinearMetaModel<Flt64>(
            name = "test-vgeneric-equiv",
            configuration = MetaModelConfiguration(manualTokenAddition = true, concurrent = false)
        )
        metaModel.add(listOf(x, y))

        // Add real constraint: 2x + 3y <= 10
        val lhs = MathLinearPolynomial(
            monomials = listOf(MathLinearMonomial(Flt64(2.0), x), MathLinearMonomial(Flt64(3.0), y)),
            constant = Flt64.zero
        )
        val inequality: MathLinearInequality = (lhs le Flt64(10.0))
        metaModel.addConstraint(relation = inequality, group = null, lazy = false, name = "c1")

        // Add real objective: minimize x
        val objPoly = MathLinearPolynomial(
            monomials = listOf(MathLinearMonomial(Flt64.one, x)),
            constant = Flt64.zero
        )
        metaModel.addObject(ObjectCategory.Minimum, objPoly, "obj", null)

        // Build MechanismModel via invoke() — this is the real MetaModel -> MechanismModel pipeline
        val mechResult = LinearMechanismModel.invoke(metaModel, concurrent = false)
        assertTrue(mechResult is Ok, "LinearMechanismModel.invoke should succeed")

        val mechModel = mechResult.value
        assertEquals(1, mechModel.constraints.size, "MechanismModel should have 1 constraint from MetaModel")

        // Verify the constraint has correct sign and rhs
        val constraint = mechModel.constraints.first()
        assertEquals(ConstraintRelation.LessEqual, constraint.sign, "Constraint should be LE")
        assertEquals(Flt64(10.0), constraint.rhsF64, "Constraint rhs should be 10")

        // Verify objective is present
        assertEquals(ObjectCategory.Minimum, mechModel.objectFunction.category, "Objective should be Minimum")
        assertTrue(mechModel.objectFunction.subObjects.isNotEmpty(), "Objective should have sub-objects")

        // Verify convertMechanismModelToF64 preserves everything
        val f64Result = convertMechanismModelToF64(mechModel)
        assertTrue(f64Result is Ok, "convertMechanismModelToF64 should return Ok")
        assertEquals(1, f64Result.value.constraints.size, "F64 model should preserve constraint count")

        val f64Constraint = f64Result.value.constraints.first()
        assertEquals(constraint.sign, f64Constraint.sign, "F64 constraint sign should match")
        assertEquals(constraint.rhsF64, f64Constraint.rhsF64, "F64 constraint rhs should match")

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
     * Test 7: Full MetaModel→MechanismModel pipeline equivalence
     *
     * Verifies two aspects:
     * - Constraint pipeline: two MetaModel instances with identical constraints built via
     *   addConstraint(relation: MathLinearInequality), then MechanismModel via invoke(),
     *   verify constraints are equivalent.
     * - Objective equivalence: addObject(category, polynomial) writes _subObjects (consumed
     *   by invoke()), while addObject(category, flattenData) writes _flattenSubObjects
     *   (consumed by solver directly, not by invoke()). Both represent the same objective
     *   but through different storage paths. We verify MetaModel-level equivalence of
     *   both stores, and that invoke() correctly reads from _subObjects.
     */
    @Test
    fun fullPipelineEquivalenceConstraintAndObjective() = runBlocking {
        val x = RealVar("x")
        val y = RealVar("y")

        // === Constraint pipeline A: addConstraint(relation) + addObject(category, polynomial) ===
        val metaModelA = LinearMetaModel<Flt64>(
            name = "test-pipeline-a",
            configuration = MetaModelConfiguration(manualTokenAddition = true, concurrent = false)
        )
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
        metaModelA.addObject(ObjectCategory.Minimum, objPolyA, "obj", null)

        // Build MechanismModel via invoke() — real pipeline (reads _subObjects)
        val mechResultA = LinearMechanismModel.invoke(metaModelA, concurrent = false)
        assertTrue(mechResultA is Ok, "LinearMechanismModel.invoke should succeed for A")
        val mechModelA = mechResultA.value

        // === Objective path B: addObject(category, flattenData) only ===
        // This writes _flattenSubObjects (not _subObjects). invoke() does NOT read
        // _flattenSubObjects, so B cannot go through invoke(). Instead we verify
        // the MetaModel-level storage is correct.
        val metaModelB = LinearMetaModel<Flt64>(
            name = "test-pipeline-b",
            configuration = MetaModelConfiguration(manualTokenAddition = true, concurrent = false)
        )
        metaModelB.add(listOf(x, y))

        val lhsB = MathLinearPolynomial(
            monomials = listOf(MathLinearMonomial(Flt64(2.0), x), MathLinearMonomial(Flt64(3.0), y)),
            constant = Flt64.zero
        )
        val inequalityB: MathLinearInequality = (lhsB le Flt64(10.0))
        metaModelB.addConstraint(relation = inequalityB, group = null, lazy = false, name = "c1")

        // B path: addObject via flattenData only — writes to _flattenSubObjects
        val flattenData = LinearFlattenDataF64(objPolyA.monomials, objPolyA.constant)
        metaModelB.addObject(ObjectCategory.Minimum, flattenData)

        // === Verify constraint equivalence at MetaModel level ===
        assertEquals(metaModelA.relationConstraints.size, metaModelB.relationConstraints.size,
            "Both MetaModels should have same constraint count")
        val constraintA = metaModelA.relationConstraints.first()
        val constraintB = metaModelB.relationConstraints.first()
        assertEquals(constraintA.sign, constraintB.sign, "Constraint signs should match at MetaModel level")
        assertEquals(constraintA.flattenData.constant, constraintB.flattenData.constant,
            "Constraint flattenData constants should match")

        // === Verify objective equivalence at MetaModel level ===
        // A's _subObjects and B's _flattenSubObjects should represent the same objective
        assertEquals(1, metaModelA.subObjects.size, "Model A should have 1 subObject (from addObject(polynomial))")
        assertEquals(1, metaModelB.flattenSubObjects.size, "Model B should have 1 flattenSubObject (from addObject(flattenData))")
        assertEquals(0, metaModelB.subObjects.size, "Model B should have 0 subObjects (only flattenData path used)")

        val subObjA = metaModelA.subObjects.first()
        val flattenSubObjB = metaModelB.flattenSubObjects.first()
        assertEquals(subObjA.category, flattenSubObjB.category, "Objective categories should match")
        assertEquals(subObjA.polynomial.constant, flattenSubObjB.constantF64, "Objective constants should match")
        assertEquals(subObjA.polynomial.monomials.size, flattenSubObjB.cells.size,
            "Objective monomial/cell count should match")

        // === Verify MechanismModel from A has correct objective (invoke reads _subObjects) ===
        assertEquals(ObjectCategory.Minimum, mechModelA.objectFunction.category,
            "MechanismModel A objective should be Minimum")
        assertTrue(mechModelA.objectFunction.subObjects.isNotEmpty(),
            "MechanismModel A should have objective sub-objects")

        // === Verify convertMechanismModelToF64 preserves constraints ===
        val f64ResultA = convertMechanismModelToF64(mechModelA)
        assertTrue(f64ResultA is Ok, "convertMechanismModelToF64 should succeed for A")
        assertEquals(1, f64ResultA.value.constraints.size, "F64 model should preserve constraint count")

        metaModelA.close()
        metaModelB.close()
    }

    /**
     * Test 8: Plugin boundary Double conversion — quadratic constraints survive V→F64 boundary
     *
     * Creates a QuadraticMetaModel<Flt64> with a real quadratic constraint (x*y <= 5)
     * and a linear constraint (x <= 3), builds QuadraticMechanismModel via invoke(),
     * then verifies convertMechanismModelToF64 preserves ALL constraints including
     * the quadratic one.
     */
    @Test
    fun pluginBoundaryDoubleConversionPreservesConstraints() = runBlocking {
        val x = RealVar("x")
        val y = RealVar("y")

        val metaModel = QuadraticMetaModel<Flt64>(
            name = "test-boundary",
            configuration = MetaModelConfiguration(manualTokenAddition = true, concurrent = false)
        )
        metaModel.add(listOf(x, y))

        // Add quadratic constraint: x*y <= 5
        val quadLhs = MathQuadraticPolynomial(
            monomials = listOf(
                MathQuadraticMonomial(Flt64.one, x, y)
            ),
            constant = Flt64.zero
        )
        val quadInequality: MathQuadraticInequality = (quadLhs le Flt64(5.0))
        metaModel.addConstraint(relation = quadInequality, group = null, lazy = false, name = "qc1")

        // Add linear constraint: x <= 3 (converted to quadratic internally)
        val linLhs = MathLinearPolynomial(
            monomials = listOf(MathLinearMonomial(Flt64.one, x)),
            constant = Flt64.zero
        )
        val linInequality: MathLinearInequality = (linLhs le Flt64(3.0))
        metaModel.addConstraint(relation = linInequality, group = null, lazy = false, name = "lc1")

        // Add quadratic objective: minimize x*y
        val objPoly = MathQuadraticPolynomial(
            monomials = listOf(MathQuadraticMonomial(Flt64.one, x, y)),
            constant = Flt64.zero
        )
        metaModel.addObject(ObjectCategory.Minimum, objPoly, "obj", null)

        // Build MechanismModel via invoke() — real pipeline
        val mechResult = QuadraticMechanismModel.invoke(metaModel, concurrent = false)
        assertTrue(mechResult is Ok, "QuadraticMechanismModel.invoke should succeed")
        val mechModel = mechResult.value

        // Verify both constraints are present in the MechanismModel
        assertEquals(2, mechModel.constraints.size,
            "MechanismModel should have 2 constraints (1 quadratic + 1 linear)")

        // Verify the quadratic constraint has correct sign and rhs
        val qcConstraint = mechModel.constraints[0]
        assertEquals(ConstraintRelation.LessEqual, qcConstraint.sign, "Quadratic constraint should be LE")
        assertEquals(Flt64(5.0), qcConstraint.rhsF64, "Quadratic constraint rhs should be 5")

        // Verify the linear constraint (converted to quadratic form) has correct sign and rhs
        val lcConstraint = mechModel.constraints[1]
        assertEquals(ConstraintRelation.LessEqual, lcConstraint.sign, "Linear constraint should be LE")
        assertEquals(Flt64(3.0), lcConstraint.rhsF64, "Linear constraint rhs should be 3")

        // Verify objective is present
        assertEquals(ObjectCategory.Minimum, mechModel.objectFunction.category, "Objective should be Minimum")
        assertTrue(mechModel.objectFunction.subObjects.isNotEmpty(), "Objective should have sub-objects")

        // Verify convertMechanismModelToF64 preserves all constraints
        val f64Result = convertMechanismModelToF64(mechModel)
        assertTrue(f64Result is Ok, "convertMechanismModelToF64 should succeed for QuadraticMechanismModel<Flt64>")

        val f64Model = f64Result.value
        assertEquals(mechModel.constraints.size, f64Model.constraints.size,
            "F64 model should have same constraint count as original")

        // Verify each constraint's sign and rhs are preserved across the V→F64 boundary
        for (i in mechModel.constraints.indices) {
            val orig = mechModel.constraints[i]
            val f64 = f64Model.constraints[i]
            assertEquals(orig.sign, f64.sign, "Constraint $i sign should match after F64 conversion")
            assertEquals(orig.rhsF64, f64.rhsF64, "Constraint $i rhsF64 should match after F64 conversion")
        }

        // Verify the quadratic constraint's lhs has a quadratic cell (token2 != null)
        val f64QcConstraint = f64Model.constraints[0]
        val hasQuadraticCell = f64QcConstraint.lhs.any { cell ->
            (cell as? QuadraticCellF64)?.token2 != null
        }
        assertTrue(hasQuadraticCell, "Quadratic constraint should have at least one quadratic cell (token2 != null) after F64 conversion")

        metaModel.close()
    }
}
