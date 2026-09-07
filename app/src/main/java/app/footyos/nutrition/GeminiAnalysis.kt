package app.footyos.nutrition

import app.footyos.data.local.FootyDao
import app.footyos.data.local.PhotoAnalysisEntity
import app.footyos.photos.MealPhotos
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Application-owned work survives navigation; durable claims prevent automatic duplicate calls. */
class GeminiAnalysis(
    private val dao: FootyDao,
    private val photos: MealPhotos,
    private val keys: GeminiKeyStore,
    private val transport: GeminiTransport = GeminiClient(),
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    suspend fun analyze(photo: String, retry: Boolean = false): PhotoAnalysisEntity = scope.async {
        mutex.withLock {
            val cached = dao.photoAnalysis(photo)
            if (cached != null && (cached.status == "complete" || !retry)) return@withLock cached
            val base = PhotoAnalysisEntity(photo, "pending", null, GeminiClient.MODEL, System.currentTimeMillis())
            val key = try { keys.read() } catch (_: Exception) { null }
                ?: return@withLock base.copy(status = "setup")
            val file = photos.file(photo)
            if (!file.exists() || System.currentTimeMillis() - file.lastModified() > MealPhotos.RETENTION_MS) return@withLock base.copy(status = "expired")
            if (cached != null) dao.saveAnalysis(base)
            else if (dao.claimAnalysis(base) == -1L) return@withLock requireNotNull(dao.photoAnalysis(photo))
            val result = try {
                val response = transport.estimate(file.readBytes(), key)
                GeminiMealResult.parse(response.json)
                base.copy(status = "complete", resultJson = response.json, model = response.model)
            } catch (e: CancellationException) { throw e }
            catch (e: GeminiFailure) { base.copy(status = e.reason) }
            catch (_: java.net.SocketTimeoutException) { base.copy(status = "timeout") }
            catch (_: java.net.UnknownHostException) { base.copy(status = "offline") }
            catch (_: javax.net.ssl.SSLException) { base.copy(status = "tls") }
            catch (_: java.io.IOException) { base.copy(status = "network") }
            catch (_: IllegalArgumentException) { base.copy(status = "invalid_output") }
            catch (_: org.json.JSONException) { base.copy(status = "invalid_output") }
            catch (_: Exception) { base.copy(status = "failed") }
            dao.saveAnalysis(result)
            result
        }
    }.await()
}
