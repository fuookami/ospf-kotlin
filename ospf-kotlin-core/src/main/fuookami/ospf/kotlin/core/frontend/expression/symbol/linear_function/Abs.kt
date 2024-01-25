package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

class AbsFunction(
    private val x: AbstractLinearPolynomial<*>,
    private val extract: Boolean = true,
    override var name: String = "${x}_abs",
    override var displayName: String? = "|$x|"
) : LinearFunctionSymbol {
    private lateinit var neg: PctVar
    private lateinit var pos: PctVar
    private lateinit var p: BinVar

    private lateinit var y: MutableLinearPolynomial

    override val range get() = y.range
    override val lowerBound get() = y.lowerBound
    override val upperBound get() = y.upperBound

    override val cells get() = y.cells
    override val cached get() = y.cached

    private val possibleUpperBound get() = max(abs(x.lowerBound), abs(x.upperBound))
    private var m = possibleUpperBound

    override fun flush(force: Boolean) {
        y.flush(force)
        val newM = possibleUpperBound
        if (m neq newM) {
            y.range.set(ValueRange(-m, m, Flt64))
            y.asMutable() *= m / newM
            m = newM
        }
    }

    override fun register(tokenTable: LinearMutableTokenTable): Try {
        if (!::neg.isInitialized) {
            neg = PctVar("${name}_neg")
        }
        when (val result = tokenTable.add(neg)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        if (!::pos.isInitialized) {
            pos = PctVar("${name}_pos")
        }
        when (val result = tokenTable.add(pos)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        if (extract) {
            if (!::p.isInitialized) {
                p = BinVar("${name}_p")
            }
            when (val result = tokenTable.add(p)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        if (!::y.isInitialized) {
            y = (m * pos + m * neg).toMutable()
            y.name = "${name}_abs_y"
            y.range.set(ValueRange(Flt64.zero, m, Flt64))
        }

        return Ok(success)
    }

    override fun register(model: AbstractLinearModel): Try {
        model.addConstraint(
            x eq (-m * neg + m * pos),
            name
        )

        if (extract) {
            model.addConstraint(
                neg + pos leq Flt64.one,
                "${name}_b"
            )

            model.addConstraint(
                neg leq Flt64.one - p,
                "${name}_n"
            )
        }

        return Ok(success)
    }

    override fun toString(): String {
        return displayName ?: name
    }

    override fun toRawString(unfold: Boolean): String {
        return "|${x.toRawString(unfold)}|"
    }

    override fun value(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return x.value(tokenList, zeroIfNone)?.let { abs(it) }
    }

    override fun value(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return x.value(results, tokenList, zeroIfNone)?.let { abs(it) }
    }

}
