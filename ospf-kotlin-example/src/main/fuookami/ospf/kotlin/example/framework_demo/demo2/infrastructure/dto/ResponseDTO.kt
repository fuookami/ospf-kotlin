package fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto

import kotlinx.serialization.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.Diagnostics

@Serializable
data class ResponseDTO(
    val succeed: Boolean,
    val status: String = "",
    val objective: Double? = null,
    val assignments: List<String> = emptyList(),
    val notes: List<String> = emptyList(),
    val diagnostics: List<DiagnosticNote> = emptyList()
) {
    constructor(
        request: RequestDTO,
        error: Error<ErrorCode>
    ): this(
        succeed = false,
        status = "Error",
        notes = listOf(error.message)
    )

    companion object {
        fun noSolution(status: String, notes: List<String>): ResponseDTO = ResponseDTO(
            succeed = false,
            status = status,
            notes = notes,
            diagnostics = Diagnostics.buildStructured(notes)
        )

        fun optimal(
            objective: Double,
            assignments: List<String>,
            notes: List<String>
        ): ResponseDTO = ResponseDTO(
            succeed = true,
            status = "Optimal",
            objective = objective,
            assignments = assignments,
            notes = notes,
            diagnostics = Diagnostics.buildStructured(notes)
        )
    }
}