@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package fuookami.ospf.kotlin.core.intermediate_model

import fuookami.ospf.kotlin.core.intermediate_model.monomial.LinearMonomial
import fuookami.ospf.kotlin.core.intermediate_model.monomial.LinearMonomialCell
import fuookami.ospf.kotlin.core.intermediate_model.monomial.times
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.utils.functional.Either
import fuookami.ospf.kotlin.math.BalancedTrivalent
import fuookami.ospf.kotlin.math.Trivalent
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.inequality.Flt64LinearInequality as MathLinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequality as MathQuadraticInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial as UtilsLinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial as UtilsQuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial as UtilsLinearPolynomial
import fuookami.ospf.kotlin.quantities.quantity.toFlt64
import fuookami.ospf.kotlin.math.symbol.operation.evaluate
import fuookami.ospf.kotlin.math.symbol.adapter.MissingValuePolicy

sealed class AbstractLinearPolynomial<Self : AbstractLinearPolynomial<Self>> :
    Polynomial<Self, LinearMonomial, LinearMonomialCell>,
    ToLinearPolynomial<LinearPolynomial>, ToQuadraticPolynomial<QuadraticPolynomial> {

    abstract override val monomials: List<LinearMonomial>
    abstract fun toUtilsPolynomial(): fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial<Flt64>
    override val category get() = Linear

    private val rangeCacheKey = newTokenCacheKey(
        category = Linear,
        prefix = "__linear_polynomial_range_cache__"
    )
    private val flattenCacheKey = newTokenCacheKey(
        category = Linear,
        prefix = "__linear_polynomial_flatten_cache__"
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
            return monomials.mapNotNull {
                when (val symbol = it.symbol.symbol) {
                    is Either.Right -> symbol.value
                    else -> null
                }
            }.toSet()
        }

    val flattenedMonomials: LinearFlattenData
        get() {
            val tokenTable = cacheTokenTable()
            val cachedFlatten = tokenTable?.cachedLinearFlattenValue(flattenCacheKey)
            if (cachedFlatten != null) return cachedFlatten
            val flattenData = calculateLinearFlattenedMonomials(monomials, constant)
            tokenTable?.cacheLinearFlatten(flattenCacheKey, flattenData)
            return flattenData
        }

    @Deprecated(
        message = "Use flattenedMonomials instead. cells is transitional.",
        level = DeprecationLevel.WARNING
    )
    override val cells: List<LinearMonomialCell>
        get() = flattenedMonomials.toLinearMonomialCells()

    override val cached: Boolean
        get() = cacheTokenTable()?.cachedLinearFlatten(flattenCacheKey) == true

    abstract operator fun plus(rhs: LinearIntermediateSymbol): Self
    abstract operator fun plus(rhs: Iterable<LinearIntermediateSymbol>): Self
    abstract operator fun minus(rhs: LinearIntermediateSymbol): Self
    abstract operator fun minus(rhs: Iterable<LinearIntermediateSymbol>): Self

    final override fun toLinearPolynomial(): LinearPolynomial {
        return when (this) {
            is LinearPolynomial -> this
            is MutableLinearPolynomial -> LinearPolynomial(monomials.map { it.copy() }, constant, name, displayName)
        }
    }

    override fun toQuadraticPolynomial(): QuadraticPolynomial {
        return QuadraticPolynomial(
            monomials = monomials.map { fuookami.ospf.kotlin.core.intermediate_model.monomial.QuadraticMonomial(it) },
            constant = constant
        )
    }

    override fun toMathLinearInequality(): MathLinearInequality {
        return this eq true
    }

    override fun toMutable(): MutableLinearPolynomial {
        return MutableLinearPolynomial(
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
            tokenTable?.clearLinearFlatten(flattenCacheKey)
        }
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

class LinearPolynomial(
    override val monomials: List<LinearMonomial> = emptyList(),
    override val constant: Flt64 = Flt64.zero,
    override var name: String = "",
    override var displayName: String? = null
) : AbstractLinearPolynomial<LinearPolynomial>() {
    companion object {
        operator fun invoke(item: AbstractVariableItem<*, *>, name: String = "", displayName: String? = null): LinearPolynomial {
            return LinearPolynomial(listOf(LinearMonomial(item)), name = name.ifEmpty { item.name }, displayName = displayName)
        }

        operator fun invoke(symbol: LinearIntermediateSymbol, name: String = "", displayName: String? = null): LinearPolynomial {
            return LinearPolynomial(listOf(LinearMonomial(symbol)), name = name.ifEmpty { symbol.name }, displayName = displayName ?: symbol.displayName)
        }

        operator fun invoke(monomial: LinearMonomial, name: String = "", displayName: String? = null): LinearPolynomial {
            return LinearPolynomial(listOf(monomial), name = name.ifEmpty { monomial.name }, displayName = displayName ?: monomial.displayName)
        }

        operator fun invoke(polynomial: AbstractLinearPolynomial<*>, name: String = "", displayName: String? = null): LinearPolynomial {
            return LinearPolynomial(polynomial.monomials.map { it.copy() }, polynomial.constant, name.ifEmpty { polynomial.name }, displayName ?: polynomial.displayName)
        }

        operator fun invoke(constant: Int, name: String = "", displayName: String? = null): LinearPolynomial =
            LinearPolynomial(constant = Flt64(constant), name = name, displayName = displayName)

        operator fun invoke(constant: Double, name: String = "", displayName: String? = null): LinearPolynomial =
            LinearPolynomial(constant = Flt64(constant), name = name, displayName = displayName)

        operator fun invoke(constant: Boolean, name: String = "", displayName: String? = null): LinearPolynomial =
            LinearPolynomial(constant = if (constant) Flt64.one else Flt64.zero, name = name, displayName = displayName)

        operator fun invoke(constant: Trivalent, name: String = "", displayName: String? = null): LinearPolynomial =
            LinearPolynomial(constant = constant.value.toFlt64(), name = name, displayName = displayName)

        operator fun invoke(constant: BalancedTrivalent, name: String = "", displayName: String? = null): LinearPolynomial =
            LinearPolynomial(constant = constant.value.toFlt64(), name = name, displayName = displayName)

        operator fun <T : RealNumber<T>> invoke(constant: T, name: String = "", displayName: String? = null): LinearPolynomial =
            LinearPolynomial(constant = constant.toFlt64(), name = name, displayName = displayName)
    }

    override val discrete by lazy { monomials.all { it.discrete } && constant.round() eq constant }

    override fun copy(): LinearPolynomial = LinearPolynomial(monomials.map { it.copy() }, constant)

    override fun copy(name: String, displayName: String?): LinearPolynomial =
        LinearPolynomial(monomials.map { it.copy() }, constant, name, displayName)

    override fun plus(rhs: AbstractVariableItem<*, *>): LinearPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(LinearMonomial(rhs))
        return LinearPolynomial(newMonomials, constant)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("plusVariables")
    override fun plus(rhs: Iterable<AbstractVariableItem<*, *>>): LinearPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.map { LinearMonomial(it) })
        return LinearPolynomial(newMonomials, constant)
    }

    override fun plus(rhs: LinearIntermediateSymbol): LinearPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(LinearMonomial(rhs))
        return LinearPolynomial(newMonomials, constant)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("plusSymbols")
    override fun plus(rhs: Iterable<LinearIntermediateSymbol>): LinearPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.map { LinearMonomial(it) })
        return LinearPolynomial(newMonomials, constant)
    }

    override fun plus(rhs: LinearMonomial): LinearPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(rhs.copy())
        return LinearPolynomial(newMonomials, constant)
    }

    override fun plus(rhs: Polynomial<*, LinearMonomial, LinearMonomialCell>): LinearPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.monomials.map { it.copy() })
        return LinearPolynomial(newMonomials, constant + rhs.constant)
    }

    override fun plus(rhs: Flt64): LinearPolynomial = LinearPolynomial(monomials.map { it.copy() }, constant + rhs)

    override fun minus(rhs: AbstractVariableItem<*, *>): LinearPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(LinearMonomial(-Flt64.one, rhs))
        return LinearPolynomial(newMonomials, constant)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("minusVariables")
    override fun minus(rhs: Iterable<AbstractVariableItem<*, *>>): LinearPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.map { LinearMonomial(-Flt64.one, it) })
        return LinearPolynomial(newMonomials, constant)
    }

    override fun minus(rhs: LinearIntermediateSymbol): LinearPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(LinearMonomial(-Flt64.one, rhs))
        return LinearPolynomial(newMonomials, constant)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("minusSymbols")
    override fun minus(rhs: Iterable<LinearIntermediateSymbol>): LinearPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.map { LinearMonomial(-Flt64.one, it) })
        return LinearPolynomial(newMonomials, constant)
    }

    override fun minus(rhs: LinearMonomial): LinearPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(-rhs)
        return LinearPolynomial(newMonomials, constant)
    }

    override fun minus(rhs: Polynomial<*, LinearMonomial, LinearMonomialCell>): LinearPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.monomials.map { -it })
        return LinearPolynomial(newMonomials, constant - rhs.constant)
    }

    override fun minus(rhs: Flt64): LinearPolynomial = LinearPolynomial(monomials.map { it.copy() }, constant - rhs)

    override fun times(rhs: Flt64): LinearPolynomial = LinearPolynomial(monomials.map { rhs * it }, constant)

    override fun div(rhs: Flt64): LinearPolynomial = LinearPolynomial(monomials.map { it / rhs }, constant)

    override fun toMathQuadraticInequality(): MathQuadraticInequality {
        return toQuadraticPolynomial() eq true
    }

    override fun toUtilsPolynomial(): fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial<Flt64> {
        val utilsMonomials = flattenedMonomials.monomials.map { m ->
            UtilsLinearMonomial(m.coefficient, m.symbol)
        }
        return fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial(
            monomials = utilsMonomials,
            constant = flattenedMonomials.constant
        )
    }
}

class MutableLinearPolynomial(
    override var monomials: MutableList<LinearMonomial> = ArrayList(),
    override var constant: Flt64 = Flt64.zero,
    override var name: String = "",
    override var displayName: String? = null
) : AbstractLinearPolynomial<MutableLinearPolynomial>(),
    MutablePolynomial<MutableLinearPolynomial, LinearMonomial, LinearMonomialCell> {

    override fun toUtilsPolynomial(): fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial<Flt64> {
        val utilsMonomials = flattenedMonomials.monomials.map { m ->
            UtilsLinearMonomial(m.coefficient, m.symbol)
        }
        return fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial(
            monomials = utilsMonomials,
            constant = flattenedMonomials.constant
        )
    }

    companion object {
        operator fun invoke(item: AbstractVariableItem<*, *>, name: String = "", displayName: String? = null): MutableLinearPolynomial {
            return MutableLinearPolynomial(monomials = mutableListOf(LinearMonomial(item)), name = name.ifEmpty { item.name }, displayName = displayName)
        }

        operator fun invoke(symbol: LinearIntermediateSymbol, name: String = "", displayName: String? = null): MutableLinearPolynomial {
            return MutableLinearPolynomial(monomials = mutableListOf(LinearMonomial(symbol)), name = name.ifEmpty { symbol.name }, displayName = displayName ?: symbol.displayName)
        }

        operator fun invoke(monomial: LinearMonomial, name: String = "", displayName: String? = null): MutableLinearPolynomial {
            return MutableLinearPolynomial(monomials = mutableListOf(monomial), name = name.ifEmpty { monomial.name }, displayName = displayName ?: monomial.displayName)
        }

        operator fun invoke(polynomial: AbstractLinearPolynomial<*>, name: String = "", displayName: String? = null): MutableLinearPolynomial {
            return MutableLinearPolynomial(polynomial.monomials.map { it.copy() }.toMutableList(), polynomial.constant, name.ifEmpty { polynomial.name }, displayName ?: polynomial.displayName)
        }

        operator fun invoke(constant: Int, name: String = "", displayName: String? = null): MutableLinearPolynomial =
            MutableLinearPolynomial(constant = Flt64(constant), name = name, displayName = displayName)

        operator fun invoke(constant: Double, name: String = "", displayName: String? = null): MutableLinearPolynomial =
            MutableLinearPolynomial(constant = Flt64(constant), name = name, displayName = displayName)

        operator fun invoke(constant: Boolean, name: String = "", displayName: String? = null): MutableLinearPolynomial =
            MutableLinearPolynomial(constant = if (constant) Flt64.one else Flt64.zero, name = name, displayName = displayName)

        operator fun invoke(constant: Trivalent, name: String = "", displayName: String? = null): MutableLinearPolynomial =
            MutableLinearPolynomial(constant = constant.value.toFlt64(), name = name, displayName = displayName)

        operator fun invoke(constant: BalancedTrivalent, name: String = "", displayName: String? = null): MutableLinearPolynomial =
            MutableLinearPolynomial(constant = constant.value.toFlt64(), name = name, displayName = displayName)

        operator fun <T : RealNumber<T>> invoke(constant: T, name: String = "", displayName: String? = null): MutableLinearPolynomial =
            MutableLinearPolynomial(constant = constant.toFlt64(), name = name, displayName = displayName)
    }

    override fun toMutable(): MutableLinearPolynomial = this
    override fun asMutable(): MutableLinearPolynomial = this

    override fun copy(): MutableLinearPolynomial =
        MutableLinearPolynomial(monomials.map { it.copy() }.toMutableList(), constant)

    override fun copy(name: String, displayName: String?): MutableLinearPolynomial =
        MutableLinearPolynomial(monomials.map { it.copy() }.toMutableList(), constant, name, displayName)

    override fun plus(rhs: AbstractVariableItem<*, *>): MutableLinearPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(LinearMonomial(rhs))
        return MutableLinearPolynomial(newMonomials, constant)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("plusVariables")
    override fun plus(rhs: Iterable<AbstractVariableItem<*, *>>): MutableLinearPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.map { LinearMonomial(it) })
        return MutableLinearPolynomial(newMonomials, constant)
    }

    override fun plus(rhs: LinearIntermediateSymbol): MutableLinearPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(LinearMonomial(rhs))
        return MutableLinearPolynomial(newMonomials, constant)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("plusSymbols")
    override fun plus(rhs: Iterable<LinearIntermediateSymbol>): MutableLinearPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.map { LinearMonomial(it) })
        return MutableLinearPolynomial(newMonomials, constant)
    }

    override fun plus(rhs: LinearMonomial): MutableLinearPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(rhs.copy())
        return MutableLinearPolynomial(newMonomials, constant)
    }

    override fun plus(rhs: Polynomial<*, LinearMonomial, LinearMonomialCell>): MutableLinearPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.monomials.map { it.copy() })
        return MutableLinearPolynomial(newMonomials, constant + rhs.constant)
    }

    override fun plus(rhs: Flt64): MutableLinearPolynomial =
        MutableLinearPolynomial(monomials.map { it.copy() }.toMutableList(), constant + rhs)

    override fun minus(rhs: AbstractVariableItem<*, *>): MutableLinearPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(LinearMonomial(-Flt64.one, rhs))
        return MutableLinearPolynomial(newMonomials, constant)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("minusVariables")
    override fun minus(rhs: Iterable<AbstractVariableItem<*, *>>): MutableLinearPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.map { LinearMonomial(-Flt64.one, it) })
        return MutableLinearPolynomial(newMonomials, constant)
    }

    override fun minus(rhs: LinearIntermediateSymbol): MutableLinearPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(LinearMonomial(-Flt64.one, rhs))
        return MutableLinearPolynomial(newMonomials, constant)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("minusSymbols")
    override fun minus(rhs: Iterable<LinearIntermediateSymbol>): MutableLinearPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.map { LinearMonomial(-Flt64.one, it) })
        return MutableLinearPolynomial(newMonomials, constant)
    }

    override fun minus(rhs: LinearMonomial): MutableLinearPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(-rhs)
        return MutableLinearPolynomial(newMonomials, constant)
    }

    override fun minus(rhs: Polynomial<*, LinearMonomial, LinearMonomialCell>): MutableLinearPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.monomials.map { -it })
        return MutableLinearPolynomial(newMonomials, constant - rhs.constant)
    }

    override fun minus(rhs: Flt64): MutableLinearPolynomial =
        MutableLinearPolynomial(monomials.map { it.copy() }.toMutableList(), constant - rhs)

    override fun times(rhs: Flt64): MutableLinearPolynomial =
        MutableLinearPolynomial((monomials.map { rhs * it }).toMutableList(), constant)

    override fun div(rhs: Flt64): MutableLinearPolynomial =
        MutableLinearPolynomial((monomials.map { it / rhs }).toMutableList(), constant)

    override fun plusAssign(rhs: AbstractVariableItem<*, *>) { monomials.add(LinearMonomial(rhs)) }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("plusAssignVariables")
    override fun plusAssign(rhs: Iterable<AbstractVariableItem<*, *>>) { monomials.addAll(rhs.map { LinearMonomial(it) }) }

    fun plusAssign(rhs: LinearIntermediateSymbol) { monomials.add(LinearMonomial(rhs)) }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("plusAssignSymbols")
    fun plusAssign(rhs: Iterable<LinearIntermediateSymbol>) { monomials.addAll(rhs.map { LinearMonomial(it) }) }

    override fun plusAssign(rhs: LinearMonomial) { monomials.add(rhs.copy()) }

    override fun plusAssign(rhs: Polynomial<*, LinearMonomial, LinearMonomialCell>) {
        monomials.addAll(rhs.monomials.map { it.copy() })
        constant += rhs.constant
    }

    override fun plusAssign(rhs: Flt64) { constant += rhs }

    override fun minusAssign(rhs: AbstractVariableItem<*, *>) { monomials.add(LinearMonomial(-Flt64.one, rhs)) }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("minusAssignVariables")
    override fun minusAssign(rhs: Iterable<AbstractVariableItem<*, *>>) { monomials.addAll(rhs.map { LinearMonomial(-Flt64.one, it) }) }

    fun minusAssign(rhs: LinearIntermediateSymbol) { monomials.add(LinearMonomial(-Flt64.one, rhs)) }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("minusAssignSymbols")
    fun minusAssign(rhs: Iterable<LinearIntermediateSymbol>) { monomials.addAll(rhs.map { LinearMonomial(-Flt64.one, it) }) }

    override fun minusAssign(rhs: LinearMonomial) { monomials.add(-rhs) }

    override fun minusAssign(rhs: Polynomial<*, LinearMonomial, LinearMonomialCell>) {
        monomials.addAll(rhs.monomials.map { -it })
        constant -= rhs.constant
    }

    override fun minusAssign(rhs: Flt64) { constant -= rhs }

    override fun timesAssign(rhs: Flt64) {
        monomials.replaceAll { (rhs * it) as LinearMonomial }
    }

    override fun divAssign(rhs: Flt64) {
        monomials.replaceAll { (it / rhs) as LinearMonomial }
    }
}
