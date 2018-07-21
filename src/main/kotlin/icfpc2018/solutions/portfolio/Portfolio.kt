package icfpc2018.solutions.portfolio

import icfpc2018.bot.state.*
import icfpc2018.bot.util.persistentTreeSetOf
import icfpc2018.solutions.Solution
import icfpc2018.solutions.groundedSlices.GroundedSlices
import icfpc2018.solutions.slices.Slices

class Portfolio(val target: Model, val system: System) : Solution {
    val model = Model(target.size)
    val bot = Bot(1, Point(0, 0, 0), (2..20).toSortedSet())
    val initialState = State(0, Harmonics.LOW, model, persistentTreeSetOf(bot))
    val solutionNames = listOf("slices", "grounded_slices")
    val systems = solutionNames.map { it to System(initialState) }.toMap()
    val solutions = mutableMapOf(
            "slices" to Slices(target, systems.getValue("slices")),
            "grounded_slices" to GroundedSlices(target,  systems.getValue("grounded_slices"))
    )

    override fun solve() {
        solutions.forEach { (_, value) -> value.solve() }
        val best = systems.minBy { it.value.currentState.energy }!!.value
        system.commandTrace = best.commandTrace
        system.stateTrace = best.stateTrace
        system.currentState = best.currentState
    }

}