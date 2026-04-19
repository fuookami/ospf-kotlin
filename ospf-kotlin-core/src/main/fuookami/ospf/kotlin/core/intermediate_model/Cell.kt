package fuookami.ospf.kotlin.core.intermediate_model

import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.Token
import fuookami.ospf.kotlin.core.variable.TokenF64
import fuookami.ospf.kotlin.core.variable.VariableItemKey
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber

/**
 * Generic Cell<V> - V is a real type parameter with dual-view access.
 *
 * Dual-view pattern:
 *   - Flt64 view: `evaluateF64()`, `coefficientF64` (solver-compatible, internal)
 *   - V-typed view: `evaluate()`, `coefficient` (type-safe, public API)
 */
interface Cell<V : RealNumber<V>> {
    /** V-typed evaluation (primary public API). */
    fun evaluate(): V?
    fun evaluate(solution: List<Flt64>): V?
    fun evaluate(solution: Map<VariableItemKey, Flt64>): V?

    /** Flt64 view of evaluation (solver-compatible, internal). */
    fun evaluateF64(): Flt64?
    fun evaluateF64(solution: List<Flt64>): Flt64?
    fun evaluateF64(solution: Map<VariableItemKey, Flt64>): Flt64?

    /** V-typed evaluation via explicit IntoValue<V> conversion. Kept for backward compatibility. */
    fun evaluateAsV(converter: IntoValue<V>): V? = evaluateF64()?.let { converter.intoValue(it) }
}

typealias CellF64 = Cell<Flt64>

interface LinearCell<V : RealNumber<V>> : Cell<V> {
    /** V-typed coefficient (primary public API). */
    val coefficient: V
    val token: Token<V>

    /** Flt64 view of coefficient (solver-compatible, internal). */
    val coefficientF64: Flt64

    /** V-typed coefficient via explicit IntoValue<V> conversion. Kept for backward compatibility. */
    fun coefficientAsV(converter: IntoValue<V>): V = converter.intoValue(coefficientF64)
}

typealias LinearCellF64 = LinearCell<Flt64>
typealias LinearCellI = LinearCell<Flt64>

interface QuadraticCell<V : RealNumber<V>> : Cell<V> {
    /** V-typed coefficient (primary public API). */
    val coefficient: V
    val token1: Token<V>
    val token2: Token<V>?

    /** Flt64 view of coefficient (solver-compatible, internal). */
    val coefficientF64: Flt64

    /** V-typed coefficient via explicit IntoValue<V> conversion. Kept for backward compatibility. */
    fun coefficientAsV(converter: IntoValue<V>): V = converter.intoValue(coefficientF64)
}

typealias QuadraticCellF64 = QuadraticCell<Flt64>
typealias QuadraticCellI = QuadraticCell<Flt64>

class LinearCellImpl(
    private val tokenTable: LegacyAbstractTokenTable,
    private val _coefficient: Flt64,
    override val token: TokenF64
) : LinearCell<Flt64> {
    override val coefficientF64: Flt64 get() = _coefficient
    @Suppress("UNCHECKED_CAST")
    override val coefficient: Flt64 get() = _coefficient

    override fun evaluate(): Flt64? {
        return token.resultF64?.let { _coefficient * it }
    }

    override fun evaluate(solution: List<Flt64>): Flt64? {
        return tokenTable.indexOf(token)?.let {
            _coefficient * solution[it]
        }
    }

    override fun evaluate(solution: Map<VariableItemKey, Flt64>): Flt64? {
        return solution[token.key]?.let { _coefficient * it }
    }

    override fun evaluateF64(): Flt64? = evaluate()
    override fun evaluateF64(solution: List<Flt64>): Flt64? = evaluate(solution)
    override fun evaluateF64(solution: Map<VariableItemKey, Flt64>): Flt64? = evaluate(solution)

    override fun toString(): String {
        return if (_coefficient eq Flt64.one) {
            token.name
        } else {
            "$_coefficient * ${token.name}"
        }
    }
}

class QuadraticCellImpl(
    private val tokenTable: LegacyAbstractTokenTable,
    private val _coefficient: Flt64,
    override val token1: TokenF64,
    override val token2: TokenF64? = null
) : QuadraticCell<Flt64> {
    override val coefficientF64: Flt64 get() = _coefficient
    @Suppress("UNCHECKED_CAST")
    override val coefficient: Flt64 get() = _coefficient

    override fun evaluate(): Flt64? {
        return if (token2 == null) {
            token1.resultF64?.let { _coefficient * it }
        } else {
            token1.resultF64?.let { result1 -> token2.resultF64?.let { result2 -> _coefficient * result1 * result2 } }
        }
    }

    override fun evaluate(solution: List<Flt64>): Flt64? {
        return if (token2 == null) {
            tokenTable.indexOf(token1)?.let {
                _coefficient * solution[it]
            }
        } else {
            tokenTable.indexOf(token1)?.let { index1 ->
                tokenTable.indexOf(token2)?.let { index2 ->
                    _coefficient * solution[index1] * solution[index2]
                }
            }
        }
    }

    override fun evaluate(solution: Map<VariableItemKey, Flt64>): Flt64? {
        return if (token2 == null) {
            solution[token1.key]?.let { _coefficient * it }
        } else {
            solution[token1.key]?.let { result1 -> solution[token2.key]?.let { result2 -> _coefficient * result1 * result2 } }
        }
    }

    override fun evaluateF64(): Flt64? = evaluate()
    override fun evaluateF64(solution: List<Flt64>): Flt64? = evaluate(solution)
    override fun evaluateF64(solution: Map<VariableItemKey, Flt64>): Flt64? = evaluate(solution)

    override fun toString(): String {
        return if (token2 == null) {
            if (_coefficient eq Flt64.one) {
                token1.name
            } else {
                "$_coefficient * ${token1.name}"
            }
        } else {
            if (_coefficient eq Flt64.one) {
                "${token1.name} * ${token2.name}"
            } else {
                "$_coefficient * ${token1.name} * ${token2.name}"
            }
        }
    }
}
