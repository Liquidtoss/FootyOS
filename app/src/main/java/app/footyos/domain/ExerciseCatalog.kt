package app.footyos.domain

data class Exercise(
    val id: String,
    val name: String,
    val prescription: String,
    val movement: Movement,
    val cues: List<String>,
    val mistakes: List<String>,
)

enum class Movement {
    Press,
    Row,
    OverheadPress,
    Carry,
    Swing,
    Squat,
    SplitSquat,
    Hinge,
    CalfRaise,
    TibialisRaise,
    Copenhagen,
    HamstringCurl,
}

object ExerciseCatalog {
    val exercises = listOf(
        Exercise(
            id = "floor_press",
            name = "Kettlebell floor press",
            prescription = "3 × 8–12",
            movement = Movement.Press,
            cues = listOf(
                "Keep the shoulder blade settled against the floor.",
                "Stack the wrist over the elbow.",
                "Lower under control until the upper arm reaches the floor.",
            ),
            mistakes = listOf("Excessive elbow flare", "Bent wrist", "Bouncing off the floor"),
        ),
        Exercise(
            id = "row",
            name = "1-arm kettlebell row",
            prescription = "3 × 8–12 / side",
            movement = Movement.Row,
            cues = listOf(
                "Brace the trunk and keep the hips square.",
                "Pull the elbow toward the back pocket.",
                "Control the lowering phase.",
            ),
            mistakes = listOf("Torso rotation", "Shrugging", "Jerking the bell"),
        ),
        Exercise(
            id = "overhead_press",
            name = "1-arm overhead press",
            prescription = "3 × 6–10 / side",
            movement = Movement.OverheadPress,
            cues = listOf(
                "Brace the abs and glutes.",
                "Finish with the arm stacked over the shoulder.",
                "Keep the ribs down instead of leaning backward.",
            ),
            mistakes = listOf("Back hyperextension", "Unstable wrist", "Pressing around the side"),
        ),
        Exercise(
            id = "suitcase_carry",
            name = "Suitcase carry",
            prescription = "3 × 40 sec / side",
            movement = Movement.Carry,
            cues = listOf(
                "Stand tall without leaning toward or away from the bell.",
                "Walk with quiet, controlled steps.",
                "Keep the ribs stacked over the pelvis.",
            ),
            mistakes = listOf("Side bending", "Shrugging", "Rushing the steps"),
        ),
        Exercise(
            id = "swing",
            name = "Kettlebell swing",
            prescription = "4 × 8",
            movement = Movement.Swing,
            cues = listOf(
                "Hinge at the hips rather than squatting.",
                "Snap the hips; the arms guide the bell.",
                "Finish tall without leaning backward.",
            ),
            mistakes = listOf("Squatting the swing", "Lifting with the shoulders", "Hyperextending at the top"),
        ),
        Exercise(
            id = "goblet_squat",
            name = "Goblet squat",
            prescription = "3 × 6–10",
            movement = Movement.Squat,
            cues = listOf(
                "Keep the bell close to the chest.",
                "Keep the whole foot planted.",
                "Let the knees track over the toes.",
            ),
            mistakes = listOf("Heels lifting", "Knees collapsing inward", "Losing trunk position"),
        ),
        Exercise(
            id = "split_squat",
            name = "Bulgarian split squat",
            prescription = "3 × 6–8 / leg",
            movement = Movement.SplitSquat,
            cues = listOf(
                "Use a stance long enough to keep the front heel down.",
                "Drop the back knee mostly downward.",
                "Drive through the whole front foot.",
            ),
            mistakes = listOf("Stance too narrow", "Front knee collapsing inward", "Pushing mostly from the back leg"),
        ),
        Exercise(
            id = "single_leg_rdl",
            name = "Single-leg kettlebell RDL",
            prescription = "3 × 6–8 / leg",
            movement = Movement.Hinge,
            cues = listOf(
                "Keep a soft bend in the stance knee.",
                "Reach the free leg backward as the torso tips forward.",
                "Keep the pelvis mostly square to the floor.",
            ),
            mistakes = listOf("Opening the hip", "Rounding the back", "Reaching the floor at all costs"),
        ),
        Exercise(
            id = "calf_raise",
            name = "Standing calf raise",
            prescription = "3 × 10–20",
            movement = Movement.CalfRaise,
            cues = listOf("Rise through the big-toe side of the foot.", "Pause at the top.", "Lower under control."),
            mistakes = listOf("Bouncing", "Rolling the ankle outward", "Rushing reps"),
        ),
        Exercise(
            id = "tibialis_raise",
            name = "Tibialis raise",
            prescription = "2 × 15–25",
            movement = Movement.TibialisRaise,
            cues = listOf("Keep the heels planted.", "Lift the forefoot toward the shins.", "Control the lowering."),
            mistakes = listOf("Heels lifting", "Rocking the whole body", "Short range"),
        ),
        Exercise(
            id = "copenhagen",
            name = "Copenhagen plank",
            prescription = "2 × 20–30 sec / side",
            movement = Movement.Copenhagen,
            cues = listOf("Start knee-supported if needed.", "Keep the body in one line.", "Lift the bottom hip toward the top leg."),
            mistakes = listOf("Hips sagging", "Trunk rotation", "Progressing the lever too quickly"),
        ),
        Exercise(
            id = "hamstring_slider",
            name = "Hamstring slider curl",
            prescription = "2 × 8–12",
            movement = Movement.HamstringCurl,
            cues = listOf("Bridge before extending the legs.", "Slide the heels away slowly.", "Keep the pelvis controlled."),
            mistakes = listOf("Hips dropping", "Moving too fast", "Forcing through hamstring pain"),
        ),
    )

    fun byId(id: String): Exercise? = exercises.firstOrNull { it.id == id }

    val monday = listOf(
        "floor_press",
        "row",
        "overhead_press",
        "suitcase_carry",
        "copenhagen",
        "hamstring_slider",
        "calf_raise",
    )

    val wednesday = listOf(
        "swing",
        "goblet_squat",
        "split_squat",
        "single_leg_rdl",
        "floor_press",
        "row",
        "calf_raise",
        "tibialis_raise",
    )
}
