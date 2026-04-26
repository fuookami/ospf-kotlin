package fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto

import kotlinx.serialization.*
import fuookami.ospf.kotlin.utils.error.*

@Serializable
data class KPIResponseDTO(
    val succeed: Boolean,
) {
    constructor(
        request: RequestDTO,
        error: Error<ErrorCode>
    ): this(succeed = false)
}
