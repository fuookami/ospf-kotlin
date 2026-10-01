/**
 * MyBatis 排序翻译器
 * MyBatis Order By Translator
 *
 * 将 SortBy 翻译为 MyBatis-Plus ORDER BY 子句。
 * Translates SortBy to MyBatis-Plus ORDER BY clause.
*/
package fuookami.ospf.kotlin.framework.persistence.expression.translator

import java.util.concurrent.CancellationException
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import fuookami.ospf.kotlin.framework.persistence.expression.*
import fuookami.ospf.kotlin.framework.persistence.mybatis.MybatisDialect
import fuookami.ospf.kotlin.framework.persistence.mybatis.MybatisSqlClause
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * MyBatis 排序翻译器
 * MyBatis Order By Translator
 *
 * 将 SortBy 模型翻译为 MyBatis-Plus 排序表达式。
 * Translates SortBy model to MyBatis-Plus order expressions.
 *
 * @param T 实体类型 / Entity type
 * @property resolveColumnName 列名解析函数 / Column name resolver function
 * @property nullsOrderSupport 空值排序支持检测 / Nulls order support detection
 * @property dialect SQL 方言 / SQL dialect
*/
class MybatisOrderByTranslator<T : Any>(
    private val resolveColumnName: MybatisColumnNameResolver,
    private val nullsOrderSupport: NullsOrderSupport = NullsOrderSupport.Auto,
    private val dialect: MybatisDialect = MybatisDialect.Portable
) {

    /**
     * 应用排序到 Wrapper
     * Apply sort to wrapper
     *
     * @param wrapper MyBatis-Plus 查询 Wrapper / MyBatis-Plus query wrapper
     * @param sortBy 排序条件 / Sort conditions
     * @return 应用排序后的 QueryWrapper / QueryWrapper with sort applied
    */
    fun apply(wrapper: QueryWrapper<T>, sortBy: SortBy): Ret<QueryWrapper<T>> {
        return translate(sortBy).map { clause ->
            if (clause.sql.isBlank()) wrapper else wrapper.last("ORDER BY ${clause.sql}")
        }
    }

    /**
     * 翻译排序项为 SQL 子句 / Translate sort items to an SQL clause
     *
     * @param sortBy 排序条件 / Sort conditions
     * @return 排序子句或结构化错误 / Order clause or structured failure
     */
    fun translate(sortBy: SortBy): Ret<MybatisSqlClause> {
        return try {
            val clauses = mutableListOf<String>()
            for (item in sortBy.items) {
                val column = resolveColumnName(item.path) ?: return Failed(
                    ErrorCode.IllegalArgument,
                    "排序字段无法解析：${item.path} / Cannot resolve sort field: ${item.path}"
                )
                when (val translated = dialect.orderBy(
                    column = column,
                    direction = item.direction,
                    nulls = item.nulls,
                    support = nullsOrderSupport
                )) {
                    is Ok -> clauses += translated.value.sql
                    is Failed -> return Failed(translated.error)
                    is Fatal -> return Fatal(translated.errors)
                }
            }
            Ok(MybatisSqlClause(clauses.joinToString(", ")))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            persistenceFailure("MyBatis order translation", error)
        }
    }
}
