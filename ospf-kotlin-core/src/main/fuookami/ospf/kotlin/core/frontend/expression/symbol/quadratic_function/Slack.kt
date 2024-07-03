package fuookami.ospf.kotlin.core.frontend.expression.symbol.quadratic_function

import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

sealed class AbstractSlackFunction<V : Variable<*>>(
    private val x: AbstractQuadraticPolynomial<*>,
    private val y: AbstractQuadraticPolynomial<*>,
    private val withNegative: Boolean = true,
    private val withPositive: Boolean = true,
    private val threshold: Boolean = false,
    private val constraint: Boolean = true,
    override var name: String,
    override var displayName: String? = null,
    private val ctor: (String) -> V
) : QuadraticFunctionSymbol {
    private val logger = logger()

    init {
        assert(withNegative || withPositive)
    }

    private val _neg: V? by lazy {
        if (withNegative) {
            ctor("${name}_neg")
        } else {
            null
        }
    }
    val neg: AbstractQuadraticPolynomial<*>? by lazy {
        _neg?.let { QuadraticPolynomial(it) }
    }

    private val _pos: V? by lazy {
        if (withPositive) {
            ctor("${name}_pos")
        } else {
            null
        }
    }
    val pos: AbstractQuadraticPolynomial<*>? by lazy {
        _pos?.let { QuadraticPolynomial(it) }
    }

    val polyX: AbstractQuadraticPolynomial<*> by lazy {
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

    private val polyY: AbstractQuadraticPolynomial<*> by lazy {
        val polyY = if (neg != null && pos != null) {
            neg!! + pos!!
        } else if (neg != null) {
            QuadraticPolynomial(neg!!)
        } else if (pos != null) {
            QuadraticPolynomial(pos!!)
        } else {
            QuadraticPolynomial()
        }
        polyY.name = "${name}_y"
        polyY.range.set(possibleRange)
        polyY
    }

    override val range get() = polyY.range
    override val lowerBound get() = polyY.lowerBound
    override val upperBound get() = polyY.upperBound

    override val category: Category = Linear

    override val dependencies: Set<Symbol>
        get() {
            val dependencies = HashSet<Symbol>()
            dependencies.addAll(x.dependencies)
            dependencies.addAll(y.dependencies)
            return dependencies
        }
    override val cells get() = polyY.cells
    override val cached get() = polyY.cached

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
        x.flush(force)
        neg?.flush(force)
        pos?.flush(force)
        polyX.flush(force)
        polyY.flush(force)
        polyY.range.set(possibleRange)

        if (_neg != null && neg != null) {
            when (_neg) {
                is UIntVar -> {
                    (_neg!! as UIntVar).range.set(ValueRange(neg!!.lowerBound.toUInt64(), neg!!.upperBound.toUInt64()))
                }

                is URealVar -> {
                    (_neg!! as URealVar).range.set(ValueRange(neg!!.lowerBound, neg!!.upperBound))
                }
            }
        }

        if (_pos != null && pos != null) {
            when (_pos) {
                is UIntVar -> {
                    (_pos!! as UIntVar).range.set(ValueRange(pos!!.lowerBound.toUInt64(), pos!!.upperBound.toUInt64()))
                }

                is URealVar -> {
                    (_pos!! as URealVar).range.set(ValueRange(pos!!.lowerBound, pos!!.upperBound))
                }
            }
        }
    }

    override suspend fun prepare(tokenTable: AbstractTokenTable) {
        x.cells
        y.cells

        if (tokenTable.cachedSolution && tokenTable.cached(this) == false) {
            val xValue = x.value(tokenTable) ?: return
            val yValue = y.value(tokenTable) ?: return
            val negValue = yValue - xValue
            val posValue = xValue - yValue

            if (_neg != null) {
                logger.trace { "Setting SlackFunction ${name}.neg initial solution: $negValue" }
                tokenTable.find(_neg!!)?.let { token ->
                    token._result = negValue
                }
            }
            if (_pos != null) {
                logger.trace { "Setting SlackFunction ${name}.pos initial solution: $posValue" }
                tokenTable.find(_pos!!)?.let { token ->
                    token._result = posValue
                }
            }

            val slackValue = negValue + posValue
            when (tokenTable) {
                is TokenTable -> {
                    tokenTable.cachedSymbolValue[this to null] = slackValue
                }

                is MutableTokenTable -> {
                    tokenTable.cachedSymbolValue[this to null] = slackValue
                }
            }
        }
    }

    override fun register(tokenTable: MutableTokenTable): Try {
        if (_neg != null) {
            when (val result = tokenTable.add(_neg!!)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        if (_pos != null) {
            when (val result = tokenTable.add(_pos!!)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        return ok
    }

    override fun register(model: AbstractQuadraticMechanismModel): Try {
        if (constraint) {
            if (threshold) {
                if (withNegative) {
                    when (val result = model.addConstraint(
                        polyX geq y,
                        name
                    )) {
                        is Ok -> {}

                        is Failed -> {
                            return Failed(result.error)
                        }
                    }
                } else if (withPositive) {
                    when (val result = model.addConstraint(
                        polyX leq y,
                        name
                    )) {
                        is Ok -> {}

                        is Failed -> {
                            return Failed(result.error)
                        }
                    }
                }
            } else {
                when (val result = model.addConstraint(
                    polyX eq y,
                    name
                )) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }
                }
            }
        }

        return ok
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

    override fun calculateValue(tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        val xValue = x.value(tokenTable, zeroIfNone) ?: return null
        val yValue = y.value(tokenTable, zeroIfNone) ?: return null
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

    override fun calculateValue(results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        val xValue = x.value(results, tokenTable, zeroIfNone) ?: return null
        val yValue = y.value(results, tokenTable, zeroIfNone) ?: return null
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
        x: AbstractQuadraticPolynomial<*>,
        y: AbstractQuadraticPolynomial<*>,
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
        x: AbstractQuadraticPolynomial<*>,
        threshold: AbstractQuadraticPolynomial<*>,
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
    x: AbstractQuadraticPolynomial<*>,
    y: AbstractQuadraticPolynomial<*>,
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
    x: AbstractQuadraticPolynomial<*>,
    y: AbstractQuadraticPolynomial<*>,
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
