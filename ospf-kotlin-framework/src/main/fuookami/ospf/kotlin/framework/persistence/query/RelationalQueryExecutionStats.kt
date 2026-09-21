/**
 * 关系查询执行统计 / Relational query execution statistics
 *
 * 该文件只保存不可变审计数据，不负责数据库执行。 / This file stores immutable audit data and does not execute databases.
 */
package fuookami.ospf.kotlin.framework.persistence.query

import java.util.concurrent.ConcurrentLinkedQueue

/**
 * 一次关系查询执行的统计 / Statistics for one relational query execution
 *
 * [duration] 使用纳秒，便于调用方保留高精度计时；失败执行仍然会记录计划信息。
 * [duration] is measured in nanoseconds so callers can retain timing precision; failed executions still retain plan data.
 *
 * @property planHash 计划规范化哈希 / Canonical plan hash
 * @property canonical 计划规范化表示 / Canonical plan representation
 * @property duration 执行耗时（纳秒） / Execution duration in nanoseconds
 * @property rowCount 返回行数；失败时通常为 null / Returned row count, usually null on failure
 * @property failed 是否执行失败 / Whether execution failed
 * @property failure 可选失败描述 / Optional failure description
 */
data class RelationalQueryExecutionStats(
    val planHash: String,
    val canonical: String,
    val duration: Long,
    val rowCount: Long? = null,
    val failed: Boolean = false,
    val failure: String? = null
) {
    /** 执行耗时（毫秒） / Execution duration in milliseconds */
    val durationMillis: Long get() = duration / 1_000_000L

    /** 兼容审计字段名 / Compatibility audit field name */
    val canonicalHash: String get() = planHash
}

/**
 * 线程安全的关系查询统计记录器 / Thread-safe relational query statistics recorder
 *
 * 记录顺序与调用顺序一致；[snapshot] 返回独立不可变列表。
 * Record order follows insertion order; [snapshot] returns an independent immutable list.
 */
class RelationalQueryExecutionStatsRecorder {
    private val entries = ConcurrentLinkedQueue<RelationalQueryExecutionStats>()

    /**
     * 记录成功执行 / Record a successful execution
     *
     * @param plan 查询计划 / Query plan
     * @param duration 执行耗时（纳秒） / Execution duration in nanoseconds
     * @param rowCount 返回行数 / Returned row count
     * @return 新增统计 / Recorded statistics
     */
    fun recordSuccess(
        plan: RelationalQueryPlan,
        duration: Long = 0L,
        rowCount: Long
    ): RelationalQueryExecutionStats {
        return record(
            plan = plan,
            duration = duration,
            rowCount = rowCount,
            failed = false,
            failure = null
        )
    }

    /**
     * 记录失败执行 / Record a failed execution
     *
     * @param plan 查询计划 / Query plan
     * @param duration 执行耗时（纳秒） / Execution duration in nanoseconds
     * @param failure 失败描述 / Failure description
     * @return 新增统计 / Recorded statistics
     */
    fun recordFailure(
        plan: RelationalQueryPlan,
        duration: Long = 0L,
        failure: String? = null
    ): RelationalQueryExecutionStats {
        return record(
            plan = plan,
            duration = duration,
            rowCount = null,
            failed = true,
            failure = failure
        )
    }

    /**
     * 返回统计快照 / Return a statistics snapshot
     *
     * @return 当前记录的独立列表 / Independent list of current records
     */
    fun snapshot(): List<RelationalQueryExecutionStats> {
        return entries.toList()
    }

    private fun record(
        plan: RelationalQueryPlan,
        duration: Long,
        rowCount: Long?,
        failed: Boolean,
        failure: String?
    ): RelationalQueryExecutionStats {
        val stats = RelationalQueryExecutionStats(
            planHash = plan.hash(),
            canonical = plan.canonical(),
            duration = duration.coerceAtLeast(0L),
            rowCount = rowCount,
            failed = failed,
            failure = failure
        )
        entries.add(stats)
        return stats
    }
}

/** 统计记录器简写别名 / Short alias for the statistics recorder */
typealias RelationalQueryStatsRecorder = RelationalQueryExecutionStatsRecorder
