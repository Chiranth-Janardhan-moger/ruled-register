package com.chiranth7.regibook.features.pigmi.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chiranth7.regibook.R
import com.chiranth7.regibook.features.drawer.RegisterDrawerContent
import com.chiranth7.regibook.features.pigmi.data.PigmiAccount
import com.chiranth7.regibook.features.pigmi.viewmodel.PigmiViewModel
import com.chiranth7.regibook.ui.components.NotebookMarginLeft
import com.chiranth7.regibook.ui.components.NotebookPaperBackground
import com.chiranth7.regibook.ui.components.NotebookRowHeight
import com.chiranth7.regibook.util.KannadaNameHelper
import com.chiranth7.regibook.util.RegisterType
import com.chiranth7.regibook.util.SettingsManager
import kotlinx.coroutines.launch

@Composable
fun PigmiRegisterScreen(
    viewModel: PigmiViewModel,
    settingsManager: SettingsManager,
    onSwitchRegister: (RegisterType) -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val currentLang by settingsManager.currentLanguage.collectAsStateWithLifecycle()
    var isSearchActive by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            kotlinx.coroutines.delay(100)
            runCatching { focusRequester.requestFocus() }
        }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        com.chiranth7.regibook.data.firebase.FirestoreSyncManager.initialSeedIfEmpty(context)
        com.chiranth7.regibook.data.firebase.FirestoreSyncManager.syncPigmiFromCloud(context)
    }

    val inkColor = MaterialTheme.colorScheme.onBackground
    val secondaryInk = MaterialTheme.colorScheme.onSurfaceVariant

    BackHandler(enabled = drawerState.isOpen) {
        coroutineScope.launch { drawerState.close() }
    }

    BackHandler(enabled = isSearchActive) {
        isSearchActive = false
        viewModel.updateSearchQuery("")
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            RegisterDrawerContent(
                activeRegister = RegisterType.PIGMI,
                onSelectRegister = { selectedType ->
                    coroutineScope.launch { drawerState.close() }
                    if (selectedType != RegisterType.PIGMI) {
                        onSwitchRegister(selectedType)
                    }
                },
                onNavigateToSettings = {
                    coroutineScope.launch { drawerState.close() }
                    onNavigateToSettings()
                }
            )
        }
    ) {
        val fontScale = LocalDensity.current.fontScale
        val maxSrNo = remember(accounts) {
            accounts.maxOfOrNull { it.srNo } ?: 1
        }
        val maxDigits = maxSrNo.toString().length
        val marginWidth = if (maxDigits >= 5) 58.dp else 54.dp

        Box(
            modifier = modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            NotebookPaperBackground(
                lineSpacing = NotebookRowHeight,
                marginLineOffset = marginWidth,
                topStartOffset = 56.dp,
                showMarginLine = true,
                showRuledLines = false
            )

            Column(modifier = Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = isSearchActive,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(200)) togetherWith
                            fadeOut(animationSpec = tween(150))
                    },
                    label = "PigmiTopBarSearchTransition"
                ) { searchActive ->
                    if (searchActive) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    isSearchActive = false
                                    viewModel.updateSearchQuery("")
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("back_search_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.back),
                                    tint = inkColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Box(modifier = Modifier.weight(1f)) {
                                androidx.compose.runtime.CompositionLocalProvider(
                                    LocalDensity provides androidx.compose.ui.unit.Density(
                                        density = LocalDensity.current.density,
                                        fontScale = 1.0f
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(42.dp)
                                            .padding(end = 4.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                shape = RoundedCornerShape(21.dp)
                                            )
                                            .padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = null,
                                            tint = secondaryInk,
                                            modifier = Modifier.size(18.dp)
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        BasicTextField(
                                            value = searchQuery,
                                            onValueChange = { viewModel.updateSearchQuery(it) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .focusRequester(focusRequester)
                                                .testTag("search_input"),
                                            singleLine = true,
                                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                                color = inkColor,
                                                fontSize = 15.sp
                                            ),
                                            cursorBrush = SolidColor(inkColor),
                                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                            decorationBox = { innerTextField ->
                                                Box(contentAlignment = Alignment.CenterStart) {
                                                    if (searchQuery.isEmpty()) {
                                                        Text(
                                                            text = stringResource(R.string.search_hint),
                                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                                color = secondaryInk.copy(alpha = 0.5f),
                                                                fontSize = 14.sp
                                                            )
                                                        )
                                                    }
                                                    innerTextField()
                                                }
                                            }
                                        )

                                        IconButton(
                                            onClick = {
                                                if (searchQuery.isNotEmpty()) {
                                                    viewModel.updateSearchQuery("")
                                                } else {
                                                    isSearchActive = false
                                                }
                                            },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .testTag("close_search_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = stringResource(R.string.cancel),
                                                tint = secondaryInk,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { coroutineScope.launch { drawerState.open() } },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("menu_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = stringResource(R.string.menu),
                                    tint = inkColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            IconButton(
                                onClick = { isSearchActive = true },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("search_icon_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = stringResource(R.string.search),
                                    tint = inkColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                // Ruled records area with Fast Scroll numeric sidebar
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    if (accounts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(NotebookRowHeight),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = if (searchQuery.isNotEmpty()) {
                                    stringResource(R.string.search) + ": " + stringResource(R.string.empty_register)
                                } else {
                                    stringResource(R.string.empty_register)
                                },
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Serif,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    fontSize = 15.sp
                                ),
                                color = secondaryInk,
                                modifier = Modifier
                                    .padding(start = marginWidth + 12.dp, end = 16.dp)
                                    .testTag("empty_register_text")
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(end = if (isSearchActive) 8.dp else 40.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("pigmi_accounts_list")
                        ) {
                            items(
                                items = accounts,
                                key = { it.id }
                            ) { account ->
                                PigmiRow(
                                    account = account,
                                    currentLanguage = currentLang,
                                    marginWidth = marginWidth
                                )
                            }
                        }

                        if (!isSearchActive) {
                            // Right-side numeric fast scroll sidebar (e.g. 1..100..1100)
                            val maxSrNo = remember(accounts) {
                                accounts.maxOfOrNull { it.srNo } ?: accounts.size
                            }
                            val currentSrNo = remember(accounts, listState.firstVisibleItemIndex) {
                                accounts.getOrNull(listState.firstVisibleItemIndex)?.srNo
                            }

                            NumericFastScrollSidebar(
                                maxSrNo = maxSrNo,
                                currentSrNo = currentSrNo,
                                onScrollToSrNo = { targetSrNo ->
                                    val targetIdx = accounts.indexOfFirst { it.srNo >= targetSrNo }
                                        .takeIf { it >= 0 } ?: (accounts.size - 1)
                                    if (targetIdx >= 0) {
                                        coroutineScope.launch {
                                            listState.scrollToItem(targetIdx)
                                        }
                                    }
                                },
                                modifier = Modifier.align(Alignment.CenterEnd)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PigmiRow(
    account: PigmiAccount,
    currentLanguage: String = SettingsManager.LANG_ENGLISH,
    marginWidth: Dp = NotebookMarginLeft,
    modifier: Modifier = Modifier
) {
    val inkColor = MaterialTheme.colorScheme.onBackground
    val secondaryInk = MaterialTheme.colorScheme.onSurfaceVariant
    val rulingColor = MaterialTheme.colorScheme.outline
    val fontScale = LocalDensity.current.fontScale

    val digitCount = account.srNo.toString().length
    val srNoFontSize = when {
        digitCount >= 5 -> if (fontScale > 1.2f) 12.sp else 13.sp
        digitCount >= 4 -> if (fontScale > 1.2f) 13.5.sp else 14.5.sp
        else -> 15.sp
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(NotebookRowHeight)
            .testTag("pigmi_row_${account.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Margin column: Serial Number
            Box(
                modifier = Modifier
                    .width(marginWidth)
                    .padding(end = 8.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = account.srNo.toString(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Medium,
                        fontSize = srNoFontSize
                    ),
                    color = secondaryInk,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Name
            Text(
                text = account.getDisplayName(currentLanguage),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Normal,
                    fontSize = 17.sp
                ),
                color = inkColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            drawLine(
                color = rulingColor,
                start = Offset(0f, size.height),
                end = Offset(size.width, size.height),
                strokeWidth = 1.25.dp.toPx()
            )
        }
    }
}
