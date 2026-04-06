/**
 * 对数运算符
 * Logarithm Operator
 *
 * 定义对数运算相关接口，支持任意底数、常用对数、二进制对数和自然对数。
 * 对数是指数运算的逆运算，用于计算某数需要多少次幂才能得到另一个数。
 *
 * Defines logarithm operation interfaces supporting arbitrary bases, common logarithm (base 10),
 * binary logarithm (base 2), and natural logarithm (base e).
 * Logarithm is the inverse operation of exponentiation, used to compute the power needed
 * to obtain one number from another.
 *
 * 数学定义 / Mathematical definitions:
 * - logₐ(x): 以 a 为底的对数 / logarithm with base a
 * - lg(x) = log₁₀(x): 常用对数 / common logarithm
 * - lg2(x) = log₂(x): 二进制对数 / binary logarithm
 * - ln(x) = logₑ(x): 自然对数 / natural logarithm
 *
 * 接口说明 / Interface descriptions:
 * - Log: 基本对数运算接口
 * - LogFun: 对数运算函数扩展接口
 * - LogP: 带精度参数的对数运算接口
 * - LogFunP: 带精度参数的对数运算函数扩展接口
 */
package fuookami.ospf.kotlin.math.operator

interface Log<in Base, out Ret> {
    fun log(base: Base): Ret?
    fun lg(): Ret?
    fun lg2(): Ret?
    fun ln(): Ret?
}

interface LogFun<in Self, in Base, out Ret> {
    fun Self.log(base: Base): Ret
    fun Self.lg(): Ret?
    fun Self.lg2(): Ret?
    fun Self.ln(): Ret?
}

interface LogP<in Base, Ret> : Log<Base, Ret> {
    fun log(base: Base, digits: Int, precision: Ret): Ret? {
        return log(base)
    }

    fun lg(digits: Int, precision: Ret): Ret? {
        return lg()
    }

    fun lg2(digits: Int, precision: Ret): Ret? {
        return lg2()
    }

    fun ln(digits: Int, precision: Ret): Ret? {
        return ln()
    }
}

interface LogFunP<in Self, in Base, Ret> {
    fun Self.log(base: Base, digits: Int, precision: Ret): Ret?
    fun Self.lg(digits: Int, precision: Ret): Ret?
    fun Self.lg2(digits: Int, precision: Ret): Ret?
    fun Self.ln(digits: Int, precision: Ret): Ret?
}

fun <Base, Natural, Ret> log(
    base: Base,
    natural: Natural
): Ret? where Natural : Log<Base, Ret> {
    return natural.log(base)
}

fun <Base, Natural, Ret, Func> log(
    base: Base,
    natural: Natural,
    func: Func
): Ret? where Func : LogFun<Natural, Base, Ret> {
    return with(func) {
        natural.log(base)
    }
}

fun <Base, Natural, Ret> log(
    base: Base,
    natural: Natural,
    digits: Int,
    precision: Ret
): Ret? where Natural : LogP<Base, Ret> {
    return natural.log(
        base = base,
        digits = digits,
        precision = precision
    )
}

fun <Base, Natural, Ret, Func> log(
    base: Base,
    natural: Natural,
    digits: Int,
    precision: Ret,
    func: Func
): Ret? where Func : LogFunP<Natural, Base, Ret> {
    return with(func) {
        natural.log(
            base = base,
            digits = digits,
            precision = precision
        )
    }
}

fun <Base, Natural, Ret> lg(
    natural: Natural
): Ret? where Natural : Log<Base, Ret> {
    return natural.lg()
}

fun <Base, Natural, Ret, Func> lg(
    natural: Natural,
    func: Func
): Ret? where Func : LogFun<Natural, Base, Ret> {
    return with(func) {
        natural.lg()
    }
}

fun <Base, Natural, Ret> lg(
    natural: Natural,
    digits: Int,
    precision: Ret
): Ret? where Natural : LogP<Base, Ret> {
    return natural.lg(digits, precision)
}

fun <Base, Natural, Ret, Func> lg(
    natural: Natural,
    digits: Int,
    precision: Ret,
    func: Func
): Ret? where Func : LogFunP<Natural, Base, Ret> {
    return with(func) {
        natural.lg(digits, precision)
    }
}

fun <Base, Natural, Ret> lg2(
    natural: Natural
): Ret? where Natural : Log<Base, Ret> {
    return natural.lg2()
}

fun <Base, Natural, Ret, Func> lg2(
    natural: Natural,
    func: Func
): Ret? where Func : LogFun<Natural, Base, Ret> {
    return with(func) {
        natural.lg2()
    }
}

fun <Base, Natural, Ret> lg2(
    natural: Natural,
    digits: Int,
    precision: Ret
): Ret? where Natural : LogP<Base, Ret> {
    return natural.lg2(digits, precision)
}

fun <Base, Natural, Ret, Func> lg2(
    natural: Natural,
    digits: Int,
    precision: Ret,
    func: Func
): Ret? where Func : LogFunP<Natural, Base, Ret> {
    return with(func) {
        natural.lg2(digits, precision)
    }
}

fun <Base, Natural, Ret> ln(
    natural: Natural
): Ret? where Natural : Log<Base, Ret> {
    return natural.ln()
}

fun <Base, Natural, Ret, Func> ln(
    natural: Natural,
    func: Func
): Ret? where Func : LogFun<Natural, Base, Ret> {
    return with(func) {
        natural.ln()
    }
}

fun <Base, Natural, Ret> ln(
    natural: Natural,
    digits: Int,
    precision: Ret
): Ret? where Natural : LogP<Base, Ret> {
    return natural.ln(digits, precision)
}

fun <Base, Natural, Ret, Func> ln(
    natural: Natural,
    digits: Int,
    precision: Ret,
    func: Func
): Ret? where Func : LogFunP<Natural, Base, Ret> {
    return with(func) {
        natural.ln(digits, precision)
    }
}
