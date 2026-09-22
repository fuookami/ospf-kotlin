package fuookami.ospf.kotlin.core.solver

import kotlin.math.abs
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.intermediate.AbsStructure
import fuookami.ospf.kotlin.core.model.intermediate.MaxStructure
import fuookami.ospf.kotlin.core.model.intermediate.SemiStructure
import fuookami.ospf.kotlin.core.model.intermediate.LinearTriadModel
import fuookami.ospf.kotlin.core.model.intermediate.MaskingStructure
import fuookami.ospf.kotlin.core.model.intermediate.IndicatorStructure
import fuookami.ospf.kotlin.core.model.intermediate.BinaryLogicStructure
import fuookami.ospf.kotlin.core.model.intermediate.UnivariateLinearPiecewiseStructure
import fuookami.ospf.kotlin.core.token.Token
import fuookami.ospf.kotlin.core.solver.value.toSolverDouble
import fuookami.ospf.kotlin.core.solver.report.SolveReport
import fuookami.ospf.kotlin.core.solver.report.ConstraintId
import fuookami.ospf.kotlin.core.solver.report.AuditFingerprint
import fuookami.ospf.kotlin.core.solver.report.ConstraintRelation
import fuookami.ospf.kotlin.core.solver.report.SolveFingerprinting
import fuookami.ospf.kotlin.core.solver.report.ConstraintEvaluation
import fuookami.ospf.kotlin.core.solver.report.toNormalizedMathematicalModel
import fuookami.ospf.kotlin.core.variable.VariableItemKey

private val PIECEWISE_TOLERANCE = Flt64(1e-6)

private data class NormalizedPiecewise(
    val inputKey: VariableItemKey,
    val resultKey: VariableItemKey,
    val selectorKeys: List<VariableItemKey>,
    val breakpoints: List<Flt64>,
    val values: List<Flt64>
)

private data class SegmentSelection(
    val index: Int,
    val input: Double
)

private data class NormalizedAbs(
    val inputKey: VariableItemKey,
    val resultKey: VariableItemKey,
    val positiveKey: VariableItemKey,
    val negativeKey: VariableItemKey,
    val signKey: VariableItemKey
)

private fun failure(message: String): Ret<Nothing> {
    return Failed(ErrorCode.IllegalArgument, message)
}

private fun keyText(key: VariableItemKey): String {
    return "${key.identifier}:${key.index}"
}

private fun finiteSolverDouble(
    value: Flt64,
    fieldName: String
): Ret<Double> {
    return try {
        val converted = value.toSolverDouble(fieldName = fieldName)
        if (converted.isFinite()) {
            Ok(converted)
        } else {
            failure("数值必须有限：$fieldName / numeric value must be finite: $fieldName")
        }
    } catch (error: RuntimeException) {
        failure(
            "数值转换失败：$fieldName：${error.message ?: error::class.simpleName} / " +
                "numeric conversion failed at $fieldName: ${error.message ?: error::class.simpleName}"
        )
    }
}

private fun finiteSolverDoubles(
    values: List<Flt64>,
    fieldName: String
): Ret<List<Double>> {
    val converted = ArrayList<Double>(values.size)
    for ((index, value) in values.withIndex()) {
        when (val result = finiteSolverDouble(value, "$fieldName[$index]")) {
            is Ok -> converted += result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
    }
    return Ok(converted)
}

private fun numberListText(
    values: List<Flt64>,
    fieldName: String
): Ret<String> {
    return when (val result = finiteSolverDoubles(values, fieldName)) {
        is Ok -> Ok(result.value.joinToString(","))
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

private fun normalizePiecewiseStructures(
    structures: List<UnivariateLinearPiecewiseStructure<*>>
): Ret<List<NormalizedPiecewise>> {
    val normalized = ArrayList<NormalizedPiecewise>(structures.size)
    val resultKeys = HashSet<VariableItemKey>()

    for (structure in structures) {
        val data = when (val prepared = prepareNativePiecewise(structure)) {
            is Ok -> prepared.value
            is Failed -> return Failed(prepared.error)
            is Fatal -> return Fatal(prepared.errors)
        }
        val selectorKeys = structure.selectorVariables.map { it.key }
        if (selectorKeys.isNotEmpty() && selectorKeys.size != data.xPoints.size - 1) {
            return failure("原生 PWL selector 数量不匹配 / Native PWL selector count is inconsistent")
        }
        if (selectorKeys.size != selectorKeys.toSet().size) {
            return failure("原生 PWL selector key 重复 / Native PWL selector keys are duplicated")
        }
        if (!resultKeys.add(structure.resultVariable.key)) {
            return failure("原生 PWL result key 重复 / Native PWL result keys are duplicated")
        }

        normalized += NormalizedPiecewise(
            inputKey = data.inputKey,
            resultKey = data.resultKey,
            selectorKeys = selectorKeys,
            breakpoints = data.xPoints.map { Flt64(it) },
            values = data.yPoints.map { Flt64(it) }
        )
    }

    return Ok(normalized)
}

private fun normalizeAbsStructures(
    structures: List<AbsStructure<*>>
): Ret<List<NormalizedAbs>> {
    val normalized = ArrayList<NormalizedAbs>(structures.size)
    val declarationKeys = HashSet<VariableItemKey>()
    for (structure in structures) {
        val data = when (val prepared = prepareNativeAbs(structure)) {
            is Ok -> prepared.value
            is Failed -> return Failed(prepared.error)
            is Fatal -> return Fatal(prepared.errors)
        }
        val positiveKey = structure.positiveVariable.key
        val negativeKey = structure.negativeVariable.key
        val signKey = structure.signVariable.key
        val keys = listOf(data.inputKey, data.resultKey, positiveKey, negativeKey, signKey)
        if (keys.size != keys.toSet().size) {
            return failure("原生 ABS key 重复 / Native ABS keys are duplicated")
        }
        for (key in listOf(data.resultKey, positiveKey, negativeKey, signKey)) {
            if (!declarationKeys.add(key)) {
                return failure("原生 ABS key 重复 / Native ABS keys are duplicated")
            }
        }
        normalized += NormalizedAbs(
            inputKey = data.inputKey,
            resultKey = data.resultKey,
            positiveKey = positiveKey,
            negativeKey = negativeKey,
            signKey = signKey
        )
    }
    return Ok(normalized)
}

private fun validateCombinedStructureKeys(
    piecewise: List<NormalizedPiecewise>,
    abs: List<NormalizedAbs>,
    maxima: List<NativeMaxData>
): Ret<Unit> {
    val owners = HashMap<VariableItemKey, String>()

    fun claim(key: VariableItemKey, owner: String): Ret<Unit> {
        if (owners.putIfAbsent(key, owner) != null) {
            return failure("native function key 重复：${keyText(key)} / Native function key is duplicated: ${keyText(key)}")
        }
        return Ok(Unit)
    }

    for ((index, structure) in piecewise.withIndex()) {
        for ((key, label) in listOf(
            structure.resultKey to "pwl[$index].result"
        ) + structure.selectorKeys.mapIndexed { selectorIndex, key ->
            key to "pwl[$index].selector[$selectorIndex]"
        }) {
            when (val result = claim(key, label)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
    }
    for ((index, structure) in abs.withIndex()) {
        for ((key, label) in listOf(
            structure.resultKey to "abs[$index].result",
            structure.positiveKey to "abs[$index].positive",
            structure.negativeKey to "abs[$index].negative",
            structure.signKey to "abs[$index].sign"
        )) {
            when (val result = claim(key, label)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
    }
    for ((index, structure) in maxima.withIndex()) {
        for (key in listOf(structure.resultKey) + structure.selectorKeys) {
            when (val result = claim(key, "max[$index]")) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
    }
    val omittedHelpers = buildSet {
        piecewise.forEach { addAll(it.selectorKeys) }
        maxima.forEach { addAll(it.selectorKeys) }
        abs.forEach {
            add(it.positiveKey)
            add(it.negativeKey)
            add(it.signKey)
        }
    }
    for (structure in piecewise) {
        if (structure.inputKey in omittedHelpers) {
            return failure("PWL input references an omitted helper / PWL input 引用了被省略 helper")
        }
    }
    for (structure in abs) {
        if (structure.inputKey in omittedHelpers) {
            return failure("ABS input references an omitted helper / ABS input 引用了被省略 helper")
        }
    }
    if (maxima.any { structure -> structure.inputs.any { input -> input.terms.keys.any { it in omittedHelpers } } }) {
        return failure("MAX input references an omitted helper / MAX input 引用了被省略 helper")
    }
    return Ok(Unit)
}

private fun nativeColumns(model: LinearTriadModel): Ret<Map<VariableItemKey, Int>> {
    if (model.variables.size != model.tokensInSolver.size) {
        return failure("native 变量与 token 数量不一致 / Native variable and token counts differ")
    }
    val keys = model.tokensInSolver.map { it.key }
    if (keys.size != keys.toSet().size) {
        return failure("native token key 重复 / Native token keys are duplicated")
    }
    for (index in model.variables.indices) {
        val originKey = model.variables[index].origin?.key
        if (originKey != null && originKey != keys[index]) {
            return failure("native 变量与 token key 不一致 / Native variable and token keys differ")
        }
    }
    return Ok(keys.withIndex().associate { it.value to it.index })
}

private fun valuesByKey(
    values: List<Flt64>,
    columns: Map<VariableItemKey, Int>
): Ret<Map<VariableItemKey, Flt64>> {
    if (values.size != columns.size) {
        return failure("native solution 长度不匹配 / Native solution length does not match columns")
    }
    val result = LinkedHashMap<VariableItemKey, Flt64>(columns.size)
    for ((key, column) in columns) {
        val value = values.getOrNull(column)
            ?: return failure("native solution 缺少列 $column / Native solution is missing column $column")
        when (val result = finiteSolverDouble(value, "native.solution[$column]")) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        result[key] = value
    }
    return Ok(result)
}

private fun selectSegment(
    input: Flt64,
    breakpoints: List<Flt64>
): Ret<SegmentSelection> {
    if (breakpoints.size < 2) {
        return failure("PWL 至少需要两个断点 / PWL requires at least two breakpoints")
    }
    var value = when (val result = finiteSolverDouble(input, "native.pwl.input")) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    val convertedBreakpoints = when (val result = finiteSolverDoubles(breakpoints, "native.pwl.breakpoints")) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    val tolerance = when (val result = finiteSolverDouble(PIECEWISE_TOLERANCE, "native.pwl.tolerance")) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    val lower = convertedBreakpoints.first()
    val upper = convertedBreakpoints.last()
    if (value < lower) {
        if (lower - value > tolerance) {
            return failure("PWL 输入超出定义域 / PWL input is outside its domain")
        }
        value = lower
    } else if (value > upper) {
        if (value - upper > tolerance) {
            return failure("PWL 输入超出定义域 / PWL input is outside its domain")
        }
        value = upper
    }

    val index = (0 until convertedBreakpoints.lastIndex).firstOrNull { segment ->
        value >= convertedBreakpoints[segment] && value <= convertedBreakpoints[segment + 1]
    } ?: return failure("PWL 输入没有有效分段 / PWL input has no valid segment")
    return Ok(SegmentSelection(index, value))
}

private fun selectorValues(
    nativeValues: Map<VariableItemKey, Flt64>,
    structures: List<NormalizedPiecewise>
): Ret<Map<VariableItemKey, Flt64>> {
    val result = LinkedHashMap<VariableItemKey, Flt64>()
    for (structure in structures) {
        val input = nativeValues[structure.inputKey]
            ?: return failure("PWL 输入 token 缺失 / PWL input token is missing")
        if (structure.resultKey !in nativeValues) {
            return failure("PWL result token 缺失 / PWL result token is missing")
        }
        if (structure.selectorKeys.any { it in nativeValues }) {
            return failure("native solution 不应包含 selector / Native solution must omit selectors")
        }
        val selected = selectSegment(input, structure.breakpoints)
        val selection = when (selected) {
            is Ok -> selected.value
            is Failed -> return Failed(selected.error)
            is Fatal -> return Fatal(selected.errors)
        }
        for ((index, key) in structure.selectorKeys.withIndex()) {
            val value = if (index == selection.index) Flt64.one else Flt64.zero
            val previous = result[key]
            if (previous != null && previous != value) {
                return failure("PWL selector 重建冲突 / PWL selector reconstruction conflicts")
            }
            result[key] = value
        }
    }
    return Ok(result)
}

private fun absHelperValues(
    nativeValues: Map<VariableItemKey, Flt64>,
    structures: List<NormalizedAbs>
): Ret<Map<VariableItemKey, Flt64>> {
    val result = LinkedHashMap<VariableItemKey, Flt64>(structures.size * 3)
    for (structure in structures) {
        val input = nativeValues[structure.inputKey]
            ?: return failure("ABS 输入 token 缺失 / ABS input token is missing")
        if (structure.resultKey !in nativeValues) {
            return failure("ABS result token 缺失 / ABS result token is missing")
        }
        val positive = if (input gr Flt64.zero) input else Flt64.zero
        val negative = if (input ls Flt64.zero) -input else Flt64.zero
        val sign = if (input ls Flt64.zero) Flt64.zero else Flt64.one
        result[structure.positiveKey] = positive
        result[structure.negativeKey] = negative
        result[structure.signKey] = sign
    }
    return Ok(result)
}

private fun restoreValues(
    values: List<Flt64>,
    columns: Map<VariableItemKey, Int>,
    originalTokens: List<Token<Flt64>>,
    structures: List<NormalizedPiecewise>,
    absStructures: List<NormalizedAbs>,
    maxStructures: List<NativeMaxData>,
    semiStructures: List<SemiStructure<*>>
): Ret<List<Flt64>> {
    val nativeValues = when (val result = valuesByKey(values, columns)) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    val selectors = when (val result = selectorValues(nativeValues, structures)) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    val absHelpers = when (val result = absHelperValues(nativeValues, absStructures)) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    val maxHelpers = LinkedHashMap<VariableItemKey, Flt64>()
    for (structure in maxStructures) {
        val inputs = when (val result = maxInputValues(nativeValues, structure)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val selected = if (structure.minimum) {
            inputs.indexOf(inputs.minOrNull()!!)
        } else {
            inputs.indexOf(inputs.maxOrNull()!!)
        }
        for ((index, key) in structure.selectorKeys.withIndex()) {
            maxHelpers[key] = if (index == selected) Flt64.one else Flt64.zero
        }
    }
    val semiHelpers = LinkedHashMap<VariableItemKey, Flt64>()
    for (structure in semiStructures) {
        val result = nativeValues[structure.resultVariable.key]
            ?: return failure("半连续结果 token 缺失 / Semi-continuous result token is missing")
        val lower = structure.converter.fromValue(structure.lowerBound).toSolverDouble()
        semiHelpers[structure.indicatorVariable.key] = if (result.toSolverDouble() >= lower) Flt64.one else Flt64.zero
    }
    val restored = ArrayList<Flt64>(originalTokens.size)
    for (token in originalTokens) {
        val value = nativeValues[token.key] ?: selectors[token.key] ?: absHelpers[token.key] ?: maxHelpers[token.key] ?: semiHelpers[token.key]
            ?: return failure("原始 token 缺少 native 值：${keyText(token.key)} / Original token has no native value")
        restored += value
    }
    return Ok(restored)
}

private fun piecewiseEvaluations(
    values: List<Flt64>,
    columns: Map<VariableItemKey, Int>,
    structures: List<NormalizedPiecewise>,
    backendName: String
): Ret<List<ConstraintEvaluation<Flt64>>> {
    val nativeValues = when (val result = valuesByKey(values, columns)) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    val evaluations = ArrayList<ConstraintEvaluation<Flt64>>(structures.size)
    val tolerance = when (val result = finiteSolverDouble(PIECEWISE_TOLERANCE, "native.pwl.tolerance")) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    for (structure in structures) {
        val input = nativeValues[structure.inputKey]
            ?: return failure("PWL 输入 token 缺失 / PWL input token is missing")
        val result = nativeValues[structure.resultKey]
            ?: return failure("PWL result token 缺失 / PWL result token is missing")
        val selected = when (val selection = selectSegment(input, structure.breakpoints)) {
            is Ok -> selection.value
            is Failed -> return Failed(selection.error)
            is Fatal -> return Fatal(selection.errors)
        }
        val lower = structure.breakpoints[selected.index]
        val upper = structure.breakpoints[selected.index + 1]
        val fraction = (Flt64(selected.input) - lower) / (upper - lower)
        val expected = structure.values[selected.index] +
            fraction * (structure.values[selected.index + 1] - structure.values[selected.index])
        val violationValue = when (val conversion = finiteSolverDouble(expected - result, "native.pwl.violation")) {
            is Ok -> abs(conversion.value)
            is Failed -> return Failed(conversion.error)
            is Fatal -> return Fatal(conversion.errors)
        }
        val violation = Flt64(violationValue)
        evaluations += ConstraintEvaluation(
            constraintId = ConstraintId("$backendName-pwl:${keyText(structure.resultKey)}"),
            lhs = expected,
            rhs = result,
            relation = ConstraintRelation.Equal,
            slack = result - expected,
            violation = violation,
            tolerance = PIECEWISE_TOLERANCE,
            satisfied = violationValue <= tolerance
        )
    }
    return Ok(evaluations)
}

private fun absEvaluations(
    values: List<Flt64>,
    columns: Map<VariableItemKey, Int>,
    structures: List<NormalizedAbs>,
    backendName: String
): Ret<List<ConstraintEvaluation<Flt64>>> {
    val nativeValues = when (val result = valuesByKey(values, columns)) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    val evaluations = ArrayList<ConstraintEvaluation<Flt64>>(structures.size)
    val tolerance = when (val result = finiteSolverDouble(PIECEWISE_TOLERANCE, "native.abs.tolerance")) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    for (structure in structures) {
        val input = nativeValues[structure.inputKey]
            ?: return failure("ABS 输入 token 缺失 / ABS input token is missing")
        val result = nativeValues[structure.resultKey]
            ?: return failure("ABS result token 缺失 / ABS result token is missing")
        val expected = if (input ls Flt64.zero) -input else input
        val violationValue = when (val conversion = finiteSolverDouble(
            expected - result,
            "native.abs.violation"
        )) {
            is Ok -> abs(conversion.value)
            is Failed -> return Failed(conversion.error)
            is Fatal -> return Fatal(conversion.errors)
        }
        val violation = Flt64(violationValue)
        evaluations += ConstraintEvaluation(
            constraintId = ConstraintId("$backendName-abs:${keyText(structure.resultKey)}"),
            lhs = expected,
            rhs = result,
            relation = ConstraintRelation.Equal,
            slack = result - expected,
            violation = violation,
            tolerance = PIECEWISE_TOLERANCE,
            satisfied = violationValue <= tolerance
        )
    }
    return Ok(evaluations)
}

private fun maxInputValues(
    values: Map<VariableItemKey, Flt64>,
    structure: NativeMaxData
): Ret<List<Double>> {
    val inputs = ArrayList<Double>(structure.inputs.size)
    for (input in structure.inputs) {
        var value = input.constant
        for ((key, coefficient) in input.terms) {
            val variable = values[key] ?: return failure("MAX 输入 token 缺失 / MAX input token is missing")
            val converted = when (val result = finiteSolverDouble(variable, "native.max.input")) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            value += coefficient * converted
            if (!value.isFinite()) {
                return failure("MAX 输入非有限 / MAX input is not finite")
            }
        }
        inputs += value
    }
    return Ok(inputs)
}

private fun maxEvaluations(
    values: List<Flt64>,
    columns: Map<VariableItemKey, Int>,
    structures: List<NativeMaxData>,
    backendName: String
): Ret<List<ConstraintEvaluation<Flt64>>> {
    val nativeValues = when (val result = valuesByKey(values, columns)) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    val evaluations = ArrayList<ConstraintEvaluation<Flt64>>(structures.size)
    for (structure in structures) {
        val inputs = when (val result = maxInputValues(nativeValues, structure)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val actual = nativeValues[structure.resultKey]
            ?: return failure("MAX 结果 token 缺失 / MAX result token is missing")
        val expected = Flt64(inputs.maxOrNull()!!)
        val violation = when (val result = finiteSolverDouble(expected - actual, "native.max.violation")) {
            is Ok -> abs(result.value)
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        evaluations += ConstraintEvaluation(
            constraintId = ConstraintId("$backendName-max:${keyText(structure.resultKey)}"),
            lhs = expected,
            rhs = actual,
            relation = ConstraintRelation.Equal,
            slack = actual - expected,
            violation = Flt64(violation),
            tolerance = PIECEWISE_TOLERANCE,
            satisfied = violation <= 1e-6
        )
    }
    return Ok(evaluations)
}

private fun piecewiseFingerprint(
    fingerprint: AuditFingerprint,
    columns: Map<VariableItemKey, Int>,
    structures: List<NormalizedPiecewise>,
    backendName: String
): Ret<AuditFingerprint> {
    val structureLines = ArrayList<String>(structures.size)
    for (structure in structures) {
        val inputColumn = columns[structure.inputKey]
            ?: return failure("PWL 输入列缺失 / PWL input column is missing")
        val resultColumn = columns[structure.resultKey]
            ?: return failure("PWL 结果列缺失 / PWL result column is missing")
        val breakpoints = when (val result = numberListText(structure.breakpoints, "native.pwl.fingerprint.breakpoints")) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val values = when (val result = numberListText(structure.values, "native.pwl.fingerprint.values")) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        structureLines += listOf(
            "input-column=$inputColumn",
            "result-column=$resultColumn",
            "breakpoints=$breakpoints",
            "values=$values"
        ).joinToString("|")
    }
    return Ok(
        SolveFingerprinting.sha256(
            buildString {
                append(fingerprint.value)
                if (structureLines.isNotEmpty()) {
                    append('\n')
                    append(structureLines.sorted().joinToString("\n"))
                }
            },
            schemaVersion = "$backendName-pwl-1"
        )
    )
}

private fun validateNativeAbsColumns(
    columns: Map<VariableItemKey, Int>,
    structures: List<NormalizedAbs>
): Ret<Unit> {
    for (structure in structures) {
        if (structure.inputKey !in columns) {
            return failure("ABS 输入列缺失 / ABS input column is missing")
        }
        if (structure.resultKey !in columns) {
            return failure("ABS 结果列缺失 / ABS result column is missing")
        }
        if (listOf(structure.positiveKey, structure.negativeKey, structure.signKey).any { it in columns }) {
            return failure("ABS helper 仍在 native 列中 / ABS helper is still present in native columns")
        }
    }
    return Ok(Unit)
}

private fun nativeFingerprint(
    fingerprint: AuditFingerprint,
    columns: Map<VariableItemKey, Int>,
    piecewise: List<NormalizedPiecewise>,
    abs: List<NormalizedAbs>,
    maxima: List<NativeMaxData>,
    semis: List<SemiStructure<*>>,
    binaryLogic: List<BinaryLogicStructure<*>>,
    indicators: List<NativeIndicatorData>,
    maskings: List<NativeMaskingData>,
    backendName: String
): Ret<AuditFingerprint> {
    if (abs.isEmpty() && maxima.isEmpty() && semis.isEmpty() && binaryLogic.isEmpty() && indicators.isEmpty() && maskings.isEmpty()) {
        return piecewiseFingerprint(fingerprint, columns, piecewise, backendName)
    }
    val structureLines = ArrayList<String>(piecewise.size + abs.size + semis.size + binaryLogic.size)
    for (structure in piecewise) {
        val inputColumn = columns[structure.inputKey]
            ?: return failure("PWL 输入列缺失 / PWL input column is missing")
        val resultColumn = columns[structure.resultKey]
            ?: return failure("PWL 结果列缺失 / PWL result column is missing")
        val breakpoints = when (val result = numberListText(
            structure.breakpoints,
            "native.pwl.fingerprint.breakpoints"
        )) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val values = when (val result = numberListText(structure.values, "native.pwl.fingerprint.values")) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        structureLines += listOf(
            "kind=pwl",
            "input-column=$inputColumn",
            "result-column=$resultColumn",
            "breakpoints=$breakpoints",
            "values=$values"
        ).joinToString("|")
    }
    for (structure in abs) {
        val inputColumn = columns[structure.inputKey]
            ?: return failure("ABS 输入列缺失 / ABS input column is missing")
        val resultColumn = columns[structure.resultKey]
            ?: return failure("ABS 结果列缺失 / ABS result column is missing")
        structureLines += listOf(
            "kind=abs",
            "input-column=$inputColumn",
            "result-column=$resultColumn"
        ).joinToString("|")
    }
    for (structure in maxima) {
        val resultColumn = columns[structure.resultKey]
            ?: return failure("MAX 结果列缺失 / MAX result column is missing")
        val inputs = ArrayList<String>(structure.inputs.size)
        for (input in structure.inputs) {
            val terms = ArrayList<Pair<Int, Double>>(input.terms.size)
            for ((key, coefficient) in input.terms) {
                val column = columns[key] ?: return failure("MAX 输入列缺失 / MAX input column is missing")
                terms += column to coefficient
            }
            inputs += "constant=${input.constant};terms=" + terms.sortedBy { it.first }
                .joinToString(",") { "${it.first}:${it.second}" }
        }
        structureLines += "kind=max|result-column=$resultColumn|inputs=" + inputs.sorted().joinToString(";")
    }
    for (structure in semis) {
        val resultColumn = columns[structure.resultVariable.key]
            ?: return failure("半连续结果列缺失 / Semi-continuous result column is missing")
        val indicatorColumn = columns[structure.indicatorVariable.key]
            ?: return failure("半连续指示列缺失 / Semi-continuous indicator column is missing")
        val lower = structure.converter.fromValue(structure.lowerBound).toSolverDouble()
        val upper = structure.converter.fromValue(structure.upperBound).toSolverDouble()
        if (!lower.isFinite() || !upper.isFinite() || lower <= 0.0 || lower > upper) {
            return failure("半连续边界无效 / Invalid semi-continuous bounds")
        }
        structureLines += "kind=semi|result-column=$resultColumn|indicator-column=$indicatorColumn|lower=$lower|upper=$upper"
    }
    for (structure in binaryLogic) {
        val resultColumn = columns[structure.resultVariable.key] ?: return failure("二值逻辑结果列缺失 / Binary logic result column is missing")
        val inputColumns = structure.inputs.map { columns[it.key] ?: return failure("二值逻辑输入列缺失 / Binary logic input column is missing") }
        structureLines += "kind=binary-logic|operation=${structure.operation}|result-column=$resultColumn|inputs=${inputColumns.sorted().joinToString(",")}"
    }
    for (structure in indicators) {
        val resultColumn = columns[structure.resultKey] ?: return failure("Indicator result column missing")
        val terms = structure.terms.map { (key, coefficient) ->
            val column = columns[key] ?: return failure("Indicator input column missing")
            column to coefficient
        }.sortedBy { it.first }.joinToString(",") { "${it.first}:${it.second}" }
        val band = structure.zeroBand
        structure.difference?.let { combined ->
            val secondColumn = columns[combined.condition.resultKey] ?: return failure("Difference condition column missing")
            val outputColumn = columns[combined.resultKey] ?: return failure("Difference result column missing")
            structureLines += "kind=exclusive-difference|first=$resultColumn|second=$secondColumn|result=$outputColumn"
        }
        structure.conjunction?.let { combined ->
            val secondColumn = columns[combined.condition.resultKey] ?: return failure("Conjoined condition column missing")
            val outputColumn = columns[combined.resultKey] ?: return failure("Conjunction result column missing")
            structureLines += "kind=conjunction|first=$resultColumn|second=$secondColumn|result=$outputColumn"
        }
        if (band == null) {
            structureLines += "kind=positive-indicator|result=$resultColumn|constant=${structure.constant}|gap=${structure.tolerance}|positive-on-zero=${structure.positiveOnZero}|terms=$terms"
        } else {
            val sideColumn = columns[band.sideKey] ?: return failure("Zero-band side column missing")
            structureLines += "kind=zero-band|result=$resultColumn|side=$sideColumn|constant=${structure.constant}|inside=${band.tolerance}|outside=${structure.tolerance}|outside-on-zero=${structure.positiveOnZero}|terms=$terms"
        }
        structure.impliedCondition?.let { value ->
            val consequentColumn = columns[value.resultKey] ?: return failure("Implied indicator column missing")
            val consequentTerms = value.terms.map { (key, coefficient) ->
                (columns[key] ?: return failure("Implied input column missing")) to coefficient
            }.sortedBy { it.first }.joinToString(",") { "${it.first}:${it.second}" }
            structureLines += "kind=implied-condition|antecedent=$resultColumn|consequent=$consequentColumn|constant=${value.constant}|gap=${structure.tolerance}|terms=$consequentTerms"
        }
        structure.conditionalValue?.let { value ->
            val outputColumn = columns[value.resultKey] ?: return failure("Conditional value result column missing")
            val valueTerms = value.terms.map { (key, coefficient) ->
                (columns[key] ?: return failure("Conditional value input column missing")) to coefficient
            }.sortedBy { it.first }.joinToString(",") { "${it.first}:${it.second}" }
            structureLines += "kind=conditional-value|indicator=$resultColumn|result=$outputColumn|constant=${value.constant}|terms=$valueTerms"
        }
        for (key in structure.equivalentResultKeys) {
            val equivalentColumn = columns[key] ?: return failure("Indicator equivalent result column missing")
            structureLines += "kind=indicator-equivalence|result=$resultColumn|equivalent=$equivalentColumn"
        }
    }
    for (structure in maskings) {
        val maskColumn = columns[structure.maskKey] ?: return failure("Mask column missing")
        val resultColumn = columns[structure.value.resultKey] ?: return failure("Masking result column missing")
        val terms = structure.value.terms.map { (key, coefficient) ->
            (columns[key] ?: return failure("Masking input column missing")) to coefficient
        }.sortedBy { it.first }.joinToString(",") { "${it.first}:${it.second}" }
        structureLines += "kind=masking|mask=$maskColumn|result=$resultColumn|constant=${structure.value.constant}|terms=$terms"
        structure.definition?.let { definition ->
            val definitionTerms = definition.terms.map { (key, coefficient) ->
                (columns[key] ?: return failure("Mask definition column missing")) to coefficient
            }.sortedBy { it.first }.joinToString(",") { "${it.first}:${it.second}" }
            structureLines += "kind=mask-definition|mask=$maskColumn|constant=${definition.constant}|terms=$definitionTerms"
        }
    }
    val schema = if (maskings.any { it.definition != null }) {
        "$backendName-functions-masking-2"
    } else if (maskings.isNotEmpty()) {
        "$backendName-functions-masking-1"
    } else if (binaryLogic.isNotEmpty()) {
        "$backendName-functions-binary-logic-1"
    } else if (semis.isNotEmpty()) {
        "$backendName-functions-semi-1"
    } else if (indicators.any { it.difference != null }) {
        "$backendName-functions-indicator-8"
    } else if (indicators.any { it.conjunction != null }) {
        "$backendName-functions-indicator-7"
    } else if (indicators.any { it.zeroBand != null }) {
        "$backendName-functions-indicator-6"
    } else if (indicators.any { it.impliedCondition != null }) {
        "$backendName-functions-indicator-5"
    } else if (indicators.any { it.conditionalValue != null }) {
        "$backendName-functions-indicator-4"
    } else if (indicators.any { it.equivalentResultKeys.isNotEmpty() }) {
        "$backendName-functions-indicator-3"
    } else if (indicators.isNotEmpty()) {
        "$backendName-functions-indicator-2"
    } else if (maxima.isNotEmpty()) {
        "$backendName-functions-max-1"
    } else if (piecewise.isEmpty()) {
        "$backendName-abs-1"
    } else {
        "$backendName-pwl-abs-1"
    }
    return Ok(
        SolveFingerprinting.sha256(
            buildString {
                append(fingerprint.value)
                append('\n')
                append(structureLines.sorted().joinToString("\n"))
            },
            schemaVersion = schema
        )
    )
}

/**
 * 恢复原始 PWL token、原生关系指纹和残差诊断。 /
 * Restore original PWL tokens, native-relation fingerprints, and residual diagnostics.
 *
 * @param report 原生求解报告 / Native solve report
 * @param nativeModel 原生求解模型 / Native solver model
 * @param originalTokens 原始模型 token 顺序 / Original model token order
 * @param structures 原生处理的分段线性结构 / Piecewise-linear structures handled natively
 * @param backendName backend 名称，用于关系 ID 和指纹 schema / Backend name used for relation IDs and fingerprint schema
 * @param absStructures 原生处理的绝对值结构 / Absolute-value structures handled natively
 * @param maxStructures 原生处理的最大值结构 / Maximum-value structures handled natively
 * @param semiStructures 原生处理的半连续结构 / Semi-continuous structures handled natively
 * @param binaryLogicStructures 原生处理的二元逻辑结构 / Binary-logic structures handled natively
 * @param indicatorStructures 原生处理的指示结构 / Indicator structures handled natively
 * @param maskingStructures 原生处理的门控结构 / Masking structures handled natively
 * @return 恢复后的求解报告或错误 / Restored solve report or an error
 */
public fun restoreNativePiecewiseSolution(
    report: SolveReport<Flt64>,
    nativeModel: LinearTriadModel,
    originalTokens: List<Token<Flt64>>,
    structures: List<UnivariateLinearPiecewiseStructure<*>>,
    backendName: String,
    absStructures: List<AbsStructure<*>> = emptyList(),
    maxStructures: List<MaxStructure<*>> = emptyList(),
    semiStructures: List<SemiStructure<*>> = emptyList(),
    binaryLogicStructures: List<BinaryLogicStructure<*>> = emptyList(),
    indicatorStructures: List<IndicatorStructure<*>> = emptyList(),
    maskingStructures: List<MaskingStructure<*>> = emptyList()
): Ret<SolveReport<Flt64>> {
    if (originalTokens.size != originalTokens.map { it.key }.toSet().size) {
        return failure("原始 token key 重复 / Original token keys are duplicated")
    }
    val normalized = when (val result = normalizePiecewiseStructures(structures)) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    val normalizedAbs = when (val result = normalizeAbsStructures(absStructures)) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    val normalizedMax = ArrayList<NativeMaxData>(maxStructures.size)
    val normalizedIndicator = ArrayList<NativeIndicatorData>(indicatorStructures.size)
    val normalizedMasking = ArrayList<NativeMaskingData>(maskingStructures.size)
    for (structure in maskingStructures) {
        when (val result = prepareNativeMasking(structure)) {
            is Ok -> normalizedMasking += result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
    }
    for (structure in indicatorStructures) {
        when (val result = prepareNativeIndicator(structure)) {
            is Ok -> {
                normalizedIndicator += result.value
                result.value.conjunction?.let { normalizedIndicator += it.condition }
                result.value.difference?.let { normalizedIndicator += it.condition }
            }
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
    }
    for (structure in maxStructures) {
        when (val result = prepareNativeMax(structure)) {
            is Ok -> normalizedMax += result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
    }
    if (normalizedAbs.isNotEmpty() || normalizedMax.isNotEmpty()) {
        when (val result = validateCombinedStructureKeys(
            piecewise = normalized,
            abs = normalizedAbs,
            maxima = normalizedMax
        )) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
    }
    val columns = when (val result = nativeColumns(nativeModel)) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    when (val result = validateNativeAbsColumns(columns, normalizedAbs)) {
        is Ok -> {}
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    for (structure in normalizedMax) {
        if (structure.resultKey !in columns || structure.inputs.any { input -> input.terms.keys.any { it !in columns } }) {
            return failure("MAX 结果或输入列缺失 / MAX result or input column is missing")
        }
        if (structure.selectorKeys.any { it in columns }) {
            return failure("MAX selector 仍在 native 列中 / MAX selector is still present in native columns")
        }
    }
    val baseFingerprint = report.fingerprints.model ?: nativeModel.toNormalizedMathematicalModel().fingerprint()
    val updatedFingerprint = when (val result = nativeFingerprint(
        fingerprint = baseFingerprint,
        columns = columns,
        piecewise = normalized,
        abs = normalizedAbs,
        maxima = normalizedMax,
        semis = semiStructures,
        binaryLogic = binaryLogicStructures,
        indicators = normalizedIndicator,
        maskings = normalizedMasking,
        backendName = backendName
    )) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    val fingerprintedReport = report.copy(fingerprints = report.fingerprints.copy(model = updatedFingerprint))
    val solution = report.solution ?: return Ok(fingerprintedReport)
    val restoredValues = when (val result = restoreValues(
        solution.values,
        columns,
        originalTokens,
        normalized,
        normalizedAbs,
        normalizedMax,
        semiStructures
    )) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    val restoredPool = ArrayList<List<Flt64>>(solution.pool.size)
    for (poolValues in solution.pool) {
        when (val result = restoreValues(
            poolValues,
            columns,
            originalTokens,
            normalized,
            normalizedAbs,
            normalizedMax,
            semiStructures
        )) {
            is Ok -> restoredPool += result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
    }

    val pwlResiduals = when (val result = piecewiseEvaluations(
        solution.values,
        columns,
        normalized,
        backendName
    )) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    val absResiduals = when (val result = absEvaluations(
        solution.values,
        columns,
        normalizedAbs,
        backendName
    )) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    val maxResiduals = when (val result = maxEvaluations(
        values = solution.values,
        columns = columns,
        structures = normalizedMax,
        backendName = backendName
    )) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    val indicatorResiduals = ArrayList<ConstraintEvaluation<Flt64>>()
    for (structure in normalizedIndicator) {
        val resultIndex = columns[structure.resultKey] ?: return failure("Indicator result column missing")
        val flag = solution.values.getOrNull(resultIndex)?.toSolverDouble() ?: return failure("Indicator result value missing")
        var input = structure.constant
        for ((key, coefficient) in structure.terms) {
            val column = columns[key] ?: return failure("Indicator input column missing")
            input += coefficient * (solution.values.getOrNull(column)?.toSolverDouble() ?: return failure("Indicator input value missing"))
        }
        if (!input.isFinite() || !flag.isFinite()) return failure("Non-finite indicator solution")
        val roundedFlag = if (flag >= 0.5) 1.0 else 0.0
        val active = (roundedFlag == 1.0) != structure.positiveOnZero
        structure.difference?.let { combined ->
            val secondColumn = columns[combined.condition.resultKey] ?: return failure("Difference condition column missing")
            val outputColumn = columns[combined.resultKey] ?: return failure("Difference result column missing")
            val second = solution.values.getOrNull(secondColumn)?.toSolverDouble() ?: return failure("Difference condition value missing")
            val output = solution.values.getOrNull(outputColumn)?.toSolverDouble() ?: return failure("Difference result value missing")
            if (!second.isFinite() || !output.isFinite()) return failure("Non-finite difference solution")
            val roundedSecond = if (second >= 0.5) 1.0 else 0.0
            val expected = roundedFlag - roundedSecond
            val difference = maxOf(abs(output - expected), roundedFlag + roundedSecond - 1.0)
            indicatorResiduals += ConstraintEvaluation(
                constraintId = ConstraintId("$backendName-difference:${keyText(combined.resultKey)}"),
                lhs = Flt64(output),
                rhs = Flt64(expected),
                relation = ConstraintRelation.Equal,
                slack = Flt64(-difference),
                violation = Flt64(difference),
                tolerance = PIECEWISE_TOLERANCE,
                satisfied = difference <= 1e-6
            )
        }
        structure.conjunction?.let { combined ->
            val secondColumn = columns[combined.condition.resultKey] ?: return failure("Conjoined condition column missing")
            val outputColumn = columns[combined.resultKey] ?: return failure("Conjunction result column missing")
            val second = solution.values.getOrNull(secondColumn)?.toSolverDouble() ?: return failure("Conjoined condition value missing")
            val output = solution.values.getOrNull(outputColumn)?.toSolverDouble() ?: return failure("Conjunction result value missing")
            if (!second.isFinite() || !output.isFinite()) return failure("Non-finite conjunction solution")
            val expected = if (roundedFlag == 1.0 && second >= 0.5) 1.0 else 0.0
            val difference = abs(output - expected)
            indicatorResiduals += ConstraintEvaluation(
                constraintId = ConstraintId("$backendName-conjunction:${keyText(combined.resultKey)}"),
                lhs = Flt64(output),
                rhs = Flt64(expected),
                relation = ConstraintRelation.Equal,
                slack = Flt64(-difference),
                violation = Flt64(difference),
                tolerance = PIECEWISE_TOLERANCE,
                satisfied = difference <= 1e-6
            )
        }
        val band = structure.zeroBand
        var sideIntegrality = 0.0
        val measuredInput = if (band == null) input else {
            val sideColumn = columns[band.sideKey] ?: return failure("Zero-band side column missing")
            val side = solution.values.getOrNull(sideColumn)?.toSolverDouble() ?: return failure("Zero-band side value missing")
            if (!side.isFinite()) return failure("Non-finite zero-band side value")
            val roundedSide = if (side >= 0.5) 1.0 else 0.0
            sideIntegrality = abs(side - roundedSide)
            if (!active) abs(input) else if (roundedSide == 1.0) input else -input
        }
        val bound = if (active) structure.tolerance else band?.tolerance ?: 0.0
        val violation = maxOf(0.0, if (active) bound - measuredInput else measuredInput - bound,
            abs(flag - roundedFlag), sideIntegrality)
        indicatorResiduals += ConstraintEvaluation(
            constraintId = ConstraintId("$backendName-${if (band == null) "indicator" else "zero-band"}:${keyText(structure.resultKey)}"),
            lhs = Flt64(measuredInput),
            rhs = Flt64(bound),
            relation = if (active) ConstraintRelation.GreaterEqual else ConstraintRelation.LessEqual,
            slack = Flt64(if (active) measuredInput - bound else bound - measuredInput),
            violation = Flt64(violation),
            tolerance = PIECEWISE_TOLERANCE,
            satisfied = violation <= 1e-6
        )
        for (key in structure.equivalentResultKeys) {
            val column = columns[key] ?: return failure("Indicator equivalent result column missing")
            val equivalent = solution.values.getOrNull(column)?.toSolverDouble()
                ?: return failure("Indicator equivalent result value missing")
            if (!equivalent.isFinite()) return failure("Non-finite indicator equivalent result")
            val difference = abs(equivalent - flag)
            val integrality = abs(equivalent - if (equivalent >= 0.5) 1.0 else 0.0)
            val equivalentViolation = maxOf(difference, integrality)
            indicatorResiduals += ConstraintEvaluation(
                constraintId = ConstraintId("$backendName-indicator-equivalence:${keyText(structure.resultKey)}:${keyText(key)}"),
                lhs = Flt64(equivalent),
                rhs = Flt64(flag),
                relation = ConstraintRelation.Equal,
                slack = Flt64(-difference),
                violation = Flt64(equivalentViolation),
                tolerance = PIECEWISE_TOLERANCE,
                satisfied = equivalentViolation <= 1e-6
            )
        }
        structure.impliedCondition?.let { value ->
            val consequentColumn = columns[value.resultKey] ?: return failure("Implied indicator column missing")
            val consequentFlag = solution.values.getOrNull(consequentColumn)?.toSolverDouble() ?: return failure("Implied indicator value missing")
            var consequentInput = value.constant
            for ((key, coefficient) in value.terms) {
                val column = columns[key] ?: return failure("Implied input column missing")
                consequentInput += coefficient * (solution.values.getOrNull(column)?.toSolverDouble() ?: return failure("Implied input value missing"))
            }
            if (!consequentFlag.isFinite() || !consequentInput.isFinite()) return failure("Non-finite implied solution")
            val linkViolation = maxOf(0.0, flag - consequentFlag,
                abs(consequentFlag - if (consequentFlag >= 0.5) 1.0 else 0.0))
            indicatorResiduals += ConstraintEvaluation(
                constraintId = ConstraintId("$backendName-imply-link:${keyText(value.resultKey)}"),
                lhs = Flt64(flag),
                rhs = Flt64(consequentFlag),
                relation = ConstraintRelation.LessEqual,
                slack = Flt64(consequentFlag - flag),
                violation = Flt64(linkViolation),
                tolerance = PIECEWISE_TOLERANCE,
                satisfied = linkViolation <= 1e-6
            )
            if (roundedFlag == 1.0) {
                val consequentViolation = maxOf(0.0, structure.tolerance - consequentInput)
                indicatorResiduals += ConstraintEvaluation(
                    constraintId = ConstraintId("$backendName-implied-condition:${keyText(value.resultKey)}"),
                    lhs = Flt64(consequentInput),
                    rhs = Flt64(structure.tolerance),
                    relation = ConstraintRelation.GreaterEqual,
                    slack = Flt64(consequentInput - structure.tolerance),
                    violation = Flt64(consequentViolation),
                    tolerance = PIECEWISE_TOLERANCE,
                    satisfied = consequentViolation <= 1e-6
                )
            }
        }
        structure.conditionalValue?.let { value ->
            val outputColumn = columns[value.resultKey] ?: return failure("Conditional value result column missing")
            val output = solution.values.getOrNull(outputColumn)?.toSolverDouble() ?: return failure("Conditional value result missing")
            var trueValue = value.constant
            for ((key, coefficient) in value.terms) {
                val column = columns[key] ?: return failure("Conditional value input column missing")
                trueValue += coefficient * (solution.values.getOrNull(column)?.toSolverDouble() ?: return failure("Conditional value input missing"))
            }
            if (!output.isFinite() || !trueValue.isFinite()) return failure("Non-finite conditional value solution")
            val expected = if (roundedFlag == 1.0) trueValue else 0.0
            val valueViolation = abs(output - expected)
            indicatorResiduals += ConstraintEvaluation(
                constraintId = ConstraintId("$backendName-conditional-value:${keyText(value.resultKey)}"),
                lhs = Flt64(output),
                rhs = Flt64(expected),
                relation = ConstraintRelation.Equal,
                slack = Flt64(-valueViolation),
                violation = Flt64(valueViolation),
                tolerance = PIECEWISE_TOLERANCE,
                satisfied = valueViolation <= 1e-6
            )
        }
    }
    for (structure in normalizedMasking) {
        val maskColumn = columns[structure.maskKey] ?: return failure("Mask column missing")
        val outputColumn = columns[structure.value.resultKey] ?: return failure("Masking result column missing")
        val mask = solution.values.getOrNull(maskColumn)?.toSolverDouble() ?: return failure("Mask value missing")
        val output = solution.values.getOrNull(outputColumn)?.toSolverDouble() ?: return failure("Masking result missing")
        var input = structure.value.constant
        for ((key, coefficient) in structure.value.terms) {
            val column = columns[key] ?: return failure("Masking input column missing")
            input += coefficient * (solution.values.getOrNull(column)?.toSolverDouble() ?: return failure("Masking input value missing"))
        }
        if (!mask.isFinite() || !output.isFinite() || !input.isFinite()) return failure("Non-finite masking solution")
        val roundedMask = if (mask >= 0.5) 1.0 else 0.0
        structure.definition?.let { definition ->
            var definedMask = definition.constant
            for ((key, coefficient) in definition.terms) {
                val column = columns[key] ?: return failure("Mask definition column missing")
                definedMask += coefficient * (solution.values.getOrNull(column)?.toSolverDouble() ?: return failure("Mask definition input missing"))
            }
            if (!definedMask.isFinite()) return failure("Non-finite mask definition value")
            val definitionViolation = abs(mask - definedMask)
            indicatorResiduals += ConstraintEvaluation(
                constraintId = ConstraintId("$backendName-mask-definition:${keyText(structure.value.resultKey)}"),
                lhs = Flt64(mask),
                rhs = Flt64(definedMask),
                relation = ConstraintRelation.Equal,
                slack = Flt64(-definitionViolation),
                violation = Flt64(definitionViolation),
                tolerance = PIECEWISE_TOLERANCE,
                satisfied = definitionViolation <= 1e-6
            )
        }
        val expected = if (roundedMask == 1.0) input else 0.0
        val difference = abs(output - expected)
        val violation = maxOf(difference, abs(mask - roundedMask))
        indicatorResiduals += ConstraintEvaluation(
            constraintId = ConstraintId("$backendName-masking:${keyText(structure.value.resultKey)}"),
            lhs = Flt64(output),
            rhs = Flt64(expected),
            relation = ConstraintRelation.Equal,
            slack = Flt64(-difference),
            violation = Flt64(violation),
            tolerance = PIECEWISE_TOLERANCE,
            satisfied = violation <= 1e-6
        )
    }
    val restoredReport = fingerprintedReport.copy(
        solution = solution.copy(
            values = restoredValues,
            pool = restoredPool
        ),
        diagnostics = report.diagnostics.copy(
            constraintEvaluations = report.diagnostics.constraintEvaluations + pwlResiduals + absResiduals + maxResiduals + indicatorResiduals
        )
    )
    return Ok(restoredReport)
}
