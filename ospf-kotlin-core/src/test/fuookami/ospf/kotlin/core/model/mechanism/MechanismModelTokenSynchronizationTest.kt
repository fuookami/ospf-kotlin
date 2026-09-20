package fuookami.ospf.kotlin.core.model.mechanism

import kotlin.test.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.model.intermediate.LinearTriadModel
import fuookami.ospf.kotlin.core.model.intermediate.UnivariateLinearPiecewiseStructure
import fuookami.ospf.kotlin.core.symbol.function.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.token.AddableTokenCollection
import fuookami.ospf.kotlin.core.token.boundTokenTableContext
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 验证机制模型展开后的 helper token 同步与结果回写。
 * Verify helper-token synchronization and solution write-back after mechanism-model unfolding.
 */
class MechanismModelTokenSynchronizationTest {

    @Test
    fun unfoldSynchronizesIfThenHelpersForMetaModelSolutionWriteback() = runBlocking {
        val converter = IntoValue.Identity
        val ordinary = RealVar("mechanism_sync_ordinary")
        val function = IfThenFunction(
            condition = constant(Flt64.one),
            thenPoly = constant(Flt64(3.0)),
            converter = converter,
            strictBoundary = Flt64(0.1),
            name = "mechanism_sync_if_then"
        )
        val resultVariable = function.helperVariables.filterIsInstance<RealVar>().single()
        val symbol = LinearFunctionSymbolAdapter(function, converter)
        val metaModel = LinearMetaModel<Flt64>(
            name = "mechanism-token-synchronization",
            converter = converter
        )

        try {
            assertTrue(metaModel.add(ordinary) is Ok)
            assertTrue(metaModel.add(symbol) is Ok)
            val ordinaryIndex = metaModel.tokens.find(ordinary)!!.solverIndex
            assertFalse(metaModel.tokens.tokens.any { it.key == resultVariable.key })

            val firstMechanismResult = LinearMechanismModel.invoke<Flt64>(
                metaModel = metaModel,
                concurrent = false,
                blocking = true
            )
            val firstMechanism = when (firstMechanismResult) {
                is Ok -> firstMechanismResult.value
                is Failed -> fail(firstMechanismResult.error.message)
                is Fatal -> fail(firstMechanismResult.errors.joinToString { it.message })
            }

            try {
                assertEquals(ordinaryIndex, metaModel.tokens.find(ordinary)?.solverIndex)
                assertEquals(
                    firstMechanism.tokens.tokens.size,
                    metaModel.tokens.tokens.size,
                    "MetaModel token table should contain the complete unfolded solver token set"
                )
                for (helper in function.helperVariables) {
                    assertNotNull(metaModel.tokens.find(helper), "Missing synchronized helper ${helper.name}")
                }

                val reportValues = firstMechanism.tokens.tokensInSolver.map { token ->
                    when {
                        token.variable === ordinary -> Flt64(7.0)
                        token.variable === function.indicatorVar -> Flt64.one
                        token.variable === resultVariable -> Flt64(3.0)
                        else -> Flt64.zero
                    }
                }
                metaModel.tokens.setSolution(reportValues)

                assertEquals(Flt64(7.0), metaModel.tokens.find(ordinary)?.result)
                assertEquals(Flt64(3.0), metaModel.tokens.find(resultVariable)?.result)
            } finally {
                firstMechanism.close()
            }

            val helperIndices = function.helperVariables.associateWith {
                metaModel.tokens.find(it)!!.solverIndex
            }
            val secondMechanismResult = LinearMechanismModel.invoke<Flt64>(
                metaModel = metaModel,
                concurrent = false,
                blocking = true
            )
            val secondMechanism = when (secondMechanismResult) {
                is Ok -> secondMechanismResult.value
                is Failed -> fail(secondMechanismResult.error.message)
                is Fatal -> fail(secondMechanismResult.errors.joinToString { it.message })
            }

            try {
                assertEquals(ordinaryIndex, metaModel.tokens.find(ordinary)?.solverIndex)
                for ((helper, index) in helperIndices) {
                    assertEquals(index, metaModel.tokens.find(helper)?.solverIndex)
                    assertEquals(index, secondMechanism.tokens.find(helper)?.solverIndex)
                }
            } finally {
                secondMechanism.close()
            }
        } finally {
            metaModel.close()
        }
    }

    @Test
    fun piecewiseStructureRecordsItsFallbackConstraintRegion() = runBlocking {
        val converter = IntoValue.Identity
        val x = RealVar("mechanism_pwl_region_x")
        x.range.geq(Flt64.zero)
        x.range.leq(Flt64(2.0))
        val function = UnivariateLinearPiecewiseFunction(
            x = LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.zero),
            breakpoints = listOf(Flt64.zero, Flt64.one, Flt64(2.0)),
            slopes = listOf(Flt64.one, Flt64.two),
            intercepts = listOf(Flt64.zero, -Flt64.one),
            m = Flt64(10.0),
            converter = converter,
            name = "mechanism_pwl_region"
        )
        val metaModel = LinearMetaModel<Flt64>(
            name = "mechanism-pwl-region",
            converter = converter
        )
        try {
            assertTrue(metaModel.add(x) is Ok)
            assertTrue(metaModel.add(LinearFunctionSymbolAdapter(function, converter)) is Ok)
            assertTrue(metaModel.minimize(function.resultPolynomial) is Ok)
            assertTrue(metaModel.addConstraint(LinearInequality(
                lhs = function.x,
                rhs = constant(Flt64.zero),
                comparison = Comparison.GE,
                name = "preceding_bound"
            )) is Ok)
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
                assertEquals(1, mechanism.deferredFunctionConstraintRegions.size)
                val region = mechanism.deferredFunctionConstraintRegions.single()
                assertTrue(region.constraintCount > 0)
                assertTrue(region.firstConstraintIndex >= 0)
                assertTrue(region.lastConstraintIndex <= mechanism.linearConstraints.size)
                for (dumpBounds in listOf(false, true)) {
                    val triad = LinearTriadModel(
                        model = mechanism,
                        dumpConstraintsToBounds = dumpBounds,
                        concurrent = false
                    )
                    try {
                        val mapped = triad.deferredFunctionConstraintRegions.single()
                        val structure = triad.deferredFunctionStructures.single() as UnivariateLinearPiecewiseStructure<*>
                        assertSame(structure, mapped.structure)
                        assertTrue(structure.usage.inObjective)
                        assertFalse(structure.usage.inConstraint)
                        val expected = mechanism.linearConstraints.subList(region.firstConstraintIndex, region.lastConstraintIndex)
                        assertEquals(expected, triad.constraints.origins.subList(mapped.firstConstraintIndex, mapped.lastConstraintIndex))
                        assertEquals(if (dumpBounds) 0 else 1, mapped.firstConstraintIndex)
                    } finally {
                        triad.close()
                    }
                }
                val fixedResultModel = LinearTriadModel(
                    model = mechanism,
                    fixedVariables = mapOf(function.resultVar to Flt64.one),
                    concurrent = false
                )
                try {
                    val structure = fixedResultModel.deferredFunctionStructures.single() as UnivariateLinearPiecewiseStructure<*>
                    assertTrue(structure.usage.externallyReferenced)
                    assertFalse(structure.usage.supportsObjectiveOnlyNative())
                } finally {
                    fixedResultModel.close()
                }
                val convertedResult = convertMechanismModelToFlt64(mechanism)
                val converted = when (convertedResult) {
                    is Ok -> convertedResult.value as LinearMechanismModel<Flt64>
                    is Failed -> fail(convertedResult.error.message)
                    is Fatal -> fail(convertedResult.errors.joinToString { it.message })
                }
                try {
                    assertSame(mechanism.deferredFunctionStructures.single(), converted.deferredFunctionStructures.single())
                    assertEquals(mechanism.deferredFunctionConstraintRegions, converted.deferredFunctionConstraintRegions)
                    val convertedTriad = LinearTriadModel(
                        model = converted,
                        dumpConstraintsToBounds = true,
                        concurrent = false
                    )
                    try {
                        val convertedRegion = convertedTriad.deferredFunctionConstraintRegions.single()
                        assertEquals(0, convertedRegion.firstConstraintIndex)
                        assertEquals(region.constraintCount, convertedRegion.constraintCount)
                        assertTrue((convertedRegion.structure as UnivariateLinearPiecewiseStructure<*>).usage.inObjective)
                    } finally {
                        convertedTriad.close()
                    }
                } finally {
                    converted.close()
                }
                assertTrue(mechanism.addConstraint(
                    relation = LinearInequality(
                        lhs = constant(Flt64.one),
                        rhs = function.resultPolynomial,
                        comparison = Comparison.LE,
                        name = "result_on_rhs"
                    ),
                    name = "result_on_rhs"
                ) is Ok)
                val rhsModel = LinearTriadModel(model = mechanism, concurrent = false)
                try {
                    val structure = rhsModel.deferredFunctionStructures.single() as UnivariateLinearPiecewiseStructure<*>
                    assertTrue(structure.usage.inConstraint)
                } finally {
                    rhsModel.close()
                }
            } finally {
                mechanism.close()
            }
        } finally {
            metaModel.close()
        }
    }

    @Test
    fun piecewiseResultUsedAsAnotherInputRequiresComposableLowering() = runBlocking {
        val input = RealVar("nested_pwl_input")
        input.range.geq(Flt64.zero)
        input.range.leq(Flt64.two)
        fun piecewise(polynomial: LinearPolynomial<Flt64>, name: String) = UnivariateLinearPiecewiseFunction(
            x = polynomial,
            breakpoints = listOf(Flt64.zero, Flt64.one, Flt64.two),
            slopes = listOf(Flt64.one, Flt64.one),
            intercepts = listOf(Flt64.zero, Flt64.zero),
            m = Flt64(10.0),
            converter = IntoValue.Identity,
            name = name
        )
        val producer = piecewise(LinearPolynomial(listOf(LinearMonomial(Flt64.one, input)), Flt64.zero), "producer")
        val consumer = piecewise(producer.resultPolynomial, "consumer")
        val metaModel = LinearMetaModel<Flt64>(name = "nested-pwl", converter = IntoValue.Identity)
        try {
            assertTrue(metaModel.add(input) is Ok)
            assertTrue(metaModel.add(LinearFunctionSymbolAdapter(producer, IntoValue.Identity)) is Ok)
            assertTrue(metaModel.add(LinearFunctionSymbolAdapter(consumer, IntoValue.Identity)) is Ok)
            assertTrue(metaModel.minimize(consumer.resultPolynomial) is Ok)
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
                val triad = LinearTriadModel(model = mechanism, concurrent = false)
                try {
                    val structure = triad.deferredFunctionStructures.filterIsInstance<UnivariateLinearPiecewiseStructure<*>>()
                        .single { it.resultVariable.key == producer.resultVar.key }
                    assertTrue(structure.usage.nestedAsInput)
                    assertFalse(structure.usage.inObjective)
                    assertTrue(structure.usage.requiresResultVariable)
                    assertFalse(structure.usage.supportsObjectiveOnlyNative())
                } finally {
                    triad.close()
                }
            } finally {
                mechanism.close()
            }
        } finally {
            metaModel.close()
        }
    }

    @Test
    fun failedFunctionRegistrationRestoresSourceTokensBeforeRetry() = runBlocking {
        val converter = IntoValue.Identity
        val ordinary = RealVar("mechanism_sync_failure_ordinary")
        val helper = RealVar("mechanism_sync_failure_helper")
        val failingFunction = ToggleFailFunction(helper)
        val symbol = LinearFunctionSymbolAdapter(failingFunction, converter)
        val metaModel = LinearMetaModel<Flt64>(
            name = "mechanism-token-synchronization-failure",
            configuration = MetaModelConfiguration(concurrent = false),
            converter = converter
        )

        try {
            assertTrue(metaModel.add(ordinary) is Ok)
            assertTrue(metaModel.add(symbol) is Ok)
            val originalOrdinaryToken = metaModel.tokens.find(ordinary)
            assertNotNull(originalOrdinaryToken)
            val ordinaryIndex = originalOrdinaryToken.solverIndex
            val originalCallback = originalOrdinaryToken.refreshCallbacks[metaModel.tokens.tokenList]
            metaModel.tokens.setSolverSolution(mapOf(ordinary to Flt64.one))
            val originalCachedSolution = metaModel.tokens.cachedSolution
            val originalTokenCount = metaModel.tokens.tokens.size

            val failedBuild = LinearMechanismModel.invoke<Flt64>(
                metaModel = metaModel,
                concurrent = false,
                blocking = true
            )
            assertTrue(failedBuild is Failed, "Function registration failure should fail model construction")
            assertEquals(originalTokenCount, metaModel.tokens.tokens.size)
            assertTrue(metaModel.tokens.tokens.none { it.key == helper.key })
            assertSame(originalOrdinaryToken, metaModel.tokens.find(ordinary))
            assertEquals(originalCallback, originalOrdinaryToken.refreshCallbacks[metaModel.tokens.tokenList])
            assertEquals(originalCachedSolution, metaModel.tokens.cachedSolution)
            assertEquals(ordinaryIndex, metaModel.tokens.find(ordinary)?.solverIndex)
            assertNull(boundTokenTableContext(symbol))

            failingFunction.shouldFail = false
            val successfulBuild = LinearMechanismModel.invoke<Flt64>(
                metaModel = metaModel,
                concurrent = false,
                blocking = true
            )
            val mechanism = when (successfulBuild) {
                is Ok -> successfulBuild.value
                is Failed -> fail(successfulBuild.error.message)
                is Fatal -> fail(successfulBuild.errors.joinToString { it.message })
            }
            try {
                assertEquals(originalTokenCount + 1, metaModel.tokens.tokens.size)
                assertEquals(ordinaryIndex, metaModel.tokens.find(ordinary)?.solverIndex)
                assertEquals(ordinaryIndex + 1, metaModel.tokens.find(helper)?.solverIndex)
                assertNotNull(mechanism.tokens.find(helper))
            } finally {
                mechanism.close()
            }
        } finally {
            metaModel.close()
        }
        Unit
    }

    @Test
    fun failedBuildConstraintsRestoresSourceTokensBeforeRetry() = runBlocking {
        val converter = IntoValue.Identity
        val ordinary = RealVar("mechanism_build_constraints_ordinary")
        val helper = RealVar("mechanism_build_constraints_helper")
        val function = ToggleFailFunction(helper)
        val symbol = LinearFunctionSymbolAdapter(function, converter)
        val invalidIntermediate = QuadraticExpressionSymbol(
            ordinary,
            Flt64,
            name = "mechanism_build_constraints_invalid_intermediate"
        )
        val invalidConstraint = LinearInequality(
            lhs = LinearPolynomial(
                monomials = listOf(LinearMonomial(Flt64.one, invalidIntermediate)),
                constant = Flt64.zero
            ),
            rhs = constant(Flt64.zero),
            comparison = Comparison.EQ,
            name = "mechanism_build_constraints_invalid"
        )
        val metaModel = LinearMetaModel<Flt64>(
            name = "mechanism-build-constraints-failure",
            converter = converter
        )

        try {
            assertTrue(metaModel.add(ordinary) is Ok)
            assertTrue(metaModel.add(symbol) is Ok)
            assertTrue(metaModel.addConstraint(invalidConstraint, lazy = false) is Ok)

            val originalToken = metaModel.tokens.find(ordinary)
            assertNotNull(originalToken)
            val originalTokenCount = metaModel.tokens.tokens.size
            val originalConstraintCount = metaModel.constraints.size
            val originalIndex = originalToken.solverIndex

            val failedBuild = LinearMechanismModel.invoke<Flt64>(
                metaModel = metaModel,
                concurrent = false,
                blocking = true
            )
            assertTrue(failedBuild is Failed, "Constraint construction failure should fail model construction")
            assertEquals(originalTokenCount, metaModel.tokens.tokens.size)
            assertSame(originalToken, metaModel.tokens.find(ordinary))
            assertEquals(originalIndex, metaModel.tokens.find(ordinary)?.solverIndex)
            assertTrue(metaModel.tokens.tokens.none { it.key == helper.key })
            assertNull(boundTokenTableContext(symbol))

            assertTrue(metaModel.rollbackConstraintsTo(originalConstraintCount - 1) is Ok)
            function.shouldFail = false
            val successfulBuild = LinearMechanismModel.invoke<Flt64>(
                metaModel = metaModel,
                concurrent = false,
                blocking = true
            )
            val mechanism = when (successfulBuild) {
                is Ok -> successfulBuild.value
                is Failed -> fail(successfulBuild.error.message)
                is Fatal -> fail(successfulBuild.errors.joinToString { it.message })
            }
            try {
                assertEquals(originalTokenCount + 1, metaModel.tokens.tokens.size)
                assertEquals(originalIndex, metaModel.tokens.find(ordinary)?.solverIndex)
                assertNotNull(metaModel.tokens.find(helper))
                assertNotNull(mechanism.tokens.find(helper))
            } finally {
                mechanism.close()
            }
        } finally {
            metaModel.close()
        }
        Unit
    }

    @Test
    fun failedQuadraticFunctionRegistrationRestoresSourceTokensBeforeRetry() = runBlocking {
        val converter = IntoValue.Identity
        val ordinary = RealVar("mechanism_quadratic_failure_ordinary")
        val helper = RealVar("mechanism_quadratic_failure_helper")
        val function = ToggleFailFunction(helper)
        val symbol = LinearFunctionSymbolAdapter(function, converter)
        val metaModel = QuadraticMetaModel<Flt64>(
            name = "mechanism-quadratic-function-failure",
            converter = converter
        )

        try {
            assertTrue(metaModel.add(ordinary) is Ok)
            assertTrue(metaModel.add(symbol) is Ok)
            val originalToken = metaModel.tokens.find(ordinary)
            assertNotNull(originalToken)
            val originalIndex = originalToken.solverIndex
            val originalTokenCount = metaModel.tokens.tokens.size

            val failedBuild = QuadraticMechanismModel.invoke<Flt64>(
                metaModel = metaModel,
                concurrent = false,
                blocking = true
            )
            assertTrue(failedBuild is Failed, "Quadratic function registration failure should fail model construction")
            assertEquals(originalTokenCount, metaModel.tokens.tokens.size)
            assertSame(originalToken, metaModel.tokens.find(ordinary))
            assertEquals(originalIndex, metaModel.tokens.find(ordinary)?.solverIndex)
            assertTrue(metaModel.tokens.tokens.none { it.key == helper.key })
            assertNull(boundTokenTableContext(symbol))

            function.shouldFail = false
            val successfulBuild = QuadraticMechanismModel.invoke<Flt64>(
                metaModel = metaModel,
                concurrent = false,
                blocking = true
            )
            val mechanism = when (successfulBuild) {
                is Ok -> successfulBuild.value
                is Failed -> fail(successfulBuild.error.message)
                is Fatal -> fail(successfulBuild.errors.joinToString { it.message })
            }
            try {
                assertEquals(originalTokenCount + 1, metaModel.tokens.tokens.size)
                assertEquals(originalIndex, metaModel.tokens.find(ordinary)?.solverIndex)
                assertNotNull(metaModel.tokens.find(helper))
                assertNotNull(mechanism.tokens.find(helper))
            } finally {
                mechanism.close()
            }
        } finally {
            metaModel.close()
        }
        Unit
    }

    private fun constant(value: Flt64): LinearPolynomial<Flt64> {
        return LinearPolynomial(emptyList(), value)
    }

    private class ToggleFailFunction(
        private val helper: RealVar
    ) : MathFunctionSymbol<Flt64> {
        override var name: String = "mechanism_sync_failure_function"
        override var displayName: String? = null
        override val helperVariables = listOf(helper)
        var shouldFail: Boolean = true

        override fun evaluate(values: Map<Symbol, Flt64>): Flt64? {
            return Flt64.zero
        }

        override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<Flt64>): Try {
            return tokens.add(helper)
        }

        override fun registerConstraints(model: AbstractLinearMechanismModel<Flt64>): Try {
            return if (shouldFail) {
                Failed(
                    Err(
                        ErrorCode.ApplicationFailed,
                        "测试函数约束注册失败 / Test function constraint registration failed"
                    )
                )
            } else {
                ok
            }
        }
    }
}
