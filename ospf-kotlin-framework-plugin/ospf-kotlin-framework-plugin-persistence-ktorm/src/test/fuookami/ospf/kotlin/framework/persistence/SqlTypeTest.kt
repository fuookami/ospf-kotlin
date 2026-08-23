package fuookami.ospf.kotlin.framework.persistence

import java.nio.file.Files
import kotlin.time.Duration
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.milliseconds
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.ktorm.database.Database
import org.ktorm.dsl.from
import org.ktorm.dsl.insert
import org.ktorm.dsl.select
import org.ktorm.schema.Table
import org.ktorm.support.sqlite.SQLiteDialect
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64

private object DurationTable : Table<Nothing>("t_duration") {
    val durationMs = durationMs("duration_ms")
}

private object NumericTable : Table<Nothing>("t_numeric") {
    val sequence = ui64("sequence")
    val ratio = fltx("ratio")
}

class SqlTypeTest {
    @Test
    fun `durationMs stores whole milliseconds`() {
        val database = createDatabase()
        val duration = 1500.milliseconds + 750.microseconds

        database.insert(DurationTable) {
            set(DurationTable.durationMs, duration)
        }

        database.useConnection { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT duration_ms FROM t_duration").use { resultSet ->
                    assertEquals(true, resultSet.next())
                    assertEquals(1500L, resultSet.getLong("duration_ms"))
                }
            }
        }

        val row = database.from(DurationTable).select().iterator().next()
        assertEquals(1500.milliseconds, row[DurationTable.durationMs])
    }

    @Test
    fun `durationMs rejects values outside the finite SQL range`() {
        val database = createDatabase()

        assertThrows(IllegalArgumentException::class.java) {
            database.insert(DurationTable) {
                set(DurationTable.durationMs, Duration.INFINITE)
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            database.insert(DurationTable) {
                set(DurationTable.durationMs, -Duration.INFINITE)
            }
        }
    }

    @Test
    fun `numeric value objects use transformed JDBC values`() {
        val database = createNumericDatabase()
        val sequence = UInt64(7UL)
        val ratio = FltX("1.25")

        database.insert(NumericTable) {
            set(NumericTable.sequence, sequence)
            set(NumericTable.ratio, ratio)
        }

        database.useConnection { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT sequence, ratio FROM t_numeric").use { resultSet ->
                    assertEquals(true, resultSet.next())
                    assertEquals(7L, resultSet.getLong("sequence"))
                    assertEquals(0, resultSet.getBigDecimal("ratio").compareTo(java.math.BigDecimal("1.25")))
                }
            }
        }

        val row = database.from(NumericTable).select().iterator().next()
        assertEquals(sequence, row[NumericTable.sequence])
        assertEquals(ratio, row[NumericTable.ratio])
    }

    private fun createDatabase(): Database {
        val dbFile = Files.createTempFile("ospf-duration-test", ".db").toFile().apply {
            deleteOnExit()
        }
        return Database.connect(
            url = "jdbc:sqlite:${dbFile.absolutePath}",
            dialect = SQLiteDialect()
        ).also { database ->
            database.useConnection { connection ->
                connection.createStatement().use { statement ->
                    statement.execute(
                        "CREATE TABLE t_duration (duration_ms BIGINT NOT NULL)"
                    )
                }
            }
        }
    }

    private fun createNumericDatabase(): Database {
        val dbFile = Files.createTempFile("ospf-numeric-test", ".db").toFile().apply {
            deleteOnExit()
        }
        return Database.connect(
            url = "jdbc:sqlite:${dbFile.absolutePath}",
            dialect = SQLiteDialect()
        ).also { database ->
            database.useConnection { connection ->
                connection.createStatement().use { statement ->
                    statement.execute(
                        "CREATE TABLE t_numeric (sequence BIGINT NOT NULL, ratio DECIMAL(30, 18) NOT NULL)"
                    )
                }
            }
        }
    }
}
