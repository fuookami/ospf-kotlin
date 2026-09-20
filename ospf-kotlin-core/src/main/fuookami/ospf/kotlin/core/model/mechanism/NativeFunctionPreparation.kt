package fuookami.ospf.kotlin.core.model.mechanism

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.solver.prepareNativeMax
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.VariableItemKey

internal fun nativeFunctionSelectorKeys(
    model: LinearMechanismModel<Flt64>,
    nativeFunctionKeys: Set<VariableItemKey>,
    fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>?
): Ret<Set<VariableItemKey>> {
    if (nativeFunctionKeys.isEmpty()) {
        return Ok(emptySet())
    }
    fun invalid(reason: String): Ret<Set<VariableItemKey>> = Failed(
        ErrorCode.IllegalArgument,
        "无法省略原生函数的辅助变量：$reason / Cannot omit native function helpers: $reason"
    )
    if (model.functionExpansionPolicy == FunctionExpansionPolicy.EAGER) {
        return invalid("EAGER model")
    }
    val structures = model.deferredFunctionStructures.filter { it.supportsDeferredFallback() }
    val selected = structures.filter { it.resultVariableOrNull()?.key in nativeFunctionKeys }
    for (structure in selected.filterIsInstance<MaskingStructure<*>>()) {
        val retained = listOf(structure.resultVariable, structure.mask)
        if (retained.any { variable -> fixedVariables?.keys?.any { it.key == variable.key } == true ||
                model.tokens.tokens.none { it.key == variable.key } }
        ) return invalid("masking columns would be missing or substituted out")
        when (val prepared = fuookami.ospf.kotlin.core.solver.prepareNativeMasking(structure)) {
            is Ok -> Unit
            is Failed -> return invalid(prepared.error.message)
            is Fatal -> return Fatal(prepared.errors)
        }
    }
    for (structure in selected.filterIsInstance<IndicatorStructure<*>>()) {
        val publicResults = structure.retainedResultVariables
        if (publicResults.any { variable -> fixedVariables?.keys?.any { it.key == variable.key } == true }) {
            return invalid("indicator result columns would be substituted out")
        }
        when (val prepared = fuookami.ospf.kotlin.core.solver.prepareNativeIndicator(structure)) {
            is Ok -> Unit
            is Failed -> return invalid(prepared.error.message)
            is Fatal -> return Fatal(prepared.errors)
        }
    }
    if (selected.size != nativeFunctionKeys.size) {
        return invalid("unknown or duplicate function key")
    }
    if (model.deferredFunctionConstraintRegions.any {
            it.structure.resultVariableOrNull()?.key in nativeFunctionKeys
        }
    ) {
        return invalid("fallback rows already exist")
    }
    val helperEntries = selected.flatMap { structure ->
        structure.helperVariablesOrEmpty().map { helper -> structure to helper }
    }
    val helperKeys = helperEntries.map { it.second.key }
    val helperKeySet = helperKeys.toSet()
    if (helperKeys.size != helperKeySet.size
    ) {
        return invalid("helper ownership is shared between selected functions")
    }
    if (structures.filter { it.resultVariableOrNull()?.key !in nativeFunctionKeys }.any { structure ->
            structure.helperVariablesOrEmpty().any { it.key in helperKeySet }
        }
    ) {
        return invalid("helper ownership is shared between functions")
    }
    if (structures.flatMap { structure ->
            (structure as? IndicatorStructure<*>)?.retainedResultVariables ?: listOfNotNull(structure.resultVariableOrNull())
        }.any { it.key in helperKeySet }
    ) {
        return invalid("a helper key is also used as a function result")
    }
    val tokensByKey = model.tokens.tokens.associateBy { it.key }
    if (selected.filterIsInstance<IndicatorStructure<*>>().any { structure ->
            structure.retainedResultVariables.any { it.key !in tokensByKey }
        }
    ) {
        return invalid("equivalent result token is missing")
    }
    for (structure in selected) {
        if (structure.resultVariableOrNull()?.key !in tokensByKey) {
            return invalid("result token is missing")
        }
    }
    for (key in helperKeySet) {
        val token = tokensByKey[key] ?: return invalid("selector token is missing")
        if (token.result != null || fixedVariables?.keys?.any { it.key == key } == true ||
            token.lowerBound == null || token.upperBound == null
        ) {
            return invalid("helper has an external value or incomplete bounds")
        }
    }
    if (model.linearConstraints.any { constraint ->
            constraint.lhs.any { it.token.key in helperKeySet && it.coefficient != Flt64.zero }
        } || model.objectFunction.subObjects.any { objective ->
            objective.linearTerms().any { (coefficient, variable) ->
                coefficient != Flt64.zero && variable.key in helperKeySet
            }
        }
    ) {
        return invalid("helper is referenced by an ordinary row or objective")
    }
    if (structures.any { structure ->
            structure.inputPolynomialsOrEmpty().any { input ->
                input.monomials.any {
                    it.symbol !is AbstractVariableItem<*, *> ||
                        (it.symbol as AbstractVariableItem<*, *>).key in helperKeySet
                }
            }
        }
    ) {
        return invalid("helper is referenced by a function input")
    }

    for (structure in selected.filterIsInstance<UnivariateLinearPiecewiseStructure<*>>()) {
        val selectorKeys = structure.selectorVariables.map { it.key }.toSet()
        if (selectorKeys.any { key ->
                val token = tokensByKey[key] ?: return invalid("selector token is missing")
                token.lowerBound?.value?.unwrap() != Flt64.zero ||
                    token.upperBound?.value?.unwrap() != Flt64.one
            }
        ) {
            return invalid("selector has restricted bounds")
        }
    }

    for (structure in selected.filterIsInstance<MaxStructure<*>>()) {
        when (val prepared = prepareNativeMax(structure)) {
            is Ok -> {}
            is Failed -> return invalid(prepared.error.message)
            is Fatal -> return Fatal(prepared.errors)
        }
        if (structure.selectorVariables.any { selector ->
                val token = tokensByKey[selector.key] ?: return invalid("selector token is missing")
                token.lowerBound?.value?.unwrap() != Flt64.zero ||
                    token.upperBound?.value?.unwrap() != Flt64.one
            }
        ) {
            return invalid("selector has restricted bounds")
        }
        for (input in structure.inputs) {
            for (monomial in input.monomials) {
                val variable = monomial.symbol as? AbstractVariableItem<*, *>
                    ?: return invalid("MAX input contains an unknown symbol")
                val token = tokensByKey[variable.key] ?: return invalid("MAX input token is missing")
                val variableLower = variable.lowerBound?.value?.unwrap()
                    ?: return invalid("MAX input lower bound is not proven")
                val variableUpper = variable.upperBound?.value?.unwrap()
                    ?: return invalid("MAX input upper bound is not proven")
                val tokenLower = token.lowerBound?.value?.unwrap()
                    ?: return invalid("MAX input token lower bound is not proven")
                val tokenUpper = token.upperBound?.value?.unwrap()
                    ?: return invalid("MAX input token upper bound is not proven")
                if (variableLower == Flt64.minimum || variableUpper == Flt64.maximum ||
                    tokenLower == Flt64.minimum || tokenUpper == Flt64.maximum ||
                    tokenLower.compareTo(variableLower) < 0 ||
                    tokenUpper.compareTo(variableUpper) > 0
                ) {
                    return invalid("MAX input token range exceeds the variable proof")
                }
            }
        }
    }


    for (structure in selected.filterIsInstance<AbsStructure<*>>()) {
        val inputKey = structure.inputVariableKeyOrNull()
            ?: return invalid("ABS input must be 1 * variable + 0")
        val inputToken = tokensByKey[inputKey] ?: return invalid("ABS input token is missing")
        val inputLower = inputToken.lowerBound?.value?.unwrap()
            ?: return invalid("ABS input lower bound is not proven")
        val inputUpper = inputToken.upperBound?.value?.unwrap()
            ?: return invalid("ABS input upper bound is not proven")
        if (!inputLower.isFinite() || !inputUpper.isFinite() || inputLower > inputUpper) {
            return invalid("ABS input bounds are not finite")
        }
        val bigM = structure.solverBigMOrNull()
            ?: return invalid("ABS Big-M is not representable")
        val positiveBigM = bigM.first
        val negativeBigM = bigM.second
        if (!positiveBigM.isFinite() || !negativeBigM.isFinite() ||
            positiveBigM < Flt64.zero || negativeBigM < Flt64.zero
        ) {
            return invalid("ABS Big-M is not finite and nonnegative")
        }
        if (inputUpper > positiveBigM || inputLower < -negativeBigM) {
            return invalid("ABS Big-M does not cover the current input range")
        }

        val requiredPositiveUpper = if (inputUpper > Flt64.zero) inputUpper else Flt64.zero
        val requiredNegativeUpper = if (inputLower < Flt64.zero) -inputLower else Flt64.zero
        val requiredBounds = listOf(
            structure.positiveVariable to (Flt64.zero to requiredPositiveUpper),
            structure.negativeVariable to (Flt64.zero to requiredNegativeUpper),
            structure.signVariable to (Flt64.zero to Flt64.one)
        )
        for ((helper, required) in requiredBounds) {
            val original = structure.capturedHelperBounds[helper.key]
                ?: return invalid("ABS helper original bounds are missing")
            val current = helperBounds(helper)
            if (current != original) {
                return invalid("ABS helper bounds were externally restricted")
            }
            val currentLower = current.lower ?: return invalid("ABS helper lower bound is not proven")
            val currentUpper = current.upper ?: return invalid("ABS helper upper bound is not proven")
            if (currentLower > required.first || currentUpper < required.second) {
                return invalid("ABS helper bounds do not cover the required range")
            }
        }
    }

    for (structure in selected.filterIsInstance<SemiStructure<*>>()) {
        val lower = structure.converter.fromValue(structure.lowerBound)
        val upper = structure.converter.fromValue(structure.upperBound)
        if (!lower.isFinite() || !upper.isFinite() || lower <= Flt64.zero || lower > upper) {
            return invalid("SEMICONT requires finite 0 < lb <= ub")
        }
        val resultToken = tokensByKey[structure.resultVariable.key] ?: return invalid("semi result token is missing")
        if (resultToken.lowerBound == null || resultToken.upperBound == null) {
            return invalid("semi result bounds are incomplete")
        }
    }
    return Ok(helperKeySet)
}

private fun helperBounds(variable: AbstractVariableItem<*, *>): AbsHelperBoundsSnapshot {
    return AbsHelperBoundsSnapshot(
        lower = variable.lowerBound?.value?.unwrap(),
        upper = variable.upperBound?.value?.unwrap()
    )
}
