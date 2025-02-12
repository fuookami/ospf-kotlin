package fuookami.ospf.kotlin.core.frontend.expression.symbol

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.physics.unit.*
import fuookami.ospf.kotlin.utils.physics.quantity.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

interface IntermediateSymbol : Expression {
    val category: Category
    val operationCategory: Category get() = category

    val cached: Boolean
    val dependencies: Set<IntermediateSymbol>

    fun flush(force: Boolean = false)
    fun prepare(tokenTable: AbstractTokenTable)

    fun toRawString(unfold: Boolean = false): String
}

interface LinearIntermediateSymbol : IntermediateSymbol {
    val cells: List<LinearMonomialCell>
}

interface QuadraticIntermediateSymbol : IntermediateSymbol {
    val cells: List<QuadraticMonomialCell>
}

abstract class ExpressionSymbol(
    open val _polynomial: MutablePolynomial<*, *, *>,
    override val category: Category = _polynomial.category,
    override var name: String = "",
    override var displayName: String? = null
) : IntermediateSymbol {
    open val polynomial: Polynomial<*, *, *> by ::_polynomial

    open fun asMutable(): MutablePolynomial<*, *, *> {
        return _polynomial
    }

    override val discrete get() = polynomial.discrete

    override val range get() = polynomial.range
    override val lowerBound get() = polynomial.lowerBound
    override val upperBound get() = polynomial.upperBound

    override val dependencies get() = polynomial.dependencies
    override val cached get() = polynomial.cached

    override fun flush(force: Boolean) {
        polynomial.flush(force)
    }

    override fun toRawString(unfold: Boolean): String {
        return "(${polynomial.toRawString(unfold)})"
    }

    override fun evaluate(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return polynomial.evaluate(tokenList, zeroIfNone)
    }

    override fun evaluate(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return polynomial.evaluate(results, tokenList, zeroIfNone)
    }

    override fun evaluate(tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        return if (tokenTable.cachedSolution && tokenTable.cached(this, null) == false) {
            for (dependency in dependencies) {
                if (tokenTable.cachedSolution && tokenTable.cached(dependency, null) == false) {
                    dependency.evaluate(tokenTable, zeroIfNone)
                }
            }
            val value = polynomial.evaluate(tokenTable, zeroIfNone) ?: return null
            tokenTable.cache(this, null, value)
        } else {
            tokenTable.cachedValue(this, null)
        }
    }

    override fun evaluate(results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        return if (tokenTable.cached(this, results) == false) {
            for (dependency in dependencies) {
                if (tokenTable.cached(dependency, results) == false) {
                    dependency.evaluate(tokenTable, zeroIfNone)
                }
            }
            val value = polynomial.evaluate(results, tokenTable, zeroIfNone) ?: return null
            tokenTable.cache(this, results, value)
        } else {
            tokenTable.cachedValue(this, results)
        }
    }

    override fun toString(): String {
        return displayName ?: name
    }
}

class LinearExpressionSymbol(
    override val _polynomial: MutableLinearPolynomial,
    category: Category = _polynomial.category,
    name: String = "",
    displayName: String? = null
) : LinearIntermediateSymbol, ExpressionSymbol(_polynomial, category, name, displayName) {
    companion object {
        operator fun invoke(
            item: AbstractVariableItem<*, *>,
            name: String = "",
            displayName: String? = null
        ): LinearExpressionSymbol {
            return LinearExpressionSymbol(
                MutableLinearPolynomial(
                    monomials = mutableListOf(LinearMonomial(item)),
                    name = name.ifEmpty { item.name },
                    displayName = displayName
                ),
                name = name.ifEmpty { item.name },
                displayName = displayName
            )
        }

        operator fun invoke(
            symbol: LinearIntermediateSymbol,
            name: String = "",
            displayName: String? = null
        ): LinearExpressionSymbol {
            return LinearExpressionSymbol(
                MutableLinearPolynomial(
                    monomials = mutableListOf(LinearMonomial(symbol)),
                    name = name.ifEmpty { symbol.name },
                    displayName = displayName ?: symbol.displayName
                ),
                name = name.ifEmpty { symbol.name },
                displayName = displayName ?: symbol.displayName
            )
        }

        operator fun invoke(
            monomial: LinearMonomial,
            name: String = "",
            displayName: String? = null
        ): LinearExpressionSymbol {
            return LinearExpressionSymbol(
                MutableLinearPolynomial(
                    monomials = mutableListOf(monomial.copy()),
                    name = name.ifEmpty { monomial.name },
                    displayName = displayName ?: monomial.displayName
                ),
                name = name.ifEmpty { monomial.name },
                displayName = displayName ?: monomial.displayName
            )
        }

        operator fun invoke(
            polynomial: AbstractLinearPolynomial<*>,
            name: String = "",
            displayName: String? = null
        ): LinearExpressionSymbol {
            return LinearExpressionSymbol(
                MutableLinearPolynomial(
                    monomials = polynomial.monomials.map { it.copy() }.toMutableList(),
                    constant = polynomial.constant,
                    name = name.ifEmpty { polynomial.name },
                    displayName = displayName ?: polynomial.displayName
                ),
                name = name.ifEmpty { polynomial.name },
                displayName = displayName ?: polynomial.displayName
            )
        }

        operator fun <T : RealNumber<T>> invoke(
            constant: T,
            name: String = "",
            displayName: String? = null
        ): LinearExpressionSymbol {
            return LinearExpressionSymbol(
                MutableLinearPolynomial(
                    monomials = mutableListOf(),
                    constant = constant.toFlt64(),
                    name = name,
                    displayName = displayName
                ),
                name = name,
                displayName = displayName
            )
        }
    }

    override val operationCategory: Category = Linear
    override val polynomial: AbstractLinearPolynomial<*> get() = _polynomial
    override val cells by _polynomial::cells

    override fun asMutable(): MutableLinearPolynomial {
        return _polynomial
    }

    override fun prepare(tokenTable: AbstractTokenTable) {
        cells
    }
}

class QuadraticExpressionSymbol(
    override val _polynomial: MutableQuadraticPolynomial,
    category: Category = _polynomial.category,
    name: String = "",
    displayName: String? = null
) : QuadraticIntermediateSymbol, ExpressionSymbol(_polynomial, category, name, displayName) {
    companion object {
        operator fun invoke(
            item: AbstractVariableItem<*, *>,
            name: String = "",
            displayName: String? = null
        ): QuadraticExpressionSymbol {
            return QuadraticExpressionSymbol(
                MutableQuadraticPolynomial(
                    monomials = mutableListOf(QuadraticMonomial(item)),
                    name = name.ifEmpty { item.name },
                    displayName = displayName
                ),
                name = name.ifEmpty { item.name },
                displayName = displayName
            )
        }

        operator fun invoke(
            symbol: LinearIntermediateSymbol,
            name: String = "",
            displayName: String? = null
        ): QuadraticExpressionSymbol {
            return QuadraticExpressionSymbol(
                MutableQuadraticPolynomial(
                    monomials = mutableListOf(QuadraticMonomial(symbol)),
                    name = name.ifEmpty { symbol.name },
                    displayName = displayName ?: symbol.displayName
                ),
                name = name.ifEmpty { symbol.name },
                displayName = displayName ?: symbol.displayName
            )
        }

        operator fun invoke(
            symbol: QuadraticIntermediateSymbol,
            name: String = "",
            displayName: String? = null
        ): QuadraticExpressionSymbol {
            return QuadraticExpressionSymbol(
                MutableQuadraticPolynomial(
                    monomials = mutableListOf(QuadraticMonomial(symbol)),
                    name = name.ifEmpty { symbol.name },
                    displayName = displayName ?: symbol.displayName
                ),
                name = name.ifEmpty { symbol.name },
                displayName = displayName ?: symbol.displayName
            )
        }

        operator fun invoke(
            monomial: LinearMonomial,
            name: String = "",
            displayName: String? = null
        ): QuadraticExpressionSymbol {
            return QuadraticExpressionSymbol(
                MutableQuadraticPolynomial(
                    monomials = mutableListOf(QuadraticMonomial(monomial)),
                    name = name.ifEmpty { monomial.name },
                    displayName = displayName ?: monomial.displayName
                ),
                name = name.ifEmpty { monomial.name },
                displayName = displayName ?: monomial.displayName
            )
        }

        operator fun invoke(
            monomial: QuadraticMonomial,
            name: String = "",
            displayName: String? = null
        ): QuadraticExpressionSymbol {
            return QuadraticExpressionSymbol(
                MutableQuadraticPolynomial(
                    monomials = mutableListOf(monomial.copy()),
                    name = name.ifEmpty { monomial.name },
                    displayName = displayName ?: monomial.displayName
                ),
                name = name.ifEmpty { monomial.name },
                displayName = displayName ?: monomial.displayName
            )
        }

        operator fun invoke(
            polynomial: AbstractLinearPolynomial<*>,
            name: String = "",
            displayName: String? = null
        ): QuadraticExpressionSymbol {
            return QuadraticExpressionSymbol(
                MutableQuadraticPolynomial(
                    monomials = polynomial.monomials.map { QuadraticMonomial(it) }.toMutableList(),
                    constant = polynomial.constant,
                    name = name.ifEmpty { polynomial.name },
                    displayName = displayName ?: polynomial.displayName
                ),
                name = name.ifEmpty { polynomial.name },
                displayName = displayName ?: polynomial.displayName
            )
        }

        operator fun invoke(
            polynomial: AbstractQuadraticPolynomial<*>,
            name: String = "",
            displayName: String? = null
        ): QuadraticExpressionSymbol {
            return QuadraticExpressionSymbol(
                MutableQuadraticPolynomial(
                    monomials = polynomial.monomials.map { it.copy() }.toMutableList(),
                    constant = polynomial.constant,
                    name = name.ifEmpty { polynomial.name },
                    displayName = displayName ?: polynomial.displayName
                ),
                name = name.ifEmpty { polynomial.name },
                displayName = displayName ?: polynomial.displayName
            )
        }

        operator fun <T : RealNumber<T>> invoke(
            constant: T,
            name: String = "",
            displayName: String? = null
        ): QuadraticExpressionSymbol {
            return QuadraticExpressionSymbol(
                MutableQuadraticPolynomial(
                    monomials = mutableListOf(),
                    constant = constant.toFlt64(),
                    name = name,
                    displayName = displayName
                ),
                name = name,
                displayName = displayName
            )
        }
    }

    override val operationCategory: Category = Quadratic
    override val polynomial: AbstractQuadraticPolynomial<*> get() = _polynomial
    override val cells by _polynomial::cells

    override fun asMutable(): MutableQuadraticPolynomial {
        return _polynomial
    }

    override fun prepare(tokenTable: AbstractTokenTable) {
        cells
    }
}

interface FunctionSymbol : IntermediateSymbol {
    fun register(tokenTable: AbstractMutableTokenTable): Try

    override fun evaluate(tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        return if (tokenTable.cachedSolution && tokenTable.cached(this, null) == false) {
            for (dependency in dependencies) {
                if (tokenTable.cached(dependency, null) == false) {
                    dependency.evaluate(tokenTable, zeroIfNone)
                }
            }
            val value = calculateValue(tokenTable, zeroIfNone) ?: return null
            tokenTable.cache(this, null, value)
        } else {
            tokenTable.cachedValue(this, null)
        }
    }

    override fun evaluate(results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        return if (tokenTable.cached(this, results) == false) {
            for (dependency in dependencies) {
                if (tokenTable.cached(dependency, results) == false) {
                    dependency.evaluate(tokenTable, zeroIfNone)
                }
            }
            val value = calculateValue(results, tokenTable, zeroIfNone) ?: return null
            tokenTable.cache(this, results, value)
        } else {
            tokenTable.cachedValue(this, results)
        }
    }

    fun calculateValue(tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64?
    fun calculateValue(results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64?
}

interface LogicFunctionSymbol : FunctionSymbol {
    fun isTrue(tokenList: AbstractTokenList, zeroIfNone: Boolean): Boolean? {
        return this.evaluate(tokenList, zeroIfNone)?.let { it eq Flt64.one }
    }

    fun isTrue(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Boolean? {
        return this.evaluate(results, tokenList, zeroIfNone)?.let { it eq Flt64.one }
    }
}

interface LinearFunctionSymbol : LinearIntermediateSymbol, FunctionSymbol {
    fun register(model: AbstractLinearMechanismModel): Try
}

interface LinearLogicFunctionSymbol : LinearFunctionSymbol, LogicFunctionSymbol {}

interface QuadraticFunctionSymbol : QuadraticIntermediateSymbol, FunctionSymbol {
    fun register(model: AbstractQuadraticMechanismModel): Try
}

interface QuadraticLogicFunctionSymbol : QuadraticFunctionSymbol, LogicFunctionSymbol {}

operator fun LinearIntermediateSymbol.times(rhs: PhysicalUnit): Quantity<LinearIntermediateSymbol> {
    return Quantity(this, rhs)
}

operator fun LinearIntermediateSymbol.div(rhs: PhysicalUnit): Quantity<LinearIntermediateSymbol> {
    return Quantity(this, rhs.reciprocal())
}

operator fun QuadraticIntermediateSymbol.times(rhs: PhysicalUnit): Quantity<QuadraticIntermediateSymbol> {
    return Quantity(this, rhs)
}

operator fun QuadraticIntermediateSymbol.div(rhs: PhysicalUnit): Quantity<QuadraticIntermediateSymbol> {
    return Quantity(this, rhs.reciprocal())
}
