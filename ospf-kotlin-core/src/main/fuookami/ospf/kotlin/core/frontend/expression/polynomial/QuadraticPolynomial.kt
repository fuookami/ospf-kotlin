package fuookami.ospf.kotlin.core.frontend.expression.polynomial

import fuookami.ospf.kotlin.core.frontend.expression.ExpressionRange
import fuookami.ospf.kotlin.core.frontend.expression.adapter.toUtilsPolynomial
import fuookami.ospf.kotlin.core.frontend.expression.adapter.toUtilsValueProvider
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.frontend.expression.symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.frontend.expression.symbol.QuadraticIntermediateSymbol
import fuookami.ospf.kotlin.core.frontend.inequality.QuadraticInequality
import fuookami.ospf.kotlin.core.frontend.inequality.ToQuadraticInequality
import fuookami.ospf.kotlin.core.frontend.inequality.eq
import fuookami.ospf.kotlin.core.frontend.model.mechanism.AbstractTokenTable
import fuookami.ospf.kotlin.core.frontend.model.mechanism.boundTokenTableContext
import fuookami.ospf.kotlin.core.frontend.model.mechanism.newTokenCacheKey
import fuookami.ospf.kotlin.core.frontend.model.mechanism.QuadraticFlattenData
import fuookami.ospf.kotlin.core.frontend.variable.AbstractTokenList
import fuookami.ospf.kotlin.core.frontend.variable.AbstractVariableItem
import fuookami.ospf.kotlin.utils.functional.Variant3
import fuookami.ospf.kotlin.utils.functional.flatMapNotNull
import fuookami.ospf.kotlin.utils.math.BalancedTrivalent
import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.utils.math.Trivalent
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.math.symbol.adapter.MissingValuePolicy
import fuookami.ospf.kotlin.utils.math.symbol.operation.evaluate
import fuookami.ospf.kotlin.utils.physics.quantity.Quantity
import fuookami.ospf.kotlin.utils.physics.quantity.to
import fuookami.ospf.kotlin.utils.physics.quantity.toFlt64
import fuookami.ospf.kotlin.utils.physics.unit.*

interface ToQuadraticPolynomial<Poly : AbstractQuadraticPolynomial<Poly>> : ToQuadraticInequality {
    fun toQuadraticPolynomial(): Poly

    override fun toQuadraticInequality(): QuadraticInequality {
        return this.toQuadraticPolynomial() eq true
    }
}

@Throws(IllegalArgumentException::class)
fun List<Any>.toQuadraticPolynomial(): List<AbstractQuadraticPolynomial<*>> {
    return this.map {
        when (it) {
            is Int -> QuadraticPolynomial(it)
            is Double -> QuadraticPolynomial(it)
            is Boolean -> QuadraticPolynomial(it)
            is Trivalent -> QuadraticPolynomial(it)
            is BalancedTrivalent -> QuadraticPolynomial(it)
            is RealNumber<*> -> QuadraticPolynomial(it.toFlt64())
            is ToQuadraticPolynomial<*> -> it.toQuadraticPolynomial()
            else -> throw IllegalArgumentException("Cannot convert $it to a quadratic polynomial")
        }
    }
}

sealed class AbstractQuadraticPolynomial<Self : AbstractQuadraticPolynomial<Self>> :
    Polynomial<Self, QuadraticMonomial>, ToQuadraticPolynomial<QuadraticPolynomial> {
    abstract override val monomials: List<QuadraticMonomial>
    override val category: Category get() = monomials.map { it.category }.maxOrNull() ?: Linear

    private val rangeCacheKey = newTokenCacheKey(
        category = Quadratic,
        prefix = "__quadratic_polynomial_range_cache__"
    )
    private val flattenCacheKey = newTokenCacheKey(
        category = Quadratic,
        prefix = "__quadratic_polynomial_flatten_cache__"
    )

    private fun cacheTokenTable(): AbstractTokenTable? {
        return dependencies
            .asSequence()
            .mapNotNull { boundTokenTableContext(it) }
            .firstOrNull()
    }

    override val range: ExpressionRange<Flt64>
        get() {
            val tokenTable = cacheTokenTable()
            val cachedRange = tokenTable?.cachedRangeValue(rangeCacheKey)
            if (cachedRange != null) {
                return cachedRange
            }
            val range = ExpressionRange(possibleRange(monomials, constant), Flt64)
            tokenTable?.cacheRange(rangeCacheKey, range)
            return range
        }

    override val dependencies: Set<IntermediateSymbol>
        get() {
            return monomials.flatMapNotNull {
                val symbols = ArrayList<IntermediateSymbol>()
                when (val symbol = it.symbol.symbol1) {
                    is Variant3.V2 -> {
                        symbols.add(symbol.value)
                    }

                    is Variant3.V3 -> {
                        symbols.add(symbol.value)
                    }

                    else -> {}
                }
                when (val symbol = it.symbol.symbol2) {
                    is Variant3.V2 -> {
                        symbols.add(symbol.value)
                    }

                    is Variant3.V3 -> {
                        symbols.add(symbol.value)
                    }

                    else -> {}
                }
                symbols
            }.toSet()
        }

    val flattenData: QuadraticFlattenData
        get() {
            val tokenTable = cacheTokenTable()
            val cachedFlatten = tokenTable?.cachedQuadraticFlattenValue(flattenCacheKey)
            if (cachedFlatten != null) {
                return cachedFlatten
            }
            val data = monomials.fold(QuadraticFlattenData(emptyList(), Flt64.zero)) { acc, monomial ->
                acc + monomial.flattenData
            }
            tokenTable?.cacheQuadraticFlatten(flattenCacheKey, data)
            return data
        }

    override val cached: Boolean
        get() = cacheTokenTable()?.cachedQuadraticFlatten(flattenCacheKey) == true

    abstract operator fun plus(rhs: LinearIntermediateSymbol): Self

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("plusLinearSymbols")
    abstract operator fun plus(rhs: Iterable<LinearIntermediateSymbol>): Self
    abstract operator fun plus(rhs: QuadraticIntermediateSymbol): Self

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("plusQuadraticSymbols")
    abstract operator fun plus(rhs: Iterable<QuadraticIntermediateSymbol>): Self
    abstract operator fun plus(rhs: LinearMonomial): Self
    abstract operator fun plus(rhs: AbstractLinearPolynomial<*>): Self

    abstract operator fun minus(rhs: LinearIntermediateSymbol): Self

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("minusLinearSymbols")
    abstract operator fun minus(rhs: Iterable<LinearIntermediateSymbol>): Self
    abstract operator fun minus(rhs: QuadraticIntermediateSymbol): Self

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("minusQuadraticSymbols")
    abstract operator fun minus(rhs: Iterable<QuadraticIntermediateSymbol>): Self
    abstract operator fun minus(rhs: LinearMonomial): Self
    abstract operator fun minus(rhs: AbstractLinearPolynomial<*>): Self

    abstract operator fun times(rhs: AbstractVariableItem<*, *>): Self

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("timesVariables")
    abstract operator fun times(rhs: Iterable<AbstractVariableItem<*, *>>): Self

    abstract operator fun times(rhs: LinearIntermediateSymbol): Self

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("timesLinearSymbols")
    abstract operator fun times(rhs: Iterable<LinearIntermediateSymbol>): Self

    abstract operator fun times(rhs: QuadraticIntermediateSymbol): Self

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("timesQuadraticSymbols")
    abstract operator fun times(rhs: Iterable<QuadraticIntermediateSymbol>): Self
    abstract operator fun times(rhs: LinearMonomial): Self
    abstract operator fun times(rhs: QuadraticMonomial): Self
    abstract operator fun times(rhs: AbstractLinearPolynomial<*>): Self
    abstract operator fun times(rhs: AbstractQuadraticPolynomial<*>): Self

    final override fun evaluate(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return toUtilsPolynomial().evaluate(
            provider = tokenList.toUtilsValueProvider(zeroIfNone),
            policy = MissingValuePolicy.ReturnNull
        )
    }

    final override fun evaluate(tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        return toUtilsPolynomial().evaluate(
            provider = tokenTable.toUtilsValueProvider(zeroIfNone),
            policy = MissingValuePolicy.ReturnNull
        )
    }

    final override fun evaluate(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return toUtilsPolynomial().evaluate(
            provider = results.toUtilsValueProvider(
                tokenList = tokenList,
                zeroIfNone = zeroIfNone
            ),
            policy = MissingValuePolicy.ReturnNull
        )
    }

    final override fun evaluate(results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        return toUtilsPolynomial().evaluate(
            provider = results.toUtilsValueProvider(
                tokenTable = tokenTable,
                zeroIfNone = zeroIfNone
            ),
            policy = MissingValuePolicy.ReturnNull
        )
    }

    final override fun evaluate(values: Map<Symbol, Flt64>, tokenList: AbstractTokenList?, zeroIfNone: Boolean): Flt64? {
        return toUtilsPolynomial().evaluate(
            provider = values.toUtilsValueProvider(
                tokenList = tokenList,
                zeroIfNone = zeroIfNone
            ),
            policy = MissingValuePolicy.ReturnNull
        )
    }

    final override fun evaluate(values: Map<Symbol, Flt64>, tokenTable: AbstractTokenTable?, zeroIfNone: Boolean): Flt64? {
        return toUtilsPolynomial().evaluate(
            provider = values.toUtilsValueProvider(
                tokenTable = tokenTable,
                zeroIfNone = zeroIfNone
            ),
            policy = MissingValuePolicy.ReturnNull
        )
    }

    override fun toMutable(): MutableQuadraticPolynomial {
        return MutableQuadraticPolynomial(
            monomials = monomials.map { it.copy() }.toMutableList(),
            constant = constant
        )
    }

    override fun flush(force: Boolean) {
        val tokenTable = cacheTokenTable()
        val cachedRange = tokenTable?.cachedRangeValue(rangeCacheKey)
        if (force || cachedRange?.set == false) {
            tokenTable?.clearRange(rangeCacheKey)
        }
        for (monomial in monomials) {
            monomial.flush(force)
        }
        if (force || monomials.any { !it.cached }) {
            tokenTable?.clearQuadraticFlatten(flattenCacheKey)
        }
    }

    override fun toString(): String {
        return displayName
            ?: name.ifEmpty {
                if (monomials.isEmpty()) {
                    "$constant"
                } else if (constant eq Flt64.zero) {
                    monomials.joinToString(" + ") { it.toString() }
                } else {
                    "${monomials.joinToString(" + ") { it.toString() }} + $constant"
                }
            }
    }
}

class QuadraticPolynomial(
    override val monomials: List<QuadraticMonomial> = emptyList(),
    override val constant: Flt64 = Flt64.zero,
    override var name: String = "",
    override var displayName: String? = null
) : AbstractQuadraticPolynomial<QuadraticPolynomial>() {
    companion object {
        operator fun invoke(
            item: AbstractVariableItem<*, *>,
            name: String = "",
            displayName: String? = null
        ): QuadraticPolynomial {
            return QuadraticPolynomial(
                monomials = listOf(QuadraticMonomial(item)),
                name = name.ifEmpty { item.name },
                displayName = displayName
            )
        }

        operator fun invoke(
            symbol: LinearIntermediateSymbol,
            name: String = "",
            displayName: String? = null
        ): QuadraticPolynomial {
            return QuadraticPolynomial(
                monomials = listOf(QuadraticMonomial(symbol)),
                name = name.ifEmpty { symbol.name },
                displayName = displayName ?: symbol.displayName
            )
        }

        operator fun invoke(
            symbol: QuadraticIntermediateSymbol,
            name: String = "",
            displayName: String? = null
        ): QuadraticPolynomial {
            return QuadraticPolynomial(
                monomials = listOf(QuadraticMonomial(symbol)),
                name = name.ifEmpty { symbol.name },
                displayName = displayName ?: symbol.displayName
            )
        }

        operator fun invoke(
            monomial: LinearMonomial,
            name: String = "",
            displayName: String? = null
        ): QuadraticPolynomial {
            return QuadraticPolynomial(
                monomials = listOf(QuadraticMonomial(monomial)),
                name = name.ifEmpty { monomial.name },
                displayName = displayName ?: monomial.displayName
            )
        }

        operator fun invoke(
            monomial: QuadraticMonomial,
            name: String = "",
            displayName: String? = null
        ): QuadraticPolynomial {
            return QuadraticPolynomial(
                monomials = listOf(monomial),
                name = name.ifEmpty { monomial.name },
                displayName = displayName ?: monomial.displayName
            )
        }

        operator fun invoke(
            polynomial: AbstractLinearPolynomial<*>,
            name: String = "",
            displayName: String? = null
        ): QuadraticPolynomial {
            return QuadraticPolynomial(
                monomials = polynomial.monomials.map { QuadraticMonomial(it) },
                constant = polynomial.constant,
                name = name.ifEmpty { polynomial.name },
                displayName = displayName ?: polynomial.displayName
            )
        }

        operator fun invoke(
            polynomial: AbstractQuadraticPolynomial<*>,
            name: String = "",
            displayName: String? = null
        ): QuadraticPolynomial {
            return QuadraticPolynomial(
                monomials = polynomial.monomials.map { it.copy() },
                constant = polynomial.constant,
                name = name.ifEmpty { polynomial.name },
                displayName = displayName ?: polynomial.displayName
            )
        }

        operator fun invoke(
            constant: Int,
            name: String = "",
            displayName: String? = null
        ): QuadraticPolynomial {
            return QuadraticPolynomial(
                monomials = emptyList(),
                constant = Flt64(constant),
                name = name,
                displayName = displayName
            )
        }

        operator fun invoke(
            constant: Double,
            name: String = "",
            displayName: String? = null
        ): QuadraticPolynomial {
            return QuadraticPolynomial(
                monomials = emptyList(),
                constant = Flt64(constant),
                name = name,
                displayName = displayName
            )
        }

        operator fun invoke(
            constant: Boolean,
            name: String = "",
            displayName: String? = null
        ): QuadraticPolynomial {
            return QuadraticPolynomial(
                monomials = emptyList(),
                constant = if (constant) Flt64.one else Flt64.zero,
                name = name,
                displayName = displayName
            )
        }

        operator fun invoke(
            constant: Trivalent,
            name: String = "",
            displayName: String? = null
        ): QuadraticPolynomial {
            return QuadraticPolynomial(
                monomials = emptyList(),
                constant = constant.value.toFlt64(),
                name = name,
                displayName = displayName
            )
        }

        operator fun invoke(
            constant: BalancedTrivalent,
            name: String = "",
            displayName: String? = null
        ): QuadraticPolynomial {
            return QuadraticPolynomial(
                monomials = emptyList(),
                constant = constant.value.toFlt64(),
                name = name,
                displayName = displayName
            )
        }

        operator fun <T : RealNumber<T>> invoke(
            constant: T,
            name: String = "",
            displayName: String? = null
        ): QuadraticPolynomial {
            return QuadraticPolynomial(
                monomials = emptyList(),
                constant = constant.toFlt64(),
                name = name,
                displayName = displayName
            )
        }
    }

    override val discrete by lazy {
        monomials.all { it.discrete } && constant.round() eq constant
    }

    override fun copy(): QuadraticPolynomial {
        return QuadraticPolynomial(
            monomials = monomials.map { it.copy() },
            constant = constant
        )
    }

    override fun copy(name: String, displayName: String?): QuadraticPolynomial {
        return QuadraticPolynomial(
            monomials = monomials.map { it.copy() },
            constant = constant,
            name = name,
            displayName = displayName
        )
    }

    override fun plus(rhs: AbstractVariableItem<*, *>): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(QuadraticMonomial(rhs))
        return QuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("plusVariables")
    override fun plus(rhs: Iterable<AbstractVariableItem<*, *>>): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.map { QuadraticMonomial(it) })
        return QuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    override fun plus(rhs: LinearIntermediateSymbol): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(QuadraticMonomial(rhs))
        return QuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("plusLinearSymbols")
    override fun plus(rhs: Iterable<LinearIntermediateSymbol>): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.map { QuadraticMonomial(it) })
        return QuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    override fun plus(rhs: QuadraticIntermediateSymbol): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(QuadraticMonomial(rhs))
        return QuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("plusQuadraticSymbols")
    override fun plus(rhs: Iterable<QuadraticIntermediateSymbol>): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.map { QuadraticMonomial(it) })
        return QuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    override fun plus(rhs: LinearMonomial): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(QuadraticMonomial(rhs))
        return QuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    override fun plus(rhs: QuadraticMonomial): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(rhs.copy())
        return QuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    override fun plus(rhs: AbstractLinearPolynomial<*>): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.monomials.map { QuadraticMonomial(it) })
        return QuadraticPolynomial(
            monomials = newMonomials,
            constant = constant + rhs.constant
        )
    }

    override fun plus(rhs: Polynomial<*, QuadraticMonomial>): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.monomials.map { it.copy() })
        return QuadraticPolynomial(
            monomials = newMonomials,
            constant = constant + rhs.constant
        )
    }

    override fun plus(rhs: Flt64): QuadraticPolynomial {
        return QuadraticPolynomial(
            monomials = monomials.map { it.copy() },
            constant = constant + rhs
        )
    }

    override fun minus(rhs: AbstractVariableItem<*, *>): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(QuadraticMonomial(-Flt64.one, rhs))
        return QuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("minusVariables")
    override fun minus(rhs: Iterable<AbstractVariableItem<*, *>>): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.map { QuadraticMonomial(-Flt64.one, it) })
        return QuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    override fun minus(rhs: LinearIntermediateSymbol): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(QuadraticMonomial(-Flt64.one, rhs))
        return QuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("minusLinearSymbols")
    override fun minus(rhs: Iterable<LinearIntermediateSymbol>): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.map { QuadraticMonomial(-Flt64.one, it) })
        return QuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    override fun minus(rhs: QuadraticIntermediateSymbol): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(QuadraticMonomial(-Flt64.one, rhs))
        return QuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("minusQuadraticSymbols")
    override fun minus(rhs: Iterable<QuadraticIntermediateSymbol>): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.map { QuadraticMonomial(-Flt64.one, it) })
        return QuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    override fun minus(rhs: LinearMonomial): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(QuadraticMonomial(-rhs))
        return QuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    override fun minus(rhs: QuadraticMonomial): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(-rhs)
        return QuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    override fun minus(rhs: AbstractLinearPolynomial<*>): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.monomials.map { QuadraticMonomial(-it) })
        return QuadraticPolynomial(
            monomials = newMonomials,
            constant = constant - rhs.constant
        )
    }

    override fun minus(rhs: Polynomial<*, QuadraticMonomial>): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.monomials.map { -it })
        return QuadraticPolynomial(
            monomials = newMonomials,
            constant = constant - rhs.constant
        )
    }

    override fun minus(rhs: Flt64): QuadraticPolynomial {
        return QuadraticPolynomial(
            monomials = monomials.map { it.copy() },
            constant = constant - rhs
        )
    }

    @Throws(IllegalArgumentException::class)
    override fun times(rhs: AbstractVariableItem<*, *>): QuadraticPolynomial {
        if (this.category == Quadratic) {
            throw IllegalArgumentException("Invalid argument of QuadraticPolynomial.times: over quadratic.")
        }

        val newMonomials = monomials.map { it * rhs }.toMutableList()
        newMonomials.add(QuadraticMonomial(constant, rhs))
        return QuadraticPolynomial(monomials = newMonomials)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("timesVariables")
    @Throws(IllegalArgumentException::class)
    override fun times(rhs: Iterable<AbstractVariableItem<*, *>>): QuadraticPolynomial {
        if (this.category == Quadratic) {
            throw IllegalArgumentException("Invalid argument of QuadraticPolynomial.times: over quadratic.")
        }

        val newMonomials = monomials.flatMap { monomial -> rhs.map { monomial * it } }.toMutableList()
        newMonomials.addAll(rhs.map { QuadraticMonomial(constant, it) })
        return QuadraticPolynomial(monomials = newMonomials)
    }

    @Throws(IllegalArgumentException::class)
    override fun times(rhs: LinearIntermediateSymbol): QuadraticPolynomial {
        if (this.category == Quadratic) {
            throw IllegalArgumentException("Invalid argument of QuadraticPolynomial.times: over quadratic.")
        }

        val newMonomials = monomials.map { it * rhs }.toMutableList()
        newMonomials.add(QuadraticMonomial(constant, rhs))
        return QuadraticPolynomial(monomials = newMonomials)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("timesLinearSymbols")
    @Throws(IllegalArgumentException::class)
    override fun times(rhs: Iterable<LinearIntermediateSymbol>): QuadraticPolynomial {
        if (this.category == Quadratic) {
            throw IllegalArgumentException("Invalid argument of QuadraticPolynomial.times: over quadratic.")
        }

        val newMonomials = monomials.flatMap { monomial -> rhs.map { monomial * it } }.toMutableList()
        newMonomials.addAll(rhs.map { QuadraticMonomial(constant, it) })
        return QuadraticPolynomial(monomials = newMonomials)
    }

    @Throws(IllegalArgumentException::class)
    override fun times(rhs: QuadraticIntermediateSymbol): QuadraticPolynomial {
        if (this.category == Quadratic || rhs.category == Quadratic) {
            throw IllegalArgumentException("Invalid argument of QuadraticPolynomial.times: over quadratic.")
        }

        val newMonomials = monomials.map { it * rhs }.toMutableList()
        newMonomials.add(QuadraticMonomial(constant, rhs))
        return QuadraticPolynomial(monomials = newMonomials)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("timesQuadraticSymbols")
    @Throws(IllegalArgumentException::class)
    override fun times(rhs: Iterable<QuadraticIntermediateSymbol>): QuadraticPolynomial {
        if (this.category == Quadratic || rhs.any { it.category == Quadratic }) {
            throw IllegalArgumentException("Invalid argument of QuadraticPolynomial.times: over quadratic.")
        }

        val newMonomials = monomials.flatMap { monomial -> rhs.map { monomial * it } }.toMutableList()
        newMonomials.addAll(rhs.map { QuadraticMonomial(constant, it) })
        return QuadraticPolynomial(monomials = newMonomials)
    }

    @Throws(IllegalArgumentException::class)
    override fun times(rhs: LinearMonomial): QuadraticPolynomial {
        if (this.category == Quadratic) {
            throw IllegalArgumentException("Invalid argument of QuadraticPolynomial.times: over quadratic.")
        }

        val newMonomials = monomials.map { it * rhs }.toMutableList()
        newMonomials.add(QuadraticMonomial(constant * rhs))
        return QuadraticPolynomial(monomials = newMonomials)
    }

    @Throws(IllegalArgumentException::class)
    override fun times(rhs: QuadraticMonomial): QuadraticPolynomial {
        if (this.category == Quadratic || rhs.category == Quadratic) {
            throw IllegalArgumentException("Invalid argument of QuadraticPolynomial.times: over quadratic.")
        }

        val newMonomials = monomials.map { it * rhs }.toMutableList()
        newMonomials.add(constant * rhs)
        return QuadraticPolynomial(monomials = newMonomials)
    }

    @Throws(IllegalArgumentException::class)
    override fun times(rhs: AbstractLinearPolynomial<*>): QuadraticPolynomial {
        if (this.category == Quadratic) {
            throw IllegalArgumentException("Invalid argument of QuadraticPolynomial.times: over quadratic.")
        }

        val newMonomials = monomials.flatMap { lhs -> rhs.monomials.map { monomial -> lhs * monomial } }.toMutableList()
        newMonomials.addAll(monomials.map { rhs.constant * it })
        newMonomials.addAll(rhs.monomials.map { QuadraticMonomial(constant * it) })
        return QuadraticPolynomial(
            monomials = newMonomials,
            constant = this.constant * rhs.constant
        )
    }

    @Throws(IllegalArgumentException::class)
    override fun times(rhs: AbstractQuadraticPolynomial<*>): QuadraticPolynomial {
        if (this.category == Quadratic || rhs.category == Quadratic) {
            throw IllegalArgumentException("Invalid argument of QuadraticPolynomial.times: over quadratic.")
        }

        val newMonomials = monomials.flatMap { lhs -> rhs.monomials.map { monomial -> lhs * monomial } }.toMutableList()
        newMonomials.addAll(monomials.map { rhs.constant * it })
        newMonomials.addAll(rhs.monomials.map { constant * it })
        return QuadraticPolynomial(
            monomials = newMonomials,
            constant = this.constant * rhs.constant
        )
    }

    override fun times(rhs: Flt64): QuadraticPolynomial {
        return QuadraticPolynomial(
            monomials = monomials.map { rhs * it },
            constant = constant * rhs
        )
    }

    override fun div(rhs: Flt64): QuadraticPolynomial {
        return QuadraticPolynomial(
            monomials = monomials.map { it / rhs },
            constant = constant / rhs
        )
    }

    override fun toQuadraticPolynomial(): QuadraticPolynomial {
        return this
    }
}

class MutableQuadraticPolynomial(
    override var monomials: MutableList<QuadraticMonomial> = ArrayList(),
    override var constant: Flt64 = Flt64.zero,
    override var name: String = "",
    override var displayName: String? = null
) : AbstractQuadraticPolynomial<MutableQuadraticPolynomial>(),
    MutablePolynomial<MutableQuadraticPolynomial, QuadraticMonomial> {
    companion object {
        operator fun invoke(
            item: AbstractVariableItem<*, *>,
            name: String = "",
            displayName: String? = null
        ): MutableQuadraticPolynomial {
            return MutableQuadraticPolynomial(
                monomials = mutableListOf(QuadraticMonomial(item)),
                name = name.ifEmpty { item.name },
                displayName = displayName
            )
        }

        operator fun invoke(
            symbol: LinearIntermediateSymbol,
            name: String = "",
            displayName: String? = null
        ): MutableQuadraticPolynomial {
            return MutableQuadraticPolynomial(
                monomials = mutableListOf(QuadraticMonomial(symbol)),
                name = name.ifEmpty { symbol.name },
                displayName = displayName ?: symbol.displayName
            )
        }

        operator fun invoke(
            symbol: QuadraticIntermediateSymbol,
            name: String = "",
            displayName: String? = null
        ): MutableQuadraticPolynomial {
            return MutableQuadraticPolynomial(
                monomials = mutableListOf(QuadraticMonomial(symbol)),
                name = name.ifEmpty { symbol.name },
                displayName = displayName ?: symbol.displayName
            )
        }

        operator fun invoke(
            monomial: LinearMonomial,
            name: String = "",
            displayName: String? = null
        ): MutableQuadraticPolynomial {
            return MutableQuadraticPolynomial(
                monomials = mutableListOf(QuadraticMonomial(monomial)),
                name = name.ifEmpty { monomial.name },
                displayName = displayName ?: monomial.displayName
            )
        }

        operator fun invoke(
            monomial: QuadraticMonomial,
            name: String = "",
            displayName: String? = null
        ): MutableQuadraticPolynomial {
            return MutableQuadraticPolynomial(
                monomials = mutableListOf(monomial),
                name = name.ifEmpty { monomial.name },
                displayName = displayName ?: monomial.displayName
            )
        }

        operator fun invoke(
            polynomial: AbstractQuadraticPolynomial<*>,
            name: String = "",
            displayName: String? = null
        ): MutableQuadraticPolynomial {
            return MutableQuadraticPolynomial(
                monomials = polynomial.monomials.map { it.copy() }.toMutableList(),
                constant = polynomial.constant,
                name = name.ifEmpty { polynomial.name },
                displayName = displayName ?: polynomial.displayName
            )
        }

        operator fun invoke(
            constant: Int,
            name: String = "",
            displayName: String? = null
        ): MutableQuadraticPolynomial {
            return MutableQuadraticPolynomial(
                monomials = mutableListOf(),
                constant = Flt64(constant),
                name = name,
                displayName = displayName
            )
        }

        operator fun invoke(
            constant: Double,
            name: String = "",
            displayName: String? = null
        ): MutableQuadraticPolynomial {
            return MutableQuadraticPolynomial(
                monomials = mutableListOf(),
                constant = Flt64(constant),
                name = name,
                displayName = displayName
            )
        }

        operator fun invoke(
            constant: Boolean,
            name: String = "",
            displayName: String? = null
        ): MutableQuadraticPolynomial {
            return MutableQuadraticPolynomial(
                monomials = mutableListOf(),
                constant = if (constant) Flt64.one else Flt64.zero,
                name = name,
                displayName = displayName
            )
        }

        operator fun invoke(
            constant: Trivalent,
            name: String = "",
            displayName: String? = null
        ): MutableQuadraticPolynomial {
            return MutableQuadraticPolynomial(
                monomials = mutableListOf(),
                constant = constant.value.toFlt64(),
                name = name,
                displayName = displayName
            )
        }

        operator fun invoke(
            constant: BalancedTrivalent,
            name: String = "",
            displayName: String? = null
        ): MutableQuadraticPolynomial {
            return MutableQuadraticPolynomial(
                monomials = mutableListOf(),
                constant = constant.value.toFlt64(),
                name = name,
                displayName = displayName
            )
        }

        operator fun <T : RealNumber<T>> invoke(
            constant: T,
            name: String = "",
            displayName: String? = null
        ): MutableQuadraticPolynomial {
            return MutableQuadraticPolynomial(
                monomials = mutableListOf(),
                constant = constant.toFlt64(),
                name = name,
                displayName = displayName
            )
        }
    }

    override fun copy(): MutableQuadraticPolynomial {
        return MutableQuadraticPolynomial(
            monomials = monomials.map { it.copy() }.toMutableList(),
            constant = constant
        )
    }

    override fun copy(name: String, displayName: String?): MutableQuadraticPolynomial {
        return MutableQuadraticPolynomial(
            monomials = monomials.map { it.copy() }.toMutableList(),
            constant = constant,
            name = name,
            displayName = displayName
        )
    }

    override fun plus(rhs: AbstractVariableItem<*, *>): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(QuadraticMonomial(rhs))
        return MutableQuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("plusVariables")
    override fun plus(rhs: Iterable<AbstractVariableItem<*, *>>): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.map { QuadraticMonomial(it) })
        return MutableQuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    override fun plus(rhs: LinearIntermediateSymbol): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(QuadraticMonomial(rhs))
        return MutableQuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("plusLinearSymbols")
    override fun plus(rhs: Iterable<LinearIntermediateSymbol>): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.map { QuadraticMonomial(it) })
        return MutableQuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    override fun plus(rhs: QuadraticIntermediateSymbol): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(QuadraticMonomial(rhs))
        return MutableQuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("plusQuadraticSymbols")
    override fun plus(rhs: Iterable<QuadraticIntermediateSymbol>): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.map { QuadraticMonomial(it) })
        return MutableQuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    override fun plus(rhs: LinearMonomial): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(QuadraticMonomial(rhs))
        return MutableQuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    override fun plus(rhs: QuadraticMonomial): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(rhs)
        return MutableQuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    override fun plus(rhs: AbstractLinearPolynomial<*>): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.monomials.map { QuadraticMonomial(it) })
        return MutableQuadraticPolynomial(
            monomials = newMonomials,
            constant = constant + rhs.constant
        )
    }

    override fun plus(rhs: Polynomial<*, QuadraticMonomial>): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.monomials.map { it.copy() })
        return MutableQuadraticPolynomial(
            monomials = newMonomials,
            constant = constant + rhs.constant
        )
    }

    override fun plus(rhs: Flt64): MutableQuadraticPolynomial {
        return MutableQuadraticPolynomial(
            monomials = monomials.map { it.copy() }.toMutableList(),
            constant = constant + rhs
        )
    }

    override fun plusAssign(rhs: AbstractVariableItem<*, *>) {
        monomials.add(QuadraticMonomial(rhs))
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("plusAssignVariables")
    override fun plusAssign(rhs: Iterable<AbstractVariableItem<*, *>>) {
        monomials.addAll(rhs.map { QuadraticMonomial(it) })
    }

    operator fun plusAssign(rhs: LinearIntermediateSymbol) {
        monomials.add(QuadraticMonomial(rhs))
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("plusAssignLinearSymbols")
    operator fun plusAssign(rhs: Iterable<LinearIntermediateSymbol>) {
        monomials.addAll(rhs.map { QuadraticMonomial(it) })
    }

    operator fun plusAssign(rhs: QuadraticIntermediateSymbol) {
        monomials.add(QuadraticMonomial(rhs))
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("plusAssignQuadraticSymbols")
    operator fun plusAssign(rhs: Iterable<QuadraticIntermediateSymbol>) {
        monomials.addAll(rhs.map { QuadraticMonomial(it) })
    }

    operator fun plusAssign(rhs: LinearMonomial) {
        monomials.add(QuadraticMonomial(rhs))
    }

    override fun plusAssign(rhs: QuadraticMonomial) {
        monomials.add(rhs)
    }

    operator fun plusAssign(rhs: AbstractLinearPolynomial<*>) {
        monomials.addAll(rhs.monomials.map { QuadraticMonomial(it) })
        constant += rhs.constant
    }

    override fun plusAssign(rhs: Polynomial<*, QuadraticMonomial>) {
        monomials.addAll(rhs.monomials.map { it.copy() })
        constant += rhs.constant
    }

    override fun plusAssign(rhs: Flt64) {
        constant += rhs
    }

    override fun minus(rhs: AbstractVariableItem<*, *>): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(QuadraticMonomial(-Flt64.one, rhs))
        return MutableQuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("minusVariables")
    override fun minus(rhs: Iterable<AbstractVariableItem<*, *>>): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.map { QuadraticMonomial(-Flt64.one, it) })
        return MutableQuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    override fun minus(rhs: LinearIntermediateSymbol): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(QuadraticMonomial(-Flt64.one, rhs))
        return MutableQuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("minusLinearSymbols")
    override fun minus(rhs: Iterable<LinearIntermediateSymbol>): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.map { QuadraticMonomial(-Flt64.one, it) })
        return MutableQuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    override fun minus(rhs: QuadraticIntermediateSymbol): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(QuadraticMonomial(-Flt64.one, rhs))
        return MutableQuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("minusQuadraticSymbols")
    override fun minus(rhs: Iterable<QuadraticIntermediateSymbol>): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.map { QuadraticMonomial(-Flt64.one, it) })
        return MutableQuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    override fun minus(rhs: LinearMonomial): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(QuadraticMonomial(-rhs))
        return MutableQuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    override fun minus(rhs: QuadraticMonomial): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(-rhs)
        return MutableQuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    override fun minus(rhs: AbstractLinearPolynomial<*>): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.monomials.map { QuadraticMonomial(-it) })
        return MutableQuadraticPolynomial(
            monomials = newMonomials,
            constant = constant - rhs.constant
        )
    }

    override fun minus(rhs: Polynomial<*, QuadraticMonomial>): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.monomials.map { -it })
        return MutableQuadraticPolynomial(
            monomials = newMonomials,
            constant = constant - rhs.constant
        )
    }

    override fun minus(rhs: Flt64): MutableQuadraticPolynomial {
        return MutableQuadraticPolynomial(
            monomials = monomials.map { it.copy() }.toMutableList(),
            constant = constant - rhs
        )
    }

    override fun minusAssign(rhs: AbstractVariableItem<*, *>) {
        monomials.add(QuadraticMonomial(-Flt64.one, rhs))
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("minusAssignVariables")
    override fun minusAssign(rhs: Iterable<AbstractVariableItem<*, *>>) {
        monomials.addAll(rhs.map { QuadraticMonomial(-Flt64.one, it) })
    }

    operator fun minusAssign(rhs: LinearIntermediateSymbol) {
        monomials.add(QuadraticMonomial(-Flt64.one, rhs))
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("minusAssignLinearSymbols")
    operator fun minusAssign(rhs: Iterable<LinearIntermediateSymbol>) {
        monomials.addAll(rhs.map { QuadraticMonomial(-Flt64.one, it) })
    }

    operator fun minusAssign(rhs: QuadraticIntermediateSymbol) {
        monomials.add(QuadraticMonomial(-Flt64.one, rhs))
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("minusAssignQuadraticSymbols")
    operator fun minusAssign(rhs: Iterable<QuadraticIntermediateSymbol>) {
        monomials.addAll(rhs.map { QuadraticMonomial(-Flt64.one, it) })
    }

    operator fun minusAssign(rhs: LinearMonomial) {
        monomials.add(QuadraticMonomial(-rhs))
    }

    override fun minusAssign(rhs: QuadraticMonomial) {
        monomials.add(-rhs)
    }

    operator fun minusAssign(rhs: AbstractLinearPolynomial<*>) {
        monomials.addAll(rhs.monomials.map { QuadraticMonomial(-it) })
        constant -= rhs.constant
    }

    override fun minusAssign(rhs: Polynomial<*, QuadraticMonomial>) {
        monomials.addAll(rhs.monomials.map { -it })
        constant -= rhs.constant
    }

    override fun minusAssign(rhs: Flt64) {
        constant -= rhs
    }

    @Throws(IllegalArgumentException::class)
    override fun times(rhs: AbstractVariableItem<*, *>): MutableQuadraticPolynomial {
        if (this.category == Quadratic) {
            throw IllegalArgumentException("Invalid argument of MutableQuadraticPolynomial.times: over quadratic.")
        }

        val newMonomials = monomials.map { it * rhs }.toMutableList()
        newMonomials.add(QuadraticMonomial(constant, rhs))
        return MutableQuadraticPolynomial(monomials = newMonomials)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("timesVariables")
    @Throws(IllegalArgumentException::class)
    override fun times(rhs: Iterable<AbstractVariableItem<*, *>>): MutableQuadraticPolynomial {
        if (this.category == Quadratic) {
            throw IllegalArgumentException("Invalid argument of MutableQuadraticPolynomial.times: over quadratic.")
        }

        val newMonomials = monomials.flatMap { monomial -> rhs.map { monomial * it } }.toMutableList()
        newMonomials.addAll(rhs.map { QuadraticMonomial(constant, it) })
        return MutableQuadraticPolynomial(monomials = newMonomials)
    }

    @Throws(IllegalArgumentException::class)
    override fun times(rhs: LinearIntermediateSymbol): MutableQuadraticPolynomial {
        if (this.category == Quadratic) {
            throw IllegalArgumentException("Invalid argument of MutableQuadraticPolynomial.times: over quadratic.")
        }

        val newMonomials = monomials.map { it * rhs }.toMutableList()
        newMonomials.add(QuadraticMonomial(constant, rhs))
        return MutableQuadraticPolynomial(monomials = newMonomials)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("timesLinearSymbols")
    @Throws(IllegalArgumentException::class)
    override fun times(rhs: Iterable<LinearIntermediateSymbol>): MutableQuadraticPolynomial {
        if (this.category == Quadratic) {
            throw IllegalArgumentException("Invalid argument of MutableQuadraticPolynomial.times: over quadratic.")
        }

        val newMonomials = monomials.flatMap { monomial -> rhs.map { monomial * it } }.toMutableList()
        newMonomials.addAll(rhs.map { QuadraticMonomial(constant, it) })
        return MutableQuadraticPolynomial(monomials = newMonomials)
    }

    @Throws(IllegalArgumentException::class)
    override fun times(rhs: QuadraticIntermediateSymbol): MutableQuadraticPolynomial {
        if (this.category == Quadratic || rhs.category == Quadratic) {
            throw IllegalArgumentException("Invalid argument of MutableQuadraticPolynomial.times: over quadratic.")
        }

        val newMonomials = monomials.map { it * rhs }.toMutableList()
        newMonomials.add(QuadraticMonomial(constant, rhs))
        return MutableQuadraticPolynomial(monomials = newMonomials)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("timesQuadraticSymbols")
    @Throws(IllegalArgumentException::class)
    override fun times(rhs: Iterable<QuadraticIntermediateSymbol>): MutableQuadraticPolynomial {
        if (this.category == Quadratic || rhs.any { it.category == Quadratic }) {
            throw IllegalArgumentException("Invalid argument of MutableQuadraticPolynomial.times: over quadratic.")
        }

        val newMonomials = monomials.flatMap { monomial -> rhs.map { monomial * it } }.toMutableList()
        newMonomials.addAll(rhs.map { QuadraticMonomial(constant, it) })
        return MutableQuadraticPolynomial(monomials = newMonomials)
    }

    @Throws(IllegalArgumentException::class)
    override fun times(rhs: LinearMonomial): MutableQuadraticPolynomial {
        if (this.category == Quadratic) {
            throw IllegalArgumentException("Invalid argument of MutableQuadraticPolynomial.times: over quadratic.")
        }

        val newMonomials = monomials.map { it * rhs }.toMutableList()
        newMonomials.add(QuadraticMonomial(constant * rhs))
        return MutableQuadraticPolynomial(monomials = newMonomials)
    }

    @Throws(IllegalArgumentException::class)
    override fun times(rhs: QuadraticMonomial): MutableQuadraticPolynomial {
        if (this.category == Quadratic || rhs.category == Quadratic) {
            throw IllegalArgumentException("Invalid argument of MutableQuadraticPolynomial.times: over quadratic.")
        }

        val newMonomials = monomials.map { it * rhs }.toMutableList()
        newMonomials.add(constant * rhs)
        return MutableQuadraticPolynomial(monomials = newMonomials)
    }

    @Throws(IllegalArgumentException::class)
    override fun times(rhs: AbstractLinearPolynomial<*>): MutableQuadraticPolynomial {
        if (this.category == Quadratic) {
            throw IllegalArgumentException("Invalid argument of MutableQuadraticPolynomial.times: over quadratic.")
        }

        val newMonomials = monomials.flatMap { monomial -> rhs.monomials.map { monomial * it } }.toMutableList()
        newMonomials.addAll(monomials.map { rhs.constant * it })
        newMonomials.addAll(rhs.monomials.map { QuadraticMonomial(constant * it) })
        return MutableQuadraticPolynomial(
            monomials = newMonomials,
            constant = constant * rhs.constant
        )
    }

    @Throws(IllegalArgumentException::class)
    override fun times(rhs: AbstractQuadraticPolynomial<*>): MutableQuadraticPolynomial {
        if (this.category == Quadratic || rhs.category == Quadratic) {
            throw IllegalArgumentException("Invalid argument of MutableQuadraticPolynomial.times: over quadratic.")
        }

        val newMonomials = monomials.flatMap { monomial -> rhs.monomials.map { monomial * it } }.toMutableList()
        newMonomials.addAll(monomials.map { rhs.constant * it })
        newMonomials.addAll(rhs.monomials.map { constant * it })
        return MutableQuadraticPolynomial(
            monomials = newMonomials,
            constant = constant * rhs.constant
        )
    }

    override fun times(rhs: Flt64): MutableQuadraticPolynomial {
        return MutableQuadraticPolynomial(
            monomials = monomials.map { it * rhs }.toMutableList(),
            constant = constant * rhs
        )
    }

    @Throws(IllegalArgumentException::class)
    operator fun timesAssign(rhs: AbstractVariableItem<*, *>) {
        if (this.category == Quadratic) {
            throw IllegalArgumentException("Invalid argument of MutableQuadraticPolynomial.timesAssign: over quadratic.")
        }

        monomials = monomials.map { it * rhs }.toMutableList()
        monomials.add(QuadraticMonomial(constant, rhs))
        constant = Flt64.zero
    }

    @Throws(IllegalArgumentException::class)
    operator fun timesAssign(rhs: LinearIntermediateSymbol) {
        if (this.category == Quadratic) {
            throw IllegalArgumentException("Invalid argument of MutableQuadraticPolynomial.timesAssign: over quadratic.")
        }

        monomials = monomials.map { it * rhs }.toMutableList()
        monomials.add(QuadraticMonomial(constant, rhs))
        constant = Flt64.zero
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("timesAssignLinearSymbols")
    @Throws(IllegalArgumentException::class)
    operator fun timesAssign(rhs: Iterable<LinearIntermediateSymbol>) {
        if (this.category == Quadratic) {
            throw IllegalArgumentException("Invalid argument of MutableQuadraticPolynomial.timesAssign: over quadratic.")
        }

        monomials = monomials.flatMap { monomial -> rhs.map { monomial * it } }.toMutableList()
        monomials.addAll(rhs.map { QuadraticMonomial(constant, it) })
        constant = Flt64.zero
    }

    @Throws(IllegalArgumentException::class)
    operator fun timesAssign(rhs: QuadraticIntermediateSymbol) {
        if (this.category == Quadratic || rhs.category == Quadratic) {
            throw IllegalArgumentException("Invalid argument of MutableQuadraticPolynomial.timesAssign: over quadratic.")
        }

        monomials = monomials.map { it * rhs }.toMutableList()
        monomials.add(QuadraticMonomial(constant, rhs))
        constant = Flt64.zero
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("timesAssignQuadraticSymbols")
    @Throws(IllegalArgumentException::class)
    operator fun timesAssign(rhs: Iterable<QuadraticIntermediateSymbol>) {
        if (this.category == Quadratic || rhs.any { it.category == Quadratic }) {
            throw IllegalArgumentException("Invalid argument of MutableQuadraticPolynomial.timesAssign: over quadratic.")
        }

        monomials = monomials.flatMap { monomial -> rhs.map { monomial * it } }.toMutableList()
        monomials.addAll(rhs.map { QuadraticMonomial(constant, it) })
        constant = Flt64.zero
    }

    @Throws(IllegalArgumentException::class)
    operator fun timesAssign(rhs: LinearMonomial) {
        if (this.category == Quadratic) {
            throw IllegalArgumentException("Invalid argument of MutableQuadraticPolynomial.timesAssign: over quadratic.")
        }

        monomials = monomials.map { it * rhs }.toMutableList()
        monomials.add(QuadraticMonomial(constant * rhs))
        constant = Flt64.zero
    }

    @Throws(IllegalArgumentException::class)
    operator fun timesAssign(rhs: QuadraticMonomial) {
        if (this.category == Quadratic || rhs.category == Quadratic) {
            throw IllegalArgumentException("Invalid argument of MutableQuadraticPolynomial.timesAssign: over quadratic.")
        }

        monomials = monomials.map { it * rhs }.toMutableList()
        monomials.add(constant * rhs)
        constant = Flt64.zero
    }

    @Throws(IllegalArgumentException::class)
    operator fun timesAssign(rhs: AbstractLinearPolynomial<*>) {
        if (this.category == Quadratic) {
            throw IllegalArgumentException("Invalid argument of MutableQuadraticPolynomial.timesAssign: over quadratic.")
        }

        monomials = monomials.flatMap { monomial -> rhs.monomials.map { monomial * it } }.toMutableList()
        monomials.addAll(monomials.map { rhs.constant * it })
        monomials.addAll(rhs.monomials.map { QuadraticMonomial(constant * it) })
        constant *= rhs.constant
    }

    @Throws(IllegalArgumentException::class)
    operator fun timesAssign(rhs: AbstractQuadraticPolynomial<*>) {
        if (this.category == Quadratic || rhs.category == Quadratic) {
            throw IllegalArgumentException("Invalid argument of MutableQuadraticPolynomial.timesAssign: over quadratic.")
        }

        monomials = monomials.flatMap { monomial -> rhs.monomials.map { monomial * it } }.toMutableList()
        monomials.addAll(monomials.map { rhs.constant * it })
        monomials.addAll(rhs.monomials.map { constant * it })
        constant *= rhs.constant
    }

    override fun timesAssign(rhs: Flt64) {
        monomials = monomials.map { it * rhs }.toMutableList()
        constant *= rhs
    }

    override fun div(rhs: Flt64): MutableQuadraticPolynomial {
        return MutableQuadraticPolynomial(
            monomials = monomials.map { it.copy() }.toMutableList(),
            constant = constant / rhs
        )
    }

    override fun divAssign(rhs: Flt64) {
        monomials = monomials.map { it / rhs }.toMutableList()
        constant /= rhs
    }

    override fun toQuadraticPolynomial(): QuadraticPolynomial {
        return QuadraticPolynomial(this)
    }
}

// quantity polynomial conversion

fun Quantity<AbstractQuadraticPolynomial<*>>.to(targetUnit: PhysicalUnit): Quantity<QuadraticPolynomial>? {
    return unit.to(targetUnit)?.let {
        Quantity(it.value * this.value, targetUnit)
    }
}

// unary minus quantity polynomial

operator fun Quantity<AbstractQuadraticPolynomial<*>>.unaryMinus(): Quantity<QuadraticPolynomial> {
    return Quantity(QuadraticPolynomial(-this.value), this.unit)
}

// quantity polynomial plus/minus/times/div assign

@JvmName("quantityPolynomialPlusAssignQuantityVariable")
fun Quantity<MutableQuadraticPolynomial>.plusAssign(rhs: Quantity<AbstractVariableItem<*, *>>) {
    value.plusAssign(rhs.to(this.unit)!!.value)
}

@JvmName("quantityPolynomialPlusAssignQuantityVariables")
fun Quantity<MutableQuadraticPolynomial>.plusAssign(rhs: Iterable<Quantity<AbstractVariableItem<*, *>>>) {
    for (item in rhs) {
        value.plusAssign(item.to(this.unit)!!.value)
    }
}

@JvmName("quantityPolynomialPlusAssignQuantitySymbol")
fun Quantity<MutableQuadraticPolynomial>.plusAssign(rhs: Quantity<LinearIntermediateSymbol>) {
    value.plusAssign(rhs.to(this.unit)!!.value)
}

@JvmName("quantityPolynomialPlusAssignQuantitySymbols")
fun Quantity<MutableQuadraticPolynomial>.plusAssign(rhs: Iterable<Quantity<LinearIntermediateSymbol>>) {
    for (item in rhs) {
        value.plusAssign(item.to(this.unit)!!.value)
    }
}

@JvmName("quantityPolynomialPlusAssignQuantityMonomial")
fun Quantity<MutableQuadraticPolynomial>.plusAssign(rhs: Quantity<LinearMonomial>) {
    value.plusAssign(rhs.to(this.unit)!!.value)
}

@JvmName("quantityPolynomialPlusAssignQuantityPolynomial")
fun Quantity<MutableQuadraticPolynomial>.plusAssign(rhs: Quantity<AbstractLinearPolynomial<*>>) {
    value.plusAssign(rhs.to(this.unit)!!.value)
}

@JvmName("quantityPolynomialPlusAssignQuantity")
fun <V : RealNumber<V>> Quantity<MutableQuadraticPolynomial>.plusAssign(rhs: Quantity<V>) {
    value.plusAssign(rhs.toFlt64().to(this.unit)!!.value)
}

@JvmName("quantityPolynomialMinusAssignQuantityVariable")
fun Quantity<MutableQuadraticPolynomial>.minusAssign(rhs: Quantity<AbstractVariableItem<*, *>>) {
    value.minusAssign(rhs.to(this.unit)!!.value)
}

@JvmName("quantityPolynomialMinusAssignQuantityVariables")
fun Quantity<MutableQuadraticPolynomial>.minusAssign(rhs: Iterable<Quantity<AbstractVariableItem<*, *>>>) {
    for (item in rhs) {
        value.minusAssign(item.to(this.unit)!!.value)
    }
}

@JvmName("quantityPolynomialMinusAssignQuantitySymbol")
fun Quantity<MutableQuadraticPolynomial>.minusAssign(rhs: Quantity<LinearIntermediateSymbol>) {
    value.minusAssign(rhs.to(this.unit)!!.value)
}

@JvmName("quantityPolynomialMinusAssignQuantitySymbols")
fun Quantity<MutableQuadraticPolynomial>.minusAssign(rhs: Iterable<Quantity<LinearIntermediateSymbol>>) {
    for (item in rhs) {
        value.minusAssign(item.to(this.unit)!!.value)
    }
}

@JvmName("quantityPolynomialMinusAssignQuantityMonomial")
fun Quantity<MutableQuadraticPolynomial>.minusAssign(rhs: Quantity<LinearMonomial>) {
    value.minusAssign(rhs.to(this.unit)!!.value)
}

@JvmName("quantityPolynomialMinusAssignQuantityPolynomial")
fun Quantity<MutableQuadraticPolynomial>.minusAssign(rhs: Quantity<AbstractLinearPolynomial<*>>) {
    value.minusAssign(rhs.to(this.unit)!!.value)
}

@JvmName("quantityPolynomialMinusAssignQuantity")
fun <V : RealNumber<V>> Quantity<MutableQuadraticPolynomial>.minusAssign(rhs: Quantity<V>) {
    value.minusAssign(rhs.toFlt64().to(this.unit)!!.value)
}

@JvmName("quantityPolynomialTimesAssign")
fun <V : RealNumber<V>> Quantity<MutableQuadraticPolynomial>.timesAssign(rhs: V) {
    value.timesAssign(rhs.toFlt64())
}

@JvmName("quantityPolynomialDivAssign")
fun <V : RealNumber<V>> Quantity<MutableQuadraticPolynomial>.divAssign(rhs: V) {
    value.divAssign(rhs.toFlt64())
}