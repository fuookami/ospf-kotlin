package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function

import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.multi_array.*
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
    private val epsilon: Flt64 = Flt64(1e-6),
    override val parent: IntermediateSymbol? = null,
    override var name: String,
    override var displayName: String? = null
) : LinearFunctionSymbol {
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
            name: String,
            displayName: String? = null
        ): IfThenFunction {
            return IfThenFunction(
                p = p.toLinearInequality(),
                q = q.toLinearInequality(),
                constraint = constraint,
                epsilon = epsilon,
                parent = parent,
                name = name,
                displayName = displayName
            )
        }
    }

    private val p by lazy { p.normalize() }
    private val q by lazy { q.normalize() }

    private val pk: PctVariable1 by lazy {
        PctVariable1(
            if (p.name.isEmpty()) {
                "${name}_pk"
            } else {
                "${p.name}_k"
            },
            Shape1(3)
        )
    }

    private val pu: BinVar by lazy {
        BinVar(p.name.ifEmpty { "${name}_pu" })
    }

    private val qk: PctVariable1 by lazy {
        PctVariable1(
            if (q.name.isEmpty()) {
                "${name}_qk"
            } else {
                "${q.name}_k"
            },
            Shape1(3)
        )
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

    override fun prepare(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable): Flt64? {
        p.lhs.cells
        p.rhs.cells
        q.lhs.cells
        q.rhs.cells
        if (values.isNullOrEmpty()) {
            y.prepareAndCache(null, tokenTable)
        } else {
            y.prepareAndCache(values, tokenTable)
        }

        return if (!constraint && tokenTable.cachedSolution && if (values.isNullOrEmpty()) {
            tokenTable.cached(this)
        } else {
            tokenTable.cached(this, values)
        } == false) {
            val pBin = if (values.isNullOrEmpty()) {
                p.isTrue(tokenTable)
            } else {
                p.isTrue(values, tokenTable)
            } ?: return null

            val qBin = if (values.isNullOrEmpty()) {
                q.isTrue(tokenTable)
            } else {
                q.isTrue(values, tokenTable)
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
        } else {
            null
        }
    }

    override fun toString(): String {
        return displayName ?: name
    }

    override fun register(tokenTable: AbstractMutableTokenTable): Try {
        when (val result = p.register(
            parentName = name,
            k = pk,
            flag = pu,
            tokenTable = tokenTable
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = q.register(
            parentName = name,
            k = qk,
            flag = qu,
            tokenTable = tokenTable
        )) {
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
        when (val result = p.register(
            parent = parent ?: this,
            parentName = name,
            k = pk,
            flag = pu,
            epsilon = epsilon,
            model = model
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = q.register(
            parent = parent ?: this,
            parentName = name,
            k = qk,
            flag = qu,
            epsilon = epsilon,
            model = model
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        if (constraint) {
            when (val result = model.addConstraint(
                pu leq qu,
                name = "${name}_u",
                from = parent ?: this
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        } else {
            when (val result = model.addConstraint(
                u eq (qu - pu + Flt64.one),
                name = "${name}_u",
                from = parent ?: this
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        return ok
    }

    override fun register(
        tokenTable: AbstractMutableTokenTable,
        fixedValues: Map<Symbol, Flt64>
    ): Try {
        return register(tokenTable)
    }

    override fun register(
        model: AbstractLinearMechanismModel,
        fixedValues: Map<Symbol, Flt64>
    ): Try {
        val pValue = p.isTrue(fixedValues, model.tokens) ?: return register(model)
        val qValue = q.isTrue(fixedValues, model.tokens) ?: return register(model)
        val bin = !pValue || qValue

        when (val result = p.register(
            parent = parent ?: this,
            parentName = name,
            k = pk,
            flag = pu,
            epsilon = epsilon,
            model = model,
            fixedValues = fixedValues
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = q.register(
            parent = parent ?: this,
            parentName = name,
            k = qk,
            flag = qu,
            epsilon = epsilon,
            model = model,
            fixedValues = fixedValues
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        if (constraint) {
            when (val result = model.addConstraint(
                pu leq qu,
                name = "${name}_u",
                from = parent ?: this
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        } else {
            when (val result = model.addConstraint(
                u eq (qu - pu + Flt64.one),
                name = "${name}_u",
                from = parent ?: this
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }

            when (val result = model.addConstraint(
                u eq bin,
                name = "${name}_uv",
                from = parent ?: this
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
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
            "if_then(${p.toRawString(unfold - UInt64.one)}, ${q.toRawString(unfold - UInt64.one)})"
        }
    }

    override fun evaluate(
        tokenList: AbstractTokenList,
        zeroIfNone: Boolean
    ): Flt64? {
        val pv = p.isTrue(tokenList, zeroIfNone) ?: return null
        val qv = q.isTrue(tokenList, zeroIfNone) ?: return null
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
        val pv = p.isTrue(results, tokenList, zeroIfNone) ?: return null
        val qv = q.isTrue(results, tokenList, zeroIfNone) ?: return null
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
        val pv = p.isTrue(values, tokenList, zeroIfNone) ?: return null
        val qv = q.isTrue(values, tokenList, zeroIfNone) ?: return null
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
        val pv = p.isTrue(tokenTable, zeroIfNone) ?: return null
        val qv = q.isTrue(tokenTable, zeroIfNone) ?: return null
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
        val pv = p.isTrue(results, tokenTable, zeroIfNone) ?: return null
        val qv = q.isTrue(results, tokenTable, zeroIfNone) ?: return null
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
        val pv = p.isTrue(values, tokenTable, zeroIfNone) ?: return null
        val qv = q.isTrue(values, tokenTable, zeroIfNone) ?: return null
        return if (UInt8(qv) geq UInt8(pv)) {
            Flt64.one
        } else {
            Flt64.zero
        }
    }
}
