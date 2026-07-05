package fuookami.ospf.kotlin.example.framework_demo.demo10.infrastructure

import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.expression.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

/** 数据序列化器，负责将领域模型转换为持久化格式 / Data serializer responsible for converting domain models to persistence format */
object DataSerializer {
    /**
     * 将时间段模型序列化为 JSON 对象
     *
     * Serialize a time slot model into a JSON object
     *
     * @param timeSlot 需要序列化的时间段模型
     * @param timeSlot the time slot model to serialize
     * @return 序列化后的 JSON 对象
     * @return the serialized JSON object
     */
    fun serializeTimeSlot(timeSlot: TimeSlot): JsonObject {
        return JsonObject(
            "id" to timeSlot.id,
            "start" to timeSlot.start,
            "end" to timeSlot.end
        )
    }

    /**
     * 将资源模型序列化为 JSON 对象
     *
     * Serialize a resource model into a JSON object
     *
     * @param resource 需要序列化的资源模型
     * @param resource the resource model to serialize
     * @return 序列化后的 JSON 对象
     * @return the serialized JSON object
     */
    fun serializeResource(resource: Resource): JsonObject {
        return JsonObject(
            "id" to resource.id,
            "name" to resource.name,
            "capacity" to resource.capacity
        )
    }

    /**
     * 将任务模型序列化为 JSON 对象
     *
     * Serialize a task model into a JSON object
     *
     * @param task 需要序列化的任务模型
     * @param task the task model to serialize
     * @return 序列化后的 JSON 对象
     * @return the serialized JSON object
     */
    fun serializeTask(task: Task): JsonObject {
        return JsonObject(
            "id" to task.id,
            "description" to task.description,
            "duration" to task.duration
        )
    }
}
