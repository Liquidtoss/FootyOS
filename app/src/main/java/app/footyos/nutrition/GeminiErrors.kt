package app.footyos.nutrition

/** Only fixed diagnostic codes are exposed; never provider messages, keys, or photo contents. */
object GeminiErrors {
    fun message(code: String): String = when (code) {
        "auth" -> "Google rejected the API key. Replace it in Gemini setup."
        "permission" -> "Google denied this project's access. Check that the Gemini API is enabled and the key permits it."
        "request" -> "Google rejected the request format (HTTP 400). Report this diagnostic so the integration can be corrected."
        "model" -> "Google could not find the configured model for this project (HTTP 404)."
        "quota" -> "Gemini quota reached (HTTP 429). Wait for quota reset or use offline entry."
        "server" -> "Google's service is temporarily unavailable (HTTP 5xx). Try again later."
        "timeout" -> "Gemini did not respond before the connection timed out. Check connectivity or try again later."
        "offline" -> "Could not reach Google. Check your internet connection, DNS, or VPN."
        "tls" -> "Could not establish a secure connection to Google. Check the phone's date/time and network."
        "network" -> "The connection to Google failed. Try another network or use offline entry."
        "blocked" -> "Google blocked analysis of this image. Try a clear photo of food only."
        "truncated" -> "Gemini's answer was cut off before completing the estimate. Retry or use offline entry."
        "no_food" -> "Gemini could not identify food in this photo. Try a clearer photo or enter the meal offline."
        "invalid_output" -> "Gemini returned incomplete or inconsistent nutrition values. Retry or enter the meal offline."
        "empty_output" -> "Google returned no estimate. Retry or use offline entry."
        "image_size" -> "This image is too large to analyze. Retake it or choose another photo."
        "response_size" -> "Google returned an unexpectedly large response. Use offline entry and report this diagnostic."
        "setup" -> "Add or replace your Gemini key in Gemini setup."
        "expired" -> "This photo expired. Add a new photo or use offline entry."
        "pending" -> "The previous request was interrupted. Use offline entry or explicitly retry."
        "loading" -> "Estimating this meal…"
        "" -> "Offline mode. Use the calculator below or enable Gemini in setup."
        else -> "The earlier request failed without a detailed diagnostic. Tap Retry Gemini to get the cause."
    }
}
