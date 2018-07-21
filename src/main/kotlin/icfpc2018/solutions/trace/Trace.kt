package icfpc2018.solutions.trace

import icfpc2018.bot.commands.Command
import icfpc2018.bot.state.System
import icfpc2018.solutions.Solution

class Trace(val trace: List<Command>, val system: System) : Solution {
    override fun solve() {
        var myTrace = trace

        with(system) {
            while (true) {
                if (currentState.bots.isEmpty()) break

                val currTrace = myTrace.subList(0, currentState.bots.size)
                myTrace = myTrace.subList(currentState.bots.size, myTrace.size)

                system.timeStep(currTrace)
            }
        }
    }
}
