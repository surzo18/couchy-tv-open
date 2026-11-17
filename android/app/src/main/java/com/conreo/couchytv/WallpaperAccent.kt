package com.conreo.couchytv

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import java.io.File

/** Sample the existing small thumbnail, never decode a second video on a TCL TV. */
object WallpaperAccent {
    private val cache = mutableMapOf<String, Int>()
    @Synchronized fun color(context: Context, uri: String, fallback: Int): Int {
        cache[uri]?.let { return it }
        val name = Uri.parse(uri).lastPathSegment ?: return fallback
        val bitmap = runCatching {
            val options = BitmapFactory.Options().apply { inSampleSize = 8 }
            if (uri.startsWith("asset:")) context.assets.open("thumbnails/${name.removeSuffix(".mp4")}.jpg")
                .use { BitmapFactory.decodeStream(it, null, options) }
            else BitmapFactory.decodeFile(File(WallpaperLibrary.folder(context), "$name.jpg").path, options)
        }.getOrNull() ?: return fallback
        val weights = FloatArray(24)
        val hsv = FloatArray(3)
        for (y in 0 until bitmap.height) for (x in 0 until bitmap.width) {
            Color.colorToHSV(bitmap.getPixel(x, y), hsv)
            if (hsv[1] > .22f && hsv[2] > .15f && hsv[2] < .98f)
                weights[(hsv[0] / 15f).toInt().coerceIn(0, 23)] += hsv[1] * hsv[2]
        }
        bitmap.recycle()
        val bin = weights.indices.maxByOrNull { weights[it] } ?: 0
        // Light, restrained accent stays readable on the dark menu at every hue.
        val result = if (weights[bin] == 0f) fallback else Color.HSVToColor(floatArrayOf(bin * 15f + 7.5f, .48f, .95f))
        if (cache.size >= 80) cache.clear()
        cache[uri] = result
        return result
    }
}
