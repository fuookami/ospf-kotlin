package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function

import fuookami.ospf.kotlin.core.frontend.expression.monomial.LinearMonomialCell
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

sealed class AbstractMaxFunction(
    protected val polynomials: List<AbstractLinearPolynomial<*>>,
    private val exact: Boolean = true,
    override var name: String,
    override var displayName: String? = null
) : LinearFunctionSymbol {
    private lateinit var minMax: RealVar
    private lateinit var u: BinVariable1
    private lateinit var y: AbstractLinearPolynomial<*>

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

    override val dependencies: Set<Symbol<*, *>>
        get() {
            val dependencies = HashSet<Symbol<*, *>>()
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

    override val discrete by lazy { polynomials.all { it.discrete } }

    override fun flush(force: Boolean) {
        if (::y.isInitialized) {
            y.flush(force)
            val newM = possibleRange
            if (m neq newM) {
                minMax.range.set(m)
                m = newM
            }
        }
    }

    override suspend fun prepare() {
        for (polynomial in polynomials) {
            polynomial.cells
        }
    }

    override fun register(tokenTable: LinearMutableTokenTable): Try {
        if (!::minMax.isInitialized) {
            minMax = RealVar("${name}_y")
        }
        when (val result = tokenTable.add(minMax)) {
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

        return Ok(success)
    }

    override fun register(model: AbstractLinearModel): Try {
        for ((i, polynomial) in polynomials.withIndex()) {
            model.addConstraint(
                minMax geq polynomial,
                "${name}_lb_${polynomial.name.ifEmpty { "$i" }}"
            )
        }

        if (::u.isInitialized) {
            for ((i, polynomial) in polynomials.withIndex()) {
                model.addConstraint(
                    minMax leq (polynomial + m.upperBound.toFlt64() * (Flt64.one - u[i])),
                    "${name}_ub_${polynomial.name.ifEmpty { "$i" }}"
                )
            }

            model.addConstraint(sum(u) eq Flt64.one, "${name}_u")
        }

        return Ok(success)
    }

    override fun toString(): String {
        return displayName ?: name
    }

    override fun value(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return polynomials.maxOf { it.value(tokenList, zeroIfNone) ?: return null }
    }

    override fun value(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return polynomials.maxOf { it.value(results, tokenList, zeroIfNone) ?: return null }
    }
}

class MinMaxFunction(
    polynomials: List<AbstractLinearPolynomial<*>>,
    name: String,
    displayName: String? = name
) : AbstractMaxFunction(polynomials, true, name, displayName) {
    override fun toRawString(unfold: Boolean): String {
        return "max(${polynomials.joinToString(", ") { it.toRawString(unfold) }})"
    }
}

class MaxFunction(
    polynomials: List<AbstractLinearPolynomial<*>>,
    name: String,
    displayName: String? = name
) : AbstractMaxFunction(polynomials, false, name, displayName) {
    override fun toRawString(unfold: Boolean): String {
        return "minmax(${polynomials.joinToString(", ") { it.toRawString(unfold) }})"
    }
}
