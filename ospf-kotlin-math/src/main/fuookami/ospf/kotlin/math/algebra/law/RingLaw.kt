/**
 * 环定後
 * Ring Laws
 *
 * 提供环代数结构定律验证类，验证加法交换律、乘法结合律、乘法单位元存在性和分配律，继承臌GroupLaw 验证群定律。
 * Provides ring algebraic structure law validation class, verifying additive commutativity, multiplicative associativity, multiplicative identity existence, and distributivity, inheriting from GroupLaw for group law verification.
*/
package fuookami.ospf.kotlin.math.algebra.law

/**
 * 环定律验证器
 * Ring law validator
 *
 * 通过采样元素验证环的公理：加法交换律、乘法结合律、乘法单位元存在性、分配律。
 * 继承 GroupLaw 验证加法群定律。
 *
 * Validates ring axioms via sampled elements: additive commutativity, multiplicative associativity,
 * multiplicative identity existence, and distributivity. Inherits GroupLaw for additive group verification.
 *
 * @property Self 环元素类型 / Ring element type
 * @property samples 用于验证的采样元素集合 / Collection of sampled elements for verification
 * @property add 加法运算 / Addition operation
 * @property mul 乘法运算 / Multiplication operation
 * @property zero 加法单位元（零元） / Additive identity (zero element)
 * @property one 乘法单位元（幺元） / Multiplicative identity (one element)
 * @property negate 加法取反运算 / Additive negation operation
 * @property equal 相等性判定谓词 / Equality predicate
*/
class RingLaw<Self>(
    private val samples: Collection<Self>,
    private val add: (Self, Self) -> Self,
    private val mul: (Self, Self) -> Self,
    private val zero: Self,
    private val one: Self,
    private val negate: (Self) -> Self,
    private val equal: (Self, Self) -> Boolean
) {
    private val additiveGroup = GroupLaw(
        samples = samples,
        add = add,
        zero = zero,
        negate = negate,
        equal = equal
    )

    /**
     * 验证加法交换律
     * Verify additive commutativity
     *
     * @return 对所有采样元素满足加法交换律返回 true / True if additive commutativity holds for all sampled elements
    */
    fun additiveCommutative(): Boolean {
        for (lhs in samples) {
            for (rhs in samples) {
                if (!equal(add(lhs, rhs), add(rhs, lhs))) {
                    return false
                }
            }
        }
        return true
    }

    /**
     * 验证乘法结合律
     * Verify multiplicative associativity
     *
     * @return 对所有采样元素满足乘法结合律返回 true / True if multiplicative associativity holds for all sampled elements
    */
    fun multiplicativeAssociative(): Boolean {
        for (lhs in samples) {
            for (mid in samples) {
                for (rhs in samples) {
                    val left = mul(mul(lhs, mid), rhs)
                    val right = mul(lhs, mul(mid, rhs))
                    if (!equal(left, right)) {
                        return false
                    }
                }
            }
        }
        return true
    }

    /**
     * 验证乘法单位元存在性
     * Verify multiplicative identity existence
     *
     * @return 对所有采样元素满足乘法单位元性质返回 true / True if multiplicative identity holds for all sampled elements
    */
    fun multiplicativeIdentity(): Boolean {
        for (value in samples) {
            if (!equal(mul(value, one), value)) {
                return false
            }
            if (!equal(mul(one, value), value)) {
                return false
            }
        }
        return true
    }

    /**
     * 验证分配律
     * Verify distributivity
     *
     * @return 对所有采样元素满足分配律返回 true / True if distributivity holds for all sampled elements
    */
    fun distributive(): Boolean {
        for (lhs in samples) {
            for (mid in samples) {
                for (rhs in samples) {
                    val leftDistribute = mul(lhs, add(mid, rhs))
                    val leftExpected = add(mul(lhs, mid), mul(lhs, rhs))
                    if (!equal(leftDistribute, leftExpected)) {
                        return false
                    }

                    val rightDistribute = mul(add(lhs, mid), rhs)
                    val rightExpected = add(mul(lhs, rhs), mul(mid, rhs))
                    if (!equal(rightDistribute, rightExpected)) {
                        return false
                    }
                }
            }
        }
        return true
    }

    /**
     * 验证所有环定律（含群定律）
     * Validate all ring laws (including group laws)
     *
     * @return 所有环公理均满足返回 true / True if all ring axioms hold
    */
    fun validate(): Boolean {
        return additiveGroup.validate()
            && additiveCommutative()
            && multiplicativeAssociative()
            && multiplicativeIdentity()
            && distributive()
    }
}
