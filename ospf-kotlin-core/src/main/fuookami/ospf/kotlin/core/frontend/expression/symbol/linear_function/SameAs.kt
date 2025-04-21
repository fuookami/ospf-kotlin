package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function

import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

class SameAsFunction(
    inequalities: List<LinearInequality>,
    private val constraint: Boolean = true,
    private val fixedValue: Boolean? = null,
    override var name: String,
    override var displayName: String? = null
) : LinearFunctionSymbol {
    private val logger = logger()

    private val inequalities by lazy {
        inequalities.map { it.normalize() }
    }

    private val k: PctVariable2 by lazy {
        PctVariable2("${name}_k", Shape2(inequalities.size, 3))
    }

    private val u: BinVariable1 by lazy {
        BinVariable1("${name}_u", Shape1(inequalities.size))
    }

    private val y: BinVar by lazy {
        val y = BinVar("${name}_y")
        if (fixedValue != null) {
            y.range.eq(fixedValue)
        }
        y
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
            for (inequality in inequalities) {
                dependencies.addAll(inequality.lhs.dependencies)
                dependencies.addAll(inequality.rhs.dependencies)
            }
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
        for (inequality in inequalities) {
            inequality.flush(force)
        }
        polyY.flush(force)
        polyY.range.set(possibleRange)
    }

    override fun prepare(tokenTable: AbstractTokenTable) {
        for (inequality in inequalities) {
            inequality.lhs.cells
            inequality.rhs.cells
        }

        if (tokenTable.cachedSolution && tokenTable.cached(this) == false) {
            val values = inequalities.map { it.isTrue(tokenTable) ?: return }
            if (!constraint && inequalities.size > 1) {
                for (i in inequalities.indices) {
                    logger.trace { "Setting SameAsFunction ${name}.u[$i] to ${values[i]}" }
                    tokenTable.find(u[i])?.let { token ->
                        token._result = if (values[i]) {
                            Flt64.one
                        } else {
                            Flt64.zero
                        }
                    }
                }
            }

            val bin = values.all { it } || values.all { !it }
            val yValue = if (bin) {
                Flt64.one
            } else {
                Flt64.zero
            }
            logger.trace { "Setting SameAsFunction ${name}.y to $bin" }
            tokenTable.find(y)?.let { token ->
                token._result = yValue
            }

            tokenTable.cache(this, null, yValue)
        }
    }

    override fun register(tokenTable: AbstractMutableTokenTable): Try {
        if (!constraint && inequalities.size > 1) {
            when (val result = tokenTable.add(u)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        when (val result = tokenTable.add(y)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }

    override fun register(model: AbstractLinearMechanismModel): Try {
        if (!constraint && inequalities.size > 1) {
            for ((i, inequality) in inequalities.withIndex()) {
                when (val result = inequality.register(name, k[i, _a], u[i], model)) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }
                }
            }

            when (val result = model.addConstraint(
                y geq (sum(u) - UInt64(inequalities.size) + UInt64.one) / UInt64(inequalities.size),
                "${name}_ub"
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }

            when (val result = model.addConstraint(
                y leq sum(u) / UInt64(inequalities.size),
                "${name}_lb"
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        } else {
            for ((i, inequality) in inequalities.withIndex()) {
                when (val result = inequality.register(name, k[i, _a], y, model)) {
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
            "sane_as(${inequalities.joinToString(", ") { it.toRawString(unfold - UInt64.one) }})"
        }
    }

    override fun evaluate(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        var lastValue: Boolean? = null
        for (inequality in inequalities) {
            val value = inequality.isTrue(tokenList, zeroIfNone) ?: return null
            if (lastValue == null) {
                lastValue = value
            } else if (lastValue != value) {
                return Flt64.zero
            }
        }
        return Flt64.one
    }

    override fun evaluate(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        var lastValue: Boolean? = null
        for (inequality in inequalities) {
            val value = inequality.isTrue(results, tokenList, zeroIfNone) ?: return null
            if (lastValue == null) {
                lastValue = value
            } else if (lastValue != value) {
                return Flt64.zero
            }
        }
        return Flt64.one
    }

    override fun calculateValue(tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        var lastValue: Boolean? = null
        for (inequality in inequalities) {
            val value = inequality.isTrue(tokenTable, zeroIfNone) ?: return null
            if (lastValue == null) {
                lastValue = value
            } else if (lastValue != value) {
                return Flt64.zero
            }
        }
        return Flt64.one
    }

    override fun calculateValue(results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        var lastValue: Boolean? = null
        for (inequality in inequalities) {
            val value = inequality.isTrue(results, tokenTable, zeroIfNone) ?: return null
            if (lastValue == null) {
                lastValue = value
            } else if (lastValue != value) {
                return Flt64.zero
            }
        }
        return Flt64.one
    }
}
