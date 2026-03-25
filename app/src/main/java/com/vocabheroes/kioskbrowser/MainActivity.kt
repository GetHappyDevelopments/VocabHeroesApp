package com.vocabheroes.kioskbrowser

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var loadingContainer: LinearLayout
    private lateinit var errorContainer: LinearLayout
    private lateinit var errorMessage: TextView
    private lateinit var retryButton: Button

    private val urlPolicy = UrlPolicy(KioskConfig.allowedHosts)

    private var hasMainFrameLoadError = false
    private var pageLoadedSuccessfully = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindViews()
        enableImmersiveMode()
        setupBackNavigation()

        setupWebView()
        retryButton.setOnClickListener { loadStartUrl(force = true) }

        if (savedInstanceState == null) {
            loadStartUrl(force = true)
        } else {
            webView.restoreState(savedInstanceState)
            if (!pageLoadedSuccessfully) {
                showLoading()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        enableImmersiveMode()
        webView.onResume()
        if (!pageLoadedSuccessfully && !hasMainFrameLoadError) {
            loadStartUrl(force = false)
        }
    }

    override fun onPause() {
        webView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        webView.stopLoading()
        webView.destroy()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = false

            allowFileAccess = false
            allowContentAccess = false
            allowFileAccessFromFileURLs = false
            allowUniversalAccessFromFileURLs = false

            setSupportMultipleWindows(false)
            mediaPlaybackRequiresUserGesture = true
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            loadsImagesAutomatically = true
            builtInZoomControls = false
            displayZoomControls = false
        }

        WebView.setWebContentsDebuggingEnabled(false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            webView.settings.safeBrowsingEnabled = true
        }

        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            WebSettingsCompat.setForceDark(webView.settings, WebSettingsCompat.FORCE_DARK_OFF)
        }

        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val target = request.url.toString()
                val blocked = !urlPolicy.isAllowed(target)
                if (blocked && request.isForMainFrame) {
                    hasMainFrameLoadError = true
                    showError("Weiterleitung zu nicht erlaubter Domain blockiert: ${request.url.host}")
                }
                return blocked
            }

            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                val blocked = !urlPolicy.isAllowed(url)
                if (blocked) {
                    hasMainFrameLoadError = true
                    showError("Weiterleitung zu nicht erlaubter Domain blockiert.")
                }
                return blocked
            }

            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                hasMainFrameLoadError = false
                showLoading()
            }

            override fun onPageFinished(view: WebView, url: String?) {
                super.onPageFinished(view, url)
                if (!hasMainFrameLoadError) {
                    pageLoadedSuccessfully = true
                    showWebView()
                }
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                super.onReceivedError(view, request, error)
                if (request.isForMainFrame) {
                    hasMainFrameLoadError = true
                    showError(messageForLoadError(error.errorCode))
                }
            }

            override fun onReceivedHttpError(
                view: WebView,
                request: WebResourceRequest,
                errorResponse: WebResourceResponse
            ) {
                super.onReceivedHttpError(view, request, errorResponse)
                if (request.isForMainFrame && errorResponse.statusCode >= 400) {
                    hasMainFrameLoadError = true
                    showError("Die Zielseite hat mit HTTP ${errorResponse.statusCode} geantwortet.")
                }
            }

            override fun onReceivedSslError(
                view: WebView,
                handler: SslErrorHandler,
                error: SslError
            ) {
                handler.cancel()
                hasMainFrameLoadError = true
                showError("TLS/SSL-Fehler beim Laden der Seite.")
            }

            override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                hasMainFrameLoadError = true
                showError("Die WebView wurde unerwartet beendet. Bitte erneut laden.")
                return true
            }
        }
    }

    private fun loadStartUrl(force: Boolean) {
        val startUrl = KioskConfig.startUrl
        if (!urlPolicy.isAllowed(startUrl)) {
            showError("Start-URL ist ungültig oder durch die Sicherheitsrichtlinie nicht erlaubt.")
            return
        }

        if (!isNetworkAvailable()) {
            showError("Keine Netzwerkverbindung verfügbar. Bitte Verbindung prüfen.")
            return
        }

        if (force) {
            webView.clearHistory()
        }

        hasMainFrameLoadError = false
        pageLoadedSuccessfully = false
        showLoading()
        webView.loadUrl(startUrl)
    }

    private fun bindViews() {
        webView = findViewById(R.id.webView)
        loadingContainer = findViewById(R.id.loadingContainer)
        errorContainer = findViewById(R.id.errorContainer)
        errorMessage = findViewById(R.id.errorMessage)
        retryButton = findViewById(R.id.retryButton)
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun enableImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun showLoading() {
        loadingContainer.visibility = View.VISIBLE
        errorContainer.visibility = View.GONE
        webView.visibility = View.VISIBLE
    }

    private fun showError(message: String) {
        errorMessage.text = message
        errorContainer.visibility = View.VISIBLE
        loadingContainer.visibility = View.GONE
        webView.visibility = View.GONE
    }

    private fun showWebView() {
        webView.visibility = View.VISIBLE
        loadingContainer.visibility = View.GONE
        errorContainer.visibility = View.GONE
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = getSystemService(ConnectivityManager::class.java) ?: return false
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun messageForLoadError(errorCode: Int): String {
        return when (errorCode) {
            WebViewClient.ERROR_HOST_LOOKUP -> "DNS-/Hostfehler. Bitte Verbindung und URL prüfen."
            WebViewClient.ERROR_CONNECT -> "Verbindung zum Server fehlgeschlagen."
            WebViewClient.ERROR_TIMEOUT -> "Zeitüberschreitung beim Laden der Seite."
            WebViewClient.ERROR_IO -> "Netzwerk-/I/O-Fehler beim Laden der Seite."
            WebViewClient.ERROR_FAILED_SSL_HANDSHAKE -> "TLS/SSL-Handshake fehlgeschlagen."
            WebViewClient.ERROR_PROXY_AUTHENTICATION -> "Proxy-Authentifizierung erforderlich."
            WebViewClient.ERROR_UNSUPPORTED_SCHEME -> "Nicht unterstütztes URL-Schema."
            else -> getString(R.string.error_message_default)
        }
    }
}

