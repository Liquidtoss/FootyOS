package app.footyos.data

import app.footyos.nutrition.GeminiClient
import app.footyos.nutrition.GeminiFailure
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class GeminiProtocolTest {
    @Test fun separatesHttpFailuresWithoutEchoingProviderText() {
        assertEquals("model", GeminiClient.httpFailure(404))
        assertEquals("request", GeminiClient.httpFailure(400))
        assertEquals("permission", GeminiClient.httpFailure(403))
        assertEquals("quota", GeminiClient.httpFailure(429))
        assertEquals("server", GeminiClient.httpFailure(503))
        assertEquals("auth", GeminiClient.httpFailure(400, """{"error":{"message":"secret photo or key must not be echoed","details":[{"reason":"API_KEY_INVALID"}]}}"""))
    }
    @Test fun identifiesTruncationBlockingAndEmptyResponses() {
        assertReason("truncated", """{"candidates":[{"finishReason":"MAX_TOKENS"}]}""")
        assertReason("blocked", """{"promptFeedback":{"blockReason":"SAFETY"}}""")
        assertReason("empty_output", """{"candidates":[]}""")
        assertReason("invalid_output", "not JSON")
    }
    @Test fun acceptsACompleteProviderResponseAndRejectsInvalidMealData() {
        val meal = """{"name":"Lunch","calories":600,"protein":30,"carbs":60,"fat":20,"lowerCalories":450,"upperCalories":750,"assumptions":"Portions uncertain","foods":[{"name":"Food","grams":250}]}"""
        val response = envelope(meal)
        assertEquals(meal, GeminiClient.decodeResponse(response).json)
        assertEquals("test-model", GeminiClient.decodeResponse(response).model)
        assertReason("no_food", envelope(meal.replace("[{\"name\":\"Food\",\"grams\":250}]", "[]")))
        assertReason("invalid_output", envelope(meal.replace("\"lowerCalories\":450", "\"lowerCalories\":900")))
    }
    private fun envelope(text: String) = JSONObject().put("modelVersion", "test-model").put("candidates", JSONArray().put(
        JSONObject().put("finishReason", "STOP").put("content", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", text))))
    )).toString()
    private fun assertReason(reason: String, body: String) {
        val error = assertThrows(GeminiFailure::class.java) { GeminiClient.decodeResponse(body) }
        assertEquals(reason, error.reason)
    }
}
