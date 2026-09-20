package fuookami.ospf.kotlin.core.intermediate_model

import kotlin.test.*
import kotlinx.coroutines.runBlocking
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.function.*
import fuookami.ospf.kotlin.core.variable.IntVar

class IndicatorEquivalentUsageTest {
    @Test
    fun differenceRetainsResultAndBothPublicConditions() = runBlocking {
        val input = IntVar("difference_usage_input")
        input.range.geq(Int64(-1))
        input.range.leq(Int64(2))
        val function = BalanceTernaryzationFunction(
            x = LinearPolynomial(listOf(LinearMonomial(Flt64.one, input)), Flt64.zero),
            epsilon = Flt64.zero,
            converter = IntoValue.Identity,
            strictBoundary = Flt64.one,
            name = "difference_usage"
        )
        val upperCondition = LinearPolynomial(listOf(LinearMonomial(Flt64.one, function.negativeVar)), Flt64.zero)
        val consumer = SigmoidFunction(
            condition = upperCondition,
            converter = IntoValue.Identity,
            strictBoundary = Flt64.one,
            name = "difference_consumer"
        )
        val meta = LinearMetaModel<Flt64>(
            name = "difference_usage",
            configuration = MetaModelConfiguration(concurrent = false, dumpBlocking = true, functionExpansionPolicy = FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST),
            converter = IntoValue.Identity
        )
        try {
            requireOk(meta.add(input))
            requireOk(meta.add(LinearFunctionSymbolAdapter(function, IntoValue.Identity)))
            requireOk(meta.add(LinearFunctionSymbolAdapter(consumer, IntoValue.Identity)))
            requireOk(meta.maximize(function.result))
            requireOk(meta.addConstraint(relation = LinearInequality(
                lhs = upperCondition,
                rhs = LinearPolynomial(emptyList(), Flt64.one),
                comparison = Comparison.LE,
                name = "public_upper_condition"
            ), name = "public_upper_condition"))
            requireOk(LinearMechanismModel.invoke(metaModel = meta, concurrent = false)).use { mechanism ->
                val keys = setOf(function.positiveVar.key, consumer.indicatorVar.key)
                for (fixed in listOf(function.resultVar, function.negativeVar)) {
                    assertTrue(LinearTriadModel.invokeResult(
                        model = mechanism,
                        fixedVariables = mapOf(fixed to Flt64.zero),
                        concurrent = false,
                        nativeFunctionKeys = keys
                    ) is Failed)
                }
                requireOk(LinearTriadModel.invokeResult(
                    model = mechanism,
                    dumpConstraintsToBounds = false,
                    concurrent = false,
                    nativeFunctionKeys = keys
                )).use { triad ->
                    val structure = triad.deferredFunctionStructures.filterIsInstance<IndicatorStructure<*>>().single { it.difference != null }
                    assertTrue(structure.usage.inObjective)
                    assertTrue(structure.usage.inConstraint)
                    assertTrue(structure.usage.nestedAsInput)
                    assertTrue(function.helperVariables.all { variable -> triad.variables.any { it.origin?.key == variable.key } })
                }
            }
        } finally {
            meta.close()
        }
    }

    @Test
    fun conjunctionRetainsResultAndBothPublicConditions() = runBlocking {
        val input = IntVar("conjunction_usage_input")
        input.range.geq(Int64(-1))
        input.range.leq(Int64(2))
        val function = IfInFunction(
            x = LinearPolynomial(listOf(LinearMonomial(Flt64.one, input)), Flt64.zero),
            lower = Flt64.zero,
            upper = Flt64.one,
            converter = IntoValue.Identity,
            strictBoundary = Flt64.one,
            name = "conjunction_usage"
        )
        val upperCondition = LinearPolynomial(listOf(LinearMonomial(Flt64.one, function.leVar)), Flt64.zero)
        val consumer = SigmoidFunction(
            condition = upperCondition,
            converter = IntoValue.Identity,
            strictBoundary = Flt64.one,
            name = "conjunction_consumer"
        )
        val meta = LinearMetaModel<Flt64>(
            name = "conjunction_usage",
            configuration = MetaModelConfiguration(concurrent = false, dumpBlocking = true, functionExpansionPolicy = FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST),
            converter = IntoValue.Identity
        )
        try {
            requireOk(meta.add(input))
            requireOk(meta.add(LinearFunctionSymbolAdapter(function, IntoValue.Identity)))
            requireOk(meta.add(LinearFunctionSymbolAdapter(consumer, IntoValue.Identity)))
            requireOk(meta.maximize(function.resultPolynomial))
            requireOk(meta.addConstraint(relation = LinearInequality(
                lhs = upperCondition,
                rhs = LinearPolynomial(emptyList(), Flt64.one),
                comparison = Comparison.LE,
                name = "public_upper_condition"
            ), name = "public_upper_condition"))
            requireOk(LinearMechanismModel.invoke(metaModel = meta, concurrent = false)).use { mechanism ->
                val keys = setOf(function.geVar.key, consumer.indicatorVar.key)
                for (fixed in listOf(function.resultVar, function.leVar)) {
                    assertTrue(LinearTriadModel.invokeResult(
                        model = mechanism,
                        fixedVariables = mapOf(fixed to Flt64.zero),
                        concurrent = false,
                        nativeFunctionKeys = keys
                    ) is Failed)
                }
                requireOk(LinearTriadModel.invokeResult(
                    model = mechanism,
                    dumpConstraintsToBounds = false,
                    concurrent = false,
                    nativeFunctionKeys = keys
                )).use { triad ->
                    val structure = triad.deferredFunctionStructures.filterIsInstance<IndicatorStructure<*>>().single { it.conjunction != null }
                    assertTrue(structure.usage.inObjective)
                    assertTrue(structure.usage.inConstraint)
                    assertTrue(structure.usage.nestedAsInput)
                    assertTrue(function.helperVariables.all { variable -> triad.variables.any { it.origin?.key == variable.key } })
                }
            }
        } finally {
            meta.close()
        }
    }

    @Test
    fun zeroBandSideParticipatesInUsageAndFixedSubstitutionChecks() = runBlocking {
        val input = IntVar("band_usage_input")
        input.range.geq(Int64(-1))
        input.range.leq(Int64.one)
        val function = InequalityFunction(
            lhs = LinearPolynomial(listOf(LinearMonomial(Flt64.one, input)), Flt64.zero),
            rhs = Flt64.zero,
            sign = Comparison.EQ,
            converter = IntoValue.Identity,
            tolerance = Flt64(0.125),
            strictBoundary = Flt64(0.5),
            name = "band_usage"
        )
        val side = function.helperVariables[1]
        val sidePolynomial = LinearPolynomial(listOf(LinearMonomial(Flt64.one, side)), Flt64.zero)
        val consumer = SigmoidFunction(
            condition = sidePolynomial,
            converter = IntoValue.Identity,
            strictBoundary = Flt64.one,
            name = "band_consumer"
        )
        val meta = LinearMetaModel<Flt64>(
            name = "band_usage",
            configuration = MetaModelConfiguration(concurrent = false, dumpBlocking = true, functionExpansionPolicy = FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST),
            converter = IntoValue.Identity
        )
        try {
            requireOk(meta.add(input))
            requireOk(meta.add(LinearFunctionSymbolAdapter(function, IntoValue.Identity)))
            requireOk(meta.add(LinearFunctionSymbolAdapter(consumer, IntoValue.Identity)))
            requireOk(meta.maximize(sidePolynomial))
            requireOk(meta.addConstraint(relation = LinearInequality(
                lhs = sidePolynomial,
                rhs = LinearPolynomial(emptyList(), Flt64.one),
                comparison = Comparison.LE,
                name = "band_side_reference"
            ), name = "band_side_reference"))
            requireOk(LinearMechanismModel.invoke(metaModel = meta, concurrent = false)).use { mechanism ->
                val keys = setOf(function.helperVariables[0].key, consumer.indicatorVar.key)
                assertTrue(LinearTriadModel.invokeResult(
                    model = mechanism,
                    fixedVariables = mapOf(side to Flt64.zero),
                    concurrent = false,
                    nativeFunctionKeys = keys
                ) is Failed)
                requireOk(LinearTriadModel.invokeResult(
                    model = mechanism,
                    dumpConstraintsToBounds = false,
                    concurrent = false,
                    nativeFunctionKeys = keys
                )).use { triad ->
                    val structure = triad.deferredFunctionStructures.filterIsInstance<IndicatorStructure<*>>().single { it.zeroBand != null }
                    assertTrue(structure.usage.inObjective)
                    assertTrue(structure.usage.inConstraint)
                    assertTrue(structure.usage.nestedAsInput)
                    assertTrue(triad.variables.any { it.origin?.key == side.key })
                }
            }
        } finally {
            meta.close()
        }
    }

    @Test
    fun implicationTracksConsequentInputAndPublicIndicator() = runBlocking {
        val input = IntVar("imply_usage_input")
        input.range.geq(Int64(-1))
        input.range.leq(Int64.one)
        val polynomial = LinearPolynomial(listOf(LinearMonomial(Flt64.one, input)), Flt64.zero)
        val producer = IfFunction(
            condition = polynomial,
            converter = IntoValue.Identity,
            strictBoundary = Flt64.one,
            name = "imply_producer"
        )
        val consumer = ImplyFunction(
            antecedent = polynomial,
            consequent = producer.resultPolynomial,
            converter = IntoValue.Identity,
            strictBoundary = Flt64.one,
            name = "imply_consumer"
        )
        val output = LinearPolynomial(listOf(LinearMonomial(Flt64.one, consumer.consequentIndicatorVar)), Flt64.zero)
        val meta = LinearMetaModel<Flt64>(
            name = "imply_usage",
            configuration = MetaModelConfiguration(concurrent = false, dumpBlocking = true, functionExpansionPolicy = FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST),
            converter = IntoValue.Identity
        )
        try {
            requireOk(meta.add(input))
            requireOk(meta.add(LinearFunctionSymbolAdapter(producer, IntoValue.Identity)))
            requireOk(meta.add(LinearFunctionSymbolAdapter(consumer, IntoValue.Identity)))
            requireOk(meta.maximize(output))
            requireOk(meta.addConstraint(relation = LinearInequality(
                lhs = output,
                rhs = LinearPolynomial(emptyList(), Flt64.zero),
                comparison = Comparison.GE,
                name = "imply_reference"
            ), name = "imply_reference"))
            requireOk(LinearMechanismModel.invoke(metaModel = meta, concurrent = false)).use { mechanism ->
                val keys = setOf(producer.resultVar.key, consumer.antecedentIndicatorVar.key)
                assertTrue(LinearTriadModel.invokeResult(
                    model = mechanism,
                    fixedVariables = mapOf(consumer.consequentIndicatorVar to Flt64.one),
                    concurrent = false,
                    nativeFunctionKeys = keys
                ) is Failed)
                requireOk(LinearTriadModel.invokeResult(
                    model = mechanism,
                    dumpConstraintsToBounds = false,
                    concurrent = false,
                    nativeFunctionKeys = keys
                )).use { triad ->
                    val structures = triad.deferredFunctionStructures.filterIsInstance<IndicatorStructure<*>>()
                    val structure = structures.single { it.resultVariable.key == consumer.antecedentIndicatorVar.key }
                    assertTrue(structure.usage.inObjective)
                    assertTrue(structure.usage.inConstraint)
                    assertTrue(structures.single { it.resultVariable.key == producer.resultVar.key }.usage.nestedAsInput)
                    assertTrue(triad.variables.any { it.origin?.key == consumer.consequentIndicatorVar.key })
                }
            }
        } finally {
            meta.close()
        }
    }

    @Test
    fun conditionalValuesTrackResultReferencesAndThenDependencies() = runBlocking {
        val input = IntVar("conditional_usage_input")
        input.range.geq(Int64(-1))
        input.range.leq(Int64.one)
        val polynomial = LinearPolynomial(listOf(LinearMonomial(Flt64.one, input)), Flt64.zero)
        val producer = IfFunction(
            condition = polynomial,
            converter = IntoValue.Identity,
            strictBoundary = Flt64.one,
            name = "value_producer"
        )
        val consumer = IfThenFunction(
            condition = polynomial,
            thenPoly = producer.resultPolynomial,
            converter = IntoValue.Identity,
            strictBoundary = Flt64.one,
            name = "value_consumer"
        )
        val meta = LinearMetaModel<Flt64>(
            name = "conditional_usage",
            configuration = MetaModelConfiguration(concurrent = false, dumpBlocking = true, functionExpansionPolicy = FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST),
            converter = IntoValue.Identity
        )
        try {
            requireOk(meta.add(input))
            requireOk(meta.add(LinearFunctionSymbolAdapter(producer, IntoValue.Identity)))
            requireOk(meta.add(LinearFunctionSymbolAdapter(consumer, IntoValue.Identity)))
            requireOk(meta.maximize(consumer.resultPolynomial))
            requireOk(meta.addConstraint(relation = LinearInequality(
                lhs = consumer.resultPolynomial,
                rhs = LinearPolynomial(emptyList(), Flt64.zero),
                comparison = Comparison.GE,
                name = "value_reference"
            ), name = "value_reference"))
            requireOk(LinearMechanismModel.invoke(metaModel = meta, concurrent = false)).use { mechanism ->
                requireOk(LinearTriadModel.invokeResult(
                    model = mechanism,
                    dumpConstraintsToBounds = false,
                    concurrent = false,
                    nativeFunctionKeys = setOf(producer.resultVar.key, consumer.indicatorVar.key)
                )).use { triad ->
                    val structures = triad.deferredFunctionStructures.filterIsInstance<IndicatorStructure<*>>()
                    val output = structures.single { it.resultVariable.key == consumer.indicatorVar.key }
                    assertTrue(output.usage.inObjective)
                    assertTrue(output.usage.inConstraint)
                    assertTrue(structures.single { it.resultVariable.key == producer.resultVar.key }.usage.nestedAsInput)
                    assertTrue(triad.variables.any { it.origin?.key == output.conditionalValue!!.resultVariable.key })
                }
            }
        } finally {
            meta.close()
        }
    }

    @Test
    fun equivalentColumnsParticipateInEveryUsageCategory() = runBlocking {
        for (fixed in listOf(false, true)) {
            val input = IntVar("equivalent_usage_input")
            input.range.geq(Int64(-1))
            input.range.leq(Int64.one)
            val function = IfFunction(
                condition = LinearPolynomial(listOf(LinearMonomial(Flt64.one, input)), Flt64.zero),
                converter = IntoValue.Identity,
                strictBoundary = Flt64.one,
                name = "equivalent_usage"
            )
            val alias = LinearPolynomial(listOf(LinearMonomial(Flt64.one, function.indicatorVar)), Flt64.zero)
            val consumer = SigmoidFunction(
                condition = alias,
                converter = IntoValue.Identity,
                strictBoundary = Flt64.one,
                name = "equivalent_consumer"
            )
            val meta = LinearMetaModel<Flt64>(
                name = "equivalent_usage",
                configuration = MetaModelConfiguration(concurrent = false, dumpBlocking = true, functionExpansionPolicy = FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST),
                converter = IntoValue.Identity
            )
            try {
                requireOk(meta.add(input))
                requireOk(meta.add(LinearFunctionSymbolAdapter(function, IntoValue.Identity)))
                requireOk(meta.add(LinearFunctionSymbolAdapter(consumer, IntoValue.Identity)))
                requireOk(meta.maximize(alias))
                requireOk(meta.addConstraint(relation = LinearInequality(
                    lhs = alias,
                    rhs = LinearPolynomial(emptyList(), Flt64.one),
                    comparison = Comparison.LE,
                    name = "alias_constraint"
                ), name = "alias_constraint"))
                requireOk(LinearMechanismModel.invoke(metaModel = meta, concurrent = false)).use { mechanism ->
                    val nativeKeys = setOf(function.resultVar.key, consumer.indicatorVar.key)
                    if (fixed) {
                        assertTrue(LinearTriadModel.invokeResult(
                            model = mechanism,
                            fixedVariables = mapOf(function.indicatorVar to Flt64.zero),
                            concurrent = false,
                            nativeFunctionKeys = nativeKeys
                        ) is Failed)
                    }
                    requireOk(LinearTriadModel.invokeResult(
                        model = mechanism,
                        fixedVariables = if (fixed) mapOf(function.indicatorVar to Flt64.zero) else null,
                        dumpConstraintsToBounds = false,
                        concurrent = false,
                        nativeFunctionKeys = if (fixed) emptySet() else nativeKeys
                    )).use { triad ->
                        val structure = triad.deferredFunctionStructures.filterIsInstance<IndicatorStructure<*>>()
                            .single { it.resultVariable.key == function.resultVar.key }
                        assertTrue(structure.usage.inConstraint)
                        assertTrue(structure.usage.nestedAsInput)
                        if (fixed) assertTrue(structure.usage.externallyReferenced)
                        else assertTrue(structure.usage.inObjective)
                        assertEquals(!fixed, triad.variables.any { it.origin?.key == function.indicatorVar.key })
                    }
                }
            } finally {
                meta.close()
            }
        }
    }

    private fun <T> requireOk(result: Ret<T>): T = when (result) {
        is Ok -> result.value
        is Failed -> fail(result.error.message)
        is Fatal -> fail(result.errors.joinToString { it.message })
    }
}
