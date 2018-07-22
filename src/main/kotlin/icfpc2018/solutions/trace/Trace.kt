package icfpc2018.solutions.trace

import icfpc2018.bot.commands.*
import icfpc2018.bot.state.System
import icfpc2018.log
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

    fun ungroup(command: Command) =
        when(command) {
            is SimpleCommand -> listOf(command)
            is GroupCommand -> command.innerCommands
            else -> listOf()
        }

    fun inverted(): List<Command> {
        var myTrace = trace

        val resultTrace: MutableList<Command> = mutableListOf()

        with(system) {
            while (true) {
                if (currentState.bots.isEmpty()) break

                val currentStamp = mutableMapOf<Int, Command>()
                val currTrace = myTrace.subList(0, currentState.bots.size)
                myTrace = myTrace.subList(currentState.bots.size, myTrace.size)

                val (simple, group) = system.degroup(currTrace)
                system.timeStep(currTrace)

                simple.forEach { (bot, command) -> when(command) {
                    is Fission -> {
                        val fusion = command.inverse(arrayOf(bot))
                        currentStamp[bot.id] = fusion.p
                        currentStamp[bot.seeds.first()] = fusion.s
                    }
                    else -> currentStamp[bot.id] = command.inverse(arrayOf(bot))
                }
                     }
                group.forEach { (bots, command) ->
                    val ungrouped = ungroup(command.inverse(bots.toTypedArray()))
                    (bots zip ungrouped).forEach {(bot, cmd) ->
                        currentStamp[bot.id] = cmd
                    }
                }
                resultTrace += currentStamp.keys.sorted().map { currentStamp[it]!! }
            }
        }

        val halt = resultTrace.last()
        resultTrace.removeAt(resultTrace.lastIndex)
        require(halt is Halt)

        val res = resultTrace.reversed() + halt
        return res
    }
}
