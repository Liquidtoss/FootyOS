package app.footyos

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import app.footyos.ui.FootyOsApp
import app.footyos.ui.theme.FootyOsTheme

class MainActivity : ComponentActivity() {
    private val deepLink = mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deepLink.value = intent?.data
        enableEdgeToEdge()
        setContent {
            FootyOsTheme {
                FootyOsApp(initialUri = deepLink.value)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLink.value = intent.data
    }
}
