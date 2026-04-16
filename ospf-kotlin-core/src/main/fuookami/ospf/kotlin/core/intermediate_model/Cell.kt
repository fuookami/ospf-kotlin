package fuookami.ospf.kotlin.core.intermediate_model

import fuookami.ospf.kotlin.core.variable.Token
import fuookami.ospf.kotlin.core.variable.VariableItemKey
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber

/**
 * Generic cell interface skeleton - C2-2.6 declaration layer.
 * Phantom type parameter V for API signature; numerical kernel remains Flt64.
 */
interface CellOf<V : RealNumber<V>> {
    fun evaluate(): Flt64?
    fun evaluate(solution: List<Flt64>): Flt64?
    fun evaluate(solution: Map<VariableItemKey, Flt64>): Flt64?
}

/**
 * Legacy typealias for Flt64-specific Cell.
 * Preserved for backward compatibility.
 */
typealias Cell = CellOf<Flt64>

/**
 * Generic linear cell interface skeleton - C2-2.6 declaration layer.
 */
interface LinearCellOf<V : RealNumber<V>> : CellOf<V> {
    val coefficient: Flt64
    val token: Token
}

/**
 * Legacy typealias for Flt64-specific LinearCell interface.
 */
typealias LinearCellI = LinearCellOf<Flt64>

/**
 * Generic quadratic cell interface skeleton - C2-2.6 declaration layer.
 */
interface QuadraticCellOf<V : RealNumber<V>> : CellOf<V> {
    val coefficient: Flt64
    val token1: Token
    val token2: Token?
}

/**
 * Legacy typealias for Flt64-specific QuadraticCell interface.
 */
typealias QuadraticCellI = QuadraticCellOf<Flt64>

// Legacy implementation classes (numerical kernel unchanged)
class LinearCellImpl(
    private val tokenTable: AbstractTokenTable,
    val coefficient: Flt64,
    val token: Token
) : Cell {
    override fun evaluate(): Flt64? {
        return token.result?.let { coefficient * it }
    }

    override fun evaluate(solution: List<Flt64>): Flt64? {
        return tokenTable.indexOf(token)?.let {
            coefficient * solution[it]
        }
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

class QuadraticCellImpl(
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

    override fun evaluate(solution: List<Flt64>): Flt64? {
        return if (token2 == null) {
            tokenTable.indexOf(token1)?.let {
                coefficient * solution[it]
            }
        } else {
            tokenTable.indexOf(token1)?.let { index1 ->
                tokenTable.indexOf(token2)?.let { index2 ->
                    coefficient * solution[index1] * solution[index2]
                }
            }
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

// Legacy typealiases for implementation classes (most code uses these)
typealias LinearCell = LinearCellImpl
typealias QuadraticCell = QuadraticCellImpl



