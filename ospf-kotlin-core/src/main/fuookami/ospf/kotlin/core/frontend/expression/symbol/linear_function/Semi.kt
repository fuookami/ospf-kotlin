package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function

import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

sealed class AbstractSemiFunction<V : Variable<*>>(
    protected val x: AbstractLinearPolynomial<*>,
    protected val flag: AbstractLinearPolynomial<*>?,
    override var name: String,
    override var displayName: String? = null,
    private val ctor: (String) -> V,
    private val rangeSetting: (V, Flt64) -> Unit
) : LinearFunctionSymbol {
    private val logger = logger()

    private val offset = if (x.lowerBound!!.value.unwrap() ls Flt64.zero) {
        abs(x.lowerBound!!.value.unwrap())
    } else {
        Flt64.zero
    }

    private val y: V by lazy {
        val y = ctor("${name}_y")
        rangeSetting(y, possibleRange.upperBound.value.unwrap() + offset)
        y
    }

    private val u: BinVar by lazy {
        BinVar("${name}_u")
    }

    private val polyY: LinearPolynomial by lazy {
        val polyY = LinearPolynomial(y - offset, "${name}_y")
        polyY.range.set(possibleRange)
        polyY
    }

    override val range get() = polyY.range
    override val lowerBound get() = polyY.lowerBound
    override val upperBound get() = polyY.upperBound

    override val category = Linear

    override val dependencies: Set<IntermediateSymbol>
        get() {
            val dependencies = HashSet<IntermediateSymbol>()
            dependencies.addAll(x.dependencies)
            flag?.let { dependencies.addAll(it.dependencies) }
            return dependencies
        }
    override val cells get() = polyY.cells
    override val cached get() = polyY.cached

    private val possibleRange
        get() = ValueRange(
            (flag?.lowerBound ?: u.lowerBound)!! * x.lowerBound!!,
            (flag?.upperBound ?: u.upperBound)!! * x.upperBound!!,
            Flt64
        )

    override fun flush(force: Boolean) {
        x.flush(force)
        flag?.flush(force)
        polyY.flush(force)
        polyY.range.set(possibleRange)
    }

    override fun prepare(tokenTable: AbstractTokenTable): Flt64? {
        x.cells
        flag?.cells

        return if (tokenTable.cachedSolution && tokenTable.cached(this) == false) {
            val xValue = x.evaluate(tokenTable) ?: return null

            val bin = if (flag != null) {
                val value = flag.evaluate(tokenTable) ?: return null
                value gr Flt64.zero
            } else {
                val bin = xValue gr Flt64.zero
                logger.trace { "Setting SemiFunction ${name}.u to $bin" }
                tokenTable.find(u)?.let { token ->
                    token._result = if (bin) {
                        Flt64.one
                    } else {
                        Flt64.zero
                    }
                }
                bin
            }

            val yValue = if (bin) {
                xValue
            } else {
                Flt64.zero
            }

            logger.trace { "Setting SemiFunction ${name}.y to $yValue" }
            tokenTable.find(y)?.let { token ->
                token._result = yValue + offset
            }

            yValue
        } else {
            null
        }
    }

    override fun register(tokenTable: AbstractMutableTokenTable): Try {
        when (val result = tokenTable.add(y)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        if (flag == null) {
            when (val result = tokenTable.add(u)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        return ok
    }

    override fun register(model: AbstractLinearMechanismModel): Try {
        if (flag != null) {
            if (flag.lowerBound!!.value.unwrap() ls Flt64.zero || flag.upperBound!!.value.unwrap() gr Flt64.one) {
                return Failed(Err(ErrorCode.ApplicationFailed, "$name's domain of definition unsatisfied: $flag"))
            }
        }

        when (val result = model.addConstraint(
            (y - offset) leq x,
            "${name}_x"
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        if (flag != null) {
            when (val result = model.addConstraint(
                (y - offset) geq (x - x.upperBound!!.value.unwrap() * (Flt64.one - flag)),
                "${name}_xu"
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
            when (val result = model.addConstraint(
                (y - offset) geq (x.lowerBound!!.value.unwrap() * flag),
                "${name}_lb"
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
            when (val result = model.addConstraint(
                (y - offset) leq (x.upperBound!!.value.unwrap() * flag),
                "${name}_ub"
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        } else {
            when (val result = model.addConstraint(
                (y - offset) geq (x - x.upperBound!!.value.unwrap() * (Flt64.one - u)),
                "${name}_xu"
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
            when (val result = model.addConstraint(
                (y - offset) geq (x.lowerBound!!.value.unwrap() * u),
                "${name}_lb"
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
            when (val result = model.addConstraint(
                (y - offset) leq (x.upperBound!!.value.unwrap() * u),
                "${name}_ub"
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

    override fun toRawString(unfold: UInt64): String {
        return if (unfold eq UInt64.zero) {
            displayName ?: name
        } else {
            if (flag != null) {
                "semi(${x.toTidyRawString(unfold - UInt64.one)}, ${flag.toTidyRawString(unfold - UInt64.one)})"
            } else {
                "semi(${x.toTidyRawString(unfold - UInt64.one)})"
            }
        }
    }

    override fun evaluate(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return if (flag != null) {
            val flagValue = flag.evaluate(tokenList, zeroIfNone) ?: return null
            if (flagValue neq Flt64.zero) {
                x.evaluate(tokenList, zeroIfNone)
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
                x.evaluate(tokenList, zeroIfNone)
            } else {
                Flt64.zero
            }
        }
    }

    override fun evaluate(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return if (flag != null) {
            val flagValue = flag.evaluate(results, tokenList, zeroIfNone) ?: return null
            if (flagValue neq Flt64.zero) {
                x.evaluate(results, tokenList, zeroIfNone)
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
                x.evaluate(results, tokenList, zeroIfNone)
            } else {
                Flt64.zero
            }
        }
    }

    override fun calculateValue(tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        return if (flag != null) {
            val flagValue = flag.evaluate(tokenTable, zeroIfNone) ?: return null
            if (flagValue neq Flt64.zero) {
                x.evaluate(tokenTable, zeroIfNone)
            } else {
                Flt64.zero
            }
        } else {
            val flagValue = tokenTable.find(u)?.result
                ?: if (zeroIfNone) {
                    Flt64.zero
                } else {
                    return null
                }
            if (flagValue neq Flt64.zero) {
                x.evaluate(tokenTable, zeroIfNone)
            } else {
                Flt64.zero
            }
        }
    }

    override fun calculateValue(results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        return if (flag != null) {
            val flagValue = flag.evaluate(results, tokenTable, zeroIfNone) ?: return null
            if (flagValue neq Flt64.zero) {
                x.evaluate(results, tokenTable, zeroIfNone)
            } else {
                Flt64.zero
            }
        } else {
            val flagValue = tokenTable.tokenList.indexOf(u)?.let { results[it] }
                ?: if (zeroIfNone) {
                    Flt64.zero
                } else {
                    return null
                }
            if (flagValue neq Flt64.zero) {
                x.evaluate(results, tokenTable, zeroIfNone)
            } else {
                Flt64.zero
            }
        }
    }
}

object SemiFunction {
    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: AbstractLinearPolynomial<*>,
        flag: AbstractLinearPolynomial<*>?,
        name: String,
        displayName: String? = null
    ): AbstractSemiFunction<*> {
        return if (type.isIntegerType) {
            SemiIntegerFunction(x, flag, name, displayName)
        } else {
            SemiRealFunction(x, flag, name, displayName)
        }
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: AbstractLinearPolynomial<*>,
        flag: BinVar,
        name: String,
        displayName: String? = null
    ): AbstractSemiFunction<*> {
        return if (type.isIntegerType) {
            SemiIntegerFunction(x, flag, name, displayName)
        } else {
            SemiRealFunction(x, flag, name, displayName)
        }
    }

    operator fun invoke(
        type: VariableType<*> = UInteger,
        x: AbstractLinearPolynomial<*>,
        name: String,
        displayName: String? = null
    ): AbstractSemiFunction<*> {
        return if (type.isIntegerType) {
            SemiIntegerFunction(x, name, displayName)
        } else {
            SemiRealFunction(x, name, displayName)
        }
    }
}

class SemiIntegerFunction(
    x: AbstractLinearPolynomial<*>,
    flag: AbstractLinearPolynomial<*>?,
    name: String,
    displayName: String? = null
) : AbstractSemiFunction<UIntVar>(x, flag, name, displayName, { UIntVar(it) }, { v, ub -> v.range.leq(ub.floor().toUInt64()) }) {
    constructor(
        x: AbstractLinearPolynomial<*>,
        name: String,
        displayName: String? = null
    ) : this(x, null, name, displayName)

    constructor(
        x: AbstractLinearPolynomial<*>,
        flag: BinVar,
        name: String,
        displayName: String? = null
    ) : this(x, LinearPolynomial(flag), name, displayName)

    override val discrete = true
}

class SemiRealFunction(
    x: AbstractLinearPolynomial<*>,
    flag: AbstractLinearPolynomial<*>?,
    name: String,
    displayName: String? = null
) : AbstractSemiFunction<URealVar>(x, flag, name, displayName, { URealVar(it) }, { v, ub -> v.range.leq(ub) }) {
    constructor(
        x: AbstractLinearPolynomial<*>,
        name: String,
        displayName: String? = null
    ) : this(x, null, name, displayName)

    constructor(
        x: AbstractLinearPolynomial<*>,
        flag: AbstractVariableItem<*, Binary>,
        name: String,
        displayName: String? = null
    ) : this(x, LinearPolynomial(flag), name, displayName)
}

class ReluFunction(
    x: AbstractLinearPolynomial<*>,
    name: String = "${x}_relu",
    displayName: String? = "Relu(${x})"
) : AbstractSemiFunction<URealVar>(x, null, name, displayName, { URealVar(it) }, { v, ub -> v.range.leq(ub) }) {
    override fun toRawString(unfold: UInt64): String {
        return if (unfold eq UInt64.zero) {
            displayName ?: name
        } else {
            if (flag != null) {
                "relu(${x.toTidyRawString(unfold - UInt64.one)}, ${flag.toTidyRawString(unfold - UInt64.one)})"
            } else {
                "relu(${x.toTidyRawString(unfold - UInt64.one)})"
            }
        }
    }
}
