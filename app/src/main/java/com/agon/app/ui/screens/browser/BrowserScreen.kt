package com.agon.app.ui.screens.browser

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import com.agon.app.data.DownloadMode
import com.agon.app.data.SubtitleMethod
import com.agon.app.ui.components.StackedCirclesMenu
import com.agon.app.ui.screens.browser.components.*
import com.agon.app.viewmodel.BrowserViewModel

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

    // معالجة رسائل الخطأ والنجاح
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

    // الاستماع لطلبات التنقل من شريط العنوان
    LaunchedEffect(pendingNavigation) {
        pendingNavigation?.let { url ->
            webView?.loadUrl(url)
            viewModel.clearPendingNavigation()
        }
    }

    // ربط زر الرجوع في النظام بالرجوع داخل WebView
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
                // WebView الفعلي
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
                    }
                )

                // ==========================================
                // زر التحميل العائم (FAB) - أنيق، عصري، ودائم الظهور
                // ==========================================
                AnimatedVisibility(
                    visible = true,
                    enter = scaleIn(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
                    exit = scaleOut(animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
                ) {
                    FloatingActionButton(
                        onClick = {
                            val currentUrl = state.activeTab?.url
                            if (!currentUrl.isNullOrBlank()) {
                                // تحليل الرابط فور الضغط لاستخراج الفيديو والترجمة
                                viewModel.analyzeCurrentPage(currentUrl)
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(20.dp)
                            .size(64.dp)
                            .shadow(
                                elevation = 12.dp,
                                shape = CircleShape,
                                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            ),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = CircleShape
                    ) {
                        if (state.isAnalyzing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 3.dp
                            )
                        } else {
                            Icon(
                                Icons.Filled.Download,
                                contentDescription = "تحليل وتحميل",
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }

                // ==========================================
                // القائمة العصرية (الدوائر المتسلسلة)
                // ==========================================
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

        // ==========================================
        // النوافذ المنبثقة (Bottom Sheets & Dialogs)
        // ==========================================

        // قائمة التبويبات
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

        // صفحة بيانات الفيديو (تظهر بعد التحليل الناجح)
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

        // قائمة الضغط المطول على الروابط
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
    onLinkLongPressed: (String) -> Unit
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
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                    mediaPlaybackRequiresUserGesture = false
                    allowContentAccess = true
                    allowFileAccess = true
                    layoutAlgorithm = android.webkit.WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
                    defaultTextEncodingName = "UTF-8"
                    setSupportMultipleWindows(false)
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        return false
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
                            
                            // إخفاء عناصر YouTube السفلية عبر JavaScript
                            val hideYouTubeUI = """
                                javascript:(function() {
                                    var elements = document.querySelectorAll('#app-bar-guide-menu, .ytd-app, ytd-mini-guide-renderer, #guide-button, ytd-mini-guide-entry-renderer');
                                    elements.forEach(function(el) { el.style.display = 'none'; });
                                    var bottomNavs = document.querySelectorAll('[class*="bottom-nav"], [class*="BottomNavigation"], ytd-app > #content.ytd-app');
                                    bottomNavs.forEach(function(el) { if (el && el.style) el.style.paddingBottom = '0px'; });
                                })();
                            """.trimIndent()
                            
                            view?.evaluateJavascript(hideYouTubeUI, null)
                            
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