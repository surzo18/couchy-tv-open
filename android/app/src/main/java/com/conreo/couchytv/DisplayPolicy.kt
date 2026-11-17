package com.conreo.couchytv

/** Testable geometry policy. UI coordinates are independent of video decode resolution. */
object DisplayPolicy {
    fun fit(width:Float,height:Float):Float {
        require(width.isFinite() && height.isFinite() && width>0 && height>0)
        return minOf(width/960f,height/540f)
    }
    fun scrollDistance(offset:Float,size:Float,viewport:Float,margin:Float):Float = when {
        size+2*margin>=viewport -> offset-margin
        offset<margin -> offset-margin
        offset+size>viewport-margin -> offset+size-viewport+margin
        else -> 0f
    }
}
