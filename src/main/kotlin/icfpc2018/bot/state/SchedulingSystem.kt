package icfpc2018.bot.state

import icfpc2018.bot.commands.Command
import icfpc2018.bot.commands.Wait
import java.util.*

class SchedulingSystem(currentState: State, mode: Mode = Mode.DEBUG) : System(currentState, mode) {
    val commandQueues = mutableMapOf<Bot, Deque<Command>>()

    override fun timeStep(commands: List<Command>): Boolean {
        while (true) {
            if (commandQueues.values.all { it.isEmpty() }) return super.timeStep(commands)

            val cmds = currentState.bots.map {
                commandQueues[it]?.pollFirst() ?: Wait
            }.toList()

            if (!super.timeStep(cmds)) return false
        }
    }

    fun schedule(bot: Bot, cmd: Command) {
        commandQueues.computeIfAbsent(bot) { _ -> LinkedList() }.addLast(cmd)
    }
}
