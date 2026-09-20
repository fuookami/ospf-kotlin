package fuookami.ospf.kotlin.core.solver.gurobi11

import kotlin.math.abs
import com.gurobi.gurobi.*
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.model.intermediate.MaxStructure
import fuookami.ospf.kotlin.core.solver.NativeMaxData
import fuookami.ospf.kotlin.core.solver.NativeMaxInputData
import fuookami.ospf.kotlin.core.solver.nativeElementName
import fuookami.ospf.kotlin.core.solver.prepareNativeMax
import fuookami.ospf.kotlin.core.variable.VariableItemKey

/** Gurobi 11 原生 MAX 数据别名。 / Gurobi 11 native MAX data alias. */
internal typealias GurobiNativeMaxData = NativeMaxData

/** 使用 Gurobi 11 数值边界准备 MAX 数据。 / Prepare MAX data with Gurobi 11 numeric limits. */
internal fun prepareGurobiNativeMax(
    structure: MaxStructure<*>
): Ret<GurobiNativeMaxData> {
    return prepareNativeMax(
        structure = structure,
        maximumMagnitude = GRB.INFINITY
    )
}

/** 隔离 Gurobi 11 MAX SDK 写入。 / Isolate Gurobi 11 MAX SDK writes. */
internal fun interface GurobiNativeMaxWriter {
    fun add(data: GurobiNativeMaxData)
}

/** 先批量校验所有 MAX 数据，再执行 SDK 写入。 / Validate all MAX data before any SDK write. */
internal fun writeGurobiNativeMax(
    variableKeys: Set<VariableItemKey>,
    structures: List<MaxStructure<*>>,
    writer: GurobiNativeMaxWriter
): Try {
    val prepared = ArrayList<GurobiNativeMaxData>(structures.size)
    for (structure in structures) {
        when (val result = prepareGurobiNativeMax(structure)) {
            is Ok -> prepared += result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
    }
    for (data in prepared) {
        when (val result = validateGurobiNativeMaxData(variableKeys, data)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
    }
    for (data in prepared) {
        try {
            writer.add(data)
        } catch (error: LinkageError) {
            return Failed(
                ErrorCode.OREngineModelingException,
                "Gurobi 11 MAX API 不可用：${error.message ?: error::class.simpleName} / " +
                    "Gurobi 11 MAX API is unavailable: ${error.message ?: error::class.simpleName}"
            )
        } catch (error: Exception) {
            return Failed(
                ErrorCode.OREngineModelingException,
                "Gurobi 11 MAX 写入失败：${error.message ?: error::class.simpleName} / " +
                    "Gurobi 11 MAX write failed: ${error.message ?: error::class.simpleName}"
            )
        }
    }
    return ok
}

/** 使用 Gurobi 11 原生 MAX；仿射输入使用 SDK-only 连续变量。 / Write Gurobi 11 native MAX with SDK-only affine variables. */
internal fun addGurobiNativeMax(
    model: GRBModel,
    variables: Map<VariableItemKey, GRBVar>,
    structures: List<MaxStructure<*>>
): Try {
    return writeGurobiNativeMax(
        variableKeys = variables.keys,
        structures = structures,
        writer = GurobiNativeMaxWriter { data ->
            addGurobiNativeMaxData(model, variables, data)
        }
    )
}

private fun validateGurobiNativeMaxData(
    variableKeys: Set<VariableItemKey>,
    data: GurobiNativeMaxData
): Try {
    fun usable(value: Double): Boolean = value.isFinite() && abs(value) < GRB.INFINITY
    if (data.resultKey !in variableKeys) {
        return Failed(
            ErrorCode.IllegalArgument,
            "Gurobi 11 MAX 结果变量缺失：${data.name} / Missing Gurobi 11 MAX result variable: ${data.name}"
        )
    }
    if (data.selectorKeys.any { it in variableKeys }) {
        return Failed(
            ErrorCode.IllegalArgument,
            "Gurobi 11 MAX selector 未被省略：${data.name} / Gurobi 11 MAX selector was not omitted: ${data.name}"
        )
    }
    if (data.inputs.any { input -> input.terms.keys.any { it !in variableKeys } }) {
        return Failed(
            ErrorCode.IllegalArgument,
            "Gurobi 11 MAX 输入变量缺失：${data.name} / Missing Gurobi 11 MAX input variable: ${data.name}"
        )
    }
    if (data.inputs.any { input ->
            !usable(input.constant) || !usable(input.lowerBound) || !usable(input.upperBound) ||
                input.lowerBound > input.upperBound || input.terms.values.any { !usable(it) }
        } || data.bigMValues.any { !usable(it) }
    ) {
        return Failed(
            ErrorCode.IllegalArgument,
            "Gurobi 11 MAX 数值不可用：${data.name} / Invalid Gurobi 11 MAX numeric data: ${data.name}"
        )
    }
    return ok
}

private fun addGurobiNativeMaxData(
    model: GRBModel,
    variables: Map<VariableItemKey, GRBVar>,
    data: GurobiNativeMaxData
) {
    val nativeInputs = ArrayList<GRBVar>(data.inputs.size)
    var extremumConstant: Double? = null
    for ((index, input) in data.inputs.withIndex()) {
        val directVariable = directGurobiVariable(input)
        if (directVariable != null) {
            nativeInputs += variables.getValue(directVariable)
        } else if (input.terms.isEmpty()) {
            extremumConstant = extremumConstant?.let { current ->
                if (data.minimum) minOf(current, input.constant) else maxOf(current, input.constant)
            } ?: input.constant
        } else {
            val auxiliary = model.addVar(
                input.lowerBound,
                input.upperBound,
                0.0,
                GRB.CONTINUOUS,
                nativeElementName(
                    identityId = null,
                    fallbackName = "${data.name}_input_$index",
                    category = "max-input"
                )
            )
            val expression = GRBLinExpr()
            for ((key, coefficient) in input.terms) {
                expression.addTerm(coefficient, variables.getValue(key))
            }
            expression.addConstant(input.constant)
            model.addConstr(
                auxiliary,
                GRB.EQUAL,
                expression,
                "${data.name}_input_${index}_eq"
            )
            nativeInputs += auxiliary
        }
    }

    val result = variables.getValue(data.resultKey)
    if (nativeInputs.isEmpty()) {
        model.addConstr(result, GRB.EQUAL, extremumConstant ?: 0.0, data.name)
    } else if (data.minimum) {
        model.addGenConstrMin(
            result,
            nativeInputs.toTypedArray(),
            extremumConstant ?: GRB.INFINITY,
            data.name
        )
    } else {
        model.addGenConstrMax(
            result,
            nativeInputs.toTypedArray(),
            extremumConstant ?: -GRB.INFINITY,
            data.name
        )
    }
}

private fun directGurobiVariable(input: NativeMaxInputData): VariableItemKey? {
    if (input.constant != 0.0 || input.terms.size != 1) {
        return null
    }
    val entry = input.terms.entries.single()
    return entry.key.takeIf { entry.value == 1.0 }
}
