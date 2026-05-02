package fuookami.ospf.kotlin.example.framework_demo.demo2

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.Diagnostics

class DiagnosticsTest {
    @Test
    fun `buildStructured parses grouped note`() {
        val notes = listOf("critical|group=payload|code=payload_upper_utilization_high|msg=payload near upper")
        val diagnostics = Diagnostics.buildStructured(notes)
        assertEquals(1, diagnostics.size)
        assertEquals("critical", diagnostics[0].level)
        assertEquals("payload", diagnostics[0].group)
        assertEquals("payload_upper_utilization_high", diagnostics[0].code)
        assertEquals("payload near upper", diagnostics[0].message)
    }

    @Test
    fun `buildStructured falls back to plain note`() {
        val notes = listOf("unsupported aircraft type")
        val diagnostics = Diagnostics.buildStructured(notes)
        assertEquals(1, diagnostics.size)
        assertEquals(Diagnostics.LEVEL_DIAGNOSTIC, diagnostics[0].level)
        assertNull(diagnostics[0].group)
        assertNull(diagnostics[0].code)
        assertEquals("unsupported aircraft type", diagnostics[0].message)
    }

    @Test
    fun `pushGroupedNote formats correctly`() {
        val notes = mutableListOf<String>()
        Diagnostics.pushGroupedNote(notes, Diagnostics.LEVEL_DIAGNOSTIC, Diagnostics.GROUP_SOLVER, Diagnostics.CODE_SOLVER_PATH, "solver path=milp_direct")
        assertEquals(1, notes.size)
        assertTrue(notes[0].contains("diagnostic"))
        assertTrue(notes[0].contains("solver"))
        assertTrue(notes[0].contains("solver_path"))
        assertTrue(notes[0].contains("solver path=milp_direct"))
    }

    @Test
    fun `buildStructured handles multiple notes`() {
        val notes = listOf(
            "diagnostic|group=solver|code=solver_path|msg=solver path=milp_direct",
            "critical|group=airworthiness|code=capacity_utilization_high|msg=P1 utilization 99%",
            "plain note without structure"
        )
        val diagnostics = Diagnostics.buildStructured(notes)
        assertEquals(3, diagnostics.size)
        assertEquals("solver", diagnostics[0].group)
        assertEquals("airworthiness", diagnostics[1].group)
        assertNull(diagnostics[2].group)
    }
}
