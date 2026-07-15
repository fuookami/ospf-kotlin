/**
 * 模型文件格式
 * Model file format
*/
package fuookami.ospf.kotlin.core.model.basic

/**
 * 模型文件导出格式枚举。
 * Enumeration of model file export formats.
*/
enum class ModelFileFormat {
    /** LP 格式 / LP format */
    LP {
        override fun toString() = "lp"
    }
}
