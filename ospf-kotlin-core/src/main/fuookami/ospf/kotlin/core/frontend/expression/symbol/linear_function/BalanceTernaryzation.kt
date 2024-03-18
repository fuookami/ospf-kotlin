package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.geometry.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

class BalanceTernaryzationFunction(
    val x: AbstractLinearPolynomial<*>,
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
    private lateinit var b: PctVariable1
    private lateinit var y: BinVariable1
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

    override val dependencies by x::dependencies
    override val cells get() = polyY.cells
    override val cached
        get() = if (::polyY.isInitialized) {
            polyY.cached
        } else {
            false
        }

    private val possibleRange
        get() = ValueRange(
            if (x.lowerBound ls Flt64.zero) {
                -Int8.one
            } else if (x.lowerBound eq Flt64.zero) {
                Int8.zero
            } else {
                Int8.one
            },
            if (x.upperBound ls Flt64.zero) {
                -Int8.one
            } else if (x.upperBound eq Flt64.zero) {
                Int8.zero
            } else {
                Int8.one
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
        if (x.discrete && x.range.range in ValueRange(-Flt64.one, Flt64.one)) {
            polyY = x
            return ok
        }

        if (extract && !x.discrete) {
            if (piecewise || epsilon geq piecewiseThreshold) {
                if (!::piecewiseFunction.isInitialized) {
                    piecewiseFunction = UnivariateLinearPiecewiseFunction(
                        x,
                        listOf(
                            Point2(x.lowerBound, -Flt64.one),
                            Point2(-epsilon, -Flt64.one),
                            Point2(-epsilon + Flt32.decimalPrecision.toFlt64(), Flt64.zero),
                            Point2(epsilon - Flt32.decimalPrecision.toFlt64(), Flt64.zero),
                            Point2(epsilon, Flt64.one),
                            Point2(x.upperBound, Flt64.one)
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
                    b = PctVariable1(name = "${name}_b", Shape1(2))
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
                polyY = piecewiseFunction.b.last() - piecewiseFunction.b.first()
                polyY.range.set(possibleRange.toFlt64())
            }
        } else {
            if (!::y.isInitialized) {
                y = BinVariable1("${name}_y", Shape1(2))
                y[0].range.leq(x.lowerBound ls Flt64.zero)
                y[1].range.leq(x.upperBound gr Flt64.zero)
            }
            when (val result = tokenTable.add(y)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }

            if (!::polyY.isInitialized) {
                polyY = y[1] - y[0]
                polyY.range.set(possibleRange.toFlt64())
            }
        }

        return ok
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
                x eq x.lowerBound * b[0] + x.upperBound * b[1],
                "${name}_xb"
            )

            model.addConstraint(
                y[1] geq b[1],
                "${name}_yb_pos_lb"
            )

            model.addConstraint(
                y[1] leq (Flt64.one / epsilon) * b[1],
                "${name}_yb_pos_ub"
            )

            model.addConstraint(
                y[0] geq b[0],
                "${name}_yb_neg_lb"
            )

            model.addConstraint(
                y[0] leq (Flt64.one / epsilon) * b[0],
                "${name}_yb_neg_ub"
            )

            model.addConstraint(
                b[0] + y[1] leq Flt64.one,
                "${name}_yb_pos"
            )

            model.addConstraint(
                b[1] + y[0] leq Flt64.one,
                "${name}_yb_neg"
            )
        } else if (::y.isInitialized) {
            model.addConstraint(
                x.upperBound * y[1] geq x,
                "${name}_plb"
            )

            model.addConstraint(
                x.lowerBound * y[0] leq x,
                "${name}_nlb"
            )

            if (extract) {
                model.addConstraint(
                    x geq (x.lowerBound - Flt64.one) * (Flt64.one - y[1]) + Flt64.one,
                    "${name}_pub"
                )

                model.addConstraint(
                    x leq (x.upperBound + Flt64.one) * (Flt64.one - y[0]) - Flt64.one,
                    "${name}_nlb"
                )

                model.addConstraint(
                    sum(y) leq Flt64.one,
                    "${name}_y"
                )
            }
        }

        return ok
    }

    override fun toString(): String {
        return displayName ?: name
    }

    override fun toRawString(unfold: Boolean): String {
        return "bter(${x.toRawString(unfold)})"
    }

    override fun value(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        val value = x.value(tokenList, zeroIfNone)
            ?: return null
        return if (value ls Flt64.zero) {
            -Flt64.one
        } else if (value gr Flt64.zero) {
            Flt64.one
        } else {
            Flt64.zero
        }
    }

    override fun value(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        val value = x.value(results, tokenList, zeroIfNone)
            ?: return null
        return if (value ls Flt64.zero) {
            -Flt64.one
        } else if (value gr Flt64.zero) {
            Flt64.one
        } else {
            Flt64.zero
        }
    }
}
