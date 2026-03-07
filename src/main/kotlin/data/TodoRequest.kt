package org.delcom.data

import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import org.delcom.entities.Todo
import java.util.UUID

@Serializable
data class TodoRequest(
    var userId: String = "",
    var title: String = "",
    var description: String = "",
    var cover: String? = null,
    var urgency: String = "Low",
    var isDone: Boolean = false,
){
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "userId" to userId,
            "title" to title,
            "description" to description,
            "cover" to cover,
            "urgency" to urgency,
            "isDone" to isDone,
        )
    }

    fun toEntity(): Todo {
        return Todo(
            // Pastikan data class Todo (Entity) kamu juga sudah ada field urgency
            userId = userId,
            title = title,
            description = description,
            cover = cover,
            urgency = urgency,
            isDone = isDone,
            updatedAt = Clock.System.now()
        )
    }
}