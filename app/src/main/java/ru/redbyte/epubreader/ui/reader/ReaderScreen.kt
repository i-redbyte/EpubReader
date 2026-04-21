package ru.redbyte.epubreader.ui.reader

import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.abs
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.redbyte.epubreader.R

val LocalReaderViewModelFactory = staticCompositionLocalOf<ReaderViewModelFactory> {
    throw IllegalStateException()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    viewModel: ReaderViewModel = viewModel(factory = LocalReaderViewModelFactory.current),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var tocOpen by remember { mutableStateOf(false) }
    var swipeNavigationEnabled by remember { mutableStateOf(true) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.snackbarMessages.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            when (val s = state) {
                is ReaderUiState.Ready -> {
                    val primary = MaterialTheme.colorScheme.primary
                    val onPrimary = MaterialTheme.colorScheme.onPrimary
                    val cdSwipe = stringResource(R.string.cd_reader_swipe_navigation)
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = primary,
                            titleContentColor = onPrimary,
                            scrolledContainerColor = primary,
                        ),
                        title = {
                            Text(
                                text = s.book.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = onPrimary,
                                maxLines = 2,
                            )
                        },
                        actions = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .semantics { contentDescription = cdSwipe },
                            ) {
                                Text(
                                    text = stringResource(R.string.reader_swipe_navigation),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = onPrimary,
                                    modifier = Modifier.padding(end = 2.dp),
                                )
                                Checkbox(
                                    checked = swipeNavigationEnabled,
                                    onCheckedChange = { swipeNavigationEnabled = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = onPrimary,
                                        uncheckedColor = onPrimary.copy(alpha = 0.65f),
                                        checkmarkColor = primary,
                                    ),
                                )
                            }
                        },
                    )
                }
                else -> {}
            }
        },
        bottomBar = {
            when (val s = state) {
                is ReaderUiState.Ready -> {
                    val cdPrev = stringResource(R.string.cd_prev_chapter)
                    val cdNext = stringResource(R.string.cd_next_chapter)
                    val primary = MaterialTheme.colorScheme.primary
                    val onPrimary = MaterialTheme.colorScheme.onPrimary
                    Surface(
                        shadowElevation = 6.dp,
                        color = primary,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(
                                onClick = { tocOpen = true },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = onPrimary,
                                ),
                            ) {
                                Text(
                                    text = stringResource(R.string.action_toc),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = onPrimary,
                                )
                            }
                            Button(
                                onClick = { viewModel.selectSpine(s.spineIndex - 1) },
                                enabled = s.spineIndex > 0,
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 56.dp, minHeight = 52.dp)
                                    .semantics { contentDescription = cdPrev },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = onPrimary.copy(alpha = 0.22f),
                                    contentColor = onPrimary,
                                    disabledContainerColor = onPrimary.copy(alpha = 0.12f),
                                    disabledContentColor = onPrimary.copy(alpha = 0.38f),
                                ),
                            ) {
                                Text(
                                    text = "<",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Unspecified,
                                )
                            }
                            Button(
                                onClick = { viewModel.selectSpine(s.spineIndex + 1) },
                                enabled = s.spineIndex < s.book.spineFiles.lastIndex,
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 56.dp, minHeight = 52.dp)
                                    .semantics { contentDescription = cdNext },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = onPrimary.copy(alpha = 0.22f),
                                    contentColor = onPrimary,
                                    disabledContainerColor = onPrimary.copy(alpha = 0.12f),
                                    disabledContentColor = onPrimary.copy(alpha = 0.38f),
                                ),
                            ) {
                                Text(
                                    text = ">",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Unspecified,
                                )
                            }
                        }
                    }
                }
                else -> {}
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (val s = state) {
                ReaderUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                is ReaderUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = stringResource(R.string.error_generic, s.message),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Button(onClick = { viewModel.loadBook() }) {
                            Text(stringResource(R.string.action_retry))
                        }
                    }
                }
                is ReaderUiState.Ready -> {
                    ReaderWithSwipe(
                        viewModel = viewModel,
                        ready = s,
                        swipeEnabled = swipeNavigationEnabled,
                    )
                }
            }
        }
    }

    if (tocOpen && state is ReaderUiState.Ready) {
        val ready = state as ReaderUiState.Ready
        ModalBottomSheet(
            onDismissRequest = { tocOpen = false },
            sheetState = sheetState,
        ) {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 32.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(ready.toc, key = { e -> e.title + e.href + e.depth }) { entry ->
                    TextButton(
                        onClick = {
                            viewModel.selectTocEntry(entry)
                            scope.launch {
                                sheetState.hide()
                                tocOpen = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "${"  ".repeat(entry.depth * 2)}${entry.title}",
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReaderWithSwipe(
    viewModel: ReaderViewModel,
    ready: ReaderUiState.Ready,
    swipeEnabled: Boolean,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val zoneWidth = (maxWidth * 0.28f).coerceAtLeast(100.dp)
        ReaderWebView(
            viewModel = viewModel,
            ready = ready,
        )
        if (swipeEnabled) {
            SwipeEdgeZone(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .widthIn(min = 100.dp)
                    .width(zoneWidth)
                    .fillMaxHeight(),
                onSwipe = {
                    val st = viewModel.uiState.value
                    if (st is ReaderUiState.Ready && st.spineIndex > 0) {
                        viewModel.selectSpine(st.spineIndex - 1)
                    }
                },
                positiveDragMeansAction = true,
            )
            SwipeEdgeZone(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .widthIn(min = 100.dp)
                    .width(zoneWidth)
                    .fillMaxHeight(),
                onSwipe = {
                    val st = viewModel.uiState.value
                    if (st is ReaderUiState.Ready && st.spineIndex < st.book.spineFiles.lastIndex) {
                        viewModel.selectSpine(st.spineIndex + 1)
                    }
                },
                positiveDragMeansAction = false,
            )
        }
    }
}

@Composable
private fun SwipeEdgeZone(
    modifier: Modifier,
    onSwipe: () -> Unit,
    positiveDragMeansAction: Boolean,
) {
    var totalX by remember { mutableFloatStateOf(0f) }
    var totalY by remember { mutableFloatStateOf(0f) }
    Box(
        modifier = modifier.pointerInput(positiveDragMeansAction) {
            detectDragGestures(
                onDragStart = {
                    totalX = 0f
                    totalY = 0f
                },
                onDrag = { _, dragAmount ->
                    totalX += dragAmount.x
                    totalY += dragAmount.y
                },
                onDragEnd = {
                    if (abs(totalX) > abs(totalY) && abs(totalX) >= 56f) {
                        val ok = if (positiveDragMeansAction) totalX > 0 else totalX < 0
                        if (ok) onSwipe()
                    }
                },
            )
        },
    )
}

@Composable
private fun ReaderWebView(
    viewModel: ReaderViewModel,
    ready: ReaderUiState.Ready,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            configureForEpubReading()
            webChromeClient = WebChromeClient()
        }
    }

    DisposableEffect(lifecycleOwner, webView) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                viewModel.saveScrollFromWebView(webView)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(webView) {
        onDispose {
            webView.destroy()
        }
    }

    LaunchedEffect(ready.spineIndex, ready.fileUrl) {
        webView.loadUrl(ready.fileUrl)
    }

    val client = remember(viewModel) {
        object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?,
            ): Boolean {
                return false
            }

            override fun onPageFinished(view: WebView, url: String?) {
                super.onPageFinished(view, url)
                view.injectEpubLayoutFixCss()
                val current = viewModel.uiState.value
                if (current !is ReaderUiState.Ready) return
                val pending = current.pendingScrollRatio ?: return
                val js = ReaderViewModel.scrollToRatioJs(pending)
                val delays = longArrayOf(0L, 50L, 200L, 500L, 1000L)
                val finishedUrl = url ?: view.url
                for (d in delays) {
                    view.postDelayed({ view.evaluateJavascript(js, null) }, d)
                }
                view.postDelayed({
                    val st = viewModel.uiState.value
                    if (st is ReaderUiState.Ready &&
                        st.pendingScrollRatio != null &&
                        sameDocumentPath(st.fileUrl, finishedUrl ?: "")
                    ) {
                        viewModel.onScrollRestoreApplied()
                    }
                }, 1150L)
            }
        }
    }

    AndroidView(
        factory = { _ -> webView },
        update = { wv ->
            wv.webViewClient = client
        },
        modifier = Modifier.fillMaxSize(),
    )
}

private fun sameDocumentPath(a: String?, b: String): Boolean {
    if (a == null) return false
    val pathA = Uri.parse(a.substringBefore("#")).path ?: return false
    val pathB = Uri.parse(b.substringBefore("#")).path ?: return false
    return pathA == pathB
}
