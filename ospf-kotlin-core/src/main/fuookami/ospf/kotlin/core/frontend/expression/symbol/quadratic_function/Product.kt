package fuookami.ospf.kotlin.core.frontend.expression.symbol.quadratic_function

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

class ProductFunction(
    val polynomials: List<AbstractQuadraticPolynomial<*>>,
    override var name: String = polynomials.joinToString("*") { "$it" },
    override var displayName: String? = null
) : QuadraticFunctionSymbol {
    constructor(
        x: AbstractQuadraticPolynomial<*>,
        y: AbstractQuadraticPolynomial<*>,
        name: String = "$x*$y",
        displayName: String? = null
    ) : this(listOf(x, y), name, displayName)

    init {
        assert(polynomials.all { it.category != Quadratic })
    }

    private lateinit var y: RealVariable1
    private lateinit var polyY: AbstractQuadraticPolynomial<*>

    override val discrete = polynomials.all { it.discrete }

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

    override val dependencies: Set<Symbol>
        get() {
            val dependencies = HashSet<Symbol>()
            for (polynomial in polynomials) {
                dependencies.addAll(polynomial.dependencies)
            }
            return dependencies
        }
    override val cells get() = polyY.cells
    override val cached
        get() = if (::polyY.isInitialized) {
            polyY.cached
        } else {
            false
        }

    private val possibleRange get() = polynomials.fold(ValueRange(Flt64.one, Flt64.one)) { lhs, rhs -> lhs * rhs.range.valueRange }

    override fun flush(force: Boolean) {
        if (::polyY.isInitialized) {
            polyY.flush(force)
            polyY.range.set(possibleRange)
        }
    }

    override suspend fun prepare(tokenTable: AbstractTokenTable) {
        for (polynomial in polynomials) {
            polynomial.cells
        }
    }

    override fun register(tokenTable: MutableTokenTable): Try {
        if (polynomials.any { it.category == Quadratic }) {
            return Failed(Err(ErrorCode.ApplicationFailed, "Invalid argument of QuadraticPolynomial.times: over quadratic."))
        }

        if (!::y.isInitialized) {
            y = RealVariable1("${name}_y", Shape1(polynomials.size - 1))
        }
        when (val result = tokenTable.add(y)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        if (!::polyY.isInitialized) {
            polyY = QuadraticPolynomial(y.last())
            polyY.range.set(possibleRange)
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

    override fun toRawString(unfold: Boolean): String {
        return "product(${polynomials.joinToString(", ") { it.toRawString(unfold) }})"
    }

    override fun value(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return polynomials.fold(Flt64.one) { lhs, rhs ->
            val thisValue = rhs.value(tokenList, zeroIfNone)
                ?: return null
            lhs * thisValue
        }
    }

    override fun value(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return polynomials.fold(Flt64.one) { lhs, rhs ->
            val thisValue = rhs.value(results, tokenList, zeroIfNone)
                ?: return null
            lhs * thisValue
        }
    }

    override fun calculateValue(tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        return polynomials.fold(Flt64.one) { lhs, rhs ->
            val thisValue = rhs.value(tokenTable, zeroIfNone)
                ?: return null
            lhs * thisValue
        }
    }

    override fun calculateValue(results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        return polynomials.fold(Flt64.one) { lhs, rhs ->
            val thisValue = rhs.value(results, tokenTable, zeroIfNone)
                ?: return null
            lhs * thisValue
        }
    }
}
