/**
 * 机制模型 Flt64 转换
 * Mechanism model Flt64 conversion
*/
package fuookami.ospf.kotlin.core.model.mechanism

import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.function.*
import fuookami.ospf.kotlin.core.symbol.SolverBoundaryCasts
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 在星投影线性函数符号上注册约束（不安全转换）。
 * Register constraints on a star-projected linear function symbol (unchecked cast).
 *
 * 委托给 SolverBoundaryCasts，集中唯一的 UNCHECKED_CAST 位置。
 * Delegates to SolverBoundaryCasts as the single UNCHECKED_CAST location.
 *
 * @param model 目标线性机制模型 / target linear mechanism model
 * @return 注册结果 / registration result
*/
internal fun MathFunctionSymbolBase<*>.registerConstraintsUnchecked(model: AbstractLinearMechanismModel<*>): Try {
    return SolverBoundaryCasts.registerConstraintsLinearStar(this, model)
}

/**
 * 在星投影二次函数符号上注册约束（不安全转换）。
 * Register constraints on a star-projected quadratic function symbol (unchecked cast).
 *
 * @param model 目标二次机制模型 / target quadratic mechanism model
 * @return 注册结果 / registration result
*/
internal fun QuadraticMathFunctionSymbolBase<*>.registerConstraintsUnchecked(model: AbstractQuadraticMechanismModel<*>): Try {
    return SolverBoundaryCasts.registerConstraintsQuadraticStar(this, model)
}

/** 复制令牌并将其值转换器替换为 Flt64 恒等转换器 / Copy a token and replace its value converter with the Flt64 identity converter */
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

/** 从泛型令牌表创建 Flt64 令牌表 / Create an Flt64 token table from a generic token table */
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

/** 复制可变令牌表为 Flt64 类型（不安全转换） / Copy a mutable token table as Flt64 type (unchecked cast) */
@Suppress("UNCHECKED_CAST")
internal fun <V> copyMutableTokenTableAsFlt64(tokens: MutableTokenTable<V>): MutableTokenTable<Flt64>
        where V : RealNumber<V>, V : NumberField<V> {
    return tokens.copy() as MutableTokenTable<Flt64>
}

/** 复制并发可变令牌表为 Flt64 类型（不安全转换） / Copy a concurrent mutable token table as Flt64 type (unchecked cast) */
@Suppress("UNCHECKED_CAST")
internal fun <V> copyConcurrentMutableTokenTableAsFlt64(tokens: ConcurrentMutableTokenTable<V>): ConcurrentMutableTokenTable<Flt64>
        where V : RealNumber<V>, V : NumberField<V> {
    return tokens.copy() as ConcurrentMutableTokenTable<Flt64>
}

/** 将线性子目标转换为 Flt64 类型 / Convert a linear sub-objective to Flt64 type */
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

/** 将二次子目标转换为 Flt64 类型 / Convert a quadratic sub-objective to Flt64 type */
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

/** 将线性约束转换为 Flt64 类型 / Convert a linear constraint to Flt64 type */
private fun <V> convertLinearConstraintToFlt64(
    constraint: LinearConstraintImpl<V>,
    tokens: AbstractTokenTable<Flt64>
): Ret<LinearConstraintImpl<Flt64>> where V : RealNumber<V>, V : NumberField<V> {
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

/** 将二次约束转换为 Flt64 类型 / Convert a quadratic constraint to Flt64 type */
private fun <V> convertQuadraticConstraintToFlt64(
    constraint: QuadraticConstraintImpl<V>,
    tokens: AbstractTokenTable<Flt64>
): Ret<QuadraticConstraintImpl<Flt64>> where V : RealNumber<V>, V : NumberField<V> {
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

/** 将线性机制模型整体转换为 Flt64 类型 / Convert an entire linear mechanism model to Flt64 type */
private fun <V> convertLinearMechanismModelToFlt64(model: LinearMechanismModel<V>): Ret<LinearMechanismModel<Flt64>>
        where V : RealNumber<V>, V : NumberField<V> {
    val flt64Tokens = createFlt64TokenTable(model.tokens)
    val flt64Parent = LinearMetaModel<Flt64>(
        name = model.parent.name,
        objectCategory = model.parent.objectCategory,
        configuration = model.parent.configuration,
        converter = IntoValue.Identity
    )
    val flt64Constraints = ArrayList<LinearConstraintImpl<Flt64>>()
    for (constraint in model.linearConstraints) {
        when (val result = convertLinearConstraintToFlt64(constraint, flt64Tokens)) {
            is Ok -> flt64Constraints.add(result.value)
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
    }
    val flt64SubObjects = model.objectFunction.subObjects.map { convertLinearSubObjectToFlt64(it, flt64Tokens) }
    return Ok(LinearMechanismModel(
        parent = flt64Parent,
        name = model.name,
        constraints = flt64Constraints,
        objectFunction = SingleObject(model.objectFunction.category, flt64SubObjects),
        tokens = flt64Tokens
    ))
}

/** 将二次机制模型整体转换为 Flt64 类型 / Convert an entire quadratic mechanism model to Flt64 type */
private fun <V> convertQuadraticMechanismModelToFlt64(model: QuadraticMechanismModel<V>): Ret<QuadraticMechanismModel<Flt64>>
        where V : RealNumber<V>, V : NumberField<V> {
    val flt64Tokens = createFlt64TokenTable(model.tokens)
    val flt64Parent = QuadraticMetaModel<Flt64>(
        name = model.parent.name,
        objectCategory = model.parent.objectCategory,
        configuration = model.parent.configuration,
        converter = IntoValue.Identity
    )
    val flt64Constraints = ArrayList<QuadraticConstraintImpl<Flt64>>()
    for (constraint in model.quadraticConstraints) {
        when (val result = convertQuadraticConstraintToFlt64(constraint, flt64Tokens)) {
            is Ok -> flt64Constraints.add(result.value)
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
    }
    val flt64SubObjects = model.objectFunction.subObjects.map { convertQuadraticSubObjectToFlt64(it, flt64Tokens) }
    return Ok(QuadraticMechanismModel(
        parent = flt64Parent,
        name = model.name,
        constraints = flt64Constraints,
        objectFunction = SingleObject(model.objectFunction.category, flt64SubObjects),
        tokens = flt64Tokens
    ))
}

/** 将令牌表不安全转换为目标数值类型 / Unchecked-cast a token table to the target numeric type */
@Suppress("UNCHECKED_CAST")
internal fun <V, T> mechanismTokenTableAs(table: AbstractTokenTable<T>): AbstractTokenTable<V>
        where V : RealNumber<V>, V : NumberField<V>, T : RealNumber<T>, T : NumberField<T> {
    return table as AbstractTokenTable<V>
}

/** 将固定变量映射转换为求解器边界所需的 Symbol->Flt64 映射 / Convert fixed-variable map to Symbol->Flt64 map for solver boundary */
internal fun <V> toSolverFixedValues(
    fixedVariables: Map<AbstractVariableItem<*, *>, V>?,
    toFlt64: (V) -> Flt64
): Map<Symbol, Flt64>? {
    return fixedVariables
        ?.mapValues { (_, value) -> toFlt64(value) }
        ?.mapKeys { (variable, _) -> variable as Symbol }
}

/** 将固定变量映射的值转换为 Flt64 类型 / Convert fixed-variable map values to Flt64 type */
internal fun <V> toFlt64FixedVariables(
    fixedVariables: Map<AbstractVariableItem<*, *>, V>,
    toFlt64: (V) -> Flt64
): Map<AbstractVariableItem<*, *>, Flt64> {
    return fixedVariables.mapValues { (_, value) -> toFlt64(value) }
}

/**
 * 将类型化 MechanismModel<V> 转换为 Flt64 求解器边界模型。
 * Convert a generic MechanismModel<V> to the Flt64 solver-boundary model.
 *
 * 转换前会验证模型是具体机制模型子类，未知类型返回 Failed。
 * Validates concrete mechanism-model subclasses and returns Failed for unexpected types.
*/
internal fun <V> convertMechanismModelToFlt64(model: MechanismModel<V>): Ret<MechanismModel<Flt64>>
        where V : RealNumber<V>, V : NumberField<V> {
    return when (model) {
        is LinearMechanismModel<*> -> {
            @Suppress("UNCHECKED_CAST")
            convertLinearMechanismModelToFlt64(model as LinearMechanismModel<V>)
        }

        is QuadraticMechanismModel<*> -> {
            @Suppress("UNCHECKED_CAST")
            convertQuadraticMechanismModelToFlt64(model as QuadraticMechanismModel<V>)
        }

        else -> {
            Failed(Err(ErrorCode.IllegalArgument, "Cannot convert MechanismModel<V> to Flt64: unexpected model type ${model::class.simpleName}"))
        }
    }
}
