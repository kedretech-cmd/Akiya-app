package com.example.hybrid

import android.net.Uri
import android.view.View
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView

class HybridWebChromeClient(
    private val onProgressUpdate: (Int) -> Unit,
    private val onTitleReceived: (String) -> Unit,
    private val onRequestMediaPermissions: (PermissionRequest) -> Unit,
    private val onOpenFileChooser: (ValueCallback<Array<Uri>>?, WebChromeClient.FileChooserParams?) -> Boolean,
    private val onShowCustomViewCallback: ((View, CustomViewCallback) -> Unit)? = null,
    private val onHideCustomViewCallback: (() -> Unit)? = null
) : WebChromeClient() {

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        onProgressUpdate(newProgress)
    }

    override fun onReceivedTitle(view: WebView?, title: String?) {
        super.onReceivedTitle(view, title)
        title?.let { onTitleReceived(it) }
    }

    override fun onPermissionRequest(request: PermissionRequest?) {
        if (request == null) return
        onRequestMediaPermissions(request)
    }

    override fun onPermissionRequestCanceled(request: PermissionRequest?) {
        super.onPermissionRequestCanceled(request)
    }

    override fun onGeolocationPermissionsShowPrompt(
        origin: String?,
        callback: GeolocationPermissions.Callback?
    ) {
        callback?.invoke(origin, true, false)
    }

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        return onOpenFileChooser(filePathCallback, fileChooserParams)
    }

    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
        if (view != null && callback != null && onShowCustomViewCallback != null) {
            onShowCustomViewCallback.invoke(view, callback)
        } else {
            super.onShowCustomView(view, callback)
        }
    }

    override fun onHideCustomView() {
        if (onHideCustomViewCallback != null) {
            onHideCustomViewCallback.invoke()
        } else {
            super.onHideCustomView()
        }
    }
}
