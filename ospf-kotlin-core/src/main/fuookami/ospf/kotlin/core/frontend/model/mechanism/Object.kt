package fuookami.ospf.kotlin.core.frontend.model.mechanism

import fuookami.ospf.kotlin.core.frontend.expression.monomial.*

sealed interface Object<C : Category>

class SingleObject<C : Category>(
    val category: ObjectCategory,
    val subObjects: List<SubObject<C>>
) : Object<C> {
}

class MultiObject<C : Category> {

}
