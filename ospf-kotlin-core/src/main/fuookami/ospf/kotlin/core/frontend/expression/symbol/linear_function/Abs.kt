package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function

import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

class AbsFunction(
    private val x: AbstractLinearPolynomial<*>,
    private val extract: Boolean = true,
    m: Flt64? = null,
    override val parent: IntermediateSymbol? = null,
    args: Any? = null,
    override var name: String = "${x}_abs",
    override var displayName: String? = "|$x|"
) : LinearFunctionSymbol() {
    private val logger = logger()

    companion object {
        operator fun <T : ToLinearPolynomial<Poly>, Poly : AbstractLinearPolynomial<Poly>> invoke(
            x: T,
            extract: Boolean = true,
            parent: IntermediateSymbol? = null,
            args: Any? = null,
            name: String,
            displayName: String? = null,
        ): AbsFunction {
            return AbsFunction(
                x = x.toLinearPolynomial(),
                extract = extract,
                parent = parent,
                args = args,
                name = name,
                displayName = displayName
            )
        }
    }

    private val _args = args
    override val args get() = _args ?: parent?.args

    private val possibleUpperBound get() = max(
        abs(x.lowerBound!!.value.unwrap()),
        abs(x.upperBound!!.value.unwrap())
    )
    private val mFixed = m != null
    private var m = m ?: possibleUpperBound

    private val neg: PctVar by lazy {
        PctVar("${name}_neg")
    }

    private val pos: PctVar by lazy {
        PctVar("${name}_pos")
    }

    private val p: BinVar by lazy {
        BinVar("${name}_p")
    }

    private val y: MutableLinearPolynomial by lazy {
        val polyY = MutableLinearPolynomial(
            (this.m * pos + this.m * neg).toMutable(),
            "${name}_abs_y"
        )
        polyY.range.set(ValueRange(Flt64.zero, this.m).value!!)
        polyY
    }

    override val discrete by lazy {
        x.discrete
    }

    override val range get() = y.range
    override val lowerBound get() = y.lowerBound
    override val upperBound get() = y.upperBound

    override val category = Linear

    override val dependencies by x::dependencies
    override val cells get() = y.cells
    override val cached get() = y.cached

    override fun flush(force: Boolean) {
        x.flush(force)
        y.flush(force)
        if (!mFixed) {
            val newM = possibleUpperBound
            if (m neq newM) {
                y.range.set(ValueRange(Flt64.zero, m).value!!)
                y.asMutable() *= newM / m
                m = newM
            }
        }
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

            val pValue = xValue geq Flt64.zero
            val yValue = abs(xValue)
            val posValue = if (pValue) {
                yValue / m
            } else {
                Flt64.zero
            }
            val negValue = if (!pValue) {
                yValue / m
            } else {
                Flt64.zero
            }
            logger.trace { "Setting AbsFunction ${name}.pos initial solution: $posValue" }
            tokenTable.find(pos)?.let { token -> token._result = posValue }
            logger.trace { "Setting AbsFunction ${name}.neg initial solution: $negValue" }
            tokenTable.find(neg)?.let { token -> token._result = negValue }
            logger.trace { "Setting AbsFunction ${name}.p initial solution: $pValue" }
            tokenTable.find(p)?.let { token ->
                token._result = pValue.toFlt64()
            }

            yValue
        } else {
            null
        }
    }

    override fun register(tokenTable: AddableTokenCollection): Try {
        when (val result = tokenTable.add(listOf(neg, pos))) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        if (extract) {
            when (val result = tokenTable.add(p)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        y.range.set(ValueRange(Flt64.zero, m).value!!)

        return ok
    }

    override fun register(model: AbstractLinearMechanismModel): Try {
        when (val result = model.addConstraint(
            x eq (-m * neg + m * pos),
            name = name,
            from = parent ?: this
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        if (extract) {
            when (val result = model.addConstraint(
                neg + pos leq Flt64.one,
                name = "${name}_b",
                from = parent ?: this
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }

            when (val result = model.addConstraint(
                p geq pos,
                name = "${name}_p",
                from = parent ?: this
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }

            when (val result = model.addConstraint(
                neg leq Flt64.one - p,
                name = "${name}_n",
                from = parent ?: this
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
        val xValue = when (tokenTable) {
            is AbstractTokenTable -> {
                x.evaluate(fixedValues, tokenTable) ?: return register(tokenTable)
            }

            is FunctionSymbolRegistrationScope -> {
                x.evaluate(fixedValues, tokenTable.origin) ?: return register(tokenTable)
            }

            else -> {
                return register(tokenTable)
            }
        }

        if (xValue geq Flt64.zero) {
            when (val result = tokenTable.add(pos)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        } else {
            when (val result = tokenTable.add(neg)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        return ok
    }

    override fun register(
        model: AbstractLinearMechanismModel,
        fixedValues: Map<Symbol, Flt64>
    ): Try {
        val xValue = x.evaluate(fixedValues, model.tokens) ?: return register(model)

        if (xValue geq Flt64.zero) {
            when (val result = model.addConstraint(
                x eq m * pos,
                name = name,
                from = parent ?: this
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }

            model.tokens.find(pos)?.let { token ->
                token._result = xValue / m
            }
        } else {
            when (val result = model.addConstraint(
                x eq -m * neg,
                name = name,
                from = parent ?: this
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }

            model.tokens.find(neg)?.let { token ->
                token._result = -xValue / m
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
            "|${x.toTidyRawString(unfold - UInt64.zero)}|"
        }
    }

    override fun evaluate(
        tokenList: AbstractTokenList,
        zeroIfNone: Boolean
    ): Flt64? {
        return x.evaluate(tokenList, zeroIfNone)?.let {
            abs(it)
        }
    }

    override fun evaluate(
        results: List<Flt64>,
        tokenList: AbstractTokenList,
        zeroIfNone: Boolean
    ): Flt64? {
        return x.evaluate(results, tokenList, zeroIfNone)?.let {
            abs(it)
        }
    }

    override fun evaluate(
        values: Map<Symbol, Flt64>,
        tokenList: AbstractTokenList?,
        zeroIfNone: Boolean
    ): Flt64? {
        return x.evaluate(values, tokenList, zeroIfNone)?.let {
            abs(it)
        }
    }

    override fun calculateValue(
        tokenTable: AbstractTokenTable,
        zeroIfNone: Boolean
    ): Flt64? {
        return x.evaluate(tokenTable, zeroIfNone)?.let { abs(it) }
    }

    override fun calculateValue(
        results: List<Flt64>,
        tokenTable: AbstractTokenTable,
        zeroIfNone: Boolean
    ): Flt64? {
        return x.evaluate(results, tokenTable, zeroIfNone)?.let { abs(it) }
    }

    override fun calculateValue(
        values: Map<Symbol, Flt64>,
        tokenTable: AbstractTokenTable?,
        zeroIfNone: Boolean
    ): Flt64? {
        return x.evaluate(values, tokenTable, zeroIfNone)?.let { abs(it) }
    }
}
