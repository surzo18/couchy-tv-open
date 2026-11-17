package com.conreo.couchytv

/** Immutable clock anchor; no polling of the media service. */
data class MediaClock(val position:Long=-1,val duration:Long=0,val updated:Long=0,val speed:Float=0f,val advancing:Boolean=false) {
    fun positionAt(now:Long):Long {
        if(position<0)return -1
        val elapsed=if(advancing && updated>0 && speed.isFinite() && speed>0)(now-updated).coerceAtLeast(0)*speed.toDouble() else 0.0
        val value=(position+elapsed).coerceAtMost(Long.MAX_VALUE.toDouble()).toLong().coerceAtLeast(0)
        return if(duration>0)value.coerceAtMost(duration) else value
    }
    fun label(now:Long):String {
        val p=positionAt(now);if(p<0)return ""
        return format(p)+(if(duration>0)" / "+format(duration) else "")
    }
    companion object {
        fun format(ms:Long):String {
            val sec=ms.coerceAtLeast(0)/1000
            return if(sec>=3600)"${sec/3600}:${((sec/60)%60).toString().padStart(2,'0')}:${(sec%60).toString().padStart(2,'0')}"
            else "${sec/60}:${(sec%60).toString().padStart(2,'0')}"
        }
    }
}
