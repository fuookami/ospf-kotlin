@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function

import fuookami.ospf.kotlin.core.frontend.expression.polynomial.AbstractLinearPolynomial
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.ToLinearPolynomial
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.sum
import fuookami.ospf.kotlin.core.frontend.expression.symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.frontend.expression.symbol.LinearLogicFunctionSymbol
import fuookami.ospf.kotlin.core.frontend.expression.symbol.prepareIfNotCached
import fuookami.ospf.kotlin.core.frontend.expression.symbol.toTidyRawString
import fuookami.ospf.kotlin.core.frontend.model.mechanism.eq
import fuookami.ospf.kotlin.core.frontend.model.mechanism.geq
import fuookami.ospf.kotlin.core.frontend.model.mechanism.leq
import fuookami.ospf.kotlin.core.frontend.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.frontend.model.mechanism.AbstractTokenTable
import fuookami.ospf.kotlin.core.frontend.variable.AbstractTokenList
import fuookami.ospf.kotlin.core.frontend.variable.AddableTokenCollection
import fuookami.ospf.kotlin.core.frontend.variable.BinVar
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.toFlt64
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import org.apache.logging.log4j.kotlin.logger

class OrFunction(
    private val polynomials: List<AbstractLinearPolynomial<*>>,
    override val parent: IntermediateSymbol? = null,
    args: Any? = null,
    override var name: String,
    override var displayName: String? = null
) : LinearLogicFunctionSymbol() {
    private val logger = logger()

    companion object {
        operator fun invoke(
            polynomials: List<ToLinearPolynomial<*>>,
            parent: IntermediateSymbol? = null,
            args: Any? = null,
            name: String,
            displayName: String? = null
        ): OrFunction {
            return OrFunction(
                polynomials = polynomials.map { it.toLinearPolynomial() },
                parent = parent,
                args = args,
                name = name,
                displayName = displayName
            )
        }
    }

    internal val _args = args
    override val args get() = _args ?: parent?.args

    private val y: BinVar by lazy {
        BinVar("${name}_y")
    }

    private val polyY: AbstractLinearPolynomial<*> by lazy {
        val poly = LinearPolynomial(y)
        poly.range.set(ValueRange(Flt64.zero, Flt64.one).value!!)
        poly
    }

    override val discrete = true

    override val range get() = polyY.range
    override val lowerBound get() = polyY.lowerBound
    override val upperBound get() = polyY.upperBound

    override val category = Linear

    override val dependencies: Set<IntermediateSymbol>
        get() {
            val dependencies = HashSet<IntermediateSymbol>()
            for (polynomial in polynomials) {
                dependencies.addAll(polynomial.dependencies)
            }
            return dependencies
        }
    override val cells get() = polyY.cells
    override val cached get() = polyY.cached

    private val possibleRange
        get() = ValueRange(
            if (!polynomials.any { it.lowerBound!!.value.unwrap() eq Flt64.zero }) {
                Flt64.one
            } else {
                Flt64.zero
            },
            if (polynomials.all { it.upperBound!!.value.unwrap() eq Flt64.zero }) {
                Flt64.zero
            } else {
                Flt64.one
            }
        ).value!!

    override fun flush(force: Boolean) {
        for (polynomial in polynomials) {
            polynomial.flush(force)
        }
        polyY.flush(force)
        polyY.range.set(possibleRange)
    }

    override fun prepare(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable): Flt64? {
        return prepareIfNotCached(values, tokenTable) {
            polynomials.forEach { polynomial ->
                val value = if (values.isNullOrEmpty()) {
                    polynomial.evaluate(tokenTable)
                } else {
                    polynomial.evaluate(values, tokenTable)
                } ?: return null
                val bin = value gr Flt64.zero

                if (bin) {
                    logger.trace { "Setting OrFunction ${name}.y initial solution: true" }
                    tokenTable.find(y)?.let { token ->
                        token._result = Flt64.one
                    }
                    return Flt64.one
                }
            }

            logger.trace { "Setting OrFunction ${name}.y initial solution: false" }
            tokenTable.find(y)?.let { token ->
                token._result = Flt64.zero
            }

            Flt64.zero
        }
    }

    override fun register(tokenTable: AddableTokenCollection): Try {
        when (val result = tokenTable.add(y)) {
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

    override fun register(model: AbstractLinearMechanismModel): Try {
        // all polys must be �?(R - R-)
        for (polynomial in polynomials) {
            if (polynomial.lowerBound!!.value.unwrap() ls Flt64.zero) {
                return Failed(
                    code = ErrorCode.ApplicationFailed,
                    message = "$name's domain of definition unsatisfied: $polynomial"
                )
            }
        }

        // if any polynomial is not zero, y will be not zero
        for ((i, polynomial) in polynomials.withIndex()) {
            if (polynomial.upperBound!!.value.unwrap() gr Flt64.one) {
                when (val result = model.addConstraint(
                    relation = y geq (polynomial / polynomial.upperBound!!.value.unwrap()),
                    name = "${name}_lb_${polynomial.name.ifEmpty { "$i" }}",
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
            } else {
                when (val result = model.addConstraint(
                    relation = y geq polynomial,
                    name = "${name}_lb_${polynomial.name.ifEmpty { "$i" }}",
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

        // if all polynomials are zero, y will be zero
        when (val result = model.addConstraint(
            relation = y leq sum(polynomials),
            name = "${name}_ub",
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
        val values = polynomials.map {
            it.evaluate(fixedValues, model.tokens) ?: return register(model)
        }
        val bin = values.any {
            it gr Flt64.zero
        }

        for ((i, polynomial) in polynomials.withIndex()) {
            if (polynomial.upperBound!!.value.unwrap() gr Flt64.one) {
                when (val result = model.addConstraint(
                    relation = y geq (polynomial / polynomial.upperBound!!.value.unwrap()),
                    name = "${name}_lb_${polynomial.name.ifEmpty { "$i" }}",
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
            } else {
                when (val result = model.addConstraint(
                    relation = y geq polynomial,
                    name = "${name}_lb_${polynomial.name.ifEmpty { "$i" }}",
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

        when (val result = model.addConstraint(
            relation = y leq sum(polynomials),
            name = "${name}_ub",
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

        when (val result = model.addConstraint(
            relation = y eq bin.toFlt64(),
            name = "${name}_y",
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

    override fun toString(): String {
        return displayName ?: name
    }

    override fun toRawString(unfold: UInt64): String {
        return if (unfold eq UInt64.zero) {
            displayName ?: name
        } else {
            "or(${polynomials.joinToString(", ") { it.toTidyRawString(unfold - UInt64.one) }})"
        }
    }

    override fun evaluate(
        tokenList: AbstractTokenList,
        zeroIfNone: Boolean
    ): Flt64? {
        return if (polynomials.any {
                val thisValue = it.evaluate(tokenList, zeroIfNone) ?: return null
                thisValue neq Flt64.zero
            }) {
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
        return if (polynomials.any {
                val thisValue = it.evaluate(
                    results = results,
                    tokenList = tokenList,
                    zeroIfNone = zeroIfNone
                ) ?: return null
                thisValue neq Flt64.zero
            }) {
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
        return if (polynomials.any {
                val thisValue = it.evaluate(
                    values = values,
                    tokenList = tokenList,
                    zeroIfNone = zeroIfNone
                ) ?: return null
                thisValue neq Flt64.zero
            }) {
            Flt64.one
        } else {
            Flt64.zero
        }
    }

    override fun calculateValue(
        tokenTable: AbstractTokenTable,
        zeroIfNone: Boolean
    ): Flt64? {
        return if (polynomials.any {
                val thisValue = it.evaluate(tokenTable, zeroIfNone) ?: return null
                thisValue neq Flt64.zero
            }) {
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
        return if (polynomials.any {
                val thisValue = it.evaluate(
                    results = results,
                    tokenTable = tokenTable,
                    zeroIfNone = zeroIfNone
                ) ?: return null
                thisValue neq Flt64.zero
            }) {
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
        return if (polynomials.any {
                val thisValue = it.evaluate(
                    values = values,
                    tokenTable = tokenTable,
                    zeroIfNone = zeroIfNone
                ) ?: return null
                thisValue neq Flt64.zero
            }) {
            Flt64.one
        } else {
            Flt64.zero
        }
    }
}





