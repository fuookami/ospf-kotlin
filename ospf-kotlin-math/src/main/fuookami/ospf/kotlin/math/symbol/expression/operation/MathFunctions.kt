/**
 * 数学函数求值器
 * Math Function Evaluator
 *
 * 提供 Aviator 白名单中的 math.* 函数求值能力。
 * Provides evaluation for math.* functions from the Aviator whitelist.
*/
package fuookami.ospf.kotlin.math.symbol.expression.operation

import kotlin.math.*
import fuookami.ospf.kotlin.math.symbol.expression.ScalarFunctionEvaluator

/**
 * 数学函数求值器
 * Math Function Evaluator
 *
 * 实现 ScalarFunctionEvaluator，覆盖 Aviator 白名单中的 17 个 math.* 函数。
 * 未识别的函数委托给 DefaultScalarFunctionEvaluator。
 * Implements ScalarFunctionEvaluator, covering 17 math.* functions from the Aviator whitelist.
 * Unrecognized functions fall through to DefaultScalarFunctionEvaluator.
 *
 * 注：公式域的 validate() 必须在 AST 层面检查 ScalarFunction.name 是否在白名单内，
 * 不能依赖求值器拒绝未知函数——因为组合链包含 DefaultScalarFunctionEvaluator，
 * 后者暴露 lower/upper/trim/length/coalesce 等字符串函数。
 * Note: The formula domain's validate() must check ScalarFunction.name against the whitelist at the AST level,
 * not rely on evaluator rejection — because the composition chain includes DefaultScalarFunctionEvaluator,
 * which exposes string functions like lower/upper/trim/length/coalesce.
*/
object MathFunctionEvaluator : ScalarFunctionEvaluator {

    /** 支持的数学函数名集合 / Set of supported math function names */
    val supportedFunctions: Set<String> = setOf(
        "sqrt", "pow", "log", "log10", "exp",
        "sin", "cos", "tan", "asin", "acos", "atan",
        "floor", "ceil", "round",
        "max", "min", "abs"
    )

    override fun evaluate(name: String, arguments: List<Any?>): Any? {
        return when (name.lowercase()) {
            "sqrt" -> evaluateSqrt(arguments)
            "pow" -> evaluatePow(arguments)
            "log" -> evaluateLog(arguments)
            "log10" -> evaluateLog10(arguments)
            "exp" -> evaluateExp(arguments)
            "sin" -> evaluateSin(arguments)
            "cos" -> evaluateCos(arguments)
            "tan" -> evaluateTan(arguments)
            "asin" -> evaluateAsin(arguments)
            "acos" -> evaluateAcos(arguments)
            "atan" -> evaluateAtan(arguments)
            "floor" -> evaluateFloor(arguments)
            "ceil" -> evaluateCeil(arguments)
            "round" -> evaluateRound(arguments)
            "max" -> evaluateMax(arguments)
            "min" -> evaluateMin(arguments)
            "abs" -> DefaultScalarFunctionEvaluator.evaluate(name, arguments)
            else -> DefaultScalarFunctionEvaluator.evaluate(name, arguments)
        }
    }

/**
 * Evaluates the square root function.
 * 求值平方根函数。
 *
 * @param args function arguments containing a single numeric value / 包含单个数值的函数参数
 * @return the square root of the argument, or null if invalid / 参数的平方根，无效时返回 null
*/

    // ========== 单参数函数 / Single-argument functions ==========

    private fun evaluateSqrt(args: List<Any?>): Any? {
        val v = args.singleOrNull()?.asDoubleOrNull() ?: return null
        return sqrt(v)
    }

/**
 * Evaluates the natural logarithm function.
 * 求值自然对数函数。
 *
 * @param args function arguments containing a single numeric value / 包含单个数值的函数参数
 * @return the natural logarithm of the argument, or null if invalid / 参数的自然对数，无效时返回 null
*/
    private fun evaluateLog(args: List<Any?>): Any? {
        val v = args.singleOrNull()?.asDoubleOrNull() ?: return null
        return ln(v)
    }

/**
 * Evaluates the base-10 logarithm function.
 * 求值以 10 为底的对数函数。
 *
 * @param args function arguments containing a single numeric value / 包含单个数值的函数参数
 * @return the base-10 logarithm of the argument, or null if invalid / 参数的以 10 为底的对数，无效时返回 null
*/
    private fun evaluateLog10(args: List<Any?>): Any? {
        val v = args.singleOrNull()?.asDoubleOrNull() ?: return null
        return log10(v)
    }

/**
 * Evaluates the exponential function.
 * 求值指数函数。
 *
 * @param args function arguments containing a single numeric value / 包含单个数值的函数参数
 * @return e raised to the power of the argument, or null if invalid / e 的参数次幂，无效时返回 null
*/
    private fun evaluateExp(args: List<Any?>): Any? {
        val v = args.singleOrNull()?.asDoubleOrNull() ?: return null
        return exp(v)
    }

/**
 * Evaluates the sine function.
 * 求值正弦函数。
 *
 * @param args function arguments containing a single numeric value (radians) / 包含单个数值的函数参数（弧度）
 * @return the sine of the argument, or null if invalid / 参数的正弦值，无效时返回 null
*/
    private fun evaluateSin(args: List<Any?>): Any? {
        val v = args.singleOrNull()?.asDoubleOrNull() ?: return null
        return sin(v)
    }

/**
 * Evaluates the cosine function.
 * 求值余弦函数。
 *
 * @param args function arguments containing a single numeric value (radians) / 包含单个数值的函数参数（弧度）
 * @return the cosine of the argument, or null if invalid / 参数的余弦值，无效时返回 null
*/
    private fun evaluateCos(args: List<Any?>): Any? {
        val v = args.singleOrNull()?.asDoubleOrNull() ?: return null
        return cos(v)
    }

/**
 * Evaluates the tangent function.
 * 求值正切函数。
 *
 * @param args function arguments containing a single numeric value (radians) / 包含单个数值的函数参数（弧度）
 * @return the tangent of the argument, or null if invalid / 参数的正切值，无效时返回 null
*/
    private fun evaluateTan(args: List<Any?>): Any? {
        val v = args.singleOrNull()?.asDoubleOrNull() ?: return null
        return tan(v)
    }

/**
 * Evaluates the arcsine function.
 * 求值反正弦函数。
 *
 * @param args function arguments containing a single numeric value / 包含单个数值的函数参数
 * @return the arcsine of the argument in radians, or null if invalid / 参数的反正弦值（弧度），无效时返回 null
*/
    private fun evaluateAsin(args: List<Any?>): Any? {
        val v = args.singleOrNull()?.asDoubleOrNull() ?: return null
        return asin(v)
    }

/**
 * Evaluates the arccosine function.
 * 求值反余弦函数。
 *
 * @param args function arguments containing a single numeric value / 包含单个数值的函数参数
 * @return the arccosine of the argument in radians, or null if invalid / 参数的反余弦值（弧度），无效时返回 null
*/
    private fun evaluateAcos(args: List<Any?>): Any? {
        val v = args.singleOrNull()?.asDoubleOrNull() ?: return null
        return acos(v)
    }

/**
 * Evaluates the arctangent function.
 * 求值反正切函数。
 *
 * @param args function arguments containing a single numeric value / 包含单个数值的函数参数
 * @return the arctangent of the argument in radians, or null if invalid / 参数的反正切值（弧度），无效时返回 null
*/
    private fun evaluateAtan(args: List<Any?>): Any? {
        val v = args.singleOrNull()?.asDoubleOrNull() ?: return null
        return atan(v)
    }

/**
 * Evaluates the floor function.
 * 求值向下取整函数。
 *
 * @param args function arguments containing a single numeric value / 包含单个数值的函数参数
 * @return the largest integer less than or equal to the argument, or null if invalid / 小于或等于参数的最大整数，无效时返回 null
*/
    private fun evaluateFloor(args: List<Any?>): Any? {
        val v = args.singleOrNull()?.asDoubleOrNull() ?: return null
        return floor(v)
    }

/**
 * Evaluates the ceiling function.
 * 求值向上取整函数。
 *
 * @param args function arguments containing a single numeric value / 包含单个数值的函数参数
 * @return the smallest integer greater than or equal to the argument, or null if invalid / 大于或等于参数的最小整数，无效时返回 null
*/
    private fun evaluateCeil(args: List<Any?>): Any? {
        val v = args.singleOrNull()?.asDoubleOrNull() ?: return null
        return ceil(v)
    }

    /**
     * round 返回 Long，对齐 Aviator 行为（math.round(3.7) = 4L）。
     * round returns Long, aligning with Aviator behavior (math.round(3.7) = 4L).
     * kotlin.math.round 返回 Double（4.0），此处显式转为 Long。
     * kotlin.math.round returns Double (4.0); here we explicitly convert to Long.
     *
     * @param args function arguments containing a single numeric value / 包含单个数值的函数参数
     * @return the argument rounded to Long, or null if invalid / 参数四舍五入后的 Long 值，无效时返回 null
    */
    private fun evaluateRound(args: List<Any?>): Any? {
        val v = args.singleOrNull()?.asDoubleOrNull() ?: return null
        return round(v).toLong()
    }

/**
 * Evaluates the power function.
 * 求值幂函数。
 *
 * @param args function arguments containing base and exponent / 包含底数和指数的函数参数
 * @return base raised to the power of exponent, or null if invalid / 底数的指数次幂，无效时返回 null
*/

    // ========== 双参数函数 / Two-argument functions ==========

    private fun evaluatePow(args: List<Any?>): Any? {
        if (args.size != 2) return null
        val base = args[0]?.asDoubleOrNull() ?: return null
        val exponent = args[1]?.asDoubleOrNull() ?: return null
        return Math.pow(base, exponent)
    }

/**
 * Evaluates the maximum function.
 * 求值最大值函数。
 *
 * @param args function arguments containing two numeric values / 包含两个数值的函数参数
 * @return the larger of the two arguments, or null if invalid / 两个参数中的较大值，无效时返回 null
*/
    private fun evaluateMax(args: List<Any?>): Any? {
        if (args.size != 2) return null
        val a = args[0]?.asDoubleOrNull() ?: return null
        val b = args[1]?.asDoubleOrNull() ?: return null
        return max(a, b)
    }

/**
 * Evaluates the minimum function.
 * 求值最小值函数。
 *
 * @param args function arguments containing two numeric values / 包含两个数值的函数参数
 * @return the smaller of the two arguments, or null if invalid / 两个参数中的较小值，无效时返回 null
*/
    private fun evaluateMin(args: List<Any?>): Any? {
        if (args.size != 2) return null
        val a = args[0]?.asDoubleOrNull() ?: return null
        val b = args[1]?.asDoubleOrNull() ?: return null
        return min(a, b)
    }

/**
 * Converts this value to Double, or returns null if not a numeric type.
 * 将此值转换为 Double，若非数值类型则返回 null。
 *
 * @return the value as Double, or null if conversion is not possible / 转换后的 Double 值，无法转换时返回 null
*/

    // ========== 辅助 / Helpers ==========

    private fun Any?.asDoubleOrNull(): Double? = when (this) {
        is Double -> this
        is Float -> this.toDouble()
        is Long -> this.toDouble()
        is Int -> this.toDouble()
        is Short -> this.toDouble()
        is Byte -> this.toDouble()
        is Number -> this.toDouble()
        else -> null
    }
}
