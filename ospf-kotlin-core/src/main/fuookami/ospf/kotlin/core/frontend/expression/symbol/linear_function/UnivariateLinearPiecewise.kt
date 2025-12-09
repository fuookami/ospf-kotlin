package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function

import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.math.geometry.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

sealed class AbstractUnivariateLinearPiecewiseFunction(
    private val x: AbstractLinearPolynomial<*>,
    val points: List<Point2>,
    override val parent: IntermediateSymbol? = null,
    override var name: String,
    override var displayName: String? = null
) : LinearFunctionSymbol() {
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
                return points[i].y + dy / dx * (x - points[i].x)
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

    private val polyY: LinearPolynomial by lazy {
        val polyY = LinearPolynomial(
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
        x.cells

        return if ((!values.isNullOrEmpty() || tokenTable.cachedSolution) && if (values.isNullOrEmpty()) {
            tokenTable.cached(this)
        } else {
            tokenTable.cached(this, values)
        } == false) {
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
                    val lhs = (points[i + 1].x - xValue) / dx
                    val rhs = (xValue - points[i].x) / dx
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
        } else {
            null
        }
    }

    override fun register(tokenTable: AddableTokenCollection): Try {
        when (val result = tokenTable.add(k)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = tokenTable.add(b)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }

    override fun register(model: AbstractLinearMechanismModel): Try {
        when (val result = model.addConstraint(
            x eq sum(points.mapIndexed { i, p -> p.x * k[i] }),
            name = "${name}_x",
            from = parent ?: this
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
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
                name = "${name}_kb_${i}",
                from = parent ?: this
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
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
        }

        return ok
    }

    override fun register(
        model: AbstractLinearMechanismModel,
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
        val lhs = (points[i + 1].x - xValue) / dx
        val rhs = (xValue - points[i].x)/ dx

        when (val result = model.addConstraint(
            x eq points[i].x * k[i] + points[i + 1].x * k[i + 1],
            name = "${name}_x",
            from = parent ?: this
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
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
        return x.evaluate(results, tokenList, zeroIfNone)?.let {
            y(it)
        }
    }

    override fun evaluate(
        values: Map<Symbol, Flt64>,
        tokenList: AbstractTokenList?,
        zeroIfNone: Boolean
    ): Flt64? {
        return x.evaluate(values, tokenList, zeroIfNone)?.let {
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
        return x.evaluate(results, tokenTable, zeroIfNone)?.let {
            y(it)
        }
    }

    override fun calculateValue(
        values: Map<Symbol, Flt64>,
        tokenTable: AbstractTokenTable?,
        zeroIfNone: Boolean
    ): Flt64? {
        return x.evaluate(values, tokenTable, zeroIfNone)?.let {
            y(it)
        }
    }
}

open class UnivariateLinearPiecewiseFunction(
    x: AbstractLinearPolynomial<*>,
    points: List<Point2>,
    parent: IntermediateSymbol? = null,
    name: String,
    displayName: String? = null
) : AbstractUnivariateLinearPiecewiseFunction(
    x = x,
    points = points.sortedBy { it.x },
    parent = parent,
    name = name,
    displayName = displayName
) {
    companion object {
        operator fun <
            T : ToLinearPolynomial<Poly>,
            Poly : AbstractLinearPolynomial<Poly>
        > invoke(
            x: T,
            points: List<Point2>,
            parent: IntermediateSymbol? = null,
            name: String,
            displayName: String? = null
        ): UnivariateLinearPiecewiseFunction {
            return UnivariateLinearPiecewiseFunction(
                x = x.toLinearPolynomial(),
                points = points,
                parent = parent,
                name = name,
                displayName = displayName
            )
        }
    }
}

open class MonotoneUnivariateLinearPiecewiseFunction(
    x: AbstractLinearPolynomial<*>,
    points: List<Point2>,
    parent: IntermediateSymbol? = null,
    name: String,
    displayName: String? = null
) : AbstractUnivariateLinearPiecewiseFunction(
    x = x,
    points = points.sortedBy { it.x },
    parent = parent,
    name = name,
    displayName = displayName
) {
    companion object {
        operator fun <T : ToLinearPolynomial<Poly>, Poly : AbstractLinearPolynomial<Poly>> invoke(
            x: T,
            points: List<Point2>,
            parent: IntermediateSymbol? = null,
            name: String,
            displayName: String? = null
        ): MonotoneUnivariateLinearPiecewiseFunction {
            return MonotoneUnivariateLinearPiecewiseFunction(
                x = x.toLinearPolynomial(),
                points = points,
                parent = parent,
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

    fun x(y: Flt64): Flt64? {
        TODO("not implement yet")
    }
}
