package fuookami.ospf.kotlin.core.solver.heuristic.operator.neighborhood

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.solver.heuristic.*

/** 不丢失元素的部分排列 / Partial permutation without lost elements.
 * @property retained 保留的有序元素 / Retained ordered elements
 * @property removed 待重新插入元素 / Elements awaiting reinsertion
 */
data class RuinResult<T>(val retained: List<T>, val removed: List<T>)

/** 破坏规则 / Ruin rule. */
interface Ruin<T> {
    /** 选择移除元素 / Choose elements to remove.
     * @param solution 完整排列，元素须唯一 / Complete permutation with unique elements
     * @param amount 移除数量 / Removal count
     * @param random 独立随机流 / Independent random stream
     * @return 部分排列 / Partial permutation
     */
    fun remove(solution: List<T>, amount: Int, random: SearchRandom): Ret<RuinResult<T>>
}

private fun <T> checkRuin(solution: List<T>, amount: Int): Try {
    return if (amount !in 0..solution.size || solution.distinct().size != solution.size) searchInvalid("ruin requires unique elements and a valid removal count") else ok
}

private fun <T> partition(solution: List<T>, indices: Set<Int>): RuinResult<T> = RuinResult(
    retained = solution.filterIndexed { index, _ -> index !in indices },
    removed = solution.filterIndexed { index, _ -> index in indices }
)

/** 均匀无放回破坏 / Uniform ruin without replacement. */
class RandomRuin<T> : Ruin<T> {
    override fun remove(solution: List<T>, amount: Int, random: SearchRandom): Ret<RuinResult<T>> {
        val valid = checkRuin(solution, amount)
        if (valid !is Ok) return searchFailure(valid)
        val indices = solution.indices.toMutableList()
        repeat(amount) { offset ->
            val selected = offset + (random().toDouble() * (indices.size - offset)).toInt().coerceIn(0, indices.size - offset - 1)
            val old = indices[offset]
            indices[offset] = indices[selected]
            indices[selected] = old
        }
        return Ok(partition(solution, indices.take(amount).toSet()))
    }
}

/** 按业务移除收益选择代价最高元素 / Remove highest-cost elements using a domain removal benefit.
 * @property removalCost 移除收益，越大越先移除 / Removal benefit, larger is removed first
 */
class WorstCostRuin<T>(val removalCost: (List<T>, Int) -> Ret<Flt64>) : Ruin<T> {
    override fun remove(solution: List<T>, amount: Int, random: SearchRandom): Ret<RuinResult<T>> {
        val valid = checkRuin(solution, amount)
        if (valid !is Ok) return searchFailure(valid)
        val costs = ArrayList<Pair<Int, Double>>()
        for (index in solution.indices) {
            val value = searchBoundary { removalCost(solution.toList(), index) }.then { it }
            if (value !is Ok) return searchFailure(value)
            if (!value.value.toDouble().isFinite()) return searchInvalid("non-finite removal cost")
            costs.add(index to value.value.toDouble())
        }
        return Ok(partition(solution, costs.sortedByDescending { it.second }.take(amount).map { it.first }.toSet()))
    }
}

/** 从随机种子扩展相关簇的 Shaw 破坏 / Shaw removal expanding a related cluster from a random seed.
 * @property distance 业务相关距离，越小越相关 / Domain relatedness distance, smaller means more related
 */
class RelatednessRuin<T>(val distance: (T, T) -> Ret<Flt64>) : Ruin<T> {
    override fun remove(solution: List<T>, amount: Int, random: SearchRandom): Ret<RuinResult<T>> {
        val valid = checkRuin(solution, amount)
        if (valid !is Ok) return searchFailure(valid)
        if (amount == 0) return Ok(RuinResult(solution.toList(), emptyList()))
        val selected = linkedSetOf((random().toDouble() * solution.size).toInt().coerceIn(solution.indices))
        while (selected.size < amount) {
            val pivot = selected.elementAt((random().toDouble() * selected.size).toInt().coerceIn(0, selected.size - 1))
            var nearest = -1
            var best = Double.POSITIVE_INFINITY
            for (index in solution.indices.filter { it !in selected }) {
                val value = searchBoundary { distance(solution[pivot], solution[index]) }.then { it }
                if (value !is Ok) return searchFailure(value)
                val score = value.value.toDouble()
                if (!score.isFinite() || score < 0) return searchInvalid("relatedness must be a finite nonnegative distance")
                if (score < best) {
                    nearest = index
                    best = score
                }
            }
            selected.add(nearest)
        }
        return Ok(partition(solution, selected))
    }
}

/** 部分解的插入代价估计，不是完整目标回调；null 表示不可插入 / Partial-solution insertion estimate, not a full objective callback; null forbids insertion. */
fun interface InsertionCost<T> {
    /** 评估插入位置 / Estimate an insertion position.
     * @param partial 部分排列 / Partial permutation
     * @param item 插入元素 / Inserted element
     * @param position 插入索引 / Insertion index
     * @return 增量代价，null 为不可行 / Incremental cost, null when infeasible
     */
    fun cost(partial: List<T>, item: T, position: Int): Ret<Flt64?>
}

/** 遗憾 K 插入，不可行项不会静默丢失 / Regret-K insertion, never silently dropping infeasible items.
 * @property insertionCost 业务增量代价 / Domain incremental cost
 * @property k 前 K 佳位置；1 为全局贪心 / Top K positions; 1 means global greedy
 */
open class RegretKInsertRepair<T>(val insertionCost: InsertionCost<T>, val k: Int = 2) {
    /** 完成部分排列 / Complete a partial permutation.
     * @param ruined 破坏结果 / Ruin result
     * @return 完整排列或明确不可修复错误 / Complete permutation or explicit irreparability error
     */
    fun repair(ruined: RuinResult<T>): Ret<List<T>> {
        if (k <= 0 || (ruined.retained + ruined.removed).distinct().size != ruined.retained.size + ruined.removed.size) return searchInvalid("invalid repair definition/elements")
        val partial = ruined.retained.toMutableList()
        val remaining = ruined.removed.toMutableList()
        while (remaining.isNotEmpty()) {
            var chosenItem = -1
            var chosenPosition = -1
            var chosenRegret = Double.NEGATIVE_INFINITY
            var chosenCost = Double.POSITIVE_INFINITY
            var chosenOptions = Int.MAX_VALUE
            for ((itemIndex, item) in remaining.withIndex()) {
                val options = ArrayList<Pair<Int, Double>>()
                for (position in 0..partial.size) {
                    val outcome = searchBoundary { insertionCost.cost(partial.toList(), item, position) }.then { it }
                    if (outcome !is Ok) return searchFailure(outcome)
                    val value = outcome.value?.toDouble() ?: continue
                    if (!value.isFinite()) return searchInvalid("non-finite insertion estimate")
                    options.add(position to value)
                }
                if (options.isEmpty()) continue
                options.sortBy { it.second }
                val regret = options.take(k).map { it.second - options.first().second }.sum()
                val urgent = if (k > 1) options.size.coerceAtMost(k) else k
                if (urgent < chosenOptions || urgent == chosenOptions && (regret > chosenRegret || regret == chosenRegret && options.first().second < chosenCost)) {
                    chosenItem = itemIndex
                    chosenPosition = options.first().first
                    chosenRegret = regret
                    chosenCost = options.first().second
                    chosenOptions = urgent
                }
            }
            if (chosenItem < 0) return searchInvalid("unrepairable partial solution: no feasible insertion for remaining items")
            partial.add(chosenPosition, remaining.removeAt(chosenItem))
        }
        return Ok(partial.toList())
    }
}

/** 全局最低代价插入 / Globally cheapest insertion.
 * @param insertionCost 业务增量代价 / Domain incremental cost
 */
class GreedyInsertRepair<T>(insertionCost: InsertionCost<T>) : RegretKInsertRepair<T>(insertionCost, 1)

/** 可作为一个高层动作的破坏修复流水线 / Ruin/recreate pipeline usable as one high-level action.
 * @property ruin 破坏策略 / Ruin strategy
 * @property repair 修复策略 / Repair strategy
 */
class RuinRecreateOperator<T>(val ruin: Ruin<T>, val repair: RegretKInsertRepair<T>) {
    /** 生成完整邻居，调用方随后经 SearchProblem.evaluate 统一计费 / Generate a complete neighbor; the caller then uses metered SearchProblem.evaluate.
     * @param solution 完整排列 / Complete permutation
     * @param amount 移除数量 / Removal count
     * @param random 独立随机流 / Independent random stream
     * @return 完整排列 / Complete permutation
     */
    operator fun invoke(solution: List<T>, amount: Int, random: SearchRandom): Ret<List<T>> {
        return ruin.remove(solution, amount, random).then { repair.repair(it) }
    }
}
