package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import kotlinx.serialization.*

@JvmInline
@Serializable
value class MaterialNo(private val no: String) {
    override fun toString() = no
}

@JvmInline
@Serializable
value class PackagePattern(private val code: String) {
    override fun toString() = code

    infix fun belong(ano: PackagePattern): Boolean {
        return ano.code.startsWith(code)
    }
}

@JvmInline
@Serializable
value class PackageCode(private val code: String) {
    override fun toString() = code

    infix fun belong(ano: PackageCode): Boolean {
        return ano.code.startsWith(code)
    }
}

@JvmInline
@Serializable
value class BatchNo(private val no: String) {
    override fun toString() = no
}

val MultiBatchNo = BatchNo("*")
