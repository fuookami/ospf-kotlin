package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function

import fuookami.ospf.kotlin.core.frontend.expression.polynomial.AbstractLinearPolynomial
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.sum
import fuookami.ospf.kotlin.core.frontend.expression.symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.frontend.expression.symbol.LinearFunctionSymbol
import fuookami.ospf.kotlin.core.frontend.expression.symbol.prepareIfNotCached
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.frontend.model.mechanism.AbstractTokenTable
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.math.algebra.number.UInt64
import fuookami.ospf.kotlin.utils.math.symbol.Linear
import fuookami.ospf.kotlin.utils.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.utils.multi_array.Shape1
import fuookami.ospf.kotlin.utils.multi_array.Shape2
import fuookami.ospf.kotlin.utils.multi_array._a
import org.apache.logging.log4j.kotlin.logger

class SameAsFunction(
    inequalities: List<LinearInequality>,
    private val constraint: Boolean = true,
    private val fixed: Boolean? = null,
    private val epsilon: Flt64 = Flt64(1e-6),
    override val parent: IntermediateSymbol? = null,
    args: Any? = null,
    override var name: String,
    override var displayName: String? = null
) : LinearFunctionSymbol() {
    private val logger = logger()

    companion object {
        operator fun invoke(
            inequalities: List<ToLinearInequality>,
            constraint: Boolean = true,
            fixed: Boolean? = null,
            epsilon: Flt64 = Flt64(1e-6),
            parent: IntermediateSymbol? = null,
            args: Any? = null,
            name: String,
            displayName: String? = null
        ): SameAsFunction {
            return SameAsFunction(
                inequalities = inequalities.map { it.toLinearInequality() },
                constraint = constraint,
                fixed = fixed,
                epsilon = epsilon,
                parent = parent,
                args = args,
                name = name,
                displayName = displayName
            )
        }
    }

    internal val _args = args
    override val args get() = _args ?: parent?.args

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
        if (fixed != null) {
            y.range.eq(fixed)
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

    override fun prepare(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable): Flt64? {
        for (inequality in inequalities) {
            inequality.lhs.cells
            inequality.rhs.cells
        }

        return prepareIfNotCached(values, tokenTable) {
            val evaluatedValues = inequalities.map {
                if (values.isNullOrEmpty()) {
                    it.isTrue(tokenTable)
                } else {
                    it.isTrue(values, tokenTable)
                } ?: return null
            }
            if (!constraint && inequalities.size > 1) {
                for (i in inequalities.indices) {
                    logger.trace { "Setting SameAsFunction ${name}.u[$i] to ${evaluatedValues[i]}" }
                    tokenTable.find(u[i])?.let { token ->
                        token._result = if (evaluatedValues[i]) {
                            Flt64.one
                        } else {
                            Flt64.zero
                        }
                    }
                }
            }

            val bin = evaluatedValues.all { it } || evaluatedValues.all { !it }
            val yValue = if (bin) {
                Flt64.one
            } else {
                Flt64.zero
            }
            logger.trace { "Setting SameAsFunction ${name}.y to $bin" }
            tokenTable.find(y)?.let { token ->
                token._result = yValue
            }

            yValue
        }
    }

    override fun register(tokenTable: AddableTokenCollection): Try {
        if (!constraint && inequalities.size > 1) {
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

                    is Fatal -> {
                        return Fatal(result.errors)
                    }

                    is Fatal -> {
                        return Fatal(result.errors)
                    }
                }
            }
        } else {
            for ((i, inequality) in inequalities.withIndex()) {
                when (val result = inequality.register(
                    parentName = "${name}_i",
                    k = k[i, _a],
                    flag = null,
                    tokenTable = tokenTable
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
        }

        return ok
    }

    override fun register(model: AbstractLinearMechanismModel): Try {
        if (!constraint && inequalities.size > 1) {
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

                    is Fatal -> {
                        return Fatal(result.errors)
                    }

                    is Fatal -> {
                        return Fatal(result.errors)
                    }
                }
            }

            when (val result = model.addConstraint(
                constraint = y geq (sum(u) - UInt64(inequalities.size) + UInt64.one) / UInt64(inequalities.size),
                name = "${name}_ub",
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
                constraint = y leq sum(u) / UInt64(inequalities.size),
                name = "${name}_lb",
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
            for ((i, inequality) in inequalities.withIndex()) {
                when (val result = inequality.register(
                    parent = parent ?: this,
                    parentName = name,
                    k = k[i, _a],
                    flag = y,
                    epsilon = epsilon,
                    model = model
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
        model: AbstractLinearMechanismModel,
        fixedValues: Map<Symbol, Flt64>
    ): Try {
        if (!constraint && inequalities.size > 1) {
            val values = inequalities.map {
                it.isTrue(fixedValues, model.tokens) ?: return register(model)
            }
            val bin = if (values.all { it }) {
                Flt64.one
            } else {
                Flt64.zero
            }

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

                    is Fatal -> {
                        return Fatal(result.errors)
                    }

                    is Fatal -> {
                        return Fatal(result.errors)
                    }
                }
            }

            when (val result = model.addConstraint(
                y geq (sum(u) - UInt64(inequalities.size) + UInt64.one) / UInt64(inequalities.size),
                name = "${name}_ub",
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
                y leq sum(u) / UInt64(inequalities.size),
                name = "${name}_lb",
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
                y eq bin,
                name = "${name}_y",
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

            model.tokens.find(y)?.let { token ->
                token._result = bin
            }
        } else {
            for ((i, inequality) in inequalities.withIndex()) {
                when (val result = inequality.register(
                    parent = parent ?: this,
                    parentName = name,
                    k = k[i, _a],
                    flag = y,
                    epsilon = epsilon,
                    model = model,
                    fixedValues = fixedValues
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

    override fun evaluate(
        tokenList: AbstractTokenList,
        zeroIfNone: Boolean
    ): Flt64? {
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

    override fun evaluate(
        results: List<Flt64>,
        tokenList: AbstractTokenList,
        zeroIfNone: Boolean
    ): Flt64? {
        var lastValue: Boolean? = null
        for (inequality in inequalities) {
            val value = inequality.isTrue(
                results = results,
                tokenList = tokenList,
                zeroIfNone = zeroIfNone
            ) ?: return null
            if (lastValue == null) {
                lastValue = value
            } else if (lastValue != value) {
                return Flt64.zero
            }
        }
        return Flt64.one
    }

    override fun evaluate(
        values: Map<Symbol, Flt64>,
        tokenList: AbstractTokenList?,
        zeroIfNone: Boolean
    ): Flt64? {
        var lastValue: Boolean? = null
        for (inequality in inequalities) {
            val value = inequality.isTrue(
                values = values,
                tokenList = tokenList,
                zeroIfNone = zeroIfNone
            ) ?: return null
            if (lastValue == null) {
                lastValue = value
            } else if (lastValue != value) {
                return Flt64.zero
            }
        }
        return Flt64.one
    }

    override fun calculateValue(
        tokenTable: AbstractTokenTable,
        zeroIfNone: Boolean
    ): Flt64? {
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

    override fun calculateValue(
        results: List<Flt64>,
        tokenTable: AbstractTokenTable,
        zeroIfNone: Boolean
    ): Flt64? {
        var lastValue: Boolean? = null
        for (inequality in inequalities) {
            val value = inequality.isTrue(
                results = results,
                tokenTable = tokenTable,
                zeroIfNone = zeroIfNone
            ) ?: return null
            if (lastValue == null) {
                lastValue = value
            } else if (lastValue != value) {
                return Flt64.zero
            }
        }
        return Flt64.one
    }

    override fun calculateValue(
        values: Map<Symbol, Flt64>,
        tokenTable: AbstractTokenTable?,
        zeroIfNone: Boolean
    ): Flt64? {
        var lastValue: Boolean? = null
        for (inequality in inequalities) {
            val value = inequality.isTrue(
                values = values,
                tokenTable = tokenTable,
                zeroIfNone = zeroIfNone
            ) ?: return null
            if (lastValue == null) {
                lastValue = value
            } else if (lastValue != value) {
                return Flt64.zero
            }
        }
        return Flt64.one
    }
}





