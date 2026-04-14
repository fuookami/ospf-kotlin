@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package fuookami.ospf.kotlin.core.intermediate_symbol.legacy.linear_function

import fuookami.ospf.kotlin.core.expression.polynomial.*
import fuookami.ospf.kotlin.core.intermediate_symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearFunctionSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.LogicFunctionSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.prepareIfNotCached
import fuookami.ospf.kotlin.core.intermediate_model.*
import fuookami.ospf.kotlin.core.intermediate_model.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.intermediate_model.AbstractTokenTable
import fuookami.ospf.kotlin.core.intermediate_model.LinearConstraintInput
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.Flt64LinearInequality as MathLinearInequality
import fuookami.ospf.kotlin.math.toFlt64
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.multiarray.Shape1
import fuookami.ospf.kotlin.multiarray.Shape2
import fuookami.ospf.kotlin.multiarray._a
import org.apache.logging.log4j.kotlin.logger

sealed class AbstractSatisfiedAmountInequalityFunction(
    inputs: List<LinearConstraintInput>,
    private val constraint: Boolean = false,
    private val epsilon: Flt64 = Flt64(1e-6),
    override val parent: IntermediateSymbol? = null,
    args: Any? = null,
    override var name: String,
    override var displayName: String? = null
) : LinearFunctionSymbol() {
    private val logger = logger()

    internal val _args = args
    override val args get() = _args ?: parent?.args

    open val amount: ValueRange<UInt64>? = null

    protected val inputs by lazy { inputs }

    private val k: PctVariable2 by lazy {
        PctVariable2("${name}_k", Shape2(inputs.size, 3))
    }

    private val u: BinVariable1 by lazy {
        BinVariable1("${name}_u", Shape1(inputs.size))
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
            for (input in inputs) {
                for (monomial in input.flattenData.monomials) {
                    if (monomial.symbol is IntermediateSymbol) {
                        dependencies.add(monomial.symbol as IntermediateSymbol)
                    }
                }
            }
            return dependencies
        }
    override val cells get() = polyY.cells
    override val cached get() = polyY.cached

    private val possibleRange: ValueRange<Flt64>
        get() {
            return if (amount != null) {
                ValueRange(Flt64.zero, Flt64.one).value!!
            } else {
                ValueRange(Flt64.zero, Flt64(inputs.size)).value!!
            }
        }

    override fun flush(force: Boolean) {
        polyY.flush(force)
        polyY.range.set(possibleRange)
    }

    override fun prepare(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable): Flt64? {
        return prepareIfNotCached(values, tokenTable) {
            val count = inputs.count {
                if (values.isNullOrEmpty()) {
                    it.isTrue(tokenTable)
                } else {
                    it.isTrue(values, tokenTable)
                } ?: return null
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
        }
    }

    override fun register(tokenTable: AddableTokenCollection): Try {
        for ((i, input) in inputs.withIndex()) {
            when (val result = input.register(
                parentName = "${name}_i",
                k = k[i, _a],
                flag = u[i],
                tokenTable = tokenTable
            )) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }

        if (amount != null && !constraint) {
            when (val result = tokenTable.add(y)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }

        return ok
    }

    override fun register(model: AbstractLinearMechanismModel): Try {
        for ((i, input) in inputs.withIndex()) {
            when (val result = input.register(
                parent = parent ?: this,
                parentName = "${name}_i",
                k = k[i, _a],
                flag = u[i],
                epsilon = epsilon,
                model = model
            )) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }

        if (amount != null) {
            if (!constraint) {
                when (val result = model.addConstraint(
                    relation = sum(u) geq amount!!.lowerBound.value.unwrap() - UInt64(inputs.size) * (Flt64.one - y),
                    name = "${name}_lb",
                    from = parent ?: this
                )) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }

                when (val result = model.addConstraint(
                    relation = sum(u) leq amount!!.upperBound.value.unwrap() + UInt64(inputs.size) * (Flt64.one - y),
                    name = "${name}_ub",
                    from = parent ?: this
                )) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            } else {
                when (val result = model.addConstraint(
                    relation = sum(u) geq amount!!.lowerBound.value.unwrap(),
                    name = "${name}_lb",
                    from = parent ?: this
                )) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }

                when (val result = model.addConstraint(
                    relation = sum(u) leq amount!!.upperBound.value.unwrap(),
                    name = "${name}_ub",
                    from = parent ?: this
                )) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
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
        val values = inputs.map {
            it.isTrue(fixedValues, model.tokens) ?: return register(model)
        }
        val amountValue = UInt64(values.count { it })

        for ((i, input) in inputs.withIndex()) {
            when (val result = input.register(
                parent = parent ?: this,
                parentName = "${name}_i",
                k = k[i, _a],
                flag = u[i],
                epsilon = epsilon,
                model = model,
                fixedValues = fixedValues
            )) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }

        if (amount != null) {
            if (!constraint) {
                when (val result = model.addConstraint(
                    relation = sum(u) geq amount!!.lowerBound.value.unwrap() - UInt64(inputs.size) * (Flt64.one - y),
                    name = "${name}_lb",
                    from = parent ?: this
                )) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }

                when (val result = model.addConstraint(
                    relation = sum(u) leq amount!!.upperBound.value.unwrap() + UInt64(inputs.size) * (Flt64.one - y),
                    name = "${name}_ub",
                    from = parent ?: this
                )) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }

                val bin = amount!!.contains(amountValue)

                when (val result = model.addConstraint(
                    relation = y eq bin,
                    name = "${name}_y",
                    from = parent ?: this
                )) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }

                model.tokens.find(y)?.let { token ->
                    token._result = bin.toFlt64()
                }
            } else {
                when (val result = model.addConstraint(
                    relation = sum(u) geq amount!!.lowerBound.value.unwrap(),
                    name = "${name}_lb",
                    from = parent ?: this
                )) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }

                when (val result = model.addConstraint(
                    relation = sum(u) leq amount!!.upperBound.value.unwrap(),
                    name = "${name}_ub",
                    from = parent ?: this
                )) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
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
                "satisfied_amount_${amount}(${inputs.joinToString(", ") { it.name }})"
            } else {
                "satisfied_amount(${inputs.joinToString(", ") { it.name }})"
            }
        }
    }

    override fun evaluate(
        tokenList: AbstractTokenList,
        zeroIfNone: Boolean
    ): Flt64? {
        var counter = UInt64.zero
        for (input in inputs) {
            val value = input.isTrue(tokenList, zeroIfNone) ?: return null
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
        for (input in inputs) {
            val value = input.isTrue(
                results = results,
                tokenList = tokenList,
                zeroIfNone = zeroIfNone
            ) ?: return null
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
        for (input in inputs) {
            val value = input.isTrue(
                values = values,
                tokenList = tokenList,
                zeroIfNone = zeroIfNone
            ) ?: return null
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
        for (input in inputs) {
            val value = input.isTrue(tokenTable, zeroIfNone) ?: return null
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
        for (input in inputs) {
            val value = input.isTrue(
                results = results,
                tokenTable = tokenTable,
                zeroIfNone = zeroIfNone
            ) ?: return null
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
        for (input in inputs) {
            val value = input.isTrue(
                values = values,
                tokenTable = tokenTable,
                zeroIfNone = zeroIfNone
            ) ?: return null
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
    inequalities: List<LinearConstraintInput>,
    parent: IntermediateSymbol? = null,
    args: Any? = null,
    name: String,
    displayName: String? = null
) : AbstractSatisfiedAmountInequalityFunction(
    inputs = inequalities,
    parent = parent,
    args = args,
    name = name,
    displayName = displayName
) {
    companion object {
        operator fun invoke(
            inequalities: List<AbstractLinearPolynomial<*>>,
            parent: IntermediateSymbol? = null,
            args: Any? = null,
            name: String,
            displayName: String? = null
        ): AnyFunction {
            return AnyFunction(
                inequalities = inequalities.map {
                    LinearConstraintInput.from(
                        it.toMathLinearInequality(),
                        lhsRange = it.range.valueRange!!
                    )
                },
                parent = parent,
                args = args,
                name = name,
                displayName = displayName
            )
        }
    }

    override val amount = ValueRange(UInt64.one, UInt64(inputs.size)).value!!

    override fun toRawString(unfold: UInt64): String {
        return if (unfold eq UInt64.zero) {
            displayName ?: name
        } else {
            "any(${inputs.joinToString(", ") { it.name }})"
        }
    }
}

@Deprecated(
    message = "Use intermediate_symbol.function.AnyFunction instead",
    replaceWith = ReplaceWith("AnyFunction", "fuookami.ospf.kotlin.core.intermediate_symbol.function.AnyFunction")
)
class InListFunction(
    val x: AbstractLinearPolynomial<*>,
    val list: List<AbstractLinearPolynomial<*>>,
    parent: IntermediateSymbol? = null,
    args: Any? = null,
    name: String,
    displayName: String? = null
) : AnyFunction(
    inequalities = list.map {
        LinearConstraintInput.from(
            (x eq it).normalize(),
            lhsRange = (x - it).range.valueRange!!,
            rhsConstant = Flt64.zero
        )
    },
    parent = parent,
    args = args,
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
            args: Any? = null,
            name: String,
            displayName: String? = null
        ): InListFunction {
            return InListFunction(
                x = x.toLinearPolynomial(),
                list = list.map { it.toLinearPolynomial() },
                parent = parent,
                args = args,
                name = name,
                displayName = displayName
            )
        }
    }
}

@Deprecated(
    message = "Use intermediate_symbol.function.NotAllFunction instead",
    replaceWith = ReplaceWith("NotAllFunction", "fuookami.ospf.kotlin.core.intermediate_symbol.function.NotAllFunction")
)
class NotAllFunction(
    inequalities: List<LinearConstraintInput>,
    parent: IntermediateSymbol? = null,
    args: Any? = null,
    name: String,
    displayName: String? = null
) : AbstractSatisfiedAmountInequalityFunction(
    inputs = inequalities,
    parent = parent,
    args = args,
    name = name,
    displayName = displayName
), LogicFunctionSymbol {
    companion object {
        operator fun invoke(
            inequalities: List<AbstractLinearPolynomial<*>>,
            parent: IntermediateSymbol? = null,
            args: Any? = null,
            name: String,
            displayName: String? = null
        ): NotAllFunction {
            return NotAllFunction(
                inequalities = inequalities.map {
                    LinearConstraintInput.from(
                        it.toMathLinearInequality(),
                        lhsRange = it.range.valueRange!!
                    )
                },
                parent = parent,
                args = args,
                name = name,
                displayName = displayName
            )
        }
    }

    override val amount = ValueRange(UInt64.one, UInt64(inputs.lastIndex)).value!!

    override fun toRawString(unfold: UInt64): String {
        return if (unfold eq UInt64.zero) {
            displayName ?: name
        } else {
            "not_all(${inputs.joinToString(", ") { it.name }})"
        }
    }
}

// todo: optimize
@Deprecated(
    message = "Use intermediate_symbol.function.AllFunction instead",
    replaceWith = ReplaceWith("AllFunction", "fuookami.ospf.kotlin.core.intermediate_symbol.function.AllFunction")
)
class AllFunction(
    inequalities: List<LinearConstraintInput>,
    parent: IntermediateSymbol? = null,
    args: Any? = null,
    name: String,
    displayName: String? = null
) : AbstractSatisfiedAmountInequalityFunction(
    inputs = inequalities,
    parent = parent,
    args = args,
    name = name,
    displayName = displayName
), LogicFunctionSymbol {
    companion object {
        operator fun invoke(
            inequalities: List<AbstractLinearPolynomial<*>>,
            parent: IntermediateSymbol? = null,
            args: Any? = null,
            name: String,
            displayName: String? = null
        ): AllFunction {
            return AllFunction(
                inequalities = inequalities.map {
                    LinearConstraintInput.from(
                        it.toMathLinearInequality(),
                        lhsRange = it.range.valueRange!!
                    )
                },
                parent = parent,
                args = args,
                name = name,
                displayName = displayName
            )
        }
    }

    override val amount = ValueRange(UInt64(inputs.size), UInt64(inputs.size)).value!!

    override fun toRawString(unfold: UInt64): String {
        return if (unfold eq UInt64.zero) {
            displayName ?: name
        } else {
            "for_all(${inputs.joinToString(", ") { it.name }})"
        }
    }
}

@Deprecated(
    message = "Use intermediate_symbol.function.SatisfiedAmountInequalityFunction instead",
    replaceWith = ReplaceWith("SatisfiedAmountInequalityFunction", "fuookami.ospf.kotlin.core.intermediate_symbol.function.SatisfiedAmountInequalityFunction")
)
class SatisfiedAmountInequalityFunction(
    inequalities: List<LinearConstraintInput>,
    parent: IntermediateSymbol? = null,
    args: Any? = null,
    name: String,
    displayName: String? = null
) : AbstractSatisfiedAmountInequalityFunction(
    inputs = inequalities,
    parent = parent,
    args = args,
    name = name,
    displayName = displayName
) {
    companion object {
        operator fun invoke(
            inequalities: List<AbstractLinearPolynomial<*>>,
            parent: IntermediateSymbol? = null,
            args: Any? = null,
            name: String,
            displayName: String? = null
        ): SatisfiedAmountInequalityFunction {
            return SatisfiedAmountInequalityFunction(
                inequalities = inequalities.map {
                    LinearConstraintInput.from(
                        it.toMathLinearInequality(),
                        lhsRange = it.range.valueRange!!
                    )
                },
                parent = parent,
                args = args,
                name = name,
                displayName = displayName
            )
        }
    }
}

@Deprecated(
    message = "Use intermediate_symbol.function.AtLeastInequalityFunction instead",
    replaceWith = ReplaceWith("AtLeastInequalityFunction", "fuookami.ospf.kotlin.core.intermediate_symbol.function.AtLeastInequalityFunction")
)
class AtLeastInequalityFunction(
    inequalities: List<LinearConstraintInput>,
    constraint: Boolean = true,
    amount: UInt64,
    epsilon: Flt64 = Flt64(1e-6),
    parent: IntermediateSymbol? = null,
    args: Any? = null,
    name: String,
    displayName: String? = null
) : AbstractSatisfiedAmountInequalityFunction(
    inputs = inequalities,
    constraint = constraint,
    epsilon = epsilon,
    parent = parent,
    args = args,
    name = name,
    displayName = displayName
), LogicFunctionSymbol {
    companion object {
        operator fun invoke(
            inequalities: List<AbstractLinearPolynomial<*>>,
            constraint: Boolean = true,
            amount: UInt64,
            epsilon: Flt64 = Flt64(1e-6),
            parent: IntermediateSymbol? = null,
            args: Any? = null,
            name: String,
            displayName: String? = null
        ): AtLeastInequalityFunction {
            return AtLeastInequalityFunction(
                inequalities = inequalities.map {
                    LinearConstraintInput.from(
                        it.toMathLinearInequality(),
                        lhsRange = it.range.valueRange!!
                    )
                },
                constraint = constraint,
                amount = amount,
                epsilon = epsilon,
                parent = parent,
                args = args,
                name = name,
                displayName = displayName
            )
        }
    }

    init {
        assert(amount != UInt64.zero)
        assert(UInt64(inputs.size) geq amount)
    }

    override val amount = ValueRange(amount, UInt64(inputs.size)).value!!

    override fun toRawString(unfold: UInt64): String {
        return if (unfold eq UInt64.zero) {
            displayName ?: name
        } else {
            "at_least_${amount}(${inputs.joinToString(", ") { it.name }})"
        }
    }
}

@Deprecated(
    message = "Use intermediate_symbol.function.NumerableFunction instead",
    replaceWith = ReplaceWith("NumerableFunction", "fuookami.ospf.kotlin.core.intermediate_symbol.function.NumerableFunction")
)
class NumerableFunction(
    inequalities: List<LinearConstraintInput>,
    override val amount: ValueRange<UInt64>,
    constraint: Boolean = true,
    epsilon: Flt64 = Flt64(1e-6),
    parent: IntermediateSymbol? = null,
    args: Any? = null,
    name: String,
    displayName: String? = null
) : AbstractSatisfiedAmountInequalityFunction(
    inputs = inequalities,
    constraint = constraint,
    epsilon = epsilon,
    parent = parent,
    args = args,
    name = name,
    displayName = displayName
), LogicFunctionSymbol {
    companion object {
        operator fun invoke(
            inequalities: List<AbstractLinearPolynomial<*>>,
            amount: ValueRange<UInt64>,
            constraint: Boolean = true,
            epsilon: Flt64 = Flt64(1e-6),
            parent: IntermediateSymbol? = null,
            args: Any? = null,
            name: String,
            displayName: String? = null
        ): NumerableFunction {
            return NumerableFunction(
                inequalities = inequalities.map {
                    LinearConstraintInput.from(
                        it.toMathLinearInequality(),
                        lhsRange = it.range.valueRange!!
                    )
                },
                amount = amount,
                constraint = constraint,
                epsilon = epsilon,
                parent = parent,
                args = args,
                name = name,
                displayName = displayName
            )
        }
    }

    override fun toRawString(unfold: UInt64): String {
        return if (unfold eq UInt64.zero) {
            displayName ?: name
        } else {
            "numerable_${amount}(${inputs.joinToString(", ") { it.name }})"
        }
    }
}
