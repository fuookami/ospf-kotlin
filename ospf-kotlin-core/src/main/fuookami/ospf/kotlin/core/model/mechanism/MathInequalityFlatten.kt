package fuookami.ospf.kotlin.core.model.mechanism

import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.VariableItemKey
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequalityOf
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.core.token.LinearFlattenData
import fuookami.ospf.kotlin.core.token.QuadraticFlattenData

private val flt64Converter = object : IntoValue<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
        override fun intoValue(value: Flt64) = value
        override val zero get() = Flt64.zero
        override val one get() = Flt64.one
        override fun fromValue(value: Flt64) = value
    }

// ========== Converter-based flatten: V-typed inequality -> V-typed flatten data ==========

/**
 * Flatten a V-typed LinearInequality into LinearFlattenData<V> (identity flatten, no conversion).
 *
 * Converts lhs - rhs into a single linear form:
 *   sum(lhs.monomials) - sum(rhs.monomials) <= lhs.constant - rhs.constant
 */
internal fun <V> LinearInequality<V>.toLinearFlattenData(): LinearFlattenData<V>
        where V : RealNumber<V>, V : NumberField<V> {
    val merged = HashMap<VariableItemKey, LinearMonomial<V>>()

    for (mono in lhs.monomials) {
        val key = (mono.symbol as AbstractVariableItem<*, *>).key
        merged[key] = LinearMonomial(mono.coefficient, mono.symbol)
    }
    for (mono in rhs.monomials) {
        val key = (mono.symbol as AbstractVariableItem<*, *>).key
        val existing = merged[key]
        merged[key] = if (existing != null) {
            LinearMonomial(existing.coefficient - mono.coefficient, existing.symbol)
        } else {
            LinearMonomial(-mono.coefficient, mono.symbol)
        }
    }

    return LinearFlattenData<V>(
        monomials = merged.values.toList(),
        constant = lhs.constant - rhs.constant
    )
}

/**
 * Flatten a V-typed QuadraticInequalityOf<V> into QuadraticFlattenData<V> (identity flatten, no conversion).
 *
 * Converts lhs - rhs into a single quadratic form.
 */
internal fun <V> QuadraticInequalityOf<V>.toQuadraticFlattenData(): QuadraticFlattenData<V>
        where V : RealNumber<V>, V : NumberField<V> {
    val merged = HashMap<QuadraticMonomialKey, QuadraticMonomial<V>>()

    for (mono in lhs.monomials) {
        val key = QuadraticMonomialKey.from(mono)
        merged[key] = QuadraticMonomial(
            coefficient = mono.coefficient,
            symbol1 = mono.symbol1,
            symbol2 = mono.symbol2
        )
    }
    for (mono in rhs.monomials) {
        val key = QuadraticMonomialKey.from(mono)
        val existing = merged[key]
        merged[key] = if (existing != null) {
            QuadraticMonomial(
                coefficient = existing.coefficient - mono.coefficient,
                symbol1 = existing.symbol1,
                symbol2 = existing.symbol2
            )
        } else {
            QuadraticMonomial(
                coefficient = -mono.coefficient,
                symbol1 = mono.symbol1,
                symbol2 = mono.symbol2
            )
        }
    }

    return QuadraticFlattenData<V>(
        monomials = merged.values.toList(),
        constant = lhs.constant - rhs.constant
    )
}

// ========== Converter-based flatten: V-typed inequality -> Flt64 flatten data ==========

/**
 * Flatten a V-typed LinearInequality into LinearFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64> using an explicit converter.
 *
 * Converts lhs - rhs into a single linear form:
 *   sum(lhs.monomials) - sum(rhs.monomials) <= lhs.constant - rhs.constant
 *
 * This is the V-generic replacement for the old `LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>.flattenData`
 * extension that required casting `LinearInequality<V>` to `LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>`.
 */
internal fun <V> LinearInequality<V>.toLinearFlattenDataFlt64(converter: IntoValue<V>): LinearFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64>
        where V : RealNumber<V>, V : NumberField<V> {
    val merged = HashMap<VariableItemKey, LinearMonomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>>()

    for (mono in lhs.monomials) {
        val key = (mono.symbol as AbstractVariableItem<*, *>).key
        val flt64Coeff = converter.fromValue(mono.coefficient)
        merged[key] = LinearMonomial(flt64Coeff, mono.symbol)
    }
    for (mono in rhs.monomials) {
        val key = (mono.symbol as AbstractVariableItem<*, *>).key
        val flt64Coeff = converter.fromValue(mono.coefficient)
        val existing = merged[key]
        merged[key] = if (existing != null) {
            LinearMonomial(existing.coefficient - flt64Coeff, existing.symbol)
        } else {
            LinearMonomial(-flt64Coeff, mono.symbol)
        }
    }

    return LinearFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
        monomials = merged.values.toList(),
        constant = converter.fromValue(lhs.constant) - converter.fromValue(rhs.constant)
    )
}

/**
 * Flatten a V-typed QuadraticInequalityOf<V> into QuadraticFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64> using an explicit converter.
 *
 * Converts lhs - rhs into a single quadratic form.
 */
internal fun <V> QuadraticInequalityOf<V>.toQuadraticFlattenDataFlt64(converter: IntoValue<V>): QuadraticFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64>
        where V : RealNumber<V>, V : NumberField<V> {
    val merged = HashMap<QuadraticMonomialKey, QuadraticMonomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>>()

    for (mono in lhs.monomials) {
        val key = QuadraticMonomialKey.from(mono, converter)
        val flt64Coeff = converter.fromValue(mono.coefficient)
        merged[key] = QuadraticMonomial(
            coefficient = flt64Coeff,
            symbol1 = mono.symbol1,
            symbol2 = mono.symbol2
        )
    }
    for (mono in rhs.monomials) {
        val key = QuadraticMonomialKey.from(mono, converter)
        val flt64Coeff = converter.fromValue(mono.coefficient)
        val existing = merged[key]
        merged[key] = if (existing != null) {
            QuadraticMonomial(
                coefficient = existing.coefficient - flt64Coeff,
                symbol1 = existing.symbol1,
                symbol2 = existing.symbol2
            )
        } else {
            QuadraticMonomial(
                coefficient = -flt64Coeff,
                symbol1 = mono.symbol1,
                symbol2 = mono.symbol2
            )
        }
    }

    return QuadraticFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
        monomials = merged.values.toList(),
        constant = converter.fromValue(lhs.constant) - converter.fromValue(rhs.constant)
    )
}

// ========== Flt64-specific flatten extensions (for Flt64-typed inequalities) ==========

/** Alias for comparison, matching the old Relation.sign property */
internal val LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>.sign: Comparison get() = comparison

/** Alias for comparison, matching the old Relation.sign property */
internal val QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64>.sign: Comparison get() = comparison

/**
 * Compute LinearFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64> from LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>.
 * Flattens lhs - rhs into a single linear form.
 *
 * This is the Flt64-specific convenience for when V=Flt64 is already known.
 */
internal val LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>.flattenData: LinearFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64>
    get() = toLinearFlattenDataFlt64(flt64Converter)

/**
 * Compute QuadraticFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64> from QuadraticInequality (Flt64).
 * Flattens lhs - rhs into a single quadratic form.
 *
 * This is the Flt64-specific convenience for when V=Flt64 is already known.
 */
internal val QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64>.flattenData: QuadraticFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64>
    get() = toQuadraticFlattenDataFlt64(flt64Converter)

// ========== Internal key for merging quadratic monomials ==========

/** Internal key for merging quadratic monomials (handles commutativity of x*y = y*x) */
private data class QuadraticMonomialKey(
    val sym1Id: Int,
    val sym2Id: Int?
) {
    companion object {
        @JvmName("fromGeneric")
        fun <V> from(mono: QuadraticMonomial<V>): QuadraticMonomialKey
                where V : RealNumber<V>, V : NumberField<V> {
            val id1 = System.identityHashCode(mono.symbol1)
            val id2 = mono.symbol2?.let { System.identityHashCode(it) }
            return if (id2 != null && id1 > id2) {
                QuadraticMonomialKey(id2, id1)
            } else {
                QuadraticMonomialKey(id1, id2)
            }
        }

        @JvmName("fromGenericWithConverter")
        fun <V> from(mono: QuadraticMonomial<V>, converter: IntoValue<V>): QuadraticMonomialKey
                where V : RealNumber<V>, V : NumberField<V> {
            val id1 = System.identityHashCode(mono.symbol1)
            val id2 = mono.symbol2?.let { System.identityHashCode(it) }
            return if (id2 != null && id1 > id2) {
                QuadraticMonomialKey(id2, id1)
            } else {
                QuadraticMonomialKey(id1, id2)
            }
        }

        @JvmName("fromFlt64")
        private fun from(mono: QuadraticMonomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>): QuadraticMonomialKey {
            val id1 = System.identityHashCode(mono.symbol1)
            val id2 = mono.symbol2?.let { System.identityHashCode(it) }
            return if (id2 != null && id1 > id2) {
                QuadraticMonomialKey(id2, id1)
            } else {
                QuadraticMonomialKey(id1, id2)
            }
        }
    }
}

// ========== Conversion from math types to frontend types ==========

/**
 * Create LinearFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64> directly from math LinearPolynomial.
 * Used when only one side of the inequality is needed.
 */
internal fun fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>.toFlattenData(): LinearFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
    return LinearFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
        monomials = monomials.map { LinearMonomial(it.coefficient, it.symbol) },
        constant = constant
    )
}

/**
 * Create QuadraticFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64> directly from math QuadraticPolynomial.
 * Used when only one side of the inequality is needed.
 */
internal fun fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>.toFlattenData(): QuadraticFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
    return QuadraticFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
        monomials = monomials.map {
            QuadraticMonomial(
                coefficient = it.coefficient,
                symbol1 = it.symbol1,
                symbol2 = it.symbol2
            )
        },
        constant = constant
    )
}
