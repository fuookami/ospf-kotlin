package fuookami.ospf.kotlin.core.frontend.model

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

typealias Solution = List<Flt64>

interface ModelInterface {
    val objectCategory: ObjectCategory

    fun addVar(item: AbstractVariableItem<*, *>)
    fun addVars(items: Iterable<AbstractVariableItem<*, *>>)
    fun remove(item: AbstractVariableItem<*, *>)

    fun addConstraint(
        inequality: Inequality<*, *, *>,
        name: String? = null,
        displayName: String? = null
    )

    fun addObject(
        category: ObjectCategory,
        polynomial: Polynomial<*, *, *, *>,
        name: String? = null,
        displayName: String? = null
    )

    fun <T : RealNumber<T>> addObject(
        category: ObjectCategory,
        constant: T,
        name: String? = null,
        displayName: String? = null
    )

    fun minimize(
        polynomial: Polynomial<*, *, *, *>,
        name: String? = null,
        displayName: String? = null
    ) {
        addObject(ObjectCategory.Minimum, polynomial, name, displayName)
    }

    fun maximize(
        polynomial: Polynomial<*, *, *, *>,
        name: String? = null,
        displayName: String? = null
    ) {
        addObject(ObjectCategory.Maximum, polynomial, name, displayName)
    }

    fun <T : RealNumber<T>> minimize(
        constant: T,
        name: String? = null,
        displayName: String? = null
    ) {
        addObject(ObjectCategory.Minimum, constant, name, displayName)
    }

    fun <T : RealNumber<T>> maximize(
        constant: T,
        name: String? = null,
        displayName: String? = null
    ) {
        addObject(ObjectCategory.Maximum, constant, name, displayName)
    }

    fun setSolution(solution: Solution)
    fun setSolution(solution: Map<AbstractVariableItem<*, *>, Flt64>)
    fun clearSolution()
}
