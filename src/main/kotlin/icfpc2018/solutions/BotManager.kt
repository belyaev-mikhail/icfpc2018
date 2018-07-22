package icfpc2018.solutions

import icfpc2018.bot.commands.Command
import icfpc2018.bot.commands.Wait
import icfpc2018.bot.state.Bot
import icfpc2018.bot.state.System
import java.util.*

typealias Task = Iterator<Map<Bot, Command>>

class CollisionError : Exception()

class BotManager(bots: List<Bot>, val system: System) {
    private val botPool = ArrayList<Bot>(bots)

    private val taskPool = ArrayList<Task>()

    fun add(task: Task) = taskPool.add(task)

    fun reserve(numBots: Int): List<Bot>? {
        if (botPool.size < numBots) return null
        val bots = botPool.takeLast(numBots)
        botPool.subList(botPool.size - numBots, botPool.size).clear()
        return bots
    }

    fun release(bots: List<Bot>) {
        botPool.addAll(bots)
    }

    fun timeStep() {
        val commands = TreeMap<Bot, Command>()
        val done = ArrayList<Task>()
        for (task in taskPool) {
            val taskCommands = task.next()
            if (taskCommands.keys.any { it in commands })
                throw CollisionError()
            commands.putAll(taskCommands)
            if (task.hasNext()) continue
            done.add(task)
        }
        for (task in done) {
            taskPool.remove(task)
        }
        for (bot in botPool) {
            if (bot in commands) throw CollisionError()
            commands[bot] = Wait
        }
        system.timeStep(commands.values.toList())
    }
}