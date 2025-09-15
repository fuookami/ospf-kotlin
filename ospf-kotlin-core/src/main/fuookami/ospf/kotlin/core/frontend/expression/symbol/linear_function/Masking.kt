package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function

import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

class MaskingFunction(
    private val x: AbstractLinearPolynomial<*>,
    mask: AbstractLinearPolynomial<*>? = null,
    override val parent: IntermediateSymbol? = null,
    override var name: String,
    override var displayName: String? = null
) : LinearFunctionSymbol {
    private val logger = logger()

    companion object {
        operator fun <
            T1 : ToLinearPolynomial<Poly1>,
            T2 : ToLinearPolynomial<Poly2>,
            Poly1 : AbstractLinearPolynomial<Poly1>,
            Poly2 : AbstractLinearPolynomial<Poly2>
        > invoke(
            x: T1,
            mask: T2,
            parent: IntermediateSymbol? = null,
            name: String,
            displayName: String? = null
        ): MaskingFunction {
            return MaskingFunction(
                x = x.toLinearPolynomial(),
                mask = mask.toLinearPolynomial(),
                parent = parent,
                name = name,
                displayName = displayName
            )
        }
    }

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
        val polyY = if (x.range.range?.contains(Flt64.zero) == true) {
            LinearPolynomial(x)
        } else {
            LinearPolynomial(y)
        }
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
            dependencies.addAll(mask.dependencies)
            return dependencies
        }
    override val cells get() = polyY.cells
    override val cached get() = polyY.cached

    private val possibleRange
        get() = ValueRange(
            if (x.lowerBound!!.value.unwrap() ls Flt64.zero) {
                x.lowerBound!!.value.unwrap()
            } else {
                Flt64.zero
            },
            if (x.upperBound!!.value.unwrap() geq Flt64.zero) {
                x.upperBound!!.value.unwrap()
            } else {
                Flt64.zero
            }
        ).value!!
    private val m: Flt64 by lazy {
        max(
            abs(x.lowerBound!!.value.unwrap()),
            abs(x.upperBound!!.value.unwrap())
        )
    }

    override fun flush(force: Boolean) {
        x.flush(force)
        mask.flush(force)
        polyY.flush(force)
        polyY.range.set(possibleRange)
    }

    override fun prepare(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable): Flt64? {
        x.cells
        mask.cells

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

            val maskValue = if (values.isNullOrEmpty()) {
                mask.evaluate(tokenTable)
            } else {
                mask.evaluate(values, tokenTable)
            }?.let {
                it gr Flt64.zero
            } ?: return null

            val yValue = if (maskValue) {
                xValue
            } else {
                Flt64.zero
            }

            if (x.range.range?.contains(Flt64.zero) != true) {
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

    override fun register(tokenTable: AbstractMutableTokenTable): Try {
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

        if (x.range.range?.contains(Flt64.zero) == true) {
            when (val result = model.addConstraint(
                x leq m * mask,
                name = "${name}_ub",
                from = parent ?: this
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
            when (val result = model.addConstraint(
                x geq -m * mask,
                name = "${name}_lb",
                from = parent ?: this
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        } else {
            when (val result = model.addConstraint(
                y leq x + m * (Flt64.one - mask),
                name = "${name}_ub",
                from = parent ?: this
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
            when (val result = model.addConstraint(
                y geq x - m * (Flt64.one - mask),
                name = "${name}_lb",
                from = parent ?: this
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
            when (val result = model.addConstraint(
                y leq m * mask,
                name = "${name}_ym_ub",
                from = parent ?: this
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
            when (val result = model.addConstraint(
                y geq -m * mask,
                name = "${name}_ym_lb",
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
        tokenTable: AbstractMutableTokenTable,
        fixedValues: Map<Symbol, Flt64>,
    ): Try {
        return register(tokenTable)
    }

    override fun register(
        model: AbstractLinearMechanismModel,
        fixedValues: Map<Symbol, Flt64>
    ): Try {
        val xValue = x.evaluate(fixedValues, model.tokens) ?: return register(model)
        val maskValue = mask.evaluate(fixedValues, model.tokens) ?: return register(model)
        val maskBin = maskValue gr Flt64.zero

        if (x.range.range?.contains(Flt64.zero) == true) {
            when (val result = model.addConstraint(
                x leq m * mask,
                name = "${name}_ub",
                from = parent ?: this
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
            when (val result = model.addConstraint(
                x geq -m * mask,
                name = "${name}_lb",
                from = parent ?: this
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        } else {
            when (val result = model.addConstraint(
                y leq x + m * (Flt64.one - mask),
                name = "${name}_ub",
                from = parent ?: this
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
            when (val result = model.addConstraint(
                y geq x - m * (Flt64.one - mask),
                name = "${name}_lb",
                from = parent ?: this
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
            when (val result = model.addConstraint(
                y leq m * mask,
                name = "${name}_ym_ub",
                from = parent ?: this
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
            when (val result = model.addConstraint(
                y geq -m * mask,
                name = "${name}_ym_lb",
                from = parent ?: this
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }

            when (val result = model.addConstraint(
                y eq if (maskBin) {
                    xValue
                } else {
                    Flt64.zero
                },
                name = "${name}_y",
                from = parent ?: this
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }

            model.tokens.find(y)?.let { token ->
                token._result = if (maskBin) {
                    xValue
                } else {
                    Flt64.zero
                }
            }
        }

        if (!externalMask) {
            when (val result = model.addConstraint(
                u eq maskBin,
                name = "${name}_u",
                from = parent ?: this
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }

            model.tokens.find(u)?.let { token ->
                token._result = maskBin.toFlt64()
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
            "masking(${x.toTidyRawString(unfold - UInt64.one)}, ${mask.toTidyRawString(unfold - UInt64.one)})"
        }
    }

    override fun evaluate(
        tokenList: AbstractTokenList,
        zeroIfNone: Boolean
    ): Flt64? {
        val maskValue = mask.evaluate(tokenList, zeroIfNone) ?: return null
        return if (maskValue neq Flt64.zero) {
            x.evaluate(tokenList, zeroIfNone)
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
            x.evaluate(results, tokenList, zeroIfNone)
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
            x.evaluate(values, tokenList, zeroIfNone)
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
            x.evaluate(tokenTable, zeroIfNone)
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
            x.evaluate(results, tokenTable, zeroIfNone)
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
            x.evaluate(values, tokenTable, zeroIfNone)
        } else {
            Flt64.zero
        }
    }
}
