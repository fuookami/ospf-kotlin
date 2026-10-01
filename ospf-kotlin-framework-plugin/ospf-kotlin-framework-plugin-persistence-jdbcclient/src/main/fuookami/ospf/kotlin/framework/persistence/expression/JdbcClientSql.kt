/** JdbcClient SQL 工具 / JdbcClient SQL utilities */
package fuookami.ospf.kotlin.framework.persistence.expression

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.framework.persistence.jdbcclient.JdbcClientDialect

internal fun quoteIdentifierPath(path: String, dialect: JdbcClientDialect): Ret<String> {
    val parts = path.split('.')
    if (parts.isEmpty() || parts.any { it.isBlank() }) {
        return Failed(ErrorCode.IllegalArgument, "标识符路径无效：$path / Invalid identifier path: $path")
    }

    val quoted = mutableListOf<String>()
    for (part in parts) {
        when (val result = dialect.quoteIdentifier(part)) {
            is Ok -> quoted.add(result.value)
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
    }
    return Ok(quoted.joinToString("."))
}

internal fun <T> exceptionFailure(operation: String, exception: Exception): Ret<T> {
    return Failed(
        ErrorCode.ApplicationFailed,
        "$operation：${exception.message ?: exception::class.simpleName} / $operation: ${exception.message ?: exception::class.simpleName}"
    )
}
