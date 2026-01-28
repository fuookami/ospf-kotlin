package fuookami.ospf.kotlin.core.frontend.expression.symbol.quadratic_function

import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

class RoundingFunction(
    private val x: AbstractQuadraticPolynomial<*>,
    private val d: AbstractQuadraticPolynomial<*>,
    override val parent: IntermediateSymbol? = null,
    args: Any? = null,
    override var name: String = "round_${x}_${d}",
    override var displayName: String? = "⌊$x/$d⌉"
) : QuadraticFunctionSymbol() {
    private val logger = logger()

    companion object {
        operator fun <
            T : ToQuadraticPolynomial<Poly>,
            Poly : AbstractQuadraticPolynomial<Poly>
        > invoke(
            x: T,
            d: Int,
            parent: IntermediateSymbol? = null,
            args: Any? = null,
            name: String = "round_${x}_${d}",
            displayName: String? = "⌊$x/$d⌉"
        ): RoundingFunction {
            return RoundingFunction(
                x = x.toQuadraticPolynomial(),
                d = QuadraticPolynomial(d),
                parent = parent,
                args = args,
                name = name,
                displayName
            )
        }

        operator fun <
            T : ToQuadraticPolynomial<Poly>,
            Poly : AbstractQuadraticPolynomial<Poly>
        > invoke(
            x: T,
            d: Double,
            parent: IntermediateSymbol? = null,
            args: Any? = null,
            name: String = "round_${x}_${d}",
            displayName: String? = "⌊$x/$d⌉"
        ): RoundingFunction {
            return RoundingFunction(
                x = x.toQuadraticPolynomial(),
                d = QuadraticPolynomial(d),
                parent = parent,
                args = args,
                name = name,
                displayName = displayName
            )
        }

        operator fun <
            T1 : ToQuadraticPolynomial<Poly>,
            Poly : AbstractQuadraticPolynomial<Poly>,
            T2 : RealNumber<T2>
        > invoke(
            x: T1,
            d: T2,
            parent: IntermediateSymbol? = null,
            args: Any? = null,
            name: String = "round_${x}_${d}",
            displayName: String? = "⌊$x/$d⌉"
        ): RoundingFunction {
            return RoundingFunction(
                x = x.toQuadraticPolynomial(),
                d = QuadraticPolynomial(d),
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
            d: T2,
            parent: IntermediateSymbol? = null,
            args: Any? = null,
            name: String = "round_${x}_${d}",
            displayName: String? = "⌊$x/$d⌉"
        ): RoundingFunction {
            return RoundingFunction(
                x = x.toQuadraticPolynomial(),
                d = d.toQuadraticPolynomial(),
                parent = parent,
                args = args,
                name = name,
                displayName = displayName
            )
        }
    }

    internal val _args = args
    override val args get() = _args ?: parent?.args

    private val dLinear: LinearFunction by lazy {
        LinearFunction(
            polynomial = d,
            parent = parent ?: this,
            name = "${name}_d"
        )
    }

    private val q: IntVar by lazy {
        val q = IntVar("${name}_q")
        q.range.set(
            ValueRange(
                possibleRange.lowerBound.value.unwrap().toInt64(),
                possibleRange.upperBound.value.unwrap().toInt64()
            ).value!!
        )
        q
    }

    private val r: RealVar by lazy {
        val r = RealVar("${name}_r")
        r.range.leq(possibleModUpperBound)
        r
    }

    val remainder: AbstractQuadraticPolynomial<*> by lazy {
        val remainder = QuadraticPolynomial(r, "${name}_remainder")
        remainder
    }

    private val y: AbstractQuadraticPolynomial<*> by lazy {
        val y = QuadraticPolynomial(q, "${name}_y")
        y.range.set(possibleRange)
        y
    }

    override val discrete by lazy {
        x.discrete && d.discrete
    }

    override val range get() = y.range
    override val lowerBound get() = y.lowerBound
    override val upperBound get() = y.upperBound

    override val category = Linear

    override val dependencies get() = x.dependencies + d.dependencies
    override val cells get() = y.cells
    override val cached get() = y.cached

    private val possibleRange: ValueRange<Flt64>
        get() {
            return if (d.range.range!!.contains(Flt64.zero)) {
                ValueRange(
                    (x.upperBound!!.value.unwrap() / d.lowerBound!!.value.unwrap()).round(),
                    Flt64.maximum
                ).value!!
            } else {
                val q1 = (x.upperBound!!.value.unwrap() / d.upperBound!!.value.unwrap()).round()
                val q2 = (x.upperBound!!.value.unwrap() / d.lowerBound!!.value.unwrap()).round()
                val q3 = (x.lowerBound!!.value.unwrap() / d.upperBound!!.value.unwrap()).round()
                val q4 = (x.lowerBound!!.value.unwrap() / d.lowerBound!!.value.unwrap()).round()
                ValueRange(min(q1, q2, q3, q4), max(q1, q2, q3, q4)).value!!
            }
        }

    private val possibleModUpperBound
        get() = max(
            if (d.upperBound!!.value.unwrap() geq Flt64.zero) {
                d.upperBound!!.value.unwrap() / Flt64.two
            } else {
                d.upperBound!!.value.unwrap().abs() / Flt64.two
            },
            if (d.lowerBound!!.value.unwrap() geq Flt64.zero) {
                d.upperBound!!.value.unwrap() / Flt64.two
            } else {
                d.lowerBound!!.value.unwrap().abs() / Flt64.two
            }
        )

    override fun flush(force: Boolean) {
        x.flush(force)
        y.flush(force)
        q.range.set(ValueRange(possibleRange.lowerBound.value.unwrap().toInt64(), possibleRange.upperBound.value.unwrap().toInt64()).value!!)
        r.range.set(ValueRange(Flt64.zero, possibleModUpperBound).value!!)
        y.range.set(possibleRange)
    }

    override fun prepare(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable): Flt64? {
        x.cells

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

            val dValue = if (values.isNullOrEmpty()) {
                d.evaluate(tokenTable)
            } else {
                d.evaluate(values, tokenTable)
            } ?: return null

            val qValue = (xValue / dValue).round()
            logger.trace { "Setting FloorFunction ${name}.q initial solution: $qValue" }
            tokenTable.find(q)?.let { token -> token._result = qValue }
            val rValue = xValue - qValue * dValue
            logger.trace { "Setting FloorFunction ${name}.r initial solution: $rValue" }
            tokenTable.find(r)?.let { token -> token._result = rValue }

            qValue
        } else {
            null
        }
    }

    override fun register(tokenTable: AddableTokenCollection): Try {
        when (val result = dLinear.register(tokenTable)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = tokenTable.add(q)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = tokenTable.add(r)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }

    override fun register(model: AbstractQuadraticMechanismModel): Try {
        when (val result = dLinear.register(model)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = model.addConstraint(
            x eq (dLinear * q + r),
            name = name,
            from = parent ?: this
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }

    override fun register(
        tokenTable: AddableTokenCollection,
        fixedValues: Map<Symbol, Flt64>
    ): Try {
        when (val result = dLinear.register(tokenTable, fixedValues)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = tokenTable.add(q)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = tokenTable.add(r)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }

    override fun register(
        model: AbstractQuadraticMechanismModel,
        fixedValues: Map<Symbol, Flt64>
    ): Try {
        val xValue = x.evaluate(fixedValues, model.tokens) ?: return register(model)
        val dValue = d.evaluate(fixedValues, model.tokens) ?: return register(model)
        val qValue = (xValue / dValue).round()
        val rValue = xValue - qValue * dValue

        when (val result = dLinear.register(model, fixedValues)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = model.addConstraint(
            x eq (dLinear * q + r),
            name
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = model.addConstraint(
            q eq qValue,
            name = "${name}_q",
            from = parent ?: this
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        model.tokens.find(q)?.let { token ->
            token._result = qValue
        }

        when (val result = model.addConstraint(
            r eq rValue,
            name = "${name}_r",
            from = parent ?: this
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        model.tokens.find(r)?.let { token ->
            token._result = rValue
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
            "⌊${x.toTidyRawString(unfold - UInt64.one)} / ${d.toTidyRawString(unfold - UInt64.one)}⌉"
        }
    }

    override fun evaluate(
        tokenList: AbstractTokenList,
        zeroIfNone: Boolean
    ): Flt64? {
        val xValue = x.evaluate(tokenList, zeroIfNone) ?: return null
        val dValue = d.evaluate(tokenList, zeroIfNone) ?: return null
        return (xValue / dValue).round()
    }

    override fun evaluate(
        results: List<Flt64>,
        tokenList: AbstractTokenList,
        zeroIfNone: Boolean
    ): Flt64? {
        val xValue = x.evaluate(results, tokenList, zeroIfNone) ?: return null
        val dValue = d.evaluate(results, tokenList, zeroIfNone) ?: return null
        return (xValue / dValue).round()
    }

    override fun evaluate(
        values: Map<Symbol, Flt64>,
        tokenList: AbstractTokenList?,
        zeroIfNone: Boolean
    ): Flt64? {
        val xValue = x.evaluate(values, tokenList, zeroIfNone) ?: return null
        val dValue = d.evaluate(values, tokenList, zeroIfNone) ?: return null
        return (xValue / dValue).round()
    }

    override fun calculateValue(
        tokenTable: AbstractTokenTable,
        zeroIfNone: Boolean
    ): Flt64? {
        val xValue = x.evaluate(tokenTable, zeroIfNone) ?: return null
        val dValue = d.evaluate(tokenTable, zeroIfNone) ?: return null
        return (xValue / dValue).round()
    }

    override fun calculateValue(
        results: List<Flt64>,
        tokenTable: AbstractTokenTable,
        zeroIfNone: Boolean
    ): Flt64? {
        val xValue = x.evaluate(results, tokenTable, zeroIfNone) ?: return null
        val dValue = d.evaluate(results, tokenTable, zeroIfNone) ?: return null
        return (xValue / dValue).round()
    }

    override fun calculateValue(
        values: Map<Symbol, Flt64>,
        tokenTable: AbstractTokenTable?,
        zeroIfNone: Boolean
    ): Flt64? {
        val xValue = x.evaluate(values, tokenTable, zeroIfNone) ?: return null
        val dValue = d.evaluate(values, tokenTable, zeroIfNone) ?: return null
        return (xValue / dValue).round()
    }
}
