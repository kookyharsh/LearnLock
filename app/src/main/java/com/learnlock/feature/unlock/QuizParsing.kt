package com.learnlock.feature.unlock

import org.json.JSONArray

fun String?.isValidSnippet(): Boolean {
    if (this == null) return false
    val trimmed = this.trim()
    return trimmed.isNotEmpty() && !trimmed.equals("null", ignoreCase = true)
}

internal fun isProgrammingSubject(topic: String): Boolean {
    val normalized = topic.lowercase()
    return listOf(
        "programming", "computer", "software", "technology", "coding",
        "python", "java", "kotlin", "javascript", "sql", "web development",
    ).any(normalized::contains)
}

internal fun looksLikeProgrammingCode(value: String?): Boolean {
    if (value.isNullOrBlank()) return false
    val normalized = value.lowercase()
    return listOf(
        "def ", "return ", "function ", "const ", "let ", "var ",
        "if (", "if ", "{", "};", "public class", "select ", "import ",
    ).any(normalized::contains)
}

fun getCorrectOptionIndex(q: QuizQuestion): Int {
    val cAns = q.correctAnswer.trim()
    val idxAsInt = cAns.toIntOrNull()
    if (idxAsInt != null && idxAsInt in q.optionsList.indices) {
        return idxAsInt
    }
    val letterIdx = when (cAns.uppercase()) {
        "A" -> 0; "B" -> 1; "C" -> 2; "D" -> 3; else -> -1
    }
    if (letterIdx != -1 && letterIdx < q.optionsList.size) {
        return letterIdx
    }
    val textMatchIdx = q.optionsList.indexOfFirst { it.trim().equals(cAns, ignoreCase = true) }
    if (textMatchIdx != -1) return textMatchIdx
    // Ungradable: the stored answer matches nothing. Callers must exclude
    // such questions from grading instead of accepting option A.
    return -1
}

/** CODE / FILL_BLANK questions are answered by typing, not by picking. */
internal fun isTextAnswerQuestion(q: QuizQuestion): Boolean =
    q.questionType == "CODE" || q.questionType == "FILL_BLANK" || q.questionType == "SHORT_ANSWER"

internal fun isQuestionGradable(q: QuizQuestion): Boolean = when (q.questionType) {
    "SHORT_ANSWER" -> false
    "CODE", "FILL_BLANK" -> q.correctAnswer.isNotBlank()
    "MULTI_SELECT", "DRAG_BLANK" -> parseIndexAnswer(q.correctAnswer).isNotEmpty()
    else -> getCorrectOptionIndex(q) != -1
}

internal fun isQuestionCorrect(q: QuizQuestion, selectedIdx: Int?, codeText: String?): Boolean {
    if (q.questionType == "SHORT_ANSWER") return false
    if (q.questionType == "MULTI_SELECT") {
        val selected = codeText?.splitToSequence(",")?.mapNotNull { it.toIntOrNull() }?.toSet().orEmpty()
        return selected.isNotEmpty() && selected == parseIndexAnswer(q.correctAnswer).toSet()
    }
    if (q.questionType == "DRAG_BLANK") {
        val selected = codeText?.splitToSequence(",")?.mapNotNull { it.toIntOrNull() }?.toList().orEmpty()
        return selected.isNotEmpty() && selected == parseIndexAnswer(q.correctAnswer)
    }
    if (isTextAnswerQuestion(q)) {
        if (q.correctAnswer.isBlank()) return false
        val userText = codeText?.trim().orEmpty()
        return userText.isNotBlank() && (listOf(q.correctAnswer) + q.acceptedAnswers).any { userText.equals(it.trim(), ignoreCase = true) }
    }
    val correctOptIdx = getCorrectOptionIndex(q)
    return correctOptIdx != -1 && (selectedIdx ?: -1) == correctOptIdx
}

internal fun parseIndexAnswer(answer: String): List<Int> = try {
    val array = JSONArray(answer)
    (0 until array.length()).mapNotNull { array.optString(it).toIntOrNull() }
} catch (_: Exception) {
    answer.split(",").mapNotNull { it.trim().toIntOrNull() }
}

fun parseQuestionsList(
    questionsJson: String?,
    fallbackQuestionText: String,
    fallbackQuestionType: String,
    fallbackOptionsJson: String?,
    fallbackCodePrefix: String?,
    fallbackCorrectAnswer: String,
    fallbackExplanation: String,
): List<QuizQuestion> {
    val cleanFallbackPrefix = fallbackCodePrefix?.takeIf { it.isValidSnippet() }
    if (!questionsJson.isNullOrBlank()) {
        try {
            val array = JSONArray(questionsJson)
            val list = mutableListOf<QuizQuestion>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val qText = obj.optString("questionText", obj.optString("question", fallbackQuestionText))
                var qType = obj.optString("questionType", fallbackQuestionType).uppercase()
                qType = when (qType) {
                    "MULTIPLE_CHOICE" -> "MCQ"
                    "MULTIPLE_SELECT" -> "MULTI_SELECT"
                    "FILL_IN_THE_BLANK" -> "FILL_BLANK"
                    "DRAG_AND_DROP" -> "DRAG_BLANK"
                    else -> qType
                }
                val optArray = obj.optJSONArray("options")
                val opts = mutableListOf<String>()
                if (optArray != null) {
                    for (j in 0 until optArray.length()) {
                        opts.add(optArray.getString(j))
                    }
                }

                if (opts.isEmpty() && qType == "TRUE_FALSE") {
                    opts.addAll(listOf("True", "False"))
                }
                val rawCodePref = if (obj.isNull("codeSnippetPrefix")) null else obj.getString("codeSnippetPrefix")
                val codePref = rawCodePref?.takeIf { it.isValidSnippet() }
                val cAns = obj.optString("correctAnswer", "0")
                val exp = obj.optString("explanation", fallbackExplanation)
                val acceptedAnswers = obj.optJSONArray("acceptedAnswers")?.let { arr ->
                    (0 until arr.length()).map { arr.optString(it) }
                }.orEmpty()
                list.add(QuizQuestion(qText, qType, opts, codePref, cAns, exp, acceptedAnswers, obj.optString("hint").takeIf { it.isNotBlank() && it != "null" }))
            }
            if (list.isNotEmpty()) return list
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    return listOf(
        QuizQuestion(
            questionText = fallbackQuestionText,
            questionType = fallbackQuestionType,
            optionsList = parseOptionsJson(fallbackOptionsJson),
            codeSnippetPrefix = cleanFallbackPrefix,
            correctAnswer = fallbackCorrectAnswer,
            explanation = fallbackExplanation
        )
    )
}

private fun parseOptionsJson(jsonStr: String?): List<String> {
    if (jsonStr.isNullOrBlank()) return emptyList()
    return try {
        val array = JSONArray(jsonStr)
        val list = mutableListOf<String>()
        for (i in 0 until array.length()) {
            list.add(array.getString(i))
        }
        list
    } catch (_: Exception) {
        emptyList()
    }
}
