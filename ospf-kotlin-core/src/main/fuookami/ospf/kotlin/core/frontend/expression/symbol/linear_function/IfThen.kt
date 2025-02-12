package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function

import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

class IfThenFunction(
    p: LinearInequality,
    q: LinearInequality,
    private val constraint: Boolean = true,
    override var name: String,
    override var displayName: String? = null
) : LinearFunctionSymbol {
    private val p by lazy { p.normalize() }
    private val q by lazy { q.normalize() }

    private val pu: BinVar by lazy {
        BinVar(p.name.ifEmpty { "${name}_pu" })
    }

    private val qu: BinVar by lazy {
        BinVar(q.name.ifEmpty { "${name}_qu" })
    }

    private val u: UIntVar by lazy {
        UIntVar("${name}_u")
    }

    private val y: BinaryzationFunction by lazy {
        BinaryzationFunction(
            x = LinearPolynomial(Flt64.two * u),
            name = "${name}_y"
        )
    }

    private val polyY: AbstractLinearPolynomial<*> by lazy {
        if (!constraint) {
            LinearPolynomial(y)
        } else {
            LinearPolynomial(1)
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
            dependencies.addAll(p.lhs.dependencies)
            dependencies.addAll(p.rhs.dependencies)
            dependencies.addAll(q.lhs.dependencies)
            dependencies.addAll(q.rhs.dependencies)
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
        p.flush(force)
        q.flush(force)
        if (!constraint) {
            y.flush(force)
        }
        polyY.flush(force)
        polyY.range.set(possibleRange)
    }

    override fun prepare(tokenTable: AbstractTokenTable) {
        p.lhs.cells
        p.rhs.cells
        q.lhs.cells
        q.rhs.cells
        y.prepare(tokenTable)

        if (!constraint && tokenTable.cachedSolution && tokenTable.cached(this) == false) {
            val pBin = p.isTrue(tokenTable) ?: return
            val qBin = q.isTrue(tokenTable) ?: return

            logger.trace { "Setting IfThenFunction ${name}.pu initial solution: $pBin" }
            tokenTable.find(pu)?.let { token ->
                token._result = if (pBin) {
                    Flt64.one
                } else {
                    Flt64.zero
                }
            }

            logger.trace { "Setting IfThenFunction ${name}.qu initial solution: $qBin" }
            tokenTable.find(qu)?.let { token ->
                token._result = if (qBin) {
                    Flt64.one
                } else {
                    Flt64.zero
                }
            }

            val uValue = UInt8(qBin).toFlt64() - UInt8(pBin).toFlt64() + Flt64.one
            logger.trace { "Setting IfThenFunction ${name}.u initial solution: $uValue" }
            tokenTable.find(u)?.let { token ->
                token._result = uValue
            }

            val bin = uValue neq Flt64.zero
            val yValue = if (bin) {
                Flt64.one
            } else {
                Flt64.zero
            }

            tokenTable.cache(this, null, yValue)
        }
    }

    override fun toString(): String {
        return displayName ?: name
    }

    override fun register(tokenTable: AbstractMutableTokenTable): Try {
        when (val result = tokenTable.add(pu)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = tokenTable.add(qu)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        if (!constraint) {
            when (val result = tokenTable.add(u)) {
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
        }

        return ok
    }

    override fun register(model: AbstractLinearMechanismModel): Try {
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
            when (val result = model.addConstraint(
                pu leq qu,
                "${name}_u"
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        } else {
            when (val result = model.addConstraint(
                u eq (qu - pu + Flt64.one),
                "${name}_u"
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        return ok
    }

    override fun toRawString(unfold: Boolean): String {
        return "if_then(${p.toRawString(unfold)}, ${q.toRawString(unfold)})"
    }

    override fun evaluate(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        val pv = p.isTrue(tokenList, zeroIfNone) ?: return null
        val qv = q.isTrue(tokenList, zeroIfNone) ?: return null
        return if (UInt8(qv) geq UInt8(pv)) {
            Flt64.one
        } else {
            Flt64.zero
        }
    }

    override fun evaluate(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        val pv = p.isTrue(results, tokenList, zeroIfNone) ?: return null
        val qv = q.isTrue(results, tokenList, zeroIfNone) ?: return null
        return if (UInt8(qv) geq UInt8(pv)) {
            Flt64.one
        } else {
            Flt64.zero
        }
    }

    override fun calculateValue(tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        val pv = p.isTrue(tokenTable, zeroIfNone) ?: return null
        val qv = q.isTrue(tokenTable, zeroIfNone) ?: return null
        return if (UInt8(qv) geq UInt8(pv)) {
            Flt64.one
        } else {
            Flt64.zero
        }
    }

    override fun calculateValue(results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        val pv = p.isTrue(results, tokenTable, zeroIfNone) ?: return null
        val qv = q.isTrue(results, tokenTable, zeroIfNone) ?: return null
        return if (UInt8(qv) geq UInt8(pv)) {
            Flt64.one
        } else {
            Flt64.zero
        }
    }
}
