/**
 * 版本信息
 *
 * Version information for the ospf-kotlin-utils library.
 * ospf-kotlin-utils 库的版本信息。
 */
package fuookami.ospf.kotlin.utils.config

/**
 * 版本信息对象
 *
 * A singleton object containing version information for the library.
 * 包含库版本信息的单例对象。
 *
 * The version follows semantic versioning (MAJOR.MINOR.PATCH).
 * 版本遵循语义化版本规范（主版本.次版本.修订版本）。
 */
data object Version {
    /**
     * 主版本号
     *
     * The major version number, incremented for incompatible API changes.
     * 主版本号，当有不兼容的 API 更改时递增。
     */
    val majorVersion: UInt get() = 1U

    /**
     * 次版本号
     *
     * The minor version number, incremented for backward-compatible functionality additions.
     * 次版本号，当添加向后兼容的功能时递增。
     */
    val minorVersion: UInt get() = 0U

    /**
     * 修订版本号
     *
     * The patch version number, incremented for backward-compatible bug fixes.
     * 修订版本号，当进行向后兼容的错误修复时递增。
     */
    val modifyVersion: UInt get() = 0U

    /**
     * 完整版本字符串
     *
     * The complete version string in "MAJOR.MINOR.PATCH" format.
     * 完整的版本字符串，格式为"主版本.次版本.修订版本"。
     */
    val version: String by lazy { "$majorVersion.$minorVersion.$modifyVersion" }
}