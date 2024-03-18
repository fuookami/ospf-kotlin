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

sealed class AbstractSlackRangeFunction<V : Variable<*>>(
    private val x: AbstractLinearPolynomial<*>,
    private val lb: AbstractLinearPolynomial<*>,
    private val ub: AbstractLinearPolynomial<*>,
    private val constraint: Boolean = true,
    override var name: String,
    override var displayName: String? = null,
    private val ctor: (String) -> V
) : LinearFunctionSymbol {
    val neg: V by lazy { ctor("${name}_neg") }
    val pos: V by lazy { ctor("${name}_pos") }
    val polyX: AbstractLinearPolynomial<*> by lazy {
        val polynomial = x + neg - pos
        polynomial.name = "${name}_x"
        polynomial
    }
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
            dependencies.addAll(x.dependencies)
            dependencies.addAll(lb.dependencies)
            dependencies.addAll(ub.dependencies)
            return dependencies
        }
    override val cells get() = y.cells
    override val cached
        get() = if (::y.isInitialized) {
            y.cached
        } else {
            false
        }

    private val possibleRange: ValueRange<Flt64>
        get() {
            val max = max(x.upperBound - ub.lowerBound, lb.upperBound - x.lowerBound)
            return ValueRange(Flt64.zero, max)
        }

    override fun flush(force: Boolean) {
        if (::y.isInitialized) {
            y.flush(force)
            y.range.set(possibleRange)
        }
    }

    override suspend fun prepare() {
        x.cells
        lb.cells
        ub.cells
    }

    override fun register(tokenTable: MutableTokenTable<LinearMonomialCell, Linear>): Try {
        when (val result = tokenTable.add(neg)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = tokenTable.add(pos)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        if (!::y.isInitialized) {
            y = neg + pos
            y.name = "${name}_y"
        }

        return Ok(success)
    }

    override fun register(model: AbstractLinearModel): Try {
        if (x.range.range.intersect(ValueRange(lb.lowerBound, ub.upperBound)).empty) {
            return Failed(
                Err(
                    ErrorCode.ApplicationFailed,
                    "$name's domain of definition unsatisfied: $x's domain is without intersection with $y's domain"
                )
            )
        }

        if (constraint) {
            model.addConstraint(
                polyX leq ub,
                "${name}_ub"
            )

            model.addConstraint(
                polyX geq lb,
                "${name}_lb"
            )
        }

        return Ok(success)
    }

    override fun toString(): String {
        return displayName ?: name
    }

    override fun toRawString(unfold: Boolean): String {
        return "slack_range(${x.toRawString(unfold)}, [${lb.toRawString(unfold)}, ${ub.toRawString(unfold)}])"
    }

    override fun value(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        val xValue = x.value(tokenList, zeroIfNone) ?: return null
        val lbValue = lb.value(tokenList, zeroIfNone) ?: return null
        val ubValue = ub.value(tokenList, zeroIfNone) ?: return null
        return if (xValue ls lbValue) {
            lbValue - xValue
        } else if (xValue gr ubValue) {
            xValue - ubValue
        } else {
            Flt64.zero
        }
    }

    override fun value(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        val xValue = x.value(results, tokenList, zeroIfNone) ?: return null
        val lbValue = lb.value(results, tokenList, zeroIfNone) ?: return null
        val ubValue = ub.value(results, tokenList, zeroIfNone) ?: return null
        return if (xValue ls lbValue) {
            lbValue - xValue
        } else if (xValue gr ubValue) {
            xValue - ubValue
        } else {
            Flt64.zero
        }
    }
}

object SlackRangeFunction {
    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: AbstractLinearPolynomial<*>,
        lb: AbstractLinearPolynomial<*>,
        ub: AbstractLinearPolynomial<*>,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackRangeFunction<*> {
        return if (type.isIntegerType) {
            UIntegerSlackRangeFunction(x, lb, ub, constraint, name, displayName)
        } else {
            URealSlackRangeFunction(x, lb, ub, constraint, name, displayName)
        }
    }
}

class UIntegerSlackRangeFunction(
    x: AbstractLinearPolynomial<*>,
    lb: AbstractLinearPolynomial<*>,
    ub: AbstractLinearPolynomial<*>,
    constraint: Boolean = true,
    name: String,
    displayName: String? = null,
) : AbstractSlackRangeFunction<UIntVar>(x, lb, ub, constraint, name, displayName, { UIntVar(it) }) {
    override val discrete = true
}

class URealSlackRangeFunction(
    x: AbstractLinearPolynomial<*>,
    lb: AbstractLinearPolynomial<*>,
    ub: AbstractLinearPolynomial<*>,
    constraint: Boolean = true,
    name: String,
    displayName: String? = null,
) : AbstractSlackRangeFunction<URealVar>(x, lb, ub, constraint, name, displayName, { URealVar(it) })

