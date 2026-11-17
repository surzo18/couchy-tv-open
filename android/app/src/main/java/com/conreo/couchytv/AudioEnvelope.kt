package com.conreo.couchytv

/** Pure playback-position envelope: no timers advance while audio is paused. */
object AudioEnvelope {
    fun gain(positionMs: Long, durationMs: Long, startMs: Long): Float {
        val incoming = ((positionMs - startMs).coerceAtLeast(0) / 1500f).coerceIn(0f, 1f)
        val outgoing = if (durationMs > 0) ((durationMs - positionMs).coerceAtLeast(0) / 2500f).coerceIn(0f, 1f) else 1f
        val x = minOf(incoming, outgoing)
        return x * x * (3f - 2f * x)
    }
}
