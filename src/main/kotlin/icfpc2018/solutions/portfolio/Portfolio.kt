package icfpc2018.solutions.portfolio

import icfpc2018.Config
import icfpc2018.bot.state.*
import icfpc2018.bot.util.persistentTreeSetOf
import icfpc2018.log
import icfpc2018.solutions.Solution
import icfpc2018.solutions.getSolutionByName

class Portfolio(val target: Model, val system: System) : Solution {
    val model = Model(target.size)
    val bot = Bot(1, Point(0, 0, 0), (2..Config.maxBots).toSortedSet())
    val initialState = State(0, Harmonics.LOW, model, persistentTreeSetOf(bot))

    companion object {
        val solutionNames = listOf("grounded_slices", "bounded_slices", "grounded_bounded_slices")
    }

    override fun solve() {
        var initialized = false
        var bestSolutionName = "none"
        for (solutionName in solutionNames) {
            val solutionSystem = System(initialState)
            val solution = getSolutionByName(solutionName, target, solutionSystem)
            solution.solve()

            if (solutionSystem.currentState.matrix == target) {
                if (!initialized || system.score > solutionSystem.score) {
                    system.commandTrace = solutionSystem.commandTrace
                    system.stateTrace = solutionSystem.stateTrace
                    system.currentState = solutionSystem.currentState
                    initialized = true
                    bestSolutionName = solutionName
                }
            }
        }
        log.info("Best portfolio solution: $bestSolutionName")
    }

}