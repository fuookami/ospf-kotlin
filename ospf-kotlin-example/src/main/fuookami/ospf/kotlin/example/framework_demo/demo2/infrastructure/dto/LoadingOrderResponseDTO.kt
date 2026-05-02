package fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto

import kotlinx.serialization.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.Diagnostics

@Serializable
data class LoadingOrderResponseDTO(
    val succeed: Boolean,
    val status: String = "",
    val orders: List<String> = emptyList(),
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
        fun success(
            orders: List<String>,
            notes: List<String> = emptyList()
        ): LoadingOrderResponseDTO = LoadingOrderResponseDTO(
            succeed = true,
            status = "Optimal",
            orders = orders,
            notes = notes,
            diagnostics = Diagnostics.buildStructured(notes)
        )
    }
}
