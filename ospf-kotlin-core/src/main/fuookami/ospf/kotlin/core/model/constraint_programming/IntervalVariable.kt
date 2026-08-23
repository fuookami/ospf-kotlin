/**
 * CP interval 与排程约束。 / CP intervals and scheduling constraints.
 */
package fuookami.ospf.kotlin.core.model.constraint_programming

import java.math.BigInteger
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.core.solver.report.ModelElementOrigin
import fuookami.ospf.kotlin.core.solver.report.VariableId

/**
 * 稳定 interval 标识。 / Stable interval identifier.
 *
 * @property value 稳定标识值 / Stable identifier value
 */
@JvmInline
value class IntervalId(val value: String)

/**
 * 一次 interval 求值结果。 / Evaluated interval value.
 *
 * @property start 起始时间 / Start time
 * @property size duration / Duration
 * @property end 结束时间 / End time
 * @property present 是否存在 / Whether the interval is present
 */
data class IntervalValue(
    val start: Int64,
    val size: Int64,
    val end: Int64,
    val present: Boolean = true
)

/**
 * CP interval 变量，满足 `end = start + size`。[size] 可以是常量表达式（固定 duration）或整数变量表达式（变量 duration）。[presence] / CP interval variable satisfying `end = start + size`.
 * 非空时表示 optional interval。 / [size] may be a constant expression (fixed duration) or an integer-variable expression (variable duration). A non-null [presence] makes it optional.
 *
 * @property id 稳定 interval ID / Stable interval ID
 * @property start 起始时间表达式 / Start-time expression
 * @property size duration 表达式 / Duration expression
 * @property end 结束时间表达式 / End-time expression
 * @property presence 可选存在文字 / Optional presence literal
 * @property scope 身份作用域 / Identity scope
 * @property origin 稳定身份来源 / Stable identity origin
 * @property identityProvenance 完整身份来源集合 / Complete identity provenance
 */
data class IntervalVariable(
    val id: IntervalId,
    val start: ConstraintProgrammingExpression,
    val size: ConstraintProgrammingExpression,
    val end: ConstraintProgrammingExpression,
    val presence: BooleanLiteral? = null,
    val scope: String = "model-local",
    val origin: String? = null,
    val identityProvenance: List<ModelElementOrigin> = emptyList()
) {
    /** duration 语义别名。 / Duration semantic alias. */
    val duration: ConstraintProgrammingExpression
        get() = size

    /** 是否为 optional interval。 / Whether this is an optional interval. */
    val optional: Boolean
        get() = presence != null

    /** 涉及的稳定变量 ID。 / Stable variable IDs referenced by the interval. */
    val variables: Set<VariableId>
        get() = linkedSetOf<VariableId>().apply {
            addAll(this@IntervalVariable.start.variables)
            addAll(this@IntervalVariable.size.variables)
            addAll(this@IntervalVariable.end.variables)
            addAll(presence?.variableId?.let(::setOf).orEmpty())
        }

    /**
     * 求值并校验 interval 一致性。 / Evaluate and validate interval consistency.
     *
     * @param values 变量赋值 / Variable assignments
     * @return interval 值或结构化错误 / The interval value or a structured error
     */
    fun evaluate(values: Map<VariableId, Int64>): Ret<IntervalValue> {
        if (presence != null) {
            val present = presence.evaluateInteger(values)
            if (present.failed) {
                return propagateIntervalFailure(present)
            }
            if (present.value == false) {
                return ok(IntervalValue(Int64.zero, Int64.zero, Int64.zero, present = false))
            }
        }

        val startValue = start.evaluate(values)
        if (startValue.failed) {
            return propagateIntervalFailure(startValue)
        }
        val sizeValue = size.evaluate(values)
        if (sizeValue.failed) {
            return propagateIntervalFailure(sizeValue)
        }
        val endValue = end.evaluate(values)
        if (endValue.failed) {
            return propagateIntervalFailure(endValue)
        }

        val startInteger = startValue.value!!
        val sizeInteger = sizeValue.value!!
        val endInteger = endValue.value!!
        if (sizeInteger < Int64.zero) {
            return Failed(
                ErrorCode.IllegalArgument,
                "interval duration 不得为负：$sizeInteger / Interval size must not be negative: $sizeInteger"
            )
        }
        val expectedEnd = BigInteger.valueOf(startInteger.toLong())
            .add(BigInteger.valueOf(sizeInteger.toLong()))
        if (expectedEnd < BigInteger.valueOf(Long.MIN_VALUE) ||
            expectedEnd > BigInteger.valueOf(Long.MAX_VALUE)
        ) {
            return Failed(
                ErrorCode.IllegalArgument,
                "interval 起止时间溢出 Int64 / Interval start-plus-size overflows Int64"
            )
        }
        if (endInteger.toLong() != expectedEnd.toLong()) {
            return Failed(
                ErrorCode.IllegalArgument,
                "interval 必须满足 end = start + size / Interval must satisfy end = start + size"
            )
        }
        return ok(IntervalValue(startInteger, sizeInteger, endInteger, present = true))
    }

    companion object {
        /**
         * 创建 interval。 / Create an interval.
         *
         * @param id 稳定 interval ID / Stable interval ID
         * @param start 起始时间表达式 / Start-time expression
         * @param size duration 表达式 / Duration expression
         * @param end 结束时间表达式 / End-time expression
         * @param presence 可选存在文字 / Optional presence literal
         * @param scope 身份作用域 / Identity scope
         * @param origin 稳定身份来源 / Stable identity origin
         * @param identityProvenance 完整身份来源集合 / Complete identity provenance
         * @return interval 或结构化错误 / Interval or a structured error
         */
        fun create(
            id: IntervalId,
            start: ConstraintProgrammingExpression,
            size: ConstraintProgrammingExpression,
            end: ConstraintProgrammingExpression,
            presence: BooleanLiteral? = null,
            scope: String = "model-local",
            origin: String? = null,
            identityProvenance: List<ModelElementOrigin> = emptyList()
        ): Ret<IntervalVariable> {
            if (id.value.isBlank() || validateConstraintProgrammingIdentity(
                    id = id.value,
                    scope = scope,
                    origin = origin,
                    provenance = identityProvenance,
                    identityNamespace = "cp",
                    identitySchemaVersion = "1.0"
                ) != null
            ) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "interval 身份元数据无效 / Interval identity metadata is invalid"
                )
            }
            return ok(IntervalVariable(id, start, size, end, presence, scope, origin, identityProvenance))
        }

        /**
         * 使用字符串标识创建 interval。 / Create an interval with a string identifier.
         *
         * @param id 稳定 interval ID 字符串 / Stable interval ID string
         * @param start 起始时间表达式 / Start-time expression
         * @param size duration 表达式 / Duration expression
         * @param end 结束时间表达式 / End-time expression
         * @param presence 可选存在文字 / Optional presence literal
         * @param scope 身份作用域 / Identity scope
         * @param origin 稳定身份来源 / Stable identity origin
         * @param identityProvenance 完整身份来源集合 / Complete identity provenance
         * @return interval 或结构化错误 / Interval or a structured error
         */
        fun create(
            id: String,
            start: ConstraintProgrammingExpression,
            size: ConstraintProgrammingExpression,
            end: ConstraintProgrammingExpression,
            presence: BooleanLiteral? = null,
            scope: String = "model-local",
            origin: String? = null,
            identityProvenance: List<ModelElementOrigin> = emptyList()
        ): Ret<IntervalVariable> {
            return create(IntervalId(id), start, size, end, presence, scope, origin, identityProvenance)
        }

        /**
         * 创建固定 duration interval。 / Create a fixed-duration interval.
         *
         * @param id 稳定 interval ID / Stable interval ID
         * @param start 起始时间表达式 / Start-time expression
         * @param size 固定 duration / Fixed duration
         * @param end 结束时间表达式 / End-time expression
         * @param presence 可选存在文字 / Optional presence literal
         * @param scope 身份作用域 / Identity scope
         * @param origin 稳定身份来源 / Stable identity origin
         * @param identityProvenance 完整身份来源集合 / Complete identity provenance
         * @return interval 或结构化错误 / Interval or a structured error
         */
        fun fixed(
            id: IntervalId,
            start: ConstraintProgrammingExpression,
            size: Int64,
            end: ConstraintProgrammingExpression,
            presence: BooleanLiteral? = null,
            scope: String = "model-local",
            origin: String? = null,
            identityProvenance: List<ModelElementOrigin> = emptyList()
        ): Ret<IntervalVariable> {
            if (size < Int64.zero) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "interval duration 不得为负：$size / Interval size must not be negative: $size"
                )
            }
            return create(
                id,
                start,
                ConstraintProgrammingExpression.Constant(size),
                end,
                presence,
                scope,
                origin,
                identityProvenance
            )
        }
    }
}

/**
 * NoOverlap 排程约束。 / No-overlap scheduling constraint.
 *
 * @property intervals 参与排程的 interval / Scheduled intervals
 */
data class NoOverlap(
    val intervals: List<IntervalVariable>
) : ConstraintProgrammingConstraint {
    override val variables: Set<VariableId>
        get() = intervals.flatMapTo(linkedSetOf()) { it.variables }

    override fun isSatisfied(values: Map<VariableId, Int64>): Ret<Boolean> {
        val evaluated = ArrayList<IntervalValue>()
        for (interval in intervals) {
            val result = interval.evaluate(values)
            if (result.failed) {
                return propagateIntervalFailure(result)
            }
            if (result.value!!.present) {
                evaluated += result.value!!
            }
        }
        for (i in evaluated.indices) {
            for (j in i + 1 until evaluated.size) {
                val left = evaluated[i]
                val right = evaluated[j]
                if (left.start < right.end && right.start < left.end) {
                    return ok(false)
                }
            }
        }
        return ok(true)
    }

    companion object {
        /**
         * 创建 NoOverlap 并拒绝空集合。 / Create NoOverlap and reject an empty collection.
         *
         * @param intervals 参与排程的 interval / Scheduled intervals
         * @return NoOverlap 约束或结构化错误 / NoOverlap constraint or a structured error
         */
        fun create(intervals: Iterable<IntervalVariable>): Ret<NoOverlap> {
            val list = intervals.toList()
            if (list.isEmpty()) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "NoOverlap interval 集合不能为空 / NoOverlap interval collection must not be empty"
                )
            }
            return ok(NoOverlap(list))
        }
    }
}

/**
 * Cumulative 容量约束。 / Cumulative capacity constraint.
 *
 * @property intervals 参与容量约束的 interval / Intervals consuming capacity
 * @property demands 对应资源需求 / Corresponding resource demands
 * @property capacity 容量表达式 / Capacity expression
 */
data class Cumulative(
    val intervals: List<IntervalVariable>,
    val demands: List<ConstraintProgrammingExpression>,
    val capacity: ConstraintProgrammingExpression
) : ConstraintProgrammingConstraint {
    override val variables: Set<VariableId>
        get() = linkedSetOf<VariableId>().apply {
            intervals.forEach { addAll(it.variables) }
            demands.forEach { addAll(it.variables) }
            addAll(capacity.variables)
        }

    override fun isSatisfied(values: Map<VariableId, Int64>): Ret<Boolean> {
        if (intervals.size != demands.size) {
            return Failed(
                ErrorCode.IllegalArgument,
                "Cumulative interval 与 demand 数量必须一致 / Cumulative interval and demand counts must match"
            )
        }
        val evaluated = ArrayList<Pair<IntervalValue, Int64>>()
        for (index in intervals.indices) {
            val intervalResult = intervals[index].evaluate(values)
            if (intervalResult.failed) {
                return propagateIntervalFailure(intervalResult)
            }
            if (!intervalResult.value!!.present) {
                continue
            }
            val demandResult = demands[index].evaluate(values)
            if (demandResult.failed) {
                return propagateIntervalFailure(demandResult)
            }
            val demand = demandResult.value!!
            if (demand < Int64.zero) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "cumulative demand 不得为负：$demand / Cumulative demand must not be negative: $demand"
                )
            }
            evaluated += intervalResult.value!! to demand
        }

        val capacityResult = capacity.evaluate(values)
        if (capacityResult.failed) {
            return propagateIntervalFailure(capacityResult)
        }
        val capacityValue = capacityResult.value!!
        if (capacityValue < Int64.zero) {
            return Failed(
                ErrorCode.IllegalArgument,
                "cumulative capacity 不得为负：$capacityValue / Cumulative capacity must not be negative: $capacityValue"
            )
        }

        val eventPoints = evaluated.flatMap { listOf(it.first.start, it.first.end) }.distinct()
        for (point in eventPoints) {
            var load = BigInteger.ZERO
            for ((interval, demand) in evaluated) {
                if (interval.start <= point && point < interval.end) {
                    load = load.add(BigInteger.valueOf(demand.toLong()))
                }
            }
            if (load > BigInteger.valueOf(capacityValue.toLong())) {
                return ok(false)
            }
        }
        return ok(true)
    }

    companion object {
        /**
         * 创建 Cumulative 并校验输入规模。 / Create Cumulative and validate input shape.
         *
         * @param intervals 参与容量约束的 interval / Intervals consuming capacity
         * @param demands 对应资源需求 / Corresponding resource demands
         * @param capacity 容量表达式 / Capacity expression
         * @return Cumulative 约束或结构化错误 / Cumulative constraint or a structured error
         */
        fun create(
            intervals: Iterable<IntervalVariable>,
            demands: Iterable<ConstraintProgrammingExpression>,
            capacity: ConstraintProgrammingExpression
        ): Ret<Cumulative> {
            val intervalList = intervals.toList()
            val demandList = demands.toList()
            if (intervalList.isEmpty()) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "Cumulative interval 集合不能为空 / Cumulative interval collection must not be empty"
                )
            }
            if (intervalList.size != demandList.size) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "Cumulative interval 与 demand 数量必须一致 / Cumulative interval and demand counts must match"
                )
            }
            return ok(Cumulative(intervalList, demandList, capacity))
        }
    }
}

private fun <T> propagateIntervalFailure(result: Ret<*>): Ret<T> {
    return when (result) {
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        else -> Failed(
            ErrorCode.ApplicationError,
            "CP interval 结果状态无效 / Invalid CP interval result state"
        )
    }
}
