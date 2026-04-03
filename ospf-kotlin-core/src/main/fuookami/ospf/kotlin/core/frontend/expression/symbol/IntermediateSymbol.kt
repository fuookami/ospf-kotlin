package fuookami.ospf.kotlin.core.frontend.expression.symbol

import fuookami.ospf.kotlin.core.frontend.expression.Expression
import fuookami.ospf.kotlin.core.frontend.expression.monomial.LinearMonomial
import fuookami.ospf.kotlin.core.frontend.expression.monomial.LinearMonomialCell
import fuookami.ospf.kotlin.core.frontend.expression.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.core.frontend.expression.monomial.QuadraticMonomialCell
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.frontend.model.mechanism.AbstractQuadraticMechanismModel
import fuookami.ospf.kotlin.core.frontend.model.mechanism.AbstractTokenTable
import fuookami.ospf.kotlin.core.frontend.variable.AbstractTokenList
import fuookami.ospf.kotlin.core.frontend.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.frontend.variable.AddableTokenCollection
import fuookami.ospf.kotlin.core.frontend.variable.IdentifierGenerator
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok
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

interface LinearIntermediateSymbol : IntermediateSymbol, ToLinearPolynomial<LinearPolynomial>, ToQuadraticPolynomial<QuadraticPolynomial> {
    companion object {
        fun empty(
            parent: IntermediateSymbol? = null,
            name: String = "",
            displayName: String? = null
        ): LinearIntermediateSymbol {
            return LinearExpressionSymbol(
                _polynomial = MutableLinearPolynomial(
                    name = name,
                    displayName = displayName
                ),
                parent = parent,
                name = name,
                displayName = displayName
            )
        }
    }

    val cells: List<LinearMonomialCell>

    override fun toLinearPolynomial(): LinearPolynomial {
        return LinearPolynomial(this)
    }

    override fun toQuadraticPolynomial(): QuadraticPolynomial {
        return QuadraticPolynomial(this)
    }
}

interface QuadraticIntermediateSymbol : IntermediateSymbol, ToQuadraticPolynomial<QuadraticPolynomial> {
    companion object {
        fun empty(
            parent: IntermediateSymbol? = null,
            name: String = "",
            displayName: String? = null
        ): QuadraticIntermediateSymbol {
            return QuadraticExpressionSymbol(
                _polynomial = MutableQuadraticPolynomial(
                    name = name,
                    displayName = displayName
                ),
                parent = parent,
                name = name,
                displayName = displayName
            )
        }
    }

    val cells: List<QuadraticMonomialCell>

    override fun toQuadraticPolynomial(): QuadraticPolynomial {
        return QuadraticPolynomial(this)
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

class LinearExpressionSymbol(
    override val _polynomial: MutableLinearPolynomial,
    category: Category = _polynomial.category,
    parent: IntermediateSymbol? = null,
    name: String = "",
    displayName: String? = null
) : LinearIntermediateSymbol, ExpressionSymbol(
    _polynomial = _polynomial,
    category = category,
    parent = parent,
    name = name,
    displayName = displayName
) {
    companion object {
        operator fun invoke(
            item: AbstractVariableItem<*, *>,
            parent: IntermediateSymbol? = null,
            name: String = "",
            displayName: String? = null
        ): LinearExpressionSymbol {
            return LinearExpressionSymbol(
                _polynomial = MutableLinearPolynomial(
                    monomials = mutableListOf(LinearMonomial(item)),
                    name = name.ifEmpty { item.name },
                    displayName = displayName
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
                _polynomial = MutableLinearPolynomial(
                    monomials = mutableListOf(LinearMonomial(symbol)),
                    name = name.ifEmpty { symbol.name },
                    displayName = displayName ?: symbol.displayName
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
                _polynomial = MutableLinearPolynomial(
                    monomials = mutableListOf(monomial.copy()),
                    name = name.ifEmpty { monomial.name },
                    displayName = displayName ?: monomial.displayName
                ),
                category = Linear,
                parent = parent,
                name = name.ifEmpty { monomial.name },
                displayName = displayName ?: monomial.displayName
            )
        }

        operator fun invoke(
            polynomial: AbstractLinearPolynomial<*>,
            parent: IntermediateSymbol? = null,
            name: String = "",
            displayName: String? = null
        ): LinearExpressionSymbol {
            return LinearExpressionSymbol(
                _polynomial = MutableLinearPolynomial(
                    monomials = polynomial.monomials.map { it.copy() }.toMutableList(),
                    constant = polynomial.constant,
                    name = name.ifEmpty { polynomial.name },
                    displayName = displayName ?: polynomial.displayName
                ),
                category = Linear,
                parent = parent,
                name = name.ifEmpty { polynomial.name },
                displayName = displayName ?: polynomial.displayName
            )
        }

        operator fun invoke(
            constant: Int,
            parent: IntermediateSymbol? = null,
            name: String = "",
            displayName: String? = null
        ): LinearExpressionSymbol {
            return LinearExpressionSymbol(
                _polynomial = MutableLinearPolynomial(
                    constant = constant,
                    name = name,
                    displayName = displayName
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
                _polynomial = MutableLinearPolynomial(
                    constant = constant,
                    name = name,
                    displayName = displayName
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
                _polynomial = MutableLinearPolynomial(
                    constant = constant,
                    name = name,
                    displayName = displayName
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
                _polynomial = MutableLinearPolynomial(
                    constant = constant,
                    name = name,
                    displayName = displayName
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
                _polynomial = MutableLinearPolynomial(
                    constant = constant,
                    name = name,
                    displayName = displayName
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
                _polynomial = MutableLinearPolynomial(
                    constant = constant,
                    name = name,
                    displayName = displayName
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
                _polynomial = MutableLinearPolynomial(
                    constant = Flt64.zero,
                    name = name,
                    displayName = displayName
                ),
                category = Linear,
                parent = parent,
                name = name,
                displayName = displayName
            )
        }
    }

    override val operationCategory: Category = Linear
    override val polynomial: AbstractLinearPolynomial<*> get() = _polynomial
    override val cells by _polynomial::cells

    override fun asMutable(): MutableLinearPolynomial {
        return _polynomial
    }

    override fun prepare(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable): Flt64? {
        cells
        return if (values.isNullOrEmpty()) {
            _polynomial.evaluate(tokenTable)
        } else {
            _polynomial.evaluate(values, tokenTable)
        }
    }
}

class QuadraticExpressionSymbol(
    override val _polynomial: MutableQuadraticPolynomial,
    category: Category = _polynomial.category,
    parent: IntermediateSymbol? = null,
    name: String = "",
    displayName: String? = null
) : QuadraticIntermediateSymbol, ExpressionSymbol(
    _polynomial = _polynomial,
    category = category,
    parent = parent,
    name = name,
    displayName = displayName
) {
    companion object {
        operator fun invoke(
            item: AbstractVariableItem<*, *>,
            parent: IntermediateSymbol? = null,
            name: String = "",
            displayName: String? = null
        ): QuadraticExpressionSymbol {
            return QuadraticExpressionSymbol(
                _polynomial = MutableQuadraticPolynomial(
                    monomials = mutableListOf(QuadraticMonomial(item)),
                    name = name.ifEmpty { item.name },
                    displayName = displayName
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
                _polynomial = MutableQuadraticPolynomial(
                    monomials = mutableListOf(QuadraticMonomial(symbol)),
                    name = name.ifEmpty { symbol.name },
                    displayName = displayName ?: symbol.displayName
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
                _polynomial = MutableQuadraticPolynomial(
                    monomials = mutableListOf(QuadraticMonomial(symbol)),
                    name = name.ifEmpty { symbol.name },
                    displayName = displayName ?: symbol.displayName
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
            return QuadraticExpressionSymbol(
                _polynomial = MutableQuadraticPolynomial(
                    monomials = mutableListOf(QuadraticMonomial(monomial)),
                    name = name.ifEmpty { monomial.name },
                    displayName = displayName ?: monomial.displayName
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
            return QuadraticExpressionSymbol(
                _polynomial = MutableQuadraticPolynomial(
                    monomials = mutableListOf(monomial.copy()),
                    name = name.ifEmpty { monomial.name },
                    displayName = displayName ?: monomial.displayName
                ),
                category = Quadratic,
                parent = parent,
                name = name.ifEmpty { monomial.name },
                displayName = displayName ?: monomial.displayName
            )
        }

        operator fun invoke(
            polynomial: AbstractLinearPolynomial<*>,
            parent: IntermediateSymbol? = null,
            name: String = "",
            displayName: String? = null
        ): QuadraticExpressionSymbol {
            return QuadraticExpressionSymbol(
                _polynomial = MutableQuadraticPolynomial(
                    monomials = polynomial.monomials.map { QuadraticMonomial(it) }.toMutableList(),
                    constant = polynomial.constant,
                    name = name.ifEmpty { polynomial.name },
                    displayName = displayName ?: polynomial.displayName
                ),
                category = Quadratic,
                parent = parent,
                name = name.ifEmpty { polynomial.name },
                displayName = displayName ?: polynomial.displayName
            )
        }

        operator fun invoke(
            polynomial: AbstractQuadraticPolynomial<*>,
            parent: IntermediateSymbol? = null,
            name: String = "",
            displayName: String? = null
        ): QuadraticExpressionSymbol {
            return QuadraticExpressionSymbol(
                _polynomial = MutableQuadraticPolynomial(
                    monomials = polynomial.monomials.map { it.copy() }.toMutableList(),
                    constant = polynomial.constant,
                    name = name.ifEmpty { polynomial.name },
                    displayName = displayName ?: polynomial.displayName
                ),
                category = Quadratic,
                parent = parent,
                name = name.ifEmpty { polynomial.name },
                displayName = displayName ?: polynomial.displayName
            )
        }

        operator fun invoke(
            constant: Int,
            parent: IntermediateSymbol? = null,
            name: String = "",
            displayName: String? = null
        ): QuadraticExpressionSymbol {
            return QuadraticExpressionSymbol(
                _polynomial = MutableQuadraticPolynomial(
                    constant = constant,
                    name = name,
                    displayName = displayName
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
                _polynomial = MutableQuadraticPolynomial(
                    constant = constant,
                    name = name,
                    displayName = displayName
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
                _polynomial = MutableQuadraticPolynomial(
                    constant = constant,
                    name = name,
                    displayName = displayName
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
                _polynomial = MutableQuadraticPolynomial(
                    constant = constant,
                    name = name,
                    displayName = displayName
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
                _polynomial = MutableQuadraticPolynomial(
                    constant = constant,
                    name = name,
                    displayName = displayName
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
                _polynomial = MutableQuadraticPolynomial(
                    constant = constant,
                    name = name,
                    displayName = displayName
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
                _polynomial = MutableQuadraticPolynomial(
                    name = name,
                    displayName = displayName
                ),
                category = Quadratic,
                parent = parent,
                name = name,
                displayName = displayName
            )
        }
    }

    override val operationCategory: Category = Quadratic
    override val polynomial: AbstractQuadraticPolynomial<*> get() = _polynomial
    override val cells by _polynomial::cells

    override fun asMutable(): MutableQuadraticPolynomial {
        return _polynomial
    }

    override fun prepare(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable): Flt64? {
        cells
        return if (values.isNullOrEmpty()) {
            _polynomial.evaluate(tokenTable)
        } else {
            _polynomial.evaluate(values, tokenTable)
        }
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

abstract class LinearLogicFunctionSymbol : LinearFunctionSymbol(), LogicFunctionSymbol {}

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


