package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function

import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.error.*
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
    private val logger = logger()

    private val _neg: V by lazy {
        ctor("${name}_neg")
    }
    val neg: AbstractLinearPolynomial<*> by lazy {
        LinearPolynomial(neg)
    }

    private val _pos: V by lazy {
        ctor("${name}_pos")
    }
    val pos: AbstractLinearPolynomial<*> by lazy {
        LinearPolynomial(pos)
    }

    val polyX: AbstractLinearPolynomial<*> by lazy {
        val polynomial = x + _neg - _pos
        polynomial.name = "${name}_x"
        polynomial
    }

    private val y: AbstractLinearPolynomial<*> by lazy {
        val y = LinearPolynomial(_neg + _pos, "${name}_y")
        y.range.set(possibleRange)
        y
    }

    override val range get() = y.range
    override val lowerBound get() = y.lowerBound
    override val upperBound get() = y.upperBound

    override val category: Category = Linear

    override val dependencies: Set<Symbol>
        get() {
            val dependencies = HashSet<Symbol>()
            dependencies.addAll(x.dependencies)
            dependencies.addAll(lb.dependencies)
            dependencies.addAll(ub.dependencies)
            return dependencies
        }
    override val cells get() = y.cells
    override val cached get() = y.cached

    private val possibleRange: ValueRange<Flt64>
        get() {
            val max = max(x.upperBound - ub.lowerBound, lb.upperBound - x.lowerBound)
            return ValueRange(Flt64.zero, max)
        }

    override fun flush(force: Boolean) {
        x.flush(force)
        lb.flush(force)
        ub.flush(force)
        neg.flush(force)
        pos.flush(force)
        polyX.flush(force)
        y.flush(force)
        y.range.set(possibleRange)

        when (_neg) {
            is UIntVar -> {
                (_neg as UIntVar).range.set(ValueRange(neg.lowerBound.toUInt64(), neg.upperBound.toUInt64()))
            }

            is URealVar -> {
                (_neg as URealVar).range.set(ValueRange(neg.lowerBound, neg.upperBound))
            }
        }

        when (_pos) {
            is UIntVar -> {
                (_pos as UIntVar).range.set(ValueRange(pos.lowerBound.toUInt64(), pos.upperBound.toUInt64()))
            }

            is URealVar -> {
                (_pos as URealVar).range.set(ValueRange(pos.lowerBound, pos.upperBound))
            }
        }
    }

    override suspend fun prepare(tokenTable: AbstractTokenTable) {
        x.cells
        lb.cells
        ub.cells

        if (tokenTable.cachedSolution && tokenTable.cached(this) == false) {
            val xValue = x.value(tokenTable) ?: return
            val lbValue = lb.value(tokenTable) ?: return
            val ubValue = ub.value(tokenTable) ?: return

            val posValue = if (xValue geq ubValue) {
                xValue - ubValue
            } else {
                Flt64.zero
            }
            val negValue = if (xValue leq lbValue) {
                lbValue - xValue
            } else {
                Flt64.zero
            }

            logger.trace { "Setting SlackRangeFunction ${name}.neg initial solution: $negValue" }
            tokenTable.find(_neg)?.let { token ->
                token._result = negValue
            }
            logger.trace { "Setting SlackRangeFunction ${name}.pos initial solution: $posValue" }
            tokenTable.find(_pos)?.let { token ->
                token._result = posValue
            }

            val yValue = posValue + negValue
            when (tokenTable) {
                is TokenTable -> {
                    tokenTable.cachedSymbolValue[this to null] = yValue
                }

                is MutableTokenTable -> {
                    tokenTable.cachedSymbolValue[this to null] = yValue
                }
            }
        }
    }

    override fun register(tokenTable: MutableTokenTable): Try {
        when (val result = tokenTable.add(_neg)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = tokenTable.add(_pos)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }

    override fun register(model: AbstractLinearMechanismModel): Try {
        if (x.range.range.intersect(ValueRange(lb.lowerBound, ub.upperBound)).empty) {
            return Failed(
                Err(
                    ErrorCode.ApplicationFailed,
                    "$name's domain of definition unsatisfied: $x's domain is without intersection with $y's domain"
                )
            )
        }

        if (constraint) {
            when (val result = model.addConstraint(
                polyX leq ub,
                "${name}_ub"
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }

            when (val result = model.addConstraint(
                polyX geq lb,
                "${name}_lb"
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

    override fun calculateValue(tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        val xValue = x.value(tokenTable, zeroIfNone) ?: return null
        val lbValue = lb.value(tokenTable, zeroIfNone) ?: return null
        val ubValue = ub.value(tokenTable, zeroIfNone) ?: return null
        return if (xValue ls lbValue) {
            lbValue - xValue
        } else if (xValue gr ubValue) {
            xValue - ubValue
        } else {
            Flt64.zero
        }
    }

    override fun calculateValue(results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        val xValue = x.value(results, tokenTable, zeroIfNone) ?: return null
        val lbValue = lb.value(results, tokenTable, zeroIfNone) ?: return null
        val ubValue = ub.value(results, tokenTable, zeroIfNone) ?: return null
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
