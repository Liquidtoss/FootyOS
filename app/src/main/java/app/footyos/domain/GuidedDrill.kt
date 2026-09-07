package app.footyos.domain

enum class DrillPhase { Ready, Prepare, Work, Rest, Complete }

data class DrillPlan(val sets: Int, val sides: Int = 1, val seconds: Int? = null) {
    val rounds get() = sets * sides
}

fun Exercise.drillPlan() = DrillPlan(
    sets = prescription.substringBefore(" ×").toInt(),
    sides = if ("/ side" in prescription || "/ leg" in prescription) 2 else 1,
    seconds = when (movement) { Movement.Carry -> 40; Movement.Copenhagen -> 20; else -> null },
)

data class GuidedDrill(
    val phase: DrillPhase = DrillPhase.Ready,
    val round: Int = 0,
    val remaining: Int = 0,
) {
    fun start() = copy(phase = DrillPhase.Prepare, remaining = 10)
    fun tick(plan: DrillPlan): GuidedDrill = when {
        phase !in listOf(DrillPhase.Prepare, DrillPhase.Rest, DrillPhase.Work) -> this
        phase == DrillPhase.Work && plan.seconds == null -> this
        remaining > 1 -> copy(remaining = remaining - 1)
        phase == DrillPhase.Work -> finishRound(plan)
        else -> copy(phase = DrillPhase.Work, remaining = plan.seconds ?: 0)
    }
    fun finishRound(plan: DrillPlan): GuidedDrill = if (round + 1 >= plan.rounds) {
        copy(phase = DrillPhase.Complete, round = plan.rounds, remaining = 0)
    } else {
        val switchingSides = plan.sides == 2 && round % 2 == 0
        copy(phase = DrillPhase.Rest, round = round + 1, remaining = if (switchingSides) 15 else 60)
    }
}
