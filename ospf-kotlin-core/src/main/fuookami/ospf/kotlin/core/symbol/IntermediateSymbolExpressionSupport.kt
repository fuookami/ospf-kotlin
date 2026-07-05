@file:Suppress("unused")

/**
 * 中间符号表达式支持 / Intermediate symbol expression support
 *
 * 提供中间符号的表达式求值、缓存和求解器边界支持。
 * Provides expression evaluation, caching, and solver boundary support for intermediate symbols.
 */
package fuookami.ospf.kotlin.core.symbol

import fuookami.ospf.kotlin.core.model.basic.ExpressionRange
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.IdentifierGenerator
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.quantities.unit.reciprocal

/**
 * IntermediateSymbol 表达式求值与缓存支持。
 * IntermediateSymbol expression evaluation and cache support.
 *
 * 说明：该文件统一维护 solver 边界上的 Flt64 视图、缓存命中策略与回写路径。
 * Note: this file centralizes Flt64 boundary views, cache-hit policy, and write-back paths at solver boundaries.
 *
 * 非目标：不在此处定义兼容层桥接 API；跨类型转换统一走显式 converter 与 SolverBoundaryCasts。
 * Non-goal: compatibility bridge APIs are not defined here; cross-type conversions must go through explicit converters and SolverBoundaryCasts.
 */

/**
 * 判断是否需要准备符号（使用指定缓存键）。
 * Determine whether the symbol needs preparation (with specified cache key).
 *
 * @param cacheKey 缓存键 / Cache key
 * @param values 固定值映射（可空） / Fixed values map (nullable)
 * @param tokenTable 令牌表 / Token table
 * @return 是否需要准备 / Whether preparation is needed
 */
internal fun IntermediateSymbol<*>.shouldPrepare(
    cacheKey: IntermediateSymbol<*>,
    values: Map<Symbol, Flt64>?,
    tokenTable: AbstractTokenTable<Flt64>
): Boolean {
    val tt = tokenTable
    return (!values.isNullOrEmpty() || tt.cachedSolution) && if (values.isNullOrEmpty()) {
        tt.cached(cacheKey)
    } else {
        tt.cached(cacheKey, values)
    } == false
}

/**
 * 判断是否需要准备符号（使用自身作为缓存键）。
 * Determine whether the symbol needs preparation (using self as cache key).
 *
 * @param values 固定值映射（可空） / Fixed values map (nullable)
 * @param tokenTable 令牌表 / Token table
 * @return 是否需要准备 / Whether preparation is needed
 */
internal fun IntermediateSymbol<*>.shouldPrepare(
    values: Map<Symbol, Flt64>?,
    tokenTable: AbstractTokenTable<Flt64>
): Boolean {
    return shouldPrepare(this, values, tokenTable)
}

/**
 * 判断是否需要准备符号（使用固定缓存键，不区分值集合）。
 * Determine whether the symbol needs preparation (with fixed cache key, ignoring value set).
 *
 * @param cacheKey 缓存键 / Cache key
 * @param values 固定值映射（可空） / Fixed values map (nullable)
 * @param tokenTable 令牌表 / Token table
 * @return 是否需要准备 / Whether preparation is needed
 */
internal fun IntermediateSymbol<*>.shouldPrepareWithFixedCacheKey(
    cacheKey: IntermediateSymbol<*>,
    values: Map<Symbol, Flt64>?,
    tokenTable: AbstractTokenTable<Flt64>
): Boolean {
    return (!values.isNullOrEmpty() || tokenTable.cachedSolution) && tokenTable.cached(cacheKey) == false
}

/**
 * 判断是否需要准备符号（使用自身作为固定缓存键）。
 * Determine whether the symbol needs preparation (using self as fixed cache key).
 *
 * @param values 固定值映射（可空） / Fixed values map (nullable)
 * @param tokenTable 令牌表 / Token table
 * @return 是否需要准备 / Whether preparation is needed
 */
internal fun IntermediateSymbol<*>.shouldPrepareWithFixedCacheKey(
    values: Map<Symbol, Flt64>?,
    tokenTable: AbstractTokenTable<Flt64>
): Boolean {
    return shouldPrepareWithFixedCacheKey(this, values, tokenTable)
}

/**
 * 若未缓存则准备符号并执行回调（使用指定缓存键）。
 * Prepare symbol and execute callback if not cached (with specified cache key).
 *
 * @param cacheKey 缓存键 / Cache key
 * @param values 固定值映射（可空） / Fixed values map (nullable)
 * @param tokenTable 令牌表 / Token table
 * @param block 求值回调 / Evaluation callback
 * @return 回调结果，若已缓存则返回 null / Callback result, or null if cached
 */
internal inline fun <T> IntermediateSymbol<*>.prepareIfNotCached(
    cacheKey: IntermediateSymbol<*>,
    values: Map<Symbol, Flt64>?,
    tokenTable: AbstractTokenTable<Flt64>,
    block: () -> T?
): T? {
    return if (shouldPrepare(cacheKey, values, tokenTable)) {
        block()
    } else {
        null
    }
}

/**
 * 若未缓存则准备符号并执行回调（使用自身作为缓存键）。
 * Prepare symbol and execute callback if not cached (using self as cache key).
 *
 * @param values 固定值映射（可空） / Fixed values map (nullable)
 * @param tokenTable 令牌表 / Token table
 * @param block 求值回调 / Evaluation callback
 * @return 回调结果，若已缓存则返回 null / Callback result, or null if cached
 */
internal inline fun <T> IntermediateSymbol<*>.prepareIfNotCached(
    values: Map<Symbol, Flt64>?,
    tokenTable: AbstractTokenTable<Flt64>,
    block: () -> T?
): T? {
    return prepareIfNotCached(this, values, tokenTable, block)
}

/**
 * 若未缓存则准备符号并执行回调（使用固定缓存键）。
 * Prepare symbol and execute callback if not cached (with fixed cache key).
 *
 * @param cacheKey 缓存键 / Cache key
 * @param values 固定值映射（可空） / Fixed values map (nullable)
 * @param tokenTable 令牌表 / Token table
 * @param block 求值回调 / Evaluation callback
 * @return 回调结果，若已缓存则返回 null / Callback result, or null if cached
 */
internal inline fun <T> IntermediateSymbol<*>.prepareIfNotCachedWithFixedCacheKey(
    cacheKey: IntermediateSymbol<*>,
    values: Map<Symbol, Flt64>?,
    tokenTable: AbstractTokenTable<Flt64>,
    block: () -> T?
): T? {
    return if (shouldPrepareWithFixedCacheKey(cacheKey, values, tokenTable)) {
        block()
    } else {
        null
    }
}

/**
 * 若未缓存则准备符号并执行回调（使用自身作为固定缓存键）。
 * Prepare symbol and execute callback if not cached (using self as fixed cache key).
 *
 * @param values 固定值映射（可空） / Fixed values map (nullable)
 * @param tokenTable 令牌表 / Token table
 * @param block 求值回调 / Evaluation callback
 * @return 回调结果，若已缓存则返回 null / Callback result, or null if cached
 */
internal inline fun <T> IntermediateSymbol<*>.prepareIfNotCachedWithFixedCacheKey(
    values: Map<Symbol, Flt64>?,
    tokenTable: AbstractTokenTable<Flt64>,
    block: () -> T?
): T? {
    return prepareIfNotCachedWithFixedCacheKey(this, values, tokenTable, block)
}

/**
 * 使用令牌表缓存求值，若未缓存则执行计算器回调。
 * Evaluate with cached token table, execute calculator if not cached.
 *
 * @param tokenTable 令牌表 / Token table
 * @param converter 值转换器 / Value converter
 * @param zeroIfNone 无值时是否返回零 / Whether to return zero when value is absent
 * @param calculator 求值回调 / Evaluation callback
 * @return 求值结果 / Evaluation result
 */
private fun <V> IntermediateSymbol<V>.evaluateWithCachedTokenTable(
    tokenTable: AbstractTokenTable<V>,
    converter: IntoValue<V>,
    zeroIfNone: Boolean,
    calculator: () -> V?
): V? where V : RealNumber<V>, V : NumberField<V> {
    return if (tokenTable.cachedSolution) {
        tokenTable.cacheIfNotCached(
            cacheKey = this,
            solution = null
        ) {
            for (dependency in dependencies) {
                if (tokenTable.cachedSolution) {
                    SolverBoundaryCasts.dependencyAsIntermediate<V>(dependency).evaluate(
                        tokenTable = tokenTable,
                        converter = converter,
                        zeroIfNone = zeroIfNone
                    )
                }
            }
            calculator()
        }
    } else {
        tokenTable.cachedValue(
            cacheKey = this,
            solution = null
        )
    }
}

/**
 * 使用求解器结果列表和令牌表缓存求值，若未缓存则执行计算器回调。
 * Evaluate with solver results list and cached token table, execute calculator if not cached.
 *
 * @param results 求解器结果列表 / Solver results list
 * @param tokenTable 令牌表 / Token table
 * @param converter 值转换器 / Value converter
 * @param zeroIfNone 无值时是否返回零 / Whether to return zero when value is absent
 * @param calculator 求值回调 / Evaluation callback
 * @return 求值结果 / Evaluation result
 */
private fun <V> IntermediateSymbol<V>.evaluateWithCachedTokenTable(
    results: List<Flt64>,
    tokenTable: AbstractTokenTable<V>,
    converter: IntoValue<V>,
    zeroIfNone: Boolean,
    calculator: () -> V?
): V? where V : RealNumber<V>, V : NumberField<V> {
    return if (tokenTable.cachedSolution) {
        tokenTable.cacheSolverIfNotCached(this, results, {
            for (dependency in dependencies) {
                if (tokenTable.cachedSolution) {
                    val dep = SolverBoundaryCasts.dependencyAsIntermediate<V>(dependency)
                    when (dep) {
                        is LinearExpressionSymbol<V> -> dep.evaluateSolver(results, tokenTable, converter, zeroIfNone)
                        is QuadraticExpressionSymbol<V> -> dep.evaluateSolver(results, tokenTable, converter, zeroIfNone)
                    }
                }
            }
            calculator()
        }, converter)
    } else {
        tokenTable.cachedSolverValue(this, results, converter)
    }
}

/**
 * 使用固定值映射和令牌表缓存求值，若未缓存则执行计算器回调。
 * Evaluate with fixed values map and cached token table, execute calculator if not cached.
 *
 * @param values Flt64 固定值映射 / Flt64 fixed values map
 * @param tokenTable 令牌表 / Token table
 * @param converter 值转换器 / Value converter
 * @param zeroIfNone 无值时是否返回零 / Whether to return zero when value is absent
 * @param calculator 求值回调 / Evaluation callback
 * @return 求值结果 / Evaluation result
 */
private fun <V> IntermediateSymbol<V>.evaluateWithCachedTokenTable(
    values: Map<Symbol, Flt64>,
    tokenTable: AbstractTokenTable<V>,
    converter: IntoValue<V>,
    zeroIfNone: Boolean,
    calculator: () -> V?
): V? where V : RealNumber<V>, V : NumberField<V> {
    values[this]?.let { value ->
        tokenTable.cacheSolver(
            cacheKey = this,
            fixedValues = values,
            value = converter.intoValue(value),
            converter = converter
        )
        return converter.intoValue(value)
    }

    return if (values.isNotEmpty() || tokenTable.cachedSolution) {
        tokenTable.cacheSolverIfNotCached(this, values, {
            for (dependency in dependencies) {
                if (values.isNotEmpty() || tokenTable.cachedSolution) {
                    val dep = SolverBoundaryCasts.dependencyAsIntermediate<V>(dependency)
                    when (dep) {
                        is LinearExpressionSymbol<V> -> dep.evaluateSolver(values, tokenTable, converter, zeroIfNone)
                        is QuadraticExpressionSymbol<V> -> dep.evaluateSolver(values, tokenTable, converter, zeroIfNone)
                    }
                }
            }
            calculator()
        }, converter)
    } else {
        tokenTable.cachedSolverValue(this, values, converter)
    }
}

/**
 * 线性中间符号表达式，由线性多项式支持，可在求解器边界进行缓存求值。
 * Linear intermediate symbol expression backed by a linear polynomial, supporting cached evaluation at solver boundaries.
 *
 * @property _utilsPolynomial 可变线性多项式存储 / Mutable linear polynomial storage
 * @param category 符号类别 / Symbol category
 * @param parent 父级符号（可空） / Parent symbol (nullable)
 * @param name 符号名称 / Symbol name
 * @param displayName 显示名称（可空） / Display name (nullable)
 */
class LinearExpressionSymbol<V>(
    internal val _utilsPolynomial: MutableLinearPolynomial<V>,
    category: Category = Linear,
    parent: IntermediateSymbol<*>? = null,
    name: String = "",
    displayName: String? = null
) : LinearIntermediateSymbol<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
    internal var _group: AbstractSymbolCombination<*>? = null
    internal var _index: Int? = null
    override val identifier: UInt64 by lazy {
        _group?.identifier ?: IdentifierGenerator.gen()
    }
    override val index: Int by lazy {
        _index ?: 0
    }

    override val category: Category = category
    override val parent: IntermediateSymbol<*>? = parent
    override var name: String = name
    override var displayName: String? = displayName

    override val operationCategory: Category = Linear

    companion object {
        /**
         * 从变量项创建线性表达式符号。
         * Create linear expression symbol from variable item.
         *
         * @param item 变量项 / Variable item
         * @param constants 实数常量定义 / Real number constants definition
         * @param parent 父级符号（可空） / Parent symbol (nullable)
         * @param name 符号名称 / Symbol name
         * @param displayName 显示名称（可空） / Display name (nullable)
         * @return 线性表达式符号 / Linear expression symbol
         */
        operator fun <V> invoke(
            item: AbstractVariableItem<*, *>,
            constants: RealNumberConstants<V>,
            parent: IntermediateSymbol<*>? = null,
            name: String = "",
            displayName: String? = null
        ): LinearExpressionSymbol<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
            return LinearExpressionSymbol(
                _utilsPolynomial = MutableLinearPolynomial(
                    monomials = listOf(LinearMonomial(constants.one, item)),
                    constant = constants.zero
                ),
                category = Linear,
                parent = parent,
                name = name.ifEmpty { item.name },
                displayName = displayName
            )
        }

        /**
         * 从线性中间符号创建线性表达式符号。
         * Create linear expression symbol from linear intermediate symbol.
         *
         * @param symbol 线性中间符号 / Linear intermediate symbol
         * @param constants 实数常量定义 / Real number constants definition
         * @param parent 父级符号（可空） / Parent symbol (nullable)
         * @param name 符号名称 / Symbol name
         * @param displayName 显示名称（可空） / Display name (nullable)
         * @return 线性表达式符号 / Linear expression symbol
         */
        operator fun <V> invoke(
            symbol: LinearIntermediateSymbol<*>,
            constants: RealNumberConstants<V>,
            parent: IntermediateSymbol<*>? = null,
            name: String = "",
            displayName: String? = null
        ): LinearExpressionSymbol<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
            return LinearExpressionSymbol(
                _utilsPolynomial = MutableLinearPolynomial(
                    monomials = listOf(LinearMonomial(constants.one, symbol)),
                    constant = constants.zero
                ),
                category = Linear,
                parent = parent,
                name = name.ifEmpty { symbol.name },
                displayName = displayName ?: symbol.displayName
            )
        }

        /**
         * 从不可变线性多项式创建线性表达式符号。
         * Create linear expression symbol from immutable linear polynomial.
         *
         * @param polynomial 线性多项式 / Linear polynomial
         * @param parent 父级符号（可空） / Parent symbol (nullable)
         * @param name 符号名称 / Symbol name
         * @param displayName 显示名称（可空） / Display name (nullable)
         * @return 线性表达式符号 / Linear expression symbol
         */
        operator fun <V> invoke(
            polynomial: LinearPolynomial<V>,
            parent: IntermediateSymbol<*>? = null,
            name: String = "",
            displayName: String? = null
        ): LinearExpressionSymbol<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
            return LinearExpressionSymbol(
                _utilsPolynomial = MutableLinearPolynomial(
                    monomials = polynomial.monomials,
                    constant = polynomial.constant
                ),
                category = Linear,
                parent = parent,
                name = name.ifEmpty { polynomial.toString() },
                displayName = displayName
            )
        }

        /**
         * 从线性单项式创建线性表达式符号。
         * Create linear expression symbol from linear monomial.
         *
         * @param monomial 线性单项式 / Linear monomial
         * @param parent 父级符号（可空） / Parent symbol (nullable)
         * @param name 符号名称 / Symbol name
         * @param displayName 显示名称（可空） / Display name (nullable)
         * @return 线性表达式符号 / Linear expression symbol
         */
        operator fun <V> invoke(
            monomial: LinearMonomial<V>,
            parent: IntermediateSymbol<*>? = null,
            name: String = "",
            displayName: String? = null
        ): LinearExpressionSymbol<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
            return LinearExpressionSymbol(
                _utilsPolynomial = MutableLinearPolynomial(
                    monomials = listOf(monomial),
                    constant = monomial.coefficient - monomial.coefficient
                ),
                category = Linear,
                parent = parent,
                name = name.ifEmpty { monomial.toString() },
                displayName = displayName
            )
        }

        /**
         * 从可变线性多项式创建线性表达式符号。
         * Create linear expression symbol from mutable linear polynomial.
         *
         * @param polynomial 可变线性多项式 / Mutable linear polynomial
         * @param parent 父级符号（可空） / Parent symbol (nullable)
         * @param name 符号名称 / Symbol name
         * @param displayName 显示名称（可空） / Display name (nullable)
         * @return 线性表达式符号 / Linear expression symbol
         */
        operator fun <V> invoke(
            polynomial: MutableLinearPolynomial<V>,
            parent: IntermediateSymbol<*>? = null,
            name: String = "",
            displayName: String? = null
        ): LinearExpressionSymbol<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
            return LinearExpressionSymbol(
                _utilsPolynomial = polynomial,
                category = Linear,
                parent = parent,
                name = name.ifEmpty { polynomial.toString() },
                displayName = displayName
            )
        }

        /**
         * 从常量值创建线性表达式符号。
         * Create linear expression symbol from constant value.
         *
         * @param constant 常量值 / Constant value
         * @param parent 父级符号（可空） / Parent symbol (nullable)
         * @param name 符号名称 / Symbol name
         * @param displayName 显示名称（可空） / Display name (nullable)
         * @return 线性表达式符号 / Linear expression symbol
         */
        operator fun <V> invoke(
            constant: V,
            parent: IntermediateSymbol<*>? = null,
            name: String = "",
            displayName: String? = null
        ): LinearExpressionSymbol<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
            return LinearExpressionSymbol(
                _utilsPolynomial = MutableLinearPolynomial(
                    monomials = emptyList(),
                    constant = constant
                ),
                category = Linear,
                parent = parent,
                name = name,
                displayName = displayName
            )
        }

        /**
         * 从实数常量定义创建零值线性表达式符号。
         * Create zero-valued linear expression symbol from real number constants.
         *
         * @param constants 实数常量定义 / Real number constants definition
         * @param parent 父级符号（可空） / Parent symbol (nullable)
         * @param name 符号名称 / Symbol name
         * @param displayName 显示名称（可空） / Display name (nullable)
         * @return 零值线性表达式符号 / Zero-valued linear expression symbol
         */
        operator fun <V> invoke(
            constants: RealNumberConstants<V>,
            parent: IntermediateSymbol<*>? = null,
            name: String = "",
            displayName: String? = null
        ): LinearExpressionSymbol<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
            return LinearExpressionSymbol(
                _utilsPolynomial = MutableLinearPolynomial(
                    monomials = emptyList(),
                    constant = constants.zero
                ),
                category = Linear,
                parent = parent,
                name = name,
                displayName = displayName
            )
        }
    }

    // 求解器边界使用的 Flt64 视图。
    // Flt64 view used at the solver boundary.
    private val _polyFlt64: MutableLinearPolynomial<Flt64> get() = SolverBoundaryCasts.linearPolynomialAsFlt64(_utilsPolynomial)

    override val polynomial: LinearPolynomial<V> get() = _utilsPolynomial.toLinearPolynomial()

    /** 获取线性扁平化数据（单项式和常量）。 / Get linear flatten data (monomials and constant). */
    val flattenedMonomials: LinearFlattenData<V>
        get() = LinearFlattenData(
            monomials = _utilsPolynomial.monomials,
            constant = _utilsPolynomial.constant
        )

    override fun asMutable(): MutableLinearPolynomial<V> {
        return _utilsPolynomial
    }

    /**
     * 通过令牌表求值符号（求解器边界 Flt64 路径）。
     * Evaluate symbol via token table (solver boundary Flt64 path).
     *
     * @param symbol 待求值符号 / Symbol to evaluate
     * @param tokenTable 令牌表 / Token table
     * @param converter 值转换器 / Value converter
     * @param zeroIfNone 无值时是否返回零 / Whether to return zero when value is absent
     * @return Flt64 求值结果 / Flt64 evaluation result
     */
    private fun evaluateSymbol(symbol: Symbol, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): Flt64? {
        return when (symbol) {
            is AbstractVariableItem<*, *> -> {
                tokenTable.find(symbol)?.resultFlt64 ?: if (zeroIfNone) Flt64.zero else null
            }
            is LinearExpressionSymbol<*> -> {
                symbol.evaluate(SolverBoundaryCasts.tokenListAsFlt64(tokenTable), zeroIfNone)
            }
            else -> null
        }
    }

    /**
     * 通过求解器结果列表和令牌表求值符号。
     * Evaluate symbol via solver results list and token table.
     *
     * @param symbol 待求值符号 / Symbol to evaluate
     * @param results 求解器结果列表 / Solver results list
     * @param tokenTable 令牌表 / Token table
     * @param converter 值转换器 / Value converter
     * @param zeroIfNone 无值时是否返回零 / Whether to return zero when value is absent
     * @return Flt64 求值结果 / Flt64 evaluation result
     */
    private fun evaluateSymbol(symbol: Symbol, results: List<Flt64>, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): Flt64? {
        return when (symbol) {
            is AbstractVariableItem<*, *> -> {
                val index = tokenTable.indexOf(symbol)
                if (index != null && index >= 0 && index < results.size) results[index]
                else if (zeroIfNone) Flt64.zero else null
            }
            is LinearExpressionSymbol<*> -> {
                symbol.evaluate(results, SolverBoundaryCasts.tokenListAsFlt64(tokenTable), zeroIfNone)
            }
            else -> null
        }
    }

    /**
     * 通过固定值映射和可空令牌表求值符号。
     * Evaluate symbol via fixed values map and nullable token table.
     *
     * @param symbol 待求值符号 / Symbol to evaluate
     * @param values Flt64 固定值映射 / Flt64 fixed values map
     * @param tokenTable 令牌表（可空） / Token table (nullable)
     * @param converter 值转换器 / Value converter
     * @param zeroIfNone 无值时是否返回零 / Whether to return zero when value is absent
     * @return Flt64 求值结果 / Flt64 evaluation result
     */
    private fun evaluateSymbol(symbol: Symbol, values: Map<Symbol, Flt64>, tokenTable: AbstractTokenTable<V>?, converter: IntoValue<V>, zeroIfNone: Boolean): Flt64? {
        return when (symbol) {
            is AbstractVariableItem<*, *> -> {
                values[symbol] ?: tokenTable?.find(symbol)?.resultFlt64 ?: if (zeroIfNone) Flt64.zero else null
            }
            is LinearExpressionSymbol<*> -> {
                symbol.evaluate(values, SolverBoundaryCasts.tokenListAsFlt64OrNull(tokenTable), zeroIfNone)
            }
            else -> values[symbol] ?: if (zeroIfNone) Flt64.zero else null
        }
    }

    /**
     * 通过 Flt64 令牌列表直接求值符号。
     * Evaluate symbol directly via Flt64 token list.
     *
     * @param symbol 待求值符号 / Symbol to evaluate
     * @param tokenList Flt64 令牌列表 / Flt64 token list
     * @param zeroIfNone 无值时是否返回零 / Whether to return zero when value is absent
     * @return Flt64 求值结果 / Flt64 evaluation result
     */
    private fun evaluateSymbol(symbol: Symbol, tokenList: AbstractTokenList<Flt64>, zeroIfNone: Boolean): Flt64? {
        return when (symbol) {
            is AbstractVariableItem<*, *> -> {
                val token = tokenList.find(symbol)
                token?.resultFlt64 ?: if (zeroIfNone) Flt64.zero else null
            }
            is LinearExpressionSymbol<*> -> symbol.evaluate(tokenList, zeroIfNone)
            else -> null
        }
    }

    /**
     * 通过求解器结果列表和 Flt64 令牌列表求值符号。
     * Evaluate symbol via solver results list and Flt64 token list.
     *
     * @param symbol 待求值符号 / Symbol to evaluate
     * @param results 求解器结果列表 / Solver results list
     * @param tokenList Flt64 令牌列表 / Flt64 token list
     * @param zeroIfNone 无值时是否返回零 / Whether to return zero when value is absent
     * @return Flt64 求值结果 / Flt64 evaluation result
     */
    private fun evaluateSymbol(symbol: Symbol, results: List<Flt64>, tokenList: AbstractTokenList<Flt64>, zeroIfNone: Boolean): Flt64? {
        return when (symbol) {
            is AbstractVariableItem<*, *> -> {
                val index = tokenList.indexOf(symbol)
                if (index != null && index >= 0 && index < results.size) results[index]
                else if (zeroIfNone) Flt64.zero else null
            }
            is LinearExpressionSymbol<*> -> symbol.evaluate(results, tokenList, zeroIfNone)
            else -> null
        }
    }

    /**
     * 通过固定值映射和 Flt64 令牌列表求值符号。
     * Evaluate symbol via fixed values map and Flt64 token list.
     *
     * @param symbol 待求值符号 / Symbol to evaluate
     * @param values Flt64 固定值映射 / Flt64 fixed values map
     * @param tokenList Flt64 令牌列表（可空） / Flt64 token list (nullable)
     * @param zeroIfNone 无值时是否返回零 / Whether to return zero when value is absent
     * @return Flt64 求值结果 / Flt64 evaluation result
     */
    private fun evaluateSymbol(symbol: Symbol, values: Map<Symbol, Flt64>, tokenList: AbstractTokenList<Flt64>?, zeroIfNone: Boolean): Flt64? {
        return when (symbol) {
            is AbstractVariableItem<*, *> -> {
                values[symbol] ?: tokenList?.find(symbol)?.resultFlt64 ?: if (zeroIfNone) Flt64.zero else null
            }
            is LinearExpressionSymbol<*> -> symbol.evaluate(values, tokenList, zeroIfNone)
            else -> values[symbol] ?: if (zeroIfNone) Flt64.zero else null
        }
    }

    override val dependencies: Set<IntermediateSymbol<*>>
        get() = _utilsPolynomial.monomials
            .mapNotNull { monomial ->
                when (val sym = monomial.symbol) {
                    is LinearIntermediateSymbol<*> -> sym
                    else -> null
                }
            }
            .toSet()

    /**
     * 计算线性表达式的 Flt64 值域。
     * Calculate the Flt64 value range of the linear expression.
     *
     * @return Flt64 值域，若依赖符号无值域则返回 null / Flt64 value range, or null if dependency symbols have no range
     */
    private fun calculateRange(): ValueRange<Flt64>? {
        val poly = _polyFlt64
        var range: ValueRange<Flt64>? = ValueRange(poly.constant, Flt64).value
        for (monomial in poly.monomials) {
            val symRange = when (val sym = monomial.symbol) {
                is AbstractVariableItem<*, *> -> sym.range.valueRange
                is LinearIntermediateSymbol<*> -> sym.range.valueRange
                else -> null
            }
            if (symRange != null) {
                val scaled = monomial.coefficient * symRange
                range = range?.let { r -> scaled?.let { s -> r + s } }
            } else {
                range = null
                break
            }
        }
        return range
    }

    override val range: ExpressionRange<V>
        get() = SolverBoundaryCasts.expressionRangeFromFlt64(calculateRange())

    override val discrete: Boolean
        get() = _polyFlt64.monomials.all { monomial ->
            when (val sym = monomial.symbol) {
                is AbstractVariableItem<*, *> -> sym.type.isIntegerType && monomial.coefficient.round() eq monomial.coefficient
                is LinearIntermediateSymbol<*> -> sym.discrete && monomial.coefficient.round() eq monomial.coefficient
                else -> false
            }
        } && _polyFlt64.constant.round() eq _polyFlt64.constant

    private val rangeCacheKey = newTokenCacheKey(
        category = Linear,
        prefix = "__linear_expression_range_cache__"
    )
    private val flattenCacheKey = newTokenCacheKey(
        category = Linear,
        prefix = "__linear_expression_flatten_cache__"
    )
    /**
     * 从依赖符号中获取绑定的令牌表并转换为 Flt64 视图。
     * Retrieve the bound token table from dependencies and cast to Flt64 view.
     *
     * @return Flt64 令牌表，若无可用依赖则返回 null / Flt64 token table, or null if no dependency provides one
     */
    private fun cacheTokenTable(): AbstractTokenTable<Flt64>? {
        return SolverBoundaryCasts.tokenTableAsFlt64OrNull(
            dependencies
                .asSequence()
                .mapNotNull { boundTokenTableContext(it) }
                .firstOrNull()
        )
    }

    override val cached: Boolean
        get() {
            val tokenTable = cacheTokenTable()
            val cachedFlatten = tokenTable?.cachedLinearFlatten(flattenCacheKey)
            return cachedFlatten == true
        }

    override fun flush(force: Boolean) {
        val tokenTable = cacheTokenTable()
        val cachedRange = tokenTable?.cachedRangeValue(rangeCacheKey)
        if (force || cachedRange?.set == false) {
            tokenTable?.clearRange(rangeCacheKey)
        }
        for (dep in dependencies) {
            dep.flush(force)
        }
        if (force) {
            tokenTable?.clearLinearFlatten(flattenCacheKey)
        }
    }

    /**
     * 在求解器边界准备符号值，使用 Flt64 视图求值后转换回目标类型。
     * Prepare symbol value at solver boundary, evaluate using Flt64 view then convert back.
     *
     * @param values 固定值映射（可空） / Fixed values map (nullable)
     * @param tokenTable 令牌表 / Token table
     * @param converter 值转换器 / Value converter
     * @return 求值结果 / Evaluation result
     */
    internal fun prepareSolver(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>): V? {
        // 求解器求值前触发 flatten 视图创建。
        // Trigger flatten-view creation before solver evaluation.
        flattenedMonomials

        return if (values.isNullOrEmpty()) {
            var ret = _polyFlt64.constant
            for (monomial in _polyFlt64.monomials) {
                val symbolValue = evaluateSymbol(monomial.symbol, tokenTable, converter, false) ?: return null
                ret += monomial.coefficient * symbolValue
            }
            converter.intoValue(ret)
        } else {
            var ret = _polyFlt64.constant
            for (monomial in _polyFlt64.monomials) {
                val symbolValue = evaluateSymbol(monomial.symbol, values, tokenTable, converter, false) ?: return null
                ret += monomial.coefficient * symbolValue
            }
            converter.intoValue(ret)
        }
    }

    override fun prepare(values: Map<Symbol, V>?, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>): V? {
        val flt64Values = values?.mapValues { converter.fromValue(it.value) }
        return prepareSolver(flt64Values, tokenTable, converter)
    }

    override fun toRawString(unfold: UInt64): String {
        return if (unfold neq UInt64.zero) {
            val monomialStrings = _polyFlt64.monomials.filter { it.coefficient neq Flt64.zero }.map { m ->
                val symStr = when (val sym = m.symbol) {
                    is IntermediateSymbol<*> -> sym.toRawString(unfold - UInt64.one)
                    else -> sym.name
                }
                if (m.coefficient eq Flt64.one) symStr
                else if (m.coefficient eq -Flt64.one) "-$symStr"
                else "${m.coefficient} * $symStr"
            }
            if (_polyFlt64.constant neq Flt64.zero) {
                "${monomialStrings.joinToString(" + ")} + ${_polyFlt64.constant}"
            } else {
                monomialStrings.joinToString(" + ")
            }
        } else {
            displayName ?: name
        }
    }

    /**
     * 使用 Flt64 令牌列表直接求值。
     * Evaluate directly using Flt64 token list.
     *
     * @param tokenList Flt64 令牌列表 / Flt64 token list
     * @param zeroIfNone 无值时是否返回零 / Whether to return zero when value is absent
     * @return Flt64 求值结果 / Flt64 evaluation result
     */
    internal fun evaluate(tokenList: AbstractTokenList<Flt64>, zeroIfNone: Boolean): Flt64? {
        var ret = _polyFlt64.constant
        for (monomial in _polyFlt64.monomials) {
            val symbolValue = evaluateSymbol(monomial.symbol, tokenList, zeroIfNone)
            if (symbolValue == null && !zeroIfNone) return null
            ret += monomial.coefficient * (symbolValue ?: Flt64.zero)
        }
        return ret
    }

    override fun evaluate(tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        return evaluateWithCachedTokenTable(tokenTable, converter, zeroIfNone) {
            var ret = _polyFlt64.constant
            for (monomial in _polyFlt64.monomials) {
                val symbolValue = evaluateSymbol(monomial.symbol, tokenTable, converter, zeroIfNone)
                if (symbolValue == null && !zeroIfNone) return@evaluateWithCachedTokenTable null
                ret += monomial.coefficient * (symbolValue ?: Flt64.zero)
            }
            converter.intoValue(ret)
        }
    }

    override fun evaluate(results: List<V>, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        val flt64Results = results.map { converter.fromValue(it) }
        return evaluateSolver(flt64Results, tokenTable, converter, zeroIfNone)
    }

    override fun evaluate(values: Map<Symbol, V>, tokenTable: AbstractTokenTable<V>?, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        val flt64Values = values.mapValues { converter.fromValue(it.value) }
        return evaluateSolver(flt64Values, tokenTable, converter, zeroIfNone)
    }

    /**
     * 使用求解器结果列表在求解器边界求值。
     * Evaluate at solver boundary using solver results list.
     *
     * @param results 求解器结果列表 / Solver results list
     * @param tokenTable 令牌表 / Token table
     * @param converter 值转换器 / Value converter
     * @param zeroIfNone 无值时是否返回零 / Whether to return zero when value is absent
     * @return 求值结果 / Evaluation result
     */
    internal fun evaluateSolver(results: List<Flt64>, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        return evaluateWithCachedTokenTable(results, tokenTable, converter, zeroIfNone) {
            var ret = _polyFlt64.constant
            for (monomial in _polyFlt64.monomials) {
                val symbolValue = evaluateSymbol(monomial.symbol, results, tokenTable, converter, zeroIfNone)
                if (symbolValue == null && !zeroIfNone) return@evaluateWithCachedTokenTable null
                ret += monomial.coefficient * (symbolValue ?: Flt64.zero)
            }
            converter.intoValue(ret)
        }
    }

    /**
     * 使用固定值映射在求解器边界求值。
     * Evaluate at solver boundary using fixed values map.
     *
     * @param values Flt64 固定值映射 / Flt64 fixed values map
     * @param tokenTable 令牌表（可空） / Token table (nullable)
     * @param converter 值转换器 / Value converter
     * @param zeroIfNone 无值时是否返回零 / Whether to return zero when value is absent
     * @return 求值结果 / Evaluation result
     */
    internal fun evaluateSolver(values: Map<Symbol, Flt64>, tokenTable: AbstractTokenTable<V>?, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        if (tokenTable == null) {
            if (values.containsKey(this)) return converter.intoValue(values[this]!!)
            var ret = _polyFlt64.constant
            for (monomial in _polyFlt64.monomials) {
                val symbolValue = evaluateSymbol(monomial.symbol, values, tokenTable, converter, zeroIfNone)
                if (symbolValue == null && !zeroIfNone) return null
                ret += monomial.coefficient * (symbolValue ?: Flt64.zero)
            }
            return converter.intoValue(ret)
        }
        return evaluateWithCachedTokenTable(values, tokenTable, converter, zeroIfNone) {
            if (values.containsKey(this)) {
                return@evaluateWithCachedTokenTable converter.intoValue(values[this]!!)
            }
            var ret = _polyFlt64.constant
            for (monomial in _polyFlt64.monomials) {
                val symbolValue = evaluateSymbol(monomial.symbol, values, tokenTable, converter, zeroIfNone)
                if (symbolValue == null && !zeroIfNone) {
                    return@evaluateWithCachedTokenTable null
                }
                ret += monomial.coefficient * (symbolValue ?: Flt64.zero)
            }
            converter.intoValue(ret)
        }
    }

    /**
     * 使用求解器结果列表和 Flt64 令牌列表求值。
     * Evaluate using solver results list and Flt64 token list.
     *
     * @param results 求解器结果列表 / Solver results list
     * @param tokenList Flt64 令牌列表 / Flt64 token list
     * @param zeroIfNone 无值时是否返回零 / Whether to return zero when value is absent
     * @return Flt64 求值结果 / Flt64 evaluation result
     */
    internal fun evaluate(results: List<Flt64>, tokenList: AbstractTokenList<Flt64>, zeroIfNone: Boolean): Flt64? {
        var ret = _polyFlt64.constant
        for (monomial in _polyFlt64.monomials) {
            val symbolValue = evaluateSymbol(monomial.symbol, results, tokenList, zeroIfNone)
            if (symbolValue == null && !zeroIfNone) return null
            ret += monomial.coefficient * (symbolValue ?: Flt64.zero)
        }
        return ret
    }

    /**
     * 使用固定值映射和 Flt64 令牌列表求值。
     * Evaluate using fixed values map and Flt64 token list.
     *
     * @param values Flt64 固定值映射 / Flt64 fixed values map
     * @param tokenList Flt64 令牌列表（可空） / Flt64 token list (nullable)
     * @param zeroIfNone 无值时是否返回零 / Whether to return zero when value is absent
     * @return Flt64 求值结果 / Flt64 evaluation result
     */
    internal fun evaluate(values: Map<Symbol, Flt64>, tokenList: AbstractTokenList<Flt64>?, zeroIfNone: Boolean): Flt64? {
        if (values.containsKey(this)) return values[this]!!
        var ret = _polyFlt64.constant
        for (monomial in _polyFlt64.monomials) {
            val symbolValue = evaluateSymbol(monomial.symbol, values, tokenList, zeroIfNone)
            if (symbolValue == null && !zeroIfNone) return null
            ret += monomial.coefficient * (symbolValue ?: Flt64.zero)
        }
        return ret
    }

    override fun toString(): String {
        return displayName ?: name
    }

    override fun hashCode(): Int {
        return identifier.toInt() * 31 + index
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LinearExpressionSymbol<*>) return false

        if (identifier != other.identifier) return false
        if (index != other.index) return false
        if (name != other.name) return false

        return true
    }
}

/**
 * 二次中间符号表达式，由二次多项式支持，可在求解器边界进行缓存求值。
 * Quadratic intermediate symbol expression backed by a quadratic polynomial, supporting cached evaluation at solver boundaries.
 *
 * @property _utilsPolynomial 可变二次多项式存储 / Mutable quadratic polynomial storage
 * @param category 符号类别 / Symbol category
 * @param parent 父级符号（可空） / Parent symbol (nullable)
 * @param name 符号名称 / Symbol name
 * @param displayName 显示名称（可空） / Display name (nullable)
 */
class QuadraticExpressionSymbol<V>(
    internal val _utilsPolynomial: MutableQuadraticPolynomial<V>,
    category: Category = _utilsPolynomial.category,
    parent: IntermediateSymbol<*>? = null,
    name: String = "",
    displayName: String? = null
) : QuadraticIntermediateSymbol<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
    internal var _group: AbstractSymbolCombination<*>? = null
    internal var _index: Int? = null
    override val identifier: UInt64 by lazy {
        _group?.identifier ?: IdentifierGenerator.gen()
    }
    override val index: Int by lazy {
        _index ?: 0
    }

    override val category: Category = category
    override val parent: IntermediateSymbol<*>? = parent
    override var name: String = name
    override var displayName: String? = displayName

    override val operationCategory: Category = Quadratic

    companion object {
        /**
         * 从变量项创建二次表达式符号。
         * Create quadratic expression symbol from variable item.
         *
         * @param item 变量项 / Variable item
         * @param constants 实数常量定义 / Real number constants definition
         * @param parent 父级符号（可空） / Parent symbol (nullable)
         * @param name 符号名称 / Symbol name
         * @param displayName 显示名称（可空） / Display name (nullable)
         * @return 二次表达式符号 / Quadratic expression symbol
         */
        operator fun <V> invoke(
            item: AbstractVariableItem<*, *>,
            constants: RealNumberConstants<V>,
            parent: IntermediateSymbol<*>? = null,
            name: String = "",
            displayName: String? = null
        ): QuadraticExpressionSymbol<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
            return QuadraticExpressionSymbol(
                _utilsPolynomial = MutableQuadraticPolynomial(
                    monomials = listOf(QuadraticMonomial.linear(constants.one, item)),
                    constant = constants.zero
                ),
                category = Quadratic,
                parent = parent,
                name = name.ifEmpty { item.name },
                displayName = displayName
            )
        }

        /**
         * 从线性中间符号创建二次表达式符号。
         * Create quadratic expression symbol from linear intermediate symbol.
         *
         * @param symbol 线性中间符号 / Linear intermediate symbol
         * @param constants 实数常量定义 / Real number constants definition
         * @param parent 父级符号（可空） / Parent symbol (nullable)
         * @param name 符号名称 / Symbol name
         * @param displayName 显示名称（可空） / Display name (nullable)
         * @return 二次表达式符号 / Quadratic expression symbol
         */
        operator fun <V> invoke(
            symbol: LinearIntermediateSymbol<*>,
            constants: RealNumberConstants<V>,
            parent: IntermediateSymbol<*>? = null,
            name: String = "",
            displayName: String? = null
        ): QuadraticExpressionSymbol<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
            return QuadraticExpressionSymbol(
                _utilsPolynomial = MutableQuadraticPolynomial(
                    monomials = listOf(QuadraticMonomial.linear(constants.one, symbol)),
                    constant = constants.zero
                ),
                category = Quadratic,
                parent = parent,
                name = name.ifEmpty { symbol.name },
                displayName = displayName ?: symbol.displayName
            )
        }

        /**
         * 从二次中间符号创建二次表达式符号。
         * Create quadratic expression symbol from quadratic intermediate symbol.
         *
         * @param symbol 二次中间符号 / Quadratic intermediate symbol
         * @param constants 实数常量定义 / Real number constants definition
         * @param parent 父级符号（可空） / Parent symbol (nullable)
         * @param name 符号名称 / Symbol name
         * @param displayName 显示名称（可空） / Display name (nullable)
         * @return 二次表达式符号 / Quadratic expression symbol
         */
        operator fun <V> invoke(
            symbol: QuadraticIntermediateSymbol<*>,
            constants: RealNumberConstants<V>,
            parent: IntermediateSymbol<*>? = null,
            name: String = "",
            displayName: String? = null
        ): QuadraticExpressionSymbol<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
            return QuadraticExpressionSymbol(
                _utilsPolynomial = MutableQuadraticPolynomial(
                    monomials = listOf(QuadraticMonomial.linear(constants.one, symbol)),
                    constant = constants.zero
                ),
                category = Quadratic,
                parent = parent,
                name = name.ifEmpty { symbol.name },
                displayName = displayName ?: symbol.displayName
            )
        }

        /**
         * 从不可变线性多项式创建二次表达式符号。
         * Create quadratic expression symbol from immutable linear polynomial.
         *
         * @param polynomial 线性多项式 / Linear polynomial
         * @param parent 父级符号（可空） / Parent symbol (nullable)
         * @param name 符号名称 / Symbol name
         * @param displayName 显示名称（可空） / Display name (nullable)
         * @return 二次表达式符号 / Quadratic expression symbol
         */
        operator fun <V> invoke(
            polynomial: LinearPolynomial<V>,
            parent: IntermediateSymbol<*>? = null,
            name: String = "",
            displayName: String? = null
        ): QuadraticExpressionSymbol<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
            val quadraticPolynomial = polynomial.toQuadraticPolynomial()
            return QuadraticExpressionSymbol(
                _utilsPolynomial = MutableQuadraticPolynomial(
                    monomials = quadraticPolynomial.monomials,
                    constant = quadraticPolynomial.constant
                ),
                category = quadraticPolynomial.category,
                parent = parent,
                name = name.ifEmpty { polynomial.toString() },
                displayName = displayName
            )
        }

        /**
         * 从线性单项式创建二次表达式符号。
         * Create quadratic expression symbol from linear monomial.
         *
         * @param monomial 线性单项式 / Linear monomial
         * @param parent 父级符号（可空） / Parent symbol (nullable)
         * @param name 符号名称 / Symbol name
         * @param displayName 显示名称（可空） / Display name (nullable)
         * @return 二次表达式符号 / Quadratic expression symbol
         */
        operator fun <V> invoke(
            monomial: LinearMonomial<V>,
            parent: IntermediateSymbol<*>? = null,
            name: String = "",
            displayName: String? = null
        ): QuadraticExpressionSymbol<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
            val quadraticMonomial = QuadraticMonomial.linear(monomial.coefficient, monomial.symbol)
            return QuadraticExpressionSymbol(
                _utilsPolynomial = MutableQuadraticPolynomial(
                    monomials = listOf(quadraticMonomial),
                    constant = monomial.coefficient - monomial.coefficient
                ),
                category = quadraticMonomial.category,
                parent = parent,
                name = name.ifEmpty { monomial.toString() },
                displayName = displayName
            )
        }

        /**
         * 从不可变二次多项式创建二次表达式符号。
         * Create quadratic expression symbol from immutable quadratic polynomial.
         *
         * @param polynomial 二次多项式 / Quadratic polynomial
         * @param parent 父级符号（可空） / Parent symbol (nullable)
         * @param name 符号名称 / Symbol name
         * @param displayName 显示名称（可空） / Display name (nullable)
         * @return 二次表达式符号 / Quadratic expression symbol
         */
        operator fun <V> invoke(
            polynomial: QuadraticPolynomial<V>,
            parent: IntermediateSymbol<*>? = null,
            name: String = "",
            displayName: String? = null
        ): QuadraticExpressionSymbol<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
            return QuadraticExpressionSymbol(
                _utilsPolynomial = MutableQuadraticPolynomial(
                    monomials = polynomial.monomials,
                    constant = polynomial.constant
                ),
                category = polynomial.category,
                parent = parent,
                name = name.ifEmpty { polynomial.toString() },
                displayName = displayName
            )
        }

        /**
         * 从二次单项式创建二次表达式符号。
         * Create quadratic expression symbol from quadratic monomial.
         *
         * @param monomial 二次单项式 / Quadratic monomial
         * @param parent 父级符号（可空） / Parent symbol (nullable)
         * @param name 符号名称 / Symbol name
         * @param displayName 显示名称（可空） / Display name (nullable)
         * @return 二次表达式符号 / Quadratic expression symbol
         */
        operator fun <V> invoke(
            monomial: QuadraticMonomial<V>,
            parent: IntermediateSymbol<*>? = null,
            name: String = "",
            displayName: String? = null
        ): QuadraticExpressionSymbol<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
            return QuadraticExpressionSymbol(
                _utilsPolynomial = MutableQuadraticPolynomial(
                    monomials = listOf(monomial),
                    constant = monomial.coefficient - monomial.coefficient
                ),
                category = monomial.category,
                parent = parent,
                name = name.ifEmpty { monomial.toString() },
                displayName = displayName
            )
        }

        /**
         * 从可变二次多项式创建二次表达式符号。
         * Create quadratic expression symbol from mutable quadratic polynomial.
         *
         * @param polynomial 可变二次多项式 / Mutable quadratic polynomial
         * @param parent 父级符号（可空） / Parent symbol (nullable)
         * @param name 符号名称 / Symbol name
         * @param displayName 显示名称（可空） / Display name (nullable)
         * @return 二次表达式符号 / Quadratic expression symbol
         */
        operator fun <V> invoke(
            polynomial: MutableQuadraticPolynomial<V>,
            parent: IntermediateSymbol<*>? = null,
            name: String = "",
            displayName: String? = null
        ): QuadraticExpressionSymbol<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
            return QuadraticExpressionSymbol(
                _utilsPolynomial = polynomial,
                category = polynomial.category,
                parent = parent,
                name = name.ifEmpty { polynomial.toString() },
                displayName = displayName
            )
        }

        /**
         * 从常量值创建二次表达式符号。
         * Create quadratic expression symbol from constant value.
         *
         * @param constant 常量值 / Constant value
         * @param parent 父级符号（可空） / Parent symbol (nullable)
         * @param name 符号名称 / Symbol name
         * @param displayName 显示名称（可空） / Display name (nullable)
         * @return 二次表达式符号 / Quadratic expression symbol
         */
        operator fun <V> invoke(
            constant: V,
            parent: IntermediateSymbol<*>? = null,
            name: String = "",
            displayName: String? = null
        ): QuadraticExpressionSymbol<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
            return QuadraticExpressionSymbol(
                _utilsPolynomial = MutableQuadraticPolynomial(
                    monomials = emptyList(),
                    constant = constant
                ),
                category = Quadratic,
                parent = parent,
                name = name,
                displayName = displayName
            )
        }

        /**
         * 从实数常量定义创建零值二次表达式符号。
         * Create zero-valued quadratic expression symbol from real number constants.
         *
         * @param constants 实数常量定义 / Real number constants definition
         * @param parent 父级符号（可空） / Parent symbol (nullable)
         * @param name 符号名称 / Symbol name
         * @param displayName 显示名称（可空） / Display name (nullable)
         * @return 零值二次表达式符号 / Zero-valued quadratic expression symbol
         */
        operator fun <V> invoke(
            constants: RealNumberConstants<V>,
            parent: IntermediateSymbol<*>? = null,
            name: String = "",
            displayName: String? = null
        ): QuadraticExpressionSymbol<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
            return QuadraticExpressionSymbol(
                _utilsPolynomial = MutableQuadraticPolynomial(
                    monomials = emptyList(),
                    constant = constants.zero
                ),
                category = Quadratic,
                parent = parent,
                name = name,
                displayName = displayName
            )
        }
    }

    // 求解器边界使用的 Flt64 视图。
    // Flt64 view used at the solver boundary.
    private val _polyFlt64: MutableQuadraticPolynomial<Flt64> get() = SolverBoundaryCasts.quadraticPolynomialAsFlt64(_utilsPolynomial)

    override val polynomial: QuadraticPolynomial<V> get() = _utilsPolynomial.toQuadraticPolynomial()

    /** 获取二次扁平化数据（单项式和常量）。 / Get quadratic flatten data (monomials and constant). */
    val flattenedMonomials: QuadraticFlattenData<V>
        get() = QuadraticFlattenData(
            monomials = _utilsPolynomial.monomials,
            constant = _utilsPolynomial.constant
        )

    override fun asMutable(): MutableQuadraticPolynomial<V> {
        return _utilsPolynomial
    }

    /**
     * 通过令牌表求值符号（求解器边界 Flt64 路径）。
     * Evaluate symbol via token table (solver boundary Flt64 path).
     *
     * @param symbol 待求值符号 / Symbol to evaluate
     * @param tokenTable 令牌表 / Token table
     * @param converter 值转换器 / Value converter
     * @param zeroIfNone 无值时是否返回零 / Whether to return zero when value is absent
     * @return Flt64 求值结果 / Flt64 evaluation result
     */
    private fun evaluateSymbol(symbol: Symbol, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): Flt64? {
        val tokenList = SolverBoundaryCasts.tokenListAsFlt64(tokenTable)
        return when (symbol) {
            is AbstractVariableItem<*, *> -> {
                tokenTable.find(symbol)?.resultFlt64 ?: if (zeroIfNone) Flt64.zero else null
            }
            is LinearExpressionSymbol<*> -> symbol.evaluate(tokenList, zeroIfNone)
            is QuadraticExpressionSymbol<*> -> symbol.evaluate(tokenList, zeroIfNone)
            else -> null
        }
    }

    /**
     * 通过求解器结果列表和令牌表求值符号。
     * Evaluate symbol via solver results list and token table.
     *
     * @param symbol 待求值符号 / Symbol to evaluate
     * @param results 求解器结果列表 / Solver results list
     * @param tokenTable 令牌表 / Token table
     * @param converter 值转换器 / Value converter
     * @param zeroIfNone 无值时是否返回零 / Whether to return zero when value is absent
     * @return Flt64 求值结果 / Flt64 evaluation result
     */
    private fun evaluateSymbol(symbol: Symbol, results: List<Flt64>, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): Flt64? {
        val tokenList = SolverBoundaryCasts.tokenListAsFlt64(tokenTable)
        return when (symbol) {
            is AbstractVariableItem<*, *> -> {
                val index = tokenTable.indexOf(symbol)
                if (index != null && index >= 0 && index < results.size) results[index]
                else if (zeroIfNone) Flt64.zero else null
            }
            is LinearExpressionSymbol<*> -> symbol.evaluate(results, tokenList, zeroIfNone)
            is QuadraticExpressionSymbol<*> -> symbol.evaluate(results, tokenList, zeroIfNone)
            else -> null
        }
    }

    /**
     * 通过固定值映射和可空令牌表求值符号。
     * Evaluate symbol via fixed values map and nullable token table.
     *
     * @param symbol 待求值符号 / Symbol to evaluate
     * @param values Flt64 固定值映射 / Flt64 fixed values map
     * @param tokenTable 令牌表（可空） / Token table (nullable)
     * @param converter 值转换器 / Value converter
     * @param zeroIfNone 无值时是否返回零 / Whether to return zero when value is absent
     * @return Flt64 求值结果 / Flt64 evaluation result
     */
    private fun evaluateSymbol(symbol: Symbol, values: Map<Symbol, Flt64>, tokenTable: AbstractTokenTable<V>?, converter: IntoValue<V>, zeroIfNone: Boolean): Flt64? {
        val tokenList = SolverBoundaryCasts.tokenListAsFlt64OrNull(tokenTable)
        return when (symbol) {
            is AbstractVariableItem<*, *> -> {
                values[symbol] ?: tokenTable?.find(symbol)?.resultFlt64 ?: if (zeroIfNone) Flt64.zero else null
            }
            is LinearExpressionSymbol<*> -> symbol.evaluate(values, tokenList, zeroIfNone)
            is QuadraticExpressionSymbol<*> -> symbol.evaluate(values, tokenList, zeroIfNone)
            else -> values[symbol] ?: if (zeroIfNone) Flt64.zero else null
        }
    }

    /**
     * 通过 Flt64 令牌列表直接求值符号。
     * Evaluate symbol directly via Flt64 token list.
     *
     * @param symbol 待求值符号 / Symbol to evaluate
     * @param tokenList Flt64 令牌列表 / Flt64 token list
     * @param zeroIfNone 无值时是否返回零 / Whether to return zero when value is absent
     * @return Flt64 求值结果 / Flt64 evaluation result
     */
    private fun evaluateSymbol(symbol: Symbol, tokenList: AbstractTokenList<Flt64>, zeroIfNone: Boolean): Flt64? {
        return when (symbol) {
            is AbstractVariableItem<*, *> -> {
                val token = tokenList.find(symbol)
                token?.resultFlt64 ?: if (zeroIfNone) Flt64.zero else null
            }
            is LinearExpressionSymbol<*> -> symbol.evaluate(tokenList, zeroIfNone)
            is QuadraticExpressionSymbol<*> -> symbol.evaluate(tokenList, zeroIfNone)
            else -> null
        }
    }

    /**
     * 通过求解器结果列表和 Flt64 令牌列表求值符号。
     * Evaluate symbol via solver results list and Flt64 token list.
     *
     * @param symbol 待求值符号 / Symbol to evaluate
     * @param results 求解器结果列表 / Solver results list
     * @param tokenList Flt64 令牌列表 / Flt64 token list
     * @param zeroIfNone 无值时是否返回零 / Whether to return zero when value is absent
     * @return Flt64 求值结果 / Flt64 evaluation result
     */
    private fun evaluateSymbol(symbol: Symbol, results: List<Flt64>, tokenList: AbstractTokenList<Flt64>, zeroIfNone: Boolean): Flt64? {
        return when (symbol) {
            is AbstractVariableItem<*, *> -> {
                val index = tokenList.indexOf(symbol)
                if (index != null && index >= 0 && index < results.size) results[index]
                else if (zeroIfNone) Flt64.zero else null
            }
            is LinearExpressionSymbol<*> -> symbol.evaluate(results, tokenList, zeroIfNone)
            is QuadraticExpressionSymbol<*> -> symbol.evaluate(results, tokenList, zeroIfNone)
            else -> null
        }
    }

    /**
     * 通过固定值映射和 Flt64 令牌列表求值符号。
     * Evaluate symbol via fixed values map and Flt64 token list.
     *
     * @param symbol 待求值符号 / Symbol to evaluate
     * @param values Flt64 固定值映射 / Flt64 fixed values map
     * @param tokenList Flt64 令牌列表（可空） / Flt64 token list (nullable)
     * @param zeroIfNone 无值时是否返回零 / Whether to return zero when value is absent
     * @return Flt64 求值结果 / Flt64 evaluation result
     */
    private fun evaluateSymbol(symbol: Symbol, values: Map<Symbol, Flt64>, tokenList: AbstractTokenList<Flt64>?, zeroIfNone: Boolean): Flt64? {
        return when (symbol) {
            is AbstractVariableItem<*, *> -> {
                values[symbol] ?: tokenList?.find(symbol)?.resultFlt64 ?: if (zeroIfNone) Flt64.zero else null
            }
            is LinearExpressionSymbol<*> -> symbol.evaluate(values, tokenList, zeroIfNone)
            is QuadraticExpressionSymbol<*> -> symbol.evaluate(values, tokenList, zeroIfNone)
            else -> values[symbol] ?: if (zeroIfNone) Flt64.zero else null
        }
    }

    override val dependencies: Set<IntermediateSymbol<*>>
        get() = _utilsPolynomial.monomials
            .flatMap { monomial ->
                val deps = mutableListOf<IntermediateSymbol<*>>()
                when (val sym1 = monomial.symbol1) {
                    is LinearIntermediateSymbol<*> -> deps.add(sym1)
                    is QuadraticIntermediateSymbol<*> -> deps.add(sym1)
                }
                if (monomial.symbol2 != null) {
                    when (val sym2 = monomial.symbol2!!) {
                        is LinearIntermediateSymbol<*> -> deps.add(sym2)
                        is QuadraticIntermediateSymbol<*> -> deps.add(sym2)
                    }
                }
                deps
            }
            .toSet()

    /**
     * 计算二次表达式的 Flt64 值域。
     * Calculate the Flt64 value range of the quadratic expression.
     *
     * @return Flt64 值域，若依赖符号无值域则返回 null / Flt64 value range, or null if dependency symbols have no range
     */
    private fun calculateRange(): ValueRange<Flt64>? {
        var range: ValueRange<Flt64>? = ValueRange(_polyFlt64.constant, Flt64).value
        for (monomial in _polyFlt64.monomials) {
            val sym1Range: ValueRange<Flt64>? = when (val sym1 = monomial.symbol1) {
                is AbstractVariableItem<*, *> -> sym1.range.valueRange
                is LinearIntermediateSymbol<*> -> sym1.range.valueRange
                is QuadraticIntermediateSymbol<*> -> sym1.range.valueRange
                else -> null
            }
            val sym2Range: ValueRange<Flt64>? = if (monomial.symbol2 != null) {
                when (val sym2 = monomial.symbol2!!) {
                    is AbstractVariableItem<*, *> -> sym2.range.valueRange
                    is LinearIntermediateSymbol<*> -> sym2.range.valueRange
                    is QuadraticIntermediateSymbol<*> -> sym2.range.valueRange
                    else -> null
                }
            } else null

            if (sym1Range == null) {
                range = null
                break
            }

            if (monomial.symbol2 == null) {
                val scaled = monomial.coefficient * sym1Range!!
                range = range?.let { r -> scaled?.let { s -> r + s } }
            } else if (sym2Range != null) {
                val s1r = sym1Range!!
                val s2r = sym2Range!!
                val termRange = (monomial.coefficient * s1r)?.times(s2r)
                range = range?.let { r -> termRange?.let { s -> r + s } }
            } else {
                range = null
                break
            }
        }
        return range
    }

    override val range: ExpressionRange<V>
        get() = SolverBoundaryCasts.expressionRangeFromFlt64(calculateRange())

    override val discrete: Boolean
        get() = _polyFlt64.monomials.all { monomial ->
            val sym1Discrete = when (val sym1 = monomial.symbol1) {
                is AbstractVariableItem<*, *> -> sym1.type.isIntegerType && monomial.coefficient.round() eq monomial.coefficient
                is LinearIntermediateSymbol<*> -> sym1.discrete && monomial.coefficient.round() eq monomial.coefficient
                is QuadraticIntermediateSymbol<*> -> sym1.discrete && monomial.coefficient.round() eq monomial.coefficient
                else -> false
            }
            val sym2Discrete = if (monomial.symbol2 != null) {
                when (val sym2 = monomial.symbol2!!) {
                    is AbstractVariableItem<*, *> -> sym2.type.isIntegerType && monomial.coefficient.round() eq monomial.coefficient
                    is LinearIntermediateSymbol<*> -> sym2.discrete && monomial.coefficient.round() eq monomial.coefficient
                    is QuadraticIntermediateSymbol<*> -> sym2.discrete && monomial.coefficient.round() eq monomial.coefficient
                    else -> false
                }
            } else true
            sym1Discrete && sym2Discrete
        } && _polyFlt64.constant.round() eq _polyFlt64.constant

    private val rangeCacheKey = newTokenCacheKey(
        category = Quadratic,
        prefix = "__quadratic_expression_range_cache__"
    )
    private val flattenCacheKey = newTokenCacheKey(
        category = Quadratic,
        prefix = "__quadratic_expression_flatten_cache__"
    )
    /**
     * 从依赖符号中获取绑定的令牌表并转换为 Flt64 视图。
     * Retrieve the bound token table from dependencies and cast to Flt64 view.
     *
     * @return Flt64 令牌表，若无可用依赖则返回 null / Flt64 token table, or null if no dependency provides one
     */
    private fun cacheTokenTable(): AbstractTokenTable<Flt64>? {
        return SolverBoundaryCasts.tokenTableAsFlt64OrNull(
            dependencies
                .asSequence()
                .mapNotNull { boundTokenTableContext(it) }
                .firstOrNull()
        )
    }

    override val cached: Boolean
        get() {
            val tokenTable = cacheTokenTable()
            val cachedFlatten = tokenTable?.cachedQuadraticFlatten(flattenCacheKey)
            return cachedFlatten == true
        }

    override fun flush(force: Boolean) {
        val tokenTable = cacheTokenTable()
        val cachedRange = tokenTable?.cachedRangeValue(rangeCacheKey)
        if (force || cachedRange?.set == false) {
            tokenTable?.clearRange(rangeCacheKey)
        }
        for (dep in dependencies) {
            dep.flush(force)
        }
        if (force) {
            tokenTable?.clearQuadraticFlatten(flattenCacheKey)
        }
    }

    /**
     * 在求解器边界准备符号值，使用 Flt64 视图求值后转换回目标类型。
     * Prepare symbol value at solver boundary, evaluate using Flt64 view then convert back.
     *
     * @param values 固定值映射（可空） / Fixed values map (nullable)
     * @param tokenTable 令牌表 / Token table
     * @param converter 值转换器 / Value converter
     * @return 求值结果 / Evaluation result
     */
    internal fun prepareSolver(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>): V? {
        // 求解器求值前触发 flatten 视图创建。
        // Trigger flatten-view creation before solver evaluation.
        flattenedMonomials

        return if (values.isNullOrEmpty()) {
            var ret = _polyFlt64.constant
            for (monomial in _polyFlt64.monomials) {
                val sym1Value = evaluateSymbol(monomial.symbol1, tokenTable, converter, false)
                if (sym1Value == null) return null
                val termValue = if (monomial.symbol2 != null) {
                    val sym2Value = evaluateSymbol(monomial.symbol2!!, tokenTable, converter, false)
                    if (sym2Value == null) return null
                    sym1Value * sym2Value
                } else sym1Value
                ret += monomial.coefficient * termValue
            }
            converter.intoValue(ret)
        } else {
            var ret = _polyFlt64.constant
            for (monomial in _polyFlt64.monomials) {
                val sym1Value = evaluateSymbol(monomial.symbol1, values, tokenTable, converter, false)
                if (sym1Value == null) return null
                val termValue = if (monomial.symbol2 != null) {
                    val sym2Value = evaluateSymbol(monomial.symbol2!!, values, tokenTable, converter, false)
                    if (sym2Value == null) return null
                    sym1Value * sym2Value
                } else sym1Value
                ret += monomial.coefficient * termValue
            }
            converter.intoValue(ret)
        }
    }

    override fun prepare(values: Map<Symbol, V>?, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>): V? {
        val flt64Values = values?.mapValues { converter.fromValue(it.value) }
        return prepareSolver(flt64Values, tokenTable, converter)
    }

    override fun toRawString(unfold: UInt64): String {
        return if (unfold neq UInt64.zero) {
            val monomialStrings = _polyFlt64.monomials.filter { it.coefficient neq Flt64.zero }.map { m ->
                val sym1Str = when (val s1 = m.symbol1) {
                    is IntermediateSymbol<*> -> s1.toRawString(unfold - UInt64.one)
                    else -> s1.name
                }
                val termStr = if (m.symbol2 != null) {
                    val sym2Str = when (val s2 = m.symbol2!!) {
                        is IntermediateSymbol<*> -> s2.toRawString(unfold - UInt64.one)
                        else -> s2.name
                    }
                    if (m.symbol1 == m.symbol2) "$sym1Str^2" else "$sym1Str * $sym2Str"
                } else sym1Str
                if (m.coefficient eq Flt64.one) termStr
                else if (m.coefficient eq -Flt64.one) "-$termStr"
                else "${m.coefficient} * $termStr"
            }
            if (_polyFlt64.constant neq Flt64.zero) {
                "${monomialStrings.joinToString(" + ")} + ${_polyFlt64.constant}"
            } else {
                monomialStrings.joinToString(" + ")
            }
        } else {
            displayName ?: name
        }
    }

    /**
     * 使用 Flt64 令牌列表直接求值。
     * Evaluate directly using Flt64 token list.
     *
     * @param tokenList Flt64 令牌列表 / Flt64 token list
     * @param zeroIfNone 无值时是否返回零 / Whether to return zero when value is absent
     * @return Flt64 求值结果 / Flt64 evaluation result
     */
    internal fun evaluate(tokenList: AbstractTokenList<Flt64>, zeroIfNone: Boolean): Flt64? {
        var ret = _polyFlt64.constant
        for (monomial in _polyFlt64.monomials) {
            val sym1Value = evaluateSymbol(monomial.symbol1, tokenList, zeroIfNone)
            if (sym1Value == null && !zeroIfNone) return null
            val termValue = if (monomial.symbol2 != null) {
                val sym2Value = evaluateSymbol(monomial.symbol2!!, tokenList, zeroIfNone)
                if (sym2Value == null && !zeroIfNone) return null
                (sym1Value ?: Flt64.zero) * (sym2Value ?: Flt64.zero)
            } else (sym1Value ?: Flt64.zero)
            ret += monomial.coefficient * termValue
        }
        return ret
    }

    override fun evaluate(tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        return evaluateWithCachedTokenTable(tokenTable, converter, zeroIfNone) {
            var ret = _polyFlt64.constant
            for (monomial in _polyFlt64.monomials) {
                val sym1Value = evaluateSymbol(monomial.symbol1, tokenTable, converter, zeroIfNone)
                if (sym1Value == null && !zeroIfNone) return@evaluateWithCachedTokenTable null
                val termValue = if (monomial.symbol2 != null) {
                    val sym2Value = evaluateSymbol(monomial.symbol2!!, tokenTable, converter, zeroIfNone)
                    if (sym2Value == null && !zeroIfNone) return@evaluateWithCachedTokenTable null
                    (sym1Value ?: Flt64.zero) * (sym2Value ?: Flt64.zero)
                } else (sym1Value ?: Flt64.zero)
                ret += monomial.coefficient * termValue
            }
            converter.intoValue(ret)
        }
    }

    override fun evaluate(results: List<V>, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        val flt64Results = results.map { converter.fromValue(it) }
        return evaluateSolver(flt64Results, tokenTable, converter, zeroIfNone)
    }

    override fun evaluate(values: Map<Symbol, V>, tokenTable: AbstractTokenTable<V>?, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        val flt64Values = values.mapValues { converter.fromValue(it.value) }
        return evaluateSolver(flt64Values, tokenTable, converter, zeroIfNone)
    }

    /**
     * 使用求解器结果列表和 Flt64 令牌列表求值。
     * Evaluate using solver results list and Flt64 token list.
     *
     * @param results 求解器结果列表 / Solver results list
     * @param tokenList Flt64 令牌列表 / Flt64 token list
     * @param zeroIfNone 无值时是否返回零 / Whether to return zero when value is absent
     * @return Flt64 求值结果 / Flt64 evaluation result
     */
    internal fun evaluate(results: List<Flt64>, tokenList: AbstractTokenList<Flt64>, zeroIfNone: Boolean): Flt64? {
        var ret = _polyFlt64.constant
        for (monomial in _polyFlt64.monomials) {
            val sym1Value = evaluateSymbol(monomial.symbol1, results, tokenList, zeroIfNone)
            if (sym1Value == null && !zeroIfNone) return null
            val termValue = if (monomial.symbol2 != null) {
                val sym2Value = evaluateSymbol(monomial.symbol2!!, results, tokenList, zeroIfNone)
                if (sym2Value == null && !zeroIfNone) return null
                (sym1Value ?: Flt64.zero) * (sym2Value ?: Flt64.zero)
            } else (sym1Value ?: Flt64.zero)
            ret += monomial.coefficient * termValue
        }
        return ret
    }

    /**
     * 使用求解器结果列表在求解器边界求值。
     * Evaluate at solver boundary using solver results list.
     *
     * @param results 求解器结果列表 / Solver results list
     * @param tokenTable 令牌表 / Token table
     * @param converter 值转换器 / Value converter
     * @param zeroIfNone 无值时是否返回零 / Whether to return zero when value is absent
     * @return 求值结果 / Evaluation result
     */
    internal fun evaluateSolver(results: List<Flt64>, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        return evaluateWithCachedTokenTable(results, tokenTable, converter, zeroIfNone) {
            var ret = _polyFlt64.constant
            for (monomial in _polyFlt64.monomials) {
                val sym1Value = evaluateSymbol(monomial.symbol1, results, tokenTable, converter, zeroIfNone)
                if (sym1Value == null && !zeroIfNone) return@evaluateWithCachedTokenTable null
                val termValue = if (monomial.symbol2 != null) {
                    val sym2Value = evaluateSymbol(monomial.symbol2!!, results, tokenTable, converter, zeroIfNone)
                    if (sym2Value == null && !zeroIfNone) return@evaluateWithCachedTokenTable null
                    (sym1Value ?: Flt64.zero) * (sym2Value ?: Flt64.zero)
                } else (sym1Value ?: Flt64.zero)
                ret += monomial.coefficient * termValue
            }
            converter.intoValue(ret)
        }
    }

    /**
     * 使用固定值映射和 Flt64 令牌列表求值。
     * Evaluate using fixed values map and Flt64 token list.
     *
     * @param values Flt64 固定值映射 / Flt64 fixed values map
     * @param tokenList Flt64 令牌列表（可空） / Flt64 token list (nullable)
     * @param zeroIfNone 无值时是否返回零 / Whether to return zero when value is absent
     * @return Flt64 求值结果 / Flt64 evaluation result
     */
    internal fun evaluate(values: Map<Symbol, Flt64>, tokenList: AbstractTokenList<Flt64>?, zeroIfNone: Boolean): Flt64? {
        if (values.containsKey(this)) return values[this]!!
        var ret = _polyFlt64.constant
        for (monomial in _polyFlt64.monomials) {
            val sym1Value = evaluateSymbol(monomial.symbol1, values, tokenList, zeroIfNone)
            if (sym1Value == null && !zeroIfNone) return null
            val termValue = if (monomial.symbol2 != null) {
                val sym2Value = evaluateSymbol(monomial.symbol2!!, values, tokenList, zeroIfNone)
                if (sym2Value == null && !zeroIfNone) return null
                (sym1Value ?: Flt64.zero) * (sym2Value ?: Flt64.zero)
            } else (sym1Value ?: Flt64.zero)
            ret += monomial.coefficient * termValue
        }
        return ret
    }

    /**
     * 使用固定值映射在求解器边界求值。
     * Evaluate at solver boundary using fixed values map.
     *
     * @param values Flt64 固定值映射 / Flt64 fixed values map
     * @param tokenTable 令牌表（可空） / Token table (nullable)
     * @param converter 值转换器 / Value converter
     * @param zeroIfNone 无值时是否返回零 / Whether to return zero when value is absent
     * @return 求值结果 / Evaluation result
     */
    internal fun evaluateSolver(values: Map<Symbol, Flt64>, tokenTable: AbstractTokenTable<V>?, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        if (tokenTable == null) {
            if (values.containsKey(this)) return converter.intoValue(values[this]!!)
            var ret = _polyFlt64.constant
            for (monomial in _polyFlt64.monomials) {
                val sym1Value = evaluateSymbol(monomial.symbol1, values, tokenTable, converter, zeroIfNone)
                if (sym1Value == null && !zeroIfNone) return null
                val termValue = if (monomial.symbol2 != null) {
                    val sym2Value = evaluateSymbol(monomial.symbol2!!, values, tokenTable, converter, zeroIfNone)
                    if (sym2Value == null && !zeroIfNone) return null
                    (sym1Value ?: Flt64.zero) * (sym2Value ?: Flt64.zero)
                } else (sym1Value ?: Flt64.zero)
                ret += monomial.coefficient * termValue
            }
            return converter.intoValue(ret)
        }
        return evaluateWithCachedTokenTable(values, tokenTable, converter, zeroIfNone) {
            if (values.containsKey(this)) {
                return@evaluateWithCachedTokenTable converter.intoValue(values[this]!!)
            }
            var ret = _polyFlt64.constant
            for (monomial in _polyFlt64.monomials) {
                val sym1Value = evaluateSymbol(monomial.symbol1, values, tokenTable, converter, zeroIfNone)
                if (sym1Value == null && !zeroIfNone) {
                    return@evaluateWithCachedTokenTable null
                }
                val termValue = if (monomial.symbol2 != null) {
                    val sym2Value = evaluateSymbol(monomial.symbol2!!, values, tokenTable, converter, zeroIfNone)
                    if (sym2Value == null && !zeroIfNone) {
                        return@evaluateWithCachedTokenTable null
                    }
                    (sym1Value ?: Flt64.zero) * (sym2Value ?: Flt64.zero)
                } else (sym1Value ?: Flt64.zero)
                ret += monomial.coefficient * termValue
            }
            converter.intoValue(ret)
        }
    }

    override fun toString(): String {
        return displayName ?: name
    }

    override fun hashCode(): Int {
        return identifier.toInt() * 31 + index
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is QuadraticExpressionSymbol<*>) return false

        if (identifier != other.identifier) return false
        if (index != other.index) return false
        if (name != other.name) return false

        return true
    }
}

/**
 * 将线性中间符号与物理单位相乘，创建量纲符号。
 * Multiply linear intermediate symbol by physical unit to create quantity symbol.
 *
 * @param rhs 物理单位 / Physical unit
 * @return 量纲线性中间符号 / Quantity linear intermediate symbol
 */
operator fun <V> LinearIntermediateSymbol<V>.times(rhs: PhysicalUnit): Quantity<LinearIntermediateSymbol<V>> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
    return Quantity(this, rhs)
}

/**
 * 将线性中间符号除以物理单位，创建量纲符号。
 * Divide linear intermediate symbol by physical unit to create quantity symbol.
 *
 * @param rhs 物理单位 / Physical unit
 * @return 量纲线性中间符号 / Quantity linear intermediate symbol
 */
operator fun <V> LinearIntermediateSymbol<V>.div(rhs: PhysicalUnit): Quantity<LinearIntermediateSymbol<V>> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
    return Quantity(this, rhs.reciprocal())
}

/**
 * 将二次中间符号与物理单位相乘，创建量纲符号。
 * Multiply quadratic intermediate symbol by physical unit to create quantity symbol.
 *
 * @param rhs 物理单位 / Physical unit
 * @return 量纲二次中间符号 / Quantity quadratic intermediate symbol
 */
operator fun <V> QuadraticIntermediateSymbol<V>.times(rhs: PhysicalUnit): Quantity<QuadraticIntermediateSymbol<V>> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
    return Quantity(this, rhs)
}

/**
 * 将二次中间符号除以物理单位，创建量纲符号。
 * Divide quadratic intermediate symbol by physical unit to create quantity symbol.
 *
 * @param rhs 物理单位 / Physical unit
 * @return 量纲二次中间符号 / Quantity quadratic intermediate symbol
 */
operator fun <V> QuadraticIntermediateSymbol<V>.div(rhs: PhysicalUnit): Quantity<QuadraticIntermediateSymbol<V>> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
    return Quantity(this, rhs.reciprocal())
}

/**
 * 将两个线性中间符号相加，返回线性多项式。
 * Add two linear intermediate symbols to produce a linear polynomial.
 *
 * @param rhs 右侧线性中间符号 / Right-hand linear intermediate symbol
 * @return 线性多项式 / Linear polynomial
 */
operator fun <V> LinearIntermediateSymbol<V>.plus(rhs: LinearIntermediateSymbol<V>): LinearPolynomial<V>
    where V : RealNumber<V>, V : NumberField<V> {
    val lhs = this.toLinearPolynomial()
    val rhsPoly = rhs.toLinearPolynomial()
    return LinearPolynomial(lhs.monomials + rhsPoly.monomials, lhs.constant + rhsPoly.constant)
}

/**
 * 将两个线性中间符号相减，返回线性多项式。
 * Subtract two linear intermediate symbols to produce a linear polynomial.
 *
 * @param rhs 右侧线性中间符号 / Right-hand linear intermediate symbol
 * @return 线性多项式 / Linear polynomial
 */
operator fun <V> LinearIntermediateSymbol<V>.minus(rhs: LinearIntermediateSymbol<V>): LinearPolynomial<V>
    where V : RealNumber<V>, V : NumberField<V> {
    val lhs = this.toLinearPolynomial()
    val rhsPoly = rhs.toLinearPolynomial()
    return LinearPolynomial(lhs.monomials + rhsPoly.monomials.map { LinearMonomial(-it.coefficient, it.symbol) }, lhs.constant - rhsPoly.constant)
}

// 求解器边界扩展统一委托给 SolverBoundaryCasts，避免分散 unchecked cast。
// Solver-boundary extensions delegate to SolverBoundaryCasts to avoid scattered unchecked casts.

/** 获取线性中间符号的 Flt64 求解器边界扁平化单项式数据。 / Get Flt64 solver-boundary flattened monomial data for linear intermediate symbol. */
internal val <V> LinearIntermediateSymbol<V>.solverFlattenedMonomials: LinearFlattenData<Flt64>
    where V : RealNumber<V>, V : Ring<V>, V : NumberField<V>
    get() = SolverBoundaryCasts.linearSolverFlattenedMonomials(this)

/** 获取二次中间符号的 Flt64 求解器边界扁平化单项式数据。 / Get Flt64 solver-boundary flattened monomial data for quadratic intermediate symbol. */
internal val <V> QuadraticIntermediateSymbol<V>.solverFlattenedMonomials: QuadraticFlattenData<Flt64>
    where V : RealNumber<V>, V : Ring<V>, V : NumberField<V>
    get() = SolverBoundaryCasts.quadraticSolverFlattenedMonomials(this)
