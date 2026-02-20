package fuookami.ospf.kotlin.core.frontend.expression.symbol.quadratic_function

import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

sealed class AbstractSlackFunction<V : Variable<*>>(
    private val x: AbstractQuadraticPolynomial<*>,
    private val y: AbstractQuadraticPolynomial<*>,
    val withNegative: Boolean = true,
    val withPositive: Boolean = true,
    val threshold: Boolean = false,
    val constraint: Boolean = true,
    override val parent: IntermediateSymbol? = null,
    args: Any? = null,
    override var name: String,
    override var displayName: String? = null,
    private val ctor: (String) -> V
) : QuadraticFunctionSymbol() {
    private val logger = logger()

    init {
        assert(withNegative || withPositive)
    }

    internal val _args = args
    override val args get() = _args ?: parent?.args

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
            return if (withNegative && withPositive) {
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

    override fun prepare(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable): Flt64? {
        x.cells
        y.cells

        return if ((!values.isNullOrEmpty() || tokenTable.cachedSolution) && if (values.isNullOrEmpty()) {
            tokenTable.cached(this)
        } else {
            tokenTable.cached(this, values)
        } == false) {
            val xValue = if (values.isNullOrEmpty()) {
                x.evaluate(tokenTable)
            } else {
                x.evaluate(values, tokenTable)
            } ?: return null

            val yValue = if (values.isNullOrEmpty()) {
                y.evaluate(tokenTable)
            } else {
                y.evaluate(values, tokenTable)
            } ?: return null

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

    override fun register(tokenTable: AddableTokenCollection): Try {
        when (val result = tokenTable.add(listOfNotNull(_neg, _pos))) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }

    override fun register(model: AbstractQuadraticMechanismModel): Try {
        if (constraint) {
            if (threshold) {
                if (withNegative) {
                    when (val result = model.addConstraint(
                        constraint = polyX geq y,
                        name = name,
                        from = (parent ?: this) to true
                    )) {
                        is Ok -> {}

                        is Failed -> {
                            return Failed(result.error)
                        }
                    }
                } else if (withPositive) {
                    when (val result = model.addConstraint(
                        constraint = polyX leq y,
                        name = name,
                        from = (parent ?: this) to true
                    )) {
                        is Ok -> {}

                        is Failed -> {
                            return Failed(result.error)
                        }
                    }
                }
            } else {
                when (val result = model.addConstraint(
                    constraint = polyX eq y,
                    name = name,
                    from = parent ?: this
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

    override fun register(
        tokenTable: AddableTokenCollection,
        fixedValues: Map<Symbol, Flt64>
    ): Try {
        return register(tokenTable)
    }

    override fun register(
        model: AbstractQuadraticMechanismModel,
        fixedValues: Map<Symbol, Flt64>
    ): Try {
        val xValue = x.evaluate(fixedValues, model.tokens) ?: return register(model)
        val yValue = y.evaluate(fixedValues, model.tokens) ?: return register(model)
        val negSlack = if (xValue leq yValue) {
            yValue - xValue
        } else {
            Flt64.zero
        }
        val posSlack = if (xValue geq yValue) {
            xValue - yValue
        } else {
            Flt64.zero
        }

        if (constraint) {
            if (threshold) {
                if (withNegative) {
                    when (val result = model.addConstraint(
                        polyX geq y,
                        name = name,
                        from = (parent ?: this) to true
                    )) {
                        is Ok -> {}

                        is Failed -> {
                            return Failed(result.error)
                        }
                    }
                } else if (withPositive) {
                    when (val result = model.addConstraint(
                        polyX leq y,
                        name = name,
                        from = (parent ?: this) to true
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
                    name = name,
                    from = parent ?: this
                )) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }
                }
            }
        }

        if (_neg != null) {
            model.tokens.find(_neg!!)?.let { token ->
                token._result = negSlack
            }
        }

        if (_pos != null) {
            model.tokens.find(_pos!!)?.let { token ->
                token._result = posSlack
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

    override fun evaluate(
        tokenList: AbstractTokenList,
        zeroIfNone: Boolean
    ): Flt64? {
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

    override fun evaluate(
        results: List<Flt64>,
        tokenList: AbstractTokenList,
        zeroIfNone: Boolean
    ): Flt64? {
        val xValue = x.evaluate(
            results = results,
            tokenList = tokenList,
            zeroIfNone = zeroIfNone
        ) ?: return null
        val yValue = y.evaluate(
            results = results,
            tokenList = tokenList,
            zeroIfNone = zeroIfNone
        ) ?: return null
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

    override fun evaluate(
        values: Map<Symbol, Flt64>,
        tokenList: AbstractTokenList?,
        zeroIfNone: Boolean
    ): Flt64? {
        val xValue = x.evaluate(
            values = values,
            tokenList = tokenList,
            zeroIfNone = zeroIfNone
        ) ?: return null
        val yValue = y.evaluate(
            values = values,
            tokenList = tokenList,
            zeroIfNone = zeroIfNone
        ) ?: return null
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

    override fun calculateValue(
        tokenTable: AbstractTokenTable,
        zeroIfNone: Boolean
    ): Flt64? {
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

    override fun calculateValue(
        results: List<Flt64>,
        tokenTable: AbstractTokenTable,
        zeroIfNone: Boolean
    ): Flt64? {
        val xValue = x.evaluate(
            results = results,
            tokenTable = tokenTable,
            zeroIfNone = zeroIfNone
        ) ?: return null
        val yValue = y.evaluate(
            results = results,
            tokenTable = tokenTable,
            zeroIfNone = zeroIfNone
        ) ?: return null
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

    override fun calculateValue(
        values: Map<Symbol, Flt64>,
        tokenTable: AbstractTokenTable?,
        zeroIfNone: Boolean
    ): Flt64? {
        val xValue = x.evaluate(
            values = values,
            tokenTable = tokenTable,
            zeroIfNone = zeroIfNone
        ) ?: return null
        val yValue = y.evaluate(
            values = values,
            tokenTable = tokenTable,
            zeroIfNone = zeroIfNone
        ) ?: return null
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
        x: AbstractQuadraticPolynomial<*>,
        y: AbstractQuadraticPolynomial<*>,
        type: VariableType<*> = if (x.discrete && y.discrete) {
            UInteger
        } else {
            UContinuous
        },
        withNegative: Boolean = true,
        withPositive: Boolean = true,
        threshold: Boolean = false,
        constraint: Boolean = true,
        parent: IntermediateSymbol? = null,
        args: Any? = null,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return if (type.isIntegerType) {
            UIntegerSlackFunction(
                x = x,
                y = y,
                withNegative = withNegative,
                withPositive = withPositive,
                threshold = threshold,
                constraint = constraint,
                parent = parent,
                args = args,
                name = name,
                displayName = displayName
            )
        } else {
            URealSlackFunction(
                x = x,
                y = y,
                withNegative = withNegative,
                withPositive = withPositive,
                threshold = threshold,
                constraint = constraint,
                parent = parent,
                args = args,
                name = name,
                displayName = displayName
            )
        }
    }

    operator fun <
        T : ToQuadraticPolynomial<Poly>,
        Poly : AbstractQuadraticPolynomial<Poly>
    > invoke(
        x: T,
        y: Int,
        type: VariableType<*>? = null,
        withNegative: Boolean = true,
        withPositive: Boolean = true,
        threshold: Boolean = false,
        constraint: Boolean = true,
        parent: IntermediateSymbol? = null,
        args: Any? = null,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        val xPoly = x.toQuadraticPolynomial()
        return invoke(
            x = xPoly,
            y = QuadraticPolynomial(y),
            type = type ?: if (xPoly.discrete) {
                UInteger
            } else {
                UContinuous
            },
            withNegative = withNegative,
            withPositive = withPositive,
            threshold = threshold,
            constraint = constraint,
            parent = parent,
            args = args,
            name = name,
            displayName = displayName
        )
    }

    operator fun <
        T : ToQuadraticPolynomial<Poly>,
        Poly : AbstractQuadraticPolynomial<Poly>
    > invoke(
        x: T,
        y: Double,
        type: VariableType<*>? = null,
        withNegative: Boolean = true,
        withPositive: Boolean = true,
        threshold: Boolean = false,
        constraint: Boolean = true,
        parent: IntermediateSymbol? = null,
        args: Any? = null,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        val xPoly = x.toQuadraticPolynomial()
        return invoke(
            x = xPoly,
            y = QuadraticPolynomial(y),
            type = type ?: UContinuous,
            withNegative = withNegative,
            withPositive = withPositive,
            threshold = threshold,
            constraint = constraint,
            parent = parent,
            args = args,
            name = name,
            displayName = displayName
        )
    }

    operator fun <
        T : ToQuadraticPolynomial<Poly>,
        Poly : AbstractQuadraticPolynomial<Poly>
    > invoke(
        x: T,
        y: Boolean,
        type: VariableType<*>? = null,
        withNegative: Boolean = true,
        withPositive: Boolean = true,
        threshold: Boolean = false,
        constraint: Boolean = true,
        parent: IntermediateSymbol? = null,
        args: Any? = null,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        val xPoly = x.toQuadraticPolynomial()
        return invoke(
            x = xPoly,
            y = QuadraticPolynomial(y),
            type = type ?: if (xPoly.discrete) {
                UInteger
            } else {
                UContinuous
            },
            withNegative = withNegative,
            withPositive = withPositive,
            threshold = threshold,
            constraint = constraint,
            parent = parent,
            args = args,
            name = name,
            displayName = displayName
        )
    }

    operator fun <
        T : ToQuadraticPolynomial<Poly>,
        Poly : AbstractQuadraticPolynomial<Poly>
    > invoke(
        x: T,
        y: Trivalent,
        type: VariableType<*>? = null,
        withNegative: Boolean = true,
        withPositive: Boolean = true,
        threshold: Boolean = false,
        constraint: Boolean = true,
        parent: IntermediateSymbol? = null,
        args: Any? = null,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        val xPoly = x.toQuadraticPolynomial()
        return invoke(
            x = xPoly,
            y = QuadraticPolynomial(y),
            type = type ?: if (xPoly.discrete) {
                UInteger
            } else {
                UContinuous
            },
            withNegative = withNegative,
            withPositive = withPositive,
            threshold = threshold,
            constraint = constraint,
            parent = parent,
            args = args,
            name = name,
            displayName = displayName
        )
    }

    operator fun <
        T : ToQuadraticPolynomial<Poly>,
        Poly : AbstractQuadraticPolynomial<Poly>
    > invoke(
        x: T,
        y: BalancedTrivalent,
        type: VariableType<*>? = null,
        withNegative: Boolean = true,
        withPositive: Boolean = true,
        threshold: Boolean = false,
        constraint: Boolean = true,
        parent: IntermediateSymbol? = null,
        args: Any? = null,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        val xPoly = x.toQuadraticPolynomial()
        return invoke(
            x = xPoly,
            y = QuadraticPolynomial(y),
            type = type ?: if (xPoly.discrete) {
                UInteger
            } else {
                UContinuous
            },
            withNegative = withNegative,
            withPositive = withPositive,
            threshold = threshold,
            constraint = constraint,
            parent = parent,
            args = args,
            name = name,
            displayName = displayName
        )
    }

    operator fun <
        T1 : ToQuadraticPolynomial<Poly1>,
        Poly1 : AbstractQuadraticPolynomial<Poly1>,
        T2 : RealNumber<T2>
    > invoke(
        x: T1,
        y: T2,
        type: VariableType<*>? = null,
        withNegative: Boolean = true,
        withPositive: Boolean = true,
        threshold: Boolean = false,
        constraint: Boolean = true,
        parent: IntermediateSymbol? = null,
        args: Any? = null,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        val xPoly = x.toQuadraticPolynomial()
        return invoke(
            x = xPoly,
            y = QuadraticPolynomial(y),
            type = type ?: UContinuous,
            withNegative = withNegative,
            withPositive = withPositive,
            threshold = threshold,
            constraint = constraint,
            parent = parent,
            args = args,
            name = name,
            displayName = displayName
        )
    }

    operator fun <
        T1 : ToQuadraticPolynomial<Poly1>,
        Poly1 : AbstractQuadraticPolynomial<Poly1>,
        T2 : ToQuadraticPolynomial<Poly2>,
        Poly2 : AbstractQuadraticPolynomial<Poly2>
    > invoke(
        x: T1,
        y: T2,
        type: VariableType<*>? = null,
        withNegative: Boolean = true,
        withPositive: Boolean = true,
        threshold: Boolean = false,
        constraint: Boolean = true,
        parent: IntermediateSymbol? = null,
        args: Any? = null,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        val xPoly = x.toQuadraticPolynomial()
        val yPoly = y.toQuadraticPolynomial()
        return invoke(
            x = xPoly,
            y = yPoly,
            type = type ?: if (xPoly.discrete && yPoly.discrete) {
                UInteger
            } else {
                UContinuous
            },
            withNegative = withNegative,
            withPositive = withPositive,
            threshold = threshold,
            constraint = constraint,
            parent = parent,
            args = args,
            name = name,
            displayName = displayName
        )
    }

    operator fun invoke(
        x: AbstractQuadraticPolynomial<*>,
        threshold: AbstractQuadraticPolynomial<*>,
        type: VariableType<*> = if (x.discrete && threshold.discrete) {
            UInteger
        } else {
            UContinuous
        },
        withPositive: Boolean = true,
        withNegative: Boolean? = null,
        constraint: Boolean = true,
        parent: IntermediateSymbol? = null,
        args: Any? = null,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        val positive = withNegative?.let { !it } ?: withPositive

        return if (type.isIntegerType) {
            UIntegerSlackFunction(
                x = x,
                y = threshold,
                withNegative = !positive,
                withPositive = positive,
                threshold = true,
                constraint = constraint,
                parent = parent,
                args = args,
                name = name,
                displayName = displayName
            )
        } else {
            URealSlackFunction(
                x = x,
                y = threshold,
                withNegative = !positive,
                withPositive = positive,
                threshold = true,
                constraint = constraint,
                parent = parent,
                args = args,
                name = name,
                displayName = displayName
            )
        }
    }

    operator fun <
        T : ToQuadraticPolynomial<Poly>,
        Poly : AbstractQuadraticPolynomial<Poly>
    > invoke(
        x: T,
        threshold: Int,
        type: VariableType<*>? = null,
        withPositive: Boolean = true,
        withNegative: Boolean? = null,
        constraint: Boolean = true,
        parent: IntermediateSymbol? = null,
        args: Any? = null,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        val xPoly = x.toQuadraticPolynomial()
        return invoke(
            x = xPoly,
            threshold = QuadraticPolynomial(threshold),
            type = type ?: if (xPoly.discrete) {
                UInteger
            } else {
                UContinuous
            },
            withPositive = withPositive,
            withNegative = withNegative,
            constraint = constraint,
            parent = parent,
            args = args,
            name = name,
            displayName = displayName
        )
    }

    operator fun <
        T : ToQuadraticPolynomial<Poly>,
        Poly : AbstractQuadraticPolynomial<Poly>
    > invoke(
        x: T,
        threshold: Double,
        type: VariableType<*>? = null,
        withPositive: Boolean = true,
        withNegative: Boolean? = null,
        constraint: Boolean = true,
        parent: IntermediateSymbol? = null,
        args: Any? = null,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        val xPoly = x.toQuadraticPolynomial()
        return invoke(
            x = xPoly,
            threshold = QuadraticPolynomial(threshold),
            type = type ?: UContinuous,
            withPositive = withPositive,
            withNegative = withNegative,
            constraint = constraint,
            parent = parent,
            args = args,
            name = name,
            displayName = displayName
        )
    }

    operator fun <
        T : ToQuadraticPolynomial<Poly>,
        Poly : AbstractQuadraticPolynomial<Poly>
    > invoke(
        x: T,
        threshold: Boolean,
        type: VariableType<*>? = null,
        withPositive: Boolean = true,
        withNegative: Boolean? = null,
        constraint: Boolean = true,
        parent: IntermediateSymbol? = null,
        args: Any? = null,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        val xPoly = x.toQuadraticPolynomial()
        return invoke(
            x = xPoly,
            threshold = QuadraticPolynomial(threshold),
            type = type ?: if (xPoly.discrete) {
                UInteger
            } else {
                UContinuous
            },
            withPositive = withPositive,
            withNegative = withNegative,
            constraint = constraint,
            parent = parent,
            args = args,
            name = name,
            displayName = displayName
        )
    }

    operator fun <
        T : ToQuadraticPolynomial<Poly>,
        Poly : AbstractQuadraticPolynomial<Poly>
    > invoke(
        x: T,
        threshold: Trivalent,
        type: VariableType<*>? = null,
        withPositive: Boolean = true,
        withNegative: Boolean? = null,
        constraint: Boolean = true,
        parent: IntermediateSymbol? = null,
        args: Any? = null,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        val xPoly = x.toQuadraticPolynomial()
        return invoke(
            x = xPoly,
            threshold = QuadraticPolynomial(threshold),
            type = type ?: if (xPoly.discrete) {
                UInteger
            } else {
                UContinuous
            },
            withPositive = withPositive,
            withNegative = withNegative,
            constraint = constraint,
            parent = parent,
            args = args,
            name = name,
            displayName = displayName
        )
    }

    operator fun <
        T : ToQuadraticPolynomial<Poly>,
        Poly : AbstractQuadraticPolynomial<Poly>
    > invoke(
        x: T,
        threshold: BalancedTrivalent,
        type: VariableType<*>? = null,
        withPositive: Boolean = true,
        withNegative: Boolean? = null,
        constraint: Boolean = true,
        parent: IntermediateSymbol? = null,
        args: Any? = null,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        val xPoly = x.toQuadraticPolynomial()
        return invoke(
            x = xPoly,
            threshold = QuadraticPolynomial(threshold),
            type = type ?: if (xPoly.discrete) {
                UInteger
            } else {
                UContinuous
            },
            withPositive = withPositive,
            withNegative = withNegative,
            constraint = constraint,
            parent = parent,
            args = args,
            name = name,
            displayName = displayName
        )
    }

    operator fun <
        T1 : ToQuadraticPolynomial<Poly1>,
        Poly1 : AbstractQuadraticPolynomial<Poly1>,
        T2 : RealNumber<T2>
    > invoke(
        x: T1,
        threshold: T2,
        type: VariableType<*>? = null,
        withPositive: Boolean = true,
        withNegative: Boolean? = null,
        constraint: Boolean = true,
        parent: IntermediateSymbol? = null,
        args: Any? = null,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        val xPoly = x.toQuadraticPolynomial()
        return invoke(
            x = xPoly,
            threshold = QuadraticPolynomial(threshold),
            type = type ?: UContinuous,
            withPositive = withPositive,
            withNegative = withNegative,
            constraint = constraint,
            parent = parent,
            args = args,
            name = name,
            displayName = displayName
        )
    }

    operator fun <
        T1 : ToQuadraticPolynomial<Poly1>,
        Poly1 : AbstractQuadraticPolynomial<Poly1>,
        T2 : ToQuadraticPolynomial<Poly2>,
        Poly2 : AbstractQuadraticPolynomial<Poly2>
    > invoke(
        x: T1,
        threshold: T2,
        type: VariableType<*>? = null,
        withPositive: Boolean = true,
        withNegative: Boolean? = null,
        constraint: Boolean = true,
        parent: IntermediateSymbol? = null,
        args: Any? = null,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        val xPoly = x.toQuadraticPolynomial()
        val thresholdPoly = threshold.toQuadraticPolynomial()
        return invoke(
            x = xPoly,
            threshold = thresholdPoly,
            type = type ?: if (xPoly.discrete && thresholdPoly.discrete) {
                UInteger
            } else {
                UContinuous
            },
            withPositive = withPositive,
            withNegative = withNegative,
            constraint = constraint,
            parent = parent,
            args = args,
            name = name,
            displayName = displayName
        )
    }
}

class UIntegerSlackFunction(
    x: AbstractQuadraticPolynomial<*>,
    y: AbstractQuadraticPolynomial<*>,
    withNegative: Boolean = true,
    withPositive: Boolean = true,
    threshold: Boolean = false,
    constraint: Boolean = true,
    parent: IntermediateSymbol? = null,
    args: Any? = null,
    name: String,
    displayName: String? = null,
) : AbstractSlackFunction<UIntVar>(
    x = x,
    y = y,
    withNegative = withNegative,
    withPositive = withPositive,
    threshold = threshold,
    constraint = constraint,
    parent = parent,
    args = args,
    name = name,
    displayName = displayName,
    ctor = { UIntVar(it) }
) {
    override val discrete = true
}

class URealSlackFunction(
    x: AbstractQuadraticPolynomial<*>,
    y: AbstractQuadraticPolynomial<*>,
    withNegative: Boolean = true,
    withPositive: Boolean = true,
    threshold: Boolean = false,
    constraint: Boolean = true,
    parent: IntermediateSymbol? = null,
    args: Any? = null,
    name: String,
    displayName: String? = null,
) : AbstractSlackFunction<URealVar>(
    x = x,
    y = y,
    withNegative = withNegative,
    withPositive = withPositive,
    threshold = threshold,
    constraint = constraint,
    parent = parent,
    args = args,
    name = name,
    displayName = displayName,
    ctor = { URealVar(it) }
)