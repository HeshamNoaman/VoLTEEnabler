package com.volteenabler.app.shizuku

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider

object ShizukuManager {
    const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
    const val REQUEST_CODE_PERMISSION = 7101

    private val _isInstalled = MutableStateFlow(false)
    val isInstalled: StateFlow<Boolean> = _isInstalled.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _isAuthorized = MutableStateFlow(false)
    val isAuthorized: StateFlow<Boolean> = _isAuthorized.asStateFlow()

    private var appContext: Context? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        checkStatus()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        _isRunning.value = false
        _isAuthorized.value = false
    }

    private val permissionResultListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == REQUEST_CODE_PERMISSION) {
            val granted = grantResult == PackageManager.PERMISSION_GRANTED
            _isAuthorized.value = granted
            if (granted) {
                onPermissionGranted()
            }
        }
    }

    fun init(context: Context) {
        appContext = context.applicationContext
        try {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(permissionResultListener)
        } catch (_: Exception) {}
        checkStatus()
    }

    fun checkStatus() {
        val context = appContext ?: return
        val installed = try {
            context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
        _isInstalled.value = installed

        val running = try {
            Shizuku.pingBinder()
        } catch (_: Exception) {
            false
        }
        _isRunning.value = running

        val hasShizukuPerm = try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Exception) {
            false
        }

        val hasProviderPerm = try {
            context.checkCallingOrSelfPermission(ShizukuProvider.PERMISSION) == PackageManager.PERMISSION_GRANTED
        } catch (_: Exception) {
            false
        }

        val authorized = running && (hasShizukuPerm || hasProviderPerm)
        _isAuthorized.value = authorized

        if (authorized) {
            onPermissionGranted()
        }
    }

    fun requestPermission() {
        val context = appContext ?: return
        if (!isRunning.value) {
            openShizukuApp(context)
            return
        }
        try {
            if (!Shizuku.isPreV11() && Shizuku.getVersion() >= 11) {
                Shizuku.requestPermission(REQUEST_CODE_PERMISSION)
            } else {
                openShizukuApp(context)
            }
        } catch (_: Exception) {
            openShizukuApp(context)
        }
    }

    private fun onPermissionGranted() {
        val context = appContext ?: return
        val pkg = context.packageName
        scope.launch(Dispatchers.IO) {
            try {
                ShellExecutor.exec("pm grant $pkg android.permission.WRITE_SECURE_SETTINGS")
            } catch (_: Exception) {}
        }
    }

    fun openShizukuApp(context: Context) {
        try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
            if (launchIntent != null) {
                context.startActivity(launchIntent)
            } else {
                openShizukuWebsite(context)
            }
        } catch (_: Exception) {
            openShizukuWebsite(context)
        }
    }

    fun openShizukuWebsite(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
}
