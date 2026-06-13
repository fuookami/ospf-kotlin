@file:Suppress("DEPRECATION")

/**
 * 穷举物料装箱求解器执行器。
 * Exhaustive material packing solver executor.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.packing.service

import fuookami.ospf.kotlin.math.algebra.number.FltX
import kotlin.math.abs
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.MaterialKey
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model.materialPackingScalar
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model.materialPackingZero
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64

class ExhaustiveMaterialPackingSolverExecutor : MaterialPackingSolverExecutor {
    private data class CandidateScore(
        val objective: FltX,
        val packageCount: Long,
        val slack: Long,
        val volume: FltX
    )

    override suspend fun <V : FloatingNumber<V>> solve(request: MaterialPackingMipRequest<V>): MaterialPackingMipResult {
        val startedAt = System.currentTimeMillis()
        val demands = request.demands.filterValues { it != UInt64.zero }
        if (demands.isEmpty()) {
            return MaterialPackingMipResult(
                status = MaterialPackingMipStatus.Optimal,
                selections = emptyMap(),
                objective = materialPackingZero(),
                gap = materialPackingZero(),
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

        require(request.objective.packageCountWeight.toDouble() >= 0.0) { "packageCountWeight must be non-negative." }
        require(request.objective.volumeWeight.toDouble() >= 0.0) { "volumeWeight must be non-negative." }
        require(request.objective.slackWeight.toDouble() >= 0.0) { "slackWeight must be non-negative." }

        val materials = demands.keys.toList()
        val demandedAmount = materials.map { key ->
            demands[key]?.toULong()?.toLong() ?: 0L
        }.toLongArray()
        val candidateCount = request.candidates.size
        val capacity = Array(candidateCount) { LongArray(materials.size) }
        val volume = Array(candidateCount) { materialPackingZero() }
        for ((candidateIndex, candidate) in request.candidates.withIndex()) {
            for ((materialIndex, material) in materials.withIndex()) {
                capacity[candidateIndex][materialIndex] = candidate.program.materialAmount(material).toULong().toLong()
            }
            volume[candidateIndex] = materialPackingScalar(candidate.program.volume.value.toString().toDouble())
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
                val required = ((demand + cap - 1L) / cap).toInt()
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
        var currentVolume = materialPackingZero()

        var bestSelection: IntArray? = null
        var bestScore: CandidateScore? = null

        fun scoreFrom(covered: LongArray, packageCount: Long, volumeValue: FltX): CandidateScore {
            var slack = 0L
            for (materialIndex in materials.indices) {
                slack += maxOf(covered[materialIndex] - demandedAmount[materialIndex], 0L)
            }
            val objective = request.objective.packageCountWeight * materialPackingScalar(packageCount.toDouble()) +
                request.objective.volumeWeight * volumeValue +
                request.objective.slackWeight * materialPackingScalar(slack.toDouble())
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
            val candidateObjective = candidate.objective.toDouble()
            val bestObjective = best.objective.toDouble()
            if (candidateObjective < bestObjective - eps) {
                return true
            }
            if (abs(candidateObjective - bestObjective) > eps) {
                return false
            }
            if (candidate.packageCount != best.packageCount) {
                return candidate.packageCount < best.packageCount
            }
            if (candidate.slack != best.slack) {
                return candidate.slack < best.slack
            }
            val candidateVolume = candidate.volume.toDouble()
            val bestVolume = best.volume.toDouble()
            if (abs(candidateVolume - bestVolume) > eps) {
                return candidateVolume < bestVolume
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
                    currentVolume += volume[candidateIndex] * materialPackingScalar(selectedCount.toDouble())
                    for (materialIndex in materials.indices) {
                        currentCovered[materialIndex] += capacity[candidateIndex][materialIndex] * selectedCount.toLong()
                    }
                }

                val lowerBoundObjective = request.objective.packageCountWeight * materialPackingScalar(currentPackageCount.toDouble()) +
                    request.objective.volumeWeight * currentVolume
                val bestObjective = bestScore?.objective
                if (bestObjective == null || lowerBoundObjective.toDouble() <= bestObjective.toDouble() + 1e-9) {
                    search(candidateIndex + 1)
                }

                if (selectedCount > 0) {
                    currentPackageCount -= selectedCount.toLong()
                    currentVolume -= volume[candidateIndex] * materialPackingScalar(selectedCount.toDouble())
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
            gap = materialPackingZero(),
            timeMillis = System.currentTimeMillis() - startedAt,
            rawStatus = "enumeration_optimal"
        )
    }
}

