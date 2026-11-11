package com.example.utils

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

object OsmNotes {
    suspend fun create(lat: Double, lng: Double, text: String): Result<Long> = withContext(Dispatchers.IO) {
        if (!LandmarkAddress.isValid(lat, lng) || text.trim().length < 5) {
            return@withContext Result.failure(IllegalArgumentException("نص الملاحظة قصير جداً (أقل من 5 أحرف)"))
        }
        var c: HttpURLConnection? = null
        try {
            val body = "lat=${String.format(Locale.US, "%.6f", lat)}&lon=${String.format(Locale.US, "%.6f", lng)}" +
                "&text=" + URLEncoder.encode(text.trim() + "\n\n(via AlYemenServices app)", "UTF-8")
            c = (URL("https://api.openstreetmap.org/api/0.6/notes.json").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 10_000
                readTimeout = 15_000
                setRequestProperty("User-Agent", "AlYemenServicesApp/1.0")
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            }
            c.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            if (c.responseCode in 200..299) {
                val id = JSONObject(c.inputStream.bufferedReader().use { it.readText() })
                    .optJSONObject("properties")?.optLong("id") ?: 0L
                Result.success(id)
            } else {
                Result.failure(IOException("رمز الاستجابة ${c.responseCode}"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            c?.disconnect()
        }
    }
}
