package fuookami.ospf.kotlin.math.symbol.polynomial

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.resolveArithmeticConstants

import fuookami.ospf.kotlin.math.symbol.Category
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.div
import fuookami.ospf.kotlin.math.symbol.monomial.times
import fuookami.ospf.kotlin.math.symbol.monomial.unaryMinus

/**
 * Mutable version of QuadraticPolynomial for in-place modifications.
 *
 * Use this when you need to build or modify polynomials incrementally.
 * Convert to [QuadraticPolynomial] when you need an immutable version.
 */
class MutableQuadraticPolynomial<T : NumberField<T>>(
    monomials: List<QuadraticMonomial<T>> = emptyList(),
    constant: T
) {
    internal val _monomials: MutableList<QuadraticMonomial<T>> = monomials.toMutableList()
    internal var _constant: T = constant

    val monomials: List<QuadraticMonomial<T>> get() = _monomials.toList()
    val constant: T get() = _constant

    val category: Category
        get() = if (_monomials.any { it.isQuadratic }) Quadratic else Linear

    companion object {
        /**
         * Creates a zero polynomial (constant = 0).
         * Requires T to have a companion object implementing ArithmeticConstants<T>.
         */
        inline fun <reified T> zero(): MutableQuadraticPolynomial<T> where T : NumberField<T>, T : Arithmetic<T> {
            val constants = resolveArithmeticConstants<T>("MutableQuadraticPolynomial.zero")
            return MutableQuadraticPolynomial(emptyList(), constants.zero)
        }

        /**
         * Creates a constant polynomial with value 1.
         * Requires T to have a companion object implementing ArithmeticConstants<T>.
         */
        inline fun <reified T> one(): MutableQuadraticPolynomial<T> where T : NumberField<T>, T : Arithmetic<T> {
            val constants = resolveArithmeticConstants<T>("MutableQuadraticPolynomial.one")
            return MutableQuadraticPolynomial(emptyList(), constants.one)
        }

        /**
         * Creates a constant polynomial with the given value.
         * Use this when you need a specific constant or when T doesn't support reflection.
         */
        fun <T : NumberField<T>> fromConstant(value: T): MutableQuadraticPolynomial<T> {
            return MutableQuadraticPolynomial(emptyList(), value)
        }
    }

    fun addMonomial(monomial: QuadraticMonomial<T>) {
        _monomials.add(monomial)
    }

    fun addLinearMonomial(monomial: LinearMonomial<T>) {
        _monomials.add(QuadraticMonomial.linear(monomial.coefficient, monomial.symbol))
    }

    fun addConstant(value: T) {
        _constant += value
    }

    fun setConstant(value: T) {
        _constant = value
    }

    fun clear() {
        _monomials.clear()
    }

    fun toQuadraticPolynomial(): QuadraticPolynomial<T> {
        return QuadraticPolynomial(_monomials.toList(), _constant)
    }

    fun toImmutable(): QuadraticPolynomial<T> = toQuadraticPolynomial()

    override fun toString(): String {
        return "MutableQuadraticPolynomial(monomials=$_monomials, constant=$_constant)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MutableQuadraticPolynomial<*>) return false
        return _monomials == other._monomials && _constant == other._constant
    }

    override fun hashCode(): Int {
        var result = _monomials.hashCode()
        result = 31 * result + _constant.hashCode()
        return result
    }
}

// Conversion from QuadraticPolynomial to MutableQuadraticPolynomial
fun <T : NumberField<T>> QuadraticPolynomial<T>.toMutable(): MutableQuadraticPolynomial<T> {
    return MutableQuadraticPolynomial(monomials, constant)
}

// Conversion from MutableQuadraticPolynomial to QuadraticPolynomial
fun <T : NumberField<T>> MutableQuadraticPolynomial<T>.toImmutable(): QuadraticPolynomial<T> {
    return this.toQuadraticPolynomial()
}

// Unary minus: -MutableQuadraticPolynomial<T>
operator fun <T : NumberField<T>> MutableQuadraticPolynomial<T>.unaryMinus(): MutableQuadraticPolynomial<T> {
    return MutableQuadraticPolynomial(monomials.map { -it }, -constant)
}

// Addition assignment: MutableQuadraticPolynomial<T> += QuadraticMonomial<T>
operator fun <T : NumberField<T>> MutableQuadraticPolynomial<T>.plusAssign(rhs: QuadraticMonomial<T>) {
    addMonomial(rhs)
}

// Addition assignment: MutableQuadraticPolynomial<T> += LinearMonomial<T>
operator fun <T : NumberField<T>> MutableQuadraticPolynomial<T>.plusAssign(rhs: LinearMonomial<T>) {
    addLinearMonomial(rhs)
}

// Addition assignment: MutableQuadraticPolynomial<T> += QuadraticPolynomial<T>
operator fun <T : NumberField<T>> MutableQuadraticPolynomial<T>.plusAssign(rhs: QuadraticPolynomial<T>) {
    _monomials.addAll(rhs.monomials)
    _constant = _constant + rhs.constant
}

// Addition assignment: MutableQuadraticPolynomial<T> += MutableQuadraticPolynomial<T>
operator fun <T : NumberField<T>> MutableQuadraticPolynomial<T>.plusAssign(rhs: MutableQuadraticPolynomial<T>) {
    _monomials.addAll(rhs.monomials)
    _constant = _constant + rhs.constant
}

// Addition assignment: MutableQuadraticPolynomial<T> += LinearPolynomial<T>
operator fun <T : NumberField<T>> MutableQuadraticPolynomial<T>.plusAssign(rhs: LinearPolynomial<T>) {
    _monomials.addAll(rhs.monomials.map { QuadraticMonomial.linear(it.coefficient, it.symbol) })
    _constant = _constant + rhs.constant
}

// Addition assignment: MutableQuadraticPolynomial<T> += T
operator fun <T : NumberField<T>> MutableQuadraticPolynomial<T>.plusAssign(rhs: T) {
    _constant = _constant + rhs
}

// Subtraction assignment: MutableQuadraticPolynomial<T> -= QuadraticMonomial<T>
operator fun <T : NumberField<T>> MutableQuadraticPolynomial<T>.minusAssign(rhs: QuadraticMonomial<T>) {
    _monomials.add(-rhs)
}

// Subtraction assignment: MutableQuadraticPolynomial<T> -= LinearMonomial<T>
operator fun <T : NumberField<T>> MutableQuadraticPolynomial<T>.minusAssign(rhs: LinearMonomial<T>) {
    _monomials.add(QuadraticMonomial.linear(-rhs.coefficient, rhs.symbol))
}

// Subtraction assignment: MutableQuadraticPolynomial<T> -= QuadraticPolynomial<T>
operator fun <T : NumberField<T>> MutableQuadraticPolynomial<T>.minusAssign(rhs: QuadraticPolynomial<T>) {
    _monomials.addAll(rhs.monomials.map { -it })
    _constant -= rhs.constant
}

// Subtraction assignment: MutableQuadraticPolynomial<T> -= MutableQuadraticPolynomial<T>
operator fun <T : NumberField<T>> MutableQuadraticPolynomial<T>.minusAssign(rhs: MutableQuadraticPolynomial<T>) {
    _monomials.addAll(rhs.monomials.map { -it })
    _constant -= rhs.constant
}

// Subtraction assignment: MutableQuadraticPolynomial<T> -= LinearPolynomial<T>
operator fun <T : NumberField<T>> MutableQuadraticPolynomial<T>.minusAssign(rhs: LinearPolynomial<T>) {
    _monomials.addAll(rhs.monomials.map { QuadraticMonomial.linear(-it.coefficient, it.symbol) })
    _constant -= rhs.constant
}

// Subtraction assignment: MutableQuadraticPolynomial<T> -= T
operator fun <T : NumberField<T>> MutableQuadraticPolynomial<T>.minusAssign(rhs: T) {
    _constant -= rhs
}

// Multiplication assignment: MutableQuadraticPolynomial<T> *= T
operator fun <T : NumberField<T>> MutableQuadraticPolynomial<T>.timesAssign(rhs: T) {
    for (i in _monomials.indices) {
        _monomials[i] = _monomials[i] * rhs
    }
    _constant *= rhs
}

// Division assignment: MutableQuadraticPolynomial<T> /= T
operator fun <T : NumberField<T>> MutableQuadraticPolynomial<T>.divAssign(rhs: T) {
    for (i in _monomials.indices) {
        _monomials[i] = _monomials[i] / rhs
    }
    _constant = _constant / rhs
}

