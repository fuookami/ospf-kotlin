package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function

import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

class MaskingRangeFunction(
    mask: AbstractLinearPolynomial<*>? = null,
    private val lb: Flt64,
    private val ub: Flt64,
    override val parent: IntermediateSymbol? = null,
    args: Any? = null,
    override var name: String,
    override var displayName: String? = null
) : LinearFunctionSymbol() {
    private val logger = logger()

    companion object {
        operator fun <
            T : ToLinearPolynomial<Poly>,
            Poly : AbstractLinearPolynomial<Poly>
        > invoke(
            mask: T,
            lb: Int,
            ub: Int,
            parent: IntermediateSymbol? = null,
            args: Any? = null,
            name: String,
            displayName: String? = null
        ): MaskingRangeFunction {
            return MaskingRangeFunction(
                mask = mask.toLinearPolynomial(),
                lb = Flt64(lb),
                ub = Flt64(ub),
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
            mask: T,
            lb: Double,
            ub: Double,
            parent: IntermediateSymbol? = null,
            args: Any? = null,
            name: String,
            displayName: String? = null
        ): MaskingRangeFunction {
            return MaskingRangeFunction(
                mask = mask.toLinearPolynomial(),
                lb = Flt64(lb),
                ub = Flt64(ub),
                parent = parent,
                args = args,
                name = name,
                displayName = displayName
            )
        }

        operator fun <
            T1 : ToLinearPolynomial<Poly>,
            T2 : RealNumber<T2>,
            T3 : RealNumber<T3>,
            Poly : AbstractLinearPolynomial<Poly>
        > invoke(
            mask: T1,
            lb: T2,
            ub: T3,
            parent: IntermediateSymbol? = null,
            args: Any? = null,
            name: String,
            displayName: String? = null
        ): MaskingRangeFunction {
            return MaskingRangeFunction(
                mask = mask.toLinearPolynomial(),
                lb = lb.toFlt64(),
                ub = ub.toFlt64(),
                parent = parent,
                args = args,
                name = name,
                displayName = displayName
            )
        }
    }

    internal val _args = args
    override val args get() = _args ?: parent?.args

    private val u: BinVar by lazy {
        BinVar("${name}_u")
    }

    private val externalMask: Boolean = mask != null
    private val mask: AbstractLinearPolynomial<*> by lazy {
        mask ?: LinearPolynomial(u)
    }

    private val y: RealVar by lazy {
        val y = RealVar("${name}_y")
        y.range.set(possibleRange)
        y
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

    override val dependencies: Set<IntermediateSymbol>
        get() {
            val dependencies = HashSet<IntermediateSymbol>()
            dependencies.addAll(mask.dependencies)
            return dependencies
        }
    override val cells get() = polyY.cells
    override val cached get() = polyY.cached

    private val possibleRange
        get() = ValueRange(
            if (lb ls Flt64.zero) {
                lb
            } else {
                Flt64.zero
            },
            if (ub geq Flt64.zero) {
                ub
            } else {
                Flt64.zero
            }
        ).value!!

    override fun flush(force: Boolean) {
        mask.flush(force)
        polyY.flush(force)
        polyY.range.set(possibleRange)
    }

    override fun prepare(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable): Flt64? {
        mask.cells

        return if ((!values.isNullOrEmpty() || tokenTable.cachedSolution) && if (values.isNullOrEmpty()) {
            tokenTable.cached(this)
        } else {
            tokenTable.cached(this, values)
        } == false) {
            val maskValue = if (values.isNullOrEmpty()) {
                mask.evaluate(tokenTable)
            } else {
                mask.evaluate(values, tokenTable)
            }?.let {
                it gr Flt64.zero
            } ?: return null

            val yValue = if (!maskValue) {
                Flt64.zero
            } else {
                null
            }

            if (yValue != null) {
                logger.trace { "Setting MaskingFunction ${name}.y to $yValue" }
                tokenTable.find(y)?.let { token ->
                    token._result = yValue
                }
            }

            yValue
        } else {
            null
        }
    }

    override fun register(tokenTable: AddableTokenCollection): Try {
        if (!externalMask) {
            when (val result = tokenTable.add(u)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        when (val result = tokenTable.add(y)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }

    override fun register(model: AbstractLinearMechanismModel): Try {
        if (mask.lowerBound!!.value.unwrap() ls Flt64.zero || mask.upperBound!!.value.unwrap() gr Flt64.one) {
            return Failed(Err(ErrorCode.ApplicationFailed, "$name's domain of definition unsatisfied: $mask"))
        }

        when (val result = model.addConstraint(
            y leq ub * mask,
            name = "${name}_ub",
            from = parent ?: this
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }
        when (val result = model.addConstraint(
            y geq lb * mask,
            name = "${name}_lb",
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
        fixedValues: Map<Symbol, Flt64>,
    ): Try {
        return register(tokenTable)
    }

    override fun register(
        model: AbstractLinearMechanismModel,
        fixedValues: Map<Symbol, Flt64>
    ): Try {
        val maskValue = mask.evaluate(fixedValues, model.tokens) ?: return register(model)
        val maskBin = maskValue gr Flt64.zero

        when (val result = model.addConstraint(
            y leq ub * mask,
            name = "${name}_ub",
            from = parent ?: this
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }
        when (val result = model.addConstraint(
            y geq lb * mask,
            name = "${name}_lb",
            from = parent ?: this
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
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
            "masking_range(${lb}, ${ub}, ${mask.toTidyRawString(unfold - UInt64.one)})"
        }
    }

    override fun evaluate(
        tokenList: AbstractTokenList,
        zeroIfNone: Boolean
    ): Flt64? {
        val maskValue = mask.evaluate(tokenList, zeroIfNone) ?: return null
        return if (maskValue neq Flt64.zero) {
            polyY.evaluate(tokenList, zeroIfNone)
        } else {
            Flt64.zero
        }
    }

    override fun evaluate(
        results: List<Flt64>,
        tokenList: AbstractTokenList,
        zeroIfNone: Boolean
    ): Flt64? {
        val maskValue = mask.evaluate(results, tokenList, zeroIfNone) ?: return null
        return if (maskValue neq Flt64.zero) {
            polyY.evaluate(results, tokenList, zeroIfNone)
        } else {
            Flt64.zero
        }
    }

    override fun evaluate(
        values: Map<Symbol, Flt64>,
        tokenList: AbstractTokenList?,
        zeroIfNone: Boolean
    ): Flt64? {
        val maskValue = mask.evaluate(values, tokenList, zeroIfNone) ?: return null
        return if (maskValue neq Flt64.zero) {
            polyY.evaluate(values, tokenList, zeroIfNone)
        } else {
            Flt64.zero
        }
    }

    override fun calculateValue(
        tokenTable: AbstractTokenTable,
        zeroIfNone: Boolean
    ): Flt64? {
        val maskValue = mask.evaluate(tokenTable, zeroIfNone) ?: return null
        return if (maskValue neq Flt64.zero) {
            polyY.evaluate(tokenTable, zeroIfNone)
        } else {
            Flt64.zero
        }
    }

    override fun calculateValue(
        results: List<Flt64>,
        tokenTable: AbstractTokenTable,
        zeroIfNone: Boolean
    ): Flt64? {
        val maskValue = mask.evaluate(results, tokenTable, zeroIfNone) ?: return null
        return if (maskValue neq Flt64.zero) {
            polyY.evaluate(results, tokenTable, zeroIfNone)
        } else {
            Flt64.zero
        }
    }

    override fun calculateValue(
        values: Map<Symbol, Flt64>,
        tokenTable: AbstractTokenTable?,
        zeroIfNone: Boolean
    ): Flt64? {
        val maskValue = mask.evaluate(values, tokenTable, zeroIfNone) ?: return null
        return if (maskValue neq Flt64.zero) {
            polyY.evaluate(values, tokenTable, zeroIfNone)
        } else {
            Flt64.zero
        }
    }
}
