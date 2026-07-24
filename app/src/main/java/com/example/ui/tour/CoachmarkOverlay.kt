package com.example.ui.tour

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ElegantOnPrimary
import com.example.ui.theme.ElegantPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

enum class CoachmarkStep(val title: String, val description: String) {
    LEARN_TAB(
        "Learn Tab",
        "This is where your active concept and quiz will appear whenever you unlock your device."
    ),
    HISTORY_TAB(
        "History & Progress",
        "Track your progress and review all previously generated concepts and quiz questions here."
    ),
    SETTINGS_TAB(
        "Credentials & Topics",
        "Add your Gemini API key and specify your own custom learning topics (subjects) here."
    )
}

class BubbleShape(
    private val arrowOffset: Float,
    private val arrowWidth: Float = 36f,
    private val arrowHeight: Float = 24f,
    private val arrowOnTop: Boolean = true
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            val radius = 40f
            if (arrowOnTop) {
                moveTo(radius, arrowHeight)
                lineTo(arrowOffset - arrowWidth / 2, arrowHeight)
                lineTo(arrowOffset, 0f)
                lineTo(arrowOffset + arrowWidth / 2, arrowHeight)
                lineTo(size.width - radius, arrowHeight)
                quadraticTo(size.width, arrowHeight, size.width, arrowHeight + radius)
                lineTo(size.width, size.height - radius)
                quadraticTo(size.width, size.height, size.width - radius, size.height)
                lineTo(radius, size.height)
                quadraticTo(0f, size.height, 0f, size.height - radius)
                lineTo(0f, arrowHeight + radius)
                quadraticTo(0f, arrowHeight, radius, arrowHeight)
            } else {
                moveTo(radius, 0f)
                lineTo(size.width - radius, 0f)
                quadraticTo(size.width, 0f, size.width, radius)
                lineTo(size.width, size.height - arrowHeight - radius)
                quadraticTo(size.width, size.height - arrowHeight, size.width - radius, size.height - arrowHeight)
                lineTo(arrowOffset + arrowWidth / 2, size.height - arrowHeight)
                lineTo(arrowOffset, size.height)
                lineTo(arrowOffset - arrowWidth / 2, size.height - arrowHeight)
                lineTo(radius, size.height - arrowHeight)
                quadraticTo(0f, size.height - arrowHeight, 0f, size.height - arrowHeight - radius)
                lineTo(0f, radius)
                quadraticTo(0f, 0f, radius, 0f)
            }
            close()
        }
        return Outline.Generic(path)
    }
}

@Composable
fun CoachmarkOverlay(
    activeStep: CoachmarkStep,
    targetCoordinates: LayoutCoordinates?,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    
    // Draw semi-transparent background with cut-out / spotlight effect
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clickable(enabled = false) {} // block click propagation
    ) {
        val screenWidth = constraints.maxWidth.toFloat()

        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            
            val path = Path().apply {
                addRect(Rect(0f, 0f, canvasWidth, canvasHeight))
            }
            
            val spotlightPath = Path()
            if (targetCoordinates != null && targetCoordinates.isAttached) {
                val position = targetCoordinates.positionInWindow()
                val targetSize = targetCoordinates.size
                
                // Add a slightly larger circle spotlight around target item
                val centerX = position.x + targetSize.width / 2
                val centerY = position.y + targetSize.height / 2
                val radius = maxOf(targetSize.width, targetSize.height) / 1.1f
                
                spotlightPath.addOval(
                    Rect(
                        left = centerX - radius,
                        top = centerY - radius,
                        right = centerX + radius,
                        bottom = centerY + radius
                    )
                )
            }
            
            clipPath(path) {
                if (targetCoordinates != null && targetCoordinates.isAttached) {
                    clipPath(spotlightPath, clipOp = ClipOp.Difference) {
                        drawRect(color = Color.Black.copy(alpha = 0.72f))
                    }
                } else {
                    drawRect(color = Color.Black.copy(alpha = 0.72f))
                }
            }
        }
        
        // Render target-relative bubble
        if (targetCoordinates != null && targetCoordinates.isAttached) {
            val position = targetCoordinates.positionInWindow()
            val targetSize = targetCoordinates.size
            
            // Layout position coordinates
            val targetCenterX = position.x + targetSize.width / 2
            val targetTopY = position.y
            val targetBottomY = position.y + targetSize.height
            
            // Determine arrow positioning
            val spaceAbove = targetTopY
            val arrowOnTop = spaceAbove < 350f
            
            // Determine bubble layout coords
            val bubbleWidth = 320.dp
            val bubbleWidthPx = with(density) { bubbleWidth.toPx() }
            
            // Align bubble horizontally centered over target but clamp inside screen bounds
            val paddingPx = with(density) { 16.dp.toPx() }
            val leftBoundary = paddingPx
            val rightBoundary = screenWidth - bubbleWidthPx - paddingPx
            
            val preferredLeft = targetCenterX - (bubbleWidthPx / 2)
            val bubbleLeft = preferredLeft.coerceIn(leftBoundary, rightBoundary)
            val localArrowOffset = targetCenterX - bubbleLeft
            
            val bubbleTopY = if (arrowOnTop) {
                targetBottomY + 12
            } else {
                targetTopY - with(density) { 150.dp.toPx() } - 36
            }
            
            Box(
                modifier = Modifier
                    .offset { IntOffset(bubbleLeft.toInt(), bubbleTopY.toInt()) }
                    .width(bubbleWidth)
                    .background(
                        color = DarkSurface,
                        shape = BubbleShape(
                            arrowOffset = localArrowOffset,
                            arrowOnTop = arrowOnTop
                        )
                    )
                    .padding(horizontal = 20.dp)
                    .padding(top = if (arrowOnTop) 30.dp else 16.dp, bottom = if (arrowOnTop) 16.dp else 30.dp)
            ) {
                Column {
                    Text(
                        text = activeStep.title,
                        color = ElegantPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = activeStep.description,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onSkip,
                            colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
                        ) {
                            Text("Skip Tour", fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onNext,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElegantPrimary,
                                contentColor = ElegantOnPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (activeStep == CoachmarkStep.entries.last()) "Finish" else "Next",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
