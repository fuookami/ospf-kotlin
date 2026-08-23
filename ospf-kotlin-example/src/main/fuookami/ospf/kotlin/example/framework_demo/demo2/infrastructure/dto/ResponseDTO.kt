package fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto

import kotlinx.serialization.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.Diagnostics

/**
 * Data transfer object for the main optimization response.
 * 主优化响应的数据传输对象。
 *
 * @property succeed 优化请求是否处理成功 / whether the optimization request was processed successfully
 * @property status 求解器状态字符串（如"Optimal"、"Error"） / the solver status string (e.g., "Optimal", "Error")
 * @property objective 目标函数值（如可用） / the objective function value, if available
 * @property assignments 货物到舱位的分配字符串列表 / the list of cargo-to-position assignment strings
 * @property notes 求解器的信息备注列表 / the list of informational notes from the solver
 * @property diagnostics 结构化诊断备注列表 / the list of structured diagnostic notes
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
         * Creates a response indicating no feasible solution was found.
         * 创建表示未找到可行解的响应。
         *
         * @param status 描述不可行性的求解器状态 / the solver status describing the infeasibility
         * @param notes 关于不可行性的信息备注 / the informational notes about the infeasibility
         * @return 表示无解的ResponseDTO / a ResponseDTO indicating no solution
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
         * @param objective 最优目标函数值 / the optimal objective function value
         * @param assignments 货物到舱位的分配字符串列表 / the list of cargo-to-position assignment strings
         * @param notes 求解器的信息备注 / the informational notes from the solver
         * @return 表示最优解的ResponseDTO / a ResponseDTO indicating an optimal solution
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