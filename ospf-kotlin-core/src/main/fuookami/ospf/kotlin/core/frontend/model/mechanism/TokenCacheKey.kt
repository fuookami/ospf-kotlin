package fuookami.ospf.kotlin.core.frontend.model.mechanism

import fuookami.ospf.kotlin.utils.math.algebra.number.UInt64
import fuookami.ospf.kotlin.utils.math.symbol.Category
import fuookami.ospf.kotlin.core.frontend.variable.IdentifierGenerator

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
