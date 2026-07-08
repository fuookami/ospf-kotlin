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

    // ========== 单参数函数 / Single-argument functions ==========

    private fun evaluateSqrt(args: List<Any?>): Any? {
        val v = args.singleOrNull()?.asDoubleOrNull() ?: return null
        return sqrt(v)
    }

    private fun evaluateLog(args: List<Any?>): Any? {
        val v = args.singleOrNull()?.asDoubleOrNull() ?: return null
        return ln(v)
    }

    private fun evaluateLog10(args: List<Any?>): Any? {
        val v = args.singleOrNull()?.asDoubleOrNull() ?: return null
        return log10(v)
    }

    private fun evaluateExp(args: List<Any?>): Any? {
        val v = args.singleOrNull()?.asDoubleOrNull() ?: return null
        return exp(v)
    }

    private fun evaluateSin(args: List<Any?>): Any? {
        val v = args.singleOrNull()?.asDoubleOrNull() ?: return null
        return sin(v)
    }

    private fun evaluateCos(args: List<Any?>): Any? {
        val v = args.singleOrNull()?.asDoubleOrNull() ?: return null
        return cos(v)
    }

    private fun evaluateTan(args: List<Any?>): Any? {
        val v = args.singleOrNull()?.asDoubleOrNull() ?: return null
        return tan(v)
    }

    private fun evaluateAsin(args: List<Any?>): Any? {
        val v = args.singleOrNull()?.asDoubleOrNull() ?: return null
        return asin(v)
    }

    private fun evaluateAcos(args: List<Any?>): Any? {
        val v = args.singleOrNull()?.asDoubleOrNull() ?: return null
        return acos(v)
    }

    private fun evaluateAtan(args: List<Any?>): Any? {
        val v = args.singleOrNull()?.asDoubleOrNull() ?: return null
        return atan(v)
    }

    private fun evaluateFloor(args: List<Any?>): Any? {
        val v = args.singleOrNull()?.asDoubleOrNull() ?: return null
        return floor(v)
    }

    private fun evaluateCeil(args: List<Any?>): Any? {
        val v = args.singleOrNull()?.asDoubleOrNull() ?: return null
        return ceil(v)
    }

    /**
     * round 返回 Long，对齐 Aviator 行为（math.round(3.7) = 4L）。
     * round returns Long, aligning with Aviator behavior (math.round(3.7) = 4L).
     * kotlin.math.round 返回 Double（4.0），此处显式转为 Long。
     * kotlin.math.round returns Double (4.0); here we explicitly convert to Long.
     */
    private fun evaluateRound(args: List<Any?>): Any? {
        val v = args.singleOrNull()?.asDoubleOrNull() ?: return null
        return round(v).toLong()
    }

    // ========== 双参数函数 / Two-argument functions ==========

    private fun evaluatePow(args: List<Any?>): Any? {
        if (args.size != 2) return null
        val base = args[0]?.asDoubleOrNull() ?: return null
        val exponent = args[1]?.asDoubleOrNull() ?: return null
        return Math.pow(base, exponent)
    }

    private fun evaluateMax(args: List<Any?>): Any? {
        if (args.size != 2) return null
        val a = args[0]?.asDoubleOrNull() ?: return null
        val b = args[1]?.asDoubleOrNull() ?: return null
        return max(a, b)
    }

    private fun evaluateMin(args: List<Any?>): Any? {
        if (args.size != 2) return null
        val a = args[0]?.asDoubleOrNull() ?: return null
        val b = args[1]?.asDoubleOrNull() ?: return null
        return min(a, b)
    }

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
