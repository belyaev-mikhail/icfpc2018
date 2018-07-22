package icfpc2018.solutions.regions

import icfpc2018.bot.state.Model
import icfpc2018.bot.state.System
import icfpc2018.solutions.Solution


class RegionSolution(val target: Model, val system: System) : Solution {

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