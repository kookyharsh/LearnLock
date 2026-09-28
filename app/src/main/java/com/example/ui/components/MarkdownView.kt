package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

@Composable
fun MarkdownView(
    markdownText: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    val blocks = remember(markdownText) { parseMarkdownBlocks(markdownText) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Header -> {
                    val style = when (block.level) {
                        1 -> type.headlineSmall
                        2 -> type.titleLarge
                        else -> type.titleMedium
                    }
                    Text(
                        text = parseInlineMarkdown(
                            block.content,
                            emphasisColor = colors.onSurface,
                            codeColor = colors.primary,
                        ),
                        style = style,
                        color = colors.onSurface,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .semantics { heading() },
                    )
                }

                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = parseInlineMarkdown(
                            block.content,
                            emphasisColor = colors.onSurface,
                            codeColor = colors.primary,
                        ),
                        style = type.bodyMedium,
                        color = colors.onSurface,
                    )
                }

                is MarkdownBlock.BulletItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 6.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            text = "• ",
                            style = type.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = parseInlineMarkdown(
                                block.content,
                                emphasisColor = colors.onSurface,
                                codeColor = colors.primary,
                            ),
                            style = type.bodyMedium,
                            color = colors.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                is MarkdownBlock.NumberedItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 6.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            text = "${block.number}. ",
                            style = type.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = parseInlineMarkdown(
                                block.content,
                                emphasisColor = colors.onSurface,
                                codeColor = colors.primary,
                            ),
                            style = type.bodyMedium,
                            color = colors.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                is MarkdownBlock.CodeBlock -> {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = colors.surfaceContainerHighest,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            if (block.language.isNotBlank()) {
                                Text(
                                    text = block.language.uppercase(),
                                    style = type.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = colors.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                            Text(
                                text = block.code,
                                style = type.bodyMedium,
                                fontFamily = FontFamily.Monospace,
                                color = colors.primary,
                            )
                        }
                    }
                }

                is MarkdownBlock.Quote -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(24.dp)
                                .background(colors.primary, MaterialTheme.shapes.extraSmall),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = parseInlineMarkdown(
                                block.content,
                                emphasisColor = colors.onSurface,
                                codeColor = colors.primary,
                            ),
                            style = type.bodyMedium,
                            fontStyle = FontStyle.Italic,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }

                is MarkdownBlock.Divider -> {
                    HorizontalDivider()
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
    val cleanText = text.replace("\\n", "\n")
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = cleanText.lines()
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

fun parseInlineMarkdown(text: String, emphasisColor: Color, codeColor: Color): AnnotatedString {
    return buildAnnotatedString {
        var index = 0
        while (index < text.length) {
            when {
                text.startsWith("**", index) && text.indexOf("**", index + 2) != -1 -> {
                    val end = text.indexOf("**", index + 2)
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = emphasisColor))
                    append(text.substring(index + 2, end))
                    pop()
                    index = end + 2
                }
                text.startsWith("__", index) && text.indexOf("__", index + 2) != -1 -> {
                    val end = text.indexOf("__", index + 2)
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = emphasisColor))
                    append(text.substring(index + 2, end))
                    pop()
                    index = end + 2
                }
                text.startsWith("<u>", index) && text.indexOf("</u>", index + 3) != -1 -> {
                    val end = text.indexOf("</u>", index + 3)
                    pushStyle(SpanStyle(textDecoration = TextDecoration.Underline, color = emphasisColor))
                    append(text.substring(index + 3, end))
                    pop()
                    index = end + 4
                }
                text.startsWith("`", index) && text.indexOf("`", index + 1) != -1 -> {
                    val end = text.indexOf("`", index + 1)
                    pushStyle(SpanStyle(fontFamily = FontFamily.Monospace, color = codeColor))
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
