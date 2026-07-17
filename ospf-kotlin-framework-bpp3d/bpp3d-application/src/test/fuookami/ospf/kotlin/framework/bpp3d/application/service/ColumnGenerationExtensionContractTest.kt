package fuookami.ospf.kotlin.framework.bpp3d.application.service

import kotlin.test.*
import kotlin.time.Duration
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.basic.RegistrationStatusCallBack
import fuookami.ospf.kotlin.core.solver.output.*
import fuookami.ospf.kotlin.core.symbol.LinearExpressionSymbol
import fuookami.ospf.kotlin.framework.solver.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.Bpp3dDemandEntry
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation.*

class ColumnGenerationExtensionContractTest {
    @Test
    fun fallibleInitialColumnsShouldReportStageToFailureAnalyzer() = runBlocking {
        var analyzedFailure: ColumnGenerationFailure<FltX>? = null
        val algorithm = ColumnGenerationAlgorithm(
            layerGenerator = emptyLayerGenerator(),
            initialColumnsWithResult = {
                Failed(ErrorCode.IllegalArgument, "初始列无效 / Invalid initial columns")
            },
            failureAnalyzer = ColumnGenerationFailureAnalyzer { failure ->
                analyzedFailure = failure
                ok
            }
        )

        val result = algorithm.solve(
            items = emptyList(),
            config = ColumnGenerationConfig(finalMilpEnabled = false)
        )

        assertTrue(result is Failed)
        assertEquals(ColumnGenerationFailureStage.InitialColumns, analyzedFailure?.stage)
        assertTrue(analyzedFailure?.state?.columns?.isEmpty() == true)
    }

    @Test
    fun fallibleCandidateFilterShouldReceiveAdditionalShadowPrices() = runBlocking {
        var capturedPrices: Map<String, FltX> = emptyMap()
        val algorithm = ColumnGenerationAlgorithm(
            layerGenerator = emptyLayerGenerator(),
            rmpSolver = ColumnGenerationRmpSolver {
                Ok(ColumnGenerationLpResult(
                    shadowPrices = emptyMap(),
                    additionalShadowPrices = mapOf("dynamic:constraint" to FltX(2.0))
                ))
            },
            filterByReducedCostWithResult = { state, candidates ->
                capturedPrices = state.additionalShadowPrices
                Ok(candidates)
            }
        )

        val result = algorithm.solve(
            items = emptyList(),
            config = ColumnGenerationConfig(finalMilpEnabled = false)
        )

        assertTrue(result is Ok)
        assertEquals(FltX(2.0), capturedPrices["dynamic:constraint"])
    }

    @Test
    fun standardRmpExtensionsShouldAddModelRowsAndExtractDualMetadata() = runBlocking {
        val solver = EmptyColumnGenerationSolver()
        val executors = ColumnGenerationStandardExecutors.fromDemandEntries(
            solver = solver,
            itemDemands = emptyList(),
            demandEntries = emptyList<Bpp3dDemandEntry<FltX>>()
        )
        var modelExtensionInvoked = false
        var solutionExtensionInvoked = false
        val modelExtension = ColumnGenerationRmpModelExtension { context ->
            modelExtensionInvoked = true
            val marker = LinearExpressionSymbol(FltX.zero, name = "dynamic_extension_marker")
            when (val result = context.model.add(marker)) {
                is Ok -> {}
                is Failed -> return@ColumnGenerationRmpModelExtension Failed(result.error)
                is Fatal -> return@ColumnGenerationRmpModelExtension Fatal(result.errors)
            }
            val relation = LinearInequality(
                lhs = marker.polynomial,
                rhs = LinearPolynomial(emptyList(), FltX.zero),
                comparison = Comparison.EQ
            )
            return@ColumnGenerationRmpModelExtension context.model.addConstraint(
                relation = relation,
                name = "dynamic_extension_constraint"
            )
        }
        val solutionExtension = ColumnGenerationRmpSolutionExtension { context, dualSolution ->
            solutionExtensionInvoked = true
            assertTrue(context.model.constraints.isNotEmpty())
            assertEquals(Flt64(4.0), dualSolution.constraints[context.model.constraints.last()])
            Ok(ColumnGenerationRmpExtensionResult(
                additionalShadowPrices = mapOf("dynamic:constraint" to FltX(3.0)),
                info = mapOf("dynamic_extension" to "registered")
            ))
        }

        val result = executors.rmpSolver(
            modelExtensions = listOf(modelExtension),
            solutionExtensions = listOf(solutionExtension)
        ).solve(ColumnGenerationState(iteration = 0, columns = emptyList()))

        assertTrue(result is Ok)
        val lpResult = (result as Ok).value
        assertTrue(modelExtensionInvoked)
        assertTrue(solutionExtensionInvoked)
        assertEquals(FltX(3.0), lpResult.additionalShadowPrices["dynamic:constraint"])
        assertEquals("registered", lpResult.info["dynamic_extension"])
    }

    private fun emptyLayerGenerator(): Bpp3dLayerGenerator<FltX> {
        return object : Bpp3dLayerGenerator<FltX> {
            override suspend fun generate(
                request: Bpp3dLayerGenerationRequest<FltX>
            ): List<Bpp3dLayerGenerationResult<FltX>> {
                return emptyList()
            }
        }
    }
}

private class EmptyColumnGenerationSolver : ColumnGenerationSolver {
    override val name: String = "empty-column-generation-solver"

    override suspend fun solveMILP(
        name: String,
        metaModel: LinearMetaModel<Flt64>,
        toLogModel: Boolean,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<FeasibleSolverOutput<Flt64>> {
        return Ok(emptyOutput(metaModel))
    }

    override suspend fun solveLP(
        name: String,
        metaModel: LinearMetaModel<Flt64>,
        toLogModel: Boolean,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<ColumnGenerationSolver.LPResult> {
        return when (val result = LinearMechanismModel(metaModel = metaModel)) {
            is Ok -> result.value.use { model ->
                @Suppress("UNCHECKED_CAST")
                val dualSolution = model.constraints.associate { constraint ->
                    (constraint as Constraint<Flt64, Linear>) to Flt64(4.0)
                }
                Ok(ColumnGenerationSolver.LPResult(
                    result = emptyOutput(metaModel),
                    dualSolution = dualSolution
                ))
            }
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    private fun emptyOutput(metaModel: LinearMetaModel<Flt64>): FeasibleSolverOutput<Flt64> {
        return FeasibleSolverOutput(
            obj = Flt64.zero,
            solution = List(metaModel.tokens.tokensInSolver.size) { Flt64.zero },
            time = Duration.ZERO,
            possibleBestObj = Flt64.zero,
            gap = Flt64.zero
        )
    }
}
