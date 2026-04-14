@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package fuookami.ospf.kotlin.core.intermediate_symbol.legacy.linear_function

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
import fuookami.ospf.kotlin.core.variable.AbstractTokenList
import fuookami.ospf.kotlin.core.variable.AddableTokenCollection
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.core.variable.RealVar
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
    private val x: AbstractLinearPolynomial<*>,
    mask: AbstractLinearPolynomial<*>? = null,
    m: Flt64? = null,
    override val parent: IntermediateSymbol? = null,
    args: Any? = null,
    override var name: String,
    override var displayName: String? = null
) : LinearFunctionSymbol() {
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
            m: Flt64? = null,
            parent: IntermediateSymbol? = null,
            args: Any? = null,
            name: String,
            displayName: String? = null
        ): MaskingFunction {
            return MaskingFunction(
                x = x.toLinearPolynomial(),
                mask = mask.toLinearPolynomial(),
                m = m,
                parent = parent,
                args = args,
                name = name,
                displayName = displayName
            )
        }
    }

    internal val _args = args
    override val args get() = _args ?: parent?.args

    private val possibleUpperBound
        get() = max(
            abs(x.lowerBound!!.value.unwrap()),
            abs(x.upperBound!!.value.unwrap())
        )
    private val mFixed = m != null
    private var m = m ?: possibleUpperBound

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

    override fun flush(force: Boolean) {
        x.flush(force)
        mask.flush(force)
        y.range.set(possibleRange)
        polyY.flush(force)
        polyY.range.set(possibleRange)
        if (!mFixed) {
            m = possibleUpperBound
        }
    }

    override fun prepare(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable): Flt64? {
        return prepareIfNotCached(values, tokenTable) {
            val xValue = if (values.isNullOrEmpty()) {
                x.evaluate(tokenTable)
            } else {
                x.evaluate(
                    values = values,
                    tokenTable = tokenTable
                )
            } ?: return null

            val maskValue = if (values.isNullOrEmpty()) {
                mask.evaluate(tokenTable)
            } else {
                mask.evaluate(
                    values = values,
                    tokenTable = tokenTable
                )
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
            tokenTable.add(u).takeUnless { it.ok }?.let { return it }
        }

        tokenTable.add(y).takeUnless { it.ok }?.let { return it }

        return ok
    }

    override fun register(model: AbstractLinearMechanismModel): Try {
        if (mask.lowerBound!!.value.unwrap() ls Flt64.zero || mask.upperBound!!.value.unwrap() gr Flt64.one) {
            return Failed(
                code = ErrorCode.ApplicationFailed,
                message = "$name's domain of definition unsatisfied: $mask"
            )
        }

        model.addConstraint( relation = y leq x + m * (Flt64.one - mask), name = "${name}_ub", from = parent ?: this ).takeUnless { it.ok }?.let { return it }
        model.addConstraint( relation = y geq x - m * (Flt64.one - mask), name = "${name}_lb", from = parent ?: this ).takeUnless { it.ok }?.let { return it }
        model.addConstraint( relation = y leq m * mask, name = "${name}_ym_ub", from = parent ?: this ).takeUnless { it.ok }?.let { return it }
        model.addConstraint( relation = y geq -m * mask, name = "${name}_ym_lb", from = parent ?: this ).takeUnless { it.ok }?.let { return it }

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
        val xValue = x.evaluate(
            values = fixedValues,
            tokenTable = model.tokens
        ) ?: return register(model)
        val maskValue = mask.evaluate(
            values = fixedValues,
            tokenTable = model.tokens
        ) ?: return register(model)
        val maskBin = maskValue gr Flt64.zero

        model.addConstraint( y leq x + m * (Flt64.one - mask), name = "${name}_ub", from = parent ?: this ).takeUnless { it.ok }?.let { return it }
        model.addConstraint( y geq x - m * (Flt64.one - mask), name = "${name}_lb", from = parent ?: this ).takeUnless { it.ok }?.let { return it }
        model.addConstraint( y leq m * mask, name = "${name}_ym_ub", from = parent ?: this ).takeUnless { it.ok }?.let { return it }
        model.addConstraint( y geq -m * mask, name = "${name}_ym_lb", from = parent ?: this ).takeUnless { it.ok }?.let { return it }

        model.addConstraint( y eq if (maskBin) { xValue } else { Flt64.zero }, name = "${name}_y", from = parent ?: this ).takeUnless { it.ok }?.let { return it }

        model.tokens.find(y)?.let { token ->
            token._result = if (maskBin) {
                xValue
            } else {
                Flt64.zero
            }
        }

        if (!externalMask) {
            model.addConstraint( u eq maskBin, name = "${name}_u", from = parent ?: this ).takeUnless { it.ok }?.let { return it }

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





