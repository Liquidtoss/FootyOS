package app.footyos.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.footyos.FootyOsApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun GeminiSetup() {
    val context = LocalContext.current
    val keys = (context.applicationContext as FootyOsApplication).container.geminiKeys
    val configured by keys.configured.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }
    // Never save the plaintext key in saved-instance state.
    var key by remember { mutableStateOf("") }
    var agreed by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Gemini photo estimates", style = MaterialTheme.typography.titleLarge)
            Text(if (configured) "Automatic analysis enabled for new photos." else "Offline mode · add your own Gemini key to enable photo estimates.")
            TextButton(enabled = !busy, onClick = { expanded = !expanded; key = "" }) { Text(if (expanded) "Close setup" else "Gemini setup") }
            if (expanded) {
                Text("Private installation only. Create a key in Google AI Studio using a project without billing enabled. The app cannot verify your billing tier.")
                TextButton(onClick = { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://aistudio.google.com/apikey"))) }) { Text("Open Google AI Studio") }
                Text("Enabling sends each added meal photo to Google once. Free-tier content may be used to improve Google products and reviewed by humans. Local photo deletion does not delete Google's copies.")
                OutlinedTextField(key, { key = it }, label = { Text("Gemini API key") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, enabled = !busy)
                Row {
                    Checkbox(agreed, { agreed = it }, enabled = !busy)
                    Text("My project has no billing enabled, and I agree to send meal photos for analysis.", Modifier.weight(1f))
                }
                Button(enabled = !busy && agreed && key.trim().length in 20..300, onClick = {
                    busy = true
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) { keys.save(key) }
                            key = ""; expanded = false; agreed = false; error = null
                        } catch (_: Exception) { error = "Could not securely save the key. Try again." }
                        finally { busy = false }
                    }
                }) { Text("Save key and enable") }
                if (configured) TextButton(enabled = !busy, onClick = {
                    busy = true
                    scope.launch {
                        try { withContext(Dispatchers.IO) { keys.clear() }; key = ""; error = null }
                        catch (_: Exception) { error = "Could not remove the key. Try again." }
                        finally { busy = false }
                    }
                }) { Text("Remove key and disable") }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}
