/**
 * 求解器建模数据准备工具 / Solver modeling data preparation utilities
*/
package fuookami.ospf.kotlin.core.solver

import java.security.MessageDigest
import fuookami.ospf.kotlin.core.model.basic.Variable
import fuookami.ospf.kotlin.core.solver.value.toSolverDouble
import fuookami.ospf.kotlin.core.solver.report.ModelElementScope

/**
 * solver 边界公共 helper（仅用于 solver dump 前数据准备）。
 * Shared helper at solver boundary (only for pre-dump data preparation).
 *
 * @property lowerBounds 变量下界数组 / Variable lower bounds array
 * @property upperBounds 变量上界数组 / Variable upper bounds array
 * @property names 变量名称数组 / Variable names array
 * @property initialResults 初始解列表（列索引, 值）/ Initial solution list (column index, value)
*/
data class VariableDumpingData(
    val lowerBounds: DoubleArray,
    val upperBounds: DoubleArray,
    val names: Array<String>,
    val initialResults: List<Pair<Int, Double>>
)

/**
 * 准备变量转储数据，将变量列表转换为求解器所需的数组格式。 / Prepare variable dumping data, converting variable list to array format required by solver.
 *
 * @param variables 变量列表 / Variable list
 * @param scopeName 作用域名称（用于错误信息）/ Scope name (for error messages)
 * @return 变量转储数据 / Variable dumping data
*/
fun prepareVariableDumpingData(
    variables: List<Variable>,
    scopeName: String
): VariableDumpingData {
    val variableAmount = variables.size
    val lowerBounds = DoubleArray(variableAmount)
    val upperBounds = DoubleArray(variableAmount)
    val names = Array(variableAmount) { "" }
    val initialResults = ArrayList<Pair<Int, Double>>()
    for ((col, variable) in variables.withIndex()) {
        lowerBounds[col] = variable.lowerBound.toSolverDouble("$scopeName.variables[$col].lowerBound")
        upperBounds[col] = variable.upperBound.toSolverDouble("$scopeName.variables[$col].upperBound")
        names[col] = variable.name
        variable.initialResult?.let {
            initialResults.add(col to it.toSolverDouble("$scopeName.variables[$col].initialResult"))
        }
    }
    return VariableDumpingData(
        lowerBounds = lowerBounds,
        upperBounds = upperBounds,
        names = names,
        initialResults = initialResults
    )
}

/**
 * 为原生求解器生成可追踪的 artifact 名称。 / Build a traceable native artifact name.
 *
 * Stable source elements are projected to a namespaced, sanitized native name so exported native
 * artifacts can be traced back to the source identity. Model-local elements keep their display
 * name so they cannot be mistaken for cross-rebuild identities. / 稳定源元素投影为带命名空间且
 * 经过净化的原生名称，使导出的原生 artifact 可以回查到源身份；model-local 元素保留展示名称，
 * 避免被误认为跨重建身份。
 *
 * @param identityId 元素 ID；只有 Stable 作用域才会投影 / Element ID; only Stable scope is projected
 * @param identityScope 元素身份作用域 / Element identity scope
 * @param fallbackName 无稳定 ID 时的原生名称 / Native name used without a stable ID
 * @param category 元素类别，如 variable/constraint / Element category, e.g. variable/constraint
 * @return 原生 artifact 名称 / Native artifact name
 */
fun nativeElementName(
    identityId: String?,
    fallbackName: String,
    category: String,
    identityScope: ModelElementScope = ModelElementScope.ModelLocal
): String {
    val id = identityId?.takeIf {
        identityScope == ModelElementScope.Stable && it.isNotBlank()
    }
    return if (id == null) fallbackName else "ospf-$category-${sanitizeNativeName(id)}"
}

/**
 * 原生求解器名称的最大长度。 / Maximum length of a native solver name.
 *
 * SCIP/Gurobi 对名称长度有软/硬限制，投影名称需要保留余量；超过该长度时按
 * “安全前缀 + SHA-256 摘要”截断。 / Both SCIP and Gurobi limit name length; longer
 * names are truncated to a safe prefix plus a SHA-256 digest.
 */
private const val MAX_NATIVE_NAME_LENGTH = 180

private const val NATIVE_NAME_HEX_DIGITS = "0123456789abcdef"

/**
 * 净化原生求解器名称中的非法字符。 / Sanitize characters unsupported by native solver names.
 *
 * 使用可逆的 UTF-8 字节十六进制转义：保留 `[A-Za-z0-9.:-]`，其余字节（含下划线自身）
 * 编码为 `_h<2 位 hex>_`，因此 `a/b`、`a b`、`a_b` 等输入不会碰撞，短名称可以无损往返。
 * 编码结果超过 [MAX_NATIVE_NAME_LENGTH] 时，截断为安全前缀并追加确定性 SHA-256 摘要，
 * 该超长分支只保证确定性与唯一性，不承诺可逆。 / Escapes every UTF-8 byte outside
 * `[A-Za-z0-9.:-]` (including the underscore itself) as `_h<2-hex>_`, so inputs like
 * `a/b`, `a b` and `a_b` never collide and short names round-trip losslessly. Names
 * longer than [MAX_NATIVE_NAME_LENGTH] are truncated to a safe prefix plus a
 * deterministic SHA-256 digest; that branch is deterministic and collision-safe but
 * not reversible.
 *
 * @param value 原始名称 / Raw name
 * @return 净化后的名称 / Sanitized name
 */
fun sanitizeNativeName(value: String): String {
    val encoded = encodeNativeName(value)
    return if (encoded.length <= MAX_NATIVE_NAME_LENGTH) {
        encoded
    } else {
        val digest = sha256Hex(encoded)
        encoded.take(MAX_NATIVE_NAME_LENGTH - digest.length - 1) + "_" + digest
    }
}

private fun encodeNativeName(value: String): String = buildString(value.length + 8) {
    for (byte in value.toByteArray(Charsets.UTF_8)) {
        val unsigned = byte.toInt() and 0xFF
        val ch = unsigned.toChar()
        if (ch in 'A'..'Z' || ch in 'a'..'z' || ch in '0'..'9' || ch == '.' || ch == ':' || ch == '-') {
            append(ch)
        } else {
            append('_')
            append('h')
            append(NATIVE_NAME_HEX_DIGITS[unsigned ushr 4])
            append(NATIVE_NAME_HEX_DIGITS[unsigned and 0x0F])
            append('_')
        }
    }
}

private fun sha256Hex(value: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
    return digest.joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xFF) }
}

/**
 * 计算约束分段大小，用于并发转储时的任务划分。 / Compute constraint segment size for task partitioning during concurrent dumping.
 *
 * @param constraintSize 约束总数 / Total constraint count
 * @param availableProcessors 可用处理器数 / Available processor count
 * @return 分段大小 / Segment size
*/
fun computeConstraintSegmentSize(
    constraintSize: Int,
    availableProcessors: Int = Runtime.getRuntime().availableProcessors()
): Int {
    if (constraintSize <= 0) {
        return 10
    }
    val workerCount = (availableProcessors - 1).coerceAtLeast(1)
    var ratio = constraintSize / workerCount
    if (ratio < 10) {
        return 10
    }
    var segment = 1
    while (ratio >= 10) {
        ratio /= 10
        segment *= 10
    }
    return segment
}
