package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function

import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

sealed class AbstractSlackRangeFunction<V : Variable<*>>(
    private val x: AbstractLinearPolynomial<*>,
    private val lb: AbstractLinearPolynomial<*>,
    private val ub: AbstractLinearPolynomial<*>,
    val constraint: Boolean = true,
    override val parent: IntermediateSymbol? = null,
    args: Any? = null,
    override var name: String,
    override var displayName: String? = null,
    private val ctor: (String) -> V
) : LinearFunctionSymbol() {
    private val logger = logger()

    internal val _args = args
    override val args get() = _args ?: parent?.args

    private val _neg: V by lazy {
        ctor("${name}_neg")
    }
    val neg: AbstractLinearPolynomial<*> by lazy {
        LinearPolynomial(_neg)
    }

    private val _pos: V by lazy {
        ctor("${name}_pos")
    }
    val pos: AbstractLinearPolynomial<*> by lazy {
        LinearPolynomial(_pos)
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

    override val category = Linear

    override val dependencies: Set<IntermediateSymbol>
        get() {
            val dependencies = HashSet<IntermediateSymbol>()
            dependencies.addAll(x.dependencies)
            dependencies.addAll(lb.dependencies)
            dependencies.addAll(ub.dependencies)
            return dependencies
        }
    override val cells get() = y.cells
    override val cached get() = y.cached

    private val possibleRange: ValueRange<Flt64>
        get() {
            val max = max(
                x.upperBound!!.value.unwrap() - ub.lowerBound!!.value.unwrap(),
                lb.upperBound!!.value.unwrap() - x.lowerBound!!.value.unwrap()
            )
            return if (max leq Flt64.zero) {
                ValueRange(Flt64.zero, Flt64.zero).value!!
            } else {
                ValueRange(Flt64.zero, max).value!!
            }
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
                (_neg as UIntVar).range.set(
                    ValueRange(
                        neg.lowerBound!!.value.unwrap().toUInt64(),
                        neg.upperBound!!.value.unwrap().toUInt64()
                    ).value!!
                )
            }

            is URealVar -> {
                (_neg as URealVar).range.set(
                    ValueRange(
                        neg.lowerBound!!.value.unwrap(),
                        neg.upperBound!!.value.unwrap()
                    ).value!!
                )
            }
        }

        when (_pos) {
            is UIntVar -> {
                (_pos as UIntVar).range.set(
                    ValueRange(
                        pos.lowerBound!!.value.unwrap().toUInt64(),
                        pos.upperBound!!.value.unwrap().toUInt64()
                    ).value!!
                )
            }

            is URealVar -> {
                (_pos as URealVar).range.set(
                    ValueRange(
                        pos.lowerBound!!.value.unwrap(),
                        pos.upperBound!!.value.unwrap()
                    ).value!!
                )
            }
        }
    }

    override fun prepare(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable): Flt64? {
        x.cells
        lb.cells
        ub.cells

        return if ((!values.isNullOrEmpty() || tokenTable.cachedSolution) && if (values.isNullOrEmpty()) {
            tokenTable.cached(this)
        } else {
            tokenTable.cached(this, values)
        } == false) {
            val xValue = if (values.isNullOrEmpty()) {
                x.evaluate(tokenTable)
            } else {
                x.evaluate(
                    values = values,
                    tokenTable = tokenTable
                )
            } ?: return null

            val lbValue = if (values.isNullOrEmpty()) {
                lb.evaluate(tokenTable)
            } else {
                lb.evaluate(
                    values = values,
                    tokenTable = tokenTable
                )
            } ?: return null

            val ubValue = if (values.isNullOrEmpty()) {
                ub.evaluate(tokenTable)
            } else {
                ub.evaluate(
                    values = values,
                    tokenTable = tokenTable
                )
            } ?: return null

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

            posValue + negValue
        } else {
            null
        }
    }

    override fun register(tokenTable: AddableTokenCollection): Try {
        when (val result = tokenTable.add(listOf(_neg, _pos))) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }

    override fun register(model: AbstractLinearMechanismModel): Try {
        if (constraint) {
            when (val result = model.addConstraint(
                constraint = polyX leq ub,
                name = "${name}_ub",
                from = (parent ?: this) to true
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }

            when (val result = model.addConstraint(
                constraint = polyX geq lb,
                name = "${name}_lb",
                from = (parent ?: this) to true
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
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
        model: AbstractLinearMechanismModel,
        fixedValues: Map<Symbol, Flt64>
    ): Try {
        val xValue = x.evaluate(
            values = fixedValues,
            tokenTable = model.tokens
        ) ?: return register(model)
        val lbValue = lb.evaluate(
            values = fixedValues,
            tokenTable = model.tokens
        ) ?: return register(model)
        val ubValue = ub.evaluate(
            values = fixedValues,
            tokenTable = model.tokens
        ) ?: return register(model)
        val negSlack = if (xValue leq lbValue) {
            lbValue - lbValue
        } else {
            Flt64.zero
        }
        val posSlack = if (xValue geq ubValue) {
            xValue - ubValue
        } else {
            Flt64.zero
        }

        if (constraint) {
            when (val result = model.addConstraint(
                constraint = polyX leq ub,
                name = "${name}_ub",
                from = (parent ?: this) to true
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }

            when (val result = model.addConstraint(
                constraint = polyX geq lb,
                name = "${name}_lb",
                from = (parent ?: this) to true
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        when (val result = model.addConstraint(
            constraint = _neg eq negSlack,
            name = "${name}_neg",
            from = parent ?: this
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        model.tokens.find(_neg)?.let { token ->
            token._result = negSlack
        }

        when (val result = model.addConstraint(
            constraint = _pos eq posSlack,
            name = "${name}_pos",
            from = parent ?: this
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        model.tokens.find(_pos)?.let { token ->
            token._result = posSlack
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
            "slack_range(${x.toTidyRawString(unfold - UInt64.one)}, [${lb.toTidyRawString(unfold - UInt64.one)}, ${ub.toTidyRawString(unfold - UInt64.one)}])"
        }
    }

    override fun evaluate(
        tokenList: AbstractTokenList,
        zeroIfNone: Boolean
    ): Flt64? {
        val xValue = x.evaluate(tokenList, zeroIfNone) ?: return null
        val lbValue = lb.evaluate(tokenList, zeroIfNone) ?: return null
        val ubValue = ub.evaluate(tokenList, zeroIfNone) ?: return null
        return if (xValue ls lbValue) {
            lbValue - xValue
        } else if (xValue gr ubValue) {
            xValue - ubValue
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
        val lbValue = lb.evaluate(
            results = results,
            tokenList = tokenList,
            zeroIfNone = zeroIfNone
        ) ?: return null
        val ubValue = ub.evaluate(
            results = results,
            tokenList = tokenList,
            zeroIfNone = zeroIfNone
        ) ?: return null
        return if (xValue ls lbValue) {
            lbValue - xValue
        } else if (xValue gr ubValue) {
            xValue - ubValue
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
        val lbValue = lb.evaluate(
            values = values,
            tokenList = tokenList,
            zeroIfNone = zeroIfNone
        ) ?: return null
        val ubValue = ub.evaluate(
            values = values,
            tokenList = tokenList,
            zeroIfNone = zeroIfNone
        ) ?: return null
        return if (xValue ls lbValue) {
            lbValue - xValue
        } else if (xValue gr ubValue) {
            xValue - ubValue
        } else {
            Flt64.zero
        }
    }

    override fun calculateValue(
        tokenTable: AbstractTokenTable,
        zeroIfNone: Boolean
    ): Flt64? {
        val xValue = x.evaluate(tokenTable, zeroIfNone) ?: return null
        val lbValue = lb.evaluate(tokenTable, zeroIfNone) ?: return null
        val ubValue = ub.evaluate(tokenTable, zeroIfNone) ?: return null
        return if (xValue ls lbValue) {
            lbValue - xValue
        } else if (xValue gr ubValue) {
            xValue - ubValue
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
        val lbValue = lb.evaluate(
            results = results,
            tokenTable = tokenTable,
            zeroIfNone = zeroIfNone
        ) ?: return null
        val ubValue = ub.evaluate(
            results = results,
            tokenTable = tokenTable,
            zeroIfNone = zeroIfNone
        ) ?: return null
        return if (xValue ls lbValue) {
            lbValue - xValue
        } else if (xValue gr ubValue) {
            xValue - ubValue
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
        val lbValue = lb.evaluate(
            values = values,
            tokenTable = tokenTable,
            zeroIfNone = zeroIfNone
        ) ?: return null
        val ubValue = ub.evaluate(
            values = values,
            tokenTable = tokenTable,
            zeroIfNone = zeroIfNone
        ) ?: return null
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
        x: AbstractLinearPolynomial<*>,
        lb: AbstractLinearPolynomial<*>,
        ub: AbstractLinearPolynomial<*>,
        type: VariableType<*> = if (x.discrete && lb.discrete && ub.discrete) {
            UInteger
        } else {
            UContinuous
        },
        constraint: Boolean = true,
        parent: IntermediateSymbol? = null,
        args: Any? = null,
        name: String,
        displayName: String? = null,
    ): AbstractSlackRangeFunction<*> {
        return if (type.isIntegerType) {
            UIntegerSlackRangeFunction(
                x = x,
                lb = lb,
                ub = ub,
                constraint = constraint,
                parent = parent,
                args = args,
                name = name,
                displayName = displayName
            )
        } else {
            URealSlackRangeFunction(
                x = x,
                lb = lb,
                ub = ub,
                constraint = constraint,
                parent = parent,
                args = args,
                name = name,
                displayName = displayName
            )
        }
    }

    operator fun <
        T : ToLinearPolynomial<Poly>,
        Poly : AbstractLinearPolynomial<Poly>
    > invoke(
        x: T,
        lb: Int,
        ub: Int,
        type: VariableType<*>? = null,
        constraint: Boolean = true,
        parent: IntermediateSymbol? = null,
        args: Any? = null,
        name: String,
        displayName: String? = null,
    ): AbstractSlackRangeFunction<*> {
        val xPoly = x.toLinearPolynomial()
        return invoke(
            x = xPoly,
            lb = LinearPolynomial(lb),
            ub = LinearPolynomial(ub),
            type ?: if (xPoly.discrete) {
                UInteger
            } else {
                UContinuous
            },
            constraint = constraint,
            parent = parent,
            args = args,
            name = name,
            displayName = displayName
        )
    }

    operator fun <
        T : ToLinearPolynomial<Poly>,
        Poly : AbstractLinearPolynomial<Poly>
    > invoke(
        x: T,
        lb: Double,
        ub: Double,
        type: VariableType<*>? = null,
        constraint: Boolean = true,
        parent: IntermediateSymbol? = null,
        args: Any? = null,
        name: String,
        displayName: String? = null
    ): AbstractSlackRangeFunction<*> {
        val xPoly = x.toLinearPolynomial()
        return invoke(
            x = xPoly,
            lb = LinearPolynomial(lb),
            ub = LinearPolynomial(ub),
            type = type ?: UContinuous,
            constraint = constraint,
            parent = parent,
            args = args,
            name = name,
            displayName = displayName
        )
    }

    operator fun <
        T : ToLinearPolynomial<Poly>,
        Poly : AbstractLinearPolynomial<Poly>
    > invoke(
        x: T,
        lb: Trivalent,
        ub: Trivalent,
        type: VariableType<*>? = null,
        constraint: Boolean = true,
        parent: IntermediateSymbol? = null,
        args: Any? = null,
        name: String,
        displayName: String? = null,
    ): AbstractSlackRangeFunction<*> {
        val xPoly = x.toLinearPolynomial()
        return invoke(
            x = xPoly,
            lb = LinearPolynomial(lb),
            ub = LinearPolynomial(ub),
            type = type ?: if (xPoly.discrete) {
                UInteger
            } else {
                UContinuous
            },
            constraint = constraint,
            parent = parent,
            args = args,
            name = name,
            displayName = displayName
        )
    }

    operator fun <
        T : ToLinearPolynomial<Poly>,
        Poly : AbstractLinearPolynomial<Poly>
    > invoke(
        x: T,
        lb: BalancedTrivalent,
        ub: BalancedTrivalent,
        type: VariableType<*>? = null,
        constraint: Boolean = true,
        parent: IntermediateSymbol? = null,
        args: Any? = null,
        name: String,
        displayName: String? = null,
    ): AbstractSlackRangeFunction<*> {
        val xPoly = x.toLinearPolynomial()
        return invoke(
            x = xPoly,
            lb = LinearPolynomial(lb),
            ub = LinearPolynomial(ub),
            type = type ?: if (xPoly.discrete) {
                UInteger
            } else {
                UContinuous
            },
            constraint = constraint,
            parent = parent,
            args = args,
            name = name,
            displayName = displayName
        )
    }

    operator fun <
        T1 : ToLinearPolynomial<Poly>,
        Poly : AbstractLinearPolynomial<Poly>,
        T2 : RealNumber<T2>,
        T3 : RealNumber<T3>
    > invoke(
        x: T1,
        lb: T2,
        ub: T3,
        type: VariableType<*>? = null,
        constraint: Boolean = true,
        parent: IntermediateSymbol? = null,
        args: Any? = null,
        name: String,
        displayName: String? = null,
    ): AbstractSlackRangeFunction<*> {
        val xPoly = x.toLinearPolynomial()
        return invoke(
            x = xPoly,
            lb = LinearPolynomial(lb),
            ub = LinearPolynomial(ub),
            type = type ?: if (xPoly.discrete) {
                UInteger
            } else {
                UContinuous
            },
            constraint = constraint,
            parent = parent,
            args = args,
            name = name,
            displayName = displayName
        )
    }

    operator fun <
        T1 : ToLinearPolynomial<Poly1>,
        Poly1 : AbstractLinearPolynomial<Poly1>,
        T2 : ToLinearPolynomial<Poly2>,
        Poly2 : AbstractLinearPolynomial<Poly2>,
        T3 : ToLinearPolynomial<Poly3>,
        Poly3 : AbstractLinearPolynomial<Poly3>
    > invoke(
        x: T1,
        lb: T2,
        ub: T3,
        type: VariableType<*>? = null,
        constraint: Boolean = true,
        parent: IntermediateSymbol? = null,
        args: Any? = null,
        name: String,
        displayName: String? = null,
    ): AbstractSlackRangeFunction<*> {
        val xPoly = x.toLinearPolynomial()
        val lbPoly = lb.toLinearPolynomial()
        val ubPoly = ub.toLinearPolynomial()
        return invoke(
            x = x.toLinearPolynomial(),
            lb = lbPoly,
            ub = ubPoly,
            type = type ?: if (xPoly.discrete && lbPoly.discrete && ubPoly.discrete) {
                UInteger
            } else {
                UContinuous
            },
            constraint = constraint,
            parent = parent,
            args = args,
            name = name,
            displayName = displayName
        )
    }
}

class UIntegerSlackRangeFunction(
    x: AbstractLinearPolynomial<*>,
    lb: AbstractLinearPolynomial<*>,
    ub: AbstractLinearPolynomial<*>,
    constraint: Boolean = true,
    parent: IntermediateSymbol? = null,
    args: Any? = null,
    name: String,
    displayName: String? = null,
) : AbstractSlackRangeFunction<UIntVar>(
    x = x,
    lb = lb,
    ub = ub,
    constraint = constraint,
    parent = parent,
    args = args,
    name = name,
    displayName = displayName,
    ctor = { UIntVar(it) }
) {
    override val discrete = true
}

class URealSlackRangeFunction(
    x: AbstractLinearPolynomial<*>,
    lb: AbstractLinearPolynomial<*>,
    ub: AbstractLinearPolynomial<*>,
    constraint: Boolean = true,
    parent: IntermediateSymbol? = null,
    args: Any? = null,
    name: String,
    displayName: String? = null,
) : AbstractSlackRangeFunction<URealVar>(
    x = x,
    lb = lb,
    ub = ub,
    constraint = constraint,
    parent = parent,
    args = args,
    name = name,
    displayName = displayName,
    ctor = { URealVar(it) }
)