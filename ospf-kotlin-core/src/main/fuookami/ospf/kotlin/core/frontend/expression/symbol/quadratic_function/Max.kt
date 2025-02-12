package fuookami.ospf.kotlin.core.frontend.expression.symbol.quadratic_function

import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

sealed class AbstractMaxFunction(
    protected val polynomials: List<AbstractQuadraticPolynomial<*>>,
    private val exact: Boolean = true,
    override var name: String,
    override var displayName: String? = null
) : QuadraticFunctionSymbol {
    private val logger = logger()

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

    private val y: AbstractQuadraticPolynomial<*> by lazy {
        val y = QuadraticPolynomial(minmax)
        y.range.set(m)
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
        get() = ValueRange(
            polynomials.minOf { it.lowerBound!!.value.unwrap() },
            polynomials.maxOf { it.upperBound!!.value.unwrap() }
        ).value!!
    private var m = possibleRange

    override fun flush(force: Boolean) {
        for (polynomial in polynomials) {
            polynomial.flush(force)
        }
        y.flush(force)
        val newM = possibleRange
        if (m neq newM) {
            minmax.range.set(m)
            m = newM
        }
    }

    override fun prepare(tokenTable: AbstractTokenTable) {
        for (polynomial in polynomials) {
            polynomial.cells
        }

        if (tokenTable.cachedSolution && tokenTable.cached(this) == false) {
            val values = polynomials.map { it.evaluate(tokenTable) }

            if (values.all { it != null }) {
                val max = values.withIndex().maxByOrNull { it.value!! } ?: return

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

                tokenTable.cache(this, null, max.value!!)
            }
        }
    }

    override fun register(tokenTable: AbstractMutableTokenTable): Try {
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

    override fun register(model: AbstractQuadraticMechanismModel): Try {
        for ((i, polynomial) in polynomials.withIndex()) {
            when (val result = model.addConstraint(
                minmax geq polynomial,
                "${name}_lb_${polynomial.name.ifEmpty { "$i" }}"
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
                    minmax leq (polynomial + m.upperBound.value.unwrap() * (Flt64.one - u[i])),
                    "${name}_ub_${polynomial.name.ifEmpty { "$i" }}"
                )) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }
                }
            }

            when (val result = model.addConstraint(
                sum(u) eq Flt64.one,
                "${name}_u"
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

    override fun evaluate(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return polynomials.maxOf { it.evaluate(tokenList, zeroIfNone) ?: return null }
    }

    override fun evaluate(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return polynomials.maxOf { it.evaluate(results, tokenList, zeroIfNone) ?: return null }
    }

    override fun calculateValue(tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        return polynomials.maxOf { it.evaluate(tokenTable, zeroIfNone) ?: return null }
    }

    override fun calculateValue(results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        return polynomials.maxOf { it.evaluate(results, tokenTable, zeroIfNone) ?: return null }
    }
}

class MinMaxFunction(
    polynomials: List<AbstractQuadraticPolynomial<*>>,
    name: String,
    displayName: String? = name
) : AbstractMaxFunction(polynomials, true, name, displayName) {
    override fun toRawString(unfold: Boolean): String {
        return "max(${polynomials.joinToString(", ") { it.toRawString(unfold) }})"
    }
}

class MaxFunction(
    polynomials: List<AbstractQuadraticPolynomial<*>>,
    name: String,
    displayName: String? = name
) : AbstractMaxFunction(polynomials, false, name, displayName) {
    override fun toRawString(unfold: Boolean): String {
        return "minmax(${polynomials.joinToString(", ") { it.toRawString(unfold) }})"
    }
}
