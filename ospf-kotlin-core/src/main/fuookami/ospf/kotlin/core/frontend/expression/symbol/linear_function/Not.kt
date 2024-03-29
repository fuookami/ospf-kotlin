package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.geometry.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

class Not(
    private val x: AbstractLinearPolynomial<*>,
    private val extract: Boolean = true,
    private val epsilon: Flt64 = Flt64(1e-6),
    private val piecewise: Boolean = false,
    override var name: String,
    override var displayName: String? = null
) : LinearFunctionSymbol {
    companion object {
        val piecewiseThreshold: Flt64 = Flt64(1e-5)
    }

    private lateinit var piecewiseFunction: UnivariateLinearPiecewiseFunction
    private lateinit var b: PctVar
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

    override val dependencies: Set<Symbol<*, *>> by x::dependencies
    override val cells get() = polyY.cells
    override val cached
        get() = if (::polyY.isInitialized) {
            polyY.cached
        } else {
            false
        }

    private val possibleRange
        get() = ValueRange(
            if (x.upperBound eq Flt64.zero) {
                UInt8.one
            } else {
                UInt8.zero
            },
            if (x.lowerBound eq Flt64.zero) {
                UInt8.one
            } else {
                UInt8.zero
            }
        )

    override fun flush(force: Boolean) {
        if (::polyY.isInitialized) {
            polyY.flush(force)
            polyY.range.set(possibleRange.toFlt64())
        }
    }

    override suspend fun prepare() {
        x.cells
    }

    override fun register(tokenTable: MutableTokenTable<LinearMonomialCell, Linear>): Try {
        if (x.discrete && x.range.range in ValueRange(Flt64.zero, Flt64.one)) {
            polyY = x
            return Ok(success)
        }

        if (x.lowerBound ls Flt64.zero) {
            return Failed(Err(ErrorCode.ApplicationFailed, "$name's domain of definition unsatisfied: $x"))
        }

        if (extract && !x.discrete) {
            if (piecewise || epsilon geq piecewiseThreshold) {
                if (!::piecewiseFunction.isInitialized) {
                    piecewiseFunction = UnivariateLinearPiecewiseFunction(
                        x,
                        listOf(
                            Point2(Flt64.zero, Flt64.zero),
                            Point2(epsilon - Flt32.decimalPrecision.toFlt64(), Flt64.zero),
                            Point2(epsilon, Flt64.one),
                            Point2(Flt64.one, Flt64.one)
                        ),
                        "${name}_piecewise"
                    )
                }
                when (val result = piecewiseFunction.register(tokenTable)) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }
                }
            } else {
                if (!::b.isInitialized) {
                    b = PctVar("${name}_b")
                }
                when (val result = tokenTable.add(b)) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }
                }
            }
        }

        if (::piecewiseFunction.isInitialized) {
            if (!::polyY.isInitialized) {
                polyY = LinearPolynomial(piecewiseFunction.b.first())
                polyY.range.set(possibleRange.toFlt64())
            }
        } else {
            if (!::y.isInitialized) {
                y = BinVar("${name}_y")
                y.range.set(possibleRange)
            }
            when (val result = tokenTable.add(y)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }

            if (!::polyY.isInitialized) {
                polyY = LinearPolynomial(y)
                polyY.range.set(possibleRange.toFlt64())
            }
        }

        return Ok(success)
    }

    override fun register(model: AbstractLinearModel): Try {
        if (::piecewiseFunction.isInitialized) {
            when (val result = piecewiseFunction.register(model)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        } else if (::b.isInitialized) {
            model.addConstraint(
                x eq x.upperBound * b,
                "${name}_xb"
            )

            model.addConstraint(
                (Flt64.one - y) geq b,
                "${name}_lb"
            )

            model.addConstraint(
                (Flt64.one - y) leq (Flt64.one / epsilon) * b,
                "${name}_ub"
            )
        } else if (::y.isInitialized) {
            model.addConstraint(
                x.upperBound * (Flt64.one - y) geq x,
                "${name}_lb"
            )
            if (extract) {
                model.addConstraint(
                    (Flt64.one - y) leq x,
                    "${name}_ub"
                )
            }
        }

        return Ok(success)
    }

    override fun toString(): String {
        return displayName ?: name
    }

    override fun toRawString(unfold: Boolean): String {
        return "not(${x.toRawString(unfold)})"
    }

    override fun value(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        val value = x.value(tokenList, zeroIfNone)
            ?: return null
        return if (value eq Flt64.zero) {
            Flt64.one
        } else {
            Flt64.zero
        }
    }

    override fun value(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        val value = x.value(results, tokenList, zeroIfNone)
            ?: return null
        return if (value eq Flt64.zero) {
            Flt64.one
        } else {
            Flt64.zero
        }
    }
}
