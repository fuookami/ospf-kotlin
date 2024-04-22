package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

sealed class AbstractMinFunction(
    protected val polynomials: List<AbstractLinearPolynomial<*>>,
    private val exact: Boolean = true,
    override var name: String,
    override var displayName: String? = null
) : LinearFunctionSymbol {
    private lateinit var maxmin: RealVar
    private lateinit var u: BinVariable1
    private lateinit var y: AbstractLinearPolynomial<*>

    override val discrete by lazy { polynomials.all { it.discrete } }

    override val range get() = y.range
    override val lowerBound
        get() = if (::y.isInitialized) {
            y.lowerBound
        } else {
            possibleRange.lowerBound.toFlt64()
        }
    override val upperBound
        get() = if (::y.isInitialized) {
            y.upperBound
        } else {
            possibleRange.upperBound.toFlt64()
        }

    override val category: Category = Linear

    override val dependencies: Set<Symbol>
        get() {
            val dependencies = HashSet<Symbol>()
            for (polynomial in polynomials) {
                dependencies.addAll(polynomial.dependencies)
            }
            return dependencies
        }
    override val cells get() = y.cells
    override val cached
        get() = if (::y.isInitialized) {
            y.cached
        } else {
            false
        }

    private val possibleRange
        get() = ValueRange(
            polynomials.minOf { it.lowerBound },
            polynomials.maxOf { it.upperBound }
        )
    private var m = possibleRange

    override fun flush(force: Boolean) {
        if (::y.isInitialized) {
            y.flush(force)
            val newM = possibleRange
            if (m neq newM) {
                maxmin.range.set(m)
                m = newM
            }
        }
    }

    override suspend fun prepare(tokenTable: AbstractTokenTable) {
        for (polynomial in polynomials) {
            polynomial.cells
        }
    }

    override fun register(tokenTable: MutableTokenTable): Try {
        if (!::maxmin.isInitialized) {
            maxmin = RealVar("${name}_maxmin")
        }
        when (val result = tokenTable.add(maxmin)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        if (exact) {
            if (!::u.isInitialized) {
                u = BinVariable1("${name}_u", Shape1(polynomials.size))
                for ((i, polynomial) in polynomials.withIndex()) {
                    u[i].name = "${u.name}_${polynomial.name.ifEmpty { "$i" }}"
                }
            }
            when (val result = tokenTable.add(u)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        if (!::y.isInitialized) {
            y = LinearPolynomial(maxmin, "${name}_y")
            y.range.set(m)
        }

        return ok
    }

    override fun register(model: AbstractLinearMechanismModel): Try {
        for ((i, polynomial) in polynomials.withIndex()) {
            when (val result = model.addConstraint(
                maxmin leq polynomial,
                "${name}_lb_${polynomial.name.ifEmpty { "$i" }}"
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        if (::u.isInitialized) {
            for ((i, polynomial) in polynomials.withIndex()) {
                when (val result = model.addConstraint(
                    maxmin geq (polynomial - m.upperBound.toFlt64() * (Flt64.one - u[i])),
                    "${name}_ub_${polynomial.name.ifEmpty { "$i" }}"
                )) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }
                }
            }

            when (val result = model.addConstraint(
                sum(u) eq Flt64.one,
                "${name}_u"
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        return ok
    }

    override fun toString(): String {
        return displayName ?: name
    }

    override fun value(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return polynomials.minOf { it.value(tokenList, zeroIfNone) ?: return null }
    }

    override fun value(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return polynomials.minOf { it.value(results, tokenList, zeroIfNone) ?: return null }
    }

    override fun calculateValue(tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        return polynomials.minOf { it.value(tokenTable, zeroIfNone) ?: return null }
    }

    override fun calculateValue(results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        return polynomials.minOf { it.value(results, tokenTable, zeroIfNone) ?: return null }
    }
}

class MaxMinFunction(
    polynomials: List<AbstractLinearPolynomial<*>>,
    name: String,
    displayName: String? = null
) : AbstractMinFunction(polynomials, true, name, displayName) {
    override fun toRawString(unfold: Boolean): String {
        return "min(${polynomials.joinToString(", ") { it.toRawString(unfold) }})"
    }
}

class MinFunction(
    polynomials: List<AbstractLinearPolynomial<*>>,
    name: String,
    displayName: String? = null
) : AbstractMinFunction(polynomials, false, name, displayName) {
    override fun toRawString(unfold: Boolean): String {
        return "maxmin(${polynomials.joinToString(", ") { it.toRawString(unfold) }})"
    }
}
