package fuookami.ospf.kotlin.core.symbol.function

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.model.mechanism.Constraint
import fuookami.ospf.kotlin.core.model.mechanism.LinearSubObject
import fuookami.ospf.kotlin.core.model.mechanism.Object
import fuookami.ospf.kotlin.core.model.mechanism.SingleObject
import fuookami.ospf.kotlin.core.solver.report.ModelElementIdentityRegistry
import fuookami.ospf.kotlin.core.symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.test.flt64TestConverter
import fuookami.ospf.kotlin.core.token.AbstractTokenTable
import fuookami.ospf.kotlin.core.token.AutoTokenTable
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.core.variable.RealVar

class TariffFunctionTest {
    @Test
    fun incrementalTariffRejectsNegativeMarginalRates() {
        val quantity = RealVar("negative_rate_quantity")
        val result = IncrementalTariff.create(
            x = variable(quantity),
            breakpoints = listOf(Flt64.zero, Flt64.one),
            marginalRates = listOf(Flt64(-1.0)),
            converter = flt64TestConverter
        )

        assertIs<Failed<*, *, *>>(result)
    }

    @Test
    fun incrementalTariffAccumulatesEachMarginalTierContinuously() {
        val quantity = RealVar("incremental_quantity")
        val breakpoints = mutableListOf(Flt64.zero, Flt64(10.0), Flt64(20.0))
        val rates = mutableListOf(Flt64(2.0), Flt64(3.0))
        val function = assertIs<Ok<UnivariateLinearPiecewiseFunction<Flt64>, *, *>>(
            IncrementalTariff.create(
                x = variable(quantity),
                breakpoints = breakpoints,
                marginalRates = rates,
                converter = flt64TestConverter,
                baseCost = Flt64(5.0)
            )
        ).value
        breakpoints.clear()
        rates.clear()

        assertEquals(5.0, function.evaluate(mapOf(quantity to Flt64.zero))!!.toDouble(), 1e-10)
        assertEquals(25.0, function.evaluate(mapOf(quantity to Flt64(10.0)))!!.toDouble(), 1e-10)
        assertEquals(55.0, function.evaluate(mapOf(quantity to Flt64(20.0)))!!.toDouble(), 1e-10)
    }

    @Test
    fun allUnitsDiscountRelaxesUnselectedTierAcrossTheWholeQuantityDomain() {
        val quantity = RealVar("discount_quantity")
        val breakpoints = mutableListOf(Flt64.zero, Flt64(10.0), Flt64(20.0))
        val rates = mutableListOf(Flt64(10.0), Flt64(8.0))
        val function = assertIs<Ok<AllUnitsDiscountFunction<Flt64>, *, *>>(
            AllUnitsDiscountFunction.create(
                x = variable(quantity),
                breakpoints = breakpoints,
                rates = rates,
                boundaryGap = Flt64(0.5),
                converter = flt64TestConverter,
                name = "discount_test"
            )
        ).value
        breakpoints.clear()
        rates.clear()
        val model = CapturingLinearModel()

        assertTrue(function.registerConstraints(model) is Ok)
        val masks = function.helperVariables.filterIsInstance<RealVar>().filter { it !== function.resultVar }
        assertEquals(2, masks.size)
        val values = linkedMapOf<Symbol, Flt64>(
            quantity to Flt64(20.0),
            function.selectorVars[0] to Flt64.zero,
            function.selectorVars[1] to Flt64.one,
            masks[0] to Flt64.zero,
            masks[1] to Flt64(160.0),
            function.resultVar to Flt64(160.0)
        )

        assertRowsSatisfied(model.collectedConstraints, values)
        assertEquals(160.0, function.evaluate(mapOf(quantity to Flt64(20.0)))!!.toDouble(), 1e-10)
    }

    @Test
    fun fixedChargeDefaultsMinimumActivityToZeroForResultBounds() {
        val activation = BinVar("fixed_charge_activation")
        val activity = RealVar("fixed_charge_activity")
        activity.range.geq(Flt64.zero)
        activity.range.leq(Flt64(100.0))
        val function = assertIs<Ok<FixedChargeFunction<Flt64>, *, *>>(
            FixedChargeFunction.create(
                activation = activation,
                fixedCost = Flt64(25.0),
                converter = flt64TestConverter,
                activity = variable(activity),
                unitCost = Flt64(2.0),
                maximumActivity = Flt64(100.0),
                name = "fixed_charge_test"
            )
        ).value
        val model = CapturingLinearModel()

        assertEquals(0.0, lower(function.resultVar), 1e-10)
        assertEquals(225.0, upper(function.resultVar), 1e-10)
        assertTrue(function.registerConstraints(model) is Ok)
        val values = mapOf<Symbol, Flt64>(
            activation to Flt64.one,
            activity to Flt64(100.0),
            function.resultVar to Flt64(225.0)
        )
        assertRowsSatisfied(model.collectedConstraints, values)
    }

    @Test
    fun nonlinearApproximationSnapshotsMutableBreakpointsAndPolynomialTerms() {
        val input = RealVar("exp_snapshot_input")
        input.range.geq(Flt64.zero)
        input.range.leq(Flt64.one)
        val monomials = mutableListOf(LinearMonomial(Flt64.one, input))
        val x = LinearPolynomial(monomials, Flt64.zero)
        val breakpoints = mutableListOf(Flt64.zero, Flt64.one)
        val function = assertIs<Ok<ExpFunction<Flt64>, *, *>>(
            ExpFunction.create(x, breakpoints, flt64TestConverter, name = "exp_snapshot_test")
        ).value

        breakpoints.clear()
        monomials.clear()

        assertEquals(2, function.breakpoints.size)
        assertEquals(1.8591409142295225, function.evaluate(mapOf(input to Flt64(0.5)))!!.toDouble(), 1e-8)
        assertEquals(kotlin.math.exp(1.0), function.originalValue(Flt64.one).value!!.toDouble(), 1e-10)

        val model = CapturingLinearModel()
        assertTrue(function.registerConstraints(model) is Ok)
        val result = function.helperVariables.filterIsInstance<RealVar>().single()
        val selector = function.helperVariables.filterIsInstance<BinVar>().single()
        val feasible = mapOf<Symbol, Flt64>(
            input to Flt64(0.5),
            result to Flt64(1.8591409142295225),
            selector to Flt64.one
        )
        assertRowsSatisfied(model.collectedConstraints, feasible)
    }

    @Test
    fun nonlinearFactoriesRejectInvalidDomains() {
        val input = RealVar("invalid_nonlinear_input")
        val x = variable(input)

        assertIs<Failed<*, *, *>>(
            LogFunction.create(
                x = x,
                breakpoints = listOf(Flt64.zero, Flt64.one),
                converter = flt64TestConverter
            )
        )
        assertIs<Failed<*, *, *>>(
            ReciprocalFunction.create(
                x = x,
                breakpoints = listOf(Flt64(-1.0), Flt64.one),
                converter = flt64TestConverter
            )
        )
    }

    private fun variable(variable: AbstractVariableItem<*, *>): LinearPolynomial<Flt64> {
        return LinearPolynomial(listOf(LinearMonomial(Flt64.one, variable)), Flt64.zero)
    }

    private fun lower(variable: AbstractVariableItem<*, *>): Double {
        return variable.lowerBound?.value?.unwrapOrNull()?.toDouble() ?: error("Missing lower bound: ${variable.name}")
    }

    private fun upper(variable: AbstractVariableItem<*, *>): Double {
        return variable.upperBound?.value?.unwrapOrNull()?.toDouble() ?: error("Missing upper bound: ${variable.name}")
    }

    private fun assertRowsSatisfied(rows: List<LinearInequality<Flt64>>, values: Map<Symbol, Flt64>) {
        rows.forEach { row -> assertTrue(satisfies(row, values), "constraint should hold: ${row.name}") }
    }

    private fun satisfies(row: LinearInequality<Flt64>, values: Map<Symbol, Flt64>): Boolean {
        val lhs = row.lhs.evaluateWith(values)?.toDouble() ?: return false
        val rhs = row.rhs.evaluateWith(values)?.toDouble() ?: return false
        return when (row.comparison) {
            Comparison.LE -> lhs <= rhs + 1e-9
            Comparison.GE -> lhs + 1e-9 >= rhs
            Comparison.EQ -> kotlin.math.abs(lhs - rhs) <= 1e-9
            Comparison.LT -> lhs < rhs - 1e-9
            Comparison.NE -> kotlin.math.abs(lhs - rhs) > 1e-9
            Comparison.GT -> lhs > rhs + 1e-9
        }
    }

    private class CapturingLinearModel : AbstractLinearMechanismModel<Flt64> {
        override var name: String = "tariff_function_test_model"
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
                return Failed(ErrorCode.IllegalArgument, "Invalid rollback position: $size")
            }
            collectedConstraints.subList(size, collectedConstraints.size).clear()
            return ok
        }
    }
}
