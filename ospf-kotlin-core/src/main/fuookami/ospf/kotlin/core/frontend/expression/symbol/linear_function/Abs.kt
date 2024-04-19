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

    private val possibleUpperBound get() = max(abs(x.lowerBound), abs(x.upperBound))
    private var m = possibleUpperBound

    override fun flush(force: Boolean) {
        if (::y.isInitialized) {
            y.flush(force)
            val newM = possibleUpperBound
            if (m neq newM) {
                y.range.set(ValueRange(-m, m))
                y.asMutable() *= m / newM
                m = newM
            }
        }
    }

    override suspend fun prepare() {
        x.cells
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
            y.range.set(ValueRange(Flt64.zero, m))
        }

        return ok
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

        return ok
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
