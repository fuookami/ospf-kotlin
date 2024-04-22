package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

class IfInFunction(
    private val x: AbstractLinearPolynomial<*>,
    lowerBound: AbstractLinearPolynomial<*>,
    upperBound: AbstractLinearPolynomial<*>,
    override var name: String,
    override var displayName: String? = null
) : LinearLogicFunctionSymbol {
    private val lb = lowerBound
    private val ub = upperBound

    private val inequalities: List<LinearInequality>

    init {
        val lowerBoundInequality = (x geq lb).normalize()
        lowerBoundInequality.name = "${name}_lb"
        val upperBoundInequality = (x leq ub).normalize()
        upperBoundInequality.name = "${name}_ub"
        inequalities = listOf(lowerBoundInequality, upperBoundInequality)
    }

    private lateinit var y: BinVar
    private lateinit var polyY: AbstractLinearPolynomial<*>

    override val discrete = true

    override val range get() = polyY.range
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
            for (inequality in inequalities) {
                dependencies.addAll(inequality.lhs.dependencies)
                dependencies.addAll(inequality.rhs.dependencies)
            }
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
            // todo: impl by Inequality.judge()
            return ValueRange(Flt64.zero, Flt64.one)
        }

    override fun flush(force: Boolean) {
        if (::polyY.isInitialized) {
            polyY.flush(force)
            polyY.range.set(possibleRange)
        }
    }

    override suspend fun prepare(tokenTable: AbstractTokenTable) {
        x.cells
        for (inequality in inequalities) {
            inequality.lhs.cells
            inequality.rhs.cells
        }
    }

    override fun register(tokenTable: MutableTokenTable): Try {
        if (!::y.isInitialized) {
            y = BinVar("${name}_y")
        }
        when (val result = tokenTable.add(y)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        if (!::polyY.isInitialized) {
            polyY = LinearPolynomial(y)
            polyY.range.set(possibleRange)
        }

        return ok
    }

    override fun register(model: AbstractLinearMechanismModel): Try {
        for (inequality in inequalities) {
            when (val result = inequality.register(name, y, model)) {
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
        return "if_in(${x.toRawString(unfold)}, [${lb.toRawString(unfold)}, ${ub.toRawString(unfold)}])"
    }

    override fun value(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        val xValue = x.value(tokenList, zeroIfNone) ?: return null
        val lbValue = lb.value(tokenList, zeroIfNone) ?: return null
        val ubValue = ub.value(tokenList, zeroIfNone) ?: return null
        return if (lbValue leq xValue && xValue leq ubValue) {
            Flt64.one
        } else {
            Flt64.zero
        }
    }

    override fun value(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        val xValue = x.value(results, tokenList, zeroIfNone) ?: return null
        val lbValue = lb.value(results, tokenList, zeroIfNone) ?: return null
        val ubValue = ub.value(results, tokenList, zeroIfNone) ?: return null
        return if (lbValue leq xValue && xValue leq ubValue) {
            Flt64.one
        } else {
            Flt64.zero
        }
    }

    override fun calculateValue(tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        val xValue = x.value(tokenTable, zeroIfNone) ?: return null
        val lbValue = lb.value(tokenTable, zeroIfNone) ?: return null
        val ubValue = ub.value(tokenTable, zeroIfNone) ?: return null
        return if (lbValue leq xValue && xValue leq ubValue) {
            Flt64.one
        } else {
            Flt64.zero
        }
    }

    override fun calculateValue(results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        val xValue = x.value(results, tokenTable, zeroIfNone) ?: return null
        val lbValue = lb.value(results, tokenTable, zeroIfNone) ?: return null
        val ubValue = ub.value(results, tokenTable, zeroIfNone) ?: return null
        return if (lbValue leq xValue && xValue leq ubValue) {
            Flt64.one
        } else {
            Flt64.zero
        }
    }
}
