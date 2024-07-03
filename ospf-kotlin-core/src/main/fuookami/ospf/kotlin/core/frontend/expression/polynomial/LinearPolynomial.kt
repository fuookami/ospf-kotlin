package fuookami.ospf.kotlin.core.frontend.expression.polynomial

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*

@JvmName("calculateLinearPolynomialCells")
private fun cells(
    monomials: List<LinearMonomial>,
    constant: Flt64
): List<LinearMonomialCell> {
    val cells = HashMap<AbstractVariableItem<*, *>, Flt64>()
    var totalConstant = constant
    for (monomial in monomials) {
        val thisCells = monomial.cells
        for (cell in thisCells) {
            if (cell.isConstant) {
                totalConstant += cell.constant!!
            } else {
                cells[cell.pair!!.variable] = (cells[cell.pair!!.variable] ?: Flt64.zero) + cell.pair!!.coefficient
            }
        }
    }
    return cells.map { LinearMonomialCell(it.value, it.key) } + LinearMonomialCell(totalConstant)
}

sealed class AbstractLinearPolynomial<Self : AbstractLinearPolynomial<Self>> :
    Polynomial<Self, LinearMonomial, LinearMonomialCell> {
    abstract override val monomials: List<LinearMonomial>
    override val category get() = Linear

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
            return monomials.mapNotNull {
                when (val symbol = it.symbol.symbol) {
                    is Either.Right -> {
                        symbol.value
                    }

                    else -> {
                        null
                    }
                }
            }.toSet()
        }

    private var _cells: List<LinearMonomialCell> = emptyList()
    override val cells: List<LinearMonomialCell>
        get() {
            if (_cells.isEmpty()) {
                _cells = cells(monomials, constant)
            }
            return _cells
        }
    override val cached: Boolean = _cells.isNotEmpty()

    abstract operator fun plus(rhs: LinearSymbol): Self
    abstract operator fun plus(rhs: Iterable<LinearSymbol>): Self

    abstract operator fun minus(rhs: LinearSymbol): Self
    abstract operator fun minus(rhs: Iterable<LinearSymbol>): Self

    override fun toMutable(): MutableLinearPolynomial {
        return MutableLinearPolynomial(
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

class LinearPolynomial(
    override val monomials: List<LinearMonomial> = emptyList(),
    override val constant: Flt64 = Flt64.zero,
    override var name: String = "",
    override var displayName: String? = null
) : AbstractLinearPolynomial<LinearPolynomial>() {
    companion object {
        operator fun invoke(
            item: AbstractVariableItem<*, *>,
            name: String = "",
            displayName: String? = null
        ): LinearPolynomial {
            return LinearPolynomial(
                monomials = listOf(LinearMonomial(item)),
                name = name.ifEmpty { item.name },
                displayName = displayName
            )
        }

        operator fun invoke(
            symbol: LinearSymbol,
            name: String = "",
            displayName: String? = null
        ): LinearPolynomial {
            return LinearPolynomial(
                monomials = listOf(LinearMonomial(symbol)),
                name = name.ifEmpty { symbol.name },
                displayName = displayName ?: symbol.displayName
            )
        }

        operator fun invoke(
            monomial: LinearMonomial,
            name: String = "",
            displayName: String? = null
        ): LinearPolynomial {
            return LinearPolynomial(
                monomials = listOf(monomial),
                name = name.ifEmpty { monomial.name },
                displayName = displayName ?: monomial.displayName
            )
        }

        operator fun invoke(
            polynomial: AbstractLinearPolynomial<*>,
            name: String = "",
            displayName: String? = null
        ): LinearPolynomial {
            return LinearPolynomial(
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
        ): LinearPolynomial {
            return LinearPolynomial(
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
        ): LinearPolynomial {
            return LinearPolynomial(
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
        ): LinearPolynomial {
            return LinearPolynomial(
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

    override fun copy(): LinearPolynomial {
        return LinearPolynomial(
            monomials = monomials.map { it.copy() },
            constant = constant
        )
    }

    override fun copy(name: String, displayName: String?): LinearPolynomial {
        return LinearPolynomial(
            monomials = monomials.map { it.copy() },
            constant = constant,
            name = name,
            displayName = displayName
        )
    }

    override fun plus(rhs: AbstractVariableItem<*, *>): LinearPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(LinearMonomial(rhs))
        return LinearPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("plusVariables")
    override fun plus(rhs: Iterable<AbstractVariableItem<*, *>>): LinearPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.map { LinearMonomial(it) })
        return LinearPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    override fun plus(rhs: LinearSymbol): LinearPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(LinearMonomial(rhs))
        return LinearPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("plusSymbols")
    override fun plus(rhs: Iterable<LinearSymbol>): LinearPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.map { LinearMonomial(it) })
        return LinearPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    override fun plus(rhs: LinearMonomial): LinearPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(rhs.copy())
        return LinearPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    override fun plus(rhs: Polynomial<*, LinearMonomial, LinearMonomialCell>): LinearPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.monomials.map { it.copy() })
        return LinearPolynomial(
            monomials = newMonomials,
            constant = constant + rhs.constant
        )
    }

    override fun plus(rhs: Flt64): LinearPolynomial {
        return LinearPolynomial(
            monomials = monomials.map { it.copy() },
            constant = constant + rhs
        )
    }

    override fun minus(rhs: AbstractVariableItem<*, *>): LinearPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(LinearMonomial(-Flt64.one, rhs))
        return LinearPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("minusVariables")
    override fun minus(rhs: Iterable<AbstractVariableItem<*, *>>): LinearPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.map { LinearMonomial(-Flt64.one, it) })
        return LinearPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    override fun minus(rhs: LinearSymbol): LinearPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(LinearMonomial(-Flt64.one, rhs))
        return LinearPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("minusSymbols")
    override fun minus(rhs: Iterable<LinearSymbol>): LinearPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.map { LinearMonomial(-Flt64.one, it) })
        return LinearPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    override fun minus(rhs: LinearMonomial): LinearPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(-rhs)
        return LinearPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    override fun minus(rhs: Polynomial<*, LinearMonomial, LinearMonomialCell>): LinearPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.monomials.map { -it })
        return LinearPolynomial(
            monomials = newMonomials,
            constant = constant - rhs.constant
        )
    }

    override fun minus(rhs: Flt64): LinearPolynomial {
        return LinearPolynomial(
            monomials = monomials.map { it.copy() },
            constant = constant - rhs
        )
    }

    override fun times(rhs: Flt64): LinearPolynomial {
        return LinearPolynomial(
            monomials = monomials.map { rhs * it },
            constant = constant
        )
    }

    override fun div(rhs: Flt64): LinearPolynomial {
        return LinearPolynomial(
            monomials = monomials.map { it / rhs },
            constant = constant
        )
    }
}

class MutableLinearPolynomial(
    override var monomials: MutableList<LinearMonomial> = ArrayList(),
    override var constant: Flt64 = Flt64.zero,
    override var name: String = "",
    override var displayName: String? = null
) : AbstractLinearPolynomial<MutableLinearPolynomial>(),
    MutablePolynomial<MutableLinearPolynomial, LinearMonomial, LinearMonomialCell> {
    companion object {
        operator fun invoke(
            item: AbstractVariableItem<*, *>,
            name: String = "",
            displayName: String? = null
        ): MutableLinearPolynomial {
            return MutableLinearPolynomial(
                monomials = mutableListOf(LinearMonomial(item)),
                name = name.ifEmpty { item.name },
                displayName = displayName
            )
        }

        operator fun invoke(
            symbol: LinearSymbol,
            name: String = "",
            displayName: String? = null
        ): MutableLinearPolynomial {
            return MutableLinearPolynomial(
                monomials = mutableListOf(LinearMonomial(symbol)),
                name = name.ifEmpty { symbol.name },
                displayName = displayName ?: symbol.displayName
            )
        }

        operator fun invoke(
            monomial: LinearMonomial,
            name: String = "",
            displayName: String? = null
        ): MutableLinearPolynomial {
            return MutableLinearPolynomial(
                monomials = mutableListOf(monomial),
                name = name.ifEmpty { monomial.name },
                displayName = displayName ?: monomial.displayName
            )
        }

        operator fun invoke(
            polynomial: AbstractLinearPolynomial<*>,
            name: String = "",
            displayName: String? = null
        ): MutableLinearPolynomial {
            return MutableLinearPolynomial(
                monomials = polynomial.monomials.map { it.copy() }.toMutableList(),
                constant = polynomial.constant,
                name = name.ifEmpty { polynomial.name },
                displayName = displayName ?: polynomial.displayName
            )
        }

        operator fun <T : RealNumber<T>> invoke(
            constant: T,
            name: String = "",
            displayName: String? = null
        ): MutableLinearPolynomial {
            return MutableLinearPolynomial(
                monomials = mutableListOf(),
                constant = constant.toFlt64(),
                name = name,
                displayName = displayName
            )
        }
    }

    override fun toMutable(): MutableLinearPolynomial {
        return this
    }

    override fun asMutable(): MutableLinearPolynomial {
        return this
    }

    override fun copy(): MutableLinearPolynomial {
        return MutableLinearPolynomial(
            monomials = monomials.map { it.copy() }.toMutableList(),
            constant = constant
        )
    }

    override fun copy(name: String, displayName: String?): MutableLinearPolynomial {
        return MutableLinearPolynomial(
            monomials = monomials.map { it.copy() }.toMutableList(),
            constant = constant,
            name = name,
            displayName = displayName
        )
    }

    override fun plus(rhs: AbstractVariableItem<*, *>): MutableLinearPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(LinearMonomial(rhs))
        return MutableLinearPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("plusVariables")
    override fun plus(rhs: Iterable<AbstractVariableItem<*, *>>): MutableLinearPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.map { LinearMonomial(it) })
        return MutableLinearPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    override fun plus(rhs: LinearSymbol): MutableLinearPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(LinearMonomial(rhs))
        return MutableLinearPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("plusSymbols")
    override fun plus(rhs: Iterable<LinearSymbol>): MutableLinearPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.map { LinearMonomial(it) })
        return MutableLinearPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    override fun plus(rhs: LinearMonomial): MutableLinearPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(rhs.copy())
        return MutableLinearPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    override fun plus(rhs: Polynomial<*, LinearMonomial, LinearMonomialCell>): MutableLinearPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.addAll(rhs.monomials.map { it.copy() })
        return MutableLinearPolynomial(
            monomials = newMonomials,
            constant = constant + rhs.constant
        )
    }

    override fun plus(rhs: Flt64): MutableLinearPolynomial {
        return MutableLinearPolynomial(
            monomials = monomials.map { it.copy() }.toMutableList(),
            constant = constant + rhs
        )
    }

    override fun plusAssign(rhs: AbstractVariableItem<*, *>) {
        monomials.add(LinearMonomial(rhs))
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("plusAssignVariables")
    override fun plusAssign(rhs: Iterable<AbstractVariableItem<*, *>>) {
        monomials.addAll(rhs.map { LinearMonomial(it) })
    }

    fun plusAssign(rhs: LinearSymbol) {
        monomials.add(LinearMonomial(rhs))
    }

    @JvmName("plusAssignSymbols")
    fun plusAssign(rhs: Iterable<LinearSymbol>) {
        monomials.addAll(rhs.map { LinearMonomial(it) })
    }

    override fun plusAssign(rhs: LinearMonomial) {
        monomials.add(rhs)
    }

    override fun plusAssign(rhs: Polynomial<*, LinearMonomial, LinearMonomialCell>) {
        monomials.addAll(rhs.monomials.map { it.copy() })
        constant += rhs.constant
    }

    override fun plusAssign(rhs: Flt64) {
        constant += rhs
    }

    override fun minus(rhs: AbstractVariableItem<*, *>): MutableLinearPolynomial {
        val monomials = monomials.map { it.copy() }.toMutableList()
        monomials.add(LinearMonomial(-Flt64.one, rhs))
        return MutableLinearPolynomial(
            monomials = monomials,
            constant = constant
        )
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("minusAssignVariables")
    override fun minus(rhs: Iterable<AbstractVariableItem<*, *>>): MutableLinearPolynomial {
        val monomials = monomials.map { it.copy() }.toMutableList()
        monomials.addAll(rhs.map { LinearMonomial(-Flt64.one, it) })
        return MutableLinearPolynomial(
            monomials = monomials,
            constant = constant
        )
    }

    override fun minus(rhs: LinearSymbol): MutableLinearPolynomial {
        val monomials = monomials.map { it.copy() }.toMutableList()
        monomials.add(LinearMonomial(-Flt64.one, rhs))
        return MutableLinearPolynomial(
            monomials = monomials,
            constant = constant
        )
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("minusAssignSymbols")
    override fun minus(rhs: Iterable<LinearSymbol>): MutableLinearPolynomial {
        val monomials = monomials.map { it.copy() }.toMutableList()
        monomials.addAll(rhs.map { LinearMonomial(-Flt64.one, it) })
        return MutableLinearPolynomial(
            monomials = monomials,
            constant = constant
        )
    }

    override fun minus(rhs: LinearMonomial): MutableLinearPolynomial {
        val monomials = monomials.map { it.copy() }.toMutableList()
        monomials.add(-rhs)
        return MutableLinearPolynomial(
            monomials = monomials,
            constant = constant
        )
    }

    override fun minus(rhs: Polynomial<*, LinearMonomial, LinearMonomialCell>): MutableLinearPolynomial {
        val monomials = monomials.map { it.copy() }.toMutableList()
        monomials.addAll(rhs.monomials.map { -it })
        return MutableLinearPolynomial(
            monomials = monomials,
            constant = constant - rhs.constant
        )
    }

    override fun minus(rhs: Flt64): MutableLinearPolynomial {
        return MutableLinearPolynomial(
            monomials = monomials.map { it.copy() }.toMutableList(),
            constant = constant - rhs
        )
    }

    override fun minusAssign(rhs: AbstractVariableItem<*, *>) {
        monomials.add(LinearMonomial(-Flt64.one, rhs))
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("minusAssignVariables")
    override fun minusAssign(rhs: Iterable<AbstractVariableItem<*, *>>) {
        monomials.addAll(rhs.map { LinearMonomial(-Flt64.one, it) })
    }

    fun minusAssign(rhs: LinearSymbol) {
        monomials.add(LinearMonomial(-Flt64.one, rhs))
    }

    @JvmName("minusAssignSymbols")
    fun minusAssign(rhs: Iterable<LinearSymbol>) {
        monomials.addAll(rhs.map { LinearMonomial(-Flt64.one, it) })
    }

    override fun minusAssign(rhs: LinearMonomial) {
        monomials.add(-rhs)
    }

    override fun minusAssign(rhs: Polynomial<*, LinearMonomial, LinearMonomialCell>) {
        monomials.addAll(rhs.monomials.map { -it })
        constant -= rhs.constant
    }

    override fun minusAssign(rhs: Flt64) {
        constant -= rhs
    }

    override fun times(rhs: Flt64): MutableLinearPolynomial {
        return MutableLinearPolynomial(
            monomials = monomials.map { it * rhs }.toMutableList(),
            constant = constant * rhs
        )
    }

    override fun timesAssign(rhs: Flt64) {
        monomials = monomials.map { it * rhs }.toMutableList()
        constant *= rhs
    }

    override fun div(rhs: Flt64): MutableLinearPolynomial {
        return MutableLinearPolynomial(
            monomials = monomials.map { it / rhs }.toMutableList(),
            constant = constant / rhs
        )
    }

    override fun divAssign(rhs: Flt64) {
        monomials = monomials.map { it / rhs }.toMutableList()
        constant /= rhs
    }
}

// variable and constant

operator fun AbstractVariableItem<*, *>.plus(rhs: Int): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(LinearMonomial(this)),
        constant = Flt64(rhs)
    )
}

operator fun AbstractVariableItem<*, *>.plus(rhs: Double): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(LinearMonomial(this)),
        constant = Flt64(rhs)
    )
}

operator fun <T : RealNumber<T>> AbstractVariableItem<*, *>.plus(rhs: T): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(LinearMonomial(this)),
        constant = rhs.toFlt64()
    )
}

operator fun AbstractVariableItem<*, *>.minus(rhs: Int): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(LinearMonomial(this)),
        constant = -Flt64(rhs)
    )
}

operator fun AbstractVariableItem<*, *>.minus(rhs: Double): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(LinearMonomial(this)),
        constant = -Flt64(rhs)
    )
}

operator fun <T : RealNumber<T>> AbstractVariableItem<*, *>.minus(rhs: T): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(LinearMonomial(this)),
        constant = -rhs.toFlt64()
    )
}

operator fun Int.plus(rhs: AbstractVariableItem<*, *>): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(LinearMonomial(rhs)),
        constant = Flt64(this)
    )
}

operator fun Double.plus(rhs: AbstractVariableItem<*, *>): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(LinearMonomial(rhs)),
        constant = Flt64(this)
    )
}

operator fun <T : RealNumber<T>> T.plus(rhs: AbstractVariableItem<*, *>): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(LinearMonomial(rhs)),
        constant = this.toFlt64()
    )
}

operator fun Int.minus(rhs: AbstractVariableItem<*, *>): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(LinearMonomial(-Flt64.one, rhs)),
        constant = Flt64(this)
    )
}

operator fun Double.minus(rhs: AbstractVariableItem<*, *>): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(LinearMonomial(-Flt64.one, rhs)),
        constant = Flt64(this)
    )
}

operator fun <T : RealNumber<T>> T.minus(rhs: AbstractVariableItem<*, *>): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(LinearMonomial(-Flt64.one, rhs)),
        constant = this.toFlt64()
    )
}

// symbol and constant

operator fun LinearSymbol.plus(rhs: Int): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(LinearMonomial(this)),
        constant = Flt64(rhs)
    )
}

operator fun LinearSymbol.plus(rhs: Double): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(LinearMonomial(this)),
        constant = Flt64(rhs)
    )
}

operator fun <T : RealNumber<T>> LinearSymbol.plus(rhs: T): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(LinearMonomial(this)),
        constant = rhs.toFlt64()
    )
}

operator fun LinearSymbol.minus(rhs: Int): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(LinearMonomial(this)),
        constant = -Flt64(rhs)
    )
}

operator fun LinearSymbol.minus(rhs: Double): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(LinearMonomial(this)),
        constant = -Flt64(rhs)
    )
}

operator fun <T : RealNumber<T>> LinearSymbol.minus(rhs: T): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(LinearMonomial(this)),
        constant = -rhs.toFlt64()
    )
}

operator fun Int.plus(rhs: LinearSymbol): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(LinearMonomial(rhs)),
        constant = Flt64(this)
    )
}

operator fun Double.plus(rhs: LinearSymbol): LinearPolynomial {
    return Flt64(this).plus(rhs)
}

operator fun <T : RealNumber<T>> T.plus(rhs: LinearSymbol): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(LinearMonomial(rhs)),
        constant = this.toFlt64()
    )
}

operator fun Int.minus(rhs: LinearSymbol): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(LinearMonomial(-Flt64.one, rhs)),
        constant = Flt64(this)
    )
}

operator fun Double.minus(rhs: LinearSymbol): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(LinearMonomial(-Flt64.one, rhs)),
        constant = Flt64(this)
    )
}

operator fun <T : RealNumber<T>> T.minus(rhs: LinearSymbol): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(LinearMonomial(-Flt64.one, rhs)),
        constant = this.toFlt64()
    )
}

// monomial and constant

operator fun LinearMonomial.plus(rhs: Int): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(this.copy()),
        constant = Flt64(rhs)
    )
}

operator fun LinearMonomial.plus(rhs: Double): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(this.copy()),
        constant = Flt64(rhs)
    )
}

operator fun <T : RealNumber<T>> LinearMonomial.plus(rhs: T): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(this.copy()),
        constant = rhs.toFlt64()
    )
}

operator fun LinearMonomial.minus(rhs: Int): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(this.copy()),
        constant = -Flt64(rhs)
    )
}

operator fun LinearMonomial.minus(rhs: Double): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(this.copy()),
        constant = -Flt64(rhs)
    )
}

operator fun <T : RealNumber<T>> LinearMonomial.minus(rhs: T): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(this.copy()),
        constant = -rhs.toFlt64()
    )
}

operator fun Int.plus(rhs: LinearMonomial): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(rhs.copy()),
        constant = Flt64(this)
    )
}

operator fun Double.plus(rhs: LinearMonomial): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(rhs.copy()),
        constant = Flt64(this)
    )
}

operator fun <T : RealNumber<T>> T.plus(rhs: LinearMonomial): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(rhs.copy()),
        constant = this.toFlt64()
    )
}

operator fun Int.minus(rhs: LinearMonomial): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(-rhs),
        constant = Flt64(this)
    )
}

operator fun Double.minus(rhs: LinearMonomial): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(-rhs),
        constant = Flt64(this)
    )
}

operator fun <T : RealNumber<T>> T.minus(rhs: LinearMonomial): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(-rhs),
        constant = this.toFlt64()
    )
}

// polynomial and constant

operator fun Int.plus(rhs: AbstractLinearPolynomial<*>): LinearPolynomial {
    return LinearPolynomial(
        monomials = rhs.monomials.map { it.copy() },
        constant = Flt64(this) + rhs.constant
    )
}

operator fun Double.plus(rhs: AbstractLinearPolynomial<*>): LinearPolynomial {
    return LinearPolynomial(
        monomials = rhs.monomials.map { it.copy() },
        constant = Flt64(this) + rhs.constant
    )
}

operator fun <T : RealNumber<T>> T.plus(rhs: AbstractLinearPolynomial<*>): LinearPolynomial {
    return LinearPolynomial(
        monomials = rhs.monomials.map { it.copy() },
        constant = this.toFlt64() + rhs.constant
    )
}

operator fun Int.minus(rhs: AbstractLinearPolynomial<*>): LinearPolynomial {
    return LinearPolynomial(
        monomials = rhs.monomials.map { -it },
        constant = Flt64(this) - rhs.constant
    )
}

operator fun Double.minus(rhs: AbstractLinearPolynomial<*>): LinearPolynomial {
    return LinearPolynomial(
        monomials = rhs.monomials.map { -it },
        constant = Flt64(this) - rhs.constant
    )
}

operator fun <T : RealNumber<T>> T.minus(rhs: AbstractLinearPolynomial<*>): LinearPolynomial {
    return LinearPolynomial(
        monomials = rhs.monomials.map { -it },
        constant = this.toFlt64() - rhs.constant
    )
}

operator fun Int.times(rhs: AbstractLinearPolynomial<*>): LinearPolynomial {
    return LinearPolynomial(
        monomials = rhs.monomials.map { this * it },
        constant = Flt64(this) * rhs.constant
    )
}

operator fun Double.times(rhs: AbstractLinearPolynomial<*>): LinearPolynomial {
    return LinearPolynomial(
        monomials = rhs.monomials.map { this * it },
        constant = Flt64(this) * rhs.constant
    )
}

operator fun <T : RealNumber<T>> T.times(rhs: AbstractLinearPolynomial<*>): LinearPolynomial {
    return LinearPolynomial(
        monomials = rhs.monomials.map { this * it },
        constant = this.toFlt64() * rhs.constant
    )
}

// variable and variable

operator fun AbstractVariableItem<*, *>.plus(rhs: AbstractVariableItem<*, *>): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(LinearMonomial(this), LinearMonomial(rhs))
    )
}

operator fun AbstractVariableItem<*, *>.minus(rhs: AbstractVariableItem<*, *>): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(LinearMonomial(this), LinearMonomial(-Flt64.one, rhs))
    )
}

// symbol and variable

operator fun LinearSymbol.plus(rhs: AbstractVariableItem<*, *>): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(LinearMonomial(this), LinearMonomial(rhs))
    )
}

operator fun LinearSymbol.minus(rhs: AbstractVariableItem<*, *>): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(LinearMonomial(this), LinearMonomial(-Flt64.one, rhs))
    )
}

operator fun AbstractVariableItem<*, *>.plus(rhs: LinearSymbol): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(LinearMonomial(this), LinearMonomial(rhs))
    )
}

operator fun AbstractVariableItem<*, *>.minus(rhs: LinearSymbol): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(LinearMonomial(this), LinearMonomial(-Flt64.one, rhs))
    )
}

// monomial and variable

operator fun LinearMonomial.plus(rhs: AbstractVariableItem<*, *>): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(this.copy(), LinearMonomial(rhs))
    )
}

operator fun LinearMonomial.minus(rhs: AbstractVariableItem<*, *>): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(this.copy(), LinearMonomial(-Flt64.one, rhs))
    )
}

operator fun AbstractVariableItem<*, *>.plus(rhs: LinearMonomial): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(LinearMonomial(this), rhs.copy())
    )
}

operator fun AbstractVariableItem<*, *>.minus(rhs: LinearMonomial): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(LinearMonomial(this), -rhs)
    )
}

// polynomial and variable

operator fun AbstractVariableItem<*, *>.plus(rhs: LinearPolynomial): LinearPolynomial {
    val monomials = arrayListOf(LinearMonomial(this))
    monomials.addAll(rhs.monomials.map { it.copy() })
    return LinearPolynomial(
        monomials = monomials,
        constant = rhs.constant
    )
}

operator fun AbstractVariableItem<*, *>.minus(rhs: LinearPolynomial): LinearPolynomial {
    val monomials = arrayListOf(LinearMonomial(this))
    monomials.addAll(rhs.monomials.map { -it })
    return LinearPolynomial(
        monomials = monomials,
        constant = -rhs.constant
    )
}

// symbol and symbol

operator fun LinearSymbol.plus(rhs: LinearSymbol): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(LinearMonomial(this), LinearMonomial(rhs))
    )
}

operator fun LinearSymbol.minus(rhs: LinearSymbol): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(LinearMonomial(this), LinearMonomial(-Flt64.one, rhs))
    )
}

// monomial and symbol

operator fun LinearMonomial.plus(rhs: LinearSymbol): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(this.copy(), LinearMonomial(rhs))
    )
}

operator fun LinearMonomial.minus(rhs: LinearSymbol): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(this.copy(), LinearMonomial(-Flt64.one, rhs))
    )
}

operator fun LinearSymbol.plus(rhs: LinearMonomial): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(LinearMonomial(this), rhs.copy())
    )
}

operator fun LinearSymbol.minus(rhs: LinearMonomial): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(LinearMonomial(this), -rhs)
    )
}

// polynomial and symbol

operator fun LinearSymbol.plus(rhs: LinearPolynomial): LinearPolynomial {
    val newMonomials = arrayListOf(LinearMonomial(this))
    newMonomials.addAll(rhs.monomials.map { it.copy() })
    return LinearPolynomial(monomials = newMonomials)
}

operator fun LinearSymbol.minus(rhs: LinearPolynomial): LinearPolynomial {
    val newMonomials = arrayListOf(LinearMonomial(this))
    newMonomials.addAll(rhs.monomials.map { -it })
    return LinearPolynomial(monomials = newMonomials)
}

// monomial and monomial

operator fun LinearMonomial.plus(rhs: LinearMonomial): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(this.copy(), rhs.copy())
    )
}

operator fun LinearMonomial.minus(rhs: LinearMonomial): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(this.copy(), -rhs)
    )
}

// polynomial and monomial

operator fun LinearMonomial.plus(rhs: LinearPolynomial): LinearPolynomial {
    val newMonomials = arrayListOf(this.copy())
    newMonomials.addAll(rhs.monomials.map { it.copy() })
    return LinearPolynomial(monomials = newMonomials)
}

operator fun LinearMonomial.minus(rhs: LinearPolynomial): LinearPolynomial {
    val newMonomials = arrayListOf(this.copy())
    newMonomials.addAll(rhs.monomials.map { -it })
    return LinearPolynomial(monomials = newMonomials)
}

// sigma

@JvmName("sumVariables")
fun sum(
    items: Iterable<AbstractVariableItem<*, *>>,
    ctor: (AbstractVariableItem<*, *>) -> LinearMonomial = { LinearMonomial(it) }
): LinearPolynomial {
    val monomials = ArrayList<LinearMonomial>()
    for (item in items) {
        monomials.add(ctor(item))
    }
    return LinearPolynomial(monomials = monomials)
}

@JvmName("sumLinearSymbols")
fun sum(
    symbols: Iterable<LinearSymbol>,
    ctor: (LinearSymbol) -> LinearMonomial = { LinearMonomial(it) }
): LinearPolynomial {
    val monomials = ArrayList<LinearMonomial>()
    for (symbol in symbols) {
        monomials.add(ctor(symbol))
    }
    return LinearPolynomial(monomials = monomials)
}

@JvmName("sumLinearMonomials")
fun sum(monomials: Iterable<LinearMonomial>): LinearPolynomial {
    return LinearPolynomial(monomials = monomials.toList())
}

@JvmName("sumLinearPolynomials")
fun sum(polynomials: Iterable<AbstractLinearPolynomial<*>>): LinearPolynomial {
    val monomials = ArrayList<LinearMonomial>()
    var constant = Flt64.zero
    for (polynomial in polynomials) {
        monomials.addAll(polynomial.monomials)
        constant += polynomial.constant
    }
    return LinearPolynomial(monomials = monomials, constant = constant)
}

@JvmName("sumMapVariables")
fun <T> sumVars(
    objs: Iterable<T>,
    ctor: (T) -> AbstractVariableItem<*, *>?
): LinearPolynomial {
    return sum(objs.mapNotNull(ctor))
}

@JvmName("sumMapLinearSymbols")
fun <T> sumSymbols(
    objs: Iterable<T>,
    ctor: (T) -> LinearSymbol?
): LinearPolynomial {
    return sum(objs.mapNotNull(ctor))
}

fun <T> sum(
    objs: Iterable<T>,
    ctor: (T) -> LinearMonomial?
): LinearPolynomial {
    return sum(objs.mapNotNull(ctor))
}

@JvmName("sumMapLinearMonomials")
fun <T> flatSum(
    objs: Iterable<T>,
    ctor: (T) -> Iterable<LinearMonomial?>
): LinearPolynomial {
    return sum(objs.flatMap(ctor).filterNotNull())
}
