package com.chiranth7.regibook.features.pigmi.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun NumericFastScrollSidebar(
    maxSrNo: Int,
    currentSrNo: Int?,
    onScrollToSrNo: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (maxSrNo <= 0) return

    val step = calculateStep(maxSrNo)
    val labels = remember(maxSrNo, step) {
        val list = mutableListOf<Int>()
        list.add(1)
        var curr = step
        while (curr < maxSrNo) {
            list.add(curr)
            curr += step
        }
        if (list.last() != maxSrNo) {
            list.add(maxSrNo)
        }
        list
    }

    var isDragging by remember { mutableStateOf(false) }
    var activeNumber by remember { mutableStateOf<Int?>(null) }
    var columnHeight by remember { mutableFloatStateOf(1f) }
    val animatedTouchY = remember { androidx.compose.animation.core.Animatable(0f) }
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    fun handlePosition(y: Float) {
        if (columnHeight <= 0f) return
        val clampedY = y.coerceIn(0f, columnHeight)
        coroutineScope.launch {
            animatedTouchY.animateTo(
                targetValue = clampedY,
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                )
            )
        }
        val fraction = (clampedY / columnHeight).coerceIn(0f, 1f)
        val targetNumber = (1 + fraction * (maxSrNo - 1)).roundToInt().coerceIn(1, maxSrNo)
        if (activeNumber != targetNumber) {
            activeNumber = targetNumber
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
            onScrollToSrNo(targetNumber)
        }
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = 12.dp, horizontal = 2.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        // Floating preview bubble showing plain selected number (OriginOS Spring Wave)
        AnimatedVisibility(
            visible = isDragging && activeNumber != null,
            enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(120)) +
                    androidx.compose.animation.scaleIn(
                        initialScale = 0.65f,
                        animationSpec = androidx.compose.animation.core.spring(
                            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                        )
                    ),
            exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(150)) +
                    androidx.compose.animation.scaleOut(
                        targetScale = 0.7f,
                        animationSpec = androidx.compose.animation.core.tween(150)
                    ),
            modifier = Modifier
                .padding(end = 48.dp)
                .graphicsLayer {
                    // Smooth OriginOS spring tracking vertically alongside finger
                    val bubbleOffset = animatedTouchY.value - (columnHeight / 2f)
                    translationY = bubbleOffset.coerceIn(-columnHeight / 2.2f, columnHeight / 2.2f)
                }
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primary,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${activeNumber ?: 1}",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 20.sp
                        )
                    )
                }
            }
        }

        // The vertical index strip
        Column(
            modifier = Modifier
                .width(36.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(vertical = 8.dp)
                .onGloballyPositioned { coordinates ->
                    columnHeight = coordinates.size.height.toFloat()
                }
                .pointerInput(labels, maxSrNo) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        isDragging = true
                        handlePosition(down.position.y)

                        var pointerId = down.id
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId }
                                ?: event.changes.firstOrNull()

                            if (change == null || !change.pressed) {
                                break
                            }

                            pointerId = change.id
                            change.consume()
                            handlePosition(change.position.y)
                        }

                        isDragging = false
                        activeNumber = null
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            labels.forEach { num ->
                val isSelected = currentSrNo != null &&
                        (currentSrNo >= num && (labels.getOrNull(labels.indexOf(num) + 1)?.let { currentSrNo < it } ?: true))

                Text(
                    text = if (num >= 1000) "${num / 1000}k" else num.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                        fontSize = if (labels.size > 10) 9.sp else 10.sp
                    ),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 2.dp)
                )
            }
        }
    }
}

private fun calculateStep(max: Int): Int {
    if (max <= 10) return 1
    if (max <= 50) return 5
    if (max <= 100) return 10
    if (max <= 250) return 25
    if (max <= 500) return 50
    if (max <= 1200) return 100
    if (max <= 2500) return 250
    if (max <= 5000) return 500
    return 1000
}
