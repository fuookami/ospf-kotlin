package fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto

import kotlinx.serialization.*

@Serializable
data class DiagnosticNote(
    val level: String,
    val group: String? = null,
    val code: String? = null,
    val message: String
)
