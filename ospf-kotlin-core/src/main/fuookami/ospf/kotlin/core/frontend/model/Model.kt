package fuookami.ospf.kotlin.core.frontend.model

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

typealias Solution = List<Flt64>

interface Model {
    val objectCategory: ObjectCategory

    fun add(item: AbstractVariableItem<*, *>): Try
    fun add(items: Iterable<AbstractVariableItem<*, *>>): Try
    fun remove(item: AbstractVariableItem<*, *>)

    fun addObject(
        category: ObjectCategory,
        variable: AbstractVariableItem<*, *>,
        name: String? = null,
        displayName: String? = null
    ): Try

    fun <T : RealNumber<T>> addObject(
        category: ObjectCategory,
        constant: T,
        name: String? = null,
        displayName: String? = null
    ): Try

    fun minimize(
        variable: AbstractVariableItem<*, *>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(ObjectCategory.Minimum, variable, name, displayName)
    }

    fun maximize(
        variable: AbstractVariableItem<*, *>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(ObjectCategory.Maximum, variable, name, displayName)
    }

    fun <T : RealNumber<T>> minimize(
        constant: T,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(ObjectCategory.Minimum, constant, name, displayName)
    }

    fun <T : RealNumber<T>> maximize(
        constant: T,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(ObjectCategory.Maximum, constant, name, displayName)
    }

    fun setSolution(solution: Solution)
    fun setSolution(solution: Map<AbstractVariableItem<*, *>, Flt64>)
    fun clearSolution()
}

interface LinearModel : Model {
    fun addConstraint(
        constraint: LinearLogicFunctionSymbol,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addConstraint(constraint eq Flt64.one, name, displayName)
    }

    fun addConstraint(
        constraint: LinearInequality,
        name: String? = null,
        displayName: String? = null
    ): Try

    fun partition(
        polynomial: AbstractLinearPolynomial<*>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addConstraint(polynomial eq Flt64.one, name, displayName)
    }

    override fun addObject(
        category: ObjectCategory,
        variable: AbstractVariableItem<*, *>,
        name: String?,
        displayName: String?
    ): Try {
        return addObject(category, LinearPolynomial(variable), name, displayName)
    }

    override fun <T : RealNumber<T>> addObject(
        category: ObjectCategory,
        constant: T,
        name: String?,
        displayName: String?
    ): Try {
        return addObject(category, LinearPolynomial(constant), name, displayName)
    }

    fun addObject(
        category: ObjectCategory,
        monomial: LinearMonomial,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(category, LinearPolynomial(monomial), name, displayName)
    }

    fun addObject(
        category: ObjectCategory,
        symbol: LinearIntermediateSymbol,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(category, LinearPolynomial(symbol), name, displayName)
    }

    fun addObject(
        category: ObjectCategory,
        polynomial: AbstractLinearPolynomial<*>,
        name: String? = null,
        displayName: String? = null
    ): Try

    fun minimize(
        symbol: LinearMonomial,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(ObjectCategory.Minimum, symbol, name, displayName)
    }

    fun maximize(
        monomial: LinearMonomial,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(ObjectCategory.Maximum, monomial, name, displayName)
    }

    fun minimize(
        monomial: LinearIntermediateSymbol,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(ObjectCategory.Minimum, monomial, name, displayName)
    }

    fun maximize(
        symbol: LinearIntermediateSymbol,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(ObjectCategory.Maximum, symbol, name, displayName)
    }

    fun minimize(
        polynomial: AbstractLinearPolynomial<*>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(ObjectCategory.Minimum, polynomial, name, displayName)
    }

    fun maximize(
        polynomial: AbstractLinearPolynomial<*>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(ObjectCategory.Maximum, polynomial, name, displayName)
    }
}

interface QuadraticModel : LinearModel {
    override fun addConstraint(
        constraint: LinearInequality,
        name: String?,
        displayName: String?
    ): Try {
        return addConstraint(QuadraticInequality(constraint), name, displayName)
    }

    fun addConstraint(
        constraint: QuadraticFunctionSymbol,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addConstraint(constraint eq Flt64.one, name, displayName)
    }

    fun addConstraint(
        constraint: QuadraticInequality,
        name: String? = null,
        displayName: String? = null
    ): Try

    fun partition(
        polynomial: AbstractQuadraticPolynomial<*>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addConstraint(polynomial eq Flt64.one, name, displayName)
    }

    override fun addObject(
        category: ObjectCategory,
        variable: AbstractVariableItem<*, *>,
        name: String?,
        displayName: String?
    ): Try {
        return addObject(category, QuadraticPolynomial(variable), name, displayName)
    }

    override fun <T : RealNumber<T>> addObject(
        category: ObjectCategory,
        constant: T,
        name: String?,
        displayName: String?
    ): Try {
        return addObject(category, QuadraticPolynomial(constant), name, displayName)
    }

    override fun addObject(
        category: ObjectCategory,
        monomial: LinearMonomial,
        name: String?,
        displayName: String?
    ): Try {
        return addObject(category, QuadraticPolynomial(monomial), name, displayName)
    }

    override fun addObject(
        category: ObjectCategory,
        symbol: LinearIntermediateSymbol,
        name: String?,
        displayName: String?
    ): Try {
        return addObject(category, QuadraticPolynomial(symbol), name, displayName)
    }

    override fun addObject(
        category: ObjectCategory,
        polynomial: AbstractLinearPolynomial<*>,
        name: String?,
        displayName: String?
    )
            : Try {
        return addObject(category, QuadraticPolynomial(polynomial), name, displayName)
    }

    fun addObject(
        category: ObjectCategory,
        monomial: QuadraticMonomial,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(category, QuadraticPolynomial(monomial), name, displayName)
    }

    fun addObject(
        category: ObjectCategory,
        symbol: QuadraticIntermediateSymbol,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(category, QuadraticPolynomial(symbol), name, displayName)
    }

    fun addObject(
        category: ObjectCategory,
        polynomial: AbstractQuadraticPolynomial<*>,
        name: String? = null,
        displayName: String? = null
    ): Try

    fun minimize(
        monomial: QuadraticMonomial,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(ObjectCategory.Minimum, monomial, name, displayName)
    }

    fun maximize(
        monomial: QuadraticMonomial,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(ObjectCategory.Maximum, monomial, name, displayName)
    }

    fun minimize(
        symbol: QuadraticIntermediateSymbol,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(ObjectCategory.Minimum, symbol, name, displayName)
    }

    fun maximize(
        symbol: QuadraticIntermediateSymbol,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(ObjectCategory.Maximum, symbol, name, displayName)
    }

    fun minimize(
        polynomial: AbstractQuadraticPolynomial<*>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(ObjectCategory.Minimum, polynomial, name, displayName)
    }

    fun maximize(
        polynomial: AbstractQuadraticPolynomial<*>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(ObjectCategory.Maximum, polynomial, name, displayName)
    }
}
