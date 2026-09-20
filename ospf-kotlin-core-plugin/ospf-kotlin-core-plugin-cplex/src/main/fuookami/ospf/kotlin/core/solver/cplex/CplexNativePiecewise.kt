package fuookami.ospf.kotlin.core.solver.cplex

import ilog.concert.IloNumVar
import ilog.cplex.IloCplex
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.solver.NativePiecewiseData
import fuookami.ospf.kotlin.core.variable.VariableItemKey

/** CPLEX 原生 PWL 写入注入点。 / Injection point for CPLEX native PWL writes. */
internal fun interface CplexNativePiecewiseWriter {
    fun add(data: NativePiecewiseData)
}

private fun invalidNativePiecewise(reason: String): Try {
    return Failed(
        ErrorCode.OREngineModelingException,
        "CPLEX 原生 PWL 数据无效：$reason / Invalid CPLEX native PWL data: $reason"
    )
}

private fun nativeWriteFailure(reason: String): Try {
    return Failed(
        ErrorCode.OREngineModelingException,
        "CPLEX 原生 PWL 写入失败：$reason / CPLEX native PWL write failed: $reason"
    )
}

/**
 * 预检整批原生 PWL 数据后再调用 SDK writer。 / Preflight a complete native-PWL batch before invoking the SDK writer.
 *
 * 已写入的 SDK 对象不在此处回滚；调用方必须在失败时销毁整个模型。 /
 * SDK objects already written are not rolled back here; the caller must dispose the whole model on failure.
 */
internal fun writeCplexNativePiecewise(
    variableKeys: Set<VariableItemKey>,
    data: List<NativePiecewiseData>,
    writer: CplexNativePiecewiseWriter
): Try {
    val resultKeys = HashSet<VariableItemKey>()
    for (piecewise in data) {
        if (piecewise.xPoints.size < 2 ||
            piecewise.yPoints.size != piecewise.xPoints.size
        ) {
            return invalidNativePiecewise("断点和输出点数量不匹配 / breakpoint and output-point counts differ")
        }
        if (piecewise.xPoints.any { !it.isFinite() } || piecewise.yPoints.any { !it.isFinite() }) {
            return invalidNativePiecewise("存在非有限数值 / a non-finite value is present")
        }
        if ((1 until piecewise.xPoints.size).any { index ->
                piecewise.xPoints[index - 1] >= piecewise.xPoints[index]
            }
        ) {
            return invalidNativePiecewise("断点必须严格递增 / breakpoints must be strictly increasing")
        }
        if (piecewise.inputKey !in variableKeys || piecewise.resultKey !in variableKeys) {
            return invalidNativePiecewise("输入或结果变量缺失 / input or result variable is missing")
        }
        if (!resultKeys.add(piecewise.resultKey)) {
            return invalidNativePiecewise("结果变量重复 / result variable is duplicated")
        }
    }

    for (piecewise in data) {
        try {
            writer.add(piecewise)
        } catch (error: LinkageError) {
            return nativeWriteFailure(
                "SDK API 不可用：${error.message ?: error::class.simpleName} / " +
                    "SDK API is unavailable: ${error.message ?: error::class.simpleName}"
            )
        } catch (error: Exception) {
            return nativeWriteFailure(
                "${error.message ?: error::class.simpleName} / ${error::class.simpleName}"
            )
        }
    }
    return ok
}

/**
 * 用 lambda 等式和 SOS2 写入连续一元 PWL，并保留结果变量。 /
 * Write a continuous univariate PWL with lambda equalities and SOS2 while retaining the result variable.
 */
internal fun addCplexNativePiecewise(
    model: IloCplex,
    variables: Map<VariableItemKey, IloNumVar>,
    data: List<NativePiecewiseData>
): Try {
    return writeCplexNativePiecewise(
        variableKeys = variables.keys,
        data = data,
        writer = CplexNativePiecewiseWriter { piecewise ->
            val input = variables.getValue(piecewise.inputKey)
            val result = variables.getValue(piecewise.resultKey)
            val lambdaNames = Array(piecewise.xPoints.size) { index ->
                "${piecewise.name}_lambda_$index"
            }
            val lambda = model.numVarArray(
                piecewise.xPoints.size,
                0.0,
                1.0,
                lambdaNames
            )
            val sum = model.linearNumExpr()
            val xLink = model.linearNumExpr()
            val yLink = model.linearNumExpr()
            for (index in lambda.indices) {
                sum.addTerm(1.0, lambda[index])
                xLink.addTerm(piecewise.xPoints[index], lambda[index])
                yLink.addTerm(piecewise.yPoints[index], lambda[index])
            }
            model.addEq(sum, 1.0, "${piecewise.name}_lambda_sum")
            model.addEq(input, xLink, "${piecewise.name}_x_link")
            model.addEq(result, yLink, "${piecewise.name}_y_link")
            model.addSOS2(lambda, piecewise.xPoints, "${piecewise.name}_sos2")
        }
    )
}
