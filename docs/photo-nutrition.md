# Photo nutrition in FootyOS

## Private-installation Gemini setup

1. Create an API key in https://aistudio.google.com/apikey using a project with billing disabled. The app cannot detect or enforce Google's billing tier; a billed project can incur charges even on the same model endpoint.
2. Install the debug APK and open Nutrition → Gemini setup.
3. Enter the key on the phone, acknowledge photo transmission and the unbilled-project requirement, and choose Save key and enable. Do not commit keys or paste them into development chats.
4. Take or choose a meal photo, optionally describe ingredients and portions in Meal details, then tap Estimate meal. The app compresses it and sends it with those details once to `gemini-3.1-flash-lite` through HTTPS `generateContent`. Review the foods, estimated portions, calorie range, assumptions and editable macro totals, then confirm.

The key is encrypted with Android Keystore AES-GCM and stored in `noBackupFilesDir`. It is not compiled into the APK, logged, or backed up. Remove key and disable stops new analysis requests; an already submitted request may complete. This BYOK setup is for the user's private installation, not distribution with a shared developer key. A distributed version needs an authenticated backend.

Google currently lists a free tier for the selected model, subject to quota and availability. There is no paid-tier/model fallback, web grounding, background retry or automatic reanalysis. Free-service content can be used to improve Google's products and reviewed by humans, subject to regional terms. Local deletion does not control provider retention. Official references:
- https://ai.google.dev/gemini-api/docs/pricing
- https://ai.google.dev/gemini-api/docs/api-key
- https://ai.google.dev/gemini-api/terms
- https://ai.google.dev/api/generate-content

## Local storage and resource use

Camera/gallery images are decoded at a bounded size (maximum 1200-pixel long edge), orientation-normalized, and encoded as JPEG quality 80. Re-encoding removes original EXIF metadata. Gallery input is capped at 25 MB; analysis input is capped at 5 MB. Images live in an app-private directory excluded by backup allowlists. Previews load on demand and processing/network operations run off the main thread.

Android JobScheduler performs daily and launch-triggered photo cleanup. Execution may be delayed by Android. Accessing an expired image also deletes it. Photos expire after 30 days; nutrition and estimate records remain. Deleted meals cascade-delete estimate/comparison rows, while orphan images age out under retention cleanup. Photo-analysis records contain structured nutrition results, never image bytes or keys.

Room v4 has a durable request marker per photo. A completed result is reused on navigation and restart; a pending marker after process death does not automatically resend. Quota/auth/network/unusable-result failures leave offline logging available. The explicit Retry Gemini action sends another request. If the remote request completed before a local crash, exactly-once provider execution cannot be guaranteed on explicit retry.

Confirming saves the meal, raw offline snapshot, available Gemini snapshot, and comparison transactionally. Only confirmed totals contribute to macro progress. Existing daily totals are a separate manual baseline: do not log the same food in both. Nutrition and macro goals persist in Room and DataStore respectively.

## Offline fallback and agreement

The offline calculator uses user-entered per-100g nutrition label values and grams eaten for one or more foods. It is not offline photo recognition. Apply the result, correct totals if necessary, and confirm. Original offline inputs remain separate from corrected totals. Replacing a photo requires reapplying an offline estimate for that photo before comparison.

For each nonzero nutrient pair, agreement is `1 - abs(offline - gemini) / max(offline, gemini)`. The stored score is 100 times the mean for calories, protein, carbs and fat, excluding both-zero pairs; all-zero results are unscored. Signed deltas are `offline - gemini`. The formula version is `symmetric-max-v1`. Only matching photo references are compared, and the first estimate from each source is retained on duplicate delivery.

Agreement is not accuracy or probability. Example: 500 versus 600 kcal with three identical nonzero macros gives 95.83 agreement; inspect nutrient deltas as well. Offline input metadata records `geminiShownBeforeOffline` so assisted comparisons can be separated from independent ones. True accuracy evaluation needs weighed/label references, not Gemini as ground truth.

## Validation and remaining setup

Compilation and unit tests are available locally. Instrumentation covers migration, meal persistence, image expiry, agreement, encrypted key storage, successful-response reuse, quota/interrupted-request behavior, result validation and navigation. Gemini tests use a fake transport and make no provider calls. A real API smoke test still requires the user's key entered in the app. Food/portion accuracy and physical-device capture behavior remain to be evaluated.

Meal details are capped at 1,000 characters, sent only when Estimate meal is tapped, and stored with the completed response. Completed results remain cached; their original meal details are read-only.
