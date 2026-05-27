/**
 * 可变规范多项式
 * Mutable Canonical Polynomial
 *
 * 提供规范多项式的可变版本，支持原地修改操作。
 * 用于需要增量构建或修改多项式的场景，构建完成后可转换为不可变的 CanonicalPolynomial。
 * Provides a mutable version of canonical polynomials, supporting in-place modifications.
 * Used for scenarios requiring incremental building or modification of polynomials,
 * and can be converted to an immutable CanonicalPolynomial after construction.
 */
package fuookami.ospf.kotlin.math.symbol.polynomial

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.math.symbol.Category
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Nonlinear
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.math.symbol.monomial.CanonicalMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.div
import fuookami.ospf.kotlin.math.symbol.monomial.times
import fuookami.ospf.kotlin.math.symbol.monomial.unaryMinus

/**
 * 可变规范多项式
 * Mutable Canonical Polynomial
 *
 * 规范多项式的可变版本，支持原地修改操作。
 * 用于需要增量构建或修改多项式的场景，构建完成后可转换为不可变的 [CanonicalPolynomial]。
 * A mutable version of canonical polynomials, supporting in-place modifications.
 * Use this when you need to build or modify polynomials incrementally.
 * Convert to [CanonicalPolynomial] when you need an immutable version.
 *
 * @property _monomials 内部可变的规范单项式列表 / Internal mutable list of canonical monomials
 * @property _constant 内部可变的常数项 / Internal mutable constant term
 */
class MutableCanonicalPolynomial<T : NumberField<T>>(
    monomials: List<CanonicalMonomial<T>> = emptyList(),
    constant: T
) {
    internal val _monomials: MutableList<CanonicalMonomial<T>> = monomials.toMutableList()
    internal var _constant: T = constant

    /**
     * 规范单项式列表（只读）
     * List of canonical monomials (read-only)
     */
    val monomials: List<CanonicalMonomial<T>> get() = _monomials.toList()

    /**
     * 常数项
     * Constant term
     */
    val constant: T get() = _constant

    /**
     * 表达式类型分类
     * Expression type category
     *
     * 根据单项式的最高次数返回对应的分类。
     * Returns the corresponding category based on the maximum degree of monomials.
     */
    val category: Category
        get() = when (_monomials.maxOfOrNull { it.degree } ?: 0) {
            0, 1 -> Linear
            2 -> Quadratic
            else -> Nonlinear
        }

    companion object {
        /**
         * 创建零多项式（常数项为 0）
         * Creates a zero polynomial (constant = 0)
         *
         * @return 零多项式 / Zero polynomial
         */
        inline fun <reified T> zero(): MutableCanonicalPolynomial<T> where T : NumberField<T>, T : Arithmetic<T> {
            val constants = resolveArithmeticConstants<T>("MutableCanonicalPolynomial.zero")
            return MutableCanonicalPolynomial(emptyList(), constants.zero)
        }

        /**
         * 创建值为 1 的常数多项式
         * Creates a constant polynomial with value 1
         *
         * @return 值为 1 的多项式 / Polynomial with value 1
         */
        inline fun <reified T> one(): MutableCanonicalPolynomial<T> where T : NumberField<T>, T : Arithmetic<T> {
            val constants = resolveArithmeticConstants<T>("MutableCanonicalPolynomial.one")
            return MutableCanonicalPolynomial(emptyList(), constants.one)
        }

        /**
         * 使用指定值创建常数多项式
         * Creates a constant polynomial with the given value
         *
         * @param value 常数值 / Constant value
         * @return 常数多项式 / Constant polynomial
         */
        fun <T : NumberField<T>> fromConstant(value: T): MutableCanonicalPolynomial<T> {
            return MutableCanonicalPolynomial(emptyList(), value)
        }
    }

    /**
     * 添加规范单项式
     * Adds a canonical monomial
     *
     * @param monomial 要添加的规范单项式 / The canonical monomial to add
     */
    fun addMonomial(monomial: CanonicalMonomial<T>) {
        _monomials.add(monomial)
    }

    /**
     * 增加常数项
     * Adds to the constant term
     *
     * @param value 要增加的值 / The value to add
     */
    fun addConstant(value: T) {
        _constant = _constant + value
    }

    /**
     * 设置常数项
     * Sets the constant term
     *
     * @param value 新的常数项值 / The new constant term value
     */
    fun setConstant(value: T) {
        _constant = value
    }

    /**
     * 清除所有单项式
     * Clears all monomials
     */
    fun clear() {
        _monomials.clear()
    }

    /**
     * 转换为不可变的规范多项式
     * Converts to an immutable canonical polynomial
     *
     * @return 不可变的规范多项式 / Immutable canonical polynomial
     */
    fun toCanonicalPolynomial(): CanonicalPolynomial<T> {
        return CanonicalPolynomial(_monomials.toList(), _constant)
    }

    /**
     * 转换为不可变形式
     * Converts to immutable form
     *
     * @return 不可变的规范多项式 / Immutable canonical polynomial
     */
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

/**
 * 将不可变规范多项式转换为可变形式
 * Converts an immutable canonical polynomial to mutable form
 *
 * @receiver 不可变规范多项式 / Immutable canonical polynomial
 * @return 可变规范多项式 / Mutable canonical polynomial
 */
fun <T : NumberField<T>> CanonicalPolynomial<T>.toMutable(): MutableCanonicalPolynomial<T> {
    return MutableCanonicalPolynomial(monomials, constant)
}

/**
 * 将可变规范多项式转换为不可变形式
 * Converts a mutable canonical polynomial to immutable form
 *
 * @receiver 可变规范多项式 / Mutable canonical polynomial
 * @return 不可变规范多项式 / Immutable canonical polynomial
 */
fun <T : NumberField<T>> MutableCanonicalPolynomial<T>.toImmutable(): CanonicalPolynomial<T> {
    return this.toCanonicalPolynomial()
}

/**
 * 可变规范多项式的负运算符
 * Negation operator for mutable canonical polynomial
 *
 * @receiver 可变规范多项式 / Mutable canonical polynomial
 * @return 所有项取负后的可变规范多项式 / Mutable canonical polynomial with all terms negated
 */
operator fun <T : NumberField<T>> MutableCanonicalPolynomial<T>.unaryMinus(): MutableCanonicalPolynomial<T> {
    return MutableCanonicalPolynomial(monomials.map { -it }, -constant)
}

/**
 * 可变规范多项式的加法赋值运算符（规范单项式）
 * Addition assignment operator for mutable canonical polynomial (canonical monomial)
 *
 * @receiver 可变规范多项式 / Mutable canonical polynomial
 * @param rhs 规范单项式 / Canonical monomial
 */
operator fun <T : NumberField<T>> MutableCanonicalPolynomial<T>.plusAssign(rhs: CanonicalMonomial<T>) {
    addMonomial(rhs)
}

/**
 * 可变规范多项式的加法赋值运算符（规范多项式）
 * Addition assignment operator for mutable canonical polynomial (canonical polynomial)
 *
 * @receiver 可变规范多项式 / Mutable canonical polynomial
 * @param rhs 规范多项式 / Canonical polynomial
 */
operator fun <T : NumberField<T>> MutableCanonicalPolynomial<T>.plusAssign(rhs: CanonicalPolynomial<T>) {
    _monomials.addAll(rhs.monomials)
    _constant = _constant + rhs.constant
}

/**
 * 可变规范多项式的加法赋值运算符（可变规范多项式）
 * Addition assignment operator for mutable canonical polynomial (mutable canonical polynomial)
 *
 * @receiver 可变规范多项式 / Mutable canonical polynomial
 * @param rhs 可变规范多项式 / Mutable canonical polynomial
 */
operator fun <T : NumberField<T>> MutableCanonicalPolynomial<T>.plusAssign(rhs: MutableCanonicalPolynomial<T>) {
    _monomials.addAll(rhs.monomials)
    _constant = _constant + rhs.constant
}

/**
 * 可变规范多项式的加法赋值运算符（标量）
 * Addition assignment operator for mutable canonical polynomial (scalar)
 *
 * @receiver 可变规范多项式 / Mutable canonical polynomial
 * @param rhs 标量值 / Scalar value
 */
operator fun <T : NumberField<T>> MutableCanonicalPolynomial<T>.plusAssign(rhs: T) {
    _constant = _constant + rhs
}

/**
 * 可变规范多项式的减法赋值运算符（规范单项式）
 * Subtraction assignment operator for mutable canonical polynomial (canonical monomial)
 *
 * @receiver 可变规范多项式 / Mutable canonical polynomial
 * @param rhs 规范单项式 / Canonical monomial
 */
operator fun <T : NumberField<T>> MutableCanonicalPolynomial<T>.minusAssign(rhs: CanonicalMonomial<T>) {
    _monomials.add(-rhs)
}

/**
 * 可变规范多项式的减法赋值运算符（规范多项式）
 * Subtraction assignment operator for mutable canonical polynomial (canonical polynomial)
 *
 * @receiver 可变规范多项式 / Mutable canonical polynomial
 * @param rhs 规范多项式 / Canonical polynomial
 */
operator fun <T : NumberField<T>> MutableCanonicalPolynomial<T>.minusAssign(rhs: CanonicalPolynomial<T>) {
    _monomials.addAll(rhs.monomials.map { -it })
    _constant = _constant - rhs.constant
}

/**
 * 可变规范多项式的减法赋值运算符（可变规范多项式）
 * Subtraction assignment operator for mutable canonical polynomial (mutable canonical polynomial)
 *
 * @receiver 可变规范多项式 / Mutable canonical polynomial
 * @param rhs 可变规范多项式 / Mutable canonical polynomial
 */
operator fun <T : NumberField<T>> MutableCanonicalPolynomial<T>.minusAssign(rhs: MutableCanonicalPolynomial<T>) {
    _monomials.addAll(rhs.monomials.map { -it })
    _constant = _constant - rhs.constant
}

/**
 * 可变规范多项式的减法赋值运算符（标量）
 * Subtraction assignment operator for mutable canonical polynomial (scalar)
 *
 * @receiver 可变规范多项式 / Mutable canonical polynomial
 * @param rhs 标量值 / Scalar value
 */
operator fun <T : NumberField<T>> MutableCanonicalPolynomial<T>.minusAssign(rhs: T) {
    _constant = _constant - rhs
}

/**
 * 可变规范多项式的乘法赋值运算符
 * Multiplication assignment operator for mutable canonical polynomial
 *
 * @receiver 可变规范多项式 / Mutable canonical polynomial
 * @param rhs 标量值 / Scalar value
 */
operator fun <T : NumberField<T>> MutableCanonicalPolynomial<T>.timesAssign(rhs: T) {
    for (i in _monomials.indices) {
        _monomials[i] = _monomials[i] * rhs
    }
    _constant = _constant * rhs
}

/**
 * 可变规范多项式的除法赋值运算符
 * Division assignment operator for mutable canonical polynomial
 *
 * @receiver 可变规范多项式 / Mutable canonical polynomial
 * @param rhs 标量值 / Scalar value
 */
operator fun <T : NumberField<T>> MutableCanonicalPolynomial<T>.divAssign(rhs: T) {
    for (i in _monomials.indices) {
        _monomials[i] = _monomials[i] / rhs
    }
    _constant = _constant / rhs
}
