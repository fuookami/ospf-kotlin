package fuookami.ospf.kotlin.core.solver.scip

import kotlin.math.abs
import jscip.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.solver.NativePiecewiseData
import fuookami.ospf.kotlin.core.variable.VariableItemKey

/** SCIP 原生 PWL 数据别名。 / Alias for the core native PWL data. */
internal typealias ScipNativePiecewiseData = NativePiecewiseData

/**
 * 检查当前 JSCIP 是否暴露精确的 SOS2 创建方法。 /
 * Check whether the current JSCIP exposes the exact SOS2 creator method.
 */
internal fun scipSupportsNativePiecewise(): Boolean {
    return try {
        val variableArrayClass = java.lang.reflect.Array.newInstance(Variable::class.java, 0).javaClass
        val method = Scip::class.java.getMethod(
            "createConsSOS2",
            String::class.java,
            variableArrayClass,
            DoubleArray::class.java
        )
        method.returnType == Constraint::class.java
    } catch (_: NoSuchMethodException) {
        false
    } catch (_: LinkageError) {
        false
    }
}

/**
 * 在调用 SCIP 前批量校验 primitive 数据。 /
 * Validate all primitive data before invoking SCIP.
 *
 * @param variableKeys SCIP 模型中可用的变量键 / Variable keys available in the SCIP model
 * @param data 待写入的 PWL 数据 / PWL data to write
 * @param infinity SCIP 的 infinity 哨兵值 / SCIP infinity sentinel
 * @return 校验结果 / Validation result
 */
internal fun validateScipNativePiecewiseData(
    variableKeys: Set<VariableItemKey>,
    data: List<NativePiecewiseData>,
    infinity: Double
): Try {
    if (!infinity.isFinite() || infinity <= 0.0) {
        return Failed(
            ErrorCode.OREngineModelingException,
            "SCIP infinity 哨兵无效：$infinity / Invalid SCIP infinity sentinel: $infinity"
        )
    }
    for (piecewise in data) {
        if (piecewise.xPoints.size < 2 || piecewise.yPoints.size != piecewise.xPoints.size) {
            return Failed(
                ErrorCode.IllegalArgument,
                "SCIP PWL 断点与函数值数量无效：${piecewise.name} / " +
                    "Invalid SCIP PWL breakpoint and value counts: ${piecewise.name}"
            )
        }
        if (piecewise.inputKey !in variableKeys || piecewise.resultKey !in variableKeys) {
            return Failed(
                ErrorCode.IllegalArgument,
                "SCIP PWL 变量缺失：${piecewise.name} / " +
                    "Missing SCIP PWL variable: ${piecewise.name}"
            )
        }
        if (piecewise.xPoints.any { value -> !value.isFinite() || abs(value) >= infinity } ||
            piecewise.yPoints.any { value -> !value.isFinite() || abs(value) >= infinity }
        ) {
            return Failed(
                ErrorCode.IllegalArgument,
                "SCIP PWL 存在超出有限范围的数值：${piecewise.name} / " +
                    "SCIP PWL contains a non-finite or sentinel-bound value: ${piecewise.name}"
            )
        }
        if ((1 until piecewise.xPoints.size).any { index ->
                piecewise.xPoints[index - 1] >= piecewise.xPoints[index]
            }
        ) {
            return Failed(
                ErrorCode.IllegalArgument,
                "SCIP PWL 断点必须严格递增：${piecewise.name} / " +
                    "SCIP PWL breakpoints must be strictly increasing: ${piecewise.name}"
            )
        }
    }
    return ok
}

private fun recordScipFailure(
    errors: MutableList<Error<ErrorCode>>,
    operation: String,
    error: Throwable
) {
    val detail = error.message ?: error::class.simpleName
    errors += Err(
        ErrorCode.OREngineModelingException,
        "SCIP 操作失败：$operation，$detail / SCIP operation failed: $operation, $detail"
    )
}

private inline fun recordScipOperation(
    errors: MutableList<Error<ErrorCode>>,
    operation: String,
    action: () -> Unit
) {
    try {
        action()
    } catch (error: Exception) {
        recordScipFailure(errors, operation, error)
    } catch (error: LinkageError) {
        recordScipFailure(errors, operation, error)
    }
}

/**
 * 将连续一元 PWL 写入 SCIP 的 lambda/SOS2 表达。 /
 * Write a continuous univariate PWL to SCIP using lambda equations and SOS2.
 *
 * @param scip SCIP 模型 / SCIP model
 * @param variables 已创建的 SCIP 变量 / Already-created SCIP variables
 * @param data 已校验的 primitive PWL 数据 / Validated primitive PWL data
 * @return 写入结果；失败时模型由调用方丢弃 / Write result; the caller discards the model on failure
 */
internal fun addScipNativePiecewise(
    scip: Scip,
    variables: Map<VariableItemKey, Variable>,
    data: List<NativePiecewiseData>
): Try {
    val createdVariables = ArrayList<Variable>()
    val createdConstraints = ArrayList<Constraint>()
    val errors = ArrayList<Error<ErrorCode>>()

    fun releaseCaptured() {
        for (constraint in createdConstraints.asReversed()) {
            recordScipOperation(errors, "释放 SCIP 约束 creator 引用 / release a SCIP constraint creator reference") {
                scip.releaseCons(constraint)
            }
        }
        for (variable in createdVariables.asReversed()) {
            recordScipOperation(errors, "释放 SCIP 变量 creator 引用 / release a SCIP variable creator reference") {
                scip.releaseVar(variable)
            }
        }
    }

    try {
        if (!scipSupportsNativePiecewise()) {
            errors += Err(
                ErrorCode.OREngineModelingException,
                "当前 JSCIP 未暴露 createConsSOS2(String, Variable[], double[]) / " +
                    "The current JSCIP does not expose createConsSOS2(String, Variable[], double[])"
            )
        } else {
            val infinity = scip.infinity()
            when (val validation = validateScipNativePiecewiseData(variables.keys, data, infinity)) {
                is Ok -> {
                    for (piecewise in data) {
                        val input = variables[piecewise.inputKey]
                        val result = variables[piecewise.resultKey]
                        if (input == null || result == null) {
                            errors += Err(
                                ErrorCode.IllegalArgument,
                                "SCIP PWL 变量映射在写入期间缺失：${piecewise.name} / " +
                                    "SCIP PWL variable mapping disappeared during write: ${piecewise.name}"
                            )
                            break
                        }

                        val lambda = ArrayList<Variable>(piecewise.xPoints.size)
                        for (index in piecewise.xPoints.indices) {
                            val variable = scip.createVar(
                                "${piecewise.name}-lambda-$index",
                                0.0,
                                1.0,
                                0.0,
                                SCIP_Vartype.SCIP_VARTYPE_CONTINUOUS
                            )
                            createdVariables += variable
                            lambda += variable
                        }

                        val sumCoefficients = DoubleArray(lambda.size) { 1.0 }
                        val sumConstraint = scip.createConsLinear(
                            "${piecewise.name}-lambda-sum",
                            lambda.toTypedArray(),
                            sumCoefficients,
                            1.0,
                            1.0
                        )
                        createdConstraints += sumConstraint
                        scip.addCons(sumConstraint)

                        val xVariables = arrayOf(input) + lambda.toTypedArray()
                        val xCoefficients = DoubleArray(xVariables.size)
                        xCoefficients[0] = 1.0
                        for (index in piecewise.xPoints.indices) {
                            xCoefficients[index + 1] = -piecewise.xPoints[index]
                        }
                        val xConstraint = scip.createConsLinear(
                            "${piecewise.name}-x-link",
                            xVariables,
                            xCoefficients,
                            0.0,
                            0.0
                        )
                        createdConstraints += xConstraint
                        scip.addCons(xConstraint)

                        val yVariables = arrayOf(result) + lambda.toTypedArray()
                        val yCoefficients = DoubleArray(yVariables.size)
                        yCoefficients[0] = 1.0
                        for (index in piecewise.yPoints.indices) {
                            yCoefficients[index + 1] = -piecewise.yPoints[index]
                        }
                        val yConstraint = scip.createConsLinear(
                            "${piecewise.name}-y-link",
                            yVariables,
                            yCoefficients,
                            0.0,
                            0.0
                        )
                        createdConstraints += yConstraint
                        scip.addCons(yConstraint)

                        val sos2Constraint = scip.createConsSOS2(
                            "${piecewise.name}-sos2",
                            lambda.toTypedArray(),
                            DoubleArray(lambda.size) { index -> index.toDouble() + 1.0 }
                        )
                        createdConstraints += sos2Constraint
                        scip.addCons(sos2Constraint)
                    }
                }

                is Failed -> errors += validation.error
                is Fatal -> errors += validation.errors
            }
        }
    } catch (error: Exception) {
        recordScipFailure(errors, "写入 SCIP PWL / write SCIP PWL", error)
    } catch (error: LinkageError) {
        recordScipFailure(errors, "写入 SCIP PWL / write SCIP PWL", error)
    } finally {
        releaseCaptured()
    }
    return when (errors.size) {
        0 -> ok
        1 -> Failed(errors.single())
        else -> Fatal(errors.toList())
    }
}
