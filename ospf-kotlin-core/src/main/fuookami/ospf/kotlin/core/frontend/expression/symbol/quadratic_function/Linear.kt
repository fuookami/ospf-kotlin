package fuookami.ospf.kotlin.core.frontend.expression.symbol.quadratic_function

import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

class LinearFunction(
    val polynomial: AbstractQuadraticPolynomial<*>,
    override var name: String,
    override var displayName: String? = null
) : QuadraticFunctionSymbol {
    private val logger = logger()

    private lateinit var y: RealVar
    private lateinit var polyY: AbstractQuadraticPolynomial<*>

    override val discrete = polynomial.discrete

    override val range get() = polyY.range
    override val lowerBound
        get() = if (::polyY.isInitialized) {
            polyY.lowerBound
        } else {
            polynomial.lowerBound.toFlt64()
        }
    override val upperBound
        get() = if (::polyY.isInitialized) {
            polyY.upperBound
        } else {
            polynomial.upperBound.toFlt64()
        }

    override val category: Category = Linear

    override val dependencies: Set<Symbol> get() = polynomial.dependencies
    override val cells get() = polyY.cells
    override val cached
        get() = if (::polyY.isInitialized) {
            polyY.cached
        } else {
            false
        }

    override fun flush(force: Boolean) {
        if (::polyY.isInitialized) {
            polyY.flush(force)
            y.range.set(polynomial.range.valueRange)
            polyY.range.set(polynomial.range.valueRange)
        }
    }

    override suspend fun prepare(tokenTable: AbstractTokenTable) {
        polynomial.cells

        if (tokenTable.tokenList.tokens.any { it.result != null } && ::y.isInitialized) {
            polynomial.value(tokenTable)?.let {
                logger.trace { "Setting LinearFunction ${name}.y initial solution: $it" }
                tokenTable.find(y)?.let { token -> token._result = it }
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
        if (polynomial.category == Linear) {
            polyY = polynomial
        } else {
            if (!::y.isInitialized) {
                y = RealVar("${name}_y")
            }
            when (val result = tokenTable.add(y)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }

            if (!::polyY.isInitialized) {
                polyY = QuadraticPolynomial(y)
                polyY.range.set(polynomial.range.valueRange)
            }
        }

        return ok
    }

    override fun register(model: AbstractQuadraticMechanismModel): Try {
        if (::y.isInitialized) {
            when (val result = model.addConstraint(
                y eq polynomial,
                name
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        return ok
    }

    override fun toString(): String {
        return displayName ?: name
    }

    override fun toRawString(unfold: Boolean): String {
        return "linear(${polynomial.toRawString(unfold)})"
    }

    override fun value(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return polynomial.value(tokenList, zeroIfNone)
    }

    override fun value(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return polynomial.value(results, tokenList, zeroIfNone)
    }

    override fun calculateValue(tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        return polynomial.value(tokenTable, zeroIfNone)
    }

    override fun calculateValue(results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        return polynomial.value(results, tokenTable, zeroIfNone)
    }
}
