/**
 * 表达式树变换工具 / Expression tree transformation utilities
 *
 * 仅重建表达式节点，不改变表达式的求值语义或共享模型边界。
 * Rebuilds expression nodes without changing evaluation semantics or the shared model boundary.
 */
package fuookami.ospf.kotlin.math.symbol.expression

/**
 * 递归变换标量表达式 / Recursively transform a scalar expression
 *
 * 子节点先变换，当前节点最后交给回调，便于实现结构保持的后序变换。
 * Children are transformed first and the current node is passed to the callback last.
 *
 * @param transformer 节点变换回调 / Node transformation callback
 * @return 变换后的表达式 / Transformed expression
 */
@Suppress("UNCHECKED_CAST")
fun <T> ScalarExpression<T>.transform(
    transformer: (ScalarExpression<*>) -> ScalarExpression<*>
): ScalarExpression<T> {
    val rebuilt: ScalarExpression<*> = when (this) {
        is ScalarConstant<*>, is ScalarReference<*>, is ScalarSymbolReference<*>, is ScalarCustom<*> -> this
        is ScalarUnary<*> -> ScalarUnary(
            operator = operator,
            operand = operand.transform(transformer)
        )
        is ScalarBinary<*> -> ScalarBinary(
            operator = operator,
            left = left.transform(transformer),
            right = right.transform(transformer)
        )
        is ScalarFunction<*> -> ScalarFunction(
            name = name,
            arguments = arguments.map { it.transform(transformer) }
        )
        is ScalarConditional<*> -> ScalarConditional(
            condition = condition.transformScalars(transformer).transformBooleans { it },
            thenBranch = thenBranch.transform(transformer),
            elseBranch = elseBranch.transform(transformer)
        )
        is ScalarBoolean<*> -> ScalarBoolean<Any?>(
            expr.transformScalars(transformer).transformBooleans { it }
        )
    }
    return transformer(rebuilt) as ScalarExpression<T>
}

/**
 * 递归变换布尔表达式 / Recursively transform a boolean expression
 *
 * 标量子树保持不变；需要同时改写标量和布尔节点时，请先调用 [transformScalars]。
 * Scalar subtrees are preserved; call [transformScalars] first when both scalar and boolean nodes need rewriting.
 *
 * @param transformer 节点变换回调 / Node transformation callback
 * @return 变换后的表达式 / Transformed expression
 */
fun BooleanExpression.transform(
    transformer: (BooleanExpression) -> BooleanExpression
): BooleanExpression {
    return transformScalars { it }.transformBooleans(transformer)
}

/**
 * 递归变换布尔表达式中的标量节点 / Transform scalar nodes inside a boolean expression
 *
 * @param transformer 标量节点变换回调 / Scalar node transformation callback
 * @return 变换后的表达式 / Transformed expression
 */
fun BooleanExpression.transformScalars(
    transformer: (ScalarExpression<*>) -> ScalarExpression<*>
): BooleanExpression {
    return when (this) {
        is BooleanConstant, is NullCheck, is BooleanCustom -> this
        is Comparison<*> -> Comparison(
            operator = operator,
            left = left.transform(transformer),
            right = right.transform(transformer)
        )
        is InExpression<*> -> InExpression(
            value = value.transform(transformer),
            candidates = candidates.map { it.transform(transformer) },
            negated = negated
        )
        is PatternMatch<*> -> PatternMatch(
            value = value.transform(transformer),
            pattern = pattern.transform(transformer),
            mode = mode,
            negated = negated
        )
        is AndExpression -> AndExpression(operands.map { it.transformScalars(transformer) })
        is OrExpression -> OrExpression(operands.map { it.transformScalars(transformer) })
        is NotExpression -> NotExpression(operand.transformScalars(transformer))
    }
}

/**
 * 递归变换布尔表达式节点 / Transform boolean nodes recursively
 *
 * @param transformer 节点变换回调 / Node transformation callback
 * @return 变换后的表达式 / Transformed expression
 */
fun BooleanExpression.transformBooleans(
    transformer: (BooleanExpression) -> BooleanExpression
): BooleanExpression {
    val rebuilt = when (this) {
        is BooleanConstant, is NullCheck, is BooleanCustom, is Comparison<*>, is InExpression<*>, is PatternMatch<*> -> this
        is AndExpression -> AndExpression(operands.map { it.transformBooleans(transformer) })
        is OrExpression -> OrExpression(operands.map { it.transformBooleans(transformer) })
        is NotExpression -> NotExpression(operand.transformBooleans(transformer))
    }
    return transformer(rebuilt)
}
