package app.footyos.nutrition

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import android.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Private-installation BYOK. The key is never compiled into the APK or included in backups. */
class GeminiKeyStore(context: Context) {
    private val file = AtomicFile(File(context.noBackupFilesDir, "gemini-key.enc"))
    private val configuredState = MutableStateFlow(file.baseFile.exists())
    val configured = configuredState.asStateFlow()
    private fun secret(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey("footyos-gemini", null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder("footyos-gemini", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    @Synchronized fun save(value: String) {
        val key = value.trim()
        require(key.length in 20..300 && key.none { it.isWhitespace() || it.isISOControl() })
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, secret()) }
        val encrypted = cipher.doFinal(key.toByteArray(Charsets.UTF_8))
        val json = JSONObject().put("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .put("data", Base64.encodeToString(encrypted, Base64.NO_WRAP)).toString()
        val stream = file.startWrite()
        try { stream.write(json.toByteArray(Charsets.UTF_8)); file.finishWrite(stream) }
        catch (e: Exception) { file.failWrite(stream); throw e }
        configuredState.value = true
    }
    @Synchronized fun read(): String? {
        if (!file.baseFile.exists()) return null
        val json = JSONObject(String(file.readFully(), Charsets.UTF_8))
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, secret(), GCMParameterSpec(128, Base64.decode(json.getString("iv"), Base64.NO_WRAP)))
        }
        return String(cipher.doFinal(Base64.decode(json.getString("data"), Base64.NO_WRAP)), Charsets.UTF_8)
    }
    @Synchronized fun clear() { file.delete(); configuredState.value = false }
}
