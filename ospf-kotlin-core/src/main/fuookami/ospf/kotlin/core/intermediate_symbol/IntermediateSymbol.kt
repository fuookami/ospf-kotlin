@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

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
import fuookami.ospf.kotlin.core.intermediate_model.*
import fuookami.ospf.kotlin.core.intermediate_model.monomial.LinearMonomial
import fuookami.ospf.kotlin.core.intermediate_model.monomial.LinearMonomialCell
import fuookami.ospf.kotlin.core.intermediate_model.monomial.LinearMonomialSymbol
import fuookami.ospf.kotlin.core.intermediate_model.monomial.Monomial
import fuookami.ospf.kotlin.core.intermediate_model.monomial.MonomialCell
import fuookami.ospf.kotlin.core.intermediate_model.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.core.intermediate_model.monomial.QuadraticMonomialCell
import fuookami.ospf.kotlin.core.intermediate_model.ToLinearPolynomial
import fuookami.ospf.kotlin.core.intermediate_model.ToQuadraticPolynomial
import fuookami.ospf.kotlin.core.intermediate_model.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.intermediate_model.AbstractQuadraticMechanismModel
import fuookami.ospf.kotlin.core.intermediate_model.AbstractTokenTable
import fuookami.ospf.kotlin.core.intermediate_model.LinearFlattenData
import fuookami.ospf.kotlin.core.intermediate_model.QuadraticFlattenData
import fuookami.ospf.kotlin.core.intermediate_model.toLinearFlattenData
import fuookami.ospf.kotlin.core.intermediate_model.toQuadraticFlattenData
import fuookami.ospf.kotlin.core.intermediate_model.toLinearMonomialCells
import fuookami.ospf.kotlin.core.intermediate_model.toQuadraticMonomialCells
import fuookami.ospf.kotlin.core.variable.AbstractTokenList
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.AddableTokenCollection
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

internal fun Polynomial<*, *, *>.toTidyRawString(unfold: UInt64): String {
    return if (monomials.isEmpty()) {
        "$constant"
    } else if (monomials.size == 1 && constant eq Flt64.zero) {
        monomials.first().toRawString(unfold - UInt64.one)
    } else {
        toRawString(unfold - UInt64.one)
            .replace("+ 0.0 ", "")
            .removeSuffix(" + 0.0")
            .let {
                if (it.contains(" + ")) {
                    "($it)"
                } else {
                    it
                }
            }
    }
}

interface IntermediateSymbol : Symbol, Expression {
    val category: Category
    val operationCategory: Category get() = category

    val cached: Boolean
    val parent: IntermediateSymbol? get() = null
    val args: Any? get() = parent?.args
    val dependencies: Set<IntermediateSymbol>

    val identifier: UInt64
    val index: Int

    fun flush(force: Boolean = false)

    fun prepare(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable): Flt64?
    fun prepareAndCache(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable) {
        if (values.isNullOrEmpty()) {
            prepare(null, tokenTable)?.let {
                tokenTable.cache(
                    cacheKey = this,
                    solution = null,
                    value = it
                )
            }
        } else {
            prepare(values, tokenTable)?.let {
                tokenTable.cache(
                    cacheKey = this,
                    fixedValues = values,
                    value = it
                )
            }
        }
    }

    fun toRawString(unfold: UInt64 = UInt64.zero): String
}

interface LinearIntermediateSymbol : IntermediateSymbol, ToLinearPolynomial, ToQuadraticPolynomial {
    companion object {
        fun empty(
            parent: IntermediateSymbol? = null,
            name: String = "",
            displayName: String? = null
        ): LinearIntermediateSymbol {
            return LinearExpressionSymbol(
                parent = parent,
                name = name,
                displayName = displayName
            )
        }
    }

    @Deprecated(
        message = "Use flattenedMonomials instead. cells is transitional compatibility layer.",
        level = DeprecationLevel.WARNING
    )
    val cells: List<LinearMonomialCell>
    @Suppress("DEPRECATION")
    val flattenedMonomials: LinearFlattenData get() = cells.toLinearFlattenData()

    override fun toLinearPolynomial(): UtilsLinearPolynomial<Flt64> {
        return UtilsLinearPolynomial(
            monomials = flattenedMonomials.monomials.map { fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial(it.coefficient, it.symbol) },
            constant = flattenedMonomials.constant
        )
    }

    override fun toQuadraticPolynomial(): UtilsQuadraticPolynomial<Flt64> {
        val linearPoly = toLinearPolynomial()
        return UtilsQuadraticPolynomial(
            monomials = linearPoly.monomials.map { fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial(it.coefficient, it.symbol, it.symbol) },
            constant = linearPoly.constant
        )
    }
}

interface QuadraticIntermediateSymbol : IntermediateSymbol, ToQuadraticPolynomial {
    companion object {
        fun empty(
            parent: IntermediateSymbol? = null,
            name: String = "",
            displayName: String? = null
        ): QuadraticIntermediateSymbol {
            return QuadraticExpressionSymbol(
                parent = parent,
                name = name,
                displayName = displayName
            )
        }
    }

    @Deprecated(
        message = "Use flattenedMonomials instead. cells is transitional compatibility layer.",
        level = DeprecationLevel.WARNING
    )
    val cells: List<QuadraticMonomialCell>
    @Suppress("DEPRECATION")
    val flattenedMonomials: QuadraticFlattenData get() = cells.toQuadraticFlattenData()

    override fun toQuadraticPolynomial(): UtilsQuadraticPolynomial<Flt64> {
        return UtilsQuadraticPolynomial(
            monomials = flattenedMonomials.monomials.map { fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial(it.coefficient, it.symbol1, it.symbol2) },
            constant = flattenedMonomials.constant
        )
    }
}

internal fun IntermediateSymbol.shouldPrepare(
    cacheKey: IntermediateSymbol,
    values: Map<Symbol, Flt64>?,
    tokenTable: AbstractTokenTable
): Boolean {
    return (!values.isNullOrEmpty() || tokenTable.cachedSolution) && if (values.isNullOrEmpty()) {
        tokenTable.cached(cacheKey)
    } else {
        tokenTable.cached(cacheKey, values)
    } == false
}

internal fun IntermediateSymbol.shouldPrepare(
    values: Map<Symbol, Flt64>?,
    tokenTable: AbstractTokenTable
): Boolean {
    return shouldPrepare(this, values, tokenTable)
}

internal fun IntermediateSymbol.shouldPrepareWithFixedCacheKey(
    cacheKey: IntermediateSymbol,
    values: Map<Symbol, Flt64>?,
    tokenTable: AbstractTokenTable
): Boolean {
    return (!values.isNullOrEmpty() || tokenTable.cachedSolution) && tokenTable.cached(cacheKey) == false
}

internal fun IntermediateSymbol.shouldPrepareWithFixedCacheKey(
    values: Map<Symbol, Flt64>?,
    tokenTable: AbstractTokenTable
): Boolean {
    return shouldPrepareWithFixedCacheKey(this, values, tokenTable)
}

internal inline fun <T> IntermediateSymbol.prepareIfNotCached(
    cacheKey: IntermediateSymbol,
    values: Map<Symbol, Flt64>?,
    tokenTable: AbstractTokenTable,
    block: () -> T?
): T? {
    return if (shouldPrepare(cacheKey, values, tokenTable)) {
        block()
    } else {
        null
    }
}

internal inline fun <T> IntermediateSymbol.prepareIfNotCached(
    values: Map<Symbol, Flt64>?,
    tokenTable: AbstractTokenTable,
    block: () -> T?
): T? {
    return prepareIfNotCached(this, values, tokenTable, block)
}

internal inline fun <T> IntermediateSymbol.prepareIfNotCachedWithFixedCacheKey(
    cacheKey: IntermediateSymbol,
    values: Map<Symbol, Flt64>?,
    tokenTable: AbstractTokenTable,
    block: () -> T?
): T? {
    return if (shouldPrepareWithFixedCacheKey(cacheKey, values, tokenTable)) {
        block()
    } else {
        null
    }
}

internal inline fun <T> IntermediateSymbol.prepareIfNotCachedWithFixedCacheKey(
    values: Map<Symbol, Flt64>?,
    tokenTable: AbstractTokenTable,
    block: () -> T?
): T? {
    return prepareIfNotCachedWithFixedCacheKey(this, values, tokenTable, block)
}

private fun IntermediateSymbol.evaluateWithCachedTokenTable(
    tokenTable: AbstractTokenTable,
    zeroIfNone: Boolean,
    calculator: () -> Flt64?
): Flt64? {
    return if (tokenTable.cachedSolution) {
        tokenTable.cacheIfNotCached(
            cacheKey = this,
            solution = null
        ) {
            for (dependency in dependencies) {
                if (tokenTable.cachedSolution) {
                    dependency.evaluate(
                        tokenTable = tokenTable,
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

private fun IntermediateSymbol.evaluateWithCachedTokenTable(
    results: List<Flt64>,
    tokenTable: AbstractTokenTable,
    zeroIfNone: Boolean,
    calculator: () -> Flt64?
): Flt64? {
    return if (tokenTable.cachedSolution) {
        tokenTable.cacheIfNotCached(this, results) {
            for (dependency in dependencies) {
                if (tokenTable.cachedSolution) {
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
        tokenTable.cachedValue(this, results)
    }
}

private fun IntermediateSymbol.evaluateWithCachedTokenTable(
    values: Map<Symbol, Flt64>,
    tokenTable: AbstractTokenTable?,
    zeroIfNone: Boolean,
    calculator: () -> Flt64?
): Flt64? {
    values[this]?.let { value ->
        tokenTable?.cache(
            cacheKey = this,
            fixedValues = values,
            value = value
        )
        return value
    }

    if (tokenTable == null) {
        return calculator()
    }

    return if (values.isNotEmpty() || tokenTable.cachedSolution) {
        tokenTable.cacheIfNotCached(this, values) {
            for (dependency in dependencies) {
                if (values.isNotEmpty() || tokenTable.cachedSolution) {
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
        tokenTable.cachedValue(this, values)
    }
}

@Deprecated("DSL expression wrapper, use MathFunctionSymbol instead")
abstract class ExpressionSymbol(
    open val _polynomial: MutablePolynomial<*, *, *>,
    override val category: Category = _polynomial.category,
    override val parent: IntermediateSymbol? = null,
    override var name: String = "",
    override var displayName: String? = null
) : IntermediateSymbol {
    open val polynomial: Polynomial<*, *, *> by ::_polynomial

    internal var _group: AbstractSymbolCombination<*>? = null
    internal var _index: Int? = null
    override val identifier: UInt64 by lazy {
        _group?.identifier ?: IdentifierGenerator.gen()
    }
    override val index: Int by lazy {
        _index ?: 0
    }

    open fun asMutable(): MutablePolynomial<*, *, *> {
        return _polynomial
    }

    override val discrete get() = polynomial.discrete

    override val range get() = polynomial.range
    override val lowerBound get() = polynomial.lowerBound
    override val upperBound get() = polynomial.upperBound

    override val dependencies get() = polynomial.dependencies
    override val cached get() = polynomial.cached

    override fun flush(force: Boolean) {
        polynomial.flush(force)
    }

    override fun toRawString(unfold: UInt64): String {
        return if (unfold neq UInt64.zero) {
            polynomial.toRawString(unfold)
        } else {
            this.displayName ?: this.name
        }
    }

    override fun evaluate(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return polynomial.evaluate(tokenList, zeroIfNone)
    }

    override fun evaluate(tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        return evaluateWithCachedTokenTable(tokenTable, zeroIfNone) {
            polynomial.evaluate(
                tokenTable = tokenTable,
                zeroIfNone = zeroIfNone
            )
        }
    }

    override fun evaluate(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return polynomial.evaluate(
            results = results,
            tokenList = tokenList,
            zeroIfNone = zeroIfNone
        )
    }

    override fun evaluate(results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        return evaluateWithCachedTokenTable(results, tokenTable, zeroIfNone) {
            polynomial.evaluate(
                results = results,
                tokenTable = tokenTable,
                zeroIfNone = zeroIfNone
            )
        }
    }

    override fun evaluate(values: Map<Symbol, Flt64>, tokenList: AbstractTokenList?, zeroIfNone: Boolean): Flt64? {
        return if (values.containsKey(this)) {
            values[this]!!
        } else {
            polynomial.evaluate(
                values = values,
                tokenList = tokenList,
                zeroIfNone = zeroIfNone
            )
        }
    }

    override fun evaluate(values: Map<Symbol, Flt64>, tokenTable: AbstractTokenTable?, zeroIfNone: Boolean): Flt64? {
        return evaluateWithCachedTokenTable(values, tokenTable, zeroIfNone) {
            polynomial.evaluate(
                values = values,
                tokenTable = tokenTable,
                zeroIfNone = zeroIfNone
            )
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
        if (other !is ExpressionSymbol) return false

        if (identifier != other.identifier) return false
        if (index != other.index) return false
        if (name != other.name) return false

        return true
    }
}

@Deprecated("DSL expression wrapper, use MathFunctionSymbol instead")
class LinearExpressionSymbol(
    internal val _utilsPolynomial: UtilsMutableLinearPolynomial<Flt64>,
    category: Category = Linear,
    parent: IntermediateSymbol? = null,
    name: String = "",
    displayName: String? = null
) : LinearIntermediateSymbol {
    internal var _group: AbstractSymbolCombination<*>? = null
    internal var _index: Int? = null
    override val identifier: UInt64 by lazy {
        _group?.identifier ?: IdentifierGenerator.gen()
    }
    override val index: Int by lazy {
        _index ?: 0
    }

    override val category: Category = category
    override val parent: IntermediateSymbol? = parent
    override var name: String = name
    override var displayName: String? = displayName

    override val operationCategory: Category = Linear

    companion object {
        operator fun invoke(
            item: AbstractVariableItem<*, *>,
            parent: IntermediateSymbol? = null,
            name: String = "",
            displayName: String? = null
        ): LinearExpressionSymbol {
            return LinearExpressionSymbol(
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
            symbol: LinearIntermediateSymbol,
            parent: IntermediateSymbol? = null,
            name: String = "",
            displayName: String? = null
        ): LinearExpressionSymbol {
            return LinearExpressionSymbol(
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
            monomial: LinearMonomial,
            parent: IntermediateSymbol? = null,
            name: String = "",
            displayName: String? = null
        ): LinearExpressionSymbol {
            return LinearExpressionSymbol(
                _utilsPolynomial = UtilsMutableLinearPolynomial(
                    monomials = listOf(monomial.toUtilsMonomial()),
                    constant = Flt64.zero
                ),
                category = Linear,
                parent = parent,
                name = name.ifEmpty { monomial.name },
                displayName = displayName ?: monomial.displayName
            )
        }

        operator fun invoke(
            polynomial: UtilsLinearPolynomial<Flt64>,
            parent: IntermediateSymbol? = null,
            name: String = "",
            displayName: String? = null
        ): LinearExpressionSymbol {
            return LinearExpressionSymbol(
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
            constant: Int,
            parent: IntermediateSymbol? = null,
            name: String = "",
            displayName: String? = null
        ): LinearExpressionSymbol {
            return LinearExpressionSymbol(
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
            parent: IntermediateSymbol? = null,
            name: String = "",
            displayName: String? = null
        ): LinearExpressionSymbol {
            return LinearExpressionSymbol(
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
            parent: IntermediateSymbol? = null,
            name: String = "",
            displayName: String? = null
        ): LinearExpressionSymbol {
            return LinearExpressionSymbol(
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
            parent: IntermediateSymbol? = null,
            name: String = "",
            displayName: String? = null
        ): LinearExpressionSymbol {
            return LinearExpressionSymbol(
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
            parent: IntermediateSymbol? = null,
            name: String = "",
            displayName: String? = null
        ): LinearExpressionSymbol {
            return LinearExpressionSymbol(
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
            parent: IntermediateSymbol? = null,
            name: String = "",
            displayName: String? = null
        ): LinearExpressionSymbol {
            return LinearExpressionSymbol(
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
            parent: IntermediateSymbol? = null,
            name: String = "",
            displayName: String? = null
        ): LinearExpressionSymbol {
            return LinearExpressionSymbol(
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

    // polynomial property returns immutable version
    val polynomial: UtilsLinearPolynomial<Flt64> get() = _utilsPolynomial.toLinearPolynomial()

    // flattenedMonomials: extract from monomials
    override val flattenedMonomials: LinearFlattenData
        get() = LinearFlattenData(
            monomials = _utilsPolynomial.monomials,
            constant = _utilsPolynomial.constant
        )

    @Deprecated("Use flattenedMonomials instead.", level = DeprecationLevel.WARNING)
    override val cells: List<LinearMonomialCell>
        get() = flattenedMonomials.toLinearMonomialCells()

    // asMutable returns UtilsMutableLinearPolynomial<Flt64>
    fun asMutable(): UtilsMutableLinearPolynomial<Flt64> {
        return _utilsPolynomial
    }

    // Helper: evaluate a single monomial's symbol
    private fun evaluateSymbol(symbol: Symbol, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return when (symbol) {
            is AbstractVariableItem<*, *> -> {
                val token = tokenList.find(symbol)
                token?.result ?: if (zeroIfNone) Flt64.zero else null
            }
            is LinearIntermediateSymbol -> symbol.evaluate(tokenList, zeroIfNone)
            else -> null
        }
    }

    private fun evaluateSymbol(symbol: Symbol, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        return when (symbol) {
            is AbstractVariableItem<*, *> -> {
                val token = tokenTable.find(symbol)
                token?.result ?: if (zeroIfNone) Flt64.zero else null
            }
            is LinearIntermediateSymbol -> symbol.evaluate(tokenTable, zeroIfNone)
            else -> null
        }
    }

    private fun evaluateSymbol(symbol: Symbol, results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return when (symbol) {
            is AbstractVariableItem<*, *> -> {
                val index = tokenList.indexOf(symbol)
                if (index != null && index >= 0 && index < results.size) results[index]
                else if (zeroIfNone) Flt64.zero else null
            }
            is LinearIntermediateSymbol -> symbol.evaluate(results, tokenList, zeroIfNone)
            else -> null
        }
    }

    private fun evaluateSymbol(symbol: Symbol, results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        return when (symbol) {
            is AbstractVariableItem<*, *> -> {
                val index = tokenTable.indexOf(symbol)
                if (index != null && index >= 0 && index < results.size) results[index]
                else if (zeroIfNone) Flt64.zero else null
            }
            is LinearIntermediateSymbol -> symbol.evaluate(results, tokenTable, zeroIfNone)
            else -> null
        }
    }

    private fun evaluateSymbol(symbol: Symbol, values: Map<Symbol, Flt64>, tokenList: AbstractTokenList?, zeroIfNone: Boolean): Flt64? {
        return when (symbol) {
            is AbstractVariableItem<*, *> -> {
                values[symbol] ?: tokenList?.find(symbol)?.result ?: if (zeroIfNone) Flt64.zero else null
            }
            is LinearIntermediateSymbol -> symbol.evaluate(values, tokenList, zeroIfNone)
            else -> values[symbol] ?: if (zeroIfNone) Flt64.zero else null
        }
    }

    private fun evaluateSymbol(symbol: Symbol, values: Map<Symbol, Flt64>, tokenTable: AbstractTokenTable?, zeroIfNone: Boolean): Flt64? {
        return when (symbol) {
            is AbstractVariableItem<*, *> -> {
                values[symbol] ?: tokenTable?.find(symbol)?.result ?: if (zeroIfNone) Flt64.zero else null
            }
            is LinearIntermediateSymbol -> symbol.evaluate(values, tokenTable, zeroIfNone)
            else -> values[symbol] ?: if (zeroIfNone) Flt64.zero else null
        }
    }

    // dependencies: extract IntermediateSymbol from monomials.symbol
    override val dependencies: Set<IntermediateSymbol>
        get() = _utilsPolynomial.monomials
            .mapNotNull { monomial ->
                when (val sym = monomial.symbol) {
                    is LinearIntermediateSymbol -> sym
                    else -> null
                }
            }
            .toSet()

    // range/lowerBound/upperBound using possibleRange algorithm
    private fun calculateRange(): ValueRange<Flt64>? {
        var range: ValueRange<Flt64>? = ValueRange(_utilsPolynomial.constant, Flt64).value
        for (monomial in _utilsPolynomial.monomials) {
            val symRange = when (val sym = monomial.symbol) {
                is AbstractVariableItem<*, *> -> sym.range.valueRange
                is LinearIntermediateSymbol -> sym.range.valueRange
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
        get() = _utilsPolynomial.monomials.all { monomial ->
            when (val sym = monomial.symbol) {
                is AbstractVariableItem<*, *> -> sym.type.isIntegerType && monomial.coefficient.round() eq monomial.coefficient
                is LinearIntermediateSymbol -> sym.discrete && monomial.coefficient.round() eq monomial.coefficient
                else -> false
            }
        } && _utilsPolynomial.constant.round() eq _utilsPolynomial.constant

    // cached/flush via TokenTable cache key
    private val rangeCacheKey = newTokenCacheKey(
        category = Linear,
        prefix = "__linear_expression_range_cache__"
    )
    private val flattenCacheKey = newTokenCacheKey(
        category = Linear,
        prefix = "__linear_expression_flatten_cache__"
    )

    private fun cacheTokenTable(): AbstractTokenTable? {
        return dependencies
            .asSequence()
            .mapNotNull { boundTokenTableContext(it) }
            .firstOrNull()
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
    override fun prepare(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable): Flt64? {
        // Trigger flattenedMonomials to populate cache
        val flatten = flattenedMonomials

        return if (values.isNullOrEmpty()) {
            var ret = _utilsPolynomial.constant
            for (monomial in _utilsPolynomial.monomials) {
                val symbolValue = evaluateSymbol(monomial.symbol, tokenTable, false)
                if (symbolValue == null) return null
                ret += monomial.coefficient * symbolValue
            }
            ret
        } else {
            var ret = _utilsPolynomial.constant
            for (monomial in _utilsPolynomial.monomials) {
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
            val monomialStrings = _utilsPolynomial.monomials.filter { it.coefficient neq Flt64.zero }.map { m ->
                val symStr = when (val sym = m.symbol) {
                    is IntermediateSymbol -> sym.toRawString(unfold - UInt64.one)
                    else -> sym.name
                }
                if (m.coefficient eq Flt64.one) symStr
                else if (m.coefficient eq -Flt64.one) "-$symStr"
                else "${m.coefficient} * $symStr"
            }
            if (_utilsPolynomial.constant neq Flt64.zero) {
                "${monomialStrings.joinToString(" + ")} + ${_utilsPolynomial.constant}"
            } else {
                monomialStrings.joinToString(" + ")
            }
        } else {
            displayName ?: name
        }
    }

    // evaluate methods - similar to Polynomial.evaluate implementation
    override fun evaluate(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        var ret = _utilsPolynomial.constant
        for (monomial in _utilsPolynomial.monomials) {
            val symbolValue = evaluateSymbol(monomial.symbol, tokenList, zeroIfNone)
            if (symbolValue == null && !zeroIfNone) return null
            ret += monomial.coefficient * (symbolValue ?: Flt64.zero)
        }
        return ret
    }

    override fun evaluate(tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        return evaluateWithCachedTokenTable(tokenTable, zeroIfNone) {
            var ret = _utilsPolynomial.constant
            for (monomial in _utilsPolynomial.monomials) {
                val symbolValue = evaluateSymbol(monomial.symbol, tokenTable, zeroIfNone)
                if (symbolValue == null && !zeroIfNone) return@evaluateWithCachedTokenTable null
                ret += monomial.coefficient * (symbolValue ?: Flt64.zero)
            }
            ret
        }
    }

    override fun evaluate(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        var ret = _utilsPolynomial.constant
        for (monomial in _utilsPolynomial.monomials) {
            val symbolValue = evaluateSymbol(monomial.symbol, results, tokenList, zeroIfNone)
            if (symbolValue == null && !zeroIfNone) return null
            ret += monomial.coefficient * (symbolValue ?: Flt64.zero)
        }
        return ret
    }

    override fun evaluate(results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        return evaluateWithCachedTokenTable(results, tokenTable, zeroIfNone) {
            var ret = _utilsPolynomial.constant
            for (monomial in _utilsPolynomial.monomials) {
                val symbolValue = evaluateSymbol(monomial.symbol, results, tokenTable, zeroIfNone)
                if (symbolValue == null && !zeroIfNone) return@evaluateWithCachedTokenTable null
                ret += monomial.coefficient * (symbolValue ?: Flt64.zero)
            }
            ret
        }
    }

    override fun evaluate(values: Map<Symbol, Flt64>, tokenList: AbstractTokenList?, zeroIfNone: Boolean): Flt64? {
        if (values.containsKey(this)) return values[this]!!
        var ret = _utilsPolynomial.constant
        for (monomial in _utilsPolynomial.monomials) {
            val symbolValue = evaluateSymbol(monomial.symbol, values, tokenList, zeroIfNone)
            if (symbolValue == null && !zeroIfNone) return null
            ret += monomial.coefficient * (symbolValue ?: Flt64.zero)
        }
        return ret
    }

    override fun evaluate(values: Map<Symbol, Flt64>, tokenTable: AbstractTokenTable?, zeroIfNone: Boolean): Flt64? {
        return evaluateWithCachedTokenTable(values, tokenTable, zeroIfNone) {
            if (values.containsKey(this)) {
                return@evaluateWithCachedTokenTable values[this]!!
            }
            var ret = _utilsPolynomial.constant
            for (monomial in _utilsPolynomial.monomials) {
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
        if (other !is LinearExpressionSymbol) return false

        if (identifier != other.identifier) return false
        if (index != other.index) return false
        if (name != other.name) return false

        return true
    }
}

@Deprecated("DSL expression wrapper, use MathFunctionSymbol instead")
class QuadraticExpressionSymbol(
    internal val _utilsPolynomial: UtilsMutableQuadraticPolynomial<Flt64>,
    category: Category = _utilsPolynomial.category,
    parent: IntermediateSymbol? = null,
    name: String = "",
    displayName: String? = null
) : QuadraticIntermediateSymbol {
    internal var _group: AbstractSymbolCombination<*>? = null
    internal var _index: Int? = null
    override val identifier: UInt64 by lazy {
        _group?.identifier ?: IdentifierGenerator.gen()
    }
    override val index: Int by lazy {
        _index ?: 0
    }

    override val category: Category = category
    override val parent: IntermediateSymbol? = parent
    override var name: String = name
    override var displayName: String? = displayName

    override val operationCategory: Category = Quadratic

    companion object {
        operator fun invoke(
            item: AbstractVariableItem<*, *>,
            parent: IntermediateSymbol? = null,
            name: String = "",
            displayName: String? = null
        ): QuadraticExpressionSymbol {
            return QuadraticExpressionSymbol(
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
            symbol: LinearIntermediateSymbol,
            parent: IntermediateSymbol? = null,
            name: String = "",
            displayName: String? = null
        ): QuadraticExpressionSymbol {
            return QuadraticExpressionSymbol(
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
            symbol: QuadraticIntermediateSymbol,
            parent: IntermediateSymbol? = null,
            name: String = "",
            displayName: String? = null
        ): QuadraticExpressionSymbol {
            return QuadraticExpressionSymbol(
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
            monomial: LinearMonomial,
            parent: IntermediateSymbol? = null,
            name: String = "",
            displayName: String? = null
        ): QuadraticExpressionSymbol {
            val utilsLinearMonomial = monomial.toUtilsMonomial()
            return QuadraticExpressionSymbol(
                _utilsPolynomial = UtilsMutableQuadraticPolynomial(
                    monomials = listOf(UtilsQuadraticMonomial.linear(utilsLinearMonomial.coefficient, utilsLinearMonomial.symbol)),
                    constant = Flt64.zero
                ),
                category = Quadratic,
                parent = parent,
                name = name.ifEmpty { monomial.name },
                displayName = displayName ?: monomial.displayName
            )
        }

        operator fun invoke(
            monomial: QuadraticMonomial,
            parent: IntermediateSymbol? = null,
            name: String = "",
            displayName: String? = null
        ): QuadraticExpressionSymbol {
            val sym1: Symbol = when (monomial.symbol.symbol1) {
                is Variant3.V1 -> monomial.symbol.symbol1.value
                is Variant3.V2 -> monomial.symbol.symbol1.value
                is Variant3.V3 -> monomial.symbol.symbol1.value
            }
            val utilsMonomial = if (monomial.symbol.symbol2 == null) {
                UtilsQuadraticMonomial.linear(monomial.coefficient, sym1)
            } else {
                val sym2: Symbol = when (monomial.symbol.symbol2!!) {
                    is Variant3.V1 -> monomial.symbol.symbol2!!.value
                    is Variant3.V2 -> monomial.symbol.symbol2!!.value
                    is Variant3.V3 -> monomial.symbol.symbol2!!.value
                }
                UtilsQuadraticMonomial.quadratic(monomial.coefficient, sym1, sym2)
            }
            return QuadraticExpressionSymbol(
                _utilsPolynomial = UtilsMutableQuadraticPolynomial(
                    monomials = listOf(utilsMonomial),
                    constant = Flt64.zero
                ),
                category = Quadratic,
                parent = parent,
                name = name.ifEmpty { monomial.name },
                displayName = displayName ?: monomial.displayName
            )
        }

        operator fun invoke(
            constant: Int,
            parent: IntermediateSymbol? = null,
            name: String = "",
            displayName: String? = null
        ): QuadraticExpressionSymbol {
            return QuadraticExpressionSymbol(
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
            parent: IntermediateSymbol? = null,
            name: String = "",
            displayName: String? = null
        ): QuadraticExpressionSymbol {
            return QuadraticExpressionSymbol(
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
            parent: IntermediateSymbol? = null,
            name: String = "",
            displayName: String? = null
        ): QuadraticExpressionSymbol {
            return QuadraticExpressionSymbol(
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
            parent: IntermediateSymbol? = null,
            name: String = "",
            displayName: String? = null
        ): QuadraticExpressionSymbol {
            return QuadraticExpressionSymbol(
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
            parent: IntermediateSymbol? = null,
            name: String = "",
            displayName: String? = null
        ): QuadraticExpressionSymbol {
            return QuadraticExpressionSymbol(
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
            parent: IntermediateSymbol? = null,
            name: String = "",
            displayName: String? = null
        ): QuadraticExpressionSymbol {
            return QuadraticExpressionSymbol(
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
            parent: IntermediateSymbol? = null,
            name: String = "",
            displayName: String? = null
        ): QuadraticExpressionSymbol {
            return QuadraticExpressionSymbol(
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

    // polynomial property returns immutable version
    val polynomial: UtilsQuadraticPolynomial<Flt64> get() = _utilsPolynomial.toQuadraticPolynomial()

    // flattenedMonomials: extract from monomials (distinguish linear vs quadratic)
    override val flattenedMonomials: QuadraticFlattenData
        get() = QuadraticFlattenData(
            monomials = _utilsPolynomial.monomials,
            constant = _utilsPolynomial.constant
        )

    @Deprecated("Use flattenedMonomials instead.", level = DeprecationLevel.WARNING)
    override val cells: List<QuadraticMonomialCell>
        get() = flattenedMonomials.toQuadraticMonomialCells()

    // asMutable returns UtilsMutableQuadraticPolynomial<Flt64>
    fun asMutable(): UtilsMutableQuadraticPolynomial<Flt64> {
        return _utilsPolynomial
    }

    // Helper: evaluate a single symbol
    private fun evaluateSymbol(symbol: Symbol, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return when (symbol) {
            is AbstractVariableItem<*, *> -> {
                val token = tokenList.find(symbol)
                token?.result ?: if (zeroIfNone) Flt64.zero else null
            }
            is LinearIntermediateSymbol -> symbol.evaluate(tokenList, zeroIfNone)
            is QuadraticIntermediateSymbol -> symbol.evaluate(tokenList, zeroIfNone)
            else -> null
        }
    }

    private fun evaluateSymbol(symbol: Symbol, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        return when (symbol) {
            is AbstractVariableItem<*, *> -> {
                val token = tokenTable.find(symbol)
                token?.result ?: if (zeroIfNone) Flt64.zero else null
            }
            is LinearIntermediateSymbol -> symbol.evaluate(tokenTable, zeroIfNone)
            is QuadraticIntermediateSymbol -> symbol.evaluate(tokenTable, zeroIfNone)
            else -> null
        }
    }

    private fun evaluateSymbol(symbol: Symbol, results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return when (symbol) {
            is AbstractVariableItem<*, *> -> {
                val index = tokenList.indexOf(symbol)
                if (index != null && index >= 0 && index < results.size) results[index]
                else if (zeroIfNone) Flt64.zero else null
            }
            is LinearIntermediateSymbol -> symbol.evaluate(results, tokenList, zeroIfNone)
            is QuadraticIntermediateSymbol -> symbol.evaluate(results, tokenList, zeroIfNone)
            else -> null
        }
    }

    private fun evaluateSymbol(symbol: Symbol, results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        return when (symbol) {
            is AbstractVariableItem<*, *> -> {
                val index = tokenTable.indexOf(symbol)
                if (index != null && index >= 0 && index < results.size) results[index]
                else if (zeroIfNone) Flt64.zero else null
            }
            is LinearIntermediateSymbol -> symbol.evaluate(results, tokenTable, zeroIfNone)
            is QuadraticIntermediateSymbol -> symbol.evaluate(results, tokenTable, zeroIfNone)
            else -> null
        }
    }

    private fun evaluateSymbol(symbol: Symbol, values: Map<Symbol, Flt64>, tokenList: AbstractTokenList?, zeroIfNone: Boolean): Flt64? {
        return when (symbol) {
            is AbstractVariableItem<*, *> -> {
                values[symbol] ?: tokenList?.find(symbol)?.result ?: if (zeroIfNone) Flt64.zero else null
            }
            is LinearIntermediateSymbol -> symbol.evaluate(values, tokenList, zeroIfNone)
            is QuadraticIntermediateSymbol -> symbol.evaluate(values, tokenList, zeroIfNone)
            else -> values[symbol] ?: if (zeroIfNone) Flt64.zero else null
        }
    }

    private fun evaluateSymbol(symbol: Symbol, values: Map<Symbol, Flt64>, tokenTable: AbstractTokenTable?, zeroIfNone: Boolean): Flt64? {
        return when (symbol) {
            is AbstractVariableItem<*, *> -> {
                values[symbol] ?: tokenTable?.find(symbol)?.result ?: if (zeroIfNone) Flt64.zero else null
            }
            is LinearIntermediateSymbol -> symbol.evaluate(values, tokenTable, zeroIfNone)
            is QuadraticIntermediateSymbol -> symbol.evaluate(values, tokenTable, zeroIfNone)
            else -> values[symbol] ?: if (zeroIfNone) Flt64.zero else null
        }
    }

    // dependencies: extract IntermediateSymbol from monomials.symbol1/symbol2
    override val dependencies: Set<IntermediateSymbol>
        get() = _utilsPolynomial.monomials
            .flatMap { monomial ->
                val deps = mutableListOf<IntermediateSymbol>()
                when (val sym1 = monomial.symbol1) {
                    is LinearIntermediateSymbol -> deps.add(sym1)
                    is QuadraticIntermediateSymbol -> deps.add(sym1)
                }
                if (monomial.symbol2 != null) {
                    when (val sym2 = monomial.symbol2!!) {
                        is LinearIntermediateSymbol -> deps.add(sym2)
                        is QuadraticIntermediateSymbol -> deps.add(sym2)
                    }
                }
                deps
            }
            .toSet()

    // range/lowerBound/upperBound using possibleRange algorithm
    private fun calculateRange(): ValueRange<Flt64>? {
        var range: ValueRange<Flt64>? = ValueRange(_utilsPolynomial.constant, Flt64).value
        for (monomial in _utilsPolynomial.monomials) {
            val sym1Range: ValueRange<Flt64>? = when (val sym1 = monomial.symbol1) {
                is AbstractVariableItem<*, *> -> sym1.range.valueRange
                is LinearIntermediateSymbol -> sym1.range.valueRange
                is QuadraticIntermediateSymbol -> sym1.range.valueRange
                else -> null
            }
            val sym2Range: ValueRange<Flt64>? = if (monomial.symbol2 != null) {
                when (val sym2 = monomial.symbol2!!) {
                    is AbstractVariableItem<*, *> -> sym2.range.valueRange
                    is LinearIntermediateSymbol -> sym2.range.valueRange
                    is QuadraticIntermediateSymbol -> sym2.range.valueRange
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
        get() = _utilsPolynomial.monomials.all { monomial ->
            val sym1Discrete = when (val sym1 = monomial.symbol1) {
                is AbstractVariableItem<*, *> -> sym1.type.isIntegerType && monomial.coefficient.round() eq monomial.coefficient
                is LinearIntermediateSymbol -> sym1.discrete && monomial.coefficient.round() eq monomial.coefficient
                is QuadraticIntermediateSymbol -> sym1.discrete && monomial.coefficient.round() eq monomial.coefficient
                else -> false
            }
            val sym2Discrete = if (monomial.symbol2 != null) {
                when (val sym2 = monomial.symbol2!!) {
                    is AbstractVariableItem<*, *> -> sym2.type.isIntegerType && monomial.coefficient.round() eq monomial.coefficient
                    is LinearIntermediateSymbol -> sym2.discrete && monomial.coefficient.round() eq monomial.coefficient
                    is QuadraticIntermediateSymbol -> sym2.discrete && monomial.coefficient.round() eq monomial.coefficient
                    else -> false
                }
            } else true
            sym1Discrete && sym2Discrete
        } && _utilsPolynomial.constant.round() eq _utilsPolynomial.constant

    // cached/flush via TokenTable cache key
    private val rangeCacheKey = newTokenCacheKey(
        category = Quadratic,
        prefix = "__quadratic_expression_range_cache__"
    )
    private val flattenCacheKey = newTokenCacheKey(
        category = Quadratic,
        prefix = "__quadratic_expression_flatten_cache__"
    )

    private fun cacheTokenTable(): AbstractTokenTable? {
        return dependencies
            .asSequence()
            .mapNotNull { boundTokenTableContext(it) }
            .firstOrNull()
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
    override fun prepare(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable): Flt64? {
        // Trigger flattenedMonomials to populate cache
        val flatten = flattenedMonomials

        return if (values.isNullOrEmpty()) {
            var ret = _utilsPolynomial.constant
            for (monomial in _utilsPolynomial.monomials) {
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
            var ret = _utilsPolynomial.constant
            for (monomial in _utilsPolynomial.monomials) {
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
            val monomialStrings = _utilsPolynomial.monomials.filter { it.coefficient neq Flt64.zero }.map { m ->
                val sym1Str = when (val s1 = m.symbol1) {
                    is IntermediateSymbol -> s1.toRawString(unfold - UInt64.one)
                    else -> s1.name
                }
                val termStr = if (m.symbol2 != null) {
                    val sym2Str = when (val s2 = m.symbol2!!) {
                        is IntermediateSymbol -> s2.toRawString(unfold - UInt64.one)
                        else -> s2.name
                    }
                    if (m.symbol1 == m.symbol2) "$sym1Str^2" else "$sym1Str * $sym2Str"
                } else sym1Str
                if (m.coefficient eq Flt64.one) termStr
                else if (m.coefficient eq -Flt64.one) "-$termStr"
                else "${m.coefficient} * $termStr"
            }
            if (_utilsPolynomial.constant neq Flt64.zero) {
                "${monomialStrings.joinToString(" + ")} + ${_utilsPolynomial.constant}"
            } else {
                monomialStrings.joinToString(" + ")
            }
        } else {
            displayName ?: name
        }
    }

    // evaluate methods - similar to Polynomial.evaluate implementation
    override fun evaluate(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        var ret = _utilsPolynomial.constant
        for (monomial in _utilsPolynomial.monomials) {
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

    override fun evaluate(tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        return evaluateWithCachedTokenTable(tokenTable, zeroIfNone) {
            var ret = _utilsPolynomial.constant
            for (monomial in _utilsPolynomial.monomials) {
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

    override fun evaluate(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        var ret = _utilsPolynomial.constant
        for (monomial in _utilsPolynomial.monomials) {
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

    override fun evaluate(results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        return evaluateWithCachedTokenTable(results, tokenTable, zeroIfNone) {
            var ret = _utilsPolynomial.constant
            for (monomial in _utilsPolynomial.monomials) {
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

    override fun evaluate(values: Map<Symbol, Flt64>, tokenList: AbstractTokenList?, zeroIfNone: Boolean): Flt64? {
        if (values.containsKey(this)) return values[this]!!
        var ret = _utilsPolynomial.constant
        for (monomial in _utilsPolynomial.monomials) {
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

    override fun evaluate(values: Map<Symbol, Flt64>, tokenTable: AbstractTokenTable?, zeroIfNone: Boolean): Flt64? {
        return evaluateWithCachedTokenTable(values, tokenTable, zeroIfNone) {
            if (values.containsKey(this)) {
                return@evaluateWithCachedTokenTable values[this]!!
            }
            var ret = _utilsPolynomial.constant
            for (monomial in _utilsPolynomial.monomials) {
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
        if (other !is QuadraticExpressionSymbol) return false

        if (identifier != other.identifier) return false
        if (index != other.index) return false
        if (name != other.name) return false

        return true
    }
}

data class FunctionSymbolRegistrationScope(
    val tokens: MutableList<AbstractVariableItem<*, *>> = mutableListOf(),
    val origin: AbstractTokenTable? = null
) : AddableTokenCollection {
    override fun add(item: AbstractVariableItem<*, *>): Try {
        tokens.add(item)
        return ok
    }

    override fun add(items: Iterable<AbstractVariableItem<*, *>>): Try {
        tokens.addAll(items)
        return ok
    }
}

@Deprecated("Use fuookami.ospf.kotlin.core.intermediate_symbol.function.MathFunctionSymbol instead")
interface FunctionSymbol : IntermediateSymbol {
    fun register(tokenTable: AddableTokenCollection): Try

    fun register(
        tokenTable: AddableTokenCollection,
        fixedValues: Map<Symbol, Flt64>
    ): Try {
        return register(tokenTable)
    }

    override fun evaluate(tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        return evaluateWithCachedTokenTable(tokenTable, zeroIfNone) {
            calculateValue(
                tokenTable = tokenTable,
                zeroIfNone = zeroIfNone
            )
        }
    }

    override fun evaluate(results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        return evaluateWithCachedTokenTable(results, tokenTable, zeroIfNone) {
            calculateValue(
                results = results,
                tokenTable = tokenTable,
                zeroIfNone = zeroIfNone
            )
        }
    }

    override fun evaluate(values: Map<Symbol, Flt64>, tokenTable: AbstractTokenTable?, zeroIfNone: Boolean): Flt64? {
        return evaluateWithCachedTokenTable(values, tokenTable, zeroIfNone) {
            calculateValue(
                values = values,
                tokenTable = tokenTable,
                zeroIfNone = zeroIfNone
            )
        }
    }

    fun calculateValue(tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64?
    fun calculateValue(results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64?
    fun calculateValue(values: Map<Symbol, Flt64>, tokenTable: AbstractTokenTable?, zeroIfNone: Boolean): Flt64?
}

@Deprecated("Use fuookami.ospf.kotlin.core.intermediate_symbol.function.MathFunctionSymbol instead")
interface LogicFunctionSymbol : FunctionSymbol {
    fun isTrue(tokenList: AbstractTokenList, zeroIfNone: Boolean): Boolean? {
        return this.evaluate(
            tokenList = tokenList,
            zeroIfNone = zeroIfNone
        )?.let { it eq Flt64.one }
    }

    fun isTrue(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Boolean? {
        return this.evaluate(
            results = results,
            tokenList = tokenList,
            zeroIfNone = zeroIfNone
        )?.let { it eq Flt64.one }
    }

    fun isTrue(values: Map<Symbol, Flt64>, tokenList: AbstractTokenList?, zeroIfNone: Boolean): Boolean? {
        return this.evaluate(
            values = values,
            tokenList = tokenList,
            zeroIfNone = zeroIfNone
        )?.let { it eq Flt64.one }
    }
}

@Deprecated("Use fuookami.ospf.kotlin.core.intermediate_symbol.function.MathFunctionSymbol instead")
abstract class LinearFunctionSymbol : LinearIntermediateSymbol, FunctionSymbol {
    internal var _group: AbstractSymbolCombination<*>? = null
    internal var _index: Int? = null
    override val identifier: UInt64 by lazy {
        _group?.identifier ?: IdentifierGenerator.gen()
    }
    override val index: Int by lazy {
        _index ?: 0
    }

    abstract fun register(model: AbstractLinearMechanismModel): Try
    open fun register(
        model: AbstractLinearMechanismModel,
        fixedValues: Map<Symbol, Flt64>
    ): Try {
        return register(model)
    }

    override fun hashCode(): Int {
        return identifier.toInt() * 31 + index
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LinearFunctionSymbol) return false

        if (identifier != other.identifier) return false
        if (index != other.index) return false
        if (name != other.name) return false

        return true
    }
}

@Deprecated("Use fuookami.ospf.kotlin.core.intermediate_symbol.function.MathFunctionSymbol instead")
abstract class LinearLogicFunctionSymbol : LinearFunctionSymbol(), LogicFunctionSymbol {}

@Deprecated("Use fuookami.ospf.kotlin.core.intermediate_symbol.function.MathFunctionSymbol instead")
abstract class QuadraticFunctionSymbol : QuadraticIntermediateSymbol, FunctionSymbol {
    internal var _group: AbstractSymbolCombination<*>? = null
    internal var _index: Int? = null
    override val identifier: UInt64 by lazy {
        _group?.identifier ?: IdentifierGenerator.gen()
    }
    override val index: Int by lazy {
        _index ?: 0
    }

    abstract fun register(model: AbstractQuadraticMechanismModel): Try
    open fun register(
        model: AbstractQuadraticMechanismModel,
        fixedValues: Map<Symbol, Flt64>
    ): Try {
        return register(model)
    }

    override fun hashCode(): Int {
        return identifier.toInt() * 31 + index
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is QuadraticFunctionSymbol) return false

        if (identifier != other.identifier) return false
        if (index != other.index) return false
        if (name != other.name) return false

        return true
    }
}

@Deprecated("Use fuookami.ospf.kotlin.core.intermediate_symbol.function.MathFunctionSymbol instead")
abstract class QuadraticLogicFunctionSymbol : QuadraticFunctionSymbol(), LogicFunctionSymbol {}

operator fun LinearIntermediateSymbol.times(rhs: PhysicalUnit): Quantity<LinearIntermediateSymbol> {
    return Quantity(this, rhs)
}

operator fun LinearIntermediateSymbol.div(rhs: PhysicalUnit): Quantity<LinearIntermediateSymbol> {
    return Quantity(this, rhs.reciprocal())
}

operator fun QuadraticIntermediateSymbol.times(rhs: PhysicalUnit): Quantity<QuadraticIntermediateSymbol> {
    return Quantity(this, rhs)
}

operator fun QuadraticIntermediateSymbol.div(rhs: PhysicalUnit): Quantity<QuadraticIntermediateSymbol> {
    return Quantity(this, rhs.reciprocal())
}

typealias QuantityIntermediateSymbol = Quantity<IntermediateSymbol>
typealias QuantityLinearIntermediateSymbol = Quantity<LinearIntermediateSymbol>
typealias QuantityQuadraticIntermediateSymbol = Quantity<QuadraticIntermediateSymbol>
typealias QuantityExpressionSymbol = Quantity<ExpressionSymbol>
typealias QuantityLinearExpressionSymbol = Quantity<LinearExpressionSymbol>
typealias QuantityQuadraticExpressionSymbol = Quantity<QuadraticExpressionSymbol>
typealias QuantityFunctionSymbol = Quantity<FunctionSymbol>
typealias QuantityLinearFunctionSymbol = Quantity<LinearFunctionSymbol>
typealias QuantityQuadraticFunctionSymbol = Quantity<QuadraticFunctionSymbol>


