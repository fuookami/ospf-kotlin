package fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto

import kotlinx.serialization.*
import fuookami.ospf.kotlin.utils.error.*

/**
 * Data transfer object for KPI (Key Performance Indicator) response.
 * KPI（关键绩效指标）响应的数据传输对象。
 *
 * @property succeed whether the KPI request was processed successfully / KPI请求是否处理成功
 */
@Serializable
data class KPIResponseDTO(
    val succeed: Boolean,
) {
    /**
     * Constructs an error response from a failed request.
     * 从失败请求构造错误响应。
     *
     * @param request the original request DTO / 原始请求DTO
     * @param error the error encountered during processing / 处理过程中遇到的错误
     */
    constructor(
        request: RequestDTO,
        error: Error<ErrorCode>
    ): this(succeed = false)
}