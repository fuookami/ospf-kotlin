package fuookami.ospf.kotlin.core.frontend.expression.symbol.quadratic_function

import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.geometry.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

class BinaryzationFunction(
    private val x: AbstractQuadraticPolynomial<*>,
    private val epsilon: Flt64 = Flt64(1e-6),
    override var name: String,
    override var displayName: String? = null
) : QuadraticFunctionSymbol {
    private val logger = logger()

    private lateinit var linearX: LinearFunction
    private lateinit var y: BinVar
    private lateinit var polyY: AbstractQuadraticPolynomial<*>

    override val discrete = true

    override val range get() = polyY.range
    override val lowerBound
        get() = if (::polyY.isInitialized) {
            polyY.lowerBound
        } else {
            possibleRange.lowerBound.toFlt64()
        }
    override val upperBound
        get() = if (::polyY.isInitialized) {
            polyY.upperBound
        } else {
            possibleRange.upperBound.toFlt64()
        }

    override val category: Category = Linear

    override val dependencies by x::dependencies
    override val cells get() = polyY.cells
    override val cached
        get() = if (::polyY.isInitialized) {
            polyY.cached
        } else {
            false
        }

    private val possibleRange
        get() = ValueRange(
            if (x.lowerBound eq Flt64.zero) {
                UInt8.zero
            } else {
                UInt8.one
            },
            if (x.upperBound eq Flt64.zero) {
                UInt8.zero
            } else {
                UInt8.one
            }
        )

    override fun flush(force: Boolean) {
        if (::polyY.isInitialized) {
            polyY.flush(force)
            polyY.range.set(possibleRange.toFlt64())
        }
    }

    override suspend fun prepare(tokenTable: AbstractTokenTable) {
        linearX.prepare(tokenTable)

        if (tokenTable.tokenList.tokens.any { it.result != null } && ::y.isInitialized) {
            linearX.value(tokenTable)?.let {
                val value = if (it geq epsilon) { Flt64.one } else { Flt64.zero }
                logger.trace { "Setting BinaryzationFunction ${name}.y initial solution: value" }
                tokenTable.find(y)?.let { token -> token._result = value }
                when (tokenTable) {
                    is AutoAddTokenTable -> {
                        tokenTable.cachedSymbolValue[this to null] = it
                    }

                    is MutableTokenTable -> {
                        tokenTable.cachedSymbolValue[this to null] = it
                    }

                    else -> {}
                }
            }
        }
    }

    override fun register(tokenTable: MutableTokenTable): Try {
        if (x.discrete && x.range.range in ValueRange(Flt64.zero, Flt64.one)) {
            polyY = x
            return ok
        }

        if (x.lowerBound ls Flt64.zero) {
            return Failed(Err(ErrorCode.ApplicationFailed, "$name's domain of definition unsatisfied: $x"))
        }

        if (!::linearX.isInitialized) {
            linearX = LinearFunction(x, "${name}_linear")
        }
        when (val result = linearX.register(tokenTable)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        if (!::y.isInitialized) {
            y = BinVar("${name}_y")
            y.range.set(possibleRange)
        }
        when (val result = tokenTable.add(y)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        if (!::polyY.isInitialized) {
            polyY = QuadraticPolynomial(y)
            polyY.range.set(possibleRange.toFlt64())
        }

        return ok
    }

    override fun register(model: AbstractQuadraticMechanismModel): Try {
        if (::linearX.isInitialized) {
            when (val result = linearX.register(model)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
            if (::y.isInitialized) {
                model.addConstraint(
                    (Flt64.one - y) * linearX leq x.upperBound * y,
                    "${name}_ub"
                )
                model.addConstraint(
                    x geq epsilon * y,
                    "${name}_lb"
                )
            }
        }

        return ok
    }

    override fun toString(): String {
        return displayName ?: name
    }

    override fun toRawString(unfold: Boolean): String {
        return "bin(${x.toRawString(unfold)})"
    }

    override fun value(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        val value = x.value(tokenList, zeroIfNone)
            ?: return null
        return if (value neq Flt64.zero) {
            Flt64.one
        } else {
            Flt64.zero
        }
    }

    override fun value(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        val value = x.value(results, tokenList, zeroIfNone)
            ?: return null
        return if (value neq Flt64.zero) {
            Flt64.one
        } else {
            Flt64.zero
        }
    }

    override fun calculateValue(tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        val value = x.value(tokenTable, zeroIfNone)
            ?: return null
        return if (value neq Flt64.zero) {
            Flt64.one
        } else {
            Flt64.zero
        }
    }

    override fun calculateValue(results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        val value = x.value(results, tokenTable, zeroIfNone)
            ?: return null
        return if (value neq Flt64.zero) {
            Flt64.one
        } else {
            Flt64.zero
        }
    }
}
