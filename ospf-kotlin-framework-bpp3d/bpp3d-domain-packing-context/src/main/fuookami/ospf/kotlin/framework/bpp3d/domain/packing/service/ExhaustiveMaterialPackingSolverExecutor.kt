@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.bpp3d.domain.packing.service

import kotlin.math.abs
import kotlin.math.ceil
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.MaterialKey
import fuookami.ospf.kotlin.math.algebra.number.UInt64

class ExhaustiveMaterialPackingSolverExecutor : MaterialPackingSolverExecutor {
    private data class CandidateScore(
        val objective: Double,
        val packageCount: Long,
        val slack: Long,
        val volume: Double
    )

    override suspend fun solve(request: MaterialPackingMipRequest): MaterialPackingMipResult {
        val startedAt = System.currentTimeMillis()
        val demands = request.demands.filterValues { it != UInt64.zero }
        if (demands.isEmpty()) {
            return MaterialPackingMipResult(
                status = MaterialPackingMipStatus.Optimal,
                selections = emptyMap(),
                objective = 0.0,
                gap = 0.0,
                timeMillis = System.currentTimeMillis() - startedAt,
                rawStatus = "empty_demand"
            )
        }
        if (request.candidates.isEmpty()) {
            return MaterialPackingMipResult(
                status = MaterialPackingMipStatus.Infeasible,
                selections = emptyMap(),
                objective = null,
                gap = null,
                timeMillis = System.currentTimeMillis() - startedAt,
                rawStatus = "no_candidate"
            )
        }

        require(request.objective.packageCountWeight >= 0.0) { "packageCountWeight must be non-negative." }
        require(request.objective.volumeWeight >= 0.0) { "volumeWeight must be non-negative." }
        require(request.objective.slackWeight >= 0.0) { "slackWeight must be non-negative." }

        val materials = demands.keys.toList()
        val demandedAmount = materials.map { key ->
            demands[key]?.toULong()?.toLong() ?: 0L
        }.toLongArray()
        val candidateCount = request.candidates.size
        val capacity = Array(candidateCount) { LongArray(materials.size) }
        val volume = DoubleArray(candidateCount)
        for ((candidateIndex, candidate) in request.candidates.withIndex()) {
            val programMaterials = candidate.program.materials
            for ((materialIndex, material) in materials.withIndex()) {
                capacity[candidateIndex][materialIndex] = programMaterials[material]?.toULong()?.toLong() ?: 0L
            }
            volume[candidateIndex] = candidate.program.volume.value.toDouble()
        }

        for (materialIndex in materials.indices) {
            val coverable = (0 until candidateCount).any { candidateIndex ->
                capacity[candidateIndex][materialIndex] > 0L
            }
            if (!coverable) {
                return MaterialPackingMipResult(
                    status = MaterialPackingMipStatus.Infeasible,
                    selections = emptyMap(),
                    objective = null,
                    gap = null,
                    timeMillis = System.currentTimeMillis() - startedAt,
                    rawStatus = "uncovered_material:${materials[materialIndex].no}"
                )
            }
        }

        val upperBounds = IntArray(candidateCount) { candidateIndex ->
            var bound = 0
            for (materialIndex in materials.indices) {
                val cap = capacity[candidateIndex][materialIndex]
                val demand = demandedAmount[materialIndex]
                if (cap <= 0L || demand <= 0L) {
                    continue
                }
                val required = ceil(demand.toDouble() / cap.toDouble()).toInt()
                if (required > bound) {
                    bound = required
                }
            }
            bound
        }
        if (upperBounds.all { it <= 0 }) {
            return MaterialPackingMipResult(
                status = MaterialPackingMipStatus.Infeasible,
                selections = emptyMap(),
                objective = null,
                gap = null,
                timeMillis = System.currentTimeMillis() - startedAt,
                rawStatus = "zero_upper_bound"
            )
        }

        val suffixCapacity = Array(candidateCount + 1) { LongArray(materials.size) }
        for (candidateIndex in candidateCount - 1 downTo 0) {
            for (materialIndex in materials.indices) {
                suffixCapacity[candidateIndex][materialIndex] = suffixCapacity[candidateIndex + 1][materialIndex] +
                    capacity[candidateIndex][materialIndex] * upperBounds[candidateIndex]
            }
        }

        val currentSelection = IntArray(candidateCount)
        val currentCovered = LongArray(materials.size)
        var currentPackageCount = 0L
        var currentVolume = 0.0

        var bestSelection: IntArray? = null
        var bestScore: CandidateScore? = null

        fun scoreFrom(covered: LongArray, packageCount: Long, volumeValue: Double): CandidateScore {
            var slack = 0L
            for (materialIndex in materials.indices) {
                slack += maxOf(covered[materialIndex] - demandedAmount[materialIndex], 0L)
            }
            val objective = request.objective.packageCountWeight * packageCount.toDouble() +
                request.objective.volumeWeight * volumeValue +
                request.objective.slackWeight * slack.toDouble()
            return CandidateScore(
                objective = objective,
                packageCount = packageCount,
                slack = slack,
                volume = volumeValue
            )
        }

        fun betterThanBest(candidate: CandidateScore, best: CandidateScore?): Boolean {
            if (best == null) {
                return true
            }
            val eps = 1e-9
            if (candidate.objective < best.objective - eps) {
                return true
            }
            if (abs(candidate.objective - best.objective) > eps) {
                return false
            }
            if (candidate.packageCount != best.packageCount) {
                return candidate.packageCount < best.packageCount
            }
            if (candidate.slack != best.slack) {
                return candidate.slack < best.slack
            }
            if (abs(candidate.volume - best.volume) > eps) {
                return candidate.volume < best.volume
            }
            return false
        }

        fun infeasibleByCapacity(candidateIndex: Int, covered: LongArray): Boolean {
            for (materialIndex in materials.indices) {
                if (covered[materialIndex] + suffixCapacity[candidateIndex][materialIndex] < demandedAmount[materialIndex]) {
                    return true
                }
            }
            return false
        }

        fun search(candidateIndex: Int) {
            if (infeasibleByCapacity(candidateIndex, currentCovered)) {
                return
            }
            if (candidateIndex >= candidateCount) {
                for (materialIndex in materials.indices) {
                    if (currentCovered[materialIndex] < demandedAmount[materialIndex]) {
                        return
                    }
                }
                val score = scoreFrom(
                    covered = currentCovered,
                    packageCount = currentPackageCount,
                    volumeValue = currentVolume
                )
                if (betterThanBest(score, bestScore)) {
                    bestScore = score
                    bestSelection = currentSelection.copyOf()
                }
                return
            }

            val upper = upperBounds[candidateIndex]
            for (selectedCount in 0..upper) {
                currentSelection[candidateIndex] = selectedCount
                if (selectedCount > 0) {
                    currentPackageCount += selectedCount.toLong()
                    currentVolume += volume[candidateIndex] * selectedCount.toDouble()
                    for (materialIndex in materials.indices) {
                        currentCovered[materialIndex] += capacity[candidateIndex][materialIndex] * selectedCount.toLong()
                    }
                }

                val lowerBoundObjective = request.objective.packageCountWeight * currentPackageCount.toDouble() +
                    request.objective.volumeWeight * currentVolume
                val bestObjective = bestScore?.objective
                if (bestObjective == null || lowerBoundObjective <= bestObjective + 1e-9) {
                    search(candidateIndex + 1)
                }

                if (selectedCount > 0) {
                    currentPackageCount -= selectedCount.toLong()
                    currentVolume -= volume[candidateIndex] * selectedCount.toDouble()
                    for (materialIndex in materials.indices) {
                        currentCovered[materialIndex] -= capacity[candidateIndex][materialIndex] * selectedCount.toLong()
                    }
                }
            }
            currentSelection[candidateIndex] = 0
        }

        search(0)

        val best = bestSelection ?: return MaterialPackingMipResult(
            status = MaterialPackingMipStatus.Infeasible,
            selections = emptyMap(),
            objective = null,
            gap = null,
            timeMillis = System.currentTimeMillis() - startedAt,
            rawStatus = "enumeration_infeasible"
        )
        val selectionMap = LinkedHashMap<Int, UInt64>()
        for (index in best.indices) {
            val value = best[index]
            if (value > 0) {
                selectionMap[index] = UInt64(value.toULong())
            }
        }
        return MaterialPackingMipResult(
            status = MaterialPackingMipStatus.Optimal,
            selections = selectionMap,
            objective = bestScore?.objective,
            gap = 0.0,
            timeMillis = System.currentTimeMillis() - startedAt,
            rawStatus = "enumeration_optimal"
        )
    }
}
