@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package fuookami.ospf.kotlin.core.intermediate_symbol.legacy.quadratic_function

import fuookami.ospf.kotlin.core.expression.monomial.times
import fuookami.ospf.kotlin.core.expression.polynomial.*
import fuookami.ospf.kotlin.core.intermediate_symbol.*
import fuookami.ospf.kotlin.core.intermediate_model.eq
import fuookami.ospf.kotlin.core.intermediate_model.leq
import fuookami.ospf.kotlin.core.intermediate_model.AbstractQuadraticMechanismModel
import fuookami.ospf.kotlin.core.intermediate_model.AbstractTokenTable
import fuookami.ospf.kotlin.core.variable.AbstractTokenList
import fuookami.ospf.kotlin.core.variable.AddableTokenCollection
import fuookami.ospf.kotlin.core.variable.BinVariable1
import fuookami.ospf.kotlin.core.variable.PctVariable1
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.geometry.Point2
import fuookami.ospf.kotlin.math.geometry.x
import fuookami.ospf.kotlin.math.geometry.y
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.multiarray.Shape1
import org.apache.logging.log4j.kotlin.logger

sealed class AbstractUnivariateLinearPiecewiseFunction(
    private val x: AbstractQuadraticPolynomial<*>,
    val points: List<Point2>,
    override val parent: IntermediateSymbol? = null,
    args: Any? = null,
    override var name: String,
    override var displayName: String? = null
) : QuadraticFunctionSymbol() {
    private val logger = logger()

    init {
        assert(points.foldIndexed(true) { index, acc, point ->
            acc && if (index == 0) {
                true
            } else {
                point.x geq points[index - 1].x
            }
        })
    }

    internal val _args = args
    override val args get() = _args ?: parent?.args

    val size by points::size
    val indices by points::indices

    val empty: Boolean get() = size == 0
    val fixed: Boolean get() = size == 1
    val piecewise: Boolean get() = size >= 2

    fun y(x: Flt64): Flt64? {
        if (empty
            || x ls points.first().x
            || x gr points.last().x
        ) {
            return null
        }

        for (i in indices) {
            if (i == (size - 1)) {
                continue
            }

            if (points[i + 1].x geq x) {
                val dy = points[i + 1].y - points[i].y
                val dx = points[i + 1].x - points[i].x
                return dy / dx * (x - points[i].x)
            }
        }
        return null
    }

    private val k: PctVariable1 by lazy {
        PctVariable1("${name}_k", Shape1(points.size))
    }

    internal val b: BinVariable1 by lazy {
        BinVariable1("${name}_b", Shape1(points.lastIndex))
    }

    private val polyY: QuadraticPolynomial by lazy {
        val polyY = QuadraticPolynomial(
            sum(points.mapIndexed { i, p -> p.y * k[i] }),
            "${name}_y"
        )
        polyY.range.set(ValueRange(points.minOf { it.y }, points.maxOf { it.y }).value!!)
        polyY
    }

    override val range get() = polyY.range
    override val lowerBound get() = polyY.lowerBound
    override val upperBound get() = polyY.upperBound

    override val category = Linear

    override val dependencies: Set<IntermediateSymbol> by x::dependencies
    override val cells get() = polyY.cells
    override val cached get() = polyY.cached

    override fun flush(force: Boolean) {
        x.flush(force)
        polyY.flush(force)
    }

    override fun prepare(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable): Flt64? {
        return prepareIfNotCached(values, tokenTable) {
            val xValue = if (values.isNullOrEmpty()) {
                x.evaluate(tokenTable)
            } else {
                x.evaluate(values, tokenTable)
            } ?: return null

            var yValue: Flt64? = null
            for (i in indices) {
                if (i == (size - 1)) {
                    continue
                }

                if (points[i].x leq xValue && xValue leq points[i + 1].x) {
                    val dx = points[i + 1].x - points[i].x
                    val lhs = (xValue - points[i].x) / dx
                    val rhs = (points[i + 1].x - xValue) / dx
                    yValue = points[i].y * lhs + points[i + 1].y * rhs

                    logger.trace { "Setting UnivariateLinearPiecewiseFunction ${name}.b[$i] initial solution: true" }
                    tokenTable.find(b[i])?.let { token ->
                        token._result = Flt64.one
                    }
                    logger.trace { "Setting UnivariateLinearPiecewiseFunction ${name}.k[$i] initial solution: $lhs" }
                    tokenTable.find(k[i])?.let { token ->
                        token._result = lhs
                    }
                    logger.trace { "Setting UnivariateLinearPiecewiseFunction ${name}.k[${i + 1}] initial solution: $rhs" }
                    tokenTable.find(k[i + 1])?.let { token ->
                        token._result = rhs
                    }
                } else {
                    logger.trace { "Setting UnivariateLinearPiecewiseFunction ${name}.b[$i] initial solution: false" }
                    tokenTable.find(b[i])?.let { token ->
                        token._result = Flt64.zero
                    }
                    tokenTable.find(k[i])?.let { token ->
                        if (token._result == null) {
                            logger.trace { "Setting UnivariateLinearPiecewiseFunction ${name}.k[$i] initial solution: 0" }
                            token._result = Flt64.zero
                        }
                    }
                }
            }

            yValue
        }
    }

    override fun register(tokenTable: AddableTokenCollection): Try {
        when (val result = tokenTable.add(k)) {
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

        when (val result = tokenTable.add(b)) {
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
        when (val result = model.addConstraint(
            x eq sum(points.mapIndexed { i, p -> p.x * k[i] }),
            name = "${name}_x",
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
            sum(k) eq Flt64.one,
            name = "${name}_k",
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
            sum(b) eq Flt64.one,
            name = "${name}_b",
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

        for (i in indices) {
            val poly = MutableLinearPolynomial()
            if (i != 0) {
                poly += b[i - 1]
            }
            if (i != (size - 1)) {
                poly += b[i]
            }
            when (val result = model.addConstraint(
                k[i] leq poly,
                name = "${name}_kb_i",
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
        fixedValues: Map<Symbol, Flt64>
    ): Try {
        val xValue = when (tokenTable) {
            is AbstractTokenTable -> {
                x.evaluate(fixedValues, tokenTable) ?: return register(tokenTable)
            }

            is FunctionSymbolRegistrationScope -> {
                x.evaluate(fixedValues, tokenTable.origin) ?: return register(tokenTable)
            }

            else -> {
                return register(tokenTable)
            }
        }
        val i = (0 until (points.size - 1)).indexOfFirst {
            points[it].x leq xValue && xValue leq points[it + 1].x
        }
        if (i == -1) {
            return register(tokenTable)
        }

        when (val result = tokenTable.add(listOf(k[i], k[i + 1], b[i]))) {
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

    override fun register(
        model: AbstractQuadraticMechanismModel,
        fixedValues: Map<Symbol, Flt64>
    ): Try {
        val xValue = x.evaluate(fixedValues, model.tokens) ?: return register(model)
        val i = (0 until (points.size - 1)).indexOfFirst {
            points[it].x leq xValue && xValue leq points[it + 1].x
        }
        if (i == -1) {
            return register(model)
        }

        val dx = points[i + 1].x - points[i].x
        val lhs = (xValue - points[i].x) / dx
        val rhs = (points[i + 1].x - xValue) / dx

        when (val result = model.addConstraint(
            x eq points[i].x * k[i] + points[i + 1].x * k[i + 1],
            name = "${name}_x",
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
            k[i] + k[i + 1] eq Flt64.one,
            name = "${name}_k",
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

        model.tokens.find(k[i])?.let { token ->
            token._result = lhs
        }
        model.tokens.find(k[i + 1])?.let { token ->
            token._result = rhs
        }

        when (val result = model.addConstraint(
            b[i] eq Flt64.one,
            name = "${name}_b",
            from = parent ?: this
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        model.tokens.find(b[i])?.let { token ->
            token._result = Flt64.one
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
            "$name(${x.toTidyRawString(unfold - UInt64.one)}})"
        }
    }

    override fun evaluate(
        tokenList: AbstractTokenList,
        zeroIfNone: Boolean
    ): Flt64? {
        return x.evaluate(tokenList, zeroIfNone)?.let {
            y(it)
        }
    }

    override fun evaluate(
        results: List<Flt64>,
        tokenList: AbstractTokenList,
        zeroIfNone: Boolean
    ): Flt64? {
        return x.evaluate(
            results = results,
            tokenList = tokenList,
            zeroIfNone = zeroIfNone
        )?.let {
            y(it)
        }
    }

    override fun evaluate(
        values: Map<Symbol, Flt64>,
        tokenList: AbstractTokenList?,
        zeroIfNone: Boolean
    ): Flt64? {
        return x.evaluate(
            values = values,
            tokenList = tokenList,
            zeroIfNone = zeroIfNone
        )?.let {
            y(it)
        }
    }

    override fun calculateValue(
        tokenTable: AbstractTokenTable,
        zeroIfNone: Boolean
    ): Flt64? {
        return x.evaluate(tokenTable, zeroIfNone)?.let {
            y(it)
        }
    }

    override fun calculateValue(
        results: List<Flt64>,
        tokenTable: AbstractTokenTable,
        zeroIfNone: Boolean
    ): Flt64? {
        return x.evaluate(
            results = results,
            tokenTable = tokenTable,
            zeroIfNone = zeroIfNone
        )?.let {
            y(it)
        }
    }

    override fun calculateValue(
        values: Map<Symbol, Flt64>,
        tokenTable: AbstractTokenTable?,
        zeroIfNone: Boolean
    ): Flt64? {
        return x.evaluate(
            values = values,
            tokenTable = tokenTable,
            zeroIfNone = zeroIfNone
        )?.let {
            y(it)
        }
    }
}

open class UnivariateLinearPiecewiseFunction(
    x: AbstractQuadraticPolynomial<*>,
    points: List<Point2>,
    parent: IntermediateSymbol? = null,
    args: Any? = null,
    name: String,
    displayName: String? = null
) : AbstractUnivariateLinearPiecewiseFunction(
    x = x,
    points = points.sortedBy { it.x },
    parent = parent,
    args = args,
    name = name,
    displayName = displayName
) {
    companion object {
        operator fun <
                T : ToQuadraticPolynomial<Poly>,
                Poly : AbstractQuadraticPolynomial<Poly>
                > invoke(
            x: T,
            points: List<Point2>,
            parent: IntermediateSymbol? = null,
            args: Any? = null,
            name: String,
            displayName: String? = null
        ): UnivariateLinearPiecewiseFunction {
            return UnivariateLinearPiecewiseFunction(
                x = x.toQuadraticPolynomial(),
                points = points,
                parent = parent,
                args = args,
                name = name,
                displayName = displayName
            )
        }
    }
}

open class MonotoneUnivariateLinearPiecewiseFunction(
    x: AbstractQuadraticPolynomial<*>,
    points: List<Point2>,
    parent: IntermediateSymbol? = null,
    args: Any? = null,
    name: String,
    displayName: String? = null
) : AbstractUnivariateLinearPiecewiseFunction(
    x = x,
    points = points.sortedBy { it.x },
    parent = parent,
    args = args,
    name = name,
    displayName = displayName
) {
    companion object {
        operator fun <
                T : ToQuadraticPolynomial<Poly>,
                Poly : AbstractQuadraticPolynomial<Poly>
                > invoke(
            x: T,
            points: List<Point2>,
            parent: IntermediateSymbol? = null,
            args: Any? = null,
            name: String,
            displayName: String? = null
        ): MonotoneUnivariateLinearPiecewiseFunction {
            return MonotoneUnivariateLinearPiecewiseFunction(
                x = x.toQuadraticPolynomial(),
                points = points,
                parent = parent,
                args = args,
                name = name,
                displayName = displayName
            )
        }
    }

    init {
        assert(points.foldIndexed(true) { index, acc, point ->
            acc && (index == 0 || point.y geq points[index - 1].y)
        })
    }

    fun x(@Suppress("UNUSED_PARAMETER") y: Flt64): Flt64? {
        TODO("not implement yet")
    }
}





