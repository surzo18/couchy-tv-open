package com.conreo.couchytv

/** Shared deterministic retention policy, independent of Android and wall-clock reads. */
object DiagnosticRetention {
    const val MAX_ITEMS=400
    const val MAX_AGE=30L*24*60*60*1000
    fun retain(entries:List<DiagnosticEvent>,now:Long)=entries.filter{now-it.at in 0..MAX_AGE}.sortedBy{it.at}.takeLast(MAX_ITEMS)
}
