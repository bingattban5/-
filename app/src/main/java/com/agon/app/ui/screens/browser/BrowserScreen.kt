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
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import com.agon.app.data.DownloadMode
import com.agon.app.data.SubtitleMethod
import com.agon.app.ui.screens.browser.components.*
import com.agon.app.viewmodel.BrowserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel,
    navController: NavHostController? = null
) {
    val state by viewModel.state.collectAsState()
    val pendingNavigation by viewModel.pendingNavigation.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // WebView ref للإشارة إليه من الأحداث الخارجية
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

                // زر التحميل العائم (FAB)
                if (state.videoInfo != null) {
                    FloatingActionButton(
                        onClick = { viewModel.showVideoSheet() },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .size(64.dp),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = MaterialTheme.shapes.extraLarge
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
                                contentDescription = "تحميل الفيديو",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                // القائمة الرئيسية (DropdownMenu)
                DropdownMenu(
                    expanded = state.showMainMenu,
                    onDismissRequest = { viewModel.dismissMainMenu() },
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    DropdownMenuItem(
                        text = { Text("التنزيلات") },
                        onClick = {
                            viewModel.dismissMainMenu()
                            navController?.navigate("downloads")
                        },
                        leadingIcon = { Icon(Icons.Filled.Download, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("النماذج") },
                        onClick = {
                            viewModel.dismissMainMenu()
                            navController?.navigate("models")
                        },
                        leadingIcon = { Icon(Icons.Filled.Memory, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("الإعدادات") },
                        onClick = {
                            viewModel.dismissMainMenu()
                            navController?.navigate("settings")
                        },
                        leadingIcon = { Icon(Icons.Filled.Settings, contentDescription = null) }
                    )
                }
            }
        }

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

        // صفحة بيانات الفيديو
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
                }

                // WebViewClient لالتقاط أحداث التنقل
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        // نمنع فتح الروابط خارج التطبيق، نفتحها داخل WebView
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
                            onPageFinished(url, title, faviconUrl)
                            onNavigationStateChange(view?.canGoBack() ?: false, view?.canGoForward() ?: false)
                        }
                    }
                }

                // WebChromeClient لالتقاط تقدم التحميل
                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                        onProgressChanged(newProgress)
                    }
                }

                // الضغط المطول على الروابط
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
            // تحميل رابط جديد عند تغيير التبويب
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
