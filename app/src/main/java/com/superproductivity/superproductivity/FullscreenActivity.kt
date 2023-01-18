package com.superproductivity.superproductivity

import android.app.Activity
import android.app.Person
import android.content.DialogInterface
import android.content.Intent
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.webkit.*
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.anggrayudi.storage.SimpleStorageHelper


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class FullscreenActivity : AppCompatActivity() {
    private lateinit var javaScriptInterface: CommonJavaScriptInterface
    private lateinit var webView: WebView
    private lateinit var wvContainer: FrameLayout
    var isInForeground: Boolean = false
    val storageHelper = SimpleStorageHelper(this) // for scoped storage permission management on Android 10+

    @Suppress("ReplaceCallWithBinaryOperator")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, KeepAliveNotificationService::class.java))
        } else {
            startService(Intent(this, KeepAliveNotificationService::class.java))
        }
        initWebView()
        setContentView(R.layout.activity_fullscreen)
        wvContainer = findViewById(R.id.webview_container)
        wvContainer.addView(webView)
    }

    override fun onPause() {
        super.onPause()
        isInForeground = false
        Log.v("TW", "FullScreenActivity: onPause")
        callJSInterfaceFunctionIfExists("next", "onPause$")
    }

    override fun onResume() {
        super.onResume()
        isInForeground = true
        Log.v("TW", "FullScreenActivity: onResume")
        callJSInterfaceFunctionIfExists("next", "onResume$")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.v("TW", "FullScreenActivity: onNewIntent")
        val action = intent.getStringExtra("action")
        Log.v("TW", "FullScreenActivity: action $action")
        if (action == null) {
            return
        }
        when (action) {
            KeepAliveNotificationService.EXTRA_ACTION_PAUSE -> callJSInterfaceFunctionIfExists(
                "next",
                "onPauseCurrentTask$"
            )
            KeepAliveNotificationService.EXTRA_ACTION_DONE -> callJSInterfaceFunctionIfExists(
                "next",
                "onMarkCurrentTaskAsDone$"
            )
            KeepAliveNotificationService.EXTRA_ACTION_ADD_TASK -> callJSInterfaceFunctionIfExists(
                "next",
                "onAddNewTask$"
            )
        }
    }

    private fun initWebView() {
        webView = (application as App).wv
        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.useWideViewPort = true
        webSettings.loadWithOverviewMode = true
        webSettings.domStorageEnabled = true

        webView.webViewClient = WebViewController(this)
        webView.loadUrl("http://10.0.2.2:4200")
    }

    class WebViewController(activity: Activity) : WebViewClient() {
        private val activity: Activity = activity

        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            view.loadUrl(url)
            return true
        }

        override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
            val builder: AlertDialog.Builder =
                AlertDialog.Builder(activity)
            builder.setMessage("Error")
            builder.setPositiveButton("Continue",
                DialogInterface.OnClickListener { dialog, which -> handler.proceed() })
            builder.setNegativeButton("Cancel",
                DialogInterface.OnClickListener { dialog, which -> handler.cancel() })
            val dialog: AlertDialog = builder.create()
            dialog.show()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (data != null) {
            javaScriptInterface.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun callJSInterfaceFunctionIfExists(fnName: String, objectPath: String) {
        val fnFullName = "window.$WINDOW_INTERFACE_PROPERTY.$objectPath.$fnName"
        val fullObjectPath = "window.$WINDOW_INTERFACE_PROPERTY.$objectPath"
        callJavaScriptFunction("if($fullObjectPath && $fnFullName)$fnFullName()")
    }

    fun callJavaScriptFunction(script: String) {
        webView.post { webView.evaluateJavascript(script) { } }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        wvContainer.removeView(webView)
        super.onDestroy()
    }

    companion object {
        const val WINDOW_INTERFACE_PROPERTY: String = "SUPAndroid"
        const val WINDOW_PROPERTY_F_DROID: String = "SUPFDroid"
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // Save scoped storage permission on Android 10+
        storageHelper.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        // Restore scoped storage permission on Android 10+
        super.onRestoreInstanceState(savedInstanceState)
        storageHelper.onRestoreInstanceState(savedInstanceState)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        // Restore scoped storage permission on Android 10+
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Mandatory for Activity, but not for Fragment & ComponentActivity
        //storageHelper.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}
