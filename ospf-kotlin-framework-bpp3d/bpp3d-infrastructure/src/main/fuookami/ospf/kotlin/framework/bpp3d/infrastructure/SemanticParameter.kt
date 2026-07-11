/**
 * 语义参数基础设施。
 * Semantic parameter infrastructure.
*/
package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import kotlinx.serialization.Serializable
/**
 * MaterialNo class.
 * MaterialNo类。
 *
 * @property no the no property / no属性
*/
@JvmInline
@Serializable
value class MaterialNo(private val no: String) {
    override fun toString() = no
}

/**
 * PackagePattern class.
 * PackagePattern类。
 *
 * @property code the code property / code属性
*/
@JvmInline
@Serializable
value class PackagePattern(private val code: String) {
    override fun toString() = code

/**
 * belong.
 * belong。
 * @param ano another package pattern to check containment against / 用于判断包含关系的另一个包装模式
 * @return whether this pattern is a prefix of the other / 此模式是否为另一个模式的前缀
*/

    infix fun belong(ano: PackagePattern): Boolean {
        return ano.code.startsWith(code)
    }
}

/**
 * PackageCode class.
 * PackageCode类。
 *
 * @property code the code property / code属性
*/
@JvmInline
@Serializable
value class PackageCode(private val code: String) {
    override fun toString() = code

/**
 * belong.
 * belong。
 * @param ano another package code to check containment against / 用于判断包含关系的另一个包装代码
 * @return whether this code is a prefix of the other / 此代码是否为另一个代码的前缀
*/

    infix fun belong(ano: PackageCode): Boolean {
        return ano.code.startsWith(code)
    }
}

/**
 * BatchNo class.
 * BatchNo类。
 *
 * @property no the no property / no属性
*/
@JvmInline
@Serializable
value class BatchNo(private val no: String) {
    override fun toString() = no
}

val MultiBatchNo = BatchNo("*")
