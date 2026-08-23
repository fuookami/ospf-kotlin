package fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*

/**
 * Maps application boundary failures to stable public responses.
 * 将应用层边界失败映射为稳定的公开响应。
 */
internal fun unsupportedAircraftResponse(
    request: RequestDTO,
    path: String,
    pathName: String
): Pair<ResponseDTO, RenderDTO?> {
    val note = "不支持的${pathName}机型：${request.aircraftType} / Unsupported aircraft type for $path path: ${request.aircraftType}"
    return ResponseDTO.noSolution("UnsupportedAircraft", listOf(note)) to null
}

/**
 * Converts solver infeasibility into the public no-solution contract.
 * 将求解器不可行状态转换为公开的无解契约。
 */
internal fun solverFailureResponse(
    request: RequestDTO,
    notes: List<String>,
    error: Error<ErrorCode>
): Pair<ResponseDTO, RenderDTO?> {
    if (error.code == ErrorCode.ORModelInfeasible || error.code == ErrorCode.ORModelInfeasibleOrUnbounded) {
        val diagnosticNotes = notes + "求解器未返回可行解，已映射为 NoSolution：${error.code} / Solver returned no feasible solution and was mapped to NoSolution: ${error.code}"
        return ResponseDTO.noSolution("NoSolution", diagnosticNotes) to null
    }
    return ResponseDTO(request, error) to null
}

/** 为成功响应补充求解编排阶段产生的诊断信息 / Adds diagnostics produced during solve orchestration to a successful response. */
internal fun ResponseDTO.withSolverNotes(notes: List<String>): ResponseDTO {
    val combinedNotes = this.notes + notes
    return copy(
        notes = combinedNotes,
        diagnostics = Diagnostics.buildStructured(combinedNotes)
    )
}
