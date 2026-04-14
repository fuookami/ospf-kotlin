@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package fuookami.ospf.kotlin.core.intermediate_symbol.legacy.linear_function

import fuookami.ospf.kotlin.core.expression.polynomial.*
import fuookami.ospf.kotlin.core.intermediate_symbol.*
import fuookami.ospf.kotlin.core.intermediate_symbol.legacy.linear_function.AbstractOneOfFunction.Branch
import fuookami.ospf.kotlin.core.intermediate_model.eq
import fuookami.ospf.kotlin.core.intermediate_model.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.intermediate_model.AbstractTokenTable
import fuookami.ospf.kotlin.core.variable.AbstractTokenList
import fuookami.ospf.kotlin.core.variable.AddableTokenCollection
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.utils.error.Err
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.multiarray.Shape1
import fuookami.ospf.kotlin.multiarray._a
import org.apache.logging.log4j.kotlin.logger

sealed class AbstractOneOfFunction(
    protected val branches: List<Branch>,
    override val parent: IntermediateSymbol? = null,
    args: Any? = null,
    override var name: String,
    override var displayName: String? = null
) : LinearFunctionSymbol() {
    data class Branch(
        val condition: AbstractLinearPolynomial<*>? = null,
        val polynomial: AbstractLinearPolynomial<*>,
        val name: String
    ) {
        companion object {
            operator fun <
                    T1 : ToLinearPolynomial<Poly1>,
                    Poly1 : AbstractLinearPolynomial<Poly1>,
                    T2 : ToLinearPolynomial<Poly2>,
                    Poly2 : AbstractLinearPolynomial<Poly2>
                    > invoke(
                condition: T1?,
                polynomial: T2,
                name: String
            ): Branch {
                return Branch(
                    condition?.toLinearPolynomial(),
                    polynomial.toLinearPolynomial(),
                    name
                )
            }
        }
    }

    private val _args = args
    override val args = _args ?: parent?.args

    private val u: List<BinVar?> by lazy {
        branches.map {
            if (it.condition == null) {
                BinVar("${name}_${it.name}_u")
            } else {
                null
            }
        }
    }

    private val masks: SymbolCombination<MaskingFunction, Shape1> by lazy {
        SymbolCombination(
            name = "${name}_semi",
            shape = Shape1(branches.size)
        ) { b, _ ->
            val branch = branches[b]
            MaskingFunction(
                x = branch.polynomial,
                mask = branch.condition ?: LinearPolynomial(u[b]!!),
                parent = parent ?: this,
                name = "${name}_${branch.name}_semi"
            )
        }
    }

    private val y: AbstractLinearPolynomial<*> by lazy {
        val y = sum(masks[_a])
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

    override fun prepare(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable): Flt64? {
        tokenTable.cache(
            masks.mapNotNull {
                val value = if (values.isNullOrEmpty()) {
                    it.prepare(null, tokenTable)
                } else {
                    it.prepare(values, tokenTable)
                }
                if (value != null) {
                    (it as IntermediateSymbol) to value
                } else {
                    null
                }
            }.toMap()
        )

        return prepareIfNotCached(values, tokenTable) {
            val evaluatedValues = branches.mapIndexedNotNull { b, branch ->
                val semi = masks[b]
                val value = if (values.isNullOrEmpty()) {
                    semi.evaluate(tokenTable)
                } else {
                    semi.evaluate(
                        values = values,
                        tokenTable = tokenTable
                    )
                } ?: return@mapIndexedNotNull null

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

            if (evaluatedValues.size == 1) {
                evaluatedValues.first()
            } else {
                null
            }
        }
    }

    override fun register(tokenTable: AddableTokenCollection): Try {
        when (val result = tokenTable.add(u.filterNotNull())) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        masks.forEach {
            when (val result = it.register(tokenTable)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }

                is Fatal -> {
                    return Fatal(result.errors)
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

        masks.forEach {
            when (val result = it.register(model)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }

        when (val result = model.addConstraint(
            relation = sum(branches.mapIndexed { b, branch ->
                branch.condition ?: LinearPolynomial(u[b]!!)
            }) eq Flt64.one,
            name = "${name}_condition",
            from = parent ?: this
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }

            is Fatal -> {
                return Fatal(result.errors)
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
        val values = branches.map { branch ->
            val conditionValue = branch.condition?.evaluate(
                values = fixedValues,
                tokenTable = model.tokens
            ) ?: return register(model)
            val polynomialValue = branch.polynomial.evaluate(
                fixedValues, model.tokens
            ) ?: return register(model)
            (conditionValue gr Flt64.zero) to polynomialValue
        }

        masks.forEach {
            when (val result = it.register(model, fixedValues)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }

        when (val result = model.addConstraint(
            relation = sum(branches.mapIndexed { b, branch ->
                branch.condition ?: LinearPolynomial(u[b]!!)
            }) eq Flt64.one,
            name = "${name}_condition",
            from = parent ?: this
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        u.forEachIndexed { i, ui ->
            if (ui != null) {
                when (val result = model.addConstraint(
                    relation = ui eq values[i].first,
                    name = "${name}_${branches[i].name}_u",
                    from = parent ?: this
                )) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }

                    is Fatal -> {
                        return Fatal(result.errors)
                    }

                    is Fatal -> {
                        return Fatal(result.errors)
                    }
                }
            }
        }

        return ok
    }

    override fun toString(): String {
        return displayName ?: name
    }

    override fun evaluate(
        tokenList: AbstractTokenList,
        zeroIfNone: Boolean
    ): Flt64? {
        for ((b, _) in branches.withIndex()) {
            val value = masks[b].evaluate(tokenList, zeroIfNone) ?: continue
            if (value neq Flt64.zero) {
                return value
            }
        }
        return null
    }

    override fun evaluate(
        results: List<Flt64>,
        tokenList: AbstractTokenList,
        zeroIfNone: Boolean
    ): Flt64? {
        for ((b, _) in branches.withIndex()) {
            val value = masks[b].evaluate(
                results = results,
                tokenList = tokenList,
                zeroIfNone = zeroIfNone
            ) ?: continue
            if (value neq Flt64.zero) {
                return value
            }
        }
        return null
    }

    override fun evaluate(
        values: Map<Symbol, Flt64>,
        tokenList: AbstractTokenList?,
        zeroIfNone: Boolean
    ): Flt64? {
        for ((b, _) in branches.withIndex()) {
            val value = masks[b].evaluate(
                values = values,
                tokenList = tokenList,
                zeroIfNone = zeroIfNone
            ) ?: continue
            if (value neq Flt64.zero) {
                return value
            }
        }
        return null
    }

    override fun calculateValue(
        tokenTable: AbstractTokenTable,
        zeroIfNone: Boolean
    ): Flt64? {
        for ((b, _) in branches.withIndex()) {
            val value = masks[b].evaluate(tokenTable, zeroIfNone) ?: continue
            if (value neq Flt64.zero) {
                return value
            }
        }
        return null
    }

    override fun calculateValue(
        results: List<Flt64>,
        tokenTable: AbstractTokenTable,
        zeroIfNone: Boolean
    ): Flt64? {
        for ((b, _) in branches.withIndex()) {
            val value = masks[b].evaluate(
                results = results,
                tokenTable = tokenTable,
                zeroIfNone = zeroIfNone
            ) ?: continue
            if (value neq Flt64.zero) {
                return value
            }
        }
        return null
    }

    override fun calculateValue(
        values: Map<Symbol, Flt64>,
        tokenTable: AbstractTokenTable?,
        zeroIfNone: Boolean
    ): Flt64? {
        for ((b, _) in branches.withIndex()) {
            val value = masks[b].evaluate(
                values = values,
                tokenTable = tokenTable,
                zeroIfNone = zeroIfNone
            ) ?: continue
            if (value neq Flt64.zero) {
                return value
            }
        }
        return null
    }
}

@Deprecated(
    message = "Use intermediate_symbol.function.IfElseFunction instead",
    replaceWith = ReplaceWith("IfElseFunction", "fuookami.ospf.kotlin.core.intermediate_symbol.function.IfElseFunction")
)
class IfElseFunction(
    private val branch: Branch,
    private val elseBranch: Branch,
    private val condition: AbstractLinearPolynomial<*>,
    parent: IntermediateSymbol? = null,
    name: String,
    displayName: String? = null
) : AbstractOneOfFunction(
    branches = listOf(
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
    parent = parent,
    name = name,
    displayName = displayName
) {
    data class Branch(
        val polynomial: AbstractLinearPolynomial<*>,
        val name: String
    ) {
        companion object {
            operator fun <
                    T : ToLinearPolynomial<Poly>,
                    Poly : AbstractLinearPolynomial<Poly>
                    > invoke(
                polynomial: T,
                name: String
            ): Branch {
                return Branch(
                    polynomial.toLinearPolynomial(),
                    name
                )
            }
        }
    }

    companion object {
        operator fun <
                T : ToLinearPolynomial<Poly>,
                Poly : AbstractLinearPolynomial<Poly>
                > invoke(
            branch: Branch,
            elseBranch: Branch,
            condition: T,
            parent: IntermediateSymbol? = null,
            name: String,
            displayName: String? = null
        ): IfElseFunction {
            return IfElseFunction(
                branch = branch,
                elseBranch = elseBranch,
                condition = condition.toLinearPolynomial(),
                parent = parent,
                name = name,
                displayName = displayName
            )
        }
    }

    override fun toRawString(unfold: UInt64): String {
        return if (unfold eq UInt64.zero) {
            displayName ?: name
        } else {
            "if_else(${condition.toTidyRawString(unfold - UInt64.one)} -> ${branch.polynomial.toTidyRawString(unfold - UInt64.one)}, ${elseBranch.polynomial.toTidyRawString(unfold - UInt64.one)})"
        }
    }
}

@Deprecated(
    message = "Use intermediate_symbol.function.OneOfFunction instead",
    replaceWith = ReplaceWith("OneOfFunction", "fuookami.ospf.kotlin.core.intermediate_symbol.function.OneOfFunction")
)
class OneOfFunction(
    branches: List<Branch>,
    parent: IntermediateSymbol? = null,
    args: Any? = null,
    name: String,
    displayName: String? = null
) : AbstractOneOfFunction(
    branches = branches,
    parent = parent,
    args = args,
    name = name,
    displayName = displayName
) {
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





