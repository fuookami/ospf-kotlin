package fuookami.ospf.kotlin.example.framework_demo.demo2

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*

/** 验证 Demo2 应用层公开失败响应 / Verifies Demo2 application public failure responses. */
class Demo2ApplicationErrorBoundaryTest {
    @Test
    fun `all applications return UnsupportedAircraft before initialization`() = runBlocking {
        val applications = listOf(
            ApplicationCase("full-load") { request -> FullLoadAlgorithm(request) },
            ApplicationCase("predistribution") { request -> PredistributionApplication()(request) },
            ApplicationCase("weight-recommendation") { request -> WeightRecommendationApplication()(request) }
        )
        val unsupportedTypes = listOf(
            AircraftTypeInput.B767,
            AircraftTypeInput.B747,
            AircraftTypeInput.Unknown
        )

        for (application in applications) {
            for (aircraftType in unsupportedTypes) {
                val (response, render) = application.algorithm(RequestDTO.sample().copy(aircraftType = aircraftType))

                assertFalse(response.succeed, "${application.path} should reject $aircraftType")
                assertEquals("UnsupportedAircraft", response.status)
                assertTrue(response.assignments.isEmpty())
                assertNull(render)
                assertTrue(response.notes.single().contains(aircraftType.name))
                assertTrue(response.notes.single().contains("不支持"))
                assertTrue(response.notes.single().contains(application.path))
            }
        }
    }

    @Test
    fun `solver infeasibility is mapped to NoSolution`() {
        val request = RequestDTO.sample()
        val errorCodes = listOf(
            ErrorCode.ORModelInfeasible,
            ErrorCode.ORModelInfeasibleOrUnbounded
        )

        for (errorCode in errorCodes) {
            val (response, render) = solverFailureResponse(
                request = request,
                notes = listOf("solver_path=milp_direct"),
                error = Err(errorCode, "solver test failure")
            )

            assertFalse(response.succeed)
            assertEquals("NoSolution", response.status)
            assertTrue(response.notes.any { it.contains(errorCode.name) })
            assertNull(render)
        }
    }

    @Test
    fun `non infeasibility solver failures remain errors`() {
        val (response, render) = solverFailureResponse(
            request = RequestDTO.sample(),
            notes = emptyList(),
            error = Err(ErrorCode.ApplicationFailed, "application test failure")
        )

        assertFalse(response.succeed)
        assertEquals("Error", response.status)
        assertEquals(listOf("application test failure"), response.notes)
        assertNull(render)
    }

    private data class ApplicationCase(
        val path: String,
        val algorithm: suspend (RequestDTO) -> Pair<ResponseDTO, RenderDTO?>
    )
}
