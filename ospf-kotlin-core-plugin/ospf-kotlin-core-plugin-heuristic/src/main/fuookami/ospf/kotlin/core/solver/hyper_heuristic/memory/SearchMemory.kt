package fuookami.ospf.kotlin.core.solver.hyper_heuristic.memory

import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.solver.heuristic.*

/** 工作解接受规则 / Working-solution acceptance rule. */
fun interface AcceptancePolicy<O, V> where V : RealNumber<V>, V : NumberField<V> {
    /** 判断是否接收，不能回滚算法私有状态 / Decide acceptance without rolling back private algorithm state.
     * @param problem 问题语义 / Problem semantics
     * @param working 当前工作解 / Current working solutions
     * @param candidate 候选 / Candidate
     * @return 是否接受 / Whether accepted
     */
    fun accept(
        problem: SearchProblem<O, V>,
        working: List<Individual<O, V>>,
        candidate: Individual<O, V>
    ): Boolean
}

/** 默认只接受不劣解，多目标拒绝被支配解 / Accept non-worsening solutions; reject dominated multiobjective candidates. */
class NotWorseAcceptancePolicy<O, V> : AcceptancePolicy<O, V> where V : RealNumber<V>, V : NumberField<V> {
    override fun accept(
        problem: SearchProblem<O, V>,
        working: List<Individual<O, V>>,
        candidate: Individual<O, V>
    ): Boolean {
        return working.none { problem.dominates(it.fitness, candidate.fitness) }
    }
}

/** 解交换规则 / Solution exchange rule. */
fun interface ExchangePolicy<O, V> where V : RealNumber<V>, V : NumberField<V> {
    /** 选择待导入解 / Choose solutions to import.
     * @param decision 已完成决策序号 / Completed decision sequence
     * @param target 目标会话 ID / Target session ID
     * @param working 已接受工作解 / Accepted working solutions
     * @return 待复制及重评估的解 / Solutions to copy and re-evaluate
     */
    fun select(
        decision: Long,
        target: String,
        working: List<Individual<O, V>>
    ): List<Individual<O, V>>
}

/** 有界档案及工作解集，只有协调器可修改 / Bounded archive and working set, mutated only by the coordinator.
 * @property problem 目标语义 / Objective semantics
 * @property capacity 档案容量 / Archive capacity
 * @property workingCapacity 工作集容量 / Working-set capacity
 * @property acceptance 接受策略 / Acceptance policy
 * @property trim 多目标裁剪策略 / Multiobjective truncation policy
 */
class SearchMemory<O, V>(
    private val problem: SearchProblem<O, V>,
    private val capacity: Int,
    private val workingCapacity: Int,
    private val acceptance: AcceptancePolicy<O, V>,
    private val trim: (List<Individual<O, V>>, Int) -> List<Individual<O, V>> = { entries, amount -> entries.take(amount) }
) where V : RealNumber<V>, V : NumberField<V> {
    private var entries = emptyList<Individual<O, V>>()
    private var accepted = emptyList<Individual<O, V>>()

    /** 全局有效档案快照 / Global valid archive snapshot. */
    val archive: List<Individual<O, V>> get() = entries.toList()

    /** 已接受的工作解快照 / Accepted working-set snapshot. */
    val working: List<Individual<O, V>> get() = accepted.toList()

    /** 将候选与参考档案合并，不修改记忆 / Merge against a reference without mutation.
     * @param reference 参考档案 / Reference archive
     * @param candidates 已评估的有效候选 / Valid evaluated candidates
     * @return 有界新档案 / Bounded merged archive
     */
    fun merged(reference: List<Individual<O, V>>, candidates: List<Individual<O, V>>): List<Individual<O, V>> {
        val all = (reference + candidates).filter { problem.validSolution(it.solution) && problem.valid(it.fitness) && problem.feasible(it.solution) }.distinctBy { it.solution }
        if (!problem.multiObjective) {
            return all.sortedWith { lhs, rhs ->
                when {
                    problem.dominates(lhs.fitness, rhs.fitness) -> -1
                    problem.dominates(rhs.fitness, lhs.fitness) -> 1
                    else -> 0
                }
            }.take(capacity)
        }
        val front = all.filter { candidate -> all.none { problem.dominates(it.fitness, candidate.fitness) } }
            .distinctBy { it.fitness }
        return trim(front, capacity)
    }

    /** 相对于冻结参考计算独立贡献 / Compute independent contribution against a frozen reference.
     * @param reference 派发时档案 / Archive at dispatch
     * @param candidates 动作结果 / Action outcomes
     * @return 是否改善与贡献比例 / Improvement and contribution ratio
     */
    fun contribution(reference: List<Individual<O, V>>, candidates: List<Individual<O, V>>): Pair<Boolean, Double> {
        val merged = merged(reference, candidates)
        val novel = if (problem.multiObjective) merged.count { candidate ->
            reference.none { problem.equivalent(it.fitness, candidate.fitness) || problem.dominates(it.fitness, candidate.fitness) }
        } else if (merged.isNotEmpty() && (reference.isEmpty() || problem.dominates(merged.first().fitness, reference.first().fitness))) 1 else 0
        return Pair(novel > 0, novel.toDouble() / candidates.size.coerceAtLeast(1))
    }

    /** 归并有效候选并应用工作集接受规则 / Merge valid candidates and apply working-set acceptance.
     * @param candidates 已评估候选 / Evaluated candidates
     * @return 全局档案是否改进 / Whether the global archive improved
     */
    fun offer(candidates: List<Individual<O, V>>): Boolean {
        val improved = contribution(entries, candidates).first
        entries = merged(entries, candidates)
        for (candidate in candidates.filter { problem.validSolution(it.solution) && problem.valid(it.fitness) && problem.feasible(it.solution) }) {
            if (acceptance.accept(problem, accepted, candidate)) {
                accepted = (listOf(candidate) + accepted).distinctBy { it.solution }.take(workingCapacity)
            }
        }
        return improved
    }

    /** 恢复协调器记忆，输入已通过 codec 验证 / Restore coordinator memory after codec validation.
     * @param archive 档案 / Archive
     * @param working 工作集 / Working set
     */
    fun restore(archive: List<Individual<O, V>>, working: List<Individual<O, V>>) {
        entries = merged(emptyList(), archive)
        accepted = working.take(workingCapacity)
    }
}
