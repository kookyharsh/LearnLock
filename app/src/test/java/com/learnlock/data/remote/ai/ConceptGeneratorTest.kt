package com.learnlock.data.remote.ai

import androidx.test.core.app.ApplicationProvider
import com.learnlock.data.preferences.AppPreferencesManager
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConceptGeneratorTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val prefsManager = AppPreferencesManager(context)
    private val generator = ConceptGenerator(prefsManager)

    @Test
    fun testSanitizeAndNormalizeConcept_normalizesTypesAndAnswers() {
        val json = JSONObject().apply {
            put("conceptTitle", "Binary Search")
            put("conceptSummary", "Binary search finds an element in a sorted list in O(log n) time.")
            put("questions", JSONArray().apply {
                put(JSONObject().apply {
                    put("questionType", "mcq")
                    put("questionText", "What is the time complexity?")
                    put("options", JSONArray().apply {
                        put("O(1)")
                        put("O(log n)")
                        put("O(n)")
                        put("O(n^2)")
                    })
                    put("correctAnswer", "O(log n)")
                    put("explanation", "Halves search space each step.")
                })
                put(JSONObject().apply {
                    put("questionType", "true_false")
                    put("questionText", "Binary search requires a sorted list.")
                    put("options", JSONArray().apply {
                        put("True")
                        put("False")
                    })
                    put("correctAnswer", "True")
                    put("explanation", "Must be sorted.")
                })
            })
        }

        val isValid = generator.sanitizeAndNormalizeConcept(json, expectedQuestionCount = 2)
        assertTrue(isValid)

        val questions = json.getJSONArray("questions")
        val q1 = questions.getJSONObject(0)
        assertEquals("MCQ", q1.getString("questionType"))
        assertEquals("1", q1.getString("correctAnswer")) // "O(log n)" mapped to index 1

        val q2 = questions.getJSONObject(1)
        assertEquals("TRUE_FALSE", q2.getString("questionType"))
        assertEquals("0", q2.getString("correctAnswer")) // "True" mapped to "0"
    }

    @Test
    fun testSanitizeAndNormalizeConcept_normalizesFillInTheBlank() {
        val json = JSONObject().apply {
            put("conceptTitle", "Photosynthesis")
            put("conceptSummary", "Plants convert light energy into chemical energy in plants.")
            put("questions", JSONArray().apply {
                put(JSONObject().apply {
                    put("questionType", "fill_in_the_blank")
                    put("questionText", "Photosynthesis produces [blank] as a byproduct.")
                    put("correctAnswer", "oxygen")
                    put("explanation", "Oxygen is released.")
                })
            })
        }

        val isValid = generator.sanitizeAndNormalizeConcept(json, expectedQuestionCount = 1)
        assertTrue(isValid)

        val q = json.getJSONArray("questions").getJSONObject(0)
        assertEquals("FILL_BLANK", q.getString("questionType"))
        assertTrue(q.getString("questionText").contains("___"))
    }
}
