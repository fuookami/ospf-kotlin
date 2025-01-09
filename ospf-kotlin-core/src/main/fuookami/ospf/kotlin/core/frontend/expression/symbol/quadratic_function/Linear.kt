package fuookami.ospf.kotlin.core.frontend.expression.symbol.quadratic_function

import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
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

    private val y: RealVar by lazy {
        RealVar("${name}_y")
    }

    private val polyY: AbstractQuadraticPolynomial<*> by lazy {
        if (polynomial.category == Linear) {
            polynomial.copy()
        } else {
            val polyY = QuadraticPolynomial(y)
            polyY.range.set(polynomial.range.valueRange!!)
            polyY
        }
    }

    override val discrete = polynomial.discrete

    override val range get() = polyY.range
    override val lowerBound get() = polyY.lowerBound
    override val upperBound get() = polyY.upperBound

    override val category = Linear

    override val dependencies: Set<IntermediateSymbol> get() = polynomial.dependencies
    override val cells get() = polyY.cells
    override val cached get() = polyY.cached

    override fun flush(force: Boolean) {
        polynomial.flush(force)
        polyY.flush(force)
        y.range.set(polynomial.range.valueRange!!)
        polyY.range.set(polynomial.range.valueRange!!)
    }

    override fun prepare(tokenTable: AbstractTokenTable) {
        polynomial.cells

        if (tokenTable.cachedSolution && tokenTable.cached(this) == false) {
            polynomial.evaluate(tokenTable)?.let { yValue ->
                if (polynomial.category != Linear) {
                    logger.trace { "Setting LinearFunction ${name}.y initial solution: $yValue" }
                    tokenTable.find(y)?.let { token ->
                        token._result = yValue
                    }
                }

                tokenTable.cache(this, null, yValue)
            }
        }
    }

    override fun register(tokenTable: AbstractMutableTokenTable): Try {
        if (polynomial.category != Linear) {
            when (val result = tokenTable.add(y)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        return ok
    }

    override fun register(model: AbstractQuadraticMechanismModel): Try {
        if (polynomial.category != Linear) {
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

    override fun evaluate(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return polynomial.evaluate(tokenList, zeroIfNone)
    }

    override fun evaluate(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return polynomial.evaluate(results, tokenList, zeroIfNone)
    }

    override fun calculateValue(tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        return polynomial.evaluate(tokenTable, zeroIfNone)
    }

    override fun calculateValue(results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        return polynomial.evaluate(results, tokenTable, zeroIfNone)
    }
}
