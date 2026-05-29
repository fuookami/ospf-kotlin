/**
 * 群定後
 * Group Laws
 *
 * 提供群代数结构定律验证类，验证结合律、单位元存在性和逆元存在性。
 * Provides group algebraic structure law validation class, verifying associativity, identity existence, and inverse existence.
 */
package fuookami.ospf.kotlin.math.algebra.law

/**
 * 群定律验证器
 * Group law validator
 *
 * 通过采样元素验证群的三条公理：结合律、单位元存在性、逆元存在性。
 * Validates three group axioms via sampled elements: associativity, identity existence, and inverse existence.
 *
 * @property Self 群元素类型 / Group element type
 * @property samples 用于验证的采样元素集合 / Collection of sampled elements for verification
 * @property add 二元运算（群操作） / Binary operation (group operation)
 * @property zero 单位元 / Identity element
 * @property negate 取逆运算 / Inverse operation
 * @property equal 相等性判定谓词 / Equality predicate
 */
class GroupLaw<Self>(
    private val samples: Collection<Self>,
    private val add: (Self, Self) -> Self,
    private val zero: Self,
    private val negate: (Self) -> Self,
    private val equal: (Self, Self) -> Boolean
) {
    /**
     * 验证结合律
     * Verify associativity
     *
     * @return 对所有采样元素满足结合律返回 true / True if associativity holds for all sampled elements
     */
    fun isAssociative(): Boolean {
        for (lhs in samples) {
            for (mid in samples) {
                for (rhs in samples) {
                    val left = add(add(lhs, mid), rhs)
                    val right = add(lhs, add(mid, rhs))
                    if (!equal(left, right)) {
                        return false
                    }
                }
            }
        }
        return true
    }

    /**
     * 验证单位元存在性
     * Verify identity element existence
     *
     * @return 对所有采样元素满足单位元性质返回 true / True if identity property holds for all sampled elements
     */
    fun hasIdentity(): Boolean {
        for (value in samples) {
            if (!equal(add(value, zero), value)) {
                return false
            }
            if (!equal(add(zero, value), value)) {
                return false
            }
        }
        return true
    }

    /**
     * 验证逆元存在性
     * Verify inverse element existence
     *
     * @return 对所有采样元素满足逆元性质返回 true / True if inverse property holds for all sampled elements
     */
    fun hasInverse(): Boolean {
        for (value in samples) {
            val inverse = negate(value)
            if (!equal(add(value, inverse), zero)) {
                return false
            }
            if (!equal(add(inverse, value), zero)) {
                return false
            }
        }
        return true
    }

    /**
     * 验证所有群定律
     * Validate all group laws
     *
     * @return 结合律、单位元、逆元均满足返回 true / True if associativity, identity, and inverse all hold
     */
    fun validate(): Boolean {
        return isAssociative() && hasIdentity() && hasInverse()
    }
}
