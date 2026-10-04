package fuookami.ospf.kotlin.core.symbol.function

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.LinearCell
import fuookami.ospf.kotlin.core.solver.report.ModelElementIdentityRegistry
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.variable.*

class CompositeFunctionScenariosTest {
    @Test
    fun orderStatisticsPreserveTieSemanticsAndFiniteHelperBounds() {
        val x = boundedReal("order_x", -2.0, 4.0)
        val y = boundedReal("order_y", -1.0, 3.0)
        val z = boundedReal("order_z", 0.0, 5.0)
        val inputs = listOf(variable(x), variable(y), variable(z))
        val argMin = assertOk(ArgMinFunction(inputs, IntoValue.Identity, name = "scenario_argmin"))
        val argMax = assertOk(ArgMaxFunction(inputs, IntoValue.Identity, name = "scenario_argmax"))
        val kthLargest = assertOk(KthLargestFunction(inputs, 1, IntoValue.Identity, name = "scenario_kth"))
        val topK = assertOk(TopKSumFunction(inputs, 2, IntoValue.Identity, name = "scenario_top_k"))
        val kthFirst = assertOk(
            KthLargestFunction(listOf(variable(x), variable(z)), 0, IntoValue.Identity, name = "scenario_kth_first")
        )
        val kthLast = assertOk(
            KthLargestFunction(listOf(variable(x), variable(z)), 1, IntoValue.Identity, name = "scenario_kth_last")
        )
        val kthTie = assertOk(
            KthLargestFunction(listOf(variable(x), variable(y)), 1, IntoValue.Identity, name = "scenario_kth_tie")
        )
        val topKZero = assertOk(TopKSumFunction(inputs, 0, IntoValue.Identity, name = "scenario_top_k_zero"))
        val topKAll = assertOk(TopKSumFunction(inputs, inputs.size, IntoValue.Identity, name = "scenario_top_k_all"))

        val metaModel = LinearMetaModel<Flt64>(name = "order-statistics-model", converter = IntoValue.Identity)
        try {
            val zero = constant(0.0)
            assertTrue(metaModel.minimize(zero) is Ok)
            assertTrue(metaModel.add(listOf(x, y, z)) is Ok)
            val functions: List<MathFunctionSymbol<Flt64>> = listOf(
                argMin, argMax, kthLargest, topK, kthFirst, kthLast, kthTie, topKZero, topKAll
            )
            for (function in functions) {
                assertTrue(function.registerAuxiliaryTokens(metaModel.tokens) is Ok)
                assertFiniteBounds(function.helperVariables.filter { it !is BinVar })
            }
            val mechanism = buildMechanism(metaModel)
            val argMinRows = registerRows(mechanism) { argMin.registerConstraints(mechanism) }
            val argMaxRows = registerRows(mechanism) { argMax.registerConstraints(mechanism) }
            assertTrue(kthLargest.registerConstraints(mechanism) is Ok)
            assertTrue(topK.registerConstraints(mechanism) is Ok)
            val firstRows = registerRows(mechanism) { kthFirst.registerConstraints(mechanism) }
            val lastRows = registerRows(mechanism) { kthLast.registerConstraints(mechanism) }
            val tieRows = registerRows(mechanism) { kthTie.registerConstraints(mechanism) }
            assertTrue(mechanism.constraints.isNotEmpty())

            val tiedValues = mapOf<Symbol, Flt64>(x to Flt64(3.0), y to Flt64(3.0), z to Flt64.zero)
            assertEquals(2.0, argMin.evaluate(tiedValues)!!.toDouble(), 1e-10)
            assertEquals(0.0, argMax.evaluate(tiedValues)!!.toDouble(), 1e-10)
            assertEquals(3.0, kthLargest.evaluate(tiedValues)!!.toDouble(), 1e-10)
            assertEquals(6.0, topK.evaluate(tiedValues)!!.toDouble(), 1e-10)
            assertEquals(0.0, topKZero.evaluate(tiedValues)!!.toDouble(), 1e-10)
            assertEquals(6.0, topKAll.evaluate(tiedValues)!!.toDouble(), 1e-10)
            assertTrue(topKZero.helperVariables.isEmpty())
            assertTrue(topKAll.helperVariables.isEmpty())

            val argMinValues = LinkedHashMap(tiedValues)
            assignOneHotExtremum(
                argMinValues,
                argMin.helperVariables,
                resultName = "scenario_argmin_min",
                selectors = argMin.selectorVars,
                selectedIndex = 2,
                result = 0.0
            )
            val argMaxValues = LinkedHashMap(tiedValues)
            assignOneHotExtremum(
                argMaxValues,
                argMax.helperVariables,
                resultName = "scenario_argmax_max",
                selectors = argMax.selectorVars,
                selectedIndex = 0,
                result = 3.0
            )
            assertTrue(argMinRows.isNotEmpty())
            assertRowsSatisfied(argMinRows, variableKeyValues(argMinValues), "ArgMin")
            assertTrue(argMaxRows.isNotEmpty())
            assertTrue(argMaxRows.all { isSatisfied(it, variableKeyValues(argMaxValues)) })

            val alternativeArgMaxTie = LinkedHashMap(tiedValues)
            assignOneHotExtremum(
                alternativeArgMaxTie,
                argMax.helperVariables,
                resultName = "scenario_argmax_max",
                selectors = argMax.selectorVars,
                selectedIndex = 1,
                result = 3.0
            )
            assertEquals(1.0, argMax.resultPolynomial.evaluateWith(alternativeArgMaxTie)!!.toDouble(), 1e-10)
            assertTrue(argMaxRows.all { isSatisfied(it, variableKeyValues(alternativeArgMaxTie)) })

            val wrongArgMaxTie = LinkedHashMap(alternativeArgMaxTie)
            assignOneHotExtremum(
                wrongArgMaxTie,
                argMax.helperVariables,
                resultName = "scenario_argmax_max",
                selectors = argMax.selectorVars,
                selectedIndex = 2,
                result = 0.0
            )
            assertFalse(argMaxRows.all { isSatisfied(it, variableKeyValues(wrongArgMaxTie)) })

            val firstValues = LinkedHashMap(tiedValues)
            assignTwoInputSort(firstValues, kthFirst.helperVariables, kthFirst.name, 3.0, 0, 0.0, 1)
            val lastValues = LinkedHashMap(tiedValues)
            assignTwoInputSort(lastValues, kthLast.helperVariables, kthLast.name, 3.0, 0, 0.0, 1)
            val tieValues = LinkedHashMap(tiedValues)
            assignTwoInputSort(tieValues, kthTie.helperVariables, kthTie.name, 3.0, 1, 3.0, 1)

            assertTrue(firstRows.isNotEmpty() && firstRows.all { isSatisfied(it, variableKeyValues(firstValues)) })
            assertTrue(lastRows.isNotEmpty() && lastRows.all { isSatisfied(it, variableKeyValues(lastValues)) })
            assertTrue(tieRows.isNotEmpty() && tieRows.all { isSatisfied(it, variableKeyValues(tieValues)) })
            assertEquals(3.0, kthFirst.resultPolynomial.evaluateWith(firstValues)!!.toDouble(), 1e-10)
            assertEquals(0.0, kthLast.resultPolynomial.evaluateWith(lastValues)!!.toDouble(), 1e-10)
            assertEquals(3.0, kthTie.resultPolynomial.evaluateWith(tieValues)!!.toDouble(), 1e-10)

            val wrongFirst = LinkedHashMap(firstValues)
            setHelperValue(wrongFirst, kthFirst.helperVariables, "${kthFirst.name}_cmp_0_0_max_max", 2.0)
            assertFalse(firstRows.all { isSatisfied(it, variableKeyValues(wrongFirst)) })
        } finally {
            metaModel.close()
        }
    }

    @Test
    fun distanceCompositionsSatisfyTheirGeneratedRowsAndRejectPerturbations() {
        val x = boundedReal("distance_x", -5.0, 5.0)
        val y = boundedReal("distance_y", -5.0, 5.0)
        val z = boundedReal("distance_z", -5.0, 5.0)
        val left = listOf(variable(x), variable(y))
        val right = listOf(variable(y), variable(z))
        val l1 = assertOk(L1DistanceFunction(left, right, IntoValue.Identity, name = "scenario_l1"))
        val lInfinity = assertOk(LInfinityDistanceFunction(left, right, IntoValue.Identity, name = "scenario_linf"))
        val range = assertOk(RangeFunction(listOf(variable(x), variable(y), variable(z)), IntoValue.Identity, name = "scenario_range"))

        val metaModel = LinearMetaModel<Flt64>(name = "distance-composites-model", converter = IntoValue.Identity)
        try {
            assertTrue(metaModel.minimize(constant(0.0)) is Ok)
            assertTrue(metaModel.add(listOf(x, y, z)) is Ok)
            val functions: List<MathFunctionSymbol<Flt64>> = listOf(l1, lInfinity, range)
            for (function in functions) {
                assertTrue(function.registerAuxiliaryTokens(metaModel.tokens) is Ok)
            }
            val mechanism = buildMechanism(metaModel)
            val rowsByName = linkedMapOf<String, List<Constraint<Flt64, *>>>()
            for (function in functions) {
                rowsByName[function.name] = registerRows(mechanism) { function.registerConstraints(mechanism) }
            }

            val values = linkedMapOf<Symbol, Flt64>(
                x to Flt64(4.0),
                y to Flt64(-2.0),
                z to Flt64(3.0)
            )
            assignAbsoluteHelpers(values, l1.helperVariables, "scenario_l1_abs_0", 6.0)
            assignAbsoluteHelpers(values, l1.helperVariables, "scenario_l1_abs_1", -5.0)
            assignAbsoluteHelpers(values, lInfinity.helperVariables, "scenario_linf_abs_0", 6.0)
            assignAbsoluteHelpers(values, lInfinity.helperVariables, "scenario_linf_abs_1", -5.0)
            assignOneHotNamedExtremum(values, lInfinity.helperVariables, "scenario_linf_max_max", 0, 6.0)
            assignOneHotNamedExtremum(values, range.helperVariables, "scenario_range_max_max", 0, 4.0)
            assignOneHotNamedExtremum(values, range.helperVariables, "scenario_range_min_min", 1, -2.0)

            assertEquals(11.0, l1.resultPolynomial.evaluateWith(values)!!.toDouble(), 1e-10)
            assertEquals(6.0, lInfinity.resultPolynomial.evaluateWith(values)!!.toDouble(), 1e-10)
            assertEquals(6.0, range.resultPolynomial.evaluateWith(values)!!.toDouble(), 1e-10)
            for (function in functions) {
                val rows = rowsByName.getValue(function.name)
                assertTrue(rows.isNotEmpty(), "no generated rows for ${function.name}")
                assertRowsSatisfied(rows, variableKeyValues(values), function.name)
            }

            val wrongL1 = LinkedHashMap(values)
            setHelperValue(wrongL1, l1.helperVariables, "scenario_l1_abs_0_abs", 5.0)
            assertFalse(rowsByName.getValue(l1.name).all { isSatisfied(it, variableKeyValues(wrongL1)) })

            val wrongLInfinity = LinkedHashMap(values)
            setHelperValue(wrongLInfinity, lInfinity.helperVariables, "scenario_linf_max_max", 5.0)
            assertFalse(rowsByName.getValue(lInfinity.name).all { isSatisfied(it, variableKeyValues(wrongLInfinity)) })

            val wrongRange = LinkedHashMap(values)
            setHelperValue(wrongRange, range.helperVariables, "scenario_range_max_max", 3.0)
            assertFalse(rowsByName.getValue(range.name).all { isSatisfied(it, variableKeyValues(wrongRange)) })
        } finally {
            metaModel.close()
        }
    }

    @Test
    fun cardinalityFunctionsMatchEveryThreeBitAssignment() {
        val indicators = mutableListOf<AbstractVariableItem<*, *>>(
            BinVar("cardinality_x0"),
            BinVar("cardinality_x1"),
            BinVar("cardinality_x2")
        )
        val atMost = assertOk(AtMostFunction(indicators, 1, IntoValue.Identity, name = "scenario_at_most"))
        val exactly = assertOk(ExactlyFunction(indicators, 2, IntoValue.Identity, name = "scenario_exactly"))
        val atMostZero = assertOk(AtMostFunction(indicators, 0, IntoValue.Identity, name = "scenario_at_most_zero"))
        val atMostAll = assertOk(AtMostFunction(indicators, 3, IntoValue.Identity, name = "scenario_at_most_all"))
        val exactlyZero = assertOk(ExactlyFunction(indicators, 0, IntoValue.Identity, name = "scenario_exactly_zero"))
        val exactlyAll = assertOk(ExactlyFunction(indicators, 3, IntoValue.Identity, name = "scenario_exactly_all"))
        indicators.clear()

        val allFunctions = listOf(atMost, atMostZero, atMostAll)
        val exactFunctions = listOf(exactly, exactlyZero, exactlyAll)
        val inputs = (allFunctions.flatMap { it.indicators } + exactFunctions.flatMap { it.indicators }).distinct()
        val metaModel = LinearMetaModel<Flt64>(name = "cardinality-model", converter = IntoValue.Identity)
        try {
            assertTrue(metaModel.minimize(constant(0.0)) is Ok)
            assertTrue(metaModel.add(inputs) is Ok)
            for (function in allFunctions) {
                assertTrue(function.registerAuxiliaryTokens(metaModel.tokens) is Ok)
            }
            for (function in exactFunctions) {
                assertTrue(function.registerAuxiliaryTokens(metaModel.tokens) is Ok)
            }
            val mechanism = buildMechanism(metaModel)
            val rowsByName = linkedMapOf<String, List<Constraint<Flt64, *>>>()
            for (function in allFunctions + exactFunctions) {
                rowsByName[function.name] = registerRows(mechanism) { function.registerConstraints(mechanism) }
            }
            val atMostRows = rowsByName.getValue(atMost.name)
            val exactlyRows = rowsByName.getValue(exactly.name)
            assertEquals(2, atMostRows.size)
            assertEquals(5, exactlyRows.size)

            for (mask in 0 until 8) {
                val assignment = linkedMapOf<Symbol, Flt64>()
                var count = 0
                for ((index, indicator) in atMost.indicators.withIndex()) {
                    val value = if (mask and (1 shl index) != 0) Flt64.one else Flt64.zero
                    assignment[indicator] = value
                    if (value == Flt64.one) count++
                }
                val baseValues = variableKeyValues(assignment)
                val atMostValue = if (count <= 1) Flt64.one else Flt64.zero
                val atMostValues = LinkedHashMap(baseValues)
                atMostValues[atMost.resultVar.key] = atMostValue
                assertEquals(if (count <= 1) 1.0 else 0.0, atMost.evaluate(assignment)!!.toDouble(), 1e-10)
                assertEquals(if (count <= 1) setOf(1) else setOf(0), feasibleOutputs(atMostRows, atMostValues, atMost.resultVar.key))

                for ((function, expected) in listOf(
                    atMostZero to if (count == 0) 1 else 0,
                    atMostAll to 1
                )) {
                    val rows = rowsByName.getValue(function.name)
                    val outputs = feasibleOutputs(rows, baseValues, function.resultVar.key)
                    assertEquals(setOf(expected), outputs, function.name)
                    assertEquals(expected.toDouble(), function.evaluate(assignment)!!.toDouble(), 1e-10)
                }

                val exactlyValue = if (count == 2) Flt64.one else Flt64.zero
                val exactlyFeasibleOutputs = feasibleOutputsWithBinaryHelpers(
                    exactlyRows,
                    baseValues,
                    exactly.helperVariables,
                    exactly.resultVar.key
                )
                assertEquals(if (count == 2) setOf(1) else setOf(0), exactlyFeasibleOutputs)
                assertEquals(if (count == 2) 1.0 else 0.0, exactly.evaluate(assignment)!!.toDouble(), 1e-10)

                for ((function, expected) in listOf(
                    exactlyZero to if (count == 0) 1 else 0,
                    exactlyAll to if (count == 3) 1 else 0
                )) {
                    val rows = rowsByName.getValue(function.name)
                    val outputs = feasibleOutputsWithBinaryHelpers(
                        rows,
                        baseValues,
                        function.helperVariables,
                        function.resultVar.key
                    )
                    assertEquals(setOf(expected), outputs, function.name)
                    assertEquals(expected.toDouble(), function.evaluate(assignment)!!.toDouble(), 1e-10)
                }
            }
        } finally {
            metaModel.close()
        }
    }

    @Test
    fun cvarUsesNonUniformProbabilitiesAndEpigraphMinimizesTheUpperGraph() {
        val losses = listOf(0.0, 2.0, 10.0).map(::constant)
        val probabilities = mutableListOf(Flt64(0.25), Flt64(0.5), Flt64(0.25))
        val exact = assertOk(
            CvarFunction(losses, probabilities, Flt64(0.5), IntoValue.Identity, name = "scenario_cvar_exact")
        )
        val epigraph = assertOk(
            CvarEpigraphFunction(losses, probabilities, Flt64(0.5), IntoValue.Identity, name = "scenario_cvar_epigraph")
        )
        val alphaZero = assertOk(
            CvarFunction(losses, probabilities, Flt64.zero, IntoValue.Identity, name = "scenario_cvar_mean")
        )
        probabilities.clear()
        probabilities += Flt64.one

        assertEquals(6.0, exact.evaluate(emptyMap())!!.toDouble(), 1e-10)
        assertEquals(6.0, epigraph.evaluate(emptyMap())!!.toDouble(), 1e-10)
        assertEquals(3.5, alphaZero.evaluate(emptyMap())!!.toDouble(), 1e-10)

        val metaModel = LinearMetaModel<Flt64>(name = "cvar-epigraph-model", converter = IntoValue.Identity)
        try {
            assertTrue(exact.registerAuxiliaryTokens(metaModel.tokens) is Ok)
            assertTrue(epigraph.registerAuxiliaryTokens(metaModel.tokens) is Ok)
            assertTrue(metaModel.minimize(epigraph.resultPolynomial) is Ok)
            val mechanism = buildMechanism(metaModel)
            val exactRows = registerRows(mechanism) { exact.registerConstraints(mechanism) }
            assertTrue(epigraph.registerConstraints(mechanism) is Ok)
            val rows = mechanism.constraints.filter { it.name.startsWith("scenario_cvar_epigraph_excess_lb_") }
            assertTrue(exactRows.isNotEmpty())
            assertEquals(3, rows.size)

            val exactValues = linkedMapOf<Symbol, Flt64>()
            val differences = listOf(
                listOf(0.0, 2.0, 10.0),
                listOf(-2.0, 0.0, 8.0),
                listOf(-10.0, -8.0, 0.0)
            )
            val excesses = listOf(
                listOf(0.0, 2.0, 10.0),
                listOf(0.0, 0.0, 8.0),
                listOf(0.0, 0.0, 0.0)
            )
            for (thresholdIndex in differences.indices) {
                for (lossIndex in differences[thresholdIndex].indices) {
                    val prefix = "${exact.name}_excess_${thresholdIndex}_${lossIndex}_max"
                    setHelperValue(exactValues, exact.helperVariables, prefix, excesses[thresholdIndex][lossIndex])
                    val selectedDifference = if (differences[thresholdIndex][lossIndex] >= 0.0) 0 else 1
                    setHelperValue(exactValues, exact.helperVariables, "${prefix}_sel0", if (selectedDifference == 0) 1.0 else 0.0)
                    setHelperValue(exactValues, exact.helperVariables, "${prefix}_sel1", if (selectedDifference == 1) 1.0 else 0.0)
                }
            }
            setHelperValue(exactValues, exact.helperVariables, "${exact.name}_minimum_min", 6.0)
            for (candidateIndex in 0..2) {
                setHelperValue(
                    exactValues,
                    exact.helperVariables,
                    "${exact.name}_minimum_min_sel$candidateIndex",
                    if (candidateIndex == 1) 1.0 else 0.0
                )
            }
            assertRowsSatisfied(exactRows, variableKeyValues(exactValues), "CVaR exact")
            assertEquals(6.0, exact.resultPolynomial.evaluateWith(exactValues)!!.toDouble(), 1e-10)

            val perturbedExact = LinkedHashMap(exactValues)
            setHelperValue(perturbedExact, exact.helperVariables, "${exact.name}_minimum_min", 6.1)
            assertFalse(exactRows.all { isSatisfied(it, variableKeyValues(perturbedExact)) })

            val excessVariables = epigraph.helperVariables.filterIsInstance<URealVar>()
            val assignment = linkedMapOf<Symbol, Flt64>(epigraph.thresholdVar to Flt64(2.0))
            excessVariables.forEachIndexed { index, variable ->
                assignment[variable] = if (index == 2) Flt64(8.0) else Flt64.zero
            }
            assertTrue(rows.all { isSatisfied(it, variableKeyValues(assignment)) })
            assertEquals(6.0, epigraph.resultPolynomial.evaluateWith(assignment)!!.toDouble(), 1e-10)

            val inflatedAssignment = LinkedHashMap(assignment)
            inflatedAssignment[excessVariables.last()] = Flt64(9.0)
            assertTrue(rows.all { isSatisfied(it, variableKeyValues(inflatedAssignment)) })
            val inflatedValue = epigraph.resultPolynomial.evaluateWith(inflatedAssignment)!!.toDouble()
            assertEquals(6.5, inflatedValue, 1e-10)
            assertTrue(inflatedValue > exact.evaluate(emptyMap())!!.toDouble())

            val tooSmallExcess = LinkedHashMap(assignment)
            tooSmallExcess[excessVariables.last()] = Flt64(7.0)
            assertFalse(rows.all { isSatisfied(it, variableKeyValues(tooSmallExcess)) })
        } finally {
            metaModel.close()
        }
    }

    @Test
    fun expandedInputBoundsAreRejectedBeforeRowsAreWritten() {
        val inputVar = boundedReal("expanded_input", -1.0, 1.0)
        val function = assertOk(PositivePartFunction(variable(inputVar), IntoValue.Identity, name = "expanded_positive"))
        val metaModel = LinearMetaModel<Flt64>(name = "expanded-bounds-model", converter = IntoValue.Identity)
        try {
            assertTrue(metaModel.minimize(constant(0.0)) is Ok)
            assertTrue(metaModel.add(listOf(inputVar)) is Ok)
            assertTrue(function.registerAuxiliaryTokens(metaModel.tokens) is Ok)
            val mechanism = buildMechanism(metaModel)
            inputVar.range.set(ValueRange(Flt64(-2.0), Flt64(2.0)).value!!)
            val previousConstraintCount = mechanism.constraints.size

            assertTrue(function.registerConstraints(mechanism) is Failed)
            assertEquals(previousConstraintCount, mechanism.constraints.size)
        } finally {
            metaModel.close()
        }
    }

    @Test
    fun compositeConstraintRegistrationRollsBackPartialWrites() {
        val inputVar = boundedReal("atomic_clamp_input", -3.0, 4.0)
        val function = assertOk(
            ClampFunction(
                input = variable(inputVar),
                lower = Flt64(-1.0),
                upper = Flt64(2.0),
                converter = IntoValue.Identity,
                name = "atomic_clamp"
            )
        )
        val model = FailingLinearModel(failOnWrite = 2)

        assertTrue(function.registerConstraints(model) is Failed)
        assertEquals(2, model.writes)
        assertTrue(model.rows.isEmpty())
    }

    @Test
    fun orderStatisticSnapshotsCallerOwnedListsAndMonomialLists() {
        val inputVar = boundedReal("snapshot_input", 0.0, 5.0)
        val monomials = mutableListOf(LinearMonomial(Flt64.one, inputVar))
        val inputs = mutableListOf(
            LinearPolynomial(monomials, Flt64.zero),
            constant(3.0)
        )
        val function = assertOk(TopKSumFunction(inputs, 1, IntoValue.Identity, name = "snapshot_top_k"))
        inputs.clear()
        monomials.clear()

        assertEquals(4.0, function.evaluate(mapOf(inputVar to Flt64(4.0)))!!.toDouble(), 1e-10)
    }

    private fun boundedReal(name: String, lower: Double, upper: Double): RealVar = RealVar(name).also { variable ->
        variable.range.geq(Flt64(lower))
        variable.range.leq(Flt64(upper))
    }

    private fun variable(variable: RealVar): LinearPolynomial<Flt64> = LinearPolynomial(
        listOf(LinearMonomial(Flt64.one, variable)),
        Flt64.zero
    )

    private fun constant(value: Double): LinearPolynomial<Flt64> = LinearPolynomial(
        emptyList(),
        Flt64(value)
    )

    private fun <T> assertOk(result: Ret<T>): T {
        assertTrue(result is Ok)
        return result.value
    }

    private fun buildMechanism(metaModel: LinearMetaModel<Flt64>): LinearMechanismModel<Flt64> = runBlocking {
        when (val result = LinearMechanismModel.invoke(metaModel = metaModel, concurrent = false)) {
            is Ok -> result.value
            is Failed -> error(result.error.message)
            is Fatal -> error(result.errors.joinToString { it.message })
        }
    }

    private fun registerRows(
        model: LinearMechanismModel<Flt64>,
        register: () -> Try
    ): List<Constraint<Flt64, *>> {
        val firstRow = model.constraints.size
        val result = register()
        assertTrue(result is Ok, "constraint registration failed: $result")
        return model.constraints.drop(firstRow)
    }

    private fun assignOneHotExtremum(
        values: MutableMap<Symbol, Flt64>,
        helpers: List<AbstractVariableItem<*, *>>,
        resultName: String,
        selectors: List<AbstractVariableItem<*, *>>,
        selectedIndex: Int,
        result: Double
    ) {
        require(selectedIndex in selectors.indices)
        setHelperValue(values, helpers, resultName, result)
        selectors.forEachIndexed { index, selector ->
            values[selector] = if (index == selectedIndex) Flt64.one else Flt64.zero
        }
    }

    private fun assignOneHotNamedExtremum(
        values: MutableMap<Symbol, Flt64>,
        helpers: List<AbstractVariableItem<*, *>>,
        resultName: String,
        selectedIndex: Int,
        result: Double
    ) {
        val selectors = helpers
            .filter { it.name.startsWith("${resultName}_sel") }
            .sortedBy { it.name.substringAfterLast("sel").toInt() }
        assignOneHotExtremum(values, helpers, resultName, selectors, selectedIndex, result)
    }

    private fun assignTwoInputSort(
        values: MutableMap<Symbol, Flt64>,
        helpers: List<AbstractVariableItem<*, *>>,
        functionName: String,
        maximum: Double,
        maximumIndex: Int,
        minimum: Double,
        minimumIndex: Int
    ) {
        assignOneHotNamedExtremum(
            values,
            helpers,
            "${functionName}_cmp_0_0_max_max",
            maximumIndex,
            maximum
        )
        assignOneHotNamedExtremum(
            values,
            helpers,
            "${functionName}_cmp_0_0_min_min",
            minimumIndex,
            minimum
        )
    }

    private fun assignAbsoluteHelpers(
        values: MutableMap<Symbol, Flt64>,
        helpers: List<AbstractVariableItem<*, *>>,
        functionName: String,
        difference: Double
    ) {
        val positive = if (difference > 0.0) difference else 0.0
        val negative = if (difference < 0.0) -difference else 0.0
        setHelperValue(values, helpers, "${functionName}_abs", kotlin.math.abs(difference))
        setHelperValue(values, helpers, "${functionName}_abs_pos", positive)
        setHelperValue(values, helpers, "${functionName}_abs_neg", negative)
        setHelperValue(values, helpers, "${functionName}_abs_sign", if (difference >= 0.0) 1.0 else 0.0)
    }

    private fun setHelperValue(
        values: MutableMap<Symbol, Flt64>,
        helpers: List<AbstractVariableItem<*, *>>,
        variableName: String,
        value: Double
    ) {
        val variable = helpers.single { it.name == variableName }
        values[variable] = Flt64(value)
    }

    private fun assertFiniteBounds(variables: List<AbstractVariableItem<*, *>>) {
        for (variable in variables) {
            assertTrue(variable.lowerBound?.value?.unwrapOrNull() != null, "missing lower bound for ${variable.name}")
            assertTrue(variable.upperBound?.value?.unwrapOrNull() != null, "missing upper bound for ${variable.name}")
        }
    }

    private fun feasibleOutputs(
        rows: List<Constraint<Flt64, *>>,
        baseValues: Map<VariableItemKey, Flt64>,
        resultKey: VariableItemKey
    ): Set<Int> {
        val feasible = mutableSetOf<Int>()
        for (output in 0..1) {
            val values = LinkedHashMap(baseValues)
            values[resultKey] = if (output == 1) Flt64.one else Flt64.zero
            if (rows.all { isSatisfied(it, values) }) feasible += output
        }
        return feasible
    }

    private fun feasibleOutputsWithBinaryHelpers(
        rows: List<Constraint<Flt64, *>>,
        baseValues: Map<VariableItemKey, Flt64>,
        helperVariables: List<AbstractVariableItem<*, *>>,
        resultKey: VariableItemKey
    ): Set<Int> {
        val helpers = helperVariables.filter { it.key != resultKey }
        val feasible = mutableSetOf<Int>()
        for (output in 0..1) {
            for (helperMask in 0 until (1 shl helpers.size)) {
                val values = LinkedHashMap(baseValues)
                values[resultKey] = if (output == 1) Flt64.one else Flt64.zero
                for ((index, helper) in helpers.withIndex()) {
                    values[helper.key] = if (helperMask and (1 shl index) != 0) Flt64.one else Flt64.zero
                }
                if (rows.all { isSatisfied(it, values) }) feasible += output
            }
        }
        return feasible
    }

    private fun variableKeyValues(values: Map<Symbol, Flt64>): Map<VariableItemKey, Flt64> =
        values.entries.associate { (symbol, value) -> (symbol as AbstractVariableItem<*, *>).key to value }

    private fun isSatisfied(row: Constraint<Flt64, *>, values: Map<VariableItemKey, Flt64>): Boolean {
        var lhs = Flt64.zero
        for (cell in row.lhs.filterIsInstance<LinearCell<Flt64>>()) {
            lhs += cell.evaluate(values) ?: return false
        }
        return row.sign(lhs, row.rhs)
    }

    private fun assertRowsSatisfied(
        rows: List<Constraint<Flt64, *>>,
        values: Map<VariableItemKey, Flt64>,
        context: String
    ) {
        val failedRows = rows.filterNot { isSatisfied(it, values) }
        assertTrue(
            failedRows.isEmpty(),
            "$context failed rows:\n${failedRows.joinToString("\n") { describeRow(it, values) }}"
        )
    }

    private fun describeRow(row: Constraint<Flt64, *>, values: Map<VariableItemKey, Flt64>): String {
        val cells = row.lhs.filterIsInstance<LinearCell<Flt64>>()
        val missingKeys = cells.map { it.token.key }.distinct().filterNot { values.containsKey(it) }
        val lhs = cells.joinToString(" + ") { cell ->
            "${cell.coefficient}*${cell.token.name}[${cell.token.key}]=${values[cell.token.key] ?: "MISSING"}"
        }
        val evaluatedLhs = if (missingKeys.isEmpty()) {
            cells.mapNotNull { it.evaluate(values) }.fold(Flt64.zero) { total, value -> total + value }
        } else {
            null
        }
        return "row=${row.name}; lhs=$lhs; evaluatedLhs=$evaluatedLhs; relation=${row.sign}; rhs=${row.rhs}; missingKeys=$missingKeys"
    }

    private class FailingLinearModel(
        private val failOnWrite: Int
    ) : AbstractLinearMechanismModel<Flt64> {
        override var name: String = "atomic-composite-test-model"
        override val tokens: AbstractTokenTable<Flt64> = AutoTokenTable(Linear, false)
        override val identityRegistry: ModelElementIdentityRegistry? = null
        override val objectFunction: Object = SingleObject(
            category = ObjectCategory.Minimum,
            subObjects = emptyList<LinearSubObject<Flt64>>()
        )
        override val constraints: List<Constraint<Flt64, *>> get() = emptyList()
        val rows = mutableListOf<LinearInequality<Flt64>>()
        var writes: Int = 0
            private set

        override fun addConstraint(
            relation: LinearInequality<Flt64>,
            name: String?,
            from: Pair<IntermediateSymbol<out Flt64>, Boolean>?
        ): Try {
            writes++
            if (writes == failOnWrite) {
                return Failed(ErrorCode.ApplicationError, "Injected constraint write failure.")
            }
            rows += relation
            return ok
        }

        override fun rollbackConstraintsTo(size: Int): Try {
            if (size < 0 || size > rows.size) {
                return Failed(ErrorCode.IllegalArgument, "Invalid rollback position: $size")
            }
            rows.subList(size, rows.size).clear()
            return ok
        }
    }
}
