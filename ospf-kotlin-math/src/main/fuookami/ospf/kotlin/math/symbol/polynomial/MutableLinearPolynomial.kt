/**
 * 可变线性多项式
 * Mutable Linear Polynomial
 *
 * 提供线性多项式的可变版本，支持原地修改操作。
 * 用于需要增量构建或修改多项式的场景，构建完成后可转换为不可变的 LinearPolynomial。
 * Provides a mutable version of linear polynomials, supporting in-place modifications.
 * Used for scenarios requiring incremental building or modification of polynomials,
 * and can be converted to an immutable LinearPolynomial after construction.
 */
package fuookami.ospf.kotlin.math.symbol.polynomial

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.resolveArithmeticConstants

import fuookami.ospf.kotlin.math.symbol.Category
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.div
import fuookami.ospf.kotlin.math.symbol.monomial.times
import fuookami.ospf.kotlin.math.symbol.monomial.unaryMinus

/**
 * Mutable version of LinearPolynomial for in-place modifications.
 *
 * Use this when you need to build or modify polynomials incrementally.
 * Convert to [LinearPolynomial] when you need an immutable version.
 */
class MutableLinearPolynomial<T : NumberField<T>>(
    monomials: List<LinearMonomial<T>> = emptyList(),
    constant: T
) {
    internal val _monomials: MutableList<LinearMonomial<T>> = monomials.toMutableList()
    internal var _constant: T = constant

    val monomials: List<LinearMonomial<T>> get() = _monomials.toList()
    val constant: T get() = _constant

    val category: Category
        get() = Linear

    companion object {
        /**
         * Creates a zero polynomial (constant = 0).
         * Requires T to have a companion object implementing ArithmeticConstants<T>.
         */
        inline fun <reified T> zero(): MutableLinearPolynomial<T> where T : NumberField<T>, T : Arithmetic<T> {
            val constants = resolveArithmeticConstants<T>("MutableLinearPolynomial.zero")
            return MutableLinearPolynomial(emptyList(), constants.zero)
        }

        /**
         * Creates a constant polynomial with value 1.
         * Requires T to have a companion object implementing ArithmeticConstants<T>.
         */
        inline fun <reified T> one(): MutableLinearPolynomial<T> where T : NumberField<T>, T : Arithmetic<T> {
            val constants = resolveArithmeticConstants<T>("MutableLinearPolynomial.one")
            return MutableLinearPolynomial(emptyList(), constants.one)
        }

        /**
         * Creates a constant polynomial with the given value.
         * Use this when you need a specific constant or when T doesn't support reflection.
         */
        fun <T : NumberField<T>> fromConstant(value: T): MutableLinearPolynomial<T> {
            return MutableLinearPolynomial(emptyList(), value)
        }
    }

    fun addMonomial(monomial: LinearMonomial<T>) {
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

    fun toLinearPolynomial(): LinearPolynomial<T> {
        return LinearPolynomial(_monomials.toList(), _constant)
    }

    fun toImmutable(): LinearPolynomial<T> = toLinearPolynomial()

    override fun toString(): String {
        return "MutableLinearPolynomial(monomials=$_monomials, constant=$_constant)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MutableLinearPolynomial<*>) return false
        return _monomials == other._monomials && _constant == other._constant
    }

    override fun hashCode(): Int {
        var result = _monomials.hashCode()
        result = 31 * result + _constant.hashCode()
        return result
    }
}

// Conversion from LinearPolynomial to MutableLinearPolynomial
fun <T : NumberField<T>> LinearPolynomial<T>.toMutable(): MutableLinearPolynomial<T> {
    return MutableLinearPolynomial(monomials, constant)
}

// Conversion from MutableLinearPolynomial to LinearPolynomial
fun <T : NumberField<T>> MutableLinearPolynomial<T>.toImmutable(): LinearPolynomial<T> {
    return this.toLinearPolynomial()
}

// Unary minus: -MutableLinearPolynomial<T>
operator fun <T : NumberField<T>> MutableLinearPolynomial<T>.unaryMinus(): MutableLinearPolynomial<T> {
    return MutableLinearPolynomial(monomials.map { -it }, -constant)
}

// Addition assignment: MutableLinearPolynomial<T> += LinearMonomial<T>
operator fun <T : NumberField<T>> MutableLinearPolynomial<T>.plusAssign(rhs: LinearMonomial<T>) {
    addMonomial(rhs)
}

// Addition assignment: MutableLinearPolynomial<T> += LinearPolynomial<T>
operator fun <T : NumberField<T>> MutableLinearPolynomial<T>.plusAssign(rhs: LinearPolynomial<T>) {
    _monomials.addAll(rhs.monomials)
    _constant = _constant + rhs.constant
}

// Addition assignment: MutableLinearPolynomial<T> += MutableLinearPolynomial<T>
operator fun <T : NumberField<T>> MutableLinearPolynomial<T>.plusAssign(rhs: MutableLinearPolynomial<T>) {
    _monomials.addAll(rhs.monomials)
    _constant = _constant + rhs.constant
}

// Addition assignment: MutableLinearPolynomial<T> += T
operator fun <T : NumberField<T>> MutableLinearPolynomial<T>.plusAssign(rhs: T) {
    _constant = _constant + rhs
}

// Subtraction assignment: MutableLinearPolynomial<T> -= LinearMonomial<T>
operator fun <T : NumberField<T>> MutableLinearPolynomial<T>.minusAssign(rhs: LinearMonomial<T>) {
    _monomials.add(-rhs)
}

// Subtraction assignment: MutableLinearPolynomial<T> -= LinearPolynomial<T>
operator fun <T : NumberField<T>> MutableLinearPolynomial<T>.minusAssign(rhs: LinearPolynomial<T>) {
    _monomials.addAll(rhs.monomials.map { -it })
    _constant = _constant - rhs.constant
}

// Subtraction assignment: MutableLinearPolynomial<T> -= MutableLinearPolynomial<T>
operator fun <T : NumberField<T>> MutableLinearPolynomial<T>.minusAssign(rhs: MutableLinearPolynomial<T>) {
    _monomials.addAll(rhs.monomials.map { -it })
    _constant = _constant - rhs.constant
}

// Subtraction assignment: MutableLinearPolynomial<T> -= T
operator fun <T : NumberField<T>> MutableLinearPolynomial<T>.minusAssign(rhs: T) {
    _constant = _constant - rhs
}

// Multiplication assignment: MutableLinearPolynomial<T> *= T
operator fun <T : NumberField<T>> MutableLinearPolynomial<T>.timesAssign(rhs: T) {
    for (i in _monomials.indices) {
        _monomials[i] = _monomials[i] * rhs
    }
    _constant = _constant * rhs
}

// Division assignment: MutableLinearPolynomial<T> /= T
operator fun <T : NumberField<T>> MutableLinearPolynomial<T>.divAssign(rhs: T) {
    for (i in _monomials.indices) {
        _monomials[i] = _monomials[i] / rhs
    }
    _constant = _constant / rhs
}

