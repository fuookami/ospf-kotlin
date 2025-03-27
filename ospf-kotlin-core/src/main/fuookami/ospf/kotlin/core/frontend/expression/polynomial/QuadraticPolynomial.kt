package fuookami.ospf.kotlin.core.frontend.expression.polynomial

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.physics.unit.*
import fuookami.ospf.kotlin.utils.physics.quantity.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*

@JvmName("calculateQuadraticPolynomialCells")
private fun cells(
    monomials: List<QuadraticMonomial>,
    constant: Flt64
): List<QuadraticMonomialCell> {
    val cells = HashMap<Pair<AbstractVariableItem<*, *>, AbstractVariableItem<*, *>?>, Flt64>()
    var totalConstant = constant
    for (monomial in monomials) {
        val thisCells = monomial.cells
        for (cell in thisCells) {
            if (cell.isConstant) {
                totalConstant += cell.constant!!
            } else {
                val key = cell.triple!!.variable1 to cell.triple!!.variable2
                cells[key] = (cells[key] ?: Flt64.zero) + cell.triple!!.coefficient
            }
        }
    }
    return cells.map { QuadraticMonomialCell(it.value, it.key.first, it.key.second) } + QuadraticMonomialCell(totalConstant)
}

sealed class AbstractQuadraticPolynomial<Self : AbstractQuadraticPolynomial<Self>> :
    Polynomial<Self, QuadraticMonomial, QuadraticMonomialCell> {
    abstract override val monomials: List<QuadraticMonomial>
    override val category: Category get() = monomials.map { it.category }.maxOrNull() ?: Linear

    private var _range: ExpressionRange<Flt64>? = null
    override val range: ExpressionRange<Flt64>
        get() {
            if (_range == null) {
                _range = ExpressionRange(possibleRange(monomials, constant), Flt64)
            }
            return _range!!
        }

    override val dependencies: Set<IntermediateSymbol>
        get() {
            return monomials.flatMapNotNull {
                val symbols = ArrayList<IntermediateSymbol>()
                when (val symbol = it.symbol.symbol1) {
                    is Variant3.V2 -> {
                        symbols.add(symbol.value)
                    }

                    is Variant3.V3 -> {
                        symbols.add(symbol.value)
                    }

                    else -> {}
                }
                when (val symbol = it.symbol.symbol2) {
                    is Variant3.V2 -> {
                        symbols.add(symbol.value)
                    }

                    is Variant3.V3 -> {
                        symbols.add(symbol.value)
                    }

                    else -> {}
                }
                symbols
            }.toSet()
        }

    private var _cells: List<QuadraticMonomialCell> = emptyList()
    override val cells: List<QuadraticMonomialCell>
        get() {
            if (_cells.isEmpty()) {
                _cells = cells(monomials, constant)
            }
            return _cells
        }
    override val cached: Boolean = _cells.isNotEmpty()

    abstract operator fun plus(rhs: LinearIntermediateSymbol): Self

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("plusLinearSymbols")
    abstract operator fun plus(rhs: Iterable<LinearIntermediateSymbol>): Self
    abstract operator fun plus(rhs: QuadraticIntermediateSymbol): Self

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("plusQuadraticSymbols")
    abstract operator fun plus(rhs: Iterable<QuadraticIntermediateSymbol>): Self
    abstract operator fun plus(rhs: LinearMonomial): Self
    abstract operator fun plus(rhs: AbstractLinearPolynomial<*>): Self

    abstract operator fun minus(rhs: LinearIntermediateSymbol): Self

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("minusLinearSymbols")
    abstract operator fun minus(rhs: Iterable<LinearIntermediateSymbol>): Self
    abstract operator fun minus(rhs: QuadraticIntermediateSymbol): Self

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("minusQuadraticSymbols")
    abstract operator fun minus(rhs: Iterable<QuadraticIntermediateSymbol>): Self
    abstract operator fun minus(rhs: LinearMonomial): Self
    abstract operator fun minus(rhs: AbstractLinearPolynomial<*>): Self

    abstract operator fun times(rhs: AbstractVariableItem<*, *>): Self

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("timesVariables")
    abstract operator fun times(rhs: Iterable<AbstractVariableItem<*, *>>): Self

    abstract operator fun times(rhs: LinearIntermediateSymbol): Self

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("timesLinearSymbols")
    abstract operator fun times(rhs: Iterable<LinearIntermediateSymbol>): Self

    abstract operator fun times(rhs: QuadraticIntermediateSymbol): Self

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("timesQuadraticSymbols")
    abstract operator fun times(rhs: Iterable<QuadraticIntermediateSymbol>): Self
    abstract operator fun times(rhs: LinearMonomial): Self
    abstract operator fun times(rhs: QuadraticMonomial): Self
    abstract operator fun times(rhs: AbstractLinearPolynomial<*>): Self
    abstract operator fun times(rhs: AbstractQuadraticPolynomial<*>): Self

    override fun toMutable(): MutableQuadraticPolynomial {
        return MutableQuadraticPolynomial(
            monomials = monomials.map { it.copy() }.toMutableList(),
            constant = constant
        )
    }

    override fun flush(force: Boolean) {
        if (force || _range?.set == false) {
            _range = null
        }
        for (monomial in monomials) {
            monomial.flush(force)
        }
        if (force || monomials.any { !it.cached }) {
            _cells = emptyList()
        }
    }

    override fun toString(): String {
        return displayName
            ?: name.ifEmpty {
                if (monomials.isEmpty()) {
                    "$constant"
                } else if (constant eq Flt64.zero) {
                    monomials.joinToString(" + ") { it.toString() }
                } else {
                    "${monomials.joinToString(" + ") { it.toString() }} + $constant"
                }
            }
    }
}

class QuadraticPolynomial(
    override val monomials: List<QuadraticMonomial> = emptyList(),
    override val constant: Flt64 = Flt64.zero,
    override var name: String = "",
    override var displayName: String? = null
) : AbstractQuadraticPolynomial<QuadraticPolynomial>() {
    companion object {
        operator fun invoke(
            item: AbstractVariableItem<*, *>,
            name: String = "",
            displayName: String? = null
        ): QuadraticPolynomial {
            return QuadraticPolynomial(
                monomials = listOf(QuadraticMonomial(item)),
                name = name.ifEmpty { item.name },
                displayName = displayName
            )
        }

        operator fun invoke(
            symbol: LinearIntermediateSymbol,
            name: String = "",
            displayName: String? = null
        ): QuadraticPolynomial {
            return QuadraticPolynomial(
                monomials = listOf(QuadraticMonomial(symbol)),
                name = name.ifEmpty { symbol.name },
                displayName = displayName ?: symbol.displayName
            )
        }

        operator fun invoke(
            symbol: QuadraticIntermediateSymbol,
            name: String = "",
            displayName: String? = null
        ): QuadraticPolynomial {
            return QuadraticPolynomial(
                monomials = listOf(QuadraticMonomial(symbol)),
                name = name.ifEmpty { symbol.name },
                displayName = displayName ?: symbol.displayName
            )
        }

        operator fun invoke(
            monomial: LinearMonomial,
            name: String = "",
            displayName: String? = null
        ): QuadraticPolynomial {
            return QuadraticPolynomial(
                monomials = listOf(QuadraticMonomial(monomial)),
                name = name.ifEmpty { monomial.name },
                displayName = displayName ?: monomial.displayName
            )
        }

        operator fun invoke(
            monomial: QuadraticMonomial,
            name: String = "",
            displayName: String? = null
        ): QuadraticPolynomial {
            return QuadraticPolynomial(
                monomials = listOf(monomial),
                name = name.ifEmpty { monomial.name },
                displayName = displayName ?: monomial.displayName
            )
        }

        operator fun invoke(
            polynomial: AbstractLinearPolynomial<*>,
            name: String = "",
            displayName: String? = null
        ): QuadraticPolynomial {
            return QuadraticPolynomial(
                monomials = polynomial.monomials.map { QuadraticMonomial(it) },
                constant = polynomial.constant,
                name = name.ifEmpty { polynomial.name },
                displayName = displayName ?: polynomial.displayName
            )
        }

        operator fun invoke(
            polynomial: AbstractQuadraticPolynomial<*>,
            name: String = "",
            displayName: String? = null
        ): QuadraticPolynomial {
            return QuadraticPolynomial(
                monomials = polynomial.monomials.map { it.copy() },
                constant = polynomial.constant,
                name = name.ifEmpty { polynomial.name },
                displayName = displayName ?: polynomial.displayName
            )
        }

        operator fun invoke(
            constant: Int,
            name: String = "",
            displayName: String? = null
        ): QuadraticPolynomial {
            return QuadraticPolynomial(
                monomials = emptyList(),
                constant = Flt64(constant),
                name = name,
                displayName = displayName
            )
        }

        operator fun invoke(
            constant: Double,
            name: String = "",
            displayName: String? = null
        ): QuadraticPolynomial {
            return QuadraticPolynomial(
                monomials = emptyList(),
                constant = Flt64(constant),
                name = name,
                displayName = displayName
            )
        }

        operator fun invoke(
            constant: Boolean,
            name: String = "",
            displayName: String? = null
        ): QuadraticPolynomial {
            return QuadraticPolynomial(
                monomials = emptyList(),
                constant = if (constant) Flt64.one else Flt64.zero,
                name = name,
                displayName = displayName
            )
        }

        operator fun invoke(
            constant: Trivalent,
            name: String = "",
            displayName: String? = null
        ): QuadraticPolynomial {
            return QuadraticPolynomial(
                monomials = emptyList(),
                constant = constant.value.toFlt64(),
                name = name,
                displayName = displayName
            )
        }

        operator fun invoke(
            constant: BalancedTrivalent,
            name: String = "",
            displayName: String? = null
        ): QuadraticPolynomial {
            return QuadraticPolynomial(
                monomials = emptyList(),
                constant = constant.value.toFlt64(),
                name = name,
                displayName = displayName
            )
        }

        operator fun <T : RealNumber<T>> invoke(
            constant: T,
            name: String = "",
            displayName: String? = null
        ): QuadraticPolynomial {
            return QuadraticPolynomial(
                monomials = emptyList(),
                constant = constant.toFlt64(),
                name = name,
                displayName = displayName
            )
        }
    }

    override val discrete by lazy {
        monomials.all { it.discrete } && constant.round() eq constant
    }

    override fun copy(): QuadraticPolynomial {
        return QuadraticPolynomial(
            monomials = monomials.map { it.copy() },
            constant = constant
        )
    }

    override fun copy(name: String, displayName: String?): QuadraticPolynomial {
        return QuadraticPolynomial(
            monomials = monomials.map { it.copy() },
            constant = constant,
            name = name,
            displayName = displayName
        )
    }

    override fun plus(rhs: AbstractVariableItem<*, *>): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(QuadraticMonomial(rhs))
        return QuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("plusVariables")
    override fun plus(rhs: Iterable<AbstractVariableItem<*, *>>): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.map { QuadraticMonomial(it) })
        return QuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    override fun plus(rhs: LinearIntermediateSymbol): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(QuadraticMonomial(rhs))
        return QuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("plusLinearSymbols")
    override fun plus(rhs: Iterable<LinearIntermediateSymbol>): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.map { QuadraticMonomial(it) })
        return QuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    override fun plus(rhs: QuadraticIntermediateSymbol): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(QuadraticMonomial(rhs))
        return QuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("plusQuadraticSymbols")
    override fun plus(rhs: Iterable<QuadraticIntermediateSymbol>): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.map { QuadraticMonomial(it) })
        return QuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    override fun plus(rhs: LinearMonomial): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(QuadraticMonomial(rhs))
        return QuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    override fun plus(rhs: QuadraticMonomial): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(rhs.copy())
        return QuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    override fun plus(rhs: AbstractLinearPolynomial<*>): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.monomials.map { QuadraticMonomial(it) })
        return QuadraticPolynomial(
            monomials = newMonomials,
            constant = constant + rhs.constant
        )
    }

    override fun plus(rhs: Polynomial<*, QuadraticMonomial, QuadraticMonomialCell>): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.monomials.map { it.copy() })
        return QuadraticPolynomial(
            monomials = newMonomials,
            constant = constant + rhs.constant
        )
    }

    override fun plus(rhs: Flt64): QuadraticPolynomial {
        return QuadraticPolynomial(
            monomials = monomials.map { it.copy() },
            constant = constant + rhs
        )
    }

    override fun minus(rhs: AbstractVariableItem<*, *>): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(QuadraticMonomial(-Flt64.one, rhs))
        return QuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("minusVariables")
    override fun minus(rhs: Iterable<AbstractVariableItem<*, *>>): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.map { QuadraticMonomial(-Flt64.one, it) })
        return QuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    override fun minus(rhs: LinearIntermediateSymbol): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(QuadraticMonomial(-Flt64.one, rhs))
        return QuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("minusLinearSymbols")
    override fun minus(rhs: Iterable<LinearIntermediateSymbol>): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.map { QuadraticMonomial(-Flt64.one, it) })
        return QuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    override fun minus(rhs: QuadraticIntermediateSymbol): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(QuadraticMonomial(-Flt64.one, rhs))
        return QuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("minusQuadraticSymbols")
    override fun minus(rhs: Iterable<QuadraticIntermediateSymbol>): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.map { QuadraticMonomial(-Flt64.one, it) })
        return QuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    override fun minus(rhs: LinearMonomial): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(QuadraticMonomial(-rhs))
        return QuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    override fun minus(rhs: QuadraticMonomial): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(-rhs)
        return QuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    override fun minus(rhs: AbstractLinearPolynomial<*>): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.monomials.map { QuadraticMonomial(-it) })
        return QuadraticPolynomial(
            monomials = newMonomials,
            constant = constant - rhs.constant
        )
    }

    override fun minus(rhs: Polynomial<*, QuadraticMonomial, QuadraticMonomialCell>): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.monomials.map { -it })
        return QuadraticPolynomial(
            monomials = newMonomials,
            constant = constant - rhs.constant
        )
    }

    override fun minus(rhs: Flt64): QuadraticPolynomial {
        return QuadraticPolynomial(
            monomials = monomials.map { it.copy() },
            constant = constant - rhs
        )
    }

    @Throws(IllegalArgumentException::class)
    override fun times(rhs: AbstractVariableItem<*, *>): QuadraticPolynomial {
        if (this.category == Quadratic) {
            throw IllegalArgumentException("Invalid argument of QuadraticPolynomial.times: over quadratic.")
        }

        val newMonomials = monomials.map { it * rhs }.toMutableList()
        newMonomials.add(QuadraticMonomial(constant, rhs))
        return QuadraticPolynomial(monomials = newMonomials)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("timesVariables")
    @Throws(IllegalArgumentException::class)
    override fun times(rhs: Iterable<AbstractVariableItem<*, *>>): QuadraticPolynomial {
        if (this.category == Quadratic) {
            throw IllegalArgumentException("Invalid argument of QuadraticPolynomial.times: over quadratic.")
        }

        val newMonomials = monomials.flatMap { monomial -> rhs.map { monomial * it } }.toMutableList()
        newMonomials.addAll(rhs.map { QuadraticMonomial(constant, it) })
        return QuadraticPolynomial(monomials = newMonomials)
    }

    @Throws(IllegalArgumentException::class)
    override fun times(rhs: LinearIntermediateSymbol): QuadraticPolynomial {
        if (this.category == Quadratic) {
            throw IllegalArgumentException("Invalid argument of QuadraticPolynomial.times: over quadratic.")
        }

        val newMonomials = monomials.map { it * rhs }.toMutableList()
        newMonomials.add(QuadraticMonomial(constant, rhs))
        return QuadraticPolynomial(monomials = newMonomials)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("timesLinearSymbols")
    @Throws(IllegalArgumentException::class)
    override fun times(rhs: Iterable<LinearIntermediateSymbol>): QuadraticPolynomial {
        if (this.category == Quadratic) {
            throw IllegalArgumentException("Invalid argument of QuadraticPolynomial.times: over quadratic.")
        }

        val newMonomials = monomials.flatMap { monomial -> rhs.map { monomial * it } }.toMutableList()
        newMonomials.addAll(rhs.map { QuadraticMonomial(constant, it) })
        return QuadraticPolynomial(monomials = newMonomials)
    }

    @Throws(IllegalArgumentException::class)
    override fun times(rhs: QuadraticIntermediateSymbol): QuadraticPolynomial {
        if (this.category == Quadratic || rhs.category == Quadratic) {
            throw IllegalArgumentException("Invalid argument of QuadraticPolynomial.times: over quadratic.")
        }

        val newMonomials = monomials.map { it * rhs }.toMutableList()
        newMonomials.add(QuadraticMonomial(constant, rhs))
        return QuadraticPolynomial(monomials = newMonomials)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("timesQuadraticSymbols")
    @Throws(IllegalArgumentException::class)
    override fun times(rhs: Iterable<QuadraticIntermediateSymbol>): QuadraticPolynomial {
        if (this.category == Quadratic || rhs.any { it.category == Quadratic }) {
            throw IllegalArgumentException("Invalid argument of QuadraticPolynomial.times: over quadratic.")
        }

        val newMonomials = monomials.flatMap { monomial -> rhs.map { monomial * it } }.toMutableList()
        newMonomials.addAll(rhs.map { QuadraticMonomial(constant, it) })
        return QuadraticPolynomial(monomials = newMonomials)
    }

    @Throws(IllegalArgumentException::class)
    override fun times(rhs: LinearMonomial): QuadraticPolynomial {
        if (this.category == Quadratic) {
            throw IllegalArgumentException("Invalid argument of QuadraticPolynomial.times: over quadratic.")
        }

        val newMonomials = monomials.map { it * rhs }.toMutableList()
        newMonomials.add(QuadraticMonomial(constant * rhs))
        return QuadraticPolynomial(monomials = newMonomials)
    }

    @Throws(IllegalArgumentException::class)
    override fun times(rhs: QuadraticMonomial): QuadraticPolynomial {
        if (this.category == Quadratic || rhs.category == Quadratic) {
            throw IllegalArgumentException("Invalid argument of QuadraticPolynomial.times: over quadratic.")
        }

        val newMonomials = monomials.map { it * rhs }.toMutableList()
        newMonomials.add(constant * rhs)
        return QuadraticPolynomial(monomials = newMonomials)
    }

    @Throws(IllegalArgumentException::class)
    override fun times(rhs: AbstractLinearPolynomial<*>): QuadraticPolynomial {
        if (this.category == Quadratic) {
            throw IllegalArgumentException("Invalid argument of QuadraticPolynomial.times: over quadratic.")
        }

        val newMonomials = monomials.flatMap { lhs -> rhs.monomials.map { monomial -> lhs * monomial } }.toMutableList()
        newMonomials.addAll(monomials.map { rhs.constant * it })
        newMonomials.addAll(rhs.monomials.map { QuadraticMonomial(constant * it) })
        return QuadraticPolynomial(
            monomials = newMonomials,
            constant = this.constant * rhs.constant
        )
    }

    @Throws(IllegalArgumentException::class)
    override fun times(rhs: AbstractQuadraticPolynomial<*>): QuadraticPolynomial {
        if (this.category == Quadratic || rhs.category == Quadratic) {
            throw IllegalArgumentException("Invalid argument of QuadraticPolynomial.times: over quadratic.")
        }

        val newMonomials = monomials.flatMap { lhs -> rhs.monomials.map { monomial -> lhs * monomial } }.toMutableList()
        newMonomials.addAll(monomials.map { rhs.constant * it })
        newMonomials.addAll(rhs.monomials.map { constant * it })
        return QuadraticPolynomial(
            monomials = newMonomials,
            constant = this.constant * rhs.constant
        )
    }

    override fun times(rhs: Flt64): QuadraticPolynomial {
        return QuadraticPolynomial(
            monomials = monomials.map { rhs * it },
            constant = constant
        )
    }

    override fun div(rhs: Flt64): QuadraticPolynomial {
        return QuadraticPolynomial(
            monomials = monomials.map { it / rhs },
            constant = constant / rhs
        )
    }
}

class MutableQuadraticPolynomial(
    override var monomials: MutableList<QuadraticMonomial> = ArrayList(),
    override var constant: Flt64 = Flt64.zero,
    override var name: String = "",
    override var displayName: String? = null
) : AbstractQuadraticPolynomial<MutableQuadraticPolynomial>(),
    MutablePolynomial<MutableQuadraticPolynomial, QuadraticMonomial, QuadraticMonomialCell> {
    companion object {
        operator fun invoke(
            item: AbstractVariableItem<*, *>,
            name: String = "",
            displayName: String? = null
        ): MutableQuadraticPolynomial {
            return MutableQuadraticPolynomial(
                monomials = mutableListOf(QuadraticMonomial(item)),
                name = name.ifEmpty { item.name },
                displayName = displayName
            )
        }

        operator fun invoke(
            symbol: LinearIntermediateSymbol,
            name: String = "",
            displayName: String? = null
        ): MutableQuadraticPolynomial {
            return MutableQuadraticPolynomial(
                monomials = mutableListOf(QuadraticMonomial(symbol)),
                name = name.ifEmpty { symbol.name },
                displayName = displayName ?: symbol.displayName
            )
        }

        operator fun invoke(
            symbol: QuadraticIntermediateSymbol,
            name: String = "",
            displayName: String? = null
        ): MutableQuadraticPolynomial {
            return MutableQuadraticPolynomial(
                monomials = mutableListOf(QuadraticMonomial(symbol)),
                name = name.ifEmpty { symbol.name },
                displayName = displayName ?: symbol.displayName
            )
        }

        operator fun invoke(
            monomial: LinearMonomial,
            name: String = "",
            displayName: String? = null
        ): MutableQuadraticPolynomial {
            return MutableQuadraticPolynomial(
                monomials = mutableListOf(QuadraticMonomial(monomial)),
                name = name.ifEmpty { monomial.name },
                displayName = displayName ?: monomial.displayName
            )
        }

        operator fun invoke(
            monomial: QuadraticMonomial,
            name: String = "",
            displayName: String? = null
        ): MutableQuadraticPolynomial {
            return MutableQuadraticPolynomial(
                monomials = mutableListOf(monomial),
                name = name.ifEmpty { monomial.name },
                displayName = displayName ?: monomial.displayName
            )
        }

        operator fun invoke(
            polynomial: AbstractQuadraticPolynomial<*>,
            name: String = "",
            displayName: String? = null
        ): MutableQuadraticPolynomial {
            return MutableQuadraticPolynomial(
                monomials = polynomial.monomials.map { it.copy() }.toMutableList(),
                constant = polynomial.constant,
                name = name.ifEmpty { polynomial.name },
                displayName = displayName ?: polynomial.displayName
            )
        }

        operator fun invoke(
            constant: Int,
            name: String = "",
            displayName: String? = null
        ): MutableQuadraticPolynomial {
            return MutableQuadraticPolynomial(
                monomials = mutableListOf(),
                constant = Flt64(constant),
                name = name,
                displayName = displayName
            )
        }

        operator fun invoke(
            constant: Double,
            name: String = "",
            displayName: String? = null
        ): MutableQuadraticPolynomial {
            return MutableQuadraticPolynomial(
                monomials = mutableListOf(),
                constant = Flt64(constant),
                name = name,
                displayName = displayName
            )
        }

        operator fun invoke(
            constant: Boolean,
            name: String = "",
            displayName: String? = null
        ): MutableQuadraticPolynomial {
            return MutableQuadraticPolynomial(
                monomials = mutableListOf(),
                constant = if (constant) Flt64.one else Flt64.zero,
                name = name,
                displayName = displayName
            )
        }

        operator fun invoke(
            constant: Trivalent,
            name: String = "",
            displayName: String? = null
        ): MutableQuadraticPolynomial {
            return MutableQuadraticPolynomial(
                monomials = mutableListOf(),
                constant = constant.value.toFlt64(),
                name = name,
                displayName = displayName
            )
        }

        operator fun invoke(
            constant: BalancedTrivalent,
            name: String = "",
            displayName: String? = null
        ): MutableQuadraticPolynomial {
            return MutableQuadraticPolynomial(
                monomials = mutableListOf(),
                constant = constant.value.toFlt64(),
                name = name,
                displayName = displayName
            )
        }

        operator fun <T : RealNumber<T>> invoke(
            constant: T,
            name: String = "",
            displayName: String? = null
        ): MutableQuadraticPolynomial {
            return MutableQuadraticPolynomial(
                monomials = mutableListOf(),
                constant = constant.toFlt64(),
                name = name,
                displayName = displayName
            )
        }
    }

    override fun copy(): MutableQuadraticPolynomial {
        return MutableQuadraticPolynomial(
            monomials = monomials.map { it.copy() }.toMutableList(),
            constant = constant
        )
    }

    override fun copy(name: String, displayName: String?): MutableQuadraticPolynomial {
        return MutableQuadraticPolynomial(
            monomials = monomials.map { it.copy() }.toMutableList(),
            constant = constant,
            name = name,
            displayName = displayName
        )
    }

    override fun plus(rhs: AbstractVariableItem<*, *>): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(QuadraticMonomial(rhs))
        return MutableQuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("plusVariables")
    override fun plus(rhs: Iterable<AbstractVariableItem<*, *>>): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.map { QuadraticMonomial(it) })
        return MutableQuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    override fun plus(rhs: LinearIntermediateSymbol): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(QuadraticMonomial(rhs))
        return MutableQuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("plusLinearSymbols")
    override fun plus(rhs: Iterable<LinearIntermediateSymbol>): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.map { QuadraticMonomial(it) })
        return MutableQuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    override fun plus(rhs: QuadraticIntermediateSymbol): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(QuadraticMonomial(rhs))
        return MutableQuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("plusQuadraticSymbols")
    override fun plus(rhs: Iterable<QuadraticIntermediateSymbol>): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.map { QuadraticMonomial(it) })
        return MutableQuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    override fun plus(rhs: LinearMonomial): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(QuadraticMonomial(rhs))
        return MutableQuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    override fun plus(rhs: QuadraticMonomial): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(rhs)
        return MutableQuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    override fun plus(rhs: AbstractLinearPolynomial<*>): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.monomials.map { QuadraticMonomial(it) })
        return MutableQuadraticPolynomial(
            monomials = newMonomials,
            constant = constant + rhs.constant
        )
    }

    override fun plus(rhs: Polynomial<*, QuadraticMonomial, QuadraticMonomialCell>): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.monomials.map { it.copy() })
        return MutableQuadraticPolynomial(
            monomials = newMonomials,
            constant = constant + rhs.constant
        )
    }

    override fun plus(rhs: Flt64): MutableQuadraticPolynomial {
        return MutableQuadraticPolynomial(
            monomials = monomials.map { it.copy() }.toMutableList(),
            constant = constant + rhs
        )
    }

    override fun plusAssign(rhs: AbstractVariableItem<*, *>) {
        monomials.add(QuadraticMonomial(rhs))
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("plusAssignVariables")
    override fun plusAssign(rhs: Iterable<AbstractVariableItem<*, *>>) {
        monomials.addAll(rhs.map { QuadraticMonomial(it) })
    }

    operator fun plusAssign(rhs: LinearIntermediateSymbol) {
        monomials.add(QuadraticMonomial(rhs))
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("plusAssignLinearSymbols")
    operator fun plusAssign(rhs: Iterable<LinearIntermediateSymbol>) {
        monomials.addAll(rhs.map { QuadraticMonomial(it) })
    }

    operator fun plusAssign(rhs: QuadraticIntermediateSymbol) {
        monomials.add(QuadraticMonomial(rhs))
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("plusAssignQuadraticSymbols")
    operator fun plusAssign(rhs: Iterable<QuadraticIntermediateSymbol>) {
        monomials.addAll(rhs.map { QuadraticMonomial(it) })
    }

    operator fun plusAssign(rhs: LinearMonomial) {
        monomials.add(QuadraticMonomial(rhs))
    }

    override fun plusAssign(rhs: QuadraticMonomial) {
        monomials.add(rhs)
    }

    operator fun plusAssign(rhs: AbstractLinearPolynomial<*>) {
        monomials.addAll(rhs.monomials.map { QuadraticMonomial(it) })
        constant += rhs.constant
    }

    override fun plusAssign(rhs: Polynomial<*, QuadraticMonomial, QuadraticMonomialCell>) {
        monomials.addAll(rhs.monomials.map { it.copy() })
        constant += rhs.constant
    }

    override fun plusAssign(rhs: Flt64) {
        constant += rhs
    }

    override fun minus(rhs: AbstractVariableItem<*, *>): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(QuadraticMonomial(-Flt64.one, rhs))
        return MutableQuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("minusVariables")
    override fun minus(rhs: Iterable<AbstractVariableItem<*, *>>): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.map { QuadraticMonomial(-Flt64.one, it) })
        return MutableQuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    override fun minus(rhs: LinearIntermediateSymbol): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(QuadraticMonomial(-Flt64.one, rhs))
        return MutableQuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("minusLinearSymbols")
    override fun minus(rhs: Iterable<LinearIntermediateSymbol>): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.map { QuadraticMonomial(-Flt64.one, it) })
        return MutableQuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    override fun minus(rhs: QuadraticIntermediateSymbol): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(QuadraticMonomial(-Flt64.one, rhs))
        return MutableQuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("minusQuadraticSymbols")
    override fun minus(rhs: Iterable<QuadraticIntermediateSymbol>): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.map { QuadraticMonomial(-Flt64.one, it) })
        return MutableQuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    override fun minus(rhs: LinearMonomial): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(QuadraticMonomial(-rhs))
        return MutableQuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    override fun minus(rhs: QuadraticMonomial): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(-rhs)
        return MutableQuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    override fun minus(rhs: AbstractLinearPolynomial<*>): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.monomials.map { QuadraticMonomial(-it) })
        return MutableQuadraticPolynomial(
            monomials = newMonomials,
            constant = constant - rhs.constant
        )
    }

    override fun minus(rhs: Polynomial<*, QuadraticMonomial, QuadraticMonomialCell>): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.monomials.map { -it })
        return MutableQuadraticPolynomial(
            monomials = newMonomials,
            constant = constant - rhs.constant
        )
    }

    override fun minus(rhs: Flt64): MutableQuadraticPolynomial {
        return MutableQuadraticPolynomial(
            monomials = monomials.map { it.copy() }.toMutableList(),
            constant = constant - rhs
        )
    }

    override fun minusAssign(rhs: AbstractVariableItem<*, *>) {
        monomials.add(QuadraticMonomial(-Flt64.one, rhs))
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("minusAssignVariables")
    override fun minusAssign(rhs: Iterable<AbstractVariableItem<*, *>>) {
        monomials.addAll(rhs.map { QuadraticMonomial(-Flt64.one, it) })
    }

    operator fun minusAssign(rhs: LinearIntermediateSymbol) {
        monomials.add(QuadraticMonomial(-Flt64.one, rhs))
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("minusAssignLinearSymbols")
    operator fun minusAssign(rhs: Iterable<LinearIntermediateSymbol>) {
        monomials.addAll(rhs.map { QuadraticMonomial(-Flt64.one, it) })
    }

    operator fun minusAssign(rhs: QuadraticIntermediateSymbol) {
        monomials.add(QuadraticMonomial(-Flt64.one, rhs))
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("minusAssignQuadraticSymbols")
    operator fun minusAssign(rhs: Iterable<QuadraticIntermediateSymbol>) {
        monomials.addAll(rhs.map { QuadraticMonomial(-Flt64.one, it) })
    }

    operator fun minusAssign(rhs: LinearMonomial) {
        monomials.add(QuadraticMonomial(-rhs))
    }

    override fun minusAssign(rhs: QuadraticMonomial) {
        monomials.add(-rhs)
    }

    operator fun minusAssign(rhs: AbstractLinearPolynomial<*>) {
        monomials.addAll(rhs.monomials.map { QuadraticMonomial(-it) })
        constant -= rhs.constant
    }

    override fun minusAssign(rhs: Polynomial<*, QuadraticMonomial, QuadraticMonomialCell>) {
        monomials.addAll(rhs.monomials.map { -it })
        constant -= rhs.constant
    }

    override fun minusAssign(rhs: Flt64) {
        constant -= rhs
    }

    @Throws(IllegalArgumentException::class)
    override fun times(rhs: AbstractVariableItem<*, *>): MutableQuadraticPolynomial {
        if (this.category == Quadratic) {
            throw IllegalArgumentException("Invalid argument of MutableQuadraticPolynomial.times: over quadratic.")
        }

        val newMonomials = monomials.map { it * rhs }.toMutableList()
        newMonomials.add(QuadraticMonomial(constant, rhs))
        return MutableQuadraticPolynomial(monomials = newMonomials)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("timesVariables")
    @Throws(IllegalArgumentException::class)
    override fun times(rhs: Iterable<AbstractVariableItem<*, *>>): MutableQuadraticPolynomial {
        if (this.category == Quadratic) {
            throw IllegalArgumentException("Invalid argument of MutableQuadraticPolynomial.times: over quadratic.")
        }

        val newMonomials = monomials.flatMap { monomial -> rhs.map { monomial * it } }.toMutableList()
        newMonomials.addAll(rhs.map { QuadraticMonomial(constant, it) })
        return MutableQuadraticPolynomial(monomials = newMonomials)
    }

    @Throws(IllegalArgumentException::class)
    override fun times(rhs: LinearIntermediateSymbol): MutableQuadraticPolynomial {
        if (this.category == Quadratic) {
            throw IllegalArgumentException("Invalid argument of MutableQuadraticPolynomial.times: over quadratic.")
        }

        val newMonomials = monomials.map { it * rhs }.toMutableList()
        newMonomials.add(QuadraticMonomial(constant, rhs))
        return MutableQuadraticPolynomial(monomials = newMonomials)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("timesLinearSymbols")
    @Throws(IllegalArgumentException::class)
    override fun times(rhs: Iterable<LinearIntermediateSymbol>): MutableQuadraticPolynomial {
        if (this.category == Quadratic) {
            throw IllegalArgumentException("Invalid argument of MutableQuadraticPolynomial.times: over quadratic.")
        }

        val newMonomials = monomials.flatMap { monomial -> rhs.map { monomial * it } }.toMutableList()
        newMonomials.addAll(rhs.map { QuadraticMonomial(constant, it) })
        return MutableQuadraticPolynomial(monomials = newMonomials)
    }

    @Throws(IllegalArgumentException::class)
    override fun times(rhs: QuadraticIntermediateSymbol): MutableQuadraticPolynomial {
        if (this.category == Quadratic || rhs.category == Quadratic) {
            throw IllegalArgumentException("Invalid argument of MutableQuadraticPolynomial.times: over quadratic.")
        }

        val newMonomials = monomials.map { it * rhs }.toMutableList()
        newMonomials.add(QuadraticMonomial(constant, rhs))
        return MutableQuadraticPolynomial(monomials = newMonomials)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("timesQuadraticSymbols")
    @Throws(IllegalArgumentException::class)
    override fun times(rhs: Iterable<QuadraticIntermediateSymbol>): MutableQuadraticPolynomial {
        if (this.category == Quadratic || rhs.any { it.category == Quadratic }) {
            throw IllegalArgumentException("Invalid argument of MutableQuadraticPolynomial.times: over quadratic.")
        }

        val newMonomials = monomials.flatMap { monomial -> rhs.map { monomial * it } }.toMutableList()
        newMonomials.addAll(rhs.map { QuadraticMonomial(constant, it) })
        return MutableQuadraticPolynomial(monomials = newMonomials)
    }

    @Throws(IllegalArgumentException::class)
    override fun times(rhs: LinearMonomial): MutableQuadraticPolynomial {
        if (this.category == Quadratic) {
            throw IllegalArgumentException("Invalid argument of MutableQuadraticPolynomial.times: over quadratic.")
        }

        val newMonomials = monomials.map { it * rhs }.toMutableList()
        newMonomials.add(QuadraticMonomial(constant * rhs))
        return MutableQuadraticPolynomial(monomials = newMonomials)
    }

    @Throws(IllegalArgumentException::class)
    override fun times(rhs: QuadraticMonomial): MutableQuadraticPolynomial {
        if (this.category == Quadratic || rhs.category == Quadratic) {
            throw IllegalArgumentException("Invalid argument of MutableQuadraticPolynomial.times: over quadratic.")
        }

        val newMonomials = monomials.map { it * rhs }.toMutableList()
        newMonomials.add(constant * rhs)
        return MutableQuadraticPolynomial(monomials = newMonomials)
    }

    @Throws(IllegalArgumentException::class)
    override fun times(rhs: AbstractLinearPolynomial<*>): MutableQuadraticPolynomial {
        if (this.category == Quadratic) {
            throw IllegalArgumentException("Invalid argument of MutableQuadraticPolynomial.times: over quadratic.")
        }

        val newMonomials = monomials.flatMap { monomial -> rhs.monomials.map { monomial * it } }.toMutableList()
        newMonomials.addAll(monomials.map { rhs.constant * it })
        newMonomials.addAll(rhs.monomials.map { QuadraticMonomial(constant * it) })
        return MutableQuadraticPolynomial(
            monomials = newMonomials,
            constant = constant * rhs.constant
        )
    }

    @Throws(IllegalArgumentException::class)
    override fun times(rhs: AbstractQuadraticPolynomial<*>): MutableQuadraticPolynomial {
        if (this.category == Quadratic || rhs.category == Quadratic) {
            throw IllegalArgumentException("Invalid argument of MutableQuadraticPolynomial.times: over quadratic.")
        }

        val newMonomials = monomials.flatMap { monomial -> rhs.monomials.map { monomial * it } }.toMutableList()
        newMonomials.addAll(monomials.map { rhs.constant * it })
        newMonomials.addAll(rhs.monomials.map { constant * it })
        return MutableQuadraticPolynomial(
            monomials = newMonomials,
            constant = constant * rhs.constant
        )
    }

    @Throws(IllegalArgumentException::class)
    override fun times(rhs: Flt64): MutableQuadraticPolynomial {
        return MutableQuadraticPolynomial(
            monomials = monomials.map { it * rhs }.toMutableList(),
            constant = constant * rhs
        )
    }

    @Throws(IllegalArgumentException::class)
    operator fun timesAssign(rhs: AbstractVariableItem<*, *>) {
        if (this.category == Quadratic) {
            throw IllegalArgumentException("Invalid argument of MutableQuadraticPolynomial.timesAssign: over quadratic.")
        }

        monomials = monomials.map { it * rhs }.toMutableList()
        monomials.add(QuadraticMonomial(constant, rhs))
        constant = Flt64.zero
    }

    @Throws(IllegalArgumentException::class)
    operator fun timesAssign(rhs: LinearIntermediateSymbol) {
        if (this.category == Quadratic) {
            throw IllegalArgumentException("Invalid argument of MutableQuadraticPolynomial.timesAssign: over quadratic.")
        }

        monomials = monomials.map { it * rhs }.toMutableList()
        monomials.add(QuadraticMonomial(constant, rhs))
        constant = Flt64.zero
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("timesAssignLinearSymbols")
    @Throws(IllegalArgumentException::class)
    operator fun timesAssign(rhs: Iterable<LinearIntermediateSymbol>) {
        if (this.category == Quadratic) {
            throw IllegalArgumentException("Invalid argument of MutableQuadraticPolynomial.timesAssign: over quadratic.")
        }

        monomials = monomials.flatMap { monomial -> rhs.map { monomial * it } }.toMutableList()
        monomials.addAll(rhs.map { QuadraticMonomial(constant, it) })
        constant = Flt64.zero
    }

    @Throws(IllegalArgumentException::class)
    operator fun timesAssign(rhs: QuadraticIntermediateSymbol) {
        if (this.category == Quadratic || rhs.category == Quadratic) {
            throw IllegalArgumentException("Invalid argument of MutableQuadraticPolynomial.timesAssign: over quadratic.")
        }

        monomials = monomials.map { it * rhs }.toMutableList()
        monomials.add(QuadraticMonomial(constant, rhs))
        constant = Flt64.zero
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("timesAssignQuadraticSymbols")
    @Throws(IllegalArgumentException::class)
    operator fun timesAssign(rhs: Iterable<QuadraticIntermediateSymbol>) {
        if (this.category == Quadratic || rhs.any { it.category == Quadratic }) {
            throw IllegalArgumentException("Invalid argument of MutableQuadraticPolynomial.timesAssign: over quadratic.")
        }

        monomials = monomials.flatMap { monomial -> rhs.map { monomial * it } }.toMutableList()
        monomials.addAll(rhs.map { QuadraticMonomial(constant, it) })
        constant = Flt64.zero
    }

    @Throws(IllegalArgumentException::class)
    operator fun timesAssign(rhs: LinearMonomial) {
        if (this.category == Quadratic) {
            throw IllegalArgumentException("Invalid argument of MutableQuadraticPolynomial.timesAssign: over quadratic.")
        }

        monomials = monomials.map { it * rhs }.toMutableList()
        monomials.add(QuadraticMonomial(constant * rhs))
        constant = Flt64.zero
    }

    @Throws(IllegalArgumentException::class)
    operator fun timesAssign(rhs: QuadraticMonomial) {
        if (this.category == Quadratic || rhs.category == Quadratic) {
            throw IllegalArgumentException("Invalid argument of MutableQuadraticPolynomial.timesAssign: over quadratic.")
        }

        monomials = monomials.map { it * rhs }.toMutableList()
        monomials.add(constant * rhs)
        constant = Flt64.zero
    }

    @Throws(IllegalArgumentException::class)
    operator fun timesAssign(rhs: AbstractLinearPolynomial<*>) {
        if (this.category == Quadratic) {
            throw IllegalArgumentException("Invalid argument of MutableQuadraticPolynomial.timesAssign: over quadratic.")
        }

        monomials = monomials.flatMap { monomial -> rhs.monomials.map { monomial * it } }.toMutableList()
        monomials.addAll(monomials.map { rhs.constant * it })
        monomials.addAll(rhs.monomials.map { QuadraticMonomial(constant * it) })
        constant *= rhs.constant
    }

    @Throws(IllegalArgumentException::class)
    operator fun timesAssign(rhs: AbstractQuadraticPolynomial<*>) {
        if (this.category == Quadratic || rhs.category == Quadratic) {
            throw IllegalArgumentException("Invalid argument of MutableQuadraticPolynomial.timesAssign: over quadratic.")
        }

        monomials = monomials.flatMap { monomial -> rhs.monomials.map { monomial * it } }.toMutableList()
        monomials.addAll(monomials.map { rhs.constant * it })
        monomials.addAll(rhs.monomials.map { constant * it })
        constant *= rhs.constant
    }

    override fun timesAssign(rhs: Flt64) {
        monomials = monomials.map { it * rhs }.toMutableList()
        constant *= rhs
    }

    override fun div(rhs: Flt64): MutableQuadraticPolynomial {
        return MutableQuadraticPolynomial(
            monomials = monomials.map { it.copy() }.toMutableList(),
            constant = constant / rhs
        )
    }

    override fun divAssign(rhs: Flt64) {
        monomials = monomials.map { it / rhs }.toMutableList()
        constant /= rhs
    }
}

// quantity polynomial conversion

fun Quantity<AbstractQuadraticPolynomial<*>>.to(targetUnit: PhysicalUnit): Quantity<QuadraticPolynomial>? {
    return unit.to(targetUnit)?.let {
        Quantity(it.value * this.value, targetUnit)
    }
}

// unary minus quantity polynomial

operator fun Quantity<AbstractQuadraticPolynomial<*>>.unaryMinus(): Quantity<QuadraticPolynomial> {
    return Quantity(QuadraticPolynomial(-this.value), this.unit)
}

// quantity polynomial plus/minus/times/div assign

@JvmName("quantityPolynomialPlusAssignQuantityVariable")
fun Quantity<MutableQuadraticPolynomial>.plusAssign(rhs: Quantity<AbstractVariableItem<*, *>>) {
    value.plusAssign(rhs.to(this.unit)!!.value)
}

@JvmName("quantityPolynomialPlusAssignQuantityVariables")
fun Quantity<MutableQuadraticPolynomial>.plusAssign(rhs: Iterable<Quantity<AbstractVariableItem<*, *>>>) {
    for (item in rhs) {
        value.plusAssign(item.to(this.unit)!!.value)
    }
}

@JvmName("quantityPolynomialPlusAssignQuantitySymbol")
fun Quantity<MutableQuadraticPolynomial>.plusAssign(rhs: Quantity<LinearIntermediateSymbol>) {
    value.plusAssign(rhs.to(this.unit)!!.value)
}

@JvmName("quantityPolynomialPlusAssignQuantitySymbols")
fun Quantity<MutableQuadraticPolynomial>.plusAssign(rhs: Iterable<Quantity<LinearIntermediateSymbol>>) {
    for (item in rhs) {
        value.plusAssign(item.to(this.unit)!!.value)
    }
}

@JvmName("quantityPolynomialPlusAssignQuantityMonomial")
fun Quantity<MutableQuadraticPolynomial>.plusAssign(rhs: Quantity<LinearMonomial>) {
    value.plusAssign(rhs.to(this.unit)!!.value)
}

@JvmName("quantityPolynomialPlusAssignQuantityPolynomial")
fun Quantity<MutableQuadraticPolynomial>.plusAssign(rhs: Quantity<AbstractLinearPolynomial<*>>) {
    value.plusAssign(rhs.to(this.unit)!!.value)
}

@JvmName("quantityPolynomialPlusAssignQuantity")
fun<V : RealNumber<V>> Quantity<MutableQuadraticPolynomial>.plusAssign(rhs: Quantity<V>) {
    value.plusAssign(rhs.toFlt64().to(this.unit)!!.value)
}

@JvmName("quantityPolynomialMinusAssignQuantityVariable")
fun Quantity<MutableQuadraticPolynomial>.minusAssign(rhs: Quantity<AbstractVariableItem<*, *>>) {
    value.minusAssign(rhs.to(this.unit)!!.value)
}

@JvmName("quantityPolynomialMinusAssignQuantityVariables")
fun Quantity<MutableQuadraticPolynomial>.minusAssign(rhs: Iterable<Quantity<AbstractVariableItem<*, *>>>) {
    for (item in rhs) {
        value.minusAssign(item.to(this.unit)!!.value)
    }
}

@JvmName("quantityPolynomialMinusAssignQuantitySymbol")
fun Quantity<MutableQuadraticPolynomial>.minusAssign(rhs: Quantity<LinearIntermediateSymbol>) {
    value.minusAssign(rhs.to(this.unit)!!.value)
}

@JvmName("quantityPolynomialMinusAssignQuantitySymbols")
fun Quantity<MutableQuadraticPolynomial>.minusAssign(rhs: Iterable<Quantity<LinearIntermediateSymbol>>) {
    for (item in rhs) {
        value.minusAssign(item.to(this.unit)!!.value)
    }
}

@JvmName("quantityPolynomialMinusAssignQuantityMonomial")
fun Quantity<MutableQuadraticPolynomial>.minusAssign(rhs: Quantity<LinearMonomial>) {
    value.minusAssign(rhs.to(this.unit)!!.value)
}

@JvmName("quantityPolynomialMinusAssignQuantityPolynomial")
fun Quantity<MutableQuadraticPolynomial>.minusAssign(rhs: Quantity<AbstractLinearPolynomial<*>>) {
    value.minusAssign(rhs.to(this.unit)!!.value)
}

@JvmName("quantityPolynomialMinusAssignQuantity")
fun<V : RealNumber<V>> Quantity<MutableQuadraticPolynomial>.minusAssign(rhs: Quantity<V>) {
    value.minusAssign(rhs.toFlt64().to(this.unit)!!.value)
}

@JvmName("quantityPolynomialTimesAssign")
fun<V: RealNumber<V>> Quantity<MutableQuadraticPolynomial>.timesAssign(rhs: V) {
    value.timesAssign(rhs.toFlt64())
}

@JvmName("quantityPolynomialDivAssign")
fun<V: RealNumber<V>> Quantity<MutableQuadraticPolynomial>.divAssign(rhs: V) {
    value.divAssign(rhs.toFlt64())
}

// symbol and constant

operator fun QuadraticIntermediateSymbol.plus(rhs: Int): QuadraticPolynomial {
    return this.plus(Flt64(rhs))
}

operator fun QuadraticIntermediateSymbol.plus(rhs: Double): QuadraticPolynomial {
    return this.plus(Flt64(rhs))
}

operator fun <T : RealNumber<T>> QuadraticIntermediateSymbol.plus(rhs: T): QuadraticPolynomial {
    return QuadraticPolynomial(
        monomials = listOf(QuadraticMonomial(this)),
        constant = rhs.toFlt64()
    )
}

operator fun QuadraticIntermediateSymbol.minus(rhs: Int): QuadraticPolynomial {
    return this.minus(Flt64(rhs))
}

operator fun QuadraticIntermediateSymbol.minus(rhs: Double): QuadraticPolynomial {
    return this.minus(Flt64(rhs))
}

operator fun <T : RealNumber<T>> QuadraticIntermediateSymbol.minus(rhs: T): QuadraticPolynomial {
    return QuadraticPolynomial(
        monomials = listOf(QuadraticMonomial(this)),
        constant = -rhs.toFlt64()
    )
}

operator fun Int.plus(rhs: QuadraticIntermediateSymbol): QuadraticPolynomial {
    return Flt64(this).plus(rhs)
}

operator fun Double.plus(rhs: QuadraticIntermediateSymbol): QuadraticPolynomial {
    return Flt64(this).plus(rhs)
}

operator fun <T : RealNumber<T>> T.plus(rhs: QuadraticIntermediateSymbol): QuadraticPolynomial {
    return QuadraticPolynomial(
        monomials = listOf(QuadraticMonomial(rhs)),
        constant = this.toFlt64()
    )
}

operator fun Int.minus(rhs: QuadraticIntermediateSymbol): QuadraticPolynomial {
    return Flt64(this).minus(rhs)
}

operator fun Double.minus(rhs: QuadraticIntermediateSymbol): QuadraticPolynomial {
    return Flt64(this).minus(rhs)
}

operator fun <T : RealNumber<T>> T.minus(rhs: QuadraticIntermediateSymbol): QuadraticPolynomial {
    return QuadraticPolynomial(
        monomials = listOf(QuadraticMonomial(-Flt64.one, rhs)),
        constant = this.toFlt64()
    )
}

// quantity symbol and quantity

@JvmName("quantitySymbolPlusQuantity")
operator fun <T : RealNumber<T>> Quantity<QuadraticIntermediateSymbol>.plus(rhs: Quantity<T>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value + rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.value + rhs.toFlt64().to(this.unit)!!.value, this.unit)
        } else {
            Quantity(this.to(rhs.unit)!!.value + rhs.value, rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantitySymbolMinusQuantity")
operator fun <T : RealNumber<T>> Quantity<QuadraticIntermediateSymbol>.minus(rhs: Quantity<T>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value - rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.value - rhs.toFlt64().to(this.unit)!!.value, this.unit)
        } else {
            Quantity(this.to(rhs.unit)!!.value - rhs.value, rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityPlusQuantitySymbol")
operator fun <T : RealNumber<T>> Quantity<T>.plus(rhs: Quantity<QuadraticIntermediateSymbol>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value + rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.toFlt64().value + rhs.to(this.unit)!!.value, this.unit)
        } else {
            Quantity(this.toFlt64().to(rhs.unit)!!.value + rhs.value, rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityMinusQuantitySymbol")
operator fun <T : RealNumber<T>> Quantity<T>.minus(rhs: Quantity<QuadraticIntermediateSymbol>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value - rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.toFlt64().value - rhs.to(this.unit)!!.value, this.unit)
        } else {
            Quantity(this.toFlt64().to(rhs.unit)!!.value - rhs.value, rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

// monomial and constant

operator fun QuadraticMonomial.plus(rhs: Int): QuadraticPolynomial {
    return this.plus(Flt64(rhs))
}

operator fun QuadraticMonomial.plus(rhs: Double): QuadraticPolynomial {
    return this.minus(Flt64(rhs))
}

operator fun <T : RealNumber<T>> QuadraticMonomial.plus(rhs: T): QuadraticPolynomial {
    return QuadraticPolynomial(
        monomials = listOf(this.copy()),
        constant = rhs.toFlt64()
    )
}

operator fun QuadraticMonomial.minus(rhs: Int): QuadraticPolynomial {
    return this.minus(Flt64(rhs))
}

operator fun QuadraticMonomial.minus(rhs: Double): QuadraticPolynomial {
    return this.minus(Flt64(rhs))
}

operator fun <T : RealNumber<T>> QuadraticMonomial.minus(rhs: T): QuadraticPolynomial {
    return QuadraticPolynomial(
        monomials = listOf(this.copy()),
        constant = -rhs.toFlt64()
    )
}

operator fun Int.plus(rhs: QuadraticMonomial): QuadraticPolynomial {
    return Flt64(this).plus(rhs)
}

operator fun Double.plus(rhs: QuadraticMonomial): QuadraticPolynomial {
    return Flt64(this).plus(rhs)
}

operator fun <T : RealNumber<T>> T.plus(rhs: QuadraticMonomial): QuadraticPolynomial {
    return QuadraticPolynomial(
        monomials = listOf(rhs.copy()),
        constant = this.toFlt64()
    )
}

operator fun Int.minus(rhs: QuadraticMonomial): QuadraticPolynomial {
    return Flt64(this).minus(rhs)
}

operator fun Double.minus(rhs: QuadraticMonomial): QuadraticPolynomial {
    return Flt64(this).minus(rhs)
}

operator fun <T : RealNumber<T>> T.minus(rhs: QuadraticMonomial): QuadraticPolynomial {
    return QuadraticPolynomial(
        monomials = listOf(-rhs),
        constant = this.toFlt64()
    )
}

// quantity monomial and quantity

@JvmName("quantityMonomialPlusQuantity")
operator fun <T : RealNumber<T>> Quantity<QuadraticMonomial>.plus(rhs: Quantity<T>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value + rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.value + rhs.toFlt64().to(this.unit)!!.value, this.unit)
        } else {
            Quantity(this.to(rhs.unit)!!.value + rhs.value, rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityMonomialMinusQuantity")
operator fun <T : RealNumber<T>> Quantity<QuadraticMonomial>.minus(rhs: Quantity<T>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value - rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.value - rhs.toFlt64().to(this.unit)!!.value, this.unit)
        } else {
            Quantity(this.to(rhs.unit)!!.value - rhs.value, rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityPlusQuantityMonomial")
operator fun <T : RealNumber<T>> Quantity<T>.plus(rhs: Quantity<QuadraticMonomial>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value + rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.toFlt64().value + rhs.to(this.unit)!!.value, this.unit)
        } else {
            Quantity(this.toFlt64().to(rhs.unit)!!.value + rhs.value, rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityMinusQuantityMonomial")
operator fun <T : RealNumber<T>> Quantity<T>.minus(rhs: Quantity<QuadraticMonomial>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value - rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.toFlt64().value - rhs.to(this.unit)!!.value, this.unit)
        } else {
            Quantity(this.toFlt64().to(rhs.unit)!!.value - rhs.value, rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

// polynomial and constant

operator fun Int.plus(rhs: AbstractQuadraticPolynomial<*>): QuadraticPolynomial {
    return Flt64(this).plus(rhs)
}

operator fun Double.plus(rhs: AbstractQuadraticPolynomial<*>): QuadraticPolynomial {
    return Flt64(this).plus(rhs)
}

operator fun <T : RealNumber<T>> T.plus(rhs: AbstractQuadraticPolynomial<*>): QuadraticPolynomial {
    return QuadraticPolynomial(
        monomials = rhs.monomials.map { it.copy() },
        constant = this.toFlt64() + rhs.constant
    )
}

operator fun Int.minus(rhs: AbstractQuadraticPolynomial<*>): QuadraticPolynomial {
    return Flt64(this).minus(rhs)
}

operator fun Double.minus(rhs: AbstractQuadraticPolynomial<*>): QuadraticPolynomial {
    return Flt64(this).minus(rhs)
}

operator fun <T : RealNumber<T>> T.minus(rhs: AbstractQuadraticPolynomial<*>): QuadraticPolynomial {
    return QuadraticPolynomial(
        monomials = rhs.monomials.map { -it },
        constant = this.toFlt64() - rhs.constant
    )
}

operator fun Int.times(rhs: AbstractQuadraticPolynomial<*>): QuadraticPolynomial {
    return Flt64(this).times(rhs)
}

operator fun Double.times(rhs: AbstractQuadraticPolynomial<*>): QuadraticPolynomial {
    return Flt64(this).times(rhs)
}

operator fun <T : RealNumber<T>> T.times(rhs: AbstractQuadraticPolynomial<*>): QuadraticPolynomial {
    return QuadraticPolynomial(
        monomials = rhs.monomials.map { this * it },
        constant = this.toFlt64() * rhs.constant
    )
}

// quantity polynomial and constant

@JvmName("quantityPolynomialTimesInt")
operator fun Quantity<AbstractQuadraticPolynomial<*>>.times(rhs: Int): Quantity<QuadraticPolynomial> {
    return Quantity(QuadraticPolynomial(this.value * Flt64(rhs)), this.unit)
}

@JvmName("quantityPolynomialTimesDouble")
operator fun Quantity<AbstractQuadraticPolynomial<*>>.times(rhs: Double): Quantity<QuadraticPolynomial> {
    return Quantity(QuadraticPolynomial(this.value * Flt64(rhs)), this.unit)
}

@JvmName("quantityPolynomialTimesRealNumber")
operator fun <T : RealNumber<T>> Quantity<AbstractQuadraticPolynomial<*>>.times(rhs: T): Quantity<QuadraticPolynomial> {
    return Quantity(QuadraticPolynomial(this.value * rhs), this.unit)
}

@JvmName("intTimesQuantityPolynomial")
operator fun Int.times(rhs: Quantity<AbstractQuadraticPolynomial<*>>): Quantity<QuadraticPolynomial> {
    return Quantity(Flt64(this) * rhs.value, rhs.unit)
}

@JvmName("doubleTimesQuantityPolynomial")
operator fun Double.times(rhs: Quantity<AbstractQuadraticPolynomial<*>>): Quantity<QuadraticPolynomial> {
    return Quantity(Flt64(this) * rhs.value, rhs.unit)
}

@JvmName("realNumberTimesQuantityPolynomial")
operator fun <T : RealNumber<T>> T.times(rhs: Quantity<AbstractQuadraticPolynomial<*>>): Quantity<QuadraticPolynomial> {
    return Quantity(this * rhs.value, rhs.unit)
}

@JvmName("quantityPolynomialDivInt")
operator fun Quantity<AbstractQuadraticPolynomial<*>>.div(rhs: Int): Quantity<QuadraticPolynomial> {
    return Quantity(QuadraticPolynomial(this.value / Flt64(rhs)), this.unit)
}

@JvmName("quantityPolynomialDivDouble")
operator fun Quantity<AbstractQuadraticPolynomial<*>>.div(rhs: Double): Quantity<QuadraticPolynomial> {
    return Quantity(QuadraticPolynomial(this.value / Flt64(rhs)), this.unit)
}

@JvmName("quantityPolynomialDivRealNumber")
operator fun <T : RealNumber<T>> Quantity<AbstractQuadraticPolynomial<*>>.div(rhs: T): Quantity<QuadraticPolynomial> {
    return Quantity(QuadraticPolynomial(this.value / rhs), this.unit)
}

// polynomial and quantity

@JvmName("polynomialTimesQuantity")
operator fun <T : RealNumber<T>> AbstractQuadraticPolynomial<*>.times(rhs: Quantity<T>): Quantity<QuadraticPolynomial> {
    return Quantity(QuadraticPolynomial(this * rhs.value), rhs.unit)
}

@JvmName("quantityTimesPolynomial")
operator fun <T : RealNumber<T>> Quantity<T>.times(rhs: AbstractQuadraticPolynomial<*>): Quantity<QuadraticPolynomial> {
    return Quantity(QuadraticPolynomial(this.value * rhs), this.unit)
}

@JvmName("polynomialDivQuantity")
operator fun <T : RealNumber<T>> AbstractQuadraticPolynomial<*>.div(rhs: Quantity<T>): Quantity<QuadraticPolynomial> {
    return Quantity(QuadraticPolynomial(this / rhs.value), rhs.unit.reciprocal())
}

// quantity polynomial and quantity

@JvmName("quantityPolynomialPlusQuantity")
operator fun <T : RealNumber<T>> Quantity<AbstractQuadraticPolynomial<*>>.plus(rhs: Quantity<T>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(QuadraticPolynomial(this.value + rhs.value), this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(QuadraticPolynomial(this.value + rhs.toFlt64().to(this.unit)!!.value), this.unit)
        } else {
            Quantity(QuadraticPolynomial(this.to(rhs.unit)!!.value + rhs.value), rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityPolynomialMinusQuantity")
operator fun <T : RealNumber<T>> Quantity<AbstractQuadraticPolynomial<*>>.minus(rhs: Quantity<T>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(QuadraticPolynomial(this.value - rhs.value), this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(QuadraticPolynomial(this.value - rhs.toFlt64().to(this.unit)!!.value), this.unit)
        } else {
            Quantity(QuadraticPolynomial(this.to(rhs.unit)!!.value - rhs.value), rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityPlusQuantityPolynomial")
operator fun <T : RealNumber<T>> Quantity<T>.plus(rhs: Quantity<AbstractQuadraticPolynomial<*>>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(QuadraticPolynomial(this.value + rhs.value), this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(QuadraticPolynomial(this.toFlt64().value + rhs.to(this.unit)!!.value), this.unit)
        } else {
            Quantity(QuadraticPolynomial(this.toFlt64().to(rhs.unit)!!.value + rhs.value), rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityMinusQuantityPolynomial")
operator fun <T : RealNumber<T>> Quantity<T>.minus(rhs: Quantity<AbstractQuadraticPolynomial<*>>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(QuadraticPolynomial(this.value - rhs.value), this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(QuadraticPolynomial(this.toFlt64().value - rhs.to(this.unit)!!.value), this.unit)
        } else {
            Quantity(QuadraticPolynomial(this.toFlt64().to(rhs.unit)!!.value - rhs.value), rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityPolynomialTimesQuantity")
operator fun <T : RealNumber<T>> Quantity<AbstractQuadraticPolynomial<*>>.times(rhs: Quantity<T>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(QuadraticPolynomial(this.value * rhs.value), this.unit * rhs.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(QuadraticPolynomial(this.value * rhs.toFlt64().to(this.unit)!!.value), this.unit * rhs.unit)
        } else {
            Quantity(QuadraticPolynomial(this.to(rhs.unit)!!.value * rhs.value), this.unit * rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityTimesQuantityPolynomial")
operator fun <T : RealNumber<T>> Quantity<T>.times(rhs: Quantity<AbstractQuadraticPolynomial<*>>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(QuadraticPolynomial(this.value * rhs.value), this.unit * rhs.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(QuadraticPolynomial(this.toFlt64().value * rhs.to(this.unit)!!.value), this.unit * rhs.unit)
        } else {
            Quantity(QuadraticPolynomial(this.toFlt64().to(rhs.unit)!!.value * rhs.value), this.unit * rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityPolynomialDivQuantity")
operator fun <T : RealNumber<T>> Quantity<AbstractQuadraticPolynomial<*>>.div(rhs: Quantity<T>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(QuadraticPolynomial(this.value / rhs.value), this.unit / rhs.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(QuadraticPolynomial(this.value / rhs.toFlt64().to(this.unit)!!.value), this.unit / rhs.unit)
        } else {
            Quantity(QuadraticPolynomial(this.to(rhs.unit)!!.value / rhs.value), this.unit / rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

// symbol and variable

operator fun QuadraticIntermediateSymbol.plus(rhs: AbstractVariableItem<*, *>): QuadraticPolynomial {
    return QuadraticPolynomial(monomials = listOf(QuadraticMonomial(this), QuadraticMonomial(rhs)))
}

operator fun QuadraticIntermediateSymbol.minus(rhs: AbstractVariableItem<*, *>): QuadraticPolynomial {
    return QuadraticPolynomial(monomials = listOf(QuadraticMonomial(this), QuadraticMonomial(-Flt64.one, rhs)))
}

operator fun AbstractVariableItem<*, *>.plus(rhs: QuadraticIntermediateSymbol): QuadraticPolynomial {
    return QuadraticPolynomial(monomials = listOf(QuadraticMonomial(this), QuadraticMonomial(rhs)))
}

operator fun AbstractVariableItem<*, *>.minus(rhs: QuadraticIntermediateSymbol): QuadraticPolynomial {
    return QuadraticPolynomial(monomials = listOf(QuadraticMonomial(this), QuadraticMonomial(-Flt64.one, rhs)))
}

// quantity symbol and quantity variable

@JvmName("quantitySymbolPlusQuantityVariable")
operator fun Quantity<QuadraticIntermediateSymbol>.plus(rhs: Quantity<AbstractVariableItem<*, *>>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value + rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.value + rhs.to(this.unit)!!.value, this.unit)
        } else {
            Quantity(this.to(rhs.unit)!!.value + rhs.value, rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantitySymbolMinusQuantityVariable")
operator fun Quantity<QuadraticIntermediateSymbol>.minus(rhs: Quantity<AbstractVariableItem<*, *>>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value - rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.value - rhs.to(this.unit)!!.value, this.unit)
        } else {
            Quantity(this.to(rhs.unit)!!.value - rhs.value, rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityVariablePlusQuantitySymbol")
operator fun Quantity<AbstractVariableItem<*, *>>.plus(rhs: Quantity<QuadraticIntermediateSymbol>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value + rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.to(rhs.unit)!!.value + rhs.value, rhs.unit)
        } else {
            Quantity(this.value + rhs.to(this.unit)!!.value, this.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityVariableMinusQuantitySymbol")
operator fun Quantity<AbstractVariableItem<*, *>>.minus(rhs: Quantity<QuadraticIntermediateSymbol>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value - rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.to(rhs.unit)!!.value - rhs.value, rhs.unit)
        } else {
            Quantity(this.value - rhs.to(this.unit)!!.value, this.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

// monomial and variable

operator fun QuadraticMonomial.plus(rhs: AbstractVariableItem<*, *>): QuadraticPolynomial {
    return QuadraticPolynomial(monomials = listOf(this.copy(), QuadraticMonomial(rhs)))
}

operator fun QuadraticMonomial.minus(rhs: AbstractVariableItem<*, *>): QuadraticPolynomial {
    return QuadraticPolynomial(monomials = listOf(this.copy(), QuadraticMonomial(-Flt64.one, rhs)))
}

operator fun AbstractVariableItem<*, *>.plus(rhs: QuadraticMonomial): QuadraticPolynomial {
    return QuadraticPolynomial(monomials = listOf(QuadraticMonomial(this), rhs.copy()))
}

operator fun AbstractVariableItem<*, *>.minus(rhs: QuadraticMonomial): QuadraticPolynomial {
    return QuadraticPolynomial(monomials = listOf(QuadraticMonomial(this), -rhs))
}

// quantity monomial and quantity variable

@JvmName("quantityMonomialPlusQuantityVariable")
operator fun Quantity<QuadraticMonomial>.plus(rhs: Quantity<AbstractVariableItem<*, *>>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value + rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.value + rhs.to(this.unit)!!.value, this.unit)
        } else {
            Quantity(this.to(rhs.unit)!!.value + rhs.value, rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityMonomialMinusQuantityVariable")
operator fun Quantity<QuadraticMonomial>.minus(rhs: Quantity<AbstractVariableItem<*, *>>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value - rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.value - rhs.to(this.unit)!!.value, this.unit)
        } else {
            Quantity(this.to(rhs.unit)!!.value - rhs.value, rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityVariablePlusQuantityMonomial")
operator fun Quantity<AbstractVariableItem<*, *>>.plus(rhs: Quantity<QuadraticMonomial>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value + rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.to(rhs.unit)!!.value + rhs.value, rhs.unit)
        } else {
            Quantity(this.value + rhs.to(this.unit)!!.value, this.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityVariableMinusQuantityMonomial")
operator fun Quantity<AbstractVariableItem<*, *>>.minus(rhs: Quantity<QuadraticMonomial>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value - rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.to(rhs.unit)!!.value - rhs.value, rhs.unit)
        } else {
            Quantity(this.value - rhs.to(this.unit)!!.value, this.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

// polynomial and variable

operator fun AbstractVariableItem<*, *>.plus(rhs: AbstractQuadraticPolynomial<*>): QuadraticPolynomial {
    val newMonomials = rhs.monomials.map { it.copy() }.toMutableList()
    newMonomials.add(QuadraticMonomial(this))
    return QuadraticPolynomial(
        monomials = newMonomials,
        constant = rhs.constant
    )
}

operator fun AbstractVariableItem<*, *>.minus(rhs: AbstractQuadraticPolynomial<*>): QuadraticPolynomial {
    val newMonomials = mutableListOf(QuadraticMonomial(this))
    newMonomials.addAll(rhs.monomials.map { -it })
    return QuadraticPolynomial(
        monomials = newMonomials,
        constant = -rhs.constant
    )
}

operator fun AbstractLinearPolynomial<*>.times(rhs: AbstractVariableItem<*, *>): QuadraticPolynomial {
    val newMonomials = this.monomials.map { it * rhs }.toMutableList()
    newMonomials.add(QuadraticMonomial(this.constant, rhs))
    return QuadraticPolynomial(monomials = newMonomials)
}

operator fun AbstractVariableItem<*, *>.times(rhs: AbstractLinearPolynomial<*>): QuadraticPolynomial {
    val newMonomials = rhs.monomials.map { this * it }.toMutableList()
    newMonomials.add(QuadraticMonomial(rhs.constant, this))
    return QuadraticPolynomial(monomials = newMonomials)
}

@Throws(IllegalArgumentException::class)
operator fun AbstractVariableItem<*, *>.times(rhs: AbstractQuadraticPolynomial<*>): QuadraticPolynomial {
    if (rhs.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of QuadraticPolynomial.times: over quadratic.")
    }

    val newMonomials = rhs.monomials.map { this * it }.toMutableList()
    newMonomials.add(QuadraticMonomial(rhs.constant, this))
    return QuadraticPolynomial(monomials = newMonomials)
}

// quantity polynomial and variable

@JvmName("quantityLinearPolynomialTimesVariable")
operator fun Quantity<AbstractLinearPolynomial<*>>.times(rhs: AbstractVariableItem<*, *>): Quantity<QuadraticPolynomial> {
    return Quantity(this.value * rhs, this.unit)
}

@JvmName("quantityQuadraticPolynomialTimesVariable")
operator fun Quantity<AbstractQuadraticPolynomial<*>>.times(rhs: AbstractVariableItem<*, *>): Quantity<QuadraticPolynomial> {
    return Quantity(QuadraticPolynomial(this.value * rhs), this.unit)
}

@JvmName("variableTimesQuantityLinearPolynomial")
operator fun AbstractVariableItem<*, *>.times(rhs: Quantity<AbstractLinearPolynomial<*>>): Quantity<QuadraticPolynomial> {
    return Quantity(this * rhs.value, rhs.unit)
}

@JvmName("variableTimesQuantityQuadraticPolynomial")
operator fun AbstractVariableItem<*, *>.times(rhs: Quantity<AbstractQuadraticPolynomial<*>>): Quantity<QuadraticPolynomial> {
    return Quantity(this * rhs.value, rhs.unit)
}

// polynomial and quantity variable

@JvmName("linearPolynomialTimesQuantityVariable")
operator fun AbstractLinearPolynomial<*>.times(rhs: Quantity<AbstractVariableItem<*, *>>): Quantity<QuadraticPolynomial> {
    return Quantity(this * rhs.value, rhs.unit)
}

@JvmName("quadraticPolynomialTimesQuantityVariable")
operator fun AbstractQuadraticPolynomial<*>.times(rhs: Quantity<AbstractVariableItem<*, *>>): Quantity<QuadraticPolynomial> {
    return Quantity(QuadraticPolynomial(this * rhs.value), rhs.unit)
}

@JvmName("quantityVariableTimesLinearPolynomial")
operator fun Quantity<AbstractVariableItem<*, *>>.times(rhs: AbstractLinearPolynomial<*>): Quantity<QuadraticPolynomial> {
    return Quantity(this.value * rhs, this.unit)
}

@JvmName("quantityVariableTimesQuadraticPolynomial")
operator fun Quantity<AbstractVariableItem<*, *>>.times(rhs: AbstractQuadraticPolynomial<*>): Quantity<QuadraticPolynomial> {
    return Quantity(this.value * rhs, this.unit)
}

// quantity polynomial and quantity variable

@JvmName("quantityQuadraticPolynomialPlusQuantityVariable")
operator fun Quantity<AbstractQuadraticPolynomial<*>>.plus(rhs: Quantity<AbstractVariableItem<*, *>>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(QuadraticPolynomial(this.value + rhs.value), this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(QuadraticPolynomial(this.value + rhs.to(this.unit)!!.value), this.unit)
        } else {
            Quantity(this.to(rhs.unit)!!.value + rhs.value, rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityQuadraticPolynomialMinusQuantityVariable")
operator fun Quantity<AbstractQuadraticPolynomial<*>>.minus(rhs: Quantity<AbstractVariableItem<*, *>>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(QuadraticPolynomial(this.value - rhs.value), this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(QuadraticPolynomial(this.value - rhs.to(this.unit)!!.value), this.unit)
        } else {
            Quantity(this.to(rhs.unit)!!.value - rhs.value, rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityVariablePlusQuantityQuadraticPolynomial")
operator fun Quantity<AbstractVariableItem<*, *>>.plus(rhs: Quantity<AbstractQuadraticPolynomial<*>>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value + rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.to(rhs.unit)!!.value + rhs.value, rhs.unit)
        } else {
            Quantity(this.value + rhs.to(this.unit)!!.value, this.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityVariableMinusQuantityQuadraticPolynomial")
operator fun Quantity<AbstractVariableItem<*, *>>.minus(rhs: Quantity<AbstractQuadraticPolynomial<*>>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value - rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.to(rhs.unit)!!.value - rhs.value, rhs.unit)
        } else {
            Quantity(this.value - rhs.to(this.unit)!!.value, this.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearPolynomialTimesQuantityVariable")
operator fun Quantity<AbstractLinearPolynomial<*>>.times(rhs: Quantity<AbstractVariableItem<*, *>>): Quantity<QuadraticPolynomial> {
    return Quantity(this.value * rhs.value, this.unit * rhs.unit)
}

@JvmName("quantityQuadraticPolynomialTimesQuantityVariable")
operator fun Quantity<AbstractQuadraticPolynomial<*>>.times(rhs: Quantity<AbstractVariableItem<*, *>>): Quantity<QuadraticPolynomial> {
    return Quantity(QuadraticPolynomial(this.value * rhs.value), this.unit * rhs.unit)
}

@JvmName("quantityVariableTimesLinearPolynomial")
operator fun Quantity<AbstractVariableItem<*, *>>.times(rhs: Quantity<AbstractLinearPolynomial<*>>): Quantity<QuadraticPolynomial> {
    return Quantity(this.value * rhs.value, this.unit * rhs.unit)
}

@JvmName("quantityVariableTimesQuadraticPolynomial")
operator fun Quantity<AbstractVariableItem<*, *>>.times(rhs: Quantity<AbstractQuadraticPolynomial<*>>): Quantity<QuadraticPolynomial> {
    return Quantity(this.value * rhs.value, this.unit * rhs.unit)
}

// symbol and symbol

operator fun QuadraticIntermediateSymbol.plus(rhs: LinearIntermediateSymbol): QuadraticPolynomial {
    return QuadraticPolynomial(monomials = listOf(QuadraticMonomial(this), QuadraticMonomial(rhs)))
}

operator fun QuadraticIntermediateSymbol.minus(rhs: LinearIntermediateSymbol): QuadraticPolynomial {
    return QuadraticPolynomial(monomials = listOf(QuadraticMonomial(this), QuadraticMonomial(-Flt64.one, rhs)))
}

operator fun LinearIntermediateSymbol.plus(rhs: QuadraticIntermediateSymbol): QuadraticPolynomial {
    return QuadraticPolynomial(monomials = listOf(QuadraticMonomial(this), QuadraticMonomial(rhs)))
}

operator fun LinearIntermediateSymbol.minus(rhs: QuadraticIntermediateSymbol): QuadraticPolynomial {
    return QuadraticPolynomial(monomials = listOf(QuadraticMonomial(this), QuadraticMonomial(-Flt64.one, rhs)))
}

operator fun QuadraticIntermediateSymbol.plus(rhs: QuadraticIntermediateSymbol): QuadraticPolynomial {
    return QuadraticPolynomial(monomials = listOf(QuadraticMonomial(this), QuadraticMonomial(rhs)))
}

operator fun QuadraticIntermediateSymbol.minus(rhs: QuadraticIntermediateSymbol): QuadraticPolynomial {
    return QuadraticPolynomial(monomials = listOf(QuadraticMonomial(this), QuadraticMonomial(-Flt64.one, rhs)))
}

// quantity symbol and quantity symbol

@JvmName("quantityQuadraticSymbolPlusQuantityLinearSymbol")
operator fun Quantity<QuadraticIntermediateSymbol>.plus(rhs: Quantity<LinearIntermediateSymbol>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value + rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.value + rhs.to(this.unit)!!.value, this.unit)
        } else {
            Quantity(this.to(rhs.unit)!!.value + rhs.value, rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityQuadraticSymbolMinusQuantityLinearSymbol")
operator fun Quantity<QuadraticIntermediateSymbol>.minus(rhs: Quantity<LinearIntermediateSymbol>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value - rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.value - rhs.to(this.unit)!!.value, this.unit)
        } else {
            Quantity(this.to(rhs.unit)!!.value - rhs.value, rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearSymbolPlusQuantityQuadraticSymbol")
operator fun Quantity<LinearIntermediateSymbol>.plus(rhs: Quantity<QuadraticIntermediateSymbol>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value + rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.to(rhs.unit)!!.value + rhs.value, rhs.unit)
        } else {
            Quantity(this.value + rhs.to(this.unit)!!.value, this.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearSymbolMinusQuantityQuadraticSymbol")
operator fun Quantity<LinearIntermediateSymbol>.minus(rhs: Quantity<QuadraticIntermediateSymbol>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value - rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.to(rhs.unit)!!.value - rhs.value, rhs.unit)
        } else {
            Quantity(this.value - rhs.to(this.unit)!!.value, this.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityQuadraticSymbolPlusQuantityQuadraticSymbol")
operator fun Quantity<QuadraticIntermediateSymbol>.plus(rhs: Quantity<QuadraticIntermediateSymbol>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value + rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.value + rhs.to(this.unit)!!.value, this.unit)
        } else {
            Quantity(this.to(rhs.unit)!!.value + rhs.value, rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityQuadraticSymbolMinusQuantityQuadraticSymbol")
operator fun Quantity<QuadraticIntermediateSymbol>.minus(rhs: Quantity<QuadraticIntermediateSymbol>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value - rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.value - rhs.to(this.unit)!!.value, this.unit)
        } else {
            Quantity(this.to(rhs.unit)!!.value - rhs.value, rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

// monomial and symbol

operator fun LinearMonomial.plus(rhs: QuadraticIntermediateSymbol): QuadraticPolynomial {
    return QuadraticPolynomial(monomials = listOf(QuadraticMonomial(this), QuadraticMonomial(rhs)))
}

operator fun LinearMonomial.minus(rhs: QuadraticIntermediateSymbol): QuadraticPolynomial {
    return QuadraticPolynomial(monomials = listOf(QuadraticMonomial(this), QuadraticMonomial(-Flt64.one, rhs)))
}

operator fun QuadraticIntermediateSymbol.plus(rhs: LinearMonomial): QuadraticPolynomial {
    return QuadraticPolynomial(monomials = listOf(QuadraticMonomial(this), QuadraticMonomial(rhs)))
}

operator fun QuadraticIntermediateSymbol.minus(rhs: LinearMonomial): QuadraticPolynomial {
    return QuadraticPolynomial(monomials = listOf(QuadraticMonomial(this), QuadraticMonomial(-rhs)))
}

operator fun QuadraticMonomial.plus(rhs: LinearIntermediateSymbol): QuadraticPolynomial {
    return QuadraticPolynomial(monomials = listOf(this.copy(), QuadraticMonomial(rhs)))
}

operator fun QuadraticMonomial.minus(rhs: LinearIntermediateSymbol): QuadraticPolynomial {
    return QuadraticPolynomial(monomials = listOf(this.copy(), QuadraticMonomial(-Flt64.one, rhs)))
}

operator fun LinearIntermediateSymbol.plus(rhs: QuadraticMonomial): QuadraticPolynomial {
    return QuadraticPolynomial(monomials = listOf(QuadraticMonomial(this), rhs.copy()))
}

operator fun LinearIntermediateSymbol.minus(rhs: QuadraticMonomial): QuadraticPolynomial {
    return QuadraticPolynomial(monomials = listOf(QuadraticMonomial(this), -rhs))
}

operator fun QuadraticMonomial.plus(rhs: QuadraticIntermediateSymbol): QuadraticPolynomial {
    return QuadraticPolynomial(monomials = listOf(this.copy(), QuadraticMonomial(rhs)))
}

operator fun QuadraticMonomial.minus(rhs: QuadraticIntermediateSymbol): QuadraticPolynomial {
    return QuadraticPolynomial(monomials = listOf(this.copy(), QuadraticMonomial(-Flt64.one, rhs)))
}

operator fun QuadraticIntermediateSymbol.plus(rhs: QuadraticMonomial): QuadraticPolynomial {
    return QuadraticPolynomial(monomials = listOf(QuadraticMonomial(this), rhs.copy()))
}

operator fun QuadraticIntermediateSymbol.minus(rhs: QuadraticMonomial): QuadraticPolynomial {
    return QuadraticPolynomial(monomials = listOf(QuadraticMonomial(this), -rhs))
}

// quantity monomial and quantity symbol

@JvmName("quantityLinearMonomialPlusQuantityQuadraticSymbol")
operator fun Quantity<LinearMonomial>.plus(rhs: Quantity<QuadraticIntermediateSymbol>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value + rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.value + rhs.to(this.unit)!!.value, this.unit)
        } else {
            Quantity(this.to(rhs.unit)!!.value + rhs.value, rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearMonomialMinusQuantityQuadraticSymbol")
operator fun Quantity<LinearMonomial>.minus(rhs: Quantity<QuadraticIntermediateSymbol>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value - rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.value - rhs.to(this.unit)!!.value, this.unit)
        } else {
            Quantity(this.to(rhs.unit)!!.value - rhs.value, rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityQuadraticSymbolPlusQuantityLinearMonomial")
operator fun Quantity<QuadraticIntermediateSymbol>.plus(rhs: Quantity<LinearMonomial>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value + rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.to(rhs.unit)!!.value + rhs.value, rhs.unit)
        } else {
            Quantity(this.value + rhs.to(this.unit)!!.value, this.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityQuadraticSymbolMinusQuantityLinearMonomial")
operator fun Quantity<QuadraticIntermediateSymbol>.minus(rhs: Quantity<LinearMonomial>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value - rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.to(rhs.unit)!!.value - rhs.value, rhs.unit)
        } else {
            Quantity(this.value - rhs.to(this.unit)!!.value, this.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityQuadraticMonomialPlusQuantityLinearSymbol")
operator fun Quantity<QuadraticMonomial>.plus(rhs: Quantity<LinearIntermediateSymbol>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value + rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.value + rhs.to(this.unit)!!.value, this.unit)
        } else {
            Quantity(this.to(rhs.unit)!!.value + rhs.value, rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityQuadraticMonomialMinusQuantityLinearSymbol")
operator fun Quantity<QuadraticMonomial>.minus(rhs: Quantity<LinearIntermediateSymbol>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value - rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.value - rhs.to(this.unit)!!.value, this.unit)
        } else {
            Quantity(this.to(rhs.unit)!!.value - rhs.value, rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearSymbolPlusQuantityQuadraticMonomial")
operator fun Quantity<LinearIntermediateSymbol>.plus(rhs: Quantity<QuadraticMonomial>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value + rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.to(rhs.unit)!!.value + rhs.value, rhs.unit)
        } else {
            Quantity(this.value + rhs.to(this.unit)!!.value, this.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearSymbolMinusQuantityQuadraticMonomial")
operator fun Quantity<LinearIntermediateSymbol>.minus(rhs: Quantity<QuadraticMonomial>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value - rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.to(rhs.unit)!!.value - rhs.value, rhs.unit)
        } else {
            Quantity(this.value - rhs.to(this.unit)!!.value, this.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityQuadraticMonomialPlusQuantityQuadraticSymbol")
operator fun Quantity<QuadraticMonomial>.plus(rhs: Quantity<QuadraticIntermediateSymbol>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value + rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.value + rhs.to(this.unit)!!.value, this.unit)
        } else {
            Quantity(this.to(rhs.unit)!!.value + rhs.value, rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityQuadraticMonomialMinusQuantityQuadraticSymbol")
operator fun Quantity<QuadraticMonomial>.minus(rhs: Quantity<QuadraticIntermediateSymbol>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value - rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.value - rhs.to(this.unit)!!.value, this.unit)
        } else {
            Quantity(this.to(rhs.unit)!!.value - rhs.value, rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityQuadraticSymbolPlusQuantityQuadraticMonomial")
operator fun Quantity<QuadraticIntermediateSymbol>.plus(rhs: Quantity<QuadraticMonomial>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value + rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.to(rhs.unit)!!.value + rhs.value, rhs.unit)
        } else {
            Quantity(this.value + rhs.to(this.unit)!!.value, this.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityQuadraticSymbolMinusQuantityQuadraticMonomial")
operator fun Quantity<QuadraticIntermediateSymbol>.minus(rhs: Quantity<QuadraticMonomial>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value - rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.to(rhs.unit)!!.value - rhs.value, rhs.unit)
        } else {
            Quantity(this.value - rhs.to(this.unit)!!.value, this.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

// polynomial and symbol

operator fun LinearIntermediateSymbol.times(rhs: AbstractLinearPolynomial<*>): QuadraticPolynomial {
    val newMonomials = rhs.monomials.map { this * it }.toMutableList()
    newMonomials.add(QuadraticMonomial(rhs.constant, this))
    return QuadraticPolynomial(monomials = newMonomials)
}

operator fun AbstractLinearPolynomial<*>.times(rhs: LinearIntermediateSymbol): QuadraticPolynomial {
    val newMonomials = this.monomials.map { it * rhs }.toMutableList()
    newMonomials.add(QuadraticMonomial(this.constant, rhs))
    return QuadraticPolynomial(monomials = newMonomials)
}

operator fun AbstractLinearPolynomial<*>.plus(rhs: QuadraticIntermediateSymbol): QuadraticPolynomial {
    val newMonomials = this.monomials.map { QuadraticMonomial(it) }.toMutableList()
    newMonomials.add(QuadraticMonomial(rhs))
    return QuadraticPolynomial(
        monomials = newMonomials,
        constant = this.constant
    )
}

operator fun AbstractLinearPolynomial<*>.minus(rhs: QuadraticIntermediateSymbol): QuadraticPolynomial {
    val newMonomials = this.monomials.map { QuadraticMonomial(it) }.toMutableList()
    newMonomials.add(QuadraticMonomial(-Flt64.one, rhs))
    return QuadraticPolynomial(
        monomials = newMonomials,
        constant = this.constant
    )
}

@Throws(IllegalArgumentException::class)
operator fun AbstractLinearPolynomial<*>.times(rhs: QuadraticIntermediateSymbol): QuadraticPolynomial {
    if (rhs.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of QuadraticPolynomial.times: over quadratic.")
    }

    val newMonomials = this.monomials.map { it * rhs }.toMutableList()
    newMonomials.add(QuadraticMonomial(this.constant, rhs))
    return QuadraticPolynomial(monomials = newMonomials)
}

operator fun LinearIntermediateSymbol.plus(rhs: AbstractQuadraticPolynomial<*>): QuadraticPolynomial {
    val newMonomials = mutableListOf(QuadraticMonomial(this))
    newMonomials.addAll(rhs.monomials.map { it.copy() })
    return QuadraticPolynomial(
        monomials = newMonomials,
        constant = rhs.constant
    )
}

operator fun LinearIntermediateSymbol.minus(rhs: AbstractQuadraticPolynomial<*>): QuadraticPolynomial {
    val newMonomials = mutableListOf(QuadraticMonomial(this))
    newMonomials.addAll(rhs.monomials.map { -it })
    return QuadraticPolynomial(
        monomials = newMonomials,
        constant = -rhs.constant
    )
}

@Throws(IllegalArgumentException::class)
operator fun LinearIntermediateSymbol.times(rhs: AbstractQuadraticPolynomial<*>): QuadraticPolynomial {
    if (rhs.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of LinearSymbol.times: over quadratic.")
    }

    val newMonomials = rhs.monomials.map { this * it }.toMutableList()
    newMonomials.add(QuadraticMonomial(rhs.constant, this))
    return QuadraticPolynomial(monomials = newMonomials)
}

operator fun QuadraticIntermediateSymbol.plus(rhs: AbstractLinearPolynomial<*>): QuadraticPolynomial {
    val newMonomials = mutableListOf(QuadraticMonomial(this))
    newMonomials.addAll(rhs.monomials.map { QuadraticMonomial(it) })
    return QuadraticPolynomial(
        monomials = newMonomials,
        constant = rhs.constant
    )
}

operator fun QuadraticIntermediateSymbol.minus(rhs: AbstractLinearPolynomial<*>): QuadraticPolynomial {
    val newMonomials = mutableListOf(QuadraticMonomial(this))
    newMonomials.addAll(rhs.monomials.map { QuadraticMonomial(-it) })
    return QuadraticPolynomial(
        monomials = newMonomials,
        constant = -rhs.constant
    )
}

@Throws(IllegalArgumentException::class)
operator fun QuadraticIntermediateSymbol.times(rhs: AbstractLinearPolynomial<*>): QuadraticPolynomial {
    if (this.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of QuadraticSymbol.times: over quadratic.")
    }

    val newMonomials = rhs.monomials.map { this * it }.toMutableList()
    newMonomials.add(QuadraticMonomial(rhs.constant, this))
    return QuadraticPolynomial(monomials = newMonomials)
}

operator fun QuadraticIntermediateSymbol.plus(rhs: AbstractQuadraticPolynomial<*>): QuadraticPolynomial {
    val newMonomials = mutableListOf(QuadraticMonomial(this))
    newMonomials.addAll(rhs.monomials.map { it.copy() })
    return QuadraticPolynomial(
        monomials = newMonomials,
        constant = rhs.constant
    )
}

operator fun QuadraticIntermediateSymbol.minus(rhs: AbstractQuadraticPolynomial<*>): QuadraticPolynomial {
    val newMonomials = mutableListOf(QuadraticMonomial(this))
    newMonomials.addAll(rhs.monomials.map { -it })
    return QuadraticPolynomial(
        monomials = newMonomials,
        constant = rhs.constant
    )
}

@Throws(IllegalArgumentException::class)
operator fun QuadraticIntermediateSymbol.times(rhs: AbstractQuadraticPolynomial<*>): QuadraticPolynomial {
    if (this.category == Quadratic || rhs.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of QuadraticSymbol.times: over quadratic.")
    }

    val newMonomials = rhs.monomials.map { this * it }.toMutableList()
    newMonomials.add(QuadraticMonomial(rhs.constant, this))
    return QuadraticPolynomial(monomials = newMonomials)
}

// quantity polynomial and symbol

@JvmName("linearSymbolTimesQuantityLinearPolynomial")
operator fun LinearIntermediateSymbol.times(rhs: Quantity<AbstractLinearPolynomial<*>>): Quantity<QuadraticPolynomial> {
    return Quantity(this * rhs.value, rhs.unit)
}

@JvmName("quantityLinearPolynomialTimesLinearSymbol")
operator fun Quantity<AbstractLinearPolynomial<*>>.times(rhs: LinearIntermediateSymbol): Quantity<QuadraticPolynomial> {
    return Quantity(this.value * rhs, this.unit)
}

@JvmName("quadraticSymbolTimesQuantityLinearPolynomial")
operator fun QuadraticIntermediateSymbol.times(rhs: Quantity<AbstractLinearPolynomial<*>>): Quantity<QuadraticPolynomial> {
    return Quantity(this * rhs.value, rhs.unit)
}

@JvmName("quantityLinearPolynomialTimesQuadraticSymbol")
operator fun Quantity<AbstractLinearPolynomial<*>>.times(rhs: QuadraticIntermediateSymbol): Quantity<QuadraticPolynomial> {
    return Quantity(this.value * rhs, this.unit)
}

@JvmName("linearSymbolTimesQuantityQuadraticPolynomial")
operator fun LinearIntermediateSymbol.times(rhs: Quantity<AbstractQuadraticPolynomial<*>>): Quantity<QuadraticPolynomial> {
    return Quantity(this * rhs.value, rhs.unit)
}

@JvmName("quantityQuadraticPolynomialTimesLinearSymbol")
operator fun Quantity<AbstractQuadraticPolynomial<*>>.times(rhs: LinearIntermediateSymbol): Quantity<QuadraticPolynomial> {
    return Quantity(QuadraticPolynomial(this.value * rhs), this.unit)
}

@JvmName("quadraticSymbolTimesQuantityQuadraticPolynomial")
operator fun QuadraticIntermediateSymbol.times(rhs: Quantity<AbstractQuadraticPolynomial<*>>): Quantity<QuadraticPolynomial> {
    return Quantity(this * rhs.value, rhs.unit)
}

@JvmName("quantityQuadraticPolynomialTimesQuadraticSymbol")
operator fun Quantity<AbstractQuadraticPolynomial<*>>.times(rhs: QuadraticIntermediateSymbol): Quantity<QuadraticPolynomial> {
    return Quantity(QuadraticPolynomial(this.value * rhs), this.unit)
}

// polynomial and quantity symbol

@JvmName("quantityLinearSymbolTimesLinearPolynomial")
operator fun Quantity<LinearIntermediateSymbol>.times(rhs: AbstractLinearPolynomial<*>): Quantity<QuadraticPolynomial> {
    return Quantity(this.value * rhs, this.unit)
}

@JvmName("linearPolynomialTimesQuantityLinearSymbol")
operator fun AbstractLinearPolynomial<*>.times(rhs: Quantity<LinearIntermediateSymbol>): Quantity<QuadraticPolynomial> {
    return Quantity(this * rhs.value, rhs.unit)
}

@JvmName("quantityQuadraticSymbolTimesLinearPolynomial")
operator fun Quantity<QuadraticIntermediateSymbol>.times(rhs: AbstractLinearPolynomial<*>): Quantity<QuadraticPolynomial> {
    return Quantity(this.value * rhs, this.unit)
}

@JvmName("linearPolynomialTimesQuantityQuadraticSymbol")
operator fun AbstractLinearPolynomial<*>.times(rhs: Quantity<QuadraticIntermediateSymbol>): Quantity<QuadraticPolynomial> {
    return Quantity(this * rhs.value, rhs.unit)
}

@JvmName("quantityLinearSymbolTimesQuadraticPolynomial")
operator fun Quantity<LinearIntermediateSymbol>.times(rhs: AbstractQuadraticPolynomial<*>): Quantity<QuadraticPolynomial> {
    return Quantity(this.value * rhs, this.unit)
}

@JvmName("quadraticPolynomialTimesQuantityLinearSymbol")
operator fun AbstractQuadraticPolynomial<*>.times(rhs: Quantity<LinearIntermediateSymbol>): Quantity<QuadraticPolynomial> {
    return Quantity(QuadraticPolynomial(this * rhs.value), rhs.unit)
}

@JvmName("quantityQuadraticSymbolTimesQuadraticPolynomial")
operator fun Quantity<QuadraticIntermediateSymbol>.times(rhs: AbstractQuadraticPolynomial<*>): Quantity<QuadraticPolynomial> {
    return Quantity(this.value * rhs, this.unit)
}

@JvmName("quadraticPolynomialTimesQuantityQuadraticSymbol")
operator fun AbstractQuadraticPolynomial<*>.times(rhs: Quantity<QuadraticIntermediateSymbol>): Quantity<QuadraticPolynomial> {
    return Quantity(QuadraticPolynomial(this * rhs.value), rhs.unit)
}

// quantity polynomial and quantity symbol

@JvmName("quantityLinearSymbolTimesQuantityLinearPolynomial")
operator fun Quantity<LinearIntermediateSymbol>.times(rhs: Quantity<AbstractLinearPolynomial<*>>): Quantity<QuadraticPolynomial> {
    return Quantity(this.value * rhs.value, this.unit * rhs.unit)
}

@JvmName("quantityLinearPolynomialTimesQuantityLinearSymbol")
operator fun Quantity<AbstractLinearPolynomial<*>>.times(rhs: Quantity<LinearIntermediateSymbol>): Quantity<QuadraticPolynomial> {
    return Quantity(this.value * rhs.value, this.unit * rhs.unit)
}

@JvmName("quantityQuadraticSymbolTimesQuantityLinearPolynomial")
operator fun Quantity<QuadraticIntermediateSymbol>.times(rhs: Quantity<AbstractLinearPolynomial<*>>): Quantity<QuadraticPolynomial> {
    return Quantity(this.value * rhs.value, this.unit * rhs.unit)
}

@JvmName("quantityQuadraticSymbolPlusQuantityLinearPolynomial")
operator fun Quantity<QuadraticIntermediateSymbol>.plus(rhs: Quantity<AbstractLinearPolynomial<*>>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value + rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.to(rhs.unit)!!.value + rhs.value, rhs.unit)
        } else {
            Quantity(this.value + rhs.to(this.unit)!!.value, this.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityQuadraticSymbolMinusQuantityLinearPolynomial")
operator fun Quantity<QuadraticIntermediateSymbol>.minus(rhs: Quantity<AbstractLinearPolynomial<*>>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value - rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.to(rhs.unit)!!.value - rhs.value, rhs.unit)
        } else {
            Quantity(this.value - rhs.to(this.unit)!!.value, this.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearPolynomialTimesQuantityQuadraticSymbol")
operator fun Quantity<AbstractLinearPolynomial<*>>.times(rhs: Quantity<QuadraticIntermediateSymbol>): Quantity<QuadraticPolynomial> {
    return Quantity(this.value * rhs.value, this.unit * rhs.unit)
}

@JvmName("quantityLinearPolynomialPlusQuantityQuadraticSymbol")
operator fun Quantity<AbstractLinearPolynomial<*>>.plus(rhs: Quantity<QuadraticIntermediateSymbol>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value + rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.to(rhs.unit)!!.value + rhs.value, rhs.unit)
        } else {
            Quantity(this.value + rhs.to(this.unit)!!.value, this.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearPolynomialMinusQuantityQuadraticSymbol")
operator fun Quantity<AbstractLinearPolynomial<*>>.minus(rhs: Quantity<QuadraticIntermediateSymbol>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value - rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.to(rhs.unit)!!.value - rhs.value, rhs.unit)
        } else {
            Quantity(this.value - rhs.to(this.unit)!!.value, this.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("QuantityLinearSymbolTimesQuantityQuadraticPolynomial")
operator fun Quantity<LinearIntermediateSymbol>.times(rhs: Quantity<AbstractQuadraticPolynomial<*>>): Quantity<QuadraticPolynomial> {
    return Quantity(this.value * rhs.value, this.unit * rhs.unit)
}

@JvmName("quantityLinearSymbolPlusQuantityQuadraticPolynomial")
operator fun Quantity<LinearIntermediateSymbol>.plus(rhs: Quantity<AbstractQuadraticPolynomial<*>>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value + rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.to(rhs.unit)!!.value + rhs.value, rhs.unit)
        } else {
            Quantity(this.value + rhs.to(this.unit)!!.value, this.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearSymbolMinusQuantityQuadraticPolynomial")
operator fun Quantity<LinearIntermediateSymbol>.minus(rhs: Quantity<AbstractQuadraticPolynomial<*>>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value - rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.to(rhs.unit)!!.value - rhs.value, rhs.unit)
        } else {
            Quantity(this.value - rhs.to(this.unit)!!.value, this.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityQuadraticSymbolTimesQuantityQuadraticPolynomial")
operator fun Quantity<QuadraticIntermediateSymbol>.times(rhs: Quantity<AbstractQuadraticPolynomial<*>>): Quantity<QuadraticPolynomial> {
    return Quantity(this.value * rhs.value, this.unit * rhs.unit)
}

@JvmName("quantityQuadraticSymbolPlusQuantityQuadraticPolynomial")
operator fun Quantity<QuadraticIntermediateSymbol>.plus(rhs: Quantity<AbstractQuadraticPolynomial<*>>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value + rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.to(rhs.unit)!!.value + rhs.value, rhs.unit)
        } else {
            Quantity(this.value + rhs.to(this.unit)!!.value, this.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityQuadraticSymbolMinusQuantityQuadraticPolynomial")
operator fun Quantity<QuadraticIntermediateSymbol>.minus(rhs: Quantity<AbstractQuadraticPolynomial<*>>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value - rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.to(rhs.unit)!!.value - rhs.value, rhs.unit)
        } else {
            Quantity(this.value - rhs.to(this.unit)!!.value, this.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

// monomial and monomial

operator fun LinearMonomial.plus(rhs: QuadraticMonomial): QuadraticPolynomial {
    return QuadraticPolynomial(
        monomials = listOf(QuadraticMonomial(this), rhs.copy())
    )
}

operator fun LinearMonomial.minus(rhs: QuadraticMonomial): QuadraticPolynomial {
    return QuadraticPolynomial(
        monomials = listOf(QuadraticMonomial(this), -rhs)
    )
}

operator fun QuadraticMonomial.plus(rhs: LinearMonomial): QuadraticPolynomial {
    return QuadraticPolynomial(
        monomials = listOf(this.copy(), QuadraticMonomial(rhs))
    )
}

operator fun QuadraticMonomial.minus(rhs: LinearMonomial): QuadraticPolynomial {
    return QuadraticPolynomial(
        monomials = listOf(this.copy(), QuadraticMonomial(-rhs))
    )
}

operator fun QuadraticMonomial.plus(rhs: QuadraticMonomial): QuadraticPolynomial {
    return QuadraticPolynomial(
        monomials = listOf(this.copy(), rhs.copy())
    )
}

operator fun QuadraticMonomial.minus(rhs: QuadraticMonomial): QuadraticPolynomial {
    return QuadraticPolynomial(
        monomials = listOf(this.copy(), -rhs)
    )
}

// quantity monomial and quantity monomial

@JvmName("quantityLinearMonomialPlusQuantityQuadraticMonomial")
operator fun Quantity<LinearMonomial>.plus(rhs: Quantity<QuadraticMonomial>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value + rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.to(rhs.unit)!!.value + rhs.value, rhs.unit)
        } else {
            Quantity(this.value + rhs.to(this.unit)!!.value, this.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearMonomialMinusQuantityQuadraticMonomial")
operator fun Quantity<LinearMonomial>.minus(rhs: Quantity<QuadraticMonomial>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value - rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.to(rhs.unit)!!.value - rhs.value, rhs.unit)
        } else {
            Quantity(this.value - rhs.to(this.unit)!!.value, this.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityQuadraticMonomialPlusQuantityLinearMonomial")
operator fun Quantity<QuadraticMonomial>.plus(rhs: Quantity<LinearMonomial>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value + rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.to(rhs.unit)!!.value + rhs.value, rhs.unit)
        } else {
            Quantity(this.value + rhs.to(this.unit)!!.value, this.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityQuadraticMonomialMinusQuantityLinearMonomial")
operator fun Quantity<QuadraticMonomial>.minus(rhs: Quantity<LinearMonomial>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value - rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.to(rhs.unit)!!.value - rhs.value, rhs.unit)
        } else {
            Quantity(this.value - rhs.to(this.unit)!!.value, this.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityQuadraticMonomialPlusQuantityQuadraticMonomial")
operator fun Quantity<QuadraticMonomial>.plus(rhs: Quantity<QuadraticMonomial>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value + rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.to(rhs.unit)!!.value + rhs.value, rhs.unit)
        } else {
            Quantity(this.value + rhs.to(this.unit)!!.value, this.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityQuadraticMonomialMinusQuantityQuadraticMonomial")
operator fun Quantity<QuadraticMonomial>.minus(rhs: Quantity<QuadraticMonomial>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value - rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.to(rhs.unit)!!.value - rhs.value, rhs.unit)
        } else {
            Quantity(this.value - rhs.to(this.unit)!!.value, this.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

// polynomial and monomial

operator fun LinearMonomial.times(rhs: AbstractLinearPolynomial<*>): QuadraticPolynomial {
    val newMonomials = rhs.monomials.map { this * it }.toMutableList()
    newMonomials.add(QuadraticMonomial(rhs.constant * this))
    return QuadraticPolynomial(monomials = newMonomials)
}

operator fun AbstractLinearPolynomial<*>.times(rhs: LinearMonomial): QuadraticPolynomial {
    val newMonomials = this.monomials.map { it * rhs }.toMutableList()
    newMonomials.add(QuadraticMonomial(this.constant * rhs))
    return QuadraticPolynomial(monomials = newMonomials)
}

operator fun LinearMonomial.plus(rhs: AbstractQuadraticPolynomial<*>): QuadraticPolynomial {
    val newMonomials = mutableListOf(QuadraticMonomial(this))
    newMonomials.addAll(rhs.monomials.map { it.copy() })
    return QuadraticPolynomial(
        monomials = newMonomials,
        constant = rhs.constant
    )
}

operator fun LinearMonomial.minus(rhs: AbstractQuadraticPolynomial<*>): QuadraticPolynomial {
    val newMonomials = mutableListOf(QuadraticMonomial(this))
    newMonomials.addAll(rhs.monomials.map { -it })
    return QuadraticPolynomial(
        monomials = newMonomials,
        constant = -rhs.constant
    )
}

@Throws(IllegalArgumentException::class)
operator fun LinearMonomial.times(rhs: AbstractQuadraticPolynomial<*>): QuadraticPolynomial {
    if (rhs.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of LinearMonomial.times: over quadratic.")
    }

    val newMonomials = rhs.monomials.map { this * it }.toMutableList()
    newMonomials.add(QuadraticMonomial(rhs.constant * this))
    return QuadraticPolynomial(monomials = newMonomials)
}

operator fun AbstractLinearPolynomial<*>.plus(rhs: QuadraticMonomial): QuadraticPolynomial {
    val newMonomials = this.monomials.map { QuadraticMonomial(it) }.toMutableList()
    newMonomials.add(rhs.copy())
    return QuadraticPolynomial(
        monomials = newMonomials,
        constant = this.constant
    )
}

operator fun AbstractLinearPolynomial<*>.minus(rhs: QuadraticMonomial): QuadraticPolynomial {
    val newMonomials = this.monomials.map { QuadraticMonomial(it) }.toMutableList()
    newMonomials.add(-rhs)
    return QuadraticPolynomial(
        monomials = newMonomials,
        constant = this.constant
    )
}

@Throws(IllegalArgumentException::class)
operator fun AbstractLinearPolynomial<*>.times(rhs: QuadraticMonomial): QuadraticPolynomial {
    if (rhs.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of LinearPolynomial.times: over quadratic.")
    }

    val newMonomials = this.monomials.map { it * rhs }.toMutableList()
    newMonomials.add(this.constant * rhs)
    return QuadraticPolynomial(monomials = newMonomials)
}

operator fun QuadraticMonomial.plus(rhs: AbstractLinearPolynomial<*>): QuadraticPolynomial {
    val newMonomials = mutableListOf(this.copy())
    newMonomials.addAll(rhs.monomials.map { QuadraticMonomial(it) })
    return QuadraticPolynomial(
        monomials = newMonomials,
        constant = rhs.constant
    )
}

operator fun QuadraticMonomial.minus(rhs: AbstractLinearPolynomial<*>): QuadraticPolynomial {
    val newMonomials = mutableListOf(this.copy())
    newMonomials.addAll(rhs.monomials.map { QuadraticMonomial(-it) })
    return QuadraticPolynomial(
        monomials = newMonomials,
        constant = -rhs.constant
    )
}

@Throws(IllegalArgumentException::class)
operator fun QuadraticMonomial.times(rhs: AbstractLinearPolynomial<*>): QuadraticPolynomial {
    if (this.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of QuadraticMonomial.times: over quadratic.")
    }

    val newMonomials = rhs.monomials.map { this * it }.toMutableList()
    newMonomials.add(rhs.constant * this)
    return QuadraticPolynomial(monomials = newMonomials)
}

operator fun QuadraticMonomial.plus(rhs: AbstractQuadraticPolynomial<*>): QuadraticPolynomial {
    val newMonomials = mutableListOf(this.copy())
    newMonomials.addAll(rhs.monomials.map { it.copy() })
    return QuadraticPolynomial(
        monomials = newMonomials,
        constant = rhs.constant
    )
}

operator fun QuadraticMonomial.minus(rhs: AbstractQuadraticPolynomial<*>): QuadraticPolynomial {
    val newMonomials = mutableListOf(this.copy())
    newMonomials.addAll(rhs.monomials.map { -it })
    return QuadraticPolynomial(
        monomials = newMonomials,
        constant = -rhs.constant
    )
}

@Throws(IllegalArgumentException::class)
operator fun QuadraticMonomial.times(rhs: AbstractQuadraticPolynomial<*>): QuadraticPolynomial {
    if (this.category == Quadratic || rhs.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of QuadraticMonomial.times: over quadratic.")
    }

    val newMonomials = rhs.monomials.map { this * it }.toMutableList()
    newMonomials.add(rhs.constant * this)
    return QuadraticPolynomial(monomials = newMonomials)
}

// quantity polynomial and monomial

@JvmName("linearMonomialTimesQuantityLinearPolynomial")
operator fun LinearMonomial.times(rhs: Quantity<AbstractLinearPolynomial<*>>): Quantity<QuadraticPolynomial> {
    return Quantity(this * rhs.value, rhs.unit)
}

@JvmName("quantityLinearPolynomialTimesLinearMonomial")
operator fun Quantity<AbstractLinearPolynomial<*>>.times(rhs: LinearMonomial): Quantity<QuadraticPolynomial> {
    return Quantity(this.value * rhs, this.unit)
}

@JvmName("linearMonomialTimesQuantityQuadraticPolynomial")
operator fun LinearMonomial.times(rhs: Quantity<AbstractQuadraticPolynomial<*>>): Quantity<QuadraticPolynomial> {
    return Quantity(this * rhs.value, rhs.unit)
}

@JvmName("quantityQuadraticPolynomialTimesLinearMonomial")
operator fun Quantity<AbstractQuadraticPolynomial<*>>.times(rhs: LinearMonomial): Quantity<QuadraticPolynomial> {
    return Quantity(QuadraticPolynomial(this.value * rhs), this.unit)
}

@JvmName("quadraticMonomialPlusQuantityLinearPolynomial")
operator fun QuadraticMonomial.times(rhs: Quantity<AbstractLinearPolynomial<*>>): Quantity<QuadraticPolynomial> {
    return Quantity(this * rhs.value, rhs.unit)
}

@JvmName("quantityLinearPolynomialPlusQuadraticMonomial")
operator fun Quantity<AbstractLinearPolynomial<*>>.times(rhs: QuadraticMonomial): Quantity<QuadraticPolynomial> {
    return Quantity(this.value * rhs, this.unit)
}

@JvmName("quadraticMonomialTimesQuantityQuadraticPolynomial")
operator fun QuadraticMonomial.times(rhs: Quantity<AbstractQuadraticPolynomial<*>>): Quantity<QuadraticPolynomial> {
    return Quantity(this * rhs.value, rhs.unit)
}

@JvmName("quantityQuadraticPolynomialTimesQuadraticMonomial")
operator fun Quantity<AbstractQuadraticPolynomial<*>>.times(rhs: QuadraticMonomial): Quantity<QuadraticPolynomial> {
    return Quantity(QuadraticPolynomial(this.value * rhs), this.unit)
}

// polynomial and quantity monomial

@JvmName("quantityLinearMonomialTimesLinearPolynomial")
operator fun Quantity<LinearMonomial>.times(rhs: AbstractLinearPolynomial<*>): Quantity<QuadraticPolynomial> {
    return Quantity(this.value * rhs, this.unit)
}

@JvmName("linearPolynomialTimesQuantityLinearMonomial")
operator fun AbstractLinearPolynomial<*>.times(rhs: Quantity<LinearMonomial>): Quantity<QuadraticPolynomial> {
    return Quantity(this * rhs.value, rhs.unit)
}

@JvmName("quantityLinearMonomialTimesQuadraticPolynomial")
operator fun Quantity<LinearMonomial>.times(rhs: AbstractQuadraticPolynomial<*>): Quantity<QuadraticPolynomial> {
    return Quantity(this.value * rhs, this.unit)
}

@JvmName("quadraticPolynomialTimesQuantityLinearMonomial")
operator fun AbstractQuadraticPolynomial<*>.times(rhs: Quantity<LinearMonomial>): Quantity<QuadraticPolynomial> {
    return Quantity(QuadraticPolynomial(this * rhs.value), rhs.unit)
}

@JvmName("quantityQuadraticMonomialTimesLinearPolynomial")
operator fun Quantity<QuadraticMonomial>.times(rhs: AbstractLinearPolynomial<*>): Quantity<QuadraticPolynomial> {
    return Quantity(this.value * rhs, this.unit)
}

@JvmName("linearPolynomialTimesQuantityQuadraticMonomial")
operator fun AbstractLinearPolynomial<*>.times(rhs: Quantity<QuadraticMonomial>): Quantity<QuadraticPolynomial> {
    return Quantity(this * rhs.value, rhs.unit)
}

@JvmName("quantityQuadraticMonomialTimesQuadraticPolynomial")
operator fun Quantity<QuadraticMonomial>.times(rhs: AbstractQuadraticPolynomial<*>): Quantity<QuadraticPolynomial> {
    return Quantity(this.value * rhs, this.unit)
}

@JvmName("quadraticPolynomialTimesQuantityQuadraticMonomial")
operator fun AbstractQuadraticPolynomial<*>.times(rhs: Quantity<QuadraticMonomial>): Quantity<QuadraticPolynomial> {
    return Quantity(QuadraticPolynomial(this * rhs.value), rhs.unit)
}

// quantity polynomial and quantity monomial

@JvmName("quantityLinearMonomialTimesQuantityLinearPolynomial")
operator fun Quantity<LinearMonomial>.times(rhs: Quantity<AbstractLinearPolynomial<*>>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value * rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.to(rhs.unit)!!.value * rhs.value, rhs.unit)
        } else {
            Quantity(this.value * rhs.to(this.unit)!!.value, this.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearPolynomialTimesQuantityLinearMonomial")
operator fun Quantity<AbstractLinearPolynomial<*>>.times(rhs: Quantity<LinearMonomial>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value * rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.to(rhs.unit)!!.value * rhs.value, rhs.unit)
        } else {
            Quantity(this.value * rhs.to(this.unit)!!.value, this.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearMonomialPlusQuantityQuadraticPolynomial")
operator fun Quantity<LinearMonomial>.plus(rhs: Quantity<AbstractQuadraticPolynomial<*>>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value + rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.to(rhs.unit)!!.value + rhs.value, rhs.unit)
        } else {
            Quantity(this.value + rhs.to(this.unit)!!.value, this.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearMonomialMinusQuantityQuadraticPolynomial")
operator fun Quantity<LinearMonomial>.minus(rhs: Quantity<AbstractQuadraticPolynomial<*>>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value - rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.to(rhs.unit)!!.value - rhs.value, rhs.unit)
        } else {
            Quantity(this.value - rhs.to(this.unit)!!.value, this.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearMonomialTimesQuantityQuadraticPolynomial")
operator fun Quantity<LinearMonomial>.times(rhs: Quantity<AbstractQuadraticPolynomial<*>>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value * rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.to(rhs.unit)!!.value * rhs.value, rhs.unit)
        } else {
            Quantity(this.value * rhs.to(this.unit)!!.value, this.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityQuadraticPolynomialPlusQuantityLinearMonomial")
operator fun Quantity<AbstractQuadraticPolynomial<*>>.plus(rhs: Quantity<LinearMonomial>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(QuadraticPolynomial(this.value + rhs.value), this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(QuadraticPolynomial(this.to(rhs.unit)!!.value + rhs.value), rhs.unit)
        } else {
            Quantity(QuadraticPolynomial(this.value + rhs.to(this.unit)!!.value), this.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityQuadraticPolynomialMinusQuantityLinearMonomial")
operator fun Quantity<AbstractQuadraticPolynomial<*>>.minus(rhs: Quantity<LinearMonomial>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(QuadraticPolynomial(this.value - rhs.value), this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(QuadraticPolynomial(this.to(rhs.unit)!!.value - rhs.value), rhs.unit)
        } else {
            Quantity(QuadraticPolynomial(this.value - rhs.to(this.unit)!!.value), this.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityQuadraticPolynomialTimesQuantityLinearMonomial")
operator fun Quantity<AbstractQuadraticPolynomial<*>>.times(rhs: Quantity<LinearMonomial>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(QuadraticPolynomial(this.value * rhs.value), this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(QuadraticPolynomial(this.to(rhs.unit)!!.value * rhs.value), rhs.unit)
        } else {
            Quantity(QuadraticPolynomial(this.value * rhs.to(this.unit)!!.value), this.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityQuadraticMonomialPlusQuantityLinearPolynomial")
operator fun Quantity<QuadraticMonomial>.plus(rhs: Quantity<AbstractLinearPolynomial<*>>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value + rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.to(rhs.unit)!!.value + rhs.value, rhs.unit)
        } else {
            Quantity(this.value + rhs.to(this.unit)!!.value, this.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityQuadraticMonomialMinusQuantityLinearPolynomial")
operator fun Quantity<QuadraticMonomial>.minus(rhs: Quantity<AbstractLinearPolynomial<*>>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value - rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.to(rhs.unit)!!.value - rhs.value, rhs.unit)
        } else {
            Quantity(this.value - rhs.to(this.unit)!!.value, this.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityQuadraticMonomialTimesQuantityLinearPolynomial")
operator fun Quantity<QuadraticMonomial>.times(rhs: Quantity<AbstractLinearPolynomial<*>>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value * rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.to(rhs.unit)!!.value * rhs.value, rhs.unit)
        } else {
            Quantity(this.value * rhs.to(this.unit)!!.value, this.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearPolynomialPlusQuantityQuadraticMonomial")
operator fun Quantity<AbstractLinearPolynomial<*>>.plus(rhs: Quantity<QuadraticMonomial>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value + rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.to(rhs.unit)!!.value + rhs.value, rhs.unit)
        } else {
            Quantity(this.value + rhs.to(this.unit)!!.value, this.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearPolynomialMinusQuantityQuadraticMonomial")
operator fun Quantity<AbstractLinearPolynomial<*>>.minus(rhs: Quantity<QuadraticMonomial>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value - rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.to(rhs.unit)!!.value - rhs.value, rhs.unit)
        } else {
            Quantity(this.value - rhs.to(this.unit)!!.value, this.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearPolynomialTimesQuantityQuadraticMonomial")
operator fun Quantity<AbstractLinearPolynomial<*>>.times(rhs: Quantity<QuadraticMonomial>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value * rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.to(rhs.unit)!!.value * rhs.value, rhs.unit)
        } else {
            Quantity(this.value * rhs.to(this.unit)!!.value, this.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityQuadraticMonomialPlusQuantityQuadraticPolynomial")
operator fun Quantity<QuadraticMonomial>.plus(rhs: Quantity<AbstractQuadraticPolynomial<*>>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value + rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.to(rhs.unit)!!.value + rhs.value, rhs.unit)
        } else {
            Quantity(this.value + rhs.to(this.unit)!!.value, this.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityQuadraticMonomialMinusQuantityQuadraticPolynomial")
operator fun Quantity<QuadraticMonomial>.minus(rhs: Quantity<AbstractQuadraticPolynomial<*>>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value - rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.to(rhs.unit)!!.value - rhs.value, rhs.unit)
        } else {
            Quantity(this.value - rhs.to(this.unit)!!.value, this.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityQuadraticMonomialTimesQuantityQuadraticPolynomial")
operator fun Quantity<QuadraticMonomial>.times(rhs: Quantity<AbstractQuadraticPolynomial<*>>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value * rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.to(rhs.unit)!!.value * rhs.value, rhs.unit)
        } else {
            Quantity(this.value * rhs.to(this.unit)!!.value, this.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityQuadraticPolynomialPlusQuantityQuadraticMonomial")
operator fun Quantity<AbstractQuadraticPolynomial<*>>.plus(rhs: Quantity<QuadraticMonomial>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(QuadraticPolynomial(this.value + rhs.value), this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(QuadraticPolynomial(this.to(rhs.unit)!!.value + rhs.value), rhs.unit)
        } else {
            Quantity(QuadraticPolynomial(this.value + rhs.to(this.unit)!!.value), this.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityQuadraticPolynomialMinusQuantityQuadraticMonomial")
operator fun Quantity<AbstractQuadraticPolynomial<*>>.minus(rhs: Quantity<QuadraticMonomial>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(QuadraticPolynomial(this.value - rhs.value), this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(QuadraticPolynomial(this.to(rhs.unit)!!.value - rhs.value), rhs.unit)
        } else {
            Quantity(QuadraticPolynomial(this.value - rhs.to(this.unit)!!.value), this.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityQuadraticPolynomialTimesQuantityQuadraticMonomial")
operator fun Quantity<AbstractQuadraticPolynomial<*>>.times(rhs: Quantity<QuadraticMonomial>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(QuadraticPolynomial(this.value * rhs.value), this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(QuadraticPolynomial(this.to(rhs.unit)!!.value * rhs.value), rhs.unit)
        } else {
            Quantity(QuadraticPolynomial(this.value * rhs.to(this.unit)!!.value), this.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

// polynomial and polynomial

operator fun AbstractLinearPolynomial<*>.times(rhs: AbstractLinearPolynomial<*>): QuadraticPolynomial {
    val newMonomials = this.monomials.flatMap { monomial1 -> rhs.monomials.map { monomial2 -> monomial1 * monomial2 } }.toMutableList()
    newMonomials.addAll(rhs.monomials.map { QuadraticMonomial(this.constant * it) })
    newMonomials.addAll(this.monomials.map { QuadraticMonomial(rhs.constant * it) })
    return QuadraticPolynomial(
        monomials = newMonomials,
        constant = this.constant * rhs.constant
    )
}

operator fun AbstractLinearPolynomial<*>.plus(rhs: AbstractQuadraticPolynomial<*>): QuadraticPolynomial {
    val newMonomials = this.monomials.map { QuadraticMonomial(it) }.toMutableList()
    newMonomials.addAll(rhs.monomials.map { it.copy() })
    return QuadraticPolynomial(
        monomials = newMonomials,
        constant = this.constant + rhs.constant
    )
}

operator fun AbstractLinearPolynomial<*>.minus(rhs: AbstractQuadraticPolynomial<*>): QuadraticPolynomial {
    val newMonomials = this.monomials.map { QuadraticMonomial(it) }.toMutableList()
    newMonomials.addAll(rhs.monomials.map { -it })
    return QuadraticPolynomial(
        monomials = newMonomials,
        constant = this.constant - rhs.constant
    )
}

@Throws(IllegalArgumentException::class)
operator fun AbstractLinearPolynomial<*>.times(rhs: AbstractQuadraticPolynomial<*>): QuadraticPolynomial {
    if (rhs.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of QuadraticMonomial.times: over quadratic.")
    }

    val newMonomials = this.monomials.flatMap { monomial1 -> rhs.monomials.map { monomial2 -> monomial1 * monomial2 } }.toMutableList()
    newMonomials.addAll(rhs.monomials.map { this.constant * it })
    newMonomials.addAll(this.monomials.map { QuadraticMonomial(rhs.constant * it) })
    return QuadraticPolynomial(
        monomials = newMonomials,
        constant = this.constant * rhs.constant
    )
}

// quantity polynomial and polynomial

@JvmName("quantityLinearPolynomialTimesLinearPolynomial")
operator fun Quantity<AbstractLinearPolynomial<*>>.times(rhs: AbstractLinearPolynomial<*>): Quantity<QuadraticPolynomial> {
    return Quantity(this.value * rhs, this.unit)
}

@JvmName("linearPolynomialTimesQuantityLinearPolynomial")
operator fun AbstractLinearPolynomial<*>.times(rhs: Quantity<AbstractLinearPolynomial<*>>): Quantity<QuadraticPolynomial> {
    return Quantity(this * rhs.value, rhs.unit)
}

@JvmName("quantityLinearPolynomialTimesQuadraticPolynomial")
operator fun Quantity<AbstractLinearPolynomial<*>>.times(rhs: AbstractQuadraticPolynomial<*>): Quantity<QuadraticPolynomial> {
    return Quantity(this.value * rhs, this.unit)
}

@JvmName("quadraticPolynomialTimesQuantityLinearPolynomial")
operator fun AbstractQuadraticPolynomial<*>.times(rhs: Quantity<AbstractLinearPolynomial<*>>): Quantity<QuadraticPolynomial> {
    return Quantity(QuadraticPolynomial(this * rhs.value), rhs.unit)
}

@JvmName("linearPolynomialTimesQuantityQuadraticPolynomial")
operator fun AbstractLinearPolynomial<*>.times(rhs: Quantity<AbstractQuadraticPolynomial<*>>): Quantity<QuadraticPolynomial> {
    return Quantity(this * rhs.value, rhs.unit)
}

@JvmName("quantityQuadraticPolynomialTimesLinearPolynomial")
operator fun Quantity<AbstractQuadraticPolynomial<*>>.times(rhs: AbstractLinearPolynomial<*>): Quantity<QuadraticPolynomial> {
    return Quantity(QuadraticPolynomial(this.value * rhs), this.unit)
}

@JvmName("quadraticPolynomialTimesQuantityQuadraticPolynomial")
operator fun AbstractQuadraticPolynomial<*>.times(rhs: Quantity<AbstractQuadraticPolynomial<*>>): Quantity<QuadraticPolynomial> {
    return Quantity(QuadraticPolynomial(this * rhs.value), rhs.unit)
}

@JvmName("quantityQuadraticPolynomialTimesQuadraticPolynomial")
operator fun Quantity<AbstractQuadraticPolynomial<*>>.times(rhs: AbstractQuadraticPolynomial<*>): Quantity<QuadraticPolynomial> {
    return Quantity(QuadraticPolynomial(this.value * rhs), this.unit)
}

// quantity polynomial and quantity polynomial

@JvmName("quantityLinearPolynomialTimesQuantityLinearPolynomial")
operator fun Quantity<AbstractLinearPolynomial<*>>.times(rhs: Quantity<AbstractLinearPolynomial<*>>): Quantity<QuadraticPolynomial> {
    return Quantity(this.value * rhs.value, this.unit * rhs.unit)
}

@JvmName("quantityLinearPolynomialPlusQuantityQuadraticPolynomial")
operator fun Quantity<AbstractLinearPolynomial<*>>.plus(rhs: Quantity<AbstractQuadraticPolynomial<*>>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value + rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.to(rhs.unit)!!.value + rhs.value, rhs.unit)
        } else {
            Quantity(this.value + rhs.to(this.unit)!!.value, this.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearPolynomialMinusQuantityQuadraticPolynomial")
operator fun Quantity<AbstractLinearPolynomial<*>>.minus(rhs: Quantity<AbstractQuadraticPolynomial<*>>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value - rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.to(rhs.unit)!!.value - rhs.value, rhs.unit)
        } else {
            Quantity(this.value - rhs.to(this.unit)!!.value, this.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearPolynomialTimesQuantityQuadraticPolynomial")
operator fun Quantity<AbstractLinearPolynomial<*>>.times(rhs: Quantity<AbstractQuadraticPolynomial<*>>): Quantity<QuadraticPolynomial> {
    return Quantity(this.value * rhs.value, this.unit * rhs.unit)
}

@JvmName("quantityQuadraticPolynomialPlusQuantityLinearPolynomial")
operator fun Quantity<AbstractQuadraticPolynomial<*>>.plus(rhs: Quantity<AbstractLinearPolynomial<*>>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(QuadraticPolynomial(this.value + rhs.value), this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(QuadraticPolynomial(this.to(rhs.unit)!!.value + rhs.value), rhs.unit)
        } else {
            Quantity(QuadraticPolynomial(this.value + rhs.to(this.unit)!!.value), this.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityQuadraticPolynomialMinusQuantityLinearPolynomial")
operator fun Quantity<AbstractQuadraticPolynomial<*>>.minus(rhs: Quantity<AbstractLinearPolynomial<*>>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(QuadraticPolynomial(this.value - rhs.value), this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(QuadraticPolynomial(this.to(rhs.unit)!!.value - rhs.value), rhs.unit)
        } else {
            Quantity(QuadraticPolynomial(this.value - rhs.to(this.unit)!!.value), this.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityQuadraticPolynomialTimesQuantityLinearPolynomial")
operator fun Quantity<AbstractQuadraticPolynomial<*>>.times(rhs: Quantity<AbstractLinearPolynomial<*>>): Quantity<QuadraticPolynomial> {
    return Quantity(QuadraticPolynomial(this.value * rhs.value), this.unit * rhs.unit)
}

@JvmName("quantityQuadraticPolynomialPlusQuantityQuadraticPolynomial")
operator fun Quantity<AbstractQuadraticPolynomial<*>>.plus(rhs: Quantity<AbstractQuadraticPolynomial<*>>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(QuadraticPolynomial(this.value + rhs.value), this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(QuadraticPolynomial(this.to(rhs.unit)!!.value + rhs.value), rhs.unit)
        } else {
            Quantity(QuadraticPolynomial(this.value + rhs.to(this.unit)!!.value), this.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityQuadraticPolynomialMinusQuantityQuadraticPolynomial")
operator fun Quantity<AbstractQuadraticPolynomial<*>>.minus(rhs: Quantity<AbstractQuadraticPolynomial<*>>): Quantity<QuadraticPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(QuadraticPolynomial(this.value - rhs.value), this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(QuadraticPolynomial(this.to(rhs.unit)!!.value - rhs.value), rhs.unit)
        } else {
            Quantity(QuadraticPolynomial(this.value - rhs.to(this.unit)!!.value), this.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityQuadraticPolynomialTimesQuantityQuadraticPolynomial")
operator fun Quantity<AbstractQuadraticPolynomial<*>>.times(rhs: Quantity<AbstractQuadraticPolynomial<*>>): Quantity<QuadraticPolynomial> {
    return Quantity(QuadraticPolynomial(this.value * rhs.value), this.unit * rhs.unit)
}

// sigma

@JvmName("sumQuadraticSymbols")
fun qsum(
    symbols: Iterable<QuadraticIntermediateSymbol>,
    ctor: (QuadraticIntermediateSymbol) -> QuadraticMonomial = { QuadraticMonomial(it) }
): QuadraticPolynomial {
    val monomials = ArrayList<QuadraticMonomial>()
    for (symbol in symbols) {
        monomials.add(ctor(symbol))
    }
    return QuadraticPolynomial(monomials = monomials)
}

@JvmName("sumQuadraticMonomials")
fun qsum(monomials: Iterable<QuadraticMonomial>): QuadraticPolynomial {
    return QuadraticPolynomial(monomials = monomials.toList())
}

@JvmName("sumQuadraticPolynomials")
fun qsum(polynomials: Iterable<AbstractQuadraticPolynomial<*>>): QuadraticPolynomial {
    val monomials = ArrayList<QuadraticMonomial>()
    var constant = Flt64.zero
    for (polynomial in polynomials) {
        monomials.addAll(polynomial.monomials)
        constant += polynomial.constant
    }
    return QuadraticPolynomial(monomials = monomials, constant = constant)
}

fun <T> qsumSymbols(
    objs: Iterable<T>,
    ctor: (T) -> QuadraticIntermediateSymbol?
): QuadraticPolynomial {
    return qsum(objs.mapNotNull(ctor))
}

fun <T> qsum(
    objs: Iterable<T>,
    ctor: (T) -> QuadraticMonomial?
): QuadraticPolynomial {
    return qsum(objs.mapNotNull(ctor))
}

@JvmName("sumMapQuadraticMonomials")
fun <T> flatQSum(
    objs: Iterable<T>,
    ctor: (T) -> Iterable<QuadraticMonomial?>
): QuadraticPolynomial {
    return qsum(objs.flatMap(ctor).filterNotNull())
}

// quantity sigma

@JvmName("sumQuantityLinearMonomials")
fun qtyQSum(monomials: Iterable<Quantity<QuadraticMonomial>>): Quantity<QuadraticPolynomial> {
    val quantityMonomials = monomials.toList()
    return if (quantityMonomials.isEmpty()) {
        Quantity(QuadraticPolynomial(), NoneUnit)
    } else {
        quantityMonomials.subList(1, quantityMonomials.size)
            .fold(Quantity(QuadraticPolynomial(monomials = listOf(quantityMonomials.first().value)), quantityMonomials.first().unit)) { acc, quantity ->
                acc + quantity
            }
    }
}

@JvmName("sumQuantityLinearPolynomials")
fun qtyQSum(polynomials: Iterable<Quantity<AbstractQuadraticPolynomial<*>>>): Quantity<QuadraticPolynomial> {
    val quantityPolynomials = polynomials.toList()
    return if (quantityPolynomials.isEmpty()) {
        Quantity(QuadraticPolynomial(), NoneUnit)
    } else {
        quantityPolynomials.subList(1, quantityPolynomials.size)
            .fold(Quantity(QuadraticPolynomial(quantityPolynomials.first().value), quantityPolynomials.first().unit)) { acc, quantity ->
                acc + quantity
            }
    }
}

@JvmName("sumMapQuantityMonomials")
fun <T> qtyQSum(
    objs: Iterable<T>,
    extractor: (T) -> Quantity<QuadraticMonomial>?
): Quantity<QuadraticPolynomial> {
    return qtyQSum(objs.mapNotNull(extractor))
}

@JvmName("sumMapQuantityMonomialLists")
fun <T> flatQtyQSum(
    objs: Iterable<T>,
    extractor: (T) -> Iterable<Quantity<QuadraticMonomial>?>
): Quantity<QuadraticPolynomial> {
    return qtyQSum(objs.flatMap(extractor).filterNotNull())
}
