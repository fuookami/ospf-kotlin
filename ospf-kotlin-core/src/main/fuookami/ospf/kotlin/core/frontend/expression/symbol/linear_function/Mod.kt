package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function

import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

class ModFunction(
    private val x: AbstractLinearPolynomial<*>,
    private val d: Flt64,
    override var name: String = "${x}_mod_${d}",
    override var displayName: String? = "$x mod $d"
) : LinearFunctionSymbol {
    private val logger = logger()

    private lateinit var q: IntVar
    private lateinit var r: URealVar
    private lateinit var y: AbstractLinearPolynomial<*>

    override val discrete: Boolean by lazy { x.discrete && (d.round() eq d) }

    override val range get() = y.range
    override val lowerBound
        get() = if (::y.isInitialized) {
            y.lowerBound
        } else {
            Flt64.zero
        }
    override val upperBound
        get() = if (::y.isInitialized) {
            y.upperBound
        } else {
            possibleUpperBound
        }

    override val category: Category = Linear

    override val dependencies by x::dependencies
    override val cells get() = y.cells
    override val cached
        get() = if (::y.isInitialized) {
            y.cached
        } else {
            false
        }

    private val possibleUpperBound
        get() = if (d geq Flt64.zero) {
            d.floor()
        } else {
            d.ceil().abs()
        }

    override fun flush(force: Boolean) {
        if (::y.isInitialized) {
            y.flush(force)
        }
    }

    override suspend fun prepare(tokenTable: AbstractTokenTable) {
        x.cells

        if (tokenTable.tokenList.tokens.any { it.result != null }) {
            x.value(tokenTable)?.let { xValue ->
                val qValue = (xValue / d).let {
                    if (it geq Flt64.zero) {
                        it.floor()
                    } else {
                        it.ceil()
                    }
                }
                logger.trace { "Setting ModFunction ${name}.q initial solution: $qValue" }
                tokenTable.find(q)?.let { token -> token._result = qValue }
                val rValue = xValue - qValue * d
                logger.trace { "Setting ModFunction ${name}.r initial solution: $rValue" }
                tokenTable.find(r)?.let { token -> token._result = rValue }

                when (tokenTable) {
                    is AutoAddTokenTable -> {
                        tokenTable.cachedSymbolValue[this to null] = rValue
                    }

                    is MutableTokenTable -> {
                        tokenTable.cachedSymbolValue[this to null] = rValue
                    }

                    else -> {}
                }
            }
        }
    }

    override fun register(tokenTable: MutableTokenTable): Try {
        if (!::q.isInitialized) {
            q = IntVar("${name}_q")
        }
        when (val result = tokenTable.add(q)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        if (!::r.isInitialized) {
            r = URealVar("${name}_r")
            r.range.leq(possibleUpperBound)
        }
        when (val result = tokenTable.add(r)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        if (!::y.isInitialized) {
            y = LinearPolynomial(r)
            y.name = "${name}_y"
            y.range.set(ValueRange(Flt64.zero, possibleUpperBound))
        }

        return ok
    }

    override fun register(model: AbstractLinearMechanismModel): Try {
        when (val result = model.addConstraint(
            x eq (d * q + r),
            name = name
        )) {
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

    override fun toRawString(unfold: Boolean): String {
        return "${x.toRawString(unfold)} mod $d"
    }

    override fun value(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return x.value(tokenList, zeroIfNone)?.let { it % d }
    }

    override fun value(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return x.value(results, tokenList, zeroIfNone)?.let { it % d }
    }

    override fun calculateValue(tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        return x.value(tokenTable, zeroIfNone)?.let { it % d }
    }

    override fun calculateValue(results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        return x.value(results, tokenTable, zeroIfNone)?.let { it % d }
    }
}
