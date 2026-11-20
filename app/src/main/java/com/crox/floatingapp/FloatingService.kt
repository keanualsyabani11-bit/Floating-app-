package com.crox.floatingapp

import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageButton
import android.widget.LinearLayout

enum class BubbleShape { CIRCLE, SQUARE, TRIANGLE }
enum class BubbleColor { BLUE, GREEN, RED }

class FloatingService : Service() {

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val CHANNEL_ID = "FloatingBubbleChannel"
        const val NOTIF_ID = 1
        private var instance: FloatingService? = null
        fun updateShape(shape: BubbleShape) { instance?.applyShape(shape) }
        fun updateColor(color: BubbleColor) { instance?.applyColor(color) }
    }

    private lateinit var windowManager: WindowManager
    private var bubbleView: View? = null
    private var expandedView: View? = null
    private var currentShape = BubbleShape.CIRCLE
    private var currentColor = BubbleColor.BLUE
    private var isExpanded = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification())
        when (intent?.action) {
            ACTION_START -> showBubble()
            ACTION_STOP -> { removeBubble(); stopSelf() }
        }
        return START_STICKY
    }

    private fun showBubble() {
        if (bubbleView != null) return
        val params = WindowManager.LayoutParams(
            120, 120,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = 100; y = 300 }

        val view = LayoutInflater.from(this).inflate(R.layout.bubble_view, null)
        bubbleView = view
        applyShape(currentShape)
        setupBubbleDrag(view, params)
        view.setOnClickListener { if (isExpanded) collapseWindow() else expandWindow() }
        windowManager.addView(view, params)
    }

    private fun setupBubbleDrag(view: View, params: WindowManager.LayoutParams) {
        var ix = 0; var iy = 0; var tx = 0f; var ty = 0f; var dragging = false
        view.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { ix = params.x; iy = params.y; tx = e.rawX; ty = e.rawY; dragging = false; false }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (e.rawX - tx).toInt(); val dy = (e.rawY - ty).toInt()
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) dragging = true
                    params.x = ix + dx; params.y = iy + dy
                    windowManager.updateViewLayout(view, params); true
                }
                MotionEvent.ACTION_UP -> { if (!dragging) view.performClick(); dragging }
                else -> false
            }
        }
    }

    private fun expandWindow() {
        isExpanded = true
        if (expandedView != null) return
        val dm = resources.displayMetrics
        val params = WindowManager.LayoutParams(
            (dm.widthPixels * 0.92f).toInt(), (dm.heightPixels * 0.75f).toInt(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.CENTER }

        val view = LayoutInflater.from(this).inflate(R.layout.expanded_view, null)
        expandedView = view

        val webView = view.findViewById<WebView>(R.id.webView)
        webView.settings.apply {
            javaScriptEnabled = true; domStorageEnabled = true
            useWideViewPort = true; loadWithOverviewMode = true
            setSupportZoom(true); builtInZoomControls = true; displayZoomControls = false
        }
        webView.webViewClient = WebViewClient()
        webView.loadUrl("https://latihansoalonline.com")

        view.findViewById<ImageButton>(R.id.btnClose).setOnClickListener { collapseWindow() }

        val header = view.findViewById<LinearLayout>(R.id.header)
        var ix = 0; var iy = 0; var tx = 0f; var ty = 0f
        header.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { ix = params.x; iy = params.y; tx = e.rawX; ty = e.rawY; true }
                MotionEvent.ACTION_MOVE -> { params.x = ix + (e.rawX-tx).toInt(); params.y = iy + (e.rawY-ty).toInt(); windowManager.updateViewLayout(view, params); true }
                else -> false
            }
        }
        windowManager.addView(view, params)
    }

    private fun collapseWindow() {
        isExpanded = false
        expandedView?.let { windowManager.removeView(it); expandedView = null }
    }

    fun applyShape(shape: BubbleShape) {
        currentShape = shape
        bubbleView?.setBackgroundResource(when (shape) {
            BubbleShape.CIRCLE -> R.drawable.bubble_circle
            BubbleShape.SQUARE -> R.drawable.bubble_square
            BubbleShape.TRIANGLE -> R.drawable.bubble_triangle
        })
    }

    fun applyColor(color: BubbleColor) { currentColor = color; applyShape(currentShape) }

    private fun removeBubble() {
        collapseWindow()
        bubbleView?.let { windowManager.removeView(it); bubbleView = null }
    }

    override fun onDestroy() { instance = null; removeBubble(); super.onDestroy() }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "Floating Bubble", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    private fun buildNotification(): Notification {
        val pi = PendingIntent.getService(this, 0,
            Intent(this, FloatingService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Floating App Aktif")
            .setContentText("Ketuk bubble untuk buka browser")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .addAction(android.R.drawable.ic_delete, "Tutup", pi)
            .build()
    }
}
