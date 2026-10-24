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
    val objectiveTerms = terms(snapshot.objective)
    val rowTerms = snapshot.rows.associate { it.id to terms(it.coefficients) }
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

    private fun terms(coefficients: Map<AbstractVariableItem<*, *>, V>): Map<String, List<Pair<ColumnDecision, V>>> {
        // 稀疏项仍按原 decision 顺序求和，不能使用行 Map 的遍历顺序。 / Sum sparse terms in original decision order, never row-map iteration order.
        return coefficients.mapNotNull { (variable, coefficient) ->
            positions[variable]?.let { it to coefficient }
        }.sortedBy { it.first.index }.groupBy(
            keySelector = { it.first.value.blockId },
            valueTransform = { it.first.value to it.second }
        )
    }
}
