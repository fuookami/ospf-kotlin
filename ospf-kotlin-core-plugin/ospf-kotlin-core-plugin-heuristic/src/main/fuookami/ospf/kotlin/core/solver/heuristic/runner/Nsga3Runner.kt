/**
 * NSGA-III 骨架 / NSGA-III skeleton
*/
package fuookami.ospf.kotlin.core.solver.heuristic.runner

import fuookami.ospf.kotlin.utils.functional.Generator
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.solver.heuristic.*
import fuookami.ospf.kotlin.core.solver.heuristic.operator.multi_objective.Dominance
import fuookami.ospf.kotlin.core.solver.heuristic.operator.multi_objective.FastNonDominatedSort
import fuookami.ospf.kotlin.core.solver.heuristic.operator.multi_objective.ReferencePoints
import fuookami.ospf.kotlin.core.solver.heuristic.operator.update.SolutionUpdate
import fuookami.ospf.kotlin.core.solver.heuristic.operator.stopping.StoppingCriterion
import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.heuristic.operator.mutation.Mutation

/**
 * NSGA-III 骨架。 / NSGA-III skeleton.
 *
 * 与 NSGA-II 的差别只在一处：**最后一层的截断方式**。 / The only difference from NSGA-II is **how the last layer is truncated**.
 *
 * NSGA-II 按拥挤距离保留稀疏的解；当目标数达到 4 个以上时，拥挤距离几乎无法区分个体，前沿会塌缩。
 * NSGA-III 改为预先铺一组参考方向，按「哪个方向的成员最少」补位，从而在任意目标数下维持分布性。 /
 * NSGA-II keeps sparse solutions by crowding distance; once there are four or more objectives the crowding distance barely
 * discriminates and the front collapses. NSGA-III instead lays out reference directions in advance and fills the direction with
 * the fewest members, preserving spread at any number of objectives.
 *
 * 因此它继承 [Nsga2Runner]，只覆写选择步骤——循环、繁殖、档案全部复用。 /
 * It therefore extends [Nsga2Runner] and overrides only the selection step, reusing the loop, breeding, and archive.
 *
 * @param V 值类型 / Value type
 * @property divisions 参考点在每个目标上的分割数 / Divisions per objective for the reference points
*/
class Nsga3Runner<V>(
    size: Int,
    objectivesOf: (Solution<V>) -> List<Flt64>,
    dominance: Dominance,
    update: SolutionUpdate,
    mutation: Mutation,
    mutationRate: Flt64,
    entropy: Generator<Flt64>,
    stopping: StoppingCriterion,
    converter: IntoValue<V>,
    archiveAmount: Int,
    val divisions: Int = 8
) : Nsga2Runner<V>(
    size = size,
    objectivesOf = objectivesOf,
    dominance = dominance,
    update = update,
    mutation = mutation,
    mutationRate = mutationRate,
    entropy = entropy,
    stopping = stopping,
    converter = converter,
    archiveAmount = archiveAmount
) where V : RealNumber<V>, V : NumberField<V> {

    override val name: String get() = "nsga-iii"

    override fun select(
        candidates: List<Individual<List<Flt64>, V>>
    ): List<Individual<List<Flt64>, V>> {
        if (candidates.size <= size) {
            return candidates
        }

        val fronts = FastNonDominatedSort.sort(
            items = candidates,
            objectivesOf = { it.fitness },
            dominance = dominance
        )

        val selected = ArrayList<Individual<List<Flt64>, V>>(size)
        var lastFront: List<Individual<List<Flt64>, V>> = emptyList()

        for (front in fronts) {
            if (selected.size + front.size <= size) {
                selected.addAll(front)
            } else {
                lastFront = front
                break
            }
        }

        if (lastFront.isEmpty()) {
            return selected
        }

        val remaining = size - selected.size
        val referencePoints = ReferencePoints.dasDennis(
            objectiveAmount = candidates.first().fitness.size,
            divisions = divisions
        )
        if (referencePoints.isEmpty()) {
            // 参考点生成失败时退化为「按目标顺序截断」，保证仍然返回合法结果而不抛异常。
            // When the reference points cannot be generated, fall back to truncation in objective order so a valid result is
            // still returned instead of throwing.
            selected.addAll(lastFront.take(remaining))
            return selected
        }

        // 归一化：用「已选个体 + 末层候选」的逐目标极值做 min-max，使距离在量纲上可比。
        // Normalisation: min-max per objective over "already selected + last front" so distances are dimensionally comparable.
        val pool = selected + lastFront
        val dimension = candidates.first().fitness.size
        val minimum = DoubleArray(dimension) { Double.POSITIVE_INFINITY }
        val maximum = DoubleArray(dimension) { Double.NEGATIVE_INFINITY }
        for (individual in pool) {
            for (index in 0 until dimension) {
                val value = individual.fitness[index].toDouble()
                if (value < minimum[index]) {
                    minimum[index] = value
                }
                if (value > maximum[index]) {
                    maximum[index] = value
                }
            }
        }

        fun normalise(individual: Individual<List<Flt64>, V>): List<Double> {
            return List(dimension) { index ->
                val span = maximum[index] - minimum[index]
                if (span <= 1.0e-12) 0.0 else (individual.fitness[index].toDouble() - minimum[index]) / span
            }
        }

        // 统计每个参考方向当前已有的成员数。
        // Count the current members of every reference direction.
        val nicheCount = IntArray(referencePoints.size)
        for (individual in selected) {
            var nearest = 0
            var bestDistance = Double.POSITIVE_INFINITY
            val objectives = normalise(individual)
            for (index in referencePoints.indices) {
                val distance = ReferencePoints.perpendicularDistance(objectives, referencePoints[index])
                if (distance < bestDistance) {
                    bestDistance = distance
                    nearest = index
                }
            }
            nicheCount[nearest] += 1
        }

        // 逐次补位：每次找成员最少的参考方向，把该方向上距离最近的末层个体纳入。
        // Fill one slot at a time: pick the reference direction with the fewest members and take the closest last-front individual.
        val available = lastFront.toMutableList()
        repeat(remaining) {
            if (available.isEmpty()) {
                return@repeat
            }

            var targetReference = 0
            var fewest = Int.MAX_VALUE
            for (index in nicheCount.indices) {
                if (nicheCount[index] < fewest && available.any { candidate ->
                        nearestReference(normalise(candidate), referencePoints) == index
                    }
                ) {
                    fewest = nicheCount[index]
                    targetReference = index
                }
            }

            var chosenIndex = -1
            var bestDistance = Double.POSITIVE_INFINITY
            for (index in available.indices) {
                val normalized = normalise(available[index])
                if (nearestReference(normalized, referencePoints) != targetReference) {
                    continue
                }
                val distance = ReferencePoints.perpendicularDistance(normalized, referencePoints[targetReference])
                if (distance < bestDistance) {
                    bestDistance = distance
                    chosenIndex = index
                }
            }

            if (chosenIndex < 0) {
                chosenIndex = 0
            }
            selected.add(available.removeAt(chosenIndex))
            nicheCount[targetReference] += 1
        }

        return selected
    }

    /** 找到与给定目标向量最近的参考方向 / Find the nearest reference direction for the given objective vector */
    private fun nearestReference(objectives: List<Double>, referencePoints: List<List<Double>>): Int {
        var nearest = 0
        var bestDistance = Double.POSITIVE_INFINITY
        for (index in referencePoints.indices) {
            val distance = ReferencePoints.perpendicularDistance(objectives, referencePoints[index])
            if (distance < bestDistance) {
                bestDistance = distance
                nearest = index
            }
        }
        return nearest
    }
}
