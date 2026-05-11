@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol

import fuookami.ospf.kotlin.core.model.basic.ExpressionRange
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.IdentifierGenerator
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.utils.functional.Variant3
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.Ring
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.math.symbol.Category
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.operation.ToLinearPolynomial
import fuookami.ospf.kotlin.math.symbol.operation.ToQuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.MutableLinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.MutableQuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.quantities.unit.reciprocal
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.model.mechanism.AbstractQuadraticMechanismModel
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality


interface IntermediateSymbol<V> : Symbol where V : RealNumber<V>, V : NumberField<V> {
    override var name: String
    override var displayName: String?

    val discrete: Boolean get() = false

    val range: ExpressionRange<V>
    val lowerBound: Bound<V>? get() = range.lowerBound
    val upperBound: Bound<V>? get() = range.upperBound
    val fixedValue: V? get() = range.fixedValue

    // --- V-typed primary path (abstract) ---
    fun prepare(values: Map<Symbol, V>?, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>): V?

    fun prepareAndCache(values: Map<Symbol, V>?, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>) {
        if (values.isNullOrEmpty()) {
            prepare(null, tokenTable, converter)?.let {
                tokenTable.cache(cacheKey = this, solution = null, value = it)
            }
        } else {
            prepare(values, tokenTable, converter)?.let {
                tokenTable.cache(cacheKey = this, fixedValues = values, value = it)
            }
        }
    }

    fun evaluate(tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean = false): V?
    fun evaluate(results: List<V>, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean = false): V?
    fun evaluate(values: Map<Symbol, V>, tokenTable: AbstractTokenTable<V>?, converter: IntoValue<V>, zeroIfNone: Boolean = false): V?
    fun evaluateFromTokens(tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean = false): V? =
        evaluate(tokenTable, converter, zeroIfNone)

    val category: Category
    val operationCategory: Category get() = category

    val cached: Boolean
    val parent: IntermediateSymbol<*>? get() = null
    val args: Any? get() = parent?.args
    val dependencies: Set<IntermediateSymbol<*>>

    val identifier: UInt64
    val index: Int

    fun flush(force: Boolean = false)

    fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try = ok

    fun toRawString(unfold: UInt64 = UInt64.zero): String
}


interface LinearIntermediateSymbol<V> : IntermediateSymbol<V>, ToLinearPolynomial<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
    companion object {
        fun empty(
            parent: IntermediateSymbol<*>? = null,
            name: String = "",
            displayName: String? = null
        ): LinearIntermediateSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
            return LinearExpressionSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
                _utilsPolynomial = MutableLinearPolynomial(
                    monomials = emptyList(),
                    constant = Flt64.zero
                ),
                parent = parent,
                name = name,
                displayName = displayName
            )
        }
    }

    val polynomial: LinearPolynomial<V>

    fun asMutable(): MutableLinearPolynomial<V>

    override fun toLinearPolynomial(): LinearPolynomial<V> = polynomial
}

interface QuadraticIntermediateSymbol<V> : IntermediateSymbol<V>, ToQuadraticPolynomial<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
    companion object {
        fun empty(
            parent: IntermediateSymbol<*>? = null,
            name: String = "",
            displayName: String? = null
        ): QuadraticIntermediateSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
            return QuadraticExpressionSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
                _utilsPolynomial = MutableQuadraticPolynomial(
                    monomials = emptyList(),
                    constant = Flt64.zero
                ),
                parent = parent,
                name = name,
                displayName = displayName
            )
        }
    }

    val polynomial: QuadraticPolynomial<V>

    fun asMutable(): MutableQuadraticPolynomial<V>

    override fun toQuadraticPolynomial(): QuadraticPolynomial<V> = polynomial
}

internal fun IntermediateSymbol<*>.shouldPrepare(
    cacheKey: IntermediateSymbol<*>,
    values: Map<Symbol, Flt64>?,
    tokenTable: AbstractTokenTable<fuookami.ospf.kotlin.math.algebra.number.Flt64>
): Boolean {
    val tt = tokenTable
    return (!values.isNullOrEmpty() || tt.cachedSolution) && if (values.isNullOrEmpty()) {
        tt.cached(cacheKey)
    } else {
        tt.cached(cacheKey, values)
    } == false
}

internal fun IntermediateSymbol<*>.shouldPrepare(
    values: Map<Symbol, Flt64>?,
    tokenTable: AbstractTokenTable<fuookami.ospf.kotlin.math.algebra.number.Flt64>
): Boolean {
    return shouldPrepare(this, values, tokenTable)
}

internal fun IntermediateSymbol<*>.shouldPrepareWithFixedCacheKey(
    cacheKey: IntermediateSymbol<*>,
    values: Map<Symbol, Flt64>?,
    tokenTable: AbstractTokenTable<fuookami.ospf.kotlin.math.algebra.number.Flt64>
): Boolean {
    val tt = tokenTable
    return (!values.isNullOrEmpty() || tt.cachedSolution) && tt.cached(cacheKey) == false
}

internal fun IntermediateSymbol<*>.shouldPrepareWithFixedCacheKey(
    values: Map<Symbol, Flt64>?,
    tokenTable: AbstractTokenTable<fuookami.ospf.kotlin.math.algebra.number.Flt64>
): Boolean {
    return shouldPrepareWithFixedCacheKey(this, values, tokenTable)
}

internal inline fun <T> IntermediateSymbol<*>.prepareIfNotCached(
    cacheKey: IntermediateSymbol<*>,
    values: Map<Symbol, Flt64>?,
    tokenTable: AbstractTokenTable<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
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
    tokenTable: AbstractTokenTable<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
    block: () -> T?
): T? {
    return prepareIfNotCached(this, values, tokenTable, block)
}

internal inline fun <T> IntermediateSymbol<*>.prepareIfNotCachedWithFixedCacheKey(
    cacheKey: IntermediateSymbol<*>,
    values: Map<Symbol, Flt64>?,
    tokenTable: AbstractTokenTable<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
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
    tokenTable: AbstractTokenTable<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
    block: () -> T?
): T? {
    return prepareIfNotCachedWithFixedCacheKey(this, values, tokenTable, block)
}

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
                    (/* unchecked */ dependency as IntermediateSymbol<V>).evaluate(
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

private fun <V> IntermediateSymbol<V>.evaluateWithCachedTokenTable(
    results: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
    tokenTable: AbstractTokenTable<V>,
    converter: IntoValue<V>,
    zeroIfNone: Boolean,
    calculator: () -> V?
): V? where V : RealNumber<V>, V : NumberField<V> {
    return if (tokenTable.cachedSolution) {
        tokenTable.cacheSolverIfNotCached(this, results, {
            for (dependency in dependencies) {
                if (tokenTable.cachedSolution) {
                    val dep = dependency as IntermediateSymbol<V>
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
                    val dep = dependency as IntermediateSymbol<V>
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
        operator fun invoke(
            item: AbstractVariableItem<*, *>,
            parent: IntermediateSymbol<*>? = null,
            name: String = "",
            displayName: String? = null
        ): LinearExpressionSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
            return LinearExpressionSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
                _utilsPolynomial = MutableLinearPolynomial(
                    monomials = listOf(LinearMonomial(Flt64.one, item)),
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
        ): LinearExpressionSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
            return LinearExpressionSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
                _utilsPolynomial = MutableLinearPolynomial(
                    monomials = listOf(LinearMonomial(Flt64.one, symbol)),
                    constant = Flt64.zero
                ),
                category = Linear,
                parent = parent,
                name = name.ifEmpty { symbol.name },
                displayName = displayName ?: symbol.displayName
            )
        }

        operator fun invoke(
            polynomial: LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
            parent: IntermediateSymbol<*>? = null,
            name: String = "",
            displayName: String? = null
        ): LinearExpressionSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
            return LinearExpressionSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
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

        operator fun invoke(
            monomial: LinearMonomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
            parent: IntermediateSymbol<*>? = null,
            name: String = "",
            displayName: String? = null
        ): LinearExpressionSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
            return LinearExpressionSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
                _utilsPolynomial = MutableLinearPolynomial(
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
            polynomial: MutableLinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
            parent: IntermediateSymbol<*>? = null,
            name: String = "",
            displayName: String? = null
        ): LinearExpressionSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
            return LinearExpressionSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
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
        ): LinearExpressionSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
            return LinearExpressionSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
                _utilsPolynomial = MutableLinearPolynomial(
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
        ): LinearExpressionSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
            return LinearExpressionSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
                _utilsPolynomial = MutableLinearPolynomial(
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
        ): LinearExpressionSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
            return LinearExpressionSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
                _utilsPolynomial = MutableLinearPolynomial(
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
        ): LinearExpressionSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
            return LinearExpressionSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
                _utilsPolynomial = MutableLinearPolynomial(
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
        ): LinearExpressionSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
            return LinearExpressionSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
                _utilsPolynomial = MutableLinearPolynomial(
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
        ): LinearExpressionSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
            return LinearExpressionSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
                _utilsPolynomial = MutableLinearPolynomial(
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
        ): LinearExpressionSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
            return LinearExpressionSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
                _utilsPolynomial = MutableLinearPolynomial(
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
    private val _polyFlt64: MutableLinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64> get() = _utilsPolynomial as MutableLinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>

    // polynomial property returns immutable version
    override val polynomial: LinearPolynomial<V> get() = _utilsPolynomial.toLinearPolynomial()

    // flattenedMonomials: extract from monomials
    @kotlin.Deprecated("Use flattenedMonomialsAsV instead. This Flt64-specific property will be removed in a future version.", level = DeprecationLevel.WARNING)
    val flattenedMonomials: LinearFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64>
        get() = LinearFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
            monomials = _polyFlt64.monomials,
            constant = _polyFlt64.constant
        )

    val flattenedMonomialsAsV: LinearFlattenData<V>
        get() = LinearFlattenData(
            monomials = _utilsPolynomial.monomials,
            constant = _utilsPolynomial.constant
        )

    // asMutable returns MutableLinearPolynomial<V>
    override fun asMutable(): MutableLinearPolynomial<V> {
        return _utilsPolynomial
    }

    // Helper: evaluate a single monomial's symbol (V-typed path)
    // Internally computes in Flt64 (via resultFlt64 / Flt64 evaluate(tokenList)), then the caller converts via converter.intoValue().
    private fun evaluateSymbol(symbol: Symbol, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): Flt64? {
        return when (symbol) {
            is AbstractVariableItem<*, *> -> {
                tokenTable.find(symbol)?.resultFlt64 ?: if (zeroIfNone) Flt64.zero else null
            }
            is LinearExpressionSymbol<*> -> {
                symbol.evaluate(tokenTable.tokenList as AbstractTokenList<fuookami.ospf.kotlin.math.algebra.number.Flt64>, zeroIfNone)
            }
            else -> null
        }
    }

    private fun evaluateSymbol(symbol: Symbol, results: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): Flt64? {
        return when (symbol) {
            is AbstractVariableItem<*, *> -> {
                val index = tokenTable.indexOf(symbol)
                if (index != null && index >= 0 && index < results.size) results[index]
                else if (zeroIfNone) Flt64.zero else null
            }
            is LinearExpressionSymbol<*> -> {
                symbol.evaluate(results, tokenTable.tokenList as AbstractTokenList<fuookami.ospf.kotlin.math.algebra.number.Flt64>, zeroIfNone)
            }
            else -> null
        }
    }

    // Nullable tokenTable variant for evaluate(values, tokenTable?, ...) path
    private fun evaluateSymbol(symbol: Symbol, values: Map<Symbol, Flt64>, tokenTable: AbstractTokenTable<V>?, converter: IntoValue<V>, zeroIfNone: Boolean): Flt64? {
        return when (symbol) {
            is AbstractVariableItem<*, *> -> {
                values[symbol] ?: tokenTable?.find(symbol)?.resultFlt64 ?: if (zeroIfNone) Flt64.zero else null
            }
            is LinearExpressionSymbol<*> -> {
                symbol.evaluate(values, tokenTable?.tokenList as AbstractTokenList<fuookami.ospf.kotlin.math.algebra.number.Flt64>?, zeroIfNone)
            }
            else -> values[symbol] ?: if (zeroIfNone) Flt64.zero else null
        }
    }

    // Helper: evaluate a single monomial's symbol (Flt64 tokenList path)
    private fun evaluateSymbol(symbol: Symbol, tokenList: AbstractTokenList<fuookami.ospf.kotlin.math.algebra.number.Flt64>, zeroIfNone: Boolean): Flt64? {
        return when (symbol) {
            is AbstractVariableItem<*, *> -> {
                val token = tokenList.find(symbol)
                token?.resultFlt64 ?: if (zeroIfNone) Flt64.zero else null
            }
            is LinearExpressionSymbol<*> -> symbol.evaluate(tokenList, zeroIfNone)
            else -> null
        }
    }

    private fun evaluateSymbol(symbol: Symbol, results: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>, tokenList: AbstractTokenList<fuookami.ospf.kotlin.math.algebra.number.Flt64>, zeroIfNone: Boolean): Flt64? {
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

    private fun evaluateSymbol(symbol: Symbol, values: Map<Symbol, Flt64>, tokenList: AbstractTokenList<fuookami.ospf.kotlin.math.algebra.number.Flt64>?, zeroIfNone: Boolean): Flt64? {
        return when (symbol) {
            is AbstractVariableItem<*, *> -> {
                values[symbol] ?: tokenList?.find(symbol)?.resultFlt64 ?: if (zeroIfNone) Flt64.zero else null
            }
            is LinearExpressionSymbol<*> -> symbol.evaluate(values, tokenList, zeroIfNone)
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
    private fun calculateRange(): ValueRange<fuookami.ospf.kotlin.math.algebra.number.Flt64>? {
        val poly = _polyFlt64
        var range: ValueRange<fuookami.ospf.kotlin.math.algebra.number.Flt64>? = ValueRange(poly.constant, Flt64).value
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
        get() = SolverBoundaryCasts.expressionRangeVFromFlt64(calculateRange())

    // discrete: check if all monomials are discrete and constant is integral
    override val discrete: Boolean
        get() = _polyFlt64.monomials.all { monomial ->
            when (val sym = monomial.symbol) {
                is AbstractVariableItem<*, *> -> sym.type.isIntegerType && monomial.coefficient.round() eq monomial.coefficient
                is LinearIntermediateSymbol<*> -> sym.discrete && monomial.coefficient.round() eq monomial.coefficient
                else -> false
            }
        } && _polyFlt64.constant.round() eq _polyFlt64.constant

    // cached/flush via TokenTable cache key
    private val rangeCacheKey = newTokenCacheKey(
        category = Linear,
        prefix = "__linear_expression_range_cache__"
    )
    private val flattenCacheKey = newTokenCacheKey(
        category = Linear,
        prefix = "__linear_expression_flatten_cache__"
    )
    private fun cacheTokenTable(): AbstractTokenTable<fuookami.ospf.kotlin.math.algebra.number.Flt64>? {
        return dependencies
            .asSequence()
            .mapNotNull { boundTokenTableContext(it) }
            .firstOrNull() as? AbstractTokenTable<fuookami.ospf.kotlin.math.algebra.number.Flt64>
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

    // prepare: V-typed primary path (P4-5)
    internal fun prepareSolver(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>): V? {
        // Trigger flatten view creation before solver-evaluation path.
        flattenedMonomialsAsV

        return if (values.isNullOrEmpty()) {
            var ret = _polyFlt64.constant
            for (monomial in _polyFlt64.monomials) {
                val symbolValue = evaluateSymbol(monomial.symbol, tokenTable, converter, false)
                if (symbolValue == null) return null
                ret += monomial.coefficient * symbolValue
            }
            converter.intoValue(ret)
        } else {
            var ret = _polyFlt64.constant
            for (monomial in _polyFlt64.monomials) {
                val symbolValue = evaluateSymbol(monomial.symbol, values, tokenTable, converter, false)
                if (symbolValue == null) return null
                ret += monomial.coefficient * symbolValue
            }
            converter.intoValue(ret)
        }
    }

    override fun prepare(values: Map<Symbol, V>?, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>): V? {
        val flt64Values = values?.mapValues { converter.fromValue(it.value) }
        return prepareSolver(flt64Values, tokenTable, converter)
    }

    // toRawString
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

    // evaluate methods - similar to Polynomial.evaluate implementation
    internal fun evaluate(tokenList: AbstractTokenList<fuookami.ospf.kotlin.math.algebra.number.Flt64>, zeroIfNone: Boolean): Flt64? {
        var ret = _polyFlt64.constant
        for (monomial in _polyFlt64.monomials) {
            val symbolValue = evaluateSymbol(monomial.symbol, tokenList, zeroIfNone)
            if (symbolValue == null && !zeroIfNone) return null
            ret += monomial.coefficient * (symbolValue ?: Flt64.zero)
        }
        return ret
    }

    // V-typed evaluate methods (P4-5)
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

    internal fun evaluateSolver(results: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
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

    // Flt64 convenience overloads (solver boundary)
    internal fun evaluate(results: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>, tokenList: AbstractTokenList<fuookami.ospf.kotlin.math.algebra.number.Flt64>, zeroIfNone: Boolean): Flt64? {
        var ret = _polyFlt64.constant
        for (monomial in _polyFlt64.monomials) {
            val symbolValue = evaluateSymbol(monomial.symbol, results, tokenList, zeroIfNone)
            if (symbolValue == null && !zeroIfNone) return null
            ret += monomial.coefficient * (symbolValue ?: Flt64.zero)
        }
        return ret
    }

    internal fun evaluate(values: Map<Symbol, Flt64>, tokenList: AbstractTokenList<fuookami.ospf.kotlin.math.algebra.number.Flt64>?, zeroIfNone: Boolean): Flt64? {
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
        operator fun invoke(
            item: AbstractVariableItem<*, *>,
            parent: IntermediateSymbol<*>? = null,
            name: String = "",
            displayName: String? = null
        ): QuadraticExpressionSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
            return QuadraticExpressionSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
                _utilsPolynomial = MutableQuadraticPolynomial(
                    monomials = listOf(QuadraticMonomial.linear(Flt64.one, item)),
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
        ): QuadraticExpressionSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
            return QuadraticExpressionSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
                _utilsPolynomial = MutableQuadraticPolynomial(
                    monomials = listOf(QuadraticMonomial.linear(Flt64.one, symbol)),
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
        ): QuadraticExpressionSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
            return QuadraticExpressionSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
                _utilsPolynomial = MutableQuadraticPolynomial(
                    monomials = listOf(QuadraticMonomial.linear(Flt64.one, symbol)),
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
        ): QuadraticExpressionSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
            return QuadraticExpressionSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
                _utilsPolynomial = MutableQuadraticPolynomial(
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
        ): QuadraticExpressionSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
            return QuadraticExpressionSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
                _utilsPolynomial = MutableQuadraticPolynomial(
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
        ): QuadraticExpressionSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
            return QuadraticExpressionSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
                _utilsPolynomial = MutableQuadraticPolynomial(
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
        ): QuadraticExpressionSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
            return QuadraticExpressionSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
                _utilsPolynomial = MutableQuadraticPolynomial(
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
        ): QuadraticExpressionSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
            return QuadraticExpressionSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
                _utilsPolynomial = MutableQuadraticPolynomial(
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
        ): QuadraticExpressionSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
            return QuadraticExpressionSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
                _utilsPolynomial = MutableQuadraticPolynomial(
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
        ): QuadraticExpressionSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
            return QuadraticExpressionSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
                _utilsPolynomial = MutableQuadraticPolynomial(
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
    private val _polyFlt64: MutableQuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64> get() = _utilsPolynomial as MutableQuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>

    // polynomial property returns immutable version
    override val polynomial: QuadraticPolynomial<V> get() = _utilsPolynomial.toQuadraticPolynomial()

    // flattenedMonomials: extract from monomials (distinguish linear vs quadratic)
    @kotlin.Deprecated("Use flattenedMonomialsAsV instead. This Flt64-specific property will be removed in a future version.", level = DeprecationLevel.WARNING)
    val flattenedMonomials: QuadraticFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64>
        get() = QuadraticFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
            monomials = _polyFlt64.monomials,
            constant = _polyFlt64.constant
        )

    val flattenedMonomialsAsV: QuadraticFlattenData<V>
        get() = QuadraticFlattenData(
            monomials = _utilsPolynomial.monomials,
            constant = _utilsPolynomial.constant
        )

    // asMutable returns MutableQuadraticPolynomial<V>
    override fun asMutable(): MutableQuadraticPolynomial<V> {
        return _utilsPolynomial
    }

    // Helper: evaluate a single symbol (V-typed path)
    // Internally computes in Flt64 (via resultFlt64 / Flt64 evaluate(tokenList)), then the caller converts via converter.intoValue().
    private fun evaluateSymbol(symbol: Symbol, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): Flt64? {
        val tokenList = tokenTable.tokenList as AbstractTokenList<fuookami.ospf.kotlin.math.algebra.number.Flt64>
        return when (symbol) {
            is AbstractVariableItem<*, *> -> {
                tokenTable.find(symbol)?.resultFlt64 ?: if (zeroIfNone) Flt64.zero else null
            }
            is LinearExpressionSymbol<*> -> symbol.evaluate(tokenList, zeroIfNone)
            is QuadraticExpressionSymbol<*> -> symbol.evaluate(tokenList, zeroIfNone)
            else -> null
        }
    }

    private fun evaluateSymbol(symbol: Symbol, results: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): Flt64? {
        val tokenList = tokenTable.tokenList as AbstractTokenList<fuookami.ospf.kotlin.math.algebra.number.Flt64>
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

    // Nullable tokenTable variant for evaluate(values, tokenTable?, ...) path
    private fun evaluateSymbol(symbol: Symbol, values: Map<Symbol, Flt64>, tokenTable: AbstractTokenTable<V>?, converter: IntoValue<V>, zeroIfNone: Boolean): Flt64? {
        val tokenList = tokenTable?.tokenList as AbstractTokenList<fuookami.ospf.kotlin.math.algebra.number.Flt64>?
        return when (symbol) {
            is AbstractVariableItem<*, *> -> {
                values[symbol] ?: tokenTable?.find(symbol)?.resultFlt64 ?: if (zeroIfNone) Flt64.zero else null
            }
            is LinearExpressionSymbol<*> -> symbol.evaluate(values, tokenList, zeroIfNone)
            is QuadraticExpressionSymbol<*> -> symbol.evaluate(values, tokenList, zeroIfNone)
            else -> values[symbol] ?: if (zeroIfNone) Flt64.zero else null
        }
    }

    // Helper: evaluate a single monomial's symbol (Flt64 tokenList path)
    private fun evaluateSymbol(symbol: Symbol, tokenList: AbstractTokenList<fuookami.ospf.kotlin.math.algebra.number.Flt64>, zeroIfNone: Boolean): Flt64? {
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

    private fun evaluateSymbol(symbol: Symbol, results: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>, tokenList: AbstractTokenList<fuookami.ospf.kotlin.math.algebra.number.Flt64>, zeroIfNone: Boolean): Flt64? {
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

    private fun evaluateSymbol(symbol: Symbol, values: Map<Symbol, Flt64>, tokenList: AbstractTokenList<fuookami.ospf.kotlin.math.algebra.number.Flt64>?, zeroIfNone: Boolean): Flt64? {
        return when (symbol) {
            is AbstractVariableItem<*, *> -> {
                values[symbol] ?: tokenList?.find(symbol)?.resultFlt64 ?: if (zeroIfNone) Flt64.zero else null
            }
            is LinearExpressionSymbol<*> -> symbol.evaluate(values, tokenList, zeroIfNone)
            is QuadraticExpressionSymbol<*> -> symbol.evaluate(values, tokenList, zeroIfNone)
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
    private fun calculateRange(): ValueRange<fuookami.ospf.kotlin.math.algebra.number.Flt64>? {
        var range: ValueRange<fuookami.ospf.kotlin.math.algebra.number.Flt64>? = ValueRange(_polyFlt64.constant, Flt64).value
        for (monomial in _polyFlt64.monomials) {
            val sym1Range: ValueRange<fuookami.ospf.kotlin.math.algebra.number.Flt64>? = when (val sym1 = monomial.symbol1) {
                is AbstractVariableItem<*, *> -> sym1.range.valueRange
                is LinearIntermediateSymbol<*> -> sym1.range.valueRange
                is QuadraticIntermediateSymbol<*> -> sym1.range.valueRange
                else -> null
            }
            val sym2Range: ValueRange<fuookami.ospf.kotlin.math.algebra.number.Flt64>? = if (monomial.symbol2 != null) {
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

    override val range: ExpressionRange<V>
        get() = SolverBoundaryCasts.expressionRangeVFromFlt64(calculateRange())

    // discrete: check if all monomials are discrete and constant is integral
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

    // cached/flush via TokenTable cache key
    private val rangeCacheKey = newTokenCacheKey(
        category = Quadratic,
        prefix = "__quadratic_expression_range_cache__"
    )
    private val flattenCacheKey = newTokenCacheKey(
        category = Quadratic,
        prefix = "__quadratic_expression_flatten_cache__"
    )
    private fun cacheTokenTable(): AbstractTokenTable<fuookami.ospf.kotlin.math.algebra.number.Flt64>? {
        return dependencies
            .asSequence()
            .mapNotNull { boundTokenTableContext(it) }
            .firstOrNull() as? AbstractTokenTable<fuookami.ospf.kotlin.math.algebra.number.Flt64>
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

    // prepare: V-typed primary path (P4-5)
    internal fun prepareSolver(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>): V? {
        // Trigger flatten view creation before solver-evaluation path.
        flattenedMonomialsAsV

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

    // toRawString
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

    // evaluate methods - similar to Polynomial.evaluate implementation
    internal fun evaluate(tokenList: AbstractTokenList<fuookami.ospf.kotlin.math.algebra.number.Flt64>, zeroIfNone: Boolean): Flt64? {
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

    // V-typed evaluate methods (P4-5)
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

    internal fun evaluate(results: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>, tokenList: AbstractTokenList<fuookami.ospf.kotlin.math.algebra.number.Flt64>, zeroIfNone: Boolean): Flt64? {
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

    internal fun evaluateSolver(results: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
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

    internal fun evaluate(values: Map<Symbol, Flt64>, tokenList: AbstractTokenList<fuookami.ospf.kotlin.math.algebra.number.Flt64>?, zeroIfNone: Boolean): Flt64? {
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

operator fun <V> LinearIntermediateSymbol<V>.plus(rhs: LinearIntermediateSymbol<V>): LinearPolynomial<V>
    where V : RealNumber<V>, V : NumberField<V> {
    val lhs = this.toLinearPolynomial()
    val rhsPoly = rhs.toLinearPolynomial()
    return LinearPolynomial(lhs.monomials + rhsPoly.monomials, lhs.constant + rhsPoly.constant)
}

operator fun <V> LinearIntermediateSymbol<V>.minus(rhs: LinearIntermediateSymbol<V>): LinearPolynomial<V>
    where V : RealNumber<V>, V : NumberField<V> {
    val lhs = this.toLinearPolynomial()
    val rhsPoly = rhs.toLinearPolynomial()
    return LinearPolynomial(lhs.monomials + rhsPoly.monomials.map { LinearMonomial(-it.coefficient, it.symbol) }, lhs.constant - rhsPoly.constant)
}

// Solver-boundary extensions: delegate to SolverBoundaryCasts (single UNCHECKED_CAST location)
internal val <V> LinearIntermediateSymbol<V>.solverFlattenedMonomials: LinearFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64>
    where V : RealNumber<V>, V : Ring<V>, V : NumberField<V>
    get() = SolverBoundaryCasts.linearSolverFlattenedMonomials(this)

internal val <V> QuadraticIntermediateSymbol<V>.solverFlattenedMonomials: QuadraticFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64>
    where V : RealNumber<V>, V : Ring<V>, V : NumberField<V>
    get() = SolverBoundaryCasts.quadraticSolverFlattenedMonomials(this)





