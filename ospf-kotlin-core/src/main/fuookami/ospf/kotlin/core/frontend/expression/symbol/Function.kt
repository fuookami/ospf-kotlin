package fuookami.ospf.kotlin.core.frontend.expression.symbol

import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

interface Function<C: Category>: Symbol<C> {
    fun register(tokenTable: TokenTable<C>)
    fun register(model: Model<C>)
}
