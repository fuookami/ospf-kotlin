package fuookami.ospf.kotlin.core.model.mechanism

import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.model.intermediate.Cell
import fuookami.ospf.kotlin.core.model.intermediate.LinearCellF64
import fuookami.ospf.kotlin.core.model.intermediate.LinearCell
import fuookami.ospf.kotlin.core.model.intermediate.QuadraticCellF64
import fuookami.ospf.kotlin.core.model.intermediate.QuadraticCell
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.token.AbstractTokenTable
import fuookami.ospf.kotlin.core.token.AbstractTokenTableF64
import fuookami.ospf.kotlin.core.token.LinearFlattenDataF64
import fuookami.ospf.kotlin.core.token.QuadraticFlattenDataF64
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * Generic SubObject<V> - V is a real type parameter with dual-view access.
 *
 * Dual-view pattern:
 *   - Flt64 view: `evaluateF64()`, `constantF64` (solver-compatible, internal)
 *   - V-typed view: `evaluate()`, `constant` (type-safe, public API)
 *
 * Internal storage is Flt64; conversion to V happens at the boundary via IntoValue<V>.
 */
sealed class SubObject<V : RealNumber<V>>(
    val category: ObjectCategory,
    val name: String = ""
) {
    abstract val cells: List<Cell<V>>

    /** Flt64 view of constant (solver-compatible, internal). */
    abstract val constantF64: Flt64

    /** V-typed constant (primary public API). Uses IntoValue<V> conversion at boundary. */
    abstract val constant: V

    /** Flt64 view of evaluation (solver-compatible, internal). */
    fun evaluateF64(): Flt64? {
        var ret = constantF64
        for (cell in cells) {
            ret += cell.evaluateF64() ?: return null
        }
        return ret
    }

    fun evaluateF64(results: Solution): Flt64? {
        var ret = constantF64
        for (cell in cells) {
            ret += cell.evaluateF64(results) ?: return null
        }
        return ret
    }

    /** V-typed evaluation (primary public API). */
    abstract fun evaluate(): V?
    abstract fun evaluate(results: Solution): V?

    /** V-typed evaluation via explicit IntoValue<V> conversion. Kept for backward compatibility. */
    fun evaluateAsV(converter: IntoValue<V>): V? = evaluateF64()?.let { converter.intoValue(it) }

    /** V-typed constant via explicit IntoValue<V> conversion. Kept for backward compatibility. */
    fun constantAsV(converter: IntoValue<V>): V = converter.intoValue(constantF64)
}

class LinearSubObject<V : RealNumber<V>>(
    category: ObjectCategory,
    override val cells: ArrayList<LinearCell<V>>,
    private val _constant: V,
    private val _constantF64: Flt64 = Flt64.zero,
    name: String = ""
) : SubObject<V>(category, name) {
    override val constantF64: Flt64 get() = _constantF64
    override val constant: V get() = _constant

    override fun evaluate(): V? {
        var ret = constant
        for (cell in cells) {
            ret += cell.evaluate() ?: return null
        }
        return ret
    }

    override fun evaluate(results: Solution): V? {
        var ret = constant
        for (cell in cells) {
            ret += cell.evaluate(results) ?: return null
        }
        return ret
    }

    companion object {
        /**
         * Create LinearSubObject from LinearFlattenDataF64 (new API)
         */
        operator fun invoke(
            category: ObjectCategory,
            flattenData: LinearFlattenDataF64,
            tokens: AbstractTokenTableF64,
            name: String = ""
        ): LinearSubObjectF64 {
            val cells = createLinearCells(flattenData.monomials, tokens)
            return LinearSubObject(
                category = category,
                cells = cells,
                _constant = flattenData.constant,
                _constantF64 = flattenData.constant,
                name = name
            )
        }

        operator fun <V> invoke(
            category: ObjectCategory,
            flattenData: LinearFlattenDataF64,
            tokens: AbstractTokenTable<V>,
            name: String = "",
            converter: IntoValue<V>
        ): LinearSubObject<V> where V : RealNumber<V>, V : NumberField<V> {
            val cells = ArrayList<LinearCell<V>>(flattenData.monomials.size)
            for (monomial in flattenData.monomials) {
                val variable = monomial.symbol as AbstractVariableItem<*, *>
                val token = tokens.find(variable)
                if (token != null && monomial.coefficient neq Flt64.zero) {
                    cells.add(
                        fuookami.ospf.kotlin.core.model.intermediate.LinearCellImpl(
                            tokenTable = tokens,
                            _coefficientF64 = monomial.coefficient,
                            token = token,
                            converter = converter
                        )
                    )
                }
            }
            return LinearSubObject(
                category = category,
                cells = cells,
                _constant = converter.intoValue(flattenData.constant),
                _constantF64 = flattenData.constant,
                name = name
            )
        }
    }
}

class QuadraticSubObject<V : RealNumber<V>>(
    category: ObjectCategory,
    override val cells: ArrayList<QuadraticCell<V>>,
    private val _constant: V,
    private val _constantF64: Flt64 = Flt64.zero,
    name: String = ""
) : SubObject<V>(category, name) {
    override val constantF64: Flt64 get() = _constantF64
    override val constant: V get() = _constant

    override fun evaluate(): V? {
        var ret = constant
        for (cell in cells) {
            ret += cell.evaluate() ?: return null
        }
        return ret
    }

    override fun evaluate(results: Solution): V? {
        var ret = constant
        for (cell in cells) {
            ret += cell.evaluate(results) ?: return null
        }
        return ret
    }

    companion object {
        /**
         * Create QuadraticSubObject from QuadraticFlattenDataF64 (new API)
         */
        operator fun invoke(
            category: ObjectCategory,
            flattenData: QuadraticFlattenDataF64,
            tokens: AbstractTokenTableF64,
            name: String = ""
        ): QuadraticSubObjectF64 {
            val cells = createQuadraticCells(flattenData.monomials, tokens)
            return QuadraticSubObject(
                category = category,
                cells = cells,
                _constant = flattenData.constant,
                _constantF64 = flattenData.constant,
                name = name
            )
        }

        operator fun <V> invoke(
            category: ObjectCategory,
            flattenData: QuadraticFlattenDataF64,
            tokens: AbstractTokenTable<V>,
            name: String = "",
            converter: IntoValue<V>
        ): QuadraticSubObject<V> where V : RealNumber<V>, V : NumberField<V> {
            val cells = ArrayList<QuadraticCell<V>>(flattenData.monomials.size)
            for (monomial in flattenData.monomials) {
                val variable1 = monomial.symbol1 as AbstractVariableItem<*, *>
                val token1 = tokens.find(variable1)
                val token2 = if (monomial.symbol2 != null) {
                    tokens.find(monomial.symbol2 as AbstractVariableItem<*, *>) ?: continue
                } else {
                    null
                }
                if (token1 != null && monomial.coefficient neq Flt64.zero) {
                    cells.add(
                        fuookami.ospf.kotlin.core.model.intermediate.QuadraticCellImpl(
                            tokenTable = tokens,
                            _coefficientF64 = monomial.coefficient,
                            token1 = token1,
                            token2 = token2,
                            converter = converter
                        )
                    )
                }
            }
            return QuadraticSubObject(
                category = category,
                cells = cells,
                _constant = converter.intoValue(flattenData.constant),
                _constantF64 = flattenData.constant,
                name = name
            )
        }
    }
}

typealias LinearSubObjectF64 = LinearSubObject<F64>
typealias QuadraticSubObjectF64 = QuadraticSubObject<F64>
