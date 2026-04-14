@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package fuookami.ospf.kotlin.core.intermediate_symbol.legacy.quadratic_function

import fuookami.ospf.kotlin.core.expression.polynomial.*
import fuookami.ospf.kotlin.core.intermediate_symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.QuadraticFunctionSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.prepareIfNotCached
import fuookami.ospf.kotlin.core.intermediate_symbol.toTidyRawString
import fuookami.ospf.kotlin.core.intermediate_model.eq
import fuookami.ospf.kotlin.core.intermediate_model.geq
import fuookami.ospf.kotlin.core.intermediate_model.leq
import fuookami.ospf.kotlin.core.intermediate_model.AbstractQuadraticMechanismModel
import fuookami.ospf.kotlin.core.intermediate_model.AbstractTokenTable
import fuookami.ospf.kotlin.core.variable.AbstractTokenList
import fuookami.ospf.kotlin.core.variable.AddableTokenCollection
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.utils.error.Err
import fuookami.ospf.kotlin.utils.error.ErrorCode
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

class MaskingFunction(
    private val x: AbstractQuadraticPolynomial<*>,
    mask: AbstractQuadraticPolynomial<*>? = null,
    override val parent: IntermediateSymbol? = null,
    args: Any? = null,
    override var name: String,
    override var displayName: String? = null
) : QuadraticFunctionSymbol() {
    private val logger = logger()

    companion object {
        operator fun <
                T : ToQuadraticPolynomial<Poly>,
                Poly : AbstractQuadraticPolynomial<Poly>
                > invoke(
            x: T,
            mask: AbstractQuadraticPolynomial<*>,
            parent: IntermediateSymbol? = null,
            args: Any? = null,
            name: String,
            displayName: String? = null
        ): MaskingFunction {
            return MaskingFunction(
                x = x.toQuadraticPolynomial(),
                mask = mask,
                parent = parent,
                args = args,
                name = name,
                displayName = displayName
            )
        }

        operator fun <
                T : ToQuadraticPolynomial<Poly>,
                Poly : AbstractQuadraticPolynomial<Poly>
                > invoke(
            x: AbstractQuadraticPolynomial<*>,
            mask: T,
            parent: IntermediateSymbol? = null,
            args: Any? = null,
            name: String,
            displayName: String? = null
        ): MaskingFunction {
            return MaskingFunction(
                x = x,
                mask = mask.toQuadraticPolynomial(),
                parent = parent,
                args = args,
                name = name,
                displayName = displayName
            )
        }

        operator fun <
                T1 : ToQuadraticPolynomial<Poly1>,
                T2 : ToQuadraticPolynomial<Poly2>,
                Poly1 : AbstractQuadraticPolynomial<Poly1>,
                Poly2 : AbstractQuadraticPolynomial<Poly2>
                > invoke(
            x: T1,
            mask: T2,
            parent: IntermediateSymbol? = null,
            args: Any? = null,
            name: String,
            displayName: String? = null
        ): MaskingFunction {
            return MaskingFunction(
                x = x.toQuadraticPolynomial(),
                mask = mask.toQuadraticPolynomial(),
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
    private val mask: AbstractQuadraticPolynomial<*> by lazy {
        mask ?: QuadraticPolynomial(u)
    }

    private val y: RealVar by lazy {
        val y = RealVar("${name}_y")
        y.range.set(possibleRange)
        y
    }

    private val polyY: QuadraticPolynomial by lazy {
        val polyY = if (x.range.range?.contains(Flt64.zero) == true) {
            QuadraticPolynomial(x)
        } else {
            QuadraticPolynomial(y)
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
        return prepareIfNotCached(values, tokenTable) {
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
        }
    }

    override fun register(tokenTable: AddableTokenCollection): Try {
        if (!externalMask) {
            when (val result = tokenTable.add(u)) {
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
        }

        when (val result = tokenTable.add(y)) {
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

    override fun register(model: AbstractQuadraticMechanismModel): Try {
        if (mask.lowerBound!!.value.unwrap() ls Flt64.zero || mask.upperBound!!.value.unwrap() gr Flt64.one) {
            return Failed(Err(ErrorCode.ApplicationFailed, "$name's domain of definition unsatisfied: $mask"))
        }

        if (x.range.range?.contains(Flt64.zero) == true) {
            when (val result = model.addConstraint(
                relation = x leq m * mask,
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
                relation = x geq -m * mask,
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
        } else {
            when (val result = model.addConstraint(
                relation = y leq x + m * (Flt64.one - mask),
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
                relation = y geq x - m * (Flt64.one - mask),
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
                relation = y leq m * mask,
                name = "${name}_ym_ub",
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
                relation = y geq -m * mask,
                name = "${name}_ym_lb",
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
        model: AbstractQuadraticMechanismModel,
        fixedValues: Map<Symbol, Flt64>
    ): Try {
        val xValue = x.evaluate(fixedValues, model.tokens) ?: return register(model)
        val maskValue = mask.evaluate(fixedValues, model.tokens) ?: return register(model)
        val maskBin = maskValue gr Flt64.zero

        if (x.range.range?.contains(Flt64.zero) == true) {
            when (val result = model.addConstraint(
                relation = x leq m * mask,
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
                relation = x geq -m * mask,
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
        } else {
            when (val result = model.addConstraint(
                relation = y leq x + m * (Flt64.one - mask),
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
                relation = y geq x - m * (Flt64.one - mask),
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
                relation = y leq m * mask,
                name = "${name}_ym_ub",
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
                relation = y geq -m * mask,
                name = "${name}_ym_lb",
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
                relation = y eq if (maskBin) {
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

                is Fatal -> {
                    return Fatal(result.errors)
                }

                is Fatal -> {
                    return Fatal(result.errors)
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
                relation = u eq maskBin,
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
        val maskValue = mask.evaluate(
            results = results,
            tokenList = tokenList,
            zeroIfNone = zeroIfNone
        ) ?: return null
        return if (maskValue neq Flt64.zero) {
            x.evaluate(
                results = results,
                tokenList = tokenList,
                zeroIfNone = zeroIfNone
            )
        } else {
            Flt64.zero
        }
    }

    override fun evaluate(
        values: Map<Symbol, Flt64>,
        tokenList: AbstractTokenList?,
        zeroIfNone: Boolean
    ): Flt64? {
        val maskValue = mask.evaluate(
            values = values,
            tokenList = tokenList,
            zeroIfNone = zeroIfNone
        ) ?: return null
        return if (maskValue neq Flt64.zero) {
            x.evaluate(
                values = values,
                tokenList = tokenList,
                zeroIfNone = zeroIfNone
            )
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
        val maskValue = mask.evaluate(
            results = results,
            tokenTable = tokenTable,
            zeroIfNone = zeroIfNone
        ) ?: return null
        return if (maskValue neq Flt64.zero) {
            x.evaluate(
                results = results,
                tokenTable = tokenTable,
                zeroIfNone = zeroIfNone
            )
        } else {
            Flt64.zero
        }
    }

    override fun calculateValue(
        values: Map<Symbol, Flt64>,
        tokenTable: AbstractTokenTable?,
        zeroIfNone: Boolean
    ): Flt64? {
        val maskValue = mask.evaluate(
            values = values,
            tokenTable = tokenTable,
            zeroIfNone = zeroIfNone
        ) ?: return null
        return if (maskValue neq Flt64.zero) {
            x.evaluate(
                values = values,
                tokenTable = tokenTable,
                zeroIfNone = zeroIfNone
            )
        } else {
            Flt64.zero
        }
    }
}





