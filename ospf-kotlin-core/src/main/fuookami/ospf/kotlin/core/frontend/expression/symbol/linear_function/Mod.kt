package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function

import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

class ModFunction(
    private val x: AbstractLinearPolynomial<*>,
    private val d: Flt64,
    private val epsilon: Flt64 = Flt64(1e-6),
    override val parent: IntermediateSymbol? = null,
    args: Any? = null,
    override var name: String = "${x}_mod_${d}",
    override var displayName: String? = "$x mod $d"
) : LinearFunctionSymbol() {
    private val logger = logger()

    companion object {
        operator fun <
            T : ToLinearPolynomial<Poly>,
            Poly : AbstractLinearPolynomial<Poly>
        > invoke(
            x: T,
            d: Int,
            epsilon: Flt64 = Flt64(1e-6),
            parent: IntermediateSymbol? = null,
            args: Any? = null,
            name: String = "${x}_mod_${d}",
            displayName: String? = "$x mod $d"
        ): ModFunction {
            return ModFunction(
                x = x.toLinearPolynomial(),
                d = Flt64(d),
                epsilon = epsilon,
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
            d: Double,
            epsilon: Flt64 = Flt64(1e-6),
            parent: IntermediateSymbol? = null,
            args: Any? = null,
            name: String = "${x}_mod_${d}",
            displayName: String? = "$x mod $d"
        ): ModFunction {
            return ModFunction(
                x = x.toLinearPolynomial(),
                d = Flt64(d),
                epsilon = epsilon,
                parent = parent,
                args = args,
                name = name,
                displayName = displayName
            )
        }

        operator fun <
            T1 : ToLinearPolynomial<Poly>,
            Poly : AbstractLinearPolynomial<Poly>,
            T2 : RealNumber<T2>
        > invoke(
            x: T1,
            d: T2,
            epsilon: Flt64 = Flt64(1e-6),
            parent: IntermediateSymbol? = null,
            args: Any? = null,
            name: String = "${x}_mod_${d}",
            displayName: String? = "$x mod $d"
        ): ModFunction {
            return ModFunction(
                x = x.toLinearPolynomial(),
                d = d.toFlt64(),
                epsilon = epsilon,
                parent = parent,
                args = args,
                name = name,
                displayName = displayName
            )
        }
    }

    private val _args = args
    override val args = _args ?: parent?.args

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

    private val r: URealVar by lazy {
        val r = URealVar("${name}_r")
        r.range.leq(d.abs() - epsilon)
        r
    }

    val quotient: AbstractLinearPolynomial<*> by lazy {
        val quotient = LinearPolynomial(q, "${name}_quotient")
        quotient
    }

    private val y: AbstractLinearPolynomial<*> by lazy {
        val y = LinearPolynomial(r, "${name}_y")
        y.range.set(ValueRange(Flt64.zero, d.abs() - epsilon).value!!)
        y
    }

    override val discrete: Boolean by lazy {
        x.discrete && (d.round() eq d)
    }

    override val range get() = y.range
    override val lowerBound get() = y.lowerBound
    override val upperBound get() = y.upperBound

    override val category = Linear

    override val dependencies by x::dependencies
    override val cells get() = y.cells
    override val cached get() = y.cached

    private val possibleRange
        get() = ValueRange(
            (x.lowerBound!!.value.unwrap() / d).floor(),
            (x.upperBound!!.value.unwrap() / d).floor()
        ).value!!

    override fun flush(force: Boolean) {
        x.flush(force)
        y.flush(force)
        q.range.set(ValueRange(possibleRange.lowerBound.value.unwrap().toInt64(), possibleRange.upperBound.value.unwrap().toInt64()).value!!)
        r.range.set(ValueRange(Flt64.zero, d.abs() - epsilon).value!!)
        y.range.set(ValueRange(Flt64.zero, d.abs() - epsilon).value!!)
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
                x.evaluate(
                    values = values,
                    tokenTable = tokenTable
                )
            } ?: return null

            val qValue = (xValue / d).let {
                if (it geq Flt64.zero) {
                    it.floor()
                } else {
                    it.ceil()
                }
            }
            logger.trace { "Setting ModFunction ${name}.q initial solution: $qValue" }
            tokenTable.find(q)?.let { token -> token._result = qValue }
            val rValue = xValue - qValue * d
            logger.trace { "Setting ModFunction ${name}.r initial solution: $rValue" }
            tokenTable.find(r)?.let { token -> token._result = rValue }

            rValue
        } else {
            null
        }
    }

    override fun register(tokenTable: AddableTokenCollection): Try {
        when (val result = tokenTable.add(listOf(q, r))) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }

    override fun register(model: AbstractLinearMechanismModel): Try {
        when (val result = model.addConstraint(
            constraint = x eq (d * q + r),
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
        val qValue = (xValue / d).floor()
        val rValue = xValue - qValue * d

        when (val result = model.addConstraint(
            constraint = x eq (d * q + r),
            name = name,
            from = parent ?: this
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = model.addConstraint(
            constraint = q eq qValue,
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
            constraint = r eq rValue,
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
            "(${x.toTidyRawString(unfold - UInt64.one)} mod $d)"
        }
    }

    override fun evaluate(
        tokenList: AbstractTokenList,
        zeroIfNone: Boolean
    ): Flt64? {
        return x.evaluate(tokenList, zeroIfNone)?.let {
            it % d
        }
    }

    override fun evaluate(
        results: List<Flt64>,
        tokenList: AbstractTokenList,
        zeroIfNone: Boolean
    ): Flt64? {
        return x.evaluate(
            results = results,
            tokenList = tokenList,
            zeroIfNone = zeroIfNone
        )?.let {
            it % d
        }
    }

    override fun evaluate(
        values: Map<Symbol, Flt64>,
        tokenList: AbstractTokenList?,
        zeroIfNone: Boolean
    ): Flt64? {
        return x.evaluate(
            values = values,
            tokenList = tokenList,
            zeroIfNone = zeroIfNone
        )?.let {
            it % d
        }
    }

    override fun calculateValue(
        tokenTable: AbstractTokenTable,
        zeroIfNone: Boolean
    ): Flt64? {
        return x.evaluate(tokenTable, zeroIfNone)?.let {
            it % d
        }
    }

    override fun calculateValue(
        results: List<Flt64>,
        tokenTable: AbstractTokenTable,
        zeroIfNone: Boolean
    ): Flt64? {
        return x.evaluate(
            results = results,
            tokenTable = tokenTable,
            zeroIfNone = zeroIfNone
        )?.let {
            it % d
        }
    }

    override fun calculateValue(
        values: Map<Symbol, Flt64>,
        tokenTable: AbstractTokenTable?,
        zeroIfNone: Boolean
    ): Flt64? {
        return x.evaluate(
            values = values,
            tokenTable = tokenTable,
            zeroIfNone = zeroIfNone
        )?.let {
            it % d
        }
    }
}