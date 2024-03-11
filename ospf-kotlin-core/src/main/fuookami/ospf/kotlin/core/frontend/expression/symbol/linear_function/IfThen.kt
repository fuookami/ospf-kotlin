package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

class IfThen(
    p: LinearInequality,
    q: LinearInequality,
    private val constraint: Boolean = true,
    override var name: String,
    override var displayName: String? = null
) : LinearFunctionSymbol {
    private val p by lazy { p.normalize() }
    private val q by lazy { q.normalize() }

    private lateinit var pu: BinVar
    private lateinit var qu: BinVar
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
            dependencies.addAll(p.lhs.dependencies)
            dependencies.addAll(p.rhs.dependencies)
            dependencies.addAll(q.lhs.dependencies)
            dependencies.addAll(q.rhs.dependencies)
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
        p.lhs.cells
        p.rhs.cells
        q.lhs.cells
        q.rhs.cells
    }

    override fun toString(): String {
        return displayName ?: name
    }

    override fun register(tokenTable: MutableTokenTable<LinearMonomialCell, Linear>): Try {
        if (!::pu.isInitialized) {
            pu = BinVar(p.name.ifEmpty { "${name}_pu" })
        }
        when (val result = tokenTable.add(pu)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        if (!::qu.isInitialized) {
            qu = BinVar(q.name.ifEmpty { "${name}_qu" })
        }
        when (val result = tokenTable.add(qu)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        if (!constraint) {
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
            }
        } else {
            if (!::polyY.isInitialized) {
                polyY = LinearPolynomial(1)
            }
        }

        return Ok(success)
    }

    override fun register(model: AbstractLinearModel): Try {
        when (val result = p.register(name, pu, model)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = q.register(name, qu, model)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        if (constraint) {
            model.addConstraint(
                pu leq qu,
                "${name}_u"
            )
        } else {
            model.addConstraint(
                pu leq qu + (Flt64.one - y),
                "${name}_u"
            )
        }

        return Ok(success)
    }

    override fun toRawString(unfold: Boolean): String {
        return "if_then(${p.toRawString(unfold)}, ${q.toRawString(unfold)})"
    }

    override fun value(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        val pv = p.isTrue(tokenList, zeroIfNone) ?: return null
        val qv = q.isTrue(tokenList, zeroIfNone) ?: return null
        return if (UInt8(qv) geq UInt8(pv)) {
            Flt64.one
        } else {
            Flt64.zero
        }
    }

    override fun value(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        val pv = p.isTrue(results, tokenList, zeroIfNone) ?: return null
        val qv = q.isTrue(results, tokenList, zeroIfNone) ?: return null
        return if (UInt8(qv) geq UInt8(pv)) {
            Flt64.one
        } else {
            Flt64.zero
        }
    }
}
