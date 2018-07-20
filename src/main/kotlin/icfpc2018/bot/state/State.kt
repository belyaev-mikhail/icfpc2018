package icfpc2018.bot.state

import icfpc2018.bot.commands.Command
import kotlin.math.abs
import kotlin.math.max

enum class Harmonics {
    LOW, HIGH
}

data class Bot(val id: Int, val position: Point, val seeds: Set<Int>)

data class State(val energy: Int, val harmonics: Harmonics, val matrix: Model, val bots: List<Bot>)

data class Point(val x: Int, val y: Int, val z: Int)

class System(initialState: State) {

    var currentState: State = initialState

    var commandTrace = ArrayList<Command>()

    var stateTrace = ArrayList<State>()

    fun apply(bot: Bot, command: Command) {
        commandTrace.add(command)
        val state = command.apply(bot, currentState)
        stateTrace.add(state)
    }
}

open class CoordDiff(val dx: Int, val dy: Int, val dz: Int) {
    val mlen by lazy {
        abs(dx) + abs(dy) + abs(dz)
    }

    val clen by lazy {
        max(abs(dx), max(abs(dy), abs(dz)))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CoordDiff) return false

        if (dx != other.dx) return false
        if (dy != other.dy) return false
        if (dz != other.dz) return false

        return true
    }

    override fun hashCode(): Int {
        var result = dx
        result = 31 * result + dy
        result = 31 * result + dz
        return result
    }

    override fun toString(): String {
        return "CoordDiff(dx=$dx, dy=$dy, dz=$dz)"
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

class LongCoordDiff(dx: Int = 0, dy: Int = 0, dz: Int = 0) : LinearCoordDiff(dx, dy, dz) {
    init {
        assert(mlen <= 15)
    }
}

class ShortCoordDiff(dx: Int = 0, dy: Int = 0, dz: Int = 0) : LinearCoordDiff(dx, dy, dz) {
    init {
        assert(mlen <= 5)
    }
}

class NearCoordDiff(dx: Int, dy: Int, dz: Int) : CoordDiff(dx, dy, dz) {
    init {
        assert(mlen in 1..2 && clen == 1)
    }
}
