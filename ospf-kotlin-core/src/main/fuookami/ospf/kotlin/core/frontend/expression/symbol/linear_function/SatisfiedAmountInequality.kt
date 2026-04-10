package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function

import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.frontend.expression.symbol.LinearFunctionSymbol
import fuookami.ospf.kotlin.core.frontend.expression.symbol.LogicFunctionSymbol
import fuookami.ospf.kotlin.core.frontend.expression.symbol.prepareIfNotCached
import fuookami.ospf.kotlin.core.frontend.inequality.LinearInequality
import fuookami.ospf.kotlin.core.frontend.inequality.Sign as InequalitySign
import fuookami.ospf.kotlin.core.frontend.inequality.ToLinearInequality
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.frontend.model.mechanism.AbstractTokenTable
import fuookami.ospf.kotlin.core.frontend.model.mechanism.LinearConstraintInput
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality as MathLinearInequality
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Symbol
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
                    relation = sum(u) leq amount!!.upperBound.value.unwrap().toFlt64() + UInt64(inputs.size).toFlt64() * (Flt64.one - y),
                    name = "${name}_ub",
                    from = parent ?: this
                )) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            } else {
                when (val result = model.addConstraint(
                    relation = sum(u) geq amount!!.lowerBound.value.unwrap().toFlt64(),
                    name = "${name}_lb",
                    from = parent ?: this
                )) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }

                when (val result = model.addConstraint(
                    relation = sum(u) leq amount!!.upperBound.value.unwrap().toFlt64(),
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
                    relation = sum(u) leq amount!!.upperBound.value.unwrap().toFlt64() + UInt64(inputs.size).toFlt64() * (Flt64.one - y),
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
                    relation = sum(u) geq amount!!.lowerBound.value.unwrap().toFlt64(),
                    name = "${name}_lb",
                    from = parent ?: this
                )) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }

                when (val result = model.addConstraint(
                    relation = sum(u) leq amount!!.upperBound.value.unwrap().toFlt64(),
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
    inequalities: List<LinearInequality>,
    parent: IntermediateSymbol? = null,
    args: Any? = null,
    name: String,
    displayName: String? = null
) : AbstractSatisfiedAmountInequalityFunction(
    inputs = inequalities.map { LinearConstraintInput.from(it) },
    parent = parent,
    args = args,
    name = name,
    displayName = displayName
) {
    companion object {
        operator fun invoke(
            inequalities: List<ToLinearInequality>,
            parent: IntermediateSymbol? = null,
            args: Any? = null,
            name: String,
            displayName: String? = null
        ): AnyFunction {
            return AnyFunction(
                inequalities = inequalities.map { it.toLinearInequality() },
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

class InListFunction(
    val x: AbstractLinearPolynomial<*>,
    val list: List<AbstractLinearPolynomial<*>>,
    parent: IntermediateSymbol? = null,
    args: Any? = null,
    name: String,
    displayName: String? = null
) : AnyFunction(
    inequalities = list.map { LinearInequality(x.copy(), it.copy(), InequalitySign.Equal) },
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
                list = list.map { it.toLinearInequality().lhs },
                parent = parent,
                args = args,
                name = name,
                displayName = displayName
            )
        }
    }
}

class NotAllFunction(
    inequalities: List<LinearInequality>,
    parent: IntermediateSymbol? = null,
    args: Any? = null,
    name: String,
    displayName: String? = null
) : AbstractSatisfiedAmountInequalityFunction(
    inputs = inequalities.map { LinearConstraintInput.from(it) },
    parent = parent,
    args = args,
    name = name,
    displayName = displayName
), LogicFunctionSymbol {
    companion object {
        operator fun invoke(
            inequalities: List<ToLinearInequality>,
            parent: IntermediateSymbol? = null,
            args: Any? = null,
            name: String,
            displayName: String? = null
        ): NotAllFunction {
            return NotAllFunction(
                inequalities = inequalities.map { it.toLinearInequality() },
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
class AllFunction(
    inequalities: List<LinearInequality>,
    parent: IntermediateSymbol? = null,
    args: Any? = null,
    name: String,
    displayName: String? = null
) : AbstractSatisfiedAmountInequalityFunction(
    inputs = inequalities.map { LinearConstraintInput.from(it) },
    parent = parent,
    args = args,
    name = name,
    displayName = displayName
), LogicFunctionSymbol {
    companion object {
        operator fun invoke(
            inequalities: List<ToLinearInequality>,
            parent: IntermediateSymbol? = null,
            args: Any? = null,
            name: String,
            displayName: String? = null
        ): AllFunction {
            return AllFunction(
                inequalities = inequalities.map { it.toLinearInequality() },
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

class SatisfiedAmountInequalityFunction(
    inequalities: List<LinearInequality>,
    parent: IntermediateSymbol? = null,
    args: Any? = null,
    name: String,
    displayName: String? = null
) : AbstractSatisfiedAmountInequalityFunction(
    inputs = inequalities.map { LinearConstraintInput.from(it) },
    parent = parent,
    args = args,
    name = name,
    displayName = displayName
) {
    companion object {
        operator fun invoke(
            inequalities: List<ToLinearInequality>,
            parent: IntermediateSymbol? = null,
            args: Any? = null,
            name: String,
            displayName: String? = null
        ): SatisfiedAmountInequalityFunction {
            return SatisfiedAmountInequalityFunction(
                inequalities = inequalities.map { it.toLinearInequality() },
                parent = parent,
                args = args,
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
    args: Any? = null,
    name: String,
    displayName: String? = null
) : AbstractSatisfiedAmountInequalityFunction(
    inputs = inequalities.map { LinearConstraintInput.from(it) },
    constraint = constraint,
    epsilon = epsilon,
    parent = parent,
    args = args,
    name = name,
    displayName = displayName
), LogicFunctionSymbol {
    companion object {
        operator fun invoke(
            inequalities: List<ToLinearInequality>,
            constraint: Boolean = true,
            amount: UInt64,
            epsilon: Flt64 = Flt64(1e-6),
            parent: IntermediateSymbol? = null,
            args: Any? = null,
            name: String,
            displayName: String? = null
        ): AtLeastInequalityFunction {
            return AtLeastInequalityFunction(
                inequalities = inequalities.map { it.toLinearInequality() },
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

class NumerableFunction(
    inequalities: List<LinearInequality>,
    override val amount: ValueRange<UInt64>,
    constraint: Boolean = true,
    epsilon: Flt64 = Flt64(1e-6),
    parent: IntermediateSymbol? = null,
    args: Any? = null,
    name: String,
    displayName: String? = null
) : AbstractSatisfiedAmountInequalityFunction(
    inputs = inequalities.map { LinearConstraintInput.from(it) },
    constraint = constraint,
    epsilon = epsilon,
    parent = parent,
    args = args,
    name = name,
    displayName = displayName
), LogicFunctionSymbol {
    companion object {
        operator fun invoke(
            inequalities: List<ToLinearInequality>,
            amount: ValueRange<UInt64>,
            constraint: Boolean = true,
            epsilon: Flt64 = Flt64(1e-6),
            parent: IntermediateSymbol? = null,
            args: Any? = null,
            name: String,
            displayName: String? = null
        ): NumerableFunction {
            return NumerableFunction(
                inequalities = inequalities.map { it.toLinearInequality() },
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
