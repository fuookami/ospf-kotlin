package fuookami.ospf.kotlin.core.frontend.expression.symbol.quadratic_function

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

class SemiFunction(
    private val x: AbstractQuadraticPolynomial<*>,
    override val parent: IntermediateSymbol? = null,
    override var name: String,
    override var displayName: String? = null,
) : QuadraticFunctionSymbol {
    companion object {
        operator fun <
            T: ToQuadraticPolynomial<Poly>,
            Poly : AbstractQuadraticPolynomial<Poly>
        > invoke(
            x: T,
            parent: IntermediateSymbol? = null,
            name: String,
            displayName: String? = null,
        ): SemiFunction {
            return SemiFunction(
                x = x.toQuadraticPolynomial(),
                parent = parent,
                name = name,
                displayName = displayName
            )
        }
    }

    private val logger = logger()

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

    private val polyY: QuadraticPolynomial by lazy {
        val polyY = QuadraticPolynomial(y)
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
        } else {
            null
        }
    }

    override fun register(tokenTable: AddableTokenCollection): Try {
        when (val result = tokenTable.add(y)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = tokenTable.add(u)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }

    override fun register(model: AbstractQuadraticMechanismModel): Try {
        when (val result = model.addConstraint(
            y geq x,
            name = "${name}_lb",
            from = parent ?: this
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }
        when (val result = model.addConstraint(
            y leq x + m * u,
            name = "${name}_ub",
            from = parent ?: this
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }
        when (val result = model.addConstraint(
            y leq m * (Flt64.one - u),
            name = "${name}_yu",
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
        model: AbstractQuadraticMechanismModel,
        fixedValues: Map<Symbol, Flt64>
    ): Try {
        val xValue = x.evaluate(fixedValues, model.tokens) ?: return register(model)
        val yValue = max(Flt64.zero, xValue)
        val bin = xValue gr Flt64.zero

        when (val result = model.addConstraint(
            y geq x,
            name = "${name}_lb",
            from = parent ?: this
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }
        when (val result = model.addConstraint(
            y leq x + m * u,
            name = "${name}_ub",
            from = parent ?: this
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }
        when (val result = model.addConstraint(
            y leq m * (Flt64.one - u),
            name = "${name}_yu",
            from = parent ?: this
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = model.addConstraint(
            y eq yValue,
            name = "${name}_y",
            from = parent ?: this
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        model.tokens.find(y)?.let { token ->
            token._result = yValue
        }

        when (val result = model.addConstraint(
            u eq bin,
            name = "${name}_u",
            from = parent ?: this
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
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
        val xValue = x.evaluate(results, tokenList, zeroIfNone) ?: return null
        return max(Flt64.zero, xValue)
    }

    override fun evaluate(
        values: Map<Symbol, Flt64>,
        tokenList: AbstractTokenList?,
        zeroIfNone: Boolean
    ): Flt64? {
        val xValue = x.evaluate(values, tokenList, zeroIfNone) ?: return null
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
        val xValue = x.evaluate(results, tokenTable, zeroIfNone) ?: return null
        return max(Flt64.zero, xValue)
    }

    override fun calculateValue(
        values: Map<Symbol, Flt64>,
        tokenTable: AbstractTokenTable?,
        zeroIfNone: Boolean
    ): Flt64? {
        val xValue = x.evaluate(values, tokenTable, zeroIfNone) ?: return null
        return max(Flt64.zero, xValue)
    }
}
