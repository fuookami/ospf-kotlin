package fuookami.ospf.kotlin.core.model.mechanism

import kotlin.test.*
import kotlinx.coroutines.runBlocking
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.function.*
import fuookami.ospf.kotlin.core.variable.RealVar

class MechanismFunctionFallbackTargetTest {
    @Test
    fun piecewiseMaterializerRecreatesEagerRowsWithStableTokens() = runBlocking {
        val input = RealVar("fallback_pwl_input")
        input.range.geq(Flt64.zero)
        input.range.leq(Flt64.two)
        val function = UnivariateLinearPiecewiseFunction(
            x = LinearPolynomial(listOf(LinearMonomial(Flt64.one, input)), Flt64.one),
            breakpoints = listOf(Flt64.one, Flt64.two, Flt64(3.0)),
            slopes = listOf(Flt64.one, Flt64.two),
            intercepts = listOf(Flt64.zero, -Flt64.two),
            converter = IntoValue.Identity,
            name = "fallback_pwl"
        )
        val metaModel = LinearMetaModel<Flt64>(name = "fallback-pwl", converter = IntoValue.Identity)
        try {
            assertTrue(metaModel.add(input) is Ok)
            assertTrue(metaModel.add(LinearFunctionSymbolAdapter(function, IntoValue.Identity)) is Ok)
            val result = LinearMechanismModel.invoke<Flt64>(
                metaModel = metaModel,
                concurrent = false,
                blocking = true
            )
            val mechanism = when (result) {
                is Ok -> result.value
                is Failed -> fail(result.error.message)
                is Fatal -> fail(result.errors.joinToString { it.message })
            }
            try {
                val originalRows = mechanism.linearConstraints.toList()
                val originalTokens = mechanism.tokens.tokens.toList()
                val target = MechanismFunctionFallbackTarget(mechanism)
                assertTrue(target.rollback(0) is Ok)
                val materialized = target.materialize(
                    structures = mechanism.deferredFunctionStructures,
                    materializer = PwlFallbackMaterializer
                )
                assertTrue(materialized is Ok)
                assertEquals(originalTokens, mechanism.tokens.tokens.toList())
                assertEquals(originalRows.size, mechanism.linearConstraints.size)
                for ((eager, fallback) in originalRows.zip(mechanism.linearConstraints)) {
                    assertEquals(eager.name, fallback.name)
                    assertEquals(eager.sign, fallback.sign)
                    assertEquals(eager.rhs, fallback.rhs)
                    assertEquals(eager.lhs.map { it.token.key to it.coefficient }, fallback.lhs.map { it.token.key to it.coefficient })
                }
                assertEquals(1, mechanism.deferredFunctionConstraintRegions.size)
            } finally {
                mechanism.close()
            }
        } finally {
            metaModel.close()
        }
    }

    @Test
    fun successfulTransactionBindsRegionsAndFailedTransactionPreservesThem() = runBlocking {
        val variable = RealVar("fallback_target_input")
        val firstStructure = object : DeferredFunctionStructure {}
        val secondStructure = object : DeferredFunctionStructure {}
        val thirdStructure = object : DeferredFunctionStructure {}
        val model = LinearMetaModel<Flt64>(
            name = "fallback-target",
            converter = IntoValue.Identity
        )
        try {
            assertTrue(model.add(variable) is Ok)
            val mechanismResult = LinearMechanismModel.invoke<Flt64>(
                metaModel = model,
                concurrent = false,
                blocking = true
            )
            val mechanism = when (mechanismResult) {
                is Ok -> mechanismResult.value
                is Failed -> fail(mechanismResult.error.message)
                is Fatal -> fail(mechanismResult.errors.joinToString { it.message })
            }
            try {
                val target = MechanismFunctionFallbackTarget(mechanism)
                val constraint = LinearInequality(
                    lhs = LinearPolynomial(listOf(LinearMonomial(Flt64.one, variable)), Flt64.zero),
                    rhs = LinearPolynomial(emptyList(), Flt64.one),
                    comparison = Comparison.LE,
                    name = "fallback_target_bound"
                )
                val first = target.materialize(listOf(firstStructure)) {
                    Ok(DeferredFunctionFallbackConstraints(listOf(constraint)))
                }
                assertTrue(first is Ok)
                assertEquals(1, target.constraintCount)
                val originalRegions = mechanism.deferredFunctionConstraintRegions
                assertEquals(1, originalRegions.size)
                assertSame(firstStructure, originalRegions.single().structure)
                assertEquals(0, originalRegions.single().firstConstraintIndex)
                assertEquals(1, originalRegions.single().constraintCount)

                val duplicate = target.materialize(listOf(firstStructure)) {
                    fail("An already bound structure must not be materialized twice")
                }
                assertTrue(duplicate is Failed)
                val failed = target.materialize(listOf(thirdStructure, secondStructure)) { structure ->
                    if (structure === secondStructure) {
                        Failed(ErrorCode.ApplicationError, "expected materialization failure")
                    } else {
                        Ok(DeferredFunctionFallbackConstraints(listOf(constraint)))
                    }
                }
                assertTrue(failed is Failed)
                assertEquals(1, target.constraintCount)
                assertEquals(originalRegions, mechanism.deferredFunctionConstraintRegions)
                assertTrue(target.rollback(0) is Ok)
                assertTrue(mechanism.deferredFunctionConstraintRegions.isEmpty())
            } finally {
                mechanism.close()
            }
        } finally {
            model.close()
        }
    }
}
