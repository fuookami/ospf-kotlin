package fuookami.ospf.kotlin.core.frontend.model

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

typealias Solution = List<Flt64>

interface ModelInterface {
    val objectCategory: ObjectCategory

    fun addVar(item: Item<*, *>)
    fun addVars(items: Combination<*, *, *>)
    fun addVars(items: CombinationView<*, *>)
    fun remove(item: Item<*, *>)

    fun addConstraint(inequality: Inequality<*>, name: String? = null, displayName: String? = null)

    fun addObject(category: ObjectCategory, polynomial: Polynomial<*>, name: String? = null, displayName: String? = null)

    fun minimize(polynomial: Polynomial<*>, name: String? = null, displayName: String? = null) {
        addObject(ObjectCategory.Minimum, polynomial, name, displayName)
    }

    fun maximize(polynomial: Polynomial<*>, name: String? = null, displayName: String? = null) {
        addObject(ObjectCategory.Maximum, polynomial, name, displayName)
    }

    fun setSolution(solution: Solution)
    fun setSolution(solution: Map<Item<*, *>, Flt64>)
    fun clearSolution()
}
