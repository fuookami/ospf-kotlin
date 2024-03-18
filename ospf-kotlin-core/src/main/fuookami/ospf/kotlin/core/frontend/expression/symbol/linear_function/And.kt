package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function

import fuookami.ospf.kotlin.core.frontend.expression.monomial.LinearMonomialCell
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

class AndFunction(
    private val polynomials: List<AbstractLinearPolynomial<*>>,
    override var name: String,
    override var displayName: String? = null
) : LinearLogicFunctionSymbol {
    private lateinit var maxmin: MaxMinFunction
    private lateinit var bin: BinaryzationFunction
    private lateinit var y: BinVar
    private lateinit var polyY: AbstractLinearPolynomial<*>

    override val discrete = true

    override val range get() = polyY.range
    override val lowerBound
        get() = if (::polyY.isInitialized) {
            polyY.lowerBound
        } else {
            possibleRange.lowerBound.toFlt64()
        }
    override val upperBound
        get() = if (::polyY.isInitialized) {
            polyY.upperBound
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
    override val cells get() = polyY.cells
    override val cached
        get() = if (::polyY.isInitialized) {
            polyY.cached
        } else {
            false
        }

    private val possibleRange
        get() = ValueRange(
            if (polynomials.any { it.lowerBound.toFlt64() eq Flt64.zero }) {
                Flt64.zero
            } else {
                Flt64.one
            },
            if (polynomials.any { it.upperBound.toFlt64() eq Flt64.zero }) {
                Flt64.zero
            } else {
                Flt64.one
            }
        )

    override fun flush(force: Boolean) {
        if (::polyY.isInitialized) {
            polyY.flush(force)
            polyY.range.set(possibleRange)
        }
    }

    override suspend fun prepare() {
        for (polynomial in polynomials) {
            polynomial.cells
        }
    }

    override fun register(tokenTable: LinearMutableTokenTable): Try {
        // all polys must be ∈ (R - R-)
        for (polynomial in polynomials) {
            if (polynomial.lowerBound ls Flt64.zero) {
                return Failed(Err(ErrorCode.ApplicationFailed, "$name's domain of definition unsatisfied: $polynomial"))
            }
        }

        if (polynomials.size == 1) {
            if (!::bin.isInitialized) {
                bin = BinaryzationFunction(polynomials[0], name = "${name}_bin")
            }
            when (val result = bin.register(tokenTable)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        } else if (polynomials.all { it.discrete && it.upperBound leq Flt64.one }) {
            if (!::y.isInitialized) {
                y = BinVar("${name}_y")
            }
            when (val result = tokenTable.add(y)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        } else {
            if (!::maxmin.isInitialized) {
                maxmin = MaxMinFunction(polynomials, "${name}_maxmin")
            }
            when (val result = maxmin.register(tokenTable)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }

            if (!::bin.isInitialized) {
                bin = BinaryzationFunction(LinearPolynomial(maxmin), name = "${name}_bin")
            }
            when (val result = bin.register(tokenTable)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        if (!::polyY.isInitialized) {
            polyY = if (::y.isInitialized) {
                LinearPolynomial(y)
            } else {
                assert(::bin.isInitialized)
                LinearPolynomial(bin)
            }
            polyY.range.set(possibleRange)
        }

        return Ok(success)
    }

    override fun register(model: AbstractLinearModel): Try {
        if (::maxmin.isInitialized) {
            when (val result = maxmin.register(model)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        if (::bin.isInitialized) {
            when (val result = bin.register(model)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        if (::y.isInitialized) {
            // if any polynomial is zero, y will be zero
            for ((i, polynomial) in polynomials.withIndex()) {
                model.addConstraint(
                    y leq polynomial,
                    "${name}_ub_${polynomial.name.ifEmpty { "$i" }}"
                )
            }
            // if all polynomial are not zero, y will be not zero
            model.addConstraint(
                y geq (sum(polynomials) - Flt64(polynomials.size - 1)),
                "${name}_lb"
            )
        }

        return Ok(success)
    }

    override fun toString(): String {
        return displayName ?: name
    }

    override fun toRawString(unfold: Boolean): String {
        return "and(${polynomials.joinToString(", ") { it.toRawString(unfold) }})"
    }

    override fun value(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return if (polynomials.all {
                val thisValue = it.value(tokenList, zeroIfNone) ?: return null
                thisValue neq Flt64.zero
            }) {
            Flt64.one
        } else {
            Flt64.zero
        }
    }

    override fun value(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return if (polynomials.all {
                val thisValue = it.value(results, tokenList, zeroIfNone) ?: return null
                thisValue neq Flt64.zero
            }) {
            Flt64.one
        } else {
            Flt64.zero
        }
    }
}
