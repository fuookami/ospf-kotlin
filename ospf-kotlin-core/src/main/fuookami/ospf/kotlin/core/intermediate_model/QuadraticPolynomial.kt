@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package fuookami.ospf.kotlin.core.intermediate_model

import fuookami.ospf.kotlin.core.intermediate_model.monomial.LinearMonomial
import fuookami.ospf.kotlin.core.intermediate_model.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.core.intermediate_model.monomial.QuadraticMonomialCell
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.QuadraticIntermediateSymbol
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.utils.functional.Variant3
import fuookami.ospf.kotlin.utils.functional.flatMapNotNull
import fuookami.ospf.kotlin.math.BalancedTrivalent
import fuookami.ospf.kotlin.math.Trivalent
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.symbol.Category
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.maxOrNull
import fuookami.ospf.kotlin.quantities.quantity.toFlt64
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial as UtilsLinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial as UtilsQuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial as UtilsLinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial as UtilsQuadraticPolynomial

sealed class AbstractQuadraticPolynomial<Self : AbstractQuadraticPolynomial<Self>> :
    Polynomial<Self, QuadraticMonomial, QuadraticMonomialCell>,
    ToQuadraticPolynomial<QuadraticPolynomial> {

    abstract override val monomials: List<QuadraticMonomial>
    abstract fun toUtilsPolynomial(): UtilsQuadraticPolynomial<Flt64>
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
            if (cachedRange != null) return cachedRange
            val range = ExpressionRange(possibleRange(monomials, constant), Flt64)
            tokenTable?.cacheRange(rangeCacheKey, range)
            return range
        }

    override val dependencies: Set<fuookami.ospf.kotlin.core.intermediate_symbol.IntermediateSymbol>
        get() {
            return monomials.flatMapNotNull {
                val symbols = ArrayList<fuookami.ospf.kotlin.core.intermediate_symbol.IntermediateSymbol>()
                when (val symbol = it.symbol.symbol1) {
                    is Variant3.V2 -> symbols.add(symbol.value)
                    is Variant3.V3 -> symbols.add(symbol.value)
                    else -> {}
                }
                when (val symbol = it.symbol.symbol2) {
                    is Variant3.V2 -> symbols.add(symbol.value)
                    is Variant3.V3 -> symbols.add(symbol.value)
                    else -> {}
                }
                symbols
            }.toSet()
        }

    val flattenedMonomials: QuadraticFlattenData
        get() {
            val tokenTable = cacheTokenTable()
            val cachedFlatten = tokenTable?.cachedQuadraticFlattenValue(flattenCacheKey)
            if (cachedFlatten != null) return cachedFlatten
            val flattenData = calculateQuadraticFlattenedMonomials(monomials, constant)
            tokenTable?.cacheQuadraticFlatten(flattenCacheKey, flattenData)
            return flattenData
        }

    @Deprecated(
        message = "Use flattenedMonomials instead. cells is transitional.",
        level = DeprecationLevel.WARNING
    )
    override val cells: List<QuadraticMonomialCell>
        get() = flattenedMonomials.toQuadraticMonomialCells()

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

    override fun toQuadraticPolynomial(): QuadraticPolynomial = when (this) {
        is QuadraticPolynomial -> this
        is MutableQuadraticPolynomial -> QuadraticPolynomial(monomials.map { it.copy() }, constant, name, displayName)
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
        if (force || cachedRange?.set == false) tokenTable?.clearRange(rangeCacheKey)
        for (monomial in monomials) monomial.flush(force)
        if (force || monomials.any { !it.cached }) tokenTable?.clearQuadraticFlatten(flattenCacheKey)
    }

    override fun toString(): String {
        return displayName
            ?: name.ifEmpty {
                if (monomials.isEmpty()) "$constant"
                else if (constant eq Flt64.zero) monomials.joinToString(" + ") { it.toString() }
                else "${monomials.joinToString(" + ") { it.toString() }} + $constant"
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
        operator fun invoke(item: AbstractVariableItem<*, *>, name: String = "", displayName: String? = null): QuadraticPolynomial =
            QuadraticPolynomial(listOf(QuadraticMonomial(item)), name = name.ifEmpty { item.name }, displayName = displayName)

        operator fun invoke(symbol: LinearIntermediateSymbol, name: String = "", displayName: String? = null): QuadraticPolynomial =
            QuadraticPolynomial(listOf(QuadraticMonomial(symbol)), name = name.ifEmpty { symbol.name }, displayName = displayName ?: symbol.displayName)

        operator fun invoke(symbol: QuadraticIntermediateSymbol, name: String = "", displayName: String? = null): QuadraticPolynomial =
            QuadraticPolynomial(listOf(QuadraticMonomial(symbol)), name = name.ifEmpty { symbol.name }, displayName = displayName ?: symbol.displayName)

        operator fun invoke(monomial: LinearMonomial, name: String = "", displayName: String? = null): QuadraticPolynomial =
            QuadraticPolynomial(listOf(QuadraticMonomial(monomial)), name = name.ifEmpty { monomial.name }, displayName = displayName ?: monomial.displayName)

        operator fun invoke(monomial: QuadraticMonomial, name: String = "", displayName: String? = null): QuadraticPolynomial =
            QuadraticPolynomial(listOf(monomial), name = name.ifEmpty { monomial.name }, displayName = displayName ?: monomial.displayName)

        operator fun invoke(polynomial: AbstractLinearPolynomial<*>, name: String = "", displayName: String? = null): QuadraticPolynomial =
            QuadraticPolynomial(polynomial.monomials.map { QuadraticMonomial(it) }, polynomial.constant, name.ifEmpty { polynomial.name }, displayName ?: polynomial.displayName)

        operator fun invoke(polynomial: AbstractQuadraticPolynomial<*>, name: String = "", displayName: String? = null): QuadraticPolynomial =
            QuadraticPolynomial(polynomial.monomials.map { it.copy() }, polynomial.constant, name.ifEmpty { polynomial.name }, displayName ?: polynomial.displayName)

        operator fun invoke(constant: Int, name: String = "", displayName: String? = null): QuadraticPolynomial =
            QuadraticPolynomial(constant = Flt64(constant), name = name, displayName = displayName)

        operator fun invoke(constant: Double, name: String = "", displayName: String? = null): QuadraticPolynomial =
            QuadraticPolynomial(constant = Flt64(constant), name = name, displayName = displayName)

        operator fun invoke(constant: Boolean, name: String = "", displayName: String? = null): QuadraticPolynomial =
            QuadraticPolynomial(constant = if (constant) Flt64.one else Flt64.zero, name = name, displayName = displayName)

        operator fun invoke(constant: Trivalent, name: String = "", displayName: String? = null): QuadraticPolynomial =
            QuadraticPolynomial(constant = constant.value.toFlt64(), name = name, displayName = displayName)

        operator fun invoke(constant: BalancedTrivalent, name: String = "", displayName: String? = null): QuadraticPolynomial =
            QuadraticPolynomial(constant = constant.value.toFlt64(), name = name, displayName = displayName)

        operator fun <T : RealNumber<T>> invoke(constant: T, name: String = "", displayName: String? = null): QuadraticPolynomial =
            QuadraticPolynomial(constant = constant.toFlt64(), name = name, displayName = displayName)
    }

    override val discrete by lazy { monomials.all { it.discrete } && constant.round() eq constant }

    override fun copy(): QuadraticPolynomial = QuadraticPolynomial(monomials.map { it.copy() }, constant)

    override fun copy(name: String, displayName: String?): QuadraticPolynomial =
        QuadraticPolynomial(monomials.map { it.copy() }, constant, name, displayName)

    override fun plus(rhs: AbstractVariableItem<*, *>): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(QuadraticMonomial(rhs))
        return QuadraticPolynomial(newMonomials, constant)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("plusVariables")
    override fun plus(rhs: Iterable<AbstractVariableItem<*, *>>): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.map { QuadraticMonomial(it) })
        return QuadraticPolynomial(newMonomials, constant)
    }

    override fun plus(rhs: LinearIntermediateSymbol): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(QuadraticMonomial(rhs))
        return QuadraticPolynomial(newMonomials, constant)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("plusLinearSymbols")
    override fun plus(rhs: Iterable<LinearIntermediateSymbol>): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.map { QuadraticMonomial(it) })
        return QuadraticPolynomial(newMonomials, constant)
    }

    override fun plus(rhs: QuadraticIntermediateSymbol): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(QuadraticMonomial(rhs))
        return QuadraticPolynomial(newMonomials, constant)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("plusQuadraticSymbols")
    override fun plus(rhs: Iterable<QuadraticIntermediateSymbol>): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.map { QuadraticMonomial(it) })
        return QuadraticPolynomial(newMonomials, constant)
    }

    override fun plus(rhs: LinearMonomial): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(QuadraticMonomial(rhs))
        return QuadraticPolynomial(newMonomials, constant)
    }

    override fun plus(rhs: QuadraticMonomial): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(rhs.copy())
        return QuadraticPolynomial(newMonomials, constant)
    }

    override fun plus(rhs: AbstractLinearPolynomial<*>): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.monomials.map { QuadraticMonomial(it) })
        return QuadraticPolynomial(newMonomials, constant + rhs.constant)
    }

    override fun plus(rhs: Polynomial<*, QuadraticMonomial, QuadraticMonomialCell>): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.monomials.map { it.copy() })
        return QuadraticPolynomial(newMonomials, constant + rhs.constant)
    }

    override fun plus(rhs: Flt64): QuadraticPolynomial =
        QuadraticPolynomial(monomials.map { it.copy() }, constant + rhs)

    override fun minus(rhs: AbstractVariableItem<*, *>): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(QuadraticMonomial(-Flt64.one, rhs))
        return QuadraticPolynomial(newMonomials, constant)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("minusVariables")
    override fun minus(rhs: Iterable<AbstractVariableItem<*, *>>): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.map { QuadraticMonomial(-Flt64.one, it) })
        return QuadraticPolynomial(newMonomials, constant)
    }

    override fun minus(rhs: LinearIntermediateSymbol): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(QuadraticMonomial(-Flt64.one, rhs))
        return QuadraticPolynomial(newMonomials, constant)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("minusLinearSymbols")
    override fun minus(rhs: Iterable<LinearIntermediateSymbol>): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.map { QuadraticMonomial(-Flt64.one, it) })
        return QuadraticPolynomial(newMonomials, constant)
    }

    override fun minus(rhs: QuadraticIntermediateSymbol): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(QuadraticMonomial(-Flt64.one, rhs))
        return QuadraticPolynomial(newMonomials, constant)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("minusQuadraticSymbols")
    override fun minus(rhs: Iterable<QuadraticIntermediateSymbol>): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.map { QuadraticMonomial(-Flt64.one, it) })
        return QuadraticPolynomial(newMonomials, constant)
    }

    override fun minus(rhs: LinearMonomial): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(QuadraticMonomial(-Flt64.one, QuadraticMonomial(monomial = rhs).symbol))
        return QuadraticPolynomial(newMonomials, constant)
    }

    override fun minus(rhs: AbstractLinearPolynomial<*>): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.monomials.map { QuadraticMonomial(-it.coefficient, QuadraticMonomial(monomial = it).symbol) })
        return QuadraticPolynomial(newMonomials, constant - rhs.constant)
    }

    override fun minus(rhs: QuadraticMonomial): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(-rhs)
        return QuadraticPolynomial(newMonomials, constant)
    }

    override fun minus(rhs: Polynomial<*, QuadraticMonomial, QuadraticMonomialCell>): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.monomials.map { -it })
        return QuadraticPolynomial(newMonomials, constant - rhs.constant)
    }

    override fun minus(rhs: Flt64): QuadraticPolynomial =
        QuadraticPolynomial(monomials.map { it.copy() }, constant - rhs)

    override fun times(rhs: Flt64): QuadraticPolynomial =
        QuadraticPolynomial(monomials.map { it * rhs }, constant)

    override fun times(rhs: AbstractVariableItem<*, *>): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(monomials.map { it * rhs })
        return QuadraticPolynomial(newMonomials, constant)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("timesVariables")
    override fun times(rhs: Iterable<AbstractVariableItem<*, *>>): QuadraticPolynomial {
        var result = this
        for (item in rhs) result = result.times(item)
        return result
    }

    override fun times(rhs: LinearIntermediateSymbol): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(monomials.map { it * rhs })
        return QuadraticPolynomial(newMonomials, constant)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("timesLinearSymbols")
    override fun times(rhs: Iterable<LinearIntermediateSymbol>): QuadraticPolynomial {
        var result = this
        for (symbol in rhs) result = result.times(symbol)
        return result
    }

    override fun times(rhs: QuadraticIntermediateSymbol): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(monomials.map { it * rhs })
        return QuadraticPolynomial(newMonomials, constant)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("timesQuadraticSymbols")
    override fun times(rhs: Iterable<QuadraticIntermediateSymbol>): QuadraticPolynomial {
        var result = this
        for (symbol in rhs) result = result.times(symbol)
        return result
    }

    override fun times(rhs: LinearMonomial): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(monomials.map { it * rhs })
        return QuadraticPolynomial(newMonomials, constant)
    }

    override fun times(rhs: QuadraticMonomial): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(rhs.copy())
        return QuadraticPolynomial(newMonomials, constant)
    }

    override fun times(rhs: AbstractLinearPolynomial<*>): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        for (mono in monomials) {
            for (rhsMono in rhs.monomials) {
                newMonomials.add(mono * rhsMono)
            }
        }
        newMonomials.addAll(monomials.map { (it * rhs.constant as fuookami.ospf.kotlin.core.intermediate_model.monomial.QuadraticMonomial) })
        newMonomials.addAll(rhs.monomials.map { (QuadraticMonomial(it) * constant as fuookami.ospf.kotlin.core.intermediate_model.monomial.QuadraticMonomial) })
        return QuadraticPolynomial(newMonomials, constant)
    }

    override fun times(rhs: AbstractQuadraticPolynomial<*>): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.monomials.map { it.copy() })
        return QuadraticPolynomial(newMonomials, constant)
    }

    override fun div(rhs: Flt64): QuadraticPolynomial =
        QuadraticPolynomial(monomials.map { it / rhs }, constant)

    override fun toUtilsPolynomial(): UtilsQuadraticPolynomial<Flt64> {
        val utilsMonomials = flattenedMonomials.monomials.map { m ->
            UtilsQuadraticMonomial(m.coefficient, m.symbol1, m.symbol2)
        }
        return UtilsQuadraticPolynomial(
            monomials = utilsMonomials,
            constant = flattenedMonomials.constant
        )
    }
}

class MutableQuadraticPolynomial(
    override var monomials: MutableList<QuadraticMonomial> = ArrayList(),
    override var constant: Flt64 = Flt64.zero,
    override var name: String = "",
    override var displayName: String? = null
) : AbstractQuadraticPolynomial<MutableQuadraticPolynomial>(),
    MutablePolynomial<MutableQuadraticPolynomial, QuadraticMonomial, QuadraticMonomialCell> {

    override fun toUtilsPolynomial(): UtilsQuadraticPolynomial<Flt64> {
        val utilsMonomials = flattenedMonomials.monomials.map { m ->
            UtilsQuadraticMonomial(m.coefficient, m.symbol1, m.symbol2)
        }
        return UtilsQuadraticPolynomial(
            monomials = utilsMonomials,
            constant = flattenedMonomials.constant
        )
    }

    override fun minus(rhs: QuadraticMonomial): MutableQuadraticPolynomial {
        return MutableQuadraticPolynomial(
            monomials = (monomials + (-rhs)).toMutableList(),
            constant = constant
        )
    }

    companion object {
        operator fun invoke(item: AbstractVariableItem<*, *>, name: String = "", displayName: String? = null): MutableQuadraticPolynomial =
            MutableQuadraticPolynomial(monomials = mutableListOf(QuadraticMonomial(item)), name = name.ifEmpty { item.name }, displayName = displayName)

        operator fun invoke(symbol: LinearIntermediateSymbol, name: String = "", displayName: String? = null): MutableQuadraticPolynomial =
            MutableQuadraticPolynomial(monomials = mutableListOf(QuadraticMonomial(symbol)), name = name.ifEmpty { symbol.name }, displayName = displayName ?: symbol.displayName)

        operator fun invoke(symbol: QuadraticIntermediateSymbol, name: String = "", displayName: String? = null): MutableQuadraticPolynomial =
            MutableQuadraticPolynomial(monomials = mutableListOf(QuadraticMonomial(symbol)), name = name.ifEmpty { symbol.name }, displayName = displayName ?: symbol.displayName)

        operator fun invoke(monomial: LinearMonomial, name: String = "", displayName: String? = null): MutableQuadraticPolynomial =
            MutableQuadraticPolynomial(monomials = mutableListOf(QuadraticMonomial(monomial)), name = name.ifEmpty { monomial.name }, displayName = displayName ?: monomial.displayName)

        operator fun invoke(monomial: QuadraticMonomial, name: String = "", displayName: String? = null): MutableQuadraticPolynomial =
            MutableQuadraticPolynomial(monomials = mutableListOf(monomial), name = name.ifEmpty { monomial.name }, displayName = displayName ?: monomial.displayName)

        operator fun invoke(polynomial: AbstractLinearPolynomial<*>, name: String = "", displayName: String? = null): MutableQuadraticPolynomial =
            MutableQuadraticPolynomial(polynomial.monomials.map { QuadraticMonomial(it) }.toMutableList(), polynomial.constant, name.ifEmpty { polynomial.name }, displayName ?: polynomial.displayName)

        operator fun invoke(polynomial: AbstractQuadraticPolynomial<*>, name: String = "", displayName: String? = null): MutableQuadraticPolynomial =
            MutableQuadraticPolynomial(polynomial.monomials.map { it.copy() }.toMutableList(), polynomial.constant, name.ifEmpty { polynomial.name }, displayName ?: polynomial.displayName)

        operator fun invoke(constant: Int, name: String = "", displayName: String? = null): MutableQuadraticPolynomial =
            MutableQuadraticPolynomial(constant = Flt64(constant), name = name, displayName = displayName)

        operator fun invoke(constant: Double, name: String = "", displayName: String? = null): MutableQuadraticPolynomial =
            MutableQuadraticPolynomial(constant = Flt64(constant), name = name, displayName = displayName)

        operator fun invoke(constant: Boolean, name: String = "", displayName: String? = null): MutableQuadraticPolynomial =
            MutableQuadraticPolynomial(constant = if (constant) Flt64.one else Flt64.zero, name = name, displayName = displayName)

        operator fun invoke(constant: Trivalent, name: String = "", displayName: String? = null): MutableQuadraticPolynomial =
            MutableQuadraticPolynomial(constant = constant.value.toFlt64(), name = name, displayName = displayName)

        operator fun invoke(constant: BalancedTrivalent, name: String = "", displayName: String? = null): MutableQuadraticPolynomial =
            MutableQuadraticPolynomial(constant = constant.value.toFlt64(), name = name, displayName = displayName)

        operator fun <T : RealNumber<T>> invoke(constant: T, name: String = "", displayName: String? = null): MutableQuadraticPolynomial =
            MutableQuadraticPolynomial(constant = constant.toFlt64(), name = name, displayName = displayName)
    }

    override fun toMutable(): MutableQuadraticPolynomial = this
    override fun asMutable(): MutableQuadraticPolynomial = this

    override fun copy(): MutableQuadraticPolynomial =
        MutableQuadraticPolynomial(monomials.map { it.copy() }.toMutableList(), constant)

    override fun copy(name: String, displayName: String?): MutableQuadraticPolynomial =
        MutableQuadraticPolynomial(monomials.map { it.copy() }.toMutableList(), constant, name, displayName)

    override fun plus(rhs: AbstractVariableItem<*, *>): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(QuadraticMonomial(rhs))
        return MutableQuadraticPolynomial(newMonomials, constant)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("plusVariables")
    override fun plus(rhs: Iterable<AbstractVariableItem<*, *>>): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.map { QuadraticMonomial(it) })
        return MutableQuadraticPolynomial(newMonomials, constant)
    }

    override fun plus(rhs: LinearIntermediateSymbol): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(QuadraticMonomial(rhs))
        return MutableQuadraticPolynomial(newMonomials, constant)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("plusLinearSymbols")
    override fun plus(rhs: Iterable<LinearIntermediateSymbol>): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.map { QuadraticMonomial(it) })
        return MutableQuadraticPolynomial(newMonomials, constant)
    }

    override fun plus(rhs: QuadraticIntermediateSymbol): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(QuadraticMonomial(rhs))
        return MutableQuadraticPolynomial(newMonomials, constant)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("plusQuadraticSymbols")
    override fun plus(rhs: Iterable<QuadraticIntermediateSymbol>): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.map { QuadraticMonomial(it) })
        return MutableQuadraticPolynomial(newMonomials, constant)
    }

    override fun plus(rhs: LinearMonomial): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(QuadraticMonomial(rhs))
        return MutableQuadraticPolynomial(newMonomials, constant)
    }

    override fun plus(rhs: QuadraticMonomial): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(rhs.copy())
        return MutableQuadraticPolynomial(newMonomials, constant)
    }

    override fun plus(rhs: AbstractLinearPolynomial<*>): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.monomials.map { QuadraticMonomial(it) })
        return MutableQuadraticPolynomial(newMonomials, constant + rhs.constant)
    }

    override fun plus(rhs: Polynomial<*, QuadraticMonomial, QuadraticMonomialCell>): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.monomials.map { it.copy() })
        return MutableQuadraticPolynomial(newMonomials, constant + rhs.constant)
    }

    override fun plus(rhs: Flt64): MutableQuadraticPolynomial =
        MutableQuadraticPolynomial(monomials.map { it.copy() }.toMutableList(), constant + rhs)

    override fun minus(rhs: AbstractVariableItem<*, *>): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(QuadraticMonomial(-Flt64.one, rhs))
        return MutableQuadraticPolynomial(newMonomials, constant)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("minusVariables")
    override fun minus(rhs: Iterable<AbstractVariableItem<*, *>>): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.map { QuadraticMonomial(-Flt64.one, it) })
        return MutableQuadraticPolynomial(newMonomials, constant)
    }

    override fun minus(rhs: LinearIntermediateSymbol): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(QuadraticMonomial(-Flt64.one, rhs))
        return MutableQuadraticPolynomial(newMonomials, constant)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("minusLinearSymbols")
    override fun minus(rhs: Iterable<LinearIntermediateSymbol>): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.map { QuadraticMonomial(-Flt64.one, it) })
        return MutableQuadraticPolynomial(newMonomials, constant)
    }

    override fun minus(rhs: QuadraticIntermediateSymbol): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(QuadraticMonomial(-Flt64.one, rhs))
        return MutableQuadraticPolynomial(newMonomials, constant)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("minusQuadraticSymbols")
    override fun minus(rhs: Iterable<QuadraticIntermediateSymbol>): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.map { QuadraticMonomial(-Flt64.one, it) })
        return MutableQuadraticPolynomial(newMonomials, constant)
    }

    override fun minus(rhs: LinearMonomial): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(QuadraticMonomial(-Flt64.one, QuadraticMonomial(monomial = rhs).symbol))
        return MutableQuadraticPolynomial(newMonomials, constant)
    }

    override fun minus(rhs: AbstractLinearPolynomial<*>): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.monomials.map { QuadraticMonomial(-it.coefficient, QuadraticMonomial(monomial = it).symbol) })
        return MutableQuadraticPolynomial(newMonomials, constant - rhs.constant)
    }

    override fun minus(rhs: Polynomial<*, QuadraticMonomial, QuadraticMonomialCell>): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.monomials.map { -it })
        return MutableQuadraticPolynomial(newMonomials, constant - rhs.constant)
    }

    override fun minus(rhs: Flt64): MutableQuadraticPolynomial =
        MutableQuadraticPolynomial(monomials.map { it.copy() }.toMutableList(), constant - rhs)

    override fun times(rhs: Flt64): MutableQuadraticPolynomial =
        MutableQuadraticPolynomial((monomials.map { it * rhs }).toMutableList(), constant)

    override fun times(rhs: AbstractVariableItem<*, *>): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(monomials.map { it * rhs })
        return MutableQuadraticPolynomial(newMonomials, constant)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("timesVariables")
    override fun times(rhs: Iterable<AbstractVariableItem<*, *>>): MutableQuadraticPolynomial {
        var result = this
        for (item in rhs) result = result.times(item)
        return result
    }

    override fun times(rhs: LinearIntermediateSymbol): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(monomials.map { it * rhs })
        return MutableQuadraticPolynomial(newMonomials, constant)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("timesLinearSymbols")
    override fun times(rhs: Iterable<LinearIntermediateSymbol>): MutableQuadraticPolynomial {
        var result = this
        for (symbol in rhs) result = result.times(symbol)
        return result
    }

    override fun times(rhs: QuadraticIntermediateSymbol): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(monomials.map { it * rhs })
        return MutableQuadraticPolynomial(newMonomials, constant)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("timesQuadraticSymbols")
    override fun times(rhs: Iterable<QuadraticIntermediateSymbol>): MutableQuadraticPolynomial {
        var result = this
        for (symbol in rhs) result = result.times(symbol)
        return result
    }

    override fun times(rhs: LinearMonomial): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(monomials.map { it * rhs })
        return MutableQuadraticPolynomial(newMonomials, constant)
    }

    override fun times(rhs: QuadraticMonomial): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(rhs.copy())
        return MutableQuadraticPolynomial(newMonomials, constant)
    }

    override fun times(rhs: AbstractLinearPolynomial<*>): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        for (mono in monomials) for (rhsMono in rhs.monomials) newMonomials.add(mono * rhsMono)
        newMonomials.addAll(monomials.map { (it * rhs.constant as fuookami.ospf.kotlin.core.intermediate_model.monomial.QuadraticMonomial) })
        newMonomials.addAll(rhs.monomials.map { (QuadraticMonomial(it) * constant as fuookami.ospf.kotlin.core.intermediate_model.monomial.QuadraticMonomial) })
        return MutableQuadraticPolynomial(newMonomials, constant)
    }

    override fun times(rhs: AbstractQuadraticPolynomial<*>): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.monomials.map { it.copy() })
        return MutableQuadraticPolynomial(newMonomials, constant)
    }

    override fun div(rhs: Flt64): MutableQuadraticPolynomial =
        MutableQuadraticPolynomial((monomials.map { it / rhs }).toMutableList(), constant)

    // MutablePolynomial plusAssign/minusAssign/timesAssign/divAssign
    override fun plusAssign(rhs: AbstractVariableItem<*, *>) { monomials.add(QuadraticMonomial(rhs)) }
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("plusAssignVariables")
    override fun plusAssign(rhs: Iterable<AbstractVariableItem<*, *>>) { monomials.addAll(rhs.map { QuadraticMonomial(it) }) }
    fun plusAssign(rhs: LinearIntermediateSymbol) { monomials.add(QuadraticMonomial(rhs)) }
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("plusAssignLinearSymbols")
    fun plusAssign(rhs: Iterable<LinearIntermediateSymbol>) { monomials.addAll(rhs.map { QuadraticMonomial(it) }) }
    fun plusAssign(rhs: QuadraticIntermediateSymbol) { monomials.add(QuadraticMonomial(rhs)) }
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("plusAssignQuadraticSymbols")
    fun plusAssign(rhs: Iterable<QuadraticIntermediateSymbol>) { monomials.addAll(rhs.map { QuadraticMonomial(it) }) }
    fun plusAssign(rhs: LinearMonomial) { monomials.add(QuadraticMonomial(rhs)) }
    override fun plusAssign(rhs: QuadraticMonomial) { monomials.add(rhs.copy()) }
    fun plusAssign(rhs: AbstractLinearPolynomial<*>) { monomials.addAll(rhs.monomials.map { QuadraticMonomial(it) }); constant += rhs.constant }
    override fun plusAssign(rhs: Polynomial<*, QuadraticMonomial, QuadraticMonomialCell>) {
        monomials.addAll(rhs.monomials.map { it.copy() })
        constant += rhs.constant
    }
    override fun plusAssign(rhs: Flt64) { constant += rhs }

    override fun minusAssign(rhs: AbstractVariableItem<*, *>) { monomials.add(QuadraticMonomial(-Flt64.one, rhs)) }
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("minusAssignVariables")
    override fun minusAssign(rhs: Iterable<AbstractVariableItem<*, *>>) { monomials.addAll(rhs.map { QuadraticMonomial(-Flt64.one, it) }) }
    fun minusAssign(rhs: LinearIntermediateSymbol) { monomials.add(QuadraticMonomial(-Flt64.one, rhs)) }
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("minusAssignLinearSymbols")
    fun minusAssign(rhs: Iterable<LinearIntermediateSymbol>) { monomials.addAll(rhs.map { QuadraticMonomial(-Flt64.one, it) }) }
    fun minusAssign(rhs: QuadraticIntermediateSymbol) { monomials.add(QuadraticMonomial(-Flt64.one, rhs)) }
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("minusAssignQuadraticSymbols")
    fun minusAssign(rhs: Iterable<QuadraticIntermediateSymbol>) { monomials.addAll(rhs.map { QuadraticMonomial(-Flt64.one, it) }) }
    fun minusAssign(rhs: LinearMonomial) { monomials.add(QuadraticMonomial(-Flt64.one, QuadraticMonomial(monomial = rhs).symbol)) }
    override fun minusAssign(rhs: QuadraticMonomial) { monomials.add(-rhs) }
    override fun minusAssign(rhs: Polynomial<*, QuadraticMonomial, QuadraticMonomialCell>) {
        monomials.addAll(rhs.monomials.map { -it })
        constant -= rhs.constant
    }
    override fun minusAssign(rhs: Flt64) { constant -= rhs }

    override fun timesAssign(rhs: Flt64) {
        monomials.replaceAll { (it * rhs) as QuadraticMonomial }
    }

    override fun divAssign(rhs: Flt64) {
        monomials.replaceAll { (it / rhs) as QuadraticMonomial }
    }
}
