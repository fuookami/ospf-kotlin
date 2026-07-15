/**
 * Token 缓存键 / Token cache key
*/
package fuookami.ospf.kotlin.core.token

import fuookami.ospf.kotlin.core.variable.IdentifierGenerator
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.Category

/**
 * Token 缓存键，由类别、前缀和唯一标识符组成。
 * Token cache key composed of category, prefix, and unique identifier.
 *
 * @property category 符号操作类别 / Symbol operation category
 * @property prefix 键前缀 / Key prefix
 * @property identifier 唯一标识符 / Unique identifier
*/
internal data class TokenCacheKey(
    val category: Category,
    val prefix: String,
    val identifier: UInt64 = IdentifierGenerator.gen()
) {
    override fun toString(): String {
        return "${prefix}_${identifier}"
    }
}

/**
 * 创建新的 Token 缓存键，标识符自动生成。
 * Creates a new token cache key with auto-generated identifier.
 *
 * @param category 符号操作类别 / Symbol operation category
 * @param prefix 键前缀 / Key prefix
 * @return 新的缓存键 / New cache key
*/
internal fun newTokenCacheKey(
    category: Category,
    prefix: String
): TokenCacheKey {
    return TokenCacheKey(
        category = category,
        prefix = prefix
    )
}
