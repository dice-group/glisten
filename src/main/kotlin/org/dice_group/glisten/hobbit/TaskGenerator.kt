package org.dice_group.glisten.hobbit

import org.hobbit.core.components.AbstractTaskGenerator
import org.hobbit.core.rabbit.RabbitMQUtils



class TaskGenerator : AbstractTaskGenerator(){


    override fun generateTask(data: ByteArray?) {
        //filename
        val sourceUrl = RabbitMQUtils.readString(data)
        //if task weren't pre generated => generate them here.

        val taskId = nextTaskId

        val timestamp = System.currentTimeMillis()
        sendTaskToSystemAdapter(taskId, sourceUrl.toByteArray())

        sendTaskToEvalStorage(taskId, timestamp, RabbitMQUtils.writeString(sourceUrl))

    }




}