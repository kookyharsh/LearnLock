package com.example.ui.screens

import com.example.ui.quiz.isValidSnippet
import com.example.ui.quiz.QuizQuestion
import com.example.ui.quiz.parseQuestionsList
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.QuestionHistory
import com.example.ui.components.MarkdownView
import com.example.ui.theme.CodeBlue
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElegantOnPrimary
import com.example.ui.theme.ElegantOnPrimaryContainer
import com.example.ui.theme.ElegantPrimary
import com.example.ui.theme.ElegantPrimaryContainer
import com.example.ui.theme.GoldStar
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConceptDetailScreen(
    item: QuestionHistory,
    onBack: () -> Unit,
    onStarToggled: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var isStarred by remember { mutableStateOf(item.isStarred) }

    Scaffold(
        containerColor = DarkBackground,
        contentWindowInsets = WindowInsets.statusBars
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Bar with Back and Star
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Text(
                        text = item.topic,
                        color = ElegantOnPrimaryContainer,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )


                IconButton(
                    onClick = {
                        isStarred = !isStarred
                        onStarToggled(isStarred)
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (isStarred) Icons.Default.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Star Concept",
                        tint = if (isStarred) GoldStar else TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Spacer(modifier = Modifier.height(8.dp))

            // Unboxed Reader Container (Clean Article Layout)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                // Topic & AI Badge Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(ElegantPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = ElegantOnPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "AI CONCEPT SUMMARY",
                            color = ElegantPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DarkSurface
                    ) {
                        Text(
                            text = item.topic.uppercase(),
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = item.conceptTitle,
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 32.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Article Summary Body (Rich Markdown Format)
                MarkdownView(markdownText = item.conceptSummary ?: item.explanation)

                if (item.codeSnippetPrefix.isValidSnippet()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DarkSurface,
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = item.codeSnippetPrefix!!,
                            color = CodeBlue,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }

                // Divider and Concept Quizzes Header
                Spacer(modifier = Modifier.height(28.dp))
                HorizontalDivider(color = DarkBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(20.dp))

                val questionsList: List<QuizQuestion> = remember(item) {
                    parseQuestionsList(
                        questionsJson = item.questionsJson,
                        fallbackQuestionText = item.questionText,
                        fallbackQuestionType = item.questionType,
                        fallbackOptionsJson = item.optionsJson,
                        fallbackCodePrefix = item.codeSnippetPrefix,
                        fallbackCorrectAnswer = item.correctAnswer,
                        fallbackExplanation = item.explanation
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ElegantPrimary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "KNOWLEDGE CHECK",
                            color = ElegantPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = "• ${questionsList.size} Questions",
                        color = TextMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                questionsList.forEachIndexed { idx, q ->
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            // Question Header
                            Text(
                                text = "Q${idx + 1}. ${q.questionText}",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 22.sp
                            )

                            if (q.optionsList.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                q.optionsList.forEachIndexed { optIdx, optText ->
                                    val isCorrect = checkIsCorrect(optIdx, optText, q.correctAnswer)
                                    val isUserSel = checkIsUserSelected(optIdx, optText, item.userAnswer)

                                    val optionBorder = when {
                                        isCorrect -> SuccessGreen
                                        isUserSel -> ElegantPrimary
                                        else -> DarkBorder
                                    }
                                    val optionBg = when {
                                        isCorrect -> SuccessGreen.copy(alpha = 0.12f)
                                        isUserSel -> ElegantPrimaryContainer.copy(alpha = 0.2f)
                                        else -> DarkBackground
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = optionBg,
                                        border = CardDefaults.outlinedCardBorder().copy(
                                            brush = androidx.compose.ui.graphics.SolidColor(optionBorder)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                val letterLabel = when (optIdx) { 0 -> "A"; 1 -> "B"; 2 -> "C"; 3 -> "D"; else -> "${optIdx + 1}" }
                                                Surface(
                                                    shape = CircleShape,
                                                    color = if (isCorrect) SuccessGreen else if (isUserSel) ElegantPrimary else DarkSurface,
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text(
                                                            text = letterLabel,
                                                            color = if (isCorrect || isUserSel) ElegantOnPrimary else TextMuted,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = optText,
                                                    color = if (isCorrect || isUserSel) TextPrimary else TextSecondary,
                                                    fontSize = 14.sp,
                                                    fontWeight = if (isCorrect || isUserSel) FontWeight.SemiBold else FontWeight.Normal
                                                )
                                            }

                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (isUserSel) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = ElegantPrimary.copy(alpha = 0.2f)
                                                    ) {
                                                        Text(
                                                            text = "Your Answer",
                                                            color = ElegantPrimary,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                                if (isCorrect) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = SuccessGreen.copy(alpha = 0.2f)
                                                    ) {
                                                        Text(
                                                            text = "Correct",
                                                            color = SuccessGreen,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (q.explanation.isNotBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = DarkBackground,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "EXPLANATION",
                                            color = TextMuted,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = q.explanation,
                                            color = TextSecondary,
                                            fontSize = 13.sp,
                                            lineHeight = 19.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = onBack,
                shape = CircleShape,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("Return to App", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun parseMarkdownToAnnotatedString(text: String): AnnotatedString {
    return buildAnnotatedString {
        var index = 0
        while (index < text.length) {
            when {
                text.startsWith("**", index) && text.indexOf("**", index + 2) != -1 -> {
                    val end = text.indexOf("**", index + 2)
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = TextPrimary))
                    append(text.substring(index + 2, end))
                    pop()
                    index = end + 2
                }
                text.startsWith("__", index) && text.indexOf("__", index + 2) != -1 -> {
                    val end = text.indexOf("__", index + 2)
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = TextPrimary))
                    append(text.substring(index + 2, end))
                    pop()
                    index = end + 2
                }
                text.startsWith("<u>", index) && text.indexOf("</u>", index + 3) != -1 -> {
                    val end = text.indexOf("</u>", index + 3)
                    pushStyle(SpanStyle(textDecoration = TextDecoration.Underline, color = TextPrimary))
                    append(text.substring(index + 3, end))
                    pop()
                    index = end + 4
                }
                text.startsWith("`", index) && text.indexOf("`", index + 1) != -1 -> {
                    val end = text.indexOf("`", index + 1)
                    pushStyle(SpanStyle(fontFamily = FontFamily.Monospace, color = CodeBlue))
                    append(text.substring(index + 1, end))
                    pop()
                    index = end + 1
                }
                text.startsWith("*", index) && text.indexOf("*", index + 1) != -1 -> {
                    val end = text.indexOf("*", index + 1)
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append(text.substring(index + 1, end))
                    pop()
                    index = end + 1
                }
                text.startsWith("_", index) && text.indexOf("_", index + 1) != -1 -> {
                    val end = text.indexOf("_", index + 1)
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append(text.substring(index + 1, end))
                    pop()
                    index = end + 1
                }
                else -> {
                    append(text[index])
                    index++
                }
            }
        }
    }
}

private fun checkIsCorrect(optIndex: Int, optionText: String, correctAnswer: String): Boolean {
    val trimmed = correctAnswer.trim()
    val letter = when (optIndex) { 0 -> "A"; 1 -> "B"; 2 -> "C"; 3 -> "D"; else -> "" }
    return trimmed.equals(letter, ignoreCase = true) ||
            trimmed == optIndex.toString() ||
            trimmed.equals(optionText.trim(), ignoreCase = true)
}

private fun checkIsUserSelected(optIndex: Int, optionText: String, userAnswer: String?): Boolean {
    if (userAnswer.isNullOrBlank()) return false
    val trimmed = userAnswer.trim()
    val letter = when (optIndex) { 0 -> "A"; 1 -> "B"; 2 -> "C"; 3 -> "D"; else -> "" }
    return trimmed.equals(letter, ignoreCase = true) ||
            trimmed == optIndex.toString() ||
            trimmed.equals(optionText.trim(), ignoreCase = true)
}
