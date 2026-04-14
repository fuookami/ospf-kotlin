@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package fuookami.ospf.kotlin.core.intermediate_symbol.legacy.linear_function

import fuookami.ospf.kotlin.core.expression.monomial.times
import fuookami.ospf.kotlin.core.expression.polynomial.AbstractLinearPolynomial
import fuookami.ospf.kotlin.core.expression.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.expression.polynomial.ToLinearPolynomial
import fuookami.ospf.kotlin.core.expression.polynomial.plus
import fuookami.ospf.kotlin.core.intermediate_symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearFunctionSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.prepareIfNotCached
import fuookami.ospf.kotlin.core.intermediate_symbol.toTidyRawString
import fuookami.ospf.kotlin.core.intermediate_model.eq
import fuookami.ospf.kotlin.core.intermediate_model.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.intermediate_model.AbstractTokenTable
import fuookami.ospf.kotlin.core.variable.AbstractTokenList
import fuookami.ospf.kotlin.core.variable.AddableTokenCollection
import fuookami.ospf.kotlin.core.variable.IntVar
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import org.apache.logging.log4j.kotlin.logger

class RoundingFunction(
    private val x: AbstractLinearPolynomial<*>,
    private val d: Flt64,
    private val epsilon: Flt64 = Flt64(1e-6),
    override val parent: IntermediateSymbol? = null,
    args: Any? = null,
    override var name: String = "round_${x}_${d}",
    override var displayName: String? = "round(x/$d)"
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
            name: String = "round_${x}_${d}",
            displayName: String? = "round(x/$d)"
        ): RoundingFunction {
            return RoundingFunction(
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
            name: String = "round_${x}_${d}",
            displayName: String? = "round(x/$d)"
        ): RoundingFunction {
            return RoundingFunction(
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
            name: String = "round_${x}_${d}",
            displayName: String? = "round(x/$d)"
        ): RoundingFunction {
            return RoundingFunction(
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

    internal val _args = args
    override val args get() = _args ?: parent?.args

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
        r.range.set(ValueRange(-d.abs() / Flt64.two, d.abs() / Flt64.two - epsilon).value!!)
        r
    }

    val remainder: AbstractLinearPolynomial<*> by lazy {
        val remainder = LinearPolynomial(r, "${name}_remainder")
        remainder
    }

    private val y: AbstractLinearPolynomial<*> by lazy {
        val y = LinearPolynomial(q, "${name}_y")
        y.range.set(possibleRange)
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
            (x.lowerBound!!.value.unwrap() / d).round(),
            (x.upperBound!!.value.unwrap() / d).round()
        ).value!!

    override fun flush(force: Boolean) {
        x.flush(force)
        y.flush(force)
        q.range.set(ValueRange(possibleRange.lowerBound.value.unwrap().toInt64(), possibleRange.upperBound.value.unwrap().toInt64()).value!!)
        r.range.set(ValueRange(-d.abs() / Flt64.two, d.abs() / Flt64.two - epsilon).value!!)
        y.range.set(possibleRange)
    }

    override fun prepare(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable): Flt64? {
        return prepareIfNotCached(values, tokenTable) {
            val xValue = if (values.isNullOrEmpty()) {
                x.evaluate(tokenTable)
            } else {
                x.evaluate(values, tokenTable)
            } ?: return null

            val qValue = (xValue / d).round()
            logger.trace { "Setting FloorFunction ${name}.q initial solution: $qValue" }
            tokenTable.find(q)?.let { token -> token._result = qValue }
            val rValue = xValue - qValue * d
            logger.trace { "Setting FloorFunction ${name}.r initial solution: $rValue" }
            tokenTable.find(r)?.let { token -> token._result = rValue }

            qValue
        }
    }

    override fun register(tokenTable: AddableTokenCollection): Try {
        when (val result = tokenTable.add(listOf(q, r))) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        return ok
    }

    override fun register(model: AbstractLinearMechanismModel): Try {
        when (val result = model.addConstraint(
            relation = x eq (d * q + r),
            name = name,
            from = parent ?: this
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }

            is Fatal -> {
                return Fatal(result.errors)
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
        val xValue = x.evaluate(fixedValues, model.tokens) ?: return register(model)
        val qValue = (xValue / d).round()
        val rValue = xValue - qValue * d

        when (val result = model.addConstraint(
            relation = x eq (d * q + r),
            name = name,
            from = parent ?: this
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        when (val result = model.addConstraint(
            relation = q eq qValue,
            name = "${name}_q",
            from = parent ?: this
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        model.tokens.find(q)?.let { token ->
            token._result = qValue
        }

        when (val result = model.addConstraint(
            relation = r eq rValue,
            name = "${name}_r",
            from = parent ?: this
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }

            is Fatal -> {
                return Fatal(result.errors)
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
            "round(${x.toTidyRawString(unfold - UInt64.one)} / $d)"
        }
    }

    override fun evaluate(
        tokenList: AbstractTokenList,
        zeroIfNone: Boolean
    ): Flt64? {
        return x.evaluate(tokenList, zeroIfNone)?.let {
            (it / d).round()
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
            (it / d).round()
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
            (it / d).round()
        }
    }

    override fun calculateValue(
        tokenTable: AbstractTokenTable,
        zeroIfNone: Boolean
    ): Flt64? {
        return x.evaluate(tokenTable, zeroIfNone)?.let {
            (it / d).round()
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
            (it / d).round()
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
            (it / d).round()
        }
    }
}





