package com.agon.app.ui.screens.browser

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import com.agon.app.data.DownloadMode
import com.agon.app.data.SubtitleMethod
import com.agon.app.ui.components.StackedCirclesMenu
import com.agon.app.ui.screens.browser.components.*
import com.agon.app.viewmodel.BrowserViewModel
import kotlin.math.roundToInt

// 1. إنشاء واجهة الجسر لاستقبال الروابط من الجافاسكريبت
class VideoSnifferInterface(private val onMediaFound: (String) -> Unit) {
    @JavascriptInterface
    fun onMediaFound(url: String) {
        onMediaFound(url)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel,
    navController: NavHostController? = null
) {
    val state by viewModel.state.collectAsState()
    val pendingNavigation by viewModel.pendingNavigation.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var webView by remember { mutableStateOf<WebView?>(null) }

    // متغيرات لحفظ موقع زر التحميل العائم وتفادي خروجه من الشاشة
    var fabOffsetX by remember { mutableFloatStateOf(0f) }
    var fabOffsetY by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(state.successMessage) {
        state.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSuccess()
        }
    }

    LaunchedEffect(pendingNavigation) {
        pendingNavigation?.let { url ->
            webView?.loadUrl(url)
            viewModel.clearPendingNavigation()
        }
    }

    BackHandler(enabled = state.canGoBack) {
        webView?.goBack()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                BrowserTopBar(
                    state = state,
                    onNavigate = { input -> viewModel.navigateToInput(input) },
                    onBack = { webView?.goBack() },
                    onForward = { webView?.goForward() },
                    onReload = { webView?.reload() },
                    onStop = { webView?.stopLoading() },
                    onTabsClick = { viewModel.toggleTabsList() },
                    onMenuClick = { viewModel.toggleMainMenu() }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // WebView (الآن أصبح صائد فيديوهات متقدم)
                BrowserWebView(
                    currentUrl = state.activeTab?.url ?: "https://www.google.com",
                    onWebViewCreated = { webView = it },
                    onPageStarted = { url -> viewModel.onPageStarted(url) },
                    onPageFinished = { url, title, favicon ->
                        viewModel.onPageFinished(url, title, favicon)
                    },
                    onProgressChanged = { progress ->
                        viewModel.onPageProgressChanged(progress)
                    },
                    onNavigationStateChange = { canGoBack, canGoForward ->
                        viewModel.updateNavigationState(canGoBack, canGoForward)
                    },
                    onLinkLongPressed = { link ->
                        viewModel.onLinkLongPressed(link)
                    },
                    // إرسال الروابط المكتشفة إلى ViewModel
                    onMediaIntercepted = { mediaUrl ->
                        viewModel.onMediaIntercepted(mediaUrl)
                    }
                )

                // ==========================================
                // زر التحميل العائم (تصميم عصري ومستقر وقابل للسحب)
                // ==========================================
                AnimatedVisibility(
                    visible = true,
                    enter = scaleIn(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
                    exit = scaleOut(animationSpec = tween(300)) + fadeOut(animationSpec = tween(300)),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 32.dp, end = 24.dp) // مسافات آمنة من الحواف
                        .offset { IntOffset(fabOffsetX.roundToInt(), fabOffsetY.roundToInt()) }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    fabOffsetX += dragAmount.x
                                    fabOffsetY += dragAmount.y
                                },
                                onDragEnd = {
                                    // إعادة الزر لمكانه إذا تم سحبه بعيداً جداً خارج حدود الشاشة المعقولة
                                    if (fabOffsetX > 100f || fabOffsetX < -800f) fabOffsetX = 0f
                                    if (fabOffsetY > 100f || fabOffsetY < -1500f) fabOffsetY = 0f
                                }
                            )
                        }
                ) {
                    FloatingActionButton(
                        onClick = {
                            val currentUrl = state.activeTab?.url
                            if (!currentUrl.isNullOrBlank()) {
                                viewModel.analyzeCurrentPage(currentUrl)
                            }
                        },
                        modifier = Modifier
                            .size(64.dp)
                            .shadow(
                                elevation = 16.dp,
                                shape = RoundedCornerShape(20.dp),
                                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                spotColor = MaterialTheme.colorScheme.primary
                            ),
                        containerColor = Color.Transparent, // لجعل الخلفية تعتمد على الـ Box الداخلي
                        elevation = FloatingActionButtonDefaults.elevation(0.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        // خلفية متدرجة عصرية
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.tertiary
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.isAnalyzing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    color = Color.White,
                                    strokeWidth = 3.dp
                                )
                            } else {
                                Icon(
                                    Icons.Filled.Download,
                                    contentDescription = "تحميل",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }

                if (state.showMainMenu) {
                    StackedCirclesMenu(
                        expanded = true,
                        onDismiss = { viewModel.dismissMainMenu() },
                        onNavigate = { route ->
                            viewModel.dismissMainMenu()
                            navController?.navigate(route)
                        }
                    )
                }
            }
        }

        // النوافذ المنبثقة
        if (state.showTabsList) {
            TabsListSheet(
                tabs = state.tabs,
                activeTabIndex = state.activeTabIndex,
                onTabClick = { index -> viewModel.activateTab(index) },
                onCloseTab = { tabId -> viewModel.closeTab(tabId) },
                onNewTab = { viewModel.createNewTab() },
                onCloseOtherTabs = { viewModel.closeOtherTabs() },
                onDismiss = { viewModel.dismissTabsList() }
            )
        }

        if (state.showVideoSheet && state.videoInfo != null) {
            VideoDetectedSheet(
                state = state,
                onSelectQuality = { quality -> viewModel.selectQuality(quality) },
                onDownloadVideo = {
                    viewModel.startSpecificDownload(DownloadMode.VIDEO_ONLY, SubtitleMethod.NONE)
                },
                onDownloadSubtitle = { method ->
                    viewModel.startSpecificDownload(DownloadMode.SUBTITLE_ONLY, method)
                },
                onSearchSubtitle = { viewModel.performSubtitleSearch() },
                onDismiss = { viewModel.hideVideoSheet() }
            )
        }

        if (state.showLinkMenu && state.longPressedLink != null) {
            LongPressLinkMenu(
                link = state.longPressedLink!!,
                onOpenInBackground = {
                    viewModel.openLinkInBackground(state.longPressedLink!!)
                },
                onCopyLink = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("link", state.longPressedLink))
                    viewModel.dismissLinkMenu()
                },
                onShareLink = {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, state.longPressedLink)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "مشاركة الرابط"))
                    viewModel.dismissLinkMenu()
                },
                onDismiss = { viewModel.dismissLinkMenu() }
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun BrowserWebView(
    currentUrl: String,
    onWebViewCreated: (WebView) -> Unit,
    onPageStarted: (String) -> Unit,
    onPageFinished: (String, String, String?) -> Unit,
    onProgressChanged: (Int) -> Unit,
    onNavigationStateChange: (Boolean, Boolean) -> Unit,
    onLinkLongPressed: (String) -> Unit,
    onMediaIntercepted: (String) -> Unit
) {
    var lastLoadedUrl by remember { mutableStateOf("") }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    layoutAlgorithm = android.webkit.WebSettings.LayoutAlgorithm.NORMAL
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                    mediaPlaybackRequiresUserGesture = false
                    allowContentAccess = true
                    allowFileAccess = true
                    userAgentString = userAgentString.replace("; wv", "")
                    defaultTextEncodingName = "UTF-8"
                    setSupportMultipleWindows(false)
                }

                // ربط الجسر بالمتصفح لإرسال الروابط إلى كوتلن
                addJavascriptInterface(VideoSnifferInterface { url ->
                    onMediaIntercepted(url)
                }, "AndroidSniffer")

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        return false
                    }

                    // اصطياد طلبات الشبكة (ملفات الميديا)
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        val url = request?.url?.toString()?.lowercase() ?: ""
                        val method = request?.method ?: ""

                        if (method.equals("GET", ignoreCase = true)) {
                            if (url.contains(".mp4") || 
                                url.contains(".m3u8") || 
                                url.contains(".ts") || 
                                url.contains(".webm") ||
                                url.contains(".mp3")
                            ) {
                                onMediaIntercepted(request?.url?.toString() ?: "")
                            }
                        }
                        return super.shouldInterceptRequest(view, request)
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        url?.let { onPageStarted(it) }
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        if (url != null) {
                            val title = view?.title ?: ""
                            val faviconUrl = "https://www.google.com/s2/favicons?domain=${try { java.net.URL(url).host } catch (e: Exception) { "" }}&sz=64"
                            
                            val hideYouTubeUI = """
                                javascript:(function() {
                                    var elements = document.querySelectorAll('#app-bar-guide-menu, .ytd-app, ytd-mini-guide-renderer, #guide-button, ytd-mini-guide-entry-renderer');
                                    elements.forEach(function(el) { el.style.display = 'none'; });
                                    var bottomNavs = document.querySelectorAll('[class*="bottom-nav"], [class*="BottomNavigation"], ytd-app > #content.ytd-app');
                                    bottomNavs.forEach(function(el) { if (el && el.style) el.style.paddingBottom = '0px'; });
                                })();
                            """.trimIndent()
                            view?.evaluateJavascript(hideYouTubeUI, null)

                            // حقن سكربت صائد الفيديوهات الذي يراقب الصفحة باستمرار
                            val jsVideoSniffer = """
                                javascript:(function() {
                                    function sniffVideos() {
                                        var videos = document.getElementsByTagName('video');
                                        for (var i = 0; i < videos.length; i++) {
                                            if (videos[i].src && videos[i].src.startsWith('http')) {
                                                AndroidSniffer.onMediaFound(videos[i].src);
                                            }
                                            var sources = videos[i].getElementsByTagName('source');
                                            for (var j = 0; j < sources.length; j++) {
                                                if (sources[j].src && sources[j].src.startsWith('http')) {
                                                    AndroidSniffer.onMediaFound(sources[j].src);
                                                }
                                            }
                                        }
                                    }
                                    
                                    sniffVideos();
                                    
                                    var observer = new MutationObserver(function(mutations) {
                                        sniffVideos();
                                    });
                                    observer.observe(document.body, { childList: true, subtree: true });
                                })();
                            """.trimIndent()
                            view?.evaluateJavascript(jsVideoSniffer, null)
                            
                            onPageFinished(url, title, faviconUrl)
                            onNavigationStateChange(view?.canGoBack() ?: false, view?.canGoForward() ?: false)
                        }
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                        onProgressChanged(newProgress)
                    }
                }

                setOnLongClickListener {
                    val result = hitTestResult
                    if (result.type == android.webkit.WebView.HitTestResult.SRC_ANCHOR_TYPE ||
                        result.type == android.webkit.WebView.HitTestResult.IMAGE_TYPE) {
                        result.extra?.let { link ->
                            onLinkLongPressed(link)
                            true
                        } ?: false
                    } else {
                        false
                    }
                }

                onWebViewCreated(this)
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { webView ->
            if (currentUrl != lastLoadedUrl && currentUrl.isNotBlank()) {
                val webViewCurrentUrl = webView.url
                if (webViewCurrentUrl != currentUrl) {
                    webView.loadUrl(currentUrl)
                    lastLoadedUrl = currentUrl
                }
            }
        }
    )
}
