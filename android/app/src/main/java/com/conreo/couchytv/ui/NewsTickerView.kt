package com.conreo.couchytv.ui

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.conreo.couchytv.NewsItem

/** A small independent surface: scrolling must not invalidate the full launcher window. */
class NewsTickerView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {
    private val worker = HandlerThread("CouchyNewsRender").apply { start() }
    private val render = Handler(worker.looper)
    private val main = Handler(Looper.getMainLooper())
    @Volatile private var ready = false
    @Volatile private var active = false
    @Volatile private var released = false
    private var waitingForHeadline = false
    private val drawTask = Runnable { drawFrame() }
    private var lastFrame = 0L
    private var sampleStart = 0L
    private var sampleFrames = 0
    private var sampleDrawNs = 0L
    private var sampleReported = false
    private var identity = ""
    private var first: Bitmap? = null
    private var second: Bitmap? = null
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private var readOffset: () -> Float = { 0f }
    private var saveOffset: (Float) -> Unit = {}
    private var advance: () -> Unit = {}
    private val speed = 26f * resources.displayMetrics.density

    init {
        setZOrderOnTop(true)
        holder.setFormat(PixelFormat.TRANSLUCENT)
        holder.addCallback(this)
        isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }
    fun bind(current: NewsItem, next: NewsItem, enabled: Boolean,
        read: () -> Float, save: (Float) -> Unit, onNext: () -> Unit) {
        if (released) return
        active = enabled
        visibility = if (active) VISIBLE else INVISIBLE
        render.post {
        readOffset = read; saveOffset = save; advance = onNext
        val key = current.url + next.url
        if (key != identity) {
            identity = key
            first?.recycle(); second?.recycle()
            first = raster(current); second = raster(next)
            waitingForHeadline = false
        }
        if (!active) { lastFrame = 0; sampleStart = 0; sampleFrames = 0; sampleDrawNs = 0 }
        schedule()
        }
    }
    private fun raster(item: NewsItem): Bitmap {
        val text = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            textSize = android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP, 12f, resources.displayMetrics)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        val category = item.source + " · " + item.section + "  "
        val headline = item.title + "     •     "
        val prefixWidth = text.measureText(category)
        val width = kotlin.math.ceil(prefixWidth + text.measureText(headline)).toInt().coerceIn(1, 8192)
        val fm = text.fontMetrics
        val height = kotlin.math.ceil(fm.bottom - fm.top).toInt().coerceAtLeast(1)
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also {
            val canvas = Canvas(it)
            text.color = com.conreo.couchytv.HomeNews.sourceColor(item.source)
            canvas.drawText(category, 0f, -fm.top, text)
            text.color = Color.rgb(224, 229, 236)
            canvas.drawText(headline, prefixWidth, -fm.top, text)
        }
    }
    private fun schedule(delayMs: Long = 0) {
        render.removeCallbacks(drawTask)
        if (ready && active && !released && !waitingForHeadline && first != null) render.postDelayed(drawTask, delayMs)
    }
    private fun drawFrame() {
        if (!ready || !active || released) { lastFrame = 0; return }
        val frameTimeNanos = android.os.SystemClock.elapsedRealtimeNanos()
        val a = first ?: return
        var offset = readOffset()
        if (lastFrame != 0L) offset += ((frameTimeNanos - lastFrame) / 1_000_000_000f).coerceAtMost(.05f) * speed
        lastFrame = frameTimeNanos
        var canvas: Canvas? = null
        val drawStart = android.os.SystemClock.elapsedRealtimeNanos()
        try {
            canvas = holder.lockHardwareCanvas()
            canvas?.let {
                it.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                val y = (it.height - a.height) / 2f
                it.drawBitmap(a, -offset, y, bitmapPaint)
                second?.let { b -> it.drawBitmap(b, a.width - offset, y, bitmapPaint) }
            }
        } catch (_: IllegalStateException) { } finally {
            canvas?.let { runCatching { holder.unlockCanvasAndPost(it) } }
        }
        if (!sampleReported) {
            val end = android.os.SystemClock.elapsedRealtimeNanos()
            if (sampleStart == 0L) sampleStart = end
            sampleFrames++; sampleDrawNs += end - drawStart
            if (end - sampleStart >= 10_000_000_000L) {
                android.util.Log.i("CouchyTicker", "surface fps=${sampleFrames * 1_000_000_000L / (end - sampleStart)} drawAvgMs=${sampleDrawNs / sampleFrames / 1_000_000f}")
                sampleReported = true
            }
        }
        if (offset >= a.width) {
            saveOffset(offset - a.width)
            waitingForHeadline = true
            val callback = advance
            main.post { if (!released) callback() }
        } else saveOffset(offset)
        val elapsed = android.os.SystemClock.elapsedRealtimeNanos() - frameTimeNanos
        schedule(((16_666_667L - elapsed) / 1_000_000).coerceAtLeast(0))
    }
    override fun surfaceCreated(holder: SurfaceHolder) { ready = true; render.post { lastFrame = 0; schedule() } }
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) { render.post { schedule() } }
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        ready = false
        if (released) return
        val stopped = java.util.concurrent.CountDownLatch(1)
        render.post { render.removeCallbacks(drawTask); lastFrame = 0; stopped.countDown() }
        stopped.await(200, java.util.concurrent.TimeUnit.MILLISECONDS)
    }
    fun release() {
        active = false; ready = false; released = true
        render.post {
        render.removeCallbacks(drawTask)
        first?.recycle(); second?.recycle(); first = null; second = null; identity = ""
        worker.quitSafely()
        }
    }
}
