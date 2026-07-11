package fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto

import kotlinx.serialization.*
import fuookami.ospf.kotlin.utils.error.*

/**
 * Data transfer object for report generation response.
 * 报告生成响应的数据传输对象。
 *
 * @property succeed whether the report request was processed successfully / 报告请求是否处理成功
*/
@Serializable
data class ReportResponseDTO(
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