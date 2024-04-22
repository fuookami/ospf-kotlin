package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

sealed class AbstractSlackFunction<V : Variable<*>>(
    private val x: AbstractLinearPolynomial<*>,
    private val y: AbstractLinearPolynomial<*>,
    private val withNegative: Boolean = true,
    private val withPositive: Boolean = true,
    private val threshold: Boolean = false,
    private val constraint: Boolean = true,
    override var name: String,
    override var displayName: String? = null,
    private val ctor: (String) -> V
) : LinearFunctionSymbol {
    init {
        assert(withNegative || withPositive)
    }

    data class SlackPositive<V : Variable<*>>(
        val parent: AbstractSlackFunction<V>
    ) : LinearSymbol {
        override var name: String = "${parent.name}_pos"
        override var displayName: String? = parent.displayName?.let { "${it}_pos" }

        override val dependencies: Set<Symbol> = setOf(parent)
        override val cells: List<LinearMonomialCell> get() = listOf(LinearMonomialCell(parent._pos!!))
        override val cached: Boolean = true
        override val range: ExpressionRange<Flt64> get() = ExpressionRange(ValueRange(parent._pos!!.lowerBound, parent._pos!!.upperBound))
        override val lowerBound: Flt64 get() = parent._pos!!.lowerBound
        override val upperBound: Flt64 get() = parent._pos!!.upperBound

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
            val yValue = parent.y.value(tokenList, zeroIfNone) ?: return null
            return max(Flt64.zero, xValue - yValue)
        }

        override fun value(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
            val xValue = parent.x.value(results, tokenList, zeroIfNone) ?: return null
            val yValue = parent.y.value(results, tokenList, zeroIfNone) ?: return null
            return max(Flt64.zero, xValue - yValue)
        }

        override fun value(tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
            val xValue = parent.x.value(tokenTable, zeroIfNone) ?: return null
            val yValue = parent.y.value(tokenTable, zeroIfNone) ?: return null
            val value = max(Flt64.zero, xValue - yValue)
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
            val yValue = parent.y.value(results, tokenTable, zeroIfNone) ?: return null
            val value = max(Flt64.zero, xValue - yValue)
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
        val parent: AbstractSlackFunction<V>
    ) : LinearSymbol {
        override var name: String = "${parent.name}_neg"
        override var displayName: String? = parent.displayName?.let { "${it}_neg" }

        override val dependencies: Set<Symbol> = setOf(parent)
        override val cells: List<LinearMonomialCell> get() = listOf(LinearMonomialCell(parent._neg!!))
        override val cached: Boolean = true
        override val range: ExpressionRange<Flt64> get() = ExpressionRange(ValueRange(parent._neg!!.lowerBound, parent._neg!!.upperBound))
        override val lowerBound: Flt64 get() = parent._neg!!.lowerBound
        override val upperBound: Flt64 get() = parent._neg!!.upperBound

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
            val yValue = parent.y.value(tokenList, zeroIfNone) ?: return null
            return max(Flt64.zero, yValue - xValue)
        }

        override fun value(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
            val xValue = parent.x.value(results, tokenList, zeroIfNone) ?: return null
            val yValue = parent.y.value(results, tokenList, zeroIfNone) ?: return null
            return max(Flt64.zero, yValue - xValue)
        }

        override fun value(tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
            val xValue = parent.x.value(tokenTable, zeroIfNone) ?: return null
            val yValue = parent.y.value(tokenTable, zeroIfNone) ?: return null
            val value = max(Flt64.zero, yValue - xValue)
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
            val yValue = parent.y.value(results, tokenTable, zeroIfNone) ?: return null
            val value = max(Flt64.zero, yValue - xValue)
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
        val parent: AbstractSlackFunction<V>
    ) : LinearSymbol {
        override var name: String = "${parent.name}_x"
        override var displayName: String? = parent.displayName?.let { "${it}_x" }

        override val dependencies: Set<Symbol> = setOf(parent)
        override val cells: List<LinearMonomialCell> get() = parent._polyX.cells
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
            return parent.y.value(tokenList, zeroIfNone)
        }

        override fun value(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
            return parent.y.value(results, tokenList, zeroIfNone)
        }

        override fun value(tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
            val value = parent.y.value(tokenTable, zeroIfNone) ?: return null
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
            val value = parent.y.value(results, tokenTable, zeroIfNone) ?: return null
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

    private val _neg: V? by lazy {
        if (withNegative) {
            ctor("${name}_neg")
        } else {
            null
        }
    }
    val neg: SlackNegative<V>? by lazy {
        if (withNegative) {
            SlackNegative(this)
        } else {
            null
        }
    }

    private val _pos: V? by lazy {
        if (withPositive) {
            ctor("${name}_pos")
        } else {
            null
        }
    }
    val pos: SlackPositive<V>? by lazy {
        if (withPositive) {
            SlackPositive(this)
        } else {
            null
        }
    }

    private val _polyX: AbstractLinearPolynomial<*> by lazy {
        val polynomial = if (_neg != null && _pos != null) {
            x + _neg!! - _pos!!
        } else if (_neg != null) {
            x + _neg!!
        } else if (_pos != null) {
            x - _pos!!
        } else {
            x
        }
        polynomial.name = "${name}_x"
        polynomial
    }
    val polyX: SlackPolynomialX<V> by lazy {
        SlackPolynomialX(this)
    }

    private lateinit var polyY: AbstractLinearPolynomial<*>
    private val _range = ExpressionRange<Flt64>(possibleRange)

    override val range
        get() = if (::polyY.isInitialized) {
            polyY.range
        } else {
            _range
        }
    override val lowerBound
        get() = if (::polyY.isInitialized) {
            polyY.lowerBound
        } else {
            possibleRange.lowerBound.toFlt64()
        }
    override val upperBound
        get() = if (::polyY.isInitialized) {
            polyY.upperBound
        } else {
            possibleRange.upperBound.toFlt64()
        }

    override val category: Category = Linear

    override val dependencies: Set<Symbol>
        get() {
            val dependencies = HashSet<Symbol>()
            dependencies.addAll(x.dependencies)
            dependencies.addAll(y.dependencies)
            return dependencies
        }
    override val cells get() = polyY.cells
    override val cached
        get() = if (::polyY.isInitialized) {
            polyY.cached
        } else {
            false
        }

    private val possibleRange: ValueRange<Flt64>
        get() {
            return if (withNegative && withPositive) {
                val max = max(y.upperBound - x.lowerBound, x.upperBound - y.lowerBound)
                ValueRange(Flt64.zero, max)
            } else if (withNegative) {
                ValueRange(Flt64.zero, y.upperBound - x.lowerBound)
            } else if (withPositive) {
                ValueRange(Flt64.zero, x.upperBound - y.lowerBound)
            } else {
                ValueRange(Flt64.zero, Flt64.zero)
            }
        }

    override fun flush(force: Boolean) {
        if (::polyY.isInitialized) {
            polyY.flush(force)
            polyY.range.set(possibleRange)

            if (_neg != null && neg != null) {
                when (_neg) {
                    is UIntVar -> {
                        (_neg!! as UIntVar).range.set(ValueRange(neg!!.lowerBound.toUInt64(), neg!!.upperBound.toUInt64()))
                    }

                    is URealVar -> {
                        (_neg!! as URealVar).range.set(ValueRange(neg!!.lowerBound, neg!!.upperBound))
                    }
                }
            }

            if (_pos != null && pos != null) {
                when (_pos) {
                    is UIntVar -> {
                        (_pos!! as UIntVar).range.set(ValueRange(pos!!.lowerBound.toUInt64(), pos!!.upperBound.toUInt64()))
                    }

                    is URealVar -> {
                        (_pos!! as URealVar).range.set(ValueRange(pos!!.lowerBound, pos!!.upperBound))
                    }
                }
            }
        }
    }

    override suspend fun prepare(tokenTable: AbstractTokenTable) {
        x.cells
        y.cells
    }

    override fun register(tokenTable: MutableTokenTable): Try {
        if (_neg != null) {
            when (val result = tokenTable.add(_neg!!)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        if (_pos != null) {
            when (val result = tokenTable.add(_pos!!)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        if (!::polyY.isInitialized) {
            polyY = if (_pos != null && _neg != null) {
                _neg!! + pos!!
            } else if (_neg != null) {
                LinearPolynomial(_neg!!)
            } else if (_pos != null) {
                LinearPolynomial(_pos!!)
            } else {
                LinearPolynomial()
            }
            polyY.name = "${name}_y"
            polyY.range.set(_range.range)
        }

        return ok
    }

    override fun register(model: AbstractLinearMechanismModel): Try {
        if (constraint) {
            if (threshold) {
                if (withNegative) {
                    when (val result = model.addConstraint(
                        polyX geq y,
                        name
                    )) {
                        is Ok -> {}

                        is Failed -> {
                            return Failed(result.error)
                        }
                    }
                } else if (withPositive) {
                    when (val result = model.addConstraint(
                        polyX leq y,
                        name
                    )) {
                        is Ok -> {}

                        is Failed -> {
                            return Failed(result.error)
                        }
                    }
                }
            } else {
                when (val result = model.addConstraint(
                    polyX eq y,
                    name
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
        return "slack(${x.toRawString(unfold)}, ${y.toRawString(unfold)})"
    }

    override fun value(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        val xValue = x.value(tokenList, zeroIfNone) ?: return null
        val yValue = y.value(tokenList, zeroIfNone) ?: return null
        return if (withNegative && withPositive) {
            abs(xValue - yValue)
        } else if (withNegative) {
            max(Flt64.zero, yValue - xValue)
        } else if (withPositive) {
            max(Flt64.zero, xValue - yValue)
        } else {
            Flt64.zero
        }
    }

    override fun value(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        val xValue = x.value(results, tokenList, zeroIfNone) ?: return null
        val yValue = y.value(results, tokenList, zeroIfNone) ?: return null
        return if (withNegative && withPositive) {
            abs(xValue - yValue)
        } else if (withNegative) {
            max(Flt64.zero, yValue - xValue)
        } else if (withPositive) {
            max(Flt64.zero, xValue - yValue)
        } else {
            Flt64.zero
        }
    }

    override fun calculateValue(tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        val xValue = x.value(tokenTable, zeroIfNone) ?: return null
        val yValue = y.value(tokenTable, zeroIfNone) ?: return null
        return if (withNegative && withPositive) {
            abs(xValue - yValue)
        } else if (withNegative) {
            max(Flt64.zero, yValue - xValue)
        } else if (withPositive) {
            max(Flt64.zero, xValue - yValue)
        } else {
            Flt64.zero
        }
    }

    override fun calculateValue(results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        val xValue = x.value(results, tokenTable, zeroIfNone) ?: return null
        val yValue = y.value(results, tokenTable, zeroIfNone) ?: return null
        return if (withNegative && withPositive) {
            abs(xValue - yValue)
        } else if (withNegative) {
            max(Flt64.zero, yValue - xValue)
        } else if (withPositive) {
            max(Flt64.zero, xValue - yValue)
        } else {
            Flt64.zero
        }
    }
}

object SlackFunction {
    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: AbstractLinearPolynomial<*>,
        y: AbstractLinearPolynomial<*>,
        withNegative: Boolean = true,
        withPositive: Boolean = true,
        threshold: Boolean = false,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return if (type.isIntegerType) {
            UIntegerSlackFunction(x, y, withNegative, withPositive, threshold, constraint, name, displayName)
        } else {
            URealSlackFunction(x, y, withNegative, withPositive, threshold, constraint, name, displayName)
        }
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: AbstractLinearPolynomial<*>,
        threshold: AbstractLinearPolynomial<*>,
        withPositive: Boolean = true,
        constraint: Boolean = true,
        name: String,
        displayName: String? = null,
    ): AbstractSlackFunction<*> {
        return if (type.isIntegerType) {
            UIntegerSlackFunction(
                x,
                threshold,
                withNegative = !withPositive,
                withPositive = withPositive,
                threshold = true,
                constraint,
                name,
                displayName
            )
        } else {
            URealSlackFunction(
                x,
                threshold,
                withNegative = !withPositive,
                withPositive = withPositive,
                threshold = true,
                constraint,
                name,
                displayName
            )
        }
    }
}

class UIntegerSlackFunction(
    x: AbstractLinearPolynomial<*>,
    y: AbstractLinearPolynomial<*>,
    withNegative: Boolean = true,
    withPositive: Boolean = true,
    threshold: Boolean = false,
    constraint: Boolean = true,
    name: String,
    displayName: String? = null,
) : AbstractSlackFunction<UIntVar>(
    x,
    y,
    withNegative,
    withPositive,
    threshold,
    constraint,
    name,
    displayName,
    { UIntVar(it) }) {
    override val discrete = true
}

class URealSlackFunction(
    x: AbstractLinearPolynomial<*>,
    y: AbstractLinearPolynomial<*>,
    withNegative: Boolean = true,
    withPositive: Boolean = true,
    threshold: Boolean = false,
    constraint: Boolean = true,
    name: String,
    displayName: String? = null,
) : AbstractSlackFunction<URealVar>(
    x,
    y,
    withNegative,
    withPositive,
    threshold,
    constraint,
    name,
    displayName,
    { URealVar(it) })
