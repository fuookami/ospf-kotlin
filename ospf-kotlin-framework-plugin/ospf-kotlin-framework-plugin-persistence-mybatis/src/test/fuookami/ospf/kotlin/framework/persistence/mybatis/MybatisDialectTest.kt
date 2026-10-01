/**
 * MyBatis dialect tests.
 * MyBatis 方言测试。
 */
package fuookami.ospf.kotlin.framework.persistence.mybatis

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.framework.persistence.expression.NullsOrder
import fuookami.ospf.kotlin.framework.persistence.expression.SortDirection
import fuookami.ospf.kotlin.framework.persistence.expression.translator.MybatisScalarSql
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Ok

@DisplayName("MybatisDialect Tests / MyBatis 方言测试")
class MybatisDialectTest {
    @Test
    @DisplayName("offset-only pagination should use the database dialect / 只有 offset 时应使用数据库方言")
    fun offsetOnlyPaginationShouldUseTheDatabaseDialect() {
        assertEquals("LIMIT -1 OFFSET 7", (MybatisDialect.SQLite.pagination(null, 7) as Ok).value.sql)
        assertEquals(
            "LIMIT 18446744073709551615 OFFSET 7",
            (MybatisDialect.MySQL.pagination(null, 7) as Ok).value.sql
        )
        assertEquals("OFFSET 7 ROWS", (MybatisDialect.PostgreSQL.pagination(null, 7) as Ok).value.sql)
        assertEquals("OFFSET 7 ROWS", (MybatisDialect.H2.pagination(null, 7) as Ok).value.sql)
        assertTrue(MybatisDialect.Portable.pagination(null, 7) is Failed)
    }

    @Test
    @DisplayName("NULL order should use supported syntax or portable emulation / NULL 顺序应使用支持语法或可移植模拟")
    fun nullOrderShouldUseSupportedSyntaxOrPortableEmulation() {
        val sqlite = MybatisDialect.SQLite.orderBy(
            column = "display_name",
            direction = SortDirection.Desc,
            nulls = NullsOrder.NullsLast
        )
        val postgres = MybatisDialect.PostgreSQL.orderBy(
            column = "display_name",
            direction = SortDirection.Desc,
            nulls = NullsOrder.NullsLast
        )

        assertEquals(
            "CASE WHEN `display_name` IS NULL THEN 1 ELSE 0 END ASC, `display_name` DESC",
            (sqlite as Ok).value.sql
        )
        assertEquals("\"display_name\" DESC NULLS LAST", (postgres as Ok).value.sql)
    }

    @Test
    @DisplayName("character length should be dialect-specific / 字符长度函数应匹配数据库方言")
    fun characterLengthShouldBeDialectSpecific() {
        val argument = MybatisScalarSql("{0}", listOf("é"))

        assertEquals(
            "LENGTH({0})",
            MybatisDialect.SQLite.scalarFunction("length", listOf(argument))?.sql
        )
        assertEquals(
            "CHAR_LENGTH({0})",
            MybatisDialect.MySQL.scalarFunction("length", listOf(argument))?.sql
        )
        assertEquals(
            "CHAR_LENGTH({0})",
            MybatisDialect.PostgreSQL.scalarFunction("length", listOf(argument))?.sql
        )
        assertEquals(
            "CHAR_LENGTH({0})",
            MybatisDialect.H2.scalarFunction("length", listOf(argument))?.sql
        )
        assertNull(MybatisDialect.Portable.scalarFunction("length", listOf(argument)))
        assertNull(MybatisDialect.SQLite.scalarFunction("abs", emptyList()))
    }

    @Test
    @DisplayName("invalid identifiers should return structured failures / 非法标识符应返回结构化失败")
    fun invalidIdentifiersShouldReturnStructuredFailures() {
        val result = MybatisDialect.MySQL.quoteIdentifier("name; DROP TABLE users")

        assertTrue(result is Failed)
        assertTrue((result as Failed<*, *, *>).error.value is MybatisDialectFailure)
    }

}
