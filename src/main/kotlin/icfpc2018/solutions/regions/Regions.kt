package icfpc2018.solutions.regions

import icfpc2018.bot.state.Point
import icfpc2018.bot.state.minus

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
        get() = TODO()
//            when (direct) {
//            Direct.X -> (0 until (first - second).clen).map { }
//            Direct.Y -> {
//            }
//            Direct.Z -> {
//            }
//        }

    val direct: Direct

    init {
        direct = checkStructure() ?: throw StructureError()
    }

    private fun checkStructure(): Direct? {
        if (first.x == second.x && first.y == second.y) return Direct.Z
        if (first.x == second.x && first.z == second.z) return Direct.Y
        if (first.z == second.z && first.y == second.y) return Direct.X
        return null
    }
}

data class Voxel(val point: Point) : Region() {
    override val points: Set<Point>
        get() = setOf(point)
}
