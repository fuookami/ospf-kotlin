package fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure

import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.DiagnosticNote

object Diagnostics {
    const val LEVEL_DIAGNOSTIC = "diagnostic"
    const val LEVEL_CRITICAL = "critical"

    const val GROUP_AIRWORTHINESS = "airworthiness"
    const val GROUP_PAYLOAD = "payload"
    const val GROUP_MAC_OPTIMIZATION = "mac_optimization"
    const val GROUP_REDUNDANCY = "redundancy"
    const val GROUP_SOLVER = "solver"

    const val CODE_ENVELOPE_RANGE_INVALID = "envelope_range_invalid"
    const val CODE_PAYLOAD_UPPER_NEGATIVE = "payload_upper_negative"
    const val CODE_MIN_PAYLOAD_RATIO_OUT_OF_RANGE = "min_payload_ratio_out_of_range"
    const val CODE_MIN_PAYLOAD_GT_UPPER = "min_payload_gt_upper"
    const val CODE_MIN_PAYLOAD_GT_TOTAL_CAPACITY = "min_payload_gt_total_capacity"
    const val CODE_CARGO_EXCEEDS_ALL_POSITIONS = "cargo_exceeds_all_positions"

    const val CODE_CAPACITY_UTILIZATION_HIGH = "capacity_utilization_high"
    const val CODE_PAYLOAD_UPPER_UTILIZATION_HIGH = "payload_upper_utilization_high"
    const val CODE_PAYLOAD_LOWER_CLOSE = "payload_lower_close"
    const val CODE_ENVELOPE_LONGITUDINAL_MAX_CLOSE = "envelope_longitudinal_max_close"
    const val CODE_ENVELOPE_LONGITUDINAL_MIN_CLOSE = "envelope_longitudinal_min_close"
    const val CODE_LATERAL_IMBALANCE_CLOSE = "lateral_imbalance_close"
    const val CODE_REDUNDANCY_DESTINATION_CONCENTRATION_HIGH = "destination_concentration_high"

    const val CODE_BENDERS_ITERATIONS = "benders_iterations"
    const val CODE_BENDERS_GAP = "benders_gap"
    const val CODE_BENDERS_TIME_MS = "benders_time_ms"
    const val CODE_BENDERS_ADAPTIVE_EFFECTIVE = "benders_adaptive_effective"
    const val CODE_BENDERS_PROBLEM_SIZE_BINARY_VARIABLES = "benders_problem_size_binary_variables"
    const val CODE_BENDERS_GAP_GUARD_EXCEEDED = "benders_gap_guard_exceeded"
    const val CODE_BENDERS_TIME_GUARD_EXCEEDED = "benders_time_guard_exceeded"
    const val CODE_BENDERS_PROGRESS_GUARD_TRIGGERED = "benders_progress_guard_triggered"
    const val CODE_BENDERS_CUT_EFFICIENCY_LOW = "benders_cut_efficiency_low"
    const val CODE_BENDERS_TRAJECTORY_WEAK = "benders_trajectory_weak"
    const val CODE_BENDERS_QUALITY_GUARD_EFFECTIVE = "benders_quality_guard_effective"
    const val CODE_BENDERS_QUALITY_SCORE = "benders_quality_score"
    const val CODE_BENDERS_QUALITY_ACTION = "benders_quality_action"
    const val CODE_BENDERS_FAILED = "benders_failed"
    const val CODE_SOLVER_PATH = "solver_path"

    fun pushGroupedNote(
        notes: MutableList<String>,
        level: String,
        group: String,
        code: String,
        message: String
    ) {
        notes.add("$level|group=$group|code=$code|msg=$message")
    }

    fun buildStructured(notes: List<String>): List<DiagnosticNote> {
        return notes.map { note -> parseGroupedNote(note) ?: DiagnosticNote(
            level = LEVEL_DIAGNOSTIC,
            message = note
        ) }
    }

    private fun parseGroupedNote(note: String): DiagnosticNote? {
        val segments = note.split("|")
        val level = segments.firstOrNull()?.trim()?.ifEmpty { return null } ?: return null

        var group: String? = null
        var code: String? = null
        var message: String? = null
        for (segment in segments.drop(1)) {
            val kv = segment.split("=", limit = 2)
            val key = kv.getOrElse(0) { "" }.trim()
            val value = kv.getOrElse(1) { "" }.trim()
            when (key) {
                "group" -> group = value
                "code" -> code = value
                "msg" -> message = value
            }
        }

        return message?.let {
            DiagnosticNote(level = level, group = group, code = code, message = it)
        }
    }
}