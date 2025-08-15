package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function

import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
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
    val neg: AbstractLinearPolynomial<*>? by lazy {
        _neg?.let { LinearPolynomial(it) }
    }

    private val _pos: V? by lazy {
        if (withPositive) {
            ctor("${name}_pos")
        } else {
            null
        }
    }
    val pos: AbstractLinearPolynomial<*>? by lazy {
        _pos?.let { LinearPolynomial(it) }
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

    private val polyY: AbstractLinearPolynomial<*> by lazy {
        val polyY = if (neg != null && pos != null) {
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
        polyY
    }

    override val range get() = polyY.range
    override val lowerBound get() = polyY.lowerBound
    override val upperBound get() = polyY.upperBound

    override val category = Linear

    override val dependencies: Set<IntermediateSymbol>
        get() {
            val dependencies = HashSet<IntermediateSymbol>()
            dependencies.addAll(x.dependencies)
            dependencies.addAll(y.dependencies)
            return dependencies
        }
    override val cells get() = polyY.cells
    override val cached get() = polyY.cached

    private val possibleRange: ValueRange<Flt64>
        get() {
            return try {
                if (withNegative && withPositive) {
                    val max = max(
                        y.upperBound!!.value.unwrap() - x.lowerBound!!.value.unwrap(),
                        x.upperBound!!.value.unwrap() - y.lowerBound!!.value.unwrap()
                    )
                    if (max leq Flt64.zero) {
                        ValueRange(Flt64.zero, Flt64.zero).value!!
                    } else {
                        ValueRange(Flt64.zero, max).value!!
                    }
                } else if (withNegative) {
                    val max = y.upperBound!!.value.unwrap() - x.lowerBound!!.value.unwrap()
                    if (max leq Flt64.zero) {
                        ValueRange(Flt64.zero, Flt64.zero).value!!
                    } else {
                        ValueRange(Flt64.zero, max).value!!
                    }
                } else if (withPositive) {
                    val max = x.upperBound!!.value.unwrap() - y.lowerBound!!.value.unwrap()
                    if (max leq Flt64.zero) {
                        ValueRange(Flt64.zero, Flt64.zero).value!!
                    } else {
                        ValueRange(Flt64.zero, max).value!!
                    }
                } else {
                    ValueRange(Flt64.zero, Flt64.zero).value!!
                }
            } catch (e: Exception) {
                e.printStackTrace()
                ValueRange(Flt64.zero, Flt64.zero).value!!
            }
        }

    override fun flush(force: Boolean) {
        x.flush(force)
        y.flush(force)
        neg?.flush(force)
        pos?.flush(force)
        polyX.flush(force)
        polyY.flush(force)
        polyY.range.set(possibleRange)

        if (_neg != null && neg != null) {
            when (_neg) {
                is UIntVar -> {
                    (_neg!! as UIntVar).range.set(
                        ValueRange(
                            neg!!.lowerBound!!.value.unwrap().toUInt64(),
                            neg!!.upperBound!!.value.unwrap().toUInt64()
                        ).value!!
                    )
                }

                is URealVar -> {
                    (_neg!! as URealVar).range.set(
                        ValueRange(
                            neg!!.lowerBound!!.value.unwrap(),
                            neg!!.upperBound!!.value.unwrap()
                        ).value!!
                    )
                }
            }
        }

        if (_pos != null && pos != null) {
            when (_pos) {
                is UIntVar -> {
                    (_pos!! as UIntVar).range.set(
                        ValueRange(
                            pos!!.lowerBound!!.value.unwrap().toUInt64(),
                            pos!!.upperBound!!.value.unwrap().toUInt64()
                        ).value!!
                    )
                }

                is URealVar -> {
                    (_pos!! as URealVar).range.set(
                        ValueRange(
                            pos!!.lowerBound!!.value.unwrap(),
                            pos!!.upperBound!!.value.unwrap()
                        ).value!!
                    )
                }
            }
        }
    }

    override fun prepare(tokenTable: AbstractTokenTable): Flt64? {
        x.cells
        y.cells

        return if (tokenTable.cachedSolution && tokenTable.cached(this) == false) {
            val xValue = x.evaluate(tokenTable) ?: return null
            val yValue = y.evaluate(tokenTable) ?: return null
            val negValue = max(Flt64.zero, yValue - xValue)
            val posValue = max(Flt64.zero, xValue - yValue)

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

            negValue + posValue
        } else {
            null
        }
    }

    override fun register(tokenTable: AbstractMutableTokenTable): Try {
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

    override fun register(model: AbstractLinearMechanismModel): Try {
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

    override fun toRawString(unfold: UInt64): String {
        return if (unfold eq UInt64.zero) {
            displayName ?: name
        } else {
            "slack(${x.toTidyRawString(unfold - UInt64.one)}, ${y.toTidyRawString(unfold - UInt64.one)})"
        }
    }

    override fun evaluate(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        val xValue = x.evaluate(tokenList, zeroIfNone) ?: return null
        val yValue = y.evaluate(tokenList, zeroIfNone) ?: return null
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

    override fun evaluate(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        val xValue = x.evaluate(results, tokenList, zeroIfNone) ?: return null
        val yValue = y.evaluate(results, tokenList, zeroIfNone) ?: return null
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
        val xValue = x.evaluate(tokenTable, zeroIfNone) ?: return null
        val yValue = y.evaluate(tokenTable, zeroIfNone) ?: return null
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
        val xValue = x.evaluate(results, tokenTable, zeroIfNone) ?: return null
        val yValue = y.evaluate(results, tokenTable, zeroIfNone) ?: return null
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
        x: AbstractVariableItem<*, *>,
        y: Int,
        withNegative: Boolean = true,
        withPositive: Boolean = true,
        threshold: Boolean = false,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            LinearPolynomial(x),
            LinearPolynomial(y),
            withNegative,
            withPositive,
            threshold,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: AbstractVariableItem<*, *>,
        y: Double,
        withNegative: Boolean = true,
        withPositive: Boolean = true,
        threshold: Boolean = false,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            LinearPolynomial(x),
            LinearPolynomial(y),
            withNegative,
            withPositive,
            threshold,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: AbstractVariableItem<*, *>,
        y: Boolean,
        withNegative: Boolean = true,
        withPositive: Boolean = true,
        threshold: Boolean = false,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            LinearPolynomial(x),
            LinearPolynomial(y),
            withNegative,
            withPositive,
            threshold,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: AbstractVariableItem<*, *>,
        y: Trivalent,
        withNegative: Boolean = true,
        withPositive: Boolean = true,
        threshold: Boolean = false,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            LinearPolynomial(x),
            LinearPolynomial(y),
            withNegative,
            withPositive,
            threshold,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: AbstractVariableItem<*, *>,
        y: BalancedTrivalent,
        withNegative: Boolean = true,
        withPositive: Boolean = true,
        threshold: Boolean = false,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            LinearPolynomial(x),
            LinearPolynomial(y),
            withNegative,
            withPositive,
            threshold,
            constraint,
            name,
            displayName
        )
    }

    operator fun <T : RealNumber<T>> invoke(
        type: VariableType<*> = UInteger,
        x: AbstractVariableItem<*, *>,
        y: T,
        withNegative: Boolean = true,
        withPositive: Boolean = true,
        threshold: Boolean = false,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            LinearPolynomial(x),
            LinearPolynomial(y),
            withNegative,
            withPositive,
            threshold,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: AbstractVariableItem<*, *>,
        y: AbstractVariableItem<*, *>,
        withNegative: Boolean = true,
        withPositive: Boolean = true,
        threshold: Boolean = false,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            LinearPolynomial(x),
            LinearPolynomial(y),
            withNegative,
            withPositive,
            threshold,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: AbstractVariableItem<*, *>,
        y: LinearIntermediateSymbol,
        withNegative: Boolean = true,
        withPositive: Boolean = true,
        threshold: Boolean = false,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            LinearPolynomial(x),
            LinearPolynomial(y),
            withNegative,
            withPositive,
            threshold,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: AbstractVariableItem<*, *>,
        y: LinearMonomial,
        withNegative: Boolean = true,
        withPositive: Boolean = true,
        threshold: Boolean = false,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            LinearPolynomial(x),
            LinearPolynomial(y),
            withNegative,
            withPositive,
            threshold,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: AbstractVariableItem<*, *>,
        y: AbstractLinearPolynomial<*>,
        withNegative: Boolean = true,
        withPositive: Boolean = true,
        threshold: Boolean = false,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            LinearPolynomial(x),
            y,
            withNegative,
            withPositive,
            threshold,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: LinearIntermediateSymbol,
        y: Int,
        withNegative: Boolean = true,
        withPositive: Boolean = true,
        threshold: Boolean = false,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            LinearPolynomial(x),
            LinearPolynomial(y),
            withNegative,
            withPositive,
            threshold,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: LinearIntermediateSymbol,
        y: Double,
        withNegative: Boolean = true,
        withPositive: Boolean = true,
        threshold: Boolean = false,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            LinearPolynomial(x),
            LinearPolynomial(y),
            withNegative,
            withPositive,
            threshold,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: LinearIntermediateSymbol,
        y: Boolean,
        withNegative: Boolean = true,
        withPositive: Boolean = true,
        threshold: Boolean = false,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            LinearPolynomial(x),
            LinearPolynomial(y),
            withNegative,
            withPositive,
            threshold,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: LinearIntermediateSymbol,
        y: Trivalent,
        withNegative: Boolean = true,
        withPositive: Boolean = true,
        threshold: Boolean = false,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            LinearPolynomial(x),
            LinearPolynomial(y),
            withNegative,
            withPositive,
            threshold,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: LinearIntermediateSymbol,
        y: BalancedTrivalent,
        withNegative: Boolean = true,
        withPositive: Boolean = true,
        threshold: Boolean = false,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            LinearPolynomial(x),
            LinearPolynomial(y),
            withNegative,
            withPositive,
            threshold,
            constraint,
            name,
            displayName
        )
    }

    operator fun <T : RealNumber<T>> invoke(
        type: VariableType<*> = UInteger,
        x: LinearIntermediateSymbol,
        y: T,
        withNegative: Boolean = true,
        withPositive: Boolean = true,
        threshold: Boolean = false,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            LinearPolynomial(x),
            LinearPolynomial(y),
            withNegative,
            withPositive,
            threshold,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: LinearIntermediateSymbol,
        y: AbstractVariableItem<*, *>,
        withNegative: Boolean = true,
        withPositive: Boolean = true,
        threshold: Boolean = false,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            LinearPolynomial(x),
            LinearPolynomial(y),
            withNegative,
            withPositive,
            threshold,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: LinearIntermediateSymbol,
        y: LinearIntermediateSymbol,
        withNegative: Boolean = true,
        withPositive: Boolean = true,
        threshold: Boolean = false,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            LinearPolynomial(x),
            LinearPolynomial(y),
            withNegative,
            withPositive,
            threshold,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: LinearIntermediateSymbol,
        y: LinearMonomial,
        withNegative: Boolean = true,
        withPositive: Boolean = true,
        threshold: Boolean = false,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            LinearPolynomial(x),
            LinearPolynomial(y),
            withNegative,
            withPositive,
            threshold,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: LinearIntermediateSymbol,
        y: AbstractLinearPolynomial<*>,
        withNegative: Boolean = true,
        withPositive: Boolean = true,
        threshold: Boolean = false,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            LinearPolynomial(x),
            y,
            withNegative,
            withPositive,
            threshold,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: LinearMonomial,
        y: Int,
        withNegative: Boolean = true,
        withPositive: Boolean = true,
        threshold: Boolean = false,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            LinearPolynomial(x),
            LinearPolynomial(y),
            withNegative,
            withPositive,
            threshold,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: LinearMonomial,
        y: Double,
        withNegative: Boolean = true,
        withPositive: Boolean = true,
        threshold: Boolean = false,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            LinearPolynomial(x),
            LinearPolynomial(y),
            withNegative,
            withPositive,
            threshold,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: LinearMonomial,
        y: Boolean,
        withNegative: Boolean = true,
        withPositive: Boolean = true,
        threshold: Boolean = false,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            LinearPolynomial(x),
            LinearPolynomial(y),
            withNegative,
            withPositive,
            threshold,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: LinearMonomial,
        y: Trivalent,
        withNegative: Boolean = true,
        withPositive: Boolean = true,
        threshold: Boolean = false,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            LinearPolynomial(x),
            LinearPolynomial(y),
            withNegative,
            withPositive,
            threshold,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: LinearMonomial,
        y: BalancedTrivalent,
        withNegative: Boolean = true,
        withPositive: Boolean = true,
        threshold: Boolean = false,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            LinearPolynomial(x),
            LinearPolynomial(y),
            withNegative,
            withPositive,
            threshold,
            constraint,
            name,
            displayName
        )
    }

    operator fun <T : RealNumber<T>> invoke(
        type: VariableType<*> = UInteger,
        x: LinearMonomial,
        y: T,
        withNegative: Boolean = true,
        withPositive: Boolean = true,
        threshold: Boolean = false,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            LinearPolynomial(x),
            LinearPolynomial(y),
            withNegative,
            withPositive,
            threshold,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: LinearMonomial,
        y: AbstractVariableItem<*, *>,
        withNegative: Boolean = true,
        withPositive: Boolean = true,
        threshold: Boolean = false,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            LinearPolynomial(x),
            LinearPolynomial(y),
            withNegative,
            withPositive,
            threshold,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: LinearMonomial,
        y: LinearIntermediateSymbol,
        withNegative: Boolean = true,
        withPositive: Boolean = true,
        threshold: Boolean = false,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            LinearPolynomial(x),
            LinearPolynomial(y),
            withNegative,
            withPositive,
            threshold,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: LinearMonomial,
        y: LinearMonomial,
        withNegative: Boolean = true,
        withPositive: Boolean = true,
        threshold: Boolean = false,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            LinearPolynomial(x),
            LinearPolynomial(y),
            withNegative,
            withPositive,
            threshold,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: LinearMonomial,
        y: AbstractLinearPolynomial<*>,
        withNegative: Boolean = true,
        withPositive: Boolean = true,
        threshold: Boolean = false,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            LinearPolynomial(x),
            y,
            withNegative,
            withPositive,
            threshold,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: AbstractLinearPolynomial<*>,
        y: Int,
        withNegative: Boolean = true,
        withPositive: Boolean = true,
        threshold: Boolean = false,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            x,
            LinearPolynomial(y),
            withNegative,
            withPositive,
            threshold,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: AbstractLinearPolynomial<*>,
        y: Double,
        withNegative: Boolean = true,
        withPositive: Boolean = true,
        threshold: Boolean = false,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            x,
            LinearPolynomial(y),
            withNegative,
            withPositive,
            threshold,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: AbstractLinearPolynomial<*>,
        y: Boolean,
        withNegative: Boolean = true,
        withPositive: Boolean = true,
        threshold: Boolean = false,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            x,
            LinearPolynomial(y),
            withNegative,
            withPositive,
            threshold,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: AbstractLinearPolynomial<*>,
        y: Trivalent,
        withNegative: Boolean = true,
        withPositive: Boolean = true,
        threshold: Boolean = false,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            x,
            LinearPolynomial(y),
            withNegative,
            withPositive,
            threshold,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: AbstractLinearPolynomial<*>,
        y: BalancedTrivalent,
        withNegative: Boolean = true,
        withPositive: Boolean = true,
        threshold: Boolean = false,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            x,
            LinearPolynomial(y),
            withNegative,
            withPositive,
            threshold,
            constraint,
            name,
            displayName
        )
    }

    operator fun <T : RealNumber<T>> invoke(
        type: VariableType<*> = UInteger,
        x: AbstractLinearPolynomial<*>,
        y: T,
        withNegative: Boolean = true,
        withPositive: Boolean = true,
        threshold: Boolean = false,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            x,
            LinearPolynomial(y),
            withNegative,
            withPositive,
            threshold,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: AbstractLinearPolynomial<*>,
        y: AbstractVariableItem<*, *>,
        withNegative: Boolean = true,
        withPositive: Boolean = true,
        threshold: Boolean = false,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            x,
            LinearPolynomial(y),
            withNegative,
            withPositive,
            threshold,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: AbstractLinearPolynomial<*>,
        y: LinearIntermediateSymbol,
        withNegative: Boolean = true,
        withPositive: Boolean = true,
        threshold: Boolean = false,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            x,
            LinearPolynomial(y),
            withNegative,
            withPositive,
            threshold,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: AbstractLinearPolynomial<*>,
        y: LinearMonomial,
        withNegative: Boolean = true,
        withPositive: Boolean = true,
        threshold: Boolean = false,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            x,
            LinearPolynomial(y),
            withNegative,
            withPositive,
            threshold,
            constraint,
            name,
            displayName
        )
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

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: AbstractVariableItem<*, *>,
        threshold: Int,
        withPositive: Boolean = true,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            LinearPolynomial(x),
            LinearPolynomial(threshold),
            withPositive,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: AbstractVariableItem<*, *>,
        threshold: Double,
        withPositive: Boolean = true,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            LinearPolynomial(x),
            LinearPolynomial(threshold),
            withPositive,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: AbstractVariableItem<*, *>,
        threshold: Boolean,
        withPositive: Boolean = true,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            LinearPolynomial(x),
            LinearPolynomial(threshold),
            withPositive,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: AbstractVariableItem<*, *>,
        threshold: Trivalent,
        withPositive: Boolean = true,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            LinearPolynomial(x),
            LinearPolynomial(threshold),
            withPositive,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: AbstractVariableItem<*, *>,
        threshold: BalancedTrivalent,
        withPositive: Boolean = true,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            LinearPolynomial(x),
            LinearPolynomial(threshold),
            withPositive,
            constraint,
            name,
            displayName
        )
    }

    operator fun <T : RealNumber<T>> invoke(
        type: VariableType<*> = UInteger,
        x: AbstractVariableItem<*, *>,
        threshold: T,
        withPositive: Boolean = true,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            LinearPolynomial(x),
            LinearPolynomial(threshold),
            withPositive,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: AbstractVariableItem<*, *>,
        threshold: AbstractVariableItem<*, *>,
        withPositive: Boolean = true,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            LinearPolynomial(x),
            LinearPolynomial(threshold),
            withPositive,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: AbstractVariableItem<*, *>,
        threshold: LinearIntermediateSymbol,
        withPositive: Boolean = true,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            LinearPolynomial(x),
            LinearPolynomial(threshold),
            withPositive,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: AbstractVariableItem<*, *>,
        threshold: LinearMonomial,
        withPositive: Boolean = true,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            LinearPolynomial(x),
            LinearPolynomial(threshold),
            withPositive,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: AbstractVariableItem<*, *>,
        threshold: AbstractLinearPolynomial<*>,
        withPositive: Boolean = true,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            LinearPolynomial(x),
            threshold,
            withPositive,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: LinearIntermediateSymbol,
        threshold: Int,
        withPositive: Boolean = true,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            LinearPolynomial(x),
            LinearPolynomial(threshold),
            withPositive,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: LinearIntermediateSymbol,
        threshold: Double,
        withPositive: Boolean = true,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            LinearPolynomial(x),
            LinearPolynomial(threshold),
            withPositive,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: LinearIntermediateSymbol,
        threshold: Boolean,
        withPositive: Boolean = true,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            LinearPolynomial(x),
            LinearPolynomial(threshold),
            withPositive,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: LinearIntermediateSymbol,
        threshold: Trivalent,
        withPositive: Boolean = true,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            LinearPolynomial(x),
            LinearPolynomial(threshold),
            withPositive,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: LinearIntermediateSymbol,
        threshold: BalancedTrivalent,
        withPositive: Boolean = true,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            LinearPolynomial(x),
            LinearPolynomial(threshold),
            withPositive,
            constraint,
            name,
            displayName
        )
    }

    operator fun <T : RealNumber<T>> invoke(
        type: VariableType<*> = UInteger,
        x: LinearIntermediateSymbol,
        threshold: T,
        withPositive: Boolean = true,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            LinearPolynomial(x),
            LinearPolynomial(threshold),
            withPositive,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: LinearIntermediateSymbol,
        threshold: AbstractVariableItem<*, *>,
        withPositive: Boolean = true,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            LinearPolynomial(x),
            LinearPolynomial(threshold),
            withPositive,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: LinearIntermediateSymbol,
        threshold: LinearIntermediateSymbol,
        withPositive: Boolean = true,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            LinearPolynomial(x),
            LinearPolynomial(threshold),
            withPositive,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: LinearIntermediateSymbol,
        threshold: LinearMonomial,
        withPositive: Boolean = true,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            LinearPolynomial(x),
            LinearPolynomial(threshold),
            withPositive,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: LinearIntermediateSymbol,
        threshold: AbstractLinearPolynomial<*>,
        withPositive: Boolean = true,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            LinearPolynomial(x),
            threshold,
            withPositive,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: LinearMonomial,
        threshold: Int,
        withPositive: Boolean = true,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            LinearPolynomial(x),
            LinearPolynomial(threshold),
            withPositive,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: LinearMonomial,
        threshold: Double,
        withPositive: Boolean = true,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            LinearPolynomial(x),
            LinearPolynomial(threshold),
            withPositive,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: LinearMonomial,
        threshold: Boolean,
        withPositive: Boolean = true,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            LinearPolynomial(x),
            LinearPolynomial(threshold),
            withPositive,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: LinearMonomial,
        threshold: Trivalent,
        withPositive: Boolean = true,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            LinearPolynomial(x),
            LinearPolynomial(threshold),
            withPositive,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: LinearMonomial,
        threshold: BalancedTrivalent,
        withPositive: Boolean = true,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            LinearPolynomial(x),
            LinearPolynomial(threshold),
            withPositive,
            constraint,
            name,
            displayName
        )
    }

    operator fun <T : RealNumber<T>> invoke(
        type: VariableType<*> = UInteger,
        x: LinearMonomial,
        threshold: T,
        withPositive: Boolean = true,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            LinearPolynomial(x),
            LinearPolynomial(threshold),
            withPositive,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: LinearMonomial,
        threshold: AbstractVariableItem<*, *>,
        withPositive: Boolean = true,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            LinearPolynomial(x),
            LinearPolynomial(threshold),
            withPositive,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: LinearMonomial,
        threshold: LinearIntermediateSymbol,
        withPositive: Boolean = true,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            LinearPolynomial(x),
            LinearPolynomial(threshold),
            withPositive,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: LinearMonomial,
        threshold: LinearMonomial,
        withPositive: Boolean = true,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            LinearPolynomial(x),
            LinearPolynomial(threshold),
            withPositive,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: LinearMonomial,
        threshold: AbstractLinearPolynomial<*>,
        withPositive: Boolean = true,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            LinearPolynomial(x),
            threshold,
            withPositive,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: AbstractLinearPolynomial<*>,
        threshold: Int,
        withPositive: Boolean = true,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            x,
            LinearPolynomial(threshold),
            withPositive,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: AbstractLinearPolynomial<*>,
        threshold: Double,
        withPositive: Boolean = true,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            x,
            LinearPolynomial(threshold),
            withPositive,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: AbstractLinearPolynomial<*>,
        threshold: Boolean,
        withPositive: Boolean = true,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            x,
            LinearPolynomial(threshold),
            withPositive,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: AbstractLinearPolynomial<*>,
        threshold: Trivalent,
        withPositive: Boolean = true,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            x,
            LinearPolynomial(threshold),
            withPositive,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: AbstractLinearPolynomial<*>,
        threshold: BalancedTrivalent,
        withPositive: Boolean = true,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            x,
            LinearPolynomial(threshold),
            withPositive,
            constraint,
            name,
            displayName
        )
    }

    operator fun <T : RealNumber<T>> invoke(
        type: VariableType<*> = UInteger,
        x: AbstractLinearPolynomial<*>,
        threshold: T,
        withPositive: Boolean = true,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            x,
            LinearPolynomial(threshold),
            withPositive,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: AbstractLinearPolynomial<*>,
        threshold: AbstractVariableItem<*, *>,
        withPositive: Boolean = true,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            x,
            LinearPolynomial(threshold),
            withPositive,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: AbstractLinearPolynomial<*>,
        threshold: LinearIntermediateSymbol,
        withPositive: Boolean = true,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            x,
            LinearPolynomial(threshold),
            withPositive,
            constraint,
            name,
            displayName
        )
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: AbstractLinearPolynomial<*>,
        threshold: LinearMonomial,
        withPositive: Boolean = true,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return invoke(
            type,
            x,
            LinearPolynomial(threshold),
            withPositive,
            constraint,
            name,
            displayName
        )
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
    { UIntVar(it) }
) {
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
    { URealVar(it) }
)
