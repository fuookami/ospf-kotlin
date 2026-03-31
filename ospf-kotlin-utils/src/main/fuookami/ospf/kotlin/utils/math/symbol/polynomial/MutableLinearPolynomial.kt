package fuookami.ospf.kotlin.utils.math.symbol.polynomial

import fuookami.ospf.kotlin.utils.math.algebra.number.*
import fuookami.ospf.kotlin.utils.math.algebra.value_range.*

import fuookami.ospf.kotlin.utils.math.symbol.Category
import fuookami.ospf.kotlin.utils.math.symbol.Linear
import fuookami.ospf.kotlin.utils.math.symbol.monomial.LinearMonomial

/**
 * Mutable version of LinearPolynomial for in-place modifications.
 * 
 * Use this when you need to build or modify polynomials incrementally.
 * Convert to [LinearPolynomial] when you need an immutable version.
 */
class MutableLinearPolynomial<T>(
    monomials: List<LinearMonomial<T>> = emptyList(),
    constant: T
) {
    private val _monomials: MutableList<LinearMonomial<T>> = monomials.toMutableList()
    private var _constant: T = constant

    val monomials: List<LinearMonomial<T>> get() = _monomials.toList()
    val constant: T get() = _constant

    val category: Category
        get() = Linear

    companion object {
        val One: Flt64 = Flt64.one

        fun <T : NumberField<T>> zero(): MutableLinearPolynomial<T> {
            return MutableLinearPolynomial(emptyList(), One as T)
        }

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