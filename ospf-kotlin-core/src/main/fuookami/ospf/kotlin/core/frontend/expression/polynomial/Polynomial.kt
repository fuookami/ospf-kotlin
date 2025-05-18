package fuookami.ospf.kotlin.core.frontend.expression.polynomial

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

sealed interface Polynomial<Self : Polynomial<Self, M, Cell>, M : Monomial<M, Cell>, Cell : MonomialCell<Cell>>
    : Expression, Copyable<Self>, Neg<Self>,
    Plus<Flt64, Self>, Minus<Flt64, Self>, Times<Flt64, Self>, Div<Flt64, Self> {
    val category: Category
    val monomials: List<M>
    val constant: Flt64
    override val discrete: Boolean get() = monomials.all { it.discrete } && constant.round() eq constant
    val dependencies: Set<IntermediateSymbol>
    val cells: List<Cell>
    val cached: Boolean

    override fun unaryMinus(): Self {
        return this.times(-Flt64.one)
    }

    operator fun <T : RealNumber<T>> plus(rhs: T): Self {
        return this.plus(rhs.toFlt64())
    }

    operator fun plus(rhs: AbstractVariableItem<*, *>): Self

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("plusVariables")
    operator fun plus(rhs: Iterable<AbstractVariableItem<*, *>>): Self
    operator fun plus(rhs: M): Self
    operator fun plus(rhs: Polynomial<*, M, Cell>): Self

    operator fun <T : RealNumber<T>> minus(rhs: T): Self {
        return this.minus(rhs.toFlt64())
    }

    operator fun minus(rhs: AbstractVariableItem<*, *>): Self

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("minusVariables")
    operator fun minus(rhs: Iterable<AbstractVariableItem<*, *>>): Self
    operator fun minus(rhs: M): Self
    operator fun minus(rhs: Polynomial<*, M, Cell>): Self

    operator fun times(rhs: Int): Self {
        return this.times(Flt64(rhs))
    }

    operator fun times(rhs: Double): Self {
        return this.times(Flt64(rhs))
    }

    operator fun <T : RealNumber<T>> times(rhs: T): Self {
        return this.times(rhs.toFlt64())
    }

    operator fun div(rhs: Int): Self {
        return this.div(Flt64(rhs))
    }

    operator fun div(rhs: Double): Self {
        return this.div(Flt64(rhs))
    }

    operator fun <T : RealNumber<T>> div(rhs: T): Self {
        return this.div(rhs.toFlt64())
    }

    fun toMutable(): MutablePolynomial<*, M, Cell>
    fun asMutable(): MutablePolynomial<*, M, Cell>? {
        return null
    }

    fun copy(name: String, displayName: String?): Self
    fun flush(force: Boolean = false)

    fun toRawString(unfold: UInt64 = UInt64.zero): String {
        return if (monomials.isEmpty()) {
            "$constant"
        } else if (constant neq Flt64.zero) {
            "${monomials.filter { it.coefficient neq Flt64.zero }.joinToString(" + ") { it.toRawString(unfold) }} + $constant"
        } else {
            monomials.filter { it.coefficient neq Flt64.zero }.joinToString(" + ") { it.toRawString(unfold) }
        }
    }

    override fun evaluate(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        var ret = constant
        for (monomial in monomials) {
            val thisValue = monomial.evaluate(tokenList, zeroIfNone)
                ?: return null
            ret += thisValue
        }
        return ret
    }

    override fun evaluate(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        var ret = constant
        for (monomial in monomials) {
            val thisValue = monomial.evaluate(results, tokenList, zeroIfNone)
                ?: return null
            ret += thisValue
        }
        return ret
    }

    override fun evaluate(tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        var ret = constant
        for (monomial in monomials) {
            val thisValue = monomial.evaluate(tokenTable, zeroIfNone)
                ?: return null
            ret += thisValue
        }
        return ret
    }

    override fun evaluate(results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        var ret = constant
        for (monomial in monomials) {
            val thisValue = monomial.evaluate(results, tokenTable, zeroIfNone)
                ?: return null
            ret += thisValue
        }
        return ret
    }
}

sealed interface MutablePolynomial<Self : MutablePolynomial<Self, M, Cell>, M : Monomial<M, Cell>, Cell : MonomialCell<Cell>>
    : Polynomial<Self, M, Cell>, PlusAssign<Flt64>, MinusAssign<Flt64>, TimesAssign<Flt64>, DivAssign<Flt64> {
    operator fun plusAssign(rhs: Int) {
        this.plusAssign(Flt64(rhs))
    }

    operator fun plusAssign(rhs: Double) {
        this.plusAssign(Flt64(rhs))
    }

    operator fun <T : RealNumber<T>> plusAssign(rhs: T) {
        this.plusAssign(rhs.toFlt64())
    }

    operator fun plusAssign(rhs: AbstractVariableItem<*, *>)

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("plusAssignVariables")
    operator fun plusAssign(rhs: Iterable<AbstractVariableItem<*, *>>)
    operator fun plusAssign(rhs: M)
    operator fun plusAssign(rhs: Polynomial<*, M, Cell>)

    operator fun minusAssign(rhs: Int) {
        this.minusAssign(Flt64(rhs))
    }

    operator fun minusAssign(rhs: Double) {
        this.minusAssign(Flt64(rhs))
    }

    operator fun <T : RealNumber<T>> minusAssign(rhs: T) {
        this.minusAssign(rhs.toFlt64())
    }

    operator fun minusAssign(rhs: AbstractVariableItem<*, *>)

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("minusAssignVariables")
    operator fun minusAssign(rhs: Iterable<AbstractVariableItem<*, *>>)
    operator fun minusAssign(rhs: M)
    operator fun minusAssign(rhs: Polynomial<*, M, Cell>)

    operator fun timesAssign(rhs: Int) {
        this.timesAssign(Flt64(rhs))
    }

    operator fun timesAssign(rhs: Double) {
        this.timesAssign(Flt64(rhs))
    }

    operator fun <T : RealNumber<T>> timesAssign(rhs: T) {
        this.timesAssign(rhs.toFlt64())
    }

    operator fun divAssign(rhs: Int) {
        this.divAssign(Flt64(rhs))
    }

    operator fun divAssign(rhs: Double) {
        this.divAssign(Flt64(rhs))
    }

    operator fun <T : RealNumber<T>> divAssign(rhs: T) {
        this.divAssign(rhs.toFlt64())
    }
}

internal fun possibleRange(
    monomials: List<Monomial<*, *>>,
    constant: Flt64
): ValueRange<Flt64>? {
    return if (monomials.isEmpty()) {
        ValueRange(
            constant,
            constant,
            Interval.Closed,
            Interval.Closed
        ).value
    } else {
        var ret = monomials[0].range.range ?: return null
        for (i in 1..<monomials.size) {
            val value = monomials[i].range.range ?: return null
            ret += value
        }
        ret += constant
        ret
    }
}
