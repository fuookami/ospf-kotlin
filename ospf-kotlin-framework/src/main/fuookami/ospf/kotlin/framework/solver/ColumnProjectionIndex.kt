package fuookami.ospf.kotlin.framework.solver

import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem

/** 单个已验证结构版本的稀疏投影索引。 / Sparse projection index for one validated structural revision. */
internal class ColumnProjectionIndex<V>(
    decisions: List<ColumnDecision>,
    blockIds: Set<String>,
    snapshot: LinearModelSnapshot<V>
) where V : RealNumber<V>, V : NumberField<V> {
    val revision = snapshot.revision
    val decisionsByBlock = decisions.groupBy { it.blockId }
    val idsByBlock = blockIds.associateWith { block -> decisionsByBlock[block].orEmpty().map { it.id }.toSet() }
    val mappedVariables = decisions.map { it.variable }.toSet()
    private val positions = decisions.withIndex().associateBy { it.value.variable }
    private val objectiveTermsCache = captureTerms(snapshot.objective).let { captured ->
        lazy { groupTerms(captured) }
    }
    private val rowTermsCache = snapshot.rows.map { it.id to captureTerms(it.coefficients) }.let { captured ->
        lazy { captured.associate { (id, terms) -> id to groupTerms(terms) } }
    }
    val objectiveTerms get() = objectiveTermsCache.value
    val rowTerms get() = rowTermsCache.value
    val localRows: Map<String, List<LinearModelRow<V>>>

    init {
        val local = blockIds.associateWith { ArrayList<LinearModelRow<V>>() }
        for (row in snapshot.rows) {
            if (row.coefficients.isEmpty()) {
                local.values.forEach { it.add(row) }
            } else {
                var owner: String? = null
                var whollyLocal = true
                for (variable in row.coefficients.keys) {
                    val block = positions[variable]?.value?.blockId
                    if (block == null || (owner != null && block != owner)) {
                        whollyLocal = false
                        break
                    }
                    owner = block
                }
                if (whollyLocal) {
                    local.getValue(owner!!).add(row)
                }
            }
        }
        localRows = local
    }

    /** 已构建派生表的观测快照，不增加热路径计数。 / Observe built derived tables without hot-path counters.
     * @return 已完成内部排序和分组的表名 / Names of tables whose internal sorting and grouping has completed
     */
    internal fun initializedTermIndexes(): Set<String> = listOf(
        "objective" to objectiveTermsCache,
        "rows" to rowTermsCache
    ).filter { it.second.isInitialized() }.map { it.first }.toSet()

    private fun captureTerms(coefficients: Map<AbstractVariableItem<*, *>, V>): MutableList<Pair<IndexedValue<ColumnDecision>, V>> {
        // 在原刷新边界读取Map并解析变量身份，不能延后外部访问器。 / Read maps and resolve variable identities at the original refresh boundary, never defer external accessors.
        return coefficients.mapNotNullTo(ArrayList()) { (variable, coefficient) ->
            positions[variable]?.let { it to coefficient }
        }
    }

    private fun groupTerms(captured: MutableList<Pair<IndexedValue<ColumnDecision>, V>>): Map<String, List<Pair<ColumnDecision, V>>> {
        // 稀疏项仍按原decision顺序求和；只延后内部排序和分组。 / Preserve original decision summation order; defer only internal sorting and grouping.
        captured.sortBy { it.first.index }
        return captured.groupBy(
            keySelector = { it.first.value.blockId },
            valueTransform = { it.first.value to it.second }
        )
    }
}
