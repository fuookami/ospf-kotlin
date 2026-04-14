@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package fuookami.ospf.kotlin.core.intermediate_symbol.legacy.linear_function

import fuookami.ospf.kotlin.core.expression.polynomial.AbstractLinearPolynomial
import fuookami.ospf.kotlin.core.expression.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.intermediate_symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearLogicFunctionSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.prepareIfNotCached
import fuookami.ospf.kotlin.core.intermediate_model.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.intermediate_model.AbstractTokenTable
import fuookami.ospf.kotlin.core.intermediate_model.LinearConstraintInput
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.number.UInt8
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.multiarray.Shape1
import org.apache.logging.log4j.kotlin.logger

@Deprecated(
    message = "Use intermediate_symbol.function.IfFunction instead",
    replaceWith = ReplaceWith("IfFunction", "fuookami.ospf.kotlin.core.intermediate_symbol.function.IfFunction")
)
class IfFunction(
    input: LinearConstraintInput,
    private val epsilon: Flt64 = Flt64(1e-6),
    override val parent: IntermediateSymbol? = null,
    args: Any? = parent?.args,
    override var name: String,
    override var displayName: String? = null
) : LinearLogicFunctionSymbol() {
    private val logger = logger()

    companion object {
        operator fun invoke(
            condition: AbstractLinearPolynomial<*>,
            epsilon: Flt64 = Flt64(1e-6),
            parent: IntermediateSymbol? = null,
            args: Any? = null,
            name: String,
            displayName: String? = null
        ): IfFunction {
            return IfFunction(
                input = LinearConstraintInput.from(
                    condition.toMathLinearInequality(),
                    lhsRange = condition.range.valueRange!!
                ),
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

    private val input by lazy { input }

    private val k: PctVariable1 by lazy {
        PctVariable1("${name}_k", Shape1(3))
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
            // Dependencies are encoded in the flattenData monomials
            val dependencies = HashSet<IntermediateSymbol>()
            for (monomial in input.flattenData.monomials) {
                if (monomial.symbol is IntermediateSymbol) {
                    dependencies.add(monomial.symbol as IntermediateSymbol)
                }
            }
            return dependencies
        }
    override val cells get() = polyY.cells
    override val cached get() = polyY.cached

    private val possibleRange: ValueRange<Flt64>
        get() {
            val rhs = input.rhsConstant
            return when (input.sign) {
                fuookami.ospf.kotlin.math.symbol.inequality.Comparison.LT,
                fuookami.ospf.kotlin.math.symbol.inequality.Comparison.LE -> {
                    if (input.lhsRange.upperBound?.value?.unwrap()?.let { it leq rhs } == true) {
                        ValueRange(Flt64.one, Flt64.one).value!!
                    } else if (input.lhsRange.lowerBound?.value?.unwrap()?.let { it gr rhs } == true) {
                        ValueRange(Flt64.zero, Flt64.zero).value!!
                    } else {
                        ValueRange(Flt64.zero, Flt64.one).value!!
                    }
                }

                fuookami.ospf.kotlin.math.symbol.inequality.Comparison.GT,
                fuookami.ospf.kotlin.math.symbol.inequality.Comparison.GE -> {
                    if (input.lhsRange.lowerBound?.value?.unwrap()?.let { it geq rhs } == true) {
                        ValueRange(Flt64.one, Flt64.one).value!!
                    } else if (input.lhsRange.upperBound?.value?.unwrap()?.let { it ls rhs } == true) {
                        ValueRange(Flt64.zero, Flt64.zero).value!!
                    } else {
                        ValueRange(Flt64.zero, Flt64.one).value!!
                    }
                }

                fuookami.ospf.kotlin.math.symbol.inequality.Comparison.EQ -> {
                    if (input.lhsRange.fixedValue?.let { it eq rhs } == true) {
                        ValueRange(Flt64.one, Flt64.one).value!!
                    } else {
                        ValueRange(Flt64.zero, Flt64.zero).value!!
                    }
                }

                fuookami.ospf.kotlin.math.symbol.inequality.Comparison.NE -> {
                    if (input.lhsRange.fixedValue?.let { it neq rhs } == true) {
                        ValueRange(Flt64.one, Flt64.one).value!!
                    } else {
                        ValueRange(Flt64.zero, Flt64.zero).value!!
                    }
                }
            }
        }

    override fun flush(force: Boolean) {
        polyY.flush(force)
        polyY.range.set(possibleRange)
    }

    override fun prepare(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable): Flt64? {
        return prepareIfNotCached(values, tokenTable) {
            val bin = if (values.isNullOrEmpty()) {
                input.isTrue(tokenTable)
            } else {
                input.isTrue(values, tokenTable)
            } ?: return null

            val yValue = if (bin) {
                Flt64.one
            } else {
                Flt64.zero
            }

            logger.trace { "Setting IfFunction ${name}.y to $bin" }
            tokenTable.find(y)?.let { token ->
                token._result = yValue
            }

            yValue
        }
    }

    override fun register(tokenTable: AddableTokenCollection): Try {
        when (val result = input.register(
            parentName = name,
            k = k,
            flag = y,
            tokenTable = tokenTable
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        return ok
    }

    override fun register(model: AbstractLinearMechanismModel): Try {
        when (val result = input.register(
            parent = parent ?: this,
            parentName = name,
            k = k,
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
        when (val result = input.register(
            parent = parent ?: this,
            parentName = name,
            k = k,
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
            "if(${name})"
        }
    }

    override fun evaluate(
        tokenList: AbstractTokenList,
        zeroIfNone: Boolean
    ): Flt64? {
        return when (input.isTrue(tokenList, zeroIfNone)) {
            true -> Flt64.one
            false -> Flt64.zero
            null -> null
        }
    }

    override fun evaluate(
        results: List<Flt64>,
        tokenList: AbstractTokenList,
        zeroIfNone: Boolean
    ): Flt64? {
        return when (input.isTrue(results, tokenList, zeroIfNone)) {
            true -> Flt64.one
            false -> Flt64.zero
            null -> null
        }
    }

    override fun evaluate(
        values: Map<Symbol, Flt64>,
        tokenList: AbstractTokenList?,
        zeroIfNone: Boolean
    ): Flt64? {
        return when (input.isTrue(values, tokenList, zeroIfNone)) {
            true -> Flt64.one
            false -> Flt64.zero
            null -> null
        }
    }

    override fun calculateValue(
        tokenTable: AbstractTokenTable,
        zeroIfNone: Boolean
    ): Flt64? {
        return when (input.isTrue(tokenTable, zeroIfNone)) {
            true -> Flt64.one
            false -> Flt64.zero
            null -> null
        }
    }

    override fun calculateValue(
        results: List<Flt64>,
        tokenTable: AbstractTokenTable,
        zeroIfNone: Boolean
    ): Flt64? {
        return when (input.isTrue(results, tokenTable, zeroIfNone)) {
            true -> Flt64.one
            false -> Flt64.zero
            null -> null
        }
    }

    override fun calculateValue(
        values: Map<Symbol, Flt64>,
        tokenTable: AbstractTokenTable?,
        zeroIfNone: Boolean
    ): Flt64? {
        return when (input.isTrue(values, tokenTable, zeroIfNone)) {
            true -> Flt64.one
            false -> Flt64.zero
            null -> null
        }
    }
}





