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

    override val dependencies: Set<IntermediateSymbol>
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

    abstract operator fun plus(rhs: LinearIntermediateSymbol): Self
    abstract operator fun plus(rhs: Iterable<LinearIntermediateSymbol>): Self

    abstract operator fun minus(rhs: LinearIntermediateSymbol): Self
    abstract operator fun minus(rhs: Iterable<LinearIntermediateSymbol>): Self

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
            symbol: LinearIntermediateSymbol,
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

        operator fun invoke(
            constant: Boolean,
            name: String = "",
            displayName: String? = null
        ): LinearPolynomial {
            return LinearPolynomial(
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
        ): LinearPolynomial {
            return LinearPolynomial(
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
        ): LinearPolynomial {
            return LinearPolynomial(
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

    override fun plus(rhs: LinearIntermediateSymbol): LinearPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(LinearMonomial(rhs))
        return LinearPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("plusSymbols")
    override fun plus(rhs: Iterable<LinearIntermediateSymbol>): LinearPolynomial {
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

    override fun minus(rhs: LinearIntermediateSymbol): LinearPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(LinearMonomial(-Flt64.one, rhs))
        return LinearPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("minusSymbols")
    override fun minus(rhs: Iterable<LinearIntermediateSymbol>): LinearPolynomial {
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
            symbol: LinearIntermediateSymbol,
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

        operator fun invoke(
            constant: Int,
            name: String = "",
            displayName: String? = null
        ): MutableLinearPolynomial {
            return MutableLinearPolynomial(
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
        ): MutableLinearPolynomial {
            return MutableLinearPolynomial(
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
        ): MutableLinearPolynomial {
            return MutableLinearPolynomial(
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
        ): MutableLinearPolynomial {
            return MutableLinearPolynomial(
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
        ): MutableLinearPolynomial {
            return MutableLinearPolynomial(
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

    override fun plus(rhs: LinearIntermediateSymbol): MutableLinearPolynomial {
        val newMonomials = monomials.map { it.copy() }.toMutableList()
        newMonomials.add(LinearMonomial(rhs))
        return MutableLinearPolynomial(
            monomials = newMonomials,
            constant = constant
        )
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("plusSymbols")
    override fun plus(rhs: Iterable<LinearIntermediateSymbol>): MutableLinearPolynomial {
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

    operator fun plusAssign(rhs: LinearIntermediateSymbol) {
        monomials.add(LinearMonomial(rhs))
    }

    @JvmName("plusAssignSymbols")
    operator fun plusAssign(rhs: Iterable<LinearIntermediateSymbol>) {
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

    override fun minus(rhs: LinearIntermediateSymbol): MutableLinearPolynomial {
        val monomials = monomials.map { it.copy() }.toMutableList()
        monomials.add(LinearMonomial(-Flt64.one, rhs))
        return MutableLinearPolynomial(
            monomials = monomials,
            constant = constant
        )
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("minusAssignSymbols")
    override fun minus(rhs: Iterable<LinearIntermediateSymbol>): MutableLinearPolynomial {
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

    operator fun minusAssign(rhs: LinearIntermediateSymbol) {
        monomials.add(LinearMonomial(-Flt64.one, rhs))
    }

    @JvmName("minusAssignSymbols")
    operator fun minusAssign(rhs: Iterable<LinearIntermediateSymbol>) {
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

// quantity polynomial conversion

fun Quantity<AbstractLinearPolynomial<*>>.to(targetUnit: PhysicalUnit): Quantity<LinearPolynomial>? {
    return this.unit.to(targetUnit)?.let {
        Quantity(it.value * this.value, targetUnit)
    }
}

// unary minus quantity polynomial

operator fun Quantity<AbstractLinearPolynomial<*>>.unaryMinus(): Quantity<LinearPolynomial> {
    return Quantity(LinearPolynomial(-this.value), this.unit)
}

// quantity polynomial plus/minus/times/div assign

@JvmName("quantityPolynomialPlusAssignQuantityVariable")
fun Quantity<MutableLinearPolynomial>.plusAssign(rhs: Quantity<AbstractVariableItem<*, *>>) {
    value.plusAssign(rhs.to(this.unit)!!.value)
}

@JvmName("quantityPolynomialPlusAssignQuantityVariables")
fun Quantity<MutableLinearPolynomial>.plusAssign(rhs: Iterable<Quantity<AbstractVariableItem<*, *>>>) {
    for (item in rhs) {
        value.plusAssign(item.to(this.unit)!!.value)
    }
}

@JvmName("quantityPolynomialPlusAssignQuantitySymbol")
fun Quantity<MutableLinearPolynomial>.plusAssign(rhs: Quantity<LinearIntermediateSymbol>) {
    value.plusAssign(rhs.to(this.unit)!!.value)
}

@JvmName("quantityPolynomialPlusAssignQuantitySymbols")
fun Quantity<MutableLinearPolynomial>.plusAssign(rhs: Iterable<Quantity<LinearIntermediateSymbol>>) {
    for (item in rhs) {
        value.plusAssign(item.to(this.unit)!!.value)
    }
}

@JvmName("quantityPolynomialPlusAssignQuantityMonomial")
fun Quantity<MutableLinearPolynomial>.plusAssign(rhs: Quantity<LinearMonomial>) {
    value.plusAssign(rhs.to(this.unit)!!.value)
}

@JvmName("quantityPolynomialPlusAssignQuantityPolynomial")
fun Quantity<MutableLinearPolynomial>.plusAssign(rhs: Quantity<AbstractLinearPolynomial<*>>) {
    value.plusAssign(rhs.to(this.unit)!!.value)
}

@JvmName("quantityPolynomialPlusAssignQuantity")
fun<V : RealNumber<V>> Quantity<MutableLinearPolynomial>.plusAssign(rhs: Quantity<V>) {
    value.plusAssign(rhs.toFlt64().to(this.unit)!!.value)
}

@JvmName("quantityPolynomialMinusAssignQuantityVariable")
fun Quantity<MutableLinearPolynomial>.minusAssign(rhs: Quantity<AbstractVariableItem<*, *>>) {
    value.minusAssign(rhs.to(this.unit)!!.value)
}

@JvmName("quantityPolynomialMinusAssignQuantityVariables")
fun Quantity<MutableLinearPolynomial>.minusAssign(rhs: Iterable<Quantity<AbstractVariableItem<*, *>>>) {
    for (item in rhs) {
        value.minusAssign(item.to(this.unit)!!.value)
    }
}

@JvmName("quantityPolynomialMinusAssignQuantitySymbol")
fun Quantity<MutableLinearPolynomial>.minusAssign(rhs: Quantity<LinearIntermediateSymbol>) {
    value.minusAssign(rhs.to(this.unit)!!.value)
}

@JvmName("quantityPolynomialMinusAssignQuantitySymbols")
fun Quantity<MutableLinearPolynomial>.minusAssign(rhs: Iterable<Quantity<LinearIntermediateSymbol>>) {
    for (item in rhs) {
        value.minusAssign(item.to(this.unit)!!.value)
    }
}

@JvmName("quantityPolynomialMinusAssignQuantityMonomial")
fun Quantity<MutableLinearPolynomial>.minusAssign(rhs: Quantity<LinearMonomial>) {
    value.minusAssign(rhs.to(this.unit)!!.value)
}

@JvmName("quantityPolynomialMinusAssignQuantityPolynomial")
fun Quantity<MutableLinearPolynomial>.minusAssign(rhs: Quantity<AbstractLinearPolynomial<*>>) {
    value.minusAssign(rhs.to(this.unit)!!.value)
}

@JvmName("quantityPolynomialMinusAssignQuantity")
fun<V : RealNumber<V>> Quantity<MutableLinearPolynomial>.minusAssign(rhs: Quantity<V>) {
    value.minusAssign(rhs.toFlt64().to(this.unit)!!.value)
}

@JvmName("quantityPolynomialTimesAssign")
fun<V: RealNumber<V>> Quantity<MutableLinearPolynomial>.timesAssign(rhs: V) {
    value.timesAssign(rhs.toFlt64())
}

@JvmName("quantityPolynomialDivAssign")
fun<V: RealNumber<V>> Quantity<MutableLinearPolynomial>.divAssign(rhs: V) {
    value.divAssign(rhs.toFlt64())
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

// quantity variable and quantity

@JvmName("quantityVariablePlusQuantity")
operator fun <T : RealNumber<T>> Quantity<AbstractVariableItem<*, *>>.plus(rhs: Quantity<T>): Quantity<LinearPolynomial> {
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

@JvmName("quantityPlusQuantityVariable")
operator fun <T : RealNumber<T>> Quantity<T>.plus(rhs: Quantity<AbstractVariableItem<*, *>>): Quantity<LinearPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value + rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.value + rhs.to(this.unit)!!.value, this.unit)
        } else {
            Quantity(this.toFlt64().unit.to(rhs.unit)!!.value + rhs.value, rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityVariableMinusQuantity")
operator fun <T : RealNumber<T>> Quantity<AbstractVariableItem<*, *>>.minus(rhs: Quantity<T>): Quantity<LinearPolynomial> {
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

@JvmName("quantityMinusQuantityVariable")
operator fun <T : RealNumber<T>> Quantity<T>.minus(rhs: Quantity<AbstractVariableItem<*, *>>): Quantity<LinearPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value - rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.value - rhs.to(this.unit)!!.value, this.unit)
        } else {
            Quantity(this.toFlt64().unit.to(rhs.unit)!!.value - rhs.value, rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

// symbol and constant

operator fun LinearIntermediateSymbol.plus(rhs: Int): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(LinearMonomial(this)),
        constant = Flt64(rhs)
    )
}

operator fun LinearIntermediateSymbol.plus(rhs: Double): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(LinearMonomial(this)),
        constant = Flt64(rhs)
    )
}

operator fun <T : RealNumber<T>> LinearIntermediateSymbol.plus(rhs: T): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(LinearMonomial(this)),
        constant = rhs.toFlt64()
    )
}

operator fun LinearIntermediateSymbol.minus(rhs: Int): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(LinearMonomial(this)),
        constant = -Flt64(rhs)
    )
}

operator fun LinearIntermediateSymbol.minus(rhs: Double): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(LinearMonomial(this)),
        constant = -Flt64(rhs)
    )
}

operator fun <T : RealNumber<T>> LinearIntermediateSymbol.minus(rhs: T): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(LinearMonomial(this)),
        constant = -rhs.toFlt64()
    )
}

operator fun Int.plus(rhs: LinearIntermediateSymbol): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(LinearMonomial(rhs)),
        constant = Flt64(this)
    )
}

operator fun Double.plus(rhs: LinearIntermediateSymbol): LinearPolynomial {
    return Flt64(this).plus(rhs)
}

operator fun <T : RealNumber<T>> T.plus(rhs: LinearIntermediateSymbol): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(LinearMonomial(rhs)),
        constant = this.toFlt64()
    )
}

operator fun Int.minus(rhs: LinearIntermediateSymbol): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(LinearMonomial(-Flt64.one, rhs)),
        constant = Flt64(this)
    )
}

operator fun Double.minus(rhs: LinearIntermediateSymbol): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(LinearMonomial(-Flt64.one, rhs)),
        constant = Flt64(this)
    )
}

operator fun <T : RealNumber<T>> T.minus(rhs: LinearIntermediateSymbol): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(LinearMonomial(-Flt64.one, rhs)),
        constant = this.toFlt64()
    )
}

// quantity symbol and quantity

@JvmName("quantitySymbolPlusQuantity")
operator fun <T : RealNumber<T>> Quantity<LinearIntermediateSymbol>.plus(rhs: Quantity<T>): Quantity<LinearPolynomial> {
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

@JvmName("quantityPlusQuantitySymbol")
operator fun <T : RealNumber<T>> Quantity<T>.plus(rhs: Quantity<LinearIntermediateSymbol>): Quantity<LinearPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value + rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.value + rhs.to(this.unit)!!.value, this.unit)
        } else {
            Quantity(this.toFlt64().unit.to(rhs.unit)!!.value + rhs.value, rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantitySymbolMinusQuantity")
operator fun <T : RealNumber<T>> Quantity<LinearIntermediateSymbol>.minus(rhs: Quantity<T>): Quantity<LinearPolynomial> {
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

@JvmName("quantityMinusQuantitySymbol")
operator fun <T : RealNumber<T>> Quantity<T>.minus(rhs: Quantity<LinearIntermediateSymbol>): Quantity<LinearPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value - rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.value - rhs.to(this.unit)!!.value, this.unit)
        } else {
            Quantity(this.toFlt64().unit.to(rhs.unit)!!.value - rhs.value, rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
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

// quantity monomial and quantity

@JvmName("quantityMonomialPlusQuantity")
operator fun <T : RealNumber<T>> Quantity<LinearMonomial>.plus(rhs: Quantity<T>): Quantity<LinearPolynomial> {
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

@JvmName("quantityPlusQuantityMonomial")
operator fun <T : RealNumber<T>> Quantity<T>.plus(rhs: Quantity<LinearMonomial>): Quantity<LinearPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value + rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.value + rhs.to(this.unit)!!.value, this.unit)
        } else {
            Quantity(this.toFlt64().unit.to(rhs.unit)!!.value + rhs.value, rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityMonomialMinusQuantity")
operator fun <T : RealNumber<T>> Quantity<LinearMonomial>.minus(rhs: Quantity<T>): Quantity<LinearPolynomial> {
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

@JvmName("quantityMinusQuantityMonomial")
operator fun <T : RealNumber<T>> Quantity<T>.minus(rhs: Quantity<LinearMonomial>): Quantity<LinearPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value - rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.value - rhs.to(this.unit)!!.value, this.unit)
        } else {
            Quantity(this.toFlt64().unit.to(rhs.unit)!!.value - rhs.value, rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

// polynomial and unit

operator fun AbstractLinearPolynomial<*>.times(rhs: PhysicalUnit): Quantity<LinearPolynomial> {
    return Quantity(LinearPolynomial(this), rhs)
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

// polynomial and quantity

@JvmName("polynomialTimesQuantity")
operator fun <T : RealNumber<T>> AbstractLinearPolynomial<*>.times(rhs: Quantity<T>): Quantity<LinearPolynomial> {
    return Quantity(LinearPolynomial(this * rhs.value), rhs.unit)
}

@JvmName("quantityTimesPolynomial")
operator fun <T : RealNumber<T>> Quantity<T>.times(rhs: AbstractLinearPolynomial<*>): Quantity<LinearPolynomial> {
    return Quantity(this.value * rhs, this.unit)
}

@JvmName("polynomialDivQuantity")
operator fun <T : RealNumber<T>> AbstractLinearPolynomial<*>.div(rhs: Quantity<T>): Quantity<LinearPolynomial> {
    return Quantity(LinearPolynomial(this / rhs.value), rhs.unit.reciprocal())
}

// quantity polynomial and constant

@JvmName("quantityPolynomialTimesInt")
operator fun Quantity<AbstractLinearPolynomial<*>>.times(rhs: Int): Quantity<LinearPolynomial> {
    return Quantity(LinearPolynomial(this.value * rhs), this.unit)
}

@JvmName("quantityPolynomialTimesDouble")
operator fun Quantity<AbstractLinearPolynomial<*>>.times(rhs: Double): Quantity<LinearPolynomial> {
    return Quantity(LinearPolynomial(this.value * rhs), this.unit)
}

@JvmName("quantityPolynomialTimesRealNumber")
operator fun <T : RealNumber<T>> Quantity<AbstractLinearPolynomial<*>>.times(rhs: T): Quantity<LinearPolynomial> {
    return Quantity(LinearPolynomial(this.value * rhs.toFlt64()), this.unit)
}

@JvmName("intTimesQuantityPolynomial")
operator fun Int.times(rhs: Quantity<AbstractLinearPolynomial<*>>): Quantity<LinearPolynomial> {
    return Quantity(this * rhs.value, rhs.unit)
}

@JvmName("doubleTimesQuantityPolynomial")
operator fun Double.times(rhs: Quantity<AbstractLinearPolynomial<*>>): Quantity<LinearPolynomial> {
    return Quantity(this * rhs.value, rhs.unit)
}

@JvmName("realNumberTimesQuantityPolynomial")
operator fun <T : RealNumber<T>> T.times(rhs: Quantity<AbstractLinearPolynomial<*>>): Quantity<LinearPolynomial> {
    return Quantity(this.toFlt64() * rhs.value, rhs.unit)
}

@JvmName("quantityPolynomialDivInt")
operator fun Quantity<AbstractLinearPolynomial<*>>.div(rhs: Int): Quantity<LinearPolynomial> {
    return Quantity(LinearPolynomial(this.value / rhs), this.unit)
}

@JvmName("quantityPolynomialDivDouble")
operator fun Quantity<AbstractLinearPolynomial<*>>.div(rhs: Double): Quantity<LinearPolynomial> {
    return Quantity(LinearPolynomial(this.value / rhs), this.unit)
}

@JvmName("quantityPolynomialDivRealNumber")
operator fun <T : RealNumber<T>> Quantity<AbstractLinearPolynomial<*>>.div(rhs: T): Quantity<LinearPolynomial> {
    return Quantity(LinearPolynomial(this.value / rhs.toFlt64()), this.unit)
}

// quantity polynomial and quantity

@JvmName("quantityPolynomialPlusQuantity")
operator fun <T : RealNumber<T>> Quantity<AbstractLinearPolynomial<*>>.plus(rhs: Quantity<T>): Quantity<LinearPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(LinearPolynomial(this.value + rhs.value), this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(LinearPolynomial(this.value + rhs.toFlt64().to(this.unit)!!.value), this.unit)
        } else {
            Quantity(LinearPolynomial(this.to(rhs.unit)!!.value + rhs.value), rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityPlusQuantityPolynomial")
operator fun <T : RealNumber<T>> Quantity<T>.plus(rhs: Quantity<AbstractLinearPolynomial<*>>): Quantity<LinearPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value + rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.value + rhs.to(this.unit)!!.value, this.unit)
        } else {
            Quantity(this.toFlt64().unit.to(rhs.unit)!!.value + rhs.value, rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityPolynomialMinusQuantity")
operator fun <T : RealNumber<T>> Quantity<AbstractLinearPolynomial<*>>.minus(rhs: Quantity<T>): Quantity<LinearPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(LinearPolynomial(this.value - rhs.value), this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(LinearPolynomial(this.value - rhs.toFlt64().to(this.unit)!!.value), this.unit)
        } else {
            Quantity(LinearPolynomial(this.to(rhs.unit)!!.value - rhs.value), rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityMinusQuantityPolynomial")
operator fun <T : RealNumber<T>> Quantity<T>.minus(rhs: Quantity<AbstractLinearPolynomial<*>>): Quantity<LinearPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(this.value - rhs.value, this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(this.value - rhs.to(this.unit)!!.value, this.unit)
        } else {
            Quantity(this.toFlt64().unit.to(rhs.unit)!!.value - rhs.value, rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityPolynomialTimesQuantity")
operator fun <T : RealNumber<T>> Quantity<AbstractLinearPolynomial<*>>.times(rhs: Quantity<T>): Quantity<LinearPolynomial> {
    return Quantity(LinearPolynomial(this.value * rhs.value), this.unit * rhs.unit)
}

@JvmName("quantityTimesQuantityPolynomial")
operator fun <T : RealNumber<T>> Quantity<T>.times(rhs: Quantity<AbstractLinearPolynomial<*>>): Quantity<LinearPolynomial> {
    return Quantity(this.value * rhs.value, this.unit * rhs.unit)
}

@JvmName("quantityPolynomialDivQuantity")
operator fun <T : RealNumber<T>> Quantity<AbstractLinearPolynomial<*>>.div(rhs: Quantity<T>): Quantity<LinearPolynomial> {
    return Quantity(LinearPolynomial(this.value / rhs.value), this.unit / rhs.unit)
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

// quantity variable and quantity variable

@JvmName("quantityVariablePlusQuantityVariable")
operator fun Quantity<AbstractVariableItem<*, *>>.plus(rhs: Quantity<AbstractVariableItem<*, *>>): Quantity<LinearPolynomial> {
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

@JvmName("quantityVariableMinusQuantityVariable")
operator fun Quantity<AbstractVariableItem<*, *>>.minus(rhs: Quantity<AbstractVariableItem<*, *>>): Quantity<LinearPolynomial> {
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

// symbol and variable

operator fun LinearIntermediateSymbol.plus(rhs: AbstractVariableItem<*, *>): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(LinearMonomial(this), LinearMonomial(rhs))
    )
}

operator fun LinearIntermediateSymbol.minus(rhs: AbstractVariableItem<*, *>): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(LinearMonomial(this), LinearMonomial(-Flt64.one, rhs))
    )
}

operator fun AbstractVariableItem<*, *>.plus(rhs: LinearIntermediateSymbol): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(LinearMonomial(this), LinearMonomial(rhs))
    )
}

operator fun AbstractVariableItem<*, *>.minus(rhs: LinearIntermediateSymbol): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(LinearMonomial(this), LinearMonomial(-Flt64.one, rhs))
    )
}

// quantity symbol and quantity variable

@JvmName("quantitySymbolPlusQuantityVariable")
operator fun Quantity<LinearIntermediateSymbol>.plus(rhs: Quantity<AbstractVariableItem<*, *>>): Quantity<LinearPolynomial> {
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

@JvmName("quantityVariablePlusQuantitySymbol")
operator fun Quantity<AbstractVariableItem<*, *>>.plus(rhs: Quantity<LinearIntermediateSymbol>): Quantity<LinearPolynomial> {
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
operator fun Quantity<LinearIntermediateSymbol>.minus(rhs: Quantity<AbstractVariableItem<*, *>>): Quantity<LinearPolynomial> {
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

@JvmName("quantityVariableMinusQuantitySymbol")
operator fun Quantity<AbstractVariableItem<*, *>>.minus(rhs: Quantity<LinearIntermediateSymbol>): Quantity<LinearPolynomial> {
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

// quantity monomial and quantity variable

@JvmName("quantityMonomialPlusQuantityVariable")
operator fun Quantity<LinearMonomial>.plus(rhs: Quantity<AbstractVariableItem<*, *>>): Quantity<LinearPolynomial> {
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

@JvmName("quantityVariablePlusQuantityMonomial")
operator fun Quantity<AbstractVariableItem<*, *>>.plus(rhs: Quantity<LinearMonomial>): Quantity<LinearPolynomial> {
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
operator fun Quantity<LinearMonomial>.minus(rhs: Quantity<AbstractVariableItem<*, *>>): Quantity<LinearPolynomial> {
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

@JvmName("quantityVariableMinusQuantityMonomial")
operator fun Quantity<AbstractVariableItem<*, *>>.minus(rhs: Quantity<LinearMonomial>): Quantity<LinearPolynomial> {
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

// polynomial and variable

operator fun AbstractVariableItem<*, *>.plus(rhs: AbstractLinearPolynomial<*>): LinearPolynomial {
    val monomials = arrayListOf(LinearMonomial(this))
    monomials.addAll(rhs.monomials.map { it.copy() })
    return LinearPolynomial(
        monomials = monomials,
        constant = rhs.constant
    )
}

operator fun AbstractVariableItem<*, *>.minus(rhs: AbstractLinearPolynomial<*>): LinearPolynomial {
    val monomials = arrayListOf(LinearMonomial(this))
    monomials.addAll(rhs.monomials.map { -it })
    return LinearPolynomial(
        monomials = monomials,
        constant = -rhs.constant
    )
}

// quantity polynomial and quantity variable

@JvmName("quantityPolynomialPlusQuantityVariable")
operator fun Quantity<LinearPolynomial>.plus(rhs: Quantity<AbstractVariableItem<*, *>>): Quantity<LinearPolynomial> {
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

@JvmName("quantityVariablePlusQuantityPolynomial")
operator fun Quantity<AbstractVariableItem<*, *>>.plus(rhs: Quantity<LinearPolynomial>): Quantity<LinearPolynomial> {
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

@JvmName("quantityPolynomialMinusQuantityVariable")
operator fun Quantity<LinearPolynomial>.minus(rhs: Quantity<AbstractVariableItem<*, *>>): Quantity<LinearPolynomial> {
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

@JvmName("quantityVariableMinusQuantityPolynomial")
operator fun Quantity<AbstractVariableItem<*, *>>.minus(rhs: Quantity<LinearPolynomial>): Quantity<LinearPolynomial> {
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

// symbol and symbol

operator fun LinearIntermediateSymbol.plus(rhs: LinearIntermediateSymbol): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(LinearMonomial(this), LinearMonomial(rhs))
    )
}

operator fun LinearIntermediateSymbol.minus(rhs: LinearIntermediateSymbol): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(LinearMonomial(this), LinearMonomial(-Flt64.one, rhs))
    )
}

// quantity symbol and quantity symbol

@JvmName("quantitySymbolPlusQuantitySymbol")
operator fun Quantity<LinearIntermediateSymbol>.plus(rhs: Quantity<LinearIntermediateSymbol>): Quantity<LinearPolynomial> {
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

@JvmName("quantitySymbolMinusQuantitySymbol")
operator fun Quantity<LinearIntermediateSymbol>.minus(rhs: Quantity<LinearIntermediateSymbol>): Quantity<LinearPolynomial> {
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

operator fun LinearMonomial.plus(rhs: LinearIntermediateSymbol): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(this.copy(), LinearMonomial(rhs))
    )
}

operator fun LinearMonomial.minus(rhs: LinearIntermediateSymbol): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(this.copy(), LinearMonomial(-Flt64.one, rhs))
    )
}

operator fun LinearIntermediateSymbol.plus(rhs: LinearMonomial): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(LinearMonomial(this), rhs.copy())
    )
}

operator fun LinearIntermediateSymbol.minus(rhs: LinearMonomial): LinearPolynomial {
    return LinearPolynomial(
        monomials = listOf(LinearMonomial(this), -rhs)
    )
}

// quantity monomial and quantity symbol

@JvmName("quantityMonomialPlusQuantitySymbol")
operator fun Quantity<LinearMonomial>.plus(rhs: Quantity<LinearIntermediateSymbol>): Quantity<LinearPolynomial> {
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

@JvmName("quantitySymbolPlusQuantityMnomial")
operator fun Quantity<LinearIntermediateSymbol>.plus(rhs: Quantity<LinearMonomial>): Quantity<LinearPolynomial> {
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

@JvmName("quantityMonomialMinusQuantitySymbol")
operator fun Quantity<LinearMonomial>.minus(rhs: Quantity<LinearIntermediateSymbol>): Quantity<LinearPolynomial> {
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

@JvmName("quantitySymbolMinusQuantityMonomial")
operator fun Quantity<LinearIntermediateSymbol>.minus(rhs: Quantity<LinearMonomial>): Quantity<LinearPolynomial> {
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

// polynomial and symbol

operator fun LinearIntermediateSymbol.plus(rhs: AbstractLinearPolynomial<*>): LinearPolynomial {
    val newMonomials = arrayListOf(LinearMonomial(this))
    newMonomials.addAll(rhs.monomials.map { it.copy() })
    return LinearPolynomial(monomials = newMonomials)
}

operator fun LinearIntermediateSymbol.minus(rhs: AbstractLinearPolynomial<*>): LinearPolynomial {
    val newMonomials = arrayListOf(LinearMonomial(this))
    newMonomials.addAll(rhs.monomials.map { -it })
    return LinearPolynomial(monomials = newMonomials)
}

// quantity polynomial and quantity symbol

@JvmName("quantityPolynomialPlusQuantitySymbol")
operator fun Quantity<AbstractLinearPolynomial<*>>.plus(rhs: Quantity<LinearIntermediateSymbol>): Quantity<LinearPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(LinearPolynomial(this.value + rhs.value), this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(LinearPolynomial(this.value + rhs.to(this.unit)!!.value), this.unit)
        } else {
            Quantity(this.to(rhs.unit)!!.value + rhs.value, rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantitySymbolPlusQuantityPolynomial")
operator fun Quantity<LinearIntermediateSymbol>.plus(rhs: Quantity<AbstractLinearPolynomial<*>>): Quantity<LinearPolynomial> {
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

@JvmName("quantityPolynomialMinusQuantitySymbol")
operator fun Quantity<AbstractLinearPolynomial<*>>.minus(rhs: Quantity<LinearIntermediateSymbol>): Quantity<LinearPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(LinearPolynomial(this.value - rhs.value), this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(LinearPolynomial(this.value - rhs.to(this.unit)!!.value), this.unit)
        } else {
            Quantity(this.to(rhs.unit)!!.value - rhs.value, rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantitySymbolMinusQuantityPolynomial")
operator fun Quantity<LinearIntermediateSymbol>.minus(rhs: Quantity<AbstractLinearPolynomial<*>>): Quantity<LinearPolynomial> {
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

// quantity monomial and quantity monomial

@JvmName("quantityMonomialPlusQuantityMonomial")
operator fun Quantity<LinearMonomial>.plus(rhs: Quantity<LinearMonomial>): Quantity<LinearPolynomial> {
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

@JvmName("quantityMonomialMinusQuantityMonomial")
operator fun Quantity<LinearMonomial>.minus(rhs: Quantity<LinearMonomial>): Quantity<LinearPolynomial> {
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

// polynomial and monomial

operator fun LinearMonomial.plus(rhs: AbstractLinearPolynomial<*>): LinearPolynomial {
    val newMonomials = arrayListOf(this.copy())
    newMonomials.addAll(rhs.monomials.map { it.copy() })
    return LinearPolynomial(monomials = newMonomials)
}

operator fun LinearMonomial.minus(rhs: AbstractLinearPolynomial<*>): LinearPolynomial {
    val newMonomials = arrayListOf(this.copy())
    newMonomials.addAll(rhs.monomials.map { -it })
    return LinearPolynomial(monomials = newMonomials)
}

// quantity polynomial and quantity monomial

@JvmName("quantityPolynomialPlusQuantityMonomial")
operator fun Quantity<AbstractLinearPolynomial<*>>.plus(rhs: Quantity<LinearMonomial>): Quantity<LinearPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(LinearPolynomial(this.value + rhs.value), this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(LinearPolynomial(this.value + rhs.to(this.unit)!!.value), this.unit)
        } else {
            Quantity(this.to(rhs.unit)!!.value + rhs.value, rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityMonomialPlusQuantityPolynomial")
operator fun Quantity<LinearMonomial>.plus(rhs: Quantity<AbstractLinearPolynomial<*>>): Quantity<LinearPolynomial> {
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

@JvmName("quantityPolynomialMinusQuantityMonomial")
operator fun Quantity<AbstractLinearPolynomial<*>>.minus(rhs: Quantity<LinearMonomial>): Quantity<LinearPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(LinearPolynomial(this.value - rhs.value), this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(LinearPolynomial(this.value - rhs.to(this.unit)!!.value), this.unit)
        } else {
            Quantity(this.to(rhs.unit)!!.value - rhs.value, rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityMonomialMinusQuantityPolynomial")
operator fun Quantity<LinearMonomial>.minus(rhs: Quantity<AbstractLinearPolynomial<*>>): Quantity<LinearPolynomial> {
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

// quantity polynomial and quantity polynomial

@JvmName("quantityPolynomialPlusQuantityPolynomial")
operator fun Quantity<AbstractLinearPolynomial<*>>.plus(rhs: Quantity<AbstractLinearPolynomial<*>>): Quantity<LinearPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(LinearPolynomial(this.value + rhs.value), this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(LinearPolynomial(this.value + rhs.to(this.unit)!!.value), this.unit)
        } else {
            Quantity(this.to(rhs.unit)!!.value + rhs.value, rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityPolynomialMinusQuantityPolynomial")
operator fun Quantity<AbstractLinearPolynomial<*>>.minus(rhs: Quantity<AbstractLinearPolynomial<*>>): Quantity<LinearPolynomial> {
    return if (this.unit == rhs.unit) {
        Quantity(LinearPolynomial(this.value - rhs.value), this.unit)
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            Quantity(LinearPolynomial(this.value - rhs.to(this.unit)!!.value), this.unit)
        } else {
            Quantity(this.to(rhs.unit)!!.value - rhs.value, rhs.unit)
        }
    } else {
        TODO("not implemented yet")
    }
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
    symbols: Iterable<LinearIntermediateSymbol>,
    ctor: (LinearIntermediateSymbol) -> LinearMonomial = { LinearMonomial(it) }
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

fun <T> sumVars(
    objs: Iterable<T>,
    extractor: (T) -> AbstractVariableItem<*, *>?
): LinearPolynomial {
    return sum(objs.mapNotNull(extractor))
}

fun <T> sumSymbols(
    objs: Iterable<T>,
    extractor: (T) -> LinearIntermediateSymbol?
): LinearPolynomial {
    return sum(objs.mapNotNull(extractor))
}

@JvmName("sumMapMonomials")
fun <T> sum(
    objs: Iterable<T>,
    extractor: (T) -> LinearMonomial?
): LinearPolynomial {
    return sum(objs.mapNotNull(extractor))
}

@JvmName("sumMapMonomialLists")
fun <T> flatSum(
    objs: Iterable<T>,
    extractor: (T) -> Iterable<LinearMonomial?>
): LinearPolynomial {
    return sum(objs.flatMap(extractor).filterNotNull())
}

// quantity sigma

@JvmName("sumQuantityLinearMonomials")
fun qtySum(monomials: Iterable<Quantity<LinearMonomial>>): Quantity<LinearPolynomial> {
    val quantityMonomials = monomials.toList()
    return if (quantityMonomials.isEmpty()) {
        Quantity(LinearPolynomial(), NoneUnit)
    } else {
        quantityMonomials.subList(1, quantityMonomials.size)
            .fold(Quantity(LinearPolynomial(monomials = listOf(quantityMonomials.first().value)), quantityMonomials.first().unit)) { acc, quantity ->
                acc + quantity
            }
    }
}

@JvmName("sumQuantityLinearPolynomials")
fun qtySum(polynomials: Iterable<Quantity<AbstractLinearPolynomial<*>>>): Quantity<LinearPolynomial> {
    val quantityPolynomials = polynomials.toList()
    return if (quantityPolynomials.isEmpty()) {
        Quantity(LinearPolynomial(), NoneUnit)
    } else {
        quantityPolynomials.subList(1, quantityPolynomials.size)
            .fold(Quantity(LinearPolynomial(quantityPolynomials.first().value), quantityPolynomials.first().unit)) { acc, quantity ->
                acc + quantity
            }
    }
}

@JvmName("sumMapQuantityMonomials")
fun <T> qtySum(
    objs: Iterable<T>,
    extractor: (T) -> Quantity<LinearMonomial>?
): Quantity<LinearPolynomial> {
    return qtySum(objs.mapNotNull(extractor))
}

@JvmName("sumMapQuantityMonomialLists")
fun <T> flatQtySum(
    objs: Iterable<T>,
    extractor: (T) -> Iterable<Quantity<LinearMonomial>?>
): Quantity<LinearPolynomial> {
    return qtySum(objs.flatMap(extractor).filterNotNull())
}
