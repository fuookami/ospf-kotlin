package fuookami.ospf.kotlin.core.frontend.model.mechanism

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*

sealed interface Cell<C : Category> {
    fun value(): Flt64?
    fun value(solution: List<Flt64>): Flt64
    fun value(solution: Map<ItemKey, Flt64>): Flt64?
}

class LinearCell(
    private val parent: LinearMetaModel,
    val coefficient: Flt64,
    val token: Token
) : Cell<Linear> {
    override fun value(): Flt64? {
        return token.result?.let { coefficient * it }
    }

    override fun value(solution: List<Flt64>): Flt64 {
        return coefficient * solution[parent.tokens.solverIndexMap[token.solverIndex]!!]
    }

    override fun value(solution: Map<ItemKey, Flt64>): Flt64? {
        return solution[token.key]?.let { coefficient * it }
    }
}
