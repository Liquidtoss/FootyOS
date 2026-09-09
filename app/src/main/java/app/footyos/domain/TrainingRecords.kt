package app.footyos.domain

import app.footyos.data.local.WorkoutEntryEntity

/** Records compare the same exercise. Rep records also require the same load. */
object TrainingRecords {
    fun history(exerciseId: String, entries: List<WorkoutEntryEntity>) = entries
        .filter { it.exerciseId == exerciseId && it.sets > 0 }
        .sortedWith(compareByDescending<WorkoutEntryEntity> { it.date }.thenByDescending { it.id })

    fun achievements(entry: WorkoutEntryEntity, previous: List<WorkoutEntryEntity>): List<String> {
        val history = history(entry.exerciseId, previous)
        if (history.isEmpty()) return listOf("First session • baseline set")
        return buildList {
            val bestLoad = history.mapNotNull { it.loadLb }.maxOrNull()
            if (entry.loadLb != null && entry.loadLb > 0 && bestLoad != null && entry.loadLb > bestLoad) {
                add("Weight PB • +${number(entry.loadLb - bestLoad)} lb")
            }
            val bestReps = history.filter { (it.loadLb ?: 0.0) == (entry.loadLb ?: 0.0) }
                .mapNotNull { it.reps }.maxOrNull()
            if (entry.reps != null && bestReps != null && entry.reps > bestReps) {
                add("Rep PB • +${entry.reps - bestReps} reps/set at the same load")
            }
            if (entry.sets > history.maxOf { it.sets }) add("Sets PB • ${entry.sets} sets")
        }
    }

    fun summary(entry: WorkoutEntryEntity): String = listOfNotNull(
        entry.loadLb?.let { "${number(it)} lb" }, "${entry.sets} sets", entry.reps?.let { "$it reps/set" },
    ).joinToString(" · ")

    fun number(value: Double) = if (value % 1.0 == 0.0) value.toLong().toString() else "%.1f".format(java.util.Locale.US, value)
}
