
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*

/**
 * 列聚合（按迭代分组）
 * Column Aggregation (grouped by iteration)
 *
 * 管理产能列的聚合，支持按迭代分组和去重。
 * Manages aggregation of capacity columns with iteration grouping and deduplication.
 *
 * @param E 执行器类型 / Executor type
 * @param A 生产动作类型 / Production action type
 * @param V 数值类型 / Numeric type
 */
class CapacityColumnAggregation<E : Executor, A : ProductionAction, V : RealNumber<V>>(
    private val _columnsIteration: MutableList<List<CapacityColumn<E, A, V>>> = ArrayList(),
    private val _columns: MutableList<CapacityColumn<E, A, V>> = ArrayList(),
    private val _removedColumns: MutableSet<CapacityColumn<E, A, V>> = HashSet()
) {
    /**
     * 按迭代分组的列
     * Columns grouped by iteration
     */
    val columnsIteration: List<List<CapacityColumn<E, A, V>>> by ::_columnsIteration

    /**
     * 所有列（扁平化）
     * All columns (flattened)
     */
    val columns: List<CapacityColumn<E, A, V>> by ::_columns

    /**
     * 已移除的列
     * Removed columns
     */
    val removedColumns: Set<CapacityColumn<E, A, V>> by ::_removedColumns

    /**
     * 最新迭代的列
     * Columns from last iteration
     */
    val lastIterationColumns: List<CapacityColumn<E, A, V>>
        get() = _columnsIteration.lastOrNull { it.isNotEmpty() } ?: emptyList()

    /**
     * 添加新列
     * Add new columns
     *
     * @param iteration Iteration number / 迭代号
     * @param newColumns New columns to add / 要添加的新列
     * @return Unduplicated columns / 去重后的列
     */
    suspend fun addColumns(
        iteration: UInt64,
        newColumns: List<CapacityColumn<E, A, V>>
    ): List<CapacityColumn<E, A, V>> {
        // Deduplicate within new columns
        // 在新列内部去重
        val unduplicatedNewColumns = coroutineScope {
            val promises = ArrayList<Deferred<List<CapacityColumn<E, A, V>>>>()
            for (columnGroup in newColumns.groupBy { it.executor }.values) {
                promises.add(async(Dispatchers.Default) {
                    val unduplicated = ArrayList<CapacityColumn<E, A, V>>()
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
            val promises = ArrayList<Deferred<CapacityColumn<E, A, V>?>>()
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
     * 移除列
     * Remove a column
     *
     * @param column Column to remove / 要移除的列
     */
    fun removeColumn(column: CapacityColumn<E, A, V>) {
        if (!_removedColumns.contains(column)) {
            _removedColumns.add(column)
            _columns.remove(column)
        }
    }

    /**
     * 批量移除列
     * Remove multiple columns
     *
     * @param columns Columns to remove / 要移除的列列表
     */
    fun removeColumns(columns: List<CapacityColumn<E, A, V>>) {
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
     * 列相等比较
     * Column equality comparison
     *
     * @param other Column to compare / 要比较的列
     * @return Whether columns are not equal / 列是否不相等
     */
    private infix fun CapacityColumn<E, A, V>.neq(other: CapacityColumn<E, A, V>): Boolean {
        if (this.executor != other.executor) return true
        if (this.slotIndex != other.slotIndex) return true
        if (this.order != other.order) return true
        if (this.allocations != other.allocations) return true
        return false
    }
}
