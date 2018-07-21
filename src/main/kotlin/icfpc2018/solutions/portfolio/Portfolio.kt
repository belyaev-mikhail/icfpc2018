package icfpc2018.solutions.portfolio

import icfpc2018.bot.state.*
import icfpc2018.bot.util.persistentTreeSetOf
import icfpc2018.solutions.Solution
import icfpc2018.solutions.getSolutionByName

class Portfolio(val target: Model, val system: System) : Solution {
    val solutionNames = listOf("slices", "grounded_slices")

    val model = Model(target.size)
    val bot = Bot(1, Point(0, 0, 0), (2..20).toSortedSet())
    val initialState = State(0, Harmonics.LOW, model, persistentTreeSetOf(bot))
    val systems = solutionNames.map { it to System(initialState) }.toMap()
    val solutions = solutionNames.map { it to getSolutionByName(it, target, systems.getValue(it)) }

    override fun solve() {
        solutions.forEach { (_, value) -> Thread().run { value.solve() } }

        val best = systems.values.filter { it.currentState.matrix == target }.minBy { it.currentState.energy }
                ?: throw IllegalStateException()
        system.commandTrace = best.commandTrace
        system.stateTrace = best.stateTrace
        system.currentState = best.currentState
    }

}