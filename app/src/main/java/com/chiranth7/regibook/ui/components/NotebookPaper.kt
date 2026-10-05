package com.chiranth7.regibook.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chiranth7.regibook.ui.theme.NotebookInkTertiary
import com.chiranth7.regibook.ui.theme.NotebookMarginLineBlackDark
import com.chiranth7.regibook.ui.theme.NotebookMarginLineBlackLight
import com.chiranth7.regibook.ui.theme.NotebookMarginLineDark
import com.chiranth7.regibook.ui.theme.NotebookMarginLineLight
import com.chiranth7.regibook.ui.theme.NotebookPaperDark
import com.chiranth7.regibook.ui.theme.NotebookPaperLight
import com.chiranth7.regibook.ui.theme.NotebookRuledLineDark
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
    useBlackMarginLine: Boolean = false,
    darkTheme: Boolean = com.chiranth7.regibook.ui.theme.LocalDarkTheme.current
) {
    val paperColor = if (darkTheme) NotebookPaperDark else Color(0xFFFFFFFF)
    val ruledColor = if (darkTheme) NotebookRuledLineDark else NotebookRuledLineLight
    val marginColor = if (useBlackMarginLine) {
        if (darkTheme) NotebookMarginLineBlackDark else NotebookMarginLineBlackLight
    } else {
        if (darkTheme) NotebookMarginLineDark else NotebookMarginLineLight
    }

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

/**
 * Notebook-style text input field.
 * Sits naturally on clean stationery with a label and underline.
 */
@Composable
fun NotebookInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else 4,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    testTag: String = ""
) {
    var isFocused by remember { mutableStateOf(false) }

    val inkColor = MaterialTheme.colorScheme.onBackground
    val secondaryInk = MaterialTheme.colorScheme.onSurfaceVariant
    val focusedUnderline = MaterialTheme.colorScheme.primary
    val unfocusedUnderline = MaterialTheme.colorScheme.outlineVariant

    val rulingColor = when {
        isError -> MaterialTheme.colorScheme.error
        isFocused -> focusedUnderline
        value.isNotEmpty() -> secondaryInk.copy(alpha = 0.6f)
        else -> unfocusedUnderline
    }

    val rulingThickness = when {
        isError || isFocused -> 1.8.dp
        value.isNotEmpty() -> 1.2.dp
        else -> 1.dp
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Field Label
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    letterSpacing = 0.5.sp
                ),
                color = if (isError) MaterialTheme.colorScheme.error else if (isFocused) focusedUnderline else secondaryInk,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            // Input Text Box with cursor & text styling
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isFocused = it.isFocused }
                    .testTag(testTag),
                singleLine = singleLine,
                maxLines = maxLines,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = inkColor,
                    fontFamily = FontFamily.Serif,
                    fontSize = 18.sp,
                    lineHeight = 26.sp
                ),
                cursorBrush = SolidColor(focusedUnderline),
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        if (value.isEmpty() && placeholder.isNotEmpty()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = FontFamily.Serif,
                                    fontSize = 17.sp
                                ),
                                color = NotebookInkTertiary
                            )
                        }
                        innerTextField()
                    }
                }
            )

            // Notebook Underline with animated feedback
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
            ) {
                drawLine(
                    color = rulingColor,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = rulingThickness.toPx()
                )
            }

            // Error Message (if any)
            if (isError && !errorMessage.isNullOrEmpty()) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
