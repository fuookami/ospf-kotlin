package fuookami.ospf.kotlin.framework.persistence

import kotlinx.serialization.*
import org.ktorm.database.*

data class MySQLClientKey(
    val name: String,
    val database: String
)

data class MySQLConfigBuilder(
    var url: String? = null,
    var name: String? = null,
    var database: String? = null,
    var userName: String? = null,
    var password: String? = null
) {
    operator fun invoke(): MySQLConfig? {
        return try {
            MySQLConfig(
                url = url!!,
                name = name!!,
                database = database!!,
                userName = userName!!,
                password = password!!
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
    val password: String
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
            val client =
                Database.connect(
                    url = "jdbc:mysql://${config.url}/${config.database}?charset=utf8mb4",
                    driver = "com.mysql.cj.jdbc.Driver",
                    user = config.userName,
                    password = config.password
                )
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
