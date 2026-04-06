/**
 * 字段绑定
 * Field Binding
 *
 * 定义 PO 字段到数据库列的映射关系。
 * Defines the mapping between PO field and database column.
 */
package fuookami.ospf.kotlin.framework.persistence.expression

import org.ktorm.schema.ColumnDeclaring
import fuookami.ospf.kotlin.math.symbol.expression.PropertyPath

/**
 * 字段绑定
 * Field Binding
 *
 * 定义 PO 字段到数据库列的映射关系，支持可选的值转换器。
 * Defines the mapping between PO field and database column, with optional value transformer.
 *
 * @param T 列数据类型 / Column data type
 * @property path 属性路径 / Property path
 * @property column Ktorm 列声明 / Ktorm column declaring
 * @property transformer 值转换器 / Value transformer
 */
data class FieldBinding<T : Any>(
    val path: PropertyPath,
    val column: ColumnDeclaring<T>,
    val transformer: ValueTransformer<T>? = null
)

/**
 * 值转换器
 * Value Transformer
 *
 * 在 PO 值和数据库列值之间进行转换。
 * Converts between PO value and database column value.
 */
interface ValueTransformer<T : Any> {
    /**
     * 将 PO 值转换为列值
     * Convert PO value to column value
     */
    fun toColumn(value: Any?): T?

    /**
     * 将列值转换为 PO 值
     * Convert column value to PO value
     */
    fun fromColumn(value: T?): Any?
}

/**
 * 枚举转换器
 * Enum Transformer
 *
 * 用于枚举类型与字符串/整数之间的转换。
 * Converts between enum and string/integer.
 */
class EnumTransformer<T : Enum<T>, C : Any>(
    private val enumClass: Class<T>,
    private val toColumnValue: (T) -> C,
    private val fromColumnValue: (C) -> T?
) : ValueTransformer<C> {
    override fun toColumn(value: Any?): C? {
        @Suppress("UNCHECKED_CAST")
        return if (value != null && enumClass.isInstance(value)) {
            toColumnValue(value as T)
        } else {
            null
        }
    }

    override fun fromColumn(value: C?): Any? {
        return value?.let { fromColumnValue(it) }
    }

    companion object {
        /**
         * 创建枚举名称转换器
         * Create enum name transformer
         */
        inline fun <reified T : Enum<T>> byName(): EnumTransformer<T, String> {
            return EnumTransformer(
                T::class.java,
                { it.name },
                { value -> T::class.java.enumConstants.find { it.name == value } }
            )
        }

        /**
         * 创建枚举序号转换器
         * Create enum ordinal transformer
         */
        inline fun <reified T : Enum<T>> byOrdinal(): EnumTransformer<T, Int> {
            return EnumTransformer(
                T::class.java,
                { it.ordinal },
                { value -> T::class.java.enumConstants.getOrNull(value) }
            )
        }
    }
}