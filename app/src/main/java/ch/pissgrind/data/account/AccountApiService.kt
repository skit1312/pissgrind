package ch.pissgrind.data.datasource.account

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

sealed class AccountResult {
    data class Success(val message: String) : AccountResult()
    data class Error(val message: String) : AccountResult()
}

@Singleton
class AccountApiService @Inject constructor() {

    private val TAG = "AccountApiService"
    private val BASE_URL = "https://register.pissgrind.rip"

    suspend fun changePassword(
        username: String,
        oldPassword: String,
        newPassword: String
    ): AccountResult = withContext(Dispatchers.IO) {
        try {
            postJson("$BASE_URL/account/change-password", mapOf(
                "username" to username,
                "old_password" to oldPassword,
                "new_password" to newPassword
            ))
        } catch (e: Exception) {
            Log.e(TAG, "changePassword failed", e)
            AccountResult.Error("Connection failed: ${e.message}")
        }
    }

    suspend fun changeEmail(
        username: String,
        password: String,
        newEmail: String
    ): AccountResult = withContext(Dispatchers.IO) {
        try {
            postJson("$BASE_URL/account/change-email", mapOf(
                "username" to username,
                "password" to password,
                "new_email" to newEmail
            ))
        } catch (e: Exception) {
            Log.e(TAG, "changeEmail failed", e)
            AccountResult.Error("Connection failed: ${e.message}")
        }
    }

    suspend fun getEmail(
        username: String,
        password: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val connection = URL("$BASE_URL/account/get-email").openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")

            val jsonBody = buildJsonBody(mapOf(
                "username" to username,
                "password" to password
            ))
            connection.outputStream.write(jsonBody.toByteArray())

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                val emailMatch = Regex("\"email\"\\s*:\\s*\"([^\"]+)\"").find(response)
                emailMatch?.groupValues?.get(1)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "getEmail failed", e)
            null
        }
    }

    private fun postJson(url: String, params: Map<String, String>): AccountResult {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")

        val jsonBody = buildJsonBody(params)
        connection.outputStream.write(jsonBody.toByteArray())

        return if (connection.responseCode == 200) {
            AccountResult.Success("Success")
        } else {
            val error = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
            val detail = Regex("\"detail\"\\s*:\\s*\"([^\"]+)\"").find(error)
            AccountResult.Error(detail?.groupValues?.get(1) ?: error)
        }
    }

    private fun buildJsonBody(params: Map<String, String>): String {
        return params.entries.joinToString(",", "{", "}") {
            "\"${it.key}\":\"${it.value.replace("\\", "\\\\").replace("\"", "\\\"")}\""
        }
    }
    suspend fun syncTrainingData(
        username: String,
        password: String,
        trainingData: String
    ): AccountResult = withContext(Dispatchers.IO) {
        try {
            val connection = URL("$BASE_URL/account/sync-training").openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")

            val body = buildJsonBodyWithRawField(
                params = mapOf(
                    "username" to username,
                    "password" to password
                ),
                rawJsonField = "training_data" to trainingData
            )
            connection.outputStream.write(body.toByteArray())

            if (connection.responseCode == 200) {
                AccountResult.Success("Synced")
            } else {
                val error = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                AccountResult.Error(error)
            }
        } catch (e: Exception) {
            Log.e(TAG, "syncTrainingData failed", e)
            AccountResult.Error("Sync failed: ${e.message}")
        }
    }

    private fun buildJsonBodyWithRawField(
        params: Map<String, String>,
        rawJsonField: Pair<String, String>
    ): String {
        val parts = mutableListOf<String>()

        params.forEach { (key, value) ->
            parts.add("\"$key\":\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
        }

        val (key, rawJson) = rawJsonField
        val escaped = rawJson
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
        parts.add("\"$key\":\"$escaped\"")

        return "{${parts.joinToString(",")}}"
    }
    suspend fun getTrainingData(
        username: String,
        password: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val connection = URL("$BASE_URL/account/get-training").openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")

            val jsonBody = buildJsonBody(mapOf(
                "username" to username,
                "password" to password
            ))
            connection.outputStream.write(jsonBody.toByteArray())

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                val match = Regex("\"training_data\"\\s*:\\s*\"(.*?)\"", RegexOption.DOT_MATCHES_ALL).find(response)
                match?.groupValues?.get(1)
                    ?.replace("\\\"", "\"")
                    ?.replace("\\\\", "\\")
                    ?.ifBlank { null }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "getTrainingData failed", e)
            null
        }
    }
}
