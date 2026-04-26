/**
 * 数值运算分派层
 * Numeric Operations Dispatch Layer
 *
 * 抽象数值类型的一元和二元运算，替代 EvaluateBoolean.kt 中的硬编码类型分支。
 * Abstracts numeric operations for unary and binary operators, replacing hardcoded type branches.
 */
package fuookami.ospf.kotlin.math.symbol.expression.operation

import fuookami.ospf.kotlin.math.symbol.expression.BinaryOperator
import fuookami.ospf.kotlin.math.symbol.expression.UnaryOperator
import kotlin.reflect.KClass

/**
 * 数值运算接口
 * Numeric Operations Interface
 *
 * 为特定数值类型提供一元和二元运算的实现。
 * Provides unary and binary operation implementations for a specific numeric type.
 *
 * @param T 数值类型 / Numeric type
 */
interface NumericOps<T : Any> {
    /** 类型标记，用于运行时匹配 / Type marker for runtime matching */
    val type: KClass<T>

    /** 取反 / Negation */
    fun negate(value: T): T

    /** 绝对值 / Absolute value */
    fun abs(value: T): T

    /** 加法 / Addition */
    fun add(left: T, right: T): T

    /** 减法 / Subtraction */
    fun subtract(left: T, right: T): T

    /** 乘法 / Multiplication */
    fun multiply(left: T, right: T): T

    /** 除法 / Division (returns null on division by zero) */
    fun divide(left: T, right: T): T?

    /** 取模 / Modulo (returns null on division by zero) */
    fun modulo(left: T, right: T): T?

    /** 幂运算 / Power */
    fun power(left: T, right: T): T?
}

/**
 * 数值分派器
 * Numeric Dispatcher
 *
 * 管理注册的 NumericOps 实例，根据运行时类型分派运算。
 * Manages registered NumericOps instances and dispatches operations based on runtime type.
 */
object NumericDispatcher {
    private val registry = mutableMapOf<KClass<out Any>, NumericOps<out Any>>()

    init {
        registerBuiltInNumericOps()
    }

    /** 注册数值运算处理器 / Register a numeric operation handler */
    fun <T : Any> register(ops: NumericOps<T>) {
        registry[ops.type] = ops
    }

    /** 获取指定类型的运算处理器 / Get operation handler for type */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> opsFor(type: KClass<T>): NumericOps<T>? {
        return registry[type] as NumericOps<T>?
    }

    /** 获取值的运算处理器 / Get operation handler for a value's type */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> opsFor(value: T): NumericOps<T>? {
        return registry[value::class] as NumericOps<T>?
    }

    /** 执行一元运算 / Execute unary operation */
    fun evaluateUnary(operator: UnaryOperator, operand: Any): Any? {
        if (operator == UnaryOperator.Positive) {
            return operand
        }

        @Suppress("UNCHECKED_CAST")
        val ops = registry[operand::class] as NumericOps<Any>? ?: return null

        return when (operator) {
            UnaryOperator.Negate -> ops.negate(operand)
            UnaryOperator.Positive -> operand
            UnaryOperator.Abs -> ops.abs(operand)
        }
    }

    /** 执行二元运算 / Execute binary operation */
    fun evaluateBinary(operator: BinaryOperator, left: Any, right: Any): Any? {
        // Both operands must be of the same type
        if (left::class != right::class) return null

        @Suppress("UNCHECKED_CAST")
        val ops = registry[left::class] as NumericOps<Any>? ?: return null

        return when (operator) {
            BinaryOperator.Add -> ops.add(left, right)
            BinaryOperator.Subtract -> ops.subtract(left, right)
            BinaryOperator.Multiply -> ops.multiply(left, right)
            BinaryOperator.Divide -> ops.divide(left, right)
            BinaryOperator.Modulo -> ops.modulo(left, right)
            BinaryOperator.Power -> ops.power(left, right)
        }
    }
}

// ============================================================================
// 内置数值类型实现 / Built-in Numeric Type Implementations
// ============================================================================

private object IntOps : NumericOps<Int> {
    override val type = Int::class
    override fun negate(value: Int) = -value
    override fun abs(value: Int) = kotlin.math.abs(value)
    override fun add(left: Int, right: Int) = left + right
    override fun subtract(left: Int, right: Int) = left - right
    override fun multiply(left: Int, right: Int) = left * right
    override fun divide(left: Int, right: Int) = if (right != 0) left / right else null
    override fun modulo(left: Int, right: Int) = if (right != 0) left % right else null
    override fun power(left: Int, right: Int) = Math.pow(left.toDouble(), right.toDouble()).toInt()
}

private object LongOps : NumericOps<Long> {
    override val type = Long::class
    override fun negate(value: Long) = -value
    override fun abs(value: Long) = kotlin.math.abs(value)
    override fun add(left: Long, right: Long) = left + right
    override fun subtract(left: Long, right: Long) = left - right
    override fun multiply(left: Long, right: Long) = left * right
    override fun divide(left: Long, right: Long) = if (right != 0L) left / right else null
    override fun modulo(left: Long, right: Long) = if (right != 0L) left % right else null
    override fun power(left: Long, right: Long) = Math.pow(left.toDouble(), right.toDouble()).toLong()
}

private object FloatOps : NumericOps<Float> {
    override val type = Float::class
    override fun negate(value: Float) = -value
    override fun abs(value: Float) = kotlin.math.abs(value)
    override fun add(left: Float, right: Float) = left + right
    override fun subtract(left: Float, right: Float) = left - right
    override fun multiply(left: Float, right: Float) = left * right
    override fun divide(left: Float, right: Float) = if (right != 0.0f) left / right else null
    override fun modulo(left: Float, right: Float) = if (right != 0.0f) left % right else null
    override fun power(left: Float, right: Float) = Math.pow(left.toDouble(), right.toDouble()).toFloat()
}

private object DoubleOps : NumericOps<Double> {
    override val type = Double::class
    override fun negate(value: Double) = -value
    override fun abs(value: Double) = kotlin.math.abs(value)
    override fun add(left: Double, right: Double) = left + right
    override fun subtract(left: Double, right: Double) = left - right
    override fun multiply(left: Double, right: Double) = left * right
    override fun divide(left: Double, right: Double) = if (right != 0.0) left / right else null
    override fun modulo(left: Double, right: Double) = if (right != 0.0) left % right else null
    override fun power(left: Double, right: Double) = Math.pow(left, right)
}

/**
 * 注册内置数值类型
 * Register built-in numeric types
 */
fun registerBuiltInNumericOps() {
    NumericDispatcher.register(IntOps)
    NumericDispatcher.register(LongOps)
    NumericDispatcher.register(FloatOps)
    NumericDispatcher.register(DoubleOps)
}
