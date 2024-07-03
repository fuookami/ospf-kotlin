package fuookami.ospf.kotlin.framework.persistence

import kotlinx.serialization.*
import org.ktorm.database.*
import org.ktorm.support.sqlite.*
import org.apache.commons.dbcp2.*
import org.apache.logging.log4j.kotlin.*

data class SqliteClientKey(
    val name: String
)

data class SqliteConfigBuilder(
    var url: String? = null,
    var name: String? = null,
    val properties: MutableMap<String, String> = mutableMapOf(),
    var maxTotal: Int = 20,
    var maxIdle: Int = 10,
    var maxOpenPreparedStatements: Int = 100
) {
    private val logger = logger()

    operator fun invoke(): SqliteConfig? {
        return try {
            SqliteConfig(
                url = url!!,
                name = name ?: "",
                properties = properties,
                maxTotal = maxTotal,
                maxIdle = maxIdle,
                maxOpenPreparedStatements = maxOpenPreparedStatements
            )
        } catch (e: Exception) {
            if (url == null) {
                logger.error("url is not set")
            }
            null
        }
    }
}

@Serializable
data class SqliteConfig(
    val url: String,
    val name: String,
    val properties: Map<String, String> = emptyMap(),
    val maxTotal: Int = 20,
    val maxIdle: Int = 10,
    val maxOpenPreparedStatements: Int = 100
) {
    val key get() = SqliteClientKey(name = name)
}

object Sqlite {
    @get:Synchronized
    private val clients: MutableMap<SqliteClientKey, BasicDataSource> = HashMap()

    @Synchronized
    fun init(builder: SqliteConfigBuilder.() -> Unit): Database? {
        val config = SqliteConfigBuilder()
        builder(config)
        return config()?.let { this(it) }
    }

    @Synchronized
    operator fun invoke(config: SqliteConfig): Database? {
        if (clients.containsKey(config.key)) {
            return Database.connect(clients[config.key]!!, SQLiteDialect())
        }

        return try {
            val dataSource = BasicDataSource().apply {
                driverClassName = "org.sqlite.JDBC"
                url = "jdbc:sqlite://${config.url}"
                maxTotal = config.maxTotal
                maxIdle = config.maxIdle
                maxOpenPreparedStatements = config.maxOpenPreparedStatements
                for ((key, value) in config.properties) {
                    addConnectionProperty(key, value)
                }
            }
            clients[config.key] = dataSource
            Database.connect(dataSource, SQLiteDialect())
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    @Synchronized
    operator fun invoke(key: SqliteClientKey? = null): Database? {
        return (if (key != null) {
            clients[key]
        } else {
            null
        } ?: clients.values.firstOrNull())?.let {
            Database.connect(it, SQLiteDialect())
        }
    }

    @Synchronized
    operator fun invoke(name: String): Database? {
        return clients.filterKeys { it.name == name }.entries.firstOrNull()?.value?.let {
            Database.connect(it, SQLiteDialect())
        }
    }
}
