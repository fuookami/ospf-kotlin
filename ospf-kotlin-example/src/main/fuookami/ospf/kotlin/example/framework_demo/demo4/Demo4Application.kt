package fuookami.ospf.kotlin.example.framework_demo.demo4

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/** 演示4的Spring Boot应用入口类 / Spring Boot application entry class for Demo4 */
@SpringBootApplication
class Demo4Application

/** 应用主入口方法 / Application main entry method
 * @param args 命令行参数 / command line arguments */
fun main(args: Array<String>) {
    runApplication<Demo4Application>(*args)
}
