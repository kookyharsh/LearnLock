package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun MarkdownView(
    markdownText: String,
    modifier: Modifier = Modifier
) {
    val blocks = parseMarkdownBlocks(markdownText)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Header -> {
                    val (fontSize, lineHeight) = when (block.level) {
                        1 -> 22.sp to 28.sp
                        2 -> 19.sp to 25.sp
                        else -> 17.sp to 22.sp
                    }
                    Text(
                        text = parseInlineMarkdown(block.content),
                        color = TextPrimary,
                        fontSize = fontSize,
                        fontWeight = FontWeight.Bold,
                        lineHeight = lineHeight,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = parseInlineMarkdown(block.content),
                        color = TextPrimary.copy(alpha = 0.9f),
                        fontSize = 15.sp,
                        lineHeight = 23.sp
                    )
                }

                is MarkdownBlock.BulletItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "• ",
                            color = ElegantPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = parseInlineMarkdown(block.content),
                            color = TextPrimary.copy(alpha = 0.9f),
                            fontSize = 15.sp,
                            lineHeight = 23.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                is MarkdownBlock.NumberedItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "${block.number}. ",
                            color = ElegantPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = parseInlineMarkdown(block.content),
                            color = TextPrimary.copy(alpha = 0.9f),
                            fontSize = 15.sp,
                            lineHeight = 23.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                is MarkdownBlock.CodeBlock -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DarkSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            if (block.language.isNotBlank()) {
                                Text(
                                    text = block.language.uppercase(),
                                    color = TextMuted,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                            Text(
                                text = block.code,
                                color = CodeBlue,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            )
                        }
                    }
                }

                is MarkdownBlock.Quote -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(24.dp)
                                .background(ElegantPrimary, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = parseInlineMarkdown(block.content),
                            color = TextSecondary,
                            fontStyle = FontStyle.Italic,
                            fontSize = 14.sp,
                            lineHeight = 21.sp
                        )
                    }
                }

                is MarkdownBlock.Divider -> {
                    HorizontalDivider(color = DarkBorder, thickness = 1.dp)
                }
            }
        }
    }
}

sealed class MarkdownBlock {
    data class Header(val level: Int, val content: String) : MarkdownBlock()
    data class Paragraph(val content: String) : MarkdownBlock()
    data class BulletItem(val content: String) : MarkdownBlock()
    data class NumberedItem(val number: String, val content: String) : MarkdownBlock()
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock()
    data class Quote(val content: String) : MarkdownBlock()
    object Divider : MarkdownBlock()
}

fun parseMarkdownBlocks(text: String): List<MarkdownBlock> {
    if (text.isBlank()) return emptyList()
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = text.lines()
    var i = 0

    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trim()

        when {
            trimmed.startsWith("```") -> {
                val lang = trimmed.removePrefix("```").trim()
                val codeLines = mutableListOf<String>()
                i++
                while (i < lines.size && !lines[i].trim().startsWith("```")) {
                    codeLines.add(lines[i])
                    i++
                }
                blocks.add(MarkdownBlock.CodeBlock(lang, codeLines.joinToString("\n")))
                i++
            }
            trimmed.startsWith("# ") -> {
                blocks.add(MarkdownBlock.Header(1, trimmed.removePrefix("# ").trim()))
                i++
            }
            trimmed.startsWith("## ") -> {
                blocks.add(MarkdownBlock.Header(2, trimmed.removePrefix("## ").trim()))
                i++
            }
            trimmed.startsWith("### ") -> {
                blocks.add(MarkdownBlock.Header(3, trimmed.removePrefix("### ").trim()))
                i++
            }
            trimmed == "---" || trimmed == "***" -> {
                blocks.add(MarkdownBlock.Divider)
                i++
            }
            trimmed.startsWith("> ") -> {
                blocks.add(MarkdownBlock.Quote(trimmed.removePrefix("> ").trim()))
                i++
            }
            trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("+ ") -> {
                val content = trimmed.substring(2).trim()
                blocks.add(MarkdownBlock.BulletItem(content))
                i++
            }
            trimmed.matches(Regex("^\\d+\\.\\s.*")) -> {
                val dotIdx = trimmed.indexOf('.')
                val num = trimmed.substring(0, dotIdx).trim()
                val content = trimmed.substring(dotIdx + 1).trim()
                blocks.add(MarkdownBlock.NumberedItem(num, content))
                i++
            }
            trimmed.isNotBlank() -> {
                blocks.add(MarkdownBlock.Paragraph(trimmed))
                i++
            }
            else -> {
                i++
            }
        }
    }
    return blocks
}

fun parseInlineMarkdown(text: String): AnnotatedString {
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
