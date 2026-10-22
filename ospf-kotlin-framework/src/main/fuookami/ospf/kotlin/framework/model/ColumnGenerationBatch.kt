package fuookami.ospf.kotlin.framework.model

/** 定价完成状态；空列集合不表示穷尽搜索。 / Pricing completion; an empty batch is not an exhaustion certificate. */
enum class ColumnGenerationCompletion { Exhausted, BudgetLimit, Interrupted, Heuristic }

/** 定价证据作用域；必须精确绑定模型、对偶、分支和执行器时隙域。 / Pricing evidence scope bound to a model, duals, branch and executor-slot domain.
 * @property request 单次定价请求唯一标识 / Unique identity for one pricing request
 * @property group 请求覆盖的执行器或执行器分组 / Executors or executor groups covered by the request
 * @property branch 当前分支状态标识 / Current branch-state identity
 * @property global 是否穷尽该请求作用域内的完整业务域 / Whether the full business domain within this request scope is covered
 * @property model 主模型标识 / Master model identity
 * @property dual 产生当前影子价格的对偶版本 / Dual revision used to produce the current shadow prices
 * @property executorSlots 执行器与时隙组合标识 / Executor-slot combinations covered by the request
 */
data class ColumnGenerationScope(
    val request: String,
    val group: String,
    val branch: String,
    val global: Boolean = false,
    val model: String = "",
    val dual: String = "",
    val executorSlots: Set<String> = emptySet()
)

/** 单次列生成结果；只承诺精确作用域内的完成度，不承诺全局整数最优。 / Per-request result; completion is scoped and is not an integer-optimality claim.
 * @property columns 候选列 / Candidate columns
 * @property completion 完成状态 / Completion
 * @property scope 证据作用域，无证据时为空 / Evidence scope, null without evidence
 * @property noImprovingColumns 独立的无改进列证明；未提供时为空 / Independent no-improving-column proof, null when unavailable
 * @property children 分执行器或时隙的子请求结果 / Child request results split by executor or time slot
 */
data class ColumnGenerationBatch<C>(
    val columns: List<C>,
    val completion: ColumnGenerationCompletion = ColumnGenerationCompletion.Heuristic,
    val scope: ColumnGenerationScope? = null,
    val noImprovingColumns: Boolean? = null,
    val children: List<ColumnGenerationBatch<C>> = emptyList()
) {
    /** 验证穷尽证据严格属于指定的完整请求域。 / Check exhaustion evidence belongs to the exact complete request domain.
     * @param expected 预期作用域 / Expected scope
     * @return 是否具有完整有效证据 / Whether evidence is complete and applicable
     */
    fun certifies(expected: ColumnGenerationScope): Boolean {
        return completion == ColumnGenerationCompletion.Exhausted && expected.global &&
            expected.request.isNotBlank() && expected.model.isNotBlank() &&
            expected.dual.isNotBlank() && expected.branch.isNotBlank() && scope == expected &&
            hasConsistentChildren(expected)
    }

    /** 验证所有子结果完整覆盖父请求，且绑定同一模型、对偶和分支状态。 / Check child results cover the parent request and share its model, duals and branch state.
     * @param expected 预期父作用域 / Expected parent scope
     * @return 子结果是否完整且一致 / Whether child results are complete and consistent
     */
    fun hasConsistentChildren(expected: ColumnGenerationScope): Boolean {
        if (children.isEmpty()) return true
        val childScopes = children.map { child -> child.scope ?: return false }
        if (childScopes.any { childScope ->
                (childScope.request != expected.request && !childScope.request.startsWith("${expected.request}:")) ||
                    childScope.model != expected.model ||
                    childScope.dual != expected.dual ||
                    childScope.branch != expected.branch
            }) {
            return false
        }
        val requestedGroups = expected.group.split('|').filter { it.isNotEmpty() }.toSet()
        val childGroups = childScopes.flatMap { it.group.split('|') }.filter { it.isNotEmpty() }.toSet()
        val childSlots = childScopes.flatMap { it.executorSlots }.toSet()
        if (childGroups != requestedGroups || childSlots != expected.executorSlots) return false
        return children.all { child ->
            val childScope = child.scope ?: return@all false
            child.certifies(childScope)
        }
    }

    /** 验证当前对偶和请求域内不存在改进列，必须有独立证明。 / Check that no improving column exists for this dual and request scope using explicit evidence.
     * @param expected 预期作用域 / Expected scope
     * @return 是否有完整的无改进列证明 / Whether an exhaustive no-improving-column certificate is available
     */
    fun certifiesNoImprovingColumns(expected: ColumnGenerationScope): Boolean {
        return noImprovingColumns == true && certifies(expected) && children.all { child ->
            val childScope = child.scope ?: return@all false
            child.certifiesNoImprovingColumns(childScope)
        }
    }
}
