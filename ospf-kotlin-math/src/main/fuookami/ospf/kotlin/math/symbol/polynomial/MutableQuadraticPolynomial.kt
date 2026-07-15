/**
 * 可变二次多项式
 * Mutable Quadratic Polynomial
 *
 * 提供二次多项式的可变版本，支持原地修改操作。
 * 用于需要增量构建或修改多项式的场景，构建完成后可转换为不可变的 QuadraticPolynomial。
 * Provides a mutable version of quadratic polynomials, supporting in-place modifications.
 * Used for scenarios requiring incremental building or modification of polynomials,
 * and can be converted to an immutable QuadraticPolynomial after construction.
*/
package fuookami.ospf.kotlin.math.symbol.polynomial

import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 可变二次多项式
 * Mutable Quadratic Polynomial
 *
 * 二次多项式的可变版本，支持原地修改操作。
 * 用于需要增量构建或修改多项式的场景，构建完成后可转换为不可变的 [QuadraticPolynomial]。
 * A mutable version of quadratic polynomials, supporting in-place modifications.
 * Use this when you need to build or modify polynomials incrementally.
 * Convert to [QuadraticPolynomial] when you need an immutable version.
 *
 * @property _monomials 内部可变的二次单项式列表 / Internal mutable list of quadratic monomials
 * @property _constant 内部可变的常数项 / Internal mutable constant term
*/
class MutableQuadraticPolynomial<T : NumberField<T>>(
    monomials: List<QuadraticMonomial<T>> = emptyList(),
    constant: T
) {
    internal val _monomials: MutableList<QuadraticMonomial<T>> = monomials.toMutableList()
    internal var _constant: T = constant

    /**
     * 二次单项式列表（只读）
     * List of quadratic monomials (read-only)
    */
    val monomials: List<QuadraticMonomial<T>> get() = _monomials.toList()

    /**
     * 常数项
     * Constant term
    */
    val constant: T get() = _constant

    /**
     * 表达式类型分类
     * Expression type category
     *
     * 如果包含真正的二次项，则返回 Quadratic；否则返回 Linear。
     * Returns Quadratic if there are true quadratic terms; otherwise returns Linear.
    */
    val category: Category
        get() = if (_monomials.any { it.isQuadratic }) Quadratic else Linear

    companion object {
        /**
         * 创建零多项式（常数项为 0）
         * Creates a zero polynomial (constant = 0)
         *
         * @return 零多项式 / Zero polynomial
        */
        inline fun <reified T> zero(): Ret<MutableQuadraticPolynomial<T>> where T : NumberField<T>, T : Arithmetic<T> {
            return resolveArithmeticConstantsSafe<T>("MutableQuadraticPolynomial.zero").mapResolved { constants ->
                MutableQuadraticPolynomial(emptyList(), constants.zero)
            }
        }

        /**
         * 创建值为 1 的常数多项式
         * Creates a constant polynomial with value 1
         *
         * @return 值为 1 的多项式 / Polynomial with value 1
        */
        inline fun <reified T> one(): Ret<MutableQuadraticPolynomial<T>> where T : NumberField<T>, T : Arithmetic<T> {
            return resolveArithmeticConstantsSafe<T>("MutableQuadraticPolynomial.one").mapResolved { constants ->
                MutableQuadraticPolynomial(emptyList(), constants.one)
            }
        }

        /**
         * 使用指定值创建常数多项式
         * Creates a constant polynomial with the given value
         *
         * @param value 常数值 / Constant value
         * @return 常数多项式 / Constant polynomial
        */
        fun <T : NumberField<T>> fromConstant(value: T): MutableQuadraticPolynomial<T> {
            return MutableQuadraticPolynomial(emptyList(), value)
        }
    }

    /**
     * 添加二次单项式
     * Adds a quadratic monomial
     *
     * @param monomial 要添加的二次单项式 / The quadratic monomial to add
    */
    fun addMonomial(monomial: QuadraticMonomial<T>) {
        _monomials.add(monomial)
    }

    /**
     * 添加线性单项式（转换为二次单项式形式）
     * Adds a linear monomial (converted to quadratic monomial form)
     *
     * @param monomial 要添加的线性单项式 / The linear monomial to add
    */
    fun addLinearMonomial(monomial: LinearMonomial<T>) {
        _monomials.add(QuadraticMonomial.linear(monomial.coefficient, monomial.symbol))
    }

    /**
     * 增加常数项
     * Adds to the constant term
     *
     * @param value 要增加的值 / The value to add
    */
    fun addConstant(value: T) {
        _constant += value
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
     * 转换为不可变的二次多项式
     * Converts to an immutable quadratic polynomial
     *
     * @return 不可变的二次多项式 / Immutable quadratic polynomial
    */
    fun toQuadraticPolynomial(): QuadraticPolynomial<T> {
        return QuadraticPolynomial(_monomials.toList(), _constant)
    }

    /**
     * 转换为不可变形式
     * Converts to immutable form
     *
     * @return 不可变的二次多项式 / Immutable quadratic polynomial
    */
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

/**
 * 将不可变二次多项式转换为可变形式
 * Converts an immutable quadratic polynomial to mutable form
 *
 * @receiver 不可变二次多项式 / Immutable quadratic polynomial
 * @return 可变二次多项式 / Mutable quadratic polynomial
*/
fun <T : NumberField<T>> QuadraticPolynomial<T>.toMutable(): MutableQuadraticPolynomial<T> {
    return MutableQuadraticPolynomial(monomials, constant)
}

/**
 * 将可变二次多项式转换为不可变形式
 * Converts a mutable quadratic polynomial to immutable form
 *
 * @receiver 可变二次多项式 / Mutable quadratic polynomial
 * @return 不可变二次多项式 / Immutable quadratic polynomial
*/
fun <T : NumberField<T>> MutableQuadraticPolynomial<T>.toImmutable(): QuadraticPolynomial<T> {
    return this.toQuadraticPolynomial()
}

/**
 * 可变二次多项式的负运算符
 * Negation operator for mutable quadratic polynomial
 *
 * @receiver 可变二次多项式 / Mutable quadratic polynomial
 * @return 所有项取负后的可变二次多项式 / Mutable quadratic polynomial with all terms negated
*/
operator fun <T : NumberField<T>> MutableQuadraticPolynomial<T>.unaryMinus(): MutableQuadraticPolynomial<T> {
    return MutableQuadraticPolynomial(monomials.map { -it }, -constant)
}

/**
 * 可变二次多项式的加法赋值运算符（二次单项式）
 * Addition assignment operator for mutable quadratic polynomial (quadratic monomial)
 *
 * @receiver 可变二次多项式 / Mutable quadratic polynomial
 * @param rhs 二次单项式 / Quadratic monomial
*/
operator fun <T : NumberField<T>> MutableQuadraticPolynomial<T>.plusAssign(rhs: QuadraticMonomial<T>) {
    addMonomial(rhs)
}

/**
 * 可变二次多项式的加法赋值运算符（线性单项式）
 * Addition assignment operator for mutable quadratic polynomial (linear monomial)
 *
 * @receiver 可变二次多项式 / Mutable quadratic polynomial
 * @param rhs 线性单项式 / Linear monomial
*/
operator fun <T : NumberField<T>> MutableQuadraticPolynomial<T>.plusAssign(rhs: LinearMonomial<T>) {
    addLinearMonomial(rhs)
}

/**
 * 可变二次多项式的加法赋值运算符（二次多项式）
 * Addition assignment operator for mutable quadratic polynomial (quadratic polynomial)
 *
 * @receiver 可变二次多项式 / Mutable quadratic polynomial
 * @param rhs 二次多项式 / Quadratic polynomial
*/
operator fun <T : NumberField<T>> MutableQuadraticPolynomial<T>.plusAssign(rhs: QuadraticPolynomial<T>) {
    _monomials.addAll(rhs.monomials)
    _constant = _constant + rhs.constant
}

/**
 * 可变二次多项式的加法赋值运算符（可变二次多项式）
 * Addition assignment operator for mutable quadratic polynomial (mutable quadratic polynomial)
 *
 * @receiver 可变二次多项式 / Mutable quadratic polynomial
 * @param rhs 可变二次多项式 / Mutable quadratic polynomial
*/
operator fun <T : NumberField<T>> MutableQuadraticPolynomial<T>.plusAssign(rhs: MutableQuadraticPolynomial<T>) {
    _monomials.addAll(rhs.monomials)
    _constant = _constant + rhs.constant
}

/**
 * 可变二次多项式的加法赋值运算符（线性多项式）
 * Addition assignment operator for mutable quadratic polynomial (linear polynomial)
 *
 * @receiver 可变二次多项式 / Mutable quadratic polynomial
 * @param rhs 线性多项式 / Linear polynomial
*/
operator fun <T : NumberField<T>> MutableQuadraticPolynomial<T>.plusAssign(rhs: LinearPolynomial<T>) {
    _monomials.addAll(rhs.monomials.map { QuadraticMonomial.linear(it.coefficient, it.symbol) })
    _constant = _constant + rhs.constant
}

/**
 * 可变二次多项式的加法赋值运算符（标量）
 * Addition assignment operator for mutable quadratic polynomial (scalar)
 *
 * @receiver 可变二次多项式 / Mutable quadratic polynomial
 * @param rhs 标量值 / Scalar value
*/
operator fun <T : NumberField<T>> MutableQuadraticPolynomial<T>.plusAssign(rhs: T) {
    _constant = _constant + rhs
}

/**
 * 可变二次多项式的减法赋值运算符（二次单项式）
 * Subtraction assignment operator for mutable quadratic polynomial (quadratic monomial)
 *
 * @receiver 可变二次多项式 / Mutable quadratic polynomial
 * @param rhs 二次单项式 / Quadratic monomial
*/
operator fun <T : NumberField<T>> MutableQuadraticPolynomial<T>.minusAssign(rhs: QuadraticMonomial<T>) {
    _monomials.add(-rhs)
}

/**
 * 可变二次多项式的减法赋值运算符（线性单项式）
 * Subtraction assignment operator for mutable quadratic polynomial (linear monomial)
 *
 * @receiver 可变二次多项式 / Mutable quadratic polynomial
 * @param rhs 线性单项式 / Linear monomial
*/
operator fun <T : NumberField<T>> MutableQuadraticPolynomial<T>.minusAssign(rhs: LinearMonomial<T>) {
    _monomials.add(QuadraticMonomial.linear(-rhs.coefficient, rhs.symbol))
}

/**
 * 可变二次多项式的减法赋值运算符（二次多项式）
 * Subtraction assignment operator for mutable quadratic polynomial (quadratic polynomial)
 *
 * @receiver 可变二次多项式 / Mutable quadratic polynomial
 * @param rhs 二次多项式 / Quadratic polynomial
*/
operator fun <T : NumberField<T>> MutableQuadraticPolynomial<T>.minusAssign(rhs: QuadraticPolynomial<T>) {
    _monomials.addAll(rhs.monomials.map { -it })
    _constant -= rhs.constant
}

/**
 * 可变二次多项式的减法赋值运算符（可变二次多项式）
 * Subtraction assignment operator for mutable quadratic polynomial (mutable quadratic polynomial)
 *
 * @receiver 可变二次多项式 / Mutable quadratic polynomial
 * @param rhs 可变二次多项式 / Mutable quadratic polynomial
*/
operator fun <T : NumberField<T>> MutableQuadraticPolynomial<T>.minusAssign(rhs: MutableQuadraticPolynomial<T>) {
    _monomials.addAll(rhs.monomials.map { -it })
    _constant -= rhs.constant
}

/**
 * 可变二次多项式的减法赋值运算符（线性多项式）
 * Subtraction assignment operator for mutable quadratic polynomial (linear polynomial)
 *
 * @receiver 可变二次多项式 / Mutable quadratic polynomial
 * @param rhs 线性多项式 / Linear polynomial
*/
operator fun <T : NumberField<T>> MutableQuadraticPolynomial<T>.minusAssign(rhs: LinearPolynomial<T>) {
    _monomials.addAll(rhs.monomials.map { QuadraticMonomial.linear(-it.coefficient, it.symbol) })
    _constant -= rhs.constant
}

/**
 * 可变二次多项式的减法赋值运算符（标量）
 * Subtraction assignment operator for mutable quadratic polynomial (scalar)
 *
 * @receiver 可变二次多项式 / Mutable quadratic polynomial
 * @param rhs 标量值 / Scalar value
*/
operator fun <T : NumberField<T>> MutableQuadraticPolynomial<T>.minusAssign(rhs: T) {
    _constant -= rhs
}

/**
 * 可变二次多项式的乘法赋值运算符
 * Multiplication assignment operator for mutable quadratic polynomial
 *
 * @receiver 可变二次多项式 / Mutable quadratic polynomial
 * @param rhs 标量值 / Scalar value
*/
operator fun <T : NumberField<T>> MutableQuadraticPolynomial<T>.timesAssign(rhs: T) {
    for (i in _monomials.indices) {
        _monomials[i] = _monomials[i] * rhs
    }
    _constant *= rhs
}

/**
 * 可变二次多项式的除法赋值运算符
 * Division assignment operator for mutable quadratic polynomial
 *
 * @receiver 可变二次多项式 / Mutable quadratic polynomial
 * @param rhs 标量值 / Scalar value
*/
operator fun <T : NumberField<T>> MutableQuadraticPolynomial<T>.divAssign(rhs: T) {
    for (i in _monomials.indices) {
        _monomials[i] = _monomials[i] / rhs
    }
    _constant = _constant / rhs
}
