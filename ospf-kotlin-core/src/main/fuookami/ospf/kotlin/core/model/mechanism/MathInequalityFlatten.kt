package fuookami.ospf.kotlin.core.model.mechanism

import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.VariableItemKey
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequalityOf
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.core.token.LinearFlattenData
import fuookami.ospf.kotlin.core.token.QuadraticFlattenData

private val flt64Converter = object : IntoValue<Flt64> {
        override fun intoValue(value: Flt64) = value
        override val zero get() = Flt64.zero
        override val one get() = Flt64.one
        override fun fromValue(value: Flt64) = value
    }

// ========== Converter-based flatten: V-typed inequality -> Flt64 flatten data ==========

/**
 * Flatten a V-typed LinearInequality into LinearFlattenData<Flt64> using an explicit converter.
 *
 * Converts lhs - rhs into a single linear form:
 *   sum(lhs.monomials) - sum(rhs.monomials) <= lhs.constant - rhs.constant
 *
 * This is the V-generic replacement for the old `LinearInequality<Flt64>.flattenData`
 * extension that required casting `LinearInequality<V>` to `LinearInequality<Flt64>`.
 */
fun <V> LinearInequality<V>.toLinearFlattenDataFlt64(converter: IntoValue<V>): LinearFlattenData<Flt64>
        where V : RealNumber<V>, V : NumberField<V> {
    val merged = HashMap<VariableItemKey, LinearMonomial<Flt64>>()

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

    return LinearFlattenData<Flt64>(
        monomials = merged.values.toList(),
        constant = converter.fromValue(lhs.constant) - converter.fromValue(rhs.constant)
    )
}

/**
 * Flatten a V-typed QuadraticInequalityOf<V> into QuadraticFlattenData<Flt64> using an explicit converter.
 *
 * Converts lhs - rhs into a single quadratic form.
 */
fun <V> QuadraticInequalityOf<V>.toQuadraticFlattenDataFlt64(converter: IntoValue<V>): QuadraticFlattenData<Flt64>
        where V : RealNumber<V>, V : NumberField<V> {
    val merged = HashMap<QuadraticMonomialKey, QuadraticMonomial<Flt64>>()

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

    return QuadraticFlattenData<Flt64>(
        monomials = merged.values.toList(),
        constant = converter.fromValue(lhs.constant) - converter.fromValue(rhs.constant)
    )
}

// ========== Flt64-specific flatten extensions (for Flt64-typed inequalities) ==========

/** Alias for comparison, matching the old Relation.sign property */
val LinearInequality<Flt64>.sign: Comparison get() = comparison

/** Alias for comparison, matching the old Relation.sign property */
val QuadraticInequality.sign: Comparison get() = comparison

/**
 * Compute LinearFlattenData<Flt64> from LinearInequality<Flt64>.
 * Flattens lhs - rhs into a single linear form.
 *
 * This is the Flt64-specific convenience for when V=Flt64 is already known.
 */
val LinearInequality<Flt64>.flattenData: LinearFlattenData<Flt64>
    get() = toLinearFlattenDataFlt64(flt64Converter)

/**
 * Compute QuadraticFlattenData<Flt64> from QuadraticInequality (Flt64).
 * Flattens lhs - rhs into a single quadratic form.
 *
 * This is the Flt64-specific convenience for when V=Flt64 is already known.
 */
val QuadraticInequality.flattenData: QuadraticFlattenData<Flt64>
    get() = toQuadraticFlattenDataFlt64(flt64Converter)

// ========== Internal key for merging quadratic monomials ==========

/** Internal key for merging quadratic monomials (handles commutativity of x*y = y*x) */
private data class QuadraticMonomialKey(
    val sym1Id: Int,
    val sym2Id: Int?
) {
    companion object {
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

        fun from(mono: QuadraticMonomial<Flt64>): QuadraticMonomialKey {
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

/** Convert math LinearPolynomial to frontend LinearPolynomial (now identity since Polynomial.kt is deleted) */
fun fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial<Flt64>.toFrontendPolynomial(): fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial<Flt64> {
    return this
}

/**
 * Create LinearFlattenData<Flt64> directly from math LinearPolynomial.
 * Used when only one side of the inequality is needed.
 */
fun fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial<Flt64>.toFlattenData(): LinearFlattenData<Flt64> {
    return LinearFlattenData<Flt64>(
        monomials = monomials.map { LinearMonomial(it.coefficient, it.symbol) },
        constant = constant
    )
}

/**
 * Create QuadraticFlattenData<Flt64> directly from math QuadraticPolynomial.
 * Used when only one side of the inequality is needed.
 */
fun fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial<Flt64>.toFlattenData(): QuadraticFlattenData<Flt64> {
    return QuadraticFlattenData<Flt64>(
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