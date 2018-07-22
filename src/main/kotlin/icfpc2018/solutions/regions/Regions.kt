package icfpc2018.solutions.regions

import icfpc2018.bot.state.CoordDiff
import icfpc2018.bot.state.Point
import icfpc2018.bot.state.minus
import icfpc2018.bot.state.plus

sealed class Region {
    abstract val points: Set<Point>
}

class StructureError : Exception()

data class Rectangle(val p1: Point, val p2: Point, val p3: Point, val p4: Point) : Region() {
    override val points: Set<Point>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    init {
        checkStructure() || throw StructureError()
    }

    private fun checkStructure(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

enum class Direct {
    X, Y, Z
}

data class Section(val first: Point, val second: Point) : Region() {
    override val points: Set<Point>
        get(): Set<Point> {
            val diff = first - second
            val length = diff.clen
            val atomDiff = CoordDiff(diff.dx / length, diff.dy / length, diff.dz / length)
            return (0 until length).map { first + atomDiff }.toSet()
        }

    init {
        checkStructure() || throw StructureError()
    }

    private fun checkStructure(): Boolean {
        if (first.x == second.x && first.y == second.y) return true
        if (first.x == second.x && first.z == second.z) return true
        if (first.z == second.z && first.y == second.y) return true
        return false
    }
}

data class Voxel(val point: Point) : Region() {
    override val points: Set<Point>
        get() = setOf(point)
}
