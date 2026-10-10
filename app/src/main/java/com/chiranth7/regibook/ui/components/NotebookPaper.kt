package com.chiranth7.regibook.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chiranth7.regibook.ui.theme.NotebookMarginLineBlackLight
import com.chiranth7.regibook.ui.theme.NotebookMarginLineLight
import com.chiranth7.regibook.ui.theme.NotebookRuledLineLight

// Standard Notebook Line Dimensions
val NotebookRowHeight: Dp = 56.dp
val NotebookMarginLeft: Dp = 56.dp

/**
 * A canvas that paints a realistic ruled notebook page:
 * - Warm ivory/parchment background in light theme
 * - Subtle red (or black ink) vertical margin line on the left
 * - Soft notebook rulings across the whole page
 */
@Composable
fun NotebookPaperBackground(
    modifier: Modifier = Modifier,
    lineSpacing: Dp = NotebookRowHeight,
    marginLineOffset: Dp = NotebookMarginLeft,
    topStartOffset: Dp = 0.dp,
    showMarginLine: Boolean = true,
    showRuledLines: Boolean = true,
    useBlackMarginLine: Boolean = false
) {
    val paperColor = Color(0xFFFFFFFF)
    val ruledColor = NotebookRuledLineLight
    val marginColor = if (useBlackMarginLine) NotebookMarginLineBlackLight else NotebookMarginLineLight

    Canvas(modifier = modifier.fillMaxSize()) {
        // 1. Draw warm paper fill
        drawRect(color = paperColor)

        val strokeWidthPx = 1.25.dp.toPx()
        val spacingPx = lineSpacing.toPx()
        val marginXPx = marginLineOffset.toPx()
        val topOffsetPx = topStartOffset.toPx()

        // 2. Draw horizontal notebook rulings only if showRuledLines is true
        if (showRuledLines && spacingPx > 0) {
            var y = topOffsetPx + spacingPx
            while (y < size.height) {
                drawLine(
                    color = ruledColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = strokeWidthPx
                )
                y += spacingPx
            }
        }

        // 3. Draw vertical margin line
        if (showMarginLine) {
            drawLine(
                color = marginColor,
                start = Offset(marginXPx, 0f),
                end = Offset(marginXPx, size.height),
                strokeWidth = 1.5.dp.toPx()
            )
        }
    }
}
