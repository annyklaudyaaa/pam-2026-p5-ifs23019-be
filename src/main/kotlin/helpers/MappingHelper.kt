package org.delcom.helpers

import kotlinx.coroutines.Dispatchers
import org.delcom.dao.TodoDAO
import org.delcom.dao.RefreshTokenDAO
import org.delcom.dao.UserDAO
import org.delcom.entities.Todo
import org.delcom.entities.RefreshToken
import org.delcom.entities.User
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

suspend fun <T> suspendTransaction(block: Transaction.() -> T): T =
    newSuspendedTransaction(Dispatchers.IO, statement = block)

fun userDAOToModel(dao: UserDAO) = User(
    id = dao.id.value.toString(),
    name = dao.name,
    username = dao.username,
    password = dao.password,
    photo = dao.photo,
    about = dao.about,
    createdAt = dao.createdAt,
    updatedAt = dao.updatedAt
)

fun refreshTokenDAOToModel(dao: RefreshTokenDAO) = RefreshToken(
    id = dao.id.value.toString(),
    userId = dao.userId.toString(),
    refreshToken = dao.refreshToken,
    authToken = dao.authToken,
    createdAt = dao.createdAt,
)

fun todoDAOToModel(dao: TodoDAO) = Todo(
    id = dao.id.value.toString(),
    userId = dao.userId.toString(),
    title = dao.title,
    description = dao.description,
    isDone =  dao.isDone,
    cover = dao.cover,
    urgency = dao.urgency,
    createdAt = dao.createdAt,
    updatedAt = dao.updatedAt
)