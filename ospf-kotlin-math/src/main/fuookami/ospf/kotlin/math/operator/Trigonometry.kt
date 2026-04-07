/**
 * 三角函数
 * Trigonometric Functions
 *
 * 定义三角函数和反三角函数接口，包括基本三角函数、双曲函数及其反函数。
 * 这些函数是数学分析的基础工具，广泛应用于几何、物理和工程计算。
 *
 * Defines interfaces for trigonometric and inverse trigonometric functions,
 * including basic trigonometric functions, hyperbolic functions, and their inverses.
 * These functions are fundamental tools in mathematical analysis, widely used in
 * geometry, physics, and engineering calculations.
 *
 * 数学定义 / Mathematical definitions:
 * 基本三角函数 / Basic trigonometric functions:
 * - sin(x): 正弦函数 / sine function
 * - cos(x): 余弦函数 / cosine function
 * - tan(x) = sin(x)/cos(x): 正切函数 / tangent function
 * - sec(x) = 1/cos(x): 正割函数 / secant function
 * - csc(x) = 1/sin(x): 余割函数 / cosecant function
 * - cot(x) = cos(x)/sin(x): 余切函数 / cotangent function
 *
 * 反三角函数 / Inverse trigonometric functions:
 * - asin(x): 反正弦函数 / arcsine function
 * - acos(x): 反余弦函数 / arccosine function
 * - atan(x): 反正切函数 / arctangent function
 * - asec(x): 反正割函数 / arcsecant function
 * - acsc(x): 反余割函数 / arccosecant function
 * - acot(x): 反余切函数 / arccotangent function
 *
 * 双曲函数 / Hyperbolic functions:
 * - sinh(x): 双曲正弦 / hyperbolic sine
 * - cosh(x): 双曲余弦 / hyperbolic cosine
 * - tanh(x): 双曲正切 / hyperbolic tangent
 * - sech(x): 双曲正割 / hyperbolic secant
 * - csch(x): 双曲余割 / hyperbolic cosecant
 * - coth(x): 双曲余切 / hyperbolic cotangent
 *
 * 反双曲函数 / Inverse hyperbolic functions:
 * - asinh(x): 反双曲正弦 / inverse hyperbolic sine
 * - acosh(x): 反双曲余弦 / inverse hyperbolic cosine
 * - atanh(x): 反双曲正切 / inverse hyperbolic tangent
 * - asech(x): 反双曲正割 / inverse hyperbolic secant
 * - acsch(x): 反双曲余割 / inverse hyperbolic cosecant
 * - acoth(x): 反双曲余切 / inverse hyperbolic cotangent
 */
package fuookami.ospf.kotlin.math.operator

/**
 * 三角函数接口
 * Trigonometric Functions Interface
 *
 * 定义完整的三角函数和双曲函数运算接口。
 * 包括基本三角函数、反三角函数、双曲函数和反双曲函数。
 * 返回值可能为 null，表示运算无定义（如超出定义域）。
 *
 * Defines the complete interface for trigonometric and hyperbolic function operations.
 * Includes basic trigonometric functions, inverse trigonometric functions, hyperbolic functions, and inverse hyperbolic functions.
 * Return value may be null, indicating the operation is undefined (e.g., outside the domain).
 *
 * @param Ret 三角函数运算的结果类型
 *
 * @param Ret The result type of trigonometric operations
 */
interface Trigonometry<out Ret> {
    /**
     * 计算正弦值 sin(x)
     * Calculates the sine value sin(x)
     *
     * @return 正弦值
     *
     * @return Sine value
     */
    fun sin(): Ret

    /**
     * 计算余弦值 cos(x)
     * Calculates the cosine value cos(x)
     *
     * @return 余弦值
     *
     * @return Cosine value
     */
    fun cos(): Ret

    /**
     * 计算正割值 sec(x) = 1/cos(x)
     * Calculates the secant value sec(x) = 1/cos(x)
     *
     * @return 正割值，如果 cos(x) = 0 则返回 null
     *
     * @return Secant value, or null if cos(x) = 0
     */
    fun sec(): Ret?

    /**
     * 计算余割值 csc(x) = 1/sin(x)
     * Calculates the cosecant value csc(x) = 1/sin(x)
     *
     * @return 余割值，如果 sin(x) = 0 则返回 null
     *
     * @return Cosecant value, or null if sin(x) = 0
     */
    fun csc(): Ret?

    /**
     * 计算正切值 tan(x) = sin(x)/cos(x)
     * Calculates the tangent value tan(x) = sin(x)/cos(x)
     *
     * @return 正切值，如果 cos(x) = 0 则返回 null
     *
     * @return Tangent value, or null if cos(x) = 0
     */
    fun tan(): Ret?

    /**
     * 计算余切值 cot(x) = cos(x)/sin(x)
     * Calculates the cotangent value cot(x) = cos(x)/sin(x)
     *
     * @return 余切值，如果 sin(x) = 0 则返回 null
     *
     * @return Cotangent value, or null if sin(x) = 0
     */
    fun cot(): Ret?

    /**
     * 计算反正弦值 asin(x)
     * Calculates the arcsine value asin(x)
     *
     * @return 反正弦值，如果 |x| > 1 则返回 null
     *
     * @return Arcsine value, or null if |x| > 1
     */
    fun asin(): Ret?

    /**
     * 计算反余弦值 acos(x)
     * Calculates the arccosine value acos(x)
     *
     * @return 反余弦值，如果 |x| > 1 则返回 null
     *
     * @return Arccosine value, or null if |x| > 1
     */
    fun acos(): Ret?

    /**
     * 计算反正割值 asec(x) = acos(1/x)
     * Calculates the arcsecant value asec(x) = acos(1/x)
     *
     * @return 反正割值，如果 |x| < 1 则返回 null
     *
     * @return Arcsecant value, or null if |x| < 1
     */
    fun asec(): Ret?

    /**
     * 计算反余割值 acsc(x) = asin(1/x)
     * Calculates the arccosecant value acsc(x) = asin(1/x)
     *
     * @return 反余割值，如果 |x| < 1 则返回 null
     *
     * @return Arccosecant value, or null if |x| < 1
     */
    fun acsc(): Ret?

    /**
     * 计算反正切值 atan(x)
     * Calculates the arctangent value atan(x)
     *
     * @return 反正切值
     *
     * @return Arctangent value
     */
    fun atan(): Ret

    /**
     * 计算反余切值 acot(x)
     * Calculates the arccotangent value acot(x)
     *
     * @return 反余切值
     *
     * @return Arccotangent value
     */
    fun acot(): Ret?

    /**
     * 计算双曲正弦值 sinh(x)
     * Calculates the hyperbolic sine value sinh(x)
     *
     * @return 双曲正弦值
     *
     * @return Hyperbolic sine value
     */
    fun sinh(): Ret

    /**
     * 计算双曲余弦值 cosh(x)
     * Calculates the hyperbolic cosine value cosh(x)
     *
     * @return 双曲余弦值
     *
     * @return Hyperbolic cosine value
     */
    fun cosh(): Ret

    /**
     * 计算双曲正割值 sech(x) = 1/cosh(x)
     * Calculates the hyperbolic secant value sech(x) = 1/cosh(x)
     *
     * @return 双曲正割值
     *
     * @return Hyperbolic secant value
     */
    fun sech(): Ret

    /**
     * 计算双曲余割值 csch(x) = 1/sinh(x)
     * Calculates the hyperbolic cosecant value csch(x) = 1/sinh(x)
     *
     * @return 双曲余割值，如果 sinh(x) = 0 则返回 null
     *
     * @return Hyperbolic cosecant value, or null if sinh(x) = 0
     */
    fun csch(): Ret?

    /**
     * 计算双曲正切值 tanh(x) = sinh(x)/cosh(x)
     * Calculates the hyperbolic tangent value tanh(x) = sinh(x)/cosh(x)
     *
     * @return 双曲正切值
     *
     * @return Hyperbolic tangent value
     */
    fun tanh(): Ret

    /**
     * 计算双曲余切值 coth(x) = cosh(x)/sinh(x)
     * Calculates the hyperbolic cotangent value coth(x) = cosh(x)/sinh(x)
     *
     * @return 双曲余切值，如果 sinh(x) = 0 则返回 null
     *
     * @return Hyperbolic cotangent value, or null if sinh(x) = 0
     */
    fun coth(): Ret?

    /**
     * 计算反双曲正弦值 asinh(x)
     * Calculates the inverse hyperbolic sine value asinh(x)
     *
     * @return 反双曲正弦值
     *
     * @return Inverse hyperbolic sine value
     */
    fun asinh(): Ret

    /**
     * 计算反双曲余弦值 acosh(x)
     * Calculates the inverse hyperbolic cosine value acosh(x)
     *
     * @return 反双曲余弦值，如果 x < 1 则返回 null
     *
     * @return Inverse hyperbolic cosine value, or null if x < 1
     */
    fun acosh(): Ret?

    /**
     * 计算反双曲正割值 asech(x)
     * Calculates the inverse hyperbolic secant value asech(x)
     *
     * @return 反双曲正割值，如果 x <= 0 或 x > 1 则返回 null
     *
     * @return Inverse hyperbolic secant value, or null if x <= 0 or x > 1
     */
    fun asech(): Ret?

    /**
     * 计算反双曲余割值 acsch(x)
     * Calculates the inverse hyperbolic cosecant value acsch(x)
     *
     * @return 反双曲余割值，如果 x = 0 则返回 null
     *
     * @return Inverse hyperbolic cosecant value, or null if x = 0
     */
    fun acsch(): Ret?

    /**
     * 计算反双曲正切值 atanh(x)
     * Calculates the inverse hyperbolic tangent value atanh(x)
     *
     * @return 反双曲正切值，如果 |x| >= 1 则返回 null
     *
     * @return Inverse hyperbolic tangent value, or null if |x| >= 1
     */
    fun atanh(): Ret?

    /**
     * 计算反双曲余切值 acoth(x)
     * Calculates the inverse hyperbolic cotangent value acoth(x)
     *
     * @return 反双曲余切值，如果 |x| <= 1 则返回 null
     *
     * @return Inverse hyperbolic cotangent value, or null if |x| <= 1
     */
    fun acoth(): Ret?
}

/**
 * 计算正弦值 sin(x)
 * Calculates the sine value sin(x)
 *
 * @param T 输入类型，必须实现 Trigonometry 接口
 * @param Ret 返回值类型
 * @param x 输入值
 * @return 正弦值
 *
 * @param T The input type, must implement the Trigonometry interface
 * @param Ret The return type
 * @param x Input value
 * @return Sine value
 */
fun <T, Ret> sin(x: T): Ret where T : Trigonometry<Ret> {
    return x.sin()
}

/**
 * 计算余弦值 cos(x)
 * Calculates the cosine value cos(x)
 *
 * @param T 输入类型，必须实现 Trigonometry 接口
 * @param Ret 返回值类型
 * @param x 输入值
 * @return 余弦值
 *
 * @param T The input type, must implement the Trigonometry interface
 * @param Ret The return type
 * @param x Input value
 * @return Cosine value
 */
fun <T, Ret> cos(x: T): Ret where T : Trigonometry<Ret> {
    return x.cos()
}

/**
 * 计算正割值 sec(x)
 * Calculates the secant value sec(x)
 *
 * @param T 输入类型，必须实现 Trigonometry 接口
 * @param Ret 返回值类型
 * @param x 输入值
 * @return 正割值，如果 cos(x) = 0 则返回 null
 *
 * @param T The input type, must implement the Trigonometry interface
 * @param Ret The return type
 * @param x Input value
 * @return Secant value, or null if cos(x) = 0
 */
fun <T, Ret> sec(x: T): Ret? where T : Trigonometry<Ret> {
    return x.sec()
}

/**
 * 计算余割值 csc(x)
 * Calculates the cosecant value csc(x)
 *
 * @param T 输入类型，必须实现 Trigonometry 接口
 * @param Ret 返回值类型
 * @param x 输入值
 * @return 余割值，如果 sin(x) = 0 则返回 null
 *
 * @param T The input type, must implement the Trigonometry interface
 * @param Ret The return type
 * @param x Input value
 * @return Cosecant value, or null if sin(x) = 0
 */
fun <T, Ret> csc(x: T): Ret? where T : Trigonometry<Ret> {
    return x.csc()
}

/**
 * 计算正切值 tan(x)
 * Calculates the tangent value tan(x)
 *
 * @param T 输入类型，必须实现 Trigonometry 接口
 * @param Ret 返回值类型
 * @param x 输入值
 * @return 正切值，如果 cos(x) = 0 则返回 null
 *
 * @param T The input type, must implement the Trigonometry interface
 * @param Ret The return type
 * @param x Input value
 * @return Tangent value, or null if cos(x) = 0
 */
fun <T, Ret> tan(x: T): Ret? where T : Trigonometry<Ret> {
    return x.tan()
}

/**
 * 计算余切值 cot(x)
 * Calculates the cotangent value cot(x)
 *
 * @param T 输入类型，必须实现 Trigonometry 接口
 * @param Ret 返回值类型
 * @param x 输入值
 * @return 余切值，如果 sin(x) = 0 则返回 null
 *
 * @param T The input type, must implement the Trigonometry interface
 * @param Ret The return type
 * @param x Input value
 * @return Cotangent value, or null if sin(x) = 0
 */
fun <T, Ret> cot(x: T): Ret? where T : Trigonometry<Ret> {
    return x.cot()
}

/**
 * 计算反正弦值 asin(x)
 * Calculates the arcsine value asin(x)
 *
 * @param T 输入类型，必须实现 Trigonometry 接口
 * @param Ret 返回值类型
 * @param x 输入值
 * @return 反正弦值，如果 |x| > 1 则返回 null
 *
 * @param T The input type, must implement the Trigonometry interface
 * @param Ret The return type
 * @param x Input value
 * @return Arcsine value, or null if |x| > 1
 */
fun <T, Ret> asin(x: T): Ret? where T : Trigonometry<Ret> {
    return x.asin()
}

/**
 * 计算反余弦值 acos(x)
 * Calculates the arccosine value acos(x)
 *
 * @param T 输入类型，必须实现 Trigonometry 接口
 * @param Ret 返回值类型
 * @param x 输入值
 * @return 反余弦值，如果 |x| > 1 则返回 null
 *
 * @param T The input type, must implement the Trigonometry interface
 * @param Ret The return type
 * @param x Input value
 * @return Arccosine value, or null if |x| > 1
 */
fun <T, Ret> acos(x: T): Ret? where T : Trigonometry<Ret> {
    return x.acos()
}

/**
 * 计算反正割值 asec(x)
 * Calculates the arcsecant value asec(x)
 *
 * @param T 输入类型，必须实现 Trigonometry 接口
 * @param Ret 返回值类型
 * @param x 输入值
 * @return 反正割值，如果 |x| < 1 则返回 null
 *
 * @param T The input type, must implement the Trigonometry interface
 * @param Ret The return type
 * @param x Input value
 * @return Arcsecant value, or null if |x| < 1
 */
fun <T, Ret> asec(x: T): Ret? where T : Trigonometry<Ret> {
    return x.asec()
}

/**
 * 计算反余割值 acsc(x)
 * Calculates the arccosecant value acsc(x)
 *
 * @param T 输入类型，必须实现 Trigonometry 接口
 * @param Ret 返回值类型
 * @param x 输入值
 * @return 反余割值，如果 |x| < 1 则返回 null
 *
 * @param T The input type, must implement the Trigonometry interface
 * @param Ret The return type
 * @param x Input value
 * @return Arccosecant value, or null if |x| < 1
 */
fun <T, Ret> acsc(x: T): Ret? where T : Trigonometry<Ret> {
    return x.acsc()
}

/**
 * 计算反正切值 atan(x)
 * Calculates the arctangent value atan(x)
 *
 * @param T 输入类型，必须实现 Trigonometry 接口
 * @param Ret 返回值类型
 * @param x 输入值
 * @return 反正切值
 *
 * @param T The input type, must implement the Trigonometry interface
 * @param Ret The return type
 * @param x Input value
 * @return Arctangent value
 */
fun <T, Ret> atan(x: T): Ret where T : Trigonometry<Ret> {
    return x.atan()
}

/**
 * 计算反余切值 acot(x)
 * Calculates the arccotangent value acot(x)
 *
 * @param T 输入类型，必须实现 Trigonometry 接口
 * @param Ret 返回值类型
 * @param x 输入值
 * @return 反余切值
 *
 * @param T The input type, must implement the Trigonometry interface
 * @param Ret The return type
 * @param x Input value
 * @return Arccotangent value
 */
fun <T, Ret> acot(x: T): Ret? where T : Trigonometry<Ret> {
    return x.acot()
}

/**
 * 计算双曲正弦值 sinh(x)
 * Calculates the hyperbolic sine value sinh(x)
 *
 * @param T 输入类型，必须实现 Trigonometry 接口
 * @param Ret 返回值类型
 * @param x 输入值
 * @return 双曲正弦值
 *
 * @param T The input type, must implement the Trigonometry interface
 * @param Ret The return type
 * @param x Input value
 * @return Hyperbolic sine value
 */
fun <T, Ret> sinh(x: T): Ret where T : Trigonometry<Ret> {
    return x.sinh()
}

/**
 * 计算双曲余弦值 cosh(x)
 * Calculates the hyperbolic cosine value cosh(x)
 *
 * @param T 输入类型，必须实现 Trigonometry 接口
 * @param Ret 返回值类型
 * @param x 输入值
 * @return 双曲余弦值
 *
 * @param T The input type, must implement the Trigonometry interface
 * @param Ret The return type
 * @param x Input value
 * @return Hyperbolic cosine value
 */
fun <T, Ret> cosh(x: T): Ret where T : Trigonometry<Ret> {
    return x.cosh()
}

/**
 * 计算双曲正割值 sech(x)
 * Calculates the hyperbolic secant value sech(x)
 *
 * @param T 输入类型，必须实现 Trigonometry 接口
 * @param Ret 返回值类型
 * @param x 输入值
 * @return 双曲正割值
 *
 * @param T The input type, must implement the Trigonometry interface
 * @param Ret The return type
 * @param x Input value
 * @return Hyperbolic secant value
 */
fun <T, Ret> sech(x: T): Ret where T : Trigonometry<Ret> {
    return x.sech()
}

/**
 * 计算双曲余割值 csch(x)
 * Calculates the hyperbolic cosecant value csch(x)
 *
 * @param T 输入类型，必须实现 Trigonometry 接口
 * @param Ret 返回值类型
 * @param x 输入值
 * @return 双曲余割值，如果 sinh(x) = 0 则返回 null
 *
 * @param T The input type, must implement the Trigonometry interface
 * @param Ret The return type
 * @param x Input value
 * @return Hyperbolic cosecant value, or null if sinh(x) = 0
 */
fun <T, Ret> csch(x: T): Ret? where T : Trigonometry<Ret> {
    return x.csch()
}

/**
 * 计算双曲正切值 tanh(x)
 * Calculates the hyperbolic tangent value tanh(x)
 *
 * @param T 输入类型，必须实现 Trigonometry 接口
 * @param Ret 返回值类型
 * @param x 输入值
 * @return 双曲正切值
 *
 * @param T The input type, must implement the Trigonometry interface
 * @param Ret The return type
 * @param x Input value
 * @return Hyperbolic tangent value
 */
fun <T, Ret> tanh(x: T): Ret where T : Trigonometry<Ret> {
    return x.tanh()
}

/**
 * 计算双曲余切值 coth(x)
 * Calculates the hyperbolic cotangent value coth(x)
 *
 * @param T 输入类型，必须实现 Trigonometry 接口
 * @param Ret 返回值类型
 * @param x 输入值
 * @return 双曲余切值，如果 sinh(x) = 0 则返回 null
 *
 * @param T The input type, must implement the Trigonometry interface
 * @param Ret The return type
 * @param x Input value
 * @return Hyperbolic cotangent value, or null if sinh(x) = 0
 */
fun <T, Ret> coth(x: T): Ret? where T : Trigonometry<Ret> {
    return x.coth()
}

/**
 * 计算反双曲正弦值 asinh(x)
 * Calculates the inverse hyperbolic sine value asinh(x)
 *
 * @param T 输入类型，必须实现 Trigonometry 接口
 * @param Ret 返回值类型
 * @param x 输入值
 * @return 反双曲正弦值
 *
 * @param T The input type, must implement the Trigonometry interface
 * @param Ret The return type
 * @param x Input value
 * @return Inverse hyperbolic sine value
 */
fun <T, Ret> asinh(x: T): Ret where T : Trigonometry<Ret> {
    return x.asinh()
}

/**
 * 计算反双曲余弦值 acosh(x)
 * Calculates the inverse hyperbolic cosine value acosh(x)
 *
 * @param T 输入类型，必须实现 Trigonometry 接口
 * @param Ret 返回值类型
 * @param x 输入值
 * @return 反双曲余弦值，如果 x < 1 则返回 null
 *
 * @param T The input type, must implement the Trigonometry interface
 * @param Ret The return type
 * @param x Input value
 * @return Inverse hyperbolic cosine value, or null if x < 1
 */
fun <T, Ret> acosh(x: T): Ret? where T : Trigonometry<Ret> {
    return x.acosh()
}

/**
 * 计算反双曲正割值 asech(x)
 * Calculates the inverse hyperbolic secant value asech(x)
 *
 * @param T 输入类型，必须实现 Trigonometry 接口
 * @param Ret 返回值类型
 * @param x 输入值
 * @return 反双曲正割值，如果 x <= 0 或 x > 1 则返回 null
 *
 * @param T The input type, must implement the Trigonometry interface
 * @param Ret The return type
 * @param x Input value
 * @return Inverse hyperbolic secant value, or null if x <= 0 or x > 1
 */
fun <T, Ret> asech(x: T): Ret? where T : Trigonometry<Ret> {
    return x.asech()
}

/**
 * 计算反双曲余割值 acsch(x)
 * Calculates the inverse hyperbolic cosecant value acsch(x)
 *
 * @param T 输入类型，必须实现 Trigonometry 接口
 * @param Ret 返回值类型
 * @param x 输入值
 * @return 反双曲余割值，如果 x = 0 则返回 null
 *
 * @param T The input type, must implement the Trigonometry interface
 * @param Ret The return type
 * @param x Input value
 * @return Inverse hyperbolic cosecant value, or null if x = 0
 */
fun <T, Ret> acsch(x: T): Ret? where T : Trigonometry<Ret> {
    return x.acsch()
}

/**
 * 计算反双曲正切值 atanh(x)
 * Calculates the inverse hyperbolic tangent value atanh(x)
 *
 * @param T 输入类型，必须实现 Trigonometry 接口
 * @param Ret 返回值类型
 * @param x 输入值
 * @return 反双曲正切值，如果 |x| >= 1 则返回 null
 *
 * @param T The input type, must implement the Trigonometry interface
 * @param Ret The return type
 * @param x Input value
 * @return Inverse hyperbolic tangent value, or null if |x| >= 1
 */
fun <T, Ret> atanh(x: T): Ret? where T : Trigonometry<Ret> {
    return x.atanh()
}

/**
 * 计算反双曲余切值 acoth(x)
 * Calculates the inverse hyperbolic cotangent value acoth(x)
 *
 * @param T 输入类型，必须实现 Trigonometry 接口
 * @param Ret 返回值类型
 * @param x 输入值
 * @return 反双曲余切值，如果 |x| <= 1 则返回 null
 *
 * @param T The input type, must implement the Trigonometry interface
 * @param Ret The return type
 * @param x Input value
 * @return Inverse hyperbolic cotangent value, or null if |x| <= 1
 */
fun <T, Ret> acoth(x: T): Ret? where T : Trigonometry<Ret> {
    return x.acoth()
}