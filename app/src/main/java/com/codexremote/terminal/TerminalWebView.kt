package com.codexremote.terminal

import android.annotation.SuppressLint
import android.graphics.Color
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

class TerminalWebController {
    private var webView: WebView? = null

    internal fun attach(value: WebView) {
        webView = value
    }

    internal fun detach(value: WebView) {
        if (webView === value) webView = null
    }

    fun write(bytes: ByteArray) {
        val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
        evaluate("window.ComsoTerminal && window.ComsoTerminal.writeBase64('$encoded')")
    }

    fun clear() = evaluate("window.ComsoTerminal && window.ComsoTerminal.clear()")

    fun focus() = evaluate("window.ComsoTerminal && window.ComsoTerminal.focus()")

    fun fit() = evaluate("window.ComsoTerminal && window.ComsoTerminal.fit()")

    fun setLocked(locked: Boolean) {
        evaluate("window.ComsoTerminal && window.ComsoTerminal.setLocked(${if (locked) "true" else "false"})")
    }

    fun setFontSize(size: Int) {
        evaluate("window.ComsoTerminal && window.ComsoTerminal.setFontSize(${size.coerceIn(9, 28)})")
    }

    private fun evaluate(script: String) {
        webView?.post { webView?.evaluateJavascript(script, null) }
    }
}

private class TerminalJavascriptBridge(
    private val onReady: (Int, Int, Int, Int) -> Unit,
    private val onInput: (ByteArray) -> Unit,
    private val onResize: (Int, Int, Int, Int) -> Unit,
    private val onFontSize: (Int) -> Unit,
    private val onError: (String) -> Unit,
) {
    @JavascriptInterface
    fun onReady(columns: Int, rows: Int, widthPx: Int, heightPx: Int) {
        onReady(columns, rows, widthPx, heightPx)
    }

    @JavascriptInterface
    fun onData(base64: String) {
        runCatching { Base64.decode(base64, Base64.DEFAULT) }.onSuccess(onInput)
    }

    @JavascriptInterface
    fun onResize(columns: Int, rows: Int, widthPx: Int, heightPx: Int) {
        onResize(columns, rows, widthPx, heightPx)
    }

    @JavascriptInterface
    fun onFontSize(size: Int) {
        onFontSize(size)
    }

    @JavascriptInterface
    fun onError(message: String) {
        onError(message.take(240))
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TerminalWebView(
    controller: TerminalWebController,
    modifier: Modifier = Modifier,
    onReady: (Int, Int, Int, Int) -> Unit,
    onInput: (ByteArray) -> Unit,
    onResize: (Int, Int, Int, Int) -> Unit,
    onFontSize: (Int) -> Unit,
    onError: (String) -> Unit,
) {
    val currentOnReady = rememberUpdatedState(onReady)
    val currentOnInput = rememberUpdatedState(onInput)
    val currentOnResize = rememberUpdatedState(onResize)
    val currentOnFontSize = rememberUpdatedState(onFontSize)
    val currentOnError = rememberUpdatedState(onError)
    val bridge = remember {
        TerminalJavascriptBridge(
            onReady = { columns, rows, width, height ->
                currentOnReady.value(columns, rows, width, height)
            },
            onInput = { currentOnInput.value(it) },
            onResize = { columns, rows, width, height ->
                currentOnResize.value(columns, rows, width, height)
            },
            onFontSize = { currentOnFontSize.value(it) },
            onError = { currentOnError.value(it) },
        )
    }
    val attachedView = remember { arrayOfNulls<WebView>(1) }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(Color.BLACK)
                isFocusable = true
                isFocusableInTouchMode = true
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = false
                settings.allowFileAccess = true
                settings.allowContentAccess = false
                settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                settings.setSupportZoom(false)
                addJavascriptInterface(bridge, "AndroidTerminal")
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = true
                }
                loadUrl("file:///android_asset/terminal/index.html")
                controller.attach(this)
                attachedView[0] = this
            }
        },
    )

    DisposableEffect(controller) {
        onDispose {
            attachedView[0]?.let { view ->
                controller.detach(view)
                view.removeJavascriptInterface("AndroidTerminal")
                view.destroy()
            }
            attachedView[0] = null
        }
    }
}
