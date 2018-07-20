package icfpc2018.solutions.trace

import icfpc2018.bot.commands.Command
import icfpc2018.bot.state.System
import icfpc2018.solutions.Solution

class Trace(val trace: List<Command>) : Solution {
    override fun apply(system: System) {
        var myTrace = trace

        with(system) {
            while (true) {
                if (currentState.bots.isEmpty()) break

                val currTrace = myTrace.take(currentState.bots.size)
                myTrace = myTrace.drop(currentState.bots.size)

                system.timeStep(currTrace)
            }
        }
    }
}
