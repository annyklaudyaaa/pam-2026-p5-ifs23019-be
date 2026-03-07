package org.delcom.repositories

import org.delcom.dao.UserDAO
import org.delcom.entities.User
import org.delcom.helpers.suspendTransaction
import org.delcom.helpers.userDAOToModel
import org.delcom.tables.UserTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import java.util.*

class UserRepository : IUserRepository {
    override suspend fun getById(userId: String): User? = suspendTransaction {
        UserDAO.findById(UUID.fromString(userId))?.let(::userDAOToModel)
    }

    override suspend fun getByUsername(username: String): User? = suspendTransaction {
        UserDAO.find { (UserTable.username eq username) }
            .limit(1)
            .map(::userDAOToModel)
            .firstOrNull()
    }

    override suspend fun create(user: User): String = suspendTransaction {
        val userDAO = UserDAO.new {
            name = user.name
            username = user.username
            password = user.password
            photo = user.photo // Tambahkan ini
            about = user.about // Tambahkan ini agar sinkron dengan fitur baru
            createdAt = user.createdAt
            updatedAt = user.updatedAt
        }

        userDAO.id.value.toString()
    }

    override suspend fun update(id: String, newUser: User): Boolean = suspendTransaction {
        val userDAO = UserDAO.findById(UUID.fromString(id))

        if (userDAO != null) {
            userDAO.name = newUser.name
            userDAO.username = newUser.username
            userDAO.password = newUser.password
            userDAO.photo = newUser.photo
            userDAO.about = newUser.about // Tambahkan ini untuk update informasi profil
            userDAO.updatedAt = newUser.updatedAt
            true
        } else {
            false
        }
    }

    override suspend fun delete(id: String): Boolean = suspendTransaction {
        val rowsDeleted = UserTable.deleteWhere {
            UserTable.id eq UUID.fromString(id)
        }
        rowsDeleted >= 1
    }
}