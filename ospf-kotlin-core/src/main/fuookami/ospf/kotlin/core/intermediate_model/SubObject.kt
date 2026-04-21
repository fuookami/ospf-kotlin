@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.core.intermediate_model

import fuookami.ospf.kotlin.core.model.Solution
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
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
    abstract val cells: List<CellF64>

    /** Flt64 view of constant (solver-compatible, internal). */
    abstract val constantF64: Flt64

    /** V-typed constant (primary public API). Uses IntoValue<V> conversion at boundary. */
    abstract val constant: V

    /** Flt64 view of evaluation (solver-compatible, internal). */
    fun evaluateF64(): Flt64? {
        var ret = constantF64
        for (cell in cells) {
            ret += cell.evaluate() ?: return null
        }
        return ret
    }

    fun evaluateF64(results: Solution): Flt64? {
        var ret = constantF64
        for (cell in cells) {
            ret += cell.evaluate(results) ?: return null
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
    override val cells: ArrayList<LinearCellF64>,
    private val _constant: Flt64 = Flt64.zero,
    name: String = ""
) : SubObject<V>(category, name) {
    override val constantF64: Flt64 get() = _constant
    @Suppress("UNCHECKED_CAST")
    override val constant: V get() = _constant as V

    @Suppress("UNCHECKED_CAST")
    override fun evaluate(): V? {
        var ret = _constant
        for (cell in cells) {
            ret += cell.evaluate() ?: return null
        }
        return ret as V
    }

    @Suppress("UNCHECKED_CAST")
    override fun evaluate(results: Solution): V? {
        var ret = _constant
        for (cell in cells) {
            ret += cell.evaluate(results) ?: return null
        }
        return ret as V
    }

    companion object {
        /**
         * Create LinearSubObject from Polynomial (legacy API).
         * Prefer the FlattenData-based constructor for new code.
         */
        @Deprecated(
            message = "Use LinearSubObject(category, flattenData, tokens, name) instead. Will be removed in M9.",
            level = DeprecationLevel.WARNING,
            replaceWith = ReplaceWith(
                "LinearSubObject(category, poly.flattenedMonomials, tokens, name)",
                "fuookami.ospf.kotlin.core.intermediate_model.LinearFlattenDataF64"
            )
        )
        @Suppress("DEPRECATION")
        operator fun invoke(
            category: ObjectCategory,
            poly: ToMathLinearPolynomial,
            tokens: LegacyAbstractTokenTable,
            name: String
        ): LinearSubObject<Flt64> {
            val lp = poly.toMathLinearPolynomial()
            val flattenData = LinearFlattenDataF64(lp.monomials, lp.constant)
            val cells = createLinearCells(flattenData.monomials, tokens)
            return LinearSubObject(
                category = category,
                cells = cells,
                _constant = flattenData.constant,
                name = name
            )
        }

        /**
         * Create LinearSubObject from LinearFlattenDataF64 (new API)
         */
        operator fun invoke(
            category: ObjectCategory,
            flattenData: LinearFlattenDataF64,
            tokens: LegacyAbstractTokenTable,
            name: String = ""
        ): LinearSubObject<Flt64> {
            val cells = createLinearCells(flattenData.monomials, tokens)
            return LinearSubObject(
                category = category,
                cells = cells,
                _constant = flattenData.constant,
                name = name
            )
        }
    }
}

class QuadraticSubObject<V : RealNumber<V>>(
    category: ObjectCategory,
    override val cells: ArrayList<QuadraticCellF64>,
    private val _constant: Flt64 = Flt64.zero,
    name: String = ""
) : SubObject<V>(category, name) {
    override val constantF64: Flt64 get() = _constant
    @Suppress("UNCHECKED_CAST")
    override val constant: V get() = _constant as V

    @Suppress("UNCHECKED_CAST")
    override fun evaluate(): V? {
        var ret = _constant
        for (cell in cells) {
            ret += cell.evaluate() ?: return null
        }
        return ret as V
    }

    @Suppress("UNCHECKED_CAST")
    override fun evaluate(results: Solution): V? {
        var ret = _constant
        for (cell in cells) {
            ret += cell.evaluate(results) ?: return null
        }
        return ret as V
    }

    companion object {
        /**
         * Create QuadraticSubObject from Polynomial (legacy API).
         * Prefer the FlattenData-based constructor for new code.
         */
        @Deprecated(
            message = "Use QuadraticSubObject(category, flattenData, tokens, name) instead. Will be removed in M9.",
            level = DeprecationLevel.WARNING,
            replaceWith = ReplaceWith(
                "QuadraticSubObject(category, poly.flattenedMonomials, tokens, name)",
                "fuookami.ospf.kotlin.core.intermediate_model.QuadraticFlattenDataF64"
            )
        )
        @Suppress("DEPRECATION")
        operator fun invoke(
            category: ObjectCategory,
            poly: ToMathQuadraticPolynomial,
            tokens: LegacyAbstractTokenTable,
            name: String
        ): QuadraticSubObject<Flt64> {
            val qp = poly.toMathQuadraticPolynomial()
            val flattenData = QuadraticFlattenDataF64(qp.monomials, qp.constant)
            val cells = createQuadraticCells(flattenData.monomials, tokens)
            return QuadraticSubObject(
                category = category,
                cells = cells,
                _constant = flattenData.constant,
                name = name
            )
        }

        /**
         * Create QuadraticSubObject from QuadraticFlattenDataF64 (new API)
         */
        operator fun invoke(
            category: ObjectCategory,
            flattenData: QuadraticFlattenDataF64,
            tokens: LegacyAbstractTokenTable,
            name: String = ""
        ): QuadraticSubObject<Flt64> {
            val cells = createQuadraticCells(flattenData.monomials, tokens)
            return QuadraticSubObject(
                category = category,
                cells = cells,
                _constant = flattenData.constant,
                name = name
            )
        }
    }
}

typealias LinearSubObjectF64 = LinearSubObject<Flt64>
typealias QuadraticSubObjectF64 = QuadraticSubObject<Flt64>
