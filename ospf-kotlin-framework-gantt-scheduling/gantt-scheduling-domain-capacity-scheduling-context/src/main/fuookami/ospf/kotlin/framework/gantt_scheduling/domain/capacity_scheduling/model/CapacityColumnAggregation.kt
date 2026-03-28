package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model

import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
import fuookami.ospf.kotlin.utils.math.algebra.number.UInt64
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * 列聚合（按迭代分组）
 * Column Aggregation (grouped by iteration)
 */
class CapacityColumnAggregation<E : Executor, A : ProductionAction>(
    private val _columnsIteration: MutableList<List<CapacityColumn<E, A>>> = ArrayList(),
    private val _columns: MutableList<CapacityColumn<E, A>> = ArrayList(),
    private val _removedColumns: MutableSet<CapacityColumn<E, A>> = HashSet()
) {
    /**
     * 按迭代分组的�?
     * Columns grouped by iteration
     */
    val columnsIteration: List<List<CapacityColumn<E, A>>> by ::_columnsIteration

    /**
     * 所有列（扁平化�?
     * All columns (flattened)
     */
    val columns: List<CapacityColumn<E, A>> by ::_columns


    val removedColumns: Set<CapacityColumn<E, A>> by ::_removedColumns

    /**
     * 最新迭代的�?
     * Columns from last iteration
     */
    val lastIterationColumns: List<CapacityColumn<E, A>>
        get() = _columnsIteration.lastOrNull { it.isNotEmpty() } ?: emptyList()

    /**
     * 添加新列
     * Add new columns
     *
     * @param iteration Iteration number / 迭代�?
     * @param newColumns New columns to add / 要添加的新列
     * @return Unduplicated columns / 去重后的�?
     */
    suspend fun addColumns(
        iteration: UInt64,
        newColumns: List<CapacityColumn<E, A>>
    ): List<CapacityColumn<E, A>> {
        // Deduplicate within new columns
        // 在新列内部去�?
        val unduplicatedNewColumns = coroutineScope {
            val promises = ArrayList<Deferred<List<CapacityColumn<E, A>>>>()
            for (columnGroup in newColumns.groupBy { it.executor }.values) {
                promises.add(async(Dispatchers.Default) {
                    val unduplicated = ArrayList<CapacityColumn<E, A>>()
                    for (column in columnGroup) {
                        if (unduplicated.all { column neq it }) {
                            unduplicated.add(column)
                        }
                    }
                    unduplicated
                })
            }
            promises.flatMap { it.await() }
        }

        // Deduplicate with existing columns
        // 与现有列去重
        val unduplicatedColumns = coroutineScope {
            val promises = ArrayList<Deferred<CapacityColumn<E, A>?>>()
            for (column in unduplicatedNewColumns) {
                promises.add(async(Dispatchers.Default) {
                    if (_columns.all { column neq it }) {
                        column
                    } else {
                        null
                    }
                })
            }
            promises.mapNotNull { it.await() }
        }

        // Ensure iteration list has enough slots
        // 确保迭代列表有足够的槽位
        while (_columnsIteration.size <= iteration.toInt()) {
            _columnsIteration.add(ArrayList())
        }

        val mergedIterationColumns = _columnsIteration[iteration.toInt()].toMutableList()
        mergedIterationColumns.addAll(unduplicatedColumns)
        _columnsIteration[iteration.toInt()] = mergedIterationColumns
        _columns.addAll(unduplicatedColumns)

        return unduplicatedColumns
    }

    /**
     * 移除�?
     * Remove a column
     */
    fun removeColumn(column: CapacityColumn<E, A>) {
        if (!_removedColumns.contains(column)) {
            _removedColumns.add(column)
            _columns.remove(column)
        }
    }

    /**
     * 批量移除�?
     * Remove multiple columns
     */
    fun removeColumns(columns: List<CapacityColumn<E, A>>) {
        for (column in columns) {
            removeColumn(column)
        }
    }

    /**
     * 清空所有列
     * Clear all columns
     */
    fun clear() {
        _columnsIteration.clear()
        _columns.clear()
        _removedColumns.clear()
    }

    /**
     * 列相等比�?
     * Column equality comparison
     */
    private infix fun CapacityColumn<E, A>.neq(other: CapacityColumn<E, A>): Boolean {
        if (this.executor != other.executor) return true
        if (this.slotIndex != other.slotIndex) return true
        if (this.order != other.order) return true
        if (this.allocations != other.allocations) return true
        return false
    }
}



