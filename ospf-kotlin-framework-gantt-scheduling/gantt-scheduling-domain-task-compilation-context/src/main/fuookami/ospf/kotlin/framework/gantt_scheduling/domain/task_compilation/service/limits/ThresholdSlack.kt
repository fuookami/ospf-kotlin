package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.service.limits

import fuookami.ospf.kotlin.core.symbol.function.LinearFunctionSymbolAdapter
import fuookami.ospf.kotlin.core.symbol.function.SlackFunction
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.VariableTypeKind
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.operation.ToLinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial

internal fun thresholdSlack(
    x: ToLinearPolynomial<Flt64>,
    threshold: Flt64,
    type: VariableTypeKind,
    name: String
): LinearFunctionSymbolAdapter<Flt64> {
    return LinearFunctionSymbolAdapter(
        delegate = SlackFunction(
            x = x.toLinearPolynomial(),
            y = LinearPolynomial(emptyList(), threshold),
            type = type,
            withNegative = false,
            withPositive = true,
            threshold = true,
            converter = IntoValue.Identity,
            name = name
        ),
        converter = IntoValue.Identity
    )
}

internal fun LinearFunctionSymbolAdapter<Flt64>.positiveSlackPolynomial(): LinearPolynomial<Flt64> {
    return requireNotNull(pos) { "threshold slack requires a positive slack polynomial" }
}

internal fun LinearFunctionSymbolAdapter<Flt64>.thresholdCappedPolynomial(): LinearPolynomial<Flt64> {
    return requireNotNull(polyX) { "threshold slack requires a capped polynomial" }
}
