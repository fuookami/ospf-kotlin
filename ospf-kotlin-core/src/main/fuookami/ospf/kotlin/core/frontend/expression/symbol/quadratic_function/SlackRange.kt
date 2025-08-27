package fuookami.ospf.kotlin.core.frontend.expression.symbol.quadratic_function

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
    private val x: AbstractQuadraticPolynomial<*>,
    private val lb: AbstractQuadraticPolynomial<*>,
    private val ub: AbstractQuadraticPolynomial<*>,
    private val constraint: Boolean = true,
    override var name: String,
    override var displayName: String? = null,
    private val ctor: (String) -> V
) : QuadraticFunctionSymbol {
    private val logger = logger()

    private val _neg: V by lazy {
        ctor("${name}_neg")
    }
    val neg: AbstractQuadraticPolynomial<*> by lazy {
        QuadraticPolynomial(_neg)
    }

    private val _pos: V by lazy {
        ctor("${name}_pos")
    }
    val pos: AbstractQuadraticPolynomial<*> by lazy {
        QuadraticPolynomial(_pos)
    }

    val polyX: AbstractQuadraticPolynomial<*> by lazy {
        val polynomial = x + _neg - _pos
        polynomial.name = "${name}_x"
        polynomial
    }

    private val y: AbstractQuadraticPolynomial<*> by lazy {
        val y = QuadraticPolynomial(neg + pos, "${name}_y")

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
                (_neg as UIntVar).range.set(ValueRange(neg.lowerBound!!.value.unwrap().toUInt64(), neg.upperBound!!.value.unwrap().toUInt64()).value!!)
            }

            is URealVar -> {
                (_neg as URealVar).range.set(ValueRange(neg.lowerBound!!.value.unwrap(), neg.upperBound!!.value.unwrap()).value!!)
            }
        }

        when (_pos) {
            is UIntVar -> {
                (_pos as UIntVar).range.set(ValueRange(pos.lowerBound!!.value.unwrap().toUInt64(), pos.upperBound!!.value.unwrap().toUInt64()).value!!)
            }

            is URealVar -> {
                (_pos as URealVar).range.set(ValueRange(pos.lowerBound!!.value.unwrap(), pos.upperBound!!.value.unwrap()).value!!)
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
                x.evaluate(values, tokenTable)
            }  ?: return null

            val lbValue = if (values.isNullOrEmpty()) {
                lb.evaluate(tokenTable)
            } else {
                lb.evaluate(values, tokenTable)
            } ?: return null

            val ubValue = if (values.isNullOrEmpty()) {
                ub.evaluate(tokenTable)
            } else {
                ub.evaluate(values, tokenTable)
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

    override fun register(tokenTable: AbstractMutableTokenTable): Try {
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

    override fun register(model: AbstractQuadraticMechanismModel): Try {
        if ((x.range.range!! intersect ValueRange(lb.lowerBound!!.value.unwrap(), ub.upperBound!!.value.unwrap()).value!!) == null) {
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

    override fun register(
        tokenTable: AbstractMutableTokenTable,
        fixedValues: Map<Symbol, Flt64>
    ): Try {
        return register(tokenTable)
    }

    override fun register(
        model: AbstractQuadraticMechanismModel,
        fixedValues: Map<Symbol, Flt64>
    ): Try {
        val xValue = x.evaluate(fixedValues, model.tokens) ?: return register(model)
        val lbValue = lb.evaluate(fixedValues, model.tokens) ?: return register(model)
        val ubValue = ub.evaluate(fixedValues, model.tokens) ?: return register(model)
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

        when (val result = model.addConstraint(
            _neg eq negSlack,
            "${name}_neg"
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
            _pos eq posSlack,
            "${name}_pos"
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
        val xValue = x.evaluate(results, tokenList, zeroIfNone) ?: return null
        val lbValue = lb.evaluate(results, tokenList, zeroIfNone) ?: return null
        val ubValue = ub.evaluate(results, tokenList, zeroIfNone) ?: return null
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
        val xValue = x.evaluate(values, tokenList, zeroIfNone) ?: return null
        val lbValue = lb.evaluate(values, tokenList, zeroIfNone) ?: return null
        val ubValue = ub.evaluate(values, tokenList, zeroIfNone) ?: return null
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
        val xValue = x.evaluate(results, tokenTable, zeroIfNone) ?: return null
        val lbValue = lb.evaluate(results, tokenTable, zeroIfNone) ?: return null
        val ubValue = ub.evaluate(results, tokenTable, zeroIfNone) ?: return null
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
        val xValue = x.evaluate(values, tokenTable, zeroIfNone) ?: return null
        val lbValue = lb.evaluate(values, tokenTable, zeroIfNone) ?: return null
        val ubValue = ub.evaluate(values, tokenTable, zeroIfNone) ?: return null
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
        x: AbstractQuadraticPolynomial<*>,
        lb: AbstractQuadraticPolynomial<*>,
        ub: AbstractQuadraticPolynomial<*>,
        type: VariableType<*> = if (x.discrete && lb.discrete && ub.discrete) {
            UInteger
        } else {
            UContinuous
        },
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

    operator fun <
        T : ToQuadraticPolynomial<Poly>,
        Poly : AbstractQuadraticPolynomial<Poly>
    > invoke(
        x: T,
        lb: Int,
        ub: Int,
        type: VariableType<*>? = null,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackRangeFunction<*> {
        val xPoly = x.toQuadraticPolynomial()
        return invoke(
            xPoly,
            QuadraticPolynomial(lb),
            QuadraticPolynomial(ub),
            type = type ?: if (xPoly.discrete) {
                UInteger
            } else {
                UContinuous
            },
            constraint,
            name,
            displayName
        )
    }

    operator fun <
        T : ToQuadraticPolynomial<Poly>,
        Poly : AbstractQuadraticPolynomial<Poly>
    > invoke(
        x: T,
        lb: Double,
        ub: Double,
        type: VariableType<*>? = null,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackRangeFunction<*> {
        return invoke(
            x.toQuadraticPolynomial(),
            QuadraticPolynomial(lb),
            QuadraticPolynomial(ub),
            type = type ?: UContinuous,
            constraint,
            name,
            displayName
        )
    }

    operator fun <
        T : ToQuadraticPolynomial<Poly>,
        Poly : AbstractQuadraticPolynomial<Poly>
    > invoke(
        x: T,
        lb: Trivalent,
        ub: Trivalent,
        type: VariableType<*>? = null,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackRangeFunction<*> {
        val xPoly = x.toQuadraticPolynomial()
        return invoke(
            xPoly,
            QuadraticPolynomial(lb.value),
            QuadraticPolynomial(ub.value),
            type = type ?: if (xPoly.discrete) {
                UInteger
            } else {
                UContinuous
            },
            constraint,
            name,
            displayName
        )
    }

    operator fun <
        T : ToQuadraticPolynomial<Poly>,
        Poly : AbstractQuadraticPolynomial<Poly>
    > invoke(
        x: T,
        lb: BalancedTrivalent,
        ub: BalancedTrivalent,
        type: VariableType<*>? = null,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackRangeFunction<*> {
        val xPoly = x.toQuadraticPolynomial()
        return invoke(
            xPoly,
            QuadraticPolynomial(lb.value),
            QuadraticPolynomial(ub.value),
            type = type ?: if (xPoly.discrete) {
                UInteger
            } else {
                UContinuous
            },
            constraint,
            name,
            displayName
        )
    }

    operator fun <
        T1 : ToQuadraticPolynomial<Poly>,
        Poly : AbstractQuadraticPolynomial<Poly>,
        T2 : RealNumber<T2>,
        T3 : RealNumber<T3>
    > invoke(
        x: T1,
        lb: T2,
        ub: T3,
        type: VariableType<*>? = null,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackRangeFunction<*> {
        return invoke(
            x.toQuadraticPolynomial(),
            QuadraticPolynomial(lb),
            QuadraticPolynomial(ub),
            type = type ?: UContinuous,
            constraint,
            name,
            displayName
        )
    }

    operator fun <
        T1 : ToQuadraticPolynomial<Poly1>,
        Poly1 : AbstractQuadraticPolynomial<Poly1>,
        T2 : ToQuadraticPolynomial<Poly2>,
        Poly2 : AbstractQuadraticPolynomial<Poly2>,
        T3 : ToQuadraticPolynomial<Poly3>,
        Poly3 : AbstractQuadraticPolynomial<Poly3>
    > invoke(
        x: T1,
        lb: T2,
        ub: T3,
        type: VariableType<*>? = null,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackRangeFunction<*> {
        val xPoly = x.toQuadraticPolynomial()
        val lbPoly = lb.toQuadraticPolynomial()
        val ubPoly = ub.toQuadraticPolynomial()
        return invoke(
            xPoly,
            lbPoly,
            ubPoly,
            type = type ?: if (xPoly.discrete && lbPoly.discrete && ubPoly.discrete) {
                UInteger
            } else {
                UContinuous
            },
            constraint,
            name,
            displayName
        )
    }
}

class UIntegerSlackRangeFunction(
    x: AbstractQuadraticPolynomial<*>,
    lb: AbstractQuadraticPolynomial<*>,
    ub: AbstractQuadraticPolynomial<*>,
    constraint: Boolean = true,
    name: String,
    displayName: String? = null,
) : AbstractSlackRangeFunction<UIntVar>(x, lb, ub, constraint, name, displayName, { UIntVar(it) }) {
    override val discrete = true
}

class URealSlackRangeFunction(
    x: AbstractQuadraticPolynomial<*>,
    lb: AbstractQuadraticPolynomial<*>,
    ub: AbstractQuadraticPolynomial<*>,
    constraint: Boolean = true,
    name: String,
    displayName: String? = null,
) : AbstractSlackRangeFunction<URealVar>(x, lb, ub, constraint, name, displayName, { URealVar(it) })
