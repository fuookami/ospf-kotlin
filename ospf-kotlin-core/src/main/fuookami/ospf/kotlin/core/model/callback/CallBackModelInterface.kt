/**
 * 回调模型接口
 * Call-back model interfaces
 */
@file:Suppress("unused")
package fuookami.ospf.kotlin.core.model.callback

import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.utils.functional.Extractor
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.functional.sumOf
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.token.AbstractMutableTokenTable
import fuookami.ospf.kotlin.core.solver.value.IntoValue

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
    val defaultObjective: ObjValue

    val tokens: AbstractMutableTokenTable<SolutionValue>
    val constraints: List<Pair<Extractor<Boolean?, Solution<SolutionValue>>, String>>

    val objectiveFunctions: List<Pair<Extractor<Obj?, Solution<SolutionValue>>, String>>

    fun initialSolutions(initialSolutionAmount: UInt64 = UInt64.one): List<Solution<SolutionValue>> {
        return emptyList()
    }

    fun operation(lhs: ObjValue, rhs: ObjValue): ObjValue

    fun objectiveValue(): ObjValue
    fun objectiveValue(obj: Obj): ObjValue

    fun objective(solution: Solution<SolutionValue>): ObjValue? {
        var obj = objectiveValue()
        for ((objectiveFunction, _) in objectiveFunctions) {
            val thisObj = objectiveFunction(solution) ?: return null
            obj = operation(obj, objectiveValue(thisObj))
        }
        return obj
    }

    fun compareObjective(lhs: ObjValue, rhs: ObjValue): Order?

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

    fun constraintSatisfied(solution: Solution<SolutionValue>): Boolean?

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

    /** Provide the IntoValue<V> converter for this V type. */
    fun converter(): IntoValue<V>

    /** Provide negative infinity for this V type. */
    fun negativeInfinity(): V

    /** Provide positive infinity for this V type. */
    fun infinity(): V
}

/**
 * 多目标回调模型接口，支持多个优先级和权重的目标函数。
 * Multi-objective call-back model interface supporting multiple priority-weighted objective functions.
 *
 * @param V 数值类型 / The numeric type
 */
interface MultiObjectiveModelInterface<V> : AbstractCallBackModelInterface<List<Pair<MultiObjectLocation<V>, V>>, List<V>, V> where V : RealNumber<V>, V : NumberField<V> {
    val objectiveLocation: List<MultiObjectLocation<V>>
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

    /** Provide the IntoValue<V> converter for this V type. */
    fun converter(): IntoValue<V>

    /** Provide negative infinity for this V type. */
    fun negativeInfinity(): V

    /** Provide positive infinity for this V type. */
    fun infinity(): V
}
