package fuookami.ospf.kotlin.core.frontend.model.mechanism

import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.utils.math.Flt64

sealed interface Cell<C : Category> {
    fun value(): Flt64?
    fun value(solution: List<Flt64>): Flt64?
    fun value(solution: Map<ItemKey, Flt64>): Flt64?
}

class LinearCell(
    val coefficient: Flt64,
    val token: Token
) : Cell<Linear> {
    override fun value(): Flt64? {
        val result = token.result
        return if (result != null) {
            coefficient * result
        } else {
            null
        }
    }

    override fun value(solution: List<Flt64>): Flt64 {
        return coefficient * solution[token.solverIndex]
    }

    override fun value(solution: Map<ItemKey, Flt64>): Flt64? {
        val result = solution[token.key]
        return if (result != null) {
            coefficient * result
        } else {
            null
        }
    }
}
