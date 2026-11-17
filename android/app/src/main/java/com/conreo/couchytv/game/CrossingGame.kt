package com.conreo.couchytv.game

import kotlin.math.*
import java.util.Random

fun coinCount(count: Int): String = "$count " + when(count) { 1 -> "minca"; in 2..4 -> "mince"; else -> "mincí" }

/** Small deterministic simulation, independent of Android and rendering. */
class CrossingGame @JvmOverloads constructor(var seed: Long = System.nanoTime(),var variant:Int=0) {
    data class Lane(val row: Int, val kind: Int, val direction: Int, val speed: Float,
        val phase: Float, val length: Float, val gap: Float, val coin: Int, val trees: Set<Int>, val coinValue: Int = 1,val sinking:Boolean=false) {
        fun start(time: Double): Float = (((phase + direction * speed * time) % gap + gap) % gap).toFloat() - gap
        fun contains(x: Float, time: Double, margin: Float = 0f): Boolean {
            val p = ((x - start(time)) % gap + gap) % gap
            return p <= length + margin || p >= gap - margin
        }
        fun trainPhase(time: Double) = ((time + phase) % 8.8).toFloat()
        fun trainX(time: Double): Float? {
            val p=trainPhase(time)
            if(p>=1.8f)return null
            return if(direction>0) -12f+p*18f else 12f-p*18f
        }
        fun warning(time: Double) = trainPhase(time)>6.6f || trainPhase(time)<1.8f
        fun submerged(time:Double)=sinking && (time+phase)%10.5>9.2
        fun sinkingWarning(time:Double)=sinking && (time+phase)%10.5>7.2
    }
    var x = 4f; private set
    var row = 0; private set
    var score = 0; private set
    var coins = 0; private set
    var time = 0.0; private set
    private var elapsed = 0.0
    private var pace = 1f
    val difficulty get() = (1 + score / 25 + coins / 5).coerceAtMost(10)
    val difficultyLabel get() = when(difficulty) { 1->"Rozjazd";2,3->"Živá premávka";4,5->"V špičke";6,7->"Rýchly prúd";8,9->"Veľká výzva";else->"Legenda" }
    val biome get() = listOf("Zelené údolie","Zimná cesta","Púštna výprava","Súmrak")[if(variant==4)3 else (score/40)%4]
    var dead = false; private set
    var reason = ""; private set
    var paused = true
    private var lastMove = -1.0
    private val lanes = LinkedHashMap<Int, Lane>()
    private val collected = mutableSetOf<Int>()
    val cachedLaneCount get() = lanes.size

    fun lane(at: Int): Lane = lanes.getOrPut(at) {
        val rng = Random(seed xor (at.toLong() * 7919L))
        val pattern=if(at<12)0 else Random(seed xor (at/12L*9173)).nextInt(3)
        val position=at%12
        val kind = when {
            at<=1 -> 0
            at>=40 && at%16==10 -> 3
            at>=40 && (at%16==9 || at%16==11) -> 0 // safe banks around the railway
            position in (when(pattern){1->setOf(0,4,8,11);2->setOf(0,2,6,10);else->setOf(0,3,7,10)}) -> 0
            position==(if(pattern==1)6 else 4) || ((at>=35 || (variant==2 && at>=12)) && position==(if(pattern==1)7 else 5)) -> 2
            position==5 && pattern==0 -> 0
            else -> 1
        }
        val coin = if (at == 1) 4 else if (kind == 0 && at > 1) rng.nextInt(9) else -1
        val trees = if (kind == 0 && at > 1) setOf(rng.nextInt(9), rng.nextInt(9)) - coin else emptySet()
        val pressure=(at.coerceAtLeast(0)/180f).coerceAtMost(1f)
        val length=if(kind==2) 2.8f-pressure*.6f else if(at>=20 && rng.nextInt(if(variant==1)2 else 3)==0) 1.9f else 1.15f
        Lane(at, kind, if (rng.nextBoolean()) 1 else -1,
            if (kind == 2) .45f + rng.nextFloat() * .2f else (.65f + rng.nextFloat() * .35f + pressure*.35f),
            rng.nextFloat() * 4f, length,
            if (kind == 2) 4.1f else maxOf(length+1.25f,4.3f-pressure*.8f+rng.nextFloat()*.5f), coin, trees,
            if(at>12 && rng.nextInt(if(variant==3)2 else 5)==0) 3 else 1,
            kind==2 && at>=(if(variant==2)24 else 90) && at%3==1)
    }
    fun hasCoin(at: Int) = lane(at).coin >= 0 && at !in collected
    fun restart(newSeed: Long = System.nanoTime()) {
        seed = newSeed; x = 4f; row = 0; score = 0; coins = 0; time = 0.0
        dead = false; reason = ""; lastMove = -1.0; elapsed=0.0;pace=1f;paused = false; lanes.clear(); collected.clear()
    }
    fun move(dx: Int, dy: Int): Boolean {
        if (paused || dead || elapsed - lastMove < .13 || abs(dx) + abs(dy) != 1) return false
        // Keep the log's fractional position when hopping forward/backward.
        // Rounding it here used to move the hero sideways unexpectedly.
        val nextX = (x + dx).coerceIn(0f, 8f)
        if(dx!=0 && abs(nextX-x)<.0001f)return false
        val nextRow = row + dy
        if (nextX !in 0f..8f || nextRow < maxOf(0, score - 7) || nextRow > 1_000_000 || nextX.roundToInt() in lane(nextRow).trees) return false
        x = nextX; row = nextRow; lastMove = elapsed
        score = maxOf(score, row)
        if (hasCoin(row) && x.roundToInt() == lane(row).coin) { collected.add(row); coins+=lane(row).coinValue }
        lanes.keys.removeAll { it < score - 9 || it > score + 16 }
        collected.removeAll { it < score - 9 }
        collected.removeAll { it < score - 9 }
        checkCollision()
        return true
    }
    fun update(delta: Float) {
        if (paused || dead) return
        val dt = delta.coerceIn(0f, .05f)
        elapsed += dt
        pace += (1f+(difficulty-1)*.085f-pace)*minOf(1f,dt*1.5f)
        time += dt * pace
        val l = lane(row)
        if (l.kind == 2) x += l.direction * l.speed * dt * pace
        checkCollision()
    }
    private fun checkCollision() {
        val l = lane(row)
        reason = when {
            x < -.25f || x > 8.25f -> "Prúd ťa odniesol z cesty."
            l.kind == 1 && l.contains(x + .5f, time, .18f) -> "Pozor na autá!"
            l.kind == 3 && l.trainX(time)?.let { x+.5f>=it-.18f && x+.5f<=it+9f+.18f }==true -> "Vlak má prednosť. Sleduj červené svetlá."
            l.kind == 2 && l.submerged(time) -> "Plošina sa ponorila. Pred ponorením bliká." 
            l.kind == 2 && !l.contains(x + .5f, time) -> "Na rieku skoč iba na plávajúce drevo."
            else -> ""
        }
        if (reason.isNotEmpty()) { dead = true; paused = true }
    }
}
