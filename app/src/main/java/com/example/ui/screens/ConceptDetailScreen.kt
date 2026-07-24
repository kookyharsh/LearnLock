package com.example.ui.screens

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

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ElegantPrimaryContainer
                ) {
                    Text(
                        text = item.topic,
                        color = ElegantOnPrimaryContainer,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

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

            // ChatGPT Style AI Response Container
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // AI Avatar Tag
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

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = item.conceptTitle,
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Markdown Bubble Container
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = DarkBackground,
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = parseMarkdownToAnnotatedString(item.conceptSummary ?: item.explanation),
                                color = TextSecondary,
                                fontSize = 15.sp,
                                lineHeight = 23.sp
                            )
                        }
                    }

                    // Concept Quizzes (3 Questions)
                    Spacer(modifier = Modifier.height(20.dp))

                    val questionsList = remember(item) {
                        com.example.ui.quiz.parseQuestionsList(
                            questionsJson = item.questionsJson,
                            fallbackQuestionText = item.questionText,
                            fallbackQuestionType = item.questionType,
                            fallbackOptionsJson = item.optionsJson,
                            fallbackCodePrefix = item.codeSnippetPrefix,
                            fallbackCorrectAnswer = item.correctAnswer,
                            fallbackExplanation = item.explanation
                        )
                    }

                    Text(
                        text = "CONCEPT QUIZZES (${questionsList.size} QUESTIONS)",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    questionsList.forEachIndexed { idx, q ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = DarkSurfaceVariant,
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Q${idx + 1}. ${q.questionText}",
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (q.optionsList.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    q.optionsList.forEach { opt ->
                                        Text(
                                            text = "• $opt",
                                            color = TextSecondary,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                                if (q.explanation.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Explanation: ${q.explanation}",
                                        color = TextMuted,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp
                                    )
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
