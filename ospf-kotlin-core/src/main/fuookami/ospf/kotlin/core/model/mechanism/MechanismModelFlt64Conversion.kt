/**
 * 机制模型 Flt64 转换
 * Mechanism model Flt64 conversion
 */
package fuookami.ospf.kotlin.core.model.mechanism

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.function.*
import fuookami.ospf.kotlin.core.symbol.SolverBoundaryCasts
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem

// 求解器边界转换：在星投影函数符号上注册约束。 / Solver-boundary conversion: register constraints on star-projected function symbols.
// 委托给 SolverBoundaryCasts，集中唯一的 UNCHECKED_CAST 位置。 / Delegates to SolverBoundaryCasts as the single UNCHECKED_CAST location.
internal fun MathFunctionSymbolBase<*>.registerConstraintsUnchecked(model: AbstractLinearMechanismModel<*>): Try {
    return SolverBoundaryCasts.registerConstraintsLinearStar(this, model)
}

internal fun QuadraticMathFunctionSymbolBase<*>.registerConstraintsUnchecked(model: AbstractQuadraticMechanismModel<*>): Try {
    return SolverBoundaryCasts.registerConstraintsQuadraticStar(this, model)
}

private fun <V> copyTokenWithFlt64Converter(token: Token<V>): Token<Flt64>
        where V : RealNumber<V>, V : NumberField<V> {
    val copied = Token<Flt64>(
        variable = token.variable,
        solverIndex = token.solverIndex,
        refreshCallbacks = mutableMapOf<AbstractTokenList<Flt64>, (Boolean) -> Unit>(),
        converter = IntoValue.Identity
    )
    copied.__result = token.resultFlt64
    return copied
}

private fun <V> createFlt64TokenTable(tokens: AbstractTokenTable<V>): AbstractTokenTable<Flt64>
        where V : RealNumber<V>, V : NumberField<V> {
    val copiedTokens = tokens.tokens.map { copyTokenWithFlt64Converter(it) }
    val copiedTokenMap = copiedTokens.associateBy { it.key }
    val copiedTokenList = TokenList(copiedTokenMap)
    return TokenTable(
        category = tokens.category,
        tokenList = copiedTokenList,
        symbols = tokens.symbols.toList()
    )
}

@Suppress("UNCHECKED_CAST")
internal fun <V> copyMutableTokenTableAsFlt64(tokens: MutableTokenTable<V>): MutableTokenTable<Flt64>
        where V : RealNumber<V>, V : NumberField<V> {
    return tokens.copy() as MutableTokenTable<Flt64>
}

@Suppress("UNCHECKED_CAST")
internal fun <V> copyConcurrentMutableTokenTableAsFlt64(tokens: ConcurrentMutableTokenTable<V>): ConcurrentMutableTokenTable<Flt64>
        where V : RealNumber<V>, V : NumberField<V> {
    return tokens.copy() as ConcurrentMutableTokenTable<Flt64>
}

private fun <V> convertLinearSubObjectToFlt64(
    subObject: LinearSubObject<V>,
    tokens: AbstractTokenTable<Flt64>
): LinearSubObject<Flt64> where V : RealNumber<V>, V : NumberField<V> {
    val flattenData = LinearFlattenData(
        monomials = subObject.linearTerms().map { (coefficient, symbol) ->
            LinearMonomial(
                coefficient = coefficient.toFlt64(),
                symbol = symbol
            )
        },
        constant = subObject.constant.toFlt64()
    )
    return LinearSubObject.invoke(
        category = subObject.category,
        flattenData = flattenData,
        tokens = tokens,
        name = subObject.name,
        converter = IntoValue.Identity
    )
}

private fun <V> convertQuadraticSubObjectToFlt64(
    subObject: QuadraticSubObject<V>,
    tokens: AbstractTokenTable<Flt64>
): QuadraticSubObject<Flt64> where V : RealNumber<V>, V : NumberField<V> {
    val flattenData = QuadraticFlattenData(
        monomials = subObject.quadraticTerms().map { (coefficient, symbol1, symbol2) ->
            if (symbol2 == null) {
                QuadraticMonomial.linear(
                    coefficient = coefficient.toFlt64(),
                    symbol = symbol1
                )
            } else {
                QuadraticMonomial.quadratic(
                    coefficient = coefficient.toFlt64(),
                    symbol1 = symbol1,
                    symbol2 = symbol2
                )
            }
        },
        constant = subObject.constant.toFlt64()
    )
    return QuadraticSubObject.invoke(
        category = subObject.category,
        flattenData = flattenData,
        tokens = tokens,
        name = subObject.name,
        converter = IntoValue.Identity
    )
}

private fun <V> convertLinearConstraintToFlt64(
    constraint: LinearConstraintImpl<V>,
    tokens: AbstractTokenTable<Flt64>
): LinearConstraintImpl<Flt64> where V : RealNumber<V>, V : NumberField<V> {
    val relation = LinearRelationImpl(
        flattenData = LinearFlattenData(
            monomials = constraint.lhs.map { cell ->
                LinearMonomial(
                    coefficient = cell.coefficient.toFlt64(),
                    symbol = cell.token.variable
                )
            },
            constant = -constraint.rhs.toFlt64()
        ),
        sign = constraint.sign.toComparison(),
        name = constraint.name
    )
    return LinearConstraintImpl(
        relation = relation,
        tokens = tokens,
        converter = IntoValue.Identity,
        lazy = constraint.lazy,
        name = constraint.name,
        origin = constraint.origin,
        from = constraint.from
    )
}

private fun <V> convertQuadraticConstraintToFlt64(
    constraint: QuadraticConstraintImpl<V>,
    tokens: AbstractTokenTable<Flt64>
): QuadraticConstraintImpl<Flt64> where V : RealNumber<V>, V : NumberField<V> {
    val relation = QuadraticRelationImpl(
        flattenData = QuadraticFlattenData(
            monomials = constraint.lhs.map { cell ->
                if (cell.token2 == null) {
                    QuadraticMonomial.linear(
                        coefficient = cell.coefficient.toFlt64(),
                        symbol = cell.token1.variable
                    )
                } else {
                    QuadraticMonomial.quadratic(
                        coefficient = cell.coefficient.toFlt64(),
                        symbol1 = cell.token1.variable,
                        symbol2 = cell.token2!!.variable
                    )
                }
            },
            constant = -constraint.rhs.toFlt64()
        ),
        sign = constraint.sign.toComparison(),
        name = constraint.name
    )
    return QuadraticConstraintImpl(
        relation = relation,
        tokens = tokens,
        converter = IntoValue.Identity,
        lazy = constraint.lazy,
        name = constraint.name,
        origin = constraint.origin,
        from = constraint.from
    )
}

private fun <V> convertLinearMechanismModelToFlt64(model: LinearMechanismModel<V>): LinearMechanismModel<Flt64>
        where V : RealNumber<V>, V : NumberField<V> {
    val flt64Tokens = createFlt64TokenTable(model.tokens)
    val flt64Parent = LinearMetaModel<Flt64>(
        name = model.parent.name,
        objectCategory = model.parent.objectCategory,
        configuration = model.parent.configuration,
        converter = IntoValue.Identity
    )
    val flt64Constraints = model.linearConstraints.map { convertLinearConstraintToFlt64(it, flt64Tokens) }
    val flt64SubObjects = model.objectFunction.subObjects.map { convertLinearSubObjectToFlt64(it, flt64Tokens) }
    return LinearMechanismModel(
        parent = flt64Parent,
        name = model.name,
        constraints = flt64Constraints,
        objectFunction = SingleObject(model.objectFunction.category, flt64SubObjects),
        tokens = flt64Tokens
    )
}

private fun <V> convertQuadraticMechanismModelToFlt64(model: QuadraticMechanismModel<V>): QuadraticMechanismModel<Flt64>
        where V : RealNumber<V>, V : NumberField<V> {
    val flt64Tokens = createFlt64TokenTable(model.tokens)
    val flt64Parent = QuadraticMetaModel<Flt64>(
        name = model.parent.name,
        objectCategory = model.parent.objectCategory,
        configuration = model.parent.configuration,
        converter = IntoValue.Identity
    )
    val flt64Constraints = model.quadraticConstraints.map { convertQuadraticConstraintToFlt64(it, flt64Tokens) }
    val flt64SubObjects = model.objectFunction.subObjects.map { convertQuadraticSubObjectToFlt64(it, flt64Tokens) }
    return QuadraticMechanismModel(
        parent = flt64Parent,
        name = model.name,
        constraints = flt64Constraints,
        objectFunction = SingleObject(model.objectFunction.category, flt64SubObjects),
        tokens = flt64Tokens
    )
}

@Suppress("UNCHECKED_CAST")
internal fun <V, T> mechanismTokenTableAs(table: AbstractTokenTable<T>): AbstractTokenTable<V>
        where V : RealNumber<V>, V : NumberField<V>, T : RealNumber<T>, T : NumberField<T> {
    return table as AbstractTokenTable<V>
}

internal fun <V> toSolverFixedValues(
    fixedVariables: Map<AbstractVariableItem<*, *>, V>?,
    toFlt64: (V) -> Flt64
): Map<Symbol, Flt64>? {
    return fixedVariables
        ?.mapValues { (_, value) -> toFlt64(value) }
        ?.mapKeys { (variable, _) -> variable as Symbol }
}

internal fun <V> toFlt64FixedVariables(
    fixedVariables: Map<AbstractVariableItem<*, *>, V>,
    toFlt64: (V) -> Flt64
): Map<AbstractVariableItem<*, *>, Flt64> {
    return fixedVariables.mapValues { (_, value) -> toFlt64(value) }
}

/**
 * 将类型化 MechanismModel<V> 转换为 Flt64 求解器边界模型。
 * Convert a typed MechanismModel<V> to the Flt64 solver-boundary model.
 *
 * 转换前会验证模型是具体机制模型子类，未知类型返回 Failed。
 * Validates concrete mechanism-model subclasses and returns Failed for unexpected types.
 */
internal fun <V> convertMechanismModelToFlt64(model: MechanismModel<V>): Ret<MechanismModel<Flt64>>
        where V : RealNumber<V>, V : NumberField<V> {
    return when (model) {
        is LinearMechanismModel<*> -> {
            @Suppress("UNCHECKED_CAST")
            Ok(convertLinearMechanismModelToFlt64(model as LinearMechanismModel<V>))
        }

        is QuadraticMechanismModel<*> -> {
            @Suppress("UNCHECKED_CAST")
            Ok(convertQuadraticMechanismModelToFlt64(model as QuadraticMechanismModel<V>))
        }

        else -> {
            Failed(Err(ErrorCode.IllegalArgument, "Cannot convert MechanismModel<V> to Flt64: unexpected model type ${model::class.simpleName}"))
        }
    }
}
