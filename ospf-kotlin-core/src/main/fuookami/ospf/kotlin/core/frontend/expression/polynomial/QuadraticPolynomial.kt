package fuookami.ospf.kotlin.core.frontend.expression.polynomial

import fuookami.ospf.kotlin.utils.math.*
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

    override val dependencies: Set<Symbol>
        get() {
            return monomials.flatMapNotNull {
                val symbols = ArrayList<Symbol>()
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

    abstract operator fun plus(rhs: LinearSymbol): Self

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("plusLinearSymbols")
    abstract operator fun plus(rhs: Iterable<LinearSymbol>): Self
    abstract operator fun plus(rhs: QuadraticSymbol): Self

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("plusQuadraticSymbols")
    abstract operator fun plus(rhs: Iterable<QuadraticSymbol>): Self
    abstract operator fun plus(rhs: LinearMonomial): Self
    abstract operator fun plus(rhs: AbstractLinearPolynomial<*>): Self

    abstract operator fun minus(rhs: LinearSymbol): Self

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("minusLinearSymbols")
    abstract operator fun minus(rhs: Iterable<LinearSymbol>): Self
    abstract operator fun minus(rhs: QuadraticSymbol): Self

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("minusQuadraticSymbols")
    abstract operator fun minus(rhs: Iterable<QuadraticSymbol>): Self
    abstract operator fun minus(rhs: LinearMonomial): Self
    abstract operator fun minus(rhs: AbstractLinearPolynomial<*>): Self

    abstract operator fun times(rhs: AbstractVariableItem<*, *>): Self

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("timesVariables")
    abstract operator fun times(rhs: Iterable<AbstractVariableItem<*, *>>): Self

    abstract operator fun times(rhs: LinearSymbol): Self

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("timesLinearSymbols")
    abstract operator fun times(rhs: Iterable<LinearSymbol>): Self

    abstract operator fun times(rhs: QuadraticSymbol): Self

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("timesQuadraticSymbols")
    abstract operator fun times(rhs: Iterable<QuadraticSymbol>): Self
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
            symbol: LinearSymbol,
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
            symbol: QuadraticSymbol,
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

    override fun plus(rhs: LinearSymbol): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(QuadraticMonomial(rhs))
        return QuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("plusLinearSymbols")
    override fun plus(rhs: Iterable<LinearSymbol>): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.map { QuadraticMonomial(it) })
        return QuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    override fun plus(rhs: QuadraticSymbol): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(QuadraticMonomial(rhs))
        return QuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("plusQuadraticSymbols")
    override fun plus(rhs: Iterable<QuadraticSymbol>): QuadraticPolynomial {
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

    override fun minus(rhs: LinearSymbol): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(QuadraticMonomial(-Flt64.one, rhs))
        return QuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("minusLinearSymbols")
    override fun minus(rhs: Iterable<LinearSymbol>): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.map { QuadraticMonomial(-Flt64.one, it) })
        return QuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    override fun minus(rhs: QuadraticSymbol): QuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(QuadraticMonomial(-Flt64.one, rhs))
        return QuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("minusQuadraticSymbols")
    override fun minus(rhs: Iterable<QuadraticSymbol>): QuadraticPolynomial {
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
    override fun times(rhs: LinearSymbol): QuadraticPolynomial {
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
    override fun times(rhs: Iterable<LinearSymbol>): QuadraticPolynomial {
        if (this.category == Quadratic) {
            throw IllegalArgumentException("Invalid argument of QuadraticPolynomial.times: over quadratic.")
        }

        val newMonomials = monomials.flatMap { monomial -> rhs.map { monomial * it } }.toMutableList()
        newMonomials.addAll(rhs.map { QuadraticMonomial(constant, it) })
        return QuadraticPolynomial(monomials = newMonomials)
    }

    @Throws(IllegalArgumentException::class)
    override fun times(rhs: QuadraticSymbol): QuadraticPolynomial {
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
    override fun times(rhs: Iterable<QuadraticSymbol>): QuadraticPolynomial {
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
            symbol: LinearSymbol,
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
            symbol: QuadraticSymbol,
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

    override fun plus(rhs: LinearSymbol): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(QuadraticMonomial(rhs))
        return MutableQuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("plusLinearSymbols")
    override fun plus(rhs: Iterable<LinearSymbol>): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.map { QuadraticMonomial(it) })
        return MutableQuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    override fun plus(rhs: QuadraticSymbol): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(QuadraticMonomial(rhs))
        return MutableQuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("plusQuadraticSymbols")
    override fun plus(rhs: Iterable<QuadraticSymbol>): MutableQuadraticPolynomial {
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

    operator fun plusAssign(rhs: LinearSymbol) {
        monomials.add(QuadraticMonomial(rhs))
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("plusAssignLinearSymbols")
    operator fun plusAssign(rhs: Iterable<LinearSymbol>) {
        monomials.addAll(rhs.map { QuadraticMonomial(it) })
    }

    operator fun plusAssign(rhs: QuadraticSymbol) {
        monomials.add(QuadraticMonomial(rhs))
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("plusAssignQuadraticSymbols")
    operator fun plusAssign(rhs: Iterable<QuadraticSymbol>) {
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

    override fun minus(rhs: LinearSymbol): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(QuadraticMonomial(-Flt64.one, rhs))
        return MutableQuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("minusLinearSymbols")
    override fun minus(rhs: Iterable<LinearSymbol>): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.map { QuadraticMonomial(-Flt64.one, it) })
        return MutableQuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    override fun minus(rhs: QuadraticSymbol): MutableQuadraticPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(QuadraticMonomial(-Flt64.one, rhs))
        return MutableQuadraticPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("minusQuadraticSymbols")
    override fun minus(rhs: Iterable<QuadraticSymbol>): MutableQuadraticPolynomial {
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

    operator fun minusAssign(rhs: LinearSymbol) {
        monomials.add(QuadraticMonomial(-Flt64.one, rhs))
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("minusAssignLinearSymbols")
    operator fun minusAssign(rhs: Iterable<LinearSymbol>) {
        monomials.addAll(rhs.map { QuadraticMonomial(-Flt64.one, it) })
    }

    operator fun minusAssign(rhs: QuadraticSymbol) {
        monomials.add(QuadraticMonomial(-Flt64.one, rhs))
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("minusAssignQuadraticSymbols")
    operator fun minusAssign(rhs: Iterable<QuadraticSymbol>) {
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
    override fun times(rhs: LinearSymbol): MutableQuadraticPolynomial {
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
    override fun times(rhs: Iterable<LinearSymbol>): MutableQuadraticPolynomial {
        if (this.category == Quadratic) {
            throw IllegalArgumentException("Invalid argument of MutableQuadraticPolynomial.times: over quadratic.")
        }

        val newMonomials = monomials.flatMap { monomial -> rhs.map { monomial * it } }.toMutableList()
        newMonomials.addAll(rhs.map { QuadraticMonomial(constant, it) })
        return MutableQuadraticPolynomial(monomials = newMonomials)
    }

    @Throws(IllegalArgumentException::class)
    override fun times(rhs: QuadraticSymbol): MutableQuadraticPolynomial {
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
    override fun times(rhs: Iterable<QuadraticSymbol>): MutableQuadraticPolynomial {
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
    operator fun timesAssign(rhs: LinearSymbol) {
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
    operator fun timesAssign(rhs: Iterable<LinearSymbol>) {
        if (this.category == Quadratic) {
            throw IllegalArgumentException("Invalid argument of MutableQuadraticPolynomial.timesAssign: over quadratic.")
        }

        monomials = monomials.flatMap { monomial -> rhs.map { monomial * it } }.toMutableList()
        monomials.addAll(rhs.map { QuadraticMonomial(constant, it) })
        constant = Flt64.zero
    }

    @Throws(IllegalArgumentException::class)
    operator fun timesAssign(rhs: QuadraticSymbol) {
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
    operator fun timesAssign(rhs: Iterable<QuadraticSymbol>) {
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

// symbol and constant

operator fun QuadraticSymbol.plus(rhs: Int): QuadraticPolynomial {
    return this.plus(Flt64(rhs))
}

operator fun QuadraticSymbol.plus(rhs: Double): QuadraticPolynomial {
    return this.plus(Flt64(rhs))
}

operator fun <T : RealNumber<T>> QuadraticSymbol.plus(rhs: T): QuadraticPolynomial {
    return QuadraticPolynomial(
        monomials = listOf(QuadraticMonomial(this)),
        constant = rhs.toFlt64()
    )
}

operator fun QuadraticSymbol.minus(rhs: Int): QuadraticPolynomial {
    return this.minus(Flt64(rhs))
}

operator fun QuadraticSymbol.minus(rhs: Double): QuadraticPolynomial {
    return this.minus(Flt64(rhs))
}

operator fun <T : RealNumber<T>> QuadraticSymbol.minus(rhs: T): QuadraticPolynomial {
    return QuadraticPolynomial(
        monomials = listOf(QuadraticMonomial(this)),
        constant = -rhs.toFlt64()
    )
}

operator fun Int.plus(rhs: QuadraticSymbol): QuadraticPolynomial {
    return Flt64(this).plus(rhs)
}

operator fun Double.plus(rhs: QuadraticSymbol): QuadraticPolynomial {
    return Flt64(this).plus(rhs)
}

operator fun <T : RealNumber<T>> T.plus(rhs: QuadraticSymbol): QuadraticPolynomial {
    return QuadraticPolynomial(
        monomials = listOf(QuadraticMonomial(rhs)),
        constant = this.toFlt64()
    )
}

operator fun Int.minus(rhs: QuadraticSymbol): QuadraticPolynomial {
    return Flt64(this).minus(rhs)
}

operator fun Double.minus(rhs: QuadraticSymbol): QuadraticPolynomial {
    return Flt64(this).minus(rhs)
}

operator fun <T : RealNumber<T>> T.minus(rhs: QuadraticSymbol): QuadraticPolynomial {
    return QuadraticPolynomial(
        monomials = listOf(QuadraticMonomial(-Flt64.one, rhs)),
        constant = this.toFlt64()
    )
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

// symbol and variable

operator fun QuadraticSymbol.plus(rhs: AbstractVariableItem<*, *>): QuadraticPolynomial {
    return QuadraticPolynomial(monomials = listOf(QuadraticMonomial(this), QuadraticMonomial(rhs)))
}

operator fun QuadraticSymbol.minus(rhs: AbstractVariableItem<*, *>): QuadraticPolynomial {
    return QuadraticPolynomial(monomials = listOf(QuadraticMonomial(this), QuadraticMonomial(-Flt64.one, rhs)))
}

operator fun AbstractVariableItem<*, *>.plus(rhs: QuadraticSymbol): QuadraticPolynomial {
    return QuadraticPolynomial(monomials = listOf(QuadraticMonomial(this), QuadraticMonomial(rhs)))
}

operator fun AbstractVariableItem<*, *>.minus(rhs: QuadraticSymbol): QuadraticPolynomial {
    return QuadraticPolynomial(monomials = listOf(QuadraticMonomial(this), QuadraticMonomial(-Flt64.one, rhs)))
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

// symbol and symbol

operator fun QuadraticSymbol.plus(rhs: LinearSymbol): QuadraticPolynomial {
    return QuadraticPolynomial(monomials = listOf(QuadraticMonomial(this), QuadraticMonomial(rhs)))
}

operator fun QuadraticSymbol.minus(rhs: LinearSymbol): QuadraticPolynomial {
    return QuadraticPolynomial(monomials = listOf(QuadraticMonomial(this), QuadraticMonomial(-Flt64.one, rhs)))
}

operator fun LinearSymbol.plus(rhs: QuadraticSymbol): QuadraticPolynomial {
    return QuadraticPolynomial(monomials = listOf(QuadraticMonomial(this), QuadraticMonomial(rhs)))
}

operator fun LinearSymbol.minus(rhs: QuadraticSymbol): QuadraticPolynomial {
    return QuadraticPolynomial(monomials = listOf(QuadraticMonomial(this), QuadraticMonomial(-Flt64.one, rhs)))
}

operator fun QuadraticSymbol.plus(rhs: QuadraticSymbol): QuadraticPolynomial {
    return QuadraticPolynomial(monomials = listOf(QuadraticMonomial(this), QuadraticMonomial(rhs)))
}

operator fun QuadraticSymbol.minus(rhs: QuadraticSymbol): QuadraticPolynomial {
    return QuadraticPolynomial(monomials = listOf(QuadraticMonomial(this), QuadraticMonomial(-Flt64.one, rhs)))
}

// monomial and symbol

operator fun LinearMonomial.plus(rhs: QuadraticSymbol): QuadraticPolynomial {
    return QuadraticPolynomial(monomials = listOf(QuadraticMonomial(this), QuadraticMonomial(rhs)))
}

operator fun LinearMonomial.minus(rhs: QuadraticSymbol): QuadraticPolynomial {
    return QuadraticPolynomial(monomials = listOf(QuadraticMonomial(this), QuadraticMonomial(-Flt64.one, rhs)))
}

operator fun QuadraticSymbol.plus(rhs: LinearMonomial): QuadraticPolynomial {
    return QuadraticPolynomial(monomials = listOf(QuadraticMonomial(this), QuadraticMonomial(rhs)))
}

operator fun QuadraticSymbol.minus(rhs: LinearMonomial): QuadraticPolynomial {
    return QuadraticPolynomial(monomials = listOf(QuadraticMonomial(this), QuadraticMonomial(-rhs)))
}

operator fun QuadraticMonomial.plus(rhs: LinearSymbol): QuadraticPolynomial {
    return QuadraticPolynomial(monomials = listOf(this.copy(), QuadraticMonomial(rhs)))
}

operator fun QuadraticMonomial.minus(rhs: LinearSymbol): QuadraticPolynomial {
    return QuadraticPolynomial(monomials = listOf(this.copy(), QuadraticMonomial(-Flt64.one, rhs)))
}

operator fun LinearSymbol.plus(rhs: QuadraticMonomial): QuadraticPolynomial {
    return QuadraticPolynomial(monomials = listOf(QuadraticMonomial(this), rhs.copy()))
}

operator fun LinearSymbol.minus(rhs: QuadraticMonomial): QuadraticPolynomial {
    return QuadraticPolynomial(monomials = listOf(QuadraticMonomial(this), -rhs))
}

operator fun QuadraticMonomial.plus(rhs: QuadraticSymbol): QuadraticPolynomial {
    return QuadraticPolynomial(monomials = listOf(this.copy(), QuadraticMonomial(rhs)))
}

operator fun QuadraticMonomial.minus(rhs: QuadraticSymbol): QuadraticPolynomial {
    return QuadraticPolynomial(monomials = listOf(this.copy(), QuadraticMonomial(-Flt64.one, rhs)))
}

operator fun QuadraticSymbol.plus(rhs: QuadraticMonomial): QuadraticPolynomial {
    return QuadraticPolynomial(monomials = listOf(QuadraticMonomial(this), rhs.copy()))
}

operator fun QuadraticSymbol.minus(rhs: QuadraticMonomial): QuadraticPolynomial {
    return QuadraticPolynomial(monomials = listOf(QuadraticMonomial(this), -rhs))
}

// polynomial and symbol

operator fun LinearSymbol.times(rhs: AbstractLinearPolynomial<*>): QuadraticPolynomial {
    val newMonomials = rhs.monomials.map { this * it }.toMutableList()
    newMonomials.add(QuadraticMonomial(rhs.constant, this))
    return QuadraticPolynomial(monomials = newMonomials)
}

operator fun AbstractLinearPolynomial<*>.times(rhs: LinearSymbol): QuadraticPolynomial {
    val newMonomials = this.monomials.map { it * rhs }.toMutableList()
    newMonomials.add(QuadraticMonomial(this.constant, rhs))
    return QuadraticPolynomial(monomials = newMonomials)
}

operator fun AbstractLinearPolynomial<*>.plus(rhs: QuadraticSymbol): QuadraticPolynomial {
    val newMonomials = this.monomials.map { QuadraticMonomial(it) }.toMutableList()
    newMonomials.add(QuadraticMonomial(rhs))
    return QuadraticPolynomial(
        monomials = newMonomials,
        constant = this.constant
    )
}

operator fun AbstractLinearPolynomial<*>.minus(rhs: QuadraticSymbol): QuadraticPolynomial {
    val newMonomials = this.monomials.map { QuadraticMonomial(it) }.toMutableList()
    newMonomials.add(QuadraticMonomial(-Flt64.one, rhs))
    return QuadraticPolynomial(
        monomials = newMonomials,
        constant = this.constant
    )
}

@Throws(IllegalArgumentException::class)
operator fun AbstractLinearPolynomial<*>.times(rhs: QuadraticSymbol): QuadraticPolynomial {
    if (rhs.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of QuadraticPolynomial.times: over quadratic.")
    }

    val newMonomials = this.monomials.map { it * rhs }.toMutableList()
    newMonomials.add(QuadraticMonomial(this.constant, rhs))
    return QuadraticPolynomial(monomials = newMonomials)
}

operator fun QuadraticSymbol.plus(rhs: AbstractLinearPolynomial<*>): QuadraticPolynomial {
    val newMonomials = mutableListOf(QuadraticMonomial(this))
    newMonomials.addAll(rhs.monomials.map { QuadraticMonomial(it) })
    return QuadraticPolynomial(
        monomials = newMonomials,
        constant = rhs.constant
    )
}

operator fun QuadraticSymbol.minus(rhs: AbstractLinearPolynomial<*>): QuadraticPolynomial {
    val newMonomials = mutableListOf(QuadraticMonomial(this))
    newMonomials.addAll(rhs.monomials.map { QuadraticMonomial(-it) })
    return QuadraticPolynomial(
        monomials = newMonomials,
        constant = -rhs.constant
    )
}

@Throws(IllegalArgumentException::class)
operator fun QuadraticSymbol.times(rhs: AbstractLinearPolynomial<*>): QuadraticPolynomial {
    if (this.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of QuadraticSymbol.times: over quadratic.")
    }

    val newMonomials = rhs.monomials.map { this * it }.toMutableList()
    newMonomials.add(QuadraticMonomial(rhs.constant, this))
    return QuadraticPolynomial(monomials = newMonomials)
}

operator fun QuadraticSymbol.plus(rhs: AbstractQuadraticPolynomial<*>): QuadraticPolynomial {
    val newMonomials = mutableListOf(QuadraticMonomial(this))
    newMonomials.addAll(rhs.monomials.map { it.copy() })
    return QuadraticPolynomial(
        monomials = newMonomials,
        constant = rhs.constant
    )
}

operator fun QuadraticSymbol.minus(rhs: AbstractQuadraticPolynomial<*>): QuadraticPolynomial {
    val newMonomials = mutableListOf(QuadraticMonomial(this))
    newMonomials.addAll(rhs.monomials.map { -it })
    return QuadraticPolynomial(
        monomials = newMonomials,
        constant = rhs.constant
    )
}

@Throws(IllegalArgumentException::class)
operator fun QuadraticSymbol.times(rhs: AbstractQuadraticPolynomial<*>): QuadraticPolynomial {
    if (this.category == Quadratic || rhs.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of QuadraticSymbol.times: over quadratic.")
    }

    val newMonomials = rhs.monomials.map { this * it }.toMutableList()
    newMonomials.add(QuadraticMonomial(rhs.constant, this))
    return QuadraticPolynomial(monomials = newMonomials)
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

// polynomial and monomial

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
    return QuadraticPolynomial(
        monomials = newMonomials,
        constant = -rhs.constant
    )
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

// sigma

@JvmName("sumQuadraticSymbols")
fun qsum(
    symbols: Iterable<QuadraticSymbol>,
    ctor: (QuadraticSymbol) -> QuadraticMonomial = { QuadraticMonomial(it) }
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

@JvmName("sumMapQuadraticSymbols")
fun <T> qsumSymbols(
    objs: Iterable<T>,
    ctor: (T) -> QuadraticSymbol?
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
