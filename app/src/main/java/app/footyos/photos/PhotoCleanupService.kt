package app.footyos.photos

import android.app.job.JobParameters
import android.app.job.JobService
import kotlinx.coroutines.*

class PhotoCleanupService : JobService() {
    private var cleanup: Job? = null
    override fun onStartJob(params: JobParameters): Boolean {
        cleanup = CoroutineScope(Dispatchers.IO).launch {
            val failed = runCatching { MealPhotos(applicationContext).prune() }.isFailure
            withContext(Dispatchers.Main) { jobFinished(params, failed) }
        }
        return true
    }
    override fun onStopJob(params: JobParameters): Boolean {
        cleanup?.cancel()
        return true
    }
}
