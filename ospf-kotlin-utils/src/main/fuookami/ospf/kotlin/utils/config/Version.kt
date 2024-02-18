package fuookami.ospf.kotlin.utils.config

data object Version {
    val majorVersion: UInt get() = 1U
    val minorVersion: UInt get() = 0U
    val modifyVersion: UInt get() = 0U
    val version: String by lazy { "$majorVersion.$minorVersion.$modifyVersion" }
}
