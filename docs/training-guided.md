# Training: guided and simple modes

The Training page shows one exercise at a time with explicit Previous / Next buttons and an always-available Session list. Guided is the initial mode; the user's mode and audio preferences persist locally. Simple mode has a compact form icon on the right and a full-width Log sets button at the bottom.

Guided sessions use a 10-second preparation countdown, spoken form cues through Android text-to-speech, repeating start/finish position diagrams, explicit rep completion, timed carries/holds, side changes, and rest. Users can pause, resume, end, or shorten rest. Plan and mode controls are hidden during active exercises; opening guidance or the session list pauses the timer. Leaving the foreground pauses the session and stops speech; the screen stays awake during active foreground training. Completed exercises are reviewed before saving. Today's progress reflects saved workout entries, not elapsed timers.

These are position diagrams, not hosted looping instructional videos. Full external technique references remain accessible through the play icon. Coach speech depends on an installed TTS voice; all cues also appear as text. Timed Copenhagen work uses the existing prescription's lower bound (20 seconds). The current workout storage supports sets/reps/load/RPE, not per-side duration history.

## Research basis

No source establishes one universally “most addictive” workout design. This implementation applies useful engagement patterns without claiming a measured retention improvement:

- Fitbod combines demonstrations, prescriptions, and logging in its exercise detail view: https://help.fitbod.me/hc/en-us/articles/30721437384215-How-to-Navigate-the-Exercise-Details-Screen
- Fitbod uses rest timers between sets: https://help.fitbod.me/hc/en-us/articles/360006340194-Rest-Timer
- Freeletics uses feedback after workouts; FootyOS retains RPE in the review/log flow: https://www.freeletics.com/en/blog/posts/what-is-the-purpose-of-the-feedback-i-am-asked-to-give-after-each-workout/

## Validation

Unit tests cover self-paced reps, timed intervals, both sides before advancing sets, completion boundaries, and catalog prescriptions. Emulator coverage checks form access, guided start/pause/end, mode switching, and manual logging access alongside existing app navigation and data tests.

## Sequential sessions and personal records

The mode selector is now a segmented Guided / Simple control. The disabled exercise dropdown is replaced by an always-available Session list, Previous / Next controls, and an Up next preview. Saving succeeds before advancing; guided Save & next automatically begins the following exercise's 10-second preparation countdown. Skipping an active exercise asks before discarding the current timer and never logs an unperformed exercise. The final exercise offers Save & finish and Review session. Completing every planned exercise produces a session completion message.

Previous load, sets, and reps appear on the exercise and in the logging dialog. A Personal bests & history panel displays records and recent entries. Saved improvements earn a trophy banner for weight, reps at the same load, or sets; first sessions establish a baseline, and ties do not earn a record. Records are calculated from existing workout history and survive app restarts without a database migration. Reps are entered per set; time/speed records are not inferred from strength-work timers. Save failures retain the entered data and do not advance or award records.

Additional research:
- Hevy shows previous workout values while logging: https://help.hevyapp.com/hc/en-us/articles/36011896355479-How-to-Use-Previous-Workout-Values-to-Improve-Performance-in-Hevy
- Hevy distinguishes weight, reps, and duration records: https://help.hevyapp.com/hc/en-us/articles/35382889578135-Exercise-Performance-Tracking-in-Library-Weight-Bodyweight-Cardio-and-Duration-Based-Exercises
- Fitbudd inserts preparation time before each exercise: https://help.fitbudd.com/en/articles/7959133-how-to-enable-exercise-prep-time-for-your-workouts

New tests cover matching-load rep comparisons, ties, first sessions, independent exercises, same-day history ordering, save/advance/record interaction, failed-save recovery, and explicit skips during active sessions.
