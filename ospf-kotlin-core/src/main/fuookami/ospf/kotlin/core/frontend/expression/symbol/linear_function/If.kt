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
import fuookami.ospf.kotlin.core.frontend.inequality.Sign
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
            val dependencies = HashSet<IntermediateSymbol>()
            dependencies.addAll(inequality.lhs.dependencies)
            dependencies.addAll(inequality.rhs.dependencies)
            return dependencies
        }
    override val cells get() = polyY.cells
    override val cached get() = polyY.cached

    private val possibleRange: ValueRange<Flt64>
        get() {
            // inequality is normalized, so rhs is constant
            val rhs = inequality.rhs.range.fixedValue!!
            return when (inequality.sign) {
                Sign.Less, Sign.LessEqual -> {
                    if (inequality.lhs.range.upperBound?.value?.unwrap()?.let { it leq rhs } == true) {
                        ValueRange(Flt64.one, Flt64.one).value!!
                    } else if (inequality.lhs.range.lowerBound?.value?.unwrap()?.let { it gr rhs } == true) {
                        ValueRange(Flt64.zero, Flt64.zero).value!!
                    } else {
                        ValueRange(Flt64.zero, Flt64.one).value!!
                    }
                }

                Sign.Greater, Sign.GreaterEqual -> {
                    if (inequality.lhs.range.lowerBound?.value?.unwrap()?.let { it geq rhs } == true) {
                        ValueRange(Flt64.one, Flt64.one).value!!
                    } else if (inequality.lhs.range.upperBound?.value?.unwrap()?.let { it ls rhs } == true) {
                        ValueRange(Flt64.zero, Flt64.zero).value!!
                    } else {
                        ValueRange(Flt64.zero, Flt64.one).value!!
                    }
                }

                Sign.Equal -> {
                    if (inequality.lhs.range.fixedValue?.let { it eq rhs } == true) {
                        ValueRange(Flt64.one, Flt64.one).value!!
                    } else {
                        ValueRange(Flt64.zero, Flt64.zero).value!!
                    }
                }

                Sign.Unequal -> {
                    if (inequality.lhs.range.fixedValue?.let { it neq rhs } == true) {
                        ValueRange(Flt64.one, Flt64.one).value!!
                    } else {
                        ValueRange(Flt64.zero, Flt64.zero).value!!
                    }
                }
            }
        }

    override fun flush(force: Boolean) {
        inequality.flush(force)
        val range = possibleRange
        if (range.fixedValue?.let { it eq Flt64.zero } == true) {
            y.range.eq(false)
        } else if (range.fixedValue?.let { it eq Flt64.one } == true) {
            y.range.eq(true)
        } else {
            y.range.set(ValueRange(UInt8.zero, UInt8.one).value!!)
        }
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
        when (val result = tokenTable.add(k)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
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
        when (val result = inequality.register(name, k, y, model)) {
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

    override fun toRawString(unfold: UInt64): String {
        return if (unfold eq UInt64.zero) {
            displayName ?: name
        } else {
            "if(${inequality.toRawString(unfold - UInt64.one)})"
        }
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
