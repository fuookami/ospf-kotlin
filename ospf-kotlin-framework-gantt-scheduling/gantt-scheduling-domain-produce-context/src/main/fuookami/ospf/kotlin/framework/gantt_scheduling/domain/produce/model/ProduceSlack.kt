package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model

import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.function.LinearFunctionSymbolAdapter
import fuookami.ospf.kotlin.core.intermediate_symbol.function.SlackFunction
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.VariableTypeKind
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial

internal fun produceConstantPolynomial(value: Flt64): LinearPolynomial<Flt64> {
    return LinearPolynomial(emptyList(), value)
}

internal fun produceSlack(
    x: LinearIntermediateSymbol<Flt64>,
    threshold: Flt64,
    type: VariableTypeKind,
    withNegative: Boolean,
    withPositive: Boolean,
    constraint: Boolean = true,
    name: String
): LinearFunctionSymbolAdapter<Flt64> {
    return LinearFunctionSymbolAdapter(
        delegate = SlackFunction(
            x = x.toLinearPolynomial(),
            y = produceConstantPolynomial(threshold),
            type = type,
            withNegative = withNegative,
            withPositive = withPositive,
            threshold = true,
            constraint = constraint,
            converter = IntoValue.Identity,
            name = name
        ),
        converter = IntoValue.Identity
    )
}
