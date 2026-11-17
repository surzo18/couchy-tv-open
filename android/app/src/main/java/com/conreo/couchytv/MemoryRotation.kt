package com.conreo.couchytv

data class MemoryChoice(val selected:String,val seen:Set<String>)
object MemoryRotation {
    fun next(ids:Set<String>,history:Set<String>,last:String?):MemoryChoice? {
        if(ids.isEmpty())return null
        val seen=history.intersect(ids).toMutableSet()
        var remaining=ids.filter{it !in seen}
        if(remaining.isEmpty()){seen.clear();remaining=ids.toList()}
        val selected=remaining.filter{it!=last}.ifEmpty{remaining}.random()
        seen.add(selected)
        return MemoryChoice(selected,seen)
    }
}
