/**
 * 标量表达式求值器
 * Scalar Expression Evaluator
 *
 * 提供标量表达式的本地求值能力，支持常量、引用、一元/二元操作、函数调用、条件表达式、布尔包装。
 * Provides local evaluation capability for scalar expressions,
 * supporting constant, reference, unary/binary operations, function calls, conditional, and boolean wrapper.
*/
package fuookami.ospf.kotlin.math.symbol.expression.operation

import fuookami.ospf.kotlin.math.symbol.expression.*
import fuookami.ospf.kotlin.math.Trivalent
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 求值标量表达式
 * Evaluate scalar expression
 *
 * @param expr 要求值的表达式 / Expression to evaluate
 * @param context 求值上下文 / Evaluation context
 * @param functionEvaluator 标量函数求值器 / Scalar function evaluator
 * @return 求值结果 / Evaluation result
*/
fun evaluateScalar(
    expr: ScalarExpression<*>,
    context: EvaluationContext,
    functionEvaluator: ScalarFunctionEvaluator = DefaultScalarFunctionEvaluator
): Ret<Any?> {
    return when (expr) {
        is ScalarConstant<*> -> Ok(expr.value)

        is ScalarReference<*> -> {
            val value = context[expr.path]
            Ok(value)
        }

        is ScalarSymbolReference<*> -> Failed(
            ErrorCode.IllegalArgument,
            "Cannot evaluate unbound symbol reference: ${expr.symbol.name}"
        )

        is ScalarUnary<*> -> evaluateScalarUnary(expr, context, functionEvaluator)

        is ScalarBinary<*> -> evaluateScalarBinary(expr, context, functionEvaluator)

        is ScalarFunction<*> -> evaluateScalarFunction(expr, context, functionEvaluator)

        is ScalarConditional<*> -> evaluateScalarConditional(expr, context, functionEvaluator)

        is ScalarBoolean<*> -> evaluateScalarBoolean(expr, context)

        is ScalarCustom<*> -> Failed(
            ErrorCode.IllegalArgument,
            "Cannot evaluate custom expression: ${expr.description ?: expr.value}"
        )
    }
}

// ========== 内部求值函数 / Internal Evaluation Functions ==========

/**
 * 求值一元操作
 * Evaluate unary operation
 *
 * @param expr the unary expression to evaluate / 待求值的一元表达式
 * @param context the evaluation context providing variable bindings / 提供变量绑定的求值上下文
 * @param functionEvaluator the evaluator for resolving function calls / 用于解析函数调用的求值器
 * @return the evaluated result of the unary operation / 一元运算的求值结果
*/
private fun evaluateScalarUnary(
    expr: ScalarUnary<*>,
    context: EvaluationContext,
    functionEvaluator: ScalarFunctionEvaluator
): Ret<Any?> {
    val operand = when (val result = evaluateScalar(expr.operand, context, functionEvaluator)) {
        is Ok -> result.value ?: return Failed(ErrorCode.ApplicationError, "Unary operand evaluated to null")
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }

    val result = NumericDispatcher.evaluateUnary(expr.operator, operand)
        ?: return Failed(ErrorCode.ApplicationError, "Unary operation ${expr.operator} not supported for type ${operand::class.simpleName}")

    return Ok(result)
}

/**
 * 求值二元操作
 * Evaluate binary operation
 *
 * @param expr the binary expression to evaluate / 待求值的二元表达式
 * @param context the evaluation context providing variable bindings / 提供变量绑定的求值上下文
 * @param functionEvaluator the evaluator for resolving function calls / 用于解析函数调用的求值器
 * @return the evaluated result of the binary operation / 二元运算的求值结果
*/
private fun evaluateScalarBinary(
    expr: ScalarBinary<*>,
    context: EvaluationContext,
    functionEvaluator: ScalarFunctionEvaluator
): Ret<Any?> {
    val left = when (val result = evaluateScalar(expr.left, context, functionEvaluator)) {
        is Ok -> result.value ?: return Failed(ErrorCode.ApplicationError, "Binary left operand evaluated to null")
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }

    val right = when (val result = evaluateScalar(expr.right, context, functionEvaluator)) {
        is Ok -> result.value ?: return Failed(ErrorCode.ApplicationError, "Binary right operand evaluated to null")
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }

    val result = NumericDispatcher.evaluateBinary(expr.operator, left, right)
        ?: return Failed(ErrorCode.ApplicationError, "Binary operation ${expr.operator} not supported for types ${left::class.simpleName} and ${right::class.simpleName}")

    return Ok(result)
}

/**
 * 求值函数调用
 * Evaluate function call
 *
 * @param expr the function call expression to evaluate / 待求值的函数调用表达式
 * @param context the evaluation context providing variable bindings / 提供变量绑定的求值上下文
 * @param functionEvaluator the evaluator for resolving function calls / 用于解析函数调用的求值器
 * @return the evaluated result of the function call / 函数调用的求值结果
*/
private fun evaluateScalarFunction(
    expr: ScalarFunction<*>,
    context: EvaluationContext,
    functionEvaluator: ScalarFunctionEvaluator
): Ret<Any?> {
    val args = mutableListOf<Any?>()
    for (argExpr in expr.arguments) {
        val argValue = when (val result = evaluateScalar(argExpr, context, functionEvaluator)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        args.add(argValue)
    }

    val result = functionEvaluator.evaluate(expr.name, args)
        ?: return Failed(ErrorCode.ApplicationError, "Unknown function: ${expr.name}")

    return Ok(result)
}

/**
 * 求值条件表达式
 * Evaluate conditional expression
 *
 * @param expr the conditional expression to evaluate / 待求值的条件表达式
 * @param context the evaluation context providing variable bindings / 提供变量绑定的求值上下文
 * @param functionEvaluator the evaluator for resolving function calls / 用于解析函数调用的求值器
 * @return the evaluated result of the selected branch / 所选分支的求值结果
*/
private fun evaluateScalarConditional(
    expr: ScalarConditional<*>,
    context: EvaluationContext,
    functionEvaluator: ScalarFunctionEvaluator
): Ret<Any?> {
    val conditionResult = evaluateBoolean(expr.condition, context)

    return when (conditionResult) {
        Trivalent.True -> evaluateScalar(expr.thenBranch, context, functionEvaluator)
        Trivalent.False -> evaluateScalar(expr.elseBranch, context, functionEvaluator)
        Trivalent.Unknown -> Failed(ErrorCode.ApplicationError, "Conditional expression evaluated to unknown")
    }
}

/**
 * 求值布尔包装表达式
 * Evaluate boolean wrapper expression
 *
 * @param expr the boolean wrapper expression to evaluate / 待求值的布尔包装表达式
 * @param context the evaluation context providing variable bindings / 提供变量绑定的求值上下文
 * @return the evaluated boolean result as a Kotlin Boolean / 求值后的布尔结果（Kotlin Boolean）
*/
private fun evaluateScalarBoolean(
    expr: ScalarBoolean<*>,
    context: EvaluationContext
): Ret<Any?> {
    val result = evaluateBoolean(expr.expr, context)
    return when (result) {
        Trivalent.True -> Ok(true)
        Trivalent.False -> Ok(false)
        Trivalent.Unknown -> Failed(ErrorCode.ApplicationError, "Boolean expression evaluated to unknown")
    }
}

// ========== 便捷扩展函数 / Convenience Extension Functions ==========

/**
 * 使用 Map 上下文求值标量表达式
 * Evaluate scalar expression with Map context
 *
 * @param values 字符串路径到值的映射 / String path to value mapping
 * @param functionEvaluator 标量函数求值器 / Scalar function evaluator
 * @return 求值结果 / Evaluation result
*/
fun ScalarExpression<*>.evaluateWith(
    values: Map<String, Any?>,
    functionEvaluator: ScalarFunctionEvaluator = DefaultScalarFunctionEvaluator
): Ret<Any?> {
    return evaluateScalar(this, MapEvaluationContext.fromStringMap(values), functionEvaluator)
}
