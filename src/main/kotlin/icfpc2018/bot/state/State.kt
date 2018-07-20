package icfpc2018.bot.state

import icfpc2018.bot.commands.Command
import kotlin.math.abs
import kotlin.math.max

enum class Harmonics {
    LOW, HIGH
}

data class Bot(val id: Int, var position: Point, val seeds: Set<Int>)

data class State(val trace: MutableList<Command>, var energy: Int, var harmonics: Harmonics, val matrix: Model, val bots: List<Bot>)

data class Point(val x: Int, val y: Int, val z: Int)

open class CoordDiff(val dx: Int, val dy: Int, val dz: Int) {
    val mlen by lazy {
        abs(dx) + abs(dy) + abs(dz)
    }

    val clen by lazy {
        max(abs(dx), max(abs(dy), abs(dz)))
    }
}

open class LinearCoordDiff(dx: Int, dy: Int, dz: Int) : CoordDiff(dx, dy, dz) {
    init {
        val xZero = if (dx == 0) 1 else 0
        val yZero = if (dy == 0) 1 else 0
        val zZero = if (dz == 0) 1 else 0
        assert(1 == xZero + yZero + zZero)
    }
}

class LongCoordDiff(dx: Int, dy: Int, dz: Int) : LinearCoordDiff(dx, dy, dz) {
    init {
        assert(mlen <= 15)
    }
}

class ShortCoordDiff(dx: Int, dy: Int, dz: Int) : LinearCoordDiff(dx, dy, dz) {
    init {
        assert(mlen <= 5)
    }
}

class NearCoordDiff(dx: Int, dy: Int, dz: Int) : CoordDiff(dx, dy, dz) {
    init {
        assert(mlen in 1..2 && clen == 1)
    }
}
