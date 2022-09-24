package fuookami.ospf.kotlin.utils.config

object Version {
    val majorVersion: UInt get() = 1U
    val minorVersion: UInt get() = 0U
    val modifyVersion: UInt get() = 0U
    val version: String get() = "$majorVersion.$minorVersion.$modifyVersion"
}
