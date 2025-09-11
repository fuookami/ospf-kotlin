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

    companion object {
        operator fun invoke(
            polynomials: List<ToQuadraticPolynomial<*>>,
            name: String = polynomials.joinToString("*") { "$it" },
            displayName: String? = null
        ): ProductFunction {
            return ProductFunction(
                polynomials.map { it.toQuadraticPolynomial() },
                name,
                displayName
            )
        }

        operator fun <
            T1 : AbstractQuadraticPolynomial<Poly1>,
            Poly1 : AbstractQuadraticPolynomial<Poly1>,
            T2 : AbstractQuadraticPolynomial<Poly2>,
            Poly2 : AbstractQuadraticPolynomial<Poly2>
        > invoke(
            x: T1,
            y: T2,
            name: String = "$x*$y",
            displayName: String? = null
        ): ProductFunction {
            return ProductFunction(
                listOf(x.toQuadraticPolynomial(), y.toQuadraticPolynomial()),
                name,
                displayName
            )
        }
    }

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

    override fun prepare(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable): Flt64? {
        for (polynomial in polynomials) {
            polynomial.cells
        }

        return if ((!values.isNullOrEmpty() || tokenTable.cachedSolution) && if (values.isNullOrEmpty()) {
            tokenTable.cached(this)
        } else {
            tokenTable.cached(this, values)
        } == false) {
            val values = polynomials.map {
                if (values.isNullOrEmpty()) {
                    it.evaluate(tokenTable)
                } else {
                    it.evaluate(values, tokenTable)
                } ?: return null
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
                    polynomials[0] * polynomials[1] eq y[0],
                    "${name}_0"
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

    override fun register(
        tokenTable: AbstractMutableTokenTable,
        fixedValues: Map<Symbol, Flt64>
    ): Try {
        return register(tokenTable)
    }

    override fun register(
        model: AbstractQuadraticMechanismModel,
        fixedValues: Map<Symbol, Flt64>
    ): Try {
        val values = polynomials.map {
            it.evaluate(fixedValues, model.tokens) ?: return register(model)
        }

        var yValue = Flt64.one
        for (i in y.indices) {
            if (i == 0) {
                yValue *= values[0] * values[1]

                when (val result = model.addConstraint(
                    polynomials[0] * polynomials[1] eq y[0],
                    "${name}_$0"
                )) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }
                }

                when (val result = model.addConstraint(
                    y[0] eq yValue,
                    "${name}_y_0"
                )) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }
                }

                model.tokens.find(y[0])?.let { token ->
                    token._result = yValue
                }
            } else {
                yValue *= values[i + 1]

                when (val result = model.addConstraint(
                    y[i - 1] * polynomials[i + 1] eq y[i],
                    "${name}_$i"
                )) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }
                }

                when (val result = model.addConstraint(
                    y[i] eq yValue,
                    "${name}_y_$i"
                )) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }
                }

                model.tokens.find(y[i])?.let { token ->
                    token._result = yValue
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

    override fun evaluate(
        tokenList: AbstractTokenList,
        zeroIfNone: Boolean
    ): Flt64? {
        return polynomials.fold(Flt64.one) { lhs, rhs ->
            val thisValue = rhs.evaluate(tokenList, zeroIfNone) ?: return null
            lhs * thisValue
        }
    }

    override fun evaluate(
        results: List<Flt64>,
        tokenList: AbstractTokenList,
        zeroIfNone: Boolean
    ): Flt64? {
        return polynomials.fold(Flt64.one) { lhs, rhs ->
            val thisValue = rhs.evaluate(results, tokenList, zeroIfNone) ?: return null
            lhs * thisValue
        }
    }

    override fun evaluate(
        values: Map<Symbol, Flt64>,
        tokenList: AbstractTokenList?,
        zeroIfNone: Boolean
    ): Flt64? {
        return polynomials.fold(Flt64.one) { lhs, rhs ->
            val thisValue = rhs.evaluate(values, tokenList, zeroIfNone) ?: return null
            lhs * thisValue
        }
    }

    override fun calculateValue(
        tokenTable: AbstractTokenTable,
        zeroIfNone: Boolean
    ): Flt64? {
        return polynomials.fold(Flt64.one) { lhs, rhs ->
            val thisValue = rhs.evaluate(tokenTable, zeroIfNone) ?: return null
            lhs * thisValue
        }
    }

    override fun calculateValue(
        results: List<Flt64>,
        tokenTable: AbstractTokenTable,
        zeroIfNone: Boolean
    ): Flt64? {
        return polynomials.fold(Flt64.one) { lhs, rhs ->
            val thisValue = rhs.evaluate(results, tokenTable, zeroIfNone) ?: return null
            lhs * thisValue
        }
    }

    override fun calculateValue(
        values: Map<Symbol, Flt64>,
        tokenTable: AbstractTokenTable?,
        zeroIfNone: Boolean
    ): Flt64? {
        return polynomials.fold(Flt64.one) { lhs, rhs ->
            val thisValue = rhs.evaluate(values, tokenTable, zeroIfNone) ?: return null
            lhs * thisValue
        }
    }
}
