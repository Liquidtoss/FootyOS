package app.footyos.calendar

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import java.time.ZonedDateTime

data class CalendarEventDraft(
    val title: String,
    val start: ZonedDateTime,
    val durationMinutes: Int,
    val deepLink: String,
)

class CalendarRepository(private val context: Context) {
    fun insertIntent(draft: CalendarEventDraft): Intent {
        val startMillis = draft.start.toInstant().toEpochMilli()
        val endMillis = draft.start.plusMinutes(draft.durationMinutes.toLong()).toInstant().toEpochMilli()

        return Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
            putExtra(CalendarContract.Events.TITLE, draft.title)
            putExtra(CalendarContract.Events.CUSTOM_APP_PACKAGE, context.packageName)
            putExtra(CalendarContract.Events.CUSTOM_APP_URI, draft.deepLink)
        }
    }
}
