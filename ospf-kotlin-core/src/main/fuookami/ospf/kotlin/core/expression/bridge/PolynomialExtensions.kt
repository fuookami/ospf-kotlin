package fuookami.ospf.kotlin.core.expression.bridge

import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial

/**
 * 从多项式中提取 IntermediateSymbol 依赖集合
 */
val LinearPolynomial<Flt64>.dependencies: Set<LinearIntermediateSymbol>
    get() {
        return monomials.mapNotNull { monomial ->
            (monomial.symbol as? LinearIntermediateSymbol)
        }.toSet()
    }

/**
 * 从单项式列表中提取符号
 */
fun List<LinearMonomial<Flt64>>.extractSymbols(): Set<Symbol> {
    return map { it.symbol }.toSet()
}
