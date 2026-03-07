package org.delcom.repositories

import org.delcom.dao.TodoDAO
import org.delcom.entities.Todo
import org.delcom.helpers.suspendTransaction
import org.delcom.helpers.todoDAOToModel
import org.delcom.tables.TodoTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import java.util.*

class TodoRepository : ITodoRepository {

    // 1. Mengambil semua data dengan Pagination, Search, dan Filter
    override suspend fun getAll(
        userId: String,
        search: String,
        page: Int,
        perPage: Int,
        isDone: Boolean?,
        urgency: String?
    ): List<Todo> = suspendTransaction {
        val userUUID = UUID.fromString(userId)
        val offsetValue = (page - 1).toLong() * perPage

        // Query dasar berdasarkan User ID
        val query = TodoTable.selectAll().where { TodoTable.userId eq userUUID }

        // Filter Search (Judul)
        if (search.isNotBlank()) {
            val keyword = "%${search.lowercase()}%"
            query.andWhere { TodoTable.title.lowerCase() like keyword }
        }

        // Filter Status Selesai
        if (isDone != null) {
            query.andWhere { TodoTable.isDone eq isDone }
        }

        // Filter Level Urgensi
        if (urgency != null) {
            query.andWhere { TodoTable.urgency eq urgency }
        }

        // Eksekusi dengan memisahkan .limit() dan .offset() untuk menghindari deprecation
        TodoDAO.wrapRows(query)
            .orderBy(TodoTable.createdAt to SortOrder.DESC)
            .limit(perPage)
            .offset(offsetValue)
            .map(::todoDAOToModel)
    }

    // 2. Mengambil Statistik untuk Halaman Home
    override suspend fun getStats(userId: String): Map<String, Long> = suspendTransaction {
        val userUUID = UUID.fromString(userId)

        val total = TodoTable.selectAll().where { TodoTable.userId eq userUUID }.count()
        val finished = TodoTable.selectAll().where {
            (TodoTable.userId eq userUUID) and (TodoTable.isDone eq true)
        }.count()
        val unfinished = total - finished

        mapOf(
            "total" to total,
            "finished" to finished,
            "unfinished" to unfinished
        )
    }

    override suspend fun getById(todoId: String): Todo? = suspendTransaction {
        TodoDAO.findById(UUID.fromString(todoId))?.let(::todoDAOToModel)
    }

    override suspend fun create(todo: Todo): String = suspendTransaction {
        val todoDAO = TodoDAO.new {
            userId = UUID.fromString(todo.userId)
            title = todo.title
            description = todo.description
            cover = todo.cover
            urgency = todo.urgency
            isDone = todo.isDone
            createdAt = todo.createdAt
            updatedAt = todo.updatedAt
        }
        todoDAO.id.value.toString()
    }

    override suspend fun update(userId: String, todoId: String, newTodo: Todo): Boolean = suspendTransaction {
        val todoDAO = TodoDAO
            .find {
                (TodoTable.id eq UUID.fromString(todoId)) and
                        (TodoTable.userId eq UUID.fromString(userId))
            }
            .limit(1)
            .firstOrNull()

        if (todoDAO != null) {
            todoDAO.title = newTodo.title
            todoDAO.description = newTodo.description
            todoDAO.cover = newTodo.cover
            todoDAO.urgency = newTodo.urgency
            todoDAO.isDone = newTodo.isDone
            todoDAO.updatedAt = newTodo.updatedAt
            true
        } else {
            false
        }
    }

    override suspend fun delete(userId: String, todoId: String): Boolean = suspendTransaction {
        val rowsDeleted = TodoTable.deleteWhere {
            (TodoTable.id eq UUID.fromString(todoId)) and
                    (TodoTable.userId eq UUID.fromString(userId))
        }
        rowsDeleted >= 1
    }
}