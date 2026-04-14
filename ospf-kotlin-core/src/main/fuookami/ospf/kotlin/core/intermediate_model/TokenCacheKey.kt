package fuookami.ospf.kotlin.core.intermediate_model

import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.Category
import fuookami.ospf.kotlin.core.variable.IdentifierGenerator

internal data class TokenCacheKey(
    val category: Category,
    val prefix: String,
    val identifier: UInt64 = IdentifierGenerator.gen()
) {
    override fun toString(): String {
        return "${prefix}_${identifier}"
    }
}

internal fun newTokenCacheKey(
    category: Category,
    prefix: String
): TokenCacheKey {
    return TokenCacheKey(
        category = category,
        prefix = prefix
    )
}
