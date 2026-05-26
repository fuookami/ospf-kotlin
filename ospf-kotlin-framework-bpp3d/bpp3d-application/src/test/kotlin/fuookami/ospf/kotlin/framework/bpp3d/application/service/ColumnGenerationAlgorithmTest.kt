package fuookami.ospf.kotlin.framework.bpp3d.application.service

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Item
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation.Bpp3dLayerGenerationRequest
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation.Bpp3dLayerGenerationResult
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation.Bpp3dLayerGenerator
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation.DemandModeKey
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ColumnGenerationAlgorithmTest {
    @Test
    fun columnGenerationAlgorithmShouldLiveInApplicationService() {
        val algorithm = ColumnGenerationAlgorithm(
            layerGenerator = object : Bpp3dLayerGenerator<Double> {
                override suspend fun generate(request: Bpp3dLayerGenerationRequest<Double>): List<Bpp3dLayerGenerationResult<Double>> {
                    return emptyList()
                }
            }
        )
        assertNotNull(algorithm)
    }

    @Test
    fun algorithmShouldStopWhenNoAcceptedColumns() = runBlocking {
        val algorithm = ColumnGenerationAlgorithm(
            layerGenerator = object : Bpp3dLayerGenerator<Double> {
                override suspend fun generate(request: Bpp3dLayerGenerationRequest<Double>): List<Bpp3dLayerGenerationResult<Double>> {
                    return emptyList()
                }
            }
        )

        val result = algorithm.solve(items = emptyList<Item>())
        assertEquals(0, result.iterationCount)
        assertEquals(0, result.columns.size)
        assertEquals(1, result.lpSolvedTimes)
        assertTrue(result.finalSolved)
    }

    @Test
    fun algorithmShouldInvokeFinalSolveAnalyzerAndHeartbeatCallbacks() = runBlocking {
        val finalSolveCounter = AtomicInteger(0)
        val analyzeCounter = AtomicInteger(0)
        val heartbeatCounter = AtomicInteger(0)
        val algorithm = ColumnGenerationAlgorithm(
            layerGenerator = object : Bpp3dLayerGenerator<Double> {
                override suspend fun generate(request: Bpp3dLayerGenerationRequest<Double>): List<Bpp3dLayerGenerationResult<Double>> {
                    return emptyList()
                }
            },
            solveFinalMilp = {
                finalSolveCounter.incrementAndGet()
            },
            analyzeSolution = {
                analyzeCounter.incrementAndGet()
            },
            onIterationHeartbeat = {
                heartbeatCounter.incrementAndGet()
            }
        )

        val result = algorithm.solve(items = emptyList<Item>())
        assertEquals(1, finalSolveCounter.get())
        assertEquals(1, analyzeCounter.get())
        assertEquals(0, heartbeatCounter.get())
        assertTrue(result.finalSolved)
    }

    @Test
    fun rmpSolverAndRequestBuilderShouldBePreferredWhenProvided() = runBlocking {
        val legacyRmpCounter = AtomicInteger(0)
        val rmpSolverCounter = AtomicInteger(0)
        val requestBuilderCounter = AtomicInteger(0)
        var observedMaxCandidates = -1
        val algorithm = ColumnGenerationAlgorithm(
            layerGenerator = object : Bpp3dLayerGenerator<Double> {
                override suspend fun generate(request: Bpp3dLayerGenerationRequest<Double>): List<Bpp3dLayerGenerationResult<Double>> {
                    observedMaxCandidates = request.maxCandidates
                    return emptyList()
                }
            },
            rmpSolver = ColumnGenerationRmpSolver {
                rmpSolverCounter.incrementAndGet()
                ColumnGenerationLpResult(
                    shadowPrices = emptyMap<DemandModeKey, Double>(),
                    objective = 42.0
                )
            },
            layerRequestBuilder = ColumnGenerationLayerRequestBuilder { state, items, _ ->
                requestBuilderCounter.incrementAndGet()
                Bpp3dLayerGenerationRequest(
                    iteration = state.iteration,
                    items = items,
                    existingLayers = state.columns,
                    shadowPrices = state.shadowPrices,
                    maxCandidates = 1
                )
            },
            solveRmpWithResult = {
                legacyRmpCounter.incrementAndGet()
                ColumnGenerationLpResult(emptyMap())
            }
        )

        val result = algorithm.solve(
            items = emptyList(),
            config = ColumnGenerationConfig(maxColumnsPerIteration = 99)
        )
        assertEquals(1, rmpSolverCounter.get())
        assertEquals(0, legacyRmpCounter.get())
        assertEquals(1, requestBuilderCounter.get())
        assertEquals(1, observedMaxCandidates)
        assertEquals(listOf(42.0), result.lpObjectives)
    }
}
