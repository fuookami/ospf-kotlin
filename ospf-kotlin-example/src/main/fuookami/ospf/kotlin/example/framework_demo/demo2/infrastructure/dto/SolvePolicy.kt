package fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto

import kotlinx.serialization.*

@Serializable
data class SolvePolicy(
    val preferBenders: Boolean = false,
    val bendersFallbackToMilp: Boolean = true
)
