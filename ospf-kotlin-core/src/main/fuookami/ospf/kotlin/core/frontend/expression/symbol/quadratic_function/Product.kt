package fuookami.ospf.kotlin.core.frontend.expression.symbol.quadratic_function

import fuookami.ospf.kotlin.core.frontend.expression.polynomial.AbstractQuadraticPolynomial
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.ToQuadraticPolynomial
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.times
import fuookami.ospf.kotlin.core.frontend.expression.symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.frontend.expression.symbol.QuadraticFunctionSymbol
import fuookami.ospf.kotlin.core.frontend.expression.symbol.prepareIfNotCached
import fuookami.ospf.kotlin.core.frontend.expression.symbol.toTidyRawString
import fuookami.ospf.kotlin.core.frontend.model.mechanism.eq
import fuookami.ospf.kotlin.core.frontend.model.mechanism.AbstractQuadraticMechanismModel
import fuookami.ospf.kotlin.core.frontend.model.mechanism.AbstractTokenTable
import fuookami.ospf.kotlin.core.frontend.variable.AbstractTokenList
import fuookami.ospf.kotlin.core.frontend.variable.AddableTokenCollection
import fuookami.ospf.kotlin.core.frontend.variable.RealVariable1
import fuookami.ospf.kotlin.utils.error.Err
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.multiarray.Shape1
import org.apache.logging.log4j.kotlin.logger

class ProductFunction(
    val polynomials: List<AbstractQuadraticPolynomial<*>>,
    override val parent: IntermediateSymbol? = null,
    args: Any? = null,
    override var name: String = polynomials.joinToString("*") { "$it" },
    override var displayName: String? = null
) : QuadraticFunctionSymbol() {
    private val logger = logger()

    companion object {
        operator fun invoke(
            polynomials: List<ToQuadraticPolynomial<*>>,
            parent: IntermediateSymbol? = null,
            args: Any? = null,
            name: String = polynomials.joinToString("*") { "$it" },
            displayName: String? = null
        ): ProductFunction {
            return ProductFunction(
                polynomials = polynomials.map { it.toQuadraticPolynomial() },
                parent = parent,
                args = args,
                name = name,
                displayName = displayName
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
            parent: IntermediateSymbol? = null,
            args: Any? = null,
            name: String = "$x*$y",
            displayName: String? = null
        ): ProductFunction {
            return ProductFunction(
                polynomials = listOf(x.toQuadraticPolynomial(), y.toQuadraticPolynomial()),
                parent = parent,
                args = args,
                name = name,
                displayName = displayName
            )
        }
    }

    constructor(
        x: AbstractQuadraticPolynomial<*>,
        y: AbstractQuadraticPolynomial<*>,
        parent: IntermediateSymbol? = null,
        args: Any? = null,
        name: String = "$x*$y",
        displayName: String? = null
    ) : this(
        polynomials = listOf(x, y),
        parent = parent,
        args = args,
        name = name,
        displayName = displayName
    )

    init {
        assert(polynomials.all { it.category != Quadratic })
    }

    internal val _args = args
    override val args get() = _args ?: parent?.args

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
        return prepareIfNotCached(values, tokenTable) {
            val evaluatedValues = polynomials.map {
                if (values.isNullOrEmpty()) {
                    it.evaluate(tokenTable)
                } else {
                    it.evaluate(values, tokenTable)
                } ?: return null
            }

            var yValue = evaluatedValues[0]
            for (i in y.indices) {
                yValue *= evaluatedValues[i + 1]

                logger.trace { "Setting ProductFunction ${name}.y[$i] initial solution: $yValue" }
                tokenTable.find(y[i])?.let { token ->
                    token._result = yValue
                }
            }

            yValue
        }
    }

    override fun register(tokenTable: AddableTokenCollection): Try {
        if (polynomials.any { it.category == Quadratic }) {
            return Failed(Err(ErrorCode.ApplicationFailed, "Invalid argument of QuadraticPolynomial.times: over quadratic."))
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
        for (i in y.indices) {
            if (i == 0) {
                when (val result = model.addConstraint(
                    relation = polynomials[0] * polynomials[1] eq y[0],
                    name = "${name}_0",
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
                    relation = y[i - 1] * polynomials[i + 1] eq y[i],
                    name = "${name}_$i",
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
        }

        return ok
    }

    override fun register(
        tokenTable: AddableTokenCollection,
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
                    relation = polynomials[0] * polynomials[1] eq y[0],
                    name = "${name}_$0",
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
                    relation = y[0] eq yValue,
                    name = "${name}_y_0",
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

                model.tokens.find(y[0])?.let { token ->
                    token._result = yValue
                }
            } else {
                yValue *= values[i + 1]

                when (val result = model.addConstraint(
                    relation = y[i - 1] * polynomials[i + 1] eq y[i],
                    name = "${name}_$i",
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
                    relation = y[i] eq yValue,
                    name = "${name}_y_$i",
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
            val thisValue = rhs.evaluate(
                results = results,
                tokenList = tokenList,
                zeroIfNone = zeroIfNone
            ) ?: return null
            lhs * thisValue
        }
    }

    override fun evaluate(
        values: Map<Symbol, Flt64>,
        tokenList: AbstractTokenList?,
        zeroIfNone: Boolean
    ): Flt64? {
        return polynomials.fold(Flt64.one) { lhs, rhs ->
            val thisValue = rhs.evaluate(
                values = values,
                tokenList = tokenList,
                zeroIfNone = zeroIfNone
            ) ?: return null
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
            val thisValue = rhs.evaluate(
                results = results,
                tokenTable = tokenTable,
                zeroIfNone = zeroIfNone
            ) ?: return null
            lhs * thisValue
        }
    }

    override fun calculateValue(
        values: Map<Symbol, Flt64>,
        tokenTable: AbstractTokenTable?,
        zeroIfNone: Boolean
    ): Flt64? {
        return polynomials.fold(Flt64.one) { lhs, rhs ->
            val thisValue = rhs.evaluate(
                values = values,
                tokenTable = tokenTable,
                zeroIfNone = zeroIfNone
            ) ?: return null
            lhs * thisValue
        }
    }
}





