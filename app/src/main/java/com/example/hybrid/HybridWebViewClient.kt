package com.example.hybrid

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

class HybridWebViewClient(
    private val context: Context,
    private val onPageLoadingChanged: (Boolean) -> Unit,
    private val onPageUrlChanged: (String) -> Unit,
    private val onErrorOccurred: (String?) -> Unit
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val uri = request?.url ?: return false
        val scheme = uri.scheme?.lowercase() ?: ""

        // Handle native intent schemes (telephone, email, sms, whatsapp)
        if (scheme == "tel" || scheme == "mailto" || scheme == "sms" || scheme == "whatsapp") {
            try {
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                return true
            } catch (e: Exception) {
                return false
            }
        }

        // Open standard http / https URLs in the current WebView
        if (scheme == "http" || scheme == "https") {
            return false
        }

        // Generic intent fallbacks
        return try {
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        onPageLoadingChanged(true)
        url?.let { onPageUrlChanged(it) }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        onPageLoadingChanged(false)
        url?.let { onPageUrlChanged(it) }

        // Inject responsive helper and Akiya bridge alias
        view?.evaluateJavascript(
            """
            (function() {
                window.isAkiyaMobileApp = true;
                window.AkiyaApp = window.AndroidBridge || {};
                
                // Ensure viewport meta tag exists for seamless mobile responsiveness
                var metaViewport = document.querySelector('meta[name="viewport"]');
                if (!metaViewport) {
                    var meta = document.createElement('meta');
                    meta.name = 'viewport';
                    meta.content = 'width=device-width, initial-scale=1.0, maximum-scale=5.0, user-scalable=yes';
                    document.getElementsByTagName('head')[0].appendChild(meta);
                }
            })();
            """.trimIndent(),
            null
        )
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)
        if (request?.isForMainFrame == true) {
            onPageLoadingChanged(false)
            val desc = error?.description?.toString() ?: "Network Connection Error"
            onErrorOccurred(desc)
        }
    }

    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
        // Gracefully handle Chromium render process / rendernode termination without terminating host process
        try {
            view?.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        } catch (e: Exception) {
            // Ignore
        }
        onPageLoadingChanged(false)
        onErrorOccurred("Graphics renderer recovered. Tap retry if page is blank.")
        return true
    }
}
