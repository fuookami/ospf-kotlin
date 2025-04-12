package fuookami.ospf.kotlin.core.frontend.inequality

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.physics.quantity.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*

class QuadraticInequality(
    override val lhs: AbstractQuadraticPolynomial<*>,
    override val rhs: AbstractQuadraticPolynomial<*>,
    sign: Sign,
    name: String = "",
    displayName: String? = null
) : Inequality<QuadraticInequality, QuadraticMonomialCell>(lhs, rhs, sign, name, displayName) {
    companion object {
        operator fun invoke(inequality: LinearInequality): QuadraticInequality {
            return QuadraticInequality(
                QuadraticPolynomial(inequality.lhs),
                QuadraticPolynomial(inequality.rhs),
                inequality.sign,
                inequality.name,
                inequality.displayName
            )
        }
    }

    override val cells: List<QuadraticMonomialCell>
        get() {
            if (_cells.isEmpty()) {
                val cells = HashMap<Pair<VariableItemKey, VariableItemKey?>, QuadraticMonomialCell>()
                var constant = Flt64.zero
                for (cell in lhs.cells) {
                    when (val symbol = cell.cell) {
                        is Either.Left -> {
                            val key = Pair(symbol.value.variable1.key, symbol.value.variable2?.key)
                            cells[key] = cells[key]?.let { it + cell } ?: cell
                        }

                        is Either.Right -> {
                            constant += symbol.value
                        }
                    }
                }
                for (cell in rhs.cells) {
                    when (val symbol = cell.cell) {
                        is Either.Left -> {
                            val key = Pair(symbol.value.variable1.key, symbol.value.variable2?.key)
                            cells[key] = cells[key]?.let { it - cell } ?: -cell
                        }

                        is Either.Right -> {
                            constant -= symbol.value
                        }
                    }
                }
                val ret = cells.map { it.value }.toMutableList()
                ret.add(QuadraticMonomialCell(constant))
                _cells = ret
            }
            return _cells
        }

    override fun reverse(name: String?, displayName: String?): QuadraticInequality {
        return QuadraticInequality(
            lhs = QuadraticPolynomial(lhs.monomials.map { it.copy() }),
            rhs = QuadraticPolynomial(rhs.monomials.map { it.copy() }),
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

    override fun normalize(): QuadraticInequality {
        return QuadraticInequality(
            lhs = QuadraticPolynomial(lhs.monomials.map { it.copy() } + rhs.monomials.map { -it }),
            rhs = QuadraticPolynomial(-lhs.constant + rhs.constant),
            sign = sign,
            name = name,
            displayName = displayName
        )
    }
}

// symbol and constant

infix fun QuadraticIntermediateSymbol.ls(rhs: Boolean): QuadraticInequality {
    return if (rhs) {
        this.ls(1)
    } else {
        this.ls(0)
    }
}

infix fun QuadraticIntermediateSymbol.ls(rhs: Trivalent): QuadraticInequality {
    return this.ls(rhs.value)
}

infix fun QuadraticIntermediateSymbol.ls(rhs: BalancedTrivalent): QuadraticInequality {
    return this.ls(rhs.value)
}

infix fun QuadraticIntermediateSymbol.ls(rhs: Int): QuadraticInequality {
    return this.ls(Flt64(rhs))
}

infix fun QuadraticIntermediateSymbol.ls(rhs: Double): QuadraticInequality {
    return this.ls(Flt64(rhs))
}

infix fun <T : RealNumber<T>> QuadraticIntermediateSymbol.ls(rhs: T): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.toFlt64()),
        Sign.Less
    )
}

infix fun QuadraticIntermediateSymbol.leq(rhs: Boolean): QuadraticInequality {
    return if (rhs) {
        this.leq(1)
    } else {
        this.leq(0)
    }
}

infix fun QuadraticIntermediateSymbol.leq(rhs: Trivalent): QuadraticInequality {
    return this.leq(rhs.value)
}

infix fun QuadraticIntermediateSymbol.leq(rhs: BalancedTrivalent): QuadraticInequality {
    return this.leq(rhs.value)
}

infix fun QuadraticIntermediateSymbol.leq(rhs: Int): QuadraticInequality {
    return this.leq(Flt64(rhs))
}

infix fun QuadraticIntermediateSymbol.leq(rhs: Double): QuadraticInequality {
    return this.leq(Flt64(rhs))
}

infix fun <T : RealNumber<T>> QuadraticIntermediateSymbol.leq(rhs: T): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.toFlt64()),
        Sign.LessEqual
    )
}

infix fun QuadraticIntermediateSymbol.eq(rhs: Boolean): QuadraticInequality {
    return if (rhs) {
        this.eq(1)
    } else {
        this.eq(0)
    }
}

infix fun QuadraticIntermediateSymbol.eq(rhs: Trivalent): QuadraticInequality {
    return this.eq(rhs.value)
}

infix fun QuadraticIntermediateSymbol.eq(rhs: BalancedTrivalent): QuadraticInequality {
    return this.eq(rhs.value)
}

infix fun QuadraticIntermediateSymbol.eq(rhs: Int): QuadraticInequality {
    return this.eq(Flt64(rhs))
}

infix fun QuadraticIntermediateSymbol.eq(rhs: Double): QuadraticInequality {
    return this.eq(Flt64(rhs))
}

infix fun <T : RealNumber<T>> QuadraticIntermediateSymbol.eq(rhs: T): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.toFlt64()),
        Sign.Equal
    )
}

infix fun QuadraticIntermediateSymbol.neq(rhs: Boolean): QuadraticInequality {
    return if (rhs) {
        this.neq(1)
    } else {
        this.neq(0)
    }
}

infix fun QuadraticIntermediateSymbol.neq(rhs: Trivalent): QuadraticInequality {
    return this.neq(rhs.value)
}

infix fun QuadraticIntermediateSymbol.neq(rhs: BalancedTrivalent): QuadraticInequality {
    return this.neq(rhs.value)
}

infix fun QuadraticIntermediateSymbol.neq(rhs: Int): QuadraticInequality {
    return this.neq(Flt64(rhs))
}

infix fun QuadraticIntermediateSymbol.neq(rhs: Double): QuadraticInequality {
    return this.neq(Flt64(rhs))
}

infix fun <T : RealNumber<T>> QuadraticIntermediateSymbol.neq(rhs: T): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.toFlt64()),
        Sign.Unequal
    )
}

infix fun QuadraticIntermediateSymbol.gr(rhs: Boolean): QuadraticInequality {
    return if (rhs) {
        this.gr(1)
    } else {
        this.gr(0)
    }
}

infix fun QuadraticIntermediateSymbol.gr(rhs: Trivalent): QuadraticInequality {
    return this.gr(rhs.value)
}

infix fun QuadraticIntermediateSymbol.gr(rhs: BalancedTrivalent): QuadraticInequality {
    return this.gr(rhs.value)
}

infix fun QuadraticIntermediateSymbol.gr(rhs: Int): QuadraticInequality {
    return this.gr(Flt64(rhs))
}

infix fun QuadraticIntermediateSymbol.gr(rhs: Double): QuadraticInequality {
    return this.gr(Flt64(rhs))
}

infix fun <T : RealNumber<T>> QuadraticIntermediateSymbol.gr(rhs: T): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.toFlt64()),
        Sign.Greater
    )
}

infix fun QuadraticIntermediateSymbol.geq(rhs: Boolean): QuadraticInequality {
    return if (rhs) {
        this.geq(1)
    } else {
        this.geq(0)
    }
}

infix fun QuadraticIntermediateSymbol.geq(rhs: Trivalent): QuadraticInequality {
    return this.geq(rhs.value)
}

infix fun QuadraticIntermediateSymbol.geq(rhs: BalancedTrivalent): QuadraticInequality {
    return this.geq(rhs.value)
}

infix fun QuadraticIntermediateSymbol.geq(rhs: Int): QuadraticInequality {
    return this.geq(Flt64(rhs))
}

infix fun QuadraticIntermediateSymbol.geq(rhs: Double): QuadraticInequality {
    return this.geq(Flt64(rhs))
}

infix fun <T : RealNumber<T>> QuadraticIntermediateSymbol.geq(rhs: T): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.toFlt64()),
        Sign.GreaterEqual
    )
}

infix fun Boolean.ls(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return if (this) {
        1.ls(rhs)
    } else {
        0.ls(rhs)
    }
}

infix fun Trivalent.ls(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return this.value.ls(rhs)
}

infix fun BalancedTrivalent.ls(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return this.value.ls(rhs)
}

infix fun Int.ls(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return Flt64(this).ls(rhs)
}

infix fun Double.ls(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return Flt64(this).ls(rhs)
}

infix fun <T : RealNumber<T>> T.ls(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.toFlt64()),
        QuadraticPolynomial(rhs),
        Sign.Less
    )
}

infix fun Boolean.leq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return if (this) {
        1.leq(rhs)
    } else {
        0.leq(rhs)
    }
}

infix fun Trivalent.leq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return this.value.leq(rhs)
}

infix fun BalancedTrivalent.leq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return this.value.leq(rhs)
}

infix fun Int.leq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return Flt64(this).leq(rhs)
}

infix fun Double.leq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return Flt64(this).leq(rhs)
}

infix fun <T : RealNumber<T>> T.leq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.toFlt64()),
        QuadraticPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun Boolean.eq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return if (this) {
        1.eq(rhs)
    } else {
        0.eq(rhs)
    }
}

infix fun Trivalent.eq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return this.value.eq(rhs)
}

infix fun BalancedTrivalent.eq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return this.value.eq(rhs)
}

infix fun Int.eq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return Flt64(this).eq(rhs)
}

infix fun Double.eq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return Flt64(this).eq(rhs)
}

infix fun <T : RealNumber<T>> T.eq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.toFlt64()),
        QuadraticPolynomial(rhs),
        Sign.Equal
    )
}

infix fun Boolean.neq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return if (this) {
        1.neq(rhs)
    } else {
        0.neq(rhs)
    }
}

infix fun Trivalent.neq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return this.value.neq(rhs)
}

infix fun BalancedTrivalent.neq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return this.value.neq(rhs)
}

infix fun Int.neq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return Flt64(this).neq(rhs)
}

infix fun Double.neq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return Flt64(this).neq(rhs)
}

infix fun <T : RealNumber<T>> T.neq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.toFlt64()),
        QuadraticPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun Boolean.gr(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return if (this) {
        1.gr(rhs)
    } else {
        0.gr(rhs)
    }
}

infix fun Trivalent.gr(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return this.value.gr(rhs)
}

infix fun BalancedTrivalent.gr(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return this.value.gr(rhs)
}

infix fun Int.gr(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return Flt64(this).gr(rhs)
}

infix fun Double.gr(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return Flt64(this).gr(rhs)
}

infix fun <T : RealNumber<T>> T.gr(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.toFlt64()),
        QuadraticPolynomial(rhs),
        Sign.Greater
    )
}

infix fun Boolean.geq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return if (this) {
        1.geq(rhs)
    } else {
        0.geq(rhs)
    }
}

infix fun Trivalent.geq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return this.value.geq(rhs)
}

infix fun BalancedTrivalent.geq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return this.value.geq(rhs)
}

infix fun Int.geq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return Flt64(this).geq(rhs)
}

infix fun Double.geq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return Flt64(this).geq(rhs)
}

infix fun <T : RealNumber<T>> T.geq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.toFlt64()),
        QuadraticPolynomial(rhs),
        Sign.GreaterEqual
    )
}

// quantity symbol and quantity

@JvmName("quantityQuadraticSymbolLsQuantity")
infix fun <T : RealNumber<T>> Quantity<QuadraticIntermediateSymbol>.ls(rhs: Quantity<T>): QuadraticInequality {
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

@JvmName("quantityQuadraticSymbolLeqQuantity")
infix fun <T : RealNumber<T>> Quantity<QuadraticIntermediateSymbol>.leq(rhs: Quantity<T>): QuadraticInequality {
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

@JvmName("quantityQuadraticSymbolEqQuantity")
infix fun <T : RealNumber<T>> Quantity<QuadraticIntermediateSymbol>.eq(rhs: Quantity<T>): QuadraticInequality {
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

@JvmName("quantityQuadraticSymbolNeqQuantity")
infix fun <T : RealNumber<T>> Quantity<QuadraticIntermediateSymbol>.neq(rhs: Quantity<T>): QuadraticInequality {
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

@JvmName("quantityQuadraticSymbolGrQuantity")
infix fun <T : RealNumber<T>> Quantity<QuadraticIntermediateSymbol>.gr(rhs: Quantity<T>): QuadraticInequality {
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

@JvmName("quantityQuadraticSymbolGeqQuantity")
infix fun <T : RealNumber<T>> Quantity<QuadraticIntermediateSymbol>.geq(rhs: Quantity<T>): QuadraticInequality {
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

@JvmName("quantityLsQuadraticSymbolQuantity")
infix fun <T : RealNumber<T>> Quantity<T>.ls(rhs: Quantity<QuadraticIntermediateSymbol>): QuadraticInequality {
    return if (this.unit == rhs.unit) {
        this.value ls rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value ls rhs.to(this.unit)!!.value
        } else {
            this.toFlt64().to(this.unit)!!.value ls rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityLeqQuadraticSymbolQuantity")
infix fun <T : RealNumber<T>> Quantity<T>.leq(rhs: Quantity<QuadraticIntermediateSymbol>): QuadraticInequality {
    return if (this.unit == rhs.unit) {
        this.value leq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value leq rhs.to(this.unit)!!.value
        } else {
            this.toFlt64().to(this.unit)!!.value leq rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityEqQuadraticSymbolQuantity")
infix fun <T : RealNumber<T>> Quantity<T>.eq(rhs: Quantity<QuadraticIntermediateSymbol>): QuadraticInequality {
    return if (this.unit == rhs.unit) {
        this.value eq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value eq rhs.to(this.unit)!!.value
        } else {
            this.toFlt64().to(this.unit)!!.value eq rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityNeqQuadraticSymbolQuantity")
infix fun <T : RealNumber<T>> Quantity<T>.neq(rhs: Quantity<QuadraticIntermediateSymbol>): QuadraticInequality {
    return if (this.unit == rhs.unit) {
        this.value neq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value neq rhs.to(this.unit)!!.value
        } else {
            this.toFlt64().to(this.unit)!!.value neq rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityGrQuadraticSymbolQuantity")
infix fun <T : RealNumber<T>> Quantity<T>.gr(rhs: Quantity<QuadraticIntermediateSymbol>): QuadraticInequality {
    return if (this.unit == rhs.unit) {
        this.value gr rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value gr rhs.to(this.unit)!!.value
        } else {
            this.toFlt64().to(this.unit)!!.value gr rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("quantityGeqQuadraticSymbolQuantity")
infix fun <T : RealNumber<T>> Quantity<T>.geq(rhs: Quantity<QuadraticIntermediateSymbol>): QuadraticInequality {
    return if (this.unit == rhs.unit) {
        this.value geq rhs.value
    } else if (this.unit.quantity == rhs.unit.quantity) {
        if (this.unit.scale.value leq rhs.unit.scale.value) {
            this.value geq rhs.to(this.unit)!!.value
        } else {
            this.toFlt64().to(this.unit)!!.value geq rhs.value
        }
    } else {
        TODO("not implemented yet")
    }
}

// monomial and constant

infix fun QuadraticMonomial.ls(rhs: Boolean): QuadraticInequality {
    return if (rhs) {
        this.ls(1)
    } else {
        this.ls(0)
    }
}

infix fun QuadraticMonomial.ls(rhs: Trivalent): QuadraticInequality {
    return this.ls(rhs.value)
}

infix fun QuadraticMonomial.ls(rhs: BalancedTrivalent): QuadraticInequality {
    return this.ls(rhs.value)
}

infix fun QuadraticMonomial.ls(rhs: Int): QuadraticInequality {
    return this.ls(Flt64(rhs))
}

infix fun QuadraticMonomial.ls(rhs: Double): QuadraticInequality {
    return this.ls(Flt64(rhs))
}

infix fun <T : RealNumber<T>> QuadraticMonomial.ls(rhs: T): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs.toFlt64()),
        Sign.Less
    )
}

infix fun QuadraticMonomial.leq(rhs: Boolean): QuadraticInequality {
    return if (rhs) {
        this.leq(1)
    } else {
        this.leq(0)
    }
}

infix fun QuadraticMonomial.leq(rhs: Trivalent): QuadraticInequality {
    return this.leq(rhs.value)
}

infix fun QuadraticMonomial.leq(rhs: BalancedTrivalent): QuadraticInequality {
    return this.leq(rhs.value)
}

infix fun QuadraticMonomial.leq(rhs: Int): QuadraticInequality {
    return this.leq(Flt64(rhs))
}

infix fun QuadraticMonomial.leq(rhs: Double): QuadraticInequality {
    return this.leq(Flt64(rhs))
}

infix fun <T : RealNumber<T>> QuadraticMonomial.leq(rhs: T): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs.toFlt64()),
        Sign.LessEqual
    )
}

infix fun QuadraticMonomial.eq(rhs: Boolean): QuadraticInequality {
    return if (rhs) {
        this.eq(1)
    } else {
        this.eq(0)
    }
}

infix fun QuadraticMonomial.eq(rhs: Trivalent): QuadraticInequality {
    return this.eq(rhs.value)
}

infix fun QuadraticMonomial.eq(rhs: BalancedTrivalent): QuadraticInequality {
    return this.eq(rhs.value)
}

infix fun QuadraticMonomial.eq(rhs: Int): QuadraticInequality {
    return this.eq(Flt64(rhs))
}

infix fun QuadraticMonomial.eq(rhs: Double): QuadraticInequality {
    return this.eq(Flt64(rhs))
}

infix fun <T : RealNumber<T>> QuadraticMonomial.eq(rhs: T): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs.toFlt64()),
        Sign.Equal
    )
}

infix fun QuadraticMonomial.neq(rhs: Boolean): QuadraticInequality {
    return if (rhs) {
        this.neq(1)
    } else {
        this.neq(0)
    }
}

infix fun QuadraticMonomial.neq(rhs: Trivalent): QuadraticInequality {
    return this.neq(rhs.value)
}

infix fun QuadraticMonomial.neq(rhs: BalancedTrivalent): QuadraticInequality {
    return this.neq(rhs.value)
}

infix fun QuadraticMonomial.neq(rhs: Int): QuadraticInequality {
    return this.neq(Flt64(rhs))
}

infix fun QuadraticMonomial.neq(rhs: Double): QuadraticInequality {
    return this.neq(Flt64(rhs))
}

infix fun <T : RealNumber<T>> QuadraticMonomial.neq(rhs: T): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs.toFlt64()),
        Sign.Unequal
    )
}

infix fun QuadraticMonomial.gr(rhs: Boolean): QuadraticInequality {
    return if (rhs) {
        this.gr(1)
    } else {
        this.gr(0)
    }
}

infix fun QuadraticMonomial.gr(rhs: Trivalent): QuadraticInequality {
    return this.gr(rhs.value)
}

infix fun QuadraticMonomial.gr(rhs: BalancedTrivalent): QuadraticInequality {
    return this.gr(rhs.value)
}

infix fun QuadraticMonomial.gr(rhs: Int): QuadraticInequality {
    return this.gr(Flt64(rhs))
}

infix fun QuadraticMonomial.gr(rhs: Double): QuadraticInequality {
    return this.gr(Flt64(rhs))
}

infix fun <T : RealNumber<T>> QuadraticMonomial.gr(rhs: T): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs.toFlt64()),
        Sign.Greater
    )
}

infix fun QuadraticMonomial.geq(rhs: Boolean): QuadraticInequality {
    return if (rhs) {
        this.geq(1)
    } else {
        this.geq(0)
    }
}

infix fun QuadraticMonomial.geq(rhs: Trivalent): QuadraticInequality {
    return this.geq(rhs.value)
}

infix fun QuadraticMonomial.geq(rhs: BalancedTrivalent): QuadraticInequality {
    return this.geq(rhs.value)
}

infix fun QuadraticMonomial.geq(rhs: Int): QuadraticInequality {
    return this.geq(Flt64(rhs))
}

infix fun QuadraticMonomial.geq(rhs: Double): QuadraticInequality {
    return this.geq(Flt64(rhs))
}

infix fun <T : RealNumber<T>> QuadraticMonomial.geq(rhs: T): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs.toFlt64()),
        Sign.GreaterEqual
    )
}

infix fun Boolean.ls(rhs: QuadraticMonomial): QuadraticInequality {
    return if (this) {
        1.ls(rhs)
    } else {
        0.ls(rhs)
    }
}

infix fun Trivalent.ls(rhs: QuadraticMonomial): QuadraticInequality {
    return this.value.ls(rhs)
}

infix fun BalancedTrivalent.ls(rhs: QuadraticMonomial): QuadraticInequality {
    return this.value.ls(rhs)
}

infix fun Int.ls(rhs: QuadraticMonomial): QuadraticInequality {
    return Flt64(this).ls(rhs)
}

infix fun Double.ls(rhs: QuadraticMonomial): QuadraticInequality {
    return Flt64(this).ls(rhs)
}

infix fun <T : RealNumber<T>> T.ls(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.toFlt64()),
        QuadraticPolynomial(rhs.copy()),
        Sign.Less
    )
}

infix fun Boolean.leq(rhs: QuadraticMonomial): QuadraticInequality {
    return if (this) {
        1.leq(rhs)
    } else {
        0.leq(rhs)
    }
}

infix fun Trivalent.leq(rhs: QuadraticMonomial): QuadraticInequality {
    return this.value.leq(rhs)
}

infix fun BalancedTrivalent.leq(rhs: QuadraticMonomial): QuadraticInequality {
    return this.value.leq(rhs)
}

infix fun Int.leq(rhs: QuadraticMonomial): QuadraticInequality {
    return Flt64(this).leq(rhs)
}

infix fun Double.leq(rhs: QuadraticMonomial): QuadraticInequality {
    return Flt64(this).leq(rhs)
}

infix fun <T : RealNumber<T>> T.leq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.toFlt64()),
        QuadraticPolynomial(rhs.copy()),
        Sign.LessEqual
    )
}

infix fun Boolean.eq(rhs: QuadraticMonomial): QuadraticInequality {
    return if (this) {
        1.eq(rhs)
    } else {
        0.eq(rhs)
    }
}

infix fun Trivalent.eq(rhs: QuadraticMonomial): QuadraticInequality {
    return this.value.eq(rhs)
}

infix fun BalancedTrivalent.eq(rhs: QuadraticMonomial): QuadraticInequality {
    return this.value.eq(rhs)
}

infix fun Int.eq(rhs: QuadraticMonomial): QuadraticInequality {
    return Flt64(this).eq(rhs)
}

infix fun Double.eq(rhs: QuadraticMonomial): QuadraticInequality {
    return Flt64(this).eq(rhs)
}

infix fun <T : RealNumber<T>> T.eq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.toFlt64()),
        QuadraticPolynomial(rhs.copy()),
        Sign.Equal
    )
}

infix fun Boolean.neq(rhs: QuadraticMonomial): QuadraticInequality {
    return if (this) {
        1.neq(rhs)
    } else {
        0.neq(rhs)
    }
}

infix fun Trivalent.neq(rhs: QuadraticMonomial): QuadraticInequality {
    return this.value.neq(rhs)
}

infix fun BalancedTrivalent.neq(rhs: QuadraticMonomial): QuadraticInequality {
    return this.value.neq(rhs)
}

infix fun Int.neq(rhs: QuadraticMonomial): QuadraticInequality {
    return Flt64(this).neq(rhs)
}

infix fun Double.neq(rhs: QuadraticMonomial): QuadraticInequality {
    return Flt64(this).neq(rhs)
}

infix fun <T : RealNumber<T>> T.neq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.toFlt64()),
        QuadraticPolynomial(rhs.copy()),
        Sign.Unequal
    )
}

infix fun Boolean.gr(rhs: QuadraticMonomial): QuadraticInequality {
    return if (this) {
        1.gr(rhs)
    } else {
        0.gr(rhs)
    }
}

infix fun Trivalent.gr(rhs: QuadraticMonomial): QuadraticInequality {
    return this.value.gr(rhs)
}

infix fun BalancedTrivalent.gr(rhs: QuadraticMonomial): QuadraticInequality {
    return this.value.gr(rhs)
}

infix fun Int.gr(rhs: QuadraticMonomial): QuadraticInequality {
    return Flt64(this).gr(rhs)
}

infix fun Double.gr(rhs: QuadraticMonomial): QuadraticInequality {
    return Flt64(this).gr(rhs)
}

infix fun <T : RealNumber<T>> T.gr(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.toFlt64()),
        QuadraticPolynomial(rhs.copy()),
        Sign.Greater
    )
}

infix fun Boolean.geq(rhs: QuadraticMonomial): QuadraticInequality {
    return if (this) {
        1.geq(rhs)
    } else {
        0.geq(rhs)
    }
}

infix fun Trivalent.geq(rhs: QuadraticMonomial): QuadraticInequality {
    return this.value.geq(rhs)
}

infix fun BalancedTrivalent.geq(rhs: QuadraticMonomial): QuadraticInequality {
    return this.value.geq(rhs)
}

infix fun Int.geq(rhs: QuadraticMonomial): QuadraticInequality {
    return Flt64(this).geq(rhs)
}

infix fun Double.geq(rhs: QuadraticMonomial): QuadraticInequality {
    return Flt64(this).geq(rhs)
}

infix fun <T : RealNumber<T>> T.geq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.toFlt64()),
        QuadraticPolynomial(rhs.copy()),
        Sign.GreaterEqual
    )
}

// quantity monomial and quantity

@JvmName("quantityQuadraticMonomialLsQuantity")
infix fun <T : RealNumber<T>> Quantity<QuadraticMonomial>.ls(rhs: Quantity<T>): QuadraticInequality {
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

@JvmName("quantityQuadraticMonomialLeqQuantity")
infix fun <T : RealNumber<T>> Quantity<QuadraticMonomial>.leq(rhs: Quantity<T>): QuadraticInequality {
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

@JvmName("quantityQuadraticMonomialEqQuantity")
infix fun <T : RealNumber<T>> Quantity<QuadraticMonomial>.eq(rhs: Quantity<T>): QuadraticInequality {
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

@JvmName("quantityQuadraticMonomialNeqQuantity")
infix fun <T : RealNumber<T>> Quantity<QuadraticMonomial>.neq(rhs: Quantity<T>): QuadraticInequality {
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

@JvmName("quantityQuadraticMonomialGrQuantity")
infix fun <T : RealNumber<T>> Quantity<QuadraticMonomial>.gr(rhs: Quantity<T>): QuadraticInequality {
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

@JvmName("quantityQuadraticMonomialGeqQuantity")
infix fun <T : RealNumber<T>> Quantity<QuadraticMonomial>.geq(rhs: Quantity<T>): QuadraticInequality {
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

@JvmName("quantityLsQuantityQuadraticMonomial")
infix fun <T : RealNumber<T>> Quantity<T>.ls(rhs: Quantity<QuadraticMonomial>): QuadraticInequality {
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

@JvmName("quantityLeqQuantityQuadraticMonomial")
infix fun <T : RealNumber<T>> Quantity<T>.leq(rhs: Quantity<QuadraticMonomial>): QuadraticInequality {
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

@JvmName("quantityEqQuantityQuadraticMonomial")
infix fun <T : RealNumber<T>> Quantity<T>.eq(rhs: Quantity<QuadraticMonomial>): QuadraticInequality {
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

@JvmName("quantityNeqQuantityQuadraticMonomial")
infix fun <T : RealNumber<T>> Quantity<T>.neq(rhs: Quantity<QuadraticMonomial>): QuadraticInequality {
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

@JvmName("quantityGrQuantityQuadraticMonomial")
infix fun <T : RealNumber<T>> Quantity<T>.gr(rhs: Quantity<QuadraticMonomial>): QuadraticInequality {
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

@JvmName("quantityGeqQuantityQuadraticMonomial")
infix fun <T : RealNumber<T>> Quantity<T>.geq(rhs: Quantity<QuadraticMonomial>): QuadraticInequality {
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

infix fun AbstractQuadraticPolynomial<*>.ls(rhs: Boolean): QuadraticInequality {
    return if (rhs) {
        this.ls(1)
    } else {
        this.ls(0)
    }
}

infix fun AbstractQuadraticPolynomial<*>.ls(rhs: Trivalent): QuadraticInequality {
    return this.ls(rhs.value)
}

infix fun AbstractQuadraticPolynomial<*>.ls(rhs: BalancedTrivalent): QuadraticInequality {
    return this.ls(rhs.value)
}

infix fun AbstractQuadraticPolynomial<*>.ls(rhs: Int): QuadraticInequality {
    return this.ls(Flt64(rhs))
}

infix fun AbstractQuadraticPolynomial<*>.ls(rhs: Double): QuadraticInequality {
    return this.ls(Flt64(rhs))
}

infix fun <T : RealNumber<T>> AbstractQuadraticPolynomial<*>.ls(rhs: T): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.Less
    )
}

infix fun AbstractQuadraticPolynomial<*>.leq(rhs: Boolean): QuadraticInequality {
    return if (rhs) {
        this.leq(1)
    } else {
        this.leq(0)
    }
}

infix fun AbstractQuadraticPolynomial<*>.leq(rhs: Trivalent): QuadraticInequality {
    return this.leq(rhs.value)
}

infix fun AbstractQuadraticPolynomial<*>.leq(rhs: BalancedTrivalent): QuadraticInequality {
    return this.leq(rhs.value)
}

infix fun AbstractQuadraticPolynomial<*>.leq(rhs: Int): QuadraticInequality {
    return this.leq(Flt64(rhs))
}

infix fun AbstractQuadraticPolynomial<*>.leq(rhs: Double): QuadraticInequality {
    return this.leq(Flt64(rhs))
}

infix fun <T : RealNumber<T>> AbstractQuadraticPolynomial<*>.leq(rhs: T): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun AbstractQuadraticPolynomial<*>.eq(rhs: Boolean): QuadraticInequality {
    return if (rhs) {
        this.eq(1)
    } else {
        this.eq(0)
    }
}

infix fun AbstractQuadraticPolynomial<*>.eq(rhs: Trivalent): QuadraticInequality {
    return this.eq(rhs.value)
}

infix fun AbstractQuadraticPolynomial<*>.eq(rhs: BalancedTrivalent): QuadraticInequality {
    return this.eq(rhs.value)
}

infix fun AbstractQuadraticPolynomial<*>.eq(rhs: Int): QuadraticInequality {
    return this.eq(Flt64(rhs))
}

infix fun AbstractQuadraticPolynomial<*>.eq(rhs: Double): QuadraticInequality {
    return this.eq(Flt64(rhs))
}

infix fun <T : RealNumber<T>> AbstractQuadraticPolynomial<*>.eq(rhs: T): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.Equal
    )
}

infix fun AbstractQuadraticPolynomial<*>.neq(rhs: Boolean): QuadraticInequality {
    return if (rhs) {
        this.neq(1)
    } else {
        this.neq(0)
    }
}

infix fun AbstractQuadraticPolynomial<*>.neq(rhs: Trivalent): QuadraticInequality {
    return this.neq(rhs.value)
}

infix fun AbstractQuadraticPolynomial<*>.neq(rhs: BalancedTrivalent): QuadraticInequality {
    return this.neq(rhs.value)
}

infix fun AbstractQuadraticPolynomial<*>.neq(rhs: Int): QuadraticInequality {
    return this.neq(Flt64(rhs))
}

infix fun AbstractQuadraticPolynomial<*>.neq(rhs: Double): QuadraticInequality {
    return this.neq(Flt64(rhs))
}

infix fun <T : RealNumber<T>> AbstractQuadraticPolynomial<*>.neq(rhs: T): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun AbstractQuadraticPolynomial<*>.gr(rhs: Boolean): QuadraticInequality {
    return if (rhs) {
        this.gr(1)
    } else {
        this.gr(0)
    }
}

infix fun AbstractQuadraticPolynomial<*>.gr(rhs: Trivalent): QuadraticInequality {
    return this.gr(rhs.value)
}

infix fun AbstractQuadraticPolynomial<*>.gr(rhs: BalancedTrivalent): QuadraticInequality {
    return this.gr(rhs.value)
}

infix fun AbstractQuadraticPolynomial<*>.gr(rhs: Int): QuadraticInequality {
    return this.gr(Flt64(rhs))
}

infix fun AbstractQuadraticPolynomial<*>.gr(rhs: Double): QuadraticInequality {
    return this.gr(Flt64(rhs))
}

infix fun <T : RealNumber<T>> AbstractQuadraticPolynomial<*>.gr(rhs: T): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.Greater
    )
}

infix fun AbstractQuadraticPolynomial<*>.geq(rhs: Boolean): QuadraticInequality {
    return if (rhs) {
        this.geq(1)
    } else {
        this.geq(0)
    }
}

infix fun AbstractQuadraticPolynomial<*>.geq(rhs: Trivalent): QuadraticInequality {
    return this.geq(rhs.value)
}

infix fun AbstractQuadraticPolynomial<*>.geq(rhs: BalancedTrivalent): QuadraticInequality {
    return this.geq(rhs.value)
}

infix fun AbstractQuadraticPolynomial<*>.geq(rhs: Int): QuadraticInequality {
    return this.geq(Flt64(rhs))
}

infix fun AbstractQuadraticPolynomial<*>.geq(rhs: Double): QuadraticInequality {
    return this.geq(Flt64(rhs))
}

infix fun <T : RealNumber<T>> AbstractQuadraticPolynomial<*>.geq(rhs: T): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.GreaterEqual
    )
}

infix fun Boolean.ls(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return if (this) {
        1.ls(rhs)
    } else {
        0.ls(rhs)
    }
}

infix fun Trivalent.ls(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return this.value.ls(rhs)
}

infix fun BalancedTrivalent.ls(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return this.value.ls(rhs)
}

infix fun Int.ls(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return Flt64(this).ls(rhs)
}

infix fun Double.ls(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return Flt64(this).ls(rhs)
}

infix fun <T : RealNumber<T>> T.ls(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.Less
    )
}

infix fun Int.leq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return Flt64(this).leq(rhs)
}

infix fun Double.leq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return Flt64(this).leq(rhs)
}

infix fun <T : RealNumber<T>> T.leq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.LessEqual
    )
}

infix fun Boolean.eq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return if (this) {
        1.eq(rhs)
    } else {
        0.eq(rhs)
    }
}

infix fun Trivalent.eq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return this.value.eq(rhs)
}

infix fun BalancedTrivalent.eq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return this.value.eq(rhs)
}

infix fun Int.eq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return Flt64(this).eq(rhs)
}

infix fun Double.eq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return Flt64(this).eq(rhs)
}

infix fun <T : RealNumber<T>> T.eq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.Equal
    )
}

infix fun Boolean.neq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return if (this) {
        1.neq(rhs)
    } else {
        0.neq(rhs)
    }
}

infix fun Trivalent.neq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return this.value.neq(rhs)
}

infix fun BalancedTrivalent.neq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return this.value.neq(rhs)
}

infix fun Int.neq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return Flt64(this).neq(rhs)
}

infix fun Double.neq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return Flt64(this).neq(rhs)
}

infix fun <T : RealNumber<T>> T.neq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.Unequal
    )
}

infix fun Boolean.gr(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return if (this) {
        1.gr(rhs)
    } else {
        0.gr(rhs)
    }
}

infix fun Trivalent.gr(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return this.value.gr(rhs)
}

infix fun BalancedTrivalent.gr(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return this.value.gr(rhs)
}

infix fun Int.gr(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return Flt64(this).gr(rhs)
}

infix fun Double.gr(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return Flt64(this).gr(rhs)
}

infix fun <T : RealNumber<T>> T.gr(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.Greater
    )
}

infix fun Boolean.geq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return if (this) {
        1.geq(rhs)
    } else {
        0.geq(rhs)
    }
}

infix fun Trivalent.geq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return this.value.geq(rhs)
}

infix fun BalancedTrivalent.geq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return this.value.geq(rhs)
}

infix fun Int.geq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return Flt64(this).geq(rhs)
}

infix fun Double.geq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return Flt64(this).geq(rhs)
}

infix fun <T : RealNumber<T>> T.geq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.GreaterEqual
    )
}

// quantity polynomial and quantity

@JvmName("quantityQuadraticPolynomialLsQuantity")
infix fun <T : RealNumber<T>> Quantity<AbstractQuadraticPolynomial<*>>.ls(rhs: Quantity<T>): QuadraticInequality {
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

@JvmName("QuantityQuadraticPolynomialLeqQuantity")
infix fun <T : RealNumber<T>> Quantity<AbstractQuadraticPolynomial<*>>.leq(rhs: Quantity<T>): QuadraticInequality {
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

@JvmName("quantityQuadraticPolynomialEqQuantity")
infix fun <T : RealNumber<T>> Quantity<AbstractQuadraticPolynomial<*>>.eq(rhs: Quantity<T>): QuadraticInequality {
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

@JvmName("quantityQuadraticPolynomialNeqQuantity")
infix fun <T : RealNumber<T>> Quantity<AbstractQuadraticPolynomial<*>>.neq(rhs: Quantity<T>): QuadraticInequality {
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

@JvmName("quantityQuadraticPolynomialGrQuantity")
infix fun <T : RealNumber<T>> Quantity<AbstractQuadraticPolynomial<*>>.gr(rhs: Quantity<T>): QuadraticInequality {
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

@JvmName("quantityQuadraticPolynomialGeqQuantity")
infix fun <T : RealNumber<T>> Quantity<AbstractQuadraticPolynomial<*>>.geq(rhs: Quantity<T>): QuadraticInequality {
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

@JvmName("quantityLsQuantityQuadraticPolynomial")
infix fun <T : RealNumber<T>> Quantity<T>.ls(rhs: Quantity<AbstractQuadraticPolynomial<*>>): QuadraticInequality {
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

@JvmName("quantityLeqQuantityQuadraticPolynomial")
infix fun <T : RealNumber<T>> Quantity<T>.leq(rhs: Quantity<AbstractQuadraticPolynomial<*>>): QuadraticInequality {
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

@JvmName("quantityEqQuantityQuadraticPolynomial")
infix fun <T : RealNumber<T>> Quantity<T>.eq(rhs: Quantity<AbstractQuadraticPolynomial<*>>): QuadraticInequality {
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

@JvmName("quantityNeqQuantityQuadraticPolynomial")
infix fun <T : RealNumber<T>> Quantity<T>.neq(rhs: Quantity<AbstractQuadraticPolynomial<*>>): QuadraticInequality {
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

@JvmName("quantityGrQuantityQuadraticPolynomial")
infix fun <T : RealNumber<T>> Quantity<T>.gr(rhs: Quantity<AbstractQuadraticPolynomial<*>>): QuadraticInequality {
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

@JvmName("quantityGeqQuantityQuadraticPolynomial")
infix fun <T : RealNumber<T>> Quantity<T>.geq(rhs: Quantity<AbstractQuadraticPolynomial<*>>): QuadraticInequality {
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

// symbol and variable

infix fun QuadraticIntermediateSymbol.ls(rhs: AbstractVariableItem<*, *>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.Less
    )
}

infix fun QuadraticIntermediateSymbol.leq(rhs: AbstractVariableItem<*, *>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun QuadraticIntermediateSymbol.eq(rhs: AbstractVariableItem<*, *>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.Equal
    )
}

infix fun QuadraticIntermediateSymbol.neq(rhs: AbstractVariableItem<*, *>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun QuadraticIntermediateSymbol.gr(rhs: AbstractVariableItem<*, *>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.Greater
    )
}

infix fun QuadraticIntermediateSymbol.geq(rhs: AbstractVariableItem<*, *>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.GreaterEqual
    )
}

infix fun AbstractVariableItem<*, *>.ls(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.Less
    )
}

infix fun AbstractVariableItem<*, *>.leq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun AbstractVariableItem<*, *>.eq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.Equal
    )
}

infix fun AbstractVariableItem<*, *>.neq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun AbstractVariableItem<*, *>.gr(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.Greater
    )
}

infix fun AbstractVariableItem<*, *>.geq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.GreaterEqual
    )
}

// quantity symbol and quantity variable

@JvmName("quantityQuadraticSymbolLsQuantityVariable")
infix fun Quantity<QuadraticIntermediateSymbol>.ls(rhs: Quantity<AbstractVariableItem<*, *>>): QuadraticInequality {
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

@JvmName("quantityQuadraticSymbolLeqQuantityVariable")
infix fun Quantity<QuadraticIntermediateSymbol>.leq(rhs: Quantity<AbstractVariableItem<*, *>>): QuadraticInequality {
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

@JvmName("quantityQuadraticSymbolEqQuantityVariable")
infix fun Quantity<QuadraticIntermediateSymbol>.eq(rhs: Quantity<AbstractVariableItem<*, *>>): QuadraticInequality {
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

@JvmName("quantityQuadraticSymbolNeqQuantityVariable")
infix fun Quantity<QuadraticIntermediateSymbol>.neq(rhs: Quantity<AbstractVariableItem<*, *>>): QuadraticInequality {
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

@JvmName("quantityQuadraticSymbolGrQuantityVariable")
infix fun Quantity<QuadraticIntermediateSymbol>.gr(rhs: Quantity<AbstractVariableItem<*, *>>): QuadraticInequality {
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

@JvmName("quantityQuadraticSymbolGeqQuantityVariable")
infix fun Quantity<QuadraticIntermediateSymbol>.geq(rhs: Quantity<AbstractVariableItem<*, *>>): QuadraticInequality {
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

@JvmName("quantityVariableLsQuantityQuadraticSymbol")
infix fun Quantity<AbstractVariableItem<*, *>>.ls(rhs: Quantity<QuadraticIntermediateSymbol>): QuadraticInequality {
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

@JvmName("quantityVariableLeqQuantityQuadraticSymbol")
infix fun Quantity<AbstractVariableItem<*, *>>.leq(rhs: Quantity<QuadraticIntermediateSymbol>): QuadraticInequality {
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

@JvmName("quantityVariableEqQuantityQuadraticSymbol")
infix fun Quantity<AbstractVariableItem<*, *>>.eq(rhs: Quantity<QuadraticIntermediateSymbol>): QuadraticInequality {
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

@JvmName("quantityVariableNeqQuantityQuadraticSymbol")
infix fun Quantity<AbstractVariableItem<*, *>>.neq(rhs: Quantity<QuadraticIntermediateSymbol>): QuadraticInequality {
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

@JvmName("quantityVariableGrQuantityQuadraticSymbol")
infix fun Quantity<AbstractVariableItem<*, *>>.gr(rhs: Quantity<QuadraticIntermediateSymbol>): QuadraticInequality {
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

@JvmName("quantityVariableGeqQuantityQuadraticSymbol")
infix fun Quantity<AbstractVariableItem<*, *>>.geq(rhs: Quantity<QuadraticIntermediateSymbol>): QuadraticInequality {
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

infix fun QuadraticMonomial.ls(rhs: AbstractVariableItem<*, *>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.Less
    )
}

infix fun QuadraticMonomial.leq(rhs: AbstractVariableItem<*, *>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun QuadraticMonomial.eq(rhs: AbstractVariableItem<*, *>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.Equal
    )
}

infix fun QuadraticMonomial.neq(rhs: AbstractVariableItem<*, *>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun QuadraticMonomial.gr(rhs: AbstractVariableItem<*, *>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.Greater
    )
}

infix fun QuadraticMonomial.geq(rhs: AbstractVariableItem<*, *>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.GreaterEqual
    )
}

infix fun AbstractVariableItem<*, *>.ls(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.Less
    )
}

infix fun AbstractVariableItem<*, *>.leq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.LessEqual
    )
}

infix fun AbstractVariableItem<*, *>.eq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.Equal
    )
}

infix fun AbstractVariableItem<*, *>.neq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.Unequal
    )
}

infix fun AbstractVariableItem<*, *>.gr(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.Greater
    )
}

infix fun AbstractVariableItem<*, *>.geq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.GreaterEqual
    )
}

// quantity monomial and quantity variable

@JvmName("quantityQuadraticMonomialLsQuantityVariable")
infix fun Quantity<QuadraticMonomial>.ls(rhs: Quantity<AbstractVariableItem<*, *>>): QuadraticInequality {
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

@JvmName("quantityQuadraticMonomialLeqQuantityVariable")
infix fun Quantity<QuadraticMonomial>.leq(rhs: Quantity<AbstractVariableItem<*, *>>): QuadraticInequality {
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

@JvmName("quantityQuadraticMonomialEqQuantityVariable")
infix fun Quantity<QuadraticMonomial>.eq(rhs: Quantity<AbstractVariableItem<*, *>>): QuadraticInequality {
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

@JvmName("quantityQuadraticMonomialNeqQuantityVariable")
infix fun Quantity<QuadraticMonomial>.neq(rhs: Quantity<AbstractVariableItem<*, *>>): QuadraticInequality {
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

@JvmName("quantityQuadraticMonomialGrQuantityVariable")
infix fun Quantity<QuadraticMonomial>.gr(rhs: Quantity<AbstractVariableItem<*, *>>): QuadraticInequality {
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

@JvmName("quantityQuadraticMonomialGeqQuantityVariable")
infix fun Quantity<QuadraticMonomial>.geq(rhs: Quantity<AbstractVariableItem<*, *>>): QuadraticInequality {
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

@JvmName("quantityVariableLsQuantityQuadraticMonomial")
infix fun Quantity<AbstractVariableItem<*, *>>.ls(rhs: Quantity<QuadraticMonomial>): QuadraticInequality {
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

@JvmName("quantityVariableLeqQuantityQuadraticMonomial")
infix fun Quantity<AbstractVariableItem<*, *>>.leq(rhs: Quantity<QuadraticMonomial>): QuadraticInequality {
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

@JvmName("quantityVariableEqQuantityQuadraticMonomial")
infix fun Quantity<AbstractVariableItem<*, *>>.eq(rhs: Quantity<QuadraticMonomial>): QuadraticInequality {
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

@JvmName("quantityVariableNeqQuantityQuadraticMonomial")
infix fun Quantity<AbstractVariableItem<*, *>>.neq(rhs: Quantity<QuadraticMonomial>): QuadraticInequality {
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

@JvmName("quantityVariableGrQuantityQuadraticMonomial")
infix fun Quantity<AbstractVariableItem<*, *>>.gr(rhs: Quantity<QuadraticMonomial>): QuadraticInequality {
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

@JvmName("quantityVariableGeqQuantityQuadraticMonomial")
infix fun Quantity<AbstractVariableItem<*, *>>.geq(rhs: Quantity<QuadraticMonomial>): QuadraticInequality {
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

infix fun AbstractQuadraticPolynomial<*>.ls(rhs: AbstractVariableItem<*, *>): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.Less
    )
}

infix fun AbstractQuadraticPolynomial<*>.leq(rhs: AbstractVariableItem<*, *>): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun AbstractQuadraticPolynomial<*>.eq(rhs: AbstractVariableItem<*, *>): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.Equal
    )
}

infix fun AbstractQuadraticPolynomial<*>.neq(rhs: AbstractVariableItem<*, *>): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun AbstractQuadraticPolynomial<*>.gr(rhs: AbstractVariableItem<*, *>): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.Greater
    )
}

infix fun AbstractQuadraticPolynomial<*>.geq(rhs: AbstractVariableItem<*, *>): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.GreaterEqual
    )
}

infix fun AbstractVariableItem<*, *>.ls(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.Less
    )
}

infix fun AbstractVariableItem<*, *>.leq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.LessEqual
    )
}

infix fun AbstractVariableItem<*, *>.eq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.Equal
    )
}

infix fun AbstractVariableItem<*, *>.neq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.Unequal
    )
}

infix fun AbstractVariableItem<*, *>.gr(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.Greater
    )
}

infix fun AbstractVariableItem<*, *>.geq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.GreaterEqual
    )
}

// quantity polynomial and quantity variable

@JvmName("quantityQuadraticPolynomialLsQuantityVariable")
infix fun Quantity<AbstractQuadraticPolynomial<*>>.ls(rhs: Quantity<AbstractVariableItem<*, *>>): QuadraticInequality {
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

@JvmName("quantityQuadraticPolynomialLeqQuantityVariable")
infix fun Quantity<AbstractQuadraticPolynomial<*>>.leq(rhs: Quantity<AbstractVariableItem<*, *>>): QuadraticInequality {
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

@JvmName("quantityQuadraticPolynomialEqQuantityVariable")
infix fun Quantity<AbstractQuadraticPolynomial<*>>.eq(rhs: Quantity<AbstractVariableItem<*, *>>): QuadraticInequality {
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

@JvmName("quantityQuadraticPolynomialNeqQuantityVariable")
infix fun Quantity<AbstractQuadraticPolynomial<*>>.neq(rhs: Quantity<AbstractVariableItem<*, *>>): QuadraticInequality {
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

@JvmName("quantityQuadraticPolynomialGrQuantityVariable")
infix fun Quantity<AbstractQuadraticPolynomial<*>>.gr(rhs: Quantity<AbstractVariableItem<*, *>>): QuadraticInequality {
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

@JvmName("quantityQuadraticPolynomialGeqQuantityVariable")
infix fun Quantity<AbstractQuadraticPolynomial<*>>.geq(rhs: Quantity<AbstractVariableItem<*, *>>): QuadraticInequality {
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

@JvmName("quantityVariableLsQuantityQuadraticPolynomial")
infix fun Quantity<AbstractVariableItem<*, *>>.ls(rhs: Quantity<AbstractQuadraticPolynomial<*>>): QuadraticInequality {
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

@JvmName("quantityVariableLeqQuantityQuadraticPolynomial")
infix fun Quantity<AbstractVariableItem<*, *>>.leq(rhs: Quantity<AbstractQuadraticPolynomial<*>>): QuadraticInequality {
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

@JvmName("quantityVariableEqQuantityQuadraticPolynomial")
infix fun Quantity<AbstractVariableItem<*, *>>.eq(rhs: Quantity<AbstractQuadraticPolynomial<*>>): QuadraticInequality {
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

@JvmName("quantityVariableNeqQuantityQuadraticPolynomial")
infix fun Quantity<AbstractVariableItem<*, *>>.neq(rhs: Quantity<AbstractQuadraticPolynomial<*>>): QuadraticInequality {
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

@JvmName("quantityVariableGrQuantityQuadraticPolynomial")
infix fun Quantity<AbstractVariableItem<*, *>>.gr(rhs: Quantity<AbstractQuadraticPolynomial<*>>): QuadraticInequality {
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

@JvmName("quantityVariableGeqQuantityQuadraticPolynomial")
infix fun Quantity<AbstractVariableItem<*, *>>.geq(rhs: Quantity<AbstractQuadraticPolynomial<*>>): QuadraticInequality {
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

// symbol and symbol

infix fun LinearIntermediateSymbol.ls(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.Less
    )
}

infix fun LinearIntermediateSymbol.leq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun LinearIntermediateSymbol.eq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.Equal
    )
}

infix fun LinearIntermediateSymbol.neq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun LinearIntermediateSymbol.gr(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.Greater
    )
}

infix fun LinearIntermediateSymbol.geq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.GreaterEqual
    )
}

infix fun QuadraticIntermediateSymbol.ls(rhs: LinearIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.Less
    )
}

infix fun QuadraticIntermediateSymbol.leq(rhs: LinearIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun QuadraticIntermediateSymbol.eq(rhs: LinearIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.Equal
    )
}

infix fun QuadraticIntermediateSymbol.neq(rhs: LinearIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun QuadraticIntermediateSymbol.gr(rhs: LinearIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.Greater
    )
}

infix fun QuadraticIntermediateSymbol.geq(rhs: LinearIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.GreaterEqual
    )
}

infix fun QuadraticIntermediateSymbol.ls(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.Less
    )
}

infix fun QuadraticIntermediateSymbol.leq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun QuadraticIntermediateSymbol.eq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.Equal
    )
}

infix fun QuadraticIntermediateSymbol.neq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun QuadraticIntermediateSymbol.gr(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.Greater
    )
}

infix fun QuadraticIntermediateSymbol.geq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.GreaterEqual
    )
}

// quantity symbol and quantity symbol

@JvmName("quantityLinearSymbolLsQuantityQuadraticSymbol")
infix fun Quantity<LinearIntermediateSymbol>.ls(rhs: Quantity<QuadraticIntermediateSymbol>): QuadraticInequality {
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

@JvmName("quantityLinearSymbolLeqQuantityQuadraticSymbol")
infix fun Quantity<LinearIntermediateSymbol>.leq(rhs: Quantity<QuadraticIntermediateSymbol>): QuadraticInequality {
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

@JvmName("quantityLinearSymbolEqQuantityQuadraticSymbol")
infix fun Quantity<LinearIntermediateSymbol>.eq(rhs: Quantity<QuadraticIntermediateSymbol>): QuadraticInequality {
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

@JvmName("quantityLinearSymbolNeqQuantityQuadraticSymbol")
infix fun Quantity<LinearIntermediateSymbol>.neq(rhs: Quantity<QuadraticIntermediateSymbol>): QuadraticInequality {
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

@JvmName("quantityLinearSymbolGrQuantityQuadraticSymbol")
infix fun Quantity<LinearIntermediateSymbol>.gr(rhs: Quantity<QuadraticIntermediateSymbol>): QuadraticInequality {
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

@JvmName("quantityLinearSymbolGeqQuantityQuadraticSymbol")
infix fun Quantity<LinearIntermediateSymbol>.geq(rhs: Quantity<QuadraticIntermediateSymbol>): QuadraticInequality {
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

@JvmName("quantityQuadraticSymbolLsQuantityLinearSymbol")
infix fun Quantity<QuadraticIntermediateSymbol>.ls(rhs: Quantity<LinearIntermediateSymbol>): QuadraticInequality {
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

@JvmName("quantityQuadraticSymbolLeqQuantityLinearSymbol")
infix fun Quantity<QuadraticIntermediateSymbol>.leq(rhs: Quantity<LinearIntermediateSymbol>): QuadraticInequality {
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

@JvmName("quantityQuadraticSymbolEqQuantityLinearSymbol")
infix fun Quantity<QuadraticIntermediateSymbol>.eq(rhs: Quantity<LinearIntermediateSymbol>): QuadraticInequality {
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

@JvmName("quantityQuadraticSymbolNeqQuantityLinearSymbol")
infix fun Quantity<QuadraticIntermediateSymbol>.neq(rhs: Quantity<LinearIntermediateSymbol>): QuadraticInequality {
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

@JvmName("quantityQuadraticSymbolGrQuantityLinearSymbol")
infix fun Quantity<QuadraticIntermediateSymbol>.gr(rhs: Quantity<LinearIntermediateSymbol>): QuadraticInequality {
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

@JvmName("quantityQuadraticSymbolGeqQuantityLinearSymbol")
infix fun Quantity<QuadraticIntermediateSymbol>.geq(rhs: Quantity<LinearIntermediateSymbol>): QuadraticInequality {
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

@JvmName("quantityQuadraticSymbolLsQuantityQuadraticSymbol")
infix fun Quantity<QuadraticIntermediateSymbol>.ls(rhs: Quantity<QuadraticIntermediateSymbol>): QuadraticInequality {
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

@JvmName("quantityQuadraticSymbolLeqQuantityQuadraticSymbol")
infix fun Quantity<QuadraticIntermediateSymbol>.leq(rhs: Quantity<QuadraticIntermediateSymbol>): QuadraticInequality {
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

@JvmName("quantityQuadraticSymbolEqQuantityQuadraticSymbol")
infix fun Quantity<QuadraticIntermediateSymbol>.eq(rhs: Quantity<QuadraticIntermediateSymbol>): QuadraticInequality {
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

@JvmName("quantityQuadraticSymbolNeqQuantityQuadraticSymbol")
infix fun Quantity<QuadraticIntermediateSymbol>.neq(rhs: Quantity<QuadraticIntermediateSymbol>): QuadraticInequality {
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

@JvmName("quantityQuadraticSymbolGrQuantityQuadraticSymbol")
infix fun Quantity<QuadraticIntermediateSymbol>.gr(rhs: Quantity<QuadraticIntermediateSymbol>): QuadraticInequality {
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

@JvmName("quantityQuadraticSymbolGeqQuantityQuadraticSymbol")
infix fun Quantity<QuadraticIntermediateSymbol>.geq(rhs: Quantity<QuadraticIntermediateSymbol>): QuadraticInequality {
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

infix fun LinearMonomial.ls(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.Less
    )
}

infix fun LinearMonomial.leq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun LinearMonomial.eq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.Equal
    )
}

infix fun LinearMonomial.neq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun LinearMonomial.gr(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.Greater
    )
}

infix fun LinearMonomial.geq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.GreaterEqual
    )
}

infix fun QuadraticIntermediateSymbol.ls(rhs: LinearMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.Less
    )
}

infix fun QuadraticIntermediateSymbol.leq(rhs: LinearMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.LessEqual
    )
}

infix fun QuadraticIntermediateSymbol.eq(rhs: LinearMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.Equal
    )
}

infix fun QuadraticIntermediateSymbol.neq(rhs: LinearMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.Unequal
    )
}

infix fun QuadraticIntermediateSymbol.gr(rhs: LinearMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.Greater
    )
}

infix fun QuadraticIntermediateSymbol.geq(rhs: LinearMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.GreaterEqual
    )
}

infix fun QuadraticMonomial.ls(rhs: LinearIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.Less
    )
}

infix fun QuadraticMonomial.leq(rhs: LinearIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun QuadraticMonomial.eq(rhs: LinearIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.Equal
    )
}

infix fun QuadraticMonomial.neq(rhs: LinearIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun QuadraticMonomial.gr(rhs: LinearIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.Greater
    )
}

infix fun QuadraticMonomial.geq(rhs: LinearIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.GreaterEqual
    )
}

infix fun LinearIntermediateSymbol.ls(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.Less
    )
}

infix fun LinearIntermediateSymbol.leq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.LessEqual
    )
}

infix fun LinearIntermediateSymbol.eq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.Equal
    )
}

infix fun LinearIntermediateSymbol.neq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.Unequal
    )
}

infix fun LinearIntermediateSymbol.gr(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.Greater
    )
}

infix fun LinearIntermediateSymbol.geq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.GreaterEqual
    )
}

infix fun QuadraticMonomial.ls(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.Less
    )
}

infix fun QuadraticMonomial.leq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun QuadraticMonomial.eq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.Equal
    )
}

infix fun QuadraticMonomial.neq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun QuadraticMonomial.gr(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.Greater
    )
}

infix fun QuadraticMonomial.geq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.GreaterEqual
    )
}

infix fun QuadraticIntermediateSymbol.ls(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.Less
    )
}

infix fun QuadraticIntermediateSymbol.leq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.LessEqual
    )
}

infix fun QuadraticIntermediateSymbol.eq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.Equal
    )
}

infix fun QuadraticIntermediateSymbol.neq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.Unequal
    )
}

infix fun QuadraticIntermediateSymbol.gr(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.Greater
    )
}

infix fun QuadraticIntermediateSymbol.geq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.GreaterEqual
    )
}

// quantity monomial and quantity symbol

@JvmName("quantityLinearMonomialLsQuantityQuadraticSymbol")
infix fun Quantity<LinearMonomial>.ls(rhs: Quantity<QuadraticIntermediateSymbol>): QuadraticInequality {
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

@JvmName("quantityLinearMonomialLeqQuantityQuadraticSymbol")
infix fun Quantity<LinearMonomial>.leq(rhs: Quantity<QuadraticIntermediateSymbol>): QuadraticInequality {
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

@JvmName("quantityLinearMonomialEqQuantityQuadraticSymbol")
infix fun Quantity<LinearMonomial>.eq(rhs: Quantity<QuadraticIntermediateSymbol>): QuadraticInequality {
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

@JvmName("quantityLinearMonomialNeqQuantityQuadraticSymbol")
infix fun Quantity<LinearMonomial>.neq(rhs: Quantity<QuadraticIntermediateSymbol>): QuadraticInequality {
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

@JvmName("quantityLinearMonomialGrQuantityQuadraticSymbol")
infix fun Quantity<LinearMonomial>.gr(rhs: Quantity<QuadraticIntermediateSymbol>): QuadraticInequality {
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

@JvmName("quantityLinearMonomialGeqQuantityQuadraticSymbol")
infix fun Quantity<LinearMonomial>.geq(rhs: Quantity<QuadraticIntermediateSymbol>): QuadraticInequality {
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

@JvmName("quantityQuadraticSymbolLsQuantityLinearMonomial")
infix fun Quantity<QuadraticIntermediateSymbol>.ls(rhs: Quantity<LinearMonomial>): QuadraticInequality {
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

@JvmName("quantityQuadraticSymbolLeqQuantityLinearMonomial")
infix fun Quantity<QuadraticIntermediateSymbol>.leq(rhs: Quantity<LinearMonomial>): QuadraticInequality {
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

@JvmName("quantityQuadraticSymbolEqQuantityLinearMonomial")
infix fun Quantity<QuadraticIntermediateSymbol>.eq(rhs: Quantity<LinearMonomial>): QuadraticInequality {
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

@JvmName("quantityQuadraticSymbolNeqQuantityLinearMonomial")
infix fun Quantity<QuadraticIntermediateSymbol>.neq(rhs: Quantity<LinearMonomial>): QuadraticInequality {
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

@JvmName("quantityQuadraticSymbolGrQuantityLinearMonomial")
infix fun Quantity<QuadraticIntermediateSymbol>.gr(rhs: Quantity<LinearMonomial>): QuadraticInequality {
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

@JvmName("quantityQuadraticSymbolGeqQuantityLinearMonomial")
infix fun Quantity<QuadraticIntermediateSymbol>.geq(rhs: Quantity<LinearMonomial>): QuadraticInequality {
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

@JvmName("quantityQuadraticMonomialLsQuantityLinearSymbol")
infix fun Quantity<QuadraticMonomial>.ls(rhs: Quantity<LinearIntermediateSymbol>): QuadraticInequality {
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

@JvmName("quantityQuadraticMonomialLeqQuantityLinearSymbol")
infix fun Quantity<QuadraticMonomial>.leq(rhs: Quantity<LinearIntermediateSymbol>): QuadraticInequality {
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

@JvmName("quantityQuadraticMonomialEqQuantityLinearSymbol")
infix fun Quantity<QuadraticMonomial>.eq(rhs: Quantity<LinearIntermediateSymbol>): QuadraticInequality {
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

@JvmName("quantityQuadraticMonomialNeqQuantityLinearSymbol")
infix fun Quantity<QuadraticMonomial>.neq(rhs: Quantity<LinearIntermediateSymbol>): QuadraticInequality {
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

@JvmName("quantityQuadraticMonomialGrQuantityLinearSymbol")
infix fun Quantity<QuadraticMonomial>.gr(rhs: Quantity<LinearIntermediateSymbol>): QuadraticInequality {
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

@JvmName("quantityQuadraticMonomialGeqQuantityLinearSymbol")
infix fun Quantity<QuadraticMonomial>.geq(rhs: Quantity<LinearIntermediateSymbol>): QuadraticInequality {
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

@JvmName("quantityLinearSymbolLsQuantityQuadraticMonomial")
infix fun Quantity<LinearIntermediateSymbol>.ls(rhs: Quantity<QuadraticMonomial>): QuadraticInequality {
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

@JvmName("quantityLinearSymbolLeqQuantityQuadraticMonomial")
infix fun Quantity<LinearIntermediateSymbol>.leq(rhs: Quantity<QuadraticMonomial>): QuadraticInequality {
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

@JvmName("quantityLinearSymbolEqQuantityQuadraticMonomial")
infix fun Quantity<LinearIntermediateSymbol>.eq(rhs: Quantity<QuadraticMonomial>): QuadraticInequality {
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

@JvmName("quantityLinearSymbolNeqQuantityQuadraticMonomial")
infix fun Quantity<LinearIntermediateSymbol>.neq(rhs: Quantity<QuadraticMonomial>): QuadraticInequality {
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

@JvmName("quantityLinearSymbolGrQuantityQuadraticMonomial")
infix fun Quantity<LinearIntermediateSymbol>.gr(rhs: Quantity<QuadraticMonomial>): QuadraticInequality {
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

@JvmName("quantityLinearSymbolGeqQuantityQuadraticMonomial")
infix fun Quantity<LinearIntermediateSymbol>.geq(rhs: Quantity<QuadraticMonomial>): QuadraticInequality {
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

@JvmName("quantityQuadraticMonomialLsQuantityQuadraticSymbol")
infix fun Quantity<QuadraticMonomial>.ls(rhs: Quantity<QuadraticIntermediateSymbol>): QuadraticInequality {
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

@JvmName("quantityQuadraticMonomialLeqQuantityQuadraticSymbol")
infix fun Quantity<QuadraticMonomial>.leq(rhs: Quantity<QuadraticIntermediateSymbol>): QuadraticInequality {
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

@JvmName("quantityQuadraticMonomialEqQuantityQuadraticSymbol")
infix fun Quantity<QuadraticMonomial>.eq(rhs: Quantity<QuadraticIntermediateSymbol>): QuadraticInequality {
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

@JvmName("quantityQuadraticMonomialNeqQuantityQuadraticSymbol")
infix fun Quantity<QuadraticMonomial>.neq(rhs: Quantity<QuadraticIntermediateSymbol>): QuadraticInequality {
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

@JvmName("quantityQuadraticMonomialGrQuantityQuadraticSymbol")
infix fun Quantity<QuadraticMonomial>.gr(rhs: Quantity<QuadraticIntermediateSymbol>): QuadraticInequality {
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

@JvmName("quantityQuadraticMonomialGeqQuantityQuadraticSymbol")
infix fun Quantity<QuadraticMonomial>.geq(rhs: Quantity<QuadraticIntermediateSymbol>): QuadraticInequality {
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

@JvmName("quantityQuadraticSymbolLsQuantityQuadraticMonomial")
infix fun Quantity<QuadraticIntermediateSymbol>.ls(rhs: Quantity<QuadraticMonomial>): QuadraticInequality {
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

@JvmName("quantityQuadraticSymbolLeqQuantityQuadraticMonomial")
infix fun Quantity<QuadraticIntermediateSymbol>.leq(rhs: Quantity<QuadraticMonomial>): QuadraticInequality {
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

@JvmName("quantityQuadraticSymbolEqQuantityQuadraticMonomial")
infix fun Quantity<QuadraticIntermediateSymbol>.eq(rhs: Quantity<QuadraticMonomial>): QuadraticInequality {
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

@JvmName("quantityQuadraticSymbolNeqQuantityQuadraticMonomial")
infix fun Quantity<QuadraticIntermediateSymbol>.neq(rhs: Quantity<QuadraticMonomial>): QuadraticInequality {
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

@JvmName("quantityQuadraticSymbolGrQuantityQuadraticMonomial")
infix fun Quantity<QuadraticIntermediateSymbol>.gr(rhs: Quantity<QuadraticMonomial>): QuadraticInequality {
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

@JvmName("quantityQuadraticSymbolGeqQuantityQuadraticMonomial")
infix fun Quantity<QuadraticIntermediateSymbol>.geq(rhs: Quantity<QuadraticMonomial>): QuadraticInequality {
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

infix fun AbstractLinearPolynomial<*>.ls(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.Less
    )
}

infix fun AbstractLinearPolynomial<*>.leq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun AbstractLinearPolynomial<*>.eq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.Equal
    )
}

infix fun AbstractLinearPolynomial<*>.neq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun AbstractLinearPolynomial<*>.gr(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.Greater
    )
}

infix fun AbstractLinearPolynomial<*>.geq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.GreaterEqual
    )
}

infix fun QuadraticIntermediateSymbol.ls(rhs: AbstractLinearPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.Less
    )
}

infix fun QuadraticIntermediateSymbol.leq(rhs: AbstractLinearPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun QuadraticIntermediateSymbol.eq(rhs: AbstractLinearPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.Equal
    )
}

infix fun QuadraticIntermediateSymbol.neq(rhs: AbstractLinearPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun QuadraticIntermediateSymbol.gr(rhs: AbstractLinearPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.Greater
    )
}

infix fun QuadraticIntermediateSymbol.geq(rhs: AbstractLinearPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.GreaterEqual
    )
}

infix fun AbstractQuadraticPolynomial<*>.ls(rhs: LinearIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.Less
    )
}

infix fun AbstractQuadraticPolynomial<*>.leq(rhs: LinearIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun AbstractQuadraticPolynomial<*>.eq(rhs: LinearIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.Equal
    )
}

infix fun AbstractQuadraticPolynomial<*>.neq(rhs: LinearIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun AbstractQuadraticPolynomial<*>.gr(rhs: LinearIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.Greater
    )
}

infix fun AbstractQuadraticPolynomial<*>.geq(rhs: LinearIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.GreaterEqual
    )
}

infix fun LinearIntermediateSymbol.ls(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.Less
    )
}

infix fun LinearIntermediateSymbol.leq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.LessEqual
    )
}

infix fun LinearIntermediateSymbol.eq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.Equal
    )
}

infix fun LinearIntermediateSymbol.neq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.Unequal
    )
}

infix fun LinearIntermediateSymbol.gr(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.Greater
    )
}

infix fun LinearIntermediateSymbol.geq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.GreaterEqual
    )
}

infix fun AbstractQuadraticPolynomial<*>.ls(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.Less
    )
}

infix fun AbstractQuadraticPolynomial<*>.leq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun AbstractQuadraticPolynomial<*>.eq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.Equal
    )
}

infix fun AbstractQuadraticPolynomial<*>.neq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun AbstractQuadraticPolynomial<*>.gr(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.Greater
    )
}

infix fun AbstractQuadraticPolynomial<*>.geq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.GreaterEqual
    )
}

infix fun QuadraticIntermediateSymbol.ls(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.Less
    )
}

infix fun QuadraticIntermediateSymbol.leq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.LessEqual
    )
}

infix fun QuadraticIntermediateSymbol.eq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.Equal
    )
}

infix fun QuadraticIntermediateSymbol.neq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.Unequal
    )
}

infix fun QuadraticIntermediateSymbol.gr(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.Greater
    )
}

infix fun QuadraticIntermediateSymbol.geq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.GreaterEqual
    )
}

// quantity polynomial and quantity symbol

@JvmName("quantityLinearPolynomialLsQuantityQuadraticSymbol")
infix fun Quantity<AbstractLinearPolynomial<*>>.ls(rhs: Quantity<QuadraticIntermediateSymbol>): QuadraticInequality {
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

@JvmName("quantityLinearPolynomialLeqQuantityQuadraticSymbol")
infix fun Quantity<AbstractLinearPolynomial<*>>.leq(rhs: Quantity<QuadraticIntermediateSymbol>): QuadraticInequality {
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

@JvmName("quantityLinearPolynomialEqQuantityQuadraticSymbol")
infix fun Quantity<AbstractLinearPolynomial<*>>.eq(rhs: Quantity<QuadraticIntermediateSymbol>): QuadraticInequality {
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

@JvmName("quantityLinearPolynomialNeqQuantityQuadraticSymbol")
infix fun Quantity<AbstractLinearPolynomial<*>>.neq(rhs: Quantity<QuadraticIntermediateSymbol>): QuadraticInequality {
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

@JvmName("quantityLinearPolynomialGrQuantityQuadraticSymbol")
infix fun Quantity<AbstractLinearPolynomial<*>>.gr(rhs: Quantity<QuadraticIntermediateSymbol>): QuadraticInequality {
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

@JvmName("quantityLinearPolynomialGeqQuantityQuadraticSymbol")
infix fun Quantity<AbstractLinearPolynomial<*>>.geq(rhs: Quantity<QuadraticIntermediateSymbol>): QuadraticInequality {
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

@JvmName("quantityQuadraticSymbolLsQuantityLinearPolynomial")
infix fun Quantity<QuadraticIntermediateSymbol>.ls(rhs: Quantity<AbstractLinearPolynomial<*>>): QuadraticInequality {
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

@JvmName("quantityQuadraticSymbolLeqQuantityLinearPolynomial")
infix fun Quantity<QuadraticIntermediateSymbol>.leq(rhs: Quantity<AbstractLinearPolynomial<*>>): QuadraticInequality {
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

@JvmName("quantityQuadraticSymbolEqQuantityLinearPolynomial")
infix fun Quantity<QuadraticIntermediateSymbol>.eq(rhs: Quantity<AbstractLinearPolynomial<*>>): QuadraticInequality {
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

@JvmName("quantityQuadraticSymbolNeqQuantityLinearPolynomial")
infix fun Quantity<QuadraticIntermediateSymbol>.neq(rhs: Quantity<AbstractLinearPolynomial<*>>): QuadraticInequality {
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

@JvmName("quantityQuadraticSymbolGrQuantityLinearPolynomial")
infix fun Quantity<QuadraticIntermediateSymbol>.gr(rhs: Quantity<AbstractLinearPolynomial<*>>): QuadraticInequality {
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

@JvmName("quantityQuadraticSymbolGeqQuantityLinearPolynomial")
infix fun Quantity<QuadraticIntermediateSymbol>.geq(rhs: Quantity<AbstractLinearPolynomial<*>>): QuadraticInequality {
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

@JvmName("quantityQuadraticPolynomialLsQuantityLinearSymbol")
infix fun Quantity<AbstractQuadraticPolynomial<*>>.ls(rhs: Quantity<LinearIntermediateSymbol>): QuadraticInequality {
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

@JvmName("quantityQuadraticPolynomialLeqQuantityLinearSymbol")
infix fun Quantity<AbstractQuadraticPolynomial<*>>.leq(rhs: Quantity<LinearIntermediateSymbol>): QuadraticInequality {
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

@JvmName("quantityQuadraticPolynomialEqQuantityLinearSymbol")
infix fun Quantity<AbstractQuadraticPolynomial<*>>.eq(rhs: Quantity<LinearIntermediateSymbol>): QuadraticInequality {
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

@JvmName("quantityQuadraticPolynomialNeqQuantityLinearSymbol")
infix fun Quantity<AbstractQuadraticPolynomial<*>>.neq(rhs: Quantity<LinearIntermediateSymbol>): QuadraticInequality {
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

@JvmName("quantityQuadraticPolynomialGrQuantityLinearSymbol")
infix fun Quantity<AbstractQuadraticPolynomial<*>>.gr(rhs: Quantity<LinearIntermediateSymbol>): QuadraticInequality {
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

@JvmName("quantityQuadraticPolynomialGeqQuantityLinearSymbol")
infix fun Quantity<AbstractQuadraticPolynomial<*>>.geq(rhs: Quantity<LinearIntermediateSymbol>): QuadraticInequality {
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

@JvmName("quantityLinearSymbolLsQuantityQuadraticPolynomial")
infix fun Quantity<LinearIntermediateSymbol>.ls(rhs: Quantity<AbstractQuadraticPolynomial<*>>): QuadraticInequality {
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

@JvmName("quantityLinearSymbolLeqQuantityQuadraticPolynomial")
infix fun Quantity<LinearIntermediateSymbol>.leq(rhs: Quantity<AbstractQuadraticPolynomial<*>>): QuadraticInequality {
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

@JvmName("quantityLinearSymbolEqQuantityQuadraticPolynomial")
infix fun Quantity<LinearIntermediateSymbol>.eq(rhs: Quantity<AbstractQuadraticPolynomial<*>>): QuadraticInequality {
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

@JvmName("quantityLinearSymbolNeqQuantityQuadraticPolynomial")
infix fun Quantity<LinearIntermediateSymbol>.neq(rhs: Quantity<AbstractQuadraticPolynomial<*>>): QuadraticInequality {
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

@JvmName("quantityLinearSymbolGrQuantityQuadraticPolynomial")
infix fun Quantity<LinearIntermediateSymbol>.gr(rhs: Quantity<AbstractQuadraticPolynomial<*>>): QuadraticInequality {
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

@JvmName("quantityLinearSymbolGeqQuantityQuadraticPolynomial")
infix fun Quantity<LinearIntermediateSymbol>.geq(rhs: Quantity<AbstractQuadraticPolynomial<*>>): QuadraticInequality {
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

@JvmName("quantityQuadraticPolynomialLsQuantityQuadraticSymbol")
infix fun Quantity<AbstractQuadraticPolynomial<*>>.ls(rhs: Quantity<QuadraticIntermediateSymbol>): QuadraticInequality {
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

@JvmName("quantityQuadraticPolynomialLeqQuantityQuadraticSymbol")
infix fun Quantity<AbstractQuadraticPolynomial<*>>.leq(rhs: Quantity<QuadraticIntermediateSymbol>): QuadraticInequality {
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

@JvmName("quantityQuadraticPolynomialEqQuantityQuadraticSymbol")
infix fun Quantity<AbstractQuadraticPolynomial<*>>.eq(rhs: Quantity<QuadraticIntermediateSymbol>): QuadraticInequality {
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

@JvmName("quantityQuadraticPolynomialNeqQuantityQuadraticSymbol")
infix fun Quantity<AbstractQuadraticPolynomial<*>>.neq(rhs: Quantity<QuadraticIntermediateSymbol>): QuadraticInequality {
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

@JvmName("quantityQuadraticPolynomialGrQuantityQuadraticSymbol")
infix fun Quantity<AbstractQuadraticPolynomial<*>>.gr(rhs: Quantity<QuadraticIntermediateSymbol>): QuadraticInequality {
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

@JvmName("quantityQuadraticPolynomialGeqQuantityQuadraticSymbol")
infix fun Quantity<AbstractQuadraticPolynomial<*>>.geq(rhs: Quantity<QuadraticIntermediateSymbol>): QuadraticInequality {
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

@JvmName("quantityQuadraticSymbolLsQuantityQuadraticPolynomial")
infix fun Quantity<QuadraticIntermediateSymbol>.ls(rhs: Quantity<AbstractQuadraticPolynomial<*>>): QuadraticInequality {
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

@JvmName("quantityQuadraticSymbolLeqQuantityQuadraticPolynomial")
infix fun Quantity<QuadraticIntermediateSymbol>.leq(rhs: Quantity<AbstractQuadraticPolynomial<*>>): QuadraticInequality {
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

@JvmName("quantityQuadraticSymbolEqQuantityQuadraticPolynomial")
infix fun Quantity<QuadraticIntermediateSymbol>.eq(rhs: Quantity<AbstractQuadraticPolynomial<*>>): QuadraticInequality {
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

@JvmName("quantityQuadraticSymbolNeqQuantityQuadraticPolynomial")
infix fun Quantity<QuadraticIntermediateSymbol>.neq(rhs: Quantity<AbstractQuadraticPolynomial<*>>): QuadraticInequality {
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

@JvmName("quantityQuadraticSymbolGrQuantityQuadraticPolynomial")
infix fun Quantity<QuadraticIntermediateSymbol>.gr(rhs: Quantity<AbstractQuadraticPolynomial<*>>): QuadraticInequality {
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

@JvmName("quantityQuadraticSymbolGeqQuantityQuadraticPolynomial")
infix fun Quantity<QuadraticIntermediateSymbol>.geq(rhs: Quantity<AbstractQuadraticPolynomial<*>>): QuadraticInequality {
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

infix fun LinearMonomial.ls(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.Less
    )
}

infix fun LinearMonomial.leq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.LessEqual
    )
}

infix fun LinearMonomial.eq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.Equal
    )
}

infix fun LinearMonomial.neq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.Unequal
    )
}

infix fun LinearMonomial.gr(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.Greater
    )
}

infix fun LinearMonomial.geq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.GreaterEqual
    )
}


infix fun QuadraticMonomial.ls(rhs: LinearMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.Less
    )
}

infix fun QuadraticMonomial.leq(rhs: LinearMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun QuadraticMonomial.eq(rhs: LinearMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.Equal
    )
}

infix fun QuadraticMonomial.neq(rhs: LinearMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun QuadraticMonomial.gr(rhs: LinearMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.Greater
    )
}

infix fun QuadraticMonomial.geq(rhs: LinearMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.GreaterEqual
    )
}

infix fun QuadraticMonomial.ls(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs.copy()),
        Sign.Less
    )
}

infix fun QuadraticMonomial.leq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs.copy()),
        Sign.LessEqual
    )
}

infix fun QuadraticMonomial.eq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs.copy()),
        Sign.Equal
    )
}

infix fun QuadraticMonomial.neq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs.copy()),
        Sign.Unequal
    )
}

infix fun QuadraticMonomial.gr(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs.copy()),
        Sign.Greater
    )
}

infix fun QuadraticMonomial.geq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs.copy()),
        Sign.GreaterEqual
    )
}

// quantity monomial and quantity monomial

@JvmName("quantityLinearMonomialLsQuantityQuadraticMonomial")
infix fun Quantity<LinearMonomial>.ls(rhs: Quantity<QuadraticMonomial>): QuadraticInequality {
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

@JvmName("quantityLinearMonomialLeqQuantityQuadraticMonomial")
infix fun Quantity<LinearMonomial>.leq(rhs: Quantity<QuadraticMonomial>): QuadraticInequality {
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

@JvmName("quantityLinearMonomialEqQuantityQuadraticMonomial")
infix fun Quantity<LinearMonomial>.eq(rhs: Quantity<QuadraticMonomial>): QuadraticInequality {
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

@JvmName("quantityLinearMonomialNeqQuantityQuadraticMonomial")
infix fun Quantity<LinearMonomial>.neq(rhs: Quantity<QuadraticMonomial>): QuadraticInequality {
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

@JvmName("quantityLinearMonomialGrQuantityQuadraticMonomial")
infix fun Quantity<LinearMonomial>.gr(rhs: Quantity<QuadraticMonomial>): QuadraticInequality {
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

@JvmName("quantityLinearMonomialGeqQuantityQuadraticMonomial")
infix fun Quantity<LinearMonomial>.geq(rhs: Quantity<QuadraticMonomial>): QuadraticInequality {
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

@JvmName("quantityQuadraticMonomialLsQuantityLinearMonomial")
infix fun Quantity<QuadraticMonomial>.ls(rhs: Quantity<LinearMonomial>): QuadraticInequality {
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

@JvmName("quantityQuadraticMonomialLeqQuantityLinearMonomial")
infix fun Quantity<QuadraticMonomial>.leq(rhs: Quantity<LinearMonomial>): QuadraticInequality {
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

@JvmName("quantityQuadraticMonomialEqQuantityLinearMonomial")
infix fun Quantity<QuadraticMonomial>.eq(rhs: Quantity<LinearMonomial>): QuadraticInequality {
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

@JvmName("quantityQuadraticMonomialNeqQuantityLinearMonomial")
infix fun Quantity<QuadraticMonomial>.neq(rhs: Quantity<LinearMonomial>): QuadraticInequality {
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

@JvmName("quantityQuadraticMonomialGrQuantityLinearMonomial")
infix fun Quantity<QuadraticMonomial>.gr(rhs: Quantity<LinearMonomial>): QuadraticInequality {
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

@JvmName("quantityQuadraticMonomialGeqQuantityLinearMonomial")
infix fun Quantity<QuadraticMonomial>.geq(rhs: Quantity<LinearMonomial>): QuadraticInequality {
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

@JvmName("quantityQuadraticMonomialLsQuantityQuadraticMonomial")
infix fun Quantity<QuadraticMonomial>.ls(rhs: Quantity<QuadraticMonomial>): QuadraticInequality {
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

@JvmName("quantityQuadraticMonomialLeqQuantityQuadraticMonomial")
infix fun Quantity<QuadraticMonomial>.leq(rhs: Quantity<QuadraticMonomial>): QuadraticInequality {
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

@JvmName("quantityQuadraticMonomialEqQuantityQuadraticMonomial")
infix fun Quantity<QuadraticMonomial>.eq(rhs: Quantity<QuadraticMonomial>): QuadraticInequality {
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

@JvmName("quantityQuadraticMonomialNeqQuantityQuadraticMonomial")
infix fun Quantity<QuadraticMonomial>.neq(rhs: Quantity<QuadraticMonomial>): QuadraticInequality {
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

@JvmName("quantityQuadraticMonomialGrQuantityQuadraticMonomial")
infix fun Quantity<QuadraticMonomial>.gr(rhs: Quantity<QuadraticMonomial>): QuadraticInequality {
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

@JvmName("quantityQuadraticMonomialGeqQuantityQuadraticMonomial")
infix fun Quantity<QuadraticMonomial>.geq(rhs: Quantity<QuadraticMonomial>): QuadraticInequality {
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

infix fun AbstractLinearPolynomial<*>.ls(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.Less
    )
}

infix fun AbstractLinearPolynomial<*>.leq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.LessEqual
    )
}

infix fun AbstractLinearPolynomial<*>.eq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.Equal
    )
}

infix fun AbstractLinearPolynomial<*>.neq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.Unequal
    )
}

infix fun AbstractLinearPolynomial<*>.gr(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.Greater
    )
}

infix fun AbstractLinearPolynomial<*>.geq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.GreaterEqual
    )
}

infix fun QuadraticMonomial.ls(rhs: AbstractLinearPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.Less
    )
}

infix fun QuadraticMonomial.leq(rhs: AbstractLinearPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun QuadraticMonomial.eq(rhs: AbstractLinearPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.Equal
    )
}

infix fun QuadraticMonomial.neq(rhs: AbstractLinearPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun QuadraticMonomial.gr(rhs: AbstractLinearPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.Greater
    )
}

infix fun QuadraticMonomial.geq(rhs: AbstractLinearPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.GreaterEqual
    )
}

infix fun AbstractQuadraticPolynomial<*>.ls(rhs: LinearMonomial): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.Less
    )
}

infix fun AbstractQuadraticPolynomial<*>.leq(rhs: LinearMonomial): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun AbstractQuadraticPolynomial<*>.eq(rhs: LinearMonomial): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.Equal
    )
}

infix fun AbstractQuadraticPolynomial<*>.neq(rhs: LinearMonomial): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun AbstractQuadraticPolynomial<*>.gr(rhs: LinearMonomial): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.Greater
    )
}

infix fun AbstractQuadraticPolynomial<*>.geq(rhs: LinearMonomial): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.GreaterEqual
    )
}

infix fun LinearMonomial.ls(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.Less
    )
}

infix fun LinearMonomial.leq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.LessEqual
    )
}

infix fun LinearMonomial.eq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.Equal
    )
}

infix fun LinearMonomial.neq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.Unequal
    )
}

infix fun LinearMonomial.gr(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.Greater
    )
}

infix fun LinearMonomial.geq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.GreaterEqual
    )
}

infix fun AbstractQuadraticPolynomial<*>.ls(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs.copy()),
        Sign.Less
    )
}

infix fun AbstractQuadraticPolynomial<*>.leq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs.copy()),
        Sign.LessEqual
    )
}

infix fun AbstractQuadraticPolynomial<*>.eq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs.copy()),
        Sign.Equal
    )
}

infix fun AbstractQuadraticPolynomial<*>.neq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs.copy()),
        Sign.Unequal
    )
}

infix fun AbstractQuadraticPolynomial<*>.gr(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs.copy()),
        Sign.Greater
    )
}

infix fun AbstractQuadraticPolynomial<*>.geq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs.copy()),
        Sign.GreaterEqual
    )
}

infix fun QuadraticMonomial.ls(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        rhs.copy(),
        Sign.Less
    )
}

infix fun QuadraticMonomial.leq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        rhs.copy(),
        Sign.LessEqual
    )
}

infix fun QuadraticMonomial.eq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        rhs.copy(),
        Sign.Equal
    )
}

infix fun QuadraticMonomial.neq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        rhs.copy(),
        Sign.Unequal
    )
}

infix fun QuadraticMonomial.gr(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        rhs.copy(),
        Sign.Greater
    )
}

infix fun QuadraticMonomial.geq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        rhs.copy(),
        Sign.GreaterEqual
    )
}

// quantity polynomial and quantity monomial

@JvmName("quantityLinearPolynomialLsQuantityQuadraticMonomial")
infix fun Quantity<AbstractLinearPolynomial<*>>.ls(rhs: Quantity<QuadraticMonomial>): QuadraticInequality {
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

@JvmName("quantityLinearPolynomialLeqQuantityQuadraticMonomial")
infix fun Quantity<AbstractLinearPolynomial<*>>.leq(rhs: Quantity<QuadraticMonomial>): QuadraticInequality {
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

@JvmName("quantityLinearPolynomialEqQuantityQuadraticMonomial")
infix fun Quantity<AbstractLinearPolynomial<*>>.eq(rhs: Quantity<QuadraticMonomial>): QuadraticInequality {
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

@JvmName("quantityLinearPolynomialNeqQuantityQuadraticMonomial")
infix fun Quantity<AbstractLinearPolynomial<*>>.neq(rhs: Quantity<QuadraticMonomial>): QuadraticInequality {
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

@JvmName("quantityLinearPolynomialGrQuantityQuadraticMonomial")
infix fun Quantity<AbstractLinearPolynomial<*>>.gr(rhs: Quantity<QuadraticMonomial>): QuadraticInequality {
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

@JvmName("quantityQuadraticMonomialLsQuantityLinearPolynomial")
infix fun Quantity<QuadraticMonomial>.ls(rhs: Quantity<AbstractLinearPolynomial<*>>): QuadraticInequality {
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

@JvmName("quantityQuadraticMonomialLeqQuantityLinearPolynomial")
infix fun Quantity<QuadraticMonomial>.leq(rhs: Quantity<AbstractLinearPolynomial<*>>): QuadraticInequality {
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

@JvmName("quantityQuadraticMonomialEqQuantityLinearPolynomial")
infix fun Quantity<QuadraticMonomial>.eq(rhs: Quantity<AbstractLinearPolynomial<*>>): QuadraticInequality {
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

@JvmName("quantityQuadraticMonomialNeqQuantityLinearPolynomial")
infix fun Quantity<QuadraticMonomial>.neq(rhs: Quantity<AbstractLinearPolynomial<*>>): QuadraticInequality {
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

@JvmName("quantityQuadraticMonomialGrQuantityLinearPolynomial")
infix fun Quantity<QuadraticMonomial>.gr(rhs: Quantity<AbstractLinearPolynomial<*>>): QuadraticInequality {
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

@JvmName("quantityQuadraticMonomialGeqQuantityLinearPolynomial")
infix fun Quantity<QuadraticMonomial>.geq(rhs: Quantity<AbstractLinearPolynomial<*>>): QuadraticInequality {
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

@JvmName("quantityQuadraticPolynomialsQuantityLinearMonomial")
infix fun Quantity<AbstractQuadraticPolynomial<*>>.ls(rhs: Quantity<LinearMonomial>): QuadraticInequality {
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

@JvmName("quantityQuadraticPolynomialLeqQuantityLinearMonomial")
infix fun Quantity<AbstractQuadraticPolynomial<*>>.leq(rhs: Quantity<LinearMonomial>): QuadraticInequality {
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

@JvmName("quantityQuadraticPolynomialEqQuantityLinearMonomial")
infix fun Quantity<AbstractQuadraticPolynomial<*>>.eq(rhs: Quantity<LinearMonomial>): QuadraticInequality {
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

@JvmName("quantityQuadraticPolynomialNeqQuantityLinearMonomial")
infix fun Quantity<AbstractQuadraticPolynomial<*>>.neq(rhs: Quantity<LinearMonomial>): QuadraticInequality {
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

@JvmName("quantityQuadraticPolynomialGrQuantityLinearMonomial")
infix fun Quantity<AbstractQuadraticPolynomial<*>>.gr(rhs: Quantity<LinearMonomial>): QuadraticInequality {
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

@JvmName("quantityQuadraticPolynomialGeqQuantityLinearMonomial")
infix fun Quantity<AbstractQuadraticPolynomial<*>>.geq(rhs: Quantity<LinearMonomial>): QuadraticInequality {
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

@JvmName("quantityLinearMonomialLsQuantityQuadraticPolynomial")
infix fun Quantity<LinearMonomial>.ls(rhs: Quantity<AbstractQuadraticPolynomial<*>>): QuadraticInequality {
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

@JvmName("quantityLinearMonomialLeqQuantityQuadraticPolynomial")
infix fun Quantity<LinearMonomial>.leq(rhs: Quantity<AbstractQuadraticPolynomial<*>>): QuadraticInequality {
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

@JvmName("quantityLinearMonomialEqQuantityQuadraticPolynomial")
infix fun Quantity<LinearMonomial>.eq(rhs: Quantity<AbstractQuadraticPolynomial<*>>): QuadraticInequality {
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

@JvmName("quantityLinearMonomialNeqQuantityQuadraticPolynomial")
infix fun Quantity<LinearMonomial>.neq(rhs: Quantity<AbstractQuadraticPolynomial<*>>): QuadraticInequality {
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

@JvmName("quantityLinearMonomialGrQuantityQuadraticPolynomial")
infix fun Quantity<LinearMonomial>.gr(rhs: Quantity<AbstractQuadraticPolynomial<*>>): QuadraticInequality {
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

@JvmName("quantityLinearMonomialGeqQuantityQuadraticPolynomial")
infix fun Quantity<LinearMonomial>.geq(rhs: Quantity<AbstractQuadraticPolynomial<*>>): QuadraticInequality {
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

@JvmName("quantityQuadraticPolynomialLsQuantityQuadraticMonomial")
infix fun Quantity<AbstractQuadraticPolynomial<*>>.ls(rhs: Quantity<QuadraticMonomial>): QuadraticInequality {
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

@JvmName("quantityQuadraticPolynomialLeqQuantityQuadraticMonomial")
infix fun Quantity<AbstractQuadraticPolynomial<*>>.leq(rhs: Quantity<QuadraticMonomial>): QuadraticInequality {
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

@JvmName("quantityQuadraticPolynomialEqQuantityQuadraticMonomial")
infix fun Quantity<AbstractQuadraticPolynomial<*>>.eq(rhs: Quantity<QuadraticMonomial>): QuadraticInequality {
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

@JvmName("QuantityQuadraticPolynomialNeqQuantityQuadraticMonomial")
infix fun Quantity<AbstractQuadraticPolynomial<*>>.neq(rhs: Quantity<QuadraticMonomial>): QuadraticInequality {
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

@JvmName("quantityQuadraticPolynomialGrQuantityQuadraticMonomial")
infix fun Quantity<AbstractQuadraticPolynomial<*>>.gr(rhs: Quantity<QuadraticMonomial>): QuadraticInequality {
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

@JvmName("quantityQuadraticPolynomialGeqQuantityQuadraticMonomial")
infix fun Quantity<AbstractQuadraticPolynomial<*>>.geq(rhs: Quantity<QuadraticMonomial>): QuadraticInequality {
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

@JvmName("quantityQuadraticMonomialLsQuantityQuadraticPolynomial")
infix fun Quantity<QuadraticMonomial>.ls(rhs: Quantity<AbstractQuadraticPolynomial<*>>): QuadraticInequality {
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

@JvmName("quantityQuadraticMonomialLeqQuantityQuadraticPolynomial")
infix fun Quantity<QuadraticMonomial>.leq(rhs: Quantity<AbstractQuadraticPolynomial<*>>): QuadraticInequality {
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

@JvmName("quantityQuadraticMonomialEqQuantityQuadraticPolynomial")
infix fun Quantity<QuadraticMonomial>.eq(rhs: Quantity<AbstractQuadraticPolynomial<*>>): QuadraticInequality {
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

@JvmName("quantityQuadraticMonomialNeqQuantityQuadraticPolynomial")
infix fun Quantity<QuadraticMonomial>.neq(rhs: Quantity<AbstractQuadraticPolynomial<*>>): QuadraticInequality {
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

@JvmName("quantityQuadraticMonomialGrQuantityQuadraticPolynomial")
infix fun Quantity<QuadraticMonomial>.gr(rhs: Quantity<AbstractQuadraticPolynomial<*>>): QuadraticInequality {
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

@JvmName("quantityQuadraticMonomialGeqQuantityQuadraticPolynomial")
infix fun Quantity<QuadraticMonomial>.geq(rhs: Quantity<AbstractQuadraticPolynomial<*>>): QuadraticInequality {
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

infix fun AbstractLinearPolynomial<*>.ls(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.Less
    )
}

infix fun AbstractLinearPolynomial<*>.leq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.LessEqual
    )
}

infix fun AbstractLinearPolynomial<*>.eq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.Equal
    )
}

infix fun AbstractLinearPolynomial<*>.neq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.Unequal
    )
}

infix fun AbstractLinearPolynomial<*>.gr(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.Greater
    )
}

infix fun AbstractLinearPolynomial<*>.geq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.GreaterEqual
    )
}


infix fun AbstractQuadraticPolynomial<*>.ls(rhs: AbstractLinearPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.Less
    )
}

infix fun AbstractQuadraticPolynomial<*>.leq(rhs: AbstractLinearPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun AbstractQuadraticPolynomial<*>.eq(rhs: AbstractLinearPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.Equal
    )
}

infix fun AbstractQuadraticPolynomial<*>.neq(rhs: AbstractLinearPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun AbstractQuadraticPolynomial<*>.gr(rhs: AbstractLinearPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.Greater
    )
}

infix fun AbstractQuadraticPolynomial<*>.geq(rhs: AbstractLinearPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.GreaterEqual
    )
}

infix fun AbstractQuadraticPolynomial<*>.ls(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        rhs.copy(),
        Sign.Less
    )
}

infix fun AbstractQuadraticPolynomial<*>.leq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        rhs.copy(),
        Sign.LessEqual
    )
}

infix fun AbstractQuadraticPolynomial<*>.eq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        rhs.copy(),
        Sign.Equal
    )
}

infix fun AbstractQuadraticPolynomial<*>.neq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        rhs.copy(),
        Sign.Unequal
    )
}

infix fun AbstractQuadraticPolynomial<*>.gr(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        rhs.copy(),
        Sign.Greater
    )
}

infix fun AbstractQuadraticPolynomial<*>.geq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        rhs.copy(),
        Sign.GreaterEqual
    )
}

// quantity polynomial and quantity polynomial

@JvmName("quantityLinearPolynomialLsQuantityQuadraticPolynomial")
infix fun Quantity<AbstractLinearPolynomial<*>>.ls(rhs: Quantity<AbstractQuadraticPolynomial<*>>): QuadraticInequality {
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

@JvmName("quantityLinearPolynomialLeqQuantityQuadraticPolynomial")
infix fun Quantity<AbstractLinearPolynomial<*>>.leq(rhs: Quantity<AbstractQuadraticPolynomial<*>>): QuadraticInequality {
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

@JvmName("quantityLinearPolynomialEqQuantityQuadraticPolynomial")
infix fun Quantity<AbstractLinearPolynomial<*>>.eq(rhs: Quantity<AbstractQuadraticPolynomial<*>>): QuadraticInequality {
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

@JvmName("quantityLinearPolynomialNeqQuantityQuadraticPolynomial")
infix fun Quantity<AbstractLinearPolynomial<*>>.neq(rhs: Quantity<AbstractQuadraticPolynomial<*>>): QuadraticInequality {
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

@JvmName("quantityLinearPolynomialGrQuantityQuadraticPolynomial")
infix fun Quantity<AbstractLinearPolynomial<*>>.gr(rhs: Quantity<AbstractQuadraticPolynomial<*>>): QuadraticInequality {
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

@JvmName("quantityLinearPolynomialGeqQuantityQuadraticPolynomial")
infix fun Quantity<AbstractLinearPolynomial<*>>.geq(rhs: Quantity<AbstractQuadraticPolynomial<*>>): QuadraticInequality {
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

@JvmName("quantityQuadraticPolynomialLsQuantityLinearPolynomial")
infix fun Quantity<AbstractQuadraticPolynomial<*>>.ls(rhs: Quantity<AbstractLinearPolynomial<*>>): QuadraticInequality {
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

@JvmName("quantityQuadraticPolynomialLeqQuantityLinearPolynomial")
infix fun Quantity<AbstractQuadraticPolynomial<*>>.leq(rhs: Quantity<AbstractLinearPolynomial<*>>): QuadraticInequality {
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

@JvmName("quantityQuadraticPolynomialEqQuantityLinearPolynomial")
infix fun Quantity<AbstractQuadraticPolynomial<*>>.eq(rhs: Quantity<AbstractLinearPolynomial<*>>): QuadraticInequality {
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

@JvmName("quantityQuadraticPolynomialNeqQuantityLinearPolynomial")
infix fun Quantity<AbstractQuadraticPolynomial<*>>.neq(rhs: Quantity<AbstractLinearPolynomial<*>>): QuadraticInequality {
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

@JvmName("quantityQuadraticPolynomialGrQuantityLinearPolynomial")
infix fun Quantity<AbstractQuadraticPolynomial<*>>.gr(rhs: Quantity<AbstractLinearPolynomial<*>>): QuadraticInequality {
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

@JvmName("quantityQuadraticPolynomialGeqQuantityLinearPolynomial")
infix fun Quantity<AbstractQuadraticPolynomial<*>>.geq(rhs: Quantity<AbstractLinearPolynomial<*>>): QuadraticInequality {
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

@JvmName("quantityQuadraticPolynomialLsQuantityQuadraticPolynomial")
infix fun Quantity<AbstractQuadraticPolynomial<*>>.ls(rhs: Quantity<AbstractQuadraticPolynomial<*>>): QuadraticInequality {
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

@JvmName("quantityQuadraticPolynomialLeqQuantityQuadraticPolynomial")
infix fun Quantity<AbstractQuadraticPolynomial<*>>.leq(rhs: Quantity<AbstractQuadraticPolynomial<*>>): QuadraticInequality {
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

@JvmName("quantityQuadraticPolynomialEqQuantityQuadraticPolynomial")
infix fun Quantity<AbstractQuadraticPolynomial<*>>.eq(rhs: Quantity<AbstractQuadraticPolynomial<*>>): QuadraticInequality {
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

@JvmName("quantityQuadraticPolynomialNeqQuantityQuadraticPolynomial")
infix fun Quantity<AbstractQuadraticPolynomial<*>>.neq(rhs: Quantity<AbstractQuadraticPolynomial<*>>): QuadraticInequality {
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

@JvmName("quantityQuadraticPolynomialGrQuantityQuadraticPolynomial")
infix fun Quantity<AbstractQuadraticPolynomial<*>>.gr(rhs: Quantity<AbstractQuadraticPolynomial<*>>): QuadraticInequality {
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

@JvmName("quantityQuadraticPolynomialGeqQuantityQuadraticPolynomial")
infix fun Quantity<AbstractQuadraticPolynomial<*>>.geq(rhs: Quantity<AbstractQuadraticPolynomial<*>>): QuadraticInequality {
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
