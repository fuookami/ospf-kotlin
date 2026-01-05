package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function

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

sealed class AbstractMaxFunction(
    protected val polynomials: List<AbstractLinearPolynomial<*>>,
    private val exact: Boolean = true,
    override val parent: IntermediateSymbol? = null,
    args: Any? = null,
    override var name: String,
    override var displayName: String? = null
) : LinearFunctionSymbol() {
    private val _args = args
    override val args = _args ?: parent?.args

    private val minmax: RealVar by lazy {
        RealVar("${name}_y")
    }

    private val u: BinVariable1 by lazy {
        val u = BinVariable1("${name}_u", Shape1(polynomials.size))
        for ((i, polynomial) in polynomials.withIndex()) {
            u[i].name = "${u.name}_${polynomial.name.ifEmpty { "$i" }}"
        }
        u
    }

    private val y: AbstractLinearPolynomial<*> by lazy {
        val y = LinearPolynomial(minmax)
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

        val newM = max(abs(possibleRange.lowerBound.value.unwrap()), abs(possibleRange.upperBound.value.unwrap()))
        if (m neq newM) {
            minmax.range.set(possibleRange)
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

            if (values.all { it != null }) {
                val max = values.withIndex().maxByOrNull { it.value!! } ?: return null

                logger.trace { "Setting MaxFunction ${name}.minmax to ${max.value}" }
                tokenTable.find(minmax)?.let { token ->
                    token._result = max.value!!
                }

                if (exact) {
                    for (i in polynomials.indices) {
                        if (i == max.index) {
                            logger.trace { "Setting MaxFunction ${name}.u[$i] to true" }
                            tokenTable.find(u[i])?.let { token ->
                                token._result = Flt64.one
                            }
                        } else {
                            logger.trace { "Setting MaxFunction ${name}.u[$i] to false" }
                            tokenTable.find(u[i])?.let { token ->
                                token._result = Flt64.zero
                            }
                        }
                    }
                }

                max.value!!
            } else {
                null
            }
        } else {
            null
        }
    }

    override fun register(tokenTable: AddableTokenCollection): Try {
        when (val result = tokenTable.add(minmax)) {
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

    override fun register(model: AbstractLinearMechanismModel): Try {
        for ((i, polynomial) in polynomials.withIndex()) {
            when (val result = model.addConstraint(
                minmax geq polynomial,
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
                    minmax leq (polynomial + m * (Flt64.one - u[i])),
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
                sum(u) eq Flt64.one,
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
        fixedValues: Map<Symbol, Flt64>
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
            values.withIndex().maxBy { it.value }.index
        } else {
            null
        }

        when (val result = tokenTable.add(minmax)) {
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
        model: AbstractLinearMechanismModel,
        fixedValues: Map<Symbol, Flt64>
    ): Try {
        val values = polynomials.map {
            it.evaluate(fixedValues, model.tokens) ?: return register(model)
        }
        val (index, maxValue) = values.withIndex().maxBy { it.value }

        for ((i, polynomial) in polynomials.withIndex()) {
            when (val result = model.addConstraint(
                minmax geq polynomial,
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
            minmax eq maxValue,
            name = "${name}_max",
            from = parent ?: this
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        model.tokens.find(minmax)?.let { token ->
            token._result = maxValue
        }

        if (exact) {
            for ((i, polynomial) in polynomials.withIndex()) {
                if (i == index) {
                    when (val result = model.addConstraint(
                        minmax leq polynomial,
                        name = "${name}_ub_${polynomial.name.ifEmpty { "$i" }}",
                        from = parent ?: this
                    )) {
                        is Ok -> {}

                        is Failed -> {
                            return Failed(result.error)
                        }
                    }

                    when (val result = model.addConstraint(
                        u[i] eq Flt64.one,
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
                        minmax leq (polynomial + m),
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
        return polynomials.maxOfOrNull {
            it.evaluate(tokenList, zeroIfNone) ?: return null
        } ?: Flt64.zero
    }

    override fun evaluate(
        results: List<Flt64>,
        tokenList: AbstractTokenList,
        zeroIfNone: Boolean
    ): Flt64? {
        return polynomials.maxOfOrNull {
            it.evaluate(results, tokenList, zeroIfNone) ?: return null
        } ?: Flt64.zero
    }

    override fun evaluate(
        values: Map<Symbol, Flt64>,
        tokenList: AbstractTokenList?,
        zeroIfNone: Boolean
    ): Flt64? {
        return polynomials.maxOfOrNull {
            it.evaluate(values, tokenList, zeroIfNone) ?: return null
        } ?: Flt64.zero
    }

    override fun calculateValue(
        tokenTable: AbstractTokenTable,
        zeroIfNone: Boolean
    ): Flt64? {
        return polynomials.maxOfOrNull {
            it.evaluate(tokenTable, zeroIfNone) ?: return null
        } ?: Flt64.zero
    }

    override fun calculateValue(
        results: List<Flt64>,
        tokenTable: AbstractTokenTable,
        zeroIfNone: Boolean
    ): Flt64? {
        return polynomials.maxOfOrNull {
            it.evaluate(results, tokenTable, zeroIfNone) ?: return null
        } ?: Flt64.zero
    }

    override fun calculateValue(
        values: Map<Symbol, Flt64>,
        tokenTable: AbstractTokenTable?,
        zeroIfNone: Boolean
    ): Flt64? {
        return polynomials.maxOfOrNull {
            it.evaluate(values, tokenTable, zeroIfNone) ?: return null
        } ?: Flt64.zero
    }
}

class MinMaxFunction(
    polynomials: List<AbstractLinearPolynomial<*>>,
    parent: IntermediateSymbol? = null,
    args: Any? = null,
    name: String,
    displayName: String? = name
) : AbstractMaxFunction(
    polynomials = polynomials,
    exact = true,
    parent = parent,
    args = args,
    name = name,
    displayName = displayName
) {
    companion object {
        operator fun invoke(
            polynomials: List<ToLinearPolynomial<*>>,
            parent: IntermediateSymbol? = null,
            args: Any? = null,
            name: String,
            displayName: String? = name
        ): MinMaxFunction {
            return MinMaxFunction(
                polynomials = polynomials.map { it.toLinearPolynomial() },
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
            "minmax(${polynomials.joinToString(", ") { it.toTidyRawString(unfold - UInt64.one) }})"
        }
    }
}

class MaxFunction(
    polynomials: List<AbstractLinearPolynomial<*>>,
    parent: IntermediateSymbol? = null,
    args: Any? = null,
    name: String,
    displayName: String? = name
) : AbstractMaxFunction(
    polynomials = polynomials,
    exact = false,
    parent = parent,
    args = args,
    name = name,
    displayName = displayName
 ) {
    companion object {
        operator fun invoke(
            polynomials: List<ToLinearPolynomial<*>>,
            parent: IntermediateSymbol? = null,
            args: Any? = null,
            name: String,
            displayName: String? = name
        ): MaxFunction {
            return MaxFunction(
                polynomials = polynomials.map { it.toLinearPolynomial() },
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
            "max(${polynomials.joinToString(", ") { it.toTidyRawString(unfold - UInt64.one) }})"
        }
    }
}
