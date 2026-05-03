@file:Suppress("unused")

package fuookami.ospf.kotlin.core.model.callback

import fuookami.ospf.kotlin.core.model.basic.Model
import fuookami.ospf.kotlin.core.model.basic.MulObj
import fuookami.ospf.kotlin.core.model.basic.MultiObjectLocation
import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.token.AbstractMutableTokenTable
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.utils.functional.Extractor
import fuookami.ospf.kotlin.math.functional.sumOf
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.utils.functional.Order

interface AbstractCallBackModelInterfaceV<Obj, V, TV> : Model<TV>, AutoCloseable
        where TV : RealNumber<TV>, TV : NumberField<TV> {
    val defaultObjective: V

    val tokens: AbstractMutableTokenTable<TV>
    val constraints: List<Pair<Extractor<Boolean?, Solution<TV>>, String>>

    val objectiveFunctions: List<Pair<Extractor<Obj?, Solution<TV>>, String>>

    fun initialSolutions(initialSolutionAmount: UInt64 = UInt64.one): List<Solution<TV>> {
        return emptyList()
    }

    fun operation(lhs: V, rhs: V): V

    fun objectiveValue(): V
    fun objectiveValue(obj: Obj): V

    fun objective(solution: Solution<TV>): V? {
        var obj = objectiveValue()
        for ((objectiveFunction, _) in objectiveFunctions) {
            val thisObj = objectiveFunction(solution) ?: return null
            obj = operation(obj, objectiveValue(thisObj))
        }
        return obj
    }

    fun compareObjective(lhs: V, rhs: V): Order?

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("comparePartialObjective")
    fun compareObjective(lhs: V?, rhs: V?): Order? {
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

    fun constraintSatisfied(solution: Solution<TV>): Boolean?

    fun flush()

    override fun close() {
        tokens.close()
    }
}

/** Generic callback interface where objective and token value types are both V. */
typealias AbstractCallBackModelInterface<Obj, V> = AbstractCallBackModelInterfaceV<Obj, V, V>

interface CallBackModelInterfaceV<V> : AbstractCallBackModelInterfaceV<V, V, V> where V : RealNumber<V>, V : NumberField<V> {
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

/** Flt64 convenience specialization. */
typealias CallBackModelInterface = CallBackModelInterfaceV<Flt64>

interface MultiObjectiveModelInterfaceV<V> : AbstractCallBackModelInterfaceV<MulObj, List<V>, V> where V : RealNumber<V>, V : NumberField<V> {
    val objectiveLocation: List<MultiObjectLocation>
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

    override fun objectiveValue(obj: MulObj): List<V> {
        return (0 until objectiveSize).map { index ->
            obj.fold(converter().zero) { acc, (loc, objective) ->
                if (index == loc.priority.toInt()) {
                    acc + converter().intoValue(objective) * converter().intoValue(loc.weight)
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

/** Flt64 convenience specialization. */
typealias MultiObjectiveModelInterface = MultiObjectiveModelInterfaceV<Flt64>