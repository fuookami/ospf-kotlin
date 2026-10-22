package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.network

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.path.*

/** 将网络路径转换为业务任务束；主模型和分支状态由 Gantt 调用方持有。 / Adapt network paths to business bunches; Gantt owns the master.
 * @property search 网络搜索内核 / Network search kernel
 */
class NetworkBunchGenerationContext(private val search: PathSearchContext = PathSearchContext()) {
    /** 搜索并保留完整业务路径状态。 / Search while retaining complete business path state.
     * @param problem 由当前任务、分支和对偶构造的问题 / Problem built for current tasks, branch and duals
     * @param budget 搜索预算 / Search budget
     * @param scope 当前请求作用域 / Current request scope
     * @param maximumResults 返回列限制 / Output limit
     * @param noImprovementEvidence 对搜索结果的独立无改进证明判定器 / Independent no-improvement proof predicate
     * @param onSearchCompleted 搜索完成后的统计回调 / Diagnostics callback after search completion
     * @param extract 业务任务束构造器 / Business bunch extractor
     * @return 任务束及完成信息 / Bunches and completion metadata
     */
    fun <N, A, S, B> generate(
        problem: PathProblem<N, A, S>,
        budget: PathSearchBudget,
        scope: ColumnGenerationScope,
        maximumResults: Int = Int.MAX_VALUE,
        noImprovementEvidence: ((PathSearchResult<S, A>) -> Boolean)? = null,
        onSearchCompleted: (PathSearchResult<S, A>) -> Unit = {},
        extract: (PathLabel<S, A>, PathSearchResult<S, A>) -> Ret<B?>
    ): Ret<ColumnGenerationBatch<B>> {
        val result = when (val ret = search.search(
            problem = problem,
            budget = budget,
            maximumResults = maximumResults
        )) {
            is Ok -> ret.value
            is Failed -> return Failed(ret.error)
            is Fatal -> return Fatal(ret.errors)
        }
        onSearchCompleted(result)
        val bunches = mutableListOf<B>()
        var rejected = false
        for (path in result.paths) {
            when (val ret = extract(path, result)) {
                is Ok -> {
                    val bunch = ret.value
                    if (bunch != null) bunches.add(bunch) else rejected = true
                }
                is Failed -> return Failed(ret.error)
                is Fatal -> return Fatal(ret.errors)
            }
        }
        val completion = when {
            rejected -> ColumnGenerationCompletion.Heuristic
            result.completion == PathSearchCompletion.Exhausted -> ColumnGenerationCompletion.Exhausted
            result.completion == PathSearchCompletion.BudgetLimit -> ColumnGenerationCompletion.BudgetLimit
            result.completion == PathSearchCompletion.Interrupted -> ColumnGenerationCompletion.Interrupted
            result.completion == PathSearchCompletion.UncertifiedDominance -> ColumnGenerationCompletion.Heuristic
            result.completion == PathSearchCompletion.UncertifiedValidation -> ColumnGenerationCompletion.Heuristic
            else -> ColumnGenerationCompletion.Heuristic
        }
        val provedNoImprovement = noImprovementEvidence?.invoke(result) == true &&
            completion == ColumnGenerationCompletion.Exhausted
        return ok(
            ColumnGenerationBatch(
                columns = bunches.toList(),
                completion = completion,
                scope = scope,
                noImprovingColumns = true.takeIf { provedNoImprovement }
            )
        )
    }
}
