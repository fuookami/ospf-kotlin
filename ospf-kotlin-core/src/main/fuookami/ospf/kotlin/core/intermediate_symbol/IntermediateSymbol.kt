@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol

import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial as UtilsLinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial as UtilsQuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.MutableLinearPolynomial as UtilsMutableLinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.MutableQuadraticPolynomial as UtilsMutableQuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial as UtilsLinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial as UtilsQuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.adapter.ValueProvider
import fuookami.ospf.kotlin.math.symbol.adapter.MapValueProvider
import fuookami.ospf.kotlin.math.symbol.operation.evaluate
import fuookami.ospf.kotlin.core.model.basic.ExpressionRange
import fuookami.ospf.kotlin.math.symbol.operation.ToLinearPolynomial
import fuookami.ospf.kotlin.math.symbol.operation.ToQuadraticPolynomial
import fuookami.ospf.kotlin.core.model.mechanism.ToMathLinearInequality
import fuookami.ospf.kotlin.core.model.mechanism.ToMathQuadraticInequality
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.Flt64LinearInequality as MathLinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequality as MathQuadraticInequality
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModelF64
import fuookami.ospf.kotlin.core.model.mechanism.AbstractQuadraticMechanismModelF64
import fuookami.ospf.kotlin.core.token.AbstractTokenTable
import fuookami.ospf.kotlin.core.token.LinearFlattenDataF64
import fuookami.ospf.kotlin.core.token.QuadraticFlattenDataF64
import fuookami.ospf.kotlin.core.token.LinearFlattenData
import fuookami.ospf.kotlin.core.token.QuadraticFlattenData
import fuookami.ospf.kotlin.core.token.newTokenCacheKey
import fuookami.ospf.kotlin.core.token.boundTokenTableContext
import fuookami.ospf.kotlin.core.token.AbstractTokenListF64
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.token.AddableTokenCollectionF64
import fuookami.ospf.kotlin.core.variable.IdentifierGenerator
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.utils.functional.Variant3
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.math.symbol.Category
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.quantities.unit.reciprocal
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.Ring

interface IntermediateSymbol<V : RealNumber<V>> : Symbol {
    override var name: String
    override var displayName: String?

    val discrete: Boolean get() = false

    val range: ExpressionRange<Flt64>
    val lowerBound get() = range.lowerBound?.toFlt64()
    val upperBound get() = range.upperBound?.toFlt64()
    val fixedValue get() = range.fixedValue

    fun evaluate(tokenList: AbstractTokenListF64, zeroIfNone: Boolean = false): Flt64?
    fun evaluate(tokenTable: AbstractTokenTable<*>, zeroIfNone: Boolean = false): Flt64? {
        @Suppress("UNCHECKED_CAST")
        val tt = tokenTable as AbstractTokenTable<Flt64>
        return evaluate(tokenList = tt.tokenList, zeroIfNone = zeroIfNone)
    }

    fun evaluate(results: List<Flt64>, tokenList: AbstractTokenListF64, zeroIfNone: Boolean = false): Flt64?
    fun evaluate(results: List<Flt64>, tokenTable: AbstractTokenTable<*>, zeroIfNone: Boolean = false): Flt64? {
        @Suppress("UNCHECKED_CAST")
        val tt = tokenTable as AbstractTokenTable<Flt64>
        return evaluate(results = results, tokenList = tt.tokenList, zeroIfNone = zeroIfNone)
    }

    fun evaluate(values: Map<Symbol, Flt64>, tokenList: AbstractTokenListF64? = null, zeroIfNone: Boolean = false): Flt64?
    fun evaluate(values: Map<Symbol, Flt64>, tokenTable: AbstractTokenTable<*>?, zeroIfNone: Boolean = false): Flt64? {
        @Suppress("UNCHECKED_CAST")
        val tt = tokenTable as? AbstractTokenTable<Flt64>
        return evaluate(values = values, tokenList = tt?.tokenList, zeroIfNone = zeroIfNone)
    }

    val category: Category
    val operationCategory: Category get() = category

    val cached: Boolean
    val parent: IntermediateSymbol<*>? get() = null
    val args: Any? get() = parent?.args
    val dependencies: Set<IntermediateSymbol<*>>

    val identifier: UInt64
    val index: Int

    fun flush(force: Boolean = false)

    /**
     * Prepare (generic-first view).
     * Accepts any AbstractTokenTable regardless of V, since the return is Flt64.
     */
    fun prepare(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable<*>): Flt64?
    fun prepareAndCache(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable<*>) {
        @Suppress("UNCHECKED_CAST")
        val tt = tokenTable as AbstractTokenTable<Flt64>
        if (values.isNullOrEmpty()) {
            prepare(null, tokenTable)?.let {
                tt.cache(cacheKey = this, solution = null, value = it)
            }
        } else {
            prepare(values, tokenTable)?.let {
                tt.cache(cacheKey = this, fixedValues = values, value = it)
            }
        }
    }

    /**
     * Register auxiliary tokens needed by this symbol.
     * Aligns with Rust IntermediateSymbol::register_auxiliary_tokens.
     *
     * Called by TokenTable.register() after the symbol's constraints are registered.
     * Override this to register helper variables (slack, binary, auxiliary) separately
     * from constraint registration in register(). This separates "register constraints"
     * from "register auxiliary tokens" for clarity.
     *
     * Default: no-op (non-function symbols don't register tokens).
     * LinearFunctionSymbolAdapter overrides this to register helperVariables from its delegate.
     */
    /**
     * Register auxiliary tokens (helper variables) needed by this symbol into the given token collection.
     *
     * This method separates "register variables" from "register constraints" for clarity.
     * It is called by [register] as the first step before adding constraints, and may also
     * be called independently (e.g., by TokenTable) to add only the auxiliary variables.
     *
     * ## Convention
     *
     * - This method should **only** register tokens (variables), never constraints.
     * - Subclasses that create auxiliary tokens inside their [register] method should override
     *   this method to expose the same token registrations independently.
     * - The default implementation is a no-op. Subclasses with auxiliary tokens should override it.
     * - For composite functions (those that delegate to sub-functions), this method should register
     *   only the **own** auxiliary tokens of this symbol, not the sub-function's tokens
     *   (the sub-function handles its own registration via its own [register] method).
     *   Exception: if the sub-function's tokens are already part of this symbol's [helperVariables],
     *   the default [MathFunctionSymbol.registerAuxiliaryTokens] implementation will include them.
     *
     * @param tokens The token collection to add auxiliary variables to.
     * @return [Ok] on success, [Failed] or [Fatal] on error (e.g., duplicate token).
     */
    fun registerAuxiliaryTokens(tokens: AddableTokenCollectionF64): Try = ok

    /**
     * Evaluate this symbol from token table values.
     * Aligns with Rust IntermediateSymbol::evaluate_from_tokens.
     * Default: delegates to evaluate(tokenTable, zeroIfNone=false).
     */
    fun evaluateFromTokens(tokenTable: AbstractTokenTable<*>, zeroIfNone: Boolean = false): Flt64? {
        return evaluate(tokenTable, zeroIfNone)
    }

    /** V-typed prepare via IntoValue<V> conversion. */
    fun prepareAsV(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable<*>, converter: fuookami.ospf.kotlin.core.solver.value.IntoValue<V>): V? =
        prepare(values, tokenTable)?.let { converter.intoValue(it) }

    /** V-typed evaluate via IntoValue<V> conversion. */
    fun evaluateAsV(tokenList: AbstractTokenListF64, converter: fuookami.ospf.kotlin.core.solver.value.IntoValue<V>, zeroIfNone: Boolean = false): V? =
        evaluate(tokenList, zeroIfNone)?.let { converter.intoValue(it) }

    /** V-typed evaluate via IntoValue<V> conversion. */
    fun evaluateAsV(tokenTable: AbstractTokenTable<*>, converter: fuookami.ospf.kotlin.core.solver.value.IntoValue<V>, zeroIfNone: Boolean = false): V? =
        evaluate(tokenTable, zeroIfNone)?.let { converter.intoValue(it) }

    /** V-typed evaluate from tokens via IntoValue<V> conversion. */
    fun evaluateFromTokensAsV(tokenTable: AbstractTokenTable<*>, converter: fuookami.ospf.kotlin.core.solver.value.IntoValue<V>, zeroIfNone: Boolean = false): V? =
        evaluateFromTokens(tokenTable, zeroIfNone)?.let { converter.intoValue(it) }

    fun toRawString(unfold: UInt64 = UInt64.zero): String
}

interface LinearIntermediateSymbol<V> : IntermediateSymbol<V>, ToMathLinearInequality, ToMathQuadraticInequality, ToLinearPolynomial<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
    companion object {
        fun empty(
            parent: IntermediateSymbol<*>? = null,
            name: String = "",
            displayName: String? = null
        ): LinearIntermediateSymbol<Flt64> {
            return LinearExpressionSymbol<Flt64>(
                _utilsPolynomial = UtilsMutableLinearPolynomial(
                    monomials = emptyList(),
                    constant = Flt64.zero
                ),
                parent = parent,
                name = name,
                displayName = displayName
            )
        }
    }

    @Suppress("DEPRECATION")
    val flattenedMonomials: LinearFlattenDataF64

    val flattenedMonomialsAsV: LinearFlattenData<V>
        @Suppress("UNCHECKED_CAST")
        get() = flattenedMonomials as LinearFlattenData<V>

    val polynomial: UtilsLinearPolynomial<V>

    fun asMutable(): UtilsMutableLinearPolynomial<V>

    override fun toMathLinearInequality(): MathLinearInequality {
        val lhs = UtilsLinearPolynomial(
            monomials = flattenedMonomials.monomials.map { fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial(it.coefficient, it.symbol) },
            constant = flattenedMonomials.constant
        )
        return MathLinearInequality(lhs, UtilsLinearPolynomial(emptyList(), Flt64.one), Comparison.EQ)
    }

    override fun toMathQuadraticInequality(): MathQuadraticInequality {
        val linearPoly = toMathLinearPolynomial()
        return MathQuadraticInequality(
            UtilsQuadraticPolynomial(
                monomials = linearPoly.monomials.map { fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial.linear(it.coefficient, it.symbol) },
                constant = linearPoly.constant
            ),
            UtilsQuadraticPolynomial(emptyList(), Flt64.one),
            Comparison.EQ
        )
    }

    override fun toLinearPolynomial(): UtilsLinearPolynomial<V> = polynomial
}

interface QuadraticIntermediateSymbol<V> : IntermediateSymbol<V>, ToMathQuadraticInequality, ToQuadraticPolynomial<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
    companion object {
        fun empty(
            parent: IntermediateSymbol<*>? = null,
            name: String = "",
            displayName: String? = null
        ): QuadraticIntermediateSymbol<Flt64> {
            return QuadraticExpressionSymbol<Flt64>(
                _utilsPolynomial = UtilsMutableQuadraticPolynomial(
                    monomials = emptyList(),
                    constant = Flt64.zero
                ),
                parent = parent,
                name = name,
                displayName = displayName
            )
        }
    }

    @Suppress("DEPRECATION")
    val flattenedMonomials: QuadraticFlattenDataF64

    val flattenedMonomialsAsV: QuadraticFlattenData<V>
        @Suppress("UNCHECKED_CAST")
        get() = flattenedMonomials as QuadraticFlattenData<V>

    val polynomial: UtilsQuadraticPolynomial<V>

    fun asMutable(): UtilsMutableQuadraticPolynomial<V>

    override fun toMathQuadraticInequality(): MathQuadraticInequality {
        val lhs = UtilsQuadraticPolynomial(
            monomials = flattenedMonomials.monomials.map { fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial(it.coefficient, it.symbol1, it.symbol2) },
            constant = flattenedMonomials.constant
        )
        return MathQuadraticInequality(lhs, UtilsQuadraticPolynomial(emptyList(), Flt64.one), Comparison.EQ)
    }

    override fun toQuadraticPolynomial(): UtilsQuadraticPolynomial<V> = polynomial
}

internal fun IntermediateSymbol<*>.shouldPrepare(
    cacheKey: IntermediateSymbol<*>,
    values: Map<Symbol, Flt64>?,
    tokenTable: AbstractTokenTable<*>
): Boolean {
    @Suppress("UNCHECKED_CAST")
    val tt = tokenTable as AbstractTokenTable<Flt64>
    return (!values.isNullOrEmpty() || tt.cachedSolution) && if (values.isNullOrEmpty()) {
        tt.cached(cacheKey)
    } else {
        tt.cached(cacheKey, values)
    } == false
}

internal fun IntermediateSymbol<*>.shouldPrepare(
    values: Map<Symbol, Flt64>?,
    tokenTable: AbstractTokenTable<*>
): Boolean {
    return shouldPrepare(this, values, tokenTable)
}

internal fun IntermediateSymbol<*>.shouldPrepareWithFixedCacheKey(
    cacheKey: IntermediateSymbol<*>,
    values: Map<Symbol, Flt64>?,
    tokenTable: AbstractTokenTable<*>
): Boolean {
    @Suppress("UNCHECKED_CAST")
    val tt = tokenTable as AbstractTokenTable<Flt64>
    return (!values.isNullOrEmpty() || tt.cachedSolution) && tt.cached(cacheKey) == false
}

internal fun IntermediateSymbol<*>.shouldPrepareWithFixedCacheKey(
    values: Map<Symbol, Flt64>?,
    tokenTable: AbstractTokenTable<*>
): Boolean {
    return shouldPrepareWithFixedCacheKey(this, values, tokenTable)
}

internal inline fun <T> IntermediateSymbol<*>.prepareIfNotCached(
    cacheKey: IntermediateSymbol<*>,
    values: Map<Symbol, Flt64>?,
    tokenTable: AbstractTokenTable<*>,
    block: () -> T?
): T? {
    return if (shouldPrepare(cacheKey, values, tokenTable)) {
        block()
    } else {
        null
    }
}

internal inline fun <T> IntermediateSymbol<*>.prepareIfNotCached(
    values: Map<Symbol, Flt64>?,
    tokenTable: AbstractTokenTable<*>,
    block: () -> T?
): T? {
    return prepareIfNotCached(this, values, tokenTable, block)
}

internal inline fun <T> IntermediateSymbol<*>.prepareIfNotCachedWithFixedCacheKey(
    cacheKey: IntermediateSymbol<*>,
    values: Map<Symbol, Flt64>?,
    tokenTable: AbstractTokenTable<*>,
    block: () -> T?
): T? {
    return if (shouldPrepareWithFixedCacheKey(cacheKey, values, tokenTable)) {
        block()
    } else {
        null
    }
}

internal inline fun <T> IntermediateSymbol<*>.prepareIfNotCachedWithFixedCacheKey(
    values: Map<Symbol, Flt64>?,
    tokenTable: AbstractTokenTable<*>,
    block: () -> T?
): T? {
    return prepareIfNotCachedWithFixedCacheKey(this, values, tokenTable, block)
}

private fun IntermediateSymbol<*>.evaluateWithCachedTokenTable(
    tokenTable: AbstractTokenTable<*>,
    zeroIfNone: Boolean,
    calculator: () -> Flt64?
): Flt64? {
    @Suppress("UNCHECKED_CAST")
    val tt = tokenTable as AbstractTokenTable<Flt64>
    return if (tt.cachedSolution) {
        tt.cacheIfNotCached(
            cacheKey = this,
            solution = null
        ) {
            for (dependency in dependencies) {
                if (tt.cachedSolution) {
                    dependency.evaluate(
                        tokenTable = tokenTable,
                        zeroIfNone = zeroIfNone
                    )
                }
            }
            calculator()
        }
    } else {
        tt.cachedValue(
            cacheKey = this,
            solution = null
        )
    }
}

private fun IntermediateSymbol<*>.evaluateWithCachedTokenTable(
    results: List<Flt64>,
    tokenTable: AbstractTokenTable<*>,
    zeroIfNone: Boolean,
    calculator: () -> Flt64?
): Flt64? {
    @Suppress("UNCHECKED_CAST")
    val tt = tokenTable as AbstractTokenTable<Flt64>
    return if (tt.cachedSolution) {
        tt.cacheIfNotCached(this, results) {
            for (dependency in dependencies) {
                if (tt.cachedSolution) {
                    dependency.evaluate(
                        results = results,
                        tokenTable = tokenTable,
                        zeroIfNone = zeroIfNone
                    )
                }
            }
            calculator()
        }
    } else {
        tt.cachedValue(this, results)
    }
}

private fun IntermediateSymbol<*>.evaluateWithCachedTokenTable(
    values: Map<Symbol, Flt64>,
    tokenTable: AbstractTokenTable<*>?,
    zeroIfNone: Boolean,
    calculator: () -> Flt64?
): Flt64? {
    @Suppress("UNCHECKED_CAST")
    val tt = tokenTable as? AbstractTokenTable<Flt64>
    values[this]?.let { value ->
        tt?.cache(
            cacheKey = this,
            fixedValues = values,
            value = value
        )
        return value
    }

    if (tt == null) {
        return calculator()
    }

    return if (values.isNotEmpty() || tt.cachedSolution) {
        tt.cacheIfNotCached(this, values) {
            for (dependency in dependencies) {
                if (values.isNotEmpty() || tt.cachedSolution) {
                    dependency.evaluate(
                        values = values,
                        tokenTable = tokenTable,
                        zeroIfNone = zeroIfNone
                    )
                }
            }
            calculator()
        }
    } else {
        tt.cachedValue(this, values)
    }
}

class LinearExpressionSymbol<V>(
    internal val _utilsPolynomial: UtilsMutableLinearPolynomial<V>,
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
        operator fun invoke(
            item: AbstractVariableItem<*, *>,
            parent: IntermediateSymbol<*>? = null,
            name: String = "",
            displayName: String? = null
        ): LinearExpressionSymbol<Flt64> {
            return LinearExpressionSymbol<Flt64>(
                _utilsPolynomial = UtilsMutableLinearPolynomial(
                    monomials = listOf(UtilsLinearMonomial(Flt64.one, item)),
                    constant = Flt64.zero
                ),
                category = Linear,
                parent = parent,
                name = name.ifEmpty { item.name },
                displayName = displayName
            )
        }

        operator fun invoke(
            symbol: LinearIntermediateSymbol<*>,
            parent: IntermediateSymbol<*>? = null,
            name: String = "",
            displayName: String? = null
        ): LinearExpressionSymbol<Flt64> {
            return LinearExpressionSymbol<Flt64>(
                _utilsPolynomial = UtilsMutableLinearPolynomial(
                    monomials = listOf(UtilsLinearMonomial(Flt64.one, symbol)),
                    constant = Flt64.zero
                ),
                category = Linear,
                parent = parent,
                name = name.ifEmpty { symbol.name },
                displayName = displayName ?: symbol.displayName
            )
        }

        operator fun invoke(
            polynomial: UtilsLinearPolynomial<Flt64>,
            parent: IntermediateSymbol<*>? = null,
            name: String = "",
            displayName: String? = null
        ): LinearExpressionSymbol<Flt64> {
            return LinearExpressionSymbol<Flt64>(
                _utilsPolynomial = UtilsMutableLinearPolynomial(
                    monomials = polynomial.monomials,
                    constant = polynomial.constant
                ),
                category = Linear,
                parent = parent,
                name = name.ifEmpty { polynomial.toString() },
                displayName = displayName
            )
        }

        operator fun invoke(
            monomial: UtilsLinearMonomial<Flt64>,
            parent: IntermediateSymbol<*>? = null,
            name: String = "",
            displayName: String? = null
        ): LinearExpressionSymbol<Flt64> {
            return LinearExpressionSymbol<Flt64>(
                _utilsPolynomial = UtilsMutableLinearPolynomial(
                    monomials = listOf(monomial),
                    constant = Flt64.zero
                ),
                category = Linear,
                parent = parent,
                name = name.ifEmpty { monomial.toString() },
                displayName = displayName
            )
        }

        operator fun invoke(
            polynomial: UtilsMutableLinearPolynomial<Flt64>,
            parent: IntermediateSymbol<*>? = null,
            name: String = "",
            displayName: String? = null
        ): LinearExpressionSymbol<Flt64> {
            return LinearExpressionSymbol<Flt64>(
                _utilsPolynomial = polynomial,
                category = Linear,
                parent = parent,
                name = name.ifEmpty { polynomial.toString() },
                displayName = displayName
            )
        }

        operator fun invoke(
            constant: Int,
            parent: IntermediateSymbol<*>? = null,
            name: String = "",
            displayName: String? = null
        ): LinearExpressionSymbol<Flt64> {
            return LinearExpressionSymbol<Flt64>(
                _utilsPolynomial = UtilsMutableLinearPolynomial(
                    monomials = emptyList(),
                    constant = Flt64(constant)
                ),
                category = Linear,
                parent = parent,
                name = name,
                displayName = displayName
            )
        }

        operator fun invoke(
            constant: Double,
            parent: IntermediateSymbol<*>? = null,
            name: String = "",
            displayName: String? = null
        ): LinearExpressionSymbol<Flt64> {
            return LinearExpressionSymbol<Flt64>(
                _utilsPolynomial = UtilsMutableLinearPolynomial(
                    monomials = emptyList(),
                    constant = Flt64(constant)
                ),
                category = Linear,
                parent = parent,
                name = name,
                displayName = displayName
            )
        }

        operator fun invoke(
            constant: Boolean,
            parent: IntermediateSymbol<*>? = null,
            name: String = "",
            displayName: String? = null
        ): LinearExpressionSymbol<Flt64> {
            return LinearExpressionSymbol<Flt64>(
                _utilsPolynomial = UtilsMutableLinearPolynomial(
                    monomials = emptyList(),
                    constant = if (constant) Flt64.one else Flt64.zero
                ),
                category = Linear,
                parent = parent,
                name = name,
                displayName = displayName
            )
        }

        operator fun invoke(
            constant: Trivalent,
            parent: IntermediateSymbol<*>? = null,
            name: String = "",
            displayName: String? = null
        ): LinearExpressionSymbol<Flt64> {
            return LinearExpressionSymbol<Flt64>(
                _utilsPolynomial = UtilsMutableLinearPolynomial(
                    monomials = emptyList(),
                    constant = constant.value.toFlt64()
                ),
                category = Linear,
                parent = parent,
                name = name,
                displayName = displayName
            )
        }

        operator fun invoke(
            constant: BalancedTrivalent,
            parent: IntermediateSymbol<*>? = null,
            name: String = "",
            displayName: String? = null
        ): LinearExpressionSymbol<Flt64> {
            return LinearExpressionSymbol<Flt64>(
                _utilsPolynomial = UtilsMutableLinearPolynomial(
                    monomials = emptyList(),
                    constant = constant.value.toFlt64()
                ),
                category = Linear,
                parent = parent,
                name = name,
                displayName = displayName
            )
        }

        operator fun <T : RealNumber<T>> invoke(
            constant: T,
            parent: IntermediateSymbol<*>? = null,
            name: String = "",
            displayName: String? = null
        ): LinearExpressionSymbol<Flt64> {
            return LinearExpressionSymbol<Flt64>(
                _utilsPolynomial = UtilsMutableLinearPolynomial(
                    monomials = emptyList(),
                    constant = constant.toFlt64()
                ),
                category = Linear,
                parent = parent,
                name = name,
                displayName = displayName
            )
        }

        operator fun invoke(
            parent: IntermediateSymbol<*>? = null,
            name: String = "",
            displayName: String? = null
        ): LinearExpressionSymbol<Flt64> {
            return LinearExpressionSymbol<Flt64>(
                _utilsPolynomial = UtilsMutableLinearPolynomial(
                    monomials = emptyList(),
                    constant = Flt64.zero
                ),
                category = Linear,
                parent = parent,
                name = name,
                displayName = displayName
            )
        }
    }

    // Flt64 view of internal polynomial (all runtime instances are V=Flt64)
    @Suppress("UNCHECKED_CAST")
    private val _polyF64: UtilsMutableLinearPolynomial<Flt64> get() = _utilsPolynomial as UtilsMutableLinearPolynomial<Flt64>

    // polynomial property returns immutable version
    override val polynomial: UtilsLinearPolynomial<V> get() = _utilsPolynomial.toLinearPolynomial()

    // flattenedMonomials: extract from monomials
    @Suppress("DEPRECATION")
    override val flattenedMonomials: LinearFlattenDataF64
        get() = LinearFlattenDataF64(
            monomials = _polyF64.monomials,
            constant = _polyF64.constant
        )

    override val flattenedMonomialsAsV: LinearFlattenData<V>
        get() = LinearFlattenData(
            monomials = _utilsPolynomial.monomials,
            constant = _utilsPolynomial.constant
        )

    // asMutable returns UtilsMutableLinearPolynomial<V>
    override fun asMutable(): UtilsMutableLinearPolynomial<V> {
        return _utilsPolynomial
    }

    // Helper: evaluate a single monomial's symbol
    private fun evaluateSymbol(symbol: Symbol, tokenList: AbstractTokenListF64, zeroIfNone: Boolean): Flt64? {
        return when (symbol) {
            is AbstractVariableItem<*, *> -> {
                val token = tokenList.find(symbol)
                token?.resultF64 ?: if (zeroIfNone) Flt64.zero else null
            }
            is LinearIntermediateSymbol<*> -> symbol.evaluate(tokenList, zeroIfNone)
            else -> null
        }
    }

    private fun evaluateSymbol(symbol: Symbol, tokenTable: AbstractTokenTable<*>, zeroIfNone: Boolean): Flt64? {
        @Suppress("UNCHECKED_CAST")
        val tt = tokenTable as AbstractTokenTable<Flt64>
        return when (symbol) {
            is AbstractVariableItem<*, *> -> {
                val token = tt.find(symbol)
                token?.result ?: if (zeroIfNone) Flt64.zero else null
            }
            is LinearIntermediateSymbol<*> -> symbol.evaluate(tokenTable, zeroIfNone)
            else -> null
        }
    }

    private fun evaluateSymbol(symbol: Symbol, results: List<Flt64>, tokenList: AbstractTokenListF64, zeroIfNone: Boolean): Flt64? {
        return when (symbol) {
            is AbstractVariableItem<*, *> -> {
                val index = tokenList.indexOf(symbol)
                if (index != null && index >= 0 && index < results.size) results[index]
                else if (zeroIfNone) Flt64.zero else null
            }
            is LinearIntermediateSymbol<*> -> symbol.evaluate(results, tokenList, zeroIfNone)
            else -> null
        }
    }

    private fun evaluateSymbol(symbol: Symbol, results: List<Flt64>, tokenTable: AbstractTokenTable<*>, zeroIfNone: Boolean): Flt64? {
        return when (symbol) {
            is AbstractVariableItem<*, *> -> {
                val index = tokenTable.indexOf(symbol)
                if (index != null && index >= 0 && index < results.size) results[index]
                else if (zeroIfNone) Flt64.zero else null
            }
            is LinearIntermediateSymbol<*> -> symbol.evaluate(results, tokenTable, zeroIfNone)
            else -> null
        }
    }

    private fun evaluateSymbol(symbol: Symbol, values: Map<Symbol, Flt64>, tokenList: AbstractTokenListF64?, zeroIfNone: Boolean): Flt64? {
        return when (symbol) {
            is AbstractVariableItem<*, *> -> {
                values[symbol] ?: tokenList?.find(symbol)?.resultF64 ?: if (zeroIfNone) Flt64.zero else null
            }
            is LinearIntermediateSymbol<*> -> symbol.evaluate(values, tokenList, zeroIfNone)
            else -> values[symbol] ?: if (zeroIfNone) Flt64.zero else null
        }
    }

    private fun evaluateSymbol(symbol: Symbol, values: Map<Symbol, Flt64>, tokenTable: AbstractTokenTable<*>?, zeroIfNone: Boolean): Flt64? {
        @Suppress("UNCHECKED_CAST")
        val tt = tokenTable as? AbstractTokenTable<Flt64>
        return when (symbol) {
            is AbstractVariableItem<*, *> -> {
                values[symbol] ?: tt?.find(symbol)?.result ?: if (zeroIfNone) Flt64.zero else null
            }
            is LinearIntermediateSymbol<*> -> symbol.evaluate(values, tokenTable, zeroIfNone)
            else -> values[symbol] ?: if (zeroIfNone) Flt64.zero else null
        }
    }

    // dependencies: extract IntermediateSymbol from monomials.symbol
    override val dependencies: Set<IntermediateSymbol<*>>
        get() = _utilsPolynomial.monomials
            .mapNotNull { monomial ->
                when (val sym = monomial.symbol) {
                    is LinearIntermediateSymbol<*> -> sym
                    else -> null
                }
            }
            .toSet()

    // range/lowerBound/upperBound using possibleRange algorithm
    private fun calculateRange(): ValueRange<Flt64>? {
        val poly = _polyF64
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

    override val range: ExpressionRange<Flt64>
        get() = ExpressionRange(calculateRange(), Flt64)

    // discrete: check if all monomials are discrete and constant is integral
    override val discrete: Boolean
        get() = _polyF64.monomials.all { monomial ->
            when (val sym = monomial.symbol) {
                is AbstractVariableItem<*, *> -> sym.type.isIntegerType && monomial.coefficient.round() eq monomial.coefficient
                is LinearIntermediateSymbol<*> -> sym.discrete && monomial.coefficient.round() eq monomial.coefficient
                else -> false
            }
        } && _polyF64.constant.round() eq _polyF64.constant

    // cached/flush via TokenTable cache key
    private val rangeCacheKey = newTokenCacheKey(
        category = Linear,
        prefix = "__linear_expression_range_cache__"
    )
    private val flattenCacheKey = newTokenCacheKey(
        category = Linear,
        prefix = "__linear_expression_flatten_cache__"
    )

    @Suppress("UNCHECKED_CAST")
    private fun cacheTokenTable(): AbstractTokenTable<Flt64>? {
        return dependencies
            .asSequence()
            .mapNotNull { boundTokenTableContext(it) }
            .firstOrNull() as? AbstractTokenTable<Flt64>
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

    // prepare: using math.evaluate + ValueProvider
    override fun prepare(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable<*>): Flt64? {
        // Trigger flattenedMonomials to populate cache
        val flatten = flattenedMonomials

        return if (values.isNullOrEmpty()) {
            var ret = _polyF64.constant
            for (monomial in _polyF64.monomials) {
                val symbolValue = evaluateSymbol(monomial.symbol, tokenTable, false)
                if (symbolValue == null) return null
                ret += monomial.coefficient * symbolValue
            }
            ret
        } else {
            var ret = _polyF64.constant
            for (monomial in _polyF64.monomials) {
                val symbolValue = evaluateSymbol(monomial.symbol, values, tokenTable, false)
                if (symbolValue == null) return null
                ret += monomial.coefficient * symbolValue
            }
            ret
        }
    }

    // toRawString
    override fun toRawString(unfold: UInt64): String {
        return if (unfold neq UInt64.zero) {
            val monomialStrings = _polyF64.monomials.filter { it.coefficient neq Flt64.zero }.map { m ->
                val symStr = when (val sym = m.symbol) {
                    is IntermediateSymbol<*> -> sym.toRawString(unfold - UInt64.one)
                    else -> sym.name
                }
                if (m.coefficient eq Flt64.one) symStr
                else if (m.coefficient eq -Flt64.one) "-$symStr"
                else "${m.coefficient} * $symStr"
            }
            if (_polyF64.constant neq Flt64.zero) {
                "${monomialStrings.joinToString(" + ")} + ${_polyF64.constant}"
            } else {
                monomialStrings.joinToString(" + ")
            }
        } else {
            displayName ?: name
        }
    }

    // evaluate methods - similar to Polynomial.evaluate implementation
    override fun evaluate(tokenList: AbstractTokenListF64, zeroIfNone: Boolean): Flt64? {
        var ret = _polyF64.constant
        for (monomial in _polyF64.monomials) {
            val symbolValue = evaluateSymbol(monomial.symbol, tokenList, zeroIfNone)
            if (symbolValue == null && !zeroIfNone) return null
            ret += monomial.coefficient * (symbolValue ?: Flt64.zero)
        }
        return ret
    }

    override fun evaluate(tokenTable: AbstractTokenTable<*>, zeroIfNone: Boolean): Flt64? {
        return evaluateWithCachedTokenTable(tokenTable, zeroIfNone) {
            var ret = _polyF64.constant
            for (monomial in _polyF64.monomials) {
                val symbolValue = evaluateSymbol(monomial.symbol, tokenTable, zeroIfNone)
                if (symbolValue == null && !zeroIfNone) return@evaluateWithCachedTokenTable null
                ret += monomial.coefficient * (symbolValue ?: Flt64.zero)
            }
            ret
        }
    }

    override fun evaluate(results: List<Flt64>, tokenList: AbstractTokenListF64, zeroIfNone: Boolean): Flt64? {
        var ret = _polyF64.constant
        for (monomial in _polyF64.monomials) {
            val symbolValue = evaluateSymbol(monomial.symbol, results, tokenList, zeroIfNone)
            if (symbolValue == null && !zeroIfNone) return null
            ret += monomial.coefficient * (symbolValue ?: Flt64.zero)
        }
        return ret
    }

    override fun evaluate(results: List<Flt64>, tokenTable: AbstractTokenTable<*>, zeroIfNone: Boolean): Flt64? {
        return evaluateWithCachedTokenTable(results, tokenTable, zeroIfNone) {
            var ret = _polyF64.constant
            for (monomial in _polyF64.monomials) {
                val symbolValue = evaluateSymbol(monomial.symbol, results, tokenTable, zeroIfNone)
                if (symbolValue == null && !zeroIfNone) return@evaluateWithCachedTokenTable null
                ret += monomial.coefficient * (symbolValue ?: Flt64.zero)
            }
            ret
        }
    }

    override fun evaluate(values: Map<Symbol, Flt64>, tokenList: AbstractTokenListF64?, zeroIfNone: Boolean): Flt64? {
        if (values.containsKey(this)) return values[this]!!
        var ret = _polyF64.constant
        for (monomial in _polyF64.monomials) {
            val symbolValue = evaluateSymbol(monomial.symbol, values, tokenList, zeroIfNone)
            if (symbolValue == null && !zeroIfNone) return null
            ret += monomial.coefficient * (symbolValue ?: Flt64.zero)
        }
        return ret
    }

    override fun evaluate(values: Map<Symbol, Flt64>, tokenTable: AbstractTokenTable<*>?, zeroIfNone: Boolean): Flt64? {
        return evaluateWithCachedTokenTable(values, tokenTable, zeroIfNone) {
            if (values.containsKey(this)) {
                return@evaluateWithCachedTokenTable values[this]!!
            }
            var ret = _polyF64.constant
            for (monomial in _polyF64.monomials) {
                val symbolValue = evaluateSymbol(monomial.symbol, values, tokenTable, zeroIfNone)
                if (symbolValue == null && !zeroIfNone) {
                    return@evaluateWithCachedTokenTable null
                }
                ret += monomial.coefficient * (symbolValue ?: Flt64.zero)
            }
            ret
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
        if (other !is LinearExpressionSymbol<*>) return false

        if (identifier != other.identifier) return false
        if (index != other.index) return false
        if (name != other.name) return false

        return true
    }
}

class QuadraticExpressionSymbol<V>(
    internal val _utilsPolynomial: UtilsMutableQuadraticPolynomial<V>,
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
        operator fun invoke(
            item: AbstractVariableItem<*, *>,
            parent: IntermediateSymbol<*>? = null,
            name: String = "",
            displayName: String? = null
        ): QuadraticExpressionSymbol<Flt64> {
            return QuadraticExpressionSymbol<Flt64>(
                _utilsPolynomial = UtilsMutableQuadraticPolynomial(
                    monomials = listOf(UtilsQuadraticMonomial.linear(Flt64.one, item)),
                    constant = Flt64.zero
                ),
                category = Quadratic,
                parent = parent,
                name = name.ifEmpty { item.name },
                displayName = displayName
            )
        }

        operator fun invoke(
            symbol: LinearIntermediateSymbol<*>,
            parent: IntermediateSymbol<*>? = null,
            name: String = "",
            displayName: String? = null
        ): QuadraticExpressionSymbol<Flt64> {
            return QuadraticExpressionSymbol<Flt64>(
                _utilsPolynomial = UtilsMutableQuadraticPolynomial(
                    monomials = listOf(UtilsQuadraticMonomial.linear(Flt64.one, symbol)),
                    constant = Flt64.zero
                ),
                category = Quadratic,
                parent = parent,
                name = name.ifEmpty { symbol.name },
                displayName = displayName ?: symbol.displayName
            )
        }

        operator fun invoke(
            symbol: QuadraticIntermediateSymbol<*>,
            parent: IntermediateSymbol<*>? = null,
            name: String = "",
            displayName: String? = null
        ): QuadraticExpressionSymbol<Flt64> {
            return QuadraticExpressionSymbol<Flt64>(
                _utilsPolynomial = UtilsMutableQuadraticPolynomial(
                    monomials = listOf(UtilsQuadraticMonomial.linear(Flt64.one, symbol)),
                    constant = Flt64.zero
                ),
                category = Quadratic,
                parent = parent,
                name = name.ifEmpty { symbol.name },
                displayName = displayName ?: symbol.displayName
            )
        }

        operator fun invoke(
            constant: Int,
            parent: IntermediateSymbol<*>? = null,
            name: String = "",
            displayName: String? = null
        ): QuadraticExpressionSymbol<Flt64> {
            return QuadraticExpressionSymbol<Flt64>(
                _utilsPolynomial = UtilsMutableQuadraticPolynomial(
                    monomials = emptyList(),
                    constant = Flt64(constant)
                ),
                category = Quadratic,
                parent = parent,
                name = name,
                displayName = displayName
            )
        }

        operator fun invoke(
            constant: Double,
            parent: IntermediateSymbol<*>? = null,
            name: String = "",
            displayName: String? = null
        ): QuadraticExpressionSymbol<Flt64> {
            return QuadraticExpressionSymbol<Flt64>(
                _utilsPolynomial = UtilsMutableQuadraticPolynomial(
                    monomials = emptyList(),
                    constant = Flt64(constant)
                ),
                category = Quadratic,
                parent = parent,
                name = name,
                displayName = displayName
            )
        }

        operator fun invoke(
            constant: Boolean,
            parent: IntermediateSymbol<*>? = null,
            name: String = "",
            displayName: String? = null
        ): QuadraticExpressionSymbol<Flt64> {
            return QuadraticExpressionSymbol<Flt64>(
                _utilsPolynomial = UtilsMutableQuadraticPolynomial(
                    monomials = emptyList(),
                    constant = if (constant) Flt64.one else Flt64.zero
                ),
                category = Quadratic,
                parent = parent,
                name = name,
                displayName = displayName
            )
        }

        operator fun invoke(
            constant: Trivalent,
            parent: IntermediateSymbol<*>? = null,
            name: String = "",
            displayName: String? = null
        ): QuadraticExpressionSymbol<Flt64> {
            return QuadraticExpressionSymbol<Flt64>(
                _utilsPolynomial = UtilsMutableQuadraticPolynomial(
                    monomials = emptyList(),
                    constant = constant.value.toFlt64()
                ),
                category = Quadratic,
                parent = parent,
                name = name,
                displayName = displayName
            )
        }

        operator fun invoke(
            constant: BalancedTrivalent,
            parent: IntermediateSymbol<*>? = null,
            name: String = "",
            displayName: String? = null
        ): QuadraticExpressionSymbol<Flt64> {
            return QuadraticExpressionSymbol<Flt64>(
                _utilsPolynomial = UtilsMutableQuadraticPolynomial(
                    monomials = emptyList(),
                    constant = constant.value.toFlt64()
                ),
                category = Quadratic,
                parent = parent,
                name = name,
                displayName = displayName
            )
        }

        operator fun <T : RealNumber<T>> invoke(
            constant: T,
            parent: IntermediateSymbol<*>? = null,
            name: String = "",
            displayName: String? = null
        ): QuadraticExpressionSymbol<Flt64> {
            return QuadraticExpressionSymbol<Flt64>(
                _utilsPolynomial = UtilsMutableQuadraticPolynomial(
                    monomials = emptyList(),
                    constant = constant.toFlt64()
                ),
                category = Quadratic,
                parent = parent,
                name = name,
                displayName = displayName
            )
        }

        operator fun invoke(
            parent: IntermediateSymbol<*>? = null,
            name: String = "",
            displayName: String? = null
        ): QuadraticExpressionSymbol<Flt64> {
            return QuadraticExpressionSymbol<Flt64>(
                _utilsPolynomial = UtilsMutableQuadraticPolynomial(
                    monomials = emptyList(),
                    constant = Flt64.zero
                ),
                category = Quadratic,
                parent = parent,
                name = name,
                displayName = displayName
            )
        }
    }

    // Flt64 view of internal polynomial (all runtime instances are V=Flt64)
    @Suppress("UNCHECKED_CAST")
    private val _polyF64: UtilsMutableQuadraticPolynomial<Flt64> get() = _utilsPolynomial as UtilsMutableQuadraticPolynomial<Flt64>

    // polynomial property returns immutable version
    override val polynomial: UtilsQuadraticPolynomial<V> get() = _utilsPolynomial.toQuadraticPolynomial()

    // flattenedMonomials: extract from monomials (distinguish linear vs quadratic)
    @Suppress("DEPRECATION")
    override val flattenedMonomials: QuadraticFlattenDataF64
        get() = QuadraticFlattenDataF64(
            monomials = _polyF64.monomials,
            constant = _polyF64.constant
        )

    override val flattenedMonomialsAsV: QuadraticFlattenData<V>
        get() = QuadraticFlattenData(
            monomials = _utilsPolynomial.monomials,
            constant = _utilsPolynomial.constant
        )

    // asMutable returns UtilsMutableQuadraticPolynomial<V>
    override fun asMutable(): UtilsMutableQuadraticPolynomial<V> {
        return _utilsPolynomial
    }

    // Helper: evaluate a single symbol
    private fun evaluateSymbol(symbol: Symbol, tokenList: AbstractTokenListF64, zeroIfNone: Boolean): Flt64? {
        return when (symbol) {
            is AbstractVariableItem<*, *> -> {
                val token = tokenList.find(symbol)
                token?.resultF64 ?: if (zeroIfNone) Flt64.zero else null
            }
            is LinearIntermediateSymbol<*> -> symbol.evaluate(tokenList, zeroIfNone)
            is QuadraticIntermediateSymbol<*> -> symbol.evaluate(tokenList, zeroIfNone)
            else -> null
        }
    }

    private fun evaluateSymbol(symbol: Symbol, tokenTable: AbstractTokenTable<*>, zeroIfNone: Boolean): Flt64? {
        @Suppress("UNCHECKED_CAST")
        val tt = tokenTable as AbstractTokenTable<Flt64>
        return when (symbol) {
            is AbstractVariableItem<*, *> -> {
                val token = tt.find(symbol)
                token?.result ?: if (zeroIfNone) Flt64.zero else null
            }
            is LinearIntermediateSymbol<*> -> symbol.evaluate(tokenTable, zeroIfNone)
            is QuadraticIntermediateSymbol<*> -> symbol.evaluate(tokenTable, zeroIfNone)
            else -> null
        }
    }

    private fun evaluateSymbol(symbol: Symbol, results: List<Flt64>, tokenList: AbstractTokenListF64, zeroIfNone: Boolean): Flt64? {
        return when (symbol) {
            is AbstractVariableItem<*, *> -> {
                val index = tokenList.indexOf(symbol)
                if (index != null && index >= 0 && index < results.size) results[index]
                else if (zeroIfNone) Flt64.zero else null
            }
            is LinearIntermediateSymbol<*> -> symbol.evaluate(results, tokenList, zeroIfNone)
            is QuadraticIntermediateSymbol<*> -> symbol.evaluate(results, tokenList, zeroIfNone)
            else -> null
        }
    }

    private fun evaluateSymbol(symbol: Symbol, results: List<Flt64>, tokenTable: AbstractTokenTable<*>, zeroIfNone: Boolean): Flt64? {
        return when (symbol) {
            is AbstractVariableItem<*, *> -> {
                val index = tokenTable.indexOf(symbol)
                if (index != null && index >= 0 && index < results.size) results[index]
                else if (zeroIfNone) Flt64.zero else null
            }
            is LinearIntermediateSymbol<*> -> symbol.evaluate(results, tokenTable, zeroIfNone)
            is QuadraticIntermediateSymbol<*> -> symbol.evaluate(results, tokenTable, zeroIfNone)
            else -> null
        }
    }

    private fun evaluateSymbol(symbol: Symbol, values: Map<Symbol, Flt64>, tokenList: AbstractTokenListF64?, zeroIfNone: Boolean): Flt64? {
        return when (symbol) {
            is AbstractVariableItem<*, *> -> {
                values[symbol] ?: tokenList?.find(symbol)?.resultF64 ?: if (zeroIfNone) Flt64.zero else null
            }
            is LinearIntermediateSymbol<*> -> symbol.evaluate(values, tokenList, zeroIfNone)
            is QuadraticIntermediateSymbol<*> -> symbol.evaluate(values, tokenList, zeroIfNone)
            else -> values[symbol] ?: if (zeroIfNone) Flt64.zero else null
        }
    }

    private fun evaluateSymbol(symbol: Symbol, values: Map<Symbol, Flt64>, tokenTable: AbstractTokenTable<*>?, zeroIfNone: Boolean): Flt64? {
        @Suppress("UNCHECKED_CAST")
        val tt = tokenTable as? AbstractTokenTable<Flt64>
        return when (symbol) {
            is AbstractVariableItem<*, *> -> {
                values[symbol] ?: tt?.find(symbol)?.result ?: if (zeroIfNone) Flt64.zero else null
            }
            is LinearIntermediateSymbol<*> -> symbol.evaluate(values, tokenTable, zeroIfNone)
            is QuadraticIntermediateSymbol<*> -> symbol.evaluate(values, tokenTable, zeroIfNone)
            else -> values[symbol] ?: if (zeroIfNone) Flt64.zero else null
        }
    }

    // dependencies: extract IntermediateSymbol from monomials.symbol1/symbol2
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

    // range/lowerBound/upperBound using possibleRange algorithm
    private fun calculateRange(): ValueRange<Flt64>? {
        var range: ValueRange<Flt64>? = ValueRange(_polyF64.constant, Flt64).value
        for (monomial in _polyF64.monomials) {
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
                // For quadratic term: coefficient * range1 * range2
                val termRange = (monomial.coefficient * s1r)?.times(s2r)
                range = range?.let { r -> termRange?.let { s -> r + s } }
            } else {
                range = null
                break
            }
        }
        return range
    }

    override val range: ExpressionRange<Flt64>
        get() = ExpressionRange(calculateRange(), Flt64)

    // discrete: check if all monomials are discrete and constant is integral
    override val discrete: Boolean
        get() = _polyF64.monomials.all { monomial ->
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
        } && _polyF64.constant.round() eq _polyF64.constant

    // cached/flush via TokenTable cache key
    private val rangeCacheKey = newTokenCacheKey(
        category = Quadratic,
        prefix = "__quadratic_expression_range_cache__"
    )
    private val flattenCacheKey = newTokenCacheKey(
        category = Quadratic,
        prefix = "__quadratic_expression_flatten_cache__"
    )

    @Suppress("UNCHECKED_CAST")
    private fun cacheTokenTable(): AbstractTokenTable<Flt64>? {
        return dependencies
            .asSequence()
            .mapNotNull { boundTokenTableContext(it) }
            .firstOrNull() as? AbstractTokenTable<Flt64>
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

    // prepare: using math.evaluate + ValueProvider
    override fun prepare(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable<*>): Flt64? {
        // Trigger flattenedMonomials to populate cache
        val flatten = flattenedMonomials

        return if (values.isNullOrEmpty()) {
            var ret = _polyF64.constant
            for (monomial in _polyF64.monomials) {
                val sym1Value = evaluateSymbol(monomial.symbol1, tokenTable, false)
                if (sym1Value == null) return null
                val termValue = if (monomial.symbol2 != null) {
                    val sym2Value = evaluateSymbol(monomial.symbol2!!, tokenTable, false)
                    if (sym2Value == null) return null
                    sym1Value * sym2Value
                } else sym1Value
                ret += monomial.coefficient * termValue
            }
            ret
        } else {
            var ret = _polyF64.constant
            for (monomial in _polyF64.monomials) {
                val sym1Value = evaluateSymbol(monomial.symbol1, values, tokenTable, false)
                if (sym1Value == null) return null
                val termValue = if (monomial.symbol2 != null) {
                    val sym2Value = evaluateSymbol(monomial.symbol2!!, values, tokenTable, false)
                    if (sym2Value == null) return null
                    sym1Value * sym2Value
                } else sym1Value
                ret += monomial.coefficient * termValue
            }
            ret
        }
    }

    // toRawString
    override fun toRawString(unfold: UInt64): String {
        return if (unfold neq UInt64.zero) {
            val monomialStrings = _polyF64.monomials.filter { it.coefficient neq Flt64.zero }.map { m ->
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
            if (_polyF64.constant neq Flt64.zero) {
                "${monomialStrings.joinToString(" + ")} + ${_polyF64.constant}"
            } else {
                monomialStrings.joinToString(" + ")
            }
        } else {
            displayName ?: name
        }
    }

    // evaluate methods - similar to Polynomial.evaluate implementation
    override fun evaluate(tokenList: AbstractTokenListF64, zeroIfNone: Boolean): Flt64? {
        var ret = _polyF64.constant
        for (monomial in _polyF64.monomials) {
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

    override fun evaluate(tokenTable: AbstractTokenTable<*>, zeroIfNone: Boolean): Flt64? {
        return evaluateWithCachedTokenTable(tokenTable, zeroIfNone) {
            var ret = _polyF64.constant
            for (monomial in _polyF64.monomials) {
                val sym1Value = evaluateSymbol(monomial.symbol1, tokenTable, zeroIfNone)
                if (sym1Value == null && !zeroIfNone) return@evaluateWithCachedTokenTable null
                val termValue = if (monomial.symbol2 != null) {
                    val sym2Value = evaluateSymbol(monomial.symbol2!!, tokenTable, zeroIfNone)
                    if (sym2Value == null && !zeroIfNone) return@evaluateWithCachedTokenTable null
                    (sym1Value ?: Flt64.zero) * (sym2Value ?: Flt64.zero)
                } else (sym1Value ?: Flt64.zero)
                ret += monomial.coefficient * termValue
            }
            ret
        }
    }

    override fun evaluate(results: List<Flt64>, tokenList: AbstractTokenListF64, zeroIfNone: Boolean): Flt64? {
        var ret = _polyF64.constant
        for (monomial in _polyF64.monomials) {
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

    override fun evaluate(results: List<Flt64>, tokenTable: AbstractTokenTable<*>, zeroIfNone: Boolean): Flt64? {
        return evaluateWithCachedTokenTable(results, tokenTable, zeroIfNone) {
            var ret = _polyF64.constant
            for (monomial in _polyF64.monomials) {
                val sym1Value = evaluateSymbol(monomial.symbol1, results, tokenTable, zeroIfNone)
                if (sym1Value == null && !zeroIfNone) return@evaluateWithCachedTokenTable null
                val termValue = if (monomial.symbol2 != null) {
                    val sym2Value = evaluateSymbol(monomial.symbol2!!, results, tokenTable, zeroIfNone)
                    if (sym2Value == null && !zeroIfNone) return@evaluateWithCachedTokenTable null
                    (sym1Value ?: Flt64.zero) * (sym2Value ?: Flt64.zero)
                } else (sym1Value ?: Flt64.zero)
                ret += monomial.coefficient * termValue
            }
            ret
        }
    }

    override fun evaluate(values: Map<Symbol, Flt64>, tokenList: AbstractTokenListF64?, zeroIfNone: Boolean): Flt64? {
        if (values.containsKey(this)) return values[this]!!
        var ret = _polyF64.constant
        for (monomial in _polyF64.monomials) {
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

    override fun evaluate(values: Map<Symbol, Flt64>, tokenTable: AbstractTokenTable<*>?, zeroIfNone: Boolean): Flt64? {
        return evaluateWithCachedTokenTable(values, tokenTable, zeroIfNone) {
            if (values.containsKey(this)) {
                return@evaluateWithCachedTokenTable values[this]!!
            }
            var ret = _polyF64.constant
            for (monomial in _polyF64.monomials) {
                val sym1Value = evaluateSymbol(monomial.symbol1, values, tokenTable, zeroIfNone)
                if (sym1Value == null && !zeroIfNone) {
                    return@evaluateWithCachedTokenTable null
                }
                val termValue = if (monomial.symbol2 != null) {
                    val sym2Value = evaluateSymbol(monomial.symbol2!!, values, tokenTable, zeroIfNone)
                    if (sym2Value == null && !zeroIfNone) {
                        return@evaluateWithCachedTokenTable null
                    }
                    (sym1Value ?: Flt64.zero) * (sym2Value ?: Flt64.zero)
                } else (sym1Value ?: Flt64.zero)
                ret += monomial.coefficient * termValue
            }
            ret
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

operator fun <V> LinearIntermediateSymbol<V>.times(rhs: PhysicalUnit): Quantity<LinearIntermediateSymbol<V>> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
    return Quantity(this, rhs)
}

operator fun <V> LinearIntermediateSymbol<V>.div(rhs: PhysicalUnit): Quantity<LinearIntermediateSymbol<V>> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
    return Quantity(this, rhs.reciprocal())
}

operator fun <V> QuadraticIntermediateSymbol<V>.times(rhs: PhysicalUnit): Quantity<QuadraticIntermediateSymbol<V>> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
    return Quantity(this, rhs)
}

operator fun <V> QuadraticIntermediateSymbol<V>.div(rhs: PhysicalUnit): Quantity<QuadraticIntermediateSymbol<V>> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
    return Quantity(this, rhs.reciprocal())
}

operator fun <V> LinearIntermediateSymbol<V>.plus(rhs: LinearIntermediateSymbol<V>): UtilsLinearPolynomial<Flt64> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
    val lhs = this.toMathLinearPolynomial()
    val rhsPoly = rhs.toMathLinearPolynomial()
    return UtilsLinearPolynomial(lhs.monomials + rhsPoly.monomials, lhs.constant + rhsPoly.constant)
}

operator fun <V> LinearIntermediateSymbol<V>.minus(rhs: LinearIntermediateSymbol<V>): UtilsLinearPolynomial<Flt64> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
    val lhs = this.toMathLinearPolynomial()
    val rhsPoly = rhs.toMathLinearPolynomial()
    return UtilsLinearPolynomial(lhs.monomials + rhsPoly.monomials.map { UtilsLinearMonomial(-it.coefficient, it.symbol) }, lhs.constant - rhsPoly.constant)
}

typealias IntermediateSymbolF64 = IntermediateSymbol<Flt64>
typealias LinearIntermediateSymbolF64 = LinearIntermediateSymbol<Flt64>
typealias QuadraticIntermediateSymbolF64 = QuadraticIntermediateSymbol<Flt64>
typealias LinearExpressionSymbolF64 = LinearExpressionSymbol<Flt64>
typealias QuadraticExpressionSymbolF64 = QuadraticExpressionSymbol<Flt64>

typealias QuantityIntermediateSymbol = Quantity<IntermediateSymbol<*>>
typealias QuantityLinearIntermediateSymbol = Quantity<LinearIntermediateSymbol<*>>
typealias QuantityQuadraticIntermediateSymbol = Quantity<QuadraticIntermediateSymbol<*>>
typealias QuantityLinearExpressionSymbol = Quantity<LinearExpressionSymbol<*>>
typealias QuantityQuadraticExpressionSymbol = Quantity<QuadraticExpressionSymbol<*>>


