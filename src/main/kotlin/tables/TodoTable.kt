package org.delcom.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object TodoTable : UUIDTable("todos") {
    // Menghubungkan user_id ke UserTable.id
    val userId = uuid("user_id").references(UserTable.id)
    val title = varchar("title", 100)
    val description = text("description")
    val cover = text("cover").nullable()
    val urgency = varchar("urgency", 10).default("Low")
    val isDone = bool("is_done")
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}