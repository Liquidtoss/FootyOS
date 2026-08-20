package app.footyos

import android.app.Application
import app.footyos.data.AppContainer

class FootyOsApplication : Application() {
    val container by lazy { AppContainer(this) }
}
