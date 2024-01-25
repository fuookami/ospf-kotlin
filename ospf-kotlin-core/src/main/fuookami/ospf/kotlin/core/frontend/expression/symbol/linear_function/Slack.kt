package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

sealed class AbstractSlackFunction<V : Variable<*>>(
    private val x: AbstractLinearPolynomial<*>,
    private val y: AbstractLinearPolynomial<*>,
    private val withNegative: Boolean = true,
    private val withPositive: Boolean = true,
    private val threshold: Boolean = false,
    override var name: String,
    override var displayName: String? = null,
    private val ctor: (String) -> V
) : LinearFunctionSymbol {
    init {
        assert(withNegative || withPositive)
    }

    private lateinit var _neg: V
    val neg: V get() {
        if (withNegative && !::_neg.isInitialized) {
            _neg = ctor("${name}_neg")
        }
        return _neg
    }
    private lateinit var _pos: V
    val pos: V get() {
        if (withPositive && !::_pos.isInitialized) {
            _pos = ctor("${name}_pos")
        }
        return _pos
    }
    private lateinit var polyY: LinearPolynomial

    override val range get() = polyY.range
    override val lowerBound get() = polyY.lowerBound
    override val upperBound get() = polyY.upperBound

    override val cells get() = polyY.cells
    override val cached get() = polyY.cached

    private val possibleRange: ValueRange<Flt64> get() {
            return if (withNegative && withPositive) {
                val (min ,max) = minmax(y.upperBound - x.lowerBound, x.upperBound - y.lowerBound)
                ValueRange(min, max, Flt64)
            } else if (withNegative) {
                ValueRange(Flt64.zero, y.upperBound - x.lowerBound, Flt64)
            } else if (withPositive) {
                ValueRange(Flt64.zero, x.upperBound - y.lowerBound, Flt64)
            } else {
                ValueRange(Flt64.zero, Flt64.zero, Flt64)
            }
        }

    override fun flush(force: Boolean) {
        polyY.flush(force)
        polyY.range.set(possibleRange)
    }

    override fun register(tokenTable: MutableTokenTable<LinearMonomialCell, Linear>): Try {
        if (withNegative) {
            when (val result = tokenTable.add(neg)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        if (withPositive) {
            when (val result = tokenTable.add(pos)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        if (!::polyY.isInitialized) {
            polyY = if (withNegative && withPositive) {
                neg + pos
            } else if (withNegative) {
                LinearPolynomial(neg)
            } else if (withPositive) {
                LinearPolynomial(pos)
            } else {
                LinearPolynomial()
            }
            polyY.name = "${name}_y"
            polyY.range.set(possibleRange)
        }

        return Ok(success)
    }

    override fun register(model: Model<LinearMonomialCell, Linear>): Try {
        if (x.range.range.intersect(y.range.range).empty) {
            return Failed(Err(
                ErrorCode.ApplicationFailed,
                "$name's domain of definition unsatisfied: $x's domain is without intersection with $y's domain"
            ))
        }

        val lhs = if (withNegative && withPositive) {
            x + neg - pos
        } else if (withNegative) {
            x + neg
        } else if (withPositive) {
            x - pos
        } else {
            x
        }

        if (threshold) {
            if (withNegative) {
                model.addConstraint(lhs leq y, name)
            } else if (withPositive) {
                model.addConstraint(lhs geq y, name)
            }
        } else {
            model.addConstraint(lhs eq y, name)
        }

        return Ok(success)
    }

    override fun toString(): String {
        return displayName ?: name
    }

    override fun toRawString(unfold: Boolean): String {
        return "slack(${x.toRawString(unfold)}, ${y.toRawString(unfold)})"
    }

    override fun value(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        val xValue = x.value(tokenList, zeroIfNone) ?: return null
        val yValue = y.value(tokenList, zeroIfNone) ?: return null
        return if (withNegative && withPositive) {
            abs(xValue - yValue)
        } else if (withNegative) {
            max(Flt64.zero, yValue - xValue)
        } else if (withPositive) {
            max(Flt64.zero, xValue - yValue)
        } else {
            Flt64.zero
        }
    }

    override fun value(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        val xValue = x.value(results, tokenList, zeroIfNone) ?: return null
        val yValue = y.value(results, tokenList, zeroIfNone) ?: return null
        return if (withNegative && withPositive) {
            abs(xValue - yValue)
        } else if (withNegative) {
            max(Flt64.zero, yValue - xValue)
        } else if (withPositive) {
            max(Flt64.zero, xValue - yValue)
        } else {
            Flt64.zero
        }
    }
}

object SlackFunction {
    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: AbstractLinearPolynomial<*>,
        y: AbstractLinearPolynomial<*>,
        withNegative: Boolean = true,
        withPositive: Boolean = true,
        threshold: Boolean = false,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return if (type.isIntegerType) {
            UIntegerSlackFunction(x, y, withNegative, withPositive, threshold, name, displayName)
        } else {
            URealSlackFunction(x, y, withNegative, withPositive, threshold, name, displayName)
        }
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: AbstractLinearPolynomial<*>,
        threshold: AbstractLinearPolynomial<*>,
        withPositive: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return if (type.isIntegerType) {
            UIntegerSlackFunction(x, threshold, withNegative = !withPositive, withPositive = withPositive, threshold = true, name, displayName)
        } else {
            URealSlackFunction(x, threshold, withNegative = !withPositive, withPositive = withPositive, threshold = true, name, displayName)
        }
    }
}

class UIntegerSlackFunction(
    x: AbstractLinearPolynomial<*>,
    y: AbstractLinearPolynomial<*>,
    withNegative: Boolean = true,
    withPositive: Boolean = true,
    threshold: Boolean = false,
    name: String,
    displayName: String? = null,
) : AbstractSlackFunction<UIntVar>(x, y, withNegative, withPositive, threshold, name, displayName, { UIntVar(it) }) {
    override val discrete = true
}

class URealSlackFunction(
    x: AbstractLinearPolynomial<*>,
    y: AbstractLinearPolynomial<*>,
    withNegative: Boolean = true,
    withPositive: Boolean = true,
    threshold: Boolean = false,
    name: String,
    displayName: String? = null,
) : AbstractSlackFunction<URealVar>(x, y, withNegative, withPositive, threshold, name, displayName, { URealVar(it) })
