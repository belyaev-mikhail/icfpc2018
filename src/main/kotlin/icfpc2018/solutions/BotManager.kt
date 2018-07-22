package icfpc2018.solutions

import icfpc2018.bot.commands.Command
import icfpc2018.bot.commands.Wait
import icfpc2018.bot.state.Bot
import icfpc2018.bot.state.System
import java.util.*

typealias Task = Iterator<Map<Bot, Command>>

class CollisionError : Exception()

class DeadLockError : Exception()

class BotManager(val system: System) {
    private val botPool = ArrayList<Bot>(system.currentState.bots)

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
            if (!task.hasNext()) {
                done.add(task)
                continue
            }
            val taskCommands = task.next()
            for (bot in botPool) {
                if (bot in taskCommands)
                    throw CollisionError()
            }
            if (taskCommands.keys.any { it in commands })
                throw CollisionError()
            commands.putAll(taskCommands)
        }
        for (task in done) {
            taskPool.remove(task)
        }
        for (bot in botPool) {
            if (bot in commands)
                throw CollisionError()
            commands[bot] = Wait
        }
        if (!taskPool.isEmpty() && commands.values.all { it === Wait })
            throw DeadLockError()
        system.timeStep(commands.values.toList())
    }

    fun apply() {
        while (taskPool.isNotEmpty())
            timeStep()
    }
}