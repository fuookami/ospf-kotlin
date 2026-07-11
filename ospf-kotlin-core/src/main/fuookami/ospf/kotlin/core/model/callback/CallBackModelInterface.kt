/**
 * 回调模型接口定义
 * Call-back model interface definitions
*/
@file:Suppress("unused")
package fuookami.ospf.kotlin.core.model.callback

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.token.AbstractMutableTokenTable
import fuookami.ospf.kotlin.core.solver.value.IntoValue

/**
 * 回调模型接口
 * Call-back model interfaces
*/

/**
 * 回调模型抽象接口，定义约束、目标函数和解比较的通用能力。
 * Abstract call-back model interface defining common capabilities for constraints, objective functions, and solution comparison.
 *
 * @param Obj          目标值类型 / The objective value type
 * @param ObjValue     目标聚合值类型 / The aggregated objective value type
 * @param SolutionValue 解值类型 / The solution value type
*/
interface AbstractCallBackModelInterface<Obj, ObjValue, SolutionValue> : Model<SolutionValue>, AutoCloseable
        where SolutionValue : RealNumber<SolutionValue>, SolutionValue : NumberField<SolutionValue> {

    /** 默认目标值（用于无解时的初始值） / Default objective value (used as initial value when no solution exists) */
    val defaultObjective: ObjValue

    /** 可变令牌表 / The mutable token table */
    val tokens: AbstractMutableTokenTable<SolutionValue>

    /** 约束列表（提取器与名称的配对） / Constraint list (pairs of extractor and name) */
    val constraints: List<Pair<Extractor<Boolean?, Solution<SolutionValue>>, String>>

    /** 目标函数列表（提取器与名称的配对） / Objective function list (pairs of extractor and name) */
    val objectiveFunctions: List<Pair<Extractor<Obj?, Solution<SolutionValue>>, String>>

    /**
     * 生成初始解列表，默认为空。
     * Generate a list of initial solutions, empty by default.
     *
     * @param initialSolutionAmount 初始解数量 / The number of initial solutions
     * @return 初始解列表 / The list of initial solutions
    */
    fun initialSolutions(initialSolutionAmount: UInt64 = UInt64.one): List<Solution<SolutionValue>> {
        return emptyList()
    }

    /**
     * 合并两个目标值。
     * Combine two objective values.
     *
     * @param lhs 左侧目标值 / The left-hand side objective value
     * @param rhs 右侧目标值 / The right-hand side objective value
     * @return 合并后的目标值 / The combined objective value
    */
    fun operation(lhs: ObjValue, rhs: ObjValue): ObjValue

    /**
     * 获取零目标值（单位元）。
     * Get the zero objective value (identity element).
     *
     * @return 零目标值 / The zero objective value
    */
    fun objectiveValue(): ObjValue

    /**
     * 将原始目标对象转换为目标聚合值。
     * Convert a raw objective object into an aggregated objective value.
     *
     * @param obj 原始目标对象 / The raw objective object
     * @return 聚合后的目标值 / The aggregated objective value
    */
    fun objectiveValue(obj: Obj): ObjValue

    /**
     * 计算解的目标值，遍历所有目标函数并聚合。
     * Compute the objective value of a solution by aggregating all objective functions.
     *
     * @param solution 待计算的解 / The solution to evaluate
     * @return 聚合后的目标值，任一目标函数返回 null 时整体返回 null / The aggregated objective value, or null if any objective function returns null
    */
    fun objective(solution: Solution<SolutionValue>): ObjValue? {
        var obj = objectiveValue()
        for ((objectiveFunction, _) in objectiveFunctions) {
            val thisObj = objectiveFunction(solution) ?: return null
            obj = operation(obj, objectiveValue(thisObj))
        }
        return obj
    }

    /**
     * 比较两个非空目标值的优先级顺序。
     * Compare the ordering of two non-null objective values.
     *
     * @param lhs 左侧目标值 / The left-hand side objective value
     * @param rhs 右侧目标值 / The right-hand side objective value
     * @return 比较结果 / The comparison result
    */
    fun compareObjective(lhs: ObjValue, rhs: ObjValue): Order?

    /**
     * 比较两个可空目标值的优先级顺序，null 视为最差。
     * Compare the ordering of two nullable objective values, treating null as worst.
     *
     * @param lhs 左侧目标值（可为 null） / The left-hand side objective value (nullable)
     * @param rhs 右侧目标值（可为 null） / The right-hand side objective value (nullable)
     * @return 比较结果，任一为 null 时另一个更优 / The comparison result; when either is null, the other is preferred
    */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("comparePartialObjective")
    fun compareObjective(lhs: ObjValue?, rhs: ObjValue?): Order? {
        return if (lhs != null && rhs == null) {
            Order.Less()
        } else if (lhs == null && rhs != null) {
            Order.Greater()
        } else if (lhs != null && rhs != null) {
            compareObjective(lhs, rhs)
        } else {
            null
        }
    }

    /**
     * 检查解是否满足所有约束。
     * Check whether a solution satisfies all constraints.
     *
     * @param solution 待检查的解 / The solution to check
     * @return `true` 表示满足，`false` 表示违反，`null` 表示无法确定 / `true` if satisfied, `false` if violated, `null` if undetermined
    */
    fun constraintSatisfied(solution: Solution<SolutionValue>): Boolean?

    /**
     * 刷新模型内部状态。
     * Flush the internal state of the model.
    */
    fun flush()

    override fun close() {
        tokens.close()
    }
}

/**
 * 单目标回调模型接口，目标值和解值类型相同。
 * Single-objective call-back model interface where objective and solution value types are the same.
 *
 * @param V 数值类型 / The numeric type
*/
interface CallBackModelInterface<V> : AbstractCallBackModelInterface<V, V, V> where V : RealNumber<V>, V : NumberField<V> {
    override val defaultObjective: V
        get() = if (objectCategory == ObjectCategory.Minimum) {
            negativeInfinity()
        } else {
            infinity()
        }

    override fun objectiveValue(): V {
        return converter().zero
    }

    override fun objectiveValue(obj: V): V {
        return obj
    }

    override fun operation(lhs: V, rhs: V): V {
        return lhs + rhs
    }

    override fun constraintSatisfied(solution: Solution<V>): Boolean? {
        val conv = converter()
        for (token in tokens.tokens) {
            val index = tokens.indexOf(token) ?: continue
            if (!token.containsInBounds(solution[index], conv)) {
                return false
            }
        }
        for ((constraint, _) in constraints) {
            when (constraint(solution)) {
                true -> {}
                false -> { return false }
                null -> { return null }
            }
        }
        return true
    }

    /**
     * 提供当前 V 类型的 IntoValue<V> 转换器。
     * Provide the IntoValue<V> converter for this V type.
     *
     * @return the value converter / 值转换器
    */
    fun converter(): IntoValue<V>

    /**
     * 提供当前 V 类型的负无穷值。
     * Provide negative infinity for this V type.
     *
     * @return negative infinity value / 负无穷值
    */
    fun negativeInfinity(): V

    /**
     * 提供当前 V 类型的正无穷值。
     * Provide positive infinity for this V type.
     *
     * @return positive infinity value / 正无穷值
    */
    fun infinity(): V
}

/**
 * 多目标回调模型接口，支持多个优先级和权重的目标函数。
 * Multi-objective call-back model interface supporting multiple priority-weighted objective functions.
 *
 * @param V 数值类型 / The numeric type
*/
interface MultiObjectiveModelInterface<V> : AbstractCallBackModelInterface<List<Pair<MultiObjectLocation<V>, V>>, List<V>, V> where V : RealNumber<V>, V : NumberField<V> {

    /** 目标函数位置列表 / Objective function location list */
    val objectiveLocation: List<MultiObjectLocation<V>>

    /** 目标函数数量 / Objective function count */
    val objectiveSize get() = objectiveLocation.size

    override val defaultObjective: List<V>
        get() = if (objectCategory == ObjectCategory.Minimum) {
            (0 until objectiveSize).map { negativeInfinity() }
        } else {
            (0 until objectiveSize).map { infinity() }
        }

    override fun objectiveValue(): List<V> {
        return (0 until objectiveSize).map { converter().zero }
    }

    override fun objectiveValue(obj: List<Pair<MultiObjectLocation<V>, V>>): List<V> {
        return (0 until objectiveSize).map { index ->
            obj.fold(converter().zero) { acc, (loc, objective) ->
                if (index == loc.priority.toInt()) {
                    acc + objective * loc.weight
                } else {
                    acc
                }
            }
        }
    }

    override fun operation(lhs: List<V>, rhs: List<V>): List<V> {
        return (0 until objectiveSize).map { lhs[it] + rhs[it] }
    }

    override fun constraintSatisfied(solution: Solution<V>): Boolean? {
        val conv = converter()
        for (token in tokens.tokens) {
            val index = tokens.indexOf(token) ?: continue
            if (!token.containsInBounds(solution[index], conv)) {
                return false
            }
        }
        for ((constraint, _) in constraints) {
            when (constraint(solution)) {
                true -> {}
                false -> { return false }
                null -> { return null }
            }
        }
        return true
    }

    /**
     * 提供当前 V 类型的 IntoValue<V> 转换器。
     * Provide the IntoValue<V> converter for this V type.
     *
     * @return the value converter / 值转换器
    */
    fun converter(): IntoValue<V>

    /**
     * 提供当前 V 类型的负无穷值。
     * Provide negative infinity for this V type.
     *
     * @return negative infinity value / 负无穷值
    */
    fun negativeInfinity(): V

    /**
     * 提供当前 V 类型的正无穷值。
     * Provide positive infinity for this V type.
     *
     * @return positive infinity value / 正无穷值
    */
    fun infinity(): V
}
