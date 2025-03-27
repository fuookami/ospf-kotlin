package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function

import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

sealed class AbstractOneOfFunction(
    protected val branches: List<Branch>,
    override var name: String,
    override var displayName: String? = null
) : LinearFunctionSymbol {
    data class Branch(
        val condition: LinearPolynomial?,
        val polynomial: LinearPolynomial,
        val name: String
    )

    private val u: List<BinVar?> by lazy {
        branches.map {
            if (it.condition == null) {
                BinVar("${name}_${it.name}_u")
            } else {
                null
            }
        }
    }

    private val semis: LinearIntermediateSymbols1 by lazy {
        LinearIntermediateSymbols1("${name}_semi", Shape1(branches.size)) { b, _ ->
            val branch = branches[b]
            SemiFunction(
                type = UContinuous,
                x = branch.polynomial,
                flag = branch.condition ?: LinearPolynomial(u[b]!!),
                name = "${name}_${branch.name}_semi"
            )
        }
    }

    private val y: AbstractLinearPolynomial<*> by lazy {
        val y = sum(semis[_a])
        y.range.set(possibleRange)
        y
    }

    override val discrete = true

    override val range get() = y.range
    override val lowerBound get() = y.lowerBound
    override val upperBound get() = y.upperBound

    override val category = Linear

    override val dependencies: Set<IntermediateSymbol>
        get() {
            val dependencies = HashSet<IntermediateSymbol>()
            for (branch in branches) {
                dependencies.addAll(branch.polynomial.dependencies)
                branch.condition?.let {
                    dependencies.addAll(it.dependencies)
                }
            }
            return dependencies
        }
    override val cells get() = y.cells
    override val cached get() = y.cached

    private val possibleRange: ValueRange<Flt64>
        get() {
            return ValueRange(
                branches.minOf { it.polynomial.lowerBound?.value?.unwrap() ?: Flt64.negativeInfinity },
                branches.maxOf { it.polynomial.upperBound?.value?.unwrap() ?: Flt64.infinity },
            ).value!!
        }

    override fun flush(force: Boolean) {
        for (branch in branches) {
            branch.polynomial.flush(force)
            branch.condition?.flush(force)
        }
        y.range.set(possibleRange)
    }

    override fun prepare(tokenTable: AbstractTokenTable) {
        for (branch in branches) {
            branch.polynomial.cells
            branch.condition?.cells
        }
        for (semi in semis) {
            semi.prepare(tokenTable)
        }

        if (tokenTable.cachedSolution && tokenTable.cached(this) == false) {
            val values = branches.mapIndexedNotNull { b, branch ->
                val semi = semis[b]
                semi.evaluate(tokenTable)?.let { value ->
                    if (value neq Flt64.zero) {
                        if (branch.condition == null) {
                            val ui = u[b]!!
                            logger.trace { "Setting SemiFunction ${name}.${branch.name}.u to true" }
                            tokenTable.find(ui)?.let { token ->
                                token._result = Flt64.one
                            }
                        }
                        value
                    } else {
                        if (branch.condition == null) {
                            val ui = u[b]!!
                            logger.trace { "Setting SemiFunction ${name}.${branch.name}.u to false" }
                            tokenTable.find(ui)?.let { token ->
                                token._result = Flt64.one
                            }
                        }
                        null
                    }
                }
            }

            if (values.size == 1) {
                tokenTable.cache(this, null, values.first())
            }
        }
    }

    override fun register(tokenTable: AbstractMutableTokenTable): Try {
        u.filterNotNull().forEach {
            when (val result = tokenTable.add(it)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        semis.forEach {
            when (val result = tokenTable.add(it)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        return ok
    }

    override fun register(model: AbstractLinearMechanismModel): Try {
        for (branch in branches) {
            if (branch.condition != null) {
                if (branch.condition.lowerBound!!.value.unwrap() ls Flt64.zero || branch.condition.upperBound!!.value.unwrap() gr Flt64.one) {
                    return Failed(Err(ErrorCode.ApplicationFailed, "${branch.name}'s domain of definition unsatisfied: ${branch.condition}"))
                }
            }
        }

        when (val result = model.addConstraint(
            sum(branches.mapIndexed { b, branch ->
                branch.condition ?: LinearPolynomial(u[b]!!)
            }) eq Flt64.one,
            name = "${name}_condition"
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

    override fun evaluate(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        for ((b, _) in branches.withIndex()) {
            val value = semis[b].evaluate(tokenList, zeroIfNone) ?: continue
            if (value neq Flt64.zero) {
                return value
            }
        }
        return null
    }

    override fun evaluate(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        for ((b, _) in branches.withIndex()) {
            val value = semis[b].evaluate(results, tokenList, zeroIfNone) ?: continue
            if (value neq Flt64.zero) {
                return value
            }
        }
        return null
    }

    override fun calculateValue(tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        for ((b, _) in branches.withIndex()) {
            val value = semis[b].evaluate(tokenTable, zeroIfNone) ?: continue
            if (value neq Flt64.zero) {
                return value
            }
        }
        return null
    }

    override fun calculateValue(results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        for ((b, _) in branches.withIndex()) {
            val value = semis[b].evaluate(results, tokenTable, zeroIfNone) ?: continue
            if (value neq Flt64.zero) {
                return value
            }
        }
        return null
    }
}

class IfElseFunction(
    private val branch: Branch,
    private val elseBranch: Branch,
    private val condition: LinearPolynomial,
    name: String,
    displayName: String? = null
) : AbstractOneOfFunction(
    listOf(
        Branch(
            condition = condition,
            polynomial = branch.polynomial,
            name = branch.name
        ),
        Branch(
            condition = Flt64.one - condition,
            polynomial = elseBranch.polynomial,
            name = elseBranch.name
        )
    ),
    name = name,
    displayName = displayName
) {
    data class Branch(
        val polynomial: LinearPolynomial,
        val name: String
    )

    override fun toRawString(unfold: UInt64): String {
        return if (unfold eq UInt64.zero) {
            displayName ?: name
        } else {
            "if_else(${condition.toTidyRawString(unfold - UInt64.one)} -> ${branch.polynomial.toTidyRawString(unfold - UInt64.one)}, ${elseBranch.polynomial.toTidyRawString(unfold - UInt64.one)})"
        }
    }
}

class OneOfFunction(
    branches: List<Branch>,
    name: String,
    displayName: String? = null
) : AbstractOneOfFunction(branches, name, displayName) {
    override fun toRawString(unfold: UInt64): String {
        return if (unfold eq UInt64.zero) {
            displayName ?: name
        } else {
            "one_of(${
                branches.joinToString(", ") {
                    if (it.condition != null) {
                        "${it.condition.toTidyRawString(unfold - UInt64.one)} -> ${it.polynomial.toTidyRawString(unfold - UInt64.one)}"
                    } else {
                        it.polynomial.toTidyRawString(unfold - UInt64.one)
                    }
                }
            })"
        }
    }
}
