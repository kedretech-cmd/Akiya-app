package com.example.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.hybrid.HybridWebChromeClient
import com.example.hybrid.HybridWebViewClient
import com.example.hybrid.WebAppInterface
import com.example.notifications.NotificationHelper

enum class PortalDomain(val displayName: String, val host: String, val url: String) {
    AKIYA("Akiya", "akiya.pro.et", "https://akiya.pro.et"),
    AKMEL("Akmel", "akmel.my.id", "https://akmel.my.id")
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun HybridWebViewScreen(
    initialPortal: PortalDomain = PortalDomain.AKIYA,
    incomingCallAction: String? = null,
    callerNameExtra: String? = null,
    notificationHelper: NotificationHelper,
    onToggleSystemFullscreen: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var activePortal by remember { mutableStateOf(initialPortal) }
    var currentUrl by remember { mutableStateOf(initialPortal.url) }
    var pageTitle by remember { mutableStateOf(initialPortal.displayName) }
    var isLoading by remember { mutableStateOf(true) }
    var loadProgress by remember { mutableFloatStateOf(0f) }
    var connectionError by remember { mutableStateOf<String?>(null) }

    // Start in Fullscreen mode by default for pure Web APK experience
    var isFullscreen by remember { mutableStateOf(true) }
    var isFloatingPillExpanded by remember { mutableStateOf(false) }
    var isDesktopMode by remember { mutableStateOf(false) }

    var showMenu by remember { mutableStateOf(false) }
    var showPermissionsSheet by remember { mutableStateOf(false) }
    var showAllNotificationsDialog by remember { mutableStateOf(false) }
    var showCustomUrlDialog by remember { mutableStateOf(false) }
    var customUrlInput by remember { mutableStateOf("") }

    // HTML5 custom view (e.g. video fullscreen)
    var customView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    // Call state
    var activeCallNotification by remember {
        mutableStateOf<Pair<String, Boolean>?>(
            if (incomingCallAction == NotificationHelper.ACTION_ANSWER_CALL) {
                Pair(callerNameExtra ?: "Active Call", true)
            } else null
        )
    }

    // Permission tracking state for all required capabilities
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
            } else true
        )
    }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var hasBluetoothPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                    PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    // Pending WebRTC permission request
    var pendingWebRtcRequest by remember { mutableStateOf<PermissionRequest?>(null) }

    // Pending HTML5 file chooser callback
    var filePathCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

    // Reference to WebView instance
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    // Synchronize system bars fullscreen
    LaunchedEffect(isFullscreen) {
        onToggleSystemFullscreen(isFullscreen)
    }

    // Permission Launchers for all permissions
    val requestPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCameraPermission = permissions[Manifest.permission.CAMERA] ?: hasCameraPermission
        hasAudioPermission = permissions[Manifest.permission.RECORD_AUDIO] ?: hasAudioPermission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNotificationPermission =
                permissions[Manifest.permission.POST_NOTIFICATIONS] ?: hasNotificationPermission
        }
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: hasLocationPermission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasBluetoothPermission =
                permissions[Manifest.permission.BLUETOOTH_CONNECT] ?: hasBluetoothPermission
        }

        // Handle any pending WebRTC permission request
        pendingWebRtcRequest?.let { req ->
            val requested = req.resources
            val grantedList = mutableListOf<String>()
            if (requested.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE) && hasCameraPermission) {
                grantedList.add(PermissionRequest.RESOURCE_VIDEO_CAPTURE)
            }
            if (requested.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE) && hasAudioPermission) {
                grantedList.add(PermissionRequest.RESOURCE_AUDIO_CAPTURE)
            }
            if (requested.contains(PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID)) {
                grantedList.add(PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID)
            }
            if (grantedList.isNotEmpty()) {
                req.grant(grantedList.toTypedArray())
            } else {
                req.deny()
            }
            pendingWebRtcRequest = null
        }
    }

    // File picker launcher for HTML5 inputs
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uriList = mutableListOf<Uri>()
        result.data?.data?.let { uriList.add(it) }
        result.data?.clipData?.let { clipData ->
            for (i in 0 until clipData.itemCount) {
                uriList.add(clipData.getItemAt(i).uri)
            }
        }
        filePathCallback?.onReceiveValue(if (uriList.isNotEmpty()) uriList.toTypedArray() else null)
        filePathCallback = null
    }

    // Function to request all permissions at once
    val requestAllPermissions: () -> Unit = {
        val perms = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            perms.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        requestPermissionsLauncher.launch(perms.toTypedArray())
    }

    // Auto-prompt on launch to ensure all permissions and notifications are ready
    LaunchedEffect(Unit) {
        if (!hasCameraPermission || !hasAudioPermission || !hasNotificationPermission || !hasLocationPermission) {
            requestAllPermissions()
        }
    }

    // Back handling for CustomView, Fullscreen & WebView history
    BackHandler(enabled = true) {
        if (customView != null) {
            customViewCallback?.onCustomViewHidden()
            customView = null
            customViewCallback = null
        } else if (isFloatingPillExpanded) {
            isFloatingPillExpanded = false
        } else if (webViewInstance?.canGoBack() == true) {
            webViewInstance?.goBack()
        } else if (isFullscreen) {
            isFullscreen = false
        }
    }

    // WebAppInterface bridge
    val webAppInterface = remember(context) {
        WebAppInterface(
            context = context,
            notificationHelper = notificationHelper,
            onSwitchPortal = { portalName ->
                val newPortal = if (portalName.contains("akmel", ignoreCase = true)) {
                    PortalDomain.AKMEL
                } else {
                    PortalDomain.AKIYA
                }
                activePortal = newPortal
                connectionError = null
                webViewInstance?.loadUrl(newPortal.url)
            },
            onRequestNativePermissions = {
                requestAllPermissions()
            },
            onCallStateChanged = { name, incoming ->
                activeCallNotification = if (incoming) Pair(name, true) else null
            },
            onToggleFullscreen = { reqFullscreen ->
                isFullscreen = reqFullscreen
            }
        )
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                AnimatedVisibility(
                    visible = !isFullscreen,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        // Main Top Control Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Portal Switcher Chips
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PortalChip(
                                    title = "akiya.pro.et",
                                    isSelected = activePortal == PortalDomain.AKIYA,
                                    onClick = {
                                        if (activePortal != PortalDomain.AKIYA) {
                                            activePortal = PortalDomain.AKIYA
                                            connectionError = null
                                            webViewInstance?.loadUrl(PortalDomain.AKIYA.url)
                                        }
                                    },
                                    testTag = "portal_chip_akiya"
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                PortalChip(
                                    title = "akmel.my.id",
                                    isSelected = activePortal == PortalDomain.AKMEL,
                                    onClick = {
                                        if (activePortal != PortalDomain.AKMEL) {
                                            activePortal = PortalDomain.AKMEL
                                            connectionError = null
                                            webViewInstance?.loadUrl(PortalDomain.AKMEL.url)
                                        }
                                    },
                                    testTag = "portal_chip_akmel"
                                )
                            }

                            // Actions
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val allGranted = hasCameraPermission && hasAudioPermission && hasNotificationPermission && hasLocationPermission
                                IconButton(
                                    onClick = { showPermissionsSheet = true },
                                    modifier = Modifier.testTag("permissions_button")
                                ) {
                                    Icon(
                                        imageVector = if (allGranted) Icons.Default.Security else Icons.Default.Warning,
                                        contentDescription = "All Permissions Status",
                                        tint = if (allGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                    )
                                }

                                IconButton(
                                    onClick = { showAllNotificationsDialog = true },
                                    modifier = Modifier.testTag("notifications_hub_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = "Notifications For All",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        connectionError = null
                                        webViewInstance?.reload()
                                    },
                                    modifier = Modifier.testTag("reload_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Reload Page"
                                    )
                                }

                                // Enter Fullscreen Button
                                IconButton(
                                    onClick = { isFullscreen = true },
                                    modifier = Modifier.testTag("fullscreen_toggle_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fullscreen,
                                        contentDescription = "Enter Fullscreen",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Box {
                                    IconButton(
                                        onClick = { showMenu = true },
                                        modifier = Modifier.testTag("more_menu_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "More Options"
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = showMenu,
                                        onDismissRequest = { showMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Fullscreen Mode") },
                                            leadingIcon = {
                                                Icon(Icons.Default.Fullscreen, contentDescription = null)
                                            },
                                            onClick = {
                                                showMenu = false
                                                isFullscreen = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Notifications For All Hub") },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.NotificationsActive,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            },
                                            onClick = {
                                                showMenu = false
                                                showAllNotificationsDialog = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Grant All Permissions") },
                                            leadingIcon = {
                                                Icon(Icons.Default.Security, contentDescription = null)
                                            },
                                            onClick = {
                                                showMenu = false
                                                requestAllPermissions()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    if (isDesktopMode) "Switch to Mobile View"
                                                    else "Switch to Desktop View"
                                                )
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    if (isDesktopMode) Icons.Default.Smartphone
                                                    else Icons.Default.Computer,
                                                    contentDescription = null
                                                )
                                            },
                                            onClick = {
                                                showMenu = false
                                                isDesktopMode = !isDesktopMode
                                                webViewInstance?.settings?.userAgentString = if (isDesktopMode) {
                                                    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 AkiyaHybrid/2.0"
                                                } else {
                                                    null
                                                }
                                                webViewInstance?.reload()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Open Custom URL") },
                                            leadingIcon = {
                                                Icon(Icons.Default.SwapHoriz, contentDescription = null)
                                            },
                                            onClick = {
                                                showMenu = false
                                                customUrlInput = currentUrl
                                                showCustomUrlDialog = true
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // URL and SSL Status Subheader
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Secure HTTPS",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = currentUrl,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (isLoading) {
                            LinearProgressIndicator(
                                progress = { loadProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                thickness = 1.dp
                            )
                        }
                    }
                }
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = !isFullscreen,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        tonalElevation = 3.dp,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (webViewInstance?.canGoBack() == true) {
                                        webViewInstance?.goBack()
                                    }
                                },
                                enabled = webViewInstance?.canGoBack() == true,
                                modifier = Modifier.testTag("nav_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (webViewInstance?.canGoForward() == true) {
                                        webViewInstance?.goForward()
                                    }
                                },
                                enabled = webViewInstance?.canGoForward() == true,
                                modifier = Modifier.testTag("nav_forward_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Forward"
                                )
                            }

                            IconButton(
                                onClick = {
                                    connectionError = null
                                    webViewInstance?.loadUrl(activePortal.url)
                                },
                                modifier = Modifier.testTag("nav_home_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = "Home"
                                )
                            }

                            FilledTonalButton(
                                onClick = {
                                    val target = if (activePortal == PortalDomain.AKIYA) {
                                        PortalDomain.AKMEL
                                    } else {
                                        PortalDomain.AKIYA
                                    }
                                    activePortal = target
                                    connectionError = null
                                    webViewInstance?.loadUrl(target.url)
                                },
                                modifier = Modifier.testTag("switch_portal_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (activePortal == PortalDomain.AKIYA) "akmel.my.id" else "akiya.pro.et",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            IconButton(
                                onClick = { showAllNotificationsDialog = true },
                                modifier = Modifier.testTag("quick_notifications_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = "Notification Center",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            IconButton(
                                onClick = { isFullscreen = true },
                                modifier = Modifier.testTag("quick_fullscreen_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fullscreen,
                                    contentDescription = "Fullscreen",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (isFullscreen) androidx.compose.foundation.layout.PaddingValues(0.dp) else paddingValues)
            ) {
                // Main Hybrid WebView filling entire canvas in Fullscreen
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )

                            // Gracefully configure rendering layer type to handle environments without DRM rendernodes
                            try {
                                setLayerType(View.LAYER_TYPE_HARDWARE, null)
                            } catch (e: Exception) {
                                setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                            }

                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                builtInZoomControls = true
                                displayZoomControls = false
                                allowFileAccess = true
                                allowContentAccess = true
                                mediaPlaybackRequiresUserGesture = false
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                cacheMode = WebSettings.LOAD_DEFAULT
                                setSupportZoom(true)
                                setGeolocationEnabled(true)
                            }

                            addJavascriptInterface(webAppInterface, "AndroidBridge")
                            addJavascriptInterface(webAppInterface, "AkiyaApp")

                            webChromeClient = HybridWebChromeClient(
                                onProgressUpdate = { progress ->
                                    loadProgress = progress / 100f
                                    isLoading = progress < 100
                                },
                                onTitleReceived = { title ->
                                    if (title.isNotBlank()) pageTitle = title
                                },
                                onRequestMediaPermissions = { request ->
                                    val requestedResources = request.resources
                                    val needsCamera = requestedResources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE)
                                    val needsAudio = requestedResources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE)

                                    if ((!needsCamera || hasCameraPermission) && (!needsAudio || hasAudioPermission)) {
                                        request.grant(requestedResources)
                                    } else {
                                        pendingWebRtcRequest = request
                                        requestAllPermissions()
                                    }
                                },
                                onOpenFileChooser = { callback, _ ->
                                    filePathCallback = callback
                                    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                                        addCategory(Intent.CATEGORY_OPENABLE)
                                        type = "*/*"
                                        putExtra(
                                            Intent.EXTRA_MIME_TYPES,
                                            arrayOf("image/*", "video/*", "application/pdf")
                                        )
                                    }
                                    try {
                                        filePickerLauncher.launch(
                                            Intent.createChooser(intent, "Select Media or Document")
                                        )
                                        true
                                    } catch (e: Exception) {
                                        filePathCallback = null
                                        false
                                    }
                                },
                                onShowCustomViewCallback = { view, callback ->
                                    customView = view
                                    customViewCallback = callback
                                },
                                onHideCustomViewCallback = {
                                    customView = null
                                    customViewCallback = null
                                }
                            )

                            webViewClient = HybridWebViewClient(
                                context = ctx,
                                onPageLoadingChanged = { loading ->
                                    isLoading = loading
                                },
                                onPageUrlChanged = { url ->
                                    currentUrl = url
                                    if (url.contains("akmel.my.id", ignoreCase = true)) {
                                        activePortal = PortalDomain.AKMEL
                                    } else if (url.contains("akiya.pro.et", ignoreCase = true)) {
                                        activePortal = PortalDomain.AKIYA
                                    }
                                },
                                onErrorOccurred = { desc ->
                                    connectionError = desc
                                }
                            )

                            loadUrl(activePortal.url)
                            webViewInstance = this
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // HTML5 Video / WebRTC Fullscreen Custom View Container
                customView?.let { view ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                    ) {
                        AndroidView(
                            factory = { view },
                            modifier = Modifier.fillMaxSize()
                        )
                        IconButton(
                            onClick = {
                                customViewCallback?.onCustomViewHidden()
                                customView = null
                                customViewCallback = null
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close Fullscreen Video", tint = Color.White)
                        }
                    }
                }

                // In Fullscreen Mode: Subtle top loading bar
                if (isFullscreen && isLoading) {
                    LinearProgressIndicator(
                        progress = { loadProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp)
                            .align(Alignment.TopCenter),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Active Call Notification Bar (Inside App)
                activeCallNotification?.let { (caller, isIncoming) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .align(Alignment.TopCenter)
                            .testTag("active_call_card"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = caller,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = if (isIncoming) "Incoming WebRTC Call" else "Call Connected",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            Row {
                                FilledTonalButton(
                                    onClick = {
                                        notificationHelper.dismissCallNotification()
                                        activeCallNotification = null
                                        webViewInstance?.evaluateJavascript(
                                            "if (window.onCallAccepted) { window.onCallAccepted(); }",
                                            null
                                        )
                                    },
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Text("Answer", color = MaterialTheme.colorScheme.onPrimary)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        notificationHelper.dismissCallNotification()
                                        activeCallNotification = null
                                        webViewInstance?.evaluateJavascript(
                                            "if (window.onCallDeclined) { window.onCallDeclined(); }",
                                            null
                                        )
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.CallEnd,
                                        contentDescription = "Decline",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }

                // Offline / Connection Error Overlay
                if (connectionError != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .align(Alignment.Center)
                            .testTag("connection_error_card"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(6.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Connection Unavailable",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Unable to connect to ${activePortal.host}. (${connectionError ?: ""})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Button(
                                    onClick = {
                                        connectionError = null
                                        webViewInstance?.reload()
                                    },
                                    modifier = Modifier.testTag("retry_connection_button")
                                ) {
                                    Text("Retry")
                                }
                                OutlinedButton(
                                    onClick = {
                                        val alternate = if (activePortal == PortalDomain.AKIYA) {
                                            PortalDomain.AKMEL
                                        } else {
                                            PortalDomain.AKIYA
                                        }
                                        activePortal = alternate
                                        connectionError = null
                                        webViewInstance?.loadUrl(alternate.url)
                                    },
                                    modifier = Modifier.testTag("try_alternate_portal_button")
                                ) {
                                    Text(if (activePortal == PortalDomain.AKIYA) "Try akmel.my.id" else "Try akiya.pro.et")
                                }
                            }
                        }
                    }
                }

                // Fullscreen Floating Control Pill
                if (isFullscreen && customView == null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.94f),
                            tonalElevation = 6.dp,
                            shadowElevation = 8.dp,
                            modifier = Modifier.testTag("fullscreen_floating_pill")
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                // Expanded Quick Action Row
                                AnimatedVisibility(visible = isFloatingPillExpanded) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        // Portal switcher inside full screen
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            PortalChip(
                                                title = "akiya.pro.et",
                                                isSelected = activePortal == PortalDomain.AKIYA,
                                                onClick = {
                                                    if (activePortal != PortalDomain.AKIYA) {
                                                        activePortal = PortalDomain.AKIYA
                                                        connectionError = null
                                                        webViewInstance?.loadUrl(PortalDomain.AKIYA.url)
                                                    }
                                                },
                                                testTag = "pill_chip_akiya"
                                            )
                                            PortalChip(
                                                title = "akmel.my.id",
                                                isSelected = activePortal == PortalDomain.AKMEL,
                                                onClick = {
                                                    if (activePortal != PortalDomain.AKMEL) {
                                                        activePortal = PortalDomain.AKMEL
                                                        connectionError = null
                                                        webViewInstance?.loadUrl(PortalDomain.AKMEL.url)
                                                    }
                                                },
                                                testTag = "pill_chip_akmel"
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Navigation & Tools
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    if (webViewInstance?.canGoBack() == true) webViewInstance?.goBack()
                                                },
                                                enabled = webViewInstance?.canGoBack() == true,
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                            }

                                            IconButton(
                                                onClick = {
                                                    connectionError = null
                                                    webViewInstance?.reload()
                                                },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(Icons.Default.Refresh, contentDescription = "Reload")
                                            }

                                            IconButton(
                                                onClick = {
                                                    connectionError = null
                                                    webViewInstance?.loadUrl(activePortal.url)
                                                },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(Icons.Default.Home, contentDescription = "Home")
                                            }

                                            IconButton(
                                                onClick = { showAllNotificationsDialog = true },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.NotificationsActive,
                                                    contentDescription = "Notifications For All",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }

                                            IconButton(
                                                onClick = { showPermissionsSheet = true },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Security,
                                                    contentDescription = "All Permissions",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }

                                            IconButton(
                                                onClick = { isFullscreen = false },
                                                modifier = Modifier.size(36.dp).testTag("pill_exit_fullscreen")
                                            ) {
                                                Icon(Icons.Default.FullscreenExit, contentDescription = "Exit Fullscreen")
                                            }
                                        }
                                    }
                                }

                                // Main Pill Toggle Handle
                                Row(
                                    modifier = Modifier
                                        .clickable { isFloatingPillExpanded = !isFloatingPillExpanded }
                                        .padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = activePortal.displayName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Icon(
                                        imageVector = if (isFloatingPillExpanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                        contentDescription = "Expand Fullscreen Menu",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Complete Permissions Bottom Sheet (All Permissions)
        if (showPermissionsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showPermissionsSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "All App Permissions",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Comprehensive permission status for hybrid calling, media, notifications, and location.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    PermissionItem(
                        icon = Icons.Default.Notifications,
                        title = "Push & Calling Notifications",
                        description = "Incoming call heads-up, chat alerts, and portal updates",
                        isGranted = hasNotificationPermission,
                        onRequest = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                requestPermissionsLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                            }
                        }
                    )

                    PermissionItem(
                        icon = Icons.Default.CameraAlt,
                        title = "Camera Access",
                        description = "WebRTC video calling, QR code scanner, photo uploads",
                        isGranted = hasCameraPermission,
                        onRequest = {
                            requestPermissionsLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                        }
                    )

                    PermissionItem(
                        icon = Icons.Default.Mic,
                        title = "Microphone Access",
                        description = "Two-way voice calls, video calling audio, voice notes",
                        isGranted = hasAudioPermission,
                        onRequest = {
                            requestPermissionsLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                        }
                    )

                    PermissionItem(
                        icon = Icons.Default.LocationOn,
                        title = "Location Access",
                        description = "HTML5 geolocation and portal location services",
                        isGranted = hasLocationPermission,
                        onRequest = {
                            requestPermissionsLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    )

                    PermissionItem(
                        icon = Icons.Default.Bluetooth,
                        title = "Bluetooth Devices",
                        description = "Wireless headsets and audio routing during voice/video calls",
                        isGranted = hasBluetoothPermission,
                        onRequest = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                requestPermissionsLauncher.launch(arrayOf(Manifest.permission.BLUETOOTH_CONNECT))
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            requestAllPermissions()
                            showPermissionsSheet = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("grant_all_permissions_button")
                    ) {
                        Text("Grant All Permissions Now")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Notification Center For All Types (Interactive Hub)
        if (showAllNotificationsDialog) {
            AlertDialog(
                onDismissRequest = { showAllNotificationsDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Notification Center (All Types)")
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Test all notification channels and categories supported by the hybrid platform:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // 1. Incoming Call
                        NotificationActionCard(
                            title = "1. Incoming Call Alert",
                            subtitle = "Heads-up call notification with Answer/Decline",
                            icon = Icons.Default.Call,
                            onClick = {
                                notificationHelper.showIncomingCallNotification(
                                    callerName = "Support Desk (akiya.pro.et)",
                                    callType = "Video Call",
                                    callId = "call_${System.currentTimeMillis()}"
                                )
                                activeCallNotification = Pair("Support Desk (akiya.pro.et)", true)
                                showAllNotificationsDialog = false
                            }
                        )

                        // 2. Ongoing Active Call
                        NotificationActionCard(
                            title = "2. Ongoing Call Banner",
                            subtitle = "Background active call notification with End action",
                            icon = Icons.Default.Call,
                            onClick = {
                                notificationHelper.showOngoingCallNotification(
                                    callerName = "Akmel Team (akmel.my.id)",
                                    callType = "Voice Call"
                                )
                                showAllNotificationsDialog = false
                            }
                        )

                        // 3. Missed Call
                        NotificationActionCard(
                            title = "3. Missed Call Notification",
                            subtitle = "Alerts user of an unanswered call",
                            icon = Icons.Default.CallMissed,
                            onClick = {
                                notificationHelper.showMissedCallNotification(
                                    callerName = "Alex Rivera (akiya.pro.et)",
                                    callType = "Voice Call"
                                )
                                showAllNotificationsDialog = false
                            }
                        )

                        // 4. Chat Message
                        NotificationActionCard(
                            title = "4. New Chat Message",
                            subtitle = "Messaging notification with big text style",
                            icon = Icons.Default.Chat,
                            onClick = {
                                notificationHelper.showChatMessageNotification(
                                    sender = "Akiya Portal Bot",
                                    message = "Your verification code is 849201. Click to continue."
                                )
                                showAllNotificationsDialog = false
                            }
                        )

                        // 5. Web Push Alert
                        NotificationActionCard(
                            title = "5. Web Push & Announcement",
                            subtitle = "General portal notification with URL link",
                            icon = Icons.Default.Notifications,
                            onClick = {
                                notificationHelper.showPushNotification(
                                    title = "Akiya System Update",
                                    message = "Portal services on akiya.pro.et and akmel.my.id are online.",
                                    targetUrl = activePortal.url
                                )
                                showAllNotificationsDialog = false
                            }
                        )

                        // 6. Download Completed
                        NotificationActionCard(
                            title = "6. File Download Complete",
                            subtitle = "Notification for document or media downloads",
                            icon = Icons.Default.Download,
                            onClick = {
                                notificationHelper.showDownloadNotification(
                                    fileName = "akiya_report_export.pdf",
                                    progress = 100,
                                    isCompleted = true
                                )
                                showAllNotificationsDialog = false
                            }
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAllNotificationsDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }

        // Custom URL Dialog
        if (showCustomUrlDialog) {
            AlertDialog(
                onDismissRequest = { showCustomUrlDialog = false },
                title = { Text("Open Web URL") },
                text = {
                    OutlinedTextField(
                        value = customUrlInput,
                        onValueChange = { customUrlInput = it },
                        label = { Text("URL (e.g. https://akiya.pro.et/dashboard)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showCustomUrlDialog = false
                            val target = if (!customUrlInput.startsWith("http://") && !customUrlInput.startsWith("https://")) {
                                "https://$customUrlInput"
                            } else {
                                customUrlInput
                            }
                            connectionError = null
                            webViewInstance?.loadUrl(target)
                        }
                    ) {
                        Text("Open")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCustomUrlDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun NotificationActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "Test",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PortalChip(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        modifier = Modifier.testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PermissionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    onRequest: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isGranted) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    modifier = Modifier.padding(4.dp)
                ) {
                    Text(
                        text = "Granted",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            } else {
                OutlinedButton(onClick = onRequest) {
                    Text("Grant", fontSize = 12.sp)
                }
            }
        }
    }
}
