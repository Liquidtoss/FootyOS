# Training: guided and simple modes

The Training page now shows one exercise at a time through a dropdown. Guided is the initial mode; the user's mode and audio preferences persist locally. Simple mode has a compact form icon on the right and a full-width Log sets button at the bottom.

Guided sessions use a 10-second preparation countdown, spoken form cues through Android text-to-speech, repeating start/finish position diagrams, explicit rep completion, timed carries/holds, side changes, and rest. Users can pause, resume, end, or shorten rest. Switching plans and modes is disabled during an active exercise. Leaving the foreground pauses the session and stops speech; the screen stays awake during active foreground training. Completed exercises are reviewed before saving. Today's progress reflects saved workout entries, not elapsed timers.

These are position diagrams, not hosted looping instructional videos. Full external technique references remain accessible through the play icon. Coach speech depends on an installed TTS voice; all cues also appear as text. Timed Copenhagen work uses the existing prescription's lower bound (20 seconds). The current workout storage supports sets/reps/load/RPE, not per-side duration history.

## Research basis

No source establishes one universally “most addictive” workout design. This implementation applies useful engagement patterns without claiming a measured retention improvement:

- Fitbod combines demonstrations, prescriptions, and logging in its exercise detail view: https://help.fitbod.me/hc/en-us/articles/30721437384215-How-to-Navigate-the-Exercise-Details-Screen
- Fitbod uses rest timers between sets: https://help.fitbod.me/hc/en-us/articles/360006340194-Rest-Timer
- Freeletics uses feedback after workouts; FootyOS retains RPE in the review/log flow: https://www.freeletics.com/en/blog/posts/what-is-the-purpose-of-the-feedback-i-am-asked-to-give-after-each-workout/

## Validation

Unit tests cover self-paced reps, timed intervals, both sides before advancing sets, completion boundaries, and catalog prescriptions. Emulator coverage checks form access, guided start/pause/end, mode switching, and manual logging access alongside existing app navigation and data tests.
