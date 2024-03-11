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
    private val constraint: Boolean = true,
    override var name: String,
    override var displayName: String? = null,
    private val ctor: (String) -> V
) : LinearFunctionSymbol {
    init {
        assert(withNegative || withPositive)
    }

    val neg: V? by lazy {
        if (withNegative) {
            ctor("${name}_neg")
        } else {
            null
        }
    }
    val pos: V? by lazy {
        if (withPositive) {
            ctor("${name}_pos")
        } else {
            null
        }
    }
    val polyX: AbstractLinearPolynomial<*> by lazy {
        val polynomial = if (neg != null && pos != null) {
            x + neg!! - pos!!
        } else if (neg != null) {
            x + neg!!
        } else if (pos != null) {
            x - pos!!
        } else {
            x
        }
        polynomial.name = "${name}_x"
        polynomial
    }
    private lateinit var polyY: AbstractLinearPolynomial<*>

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
            dependencies.addAll(x.dependencies)
            dependencies.addAll(y.dependencies)
            return dependencies
        }
    override val cells get() = polyY.cells
    override val cached
        get() = if (::polyY.isInitialized) {
            polyY.cached
        } else {
            false
        }

    private val possibleRange: ValueRange<Flt64>
        get() {
            return if (withNegative && withPositive) {
                val max = max(y.upperBound - x.lowerBound, x.upperBound - y.lowerBound)
                ValueRange(Flt64.zero, max)
            } else if (withNegative) {
                ValueRange(Flt64.zero, y.upperBound - x.lowerBound)
            } else if (withPositive) {
                ValueRange(Flt64.zero, x.upperBound - y.lowerBound)
            } else {
                ValueRange(Flt64.zero, Flt64.zero)
            }
        }

    override fun flush(force: Boolean) {
        if (::polyY.isInitialized) {
            polyY.flush(force)
            polyY.range.set(possibleRange)
        }
    }

    override suspend fun prepare() {
        x.cells
        y.cells
    }

    override fun register(tokenTable: MutableTokenTable<LinearMonomialCell, Linear>): Try {
        if (neg != null) {
            when (val result = tokenTable.add(neg!!)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        if (pos != null) {
            when (val result = tokenTable.add(pos!!)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        if (!::polyY.isInitialized) {
            polyY = if (pos != null && neg != null) {
                neg!! + pos!!
            } else if (neg != null) {
                LinearPolynomial(neg!!)
            } else if (pos != null) {
                LinearPolynomial(pos!!)
            } else {
                LinearPolynomial()
            }
            polyY.name = "${name}_y"
            polyY.range.set(possibleRange)
        }

        return Ok(success)
    }

    override fun register(model: AbstractLinearModel): Try {
        if (x.range.range.intersect(y.range.range).empty) {
            return Failed(
                Err(
                    ErrorCode.ApplicationFailed,
                    "$name's domain of definition unsatisfied: $x's domain is without intersection with $y's domain"
                )
            )
        }

        if (constraint) {
            if (threshold) {
                if (withNegative) {
                    model.addConstraint(polyX geq y, name)
                } else if (withPositive) {
                    model.addConstraint(polyX leq y, name)
                }
            } else {
                model.addConstraint(polyX eq y, name)
            }
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
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return if (type.isIntegerType) {
            UIntegerSlackFunction(x, y, withNegative, withPositive, threshold, constraint, name, displayName)
        } else {
            URealSlackFunction(x, y, withNegative, withPositive, threshold, constraint, name, displayName)
        }
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: AbstractLinearPolynomial<*>,
        threshold: AbstractLinearPolynomial<*>,
        withPositive: Boolean = true,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return if (type.isIntegerType) {
            UIntegerSlackFunction(
                x,
                threshold,
                withNegative = !withPositive,
                withPositive = withPositive,
                threshold = true,
                constraint,
                name,
                displayName
            )
        } else {
            URealSlackFunction(
                x,
                threshold,
                withNegative = !withPositive,
                withPositive = withPositive,
                threshold = true,
                constraint,
                name,
                displayName
            )
        }
    }
}

class UIntegerSlackFunction(
    x: AbstractLinearPolynomial<*>,
    y: AbstractLinearPolynomial<*>,
    withNegative: Boolean = true,
    withPositive: Boolean = true,
    threshold: Boolean = false,
    constraint: Boolean = true,
    name: String,
    displayName: String? = null,
) : AbstractSlackFunction<UIntVar>(
    x,
    y,
    withNegative,
    withPositive,
    threshold,
    constraint,
    name,
    displayName,
    { UIntVar(it) }) {
    override val discrete = true
}

class URealSlackFunction(
    x: AbstractLinearPolynomial<*>,
    y: AbstractLinearPolynomial<*>,
    withNegative: Boolean = true,
    withPositive: Boolean = true,
    threshold: Boolean = false,
    constraint: Boolean = true,
    name: String,
    displayName: String? = null,
) : AbstractSlackFunction<URealVar>(
    x,
    y,
    withNegative,
    withPositive,
    threshold,
    constraint,
    name,
    displayName,
    { URealVar(it) })
