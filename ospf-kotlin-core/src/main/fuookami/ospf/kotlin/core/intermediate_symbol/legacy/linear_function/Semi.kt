@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package fuookami.ospf.kotlin.core.intermediate_symbol.legacy.linear_function

import fuookami.ospf.kotlin.core.expression.monomial.times
import fuookami.ospf.kotlin.core.expression.polynomial.*
import fuookami.ospf.kotlin.core.intermediate_symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearFunctionSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.prepareIfNotCached
import fuookami.ospf.kotlin.core.intermediate_symbol.toTidyRawString
import fuookami.ospf.kotlin.core.intermediate_model.eq
import fuookami.ospf.kotlin.core.intermediate_model.geq
import fuookami.ospf.kotlin.core.intermediate_model.leq
import fuookami.ospf.kotlin.core.intermediate_model.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.intermediate_model.AbstractTokenTable
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.ordinary.max
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.toFlt64
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.math.operator.abs
import org.apache.logging.log4j.kotlin.logger

class SemiFunction(
    private val x: AbstractLinearPolynomial<*>,
    override val parent: IntermediateSymbol? = null,
    args: Any? = null,
    override var name: String,
    override var displayName: String? = null,
) : LinearFunctionSymbol() {
    companion object {
        operator fun <
                T : ToLinearPolynomial<Poly>,
                Poly : AbstractLinearPolynomial<Poly>
                > invoke(
            x: T,
            parent: IntermediateSymbol? = null,
            args: Any? = null,
            name: String,
            displayName: String? = null,
        ): SemiFunction {
            return SemiFunction(
                x = x.toLinearPolynomial(),
                parent = parent,
                args = args,
                name = name,
                displayName = displayName
            )
        }
    }

    private val logger = logger()

    internal val _args = args
    override val args get() = _args ?: parent?.args

    private val y: URealVar by lazy {
        val y = URealVar("${name}_y")
        y.range.leq(possibleRange.upperBound.value.unwrap())
        y
    }

    private val u: BinVar by lazy {
        val u = BinVar("${name}_u")
        if (x.lowerBound!!.value.unwrap() gr Flt64.zero) {
            u.range.eq(true)
        } else if (x.upperBound!!.value.unwrap() ls Flt64.zero) {
            u.range.eq(false)
        }
        u
    }

    private val polyY: LinearPolynomial by lazy {
        val polyY = LinearPolynomial(y)
        polyY.range.set(possibleRange)
        polyY
    }

    override val range get() = polyY.range
    override val lowerBound get() = polyY.lowerBound
    override val upperBound get() = polyY.upperBound

    override val category = Linear

    override val dependencies: Set<IntermediateSymbol> get() = x.dependencies
    override val cells get() = polyY.cells
    override val cached get() = polyY.cached

    private val possibleRange
        get() = ValueRange(
            max(Flt64.zero, x.lowerBound!!.value.unwrap()),
            max(Flt64.zero, x.upperBound!!.value.unwrap())
        ).value!!
    private val m: Flt64 by lazy {
        max(
            abs(x.lowerBound!!.value.unwrap()),
            abs(x.upperBound!!.value.unwrap())
        )
    }

    override fun flush(force: Boolean) {
        x.flush(force)
        polyY.flush(force)
        polyY.range.set(possibleRange)
    }

    override fun prepare(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable): Flt64? {
        return prepareIfNotCached(values, tokenTable) {
            val xValue = if (values.isNullOrEmpty()) {
                x.evaluate(tokenTable)
            } else {
                x.evaluate(values, tokenTable)
            } ?: return null

            val bin = xValue gr Flt64.zero
            logger.trace { "Setting SemiFunction ${name}.u to $bin" }
            tokenTable.find(u)?.let { token ->
                token._result = bin.toFlt64()
            }

            val yValue = max(Flt64.zero, xValue)
            logger.trace { "Setting SemiFunction ${name}.y to $yValue" }
            tokenTable.find(y)?.let { token ->
                token._result = yValue
            }

            yValue
        }
    }

    override fun register(tokenTable: AddableTokenCollection): Try {
        when (val result = tokenTable.add(listOf(y, u))) {
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
            relation = y geq x,
            name = "${name}_lb",
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
            relation = y leq x + m * u,
            name = "${name}_ub",
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
            relation = y leq m * (Flt64.one - u),
            name = "${name}_yu",
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
        val yValue = max(Flt64.zero, xValue)
        val bin = xValue gr Flt64.zero

        when (val result = model.addConstraint(
            relation = y geq x,
            name = "${name}_lb",
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
            relation = y leq x + m * u,
            name = "${name}_ub",
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
            relation = y leq m * (Flt64.one - u),
            name = "${name}_yu",
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
            relation = y eq yValue,
            name = "${name}_y",
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

        model.tokens.find(y)?.let { token ->
            token._result = yValue
        }

        when (val result = model.addConstraint(
            relation = u eq bin,
            name = "${name}_u",
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

        model.tokens.find(u)?.let { token ->
            token._result = bin.toFlt64()
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
            "semi(${x.toTidyRawString(unfold - UInt64.one)})"
        }
    }

    override fun evaluate(
        tokenList: AbstractTokenList,
        zeroIfNone: Boolean
    ): Flt64? {
        val xValue = x.evaluate(tokenList, zeroIfNone) ?: return null
        return max(Flt64.zero, xValue)
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
        return max(Flt64.zero, xValue)
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
        return max(Flt64.zero, xValue)
    }

    override fun calculateValue(
        tokenTable: AbstractTokenTable,
        zeroIfNone: Boolean
    ): Flt64? {
        val xValue = x.evaluate(tokenTable, zeroIfNone) ?: return null
        return max(Flt64.zero, xValue)
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
        return max(Flt64.zero, xValue)
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
        return max(Flt64.zero, xValue)
    }
}





