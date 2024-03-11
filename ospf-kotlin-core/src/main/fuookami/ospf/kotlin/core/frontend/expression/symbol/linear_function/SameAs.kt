package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

class SameAsFunction(
    inequalities: List<LinearInequality>,
    private val constraint: Boolean = true,
    private val fixedValue: Boolean? = null,
    override var name: String,
    override var displayName: String? = null
) : LinearFunctionSymbol {
    private val inequalities by lazy { inequalities.map { it.normalize() } }

    private lateinit var u: BinVariable1
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

    override val dependencies: Set<Symbol<*, *>>
        get() {
            val dependencies = HashSet<Symbol<*, *>>()
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

    override suspend fun prepare() {
        for (inequality in inequalities) {
            inequality.lhs.cells
            inequality.rhs.cells
        }
    }

    override fun register(tokenTable: MutableTokenTable<LinearMonomialCell, Linear>): Try {
        if (!constraint) {
            if (!::u.isInitialized) {
                u = BinVariable1("${name}_u", Shape1(inequalities.size))
            }
            when (val result = tokenTable.add(u)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        if (!::y.isInitialized) {
            y = BinVar("${name}_y")
        }
        if (fixedValue != null) {
            y.range.eq(fixedValue)
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

        return Ok(success)
    }

    override fun register(model: AbstractLinearModel): Try {
        if (::u.isInitialized) {
            for ((i, inequality) in inequalities.withIndex()) {
                when (val result = inequality.register(name, u[i], model)) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }
                }
            }

            model.addConstraint(
                y geq (sum(u) - UInt64(inequalities.size) + UInt64.one) / UInt64(inequalities.size),
                "${name}_ub"
            )

            model.addConstraint(
                y leq sum(u) / UInt64(inequalities.size),
                "${name}_lb"
            )
        } else {
            for (inequality in inequalities) {
                when (val result = inequality.register(name, y, model)) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }
                }
            }
        }

        return Ok(success)
    }

    override fun toString(): String {
        return displayName ?: name
    }

    override fun toRawString(unfold: Boolean): String {
        return "sane_as(${inequalities.joinToString(", ") { it.toRawString(unfold) }})"
    }

    override fun value(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        var counter = UInt64.zero
        for ((i, inequality) in inequalities.withIndex()) {
            if (inequality.isTrue(tokenList, zeroIfNone) ?: return null) {
                if (UInt64(i) != counter) {
                    return Flt64.zero
                } else {
                    counter += UInt64.one
                }
            } else if (counter != UInt64.zero) {
                return Flt64.zero
            }
        }
        return Flt64.one
    }

    override fun value(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        var counter = UInt64.zero
        for ((i, inequality) in inequalities.withIndex()) {
            if (inequality.isTrue(results, tokenList, zeroIfNone) ?: return null) {
                if (UInt64(i) != counter) {
                    return Flt64.zero
                } else {
                    counter += UInt64.one
                }
            } else if (counter != UInt64.zero) {
                return Flt64.zero
            }
        }
        return Flt64.one
    }
}
