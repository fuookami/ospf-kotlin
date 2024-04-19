package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

class OrFunction(
    private val polynomials: List<AbstractLinearPolynomial<*>>,
    override var name: String,
    override var displayName: String? = null
) : LinearLogicFunctionSymbol {
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

    override val category: Category = Linear

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
            if (!polynomials.any { it.lowerBound.toFlt64() eq Flt64.zero }) {
                Flt64.one
            } else {
                Flt64.zero
            },
            if (polynomials.all { it.upperBound.toFlt64() eq Flt64.zero }) {
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
        if (!::y.isInitialized) {
            y = BinVar("${name}_y")
        }
        when (val result = tokenTable.add(y)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        if (!::polyY.isInitialized) {
            polyY = LinearPolynomial(y, y.name)
            polyY.range.set(ValueRange(Flt64.zero, Flt64.one))
        }

        return ok
    }

    override fun register(model: AbstractLinearModel): Try {
        // all polys must be ∈ (R - R-)
        for (polynomial in polynomials) {
            if (polynomial.lowerBound ls Flt64.zero) {
                return Failed(Err(ErrorCode.ApplicationFailed, "$name's domain of definition unsatisfied: $polynomial"))
            }
        }

        // if any polynomial is not zero, y will be not zero
        for ((i, polynomial) in polynomials.withIndex()) {
            if (polynomial.upperBound gr Flt64.one) {
                model.addConstraint(
                    y geq (polynomial / polynomial.upperBound.toFlt64()),
                    "${name}_lb_${polynomial.name.ifEmpty { "$i" }}"
                )
            } else {
                model.addConstraint(
                    y geq polynomial,
                    "${name}_lb_${polynomial.name.ifEmpty { "$i" }}"
                )
            }
        }

        // if all polynomials are zero, y will be zero
        model.addConstraint(
            y leq sum(polynomials),
            "${name}_ub"
        )

        return ok
    }

    override fun toString(): String {
        return displayName ?: name
    }

    override fun toRawString(unfold: Boolean): String {
        return "or(${polynomials.joinToString(", ") { it.toRawString(unfold) }})"
    }

    override fun value(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return if (polynomials.any {
                val thisValue = it.value(tokenList, zeroIfNone) ?: return null
                thisValue neq Flt64.zero
            }) {
            Flt64.one
        } else {
            Flt64.zero
        }
    }

    override fun value(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return if (polynomials.any {
                val thisValue = it.value(results, tokenList, zeroIfNone) ?: return null
                thisValue neq Flt64.zero
            }) {
            Flt64.one
        } else {
            Flt64.zero
        }
    }
}
