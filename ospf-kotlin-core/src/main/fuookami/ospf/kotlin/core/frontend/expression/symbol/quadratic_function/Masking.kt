package fuookami.ospf.kotlin.core.frontend.expression.symbol.quadratic_function

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
    private val x: AbstractQuadraticPolynomial<*>,
    private val mask: AbstractQuadraticPolynomial<*>,
    override var name: String,
    override var displayName: String? = null
) : QuadraticFunctionSymbol {
    companion object {
        operator fun <
            T : ToQuadraticPolynomial<Poly>,
            Poly : AbstractQuadraticPolynomial<Poly>
        > invoke(
            x: T,
            mask: AbstractQuadraticPolynomial<*>,
            name: String,
            displayName: String? = null
        ): MaskingFunction {
            return MaskingFunction(
                x.toQuadraticPolynomial(),
                mask,
                name,
                displayName
            )
        }

        operator fun <
            T : ToQuadraticPolynomial<Poly>,
            Poly : AbstractQuadraticPolynomial<Poly>
        > invoke(
            x: AbstractQuadraticPolynomial<*>,
            mask: T,
            name: String,
            displayName: String? = null
        ): MaskingFunction {
            return MaskingFunction(
                x,
                mask.toQuadraticPolynomial(),
                name,
                displayName
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
            name: String,
            displayName: String? = null
        ): MaskingFunction {
            return MaskingFunction(
                x.toQuadraticPolynomial(),
                mask.toQuadraticPolynomial(),
                name,
                displayName
            )
        }
    }

    private val logger = logger()

    private val y: RealVar by lazy {
        val y = RealVar("${name}_y")
        y.range.set(possibleRange)
        y
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

    override fun prepare(tokenTable: AbstractTokenTable): Flt64? {
        x.cells
        mask.cells

        return if (tokenTable.cachedSolution && tokenTable.cached(this) == false) {
            val xValue = x.evaluate(tokenTable) ?: return null

            val maskValue = mask?.evaluate(tokenTable)?.let {
                it gr Flt64.zero
            } ?: return null


            val yValue = if (maskValue) {
                xValue
            } else {
                Flt64.zero
            }

            logger.trace { "Setting SemiFunction ${name}.y to $yValue" }
            tokenTable.find(y)?.let { token ->
                token._result = yValue
            }

            yValue
        } else {
            null
        }
    }

    override fun register(tokenTable: AbstractMutableTokenTable): Try {
        when (val result = tokenTable.add(y)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }

    override fun register(model: AbstractQuadraticMechanismModel): Try {
        if (mask.lowerBound!!.value.unwrap() ls Flt64.zero || mask.upperBound!!.value.unwrap() gr Flt64.one) {
            return Failed(Err(ErrorCode.ApplicationFailed, "$name's domain of definition unsatisfied: $mask"))
        }

        when (val result = model.addConstraint(
            y leq x + m * (Flt64.one - mask),
            name = "${name}_lb"
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }
        when (val result = model.addConstraint(
            y geq x - m * (Flt64.one - mask),
            name = "${name}_ub"
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }
        when (val result = model.addConstraint(
            y leq m * mask,
            name = "${name}_ym_lb"
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }
        when (val result = model.addConstraint(
            y geq -m * mask,
            name = "${name}_ym_ub"
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
}
