/**
 * 标量表达式
 * Scalar Expression
 *
 * 定义标量值的表达式 AST，包括常量、引用、一元操作、二元操作、函数调用和自定义表达式。
 * Defines the expression AST for scalar values,
 * including constant, reference, unary, binary, function call, and custom expressions.
 */
package fuookami.ospf.kotlin.math.symbol.expression

import fuookami.ospf.kotlin.math.symbol.*

/**
 * 标量表达式
 * Scalar Expression
 *
 * 表示标量值的表达式，支持多种操作类型。
 * Represents an expression for scalar values, supporting various operation types.
 */
sealed interface ScalarExpression<out T> {

    /**
     * 表达式类型名称
     * Expression type name
     */
    val typeName: String

    /**
     * 子表达式列表
     * Child expressions list
     */
    val children: List<ScalarExpression<T>>

    /**
     * 判断表达式是否是常量
     * Check if expression is constant
     */
    fun isConstant(): Boolean = when (this) {
        is ScalarConstant -> true
        is ScalarReference -> false
        is ScalarUnary -> operand.isConstant()
        is ScalarBinary -> left.isConstant() && right.isConstant()
        is ScalarFunction -> arguments.all { it.isConstant() }
        is ScalarCustom -> false
    }

    /**
     * 判断表达式是否包含引用
     * Check if expression contains references
     */
    fun containsReference(): Boolean = when (this) {
        is ScalarConstant -> false
        is ScalarReference -> true
        is ScalarUnary -> operand.containsReference()
        is ScalarBinary -> left.containsReference() || right.containsReference()
        is ScalarFunction -> arguments.any { it.containsReference() }
        is ScalarCustom -> true
    }

    /**
     * 获取表达式中的所有引用路径
     * Get all reference paths in the expression
     */
    fun collectReferences(): Set<PropertyPath> {
        val refs = mutableSetOf<PropertyPath>()
        collectReferencesInto(refs)
        return refs
    }

    /**
     * 将引用路径收集到指定集合
     * Collect reference paths into specified collection
     */
    fun collectReferencesInto(refs: MutableSet<PropertyPath>) {
        when (this) {
            is ScalarReference -> refs.add(path)
            is ScalarUnary -> operand.collectReferencesInto(refs)
            is ScalarBinary -> {
                left.collectReferencesInto(refs)
                right.collectReferencesInto(refs)
            }
            is ScalarFunction -> arguments.forEach { it.collectReferencesInto(refs) }
            is ScalarConstant, is ScalarCustom -> { }
        }
    }
}

/**
 * 标量常量
 * Scalar Constant
 *
 * 表示常量值。
 * Represents a constant value.
 */
data class ScalarConstant<T>(
    val value: T
) : ScalarExpression<T> {
    override val typeName: String = "Constant"
    override val children: List<ScalarExpression<T>> = emptyList()

    override fun toString(): String = "Constant($value)"
}

/**
 * 标量引用
 * Scalar Reference
 *
 * 表示对属性路径的引用。
 * Represents a reference to a property path.
 */
data class ScalarReference<T>(
    val path: PropertyPath,
    val symbol: PathSymbol = path.toPathSymbol()
) : ScalarExpression<T> {
    override val typeName: String = "Reference"
    override val children: List<ScalarExpression<T>> = emptyList()

    override fun toString(): String = "Reference(${path.value})"
}

/**
 * 标量一元操作
 * Scalar Unary Operation
 *
 * 表示一元操作表达式，如负号。
 * Represents a unary operation expression, such as negation.
 */
data class ScalarUnary<T>(
    val operator: UnaryOperator,
    val operand: ScalarExpression<T>
) : ScalarExpression<T> {
    override val typeName: String = "Unary"
    override val children: List<ScalarExpression<T>> = listOf(operand)

    override fun toString(): String = "${OperatorSymbols.unary(operator)}($operand)"
}

/**
 * 标量二元操作
 * Scalar Binary Operation
 *
 * 表示二元操作表达式，如加法、减法。
 * Represents a binary operation expression, such as addition, subtraction.
 */
data class ScalarBinary<T>(
    val operator: BinaryOperator,
    val left: ScalarExpression<T>,
    val right: ScalarExpression<T>
) : ScalarExpression<T> {
    override val typeName: String = "Binary"
    override val children: List<ScalarExpression<T>> = listOf(left, right)

    override fun toString(): String = "($left ${OperatorSymbols.binary(operator)} $right)"
}

/**
 * 标量函数调用
 * Scalar Function Call
 *
 * 表示函数调用表达式。
 * Represents a function call expression.
 */
data class ScalarFunction<T>(
    val name: String,
    val arguments: List<ScalarExpression<T>>
) : ScalarExpression<T> {
    override val typeName: String = "Function"
    override val children: List<ScalarExpression<T>> = arguments

    override fun toString(): String = "$name(${arguments.joinToString(", ")})"
}

/**
 * 标量自定义表达式
 * Scalar Custom Expression
 *
 * 表示自定义表达式，用于扩展。
 * Represents a custom expression for extension.
 */
data class ScalarCustom<T>(
    val value: Any,
    val description: String? = null
) : ScalarExpression<T> {
    override val typeName: String = "Custom"
    override val children: List<ScalarExpression<T>> = emptyList()

    override fun toString(): String = description ?: "Custom($value)"
}

/**
 * 标量表达式工厂
 * Scalar Expression Factory
 *
 * 提供便捷的标量表达式构造方法。
 * Provides convenient scalar expression construction methods.
 */
object ScalarExpressionFactory {
    /**
     * 创建常量表达式
     * Create constant expression
     */
    fun <T> constant(value: T): ScalarExpression<T> = ScalarConstant(value)

    /**
     * 创建引用表达式
     * Create reference expression
     */
    fun <T> reference(path: PropertyPath): ScalarExpression<T> = ScalarReference(path)

    /**
     * 创建引用表达式
     * Create reference expression
     */
    fun <T> reference(path: String): ScalarExpression<T> = reference(PropertyPath.parse(path))

    /**
     * 创建一元操作表达式
     * Create unary operation expression
     */
    fun <T> unary(operator: UnaryOperator, operand: ScalarExpression<T>): ScalarExpression<T> =
        ScalarUnary(operator, operand)

    /**
     * 创建二元操作表达式
     * Create binary operation expression
     */
    fun <T> binary(
        operator: BinaryOperator,
        left: ScalarExpression<T>,
        right: ScalarExpression<T>
    ): ScalarExpression<T> = ScalarBinary(operator, left, right)

    /**
     * 创建函数调用表达式
     * Create function call expression
     */
    fun <T> function(name: String, arguments: List<ScalarExpression<T>>): ScalarExpression<T> =
        ScalarFunction(name, arguments)

    /**
     * 创建加法表达式
     * Create addition expression
     */
    fun <T> add(left: ScalarExpression<T>, right: ScalarExpression<T>): ScalarExpression<T> =
        binary(BinaryOperator.Add, left, right)

    /**
     * 创建减法表达式
     * Create subtraction expression
     */
    fun <T> subtract(left: ScalarExpression<T>, right: ScalarExpression<T>): ScalarExpression<T> =
        binary(BinaryOperator.Subtract, left, right)

    /**
     * 创建乘法表达式
     * Create multiplication expression
     */
    fun <T> multiply(left: ScalarExpression<T>, right: ScalarExpression<T>): ScalarExpression<T> =
        binary(BinaryOperator.Multiply, left, right)

    /**
     * 创建除法表达式
     * Create division expression
     */
    fun <T> divide(left: ScalarExpression<T>, right: ScalarExpression<T>): ScalarExpression<T> =
        binary(BinaryOperator.Divide, left, right)
}