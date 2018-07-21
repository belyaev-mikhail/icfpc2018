package icfpc2018.solutions.columns

import icfpc2018.bot.state.*
import icfpc2018.bot.state.Point.Companion.MINUS_ONE_TO_ONE
import icfpc2018.solutions.Solution
import icfpc2018.solutions.columns.Columns.Mode.*

class Columns(val system: SchedulingSystem, val columnNumber: Int, val targetModel: Model) : Solution {
    init {
        assert(columnNumber > 0)
    }

    enum class Mode {
        CREATE,
        BUILD,
        MERGE,
        FINALIZE,
        HALT
    }

    var mode = CREATE

    override fun solve() {
        while (true) {
            when (mode) {
                CREATE -> handleCreate()
                BUILD -> TODO()
                MERGE -> TODO()
                FINALIZE -> TODO()
                HALT -> TODO()
            }
        }
    }

    fun handleCreate() {
        while (true) {
            var botsToCreate = columnNumber - system.currentState.bots.size

            if (0 == botsToCreate) return

            if (botsToCreate > system.currentState.bots.size) {
                botsToCreate = system.currentState.bots.size
            }

            val zipped = (1..botsToCreate).zip(system.currentState.bots)

            val volatileCoords = system.currentState.bots.map { it.position }.toMutableSet()

            for ((i, bot) in zipped) {
                val possiblePositions = bot.position.options(
                        MINUS_ONE_TO_ONE,
                        listOf(1),
                        MINUS_ONE_TO_ONE
                ).inRange(system.currentState.matrix)


            }
        }
    }

    fun handleBuild(system: System) {

    }
}
