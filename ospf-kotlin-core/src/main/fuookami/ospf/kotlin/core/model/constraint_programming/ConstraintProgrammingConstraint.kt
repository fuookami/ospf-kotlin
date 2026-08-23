/**
 * CP 首版约束 AST。 / First-batch constraint AST for CP.
 */
package fuookami.ospf.kotlin.core.model.constraint_programming

import fuookami.ospf.kotlin.core.solver.report.VariableId
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.ok

/** 整数关系。 / Integer relation. */
enum class ConstraintProgrammingComparison {
    Equal,
    LessOrEqual,
    GreaterOrEqual;

    /** 常用的数学 DSL 别名。 / Common mathematical DSL aliases. */
    companion object {
        /** 等式别名。 / Equality alias. */
        val EQ: ConstraintProgrammingComparison get() = Equal

        /** 小于等于别名。 / Less-than-or-equal alias. */
        val LE: ConstraintProgrammingComparison get() = LessOrEqual

        /** 大于等于别名。 / Greater-than-or-equal alias. */
        val GE: ConstraintProgrammingComparison get() = GreaterOrEqual
    }
}

/**
 * 整数等式 DSL。 / Integer equality DSL.
 *
 * @param rhs 右侧整数 / Right-hand integer
 * @return 整数比较约束或结构化错误 / Integer comparison or a structured error
 */
infix fun ConstraintProgrammingExpression.eq(rhs: Int64): Ret<ConstraintProgrammingConstraint.IntegerComparison> {
    return ConstraintProgrammingConstraint.equal(this, rhs)
}

/**
 * 整数小于等于 DSL。 / Integer less-than-or-equal DSL.
 *
 * @param rhs 右侧整数 / Right-hand integer
 * @return 整数比较约束或结构化错误 / Integer comparison or a structured error
 */
infix fun ConstraintProgrammingExpression.leq(rhs: Int64): Ret<ConstraintProgrammingConstraint.IntegerComparison> {
    return ConstraintProgrammingConstraint.lessOrEqual(this, rhs)
}

/**
 * 整数大于等于 DSL。 / Integer greater-than-or-equal DSL.
 *
 * @param rhs 右侧整数 / Right-hand integer
 * @return 整数比较约束或结构化错误 / Integer comparison or a structured error
 */
infix fun ConstraintProgrammingExpression.geq(rhs: Int64): Ret<ConstraintProgrammingConstraint.IntegerComparison> {
    return ConstraintProgrammingConstraint.greaterOrEqual(this, rhs)
}

/**
 * 使用整数赋值求值布尔文字。 / Evaluate a Boolean literal with integer assignments.
 *
 * @param values 按稳定变量 ID 编索引的赋值 / Assignments indexed by stable variable ID
 * @return 求值结果或结构化错误 / The value or a structured error
 */
fun BooleanLiteral.evaluateInteger(values: Map<VariableId, Int64>): Ret<Boolean> {
    val fixed = constant
    if (fixed != null) {
        return ok(fixed)
    }
    val id = variableId ?: return Failed(
        ErrorCode.IllegalArgument,
        "布尔文字缺少变量标识 / Boolean literal has no variable identifier"
    )
    val raw = values[id] ?: return Failed(
        ErrorCode.DataNotFound,
        "缺少布尔变量赋值：$id / Missing Boolean variable assignment: $id"
    )
    if (raw != Int64.zero && raw != Int64.one) {
        return Failed(
            ErrorCode.IllegalArgument,
            "布尔变量赋值必须为 0 或 1：$raw / Boolean variable assignment must be 0 or 1: $raw"
        )
    }
    val result = raw == Int64.one
    return ok(if (negated) !result else result)
}

private fun tableSatisfied(
    expressions: List<ConstraintProgrammingExpression>,
    tuples: List<List<Int64>>,
    values: Map<VariableId, Int64>,
    allowed: Boolean
): Ret<Boolean> {
    val assignment = ArrayList<Int64>(expressions.size)
    for (expression in expressions) {
        val result = expression.evaluate(values)
        if (result.failed) {
            return propagateFailure(result)
        }
        assignment += result.value!!
    }
    val present = tuples.any { it == assignment }
    return ok(if (allowed) present else !present)
}

private fun <T> propagateFailure(result: Ret<*>): Ret<T> {
    return when (result) {
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        else -> Failed(
            ErrorCode.ApplicationError,
            "CP 约束结果状态无效 / Invalid CP constraint result state"
        )
    }
}

private inline fun <T, U> Ret<T>.flatMapResult(transform: (T) -> Ret<U>): Ret<U> {
    return when (this) {
        is fuookami.ospf.kotlin.utils.functional.Ok -> transform(value)
        is Failed -> Failed(error)
        is Fatal -> Fatal(errors)
    }
}

/** Reification 方向。 / Reification direction. */
enum class ReificationDirection {
    /** literal 为真时约束成立。 / The constraint is enforced by the literal. */
    Implies,

    /** 约束成立时 literal 为真。 / The literal is enforced by the constraint. */
    ImpliedBy,

    /** literal 与约束等价。 / Equivalence between the literal and the constraint. */
    Equivalent
}

/**
 * CP 约束统一接口，提供 solver-neutral 求值契约。 / Solver-neutral constraint contract with evaluation.
 */
sealed interface ConstraintProgrammingConstraint {
    /** 约束涉及的稳定变量 ID。 / Stable variable IDs referenced by this constraint. */
    val variables: Set<VariableId>

    /**
     * 在一个整数赋值上验证约束。 / Check the constraint against an integer assignment.布尔变量使用 0/1；非 0/1 的布尔赋值返回结构化错误。 / Boolean variables use 0/1; / non-binary assignments return a structured error.
     *
     * @param values 变量赋值 / Variable assignments
     * @return true/false 或求值错误 / True/false or an evaluation error
     */
    fun isSatisfied(values: Map<VariableId, Int64>): Ret<Boolean>

    /**
     * 整数等式或不等式。 / Integer equality or inequality.
     *
     * @property expression 左侧表达式 / Left-hand expression
     * @property comparison 比较关系 / Comparison relation
     * @property rhs 右侧整数 / Right-hand integer
     */
    data class IntegerComparison(
        val expression: ConstraintProgrammingExpression,
        val comparison: ConstraintProgrammingComparison,
        val rhs: Int64
    ) : ConstraintProgrammingConstraint {
        override val variables: Set<VariableId>
            get() = expression.variables

        override fun isSatisfied(values: Map<VariableId, Int64>): Ret<Boolean> {
            return expression.evaluate(values).flatMapResult { lhs ->
                ok(
                    when (comparison) {
                        ConstraintProgrammingComparison.Equal -> lhs == rhs
                        ConstraintProgrammingComparison.LessOrEqual -> lhs <= rhs
                        ConstraintProgrammingComparison.GreaterOrEqual -> lhs >= rhs
                    }
                )
            }
        }
    }

    /**
     * BoolAnd。 / Boolean AND constraint.
     *
     * @property literals 参与合取的文字 / Literals in the conjunction
     */
    data class BoolAnd(
        val literals: List<BooleanLiteral>
    ) : ConstraintProgrammingConstraint {
        override val variables: Set<VariableId>
            get() = literals.mapNotNullTo(linkedSetOf()) { it.variableId }

        override fun isSatisfied(values: Map<VariableId, Int64>): Ret<Boolean> {
            for (literal in literals) {
                val result = literal.evaluateInteger(values)
                if (result.failed) {
                    return propagateFailure(result)
                }
                if (result.value != true) {
                    return ok(false)
                }
            }
            return ok(true)
        }
    }

    /**
     * BoolOr。 / Boolean OR constraint.
     *
     * @property literals 参与析取的文字 / Literals in the disjunction
     */
    data class BoolOr(
        val literals: List<BooleanLiteral>
    ) : ConstraintProgrammingConstraint {
        override val variables: Set<VariableId>
            get() = literals.mapNotNullTo(linkedSetOf()) { it.variableId }

        override fun isSatisfied(values: Map<VariableId, Int64>): Ret<Boolean> {
            for (literal in literals) {
                val result = literal.evaluateInteger(values)
                if (result.failed) {
                    return propagateFailure(result)
                }
                if (result.value == true) {
                    return ok(true)
                }
            }
            return ok(false)
        }
    }

    /**
     * BoolXor（恰好一个为真）。 / Boolean XOR constraint (exactly one true).
     *
     * @property literals 参与异或的文字 / Literals in the XOR constraint
     */
    data class BoolXor(
        val literals: List<BooleanLiteral>
    ) : ConstraintProgrammingConstraint {
        override val variables: Set<VariableId>
            get() = literals.mapNotNullTo(linkedSetOf()) { it.variableId }

        override fun isSatisfied(values: Map<VariableId, Int64>): Ret<Boolean> {
            var trueAmount = 0
            for (literal in literals) {
                val result = literal.evaluateInteger(values)
                if (result.failed) {
                    return propagateFailure(result)
                }
                if (result.value == true) {
                    ++trueAmount
                    if (trueAmount > 1) {
                        return ok(false)
                    }
                }
            }
            return ok(trueAmount == 1)
        }
    }

    /**
     * 单个布尔文字约束。 / Constraint requiring one Boolean literal to be true.
     *
     * @property literal 要求为真的文字 / Literal required to be true
     */
    data class Literal(
        val literal: BooleanLiteral
    ) : ConstraintProgrammingConstraint {
        override val variables: Set<VariableId>
            get() = literal.variableId?.let(::setOf) ?: emptySet()

        override fun isSatisfied(values: Map<VariableId, Int64>): Ret<Boolean> {
            return literal.evaluateInteger(values)
        }
    }

    /**
     * 蕴含约束。 / Implication constraint.
     *
     * @property enforcement 启用文字 / Enforcement literal
     * @property constraint 被蕴含的约束 / Implied constraint
     */
    data class Implication(
        val enforcement: BooleanLiteral,
        val constraint: ConstraintProgrammingConstraint
    ) : ConstraintProgrammingConstraint {
        override val variables: Set<VariableId>
            get() = linkedSetOf<VariableId>().apply {
                addAll(enforcement.variableId?.let(::setOf).orEmpty())
                addAll(constraint.variables)
            }

        override fun isSatisfied(values: Map<VariableId, Int64>): Ret<Boolean> {
            return enforcement.evaluateInteger(values).flatMapResult { enabled ->
                if (enabled) constraint.isSatisfied(values) else ok(true)
            }
        }
    }

    /**
     * Reified 约束。 / Reified constraint.
     *
     * @property literal 关联文字 / Associated literal
     * @property constraint 关联约束 / Associated constraint
     * @property direction 重ification 方向 / Reification direction
     */
    data class Reified(
        val literal: BooleanLiteral,
        val constraint: ConstraintProgrammingConstraint,
        val direction: ReificationDirection = ReificationDirection.Equivalent
    ) : ConstraintProgrammingConstraint {
        override val variables: Set<VariableId>
            get() = linkedSetOf<VariableId>().apply {
                addAll(literal.variableId?.let(::setOf).orEmpty())
                addAll(constraint.variables)
            }

        override fun isSatisfied(values: Map<VariableId, Int64>): Ret<Boolean> {
            return when (direction) {
                ReificationDirection.Implies -> literal.evaluateInteger(values).flatMapResult { literalValue ->
                    if (!literalValue) {
                        ok(true)
                    } else {
                        constraint.isSatisfied(values)
                    }
                }

                ReificationDirection.ImpliedBy -> constraint.isSatisfied(values).flatMapResult { constraintValue ->
                    if (!constraintValue) {
                        ok(true)
                    } else {
                        literal.evaluateInteger(values)
                    }
                }

                ReificationDirection.Equivalent -> literal.evaluateInteger(values).flatMapResult { literalValue ->
                    constraint.isSatisfied(values).flatMapResult { constraintValue ->
                        ok(literalValue == constraintValue)
                    }
                }
            }
        }
    }

    /**
     * 全异约束。 / All-different constraint.
     *
     * @property expressions 待比较表达式 / Expressions to compare
     */
    data class AllDifferent(
        val expressions: List<ConstraintProgrammingExpression>
    ) : ConstraintProgrammingConstraint {
        override val variables: Set<VariableId>
            get() = expressions.flatMapTo(linkedSetOf()) { it.variables }

        override fun isSatisfied(values: Map<VariableId, Int64>): Ret<Boolean> {
            val seen = HashSet<Int64>()
            for (expression in expressions) {
                val result = expression.evaluate(values)
                if (result.failed) {
                    return propagateFailure(result)
                }
                if (!seen.add(result.value!!)) {
                    return ok(false)
                }
            }
            return ok(true)
        }
    }

    /**
     * Element 约束：target 等于 values[index]。 / Element constraint: target equals values[index].
     *
     * @property index 索引表达式 / Index expression
     * @property values 候选值或表达式 / Candidate values or expressions
     * @property target 目标表达式 / Target expression
     */
    data class Element(
        val index: ConstraintProgrammingExpression,
        val values: List<*>,
        val target: ConstraintProgrammingExpression
    ) : ConstraintProgrammingConstraint {
        override val variables: Set<VariableId>
            get() = linkedSetOf<VariableId>().apply {
                addAll(index.variables)
                addAll(target.variables)
                values.filterIsInstance<ConstraintProgrammingExpression>().forEach { addAll(it.variables) }
            }

        override fun isSatisfied(values: Map<VariableId, Int64>): Ret<Boolean> {
            val indexValue = index.evaluate(values)
            if (indexValue.failed) {
                return propagateFailure(indexValue)
            }
            val position = indexValue.value!!.toLong()
            if (position < 0L || position >= this.values.size.toLong()) {
                return ok(false)
            }
            val selected = this.values[position.toInt()]
            val selectedValue = when (selected) {
                is ConstraintProgrammingExpression -> selected.evaluate(values)
                is Int64 -> ok(selected)
                is Long -> ok(Int64(selected))
                is Int -> ok(Int64(selected.toLong()))
                else -> Failed(
                    ErrorCode.IllegalArgument,
                    "Element 数组包含非整数值 / Element array contains a non-integer value"
                )
            }
            if (selectedValue.failed) {
                return propagateFailure(selectedValue)
            }
            return target.evaluate(values).flatMapResult { targetValue ->
                ok(targetValue == selectedValue.value!!)
            }
        }
    }

    /**
     * 允许赋值表约束。 / Allowed-assignment table constraint.
     *
     * @property expressions 表达式列 / Expression columns
     * @property tuples 允许的元组 / Allowed tuples
     */
    data class AllowedAssignments(
        val expressions: List<ConstraintProgrammingExpression>,
        val tuples: List<List<Int64>>
    ) : ConstraintProgrammingConstraint {
        override val variables: Set<VariableId>
            get() = expressions.flatMapTo(linkedSetOf()) { it.variables }

        override fun isSatisfied(values: Map<VariableId, Int64>): Ret<Boolean> {
            return tableSatisfied(expressions, tuples, values, allowed = true)
        }
    }

    /**
     * 禁止赋值表约束。 / Forbidden-assignment table constraint.
     *
     * @property expressions 表达式列 / Expression columns
     * @property tuples 禁止的元组 / Forbidden tuples
     */
    data class ForbiddenAssignments(
        val expressions: List<ConstraintProgrammingExpression>,
        val tuples: List<List<Int64>>
    ) : ConstraintProgrammingConstraint {
        override val variables: Set<VariableId>
            get() = expressions.flatMapTo(linkedSetOf()) { it.variables }

        override fun isSatisfied(values: Map<VariableId, Int64>): Ret<Boolean> {
            return tableSatisfied(expressions, tuples, values, allowed = false)
        }
    }

    /**
     * 基于后继表达式的回路约束。 / Circuit constraint over successor expressions.
     * 每个表达式表示一个节点的后继；取值必须构成一个排列，且从 0 出发必须访问全部节点。 /
     * Each expression is the successor of one node. The values must form one permutation and traversing from node zero must visit every node exactly once.
     *
     * @property successors 每个节点的后继表达式 / Successor expression for each node
     */
    data class Circuit(
        val successors: List<ConstraintProgrammingExpression>
    ) : ConstraintProgrammingConstraint {
        override val variables: Set<VariableId>
            get() = successors.flatMapTo(linkedSetOf()) { it.variables }

        override fun isSatisfied(values: Map<VariableId, Int64>): Ret<Boolean> {
            val next = ArrayList<Int>(successors.size)
            for (successor in successors) {
                val result = successor.evaluate(values)
                if (result.failed) {
                    return propagateFailure(result)
                }
                val value = result.value!!.toLong()
                if (value !in successors.indices) {
                    return ok(false)
                }
                next += value.toInt()
            }
            if (next.toSet().size != next.size) {
                return ok(false)
            }
            var current = 0
            val visited = HashSet<Int>()
            repeat(next.size) {
                if (!visited.add(current)) {
                    return ok(false)
                }
                current = next[current]
            }
            return ok(current == 0 && visited.size == next.size)
        }
    }

    /**
     * Deterministic automaton transition. / 确定性自动机转移。
     *
     * @property fromState 起始状态 / Source state
     * @property value 匹配值 / Matched value
     * @property toState 目标状态 / Target state
     */
    data class AutomatonTransition(
        val fromState: Int,
        val value: Int64,
        val toState: Int
    )

    /**
     * 整数序列自动机约束。/ 每一步转移必须同时匹配当前状态和表达式值。 / Automaton constraint over an integer sequence. A transition must match both the current state and the expression value.
     *
     * @property expressions 输入序列表达式 / Input sequence expressions
     * @property initialState 初始状态 / Initial state
     * @property finalStates 可接受终态 / Accepting final states
     * @property transitions 确定性转移集合 / Deterministic transitions
     */
    data class Automaton(
        val expressions: List<ConstraintProgrammingExpression>,
        val initialState: Int,
        val finalStates: Set<Int>,
        val transitions: List<AutomatonTransition>
    ) : ConstraintProgrammingConstraint {
        override val variables: Set<VariableId>
            get() = expressions.flatMapTo(linkedSetOf()) { it.variables }

        override fun isSatisfied(values: Map<VariableId, Int64>): Ret<Boolean> {
            var state = initialState
            for (expression in expressions) {
                val result = expression.evaluate(values)
                if (result.failed) {
                    return propagateFailure(result)
                }
                val transition = transitions.firstOrNull {
                    it.fromState == state && it.value == result.value
                } ?: return ok(false)
                state = transition.toState
            }
            return ok(state in finalStates)
        }
    }

    /**
     * 储液池容量约束。 / Reservoir level constraint.
     *
     * 事件按非递减时间顺序应用。 / Events are applied in non-decreasing time order.
     *
     * @property events 液位变化事件 / Level-change events
     * @property initialLevel 初始液位 / Initial level
     * @property minimumLevel 最低液位 / Minimum level
     * @property maximumLevel 最高液位 / Maximum level
     */
    data class Reservoir(
        val events: List<Event>,
        val initialLevel: Int64,
        val minimumLevel: Int64,
        val maximumLevel: Int64
    ) : ConstraintProgrammingConstraint {
        /**
         * A level change at an integer time. / 整数时间点上的液位变化。
         *
         * @property time 事件时间表达式 / Event time expression
         * @property levelChange 液位变化表达式 / Level-change expression
         */
        data class Event(
            val time: ConstraintProgrammingExpression,
            val levelChange: ConstraintProgrammingExpression
        )

        override val variables: Set<VariableId>
            get() = events.flatMapTo(linkedSetOf()) {
                linkedSetOf<VariableId>().apply {
                    addAll(it.time.variables)
                    addAll(it.levelChange.variables)
                }
            }

        override fun isSatisfied(values: Map<VariableId, Int64>): Ret<Boolean> {
            if (minimumLevel > maximumLevel || initialLevel < minimumLevel || initialLevel > maximumLevel) {
                return ok(false)
            }
            val evaluated = ArrayList<Pair<Long, Long>>(events.size)
            for (event in events) {
                val time = event.time.evaluate(values)
                if (time.failed) {
                    return propagateFailure(time)
                }
                val change = event.levelChange.evaluate(values)
                if (change.failed) {
                    return propagateFailure(change)
                }
                evaluated += time.value!!.toLong() to change.value!!.toLong()
            }
            evaluated.sortBy { it.first }
            var level = java.math.BigInteger.valueOf(initialLevel.toLong())
            val minimum = java.math.BigInteger.valueOf(minimumLevel.toLong())
            val maximum = java.math.BigInteger.valueOf(maximumLevel.toLong())
            for ((_, change) in evaluated) {
                level = level.add(java.math.BigInteger.valueOf(change))
                if (level < minimum || level > maximum) {
                    return ok(false)
                }
            }
            return ok(true)
        }
    }

    companion object {
        /**
         * 创建整数等式。 / Create an integer equality.
         *
         * @param expression 左侧表达式 / Left-hand expression
         * @param rhs 右侧整数 / Right-hand integer
         * @return 整数等式或结构化错误 / Integer equality or a structured error
         */
        fun equal(expression: ConstraintProgrammingExpression, rhs: Int64): Ret<IntegerComparison> {
            return ok(IntegerComparison(expression, ConstraintProgrammingComparison.Equal, rhs))
        }

        /**
         * 创建整数小于等于约束。 / Create an integer less-than-or-equal constraint.
         *
         * @param expression 左侧表达式 / Left-hand expression
         * @param rhs 右侧整数 / Right-hand integer
         * @return 整数约束或结构化错误 / Integer constraint or a structured error
         */
        fun lessOrEqual(expression: ConstraintProgrammingExpression, rhs: Int64): Ret<IntegerComparison> {
            return ok(IntegerComparison(expression, ConstraintProgrammingComparison.LessOrEqual, rhs))
        }

        /**
         * 创建整数大于等于约束。 / Create an integer greater-than-or-equal constraint.
         *
         * @param expression 左侧表达式 / Left-hand expression
         * @param rhs 右侧整数 / Right-hand integer
         * @return 整数约束或结构化错误 / Integer constraint or a structured error
         */
        fun greaterOrEqual(expression: ConstraintProgrammingExpression, rhs: Int64): Ret<IntegerComparison> {
            return ok(IntegerComparison(expression, ConstraintProgrammingComparison.GreaterOrEqual, rhs))
        }

        /**
         * 创建 AND 约束。 / Create an AND constraint.
         *
         * @param literals 参与合取的文字 / Literals in the conjunction
         * @return AND 约束 / AND constraint
         */
        fun boolAnd(literals: Iterable<BooleanLiteral>): Ret<BoolAnd> {
            return ok(BoolAnd(literals.toList()))
        }

        /**
         * 创建 OR 约束。 / Create an OR constraint.
         *
         * @param literals 参与析取的文字 / Literals in the disjunction
         * @return OR 约束 / OR constraint
         */
        fun boolOr(literals: Iterable<BooleanLiteral>): Ret<BoolOr> {
            return ok(BoolOr(literals.toList()))
        }

        /**
         * 创建 XOR 约束。 / Create an XOR constraint.
         *
         * @param literals 参与异或的文字 / Literals in the XOR constraint
         * @return XOR 约束 / XOR constraint
         */
        fun boolXor(literals: Iterable<BooleanLiteral>): Ret<BoolXor> {
            return ok(BoolXor(literals.toList()))
        }

        /**
         * 创建蕴含约束。 / Create an implication constraint.
         *
         * @param enforcement 启用文字 / Enforcement literal
         * @param constraint 被蕴含的约束 / Implied constraint
         * @return 蕴含约束 / Implication constraint
         */
        fun implies(
            enforcement: BooleanLiteral,
            constraint: ConstraintProgrammingConstraint
        ): Ret<Implication> {
            return ok(Implication(enforcement, constraint))
        }

        /**
         * 创建文字到文字的蕴含。 / Create a literal-to-literal implication.
         *
         * @param enforcement 启用文字 / Enforcement literal
         * @param consequence 结果文字 / Consequence literal
         * @return 蕴含约束 / Implication constraint
         */
        fun implies(
            enforcement: BooleanLiteral,
            consequence: BooleanLiteral
        ): Ret<Implication> {
            return implies(enforcement, Literal(consequence))
        }

        /**
         * 创建 Reified 约束。 / Create a reified constraint.
         *
         * @param literal 关联文字 / Associated literal
         * @param constraint 关联约束 / Associated constraint
         * @param direction 重ification 方向 / Reification direction
         * @return Reified 约束 / Reified constraint
         */
        fun reified(
            literal: BooleanLiteral,
            constraint: ConstraintProgrammingConstraint,
            direction: ReificationDirection = ReificationDirection.Equivalent
        ): Ret<Reified> {
            return ok(Reified(literal, constraint, direction))
        }

        /**
         * 创建全异约束并验证表达式非空。 / Create all-different after validating non-empty input.
         *
         * @param expressions 待比较表达式 / Expressions to compare
         * @return 全异约束或结构化错误 / All-different constraint or a structured error
         */
        fun allDifferent(expressions: Iterable<ConstraintProgrammingExpression>): Ret<AllDifferent> {
            val list = expressions.toList()
            if (list.isEmpty()) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "AllDifferent 表达式不能为空 / AllDifferent expressions must not be empty"
                )
            }
            return ok(AllDifferent(list))
        }

        /**
         * 创建 Element 约束。 / Create an element constraint.
         *
         * @param index 索引表达式 / Index expression
         * @param values 候选值或表达式 / Candidate values or expressions
         * @param target 目标表达式 / Target expression
         * @return Element 约束或结构化错误 / Element constraint or a structured error
         */
        fun element(
            index: ConstraintProgrammingExpression,
            values: Iterable<*>,
            target: ConstraintProgrammingExpression
        ): Ret<Element> {
            val list = values.toList()
            if (list.isEmpty()) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "Element 数组不能为空 / Element array must not be empty"
                )
            }
            if (list.any { it !is ConstraintProgrammingExpression && it !is Int64 && it !is Long && it !is Int }) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "Element 数组必须包含整数或整数表达式 / Element array must contain integers or integer expressions"
                )
            }
            return ok(Element(index, list, target))
        }

        /**
         * 创建允许赋值表。 / Create an allowed-assignment table.
         *
         * @param expressions 表达式列 / Expression columns
         * @param tuples 允许的元组 / Allowed tuples
         * @return 允许赋值表或结构化错误 / Allowed table or a structured error
         */
        fun allowedAssignments(
            expressions: Iterable<ConstraintProgrammingExpression>,
            tuples: Iterable<Iterable<*>>
        ): Ret<AllowedAssignments> {
            val validated = validateTable(expressions, tuples)
            if (validated.failed) {
                return propagateFailure(validated)
            }
            val (expressionList, tupleList) = validated.value!!
            return ok(AllowedAssignments(expressionList, tupleList))
        }

        /**
         * 创建禁止赋值表。 / Create a forbidden-assignment table.
         *
         * @param expressions 表达式列 / Expression columns
         * @param tuples 禁止的元组 / Forbidden tuples
         * @return 禁止赋值表或结构化错误 / Forbidden table or a structured error
         */
        fun forbiddenAssignments(
            expressions: Iterable<ConstraintProgrammingExpression>,
            tuples: Iterable<Iterable<*>>
        ): Ret<ForbiddenAssignments> {
            val validated = validateTable(expressions, tuples)
            if (validated.failed) {
                return propagateFailure(validated)
            }
            val (expressionList, tupleList) = validated.value!!
            return ok(ForbiddenAssignments(expressionList, tupleList))
        }

        /**
         * 创建回路约束。 / Create a circuit constraint.
         *
         * @param successors 后继表达式 / Successor expressions
         * @return 回路约束或结构化错误 / Circuit constraint or a structured error
         */
        fun circuit(successors: Iterable<ConstraintProgrammingExpression>): Ret<Circuit> {
            val list = successors.toList()
            if (list.isEmpty()) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "Circuit 后继表达式不能为空 / Circuit successor expressions must not be empty"
                )
            }
            return ok(Circuit(list))
        }

        /**
         * 创建自动机约束。 / Create an automaton constraint.
         *
         * @param expressions 输入序列表达式 / Input sequence expressions
         * @param initialState 初始状态 / Initial state
         * @param finalStates 可接受终态 / Accepting final states
         * @param transitions 确定性转移集合 / Deterministic transitions
         * @return 自动机约束或结构化错误 / Automaton constraint or a structured error
         */
        fun automaton(
            expressions: Iterable<ConstraintProgrammingExpression>,
            initialState: Int,
            finalStates: Set<Int>,
            transitions: Iterable<AutomatonTransition>
        ): Ret<Automaton> {
            val expressionList = expressions.toList()
            val transitionList = transitions.toList()
            if (expressionList.isEmpty() || finalStates.isEmpty() || transitionList.isEmpty()) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "Automaton 输入不能为空 / Automaton expressions, final states, and transitions must not be empty"
                )
            }
            val duplicate = transitionList
                .groupBy { it.fromState to it.value }
                .entries
                .firstOrNull { it.value.size > 1 }
            if (duplicate != null) {
                val (fromState, value) = duplicate.key
                return Failed(
                    ErrorCode.IllegalArgument,
                    "Automaton 转移必须对 (fromState, value) 确定：($fromState, $value) 存在重复转移 / " +
                        "Automaton transitions must be deterministic for (fromState, value): duplicate key ($fromState, $value)"
                )
            }
            return ok(Automaton(expressionList, initialState, finalStates.toSet(), transitionList))
        }

        /**
         * 创建储液池约束。 / Create a reservoir constraint.
         *
         * @param events 液位变化事件 / Level-change events
         * @param initialLevel 初始液位 / Initial level
         * @param minimumLevel 最低液位 / Minimum level
         * @param maximumLevel 最高液位 / Maximum level
         * @return 储液池约束或结构化错误 / Reservoir constraint or a structured error
         */
        fun reservoir(
            events: Iterable<Reservoir.Event>,
            initialLevel: Int64,
            minimumLevel: Int64,
            maximumLevel: Int64
        ): Ret<Reservoir> {
            val eventList = events.toList()
            if (eventList.isEmpty()) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "Reservoir 事件不能为空 / Reservoir events must not be empty"
                )
            }
            if (minimumLevel > maximumLevel || initialLevel < minimumLevel || initialLevel > maximumLevel) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "Reservoir 液位范围无效 / Reservoir level bounds are invalid"
                )
            }
            return ok(Reservoir(eventList, initialLevel, minimumLevel, maximumLevel))
        }

        private fun validateTable(
            expressions: Iterable<ConstraintProgrammingExpression>,
            tuples: Iterable<Iterable<*>>
        ): Ret<Pair<List<ConstraintProgrammingExpression>, List<List<Int64>>>> {
            val expressionList = expressions.toList()
            if (expressionList.isEmpty()) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "Table 表达式不能为空 / Table expressions must not be empty"
                )
            }
            val tupleList = ArrayList<List<Int64>>()
            for (tuple in tuples) {
                val converted = ArrayList<Int64>()
                for (value in tuple) {
                    val integer = when (value) {
                        is Int64 -> value
                        is Long -> Int64(value)
                        is Int -> Int64(value.toLong())
                        else -> return Failed(
                            ErrorCode.IllegalArgument,
                            "Table 元组必须包含整数 / Table tuples must contain integers"
                        )
                    }
                    converted += integer
                }
                if (converted.size != expressionList.size) {
                    return Failed(
                        ErrorCode.IllegalArgument,
                        "Table 元组长度与表达式数量不一致 / Table tuple arity does not match expression count"
                    )
                }
                tupleList += converted
            }
            if (tupleList.isEmpty()) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "Table 元组不能为空 / Table tuples must not be empty"
                )
            }
            return ok(expressionList to tupleList)
        }
    }
}
