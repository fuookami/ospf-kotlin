package fuookami.ospf.kotlin.example.framework_demo.demo2

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*

class RequestDTOTest {
    @Test
    fun `sample request has expected fields`() {
        val request = RequestDTO.sample()
        assertEquals("sample-001", request.id)
        assertEquals(3, request.cargos.size)
        assertEquals(2, request.positions.size)
        assertEquals(AircraftTypeInput.B737, request.aircraftType)
        assertFalse(request.solvePolicy.preferBenders)
        assertTrue(request.solvePolicy.bendersFallbackToMilp)
        assertEquals(4, request.bendersAdaptive.minBinaryVariables)
        assertEquals(64, request.bendersAdaptive.maxIterations)
        assertEquals(1e-6, request.bendersAdaptive.tolerance, 1e-12)
    }

    @Test
    fun `sample request cargos have correct structure`() {
        val request = RequestDTO.sample()
        val c1 = request.cargos[0]
        assertEquals("C1", c1.name)
        assertEquals(8.0, c1.weight, 0.01)
        assertEquals(10, c1.priority)
        assertEquals("S1", c1.source)
        assertEquals("D1", c1.destination)
        assertTrue(c1.requiresSeparation)
    }

    @Test
    fun `sample request positions have correct structure`() {
        val request = RequestDTO.sample()
        val p1 = request.positions[0]
        assertEquals("P1", p1.name)
        assertEquals(10.0, p1.maxWeight, 0.01)
        assertEquals(-1.0, p1.longitudinalArm, 0.01)
        assertEquals(-0.5, p1.lateralArm, 0.01)
    }

    @Test
    fun `request with benders policy`() {
        val request = RequestDTO.sample().copy(
            solvePolicy = SolvePolicy(preferBenders = true, bendersFallbackToMilp = false)
        )
        assertTrue(request.solvePolicy.preferBenders)
        assertFalse(request.solvePolicy.bendersFallbackToMilp)
    }

    @Test
    fun `DiagnosticNote serialization fields`() {
        val note = DiagnosticNote(
            level = "critical",
            group = "airworthiness",
            code = "capacity_utilization_high",
            message = "P1 utilization 99%"
        )
        assertEquals("critical", note.level)
        assertEquals("airworthiness", note.group)
        assertEquals("capacity_utilization_high", note.code)
        assertEquals("P1 utilization 99%", note.message)
    }

    @Test
    fun `ResponseDTO noSolution factory`() {
        val response = ResponseDTO.noSolution("NoSolution", listOf("test note"))
        assertFalse(response.succeed)
        assertEquals("NoSolution", response.status)
        assertEquals(1, response.notes.size)
        assertEquals(1, response.diagnostics.size)
    }

    @Test
    fun `ResponseDTO optimal factory`() {
        val response = ResponseDTO.optimal(
            objective = 18.0,
            assignments = listOf("C1 -> P1", "C2 -> P2"),
            notes = listOf("solver_path=milp_direct")
        )
        assertTrue(response.succeed)
        assertEquals("Optimal", response.status)
        assertEquals(18.0, response.objective!!, 0.01)
        assertEquals(2, response.assignments.size)
    }
}