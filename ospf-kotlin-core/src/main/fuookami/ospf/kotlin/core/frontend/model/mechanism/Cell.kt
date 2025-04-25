package fuookami.ospf.kotlin.core.frontend.model.mechanism

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.core.frontend.variable.*

sealed interface Cell {
    fun evaluate(): Flt64?
    fun evaluate(solution: List<Flt64>): Flt64
    fun evaluate(solution: Map<VariableItemKey, Flt64>): Flt64?
}

class LinearCell(
    private val tokenTable: AbstractTokenTable,
    val coefficient: Flt64,
    val token: Token
) : Cell {
    override fun evaluate(): Flt64? {
        return token.result?.let { coefficient * it }
    }

    override fun evaluate(solution: List<Flt64>): Flt64 {
        return coefficient * solution[tokenTable.tokenIndexMap[token]!!]
    }

    override fun evaluate(solution: Map<VariableItemKey, Flt64>): Flt64? {
        return solution[token.key]?.let { coefficient * it }
    }

    override fun toString(): String {
        return if (coefficient eq Flt64.one) {
            token.name
        } else {
            "$coefficient * ${token.name}"
        }
    }
}

class QuadraticCell(
    private val tokenTable: AbstractTokenTable,
    val coefficient: Flt64,
    val token1: Token,
    val token2: Token? = null
) : Cell {
    override fun evaluate(): Flt64? {
        return if (token2 == null) {
            token1.result?.let { coefficient * it }
        } else {
            token1.result?.let { result1 -> token2.result?.let { result2 -> coefficient * result1 * result2 } }
        }
    }

    override fun evaluate(solution: List<Flt64>): Flt64 {
        return if (token2 == null) {
            coefficient * solution[tokenTable.tokenIndexMap[token1]!!]
        } else {
            coefficient * solution[tokenTable.tokenIndexMap[token1]!!] * solution[tokenTable.tokenIndexMap[token2]!!]
        }
    }

    override fun evaluate(solution: Map<VariableItemKey, Flt64>): Flt64? {
        return if (token2 == null) {
            solution[token1.key]?.let { coefficient * it }
        } else {
            solution[token1.key]?.let { result1 -> solution[token2.key]?.let { result2 -> coefficient * result1 * result2 } }
        }
    }

    override fun toString(): String {
        return if (token2 == null) {
            if (coefficient eq Flt64.one) {
                token1.name
            } else {
                "$coefficient * ${token1.name}"
            }
        } else {
            if (coefficient eq Flt64.one) {
                "${token1.name} * ${token2.name}"
            } else {
                "$coefficient * ${token1.name} * ${token2.name}"
            }
        }
    }
}
