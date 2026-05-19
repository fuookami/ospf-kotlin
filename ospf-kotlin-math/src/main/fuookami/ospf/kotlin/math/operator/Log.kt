/**
 * 对数运算笌
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
 * - log₌x): 仌a 为底的对敌/ logarithm with base a
 * - lg(x) = log₁₀(x): 常用对数 / common logarithm
 * - lg2(x) = log₌x): 二进制对敌/ binary logarithm
 * - ln(x) = log₌x): 自然对数 / natural logarithm
 *
 * 接口说明 / Interface descriptions:
 * - Log: 基本对数运算接口
 * - LogFun: 对数运算函数扩展接口
 * - LogP: 带精度参数的对数运算接口
 * - LogFunP: 带精度参数的对数运算函数扩展接口
 */
package fuookami.ospf.kotlin.math.operator

/**
 * 对数运算接口
 * Logarithm Operation Interface
 *
 * 定义对数运算，支持任意底数、常用对数、二进制对数和自然对数。
 * 返回值可能为 null，表示运算无定义（如对负数或零取对数）。
 *
 * Defines logarithm operations, supporting arbitrary bases, common logarithm, binary logarithm, and natural logarithm.
 * Return value may be null, indicating the operation is undefined (e.g., logarithm of negative or zero).
 *
 * @param Base 对数底数的类垌
 * @param Ret 对数运算的结果类垌
 *
 * @param Base The type of the logarithm base
 * @param Ret The result type of the logarithm operation
 */
interface Log<in Base, out Ret> {
    /**
     * 计算以指定底数的对数
     * Calculates the logarithm with specified base
     *
     * @param base 对数底数
     * @return 对数值，如果运算无定义则返回 null
     *
     * @param base The logarithm base
     * @return The logarithm value, or null if the operation is undefined
     */
    fun log(base: Base): Ret?

    /**
     * 计算常用对数（以 10 为底，
     * Calculates the common logarithm (base 10)
     *
     * @return 常用对数值，如果运算无定义则返回 null
     *
     * @return The common logarithm value, or null if the operation is undefined
     */
    fun lg(): Ret?

    /**
     * 计算二进制对数（仌2 为底，
     * Calculates the binary logarithm (base 2)
     *
     * @return 二进制对数值，如果运算无定义则返回 null
     *
     * @return The binary logarithm value, or null if the operation is undefined
     */
    fun lg2(): Ret?

    /**
     * 计算自然对数（以 e 为底，
     * Calculates the natural logarithm (base e)
     *
     * @return 自然对数值，如果运算无定义则返回 null
     *
     * @return The natural logarithm value, or null if the operation is undefined
     */
    fun ln(): Ret?
}

/**
 * 对数运算函数扩展接口
 * Logarithm Operation Function Extension Interface
 *
 * 提供对数运算的扩展函数，用于在特定类型上添加对数运算功能。
 *
 * Provides extension functions for logarithm operations, used to add logarithm functionality to specific types.
 *
 * @param Self 接收者类垌
 * @param Base 对数底数的类垌
 * @param Ret 对数运算的结果类垌
 *
 * @param Self The receiver type
 * @param Base The type of the logarithm base
 * @param Ret The result type of the logarithm operation
 */
interface LogFun<in Self, in Base, out Ret> {
    /**
     * 计算以指定底数的对数（扩展函数）
     * Calculates the logarithm with specified base (extension function)
     *
     * @param base 对数底数
     * @return 对数倌
     *
     * @param base The logarithm base
     * @return The logarithm value
     */
    fun Self.log(base: Base): Ret

    /**
     * 计算常用对数（扩展函数）
     * Calculates the common logarithm (extension function)
     *
     * @return 常用对数值，如果运算无定义则返回 null
     *
     * @return The common logarithm value, or null if the operation is undefined
     */
    fun Self.lg(): Ret?

    /**
     * 计算二进制对数（扩展函数，
     * Calculates the binary logarithm (extension function)
     *
     * @return 二进制对数值，如果运算无定义则返回 null
     *
     * @return The binary logarithm value, or null if the operation is undefined
     */
    fun Self.lg2(): Ret?

    /**
     * 计算自然对数（扩展函数）
     * Calculates the natural logarithm (extension function)
     *
     * @return 自然对数值，如果运算无定义则返回 null
     *
     * @return The natural logarithm value, or null if the operation is undefined
     */
    fun Self.ln(): Ret?
}

/**
 * 带精度的对数运算接口
 * Precision-aware Logarithm Operation Interface
 *
 * 扩展 Log 接口，支持指定精度参数的对数运算。
 *
 * Extends the Log interface, supporting logarithm operations with specified precision parameters.
 *
 * @param Base 对数底数的类垌
 * @param Ret 对数运算的结果类垌
 *
 * @param Base The type of the logarithm base
 * @param Ret The result type of the logarithm operation
 */
interface LogP<in Base, Ret> : Log<Base, Ret> {
    /**
     * 计算以指定底数的对数，带精度参数
     * Calculates the logarithm with specified base, with precision parameters
     *
     * @param base 对数底数
     * @param digits 有效数字位数
     * @param precision 精度倌
     * @return 对数值，如果运算无定义则返回 null
     *
     * @param base The logarithm base
     * @param digits Number of significant digits
     * @param precision Precision value
     * @return The logarithm value, or null if the operation is undefined
     */
    fun log(base: Base, digits: Int, precision: Ret): Ret? {
        return log(base)
    }

    /**
     * 计算常用对数，带精度参数
     * Calculates the common logarithm, with precision parameters
     *
     * @param digits 有效数字位数
     * @param precision 精度倌
     * @return 常用对数值，如果运算无定义则返回 null
     *
     * @param digits Number of significant digits
     * @param precision Precision value
     * @return The common logarithm value, or null if the operation is undefined
     */
    fun lg(digits: Int, precision: Ret): Ret? {
        return lg()
    }

    /**
     * 计算二进制对数，带精度参敌
     * Calculates the binary logarithm, with precision parameters
     *
     * @param digits 有效数字位数
     * @param precision 精度倌
     * @return 二进制对数值，如果运算无定义则返回 null
     *
     * @param digits Number of significant digits
     * @param precision Precision value
     * @return The binary logarithm value, or null if the operation is undefined
     */
    fun lg2(digits: Int, precision: Ret): Ret? {
        return lg2()
    }

    /**
     * 计算自然对数，带精度参数
     * Calculates the natural logarithm, with precision parameters
     *
     * @param digits 有效数字位数
     * @param precision 精度倌
     * @return 自然对数值，如果运算无定义则返回 null
     *
     * @param digits Number of significant digits
     * @param precision Precision value
     * @return The natural logarithm value, or null if the operation is undefined
     */
    fun ln(digits: Int, precision: Ret): Ret? {
        return ln()
    }
}

/**
 * 带精度的对数运算函数扩展接口
 * Precision-aware Logarithm Operation Function Extension Interface
 *
 * 提供带精度参数的对数运算扩展函数。
 *
 * Provides extension functions for logarithm operations with precision parameters.
 *
 * @param Self 接收者类垌
 * @param Base 对数底数的类垌
 * @param Ret 对数运算的结果类垌
 *
 * @param Self The receiver type
 * @param Base The type of the logarithm base
 * @param Ret The result type of the logarithm operation
 */
interface LogFunP<in Self, in Base, Ret> {
    /**
     * 计算以指定底数的对数，带精度参数（扩展函数）
     * Calculates the logarithm with specified base, with precision parameters (extension function)
     *
     * @param base 对数底数
     * @param digits 有效数字位数
     * @param precision 精度倌
     * @return 对数值，如果运算无定义则返回 null
     *
     * @param base The logarithm base
     * @param digits Number of significant digits
     * @param precision Precision value
     * @return The logarithm value, or null if the operation is undefined
     */
    fun Self.log(base: Base, digits: Int, precision: Ret): Ret?

    /**
     * 计算常用对数，带精度参数（扩展函数）
     * Calculates the common logarithm, with precision parameters (extension function)
     *
     * @param digits 有效数字位数
     * @param precision 精度倌
     * @return 常用对数值，如果运算无定义则返回 null
     *
     * @param digits Number of significant digits
     * @param precision Precision value
     * @return The common logarithm value, or null if the operation is undefined
     */
    fun Self.lg(digits: Int, precision: Ret): Ret?

    /**
     * 计算二进制对数，带精度参数（扩展函数，
     * Calculates the binary logarithm, with precision parameters (extension function)
     *
     * @param digits 有效数字位数
     * @param precision 精度倌
     * @return 二进制对数值，如果运算无定义则返回 null
     *
     * @param digits Number of significant digits
     * @param precision Precision value
     * @return The binary logarithm value, or null if the operation is undefined
     */
    fun Self.lg2(digits: Int, precision: Ret): Ret?

    /**
     * 计算自然对数，带精度参数（扩展函数）
     * Calculates the natural logarithm, with precision parameters (extension function)
     *
     * @param digits 有效数字位数
     * @param precision 精度倌
     * @return 自然对数值，如果运算无定义则返回 null
     *
     * @param digits Number of significant digits
     * @param precision Precision value
     * @return The natural logarithm value, or null if the operation is undefined
     */
    fun Self.ln(digits: Int, precision: Ret): Ret?
}

/**
 * 计算以指定底数的对数
 * Calculates the logarithm with specified base
 *
 * @param Base 底数类型
 * @param Natural 操作数类型，必须实现 Log 接口
 * @param Ret 返回值类垌
 * @param base 对数底数
 * @param natural 对数的真敌
 * @return 对数值，如果运算无定义则返回 null
 *
 * @param Base The base type
 * @param Natural The operand type, must implement the Log interface
 * @param Ret The return type
 * @param base The logarithm base
 * @param natural The antilogarithm
 * @return The logarithm value, or null if the operation is undefined
 */
fun <Base, Natural, Ret> log(
    base: Base,
    natural: Natural
): Ret? where Natural : Log<Base, Ret> {
    return natural.log(base)
}

/**
 * 使用扩展函数计算以指定底数的对数
 * Calculates the logarithm with specified base using extension function
 *
 * @param Base 底数类型
 * @param Natural 操作数类垌
 * @param Ret 返回值类垌
 * @param Func 扩展函数类型
 * @param base 对数底数
 * @param natural 对数的真敌
 * @param func 对数运算函数扩展
 * @return 对数倌
 *
 * @param Base The base type
 * @param Natural The operand type
 * @param Ret The return type
 * @param Func The extension function type
 * @param base The logarithm base
 * @param natural The antilogarithm
 * @param func The logarithm operation function extension
 * @return The logarithm value
 */
fun <Base, Natural, Ret, Func> log(
    base: Base,
    natural: Natural,
    func: Func
): Ret? where Func : LogFun<Natural, Base, Ret> {
    return with(func) {
        natural.log(base)
    }
}

/**
 * 计算以指定底数的对数，带精度参数
 * Calculates the logarithm with specified base, with precision parameters
 *
 * @param Base 底数类型
 * @param Natural 操作数类型，必须实现 LogP 接口
 * @param Ret 返回值类垌
 * @param base 对数底数
 * @param natural 对数的真敌
 * @param digits 有效数字位数
 * @param precision 精度倌
 * @return 对数值，如果运算无定义则返回 null
 *
 * @param Base The base type
 * @param Natural The operand type, must implement the LogP interface
 * @param Ret The return type
 * @param base The logarithm base
 * @param natural The antilogarithm
 * @param digits Number of significant digits
 * @param precision Precision value
 * @return The logarithm value, or null if the operation is undefined
 */
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

/**
 * 使用扩展函数计算以指定底数的对数，带精度参数
 * Calculates the logarithm with specified base using extension function, with precision parameters
 *
 * @param Base 底数类型
 * @param Natural 操作数类垌
 * @param Ret 返回值类垌
 * @param Func 扩展函数类型
 * @param base 对数底数
 * @param natural 对数的真敌
 * @param digits 有效数字位数
 * @param precision 精度倌
 * @param func 对数运算函数扩展
 * @return 对数值，如果运算无定义则返回 null
 *
 * @param Base The base type
 * @param Natural The operand type
 * @param Ret The return type
 * @param Func The extension function type
 * @param base The logarithm base
 * @param natural The antilogarithm
 * @param digits Number of significant digits
 * @param precision Precision value
 * @param func The logarithm operation function extension
 * @return The logarithm value, or null if the operation is undefined
 */
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

/**
 * 计算常用对数（以 10 为底，
 * Calculates the common logarithm (base 10)
 *
 * @param Base 底数类型
 * @param Natural 操作数类型，必须实现 Log 接口
 * @param Ret 返回值类垌
 * @param natural 对数的真敌
 * @return 常用对数值，如果运算无定义则返回 null
 *
 * @param Base The base type
 * @param Natural The operand type, must implement the Log interface
 * @param Ret The return type
 * @param natural The antilogarithm
 * @return The common logarithm value, or null if the operation is undefined
 */
fun <Base, Natural, Ret> lg(
    natural: Natural
): Ret? where Natural : Log<Base, Ret> {
    return natural.lg()
}

/**
 * 使用扩展函数计算常用对数
 * Calculates the common logarithm using extension function
 *
 * @param Base 底数类型
 * @param Natural 操作数类垌
 * @param Ret 返回值类垌
 * @param Func 扩展函数类型
 * @param natural 对数的真敌
 * @param func 对数运算函数扩展
 * @return 常用对数值，如果运算无定义则返回 null
 *
 * @param Base The base type
 * @param Natural The operand type
 * @param Ret The return type
 * @param Func The extension function type
 * @param natural The antilogarithm
 * @param func The logarithm operation function extension
 * @return The common logarithm value, or null if the operation is undefined
 */
fun <Base, Natural, Ret, Func> lg(
    natural: Natural,
    func: Func
): Ret? where Func : LogFun<Natural, Base, Ret> {
    return with(func) {
        natural.lg()
    }
}

/**
 * 计算常用对数，带精度参数
 * Calculates the common logarithm, with precision parameters
 *
 * @param Base 底数类型
 * @param Natural 操作数类型，必须实现 LogP 接口
 * @param Ret 返回值类垌
 * @param natural 对数的真敌
 * @param digits 有效数字位数
 * @param precision 精度倌
 * @return 常用对数值，如果运算无定义则返回 null
 *
 * @param Base The base type
 * @param Natural The operand type, must implement the LogP interface
 * @param Ret The return type
 * @param natural The antilogarithm
 * @param digits Number of significant digits
 * @param precision Precision value
 * @return The common logarithm value, or null if the operation is undefined
 */
fun <Base, Natural, Ret> lg(
    natural: Natural,
    digits: Int,
    precision: Ret
): Ret? where Natural : LogP<Base, Ret> {
    return natural.lg(digits, precision)
}

/**
 * 使用扩展函数计算常用对数，带精度参数
 * Calculates the common logarithm using extension function, with precision parameters
 *
 * @param Base 底数类型
 * @param Natural 操作数类垌
 * @param Ret 返回值类垌
 * @param Func 扩展函数类型
 * @param natural 对数的真敌
 * @param digits 有效数字位数
 * @param precision 精度倌
 * @param func 对数运算函数扩展
 * @return 常用对数值，如果运算无定义则返回 null
 *
 * @param Base The base type
 * @param Natural The operand type
 * @param Ret The return type
 * @param Func The extension function type
 * @param natural The antilogarithm
 * @param digits Number of significant digits
 * @param precision Precision value
 * @param func The logarithm operation function extension
 * @return The common logarithm value, or null if the operation is undefined
 */
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

/**
 * 计算二进制对数（仌2 为底，
 * Calculates the binary logarithm (base 2)
 *
 * @param Base 底数类型
 * @param Natural 操作数类型，必须实现 Log 接口
 * @param Ret 返回值类垌
 * @param natural 对数的真敌
 * @return 二进制对数值，如果运算无定义则返回 null
 *
 * @param Base The base type
 * @param Natural The operand type, must implement the Log interface
 * @param Ret The return type
 * @param natural The antilogarithm
 * @return The binary logarithm value, or null if the operation is undefined
 */
fun <Base, Natural, Ret> lg2(
    natural: Natural
): Ret? where Natural : Log<Base, Ret> {
    return natural.lg2()
}

/**
 * 使用扩展函数计算二进制对敌
 * Calculates the binary logarithm using extension function
 *
 * @param Base 底数类型
 * @param Natural 操作数类垌
 * @param Ret 返回值类垌
 * @param Func 扩展函数类型
 * @param natural 对数的真敌
 * @param func 对数运算函数扩展
 * @return 二进制对数值，如果运算无定义则返回 null
 *
 * @param Base The base type
 * @param Natural The operand type
 * @param Ret The return type
 * @param Func The extension function type
 * @param natural The antilogarithm
 * @param func The logarithm operation function extension
 * @return The binary logarithm value, or null if the operation is undefined
 */
fun <Base, Natural, Ret, Func> lg2(
    natural: Natural,
    func: Func
): Ret? where Func : LogFun<Natural, Base, Ret> {
    return with(func) {
        natural.lg2()
    }
}

/**
 * 计算二进制对数，带精度参敌
 * Calculates the binary logarithm, with precision parameters
 *
 * @param Base 底数类型
 * @param Natural 操作数类型，必须实现 LogP 接口
 * @param Ret 返回值类垌
 * @param natural 对数的真敌
 * @param digits 有效数字位数
 * @param precision 度倌
 * @return 二进制对数值，如果运算无定义则返回 null
 *
 * @param Base The base type
 * @param Natural The operand type, must implement the LogP interface
 * @param Ret The return type
 * @param natural The antilogarithm
 * @param digits Number of significant digits
 * @param precision Precision value
 * @return The binary logarithm value, or null if the operation is undefined
 */
fun <Base, Natural, Ret> lg2(
    natural: Natural,
    digits: Int,
    precision: Ret
): Ret? where Natural : LogP<Base, Ret> {
    return natural.lg2(digits, precision)
}

/**
 * 使用扩展函数计算二进制对数，带精度参敌
 * Calculates the binary logarithm using extension function, with precision parameters
 *
 * @param Base 底数类型
 * @param Natural 操作数类垌
 * @param Ret 返回值类垌
 * @param Func 扩展函数类型
 * @param natural 对数的真敌
 * @param digits 有效数字位数
 * @param precision 精度倌
 * @param func 对数运算函数扩展
 * @return 二进制对数值，如果运算无定义则返回 null
 *
 * @param Base The base type
 * @param Natural The operand type
 * @param Ret The return type
 * @param Func The extension function type
 * @param natural The antilogarithm
 * @param digits Number of significant digits
 * @param precision Precision value
 * @param func The logarithm operation function extension
 * @return The binary logarithm value, or null if the operation is undefined
 */
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

/**
 * 计算自然对数（以 e 为底，
 * Calculates the natural logarithm (base e)
 *
 * @param Base 底数类型
 * @param Natural 操作数类型，必须实现 Log 接口
 * @param Ret 返回值类垌
 * @param natural 对数的真敌
 * @return 自然对数值，如果运算无定义则返回 null
 *
 * @param Base The base type
 * @param Natural The operand type, must implement the Log interface
 * @param Ret The return type
 * @param natural The antilogarithm
 * @return The natural logarithm value, or null if the operation is undefined
 */
fun <Base, Natural, Ret> ln(
    natural: Natural
): Ret? where Natural : Log<Base, Ret> {
    return natural.ln()
}

/**
 * 使用扩展函数计算自然对数
 * Calculates the natural logarithm using extension function
 *
 * @param Base 底数类型
 * @param Natural 操作数类垌
 * @param Ret 返回值类垌
 * @param Func 扩展函数类型
 * @param natural 对数的真敌
 * @param func 对数运算函数扩展
 * @return 自然对数值，如果运算无定义则返回 null
 *
 * @param Base The base type
 * @param Natural The operand type
 * @param Ret The return type
 * @param Func The extension function type
 * @param natural The antilogarithm
 * @param func The logarithm operation function extension
 * @return The natural logarithm value, or null if the operation is undefined
 */
fun <Base, Natural, Ret, Func> ln(
    natural: Natural,
    func: Func
): Ret? where Func : LogFun<Natural, Base, Ret> {
    return with(func) {
        natural.ln()
    }
}

/**
 * 计算自然对数，带精度参数
 * Calculates the natural logarithm, with precision parameters
 *
 * @param Base 底数类型
 * @param Natural 操作数类型，必须实现 LogP 接口
 * @param Ret 返回值类垌
 * @param natural 对数的真敌
 * @param digits 有效数字位数
 * @param precision 精度倌
 * @return 自然对数值，如果运算无定义则返回 null
 *
 * @param Base The base type
 * @param Natural The operand type, must implement the LogP interface
 * @param Ret The return type
 * @param natural The antilogarithm
 * @param digits Number of significant digits
 * @param precision Precision value
 * @return The natural logarithm value, or null if the operation is undefined
 */
fun <Base, Natural, Ret> ln(
    natural: Natural,
    digits: Int,
    precision: Ret
): Ret? where Natural : LogP<Base, Ret> {
    return natural.ln(digits, precision)
}

/**
 * 使用扩展函数计算自然对数，带精度参数
 * Calculates the natural logarithm using extension function, with precision parameters
 *
 * @param Base 底数类型
 * @param Natural 操作数类垌
 * @param Ret 返回值类垌
 * @param Func 扩展函数类型
 * @param natural 对数的真敌
 * @param digits 有效数字位数
 * @param precision 精度倌
 * @param func 对数运算函数扩展
 * @return 自然对数值，如果运算无定义则返回 null
 *
 * @param Base The base type
 * @param Natural The operand type
 * @param Ret The return type
 * @param Func The extension function type
 * @param natural The antilogarithm
 * @param digits Number of significant digits
 * @param precision Precision value
 * @param func The logarithm operation function extension
 * @return The natural logarithm value, or null if the operation is undefined
 */
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