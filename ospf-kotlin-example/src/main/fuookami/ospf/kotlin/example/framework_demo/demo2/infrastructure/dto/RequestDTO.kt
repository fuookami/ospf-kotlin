package fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto

import kotlinx.serialization.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*

@Serializable
data class RequestDTO(
    val id: String
) {
    val parameter: Parameter get() {
        TODO("Not yet implemented")
    }
}
