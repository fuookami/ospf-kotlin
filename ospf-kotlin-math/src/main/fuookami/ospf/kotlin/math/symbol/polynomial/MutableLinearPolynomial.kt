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

import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 可变线性多项式
 * Mutable Linear Polynomial
 *
 * 线性多项式的可变版本，支持原地修改操作。
 * 用于需要增量构建或修改多项式的场景，构建完成后可转换为不可变的 [LinearPolynomial]。
 * A mutable version of linear polynomials, supporting in-place modifications.
 * Use this when you need to build or modify polynomials incrementally.
 * Convert to [LinearPolynomial] when you need an immutable version.
 *
 * @property _monomials 内部可变的线性单项式列表 / Internal mutable list of linear monomials
 * @property _constant 内部可变的常数项 / Internal mutable constant term
*/
class MutableLinearPolynomial<T : NumberField<T>>(
    monomials: List<LinearMonomial<T>> = emptyList(),
    constant: T
) {
    internal val _monomials: MutableList<LinearMonomial<T>> = monomials.toMutableList()
    internal var _constant: T = constant

    /**
     * 线性单项式列表（只读）
     * List of linear monomials (read-only)
    */
    val monomials: List<LinearMonomial<T>> get() = _monomials.toList()

    /**
     * 常数项
     * Constant term
    */
    val constant: T get() = _constant

    /**
     * 表达式类型分类
     * Expression type category
     *
     * 可变线性多项式总是属于线性类型。
     * Mutable linear polynomials always belong to the Linear category.
    */
    val category: Category
        get() = Linear

    companion object {
        /**
         * 创建零多项式（常数项为 0）
         * Creates a zero polynomial (constant = 0)
         *
         * 需要 T 的伴生对象实现 ArithmeticConstants<T>。
         * Requires T to have a companion object implementing ArithmeticConstants<T>.
         *
         * @return 零多项式 / Zero polynomial
        */
        inline fun <reified T> zero(): Ret<MutableLinearPolynomial<T>> where T : NumberField<T>, T : Arithmetic<T> {
            return resolveArithmeticConstantsSafe<T>("MutableLinearPolynomial.zero").mapResolved { constants ->
                MutableLinearPolynomial(emptyList(), constants.zero)
            }
        }

        /**
         * 创建值为 1 的常数多项式
         * Creates a constant polynomial with value 1
         *
         * 需要 T 的伴生对象实现 ArithmeticConstants<T>。
         * Requires T to have a companion object implementing ArithmeticConstants<T>.
         *
         * @return 值为 1 的多项式 / Polynomial with value 1
        */
        inline fun <reified T> one(): Ret<MutableLinearPolynomial<T>> where T : NumberField<T>, T : Arithmetic<T> {
            return resolveArithmeticConstantsSafe<T>("MutableLinearPolynomial.one").mapResolved { constants ->
                MutableLinearPolynomial(emptyList(), constants.one)
            }
        }

        /**
         * 使用指定值创建常数多项式
         * Creates a constant polynomial with the given value
         *
         * 当需要特定的常数值或 T 不支持反射时使用此方法。
         * Use this when you need a specific constant or when T doesn't support reflection.
         *
         * @param value 常数值 / Constant value
         * @return 常数多项式 / Constant polynomial
        */
        fun <T : NumberField<T>> fromConstant(value: T): MutableLinearPolynomial<T> {
            return MutableLinearPolynomial(emptyList(), value)
        }
    }

    /**
     * 添加线性单项式
     * Adds a linear monomial
     *
     * @param monomial 要添加的线性单项式 / The linear monomial to add
    */
    fun addMonomial(monomial: LinearMonomial<T>) {
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
     * 转换为不可变的线性多项式
     * Converts to an immutable linear polynomial
     *
     * @return 不可变的线性多项式 / Immutable linear polynomial
    */
    fun toLinearPolynomial(): LinearPolynomial<T> {
        return LinearPolynomial(_monomials.toList(), _constant)
    }

    /**
     * 转换为不可变形式
     * Converts to immutable form
     *
     * @return 不可变的线性多项式 / Immutable linear polynomial
    */
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

/**
 * 将不可变线性多项式转换为可变形式
 * Converts an immutable linear polynomial to mutable form
 *
 * @receiver 不可变线性多项式 / Immutable linear polynomial
 * @return 可变线性多项式 / Mutable linear polynomial
*/
fun <T : NumberField<T>> LinearPolynomial<T>.toMutable(): MutableLinearPolynomial<T> {
    return MutableLinearPolynomial(monomials, constant)
}

/**
 * 将可变线性多项式转换为不可变形式
 * Converts a mutable linear polynomial to immutable form
 *
 * @receiver 可变线性多项式 / Mutable linear polynomial
 * @return 不可变线性多项式 / Immutable linear polynomial
*/
fun <T : NumberField<T>> MutableLinearPolynomial<T>.toImmutable(): LinearPolynomial<T> {
    return this.toLinearPolynomial()
}

/**
 * 可变线性多项式的负运算符
 * Negation operator for mutable linear polynomial
 *
 * @receiver 可变线性多项式 / Mutable linear polynomial
 * @return 所有项取负后的可变线性多项式 / Mutable linear polynomial with all terms negated
*/
operator fun <T : NumberField<T>> MutableLinearPolynomial<T>.unaryMinus(): MutableLinearPolynomial<T> {
    return MutableLinearPolynomial(monomials.map { -it }, -constant)
}

/**
 * 可变线性多项式的加法赋值运算符（单项式）
 * Addition assignment operator for mutable linear polynomial (monomial)
 *
 * @receiver 可变线性多项式 / Mutable linear polynomial
 * @param rhs 线性单项式 / Linear monomial
*/
operator fun <T : NumberField<T>> MutableLinearPolynomial<T>.plusAssign(rhs: LinearMonomial<T>) {
    addMonomial(rhs)
}

/**
 * 可变线性多项式的加法赋值运算符（多项式）
 * Addition assignment operator for mutable linear polynomial (polynomial)
 *
 * @receiver 可变线性多项式 / Mutable linear polynomial
 * @param rhs 线性多项式 / Linear polynomial
*/
operator fun <T : NumberField<T>> MutableLinearPolynomial<T>.plusAssign(rhs: LinearPolynomial<T>) {
    _monomials.addAll(rhs.monomials)
    _constant = _constant + rhs.constant
}

/**
 * 可变线性多项式的加法赋值运算符（可变多项式）
 * Addition assignment operator for mutable linear polynomial (mutable polynomial)
 *
 * @receiver 可变线性多项式 / Mutable linear polynomial
 * @param rhs 可变线性多项式 / Mutable linear polynomial
*/
operator fun <T : NumberField<T>> MutableLinearPolynomial<T>.plusAssign(rhs: MutableLinearPolynomial<T>) {
    _monomials.addAll(rhs.monomials)
    _constant = _constant + rhs.constant
}

/**
 * 可变线性多项式的加法赋值运算符（标量）
 * Addition assignment operator for mutable linear polynomial (scalar)
 *
 * @receiver 可变线性多项式 / Mutable linear polynomial
 * @param rhs 标量值 / Scalar value
*/
operator fun <T : NumberField<T>> MutableLinearPolynomial<T>.plusAssign(rhs: T) {
    _constant = _constant + rhs
}

/**
 * 可变线性多项式的减法赋值运算符（单项式）
 * Subtraction assignment operator for mutable linear polynomial (monomial)
 *
 * @receiver 可变线性多项式 / Mutable linear polynomial
 * @param rhs 线性单项式 / Linear monomial
*/
operator fun <T : NumberField<T>> MutableLinearPolynomial<T>.minusAssign(rhs: LinearMonomial<T>) {
    _monomials.add(-rhs)
}

/**
 * 可变线性多项式的减法赋值运算符（多项式）
 * Subtraction assignment operator for mutable linear polynomial (polynomial)
 *
 * @receiver 可变线性多项式 / Mutable linear polynomial
 * @param rhs 线性多项式 / Linear polynomial
*/
operator fun <T : NumberField<T>> MutableLinearPolynomial<T>.minusAssign(rhs: LinearPolynomial<T>) {
    _monomials.addAll(rhs.monomials.map { -it })
    _constant = _constant - rhs.constant
}

/**
 * 可变线性多项式的减法赋值运算符（可变多项式）
 * Subtraction assignment operator for mutable linear polynomial (mutable polynomial)
 *
 * @receiver 可变线性多项式 / Mutable linear polynomial
 * @param rhs 可变线性多项式 / Mutable linear polynomial
*/
operator fun <T : NumberField<T>> MutableLinearPolynomial<T>.minusAssign(rhs: MutableLinearPolynomial<T>) {
    _monomials.addAll(rhs.monomials.map { -it })
    _constant = _constant - rhs.constant
}

/**
 * 可变线性多项式的减法赋值运算符（标量）
 * Subtraction assignment operator for mutable linear polynomial (scalar)
 *
 * @receiver 可变线性多项式 / Mutable linear polynomial
 * @param rhs 标量值 / Scalar value
*/
operator fun <T : NumberField<T>> MutableLinearPolynomial<T>.minusAssign(rhs: T) {
    _constant = _constant - rhs
}

/**
 * 可变线性多项式的乘法赋值运算符
 * Multiplication assignment operator for mutable linear polynomial
 *
 * @receiver 可变线性多项式 / Mutable linear polynomial
 * @param rhs 标量值 / Scalar value
*/
operator fun <T : NumberField<T>> MutableLinearPolynomial<T>.timesAssign(rhs: T) {
    for (i in _monomials.indices) {
        _monomials[i] = _monomials[i] * rhs
    }
    _constant = _constant * rhs
}

/**
 * 可变线性多项式的除法赋值运算符
 * Division assignment operator for mutable linear polynomial
 *
 * @receiver 可变线性多项式 / Mutable linear polynomial
 * @param rhs 标量值 / Scalar value
*/
operator fun <T : NumberField<T>> MutableLinearPolynomial<T>.divAssign(rhs: T) {
    for (i in _monomials.indices) {
        _monomials[i] = _monomials[i] / rhs
    }
    _constant = _constant / rhs
}
