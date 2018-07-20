package icfpc2018.bot.state

import icfpc2018.bot.commands.Command
import icfpc2018.bot.state.LinearCoordDiff.Axis.*
import org.pcollections.TreePVector
import kotlin.math.abs
import kotlin.math.max

enum class Harmonics {
    LOW, HIGH
}

data class Bot(val id: Int, val position: Point, val seeds: Set<Int>)

data class State(val energy: Int, val harmonics: Harmonics, val matrix: Model, val bots: TreePVector<Bot>)

data class Point(val x: Int, val y: Int, val z: Int) {
    companion object {
        val ZERO = Point(0, 0, 0)
    }
}

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
}

operator fun Point.plus(cd: CoordDiff) = Point(x + cd.dx, y + cd.dy, z + cd.dz)

open class LinearCoordDiff(dx: Int, dy: Int, dz: Int) : CoordDiff(dx, dy, dz) {
    enum class Axis { X, Y, Z }

    lateinit var axis: Axis

    init {
        val xZero = if (dx != 0) {
            axis = X
            1
        } else 0
        val yZero = if (dy != 0) {
            axis = Y
            1
        } else 0
        val zZero = if (dz != 0) {
            axis = Z
            1
        } else 0
        assert(1 == xZero + yZero + zZero)
    }

    fun affectedCoords(origin: Point): List<Point> {
        with(origin) {
            return when (axis) {
                X -> (0..dx).map { Point(x + it, y, z) }
                Y -> (0..dy).map { Point(x, y + it, z) }
                Z -> (0..dz).map { Point(x, y, z + it) }
            }
        }
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
