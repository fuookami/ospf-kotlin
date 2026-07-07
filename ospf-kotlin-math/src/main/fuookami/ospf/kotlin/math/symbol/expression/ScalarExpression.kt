/**
 * 标量表达弌
 * Scalar Expression
 *
 * 定义标量值的表达弌AST，包括常量、引用、一元操作、二元操作、函数调用和自定义表达式。
 * Defines the expression AST for scalar values,
 * including constant, reference, unary, binary, function call, and custom expressions.
 */
package fuookami.ospf.kotlin.math.symbol.expression

import fuookami.ospf.kotlin.math.symbol.*

/**
 * 标量表达弌
 * Scalar Expression
 *
 * 表示标量值的表达式，支持多种操作类型。
 * Represents an expression for scalar values, supporting various operation types.
 */
sealed interface ScalarExpression<out T> {

    /**
     * 表达式类型名秌
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
     *
     * @return 如果表达式是常量则返回 true，否则返回 false / true if the expression is constant, false otherwise
     */
    fun isConstant(): Boolean = when (this) {
        is ScalarConstant -> true
        is ScalarReference -> false
        is ScalarSymbolReference -> false
        is ScalarUnary -> operand.isConstant()
        is ScalarBinary -> left.isConstant() && right.isConstant()
        is ScalarFunction -> arguments.all { it.isConstant() }
        is ScalarCustom -> false
    }

    /**
     * 判断表达式是否包含引甌
     * Check if expression contains references
     *
     * @return 如果表达式包含引用则返回 true，否则返回 false / true if the expression contains references, false otherwise
     */
    fun containsReference(): Boolean = when (this) {
        is ScalarConstant -> false
        is ScalarReference -> true
        is ScalarSymbolReference -> true
        is ScalarUnary -> operand.containsReference()
        is ScalarBinary -> left.containsReference() || right.containsReference()
        is ScalarFunction -> arguments.any { it.containsReference() }
        is ScalarCustom -> true
    }

    /**
     * 获取表达式中的所有引用路後
     * Get all reference paths in the expression
     *
     * @return 表达式中所有引用路径的集合 / Set of all reference paths in the expression
     */
    fun collectReferences(): Set<PropertyPath> {
        val refs = mutableSetOf<PropertyPath>()
        collectReferencesInto(refs)
        return refs
    }

    /**
     * 将引用路径收集到指定集合
     * Collect reference paths into specified collection
     *
     * @param refs 用于收集引用路径的可变集合 / Mutable set to collect reference paths into
     */
    fun collectReferencesInto(refs: MutableSet<PropertyPath>) {
        when (this) {
            is ScalarReference -> refs.add(path)
            is ScalarSymbolReference -> { }
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
/**
 * 标量常量
 * Scalar Constant
 *
 * 表示常量值。
 * Represents a constant value.
 *
 * @property value 常量值 / Constant value
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
 *
 * @property path 属性路径 / Property path
 * @property symbol 路径符号，默认从 path 转换 / Path symbol, defaults to conversion from path
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
 * 标量符号引用
 * Scalar Symbol Reference
 *
 * 表示对符号的引用（非路径形式）。
 * Represents a reference to a symbol (non-path form).
 *
 * @property symbol 符号 / Symbol
 */
data class ScalarSymbolReference<T>(
    val symbol: Symbol
) : ScalarExpression<T> {
    override val typeName: String = "SymbolReference"
    override val children: List<ScalarExpression<T>> = emptyList()

    override fun toString(): String = "SymbolReference(${symbol.name})"
}

/**
 * 标量一元操佌
 * Scalar Unary Operation
 *
 * 表示一元操作表达式，如负号。
 * Represents a unary operation expression, such as negation.
 *
 * @property operator 一元操作符 / Unary operator
 * @property operand 操作数 / Operand
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
 *
 * @property operator 二元操作符 / Binary operator
 * @property left 左操作数 / Left operand
 * @property right 右操作数 / Right operand
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
 *
 * @property name 函数名称 / Function name
 * @property arguments 参数列表 / Argument list
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
 *
 * @property value 自定义值 / Custom value
 * @property description 可选描述 / Optional description
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
 * 标量表达式工厌
 * Scalar Expression Factory
 *
 * 提供便捷的标量表达式构造方法。
 * Provides convenient scalar expression construction methods.
 */
object ScalarExpressionFactory {
    /**
     * 创建常量表达弌
     * Create constant expression
     *
     * @param value 常量值 / Constant value
     * @return 标量表达式 / Scalar expression
     */
    fun <T> constant(value: T): ScalarExpression<T> = ScalarConstant(value)

    /**
     * 创建引用表达弌
     * Create reference expression
     *
     * @param path 属性路径 / Property path
     * @return 标量表达式 / Scalar expression
     */
    fun <T> reference(path: PropertyPath): ScalarExpression<T> = ScalarReference(path)

    /**
     * 创建引用表达弌
     * Create reference expression
     *
     * @param path 路径字符串 / Path string
     * @return 标量表达式 / Scalar expression
     */
    fun <T> reference(path: String): ScalarExpression<T> = reference(PropertyPath.parse(path))

    /**
     * 创建符号引用表达式
     * Create symbol reference expression
     *
     * @param symbol 符号 / Symbol
     * @return 标量表达式 / Scalar expression
     */
    fun <T> reference(symbol: Symbol): ScalarExpression<T> = ScalarSymbolReference(symbol)

    /**
     * 创建一元操作表达式
     * Create unary operation expression
     *
     * @param operator 一元操作符 / Unary operator
     * @param operand 操作数 / Operand
     * @return 标量表达式 / Scalar expression
     */
    fun <T> unary(operator: UnaryOperator, operand: ScalarExpression<T>): ScalarExpression<T> =
        ScalarUnary(operator, operand)

    /**
     * 创建二元操作表达弌
     * Create binary operation expression
     *
     * @param operator 二元操作符 / Binary operator
     * @param left 左操作数 / Left operand
     * @param right 右操作数 / Right operand
     * @return 标量表达式 / Scalar expression
     */
    fun <T> binary(
        operator: BinaryOperator,
        left: ScalarExpression<T>,
        right: ScalarExpression<T>
    ): ScalarExpression<T> = ScalarBinary(operator, left, right)

    /**
     * 创建函数调用表达弌
     * Create function call expression
     *
     * @param name 函数名 / Function name
     * @param arguments 参数列表 / Argument list
     * @return 标量表达式 / Scalar expression
     */
    fun <T> function(name: String, arguments: List<ScalarExpression<T>>): ScalarExpression<T> =
        ScalarFunction(name, arguments)

    /**
     * 创建加法表达弌
     * Create addition expression
     *
     * @param left 左操作数 / Left operand
     * @param right 右操作数 / Right operand
     * @return 标量表达式 / Scalar expression
     */
    fun <T> add(left: ScalarExpression<T>, right: ScalarExpression<T>): ScalarExpression<T> =
        binary(BinaryOperator.Add, left, right)

    /**
     * 创建减法表达弌
     * Create subtraction expression
     *
     * @param left 左操作数 / Left operand
     * @param right 右操作数 / Right operand
     * @return 标量表达式 / Scalar expression
     */
    fun <T> subtract(left: ScalarExpression<T>, right: ScalarExpression<T>): ScalarExpression<T> =
        binary(BinaryOperator.Subtract, left, right)

    /**
     * 创建乘法表达弌
     * Create multiplication expression
     *
     * @param left 左操作数 / Left operand
     * @param right 右操作数 / Right operand
     * @return 标量表达式 / Scalar expression
     */
    fun <T> multiply(left: ScalarExpression<T>, right: ScalarExpression<T>): ScalarExpression<T> =
        binary(BinaryOperator.Multiply, left, right)

    /**
     * 创建除法表达弌
     * Create division expression
     *
     * @param left 左操作数 / Left operand
     * @param right 右操作数 / Right operand
     * @return 标量表达式 / Scalar expression
     */
    fun <T> divide(left: ScalarExpression<T>, right: ScalarExpression<T>): ScalarExpression<T> =
        binary(BinaryOperator.Divide, left, right)
}

/**
 * 标准标量函数名称
 * Standard scalar function names
 */
object ScalarFunctionNames {
    const val Abs: String = "abs"
    const val Lower: String = "lower"
    const val Upper: String = "upper"
    const val Trim: String = "trim"
    const val Length: String = "length"
    const val Coalesce: String = "coalesce"
}

/**
 * 标量函数注册表
 * Scalar function registry
 *
 * @param R 注册表使用的中间表示类型 / Intermediate representation type used by registry
 */
interface ScalarFunctionRegistry<R> {
    /**
     * 将标量函数调用翻译为目标表示
     * Translate scalar function call to target representation
     *
     * @param name 函数名 / Function name
     * @param arguments 参数列表 / Argument list
     * @return 翻译结果，不支持的函数返回 null / Translation result, null if function not supported
     */
    fun translate(name: String, arguments: List<R>): R?
}

/**
 * 标量函数求值器
 * Scalar function evaluator
 */
interface ScalarFunctionEvaluator {
    /**
     * 求值标量函数
     * Evaluate scalar function
     *
     * @param name 函数名 / Function name
     * @param arguments 参数列表 / Argument list
     * @return 求值结果 / Evaluation result
     */
    fun evaluate(name: String, arguments: List<Any?>): Any?
}
