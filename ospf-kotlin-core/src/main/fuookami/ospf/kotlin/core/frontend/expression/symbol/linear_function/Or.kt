package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function

import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

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
        for (polynomial in polynomials) {
            polynomial.cells
        }

        return if ((!values.isNullOrEmpty() || tokenTable.cachedSolution) && if (values.isNullOrEmpty()) {
            tokenTable.cached(this)
        } else {
            tokenTable.cached(this, values)
        } == false) {
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
        } else {
            null
        }
    }

    override fun register(tokenTable: AddableTokenCollection): Try {
        when (val result = tokenTable.add(y)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }

    override fun register(model: AbstractLinearMechanismModel): Try {
        // all polys must be ∈ (R - R-)
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
                    constraint = y geq (polynomial / polynomial.upperBound!!.value.unwrap()),
                    name = "${name}_lb_${polynomial.name.ifEmpty { "$i" }}",
                    from = parent ?: this
                )) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }
                }
            } else {
                when (val result = model.addConstraint(
                    constraint = y geq polynomial,
                    name = "${name}_lb_${polynomial.name.ifEmpty { "$i" }}",
                    from = parent ?: this
                )) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }
                }
            }
        }

        // if all polynomials are zero, y will be zero
        when (val result = model.addConstraint(
            constraint = y leq sum(polynomials),
            name = "${name}_ub",
            from = parent ?: this
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
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
                    constraint = y geq (polynomial / polynomial.upperBound!!.value.unwrap()),
                    name = "${name}_lb_${polynomial.name.ifEmpty { "$i" }}",
                    from = parent ?: this
                )) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }
                }
            } else {
                when (val result = model.addConstraint(
                    constraint = y geq polynomial,
                    name = "${name}_lb_${polynomial.name.ifEmpty { "$i" }}",
                    from = parent ?: this
                )) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }
                }
            }
        }

        when (val result = model.addConstraint(
            constraint = y leq sum(polynomials),
            name = "${name}_ub",
            from = parent ?: this
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = model.addConstraint(
            constraint = y eq bin.toFlt64(),
            name = "${name}_y",
            from = parent ?: this
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