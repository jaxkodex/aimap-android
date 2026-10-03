package pe.net.libre.aimap_client.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import pe.net.libre.aimap_client.home.Home
import pe.net.libre.aimap_client.home.HomeAction
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.ZoneId

/** The API said no. [status] is the HTTP status; 403 means the email is not in AIMAP_ALLOWED_EMAILS. */
class ApiException(val status: Int, message: String) : IOException(message)

/**
 * The aimap HTTP API. Every call carries a Firebase ID token from [token]; on a 401 it asks for a
 * fresh one once and retries. Everything reads except [act], aimap's own record of what you did
 * with a message: the mailbox, the bucket and the labels never change.
 */
class AimapApi(
    private val baseUrl: String,
    private val token: suspend (forceRefresh: Boolean) -> String,
) {
    suspend fun home(zone: ZoneId = ZoneId.systemDefault(), days: Int = 7): Home =
        parseHome(get("/home?days=$days&tz=" + URLEncoder.encode(zone.id, "UTF-8")))

    suspend fun accounts(): List<Account> = parseAccounts(get("/accounts"))

    /** One message's metadata and labels. A 404 means it is not in the database. */
    suspend fun message(id: Long): Message = parseMessage(get("/messages/$id"))

    /** The text body. A 404 also means the message is no longer in the bucket. */
    suspend fun messageBody(id: Long): String = parseMessageBody(get("/messages/$id/body"))

    /**
     * Marks a message handled or later, or undoes it, and returns the state it has now. A 404 means
     * aimap has no such message, a 422 that it will not take that word, a 403 that the account is
     * not allowed. The route is idempotent, so retrying it after a 401 is safe.
     */
    suspend fun act(id: Long, action: HomeAction): ActionState =
        parseAction(post("/messages/$id/actions", """{"action": "${action.wire}"}"""))

    private suspend fun get(path: String): String = try {
        request(path, token(false))
    } catch (e: ApiException) {
        if (e.status != 401) throw e
        request(path, token(true))
    }

    private suspend fun post(path: String, body: String): String = try {
        request(path, token(false), body)
    } catch (e: ApiException) {
        if (e.status != 401) throw e
        request(path, token(true), body)
    }

    private suspend fun request(path: String, bearer: String, body: String? = null): String = withContext(Dispatchers.IO) {
        val conn = URL(baseUrl + path).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            conn.setRequestProperty("Authorization", "Bearer $bearer")
            conn.setRequestProperty("Accept", "application/json")
            if (body != null) {
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.outputStream.use { it.write(body.toByteArray()) }
            }
            val status = conn.responseCode
            val body = (if (status in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) throw ApiException(status, detail(body) ?: "HTTP $status")
            body
        } finally {
            conn.disconnect()
        }
    }

    private fun detail(body: String): String? =
        runCatching { JSONObject(body).optString("detail").ifEmpty { null } }.getOrNull()
}
