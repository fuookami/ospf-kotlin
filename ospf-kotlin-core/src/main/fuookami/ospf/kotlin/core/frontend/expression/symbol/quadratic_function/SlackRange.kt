package fuookami.ospf.kotlin.core.frontend.expression.symbol.quadratic_function

import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.expression.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

sealed class AbstractSlackRangeFunction<V : Variable<*>>(
    private val x: AbstractQuadraticPolynomial<*>,
    private val lb: AbstractQuadraticPolynomial<*>,
    private val ub: AbstractQuadraticPolynomial<*>,
    private val constraint: Boolean = true,
    override var name: String,
    override var displayName: String? = null,
    private val ctor: (String) -> V
) : QuadraticFunctionSymbol {
    data class SlackPositive<V : Variable<*>>(
        val parent: AbstractSlackRangeFunction<V>
    ) : QuadraticSymbol {
        override var name: String = "${parent.name}_pos"
        override var displayName: String? = parent.displayName?.let { "${it}_pos" }

        override val dependencies: Set<Symbol> = setOf(parent)
        override val cells: List<QuadraticMonomialCell> get() = listOf(QuadraticMonomialCell(parent._pos, null))
        override val cached: Boolean = true
        override val range: ExpressionRange<Flt64> get() = ExpressionRange(ValueRange(parent._pos.lowerBound, parent._pos.upperBound))
        override val lowerBound: Flt64 get() = parent._pos.lowerBound
        override val upperBound: Flt64 get() = parent._pos.upperBound

        override val category: Category = Linear

        override fun flush(force: Boolean) {}
        override suspend fun prepare(tokenTable: AbstractTokenTable) {}

        override fun toString(): String {
            return displayName ?: name
        }

        override fun toRawString(unfold: Boolean): String {
            return "slack_pos(${parent.x.toRawString(unfold)}, ${parent.y.toRawString(unfold)})"
        }

        override fun value(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
            val xValue = parent.x.value(tokenList, zeroIfNone) ?: return null
            val ubValue = parent.ub.value(tokenList, zeroIfNone) ?: return null
            return max(Flt64.zero, xValue - ubValue)
        }

        override fun value(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
            val xValue = parent.x.value(results, tokenList, zeroIfNone) ?: return null
            val ubValue = parent.ub.value(results, tokenList, zeroIfNone) ?: return null
            return max(Flt64.zero, xValue - ubValue)
        }

        override fun value(tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
            val xValue = parent.x.value(tokenTable, zeroIfNone) ?: return null
            val ubValue = parent.ub.value(tokenTable, zeroIfNone) ?: return null
            val value = max(Flt64.zero, xValue - ubValue)
            when (tokenTable) {
                is ManualAddTokenTable -> {
                    tokenTable.cachedSymbolValue[this to null] = value
                }

                is AutoAddTokenTable -> {
                    tokenTable.cachedSymbolValue[this to null] = value
                }

                else -> {}
            }
            return value
        }

        override fun value(results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
            val xValue = parent.x.value(results, tokenTable, zeroIfNone) ?: return null
            val ubValue = parent.ub.value(results, tokenTable, zeroIfNone) ?: return null
            val value = max(Flt64.zero, xValue - ubValue)
            when (tokenTable) {
                is ManualAddTokenTable -> {
                    tokenTable.cachedSymbolValue[this to results] = value
                }

                is AutoAddTokenTable -> {
                    tokenTable.cachedSymbolValue[this to results] = value
                }

                else -> {}
            }
            return value
        }
    }

    data class SlackNegative<V : Variable<*>>(
        val parent: AbstractSlackRangeFunction<V>
    ) : QuadraticSymbol {
        override var name: String = "${parent.name}_neg"
        override var displayName: String? = parent.displayName?.let { "${it}_neg" }

        override val dependencies: Set<Symbol> = setOf(parent)
        override val cells: List<QuadraticMonomialCell> get() = listOf(QuadraticMonomialCell(parent._neg, null))
        override val cached: Boolean = true
        override val range: ExpressionRange<Flt64> get() = ExpressionRange(ValueRange(parent._neg.lowerBound, parent._neg.upperBound))
        override val lowerBound: Flt64 get() = parent._neg.lowerBound
        override val upperBound: Flt64 get() = parent._neg.upperBound

        override val category: Category = Linear

        override fun flush(force: Boolean) {}
        override suspend fun prepare(tokenTable: AbstractTokenTable) {}

        override fun toString(): String {
            return displayName ?: name
        }

        override fun toRawString(unfold: Boolean): String {
            return "slack_neg(${parent.x.toRawString(unfold)}, ${parent.y.toRawString(unfold)})"
        }

        override fun value(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
            val xValue = parent.x.value(tokenList, zeroIfNone) ?: return null
            val lbValue = parent.lb.value(tokenList, zeroIfNone) ?: return null
            return max(Flt64.zero, lbValue - xValue)
        }

        override fun value(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
            val xValue = parent.x.value(results, tokenList, zeroIfNone) ?: return null
            val lbValue = parent.lb.value(results, tokenList, zeroIfNone) ?: return null
            return max(Flt64.zero, lbValue - xValue)
        }

        override fun value(tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
            val xValue = parent.x.value(tokenTable, zeroIfNone) ?: return null
            val lbValue = parent.lb.value(tokenTable, zeroIfNone) ?: return null
            val value = max(Flt64.zero, lbValue - xValue)
            when (tokenTable) {
                is ManualAddTokenTable -> {
                    tokenTable.cachedSymbolValue[this to null] = value
                }

                is AutoAddTokenTable -> {
                    tokenTable.cachedSymbolValue[this to null] = value
                }

                else -> {}
            }
            return value
        }

        override fun value(results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
            val xValue = parent.x.value(results, tokenTable, zeroIfNone) ?: return null
            val lbValue = parent.lb.value(results, tokenTable, zeroIfNone) ?: return null
            val value = max(Flt64.zero, lbValue - xValue)
            when (tokenTable) {
                is ManualAddTokenTable -> {
                    tokenTable.cachedSymbolValue[this to results] = value
                }

                is AutoAddTokenTable -> {
                    tokenTable.cachedSymbolValue[this to results] = value
                }

                else -> {}
            }
            return value
        }
    }

    data class SlackPolynomialX<V : Variable<*>>(
        val parent: AbstractSlackRangeFunction<V>
    ) : QuadraticSymbol {
        override var name: String = "${parent.name}_x"
        override var displayName: String? = parent.displayName?.let { "${it}_x" }

        override val dependencies: Set<Symbol> = setOf(parent)
        override val cells: List<QuadraticMonomialCell> get() = parent._polyX.cells
        override val cached: Boolean get() = parent._polyX.cached
        override val range: ExpressionRange<Flt64> get() = parent._polyX.range
        override val lowerBound: Flt64 get() = parent._polyX.lowerBound
        override val upperBound: Flt64 get() = parent._polyX.upperBound

        override val category: Category = Linear

        override fun flush(force: Boolean) {}
        override suspend fun prepare(tokenTable: AbstractTokenTable) {}

        override fun toString(): String {
            return displayName ?: name
        }

        override fun toRawString(unfold: Boolean): String {
            return "slack_x(${parent.x.toRawString(unfold)}, ${parent.y.toRawString(unfold)})"
        }

        override fun value(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
            val xValue = parent.x.value(tokenList, zeroIfNone) ?: return null
            val ubValue = parent.ub.value(tokenList, zeroIfNone) ?: return null
            val lbValue = parent.lb.value(tokenList, zeroIfNone) ?: return null
            return if (xValue geq ubValue) {
                ubValue
            } else if (xValue leq lbValue) {
                lbValue
            } else {
                xValue
            }
        }

        override fun value(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
            val xValue = parent.x.value(results, tokenList, zeroIfNone) ?: return null
            val ubValue = parent.ub.value(results, tokenList, zeroIfNone) ?: return null
            val lbValue = parent.lb.value(results, tokenList, zeroIfNone) ?: return null
            return if (xValue geq ubValue) {
                ubValue
            } else if (xValue leq lbValue) {
                lbValue
            } else {
                xValue
            }
        }

        override fun value(tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
            val xValue = parent.x.value(tokenTable, zeroIfNone) ?: return null
            val ubValue = parent.ub.value(tokenTable, zeroIfNone) ?: return null
            val lbValue = parent.lb.value(tokenTable, zeroIfNone) ?: return null
            val value = if (xValue geq ubValue) {
                ubValue
            } else if (xValue leq lbValue) {
                lbValue
            } else {
                xValue
            }
            when (tokenTable) {
                is ManualAddTokenTable -> {
                    tokenTable.cachedSymbolValue[this to null] = value
                }

                is AutoAddTokenTable -> {
                    tokenTable.cachedSymbolValue[this to null] = value
                }

                else -> {}
            }
            return value
        }

        override fun value(results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
            val xValue = parent.x.value(results, tokenTable, zeroIfNone) ?: return null
            val ubValue = parent.ub.value(results, tokenTable, zeroIfNone) ?: return null
            val lbValue = parent.lb.value(results, tokenTable, zeroIfNone) ?: return null
            val value = if (xValue geq ubValue) {
                ubValue
            } else if (xValue leq lbValue) {
                lbValue
            } else {
                xValue
            }
            when (tokenTable) {
                is ManualAddTokenTable -> {
                    tokenTable.cachedSymbolValue[this to results] = value
                }

                is AutoAddTokenTable -> {
                    tokenTable.cachedSymbolValue[this to results] = value
                }

                else -> {}
            }
            return value
        }
    }

    private val _neg: V by lazy { ctor("${name}_neg") }
    val neg: SlackNegative<V> by lazy { SlackNegative(this) }

    private val _pos: V by lazy { ctor("${name}_pos") }
    val pos: SlackPositive<V> by lazy { SlackPositive(this) }

    private val _polyX: AbstractQuadraticPolynomial<*> by lazy {
        val polynomial = x + _neg - _pos
        polynomial.name = "${name}_x"
        polynomial
    }
    val polyX: SlackPolynomialX<V> by lazy {
        SlackPolynomialX(this)
    }

    private lateinit var y: AbstractQuadraticPolynomial<*>

    override val range get() = y.range
    override val lowerBound
        get() = if (::y.isInitialized) {
            y.lowerBound
        } else {
            possibleRange.lowerBound.toFlt64()
        }
    override val upperBound
        get() = if (::y.isInitialized) {
            y.upperBound
        } else {
            possibleRange.upperBound.toFlt64()
        }

    override val category: Category = Linear

    override val dependencies: Set<Symbol>
        get() {
            val dependencies = HashSet<Symbol>()
            dependencies.addAll(x.dependencies)
            dependencies.addAll(lb.dependencies)
            dependencies.addAll(ub.dependencies)
            return dependencies
        }
    override val cells get() = y.cells
    override val cached
        get() = if (::y.isInitialized) {
            y.cached
        } else {
            false
        }

    private val possibleRange: ValueRange<Flt64>
        get() {
            val max = max(x.upperBound - ub.lowerBound, lb.upperBound - x.lowerBound)
            return ValueRange(Flt64.zero, max)
        }

    override fun flush(force: Boolean) {
        if (::y.isInitialized) {
            y.flush(force)
            y.range.set(possibleRange)
        }
    }

    override suspend fun prepare(tokenTable: AbstractTokenTable) {
        x.cells
        lb.cells
        ub.cells

        if (tokenTable.tokenList.tokens.any { it.result != null }) {
            x.value(tokenTable)?.let { xValue ->
                lb.value(tokenTable)?.let { lbValue ->
                    val value = max(Flt64.zero, lbValue - xValue)
                    logger.info { "Setting SlackRangeFunction ${name}.neg initial solution: $value" }
                    tokenTable.find(_neg)?.let { it._result = value }
                }
                ub.value(tokenTable)?.let { ubValue ->
                    val value = max(Flt64.zero, xValue - ubValue)
                    logger.info { "Setting SlackRangeFunction ${name}.pos initial solution: $value" }
                    tokenTable.find(_pos)?.let { it._result = value }
                }
            }
        }
    }

    override fun register(tokenTable: MutableTokenTable): Try {
        when (val result = tokenTable.add(neg)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = tokenTable.add(pos)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        if (!::y.isInitialized) {
            y = QuadraticPolynomial(neg + pos)
            y.name = "${name}_y"
        }

        return ok
    }

    override fun register(model: AbstractQuadraticMechanismModel): Try {
        if (x.range.range.intersect(ValueRange(lb.lowerBound, ub.upperBound)).empty) {
            return Failed(
                Err(
                    ErrorCode.ApplicationFailed,
                    "$name's domain of definition unsatisfied: $x's domain is without intersection with $y's domain"
                )
            )
        }

        if (constraint) {
            when (val result = model.addConstraint(
                polyX leq ub,
                "${name}_ub"
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }

            when (val result = model.addConstraint(
                polyX geq lb,
                "${name}_lb"
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        return ok
    }

    override fun toString(): String {
        return displayName ?: name
    }

    override fun toRawString(unfold: Boolean): String {
        return "slack_range(${x.toRawString(unfold)}, [${lb.toRawString(unfold)}, ${ub.toRawString(unfold)}])"
    }

    override fun value(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        val xValue = x.value(tokenList, zeroIfNone) ?: return null
        val lbValue = lb.value(tokenList, zeroIfNone) ?: return null
        val ubValue = ub.value(tokenList, zeroIfNone) ?: return null
        return if (xValue ls lbValue) {
            lbValue - xValue
        } else if (xValue gr ubValue) {
            xValue - ubValue
        } else {
            Flt64.zero
        }
    }

    override fun value(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        val xValue = x.value(results, tokenList, zeroIfNone) ?: return null
        val lbValue = lb.value(results, tokenList, zeroIfNone) ?: return null
        val ubValue = ub.value(results, tokenList, zeroIfNone) ?: return null
        return if (xValue ls lbValue) {
            lbValue - xValue
        } else if (xValue gr ubValue) {
            xValue - ubValue
        } else {
            Flt64.zero
        }
    }

    override fun calculateValue(tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        val xValue = x.value(tokenTable, zeroIfNone) ?: return null
        val lbValue = lb.value(tokenTable, zeroIfNone) ?: return null
        val ubValue = ub.value(tokenTable, zeroIfNone) ?: return null
        return if (xValue ls lbValue) {
            lbValue - xValue
        } else if (xValue gr ubValue) {
            xValue - ubValue
        } else {
            Flt64.zero
        }
    }

    override fun calculateValue(results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        val xValue = x.value(results, tokenTable, zeroIfNone) ?: return null
        val lbValue = lb.value(results, tokenTable, zeroIfNone) ?: return null
        val ubValue = ub.value(results, tokenTable, zeroIfNone) ?: return null
        return if (xValue ls lbValue) {
            lbValue - xValue
        } else if (xValue gr ubValue) {
            xValue - ubValue
        } else {
            Flt64.zero
        }
    }
}

object SlackRangeFunction {
    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: AbstractQuadraticPolynomial<*>,
        lb: AbstractQuadraticPolynomial<*>,
        ub: AbstractQuadraticPolynomial<*>,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackRangeFunction<*> {
        return if (type.isIntegerType) {
            UIntegerSlackRangeFunction(x, lb, ub, constraint, name, displayName)
        } else {
            URealSlackRangeFunction(x, lb, ub, constraint, name, displayName)
        }
    }
}

class UIntegerSlackRangeFunction(
    x: AbstractQuadraticPolynomial<*>,
    lb: AbstractQuadraticPolynomial<*>,
    ub: AbstractQuadraticPolynomial<*>,
    constraint: Boolean = true,
    name: String,
    displayName: String? = null,
) : AbstractSlackRangeFunction<UIntVar>(x, lb, ub, constraint, name, displayName, { UIntVar(it) }) {
    override val discrete = true
}

class URealSlackRangeFunction(
    x: AbstractQuadraticPolynomial<*>,
    lb: AbstractQuadraticPolynomial<*>,
    ub: AbstractQuadraticPolynomial<*>,
    constraint: Boolean = true,
    name: String,
    displayName: String? = null,
) : AbstractSlackRangeFunction<URealVar>(x, lb, ub, constraint, name, displayName, { URealVar(it) })
