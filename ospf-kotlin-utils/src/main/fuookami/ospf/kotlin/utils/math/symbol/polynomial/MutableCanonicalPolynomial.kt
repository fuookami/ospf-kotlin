package fuookami.ospf.kotlin.utils.math.symbol.polynomial

import fuookami.ospf.kotlin.utils.math.algebra.number.*
import fuookami.ospf.kotlin.utils.math.algebra.concept.*
import fuookami.ospf.kotlin.utils.math.algebra.value_range.*
import fuookami.ospf.kotlin.utils.math.algebra.concept.NumberField

import fuookami.ospf.kotlin.utils.math.symbol.Category
import fuookami.ospf.kotlin.utils.math.symbol.Linear
import fuookami.ospf.kotlin.utils.math.symbol.Nonlinear
import fuookami.ospf.kotlin.utils.math.symbol.Quadratic
import fuookami.ospf.kotlin.utils.math.symbol.monomial.CanonicalMonomial
import fuookami.ospf.kotlin.utils.math.symbol.monomial.div
import fuookami.ospf.kotlin.utils.math.symbol.monomial.times
import fuookami.ospf.kotlin.utils.math.symbol.monomial.unaryMinus

/**
 * Mutable version of CanonicalPolynomial for in-place modifications.
 * 
 * Use this when you need to build or modify polynomials incrementally.
 * Convert to [CanonicalPolynomial] when you need an immutable version.
 */
class MutableCanonicalPolynomial<T : NumberField<T>>(
    monomials: List<CanonicalMonomial<T>> = emptyList(),
    constant: T
) {
    internal val _monomials: MutableList<CanonicalMonomial<T>> = monomials.toMutableList()
    internal var _constant: T = constant

    val monomials: List<CanonicalMonomial<T>> get() = _monomials.toList()
    val constant: T get() = _constant

    val category: Category
        get() = when (_monomials.maxOfOrNull { it.degree } ?: 0) {
            0, 1 -> Linear
            2 -> Quadratic
            else -> Nonlinear
        }

    companion object {
        val One: Flt64 = Flt64.one

        fun <T : NumberField<T>> zero(): MutableCanonicalPolynomial<T> {
            @Suppress("UNCHECKED_CAST")
            return MutableCanonicalPolynomial(emptyList(), One as T)
        }

        fun <T : NumberField<T>> fromConstant(value: T): MutableCanonicalPolynomial<T> {
            return MutableCanonicalPolynomial(emptyList(), value)
        }
    }

    fun addMonomial(monomial: CanonicalMonomial<T>) {
        _monomials.add(monomial)
    }

    fun addConstant(value: T) {
        _constant = _constant + value
    }

    fun setConstant(value: T) {
        _constant = value
    }

    fun clear() {
        _monomials.clear()
    }

    fun toCanonicalPolynomial(): CanonicalPolynomial<T> {
        return CanonicalPolynomial(_monomials.toList(), _constant)
    }

    fun toImmutable(): CanonicalPolynomial<T> = toCanonicalPolynomial()

    override fun toString(): String {
        return "MutableCanonicalPolynomial(monomials=$_monomials, constant=$_constant)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MutableCanonicalPolynomial<*>) return false
        return _monomials == other._monomials && _constant == other._constant
    }

    override fun hashCode(): Int {
        var result = _monomials.hashCode()
        result = 31 * result + _constant.hashCode()
        return result
    }
}

// Conversion from CanonicalPolynomial to MutableCanonicalPolynomial
fun <T : NumberField<T>> CanonicalPolynomial<T>.toMutable(): MutableCanonicalPolynomial<T> {
    return MutableCanonicalPolynomial(monomials, constant)
}

// Conversion from MutableCanonicalPolynomial to CanonicalPolynomial
fun <T : NumberField<T>> MutableCanonicalPolynomial<T>.toImmutable(): CanonicalPolynomial<T> {
    return this.toCanonicalPolynomial()
}

// Unary minus: -MutableCanonicalPolynomial<T>
operator fun <T : NumberField<T>> MutableCanonicalPolynomial<T>.unaryMinus(): MutableCanonicalPolynomial<T> {
    return MutableCanonicalPolynomial(monomials.map { -it }, -constant)
}

// Addition assignment: MutableCanonicalPolynomial<T> += CanonicalMonomial<T>
operator fun <T : NumberField<T>> MutableCanonicalPolynomial<T>.plusAssign(rhs: CanonicalMonomial<T>) {
    addMonomial(rhs)
}

// Addition assignment: MutableCanonicalPolynomial<T> += CanonicalPolynomial<T>
operator fun <T : NumberField<T>> MutableCanonicalPolynomial<T>.plusAssign(rhs: CanonicalPolynomial<T>) {
    _monomials.addAll(rhs.monomials)
    _constant = _constant + rhs.constant
}

// Addition assignment: MutableCanonicalPolynomial<T> += MutableCanonicalPolynomial<T>
operator fun <T : NumberField<T>> MutableCanonicalPolynomial<T>.plusAssign(rhs: MutableCanonicalPolynomial<T>) {
    _monomials.addAll(rhs.monomials)
    _constant = _constant + rhs.constant
}

// Addition assignment: MutableCanonicalPolynomial<T> += T
operator fun <T : NumberField<T>> MutableCanonicalPolynomial<T>.plusAssign(rhs: T) {
    _constant = _constant + rhs
}

// Subtraction assignment: MutableCanonicalPolynomial<T> -= CanonicalMonomial<T>
operator fun <T : NumberField<T>> MutableCanonicalPolynomial<T>.minusAssign(rhs: CanonicalMonomial<T>) {
    _monomials.add(-rhs)
}

// Subtraction assignment: MutableCanonicalPolynomial<T> -= CanonicalPolynomial<T>
operator fun <T : NumberField<T>> MutableCanonicalPolynomial<T>.minusAssign(rhs: CanonicalPolynomial<T>) {
    _monomials.addAll(rhs.monomials.map { -it })
    _constant = _constant - rhs.constant
}

// Subtraction assignment: MutableCanonicalPolynomial<T> -= MutableCanonicalPolynomial<T>
operator fun <T : NumberField<T>> MutableCanonicalPolynomial<T>.minusAssign(rhs: MutableCanonicalPolynomial<T>) {
    _monomials.addAll(rhs.monomials.map { -it })
    _constant = _constant - rhs.constant
}

// Subtraction assignment: MutableCanonicalPolynomial<T> -= T
operator fun <T : NumberField<T>> MutableCanonicalPolynomial<T>.minusAssign(rhs: T) {
    _constant = _constant - rhs
}

// Multiplication assignment: MutableCanonicalPolynomial<T> *= T
operator fun <T : NumberField<T>> MutableCanonicalPolynomial<T>.timesAssign(rhs: T) {
    for (i in _monomials.indices) {
        _monomials[i] = _monomials[i] * rhs
    }
    _constant = _constant * rhs
}

// Division assignment: MutableCanonicalPolynomial<T> /= T
operator fun <T : NumberField<T>> MutableCanonicalPolynomial<T>.divAssign(rhs: T) {
    for (i in _monomials.indices) {
        _monomials[i] = _monomials[i] / rhs
    }
    _constant = _constant / rhs
}

