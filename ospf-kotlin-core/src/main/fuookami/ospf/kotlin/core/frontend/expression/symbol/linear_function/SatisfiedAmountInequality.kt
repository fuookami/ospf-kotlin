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
    private val epsilon: Flt64 = Flt64(1e-6),
    override val parent: IntermediateSymbol? = null,
    override var name: String,
    override var displayName: String? = null
) : LinearFunctionSymbol {
    private val logger = logger()

    open val amount: ValueRange<UInt64>? = null

    protected val inequalities by lazy {
        inequalities.map { it.normalize() }
    }

    private val k: PctVariable2 by lazy {
        PctVariable2("${name}_k", Shape2(inequalities.size, 3))
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

    override fun prepare(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable): Flt64? {
        for (inequality in inequalities) {
            inequality.lhs.cells
            inequality.rhs.cells
        }

        return if ((!values.isNullOrEmpty() || tokenTable.cachedSolution) && if (values.isNullOrEmpty()) {
            tokenTable.cached(this)
        } else {
            tokenTable.cached(this, values)
        } == false) {
            val count = inequalities.count {
                if (values.isNullOrEmpty()) {
                    it.isTrue(tokenTable)
                } else {
                    it.isTrue(values, tokenTable)
                }?: return null
            }

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

            yValue
        } else {
            null
        }
    }

    override fun register(tokenTable: AddableTokenCollection): Try {
        for ((i, inequality) in inequalities.withIndex()) {
            when (val result = inequality.register(
                parentName = "${name}_i",
                k = k[i, _a],
                flag = u[i],
                tokenTable = tokenTable
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
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
            when (val result = inequality.register(
                parent = parent ?: this,
                parentName = "${name}_i",
                k = k[i, _a],
                flag = u[i],
                epsilon = epsilon,
                model = model
            )) {
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
                    name = "${name}_lb",
                    from = parent ?: this
                )) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }
                }

                when (val result = model.addConstraint(
                    sum(u) leq amount!!.upperBound.value.unwrap() + UInt64(inequalities.size) * (Flt64.one - y),
                    name = "${name}_ub",
                    from = parent ?: this
                )) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }
                }
            } else {
                when (val result = model.addConstraint(
                    sum(u) geq amount!!.lowerBound.value.unwrap(),
                    name = "${name}_lb",
                    from = parent ?: this
                )) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }
                }

                when (val result = model.addConstraint(
                    sum(u) leq amount!!.upperBound.value.unwrap(),
                    name = "${name}_ub",
                    from = parent ?: this
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
        tokenTable: AddableTokenCollection,
        fixedValues: Map<Symbol, Flt64>
    ): Try {
        return register(tokenTable)
    }

    override fun register(
        model: AbstractLinearMechanismModel,
        fixedValues: Map<Symbol, Flt64>
    ): Try {
        val values = inequalities.map {
            it.isTrue(fixedValues, model.tokens) ?: return register(model)
        }
        val amountValue = UInt64(values.count { it })

        for ((i, inequality) in inequalities.withIndex()) {
            when (val result = inequality.register(
                parent = parent ?: this,
                parentName = "${name}_i",
                k = k[i, _a],
                flag = u[i],
                epsilon = epsilon,
                model = model,
                fixedValues = fixedValues
            )) {
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
                    name = "${name}_lb",
                    from = parent ?: this
                )) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }
                }

                when (val result = model.addConstraint(
                    sum(u) leq amount!!.upperBound.value.unwrap() + UInt64(inequalities.size) * (Flt64.one - y),
                    name = "${name}_ub",
                    from = parent ?: this
                )) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }
                }

                val bin = amount!!.contains(amountValue)

                when (val result = model.addConstraint(
                    y eq bin,
                    name = "${name}_y",
                    from = parent ?: this
                )) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }
                }

                model.tokens.find(y)?.let { token ->
                    token._result = bin.toFlt64()
                }
            } else {
                when (val result = model.addConstraint(
                    sum(u) geq amount!!.lowerBound.value.unwrap(),
                    name = "${name}_lb",
                    from = parent ?: this
                )) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }
                }

                when (val result = model.addConstraint(
                    sum(u) leq amount!!.upperBound.value.unwrap(),
                    name = "${name}_ub",
                    from = parent ?: this
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
            if (amount != null) {
                "satisfied_amount_${amount}(${inequalities.joinToString(", ") { it.toRawString(unfold - UInt64.one) }})"
            } else {
                "satisfied_amount(${inequalities.joinToString(", ") { it.toRawString(unfold - UInt64.one) }})"
            }
        }
    }

    override fun evaluate(
        tokenList: AbstractTokenList,
        zeroIfNone: Boolean
    ): Flt64? {
        var counter = UInt64.zero
        for (inequality in inequalities) {
            val value = inequality.isTrue(tokenList, zeroIfNone) ?: return null
            if (value) {
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

    override fun evaluate(
        results: List<Flt64>,
        tokenList: AbstractTokenList,
        zeroIfNone: Boolean
    ): Flt64? {
        var counter = UInt64.zero
        for (inequality in inequalities) {
            val value = inequality.isTrue(results, tokenList, zeroIfNone) ?: return null
            if (value) {
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

    override fun evaluate(
        values: Map<Symbol, Flt64>,
        tokenList: AbstractTokenList?,
        zeroIfNone: Boolean
    ): Flt64? {
        var counter = UInt64.zero
        for (inequality in inequalities) {
            val value = inequality.isTrue(values, tokenList, zeroIfNone) ?: return null
            if (value) {
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

    override fun calculateValue(
        tokenTable: AbstractTokenTable,
        zeroIfNone: Boolean
    ): Flt64? {
        var counter = UInt64.zero
        for (inequality in inequalities) {
            val value = inequality.isTrue(tokenTable, zeroIfNone) ?: return null
            if (value) {
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

    override fun calculateValue(
        results: List<Flt64>,
        tokenTable: AbstractTokenTable,
        zeroIfNone: Boolean
    ): Flt64? {
        var counter = UInt64.zero
        for (inequality in inequalities) {
            val value = inequality.isTrue(results, tokenTable, zeroIfNone) ?: return null
            if (value) {
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

    override fun calculateValue(
        values: Map<Symbol, Flt64>,
        tokenTable: AbstractTokenTable?,
        zeroIfNone: Boolean
    ): Flt64? {
        var counter = UInt64.zero
        for (inequality in inequalities) {
            val value = inequality.isTrue(values, tokenTable, zeroIfNone) ?: return null
            if (value) {
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
    parent: IntermediateSymbol? = null,
    name: String,
    displayName: String? = null
) : AbstractSatisfiedAmountInequalityFunction(
    inequalities = inequalities,
    parent = parent,
    name = name,
    displayName = displayName
), LinearLogicFunctionSymbol {
    companion object {
        operator fun invoke(
            inequalities: List<ToLinearInequality>,
            parent: IntermediateSymbol? = null,
            name: String,
            displayName: String? = null
        ): AnyFunction {
            return AnyFunction(
                inequalities = inequalities.map { it.toLinearInequality() },
                parent = parent,
                name = name,
                displayName = displayName
            )
        }
    }

    override val amount = ValueRange(UInt64.one, UInt64(inequalities.size)).value!!

    override fun toRawString(unfold: UInt64): String {
        return if (unfold eq UInt64.zero) {
            displayName ?: name
        } else {
            "any(${inequalities.joinToString(", ") { it.toRawString(unfold - UInt64.one) }})"
        }
    }
}

class InListFunction(
    val x: AbstractLinearPolynomial<*>,
    val list: List<AbstractLinearPolynomial<*>>,
    parent: IntermediateSymbol? = null,
    name: String,
    displayName: String? = null
) : AnyFunction(
    inequalities = list.map { x eq it },
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
            list: List<ToLinearPolynomial<*>>,
            parent: IntermediateSymbol? = null,
            name: String,
            displayName: String? = null
        ): InListFunction {
            return InListFunction(
                x = x.toLinearPolynomial(),
                list = list.map { it.toLinearInequality().lhs },
                parent = parent,
                name = name,
                displayName = displayName
            )
        }
    }
}

class NotAllFunction(
    inequalities: List<LinearInequality>,
    parent: IntermediateSymbol? = null,
    name: String,
    displayName: String? = null
) : AbstractSatisfiedAmountInequalityFunction(
    inequalities = inequalities,
    parent = parent,
    name = name,
    displayName = displayName
), LinearLogicFunctionSymbol {
    companion object {
        operator fun invoke(
            inequalities: List<ToLinearInequality>,
            parent: IntermediateSymbol? = null,
            name: String,
            displayName: String? = null
        ): NotAllFunction {
            return NotAllFunction(
                inequalities = inequalities.map { it.toLinearInequality() },
                parent = parent,
                name = name,
                displayName = displayName
            )
        }
    }

    override val amount = ValueRange(UInt64.one, UInt64(inequalities.lastIndex)).value!!

    override fun toRawString(unfold: UInt64): String {
        return if (unfold eq UInt64.zero) {
            displayName ?: name
        } else {
            "not_all(${inequalities.joinToString(", ") { it.toRawString(unfold - UInt64.one) }})"
        }
    }
}

// todo: optimize
class AllFunction(
    inequalities: List<LinearInequality>,
    parent: IntermediateSymbol? = null,
    name: String,
    displayName: String? = null
) : AbstractSatisfiedAmountInequalityFunction(
    inequalities = inequalities,
    parent = parent,
    name = name,
    displayName = displayName
), LinearLogicFunctionSymbol {
    companion object {
        operator fun invoke(
            inequalities: List<ToLinearInequality>,
            parent: IntermediateSymbol? = null,
            name: String,
            displayName: String? = null
        ): AllFunction {
            return AllFunction(
                inequalities = inequalities.map { it.toLinearInequality() },
                parent = parent,
                name = name,
                displayName = displayName
            )
        }
    }

    override val amount = ValueRange(UInt64(inequalities.size), UInt64(inequalities.size)).value!!

    override fun toRawString(unfold: UInt64): String {
        return if (unfold eq UInt64.zero) {
            displayName ?: name
        } else {
            "for_all(${inequalities.joinToString(", ") { it.toRawString(unfold - UInt64.one) }})"
        }
    }
}

class SatisfiedAmountInequalityFunction(
    inequalities: List<LinearInequality>,
    parent: IntermediateSymbol? = null,
    name: String,
    displayName: String? = null
) : AbstractSatisfiedAmountInequalityFunction(
    inequalities = inequalities,
    parent = parent,
    name = name,
    displayName = displayName
) {
    companion object {
        operator fun invoke(
            inequalities: List<ToLinearInequality>,
            parent: IntermediateSymbol? = null,
            name: String,
            displayName: String? = null
        ): SatisfiedAmountInequalityFunction {
            return SatisfiedAmountInequalityFunction(
                inequalities = inequalities.map { it.toLinearInequality() },
                parent = parent,
                name = name,
                displayName = displayName
            )
        }
    }
}

class AtLeastInequalityFunction(
    inequalities: List<LinearInequality>,
    constraint: Boolean = true,
    amount: UInt64,
    epsilon: Flt64 = Flt64(1e-6),
    parent: IntermediateSymbol? = null,
    name: String,
    displayName: String? = null
) : AbstractSatisfiedAmountInequalityFunction(
    inequalities = inequalities,
    constraint = constraint,
    epsilon = epsilon,
    parent = parent,
    name = name,
    displayName = displayName
), LinearLogicFunctionSymbol {
    companion object {
        operator fun invoke(
            inequalities: List<ToLinearInequality>,
            constraint: Boolean = true,
            amount: UInt64,
            epsilon: Flt64 = Flt64(1e-6),
            parent: IntermediateSymbol? = null,
            name: String,
            displayName: String? = null
        ): AtLeastInequalityFunction {
            return AtLeastInequalityFunction(
                inequalities = inequalities.map { it.toLinearInequality() },
                constraint = constraint,
                amount = amount,
                epsilon = epsilon,
                parent = parent,
                name = name,
                displayName = displayName
            )
        }
    }

    init {
        assert(amount != UInt64.zero)
        assert(UInt64(inequalities.size) geq amount)
    }

    override val amount = ValueRange(amount, UInt64(inequalities.size)).value!!

    override fun toRawString(unfold: UInt64): String {
        return if (unfold eq UInt64.zero) {
            displayName ?: name
        } else {
            "at_least_${amount}(${inequalities.joinToString(", ") { it.toRawString(unfold - UInt64.one) }})"
        }
    }
}

class NumerableFunction(
    inequalities: List<LinearInequality>,
    override val amount: ValueRange<UInt64>,
    constraint: Boolean = true,
    epsilon: Flt64 = Flt64(1e-6),
    parent: IntermediateSymbol? = null,
    name: String,
    displayName: String? = null
) : AbstractSatisfiedAmountInequalityFunction(
    inequalities = inequalities,
    constraint = constraint,
    epsilon = epsilon,
    parent = parent,
    name = name,
    displayName = displayName
), LinearLogicFunctionSymbol {
    companion object {
        operator fun invoke(
            inequalities: List<ToLinearInequality>,
            amount: ValueRange<UInt64>,
            constraint: Boolean = true,
            epsilon: Flt64 = Flt64(1e-6),
            parent: IntermediateSymbol? = null,
            name: String,
            displayName: String? = null
        ): NumerableFunction {
            return NumerableFunction(
                inequalities = inequalities.map { it.toLinearInequality() },
                amount = amount,
                constraint = constraint,
                epsilon = epsilon,
                parent = parent,
                name = name,
                displayName = displayName
            )
        }
    }

    override fun toRawString(unfold: UInt64): String {
        return if (unfold eq UInt64.zero) {
            displayName ?: name
        } else {
            "numerable_${amount}(${inequalities.joinToString(", ") { it.toRawString(unfold - UInt64.one) }})"
        }
    }
}
