package fuookami.ospf.kotlin.core.intermediate_model

import kotlin.test.*
import kotlinx.coroutines.runBlocking
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.LinearFlattenData
import fuookami.ospf.kotlin.core.testing.*
import fuookami.ospf.kotlin.core.variable.*

class GenericLinearMetaModelBuildTest {
    @Test
    fun fourNumberTypesShouldBuildLinearMetaModelAndDumpMechanismModel() {
        buildLinear(GenericNumberCases.flt64)
        buildLinear(GenericNumberCases.rtn64)
        buildLinear(GenericNumberCases.fltX)
        buildLinear(GenericNumberCases.rtnX)
    }

    private fun <V> buildLinear(numberCase: GenericNumberCase<V>)
            where V : RealNumber<V>, V : NumberField<V> {
        val x = RealVar("${numberCase.name.lowercase()}_linear_x")
        val y = RealVar("${numberCase.name.lowercase()}_linear_y")
        val z = RealVar("${numberCase.name.lowercase()}_linear_z")

        val model = LinearMetaModel<V>(
            name = "generic-linear-${numberCase.name.lowercase()}",
            objectCategory = ObjectCategory.Minimum,
            converter = numberCase.converter
        )

        try {
            assertTrue(model.add(listOf(x, y, z)) is Ok, "${numberCase.name}: add variables should succeed")

            val xUpperBound = LinearInequality(
                lhs = LinearPolynomial(
                    monomials = listOf(LinearMonomial(numberCase.one, x)),
                    constant = numberCase.zero
                ),
                rhs = LinearPolynomial(emptyList(), value(numberCase, 9.0)),
                comparison = Comparison.LE
            )
            assertTrue(
                model.addConstraint(relation = xUpperBound, name = "bound_x_upper") is Ok,
                "${numberCase.name}: add x upper bound should succeed"
            )

            val yLowerBound = LinearInequality(
                lhs = LinearPolynomial(
                    monomials = listOf(LinearMonomial(numberCase.one, y)),
                    constant = numberCase.zero
                ),
                rhs = LinearPolynomial(emptyList(), value(numberCase, -2.0)),
                comparison = Comparison.GE
            )
            assertTrue(
                model.addConstraint(relation = yLowerBound, name = "bound_y_lower") is Ok,
                "${numberCase.name}: add y lower bound should succeed"
            )

            val leLhs = LinearPolynomial(
                monomials = listOf(
                    LinearMonomial(value(numberCase, 2.0), x),
                    LinearMonomial(value(numberCase, -3.0), y),
                    LinearMonomial(numberCase.one, z)
                ),
                constant = value(numberCase, 4.0)
            )
            val leRelation = LinearInequality(lhs = leLhs, rhs = LinearPolynomial(emptyList(), numberCase.zero), comparison = Comparison.LE)
            assertTrue(
                model.addConstraint(relation = leRelation, name = "c_le") is Ok,
                "${numberCase.name}: add le constraint should succeed"
            )

            val geLhs = LinearPolynomial(
                monomials = listOf(
                    LinearMonomial(value(numberCase, -1.0), x),
                    LinearMonomial(value(numberCase, 2.0), y),
                    LinearMonomial(value(numberCase, -1.0), z)
                ),
                constant = numberCase.one
            )
            val geRelation = LinearInequality(lhs = geLhs, rhs = LinearPolynomial(emptyList(), numberCase.zero), comparison = Comparison.GE)
            assertTrue(
                model.addConstraint(relation = geRelation, name = "c_ge") is Ok,
                "${numberCase.name}: add ge constraint should succeed"
            )

            val eqLhs = LinearPolynomial(
                monomials = listOf(
                    LinearMonomial(numberCase.one, x),
                    LinearMonomial(numberCase.one, y),
                    LinearMonomial(numberCase.one, z)
                ),
                constant = value(numberCase, -5.0)
            )
            val eqRelation = LinearInequality(lhs = eqLhs, rhs = LinearPolynomial(emptyList(), numberCase.zero), comparison = Comparison.EQ)
            assertTrue(
                model.addConstraint(relation = eqRelation, name = "c_eq") is Ok,
                "${numberCase.name}: add eq constraint should succeed"
            )

            val objective = LinearPolynomial(
                monomials = listOf(
                    LinearMonomial(value(numberCase, 3.0), x),
                    LinearMonomial(value(numberCase, -2.0), y),
                    LinearMonomial(value(numberCase, 4.0), z)
                ),
                constant = value(numberCase, 7.0)
            )
            assertTrue(
                model.addObject(
                    category = ObjectCategory.Minimum,
                    flattenData = LinearFlattenData(
                        monomials = objective.monomials,
                        constant = objective.constant
                    ),
                    name = "obj_linear"
                ) is Ok,
                "${numberCase.name}: add objective should succeed"
            )
            val mechanismResult = runBlocking {
                LinearMechanismModel.invoke<V>(metaModel = model, concurrent = false)
            }
            assertTrue(mechanismResult is Ok, "${numberCase.name}: dump mechanism model should succeed")
            assertMechanismModel(numberCase, mechanismResult.value)
            assertTriadModel(numberCase, mechanismResult.value, mapOf(y to Flt64.two))
        } finally {
            model.close()
        }
    }

    private fun <V> assertMechanismModel(
        numberCase: GenericNumberCase<V>,
        mechanismModel: LinearMechanismModel<V>
    ) where V : RealNumber<V>, V : NumberField<V> {
        assertEquals(5, mechanismModel.constraints.size, "${numberCase.name}: mechanism constraint count mismatch")
        assertEquals(ObjectCategory.Minimum, mechanismModel.objectFunction.category)
        assertEquals(1, mechanismModel.objectFunction.subObjects.size, "${numberCase.name}: objective sub-object amount mismatch")

        val constraintsByName = mechanismModel.constraints.associateBy { it.name }
        val boundUpper = assertNotNull(constraintsByName["bound_x_upper"], "${numberCase.name}: missing bound_x_upper")
        val boundLower = assertNotNull(constraintsByName["bound_y_lower"], "${numberCase.name}: missing bound_y_lower")
        val cLe = assertNotNull(constraintsByName["c_le"], "${numberCase.name}: missing c_le")
        val cGe = assertNotNull(constraintsByName["c_ge"], "${numberCase.name}: missing c_ge")
        val cEq = assertNotNull(constraintsByName["c_eq"], "${numberCase.name}: missing c_eq")

        assertEquals(ConstraintRelation.LessEqual, boundUpper.sign, "${numberCase.name}: bound_x_upper sign mismatch")
        assertEquals(Flt64(9.0), toFlt64(numberCase, boundUpper.rhs), "${numberCase.name}: bound_x_upper rhs mismatch")
        assertLinearCoefficients(numberCase, boundUpper, mapOf("${numberCase.name.lowercase()}_linear_x" to Flt64.one))

        assertEquals(ConstraintRelation.GreaterEqual, boundLower.sign, "${numberCase.name}: bound_y_lower sign mismatch")
        assertEquals(Flt64(-2.0), toFlt64(numberCase, boundLower.rhs), "${numberCase.name}: bound_y_lower rhs mismatch")
        assertLinearCoefficients(numberCase, boundLower, mapOf("${numberCase.name.lowercase()}_linear_y" to Flt64.one))

        assertEquals(ConstraintRelation.LessEqual, cLe.sign, "${numberCase.name}: c_le sign mismatch")
        assertEquals(Flt64(-4.0), toFlt64(numberCase, cLe.rhs), "${numberCase.name}: c_le rhs mismatch")
        assertLinearCoefficients(
            numberCase,
            cLe,
            mapOf(
                "${numberCase.name.lowercase()}_linear_x" to Flt64(2.0),
                "${numberCase.name.lowercase()}_linear_y" to Flt64(-3.0),
                "${numberCase.name.lowercase()}_linear_z" to Flt64.one
            )
        )

        assertEquals(ConstraintRelation.GreaterEqual, cGe.sign, "${numberCase.name}: c_ge sign mismatch")
        assertEquals(Flt64(-1.0), toFlt64(numberCase, cGe.rhs), "${numberCase.name}: c_ge rhs mismatch")
        assertLinearCoefficients(
            numberCase,
            cGe,
            mapOf(
                "${numberCase.name.lowercase()}_linear_x" to Flt64(-1.0),
                "${numberCase.name.lowercase()}_linear_y" to Flt64(2.0),
                "${numberCase.name.lowercase()}_linear_z" to Flt64(-1.0)
            )
        )

        assertEquals(ConstraintRelation.Equal, cEq.sign, "${numberCase.name}: c_eq sign mismatch")
        assertEquals(Flt64(5.0), toFlt64(numberCase, cEq.rhs), "${numberCase.name}: c_eq rhs mismatch")
        assertLinearCoefficients(
            numberCase,
            cEq,
            mapOf(
                "${numberCase.name.lowercase()}_linear_x" to Flt64.one,
                "${numberCase.name.lowercase()}_linear_y" to Flt64.one,
                "${numberCase.name.lowercase()}_linear_z" to Flt64.one
            )
        )

        val subObject = mechanismModel.objectFunction.subObjects.first()
        assertEquals(ObjectCategory.Minimum, subObject.category, "${numberCase.name}: objective category mismatch")
        assertEquals(Flt64(7.0), toFlt64(numberCase, subObject.constant), "${numberCase.name}: objective constant mismatch")
        val objectiveCoefficients = linearCellsToFlt64Map(numberCase, subObject.cells, numberCase.zero)
        assertEquals(
            mapOf(
                "${numberCase.name.lowercase()}_linear_x" to Flt64(3.0),
                "${numberCase.name.lowercase()}_linear_y" to Flt64(-2.0),
                "${numberCase.name.lowercase()}_linear_z" to Flt64(4.0)
            ),
            objectiveCoefficients,
            "${numberCase.name}: objective coefficient mismatch"
        )
        assertTrue(
            subObject.cells.all { it.coefficient::class == numberCase.zero::class },
            "${numberCase.name}: objective coefficient type should stay V"
        )
    }

    private fun <V> assertTriadModel(
        numberCase: GenericNumberCase<V>,
        mechanismModel: LinearMechanismModel<V>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>
    ) where V : RealNumber<V>, V : NumberField<V> {
        val flt64ModelResult = convertMechanismModelToFlt64(mechanismModel)
        assertTrue(flt64ModelResult is Ok, "${numberCase.name}: mechanism -> Flt64 conversion should succeed")
        val flt64Model = flt64ModelResult.value as? LinearMechanismModel<Flt64>
        assertNotNull(flt64Model, "${numberCase.name}: converted model should be linear mechanism model")
        val triadModel = runBlocking {
            LinearTriadModel.invoke(
                model = flt64Model,
                fixedVariables = fixedVariables,
                concurrent = false
            )
        }

        assertEquals(2, triadModel.variables.size, "${numberCase.name}: triad variable count mismatch")
        val variableByName = triadModel.variables.associateBy { it.name }
        val xName = "${numberCase.name.lowercase()}_linear_x"
        val yName = "${numberCase.name.lowercase()}_linear_y"
        val zName = "${numberCase.name.lowercase()}_linear_z"
        assertTrue(variableByName.containsKey(xName), "${numberCase.name}: x should remain in solver")
        assertTrue(variableByName.containsKey(zName), "${numberCase.name}: z should remain in solver")
        assertTrue(!variableByName.containsKey(yName), "${numberCase.name}: fixed y should be removed from solver")

        val xVar = assertNotNull(variableByName[xName], "${numberCase.name}: missing x variable")
        assertEquals(Flt64(9.0), xVar.upperBound, "${numberCase.name}: x upper bound should be extracted from bound constraint")
        val zVar = assertNotNull(variableByName[zName], "${numberCase.name}: missing z variable")
        val zToken = flt64Model.tokens.tokens.firstOrNull { it.variable.name == zName }
        assertNotNull(zToken, "${numberCase.name}: missing z token in mechanism model")
        assertEquals(zToken.upperBound!!.value.unwrap(), zVar.upperBound, "${numberCase.name}: z upper bound should keep token default")
        assertEquals(zToken.lowerBound!!.value.unwrap(), zVar.lowerBound, "${numberCase.name}: z lower bound should keep token default")

        assertEquals(3, triadModel.constraints.size, "${numberCase.name}: bound constraints should be removed from triad constraints")
        val rowIndexByName = triadModel.constraints.names.withIndex().associate { it.value to it.index }
        assertEquals(setOf("c_le", "c_ge", "c_eq"), rowIndexByName.keys, "${numberCase.name}: triad constraint names mismatch")

        val indexToName = triadModel.variables.associate { it.index to it.name }

        val leRow = assertNotNull(rowIndexByName["c_le"], "${numberCase.name}: missing c_le row")
        assertEquals(ConstraintRelation.LessEqual, triadModel.constraints.signs[leRow], "${numberCase.name}: triad c_le sign mismatch")
        assertEquals(Flt64(2.0), triadModel.constraints.rhs[leRow], "${numberCase.name}: triad c_le rhs mismatch")
        assertEquals(
            mapOf(xName to Flt64(2.0), zName to Flt64.one),
            linearRowCoefficients(triadModel, leRow, indexToName),
            "${numberCase.name}: triad c_le coefficients mismatch"
        )

        val geRow = assertNotNull(rowIndexByName["c_ge"], "${numberCase.name}: missing c_ge row")
        assertEquals(ConstraintRelation.GreaterEqual, triadModel.constraints.signs[geRow], "${numberCase.name}: triad c_ge sign mismatch")
        assertEquals(Flt64(-5.0), triadModel.constraints.rhs[geRow], "${numberCase.name}: triad c_ge rhs mismatch")
        assertEquals(
            mapOf(xName to Flt64(-1.0), zName to Flt64(-1.0)),
            linearRowCoefficients(triadModel, geRow, indexToName),
            "${numberCase.name}: triad c_ge coefficients mismatch"
        )

        val eqRow = assertNotNull(rowIndexByName["c_eq"], "${numberCase.name}: missing c_eq row")
        assertEquals(ConstraintRelation.Equal, triadModel.constraints.signs[eqRow], "${numberCase.name}: triad c_eq sign mismatch")
        assertEquals(Flt64(3.0), triadModel.constraints.rhs[eqRow], "${numberCase.name}: triad c_eq rhs mismatch")
        assertEquals(
            mapOf(xName to Flt64.one, zName to Flt64.one),
            linearRowCoefficients(triadModel, eqRow, indexToName),
            "${numberCase.name}: triad c_eq coefficients mismatch"
        )

        val objectiveCoefficients = triadModel.objective.objective.associate { cell ->
            indexToName[cell.colIndex]!! to cell.coefficient
        }
        assertEquals(
            mapOf(xName to Flt64(3.0), zName to Flt64(4.0)),
            objectiveCoefficients,
            "${numberCase.name}: triad objective coefficient mismatch"
        )
        assertEquals(Flt64(3.0), triadModel.objective.constant, "${numberCase.name}: triad objective constant mismatch")
    }

    private fun <V> assertLinearCoefficients(
        numberCase: GenericNumberCase<V>,
        constraint: Constraint<V, *>,
        expected: Map<String, Flt64>
    ) where V : RealNumber<V>, V : NumberField<V> {
        val actual = linearCellsToFlt64Map(numberCase, constraint.lhs.map { it as LinearCell<V> }, numberCase.zero)
        assertEquals(expected, actual, "${numberCase.name}: constraint '${constraint.name}' coefficient mismatch")
        assertTrue(
            constraint.lhs.all { (it as LinearCell<V>).coefficient::class == numberCase.zero::class },
            "${numberCase.name}: constraint '${constraint.name}' coefficient type should stay V"
        )
    }

    private fun <V> linearCellsToFlt64Map(
        numberCase: GenericNumberCase<V>,
        cells: List<LinearCell<V>>,
        zero: V
    ): Map<String, Flt64> where V : RealNumber<V>, V : NumberField<V> {
        val coefficients = LinkedHashMap<String, V>()
        for (cell in cells) {
            val name = cell.token.variable.name
            coefficients[name] = (coefficients[name] ?: zero) + cell.coefficient
        }
        return coefficients.mapValues { (_, value) -> toFlt64(numberCase, value) }
    }

    private fun linearRowCoefficients(
        triadModel: LinearTriadModel,
        rowIndex: Int,
        indexToName: Map<Int, String>
    ): Map<String, Flt64> {
        val coefficients = LinkedHashMap<String, Flt64>()
        for (cell in triadModel.constraints.lhs[rowIndex]) {
            val name = indexToName[cell.colIndex]!!
            coefficients[name] = (coefficients[name] ?: Flt64.zero) + cell.coefficient
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
}
