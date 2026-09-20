package fuookami.ospf.kotlin.core.solver

import kotlin.test.*
import kotlinx.coroutines.runBlocking
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.function.*
import fuookami.ospf.kotlin.core.variable.*

class NativeMaskingTest {
    @Test
    fun polynomialDefinitionPreservesItsEqualityWithoutDomainClipping() {
        val input = RealVar("defined_input")
        input.range.geq(Flt64(-2.0))
        input.range.leq(Flt64(3.0))
        val gate = RealVar("definition_input")
        gate.range.geq(Flt64(-10.0))
        gate.range.leq(Flt64(10.0))
        val function = MaskingWithPolyMaskFunction(
            input = LinearPolynomial(listOf(LinearMonomial(Flt64.one, input)), Flt64.zero),
            maskPoly = LinearPolynomial(listOf(LinearMonomial(Flt64(2.0), gate)), Flt64(-1.0)),
            converter = IntoValue.Identity,
            name = "defined_mask"
        )
        val structure = function.deferredStructure()
        val prepared = requireOk(prepareNativeMasking(structure))
        assertEquals(mapOf(gate.key to 2.0), prepared.definition!!.terms)
        assertEquals(-1.0, prepared.definition!!.constant)
        val rows = requireOk(CoreDeferredFunctionFallbackMaterializer.materialize(structure)).constraints
        assertEquals(5, rows.size)
        val definition = rows.single { it.name == "defined_mask_mask_eq" }
        assertEquals(Comparison.EQ, definition.comparison)
        assertEquals(Flt64.one, definition.lhs.constant)
        assertEquals(Flt64(-2.0), definition.lhs.monomials.single { it.symbol == gate }.coefficient)
        for (constant in listOf(Flt64.zero, Flt64.one, Flt64(0.5))) {
            assertTrue(prepareNativeMasking(structure.copy(maskDefinition = LinearPolynomial(emptyList(), constant))) is Ok)
        }
        assertTrue(prepareNativeMasking(structure.copy(maskDefinition = LinearPolynomial(emptyList(), Flt64.nan))) is Failed)
        for (self in listOf(function.maskVar, function.resultVar)) {
            assertTrue(prepareNativeMasking(structure.copy(maskDefinition = LinearPolynomial(listOf(LinearMonomial(Flt64.one, self)), Flt64.zero))) is Failed)
        }
    }

    @Test
    fun definedMaskReferencesAndDefinitionDependenciesAreTracked() = runBlocking {
        val input = RealVar("definition_usage_input")
        input.range.geq(Flt64(-1.0))
        input.range.leq(Flt64.one)
        val polynomial = LinearPolynomial(listOf(LinearMonomial(Flt64.one, input)), Flt64.zero)
        val producer = BinaryzationFunction(
            polynomial = polynomial,
            converter = IntoValue.Identity,
            tolerance = Flt64(0.125),
            name = "definition_producer"
        )
        val function = MaskingWithPolyMaskFunction(
            input = LinearPolynomial(emptyList(), Flt64(2.0)),
            maskPoly = LinearPolynomial(listOf(LinearMonomial(Flt64(-1.0), producer.helperVariables.single())), Flt64.one),
            converter = IntoValue.Identity,
            name = "definition_consumer"
        )
        val maskPolynomial = LinearPolynomial(listOf(LinearMonomial(Flt64.one, function.maskVar)), Flt64.zero)
        val meta = LinearMetaModel<Flt64>(
            name = "definition_usage",
            configuration = MetaModelConfiguration(concurrent = false, dumpBlocking = true, functionExpansionPolicy = FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST),
            converter = IntoValue.Identity
        )
        try {
            requireOk(meta.add(input))
            requireOk(meta.add(LinearFunctionSymbolAdapter(producer, IntoValue.Identity)))
            requireOk(meta.add(LinearFunctionSymbolAdapter(function, IntoValue.Identity)))
            requireOk(meta.maximize(maskPolynomial))
            requireOk(meta.addConstraint(relation = LinearInequality(
                lhs = maskPolynomial,
                rhs = LinearPolynomial(emptyList(), Flt64.one),
                comparison = Comparison.LE,
                name = "defined_mask_reference"
            ), name = "defined_mask_reference"))
            requireOk(LinearMechanismModel.invoke(metaModel = meta, concurrent = false)).use { mechanism ->
                val keys = setOf(producer.helperVariables.single().key, function.resultVar.key)
                assertTrue(LinearTriadModel.invokeResult(
                    model = mechanism,
                    fixedVariables = mapOf(function.maskVar to Flt64.zero),
                    concurrent = false,
                    nativeFunctionKeys = keys
                ) is Failed)
                requireOk(LinearTriadModel.invokeResult(
                    model = mechanism,
                    dumpConstraintsToBounds = false,
                    concurrent = false,
                    nativeFunctionKeys = keys
                )).use { triad ->
                    val structure = triad.deferredFunctionStructures.filterIsInstance<MaskingStructure<*>>().single()
                    assertTrue(structure.usage.inObjective)
                    assertTrue(structure.usage.inConstraint)
                    assertTrue(triad.deferredFunctionStructures.filterIsInstance<IndicatorStructure<*>>().single().usage.nestedAsInput)
                    assertTrue(triad.variables.any { it.origin?.key == function.maskVar.key })
                }
            }
        } finally {
            meta.close()
        }
    }

    @Test
    fun boundsAndBinaryMaskAreRequired() {
        val input = RealVar("input")
        input.range.geq(Flt64(-2.0))
        input.range.leq(Flt64(3.0))
        val function = MaskingFunction(
            input = LinearPolynomial(listOf(LinearMonomial(Flt64(-2.0), input)), Flt64.one),
            mask = BinVar("mask"),
            converter = IntoValue.Identity,
            name = "masking"
        )
        val structure = assertNotNull(function.deferredStructure())
        val data = requireOk(prepareNativeMasking(structure))
        assertEquals(-5.0, data.value.lowerBound)
        assertEquals(5.0, data.value.upperBound)
        assertEquals(4, requireOk(CoreDeferredFunctionFallbackMaterializer.materialize(structure)).constraints.size)
        for (bounds in listOf(ConditionBounds(Flt64(-4.0), Flt64(5.0)), ConditionBounds(Flt64(-5.0), Flt64(4.0)))) {
            assertTrue(prepareNativeMasking(structure.copy(value = structure.value.copy(bounds = bounds))) is Failed)
        }
        assertTrue(prepareNativeMasking(structure.copy(mask = input)) is Failed)
        assertTrue(prepareNativeMasking(structure.copy(value = structure.value.copy(resultVariable = structure.mask))) is Failed)
        assertTrue(prepareNativeMasking(structure.copy(value = structure.value.copy(input =
            LinearPolynomial(listOf(LinearMonomial(Flt64.one, function.resultVar)), Flt64.zero)
        ))) is Failed)
        input.range.set(requireOk(ValueRange(
            lb = Flt64(-2.0),
            ub = Flt64(4.0),
            lbInterval = Interval.Closed,
            ubInterval = Interval.Closed,
            constants = Flt64
        )))
        assertTrue(prepareNativeMasking(structure) is Failed)
    }

    @Test
    fun maskDependencyOutputUsageAndFixedColumnsAreRetained() = runBlocking {
        val input = RealVar("usage_input")
        input.range.geq(Flt64(-1.0))
        input.range.leq(Flt64.one)
        val polynomial = LinearPolynomial(listOf(LinearMonomial(Flt64.one, input)), Flt64.zero)
        val producer = BinaryzationFunction(
            polynomial = polynomial,
            converter = IntoValue.Identity,
            tolerance = Flt64(0.125),
            name = "mask_producer"
        )
        val mask = producer.helperVariables.single()
        val function = MaskingFunction(
            input = polynomial,
            mask = mask,
            converter = IntoValue.Identity,
            name = "mask_consumer"
        )
        val meta = LinearMetaModel<Flt64>(
            name = "mask_usage",
            configuration = MetaModelConfiguration(concurrent = false, dumpBlocking = true, functionExpansionPolicy = FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST),
            converter = IntoValue.Identity
        )
        try {
            requireOk(meta.add(input))
            requireOk(meta.add(LinearFunctionSymbolAdapter(producer, IntoValue.Identity)))
            requireOk(meta.add(LinearFunctionSymbolAdapter(function, IntoValue.Identity)))
            requireOk(meta.maximize(function.resultPolynomial))
            requireOk(meta.addConstraint(relation = LinearInequality(
                lhs = function.resultPolynomial,
                rhs = LinearPolynomial(emptyList(), Flt64.one),
                comparison = Comparison.LE,
                name = "output_reference"
            ), name = "output_reference"))
            requireOk(LinearMechanismModel.invoke(metaModel = meta, concurrent = false)).use { mechanism ->
                val keys = setOf(mask.key, function.resultVar.key)
                for (fixed in listOf(mask, function.resultVar)) {
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
                    val structure = triad.deferredFunctionStructures.filterIsInstance<MaskingStructure<*>>().single()
                    assertTrue(structure.usage.inObjective)
                    assertTrue(structure.usage.inConstraint)
                    assertTrue(triad.deferredFunctionStructures.filterIsInstance<IndicatorStructure<*>>().single().usage.nestedAsInput)
                    assertTrue(triad.variables.any { it.origin?.key == mask.key })
                    assertTrue(triad.variables.any { it.origin?.key == function.resultVar.key })
                }
            }
        } finally {
            meta.close()
        }
    }

    private fun <T> requireOk(result: Ret<T>): T = when (result) {
        is Ok -> result.value
        is Failed -> fail(result.error.message)
        is Fatal -> fail(result.errors.joinToString { it.message })
    }
}
