package icfpc2018.solutions.regions

import icfpc2018.bot.state.Model
import icfpc2018.bot.state.Point
import icfpc2018.bot.state.System
import icfpc2018.solutions.Solution

sealed class Region

data class Rectangle(val left: Int, val right: Int, val bottom: Int, val top: Int) : Region()

data class Section(val first: Point, val second: Point) : Region()

data class Voxel(val point: Point) : Region()

class Regions(val target: Model, val system: System) : Solution {

    val rectangles = split(target)

    override fun solve() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    companion object {
        fun split(model: Model): List<List<Region>> {
            return emptyList()
        }
    }
}