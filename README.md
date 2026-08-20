# FootyOS

FootyOS is a local-first Android performance system for footballers. It combines nutrition, athletic development, football skill training, match review, progress tracking, reminders, and calendar integration.

## Current product goals

- Make the daily plan obvious.
- Track body-weight and nutrition progress without sacrificing football performance.
- Build strength, speed, durability, and technical skill around the user's actual match schedule.
- Keep user data local by default.
- Make reminders open the exact FootyOS destination they refer to.
- Integrate with device calendars without requiring a cloud account.

## Android stack

- Kotlin with Android Gradle Plugin built-in Kotlin support
- Jetpack Compose and Material 3
- Lifecycle-aware ViewModels and `StateFlow`
- Room for structured performance data
- DataStore for lightweight user settings
- AlarmManager + notifications for local reminders
- `CalendarContract` intents for calendar interoperability

The project follows Android's recommended layered architecture: UI state is exposed by ViewModels, repositories own data access, and Android framework integrations are kept behind dedicated classes.

## Build

CI uses Android Gradle Plugin 9.3, Gradle 9.5, JDK 17, compile SDK 36, and target SDK 36. API 36 is the stable baseline; newer Android preview SDKs should be validated in a separate preview job rather than blocking the production build.

```bash
gradle :app:testDebugUnitTest
gradle :app:assembleDebug
```

The GitHub Actions workflow uploads a debug APK artifact after successful builds.

## Deep links

FootyOS accepts:

- `footyos://open/today`
- `footyos://open/nutrition`
- `footyos://open/training`
- `footyos://open/soccer`
- `footyos://open/progress`
- `footyos://open/schedule`

These links are used by notifications and can be embedded in calendar events.

## Privacy direction

FootyOS is intentionally local-first. A future public release can add opt-in backup or Health Connect support without making accounts or remote storage mandatory.

## Development

The native app is currently an early private build. The immediate priorities are:

1. Stabilize CI and APK delivery.
2. Complete exercise logging and form-reference screens.
3. Add editable weekly schedules and calendar sync.
4. Add automated calorie-trend recommendations.
5. Add charts, adherence, streaks, and richer match analytics.
6. Generalize onboarding for users beyond the original training profile.
