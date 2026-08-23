package fuookami.ospf.kotlin.example.framework_demo.demo2

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*

/** 验证 Demo2 应用在 Gurobi 下的端到端求解路径 / Verifies Demo2 application end-to-end solve paths with Gurobi. */
class Demo2ApplicationE2EIntegration {
    @Test
    fun `all applications solve a minimal feasible request through direct MILP`() = runBlocking {
        for ((path, result) in applications(directFeasibleRequest(), withRender = true)) {
            val (response, render) = result

            assertTrue(response.succeed, "$path direct MILP failed: ${response.notes}")
            assertEquals("Optimal", response.status)
            if (path != "predistribution") {
                assertTrue(response.assignments.isNotEmpty(), "$path direct MILP assignments: $response")
            }
            assertTrue(response.notes.contains("solver_path=milp_direct"), "$path solver notes: ${response.notes}")
            assertNotNull(render)
            if (path != "predistribution") {
                assertTrue(render?.positions?.isNotEmpty() == true)
            }
        }
    }

    @Test
    fun `all direct applications retain the default minimum payload ratio`() = runBlocking {
        val request = RequestDTO.sample().copy(
            id = "demo2-default-min-payload",
            cargos = listOf(
                CargoInput(
                    name = "C-default-min",
                    weight = 8.0,
                    priority = 10,
                    source = "S1",
                    destination = "D1"
                )
            ),
            payloadUpperBound = 10.0
        )
        assertEquals(0.6, request.minPayloadRatio, 1e-9)

        for ((path, result) in applications(request)) {
            val response = result.first
            assertTrue(response.succeed, "$path default minimum payload failed: ${response.notes}")
            assertEquals("Optimal", response.status)
            assertTrue(response.notes.contains("solver_path=milp_direct"))
        }
    }

    @Test
    fun `all applications fall back to direct MILP after a Benders failure`() = runBlocking {
        for ((path, result) in applications(bendersFailureRequest(fallbackToMilp = true), withRender = true)) {
            val (response, render) = result

            assertTrue(response.succeed, "$path Benders fallback failed: ${response.notes}")
            assertEquals("Optimal", response.status)
            assertTrue(response.notes.any { it.contains("Benders failed, falling back to MILP") })
            assertTrue(response.notes.contains("solver_path=milp_fallback_after_benders"))
            assertNotNull(render)
        }
    }

    @Test
    fun `all applications report Benders failure when fallback is disabled`() = runBlocking {
        for ((path, result) in applications(bendersFailureRequest(fallbackToMilp = false))) {
            val (response, render) = result

            assertFalse(response.succeed, "$path should expose the Benders failure")
            assertEquals("BendersFailed", response.status)
            assertTrue(response.notes.any { it.contains("Benders requested") })
            assertNull(render)
        }
    }

    @Test
    fun `full-load application solves a minimal request through Benders`() = runBlocking {
        val (response, render) = FullLoadAlgorithm(bendersSuccessRequest(), withRender = true)

        assertTrue(response.succeed, "Benders full-load failed: ${response.notes}")
        assertEquals("Optimal", response.status)
        assertTrue(response.notes.contains("solver_path=benders"))
        assertNotNull(render)
    }

    @Test
    fun `all applications solve a minimal request through Benders`() = runBlocking {
        for ((path, result) in applications(bendersSuccessRequest(), withRender = true)) {
            val (response, render) = result

            assertTrue(response.succeed, "$path Benders solve failed: ${response.notes}")
            assertEquals("Optimal", response.status)
            assertTrue(response.notes.contains("solver_path=benders"))
            assertNotNull(render)
        }
    }

    private suspend fun applications(
        request: RequestDTO,
        withRender: Boolean = false
    ): List<Pair<String, Pair<ResponseDTO, RenderDTO?>>> {
        return listOf(
            "full-load" to FullLoadAlgorithm(request, withRender = withRender),
            "predistribution" to PredistributionApplication()(request, withRender = withRender),
            "weight-recommendation" to WeightRecommendationApplication()(request, withRender = withRender)
        )
    }

    private fun directFeasibleRequest(): RequestDTO {
        return RequestDTO.sample().copy(
            id = "demo2-direct-feasible",
            cargos = listOf(
                CargoInput(
                    name = "C-direct",
                    weight = 1.0,
                    priority = 10,
                    source = "S1",
                    destination = "D1"
                )
            ),
            payloadUpperBound = 10.0,
            minPayloadRatio = 0.0
        )
    }

    private fun bendersFailureRequest(fallbackToMilp: Boolean): RequestDTO {
        return directFeasibleRequest().copy(
            solvePolicy = SolvePolicy(
                preferBenders = true,
                bendersFallbackToMilp = fallbackToMilp
            ),
            bendersAdaptive = BendersAdaptiveConfig(
                minBinaryVariables = 1,
                maxIterations = 0,
                tolerance = 1e-6
            )
        )
    }

    private fun bendersSuccessRequest(): RequestDTO {
        return directFeasibleRequest().copy(
            solvePolicy = SolvePolicy(
                preferBenders = true,
                bendersFallbackToMilp = false
            ),
            bendersAdaptive = BendersAdaptiveConfig(
                minBinaryVariables = 1,
                maxIterations = 64,
                tolerance = 1e-6
            )
        )
    }
}
