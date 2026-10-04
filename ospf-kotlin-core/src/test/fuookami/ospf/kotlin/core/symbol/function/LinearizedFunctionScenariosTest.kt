package fuookami.ospf.kotlin.core.symbol.function

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.report.ModelElementIdentityRegistry
import fuookami.ospf.kotlin.core.symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.test.flt64TestConverter
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.variable.*

class LinearizedFunctionScenariosTest {
    @Test
    fun integerProductHandlesNegativeBoundsAndRejectsUnusedBinaryCodes() {
        val integer = IntVar("integer_product_x")
        integer.range.geq(Int64(-2))
        integer.range.leq(Int64(2))
        val factor = RealVar("integer_product_factor")
        factor.range.geq(Flt64(-3.0))
        factor.range.leq(Flt64(4.0))
        val function = IntegerProductFunction(
            integer = integer,
            input = variable(factor),
            converter = flt64TestConverter,
            name = "integer_product_test"
        )
        val model = CapturingLinearModel()

        assertTrue(function.registerConstraints(model) is Ok)
        assertEquals(-8.0, lower(function.resultVar), 1e-10)
        assertEquals(8.0, upper(function.resultVar), 1e-10)
        val bits = function.helperVariables.filterIsInstance<BinVar>()
        val products = function.helperVariables.filterIsInstance<RealVar>().filter { it !== function.resultVar }
        assertEquals(3, bits.size)
        assertEquals(3, products.size)

        val feasible = linkedMapOf<Symbol, Flt64>(
            integer to Flt64(1.0),
            factor to Flt64(-1.5),
            bits[0] to Flt64.one,
            bits[1] to Flt64.one,
            bits[2] to Flt64.zero,
            products[0] to Flt64(-1.5),
            products[1] to Flt64(-1.5),
            products[2] to Flt64.zero,
            function.resultVar to Flt64(-1.5)
        )
        assertRowsSatisfied(model.collectedConstraints, feasible)
        assertEquals(-1.5, function.evaluate(feasible)!!.toDouble(), 1e-10)

        val unusedBinaryCode = LinkedHashMap(feasible)
        unusedBinaryCode[integer] = Flt64(3.0)
        unusedBinaryCode[bits[0]] = Flt64.one
        unusedBinaryCode[bits[1]] = Flt64.zero
        unusedBinaryCode[bits[2]] = Flt64.one
        unusedBinaryCode[products[1]] = Flt64.zero
        unusedBinaryCode[function.resultVar] = Flt64(-4.5)
        val widthConstraint = model.collectedConstraints.single { it.name == "integer_product_test_integer_product_width" }
        assertTrue(!satisfies(widthConstraint, unusedBinaryCode))
        assertNull(function.evaluate(unusedBinaryCode))
    }

    @Test
    fun integerProductSupportsDegenerateIntegerDomain() {
        val integer = IntVar("integer_product_fixed_x")
        integer.range.geq(Int64(-3))
        integer.range.leq(Int64(-3))
        val factor = RealVar("integer_product_fixed_factor")
        factor.range.geq(Flt64(2.0))
        factor.range.leq(Flt64(4.0))
        val function = IntegerProductFunction(integer, variable(factor), flt64TestConverter, name = "integer_product_fixed")
        val model = CapturingLinearModel()

        assertTrue(function.registerConstraints(model) is Ok)
        assertEquals(0, function.helperVariables.filterIsInstance<BinVar>().size)
        val values = linkedMapOf<Symbol, Flt64>(
            integer to Flt64(-3.0),
            factor to Flt64(3.0),
            function.resultVar to Flt64(-9.0)
        )
        assertRowsSatisfied(model.collectedConstraints, values)
        assertEquals(-9.0, function.evaluate(values)!!.toDouble(), 1e-10)
        assertEquals(-12.0, lower(function.resultVar), 1e-10)
        assertEquals(-6.0, upper(function.resultVar), 1e-10)
    }

    @Test
    fun integerProductAllowsExplicitBoundsToTightenObservedRange() {
        val integer = IntVar("integer_product_tightened_x")
        integer.range.geq(Int64.zero)
        integer.range.leq(Int64.one)
        val factor = RealVar("integer_product_tightened_factor")
        factor.range.geq(Flt64.zero)
        factor.range.leq(Flt64(10.0))
        val function = IntegerProductFunction(
            integer = integer,
            input = variable(factor),
            converter = flt64TestConverter,
            inputBounds = ConditionBounds(Flt64(2.0), Flt64(4.0)),
            name = "integer_product_tightened"
        )
        val model = CapturingLinearModel()

        assertTrue(function.registerConstraints(model) is Ok)
        assertEquals(0.0, lower(function.resultVar), 1e-10)
        assertEquals(4.0, upper(function.resultVar), 1e-10)

        val bit = function.helperVariables.filterIsInstance<BinVar>().single()
        val masked = function.helperVariables.filterIsInstance<RealVar>().single { it !== function.resultVar }
        val feasible = linkedMapOf<Symbol, Flt64>(
            integer to Flt64.one,
            factor to Flt64(3.0),
            bit to Flt64.one,
            masked to Flt64(3.0),
            function.resultVar to Flt64(3.0)
        )
        assertRowsSatisfied(model.collectedConstraints, feasible)
        assertEquals(3.0, function.evaluate(feasible)!!.toDouble(), 1e-10)
        assertNull(function.evaluate(mapOf(integer to Flt64.one, factor to Flt64.one)))
    }

    @Test
    fun integerProductRejectsInvalidInputBeforeRegistration() {
        val nonInteger = RealVar("integer_product_invalid_integer")
        nonInteger.range.geq(Flt64(0.0))
        nonInteger.range.leq(Flt64(2.0))
        val factor = RealVar("integer_product_invalid_factor")
        factor.range.geq(Flt64(1.0))
        factor.range.leq(Flt64(2.0))
        val function = IntegerProductFunction(nonInteger, variable(factor), flt64TestConverter)
        val tokens = AutoTokenTable<Flt64>(Linear, false)
        val model = CapturingLinearModel()

        assertTrue(function.registerAuxiliaryTokens(tokens) is Failed)
        assertTrue(tokens.tokens.isEmpty())
        assertTrue(function.registerConstraints(model) is Failed)
        assertTrue(model.collectedConstraints.isEmpty())
    }

    @Test
    fun selectUsesBothBranchesAndRejectsNonBinaryEvaluation() {
        val mask = BinVar("select_mask")
        val x = RealVar("select_x")
        x.range.geq(Flt64(-2.0))
        x.range.leq(Flt64(3.0))
        val then = LinearPolynomial(listOf(LinearMonomial(Flt64(2.0), x)), Flt64.one)
        val otherwise = LinearPolynomial(listOf(LinearMonomial(Flt64(-1.0), x)), Flt64(2.0))
        val function = SelectFunction(mask, then, otherwise, flt64TestConverter, name = "select_test")
        val model = CapturingLinearModel()
        assertTrue(function.registerConstraints(model) is Ok)
        assertEquals(-3.0, lower(function.resultVar), 1e-10)
        assertEquals(7.0, upper(function.resultVar), 1e-10)

        val delta = function.helperVariables.filterIsInstance<RealVar>().single { it !== function.resultVar }
        val trueBranch = linkedMapOf<Symbol, Flt64>(
            mask to Flt64.one,
            x to Flt64(2.0),
            delta to Flt64(5.0),
            function.resultVar to Flt64(5.0)
        )
        val falseBranch = linkedMapOf<Symbol, Flt64>(
            mask to Flt64.zero,
            x to Flt64(2.0),
            delta to Flt64.zero,
            function.resultVar to Flt64.zero
        )
        assertRowsSatisfied(model.collectedConstraints, trueBranch)
        assertRowsSatisfied(model.collectedConstraints, falseBranch)
        assertEquals(5.0, function.evaluate(trueBranch)!!.toDouble(), 1e-10)
        assertEquals(0.0, function.evaluate(falseBranch)!!.toDouble(), 1e-10)
        assertNull(function.evaluate(mapOf(mask to Flt64(0.5), x to Flt64(2.0))))
    }

    @Test
    fun selectAllowsExplicitBoundsToTightenObservedRange() {
        val mask = BinVar("select_tightened_mask")
        val x = RealVar("select_tightened_x")
        x.range.geq(Flt64.zero)
        x.range.leq(Flt64(10.0))
        val function = SelectFunction(
            mask = mask,
            then = variable(x),
            otherwise = LinearPolynomial(emptyList(), Flt64(3.0)),
            converter = flt64TestConverter,
            thenBounds = ConditionBounds(Flt64(2.0), Flt64(4.0)),
            otherwiseBounds = ConditionBounds(Flt64(2.0), Flt64(4.0)),
            name = "select_tightened"
        )
        val model = CapturingLinearModel()

        assertTrue(function.registerConstraints(model) is Ok)
        val delta = function.helperVariables.filterIsInstance<RealVar>().single { it !== function.resultVar }
        val feasible = linkedMapOf<Symbol, Flt64>(
            mask to Flt64.one,
            x to Flt64(3.0),
            delta to Flt64.zero,
            function.resultVar to Flt64(3.0)
        )
        assertRowsSatisfied(model.collectedConstraints, feasible)
        assertEquals(3.0, function.evaluate(feasible)!!.toDouble(), 1e-10)
        assertNull(function.evaluate(mapOf(mask to Flt64.one, x to Flt64.one)))
    }

    @Test
    fun elementBindsOneHotIndexAndRejectsOutOfRangeLookup() {
        val index = IntVar("element_index")
        index.range.geq(Int64(-1))
        index.range.leq(Int64(2))
        val function = ElementFunction.fromConstants(
            index = index,
            values = listOf(Flt64(10.0), Flt64(20.0), Flt64(30.0)),
            converter = flt64TestConverter,
            lowerIndex = -1,
            name = "element_test"
        )
        val model = CapturingLinearModel()
        assertTrue(function.registerConstraints(model) is Ok)

        val selectors = function.helperVariables.filterIsInstance<BinVar>()
        val selectedValues = function.helperVariables.filterIsInstance<RealVar>().filter { it !== function.resultVar }
        val values = linkedMapOf<Symbol, Flt64>(
            index to Flt64.zero,
            selectors[0] to Flt64.zero,
            selectors[1] to Flt64.one,
            selectors[2] to Flt64.zero,
            selectedValues[0] to Flt64.zero,
            selectedValues[1] to Flt64(20.0),
            selectedValues[2] to Flt64.zero,
            function.resultVar to Flt64(20.0)
        )
        assertRowsSatisfied(model.collectedConstraints, values)
        assertEquals(20.0, function.evaluate(values)!!.toDouble(), 1e-10)
        assertEquals(10.0, function.evaluate(mapOf(index to Flt64(-1.0)))!!.toDouble(), 1e-10)
        assertNull(function.evaluate(mapOf(index to Flt64(2.0))))

        val outOfRangeValues = LinkedHashMap(values)
        outOfRangeValues[index] = Flt64(2.0)
        val indexConstraint = model.collectedConstraints.single { it.name == "element_test_element_index" }
        assertTrue(!satisfies(indexConstraint, outOfRangeValues))
    }

    @Test
    fun elementAllowsExplicitBoundsToTightenObservedRange() {
        val index = IntVar("element_tightened_index")
        index.range.geq(Int64.zero)
        index.range.leq(Int64.zero)
        val x = RealVar("element_tightened_x")
        x.range.geq(Flt64.zero)
        x.range.leq(Flt64(10.0))
        val function = ElementFunction(
            index = index,
            values = listOf(variable(x)),
            converter = flt64TestConverter,
            valueBounds = listOf(ConditionBounds(Flt64(2.0), Flt64(4.0))),
            name = "element_tightened"
        )
        val model = CapturingLinearModel()

        assertTrue(function.registerConstraints(model) is Ok)
        val selector = function.helperVariables.filterIsInstance<BinVar>().single()
        val masked = function.helperVariables.filterIsInstance<RealVar>().single { it !== function.resultVar }
        val feasible = linkedMapOf<Symbol, Flt64>(
            index to Flt64.zero,
            x to Flt64(3.0),
            selector to Flt64.one,
            masked to Flt64(3.0),
            function.resultVar to Flt64(3.0)
        )
        assertRowsSatisfied(model.collectedConstraints, feasible)
        assertEquals(3.0, function.evaluate(feasible)!!.toDouble(), 1e-10)
        assertNull(function.evaluate(mapOf(index to Flt64.zero, x to Flt64.one)))
    }

    @Test
    fun mccormickEnvelopeAllowsInteriorRelaxationDistinctFromProduct() {
        val x = RealVar("mccormick_x")
        val y = RealVar("mccormick_y")
        x.range.geq(Flt64(2.0))
        x.range.leq(Flt64(4.0))
        y.range.geq(Flt64(-3.0))
        y.range.leq(Flt64(5.0))
        val function = McCormickEnvelopeFunction(
            left = variable(x),
            right = variable(y),
            leftBounds = ConditionBounds(Flt64(2.0), Flt64(4.0)),
            rightBounds = ConditionBounds(Flt64(-3.0), Flt64(5.0)),
            converter = flt64TestConverter,
            name = "mccormick_test"
        )
        val model = CapturingLinearModel()
        assertTrue(function.registerConstraints(model) is Ok)
        assertEquals(-12.0, lower(function.resultVar), 1e-10)
        assertEquals(20.0, upper(function.resultVar), 1e-10)

        val relaxed = linkedMapOf<Symbol, Flt64>(x to Flt64(3.0), y to Flt64(1.0), function.resultVar to Flt64.zero)
        assertRowsSatisfied(model.collectedConstraints, relaxed)
        assertEquals(0.0, function.evaluate(relaxed)!!.toDouble(), 1e-10)
        assertEquals(3.0, function.evaluateProduct(relaxed)!!.toDouble(), 1e-10)

        val outsideEnvelope = LinkedHashMap(relaxed)
        outsideEnvelope[function.resultVar] = Flt64(8.0)
        assertTrue(model.collectedConstraints.any { !satisfies(it, outsideEnvelope) })
    }

    @Test
    fun complementarityExcludesTwoPositiveValues() {
        val x = RealVar("mutex_x")
        val y = RealVar("mutex_y")
        x.range.geq(Flt64.zero)
        x.range.leq(Flt64(10.0))
        y.range.geq(Flt64.zero)
        y.range.leq(Flt64(10.0))
        val function = ComplementarityFunction(
            x = variable(x),
            y = variable(y),
            converter = flt64TestConverter,
            xBounds = ConditionBounds(Flt64.zero, Flt64(4.0)),
            yBounds = ConditionBounds(Flt64.zero, Flt64(4.0)),
            name = "mutex_test"
        )
        val model = CapturingLinearModel()
        assertTrue(function.registerConstraints(model) is Ok)
        val selector = function.selector

        for (selectorValue in listOf(Flt64.zero, Flt64.one)) {
            val bothPositive = mapOf<Symbol, Flt64>(x to Flt64(4.0), y to Flt64(3.0), selector to selectorValue)
            assertTrue(model.collectedConstraints.any { !satisfies(it, bothPositive) })
        }
        val onePositive = mapOf<Symbol, Flt64>(x to Flt64(4.0), y to Flt64.zero, selector to Flt64.one)
        assertRowsSatisfied(model.collectedConstraints, onePositive)
        assertEquals(1.0, function.evaluate(mapOf(x to Flt64(4.0), y to Flt64(3.0)))!!.toDouble(), 1e-10)
        assertEquals(0.0, function.evaluate(mapOf(x to Flt64(4.0), y to Flt64.zero))!!.toDouble(), 1e-10)
        assertNull(function.evaluate(mapOf(x to Flt64(5.0), y to Flt64.zero)))
    }

    @Test
    fun explicitBoundsBecomeDomainRowsAndTightenObservedExpressions() {
        val x = RealVar("explicit_bound_x")
        val y = RealVar("explicit_bound_y")
        x.range.geq(Flt64.zero)
        x.range.leq(Flt64(10.0))
        y.range.geq(Flt64(-10.0))
        y.range.leq(Flt64(10.0))
        val function = McCormickEnvelopeFunction(
            left = variable(x),
            right = variable(y),
            leftBounds = ConditionBounds(Flt64(2.0), Flt64(4.0)),
            rightBounds = ConditionBounds(Flt64(-3.0), Flt64(5.0)),
            converter = flt64TestConverter,
            name = "explicit_bounds_test"
        )
        val model = CapturingLinearModel()
        assertTrue(function.registerConstraints(model) is Ok)
        assertTrue(model.collectedConstraints.any { it.name == "explicit_bounds_test_left_lower" })
        assertTrue(model.collectedConstraints.any { it.name == "explicit_bounds_test_left_upper" })
        assertTrue(model.collectedConstraints.any { it.name == "explicit_bounds_test_right_lower" })
        assertTrue(model.collectedConstraints.any { it.name == "explicit_bounds_test_right_upper" })
        assertEquals(
            0.0,
            function.evaluate(mapOf(x to Flt64(3.0), y to Flt64(1.0), function.resultVar to Flt64.zero))!!.toDouble(),
            1e-10
        )
        assertNull(function.evaluate(mapOf(x to Flt64.one, y to Flt64.one, function.resultVar to Flt64.one)))
    }

    private fun variable(variable: AbstractVariableItem<*, *>): LinearPolynomial<Flt64> {
        return LinearPolynomial(listOf(LinearMonomial(Flt64.one, variable)), Flt64.zero)
    }

    private fun lower(variable: AbstractVariableItem<*, *>): Double {
        return variable.lowerBound?.value?.unwrapOrNull()?.toDouble() ?: error("missing lower bound for ${variable.name}")
    }

    private fun upper(variable: AbstractVariableItem<*, *>): Double {
        return variable.upperBound?.value?.unwrapOrNull()?.toDouble() ?: error("missing upper bound for ${variable.name}")
    }

    private fun assertRowsSatisfied(rows: List<LinearInequality<Flt64>>, values: Map<Symbol, Flt64>) {
        rows.forEach { row -> assertTrue(satisfies(row, values), "constraint should hold: ${row.name}") }
    }

    private fun satisfies(row: LinearInequality<Flt64>, values: Map<Symbol, Flt64>): Boolean {
        val lhs = row.lhs.evaluateWith(values)?.toDouble() ?: return false
        val rhs = row.rhs.evaluateWith(values)?.toDouble() ?: return false
        return when (row.comparison) {
            Comparison.LT -> lhs < rhs - 1e-9
            Comparison.LE -> lhs <= rhs + 1e-9
            Comparison.EQ -> kotlin.math.abs(lhs - rhs) <= 1e-9
            Comparison.NE -> kotlin.math.abs(lhs - rhs) > 1e-9
            Comparison.GE -> lhs + 1e-9 >= rhs
            Comparison.GT -> lhs > rhs + 1e-9
        }
    }

    private class CapturingLinearModel : AbstractLinearMechanismModel<Flt64> {
        override var name: String = "linearized_function_test_model"
        override val tokens: AbstractTokenTable<Flt64> = AutoTokenTable(Linear, false)
        override val identityRegistry: ModelElementIdentityRegistry? = null
        override val objectFunction: Object = SingleObject(
            category = ObjectCategory.Minimum,
            subObjects = emptyList<LinearSubObject<Flt64>>()
        )
        override val constraints: List<Constraint<Flt64, *>> = emptyList()
        val collectedConstraints = mutableListOf<LinearInequality<Flt64>>()

        override fun addConstraint(
            relation: LinearInequality<Flt64>,
            name: String?,
            from: Pair<IntermediateSymbol<out Flt64>, Boolean>?
        ): Try {
            collectedConstraints.add(relation)
            return ok
        }

        override fun rollbackConstraintsTo(size: Int): Try {
            if (size < 0 || size > collectedConstraints.size) {
                return Failed(Err(ErrorCode.IllegalArgument, "Invalid rollback position: $size"))
            }
            collectedConstraints.subList(size, collectedConstraints.size).clear()
            return ok
        }
    }
}
