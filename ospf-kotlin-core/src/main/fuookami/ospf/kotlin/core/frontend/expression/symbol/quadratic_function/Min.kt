package fuookami.ospf.kotlin.core.frontend.expression.symbol.quadratic_function

import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

sealed class AbstractMinFunction(
    protected val polynomials: List<AbstractQuadraticPolynomial<*>>,
    private val exact: Boolean = true,
    override val parent: IntermediateSymbol? = null,
    args: Any? = null,
    override var name: String,
    override var displayName: String? = null
) : QuadraticFunctionSymbol() {
    private val logger = logger()

    internal val _args = args
    override val args get() = _args ?: parent?.args

    private val maxmin: RealVar by lazy {
        RealVar("${name}_maxmin")
    }

    private val u: BinVariable1 by lazy {
        val u = BinVariable1("${name}_u", Shape1(polynomials.size))
        for ((i, polynomial) in polynomials.withIndex()) {
            u[i].name = "${u.name}_${polynomial.name.ifEmpty { "$i" }}"
        }
        u
    }

    private val y: AbstractQuadraticPolynomial<*> by lazy {
        val y = QuadraticPolynomial(maxmin, "${name}_y")
        y.range.set(possibleRange)
        y
    }

    override val discrete by lazy {
        polynomials.all { it.discrete }
    }

    override val range get() = y.range
    override val lowerBound get() = y.lowerBound
    override val upperBound get() = y.upperBound

    override val category = Linear

    override val dependencies: Set<IntermediateSymbol>
        get() {
            val dependencies = HashSet<IntermediateSymbol>()
            for (polynomial in polynomials) {
                dependencies.addAll(polynomial.dependencies)
            }
            return dependencies
        }
    override val cells get() = y.cells
    override val cached get() = y.cached

    private val possibleRange
        get() = if (polynomials.isNotEmpty()) {
            ValueRange(
                polynomials.minOf { it.lowerBound!!.value.unwrap() },
                polynomials.maxOf { it.upperBound!!.value.unwrap() }
            ).value!!
        } else {
            ValueRange(Flt64.zero, Flt64.zero).value!!
        }
    private var m = max(abs(possibleRange.lowerBound.value.unwrap()), abs(possibleRange.upperBound.value.unwrap()))

    override fun flush(force: Boolean) {
        for (polynomial in polynomials) {
            polynomial.flush(force)
        }
        y.flush(force)
        val newM = max(abs(possibleRange.lowerBound.value.unwrap()), abs(possibleRange.upperBound.value.unwrap()))
        if (m neq newM) {
            maxmin.range.set(possibleRange)
            m = newM
        }
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
            val values = polynomials.map {
                if (values.isNullOrEmpty()) {
                    it.evaluate(tokenTable)
                } else {
                    it.evaluate(values, tokenTable)
                }
            }

            return if (values.all { it != null }) {
                val min = values.withIndex().minByOrNull { it.value!! } ?: return null

                logger.trace { "Setting MinFunction ${name}.maxmin to ${min.value}" }
                tokenTable.find(maxmin)?.let { token ->
                    token._result = min.value!!
                }

                if (exact) {
                    for (i in polynomials.indices) {
                        if (i == min.index) {
                            logger.trace { "Setting MinFunction ${name}.u[$i] to true" }
                            tokenTable.find(u[i])?.let { token ->
                                token._result = Flt64.one
                            }
                        } else {
                            logger.trace { "Setting MinFunction ${name}.u[$i] to false" }
                            tokenTable.find(u[i])?.let { token ->
                                token._result = Flt64.zero
                            }
                        }
                    }
                }

                min.value!!
            } else {
                null
            }
        } else {
            null
        }
    }

    override fun register(tokenTable: AddableTokenCollection): Try {
        when (val result = tokenTable.add(maxmin)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        if (exact) {
            when (val result = tokenTable.add(u)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        return ok
    }

    override fun register(model: AbstractQuadraticMechanismModel): Try {
        for ((i, polynomial) in polynomials.withIndex()) {
            when (val result = model.addConstraint(
                constraint = maxmin leq polynomial,
                name = "${name}_lb_${polynomial.name.ifEmpty { "$i" }}",
                from = parent ?: this
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        if (exact) {
            for ((i, polynomial) in polynomials.withIndex()) {
                when (val result = model.addConstraint(
                    constraint = maxmin geq (polynomial - m * (Flt64.one - u[i])),
                    name = "${name}_ub_${polynomial.name.ifEmpty { "$i" }}",
                    from = parent ?: this
                )) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }
                }
            }

            when (val result = model.addConstraint(
                constraint = sum(u) eq Flt64.one,
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
        tokenTable: AddableTokenCollection,
        fixedValues: Map<Symbol, Flt64>,
    ): Try {
        val i = if (exact) {
            val values = polynomials.map {
                when (tokenTable) {
                    is AbstractTokenTable -> {
                        it.evaluate(fixedValues, tokenTable) ?: return register(tokenTable)
                    }

                    is FunctionSymbolRegistrationScope -> {
                        it.evaluate(fixedValues, tokenTable.origin) ?: return register(tokenTable)
                    }

                    else -> {
                        return register(tokenTable)
                    }
                }
            }
            values.withIndex().minBy { it.value }.index
        } else {
            null
        }

        when (val result = tokenTable.add(maxmin)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        if (exact) {
            when (val result = tokenTable.add(u[i!!])) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        return ok
    }

    override fun register(
        model: AbstractQuadraticMechanismModel,
        fixedValues: Map<Symbol, Flt64>
    ): Try {
        val values = polynomials.map {
            it.evaluate(fixedValues, model.tokens) ?: return register(model)
        }
        val (index, minValue) = values.withIndex().minBy { it.value }

        for ((i, polynomial) in polynomials.withIndex()) {
            when (val result = model.addConstraint(
                constraint = maxmin leq polynomial,
                name = "${name}_lb_${polynomial.name.ifEmpty { "$i" }}",
                from = parent ?: this
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        when (val result = model.addConstraint(
            constraint = maxmin eq minValue,
            name = "${name}_min",
            from = parent ?: this
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        model.tokens.find(maxmin)?.let { token ->
            token._result = minValue
        }

        if (exact) {
            for ((i, polynomial) in polynomials.withIndex()) {
                if (i == index) {
                    when (val result = model.addConstraint(
                        constraint = maxmin geq polynomial,
                        name = "${name}_ub_${polynomial.name.ifEmpty { "$i" }}",
                        from = parent ?: this
                    )) {
                        is Ok -> {}

                        is Failed -> {
                            return Failed(result.error)
                        }
                    }

                    when (val result = model.addConstraint(
                        constraint = u[i] eq Flt64.one,
                        name = "${name}_u_${polynomial.name.ifEmpty { "$i" }}",
                        from = parent ?: this
                    )) {
                        is Ok -> {}

                        is Failed -> {
                            return Failed(result.error)
                        }
                    }

                    model.tokens.find(u[i])?.let { token ->
                        token._result = Flt64.one
                    }
                } else {
                    when (val result = model.addConstraint(
                        constraint = maxmin geq (polynomial - m),
                        name = "${name}_ub_${polynomial.name.ifEmpty { "$i" }}",
                        from = parent ?: this
                    )) {
                        is Ok -> {}

                        is Failed -> {
                            return Failed(result.error)
                        }
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
        return polynomials.minOfOrNull {
            it.evaluate(tokenList, zeroIfNone) ?: return null
        } ?: Flt64.zero
    }

    override fun evaluate(
        results: List<Flt64>,
        tokenList: AbstractTokenList,
        zeroIfNone: Boolean
    ): Flt64? {
        return polynomials.minOfOrNull {
            it.evaluate(
                results = results,
                tokenList = tokenList,
                zeroIfNone = zeroIfNone
            ) ?: return null
        } ?: Flt64.zero
    }

    override fun evaluate(
        values: Map<Symbol, Flt64>,
        tokenList: AbstractTokenList?,
        zeroIfNone: Boolean
    ): Flt64? {
        return polynomials.minOfOrNull {
            it.evaluate(
                values = values,
                tokenList = tokenList,
                zeroIfNone = zeroIfNone
            ) ?: return null
        } ?: Flt64.zero
    }

    override fun calculateValue(
        tokenTable: AbstractTokenTable,
        zeroIfNone: Boolean
    ): Flt64? {
        return polynomials.minOfOrNull {
            it.evaluate(tokenTable, zeroIfNone) ?: return null
        } ?: Flt64.zero
    }

    override fun calculateValue(
        results: List<Flt64>,
        tokenTable: AbstractTokenTable,
        zeroIfNone: Boolean
    ): Flt64? {
        return polynomials.minOfOrNull {
            it.evaluate(
                results = results,
                tokenTable = tokenTable,
                zeroIfNone = zeroIfNone
            ) ?: return null
        } ?: Flt64.zero
    }

    override fun calculateValue(
        values: Map<Symbol, Flt64>,
        tokenTable: AbstractTokenTable?,
        zeroIfNone: Boolean
    ): Flt64? {
        return polynomials.minOfOrNull {
            it.evaluate(
                values = values,
                tokenTable = tokenTable,
                zeroIfNone = zeroIfNone
            ) ?: return null
        } ?: Flt64.zero
    }
}

class MaxMinFunction(
    polynomials: List<AbstractQuadraticPolynomial<*>>,
    parent: IntermediateSymbol? = null,
    args: Any? = null,
    name: String,
    displayName: String? = null
) : AbstractMinFunction(
    polynomials = polynomials,
    exact = true,
    parent = parent,
    args = args,
    name = name,
    displayName = displayName
) {
    companion object {
        operator fun invoke(
            polynomials: List<ToQuadraticPolynomial<*>>,
            parent: IntermediateSymbol? = null,
            args: Any? = null,
            name: String,
            displayName: String? = null
        ): MaxMinFunction {
            return MaxMinFunction(
                polynomials = polynomials.map { it.toQuadraticPolynomial() },
                parent = parent,
                args = args,
                name = name,
                displayName = displayName
            )
        }
    }

    override fun toRawString(unfold: UInt64): String {
        return if (unfold eq UInt64.zero) {
            displayName ?: name
        } else {
            "maxmin(${polynomials.joinToString(", ") { it.toTidyRawString(unfold - UInt64.one) }})"
        }
    }
}

class MinFunction(
    polynomials: List<AbstractQuadraticPolynomial<*>>,
    parent: IntermediateSymbol? = null,
    args: Any? = null,
    name: String,
    displayName: String? = null
) : AbstractMinFunction(
    polynomials = polynomials,
    exact = false,
    parent = parent,
    args = args,
    name = name,
    displayName = displayName
) {
    companion object {
        operator fun invoke(
            polynomials: List<ToQuadraticPolynomial<*>>,
            parent: IntermediateSymbol? = null,
            args: Any? = null,
            name: String,
            displayName: String? = null
        ): MinFunction {
            return MinFunction(
                polynomials = polynomials.map { it.toQuadraticPolynomial() },
                parent = parent,
                args = args,
                name = name,
                displayName = displayName
            )
        }
    }

    override fun toRawString(unfold: UInt64): String {
        return if (unfold eq UInt64.zero) {
            displayName ?: name
        } else {
            "min(${polynomials.joinToString(", ") { it.toTidyRawString(unfold - UInt64.one) }})"
        }
    }
}