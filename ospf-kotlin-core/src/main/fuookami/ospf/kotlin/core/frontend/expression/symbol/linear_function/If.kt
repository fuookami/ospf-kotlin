package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function

import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

class IfFunction(
    inequality: LinearInequality,
    override var name: String,
    override var displayName: String? = null
) : LinearLogicFunctionSymbol {
    private val logger = logger()

    private val inequality by lazy {
        inequality.normalize()
    }

    private val y: BinVar by lazy {
        BinVar("${name}_y")
    }

    private val polyY: AbstractLinearPolynomial<*> by lazy {
        val polyY = LinearPolynomial(y)
        polyY.range.set(possibleRange)
        polyY
    }

    override val discrete = true

    override val range get() = polyY.range
    override val lowerBound get() = polyY.lowerBound
    override val upperBound get() = polyY.upperBound

    override val category = Linear

    override val dependencies: Set<IntermediateSymbol>
        get() {
            val dependencies = HashSet<IntermediateSymbol>()
            dependencies.addAll(inequality.lhs.dependencies)
            dependencies.addAll(inequality.rhs.dependencies)
            return dependencies
        }
    override val cells get() = polyY.cells
    override val cached get() = polyY.cached

    private val possibleRange: ValueRange<Flt64>
        get() {
            // todo: impl by Inequality.judge()
            return ValueRange(Flt64.zero, Flt64.one).value!!
        }

    override fun flush(force: Boolean) {
        inequality.flush(force)
        polyY.flush(force)
        polyY.range.set(possibleRange)
    }

    override fun prepare(tokenTable: AbstractTokenTable) {
        inequality.lhs.cells
        inequality.rhs.cells

        if (tokenTable.cachedSolution && tokenTable.cached(this) == false) {
            inequality.isTrue(tokenTable)?.let { bin ->
                val yValue = if (bin) {
                    Flt64.one
                } else {
                    Flt64.zero
                }

                logger.trace { "Setting IfFunction ${name}.y to $bin" }
                tokenTable.find(y)?.let { token ->
                    token._result = yValue
                }

                tokenTable.cache(this, null, yValue)
            }
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

    override fun register(model: AbstractLinearMechanismModel): Try {
        when (val result = inequality.register(name, y, model)) {
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

    override fun toRawString(unfold: Boolean): String {
        return "if(${inequality.toRawString(unfold)})"
    }

    override fun evaluate(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return when (inequality.isTrue(tokenList, zeroIfNone)) {
            true -> {
                Flt64.one
            }

            false -> {
                Flt64.zero
            }

            null -> {
                null
            }
        }
    }

    override fun evaluate(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return when (inequality.isTrue(results, tokenList, zeroIfNone)) {
            true -> {
                Flt64.one
            }

            false -> {
                Flt64.zero
            }

            null -> {
                null
            }
        }
    }

    override fun calculateValue(tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        return when (inequality.isTrue(tokenTable, zeroIfNone)) {
            true -> {
                Flt64.one
            }

            false -> {
                Flt64.zero
            }

            null -> {
                null
            }
        }
    }

    override fun calculateValue(results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        return when (inequality.isTrue(results, tokenTable, zeroIfNone)) {
            true -> {
                Flt64.one
            }

            false -> {
                Flt64.zero
            }

            null -> {
                null
            }
        }
    }
}
