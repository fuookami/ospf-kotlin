package fuookami.ospf.kotlin.core.solver.heuristic

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.basic.Solution

/** 单次评估结果，拒绝额度与无效候选分别表达 / Evaluation result separating denied admission from invalid candidates.
 * @property admitted 是否消耗了请求额度 / Whether a request token was consumed
 * @property candidate 已验证候选，目标缺失或不可行时为 null / Valid candidate, null for missing objectives or infeasibility
 */
data class SearchEvaluationResult<O, V>(
    val admitted: Boolean,
    val candidate: Individual<O, V>?
) where V : RealNumber<V>, V : NumberField<V>

/** 自定义会话共用的计费评估入口 / Metered evaluation entry shared by custom sessions.
 * @param solution 待复制与验证的解 / Solution to copy and validate
 * @param budget 当前片段，包含组合父级额度 / Current slice including composite parent allowances
 * @return 评估结果，外部回调错误转换为 Ret / Evaluation result with callback errors converted to Ret
 */
fun <O, V> SearchProblem<O, V>.evaluate(
    solution: Solution<V>,
    budget: SearchSlice
): Ret<SearchEvaluationResult<O, V>> where V : RealNumber<V>, V : NumberField<V> {
    if (!budget.acquire()) return Ok(SearchEvaluationResult(false, null))
    return searchBoundary {
        val copied = solution.toList()
        if (!validSolution(copied) || !feasible(copied)) {
            SearchEvaluationResult<O, V>(true, null)
        } else {
            budget.global.objectiveCalled()
            val objective = model.objective(copied)
            SearchEvaluationResult(
                admitted = true,
                candidate = objective?.takeIf(valid)?.let { SolutionWithFitness(copied, it) }
            )
        }
    }
}
