package fuookami.ospf.kotlin.core.model.mechanism

import kotlin.test.*
import kotlinx.coroutines.runBlocking
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.intermediate.FunctionExpansionPolicy
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.function.*
import fuookami.ospf.kotlin.core.variable.*

class MaxNativeFunctionPreparationTest {
    @Test
    fun onlyExclusiveUnrestrictedSelectorsCanBeOmitted() = runBlocking {
        scenario().use { scenario ->
            val selected = nativeFunctionSelectorKeys(
                model = scenario.mechanism,
                nativeFunctionKeys = setOf(scenario.function.resultVar.key),
                fixedVariables = null
            )
            assertTrue(selected is Ok)
            assertEquals(scenario.function.selectorVars.map { it.key }.toSet(), selected.value)
            assertTrue(nativeFunctionSelectorKeys(
                model = scenario.mechanism,
                nativeFunctionKeys = setOf(scenario.function.resultVar.key),
                fixedVariables = mapOf(scenario.function.selectorVars.first() to Flt64.one)
            ) is Failed)
            (scenario.function.selectorVars.first() as BinVar).range.setTrue()
            assertTrue(scenario.selection() is Failed)
        }
    }

    @Test
    fun ordinaryAndCrossTypeReferencesPreserveSelectors() = runBlocking {
        scenario(ordinaryReference = true).use { assertTrue(it.selection() is Failed) }
        scenario(crossReference = true).use { assertTrue(it.selection() is Failed) }
    }

    @Test
    fun insufficientExplicitMRetainsFallback() = runBlocking {
        scenario(bigM = Flt64.one).use { assertTrue(it.selection() is Failed) }
    }

    private suspend fun scenario(
        ordinaryReference: Boolean = false,
        crossReference: Boolean = false,
        bigM: Flt64 = Flt64(4.0)
    ): Scenario {
        val input = RealVar("owned_max_input")
        input.range.geq(Flt64(-2.0))
        input.range.leq(Flt64(2.0))
        val function = MaxFunction(
            polynomials = listOf(
                LinearPolynomial(listOf(LinearMonomial(Flt64.one, input)), Flt64.zero),
                LinearPolynomial(emptyList(), Flt64.zero)
            ),
            bigM = bigM,
            converter = IntoValue.Identity,
            name = "owned_max"
        )
        val meta = LinearMetaModel<Flt64>(
            name = "owned-max",
            configuration = MetaModelConfiguration(
                concurrent = false,
                dumpBlocking = true,
                functionExpansionPolicy = FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST
            ),
            converter = IntoValue.Identity
        )
        assertTrue(meta.add(input) is Ok)
        assertTrue(meta.add(LinearFunctionSymbolAdapter(function, IntoValue.Identity)) is Ok)
        val helper = LinearPolynomial(
            listOf(LinearMonomial(Flt64.one, function.selectorVars.first())), Flt64.zero
        )
        if (ordinaryReference) {
            val relation = LinearInequality(helper, LinearPolynomial(emptyList(), Flt64.one), Comparison.LE)
            assertTrue(meta.addConstraint(relation = relation, name = "helper_reference") is Ok)
        }
        if (crossReference) {
            val absolute = AbsFunction(
                polynomial = helper,
                converter = IntoValue.Identity,
                bigM = Flt64(4.0),
                name = "selector_abs"
            )
            assertTrue(meta.add(LinearFunctionSymbolAdapter(absolute, IntoValue.Identity)) is Ok)
        }
        assertTrue(meta.minimize(function.resultPolynomial) is Ok)
        val mechanism = LinearMechanismModel.invoke<Flt64>(
            metaModel = meta,
            concurrent = false,
            blocking = true
        )
        assertTrue(mechanism is Ok)
        return Scenario(meta, mechanism.value, function)
    }

    private data class Scenario(
        val meta: LinearMetaModel<Flt64>,
        val mechanism: LinearMechanismModel<Flt64>,
        val function: MaxFunction<Flt64>
    ) : AutoCloseable {
        fun selection() = nativeFunctionSelectorKeys(
            model = mechanism,
            nativeFunctionKeys = setOf(function.resultVar.key),
            fixedVariables = null
        )

        override fun close() {
            mechanism.close()
            meta.close()
        }
    }
}
