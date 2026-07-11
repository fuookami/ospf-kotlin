package fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure

import kotlin.math.abs
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.RequestDTO

/**
 * Feasibility diagnostics utility for detecting infeasible or near-critical constraint conditions in the stowage problem.
 * 可行性诊断工具，用于检测配载问题中不可行或接近临界的约束条件。
*/
object FeasibilityDiagnostics {
    private const val CRITICAL_RATIO = 0.98
    private const val EPS = 1e-6

    /**
     * Appends core feasibility diagnostic notes for the given request, checking envelope ranges, payload bounds, and cargo-position compatibility.
     * 为给定请求追加核心可行性诊断信息，检查包络范围、业载边界和货物-舱位兼容性。
     *
     * @param request The request DTO containing problem input data. / 包含问题输入数据的请求 DTO
     * @param notes Mutable list for collecting diagnostic notes. / 收集诊断信息的可变列表
    */
    fun appendCoreFeasibilityDiagnostics(request: RequestDTO, notes: MutableList<String>) {
        val totalCapacity = request.positions.sumOf { it.maxWeight }
        val totalCargoWeight = request.cargos.sumOf { it.weight }
        val minPayloadRequired = minOf(request.payloadUpperBound, totalCargoWeight) * request.minPayloadRatio

        if (request.envelopeLongitudinalMomentMin > request.envelopeLongitudinalMomentMax) {
            Diagnostics.pushGroupedNote(
                notes, Diagnostics.LEVEL_DIAGNOSTIC, Diagnostics.GROUP_AIRWORTHINESS,
                Diagnostics.CODE_ENVELOPE_RANGE_INVALID,
                "infeasible: envelope_longitudinal_moment_min > envelope_longitudinal_moment_max"
            )
        }
        if (request.payloadUpperBound < 0.0) {
            Diagnostics.pushGroupedNote(
                notes, Diagnostics.LEVEL_DIAGNOSTIC, Diagnostics.GROUP_PAYLOAD,
                Diagnostics.CODE_PAYLOAD_UPPER_NEGATIVE,
                "infeasible: payload_upper_bound < 0"
            )
        }
        if (request.minPayloadRatio < 0.0 || request.minPayloadRatio > 1.0) {
            Diagnostics.pushGroupedNote(
                notes, Diagnostics.LEVEL_DIAGNOSTIC, Diagnostics.GROUP_PAYLOAD,
                Diagnostics.CODE_MIN_PAYLOAD_RATIO_OUT_OF_RANGE,
                "infeasible: min_payload_ratio must be within [0, 1]"
            )
        }
        if (minPayloadRequired > request.payloadUpperBound) {
            Diagnostics.pushGroupedNote(
                notes, Diagnostics.LEVEL_DIAGNOSTIC, Diagnostics.GROUP_PAYLOAD,
                Diagnostics.CODE_MIN_PAYLOAD_GT_UPPER,
                "infeasible: min payload requirement exceeds payload upper bound"
            )
        }
        if (minPayloadRequired > totalCapacity) {
            Diagnostics.pushGroupedNote(
                notes, Diagnostics.LEVEL_DIAGNOSTIC, Diagnostics.GROUP_AIRWORTHINESS,
                Diagnostics.CODE_MIN_PAYLOAD_GT_TOTAL_CAPACITY,
                "infeasible: min payload requirement exceeds total position capacity"
            )
        }
        for (cargo in request.cargos) {
            val canFit = request.positions.any { it.maxWeight + EPS >= cargo.weight }
            if (!canFit) {
                Diagnostics.pushGroupedNote(
                    notes, Diagnostics.LEVEL_DIAGNOSTIC, Diagnostics.GROUP_AIRWORTHINESS,
                    Diagnostics.CODE_CARGO_EXCEEDS_ALL_POSITIONS,
                    "infeasible: cargo ${cargo.name} (${String.format("%.2f", cargo.weight)}) exceeds every position max_weight"
                )
            }
        }
    }

    /**
     * Appends critical constraint notes by evaluating the solution against capacity, payload, envelope, lateral imbalance, and redundancy constraints.
     * 通过评估解与容量、业载、包络、横向不平衡和余度约束的关系，追加关键约束诊断信息。
     *
     * @param request The request DTO containing problem input data. / 包含问题输入数据的请求 DTO
     * @param xIdx Index mapping from cargo-position pairs to solution variable indices. / 从货物-舱位对到解变量索引的映射
     * @param solution The solution variable values as a double array. / 解变量值数组
     * @param notes Mutable list for collecting diagnostic notes. / 收集诊断信息的可变列表
    */
    fun appendCriticalConstraintNotes(
        request: RequestDTO,
        xIdx: List<List<Int>>,
        solution: DoubleArray,
        notes: MutableList<String>
    ) {
        val positionLoads = DoubleArray(request.positions.size)
        for (c in request.cargos.indices) {
            for (p in request.positions.indices) {
                val value = solution.getOrElse(xIdx[c][p]) { 0.0 }
                positionLoads[p] += value * request.cargos[c].weight
            }
        }

        for (p in request.positions.indices) {
            val capacity = request.positions[p].maxWeight
            if (capacity > EPS) {
                val ratio = positionLoads[p] / capacity
                if (ratio + EPS >= CRITICAL_RATIO) {
                    Diagnostics.pushGroupedNote(
                        notes, Diagnostics.LEVEL_CRITICAL, Diagnostics.GROUP_AIRWORTHINESS,
                        Diagnostics.CODE_CAPACITY_UTILIZATION_HIGH,
                        "airworthiness_capacity_${request.positions[p].name} utilization ${String.format("%.2f", ratio * 100)}%"
                    )
                }
            }
        }

        val totalPayload = positionLoads.sum()
        val totalCargoWeight = request.cargos.sumOf { it.weight }
        val minPayload = minOf(request.payloadUpperBound, totalCargoWeight) * request.minPayloadRatio
        if (request.payloadUpperBound > EPS && totalPayload / request.payloadUpperBound + EPS >= CRITICAL_RATIO) {
            Diagnostics.pushGroupedNote(
                notes, Diagnostics.LEVEL_CRITICAL, Diagnostics.GROUP_PAYLOAD,
                Diagnostics.CODE_PAYLOAD_UPPER_UTILIZATION_HIGH,
                "airworthiness_payload_upper utilization ${String.format("%.2f", totalPayload / request.payloadUpperBound * 100)}%"
            )
        }
        if (minPayload > EPS && totalPayload / minPayload <= 1.0 + (1.0 - CRITICAL_RATIO)) {
            Diagnostics.pushGroupedNote(
                notes, Diagnostics.LEVEL_CRITICAL, Diagnostics.GROUP_PAYLOAD,
                Diagnostics.CODE_PAYLOAD_LOWER_CLOSE,
                "airworthiness_payload_lower payload ${String.format("%.2f", totalPayload)} close to minimum ${String.format("%.2f", minPayload)}"
            )
        }

        var longitudinalMoment = 0.0
        var lateralMoment = 0.0
        for (c in request.cargos.indices) {
            for (p in request.positions.indices) {
                val value = solution.getOrElse(xIdx[c][p]) { 0.0 }
                val weight = value * request.cargos[c].weight
                longitudinalMoment += weight * request.positions[p].longitudinalArm
                lateralMoment += weight * request.positions[p].lateralArm
            }
        }

        val upperGap = request.envelopeLongitudinalMomentMax - longitudinalMoment
        val lowerGap = longitudinalMoment - request.envelopeLongitudinalMomentMin
        val envelopeSpan = request.envelopeLongitudinalMomentMax - request.envelopeLongitudinalMomentMin
        if (envelopeSpan > EPS) {
            if (upperGap / envelopeSpan <= (1.0 - CRITICAL_RATIO) + EPS) {
                Diagnostics.pushGroupedNote(
                    notes, Diagnostics.LEVEL_CRITICAL, Diagnostics.GROUP_AIRWORTHINESS,
                    Diagnostics.CODE_ENVELOPE_LONGITUDINAL_MAX_CLOSE,
                    "airworthiness_envelope_longitudinal_max close (${String.format("%.3f", longitudinalMoment)})"
                )
            }
            if (lowerGap / envelopeSpan <= (1.0 - CRITICAL_RATIO) + EPS) {
                Diagnostics.pushGroupedNote(
                    notes, Diagnostics.LEVEL_CRITICAL, Diagnostics.GROUP_AIRWORTHINESS,
                    Diagnostics.CODE_ENVELOPE_LONGITUDINAL_MIN_CLOSE,
                    "airworthiness_envelope_longitudinal_min close (${String.format("%.3f", longitudinalMoment)})"
                )
            }
        }

        if (request.maxLateralImbalance > EPS && abs(lateralMoment) / request.maxLateralImbalance + EPS >= CRITICAL_RATIO) {
            Diagnostics.pushGroupedNote(
                notes, Diagnostics.LEVEL_CRITICAL, Diagnostics.GROUP_MAC_OPTIMIZATION,
                Diagnostics.CODE_LATERAL_IMBALANCE_CLOSE,
                "mac_lateral imbalance ${String.format("%.3f", abs(lateralMoment))} close to limit ${String.format("%.3f", request.maxLateralImbalance)}"
            )
        }

        val destinations = request.cargos.map { it.destination }.toSortedSet()
        for (destination in destinations) {
            val destinationCargoIndices = request.cargos.indices.filter { request.cargos[it].destination == destination }
            if (destinationCargoIndices.size < 3) continue
            val maxOnSinglePosition = (destinationCargoIndices.size - 1).toDouble()
            if (maxOnSinglePosition <= EPS) continue
            for (p in request.positions.indices) {
                val loadedCount = destinationCargoIndices.sumOf { c ->
                    solution.getOrElse(xIdx[c][p]) { 0.0 }
                }
                val ratio = loadedCount / maxOnSinglePosition
                if (ratio + EPS >= CRITICAL_RATIO) {
                    Diagnostics.pushGroupedNote(
                        notes, Diagnostics.LEVEL_CRITICAL, Diagnostics.GROUP_REDUNDANCY,
                        Diagnostics.CODE_REDUNDANCY_DESTINATION_CONCENTRATION_HIGH,
                        "redundancy_destination_$destination concentration ${String.format("%.2f", ratio * 100)}% at ${request.positions[p].name}"
                    )
                }
            }
        }
    }
}
