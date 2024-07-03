package fuookami.ospf.kotlin.framework.persistence

import kotlinx.serialization.*
import org.ktorm.database.*
import org.ktorm.support.mysql.*
import org.apache.commons.dbcp2.*
import org.apache.logging.log4j.kotlin.*

data class MySQLClientKey(
    val name: String,
    val database: String
)

data class MySQLConfigBuilder(
    var url: String? = null,
    var name: String? = null,
    var database: String? = null,
    var userName: String? = null,
    var password: String? = null,
    val properties: MutableMap<String, String> = mutableMapOf(),
    val maxTotal: Int = 20,
    val maxIdle: Int = 10,
    val maxOpenPreparedStatements: Int = 100
) {
    private val logger = logger()

    operator fun invoke(): MySQLConfig? {
        return try {
            MySQLConfig(
                url = url!!,
                name = name!!,
                database = database!!,
                userName = userName!!,
                password = password!!,
                properties = properties,
                maxTotal = maxTotal,
                maxIdle = maxIdle,
                maxOpenPreparedStatements = maxOpenPreparedStatements
            )
        } catch (e: Exception) {
            if (url == null) {
                logger.error("MySQL url is not set")
            }
            if (name == null) {
                logger.error("MySQL name is not set")
            }
            if (database == null) {
                logger.error("MySQL database is not set")
            }
            if (userName == null) {
                logger.error("MySQL user name is not set")
            }
            if (password == null) {
                logger.error("MySQL password is not set")
            }
            null
        }
    }
}

@Serializable
data class MySQLConfig(
    val url: String,
    val name: String,
    val database: String,
    val userName: String,
    val password: String,
    val properties: Map<String, String> = emptyMap(),
    val maxTotal: Int = 20,
    val maxIdle: Int = 10,
    val maxOpenPreparedStatements: Int = 100
) {
    val key get() = MySQLClientKey(name = name, database = database)
}

object MySQL {
    @get:Synchronized
    private val clients: MutableMap<MySQLClientKey, BasicDataSource> = HashMap()

    @Synchronized
    fun init(builder: MySQLConfigBuilder.() -> Unit): Database? {
        val config = MySQLConfigBuilder()
        builder(config)
        return config()?.let { this(it) }
    }

    @Synchronized
    operator fun invoke(config: MySQLConfig): Database? {
        if (clients.containsKey(config.key)) {
            return Database.connect(clients[config.key]!!, MySqlDialect())
        }

        return try {
            val dataSource = BasicDataSource().apply {
                driverClassName = "com.mysql.cj.jdbc.Driver"
                url = "jdbc:mysql://${config.url}/${config.database}?charset=utf8mb4"
                username = config.userName
                password = config.password
                maxTotal = config.maxTotal
                maxIdle = config.maxIdle
                maxOpenPreparedStatements = config.maxOpenPreparedStatements
                for ((key, value) in config.properties) {
                    addConnectionProperty(key, value)
                }
            }
            clients[config.key] = dataSource
            Database.connect(dataSource, MySqlDialect())
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    @Synchronized
    operator fun invoke(key: MySQLClientKey? = null): Database? {
        return (if (key != null) {
            clients[key]
        } else {
            null
        } ?: clients.values.firstOrNull())?.let {
            Database.connect(it, MySqlDialect())
        }
    }

    @Synchronized
    operator fun invoke(name: String, dataBase: String? = null): Database? {
        return (if (dataBase != null) {
            clients[MySQLClientKey(name = name, database = dataBase)]
        } else {
            null
        } ?: clients.filterKeys { it.name == name }.entries.firstOrNull()?.value)?.let {
                Database.connect(it, MySqlDialect())
        }
    }
}
