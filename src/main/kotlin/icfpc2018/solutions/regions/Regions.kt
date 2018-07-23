package icfpc2018.solutions.regions

import icfpc2018.bot.state.CoordDiff
import icfpc2018.bot.state.Point
import icfpc2018.bot.state.minus
import icfpc2018.bot.state.plus
import icfpc2018.bot.state.coords

sealed class Region {
    abstract val points: Set<Point>
}

class StructureError : Exception()

data class Rectangle(val p1: Point, val p2: Point, val p3: Point, val p4: Point) : Region() {
    override val points: Set<Point>
        get() = Pair(p1, p3).coords().toSet()

    init {
        Section(p1, p2)
        Section(p2, p3)
        Section(p3, p4)
        Section(p4, p1)
    }
}

data class Section(val first: Point, val second: Point) : Region() {
    override val points: Set<Point>
        get() = Pair(first, second).coords().toSet()

    init {
        checkStructure() || throw StructureError()
    }

    val length: Int
        get() = when {
            first.x == second.x && first.y == second.y -> second.z - first.z
            first.x == second.x && first.z == second.z -> second.y - first.y
            first.z == second.z && first.y == second.y -> second.x - first.x
            else -> throw IllegalStateException()
        }

    private fun checkStructure() = when {
        first.x == second.x && first.y == second.y -> true
        first.x == second.x && first.z == second.z -> true
        first.z == second.z && first.y == second.y -> true
        else -> false
    }
}

data class Voxel(val point: Point) : Region() {
    override val points: Set<Point>
        get() = setOf(point)
}
