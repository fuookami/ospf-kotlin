package fuookami.ospf.kotlin.core.frontend.inequality

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.physics.quantity.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.utils.math.symbol.Linear

class LinearInequality(
    override val lhs: AbstractLinearPolynomial<*>,
    override val rhs: AbstractLinearPolynomial<*>,
    sign: Sign,
    name: String = "",
    displayName: String? = null
) : Inequality<LinearInequality, LinearMonomialCell>(lhs, rhs, sign, name, displayName) {
    override val cells: List<LinearMonomialCell>
        get() {
            if (_cells.isEmpty()) {
                val cells = HashMap<VariableItemKey, LinearMonomialCell>()
                var constant = Flt64.zero
                for (cell in lhs.cells) {
                    when (val symbol = cell.cell) {
                        is Either.Left -> {
                            cells[symbol.value.variable.key] = cells[symbol.value.variable.key]?.let { it + cell } ?: cell
                        }

                        is Either.Right -> {
                            constant += symbol.value
                        }
                    }
                }
                for (cell in rhs.cells) {
                    when (val symbol = cell.cell) {
                        is Either.Left -> {
                            cells[symbol.value.variable.key] = cells[symbol.value.variable.key]?.let { it - cell } ?: -cell
                        }

                        is Either.Right -> {
                            constant -= symbol.value
                        }
                    }
                }
                val ret = cells.map { it.value }.toMutableList()
                ret.add(LinearMonomialCell(constant))
                _cells = ret
            }
            return _cells
        }

    override fun reverse(name: String?, displayName: String?): LinearInequality {
        return LinearInequality(
            lhs = LinearPolynomial(lhs.monomials.map { it.copy() }),
            rhs = LinearPolynomial(rhs.monomials.map { it.copy() }),
            sign = sign.reverse,
            name = name ?: this.name.let {
                if (it.isNotEmpty()) {
                    "not_$it"
                } else {
                    null
                }
            } ?: "",
            displayName = displayName ?: this.displayName?.let {
                if (it.isNotEmpty()) {
                    "not_$it"
                } else {
                    null
                }
            }
        )
    }

    override fun normalize(): LinearInequality {
        return LinearInequality(
            lhs = LinearPolynomial(lhs.monomials.map { it.copy() } + rhs.monomials.map { -it }),
            rhs = LinearPolynomial(-lhs.constant + rhs.constant),
            sign = sign,
            name = name,
            displayName = displayName
        )
    }
}

// variable and constant

infix fun AbstractVariableItem<*, *>.ls(rhs: Int): LinearInequality {
    return this.ls(Flt64(rhs))
}

infix fun AbstractVariableItem<*, *>.ls(rhs: Double): LinearInequality {
    return this.ls(Flt64(rhs))
}

infix fun <T : RealNumber<T>> AbstractVariableItem<*, *>.ls(rhs: T): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs.toFlt64()),
        Sign.Less
    )
}

infix fun AbstractVariableItem<*, *>.leq(rhs: Int): LinearInequality {
    return this.leq(Flt64(rhs))
}

infix fun AbstractVariableItem<*, *>.leq(rhs: Double): LinearInequality {
    return this.leq(Flt64(rhs))
}

infix fun <T : RealNumber<T>> AbstractVariableItem<*, *>.leq(rhs: T): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs.toFlt64()),
        Sign.LessEqual
    )
}

infix fun AbstractVariableItem<*, *>.eq(rhs: Int): LinearInequality {
    return this.eq(Flt64(rhs))
}

infix fun AbstractVariableItem<*, *>.eq(rhs: Double): LinearInequality {
    return this.eq(Flt64(rhs))
}

infix fun <T : RealNumber<T>> AbstractVariableItem<*, *>.eq(rhs: T): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs.toFlt64()),
        Sign.Equal
    )
}

infix fun AbstractVariableItem<*, *>.neq(rhs: Int): LinearInequality {
    return this.neq(Flt64(rhs))
}

infix fun AbstractVariableItem<*, *>.neq(rhs: Double): LinearInequality {
    return this.neq(Flt64(rhs))
}

infix fun <T : RealNumber<T>> AbstractVariableItem<*, *>.neq(rhs: T): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs.toFlt64()),
        Sign.Unequal
    )
}

infix fun AbstractVariableItem<*, *>.gr(rhs: Int): LinearInequality {
    return this.gr(Flt64(rhs))
}

infix fun AbstractVariableItem<*, *>.gr(rhs: Double): LinearInequality {
    return this.gr(Flt64(rhs))
}


infix fun <T : RealNumber<T>> AbstractVariableItem<*, *>.gr(rhs: T): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs.toFlt64()),
        Sign.Greater
    )
}

infix fun AbstractVariableItem<*, *>.geq(rhs: Int): LinearInequality {
    return this.geq(Flt64(rhs))
}

infix fun AbstractVariableItem<*, *>.geq(rhs: Double): LinearInequality {
    return this.geq(Flt64(rhs))
}

infix fun <T : RealNumber<T>> AbstractVariableItem<*, *>.geq(rhs: T): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs.toFlt64()),
        Sign.GreaterEqual
    )
}

infix fun Int.ls(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return Flt64(this).ls(rhs)
}

infix fun Double.ls(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return Flt64(this).ls(rhs)
}

infix fun <T : RealNumber<T>> T.ls(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.toFlt64()),
        LinearPolynomial(rhs),
        Sign.Less
    )
}

infix fun Int.leq(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return Flt64(this).leq(rhs)
}

infix fun Double.leq(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return Flt64(this).leq(rhs)
}

infix fun <T : RealNumber<T>> T.leq(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.toFlt64()),
        LinearPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun Int.eq(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return Flt64(this).eq(rhs)
}

infix fun Double.eq(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return Flt64(this).eq(rhs)
}

infix fun <T : RealNumber<T>> T.eq(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.toFlt64()),
        LinearPolynomial(rhs),
        Sign.Equal
    )
}

infix fun Int.neq(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return Flt64(this).neq(rhs)
}

infix fun Double.neq(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return Flt64(this).neq(rhs)
}

infix fun <T : RealNumber<T>> T.neq(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.toFlt64()),
        LinearPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun Int.gr(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return Flt64(this).gr(rhs)
}

infix fun Double.gr(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return Flt64(this).gr(rhs)
}

infix fun <T : RealNumber<T>> T.gr(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.toFlt64()),
        LinearPolynomial(rhs),
        Sign.Greater
    )
}

infix fun Int.geq(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return Flt64(this).geq(rhs)
}

infix fun Double.geq(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return Flt64(this).geq(rhs)
}

infix fun <T : RealNumber<T>> T.geq(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.toFlt64()),
        LinearPolynomial(rhs),
        Sign.GreaterEqual
    )
}

// quantity variable and quantity

@JvmName("quantityVariableLsQuantity")
infix fun <T : RealNumber<T>> Quantity<AbstractVariableItem<*, *>>.ls(rhs: Quantity<T>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value ls rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value ls rhs.toFlt64().to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value ls rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityVariableLeqQuantity")
infix fun <T : RealNumber<T>> Quantity<AbstractVariableItem<*, *>>.leq(rhs: Quantity<T>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value leq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value leq rhs.toFlt64().to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value leq rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityVariableEqQuantity")
infix fun <T : RealNumber<T>> Quantity<AbstractVariableItem<*, *>>.eq(rhs: Quantity<T>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value eq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value eq rhs.toFlt64().to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value eq rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityVariableNeqQuantity")
infix fun <T : RealNumber<T>> Quantity<AbstractVariableItem<*, *>>.neq(rhs: Quantity<T>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value neq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value neq rhs.toFlt64().to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value neq rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityVariableGrQuantity")
infix fun <T : RealNumber<T>> Quantity<AbstractVariableItem<*, *>>.gr(rhs: Quantity<T>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value gr rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value gr rhs.toFlt64().to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value gr rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityVariableGeqQuantity")
infix fun <T : RealNumber<T>> Quantity<AbstractVariableItem<*, *>>.geq(rhs: Quantity<T>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value geq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value geq rhs.toFlt64().to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value geq rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLsQuantityVariable")
infix fun <T : RealNumber<T>> Quantity<T>.ls(rhs: Quantity<AbstractVariableItem<*, *>>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value ls rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.toFlt64().to(this.unit)!!.value ls rhs.value
        } else {
            this.value ls rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLeqQuantityVariable")
infix fun <T : RealNumber<T>> Quantity<T>.leq(rhs: Quantity<AbstractVariableItem<*, *>>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value leq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.toFlt64().to(this.unit)!!.value leq rhs.value
        } else {
            this.value leq rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityEqQuantityVariable")
infix fun <T : RealNumber<T>> Quantity<T>.eq(rhs: Quantity<AbstractVariableItem<*, *>>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value eq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.toFlt64().to(this.unit)!!.value eq rhs.value
        } else {
            this.value eq rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityNeqQuantityVariable")
infix fun <T : RealNumber<T>> Quantity<T>.neq(rhs: Quantity<AbstractVariableItem<*, *>>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value neq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.toFlt64().to(this.unit)!!.value neq rhs.value
        } else {
            this.value neq rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityGrQuantityVariable")
infix fun <T : RealNumber<T>> Quantity<T>.gr(rhs: Quantity<AbstractVariableItem<*, *>>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value gr rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.toFlt64().to(this.unit)!!.value gr rhs.value
        } else {
            this.value gr rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityGeqQuantityVariable")
infix fun <T : RealNumber<T>> Quantity<T>.geq(rhs: Quantity<AbstractVariableItem<*, *>>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value geq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.toFlt64().to(this.unit)!!.value geq rhs.value
        } else {
            this.value geq rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

// symbol and constant

infix fun LinearIntermediateSymbol.ls(rhs: Int): LinearInequality {
    return this.ls(Flt64(rhs))
}

infix fun LinearIntermediateSymbol.ls(rhs: Double): LinearInequality {
    return this.ls(Flt64(rhs))
}

infix fun <T : RealNumber<T>> LinearIntermediateSymbol.ls(rhs: T): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs.toFlt64()),
        Sign.Less
    )
}

infix fun LinearIntermediateSymbol.leq(rhs: Int): LinearInequality {
    return this.leq(Flt64(rhs))
}

infix fun LinearIntermediateSymbol.leq(rhs: Double): LinearInequality {
    return this.leq(Flt64(rhs))
}

infix fun <T : RealNumber<T>> LinearIntermediateSymbol.leq(rhs: T): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs.toFlt64()),
        Sign.LessEqual
    )
}

infix fun LinearIntermediateSymbol.eq(rhs: Int): LinearInequality {
    return this.eq(Flt64(rhs))
}

infix fun LinearIntermediateSymbol.eq(rhs: Double): LinearInequality {
    return this.eq(Flt64(rhs))
}

infix fun <T : RealNumber<T>> LinearIntermediateSymbol.eq(rhs: T): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs.toFlt64()),
        Sign.Equal
    )
}

infix fun LinearIntermediateSymbol.neq(rhs: Int): LinearInequality {
    return this.neq(Flt64(rhs))
}

infix fun LinearIntermediateSymbol.neq(rhs: Double): LinearInequality {
    return this.neq(Flt64(rhs))
}

infix fun <T : RealNumber<T>> LinearIntermediateSymbol.neq(rhs: T): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs.toFlt64()),
        Sign.Unequal
    )
}

infix fun LinearIntermediateSymbol.gr(rhs: Int): LinearInequality {
    return this.gr(Flt64(rhs))
}

infix fun LinearIntermediateSymbol.gr(rhs: Double): LinearInequality {
    return this.gr(Flt64(rhs))
}

infix fun <T : RealNumber<T>> LinearIntermediateSymbol.gr(rhs: T): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs.toFlt64()),
        Sign.Greater
    )
}

infix fun LinearIntermediateSymbol.geq(rhs: Int): LinearInequality {
    return this.geq(Flt64(rhs))
}

infix fun LinearIntermediateSymbol.geq(rhs: Double): LinearInequality {
    return this.geq(Flt64(rhs))
}

infix fun <T : RealNumber<T>> LinearIntermediateSymbol.geq(rhs: T): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs.toFlt64()),
        Sign.GreaterEqual
    )
}

infix fun Int.ls(rhs: LinearIntermediateSymbol): LinearInequality {
    return Flt64(this).ls(rhs)
}

infix fun Double.ls(rhs: LinearIntermediateSymbol): LinearInequality {
    return Flt64(this).ls(rhs)
}

infix fun <T : RealNumber<T>> T.ls(rhs: LinearIntermediateSymbol): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.toFlt64()),
        LinearPolynomial(rhs),
        Sign.Less
    )
}

infix fun Int.leq(rhs: LinearIntermediateSymbol): LinearInequality {
    return Flt64(this).leq(rhs)
}

infix fun Double.leq(rhs: LinearIntermediateSymbol): LinearInequality {
    return Flt64(this).leq(rhs)
}

infix fun <T : RealNumber<T>> T.leq(rhs: LinearIntermediateSymbol): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.toFlt64()),
        LinearPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun Int.eq(rhs: LinearIntermediateSymbol): LinearInequality {
    return Flt64(this).eq(rhs)
}

infix fun Double.eq(rhs: LinearIntermediateSymbol): LinearInequality {
    return Flt64(this).eq(rhs)
}

infix fun <T : RealNumber<T>> T.eq(rhs: LinearIntermediateSymbol): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.toFlt64()),
        LinearPolynomial(rhs),
        Sign.Equal
    )
}

infix fun Int.neq(rhs: LinearIntermediateSymbol): LinearInequality {
    return Flt64(this).neq(rhs)
}

infix fun Double.neq(rhs: LinearIntermediateSymbol): LinearInequality {
    return Flt64(this).neq(rhs)
}

infix fun <T : RealNumber<T>> T.neq(rhs: LinearIntermediateSymbol): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.toFlt64()),
        LinearPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun Int.gr(rhs: LinearIntermediateSymbol): LinearInequality {
    return Flt64(this).gr(rhs)
}

infix fun Double.gr(rhs: LinearIntermediateSymbol): LinearInequality {
    return Flt64(this).gr(rhs)
}

infix fun <T : RealNumber<T>> T.gr(rhs: LinearIntermediateSymbol): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.toFlt64()),
        LinearPolynomial(rhs),
        Sign.Greater
    )
}

infix fun Int.geq(rhs: LinearIntermediateSymbol): LinearInequality {
    return Flt64(this).geq(rhs)
}

infix fun Double.geq(rhs: LinearIntermediateSymbol): LinearInequality {
    return Flt64(this).geq(rhs)
}

infix fun <T : RealNumber<T>> T.geq(rhs: LinearIntermediateSymbol): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.toFlt64()),
        LinearPolynomial(rhs),
        Sign.GreaterEqual
    )
}

// quantity symbol and quantity

@JvmName("quantitySymbolLsQuantity")
infix fun <T : RealNumber<T>> Quantity<LinearIntermediateSymbol>.ls(rhs: Quantity<T>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value ls rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value ls rhs.toFlt64().to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value ls rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantitySymbolLeqQuantity")
infix fun <T : RealNumber<T>> Quantity<LinearIntermediateSymbol>.leq(rhs: Quantity<T>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value leq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value leq rhs.toFlt64().to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value leq rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantitySymbolEqQuantity")
infix fun <T : RealNumber<T>> Quantity<LinearIntermediateSymbol>.eq(rhs: Quantity<T>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value eq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value eq rhs.toFlt64().to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value eq rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantitySymbolNeqQuantity")
infix fun <T : RealNumber<T>> Quantity<LinearIntermediateSymbol>.neq(rhs: Quantity<T>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value neq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value neq rhs.toFlt64().to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value neq rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantitySymbolGrQuantity")
infix fun <T : RealNumber<T>> Quantity<LinearIntermediateSymbol>.gr(rhs: Quantity<T>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value gr rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value gr rhs.toFlt64().to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value gr rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantitySymbolGeqQuantity")
infix fun <T : RealNumber<T>> Quantity<LinearIntermediateSymbol>.geq(rhs: Quantity<T>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value geq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value geq rhs.toFlt64().to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value geq rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLsQuantitySymbol")
infix fun <T : RealNumber<T>> Quantity<T>.ls(rhs: Quantity<LinearIntermediateSymbol>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value ls rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.toFlt64().to(this.unit)!!.value ls rhs.value
        } else {
            this.value ls rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLeqQuantitySymbol")
infix fun <T : RealNumber<T>> Quantity<T>.leq(rhs: Quantity<LinearIntermediateSymbol>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value leq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.toFlt64().to(this.unit)!!.value leq rhs.value
        } else {
            this.value leq rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityEqQuantitySymbol")
infix fun <T : RealNumber<T>> Quantity<T>.eq(rhs: Quantity<LinearIntermediateSymbol>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value eq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.toFlt64().to(this.unit)!!.value eq rhs.value
        } else {
            this.value eq rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityNeqQuantitySymbol")
infix fun <T : RealNumber<T>> Quantity<T>.neq(rhs: Quantity<LinearIntermediateSymbol>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value neq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.toFlt64().to(this.unit)!!.value neq rhs.value
        } else {
            this.value neq rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityGrQuantitySymbol")
infix fun <T : RealNumber<T>> Quantity<T>.gr(rhs: Quantity<LinearIntermediateSymbol>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value gr rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.toFlt64().to(this.unit)!!.value gr rhs.value
        } else {
            this.value gr rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityGeqQuantitySymbol")
infix fun <T : RealNumber<T>> Quantity<T>.geq(rhs: Quantity<LinearIntermediateSymbol>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value geq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.toFlt64().to(this.unit)!!.value geq rhs.value
        } else {
            this.value geq rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

// monomial and constant

infix fun LinearMonomial.ls(rhs: Int): LinearInequality {
    return this.ls(Flt64(rhs))
}

infix fun LinearMonomial.ls(rhs: Double): LinearInequality {
    return this.ls(Flt64(rhs))
}

infix fun <T : RealNumber<T>> LinearMonomial.ls(rhs: T): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.copy()),
        LinearPolynomial(rhs.toFlt64()),
        Sign.Less
    )
}

infix fun LinearMonomial.leq(rhs: Int): LinearInequality {
    return this.leq(Flt64(rhs))
}

infix fun LinearMonomial.leq(rhs: Double): LinearInequality {
    return this.leq(Flt64(rhs))
}

infix fun <T : RealNumber<T>> LinearMonomial.leq(rhs: T): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.copy()),
        LinearPolynomial(rhs.toFlt64()),
        Sign.LessEqual
    )
}

infix fun LinearMonomial.eq(rhs: Int): LinearInequality {
    return this.eq(Flt64(rhs))
}

infix fun LinearMonomial.eq(rhs: Double): LinearInequality {
    return this.eq(Flt64(rhs))
}

infix fun <T : RealNumber<T>> LinearMonomial.eq(rhs: T): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.copy()),
        LinearPolynomial(rhs.toFlt64()),
        Sign.Equal
    )
}

infix fun LinearMonomial.neq(rhs: Int): LinearInequality {
    return this.neq(Flt64(rhs))
}

infix fun LinearMonomial.neq(rhs: Double): LinearInequality {
    return this.neq(Flt64(rhs))
}

infix fun <T : RealNumber<T>> LinearMonomial.neq(rhs: T): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.copy()),
        LinearPolynomial(rhs.toFlt64()),
        Sign.Unequal
    )
}

infix fun LinearMonomial.gr(rhs: Int): LinearInequality {
    return this.gr(Flt64(rhs))
}

infix fun LinearMonomial.gr(rhs: Double): LinearInequality {
    return this.gr(Flt64(rhs))
}

infix fun <T : RealNumber<T>> LinearMonomial.gr(rhs: T): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.copy()),
        LinearPolynomial(rhs.toFlt64()),
        Sign.Greater
    )
}

infix fun LinearMonomial.geq(rhs: Int): LinearInequality {
    return this.geq(Flt64(rhs))
}

infix fun LinearMonomial.geq(rhs: Double): LinearInequality {
    return this.geq(Flt64(rhs))
}

infix fun <T : RealNumber<T>> LinearMonomial.geq(rhs: T): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.copy()),
        LinearPolynomial(rhs.toFlt64()),
        Sign.GreaterEqual
    )
}

infix fun Int.ls(rhs: LinearMonomial): LinearInequality {
    return Flt64(this).ls(rhs)
}

infix fun Double.ls(rhs: LinearMonomial): LinearInequality {
    return Flt64(this).ls(rhs)
}

infix fun <T : RealNumber<T>> T.ls(rhs: LinearMonomial): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.toFlt64()),
        LinearPolynomial(rhs.copy()),
        Sign.Less
    )
}

infix fun Int.leq(rhs: LinearMonomial): LinearInequality {
    return Flt64(this).leq(rhs)
}

infix fun Double.leq(rhs: LinearMonomial): LinearInequality {
    return Flt64(this).leq(rhs)
}

infix fun <T : RealNumber<T>> T.leq(rhs: LinearMonomial): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.toFlt64()),
        LinearPolynomial(rhs.copy()),
        Sign.LessEqual
    )
}

infix fun Int.eq(rhs: LinearMonomial): LinearInequality {
    return Flt64(this).eq(rhs)
}

infix fun Double.eq(rhs: LinearMonomial): LinearInequality {
    return Flt64(this).eq(rhs)
}

infix fun <T : RealNumber<T>> T.eq(rhs: LinearMonomial): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.toFlt64()),
        LinearPolynomial(rhs.copy()),
        Sign.Equal
    )
}

infix fun Int.neq(rhs: LinearMonomial): LinearInequality {
    return Flt64(this).neq(rhs)
}

infix fun Double.neq(rhs: LinearMonomial): LinearInequality {
    return Flt64(this).neq(rhs)
}

infix fun <T : RealNumber<T>> T.neq(rhs: LinearMonomial): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.toFlt64()),
        LinearPolynomial(rhs.copy()),
        Sign.Unequal
    )
}

infix fun Int.gr(rhs: LinearMonomial): LinearInequality {
    return Flt64(this).gr(rhs)
}

infix fun Double.gr(rhs: LinearMonomial): LinearInequality {
    return Flt64(this).gr(rhs)
}

infix fun <T : RealNumber<T>> T.gr(rhs: LinearMonomial): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.toFlt64()),
        LinearPolynomial(rhs.copy()),
        Sign.Greater
    )
}

infix fun Int.geq(rhs: LinearMonomial): LinearInequality {
    return Flt64(this).geq(rhs)
}

infix fun Double.geq(rhs: LinearMonomial): LinearInequality {
    return Flt64(this).geq(rhs)
}

infix fun <T : RealNumber<T>> T.geq(rhs: LinearMonomial): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.toFlt64()),
        LinearPolynomial(rhs.copy()),
        Sign.GreaterEqual
    )
}

// quantity monomial and quantity

@JvmName("quantityMonomialLsQuantity")
infix fun <T : RealNumber<T>> Quantity<LinearMonomial>.ls(rhs: Quantity<T>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value ls rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value ls rhs.toFlt64().to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value ls rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityMonomialLeqQuantity")
infix fun <T : RealNumber<T>> Quantity<LinearMonomial>.leq(rhs: Quantity<T>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value leq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value leq rhs.toFlt64().to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value leq rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityMonomialEqQuantity")
infix fun <T : RealNumber<T>> Quantity<LinearMonomial>.eq(rhs: Quantity<T>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value eq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value eq rhs.toFlt64().to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value eq rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityMonomialNeqQuantity")
infix fun <T : RealNumber<T>> Quantity<LinearMonomial>.neq(rhs: Quantity<T>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value neq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value neq rhs.toFlt64().to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value neq rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityMonomialGrQuantity")
infix fun <T : RealNumber<T>> Quantity<LinearMonomial>.gr(rhs: Quantity<T>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value gr rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value gr rhs.toFlt64().to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value gr rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityMonomialGeqQuantity")
infix fun <T : RealNumber<T>> Quantity<LinearMonomial>.geq(rhs: Quantity<T>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value geq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value geq rhs.toFlt64().to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value geq rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLsQuantityMonomial")
infix fun <T : RealNumber<T>> Quantity<T>.ls(rhs: Quantity<LinearMonomial>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value ls rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.toFlt64().to(this.unit)!!.value ls rhs.value
        } else {
            this.value ls rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLeqQuantityMonomial")
infix fun <T : RealNumber<T>> Quantity<T>.leq(rhs: Quantity<LinearMonomial>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value leq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.toFlt64().to(this.unit)!!.value leq rhs.value
        } else {
            this.value leq rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityEqQuantityMonomial")
infix fun <T : RealNumber<T>> Quantity<T>.eq(rhs: Quantity<LinearMonomial>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value eq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.toFlt64().to(this.unit)!!.value eq rhs.value
        } else {
            this.value eq rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityNeqQuantityMonomial")
infix fun <T : RealNumber<T>> Quantity<T>.neq(rhs: Quantity<LinearMonomial>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value neq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.toFlt64().to(this.unit)!!.value neq rhs.value
        } else {
            this.value neq rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityGrQuantityMonomial")
infix fun <T : RealNumber<T>> Quantity<T>.gr(rhs: Quantity<LinearMonomial>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value gr rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.toFlt64().to(this.unit)!!.value gr rhs.value
        } else {
            this.value gr rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityGeqQuantityMonomial")
infix fun <T : RealNumber<T>> Quantity<T>.geq(rhs: Quantity<LinearMonomial>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value geq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.toFlt64().to(this.unit)!!.value geq rhs.value
        } else {
            this.value geq rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

// polynomial and constant

infix fun AbstractLinearPolynomial<*>.ls(rhs: Int): LinearInequality {
    return this.ls(Flt64(rhs))
}

infix fun AbstractLinearPolynomial<*>.ls(rhs: Double): LinearInequality {
    return this.ls(Flt64(rhs))
}

infix fun <T : RealNumber<T>> AbstractLinearPolynomial<*>.ls(rhs: T): LinearInequality {
    return LinearInequality(
        this.copy(),
        LinearPolynomial(rhs),
        Sign.Less
    )
}

infix fun AbstractLinearPolynomial<*>.leq(rhs: Int): LinearInequality {
    return this.leq(Flt64(rhs))
}

infix fun AbstractLinearPolynomial<*>.leq(rhs: Double): LinearInequality {
    return this.leq(Flt64(rhs))
}

infix fun <T : RealNumber<T>> AbstractLinearPolynomial<*>.leq(rhs: T): LinearInequality {
    return LinearInequality(
        this.copy(),
        LinearPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun AbstractLinearPolynomial<*>.eq(rhs: Int): LinearInequality {
    return this.eq(Flt64(rhs))
}

infix fun AbstractLinearPolynomial<*>.eq(rhs: Double): LinearInequality {
    return this.eq(Flt64(rhs))
}

infix fun <T : RealNumber<T>> AbstractLinearPolynomial<*>.eq(rhs: T): LinearInequality {
    return LinearInequality(
        this.copy(),
        LinearPolynomial(rhs),
        Sign.Equal
    )
}

infix fun AbstractLinearPolynomial<*>.neq(rhs: Int): LinearInequality {
    return this.neq(Flt64(rhs))
}

infix fun AbstractLinearPolynomial<*>.neq(rhs: Double): LinearInequality {
    return this.neq(Flt64(rhs))
}

infix fun <T : RealNumber<T>> AbstractLinearPolynomial<*>.neq(rhs: T): LinearInequality {
    return LinearInequality(
        this.copy(),
        LinearPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun AbstractLinearPolynomial<*>.gr(rhs: Int): LinearInequality {
    return this.gr(Flt64(rhs))
}

infix fun AbstractLinearPolynomial<*>.gr(rhs: Double): LinearInequality {
    return this.gr(Flt64(rhs))
}

infix fun <T : RealNumber<T>> AbstractLinearPolynomial<*>.gr(rhs: T): LinearInequality {
    return LinearInequality(
        this.copy(),
        LinearPolynomial(rhs),
        Sign.Greater
    )
}

infix fun AbstractLinearPolynomial<*>.geq(rhs: Int): LinearInequality {
    return this.geq(Flt64(rhs))
}

infix fun AbstractLinearPolynomial<*>.geq(rhs: Double): LinearInequality {
    return this.geq(Flt64(rhs))
}

infix fun <T : RealNumber<T>> AbstractLinearPolynomial<*>.geq(rhs: T): LinearInequality {
    return LinearInequality(
        this.copy(),
        LinearPolynomial(rhs),
        Sign.GreaterEqual
    )
}

infix fun Int.ls(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return Flt64(this).ls(rhs)
}

infix fun Double.ls(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return Flt64(this).ls(rhs)
}

infix fun <T : RealNumber<T>> T.ls(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        rhs.copy(),
        Sign.Less
    )
}

infix fun Int.leq(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return Flt64(this).leq(rhs)
}

infix fun Double.leq(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return Flt64(this).leq(rhs)
}

infix fun <T : RealNumber<T>> T.leq(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        rhs.copy(),
        Sign.LessEqual
    )
}

infix fun Int.eq(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return Flt64(this).eq(rhs)
}

infix fun Double.eq(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return Flt64(this).eq(rhs)
}

infix fun <T : RealNumber<T>> T.eq(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        rhs.copy(),
        Sign.Equal
    )
}

infix fun Int.neq(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return Flt64(this).neq(rhs)
}

infix fun Double.neq(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return Flt64(this).neq(rhs)
}

infix fun <T : RealNumber<T>> T.neq(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        rhs.copy(),
        Sign.Unequal
    )
}

infix fun Int.gr(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return Flt64(this).gr(rhs)
}

infix fun Double.gr(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return Flt64(this).gr(rhs)
}

infix fun <T : RealNumber<T>> T.gr(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        rhs.copy(),
        Sign.Greater
    )
}

infix fun Int.geq(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return Flt64(this).geq(rhs)
}

infix fun Double.geq(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return Flt64(this).geq(rhs)
}

infix fun <T : RealNumber<T>> T.geq(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        rhs.copy(),
        Sign.GreaterEqual
    )
}

// quantity polynomial and quantity

@JvmName("quantityPolynomialLsQuantity")
infix fun <T : RealNumber<T>> Quantity<AbstractLinearPolynomial<*>>.ls(rhs: Quantity<T>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value ls rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value ls rhs.toFlt64().to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value ls rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityPolynomialLeqQuantity")
infix fun <T : RealNumber<T>> Quantity<AbstractLinearPolynomial<*>>.leq(rhs: Quantity<T>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value leq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value leq rhs.toFlt64().to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value leq rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityPolynomialEqQuantity")
infix fun <T : RealNumber<T>> Quantity<AbstractLinearPolynomial<*>>.eq(rhs: Quantity<T>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value eq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value eq rhs.toFlt64().to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value eq rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityPolynomialNeqQuantity")
infix fun <T : RealNumber<T>> Quantity<AbstractLinearPolynomial<*>>.neq(rhs: Quantity<T>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value neq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value neq rhs.toFlt64().to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value neq rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityPolynomialGrQuantity")
infix fun <T : RealNumber<T>> Quantity<AbstractLinearPolynomial<*>>.gr(rhs: Quantity<T>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value gr rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value gr rhs.toFlt64().to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value gr rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityPolynomialGeqQuantity")
infix fun <T : RealNumber<T>> Quantity<AbstractLinearPolynomial<*>>.geq(rhs: Quantity<T>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value geq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value geq rhs.toFlt64().to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value geq rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLsQuantityPolynomial")
infix fun <T : RealNumber<T>> Quantity<T>.ls(rhs: Quantity<AbstractLinearPolynomial<*>>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value ls rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.toFlt64().to(this.unit)!!.value ls rhs.value
        } else {
            this.value ls rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLeqQuantityPolynomial")
infix fun <T : RealNumber<T>> Quantity<T>.leq(rhs: Quantity<AbstractLinearPolynomial<*>>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value leq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.toFlt64().to(this.unit)!!.value leq rhs.value
        } else {
            this.value leq rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityEqQuantityPolynomial")
infix fun <T : RealNumber<T>> Quantity<T>.eq(rhs: Quantity<AbstractLinearPolynomial<*>>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value eq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.toFlt64().to(this.unit)!!.value eq rhs.value
        } else {
            this.value eq rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityNeqQuantityPolynomial")
infix fun <T : RealNumber<T>> Quantity<T>.neq(rhs: Quantity<AbstractLinearPolynomial<*>>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value neq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.toFlt64().to(this.unit)!!.value neq rhs.value
        } else {
            this.value neq rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityGrQuantityPolynomial")
infix fun <T : RealNumber<T>> Quantity<T>.gr(rhs: Quantity<AbstractLinearPolynomial<*>>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value gr rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.toFlt64().to(this.unit)!!.value gr rhs.value
        } else {
            this.value gr rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityGeqQuantityPolynomial")
infix fun <T : RealNumber<T>> Quantity<T>.geq(rhs: Quantity<AbstractLinearPolynomial<*>>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value geq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.toFlt64().to(this.unit)!!.value geq rhs.value
        } else {
            this.value geq rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

// variable and variable

infix fun AbstractVariableItem<*, *>.ls(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs),
        Sign.Less
    )
}

infix fun AbstractVariableItem<*, *>.leq(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun AbstractVariableItem<*, *>.eq(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs),
        Sign.Equal
    )
}

infix fun AbstractVariableItem<*, *>.neq(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun AbstractVariableItem<*, *>.gr(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs),
        Sign.Greater
    )
}

infix fun AbstractVariableItem<*, *>.geq(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs),
        Sign.GreaterEqual
    )
}

// quantity variable and quantity variable

@JvmName("quantityVariableLsQuantityVariable")
infix fun Quantity<AbstractVariableItem<*, *>>.ls(rhs: Quantity<AbstractVariableItem<*, *>>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value ls rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.to(this.unit)!!.value ls rhs.value
        } else {
            this.value ls rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityVariableLeqQuantityVariable")
infix fun Quantity<AbstractVariableItem<*, *>>.leq(rhs: Quantity<AbstractVariableItem<*, *>>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value leq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.to(this.unit)!!.value leq rhs.value
        } else {
            this.value leq rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityVariableEqQuantityVariable")
infix fun Quantity<AbstractVariableItem<*, *>>.eq(rhs: Quantity<AbstractVariableItem<*, *>>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value eq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.to(this.unit)!!.value eq rhs.value
        } else {
            this.value eq rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityVariableNeqQuantityVariable")
infix fun Quantity<AbstractVariableItem<*, *>>.neq(rhs: Quantity<AbstractVariableItem<*, *>>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value neq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.to(this.unit)!!.value neq rhs.value
        } else {
            this.value neq rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityVariableGrQuantityVariable")
infix fun Quantity<AbstractVariableItem<*, *>>.gr(rhs: Quantity<AbstractVariableItem<*, *>>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value gr rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.to(this.unit)!!.value gr rhs.value
        } else {
            this.value gr rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityVariableGeqQuantityVariable")
infix fun Quantity<AbstractVariableItem<*, *>>.geq(rhs: Quantity<AbstractVariableItem<*, *>>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value geq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.to(this.unit)!!.value geq rhs.value
        } else {
            this.value geq rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

// symbol and variable

infix fun LinearIntermediateSymbol.ls(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs),
        Sign.Less
    )
}

infix fun LinearIntermediateSymbol.leq(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun LinearIntermediateSymbol.eq(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs),
        Sign.Equal
    )
}

infix fun LinearIntermediateSymbol.neq(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun LinearIntermediateSymbol.gr(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs),
        Sign.Greater
    )
}

infix fun LinearIntermediateSymbol.geq(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs),
        Sign.GreaterEqual
    )
}

infix fun AbstractVariableItem<*, *>.ls(rhs: LinearIntermediateSymbol): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs),
        Sign.Less
    )
}

infix fun AbstractVariableItem<*, *>.leq(rhs: LinearIntermediateSymbol): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun AbstractVariableItem<*, *>.eq(rhs: LinearIntermediateSymbol): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs),
        Sign.Equal
    )
}

infix fun AbstractVariableItem<*, *>.neq(rhs: LinearIntermediateSymbol): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun AbstractVariableItem<*, *>.gr(rhs: LinearIntermediateSymbol): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs),
        Sign.Greater
    )
}

infix fun AbstractVariableItem<*, *>.geq(rhs: LinearIntermediateSymbol): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs),
        Sign.GreaterEqual
    )
}

// quantity symbol and quantity variable

@JvmName("quantityLinearSymbolLsQuantityVariable")
infix fun Quantity<LinearIntermediateSymbol>.ls(rhs: Quantity<AbstractVariableItem<*, *>>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value ls rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.to(this.unit)!!.value ls rhs.value
        } else {
            this.value ls rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearSymbolLeqQuantityVariable")
infix fun Quantity<LinearIntermediateSymbol>.leq(rhs: Quantity<AbstractVariableItem<*, *>>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value leq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.to(this.unit)!!.value leq rhs.value
        } else {
            this.value leq rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearSymbolEqQuantityVariable")
infix fun Quantity<LinearIntermediateSymbol>.eq(rhs: Quantity<AbstractVariableItem<*, *>>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value eq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.to(this.unit)!!.value eq rhs.value
        } else {
            this.value eq rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearSymbolNeqQuantityVariable")
infix fun Quantity<LinearIntermediateSymbol>.neq(rhs: Quantity<AbstractVariableItem<*, *>>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value neq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.to(this.unit)!!.value neq rhs.value
        } else {
            this.value neq rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearSymbolGrQuantityVariable")
infix fun Quantity<LinearIntermediateSymbol>.gr(rhs: Quantity<AbstractVariableItem<*, *>>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value gr rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.to(this.unit)!!.value gr rhs.value
        } else {
            this.value gr rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearSymbolGeqQuantityVariable")
infix fun Quantity<LinearIntermediateSymbol>.geq(rhs: Quantity<AbstractVariableItem<*, *>>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value geq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.to(this.unit)!!.value geq rhs.value
        } else {
            this.value geq rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityVariableLsQuantityLinearSymbol")
infix fun Quantity<AbstractVariableItem<*, *>>.ls(rhs: Quantity<LinearIntermediateSymbol>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value ls rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.to(this.unit)!!.value ls rhs.value
        } else {
            this.value ls rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityVariableLeqQuantityLinearSymbol")
infix fun Quantity<AbstractVariableItem<*, *>>.leq(rhs: Quantity<LinearIntermediateSymbol>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value leq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.to(this.unit)!!.value leq rhs.value
        } else {
            this.value leq rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityVariableEqQuantityLinearSymbol")
infix fun Quantity<AbstractVariableItem<*, *>>.eq(rhs: Quantity<LinearIntermediateSymbol>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value eq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.to(this.unit)!!.value eq rhs.value
        } else {
            this.value eq rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityVariableNeqQuantityLinearSymbol")
infix fun Quantity<AbstractVariableItem<*, *>>.neq(rhs: Quantity<LinearIntermediateSymbol>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value neq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.to(this.unit)!!.value neq rhs.value
        } else {
            this.value neq rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityVariableGrQuantityLinearSymbol")
infix fun Quantity<AbstractVariableItem<*, *>>.gr(rhs: Quantity<LinearIntermediateSymbol>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value gr rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.to(this.unit)!!.value gr rhs.value
        } else {
            this.value gr rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityVariableGeqQuantityLinearSymbol")
infix fun Quantity<AbstractVariableItem<*, *>>.geq(rhs: Quantity<LinearIntermediateSymbol>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value geq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.to(this.unit)!!.value geq rhs.value
        } else {
            this.value geq rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

// monomial and variable

infix fun LinearMonomial.ls(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.copy()),
        LinearPolynomial(rhs),
        Sign.Less
    )
}

infix fun LinearMonomial.leq(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.copy()),
        LinearPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun LinearMonomial.eq(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.copy()),
        LinearPolynomial(rhs),
        Sign.Equal
    )
}

infix fun LinearMonomial.neq(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.copy()),
        LinearPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun LinearMonomial.gr(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.copy()),
        LinearPolynomial(rhs),
        Sign.Greater
    )
}

infix fun LinearMonomial.geq(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.copy()),
        LinearPolynomial(rhs),
        Sign.GreaterEqual
    )
}

infix fun AbstractVariableItem<*, *>.ls(rhs: LinearMonomial): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs.copy()),
        Sign.Less
    )
}

infix fun AbstractVariableItem<*, *>.leq(rhs: LinearMonomial): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs.copy()),
        Sign.LessEqual
    )
}

infix fun AbstractVariableItem<*, *>.eq(rhs: LinearMonomial): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs.copy()),
        Sign.Equal
    )
}

infix fun AbstractVariableItem<*, *>.neq(rhs: LinearMonomial): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs.copy()),
        Sign.Unequal
    )
}

infix fun AbstractVariableItem<*, *>.gr(rhs: LinearMonomial): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs.copy()),
        Sign.Greater
    )
}

infix fun AbstractVariableItem<*, *>.geq(rhs: LinearMonomial): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs.copy()),
        Sign.GreaterEqual
    )
}

// quantity monomial and quantity variable

@JvmName("quantityLinearMonomialLsQuantityVariable")
infix fun Quantity<LinearMonomial>.ls(rhs: Quantity<AbstractVariableItem<*, *>>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value ls rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.to(this.unit)!!.value ls rhs.value
        } else {
            this.value ls rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearMonomialLeqQuantityVariable")
infix fun Quantity<LinearMonomial>.leq(rhs: Quantity<AbstractVariableItem<*, *>>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value leq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.to(this.unit)!!.value leq rhs.value
        } else {
            this.value leq rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("QuantityLinearMonomialEqQuantityVariable")
infix fun Quantity<LinearMonomial>.eq(rhs: Quantity<AbstractVariableItem<*, *>>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value eq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.to(this.unit)!!.value eq rhs.value
        } else {
            this.value eq rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearMonomialNeqQuantityVariable")
infix fun Quantity<LinearMonomial>.neq(rhs: Quantity<AbstractVariableItem<*, *>>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value neq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.to(this.unit)!!.value neq rhs.value
        } else {
            this.value neq rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearMonomialGrQuantityVariable")
infix fun Quantity<LinearMonomial>.gr(rhs: Quantity<AbstractVariableItem<*, *>>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value gr rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.to(this.unit)!!.value gr rhs.value
        } else {
            this.value gr rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearMonomialGeqQuantityVariable")
infix fun Quantity<LinearMonomial>.geq(rhs: Quantity<AbstractVariableItem<*, *>>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value geq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.to(this.unit)!!.value geq rhs.value
        } else {
            this.value geq rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityVariableLsQuantityLinearMonomial")
infix fun Quantity<AbstractVariableItem<*, *>>.ls(rhs: Quantity<LinearMonomial>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value ls rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.to(this.unit)!!.value ls rhs.value
        } else {
            this.value ls rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityVariableLeqQuantityLinearMonomial")
infix fun Quantity<AbstractVariableItem<*, *>>.leq(rhs: Quantity<LinearMonomial>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value leq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.to(this.unit)!!.value leq rhs.value
        } else {
            this.value leq rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityVariableEqQuantityLinearMonomial")
infix fun Quantity<AbstractVariableItem<*, *>>.eq(rhs: Quantity<LinearMonomial>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value eq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.to(this.unit)!!.value eq rhs.value
        } else {
            this.value eq rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityVariableNeqQuantityLinearMonomial")
infix fun Quantity<AbstractVariableItem<*, *>>.neq(rhs: Quantity<LinearMonomial>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value neq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.to(this.unit)!!.value neq rhs.value
        } else {
            this.value neq rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityVariableGrQuantityLinearMonomial")
infix fun Quantity<AbstractVariableItem<*, *>>.gr(rhs: Quantity<LinearMonomial>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value gr rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.to(this.unit)!!.value gr rhs.value
        } else {
            this.value gr rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityVariableGeqQuantityLinearMonomial")
infix fun Quantity<AbstractVariableItem<*, *>>.geq(rhs: Quantity<LinearMonomial>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value geq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.to(this.unit)!!.value geq rhs.value
        } else {
            this.value geq rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

// polynomial and variable

infix fun AbstractLinearPolynomial<*>.ls(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return LinearInequality(
        this.copy(),
        LinearPolynomial(rhs),
        Sign.Less
    )
}

infix fun AbstractLinearPolynomial<*>.leq(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return LinearInequality(
        this.copy(),
        LinearPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun AbstractLinearPolynomial<*>.eq(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return LinearInequality(
        this.copy(),
        LinearPolynomial(rhs),
        Sign.Equal
    )
}

infix fun AbstractLinearPolynomial<*>.neq(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return LinearInequality(
        this.copy(),
        LinearPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun AbstractLinearPolynomial<*>.gr(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return LinearInequality(
        this.copy(),
        LinearPolynomial(rhs),
        Sign.Greater
    )
}

infix fun AbstractLinearPolynomial<*>.geq(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return LinearInequality(
        this.copy(),
        LinearPolynomial(rhs),
        Sign.GreaterEqual
    )
}

infix fun AbstractVariableItem<*, *>.ls(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        rhs.copy(),
        Sign.Less
    )
}

infix fun AbstractVariableItem<*, *>.leq(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        rhs.copy(),
        Sign.LessEqual
    )
}

infix fun AbstractVariableItem<*, *>.eq(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        rhs.copy(),
        Sign.Equal
    )
}

infix fun AbstractVariableItem<*, *>.neq(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        rhs.copy(),
        Sign.Unequal
    )
}

infix fun AbstractVariableItem<*, *>.gr(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        rhs.copy(),
        Sign.Greater
    )
}

infix fun AbstractVariableItem<*, *>.geq(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        rhs.copy(),
        Sign.GreaterEqual
    )
}

// quantity polynomial and quantity variable

@JvmName("quantityLinearPolynomialLsQuantityVariable")
infix fun Quantity<AbstractLinearPolynomial<*>>.ls(rhs: Quantity<AbstractVariableItem<*, *>>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value ls rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.to(this.unit)!!.value ls rhs.value
        } else {
            this.value ls rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearPolynomialLeqQuantityVariable")
infix fun Quantity<AbstractLinearPolynomial<*>>.leq(rhs: Quantity<AbstractVariableItem<*, *>>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value leq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.to(this.unit)!!.value leq rhs.value
        } else {
            this.value leq rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearPolynomialEqQuantityVariable")
infix fun Quantity<AbstractLinearPolynomial<*>>.eq(rhs: Quantity<AbstractVariableItem<*, *>>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value eq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.to(this.unit)!!.value eq rhs.value
        } else {
            this.value eq rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearPolynomialNeqQuantityVariable")
infix fun Quantity<AbstractLinearPolynomial<*>>.neq(rhs: Quantity<AbstractVariableItem<*, *>>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value neq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.to(this.unit)!!.value neq rhs.value
        } else {
            this.value neq rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearPolynomialGrQuantityVariable")
infix fun Quantity<AbstractLinearPolynomial<*>>.gr(rhs: Quantity<AbstractVariableItem<*, *>>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value gr rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.to(this.unit)!!.value gr rhs.value
        } else {
            this.value gr rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearPolynomialGeqQuantityVariable")
infix fun Quantity<AbstractLinearPolynomial<*>>.geq(rhs: Quantity<AbstractVariableItem<*, *>>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value geq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.to(this.unit)!!.value geq rhs.value
        } else {
            this.value geq rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityVariableLsQuantityLinearPolynomial")
infix fun Quantity<AbstractVariableItem<*, *>>.ls(rhs: Quantity<AbstractLinearPolynomial<*>>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value ls rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.to(this.unit)!!.value ls rhs.value
        } else {
            this.value ls rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityVariableLeqQuantityLinearPolynomial")
infix fun Quantity<AbstractVariableItem<*, *>>.leq(rhs: Quantity<AbstractLinearPolynomial<*>>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value leq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.to(this.unit)!!.value leq rhs.value
        } else {
            this.value leq rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityVariableEqQuantityLinearPolynomial")
infix fun Quantity<AbstractVariableItem<*, *>>.eq(rhs: Quantity<AbstractLinearPolynomial<*>>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value eq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.to(this.unit)!!.value eq rhs.value
        } else {
            this.value eq rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityVariableNeqQuantityLinearPolynomial")
infix fun Quantity<AbstractVariableItem<*, *>>.neq(rhs: Quantity<AbstractLinearPolynomial<*>>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value neq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.to(this.unit)!!.value neq rhs.value
        } else {
            this.value neq rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityVariableGrQuantityLinearPolynomial")
infix fun Quantity<AbstractVariableItem<*, *>>.gr(rhs: Quantity<AbstractLinearPolynomial<*>>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value gr rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.to(this.unit)!!.value gr rhs.value
        } else {
            this.value gr rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityVariableGeqQuantityLinearPolynomial")
infix fun Quantity<AbstractVariableItem<*, *>>.geq(rhs: Quantity<AbstractLinearPolynomial<*>>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value geq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.to(this.unit)!!.value geq rhs.value
        } else {
            this.value geq rhs.to(this.unit)!!.value
        }
    } else {
        TODO("not implemented yet")
    }
}

// symbol and symbol

infix fun LinearIntermediateSymbol.ls(rhs: LinearIntermediateSymbol): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs),
        Sign.Less
    )
}

infix fun LinearIntermediateSymbol.leq(rhs: LinearIntermediateSymbol): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun LinearIntermediateSymbol.eq(rhs: LinearIntermediateSymbol): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs),
        Sign.Equal
    )
}

infix fun LinearIntermediateSymbol.neq(rhs: LinearIntermediateSymbol): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun LinearIntermediateSymbol.gr(rhs: LinearIntermediateSymbol): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs),
        Sign.Greater
    )
}

infix fun LinearIntermediateSymbol.geq(rhs: LinearIntermediateSymbol): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs),
        Sign.GreaterEqual
    )
}

// quantity symbol and quantity symbol

@JvmName("quantityLinearSymbolLsQuantityLinearSymbol")
infix fun Quantity<LinearIntermediateSymbol>.ls(rhs: Quantity<LinearIntermediateSymbol>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value ls rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value ls rhs.to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value ls rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearSymbolLeqQuantityLinearSymbol")
infix fun Quantity<LinearIntermediateSymbol>.leq(rhs: Quantity<LinearIntermediateSymbol>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value leq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value leq rhs.to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value leq rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearSymbolEqQuantityLinearSymbol")
infix fun Quantity<LinearIntermediateSymbol>.eq(rhs: Quantity<LinearIntermediateSymbol>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value eq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value eq rhs.to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value eq rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearSymbolNeqQuantityLinearSymbol")
infix fun Quantity<LinearIntermediateSymbol>.neq(rhs: Quantity<LinearIntermediateSymbol>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value neq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value neq rhs.to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value neq rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearSymbolGrQuantityLinearSymbol")
infix fun Quantity<LinearIntermediateSymbol>.gr(rhs: Quantity<LinearIntermediateSymbol>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value gr rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value gr rhs.to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value gr rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearSymbolGeqQuantityLinearSymbol")
infix fun Quantity<LinearIntermediateSymbol>.geq(rhs: Quantity<LinearIntermediateSymbol>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value geq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value geq rhs.to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value geq rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

// monomial and symbol

infix fun LinearMonomial.ls(rhs: LinearIntermediateSymbol): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.copy()),
        LinearPolynomial(rhs),
        Sign.Less
    )
}

infix fun LinearMonomial.leq(rhs: LinearIntermediateSymbol): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.copy()),
        LinearPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun LinearMonomial.eq(rhs: LinearIntermediateSymbol): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.copy()),
        LinearPolynomial(rhs),
        Sign.Equal
    )
}

infix fun LinearMonomial.neq(rhs: LinearIntermediateSymbol): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.copy()),
        LinearPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun LinearMonomial.gr(rhs: LinearIntermediateSymbol): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.copy()),
        LinearPolynomial(rhs),
        Sign.Greater
    )
}

infix fun LinearMonomial.geq(rhs: LinearIntermediateSymbol): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.copy()),
        LinearPolynomial(rhs),
        Sign.GreaterEqual
    )
}

infix fun LinearIntermediateSymbol.ls(rhs: LinearMonomial): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs.copy()),
        Sign.Less
    )
}

infix fun LinearIntermediateSymbol.leq(rhs: LinearMonomial): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs.copy()),
        Sign.LessEqual
    )
}

infix fun LinearIntermediateSymbol.eq(rhs: LinearMonomial): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs.copy()),
        Sign.Equal
    )
}

infix fun LinearIntermediateSymbol.neq(rhs: LinearMonomial): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs.copy()),
        Sign.Unequal
    )
}

infix fun LinearIntermediateSymbol.gr(rhs: LinearMonomial): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs.copy()),
        Sign.Greater
    )
}

infix fun LinearIntermediateSymbol.geq(rhs: LinearMonomial): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs.copy()),
        Sign.GreaterEqual
    )
}

// quantity monomial and quantity symbol

@JvmName("quantityLinearMonomialLsQuantityLinearSymbol")
infix fun Quantity<LinearMonomial>.ls(rhs: Quantity<LinearIntermediateSymbol>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value ls rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value ls rhs.to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value ls rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearMonomialLeqQuantityLinearSymbol")
infix fun Quantity<LinearMonomial>.leq(rhs: Quantity<LinearIntermediateSymbol>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value leq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value leq rhs.to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value leq rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearMonomialEqQuantityLinearSymbol")
infix fun Quantity<LinearMonomial>.eq(rhs: Quantity<LinearIntermediateSymbol>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value eq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value eq rhs.to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value eq rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearMonomialNeqQuantityLinearSymbol")
infix fun Quantity<LinearMonomial>.neq(rhs: Quantity<LinearIntermediateSymbol>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value neq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value neq rhs.to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value neq rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearMonomialGrQuantityLinearSymbol")
infix fun Quantity<LinearMonomial>.gr(rhs: Quantity<LinearIntermediateSymbol>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value gr rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value gr rhs.to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value gr rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearMonomialGeqQuantityLinearSymbol")
infix fun Quantity<LinearMonomial>.geq(rhs: Quantity<LinearIntermediateSymbol>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value geq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value geq rhs.to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value geq rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearSymbolLsQuantityLinearMonomial")
infix fun Quantity<LinearIntermediateSymbol>.ls(rhs: Quantity<LinearMonomial>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value ls rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value ls rhs.to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value ls rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearSymbolLeqQuantityLinearMonomial")
infix fun Quantity<LinearIntermediateSymbol>.leq(rhs: Quantity<LinearMonomial>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value leq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value leq rhs.to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value leq rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearSymbolEqQuantityLinearMonomial")
infix fun Quantity<LinearIntermediateSymbol>.eq(rhs: Quantity<LinearMonomial>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value eq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value eq rhs.to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value eq rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearSymbolNeqQuantityLinearMonomial")
infix fun Quantity<LinearIntermediateSymbol>.neq(rhs: Quantity<LinearMonomial>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value neq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value neq rhs.to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value neq rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearSymbolGrQuantityLinearMonomial")
infix fun Quantity<LinearIntermediateSymbol>.gr(rhs: Quantity<LinearMonomial>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value gr rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value gr rhs.to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value gr rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearSymbolGeqQuantityLinearMonomial")
infix fun Quantity<LinearIntermediateSymbol>.geq(rhs: Quantity<LinearMonomial>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value geq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value geq rhs.to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value geq rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

// polynomial and symbol

infix fun AbstractLinearPolynomial<*>.ls(rhs: LinearIntermediateSymbol): LinearInequality {
    return LinearInequality(
        this.copy(),
        LinearPolynomial(rhs),
        Sign.Less
    )
}

infix fun AbstractLinearPolynomial<*>.leq(rhs: LinearIntermediateSymbol): LinearInequality {
    return LinearInequality(
        this.copy(),
        LinearPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun AbstractLinearPolynomial<*>.eq(rhs: LinearIntermediateSymbol): LinearInequality {
    return LinearInequality(
        this.copy(),
        LinearPolynomial(rhs),
        Sign.Equal
    )
}

infix fun AbstractLinearPolynomial<*>.neq(rhs: LinearIntermediateSymbol): LinearInequality {
    return LinearInequality(
        this.copy(),
        LinearPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun AbstractLinearPolynomial<*>.gr(rhs: LinearIntermediateSymbol): LinearInequality {
    return LinearInequality(
        this.copy(),
        LinearPolynomial(rhs),
        Sign.Greater
    )
}

infix fun AbstractLinearPolynomial<*>.geq(rhs: LinearIntermediateSymbol): LinearInequality {
    return LinearInequality(
        this.copy(),
        LinearPolynomial(rhs),
        Sign.GreaterEqual
    )
}

infix fun LinearIntermediateSymbol.ls(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        rhs.copy(),
        Sign.Less
    )
}

infix fun LinearIntermediateSymbol.leq(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        rhs.copy(),
        Sign.LessEqual
    )
}

infix fun LinearIntermediateSymbol.eq(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        rhs.copy(),
        Sign.Equal
    )
}

infix fun LinearIntermediateSymbol.neq(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        rhs.copy(),
        Sign.Unequal
    )
}

infix fun LinearIntermediateSymbol.gr(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        rhs.copy(),
        Sign.Greater
    )
}

infix fun LinearIntermediateSymbol.geq(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        rhs.copy(),
        Sign.GreaterEqual
    )
}

// quantity polynomial and quantity symbol

@JvmName("quantityLinearPolynomialLsQuantityLinearSymbol")
infix fun Quantity<AbstractLinearPolynomial<*>>.ls(rhs: Quantity<LinearIntermediateSymbol>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value ls rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value ls rhs.to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value ls rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearPolynomialLeqQuantityLinearSymbol")
infix fun Quantity<AbstractLinearPolynomial<*>>.leq(rhs: Quantity<LinearIntermediateSymbol>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value leq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value leq rhs.to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value leq rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearPolynomialEqQuantityLinearSymbol")
infix fun Quantity<AbstractLinearPolynomial<*>>.eq(rhs: Quantity<LinearIntermediateSymbol>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value eq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value eq rhs.to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value eq rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearPolynomialNeqQuantityLinearSymbol")
infix fun Quantity<AbstractLinearPolynomial<*>>.neq(rhs: Quantity<LinearIntermediateSymbol>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value neq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value neq rhs.to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value neq rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearPolynomialGrQuantityLinearSymbol")
infix fun Quantity<AbstractLinearPolynomial<*>>.gr(rhs: Quantity<LinearIntermediateSymbol>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value gr rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value gr rhs.to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value gr rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearPolynomialGeqQuantityLinearSymbol")
infix fun Quantity<AbstractLinearPolynomial<*>>.geq(rhs: Quantity<LinearIntermediateSymbol>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value geq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value geq rhs.to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value geq rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearSymbolLsQuantityLinearPolynomial")
infix fun Quantity<LinearIntermediateSymbol>.ls(rhs: Quantity<AbstractLinearPolynomial<*>>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value ls rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value ls rhs.to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value ls rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearSymbolLeqQuantityLinearPolynomial")
infix fun Quantity<LinearIntermediateSymbol>.leq(rhs: Quantity<AbstractLinearPolynomial<*>>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value leq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value leq rhs.to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value leq rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearSymbolEqQuantityLinearPolynomial")
infix fun Quantity<LinearIntermediateSymbol>.eq(rhs: Quantity<AbstractLinearPolynomial<*>>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value eq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value eq rhs.to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value eq rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearSymbolNeqQuantityLinearPolynomial")
infix fun Quantity<LinearIntermediateSymbol>.neq(rhs: Quantity<AbstractLinearPolynomial<*>>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value neq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value neq rhs.to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value neq rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearSymbolGrQuantityLinearPolynomial")
infix fun Quantity<LinearIntermediateSymbol>.gr(rhs: Quantity<AbstractLinearPolynomial<*>>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value gr rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value gr rhs.to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value gr rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearSymbolGeqQuantityLinearPolynomial")
infix fun Quantity<LinearIntermediateSymbol>.geq(rhs: Quantity<AbstractLinearPolynomial<*>>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value geq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value geq rhs.to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value geq rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

// monomial and monomial

infix fun LinearMonomial.ls(rhs: LinearMonomial): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.copy()),
        LinearPolynomial(rhs.copy()),
        Sign.Less
    )
}

infix fun LinearMonomial.leq(rhs: LinearMonomial): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.copy()),
        LinearPolynomial(rhs.copy()),
        Sign.LessEqual
    )
}

infix fun LinearMonomial.eq(rhs: LinearMonomial): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.copy()),
        LinearPolynomial(rhs.copy()),
        Sign.Equal
    )
}

infix fun LinearMonomial.neq(rhs: LinearMonomial): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.copy()),
        LinearPolynomial(rhs.copy()),
        Sign.Unequal
    )
}

infix fun LinearMonomial.gr(rhs: LinearMonomial): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.copy()),
        LinearPolynomial(rhs.copy()),
        Sign.Greater
    )
}

infix fun LinearMonomial.geq(rhs: LinearMonomial): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.copy()),
        LinearPolynomial(rhs.copy()),
        Sign.GreaterEqual
    )
}

// quantity monomial and quantity monomial

@JvmName("quantityLinearMonomialLsQuantityLinearMonomial")
infix fun Quantity<LinearMonomial>.ls(rhs: Quantity<LinearMonomial>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value ls rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value ls rhs.to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value ls rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearMonomialLeqQuantityLinearMonomial")
infix fun Quantity<LinearMonomial>.leq(rhs: Quantity<LinearMonomial>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value leq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value leq rhs.to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value leq rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearMonomialEqQuantityLinearMonomial")
infix fun Quantity<LinearMonomial>.eq(rhs: Quantity<LinearMonomial>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value eq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value eq rhs.to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value eq rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearMonomialNeqQuantityLinearMonomial")
infix fun Quantity<LinearMonomial>.neq(rhs: Quantity<LinearMonomial>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value neq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value neq rhs.to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value neq rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearMonomialGrQuantityLinearMonomial")
infix fun Quantity<LinearMonomial>.gr(rhs: Quantity<LinearMonomial>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value gr rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value gr rhs.to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value gr rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearMonomialGeqQuantityLinearMonomial")
infix fun Quantity<LinearMonomial>.geq(rhs: Quantity<LinearMonomial>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value geq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value geq rhs.to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value geq rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

// polynomial and monomial

infix fun AbstractLinearPolynomial<*>.ls(rhs: LinearMonomial): LinearInequality {
    return LinearInequality(
        this.copy(),
        LinearPolynomial(rhs.copy()),
        Sign.Less
    )
}

infix fun AbstractLinearPolynomial<*>.leq(rhs: LinearMonomial): LinearInequality {
    return LinearInequality(
        this.copy(),
        LinearPolynomial(rhs.copy()),
        Sign.LessEqual
    )
}

infix fun AbstractLinearPolynomial<*>.eq(rhs: LinearMonomial): LinearInequality {
    return LinearInequality(
        this.copy(),
        LinearPolynomial(rhs.copy()),
        Sign.Equal
    )
}

infix fun AbstractLinearPolynomial<*>.neq(rhs: LinearMonomial): LinearInequality {
    return LinearInequality(
        this.copy(),
        LinearPolynomial(rhs.copy()),
        Sign.Unequal
    )
}

infix fun AbstractLinearPolynomial<*>.gr(rhs: LinearMonomial): LinearInequality {
    return LinearInequality(
        this.copy(),
        LinearPolynomial(rhs.copy()),
        Sign.Greater
    )
}

infix fun AbstractLinearPolynomial<*>.geq(rhs: LinearMonomial): LinearInequality {
    return LinearInequality(
        this.copy(),
        LinearPolynomial(rhs.copy()),
        Sign.GreaterEqual
    )
}

infix fun LinearMonomial.ls(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.copy()),
        rhs.copy(),
        Sign.Less
    )
}

infix fun LinearMonomial.leq(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.copy()),
        rhs.copy(),
        Sign.LessEqual
    )
}

infix fun LinearMonomial.eq(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.copy()),
        rhs.copy(),
        Sign.Equal
    )
}

infix fun LinearMonomial.neq(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.copy()),
        rhs.copy(),
        Sign.Unequal
    )
}

infix fun LinearMonomial.gr(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.copy()),
        rhs.copy(),
        Sign.Greater
    )
}

infix fun LinearMonomial.geq(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.copy()),
        rhs.copy(),
        Sign.GreaterEqual
    )
}

// quantity polynomial and quantity monomial

@JvmName("quantityLinearPolynomialLsQuantityLinearMonomial")
infix fun Quantity<AbstractLinearPolynomial<*>>.ls(rhs: Quantity<LinearMonomial>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value ls rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value ls rhs.to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value ls rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearPolynomialLeqQuantityLinearMonomial")
infix fun Quantity<AbstractLinearPolynomial<*>>.leq(rhs: Quantity<LinearMonomial>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value leq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value leq rhs.to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value leq rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearPolynomialEqQuantityLinearMonomial")
infix fun Quantity<AbstractLinearPolynomial<*>>.eq(rhs: Quantity<LinearMonomial>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value eq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value eq rhs.to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value eq rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearPolynomialNeqQuantityLinearMonomial")
infix fun Quantity<AbstractLinearPolynomial<*>>.neq(rhs: Quantity<LinearMonomial>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value neq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value neq rhs.to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value neq rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearPolynomialGrQuantityLinearMonomial")
infix fun Quantity<AbstractLinearPolynomial<*>>.gr(rhs: Quantity<LinearMonomial>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value gr rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value gr rhs.to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value gr rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearPolynomialGeqQuantityLinearMonomial")
infix fun Quantity<AbstractLinearPolynomial<*>>.geq(rhs: Quantity<LinearMonomial>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value geq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value geq rhs.to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value geq rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearMonomialLsQuantityLinearPolynomial")
infix fun Quantity<LinearMonomial>.ls(rhs: Quantity<AbstractLinearPolynomial<*>>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value ls rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value ls rhs.to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value ls rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearMonomialLeqQuantityLinearPolynomial")
infix fun Quantity<LinearMonomial>.leq(rhs: Quantity<AbstractLinearPolynomial<*>>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value leq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value leq rhs.to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value leq rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearMonomialEqQuantityLinearPolynomial")
infix fun Quantity<LinearMonomial>.eq(rhs: Quantity<AbstractLinearPolynomial<*>>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value eq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value eq rhs.to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value eq rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearMonomialNeqQuantityLinearPolynomial")
infix fun Quantity<LinearMonomial>.neq(rhs: Quantity<AbstractLinearPolynomial<*>>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value neq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value neq rhs.to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value neq rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearMonomialGrQuantityLinearPolynomial")
infix fun Quantity<LinearMonomial>.gr(rhs: Quantity<AbstractLinearPolynomial<*>>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value gr rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value gr rhs.to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value gr rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearMonomialGeqQuantityLinearPolynomial")
infix fun Quantity<LinearMonomial>.geq(rhs: Quantity<AbstractLinearPolynomial<*>>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value geq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value geq rhs.to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value geq rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

// polynomial and polynomial

infix fun AbstractLinearPolynomial<*>.ls(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return LinearInequality(
        this.copy(),
        rhs.copy(),
        Sign.Less
    )
}

infix fun AbstractLinearPolynomial<*>.leq(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return LinearInequality(
        this.copy(),
        rhs.copy(),
        Sign.LessEqual
    )
}

infix fun AbstractLinearPolynomial<*>.eq(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return LinearInequality(
        this.copy(),
        rhs.copy(),
        Sign.Equal
    )
}

infix fun AbstractLinearPolynomial<*>.neq(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return LinearInequality(
        this.copy(),
        rhs.copy(),
        Sign.Unequal
    )
}

infix fun AbstractLinearPolynomial<*>.gr(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return LinearInequality(
        this.copy(),
        rhs.copy(),
        Sign.Greater
    )
}

infix fun AbstractLinearPolynomial<*>.geq(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return LinearInequality(
        this.copy(),
        rhs.copy(),
        Sign.GreaterEqual
    )
}

// quantity polynomial and quantity polynomial

@JvmName("quantityLinearPolynomialLsQuantityLinearPolynomial")
infix fun Quantity<AbstractLinearPolynomial<*>>.ls(rhs: Quantity<AbstractLinearPolynomial<*>>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value ls rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value ls rhs.to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value ls rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearPolynomialLeqQuantityLinearPolynomial")
infix fun Quantity<AbstractLinearPolynomial<*>>.leq(rhs: Quantity<AbstractLinearPolynomial<*>>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value leq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value leq rhs.to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value leq rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearPolynomialEqQuantityLinearPolynomial")
infix fun Quantity<AbstractLinearPolynomial<*>>.eq(rhs: Quantity<AbstractLinearPolynomial<*>>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value eq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value eq rhs.to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value eq rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearPolynomialNeqQuantityLinearPolynomial")
infix fun Quantity<AbstractLinearPolynomial<*>>.neq(rhs: Quantity<AbstractLinearPolynomial<*>>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value neq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value neq rhs.to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value neq rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearPolynomialGrQuantityLinearPolynomial")
infix fun Quantity<AbstractLinearPolynomial<*>>.gr(rhs: Quantity<AbstractLinearPolynomial<*>>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value gr rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value gr rhs.to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value gr rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLinearPolynomialGeqQuantityLinearPolynomial")
infix fun Quantity<AbstractLinearPolynomial<*>>.geq(rhs: Quantity<AbstractLinearPolynomial<*>>): LinearInequality {
    return if (this.unit == rhs.unit) {
        this.value geq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value geq rhs.to(this.unit)!!.value
        } else {
            this.to(rhs.unit)!!.value geq rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}
