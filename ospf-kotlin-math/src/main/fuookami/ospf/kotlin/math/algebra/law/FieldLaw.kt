/**
 * 域定後
 * Field Laws
 *
 * 提供域代数结构定律验证类，验证乘法交换律和乘法逆元存在性，继承臌RingLaw 验证环定律。
 * Provides field algebraic structure law validation class, verifying multiplicative commutativity and multiplicative inverse existence, inheriting from RingLaw for ring law verification.
*/
package fuookami.ospf.kotlin.math.algebra.law

/**
 * 域定律验证器
 * Field law validator
 *
 * 通过采样元素验证域的公理：乘法交换律、乘法逆元存在性。
 * 继承 RingLaw 验证环定律。
 *
 * Validates field axioms via sampled elements: multiplicative commutativity and multiplicative inverse existence.
 * Inherits RingLaw for ring law verification.
 *
 * @property Self 域元素类型 / Field element type
 * @property samples 用于验证的采样元素集合 / Collection of sampled elements for verification
 * @property add 加法运算 / Addition operation
 * @property mul 乘法运算 / Multiplication operation
 * @property zero 加法单位元（零元） / Additive identity (zero element)
 * @property one 乘法单位元（幺元） / Multiplicative identity (one element)
 * @property negate 加法取反运算 / Additive negation operation
 * @property reciprocal 乘法取逆运算 / Multiplicative reciprocal operation
 * @property isZero 判断元素是否为零元的谓词 / Predicate to check if an element is zero
 * @property equal 相等性判定谓词 / Equality predicate
*/
class FieldLaw<Self>(
    private val samples: Collection<Self>,
    private val add: (Self, Self) -> Self,
    private val mul: (Self, Self) -> Self,
    private val zero: Self,
    private val one: Self,
    private val negate: (Self) -> Self,
    private val reciprocal: (Self) -> Self,
    private val isZero: (Self) -> Boolean,
    private val equal: (Self, Self) -> Boolean
) {
    private val ring = RingLaw(
        samples = samples,
        add = add,
        mul = mul,
        zero = zero,
        one = one,
        negate = negate,
        equal = equal
    )

    /**
     * 验证乘法交换律
     * Verify multiplicative commutativity
     *
     * @return 对所有采样元素满足乘法交换律返回 true / True if multiplicative commutativity holds for all sampled elements
    */
    fun multiplicativeCommutative(): Boolean {
        for (lhs in samples) {
            for (rhs in samples) {
                if (!equal(mul(lhs, rhs), mul(rhs, lhs))) {
                    return false
                }
            }
        }
        return true
    }

    /**
     * 验证乘法逆元存在性
     * Verify multiplicative inverse existence
     *
     * @return 对所有非零采样元素满足乘法逆元性质返回 true / True if multiplicative inverse holds for all non-zero sampled elements
    */
    fun multiplicativeInverse(): Boolean {
        for (value in samples) {
            if (isZero(value)) {
                continue
            }
            val inverse = reciprocal(value)
            if (!equal(mul(value, inverse), one)) {
                return false
            }
            if (!equal(mul(inverse, value), one)) {
                return false
            }
        }
        return true
    }

    /**
     * 验证所有域定律（含环定律）
     * Validate all field laws (including ring laws)
     *
     * @return 所有域公理均满足返回 true / True if all field axioms hold
    */
    fun validate(): Boolean {
        return ring.validate()
            && multiplicativeCommutative()
            && multiplicativeInverse()
    }
}
