package icfpc2018.bot.state

import icfpc2018.bot.commands.Command
import icfpc2018.bot.commands.SMove
import icfpc2018.bot.commands.Wait
import java.util.*

class SchedulingSystem(currentState: State, mode: Mode = Mode.DEBUG) : System(currentState, mode) {
    val commandQueues = mutableMapOf<Int, Deque<Command>>()

    override fun timeStep(commands: List<Command>): Boolean {
        while (true) {
            if (commandQueues.values.all { it.isEmpty() }) return super.timeStep(commands)

            val cmds = currentState.bots.map {
                commandQueues[it.id]?.pollFirst() ?: Wait
            }.toList()

            if (!super.timeStep(cmds)) return false
        }
    }

    fun schedule(bot: Bot, cmd: Command) {
        commandQueues.computeIfAbsent(bot.id) { _ -> LinkedList<Command>() }.addLast(cmd)
    }

    fun scheduleSMove(bot: Bot, axis: LinearCoordDiff.Axis, length: Int) {
        val repeat = length / 15

        for (i in 0 until repeat) {
            schedule(bot, SMove(
                    lld = LongCoordDiff.fromAxis(axis, 15)
            ))
        }

        val rest = length % 15
        if (rest != 0) {
            schedule(bot, SMove(
                    lld = LongCoordDiff.fromAxis(axis, rest)
            ))
        }
    }
}
