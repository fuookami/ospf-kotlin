package fuookami.ospf.kotlin.core.frontend.expression.symbol.quadratic_function

import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

class ProductFunction(
    val polynomials: List<AbstractQuadraticPolynomial<*>>,
    override var name: String = polynomials.joinToString("*") { "$it" },
    override var displayName: String? = null
) : QuadraticFunctionSymbol {
    private val logger = logger()

    constructor(
        x: AbstractQuadraticPolynomial<*>,
        y: AbstractQuadraticPolynomial<*>,
        name: String = "$x*$y",
        displayName: String? = null
    ) : this(listOf(x, y), name, displayName)

    init {
        assert(polynomials.all { it.category != Quadratic })
    }

    private val y: RealVariable1 by lazy {
        RealVariable1("${name}_y", Shape1(polynomials.lastIndex))
    }

    private val polyY: AbstractQuadraticPolynomial<*> by lazy {
        val polyY = QuadraticPolynomial(y.last())
        polyY.range.set(possibleRange)
        polyY
    }

    override val discrete by lazy {
        polynomials.all { it.discrete }
    }

    override val range get() = polyY.range
    override val lowerBound get() = polyY.lowerBound
    override val upperBound get() = polyY.upperBound

    override val category = Linear

    override val dependencies: Set<IntermediateSymbol>
        get() {
            val dependencies = HashSet<IntermediateSymbol>()
            for (polynomial in polynomials) {
                dependencies.addAll(polynomial.dependencies)
            }
            return dependencies
        }
    override val cells get() = polyY.cells
    override val cached get() = polyY.cached

    private val possibleRange
        get() = polynomials.fold(ValueRange(Flt64.one, Flt64.one).value!!) { lhs, rhs ->
            (lhs * rhs.range.valueRange!!)!!
        }

    override fun flush(force: Boolean) {
        for (polynomial in polynomials) {
            polynomial.flush(force)
        }
        polyY.flush(force)
        polyY.range.set(possibleRange)
    }

    override fun prepare(tokenTable: AbstractTokenTable): Flt64? {
        for (polynomial in polynomials) {
            polynomial.cells
        }

        return if (tokenTable.cachedSolution && tokenTable.cached(this) == false) {
            val values = polynomials.map {
                it.evaluate(tokenTable) ?: return null
            }

            var yValue = values[0]
            for (i in y.indices) {
                yValue *= values[i + 1]

                logger.trace { "Setting ProductFunction ${name}.y[$i] initial solution: $yValue" }
                tokenTable.find(y[i])?.let { token ->
                    token._result = yValue
                }
            }

            yValue
        } else {
            null
        }
    }

    override fun register(tokenTable: AbstractMutableTokenTable): Try {
        if (polynomials.any { it.category == Quadratic }) {
            return Failed(Err(ErrorCode.ApplicationFailed, "Invalid argument of QuadraticPolynomial.times: over quadratic."))
        }

        when (val result = tokenTable.add(y)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }

    override fun register(model: AbstractQuadraticMechanismModel): Try {
        for (i in y.indices) {
            if (i == 0) {
                when (val result = model.addConstraint(
                    polynomials[i] * polynomials[i + 1] eq y[i],
                    "${name}_$i"
                )) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }
                }
            } else {
                when (val result = model.addConstraint(
                    y[i - 1] * polynomials[i + 1] eq y[i],
                    "${name}_$i"
                )) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }
                }
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
            "product(${polynomials.joinToString(", ") { it.toTidyRawString(unfold - UInt64.one) }})"
        }
    }

    override fun evaluate(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return polynomials.fold(Flt64.one) { lhs, rhs ->
            val thisValue = rhs.evaluate(tokenList, zeroIfNone)
                ?: return null
            lhs * thisValue
        }
    }

    override fun evaluate(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return polynomials.fold(Flt64.one) { lhs, rhs ->
            val thisValue = rhs.evaluate(results, tokenList, zeroIfNone)
                ?: return null
            lhs * thisValue
        }
    }

    override fun calculateValue(tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        return polynomials.fold(Flt64.one) { lhs, rhs ->
            val thisValue = rhs.evaluate(tokenTable, zeroIfNone)
                ?: return null
            lhs * thisValue
        }
    }

    override fun calculateValue(results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        return polynomials.fold(Flt64.one) { lhs, rhs ->
            val thisValue = rhs.evaluate(results, tokenTable, zeroIfNone)
                ?: return null
            lhs * thisValue
        }
    }
}
