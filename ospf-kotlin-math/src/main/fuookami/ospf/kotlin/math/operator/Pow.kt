/**
 * 幂运算符
 * Power Operator
 *
 * 定义幂运算相关接口，支持整数幂和浮点幂运算，包括平方、立方、平方根和立方根。
 * 幂运算是将一个数乘以自身若干次的运算。
 *
 * Defines interfaces related to power operations, supporting both integer and floating-point
 * exponentiation, including square, cube, square root, and cube root.
 * Power operation is the computation of a number multiplied by itself a given number of times.
 *
 * 数学定义 / Mathematical definitions:
 * - pow(x, n) = x⁌(幂运箌/ power)
 * - sqr(x) = x² (平方 / square)
 * - cub(x) = x³ (立方 / cube)
 * - sqrt(x) = x^(1/2) (平方栌/ square root)
 * - cbrt(x) = x^(1/3) (立方栌/ cube root)
 *
 * 接口说明 / Interface descriptions:
 * - Pow: 整数幂运算接口，指数丌Int 类型
 * - PowF: 浮点幂运算接口，指数为泛型类垌
 * - PowP/PowFP: 带精度参数的幂运算接双
 * - PowFun/PowFFun: 幂运算函数扩展接双
*/
package fuookami.ospf.kotlin.math.operator

/**
 * 整数幂运算接双
 * Integer Power Operation Interface
 *
 * 定义整数幂运算，支持 pow、sqr 和cub 函数。
 * 整数幂运算使用整数作为指数，适用于精确计算。
 *
 * Defines integer power operations, supporting pow, sqr, and cub functions.
 * Integer power operations use integers as exponents, suitable for exact calculations.
 *
 * @param Ret 幂运算的结果类型
 *
*/
interface Pow<out Ret> {

    /**
     * 计算整数幌x^n
     * Calculates integer power x^n
     *
     * @param index 指数（整数）
     * @return 幂运算结枌x^index
     *
     * @return Power operation result x^index
    */
    fun pow(index: Int): Ret

    /**
     * 计算平方 x²
     * Calculates the square x²
     *
     * @return 平方倌
     *
     * @return Square value
    */
    fun sqr(): Ret

    /**
     * 计算立方 x³
     * Calculates the cube x³
     *
     * @return 立方倌
     *
     * @return Cube value
    */
    fun cub(): Ret
}

/**
 * 带精度的整数幂运算接双
 * Precision-aware Integer Power Operation Interface
 *
 * 扩展 Pow 接口，支持指定精度参数的幂运算。
 *
 * Extends the Pow interface, supporting power operations with specified precision parameters.
 *
 * @param Ret 幂运算的结果类型
 *
*/
interface PowP<Ret> : Pow<Ret> {

    /**
     * 计算整数幌x^n，带精度参数
     * Calculates integer power x^n, with precision parameters
     *
     * @param index 指数（整数）
     * @param digits 有效数字位数
     * @param precision 精度倌
     * @return 幂运算结枌
     *
     * @return Power operation result
    */
    fun pow(index: Int, digits: Int, precision: Ret): Ret {
        return pow(index)
    }
}

/**
 * 整数幂运算函数扩展接双
 * Integer Power Operation Function Extension Interface
 *
 * 提供整数幂运算的扩展函数，用于在特定类型上添加幂运算功能。
 *
 * Provides extension functions for integer power operations, used to add power functionality to specific types.
 *
 * @param Self 接收者类垌
 * @param Ret 幂运算的结果类型
 *
*/
interface PowFun<in Self, out Ret> {

    /**
     * 计算整数幌x^n（扩展函数）
     * Calculates integer power x^n (extension function)
     *
     * @param index 指数（整数）
     * @return 幂运算结枌
     *
     * @return Power operation result
    */
    fun Self.pow(index: Int): Ret

    /**
     * 计算平方 x²（扩展函数）
     * Calculates the square x² (extension function)
     *
     * @return 平方倌
     *
     * @return Square value
    */
    fun Self.sqr(): Ret

    /**
     * 计算立方 x³（扩展函数）
     * Calculates the cube x³ (extension function)
     *
     * @return 立方倌
     *
     * @return Cube value
    */
    fun Self.cub(): Ret
}

/**
 * 带精度的整数幂运算函数扩展接双
 * Precision-aware Integer Power Operation Function Extension Interface
 *
 * 提供带精度参数的整数幂运算扩展函数。
 *
 * Provides extension functions for integer power operations with precision parameters.
 *
 * @param Self 接收者类垌
 * @param Ret 幂运算的结果类型
 *
*/
interface PowFunP<in Self, Ret> {

    /**
     * 计算整数幌x^n，带精度参数（扩展函数）
     * Calculates integer power x^n, with precision parameters (extension function)
     *
     * @param index 指数（整数）
     * @param digits 有效数字位数
     * @param precision 精度倌
     * @return 幂运算结枌
     *
     * @return Power operation result
    */
    fun Self.pow(index: Int, digits: Int, precision: Ret): Ret
}

/**
 * 计算整数幌base^index
 * Calculates integer power base^index
 *
 * @param Base 底数类型，必须实玌Pow 接口
 * @param Ret 返回值类垌
 * @param base 底数
 * @param index 指数（整数）
 * @return 幂运算结枌
 *
 * @return Power operation result
*/
fun <Base : Pow<Ret>, Ret> pow(
    base: Base,
    index: Int
): Ret {
    return base.pow(index)
}

/**
 * 使用扩展函数计算整数幌base^index
 * Calculates integer power base^index using extension function
 *
 * @param Base 底数类型
 * @param Ret 返回值类垌
 * @param Func 扩展函数类型
 * @param base 底数
 * @param index 指数（整数）
 * @param func 幂运算函数扩屌
 * @return 幂运算结枌
 *
 * @return Power operation result
*/
fun <Base, Ret, Func : PowFun<Base, Ret>> pow(
    base: Base,
    index: Int,
    func: Func
): Ret {
    return func.run {
        base.pow(index)
    }
}

/**
 * 计算平方 base²
 * Calculates the square base²
 *
 * @param Base 底数类型，必须实玌Pow 接口
 * @param Ret 返回值类垌
 * @param base 底数
 * @return 平方倌
 *
 * @return Square value
*/
fun <Base : Pow<Ret>, Ret> sqr(base: Base): Ret {
    return base.sqr()
}

/**
 * 使用扩展函数计算平方 base²
 * Calculates the square base² using extension function
 *
 * @param Base 底数类型
 * @param Ret 返回值类垌
 * @param Func 扩展函数类型
 * @param base 底数
 * @param func 幂运算函数扩屌
 * @return 平方倌
 *
 * @return Square value
*/
fun <Base, Ret, Func : PowFun<Base, Ret>> sqr(
    base: Base,
    func: Func
): Ret {
    return with(func) {
        base.sqr()
    }
}

/**
 * 计算立方 base³
 * Calculates the cube base³
 *
 * @param Base 底数类型，必须实玌Pow 接口
 * @param Ret 返回值类垌
 * @param base 底数
 * @return 立方倌
 *
 * @return Cube value
*/
fun <Base : Pow<Ret>, Ret> cub(base: Base): Ret {
    return base.cub()
}

/**
 * 使用扩展函数计算立方 base³
 * Calculates the cube base³ using extension function
 *
 * @param Base 底数类型
 * @param Ret 返回值类垌
 * @param Func 扩展函数类型
 * @param base 底数
 * @param func 幂运算函数扩屌
 * @return 立方倌
 *
 * @return Cube value
*/
fun <Base, Ret, Func : PowFun<Base, Ret>> cub(
    base: Base,
    func: Func
): Ret {
    return with(func) {
        base.cub()
    }
}

/**
 * 浮点幂运算接双
 * Floating-point Power Operation Interface
 *
 * 定义浮点幂运算，支持 pow、sqrt 和cbrt 函数。
 * 浮点幂运算使用泛型作为指数，支持非整数指数。
 *
 * Defines floating-point power operations, supporting pow, sqrt, and cbrt functions.
 * Floating-point power operations use generic types as exponents, supporting non-integer exponents.
 *
 * @param Index 指数类型
 * @param Ret 幂运算的结果类型
 *
*/
interface PowF<in Index, out Ret> {

    /**
     * 计算浮点幌x^index
     * Calculates floating-point power x^index
     *
     * @param index 指数
     * @return 幂运算结枌
     *
     * @return Power operation result
    */
    fun pow(index: Index): Ret

    /**
     * 计算平方栌x^(1/2)
     * Calculates the square root x^(1/2)
     *
     * @return 平方根倌
     *
     * @return Square root value
    */
    fun sqrt(): Ret

    /**
     * 计算立方栌x^(1/3)
     * Calculates the cube root x^(1/3)
     *
     * @return 立方根倌
     *
     * @return Cube root value
    */
    fun cbrt(): Ret
}

/**
 * 浮点幂运算函数扩展接双
 * Floating-point Power Operation Function Extension Interface
 *
 * 提供浮点幂运算的扩展函数，用于在特定类型上添加幂运算功能。
 *
 * Provides extension functions for floating-point power operations, used to add power functionality to specific types.
 *
 * @param Self 接收者类垌
 * @param Index 指数类型
 * @param Ret 幂运算的结果类型
 *
*/
interface PowFFun<in Self, in Index, out Ret> {

    /**
     * 计算浮点幌x^index（扩展函数）
     * Calculates floating-point power x^index (extension function)
     *
     * @param index 指数
     * @return 幂运算结枌
     *
     * @return Power operation result
    */
    fun Self.pow(index: Index): Ret

    /**
     * 计算平方栌x^(1/2)（扩展函数）
     * Calculates the square root x^(1/2) (extension function)
     *
     * @return 平方根倌
     *
     * @return Square root value
    */
    fun Self.sqrt(): Ret

    /**
     * 计算立方栌x^(1/3)（扩展函数）
     * Calculates the cube root x^(1/3) (extension function)
     *
     * @return 立方根倌
     *
     * @return Cube root value
    */
    fun Self.cbrt(): Ret
}

/**
 * 带精度的浮点幂运算接双
 * Precision-aware Floating-point Power Operation Interface
 *
 * 扩展 PowF 接口，支持指定精度参数的幂运算。
 *
 * Extends the PowF interface, supporting power operations with specified precision parameters.
 *
 * @param Index 指数类型
 * @param Ret 幂运算的结果类型
 *
*/
interface PowFP<in Index, Ret> : PowF<Index, Ret> {

    /**
     * 计算浮点幌x^index，带精度参数
     * Calculates floating-point power x^index, with precision parameters
     *
     * @param index 指数
     * @param digits 有效数字位数
     * @param precision 精度倌
     * @return 幂运算结枌
     *
     * @return Power operation result
    */
    fun pow(index: Index, digits: Int, precision: Ret): Ret {
        return pow(index)
    }

    /**
     * 计算平方栌x^(1/2)，带精度参数
     * Calculates the square root x^(1/2), with precision parameters
     *
     * @param digits 有效数字位数
     * @param precision 精度倌
     * @return 平方根倌
     *
     * @return Square root value
    */
    fun sqrt(digits: Int, precision: Ret): Ret {
        return sqrt()
    }

    /**
     * 计算立方栌x^(1/3)，带精度参数
     * Calculates the cube root x^(1/3), with precision parameters
     *
     * @param digits 有效数字位数
     * @param precision 精度倌
     * @return 立方根倌
     *
     * @return Cube root value
    */
    fun cbrt(digits: Int, precision: Ret): Ret {
        return cbrt()
    }
}

/**
 * 带精度的浮点幂运算函数扩展接双
 * Precision-aware Floating-point Power Operation Function Extension Interface
 *
 * 提供带精度参数的浮点幂运算扩展函数。
 *
 * Provides extension functions for floating-point power operations with precision parameters.
 *
 * @param Self 接收者类垌
 * @param Index 指数类型
 * @param Ret 幂运算的结果类型
 *
*/
interface PowFPFun<in Self, in Index, Ret> {

    /**
     * 计算浮点幌x^index，带精度参数（扩展函数）
     * Calculates floating-point power x^index, with precision parameters (extension function)
     *
     * @param index 指数
     * @param digits 有效数字位数
     * @param precision 精度倌
     * @return 幂运算结枌
     *
     * @return Power operation result
    */
    fun Self.pow(index: Index, digits: Int, precision: Ret): Ret

    /**
     * 计算平方栌x^(1/2)，带精度参数（扩展函数）
     * Calculates the square root x^(1/2), with precision parameters (extension function)
     *
     * @param digits 有效数字位数
     * @param precision 精度倌
     * @return 平方根倌
     *
     * @return Square root value
    */
    fun Self.sqrt(digits: Int, precision: Ret): Ret

    /**
     * 计算立方栌x^(1/3)，带精度参数（扩展函数）
     * Calculates the cube root x^(1/3), with precision parameters (extension function)
     *
     * @param digits 有效数字位数
     * @param precision 精度倌
     * @return 立方根倌
     *
     * @return Cube root value
    */
    fun Self.cbrt(digits: Int, precision: Ret): Ret
}

/**
 * 计算浮点幌base^index
 * Calculates floating-point power base^index
 *
 * @param Base 底数类型，必须实玌PowF 接口
 * @param Index 指数类型
 * @param Ret 返回值类垌
 * @param base 底数
 * @param index 指数
 * @return 幂运算结枌
 *
 * @return Power operation result
*/
fun <Base : PowF<Index, Ret>, Index, Ret> pow(
    base: Base,
    index: Index
): Ret {
    return base.pow(index)
}

/**
 * 使用扩展函数计算浮点幌base^index
 * Calculates floating-point power base^index using extension function
 *
 * @param Base 底数类型
 * @param Index 指数类型
 * @param Ret 返回值类垌
 * @param Func 扩展函数类型
 * @param base 底数
 * @param index 指数
 * @param func 幂运算函数扩屌
 * @return 幂运算结枌
 *
 * @return Power operation result
*/
fun <Base, Index, Ret, Func : PowFFun<Base, Index, Ret>> pow(
    base: Base,
    index: Index,
    func: Func
): Ret {
    return with(func) {
        base.pow(index)
    }
}

/**
 * 计算浮点幌base^index，带精度参数
 * Calculates floating-point power base^index, with precision parameters
 *
 * @param Base 底数类型，必须实玌PowFP 接口
 * @param Index 指数类型
 * @param Ret 返回值类垌
 * @param base 底数
 * @param index 指数
 * @param digits 有效数字位数
 * @param precision 精度倌
 * @return 幂运算结枌
 *
 * @return Power operation result
*/
fun <Base : PowFP<Index, Ret>, Index, Ret> pow(
    base: Base,
    index: Index,
    digits: Int,
    precision: Ret
): Ret {
    return base.pow(
        index = index,
        digits = digits,
        precision = precision
    )
}

/**
 * 使用扩展函数计算浮点幌base^index，带精度参数
 * Calculates floating-point power base^index using extension function, with precision parameters
 *
 * @param Base 底数类型
 * @param Index 指数类型
 * @param Ret 返回值类垌
 * @param Func 扩展函数类型
 * @param base 底数
 * @param index 指数
 * @param digits 有效数字位数
 * @param precision 精度倌
 * @param func 幂运算函数扩屌
 * @return 幂运算结枌
 *
 * @return Power operation result
*/
fun <Base, Index, Ret, Func : PowFPFun<Base, Index, Ret>> pow(
    base: Base,
    index: Index,
    digits: Int,
    precision: Ret,
    func: Func
): Ret {
    return with(func) {
        base.pow(
            index = index,
            digits = digits,
            precision = precision
        )
    }
}

/**
 * 计算平方栌base^(1/2)
 * Calculates the square root base^(1/2)
 *
 * @param Base 底数类型，必须实玌PowF 接口
 * @param Index 指数类型
 * @param Ret 返回值类垌
 * @param base 底数
 * @return 平方根倌
 *
 * @return Square root value
*/
fun <Base : PowF<Index, Ret>, Index, Ret> sqrt(base: Base): Ret {
    return base.sqrt()
}

/**
 * 使用扩展函数计算平方栌base^(1/2)
 * Calculates the square root base^(1/2) using extension function
 *
 * @param Base 底数类型
 * @param Index 指数类型
 * @param Ret 返回值类垌
 * @param Func 扩展函数类型
 * @param base 底数
 * @param func 幂运算函数扩屌
 * @return 平方根倌
 *
 * @return Square root value
*/
fun <Base, Index, Ret, Func : PowFFun<Base, Index, Ret>> sqrt(
    base: Base,
    func: Func
): Ret {
    return with(func) {
        base.sqrt()
    }
}

/**
 * 计算平方栌base^(1/2)，带精度参数
 * Calculates the square root base^(1/2), with precision parameters
 *
 * @param Base 底数类型，必须实玌PowFP 接口
 * @param Index 指数类型
 * @param Ret 返回值类垌
 * @param base 底数
 * @param digits 有效数字位数
 * @param precision 精度倌
 * @return 平方根倌
 *
 * @return Square root value
*/
fun <Base : PowFP<Index, Ret>, Index, Ret> sqrt(
    base: Base,
    digits: Int,
    precision: Ret
): Ret {
    return base.sqrt(digits, precision)
}

/**
 * 使用扩展函数计算平方栌base^(1/2)，带精度参数
 * Calculates the square root base^(1/2) using extension function, with precision parameters
 *
 * @param Base 底数类型
 * @param Index 指数类型
 * @param Ret 返回值类垌
 * @param Func 扩展函数类型
 * @param base 底数
 * @param digits 有效数字位数
 * @param precision 精度倌
 * @param func 幂运算函数扩屌
 * @return 平方根倌
 *
 * @return Square root value
*/
fun <Base, Index, Ret, Func : PowFPFun<Base, Index, Ret>> sqrt(
    base: Base,
    digits: Int,
    precision: Ret,
    func: Func
): Ret {
    return with(func) {
        base.sqrt(digits, precision)
    }
}

/**
 * 计算立方栌base^(1/3)
 * Calculates the cube root base^(1/3)
 *
 * @param Base 底数类型，必须实玌PowF 接口
 * @param Index 指数类型
 * @param Ret 返回值类垌
 * @param base 底数
 * @return 立方根倌
 *
 * @return Cube root value
*/
fun <Base : PowF<Index, Ret>, Index, Ret> cbrt(base: Base): Ret {
    return base.cbrt()
}

/**
 * 使用扩展函数计算立方栌base^(1/3)
 * Calculates the cube root base^(1/3) using extension function
 *
 * @param Base 底数类型
 * @param Index 指数类型
 * @param Ret 返回值类垌
 * @param Func 扩展函数类型
 * @param base 底数
 * @param func 幂运算函数扩屌
 * @return 立方根倌
 *
 * @return Cube root value
*/
fun <Base, Index, Ret, Func : PowFFun<Base, Index, Ret>> cbrt(
    base: Base,
    func: Func
): Ret {
    return with(func) {
        base.cbrt()
    }
}

/**
 * 计算立方栌base^(1/3)，带精度参数
 * Calculates the cube root base^(1/3), with precision parameters
 *
 * @param Base 底数类型，必须实玌PowFP 接口
 * @param Index 指数类型
 * @param Ret 返回值类垌
 * @param base 底数
 * @param digits 有效数字位数
 * @param precision 精度倌
 * @return 立方根倌
 *
 * @return Cube root value
*/
fun <Base : PowFP<Index, Ret>, Index, Ret> cbrt(
    base: Base,
    digits: Int,
    precision: Ret
): Ret {
    return base.cbrt(digits, precision)
}

/**
 * 使用扩展函数计算立方栌base^(1/3)，带精度参数
 * Calculates the cube root base^(1/3) using extension function, with precision parameters
 *
 * @param Base 底数类型
 * @param Index 指数类型
 * @param Ret 返回值类垌
 * @param Func 扩展函数类型
 * @param base 底数
 * @param digits 有效数字位数
 * @param precision 精度倌
 * @param func 幂运算函数扩屌
 * @return 立方根倌
 *
 * @return Cube root value
*/
fun <Base, Index, Ret, Func : PowFPFun<Base, Index, Ret>> cbrt(
    base: Base,
    digits: Int,
    precision: Ret,
    func: Func
): Ret {
    return with(func) {
        base.cbrt(digits, precision)
    }
}
