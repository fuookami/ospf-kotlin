package fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto

import kotlinx.serialization.*

/**
 * A diagnostic note capturing solver or model feedback.
 * 捕获求解器或模型反馈的诊断备注。
 *
 * @property level the severity level of the diagnostic note / 诊断备注的严重级别
 * @property group the optional grouping category for the note / 备注的可选分组类别
 * @property code the optional diagnostic code identifier / 可选的诊断代码标识符
 * @property message the human-readable diagnostic message / 人类可读的诊断消息
 */
@Serializable
data class DiagnosticNote(
    val level: String,
    val group: String? = null,
    val code: String? = null,
    val message: String
)