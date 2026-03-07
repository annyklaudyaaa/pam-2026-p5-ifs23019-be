package org.delcom.services

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.delcom.data.AppException
import org.delcom.data.DataResponse
import org.delcom.data.TodoRequest
import org.delcom.helpers.ServiceHelper
import org.delcom.helpers.ValidatorHelper
import org.delcom.repositories.ITodoRepository
import org.delcom.repositories.IUserRepository
import java.io.File
import java.util.*

class TodoService(
    private val userRepo: IUserRepository,
    private val todoRepo: ITodoRepository
) {
    // Mengambil semua daftar todo dengan dukungan Pagination dan Filtering
    suspend fun getAll(call: ApplicationCall) {
        val user = ServiceHelper.getAuthUser(call, userRepo)

        // Ambil Query Parameters dari URL
        val search = call.request.queryParameters["search"] ?: ""
        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
        val perPage = call.request.queryParameters["perPage"]?.toIntOrNull() ?: 10
        val isDone = call.request.queryParameters["is_finished"]?.toBooleanStrictOrNull()
        val urgency = call.request.queryParameters["urgency"]

        // Panggil Repo dengan parameter lengkap sesuai kebutuhan praktikum
        val todos = todoRepo.getAll(user.id, search, page, perPage, isDone, urgency)

        val response = DataResponse(
            "success",
            "Berhasil mengambil daftar todo saya",
            mapOf(Pair("todos", todos))
        )
        call.respond(response)
    }

    // Mengambil statistik todo untuk Halaman Home
    suspend fun getStats(call: ApplicationCall) {
        val user = ServiceHelper.getAuthUser(call, userRepo)
        val stats = todoRepo.getStats(user.id)

        val response = DataResponse(
            "success",
            "Berhasil mengambil statistik todo",
            mapOf(Pair("stats", stats))
        )
        call.respond(response)
    }

    // Mengambil data todo berdasarkan ID
    suspend fun getById(call: ApplicationCall) {
        val todoId = call.parameters["id"]
            ?: throw AppException(400, "Data todo tidak valid!")

        val user = ServiceHelper.getAuthUser(call, userRepo)

        val todo = todoRepo.getById(todoId)
        if (todo == null || todo.userId != user.id) {
            throw AppException(404, "Data todo tidak tersedia!")
        }

        val response = DataResponse(
            "success",
            "Berhasil mengambil data todo",
            mapOf(Pair("todo", todo))
        )
        call.respond(response)
    }

    // Menambahkan data todo baru
    suspend fun post(call: ApplicationCall) {
        val user = ServiceHelper.getAuthUser(call, userRepo)

        val request = call.receive<TodoRequest>()
        request.userId = user.id

        // Validasi input termasuk field urgency yang baru
        val validator = ValidatorHelper(request.toMap())
        validator.required("title", "Judul todo tidak boleh kosong")
        validator.required("description", "Deskripsi tidak boleh kosong")
        validator.required("urgency", "Level urgensi tidak boleh kosong")
        validator.validate()

        val todoId = todoRepo.create(request.toEntity())

        val response = DataResponse(
            "success",
            "Berhasil menambahkan data todo",
            mapOf(Pair("todoId", todoId))
        )
        call.respond(response)
    }

    // Mengubah data todo
    suspend fun put(call: ApplicationCall) {
        val todoId = call.parameters["id"]
            ?: throw AppException(400, "Data todo tidak valid!")

        val user = ServiceHelper.getAuthUser(call, userRepo)

        val request = call.receive<TodoRequest>()
        request.userId = user.id

        val validator = ValidatorHelper(request.toMap())
        validator.required("title", "Judul todo tidak boleh kosong")
        validator.required("description", "Deskripsi tidak boleh kosong")
        validator.required("urgency", "Level urgensi tidak boleh kosong")
        validator.required("isDone", "Status selesai tidak boleh kosong")
        validator.validate()

        val oldTodo = todoRepo.getById(todoId)
        if (oldTodo == null || oldTodo.userId != user.id) {
            throw AppException(404, "Data todo tidak tersedia!")
        }

        // Mempertahankan cover lama saat update data teks
        request.cover = oldTodo.cover

        val isUpdated = todoRepo.update(user.id, todoId, request.toEntity())
        if (!isUpdated) {
            throw AppException(400, "Gagal memperbarui data todo!")
        }

        val response = DataResponse("success", "Berhasil mengubah data todo", null)
        call.respond(response)
    }

    // Fungsi putCover yang sebelumnya hilang (Penyebab Error)
    suspend fun putCover(call: ApplicationCall) {
        val todoId = call.parameters["id"]
            ?: throw AppException(400, "Data todo tidak valid!")

        val user = ServiceHelper.getAuthUser(call, userRepo)

        val request = TodoRequest()
        request.userId = user.id

        // Proses upload file gambar
        val multipartData = call.receiveMultipart(formFieldLimit = 1024 * 1024 * 5)
        multipartData.forEachPart { part ->
            if (part is PartData.FileItem) {
                val ext = part.originalFileName
                    ?.substringAfterLast('.', "")
                    ?.let { if (it.isNotEmpty()) ".$it" else "" }
                    ?: ""

                val fileName = UUID.randomUUID().toString() + ext
                val filePath = "uploads/todos/$fileName"

                withContext(Dispatchers.IO) {
                    val file = File(filePath)
                    file.parentFile.mkdirs()
                    part.provider().copyAndClose(file.writeChannel())
                    request.cover = filePath
                }
            }
            part.dispose()
        }

        if (request.cover == null) {
            throw AppException(404, "Cover todo tidak tersedia!")
        }

        val oldTodo = todoRepo.getById(todoId)
        if (oldTodo == null || oldTodo.userId != user.id) {
            throw AppException(404, "Data todo tidak tersedia!")
        }

        // Ambil data lama agar tidak hilang saat update cover
        request.title = oldTodo.title
        request.description = oldTodo.description
        request.urgency = oldTodo.urgency
        request.isDone = oldTodo.isDone

        val isUpdated = todoRepo.update(user.id, todoId, request.toEntity())
        if (!isUpdated) {
            throw AppException(400, "Gagal memperbarui cover todo!")
        }

        // Hapus file cover lama jika ada
        oldTodo.cover?.let { path ->
            val oldFile = File(path)
            if (oldFile.exists()) oldFile.delete()
        }

        call.respond(DataResponse("success", "Berhasil mengubah cover todo", null))
    }

    // Menghapus data todo
    suspend fun delete(call: ApplicationCall) {
        val todoId = call.parameters["id"] ?: throw AppException(400, "Data todo tidak valid!")
        val user = ServiceHelper.getAuthUser(call, userRepo)
        val oldTodo = todoRepo.getById(todoId)

        if (oldTodo == null || oldTodo.userId != user.id) {
            throw AppException(404, "Data todo tidak tersedia!")
        }

        val isDeleted = todoRepo.delete(user.id, todoId)
        if (!isDeleted) {
            throw AppException(400, "Gagal menghapus data todo!")
        }

        // Hapus file cover dari storage jika data di database berhasil dihapus
        oldTodo.cover?.let { path ->
            val file = File(path)
            if (file.exists()) file.delete()
        }

        call.respond(DataResponse("success", "Berhasil menghapus data todo", null))
    }

    // Mengambil file cover untuk ditampilkan di UI
    suspend fun getCover(call: ApplicationCall) {
        val todoId = call.parameters["id"] ?: throw AppException(400, "Data todo tidak valid!")
        val todo = todoRepo.getById(todoId) ?: return call.respond(HttpStatusCode.NotFound)

        val filePath = todo.cover ?: throw AppException(404, "Todo belum memiliki cover")
        val file = File(filePath)

        if (!file.exists()) throw AppException(404, "Cover todo tidak tersedia di server")

        call.respondFile(file)
    }
}