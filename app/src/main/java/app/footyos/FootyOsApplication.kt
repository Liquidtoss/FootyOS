package app.footyos

import android.app.Application
import app.footyos.data.AppContainer

class FootyOsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val scheduler = getSystemService(android.app.job.JobScheduler::class.java)
        val component = android.content.ComponentName(this, app.footyos.photos.PhotoCleanupService::class.java)
        if (scheduler.getPendingJob(3001) == null) {
            scheduler.schedule(android.app.job.JobInfo.Builder(3001, component)
                .setPeriodic(24 * 60 * 60 * 1000L).setPersisted(true).build())
        }
        scheduler.schedule(android.app.job.JobInfo.Builder(3002, component).setMinimumLatency(0).build())
    }
    val container by lazy { AppContainer(this) }
}
