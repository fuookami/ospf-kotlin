package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function

import fuookami.ospf.kotlin.core.frontend.expression.monomial.times
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.AbstractLinearPolynomial
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.minus
import fuookami.ospf.kotlin.core.frontend.expression.symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.frontend.expression.symbol.LinearFunctionSymbol
import fuookami.ospf.kotlin.core.frontend.expression.symbol.prepareIfNotCached
import fuookami.ospf.kotlin.core.frontend.inequality.ToLinearInequality
import fuookami.ospf.kotlin.core.frontend.model.mechanism.eq
import fuookami.ospf.kotlin.core.frontend.model.mechanism.leq
import fuookami.ospf.kotlin.core.frontend.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.frontend.model.mechanism.AbstractTokenTable
import fuookami.ospf.kotlin.core.frontend.model.mechanism.LinearConstraintInput
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.number.UInt8
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.toFlt64
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.multiarray.Shape1
import org.apache.logging.log4j.kotlin.logger

class IfThenFunction(
    pInput: LinearConstraintInput,
    qInput: LinearConstraintInput,
    private val constraint: Boolean = true,
    private val epsilon: Flt64 = Flt64(1e-6),
    override val parent: IntermediateSymbol? = null,
    args: Any? = parent?.args,
    override var name: String,
    override var displayName: String? = null
) : LinearFunctionSymbol() {
    companion object {
        operator fun <
                T1 : ToLinearInequality,
                T2 : ToLinearInequality
                > invoke(
            p: T1,
            q: T2,
            constraint: Boolean = true,
            epsilon: Flt64 = Flt64(1e-6),
            parent: IntermediateSymbol? = null,
            args: Any? = null,
            name: String,
            displayName: String? = null
        ): IfThenFunction {
            return IfThenFunction(
                pInput = LinearConstraintInput.from(p.toLinearInequality()),
                qInput = LinearConstraintInput.from(q.toLinearInequality()),
                constraint = constraint,
                epsilon = epsilon,
                parent = parent,
                args = args,
                name = name,
                displayName = displayName
            )
        }
    }

    internal val _args = args
    override val args get() = _args ?: parent?.args

    private val pInput by lazy { pInput }
    private val qInput by lazy { qInput }

    private val pk: PctVariable1 by lazy {
        PctVariable1(
            if (pInput.name.isEmpty()) {
                "${name}_pk"
            } else {
                "${pInput.name}_k"
            },
            Shape1(3)
        )
    }

    private val pu: BinVar by lazy {
        BinVar(pInput.name.ifEmpty { "${name}_pu" })
    }

    private val qk: PctVariable1 by lazy {
        PctVariable1(
            if (qInput.name.isEmpty()) {
                "${name}_qk"
            } else {
                "${qInput.name}_k"
            },
            Shape1(3)
        )
    }

    private val qu: BinVar by lazy {
        BinVar(qInput.name.ifEmpty { "${name}_qu" })
    }

    private val u: UIntVar by lazy {
        UIntVar("${name}_u")
    }

    private val y: BinaryzationFunction by lazy {
        BinaryzationFunction(
            x = LinearPolynomial(Flt64.two * u),
            parent = parent ?: this,
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
            for (monomial in pInput.flattenData.monomials + qInput.flattenData.monomials) {
                if (monomial.symbol is IntermediateSymbol) {
                    dependencies.add(monomial.symbol as IntermediateSymbol)
                }
            }
            return dependencies
        }
    override val cells get() = polyY.cells
    override val cached get() = polyY.cached

    private val possibleRange: ValueRange<Flt64>
        get() {
            return ValueRange(Flt64.zero, Flt64.one).value!!
        }

    override fun flush(force: Boolean) {
        if (!constraint) {
            y.flush(force)
        }
        polyY.flush(force)
        polyY.range.set(possibleRange)
    }

    override fun prepare(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable): Flt64? {
        y.prepareAndCache(values, tokenTable)

        return if (!constraint && tokenTable.cachedSolution) {
            prepareIfNotCached(values, tokenTable) {
                val pBin = if (values.isNullOrEmpty()) {
                    pInput.isTrue(tokenTable)
                } else {
                    pInput.isTrue(values, tokenTable)
                } ?: return null

                val qBin = if (values.isNullOrEmpty()) {
                    qInput.isTrue(tokenTable)
                } else {
                    qInput.isTrue(values, tokenTable)
                } ?: return null

                logger.trace { "Setting IfThenFunction ${name}.pu initial solution: $pBin" }
                tokenTable.find(pu)?.let { token ->
                    token._result = pBin.toFlt64()
                }

                logger.trace { "Setting IfThenFunction ${name}.qu initial solution: $qBin" }
                tokenTable.find(qu)?.let { token ->
                    token._result = qBin.toFlt64()
                }

                val uValue = UInt8(qBin).toFlt64() - UInt8(pBin).toFlt64() + Flt64.one
                logger.trace { "Setting IfThenFunction ${name}.u initial solution: $uValue" }
                tokenTable.find(u)?.let { token ->
                    token._result = uValue
                }

                val bin = uValue neq Flt64.zero
                if (bin) {
                    Flt64.one
                } else {
                    Flt64.zero
                }
            }
        } else {
            null
        }
    }

    override fun toString(): String {
        return displayName ?: name
    }

    override fun register(tokenTable: AddableTokenCollection): Try {
        when (val result = pInput.register(
            parentName = name,
            k = pk,
            flag = pu,
            tokenTable = tokenTable
        )) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        when (val result = qInput.register(
            parentName = name,
            k = qk,
            flag = qu,
            tokenTable = tokenTable
        )) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        if (!constraint) {
            when (val result = tokenTable.add(u)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }

            when (val result = y.register(tokenTable)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }

        return ok
    }

    override fun register(model: AbstractLinearMechanismModel): Try {
        when (val result = pInput.register(
            parent = parent ?: this,
            parentName = name,
            k = pk,
            flag = pu,
            epsilon = epsilon,
            model = model
        )) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        when (val result = qInput.register(
            parent = parent ?: this,
            parentName = name,
            k = qk,
            flag = qu,
            epsilon = epsilon,
            model = model
        )) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        if (constraint) {
            when (val result = model.addConstraint(
                relation = pu leq qu,
                name = "${name}_u",
                from = parent ?: this
            )) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        } else {
            when (val result = model.addConstraint(
                relation = u eq (qu - pu + Flt64.one),
                name = "${name}_u",
                from = parent ?: this
            )) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }

        return ok
    }

    override fun register(
        tokenTable: AddableTokenCollection,
        fixedValues: Map<Symbol, Flt64>
    ): Try {
        return register(tokenTable)
    }

    override fun register(
        model: AbstractLinearMechanismModel,
        fixedValues: Map<Symbol, Flt64>
    ): Try {
        val pValue = pInput.isTrue(fixedValues, model.tokens) ?: return register(model)
        val qValue = qInput.isTrue(fixedValues, model.tokens) ?: return register(model)
        val bin = !pValue || qValue

        when (val result = pInput.register(
            parent = parent ?: this,
            parentName = name,
            k = pk,
            flag = pu,
            epsilon = epsilon,
            model = model,
            fixedValues = fixedValues
        )) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        when (val result = qInput.register(
            parent = parent ?: this,
            parentName = name,
            k = qk,
            flag = qu,
            epsilon = epsilon,
            model = model,
            fixedValues = fixedValues
        )) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        if (constraint) {
            when (val result = model.addConstraint(
                pu leq qu,
                name = "${name}_u",
                from = parent ?: this
            )) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        } else {
            when (val result = model.addConstraint(
                u eq (qu - pu + Flt64.one),
                name = "${name}_u",
                from = parent ?: this
            )) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }

            when (val result = model.addConstraint(
                u eq bin,
                name = "${name}_uv",
                from = parent ?: this
            )) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }

            model.tokens.find(u)?.let { token ->
                token._result = bin.toFlt64()
            }
        }

        return ok
    }

    override fun toRawString(unfold: UInt64): String {
        return if (unfold eq UInt64.zero) {
            displayName ?: name
        } else {
            "if_then(${pInput.name}, ${qInput.name})"
        }
    }

    override fun evaluate(
        tokenList: AbstractTokenList,
        zeroIfNone: Boolean
    ): Flt64? {
        val pv = pInput.isTrue(tokenList, zeroIfNone) ?: return null
        val qv = qInput.isTrue(tokenList, zeroIfNone) ?: return null
        return if (UInt8(qv) geq UInt8(pv)) {
            Flt64.one
        } else {
            Flt64.zero
        }
    }

    override fun evaluate(
        results: List<Flt64>,
        tokenList: AbstractTokenList,
        zeroIfNone: Boolean
    ): Flt64? {
        val pv = pInput.isTrue(results, tokenList, zeroIfNone) ?: return null
        val qv = qInput.isTrue(results, tokenList, zeroIfNone) ?: return null
        return if (UInt8(qv) geq UInt8(pv)) {
            Flt64.one
        } else {
            Flt64.zero
        }
    }

    override fun evaluate(
        values: Map<Symbol, Flt64>,
        tokenList: AbstractTokenList?,
        zeroIfNone: Boolean
    ): Flt64? {
        val pv = pInput.isTrue(values, tokenList, zeroIfNone) ?: return null
        val qv = qInput.isTrue(values, tokenList, zeroIfNone) ?: return null
        return if (UInt8(qv) geq UInt8(pv)) {
            Flt64.one
        } else {
            Flt64.zero
        }
    }

    override fun calculateValue(
        tokenTable: AbstractTokenTable,
        zeroIfNone: Boolean
    ): Flt64? {
        val pv = pInput.isTrue(tokenTable, zeroIfNone) ?: return null
        val qv = qInput.isTrue(tokenTable, zeroIfNone) ?: return null
        return if (UInt8(qv) geq UInt8(pv)) {
            Flt64.one
        } else {
            Flt64.zero
        }
    }

    override fun calculateValue(
        results: List<Flt64>,
        tokenTable: AbstractTokenTable,
        zeroIfNone: Boolean
    ): Flt64? {
        val pv = pInput.isTrue(results, tokenTable, zeroIfNone) ?: return null
        val qv = qInput.isTrue(results, tokenTable, zeroIfNone) ?: return null
        return if (UInt8(qv) geq UInt8(pv)) {
            Flt64.one
        } else {
            Flt64.zero
        }
    }

    override fun calculateValue(
        values: Map<Symbol, Flt64>,
        tokenTable: AbstractTokenTable?,
        zeroIfNone: Boolean
    ): Flt64? {
        val pv = pInput.isTrue(values, tokenTable, zeroIfNone) ?: return null
        val qv = qInput.isTrue(values, tokenTable, zeroIfNone) ?: return null
        return if (UInt8(qv) geq UInt8(pv)) {
            Flt64.one
        } else {
            Flt64.zero
        }
    }
}
