/**
 * MyBatis backend and scoped session factory.
 * MyBatis 后端及作用域会话工厂。
 */
package fuookami.ospf.kotlin.framework.persistence.mybatis

import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicBoolean
import javax.sql.DataSource
import com.baomidou.mybatisplus.core.MybatisConfiguration
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder
import com.baomidou.mybatisplus.core.mapper.BaseMapper
import fuookami.ospf.kotlin.framework.persistence.expression.persistenceFailure
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import org.apache.ibatis.mapping.Environment
import org.apache.ibatis.session.SqlSession
import org.apache.ibatis.session.SqlSessionFactory
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory

/**
 * MyBatis 后端 / MyBatis backend
 *
 * 持有线程安全的 MyBatis-Plus `SqlSessionFactory`，但不拥有或关闭传入的连接池。
 * Holds a thread-safe MyBatis-Plus `SqlSessionFactory` without owning or closing the supplied connection pool.
 *
 * @property dialect SQL 方言 / SQL dialect
 */
class MybatisBackend internal constructor(
    private val sqlSessionFactory: SqlSessionFactory,
    val dialect: MybatisDialect
) {
    private val mapperRegistrationLock = Any()

    /**
     * 打开一个 mapper 会话作用域 / Open a mapper session scope
     *
     * @param mapperClass Mapper 接口类型 / Mapper interface class
     * @param autoCommit 是否自动提交 / Whether to commit automatically
     * @return 会话作用域或结构化错误 / Session scope or structured failure
     */
    fun <E : Any, M : BaseMapper<E>> openSession(
        mapperClass: Class<M>,
        autoCommit: Boolean = false
    ): Ret<MybatisSession<E, M>> {
        return try {
            registerMapper(mapperClass)
            Ok(MybatisSession(sqlSessionFactory.openSession(autoCommit), mapperClass))
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            persistenceFailure("MyBatis open session", exception)
        }
    }

    private fun registerMapper(mapperClass: Class<*>) {
        synchronized(mapperRegistrationLock) {
            val configuration = sqlSessionFactory.configuration
            if (!configuration.hasMapper(mapperClass)) {
                configuration.addMapper(mapperClass)
            }
        }
    }
}

/**
 * 创建 MyBatis 后端 / MyBatis backend factory
 */
object MybatisBackendFactory {
    /**
     * 从外部数据源创建 MyBatis 后端 / Create a MyBatis backend from an external data source
     *
     * 后端不会关闭数据源；连接池生命周期由创建数据源的数据库插件管理。
     * The backend does not close the data source; its owning database plugin manages the pool lifecycle.
     *
     * @param dataSource 外部管理的数据源 / Externally managed data source
     * @param dialect 与数据源匹配的 SQL 方言 / SQL dialect paired with the data source
     * @return MyBatis 后端或结构化错误 / MyBatis backend or structured failure
     */
    fun create(dataSource: DataSource, dialect: MybatisDialect): Ret<MybatisBackend> {
        return try {
            val configuration = MybatisConfiguration().apply {
                environment = Environment(
                    "ospf-mybatis",
                    JdbcTransactionFactory(),
                    dataSource
                )
                setMapUnderscoreToCamelCase(true)
            }
            val sqlSessionFactory = MybatisSqlSessionFactoryBuilder().build(configuration)
            Ok(MybatisBackend(sqlSessionFactory, dialect))
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            persistenceFailure("MyBatis backend creation", exception)
        }
    }
}

/**
 * 单个 SqlSession 的短生命周期作用域 / Short-lived scope around one SqlSession
 *
 * @param E 实体类型 / Entity type
 * @param M Mapper 类型 / Mapper type
 */
class MybatisSession<E : Any, M : BaseMapper<E>> internal constructor(
    private val sqlSession: SqlSession,
    private val mapperClass: Class<M>
) {
    private val closed = AtomicBoolean(false)

    /**
     * 获取当前作用域的 mapper / Get the mapper bound to this scope
     *
     * @return 当前 mapper 或结构化错误 / Mapper for this scope or structured failure
     */
    fun mapper(): Ret<M> {
        if (closed.get()) return closedFailure("mapper")
        return try {
            Ok(sqlSession.getMapper(mapperClass))
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            persistenceFailure("MyBatis mapper lookup", exception)
        }
    }

    /**
     * 提交当前事务 / Commit the current transaction
     *
     * @return 操作结果 / Operation result
     */
    fun commit(): Try = runSessionOperation("commit") { sqlSession.commit() }

    /**
     * 回滚当前事务 / Roll back the current transaction
     *
     * @return 操作结果 / Operation result
     */
    fun rollback(): Try = runSessionOperation("rollback") { sqlSession.rollback() }

    /**
     * 关闭当前会话作用域；重复关闭成功 / Close this session scope; repeated close succeeds
     *
     * @return 操作结果 / Operation result
     */
    fun close(): Try {
        if (!closed.compareAndSet(false, true)) return ok
        return try {
            sqlSession.close()
            ok
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            persistenceFailure("MyBatis session close", exception)
        }
    }

    private fun runSessionOperation(operation: String, block: () -> Unit): Try {
        if (closed.get()) return closedFailure(operation)
        return try {
            block()
            ok
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            persistenceFailure("MyBatis session $operation", exception)
        }
    }

    private fun <T> closedFailure(operation: String): Ret<T> {
        return Failed(
            ErrorCode.ApplicationFailed,
            "MyBatis 会话已关闭，无法执行 $operation / MyBatis session is closed; cannot $operation"
        )
    }
}
