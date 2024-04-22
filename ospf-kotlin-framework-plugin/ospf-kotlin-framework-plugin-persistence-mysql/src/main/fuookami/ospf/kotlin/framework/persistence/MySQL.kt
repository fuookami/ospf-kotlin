package fuookami.ospf.kotlin.framework.persistence

import kotlinx.serialization.*
import org.ktorm.database.*
import org.ktorm.support.mysql.*
import org.apache.commons.dbcp2.*

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
    private val clients: MutableMap<MySQLClientKey, Database> = HashMap()

    @Synchronized
    fun init(builder: MySQLConfigBuilder.() -> Unit): Database? {
        val config = MySQLConfigBuilder()
        builder(config)
        return config()?.let { this(it) }
    }

    @Synchronized
    operator fun invoke(config: MySQLConfig): Database? {
        if (clients.containsKey(config.key)) {
            return clients[config.key]
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
            val client = Database.connect(dataSource, MySqlDialect())
            clients[config.key] = client
            client
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    @Synchronized
    operator fun invoke(key: MySQLClientKey? = null): Database? {
        return if (key != null) {
            clients[key]
        } else {
            null
        }
            ?: clients.values.firstOrNull()
    }

    @Synchronized
    operator fun invoke(name: String, dataBase: String? = null): Database? {
        return if (dataBase != null) {
            clients[MySQLClientKey(name = name, database = dataBase)]
        } else {
            null
        }
            ?: clients.filterKeys { it.name == name }.entries.firstOrNull()?.value
    }
}
