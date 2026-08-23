package fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto

import kotlinx.serialization.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.Diagnostics

/**
 * Data transfer object for loading order optimization response.
 * 装载顺序优化响应的数据传输对象。
 *
 * @property succeed 装载顺序请求是否处理成功 / whether the loading order request was processed successfully
 * @property status 求解器状态字符串（如"Optimal"、"Error"） / the solver status string (e.g., "Optimal", "Error")
 * @property orders 装载顺序标识符列表 / the list of loading order identifiers
 * @property notes 求解器的信息备注列表 / the list of informational notes from the solver
 * @property diagnostics 结构化诊断备注列表 / the list of structured diagnostic notes
*/
@Serializable
data class LoadingOrderResponseDTO(
    val succeed: Boolean,
    val status: String = "",
    val orders: List<String> = emptyList(),
    val notes: List<String> = emptyList(),
    val diagnostics: List<DiagnosticNote> = emptyList()
) {

    /**
     * Constructs an error response from a failed request.
     * 从失败请求构造错误响应。
     *
     * @param request 原始请求DTO / the original request DTO
     * @param error 处理过程中遇到的错误 / the error encountered during processing
    */
    constructor(
        request: RequestDTO,
        error: Error<ErrorCode>
    ): this(
        succeed = false,
        status = "Error",
        notes = listOf(error.message)
    )

    companion object {
        /**
         * Creates a successful loading order response.
         * 创建成功的装载顺序响应。
         *
         * @param orders 计算得到的装载顺序标识符 / the computed loading order identifiers
         * @param notes 可选的信息备注 / the optional informational notes
         * @return 成功的LoadingOrderResponseDTO / a successful LoadingOrderResponseDTO
        */
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