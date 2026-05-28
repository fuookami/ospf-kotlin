package fuookami.ospf.kotlin.core.intermediate_model

import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.testing.*
import fuookami.ospf.kotlin.core.token.QuadraticFlattenData
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.utils.functional.Ok
import kotlin.test.*
import kotlinx.coroutines.runBlocking

class GenericQuadraticMetaModelBuildTest {
    @Test
    fun fourNumberTypesShouldBuildQuadraticMetaModelAndDumpMechanismModel() {
        buildQuadratic(GenericNumberCases.flt64)
        buildQuadratic(GenericNumberCases.rtn64)
        buildQuadratic(GenericNumberCases.fltX)
        buildQuadratic(GenericNumberCases.rtnX)
    }

    private fun <V> buildQuadratic(numberCase: GenericNumberCase<V>)
            where V : RealNumber<V>, V : NumberField<V> {
        val x = RealVar("${numberCase.name.lowercase()}_quad_x")
        val y = RealVar("${numberCase.name.lowercase()}_quad_y")
        val z = RealVar("${numberCase.name.lowercase()}_quad_z")

        val model = QuadraticMetaModel<V>(
            name = "generic-quadratic-${numberCase.name.lowercase()}",
            objectCategory = ObjectCategory.Minimum,
            converter = numberCase.converter
        )

        try {
            assertTrue(model.add(listOf(x, y, z)) is Ok, "${numberCase.name}: add variables should succeed")

            val xUpperBound = QuadraticInequalityOf(
                lhs = QuadraticPolynomial(
                    monomials = listOf(QuadraticMonomial.linear(numberCase.one, x)),
                    constant = numberCase.zero
                ),
                rhs = QuadraticPolynomial(emptyList(), value(numberCase, 8.0)),
                comparison = Comparison.LE
            )
            assertTrue(
                model.addConstraint(relation = xUpperBound, name = "bound_x_upper") is Ok,
                "${numberCase.name}: add x upper bound should succeed"
            )

            val yLowerBound = QuadraticInequalityOf(
                lhs = QuadraticPolynomial(
                    monomials = listOf(QuadraticMonomial.linear(numberCase.one, y)),
                    constant = numberCase.zero
                ),
                rhs = QuadraticPolynomial(emptyList(), value(numberCase, -1.0)),
                comparison = Comparison.GE
            )
            assertTrue(
                model.addConstraint(relation = yLowerBound, name = "bound_y_lower") is Ok,
                "${numberCase.name}: add y lower bound should succeed"
            )

            val leLhs = QuadraticPolynomial(
                monomials = listOf(
                    QuadraticMonomial.quadratic(value(numberCase, 2.0), x, y),
                    QuadraticMonomial.quadratic(value(numberCase, -3.0), x, x),
                    QuadraticMonomial.linear(value(numberCase, 4.0), z)
                ),
                constant = value(numberCase, 5.0)
            )
            val leRelation = QuadraticInequalityOf(lhs = leLhs, rhs = QuadraticPolynomial(emptyList(), numberCase.zero), comparison = Comparison.LE)
            assertTrue(
                model.addConstraint(relation = leRelation, name = "qc_le") is Ok,
                "${numberCase.name}: add qc_le should succeed"
            )

            val geLhs = QuadraticPolynomial(
                monomials = listOf(
                    QuadraticMonomial.quadratic(value(numberCase, -1.0), x, y),
                    QuadraticMonomial.quadratic(value(numberCase, 2.0), y, z),
                    QuadraticMonomial.linear(value(numberCase, -1.0), z)
                ),
                constant = numberCase.one
            )
            val geRelation = QuadraticInequalityOf(lhs = geLhs, rhs = QuadraticPolynomial(emptyList(), numberCase.zero), comparison = Comparison.GE)
            assertTrue(
                model.addConstraint(relation = geRelation, name = "qc_ge") is Ok,
                "${numberCase.name}: add qc_ge should succeed"
            )

            val eqLhs = QuadraticPolynomial(
                monomials = listOf(
                    QuadraticMonomial.quadratic(numberCase.one, x, x),
                    QuadraticMonomial.linear(numberCase.one, y),
                    QuadraticMonomial.linear(numberCase.one, z)
                ),
                constant = value(numberCase, -4.0)
            )
            val eqRelation = QuadraticInequalityOf(lhs = eqLhs, rhs = QuadraticPolynomial(emptyList(), numberCase.zero), comparison = Comparison.EQ)
            assertTrue(
                model.addConstraint(relation = eqRelation, name = "qc_eq") is Ok,
                "${numberCase.name}: add qc_eq should succeed"
            )

            val objective = QuadraticPolynomial(
                monomials = listOf(
                    QuadraticMonomial.quadratic(numberCase.one, x, y),
                    QuadraticMonomial.quadratic(value(numberCase, 2.0), x, x),
                    QuadraticMonomial.linear(value(numberCase, -3.0), z)
                ),
                constant = value(numberCase, 6.0)
            )
            assertTrue(
                model.addObject(
                    category = ObjectCategory.Minimum,
                    flattenData = QuadraticFlattenData(
                        monomials = objective.monomials,
                        constant = objective.constant
                    ),
                    name = "obj_quadratic"
                ) is Ok,
                "${numberCase.name}: add objective should succeed"
            )
            val mechanismResult = runBlocking {
                QuadraticMechanismModel.invoke<V>(metaModel = model, concurrent = false)
            }
            assertTrue(mechanismResult is Ok, "${numberCase.name}: dump mechanism model should succeed")
            assertMechanismModel(numberCase, mechanismResult.value)
            assertTetradModel(numberCase, mechanismResult.value, mapOf(y to Flt64.two))
        } finally {
            model.close()
        }
    }

    private fun <V> assertMechanismModel(
        numberCase: GenericNumberCase<V>,
        mechanismModel: QuadraticMechanismModel<V>
    ) where V : RealNumber<V>, V : NumberField<V> {
        assertEquals(5, mechanismModel.constraints.size, "${numberCase.name}: mechanism constraint count mismatch")
        assertEquals(ObjectCategory.Minimum, mechanismModel.objectFunction.category)
        assertEquals(1, mechanismModel.objectFunction.subObjects.size, "${numberCase.name}: objective sub-object amount mismatch")

        val constraintsByName = mechanismModel.constraints.associateBy { it.name }
        val boundUpper = assertNotNull(constraintsByName["bound_x_upper"], "${numberCase.name}: missing bound_x_upper")
        val boundLower = assertNotNull(constraintsByName["bound_y_lower"], "${numberCase.name}: missing bound_y_lower")
        val qcLe = assertNotNull(constraintsByName["qc_le"], "${numberCase.name}: missing qc_le")
        val qcGe = assertNotNull(constraintsByName["qc_ge"], "${numberCase.name}: missing qc_ge")
        val qcEq = assertNotNull(constraintsByName["qc_eq"], "${numberCase.name}: missing qc_eq")

        assertEquals(ConstraintRelation.LessEqual, boundUpper.sign, "${numberCase.name}: bound_x_upper sign mismatch")
        assertEquals(Flt64(8.0), toFlt64(numberCase, boundUpper.rhs), "${numberCase.name}: bound_x_upper rhs mismatch")
        assertQuadraticCoefficients(
            numberCase,
            boundUpper,
            mapOf(
                key("${numberCase.name.lowercase()}_quad_x", null) to Flt64.one
            )
        )

        assertEquals(ConstraintRelation.GreaterEqual, boundLower.sign, "${numberCase.name}: bound_y_lower sign mismatch")
        assertEquals(Flt64(-1.0), toFlt64(numberCase, boundLower.rhs), "${numberCase.name}: bound_y_lower rhs mismatch")
        assertQuadraticCoefficients(
            numberCase,
            boundLower,
            mapOf(
                key("${numberCase.name.lowercase()}_quad_y", null) to Flt64.one
            )
        )

        assertEquals(ConstraintRelation.LessEqual, qcLe.sign, "${numberCase.name}: qc_le sign mismatch")
        assertEquals(Flt64(-5.0), toFlt64(numberCase, qcLe.rhs), "${numberCase.name}: qc_le rhs mismatch")
        assertQuadraticCoefficients(
            numberCase,
            qcLe,
            mapOf(
                key("${numberCase.name.lowercase()}_quad_x", "${numberCase.name.lowercase()}_quad_y") to Flt64(2.0),
                key("${numberCase.name.lowercase()}_quad_x", "${numberCase.name.lowercase()}_quad_x") to Flt64(-3.0),
                key("${numberCase.name.lowercase()}_quad_z", null) to Flt64(4.0)
            )
        )

        assertEquals(ConstraintRelation.GreaterEqual, qcGe.sign, "${numberCase.name}: qc_ge sign mismatch")
        assertEquals(Flt64(-1.0), toFlt64(numberCase, qcGe.rhs), "${numberCase.name}: qc_ge rhs mismatch")
        assertQuadraticCoefficients(
            numberCase,
            qcGe,
            mapOf(
                key("${numberCase.name.lowercase()}_quad_x", "${numberCase.name.lowercase()}_quad_y") to Flt64(-1.0),
                key("${numberCase.name.lowercase()}_quad_y", "${numberCase.name.lowercase()}_quad_z") to Flt64(2.0),
                key("${numberCase.name.lowercase()}_quad_z", null) to Flt64(-1.0)
            )
        )

        assertEquals(ConstraintRelation.Equal, qcEq.sign, "${numberCase.name}: qc_eq sign mismatch")
        assertEquals(Flt64(4.0), toFlt64(numberCase, qcEq.rhs), "${numberCase.name}: qc_eq rhs mismatch")
        assertQuadraticCoefficients(
            numberCase,
            qcEq,
            mapOf(
                key("${numberCase.name.lowercase()}_quad_x", "${numberCase.name.lowercase()}_quad_x") to Flt64.one,
                key("${numberCase.name.lowercase()}_quad_y", null) to Flt64.one,
                key("${numberCase.name.lowercase()}_quad_z", null) to Flt64.one
            )
        )

        val subObject = mechanismModel.objectFunction.subObjects.first()
        assertEquals(ObjectCategory.Minimum, subObject.category, "${numberCase.name}: objective category mismatch")
        assertEquals(Flt64(6.0), toFlt64(numberCase, subObject.constant), "${numberCase.name}: objective constant mismatch")
        val objectiveCoefficients = quadraticCellsToFlt64Map(numberCase, subObject.cells, numberCase.zero)
        assertEquals(
            mapOf(
                key("${numberCase.name.lowercase()}_quad_x", "${numberCase.name.lowercase()}_quad_y") to Flt64.one,
                key("${numberCase.name.lowercase()}_quad_x", "${numberCase.name.lowercase()}_quad_x") to Flt64(2.0),
                key("${numberCase.name.lowercase()}_quad_z", null) to Flt64(-3.0)
            ),
            objectiveCoefficients,
            "${numberCase.name}: objective coefficient mismatch"
        )
        assertTrue(
            subObject.cells.all { it.coefficient::class == numberCase.zero::class },
            "${numberCase.name}: objective coefficient type should stay V"
        )
    }

    private fun <V> assertTetradModel(
        numberCase: GenericNumberCase<V>,
        mechanismModel: QuadraticMechanismModel<V>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>
    ) where V : RealNumber<V>, V : NumberField<V> {
        val flt64ModelResult = convertMechanismModelToFlt64(mechanismModel)
        assertTrue(flt64ModelResult is Ok, "${numberCase.name}: mechanism -> Flt64 conversion should succeed")
        val flt64Model = flt64ModelResult.value as? QuadraticMechanismModel<Flt64>
        assertNotNull(flt64Model, "${numberCase.name}: converted model should be quadratic mechanism model")
        val tetradModel = runBlocking {
            QuadraticTetradModel.invoke(
                model = flt64Model,
                fixedVariables = fixedVariables,
                concurrent = false
            )
        }

        assertEquals(2, tetradModel.variables.size, "${numberCase.name}: tetrad variable count mismatch")
        val variableByName = tetradModel.variables.associateBy { it.name }
        val xName = "${numberCase.name.lowercase()}_quad_x"
        val yName = "${numberCase.name.lowercase()}_quad_y"
        val zName = "${numberCase.name.lowercase()}_quad_z"
        assertTrue(variableByName.containsKey(xName), "${numberCase.name}: x should remain in solver")
        assertTrue(variableByName.containsKey(zName), "${numberCase.name}: z should remain in solver")
        assertTrue(!variableByName.containsKey(yName), "${numberCase.name}: fixed y should be removed from solver")

        val xVar = assertNotNull(variableByName[xName], "${numberCase.name}: missing x variable")
        assertEquals(Flt64(8.0), xVar.upperBound, "${numberCase.name}: x upper bound should be extracted from bound constraint")
        val zVar = assertNotNull(variableByName[zName], "${numberCase.name}: missing z variable")
        val zToken = flt64Model.tokens.tokens.firstOrNull { it.variable.name == zName }
        assertNotNull(zToken, "${numberCase.name}: missing z token in mechanism model")
        assertEquals(zToken.upperBound!!.value.unwrap(), zVar.upperBound, "${numberCase.name}: z upper bound should keep token default")
        assertEquals(zToken.lowerBound!!.value.unwrap(), zVar.lowerBound, "${numberCase.name}: z lower bound should keep token default")

        assertEquals(3, tetradModel.constraints.size, "${numberCase.name}: bound constraints should be removed from tetrad constraints")
        val rowIndexByName = tetradModel.constraints.names.withIndex().associate { it.value to it.index }
        assertEquals(setOf("qc_le", "qc_ge", "qc_eq"), rowIndexByName.keys, "${numberCase.name}: tetrad constraint names mismatch")
        val indexToName = tetradModel.variables.associate { it.index to it.name }

        val leRow = assertNotNull(rowIndexByName["qc_le"], "${numberCase.name}: missing qc_le row")
        assertEquals(ConstraintRelation.LessEqual, tetradModel.constraints.signs[leRow], "${numberCase.name}: tetrad qc_le sign mismatch")
        assertEquals(Flt64(-5.0), tetradModel.constraints.rhs[leRow], "${numberCase.name}: tetrad qc_le rhs mismatch")
        assertEquals(
            mapOf(
                key(xName, xName) to Flt64(-3.0),
                key(xName, null) to Flt64(4.0),
                key(zName, null) to Flt64(4.0)
            ),
            quadraticRowCoefficients(tetradModel, leRow, indexToName),
            "${numberCase.name}: tetrad qc_le coefficients mismatch"
        )

        val geRow = assertNotNull(rowIndexByName["qc_ge"], "${numberCase.name}: missing qc_ge row")
        assertEquals(ConstraintRelation.GreaterEqual, tetradModel.constraints.signs[geRow], "${numberCase.name}: tetrad qc_ge sign mismatch")
        assertEquals(Flt64(-1.0), tetradModel.constraints.rhs[geRow], "${numberCase.name}: tetrad qc_ge rhs mismatch")
        assertEquals(
            mapOf(
                key(xName, null) to Flt64(-2.0),
                key(zName, null) to Flt64(3.0)
            ),
            quadraticRowCoefficients(tetradModel, geRow, indexToName),
            "${numberCase.name}: tetrad qc_ge coefficients mismatch"
        )

        val eqRow = assertNotNull(rowIndexByName["qc_eq"], "${numberCase.name}: missing qc_eq row")
        assertEquals(ConstraintRelation.Equal, tetradModel.constraints.signs[eqRow], "${numberCase.name}: tetrad qc_eq sign mismatch")
        assertEquals(Flt64(2.0), tetradModel.constraints.rhs[eqRow], "${numberCase.name}: tetrad qc_eq rhs mismatch")
        assertEquals(
            mapOf(
                key(xName, xName) to Flt64.one,
                key(zName, null) to Flt64.one
            ),
            quadraticRowCoefficients(tetradModel, eqRow, indexToName),
            "${numberCase.name}: tetrad qc_eq coefficients mismatch"
        )

        val objectiveCoefficients = tetradModel.objective.objective
            .groupBy { cell -> indexToName[cell.colIndex1]!! to cell.colIndex2?.let { index -> indexToName[index]!! } }
            .mapValues { (_, cells) -> cells.fold(Flt64.zero) { acc, cell -> acc + cell.coefficient } }
        assertEquals(
            mapOf(
                key(xName, xName) to Flt64(2.0),
                key(xName, null) to Flt64(2.0),
                key(zName, null) to Flt64(-3.0)
            ),
            objectiveCoefficients,
            "${numberCase.name}: tetrad objective coefficient mismatch"
        )
    }

    private fun <V> assertQuadraticCoefficients(
        numberCase: GenericNumberCase<V>,
        constraint: Constraint<V, *>,
        expected: Map<Pair<String, String?>, Flt64>
    ) where V : RealNumber<V>, V : NumberField<V> {
        val actual = quadraticCellsToFlt64Map(numberCase, constraint.lhs.map { it as QuadraticCell<V> }, numberCase.zero)
        assertEquals(expected, actual, "${numberCase.name}: constraint '${constraint.name}' coefficient mismatch")
        assertTrue(
            constraint.lhs.all { (it as QuadraticCell<V>).coefficient::class == numberCase.zero::class },
            "${numberCase.name}: constraint '${constraint.name}' coefficient type should stay V"
        )
    }

    private fun <V> quadraticCellsToFlt64Map(
        numberCase: GenericNumberCase<V>,
        cells: List<QuadraticCell<V>>,
        zero: V
    ): Map<Pair<String, String?>, Flt64> where V : RealNumber<V>, V : NumberField<V> {
        val coefficients = LinkedHashMap<Pair<String, String?>, V>()
        for (cell in cells) {
            val key = cell.token1.variable.name to cell.token2?.variable?.name
            coefficients[key] = (coefficients[key] ?: zero) + cell.coefficient
        }
        return coefficients.mapValues { (_, value) -> toFlt64(numberCase, value) }
    }

    private fun quadraticRowCoefficients(
        tetradModel: QuadraticTetradModel,
        rowIndex: Int,
        indexToName: Map<Int, String>
    ): Map<Pair<String, String?>, Flt64> {
        val coefficients = LinkedHashMap<Pair<String, String?>, Flt64>()
        for (cell in tetradModel.constraints.lhs[rowIndex]) {
            val key = indexToName[cell.colIndex1]!! to cell.colIndex2?.let { indexToName[it]!! }
            coefficients[key] = (coefficients[key] ?: Flt64.zero) + cell.coefficient
        }
        return coefficients
    }

    private fun <V> value(
        numberCase: GenericNumberCase<V>,
        raw: Double
    ): V where V : RealNumber<V>, V : NumberField<V> = numberCase.converter.intoValue(Flt64(raw))

    private fun <V> toFlt64(
        numberCase: GenericNumberCase<V>,
        value: V
    ): Flt64 where V : RealNumber<V>, V : NumberField<V> = numberCase.converter.fromValue(value)

    private fun key(token1: String, token2: String?): Pair<String, String?> = token1 to token2
}
