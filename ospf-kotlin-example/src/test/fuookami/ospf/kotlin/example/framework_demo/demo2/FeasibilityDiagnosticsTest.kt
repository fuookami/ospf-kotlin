package fuookami.ospf.kotlin.example.framework_demo.demo2

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.FeasibilityDiagnostics
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.RequestDTO

class FeasibilityDiagnosticsTest {
    @Test
    fun `no diagnostics for valid sample request`() {
        val request = RequestDTO.sample()
        val notes = mutableListOf<String>()
        FeasibilityDiagnostics.appendCoreFeasibilityDiagnostics(request, notes)
        assertTrue(notes.isEmpty())
    }

    @Test
    fun `detects invalid envelope range`() {
        val request = RequestDTO.sample().copy(
            envelopeLongitudinalMomentMin = 10.0,
            envelopeLongitudinalMomentMax = -10.0
        )
        val notes = mutableListOf<String>()
        FeasibilityDiagnostics.appendCoreFeasibilityDiagnostics(request, notes)
        assertTrue(notes.any { it.contains("envelope_range_invalid") })
    }

    @Test
    fun `detects negative payload upper bound`() {
        val request = RequestDTO.sample().copy(payloadUpperBound = -1.0)
        val notes = mutableListOf<String>()
        FeasibilityDiagnostics.appendCoreFeasibilityDiagnostics(request, notes)
        assertTrue(notes.any { it.contains("payload_upper_negative") })
    }

    @Test
    fun `detects min payload ratio out of range`() {
        val request = RequestDTO.sample().copy(minPayloadRatio = 1.5)
        val notes = mutableListOf<String>()
        FeasibilityDiagnostics.appendCoreFeasibilityDiagnostics(request, notes)
        assertTrue(notes.any { it.contains("min_payload_ratio_out_of_range") })
    }

    @Test
    fun `detects cargo exceeding all positions`() {
        val request = RequestDTO.sample().copy(
            cargos = listOf(
                fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.CargoInput(
                    name = "Big", weight = 100.0, priority = 1, source = "S1", destination = "D1"
                )
            )
        )
        val notes = mutableListOf<String>()
        FeasibilityDiagnostics.appendCoreFeasibilityDiagnostics(request, notes)
        assertTrue(notes.any { it.contains("cargo_exceeds_all_positions") })
    }
}
