package fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto

import kotlinx.serialization.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.Diagnostics

/**
 * Data transfer object for the main optimization response.
 * 主优化响应的数据传输对象。
 *
 * @property succeed whether the optimization request was processed successfully / 优化请求是否处理成功
 * @property status the solver status string (e.g., "Optimal", "Error") / 求解器状态字符串（如"Optimal"、"Error"）
 * @property objective the objective function value, if available / 目标函数值（如可用）
 * @property assignments the list of cargo-to-position assignment strings / 货物到舱位的分配字符串列表
 * @property notes the list of informational notes from the solver / 求解器的信息备注列表
 * @property diagnostics the list of structured diagnostic notes / 结构化诊断备注列表
*/
@Serializable
data class ResponseDTO(
    val succeed: Boolean,
    val status: String = "",
    val objective: Double? = null,
    val assignments: List<String> = emptyList(),
    val notes: List<String> = emptyList(),
    val diagnostics: List<DiagnosticNote> = emptyList()
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
    ): this(
        succeed = false,
        status = "Error",
        notes = listOf(error.message)
    )

    companion object {
        /**
         * Creates a response indicating no feasible solution was found.
         * 创建表示未找到可行解的响应。
         *
         * @param status the solver status describing the infeasibility / 描述不可行性的求解器状态
         * @param notes the informational notes about the infeasibility / 关于不可行性的信息备注
         * @return a ResponseDTO indicating no solution / 表示无解的ResponseDTO
        */
        fun noSolution(status: String, notes: List<String>): ResponseDTO = ResponseDTO(
            succeed = false,
            status = status,
            notes = notes,
            diagnostics = Diagnostics.buildStructured(notes)
        )

        /**
         * Creates a response for an optimal solution.
         * 创建最优解的响应。
         *
         * @param objective the optimal objective function value / 最优目标函数值
         * @param assignments the list of cargo-to-position assignment strings / 货物到舱位的分配字符串列表
         * @param notes the informational notes from the solver / 求解器的信息备注
         * @return a ResponseDTO indicating an optimal solution / 表示最优解的ResponseDTO
        */
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