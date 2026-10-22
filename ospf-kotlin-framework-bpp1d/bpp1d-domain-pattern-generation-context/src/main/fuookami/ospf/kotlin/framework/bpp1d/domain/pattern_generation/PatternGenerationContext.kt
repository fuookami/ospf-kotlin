package fuookami.ospf.kotlin.framework.bpp1d.domain.pattern_generation

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.convertTo
import fuookami.ospf.kotlin.framework.bpp1d.domain.item.model.*

/** 有界候选搜索配置。 / Bounded pattern search configuration.
 * @property maxPatterns 最多保留候选数 / Maximum retained patterns
 * @property maxNodes 最多访问节点数 / Maximum visited nodes
 */
data class PatternGenerationConfig(val maxPatterns: Int = 10000, val maxNodes: Int = 100000)

/** 候选及完整性证明。 / Candidates and completeness certificate.
 * @property patterns 可行候选 / Feasible patterns
 * @property complete 是否穷尽所有可行组合 / Whether all feasible combinations were exhausted
 * @property visitedNodes 搜索节点数 / Visited nodes
 */
data class PatternGenerationResult<V : FloatingNumber<V>>(
    val patterns: List<PackingPattern<V>>,
    val complete: Boolean,
    val visitedNodes: Int
)

/** 可替换候选接收策略。 / Replaceable candidate acceptance policy. */
fun interface PatternPolicy<V : FloatingNumber<V>> {
    /** 接收候选。 / Accept a candidate.
     * @param pattern 候选 / Candidate
     * @return 是否接收 / Whether accepted
     */
    fun accept(pattern: PackingPattern<V>): Boolean
}

/** 降序初始解与有界组合候选生成。 / Decreasing initial packing and bounded combination generation.
 * @property policy 候选策略 / Candidate policy
 */
class PatternGenerationContext<V : FloatingNumber<V>>(
    private val policy: PatternPolicy<V> = PatternPolicy { true }
) {
    /** 枚举可行列，保留截断标记。 / Enumerate feasible columns, preserving truncation information.
     * @param problem 输入 / Input
     * @param config 搜索预算 / Search budget
     * @return 候选或失败 / Candidates or failure
     */
    fun generate(problem: PackingProblem<V>, config: PatternGenerationConfig = PatternGenerationConfig()): Ret<PatternGenerationResult<V>> {
        when (val result = problem.validate()) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        if (config.maxPatterns < 1 || config.maxNodes < 1) return Failed(ErrorCode.IllegalArgument, "候选预算必须为正 / Pattern budgets must be positive")
        val patterns = linkedMapOf<String, PackingPattern<V>>()
        var visited = 0
        var complete = true
        for (bin in problem.binTypes) {
            if (bin.inventory == UInt64.zero) continue
            val items = problem.items.filter { it.amount > UInt64.zero }.sortedWith { a, b ->
                b.length.convertTo(bin.length.unit)!!.value.compareTo(a.length.convertTo(bin.length.unit)!!.value)
            }
            val gap = problem.spacing.convertTo(bin.length.unit)!!.value
            val zero = bin.length.value.constants.zero
            val counts = linkedMapOf<String, UInt64>()
            fun search(index: Int, used: V, weight: V, nonempty: Boolean) {
                if (visited >= config.maxNodes || patterns.size >= config.maxPatterns) {
                    complete = false
                    return
                }
                visited++
                if (index == items.size) {
                    if (nonempty) {
                        val pattern = PackingPattern<V>(bin.id, counts.toMap())
                        if (policy.accept(pattern)) patterns[pattern.key] = pattern
                    }
                    return
                }
                val item = items[index]
                val length = item.length.convertTo(bin.length.unit)!!.value
                val itemWeight = item.weight.convertTo(bin.maxWeight.unit)!!.value
                var count = UInt64.zero
                var nextUsed = used
                var nextWeight = weight
                while (true) {
                    if (count > UInt64.zero) counts[item.id] = count else counts.remove(item.id)
                    search(index + 1, nextUsed, nextWeight, nonempty || count > UInt64.zero)
                    if (!complete || count == item.amount) break
                    nextUsed += length + if (nonempty || count > UInt64.zero) gap else zero
                    nextWeight += itemWeight
                    if (nextUsed > bin.length.value || nextWeight > bin.maxWeight.value) break
                    count += UInt64.one
                }
                counts.remove(item.id)
            }
            search(0, zero, bin.maxWeight.value.constants.zero, false)
        }
        return Ok(PatternGenerationResult(patterns.values.toList(), complete, visited))
    }

    /** 生成降序首次/最佳适应初始列；不把启发式结果当作完备证明。 / Generate FFD/BFD seeds without claiming completeness.
     * @param problem 输入 / Input
     * @param bestFit 是否选择剩余长度最小的已有箱 / Whether to choose the tightest existing bin
     * @return 初始列或失败 / Initial patterns or failure
     */
    fun initialPatterns(problem: PackingProblem<V>, bestFit: Boolean = false): Ret<List<PackingPattern<V>>> {
        when (val result = problem.validate()) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val patterns = linkedMapOf<String, PackingPattern<V>>()
        for (bin in problem.binTypes) {
            val gap = problem.spacing.convertTo(bin.length.unit)!!.value
            val zero = bin.length.value.constants.zero
            val bins = arrayListOf<Triple<MutableMap<String, UInt64>, V, V>>()
            for (item in problem.items.sortedWith { a, b -> b.length.convertTo(bin.length.unit)!!.value.compareTo(a.length.convertTo(bin.length.unit)!!.value) }) {
                if (item.amount > UInt64(100000)) return Failed(ErrorCode.IllegalArgument, "初始解展开预算超限 / Initial packing expansion budget exceeded")
                val length = item.length.convertTo(bin.length.unit)!!.value
                val weight = item.weight.convertTo(bin.maxWeight.unit)!!.value
                if (length > bin.length.value || weight > bin.maxWeight.value || item.amount == UInt64.zero) continue
                val singleton = PackingPattern<V>(bin.id, mapOf(item.id to UInt64.one))
                if (policy.accept(singleton)) patterns[singleton.key] = singleton
                repeat(item.amount.toULong().toInt()) {
                    val feasible = bins.indices.filter { bins[it].second + gap + length <= bin.length.value && bins[it].third + weight <= bin.maxWeight.value }
                    val index = if (bestFit) feasible.maxWithOrNull { a, b -> bins[a].second.compareTo(bins[b].second) } else feasible.firstOrNull()
                    if (index == null) bins.add(Triple(linkedMapOf(item.id to UInt64.one), length, weight))
                    else {
                        val current = bins[index]
                        current.first[item.id] = (current.first[item.id] ?: UInt64.zero) + UInt64.one
                        bins[index] = Triple(current.first, current.second + gap + length, current.third + weight)
                    }
                }
            }
            for (packed in bins) {
                val pattern = PackingPattern<V>(bin.id, packed.first.toMap())
                if (policy.accept(pattern)) patterns[pattern.key] = pattern
            }
        }
        return Ok(patterns.values.toList())
    }
}
