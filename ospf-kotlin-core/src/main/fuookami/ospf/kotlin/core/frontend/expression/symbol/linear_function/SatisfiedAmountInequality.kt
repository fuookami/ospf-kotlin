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

sealed class AbstractSatisfiedAmountInequalityFunction(
    inequalities: List<LinearInequality>,
    private val constraint: Boolean = false,
    override var name: String,
    override var displayName: String? = null
) : LinearFunctionSymbol {
    private val logger = logger()

    open val amount: ValueRange<UInt64>? = null

    protected val inequalities by lazy {
        inequalities.map { it.normalize() }
    }

    private val u: BinVariable1 by lazy {
        BinVariable1("${name}_u", Shape1(inequalities.size))
    }

    private val y: BinVar by lazy {
        BinVar("${name}_y")
    }

    private val polyY: AbstractLinearPolynomial<*> by lazy {
        if (amount != null) {
            if (!constraint) {
                val polyY = LinearPolynomial(y)
                polyY.range.set(possibleRange)
                polyY
            } else {
                LinearPolynomial(1)
            }
        } else {
            val polyY = sum(u)
            polyY.range.set(possibleRange)
            polyY
        }
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
            return if (amount != null) {
                ValueRange(Flt64.zero, Flt64.one).value!!
            } else {
                ValueRange(Flt64.zero, Flt64(inequalities.size)).value!!
            }
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
            val count = inequalities.count { (it.isTrue(tokenTable) ?: return) }

            val yValue = if (amount != null) {
                if (!constraint) {
                    val bin = amount!!.contains(UInt64(count))
                    val yValue = if (bin) {
                        Flt64.one
                    } else {
                        Flt64.zero
                    }

                    logger.trace { "Setting SatisfiedAmountInequalityFunction ${name}.y to $bin" }
                    tokenTable.find(y)?.let { token ->
                        token._result = yValue
                    }
                    yValue
                } else {
                    Flt64.one
                }
            } else {
                Flt64(count)
            }

            tokenTable.cache(this, null, yValue)
        }
    }

    override fun register(tokenTable: AbstractMutableTokenTable): Try {
        when (val result = tokenTable.add(u)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        if (amount != null && !constraint) {
            when (val result = tokenTable.add(y)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        return ok
    }

    override fun register(model: AbstractLinearMechanismModel): Try {
        for ((i, inequality) in inequalities.withIndex()) {
            when (val result = inequality.register(name, u[i], model)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        if (amount != null) {
            if (!constraint) {
                when (val result = model.addConstraint(
                    sum(u) geq amount!!.lowerBound.value.unwrap() - UInt64(inequalities.size) * (Flt64.one - y),
                    "${name}_lb"
                )) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }
                }

                when (val result = model.addConstraint(
                    sum(u) leq amount!!.upperBound.value.unwrap() + UInt64(inequalities.size) * (Flt64.one - y),
                    "${name}_ub"
                )) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }
                }
            } else {
                when (val result = model.addConstraint(
                    sum(u) geq amount!!.lowerBound.value.unwrap(),
                    "${name}_lb"
                )) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }
                }

                when (val result = model.addConstraint(
                    sum(u) leq amount!!.upperBound.value.unwrap(),
                    "${name}_ub"
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
        return if (amount != null) {
            "satisfied_amount_${amount}(${inequalities.joinToString(", ") { it.toRawString(unfold) }})"
        } else {
            "satisfied_amount(${inequalities.joinToString(", ") { it.toRawString(unfold) }})"
        }
    }

    override fun evaluate(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        var counter = UInt64.zero
        for (inequality in inequalities) {
            if (inequality.isTrue(tokenList, zeroIfNone) ?: return null) {
                counter += UInt64.one
            }
        }
        return if (amount != null) {
            if (amount!!.contains(counter)) {
                Flt64.one
            } else {
                Flt64.zero
            }
        } else {
            counter.toFlt64()
        }
    }

    override fun evaluate(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        var counter = UInt64.zero
        for (inequality in inequalities) {
            if (inequality.isTrue(results, tokenList, zeroIfNone) ?: return null) {
                counter += UInt64.one
            }
        }
        return if (amount != null) {
            if (amount!!.contains(counter)) {
                Flt64.one
            } else {
                Flt64.zero
            }
        } else {
            counter.toFlt64()
        }
    }

    override fun calculateValue(tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        var counter = UInt64.zero
        for (inequality in inequalities) {
            if (inequality.isTrue(tokenTable, zeroIfNone) ?: return null) {
                counter += UInt64.one
            }
        }
        return if (amount != null) {
            if (amount!!.contains(counter)) {
                Flt64.one
            } else {
                Flt64.zero
            }
        } else {
            counter.toFlt64()
        }
    }

    override fun calculateValue(results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        var counter = UInt64.zero
        for (inequality in inequalities) {
            if (inequality.isTrue(results, tokenTable, zeroIfNone) ?: return null) {
                counter += UInt64.one
            }
        }
        return if (amount != null) {
            if (amount!!.contains(counter)) {
                Flt64.one
            } else {
                Flt64.zero
            }
        } else {
            counter.toFlt64()
        }
    }
}

// todo: optimize
open class AnyFunction(
    inequalities: List<LinearInequality>,
    name: String,
    displayName: String? = null
) : AbstractSatisfiedAmountInequalityFunction(inequalities, name = name, displayName = displayName),
    LinearLogicFunctionSymbol {
    override val amount: ValueRange<UInt64> = ValueRange(UInt64.one, UInt64(inequalities.size)).value!!

    override fun toRawString(unfold: Boolean): String {
        return "any(${inequalities.joinToString(", ") { it.toRawString(unfold) }})"
    }
}

class InListFunction(
    val x: AbstractLinearPolynomial<*>,
    val list: List<AbstractLinearPolynomial<*>>,
    name: String,
    displayName: String? = null
) : AnyFunction(list.map { x eq it }, name, displayName)

class NotAllFunction(
    inequalities: List<LinearInequality>,
    name: String,
    displayName: String? = null
) : AbstractSatisfiedAmountInequalityFunction(inequalities, name = name, displayName = displayName),
    LinearLogicFunctionSymbol {
    override val amount: ValueRange<UInt64> = ValueRange(UInt64.one, UInt64(inequalities.size - 1)).value!!

    override fun toRawString(unfold: Boolean): String {
        return "for_all(${inequalities.joinToString(", ") { it.toRawString(unfold) }})"
    }
}

// todo: optimize
class AllFunction(
    inequalities: List<LinearInequality>,
    name: String,
    displayName: String? = null
) : AbstractSatisfiedAmountInequalityFunction(inequalities, name = name, displayName = displayName),
    LinearLogicFunctionSymbol {
    override val amount: ValueRange<UInt64> = ValueRange(UInt64(inequalities.size), UInt64(inequalities.size)).value!!

    override fun toRawString(unfold: Boolean): String {
        return "for_all(${inequalities.joinToString(", ") { it.toRawString(unfold) }})"
    }
}

class SatisfiedAmountInequalityFunction(
    inequalities: List<LinearInequality>,
    name: String,
    displayName: String? = null
) : AbstractSatisfiedAmountInequalityFunction(inequalities, name = name, displayName = displayName)

class AtLeastInequalityFunction(
    inequalities: List<LinearInequality>,
    constraint: Boolean = true,
    amount: UInt64,
    name: String,
    displayName: String? = null
) : AbstractSatisfiedAmountInequalityFunction(inequalities, constraint, name, displayName), LinearLogicFunctionSymbol {
    init {
        assert(amount != UInt64.zero)
        assert(UInt64(inequalities.size) geq amount)
    }

    override val amount: ValueRange<UInt64> = ValueRange(amount, UInt64(inequalities.size)).value!!

    override fun toRawString(unfold: Boolean): String {
        return "at_least_${amount}(${inequalities.joinToString(", ") { it.toRawString(unfold) }})"
    }
}

class NumerableFunction(
    inequalities: List<LinearInequality>,
    override val amount: ValueRange<UInt64>,
    constraint: Boolean = true,
    name: String,
    displayName: String? = null
) : AbstractSatisfiedAmountInequalityFunction(inequalities, constraint, name, displayName), LinearLogicFunctionSymbol {
    override fun toRawString(unfold: Boolean): String {
        return "numerable_${amount}(${inequalities.joinToString(", ") { it.toRawString(unfold) }})"
    }
}
