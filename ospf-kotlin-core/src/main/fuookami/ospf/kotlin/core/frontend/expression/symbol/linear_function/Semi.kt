package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

sealed class AbstractSemiFunction<V : Variable<*>>(
    private val polynomial: AbstractLinearPolynomial<*>,
    private val flag: AbstractLinearPolynomial<*>?,
    override var name: String,
    override var displayName: String? = null,
    private val ctor: (String) -> V
) : LinearFunctionSymbol {
    private lateinit var y: V
    private lateinit var u: BinVar
    private lateinit var polyY: LinearPolynomial

    override val range get() = polyY.range
    override val lowerBound get() = polyY.lowerBound
    override val upperBound get() = polyY.upperBound

    override val cells get() = polyY.cells
    override val cached get() = polyY.cached

    private val possibleRange
        get() = ValueRange(
            (flag?.lowerBound ?: u.lowerBound) * polynomial.lowerBound,
            (flag?.upperBound ?: u.upperBound) * polynomial.upperBound,
            Flt64
        )

    override fun flush(force: Boolean) {
        polyY.flush(force)
        polyY.range.set(possibleRange)
    }

    override fun register(tokenTable: LinearMutableTokenTable): Try {
        if (!::y.isInitialized) {
            y = ctor("${name}_y")
        }
        when (val result = tokenTable.add(y)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        if (flag == null) {
            if (!::u.isInitialized) {
                u = BinVar("${name}_u")
            }
            when (val result = tokenTable.add(u)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        if (!::polyY.isInitialized) {
            polyY = LinearPolynomial(y)
            polyY.name = "${name}_y"
            polyY.range.set(possibleRange)
        }

        return Ok(success)
    }

    override fun register(model: AbstractLinearModel): Try {
        if (polynomial.lowerBound ls Flt64.zero) {
            return Failed(Err(ErrorCode.ApplicationFailed, "$name's domain of definition unsatisfied: $polynomial"))
        }

        if (flag != null) {
            if (flag.lowerBound ls Flt64.zero || flag.upperBound gr Flt64.one) {
                return Failed(Err(ErrorCode.ApplicationFailed, "$name's domain of definition unsatisfied: $flag"))
            }
        }

        model.addConstraint(
            y leq polynomial,
            "${name}_x"
        )

        if (flag != null) {
            model.addConstraint(
                y geq (polynomial - polynomial.upperBound * (Flt64.one - flag)),
                "${name}_xu"
            )
            model.addConstraint(
                y geq (polynomial.lowerBound * flag),
                "${name}_lb"
            )
            model.addConstraint(
                y leq (polynomial.upperBound * flag),
                "${name}_ub"
            )
        } else {
            model.addConstraint(
                y geq (polynomial - polynomial.upperBound * (Flt64.one - u)),
                "${name}_xu"
            )
            model.addConstraint(
                y geq (polynomial.lowerBound * u),
                "${name}_lb"
            )
            model.addConstraint(
                y leq (polynomial.upperBound * u),
                "${name}_ub"
            )
        }

        return Ok(success)
    }

    override fun toString(): String {
        return displayName ?: name
    }

    override fun toRawString(unfold: Boolean): String {
        return "semi(${polynomial.toRawString(unfold)}})"
    }

    override fun value(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return if (flag != null) {
            val flagValue = flag.value(tokenList, zeroIfNone) ?: return null
            if (flagValue neq Flt64.zero) {
                polynomial.value(tokenList, zeroIfNone)
            } else {
                Flt64.zero
            }
        } else {
            val flagValue = tokenList.find(u)?.result
                ?: if (zeroIfNone) {
                    Flt64.zero
                } else {
                    return null
                }
            if (flagValue neq Flt64.zero) {
                polynomial.value(tokenList, zeroIfNone)
            } else {
                Flt64.zero
            }
        }
    }

    override fun value(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return if (flag != null) {
            val flagValue = flag.value(results, tokenList, zeroIfNone) ?: return null
            if (flagValue neq Flt64.zero) {
                polynomial.value(results, tokenList, zeroIfNone)
            } else {
                Flt64.zero
            }
        } else {
            val flagValue = tokenList.indexOf(u)?.let { results[it] }
                ?: if (zeroIfNone) {
                    Flt64.zero
                } else {
                    return null
                }
            if (flagValue neq Flt64.zero) {
                polynomial.value(results, tokenList, zeroIfNone)
            } else {
                Flt64.zero
            }
        }
    }
}

object SemiFunction {
    operator fun invoke(
        type: VariableType<*> = UInteger,
        polynomial: AbstractLinearPolynomial<*>,
        flag: AbstractLinearPolynomial<*>?,
        name: String,
        displayName: String? = null
    ): AbstractSemiFunction<*> {
        return if (type.isIntegerType) {
            SemiUIntegerFunction(polynomial, flag, name, displayName)
        } else {
            SemiURealFunction(polynomial, flag, name, displayName)
        }
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        polynomial: AbstractLinearPolynomial<*>,
        flag: BinVar,
        name: String,
        displayName: String? = null
    ): AbstractSemiFunction<*> {
        return if (type.isIntegerType) {
            SemiUIntegerFunction(polynomial, flag, name, displayName)
        } else {
            SemiURealFunction(polynomial, flag, name, displayName)
        }
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        polynomial: AbstractLinearPolynomial<*>,
        name: String,
        displayName: String? = null
    ): AbstractSemiFunction<*> {
        return if (type.isIntegerType) {
            SemiUIntegerFunction(polynomial, name, displayName)
        } else {
            SemiURealFunction(polynomial, name, displayName)
        }
    }
}

class SemiUIntegerFunction(
    polynomial: AbstractLinearPolynomial<*>,
    flag: AbstractLinearPolynomial<*>?,
    name: String,
    displayName: String? = null
) : AbstractSemiFunction<UIntVar>(polynomial, flag, name, displayName, { UIntVar(it) }) {
    constructor(
        polynomial: AbstractLinearPolynomial<*>,
        name: String,
        displayName: String? = null
    ) : this(polynomial, null, name, displayName)

    constructor(
        polynomial: AbstractLinearPolynomial<*>,
        flag: BinVar,
        name: String,
        displayName: String? = null
    ) : this(polynomial, LinearPolynomial(flag), name, displayName)

    override val discrete = true
}

class SemiURealFunction(
    polynomial: AbstractLinearPolynomial<*>,
    flag: AbstractLinearPolynomial<*>?,
    name: String,
    displayName: String? = null
) : AbstractSemiFunction<URealVar>(polynomial, flag, name, displayName, { URealVar(it) }) {
    constructor(
        polynomial: AbstractLinearPolynomial<*>,
        name: String,
        displayName: String? = null
    ) : this(polynomial, null, name, displayName)

    constructor(
        polynomial: AbstractLinearPolynomial<*>,
        flag: BinVar,
        name: String,
        displayName: String? = null
    ) : this(polynomial, LinearPolynomial(flag), name, displayName)
}
