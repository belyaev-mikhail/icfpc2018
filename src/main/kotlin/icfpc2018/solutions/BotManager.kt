package icfpc2018.solutions

import icfpc2018.bot.commands.*
import icfpc2018.bot.state.Bot
import icfpc2018.bot.state.GroundError
import icfpc2018.bot.state.Harmonics
import icfpc2018.bot.state.System
import java.util.*
import kotlin.coroutines.experimental.SequenceBuilder
import kotlin.coroutines.experimental.buildIterator

typealias Task = Iterator<Map<Int, Command>>

data class TaggedTask(val tag: String, val inner: Task) : Task by inner {
    override fun toString() = "$tag"

    constructor(tag: String, innerBuilder: suspend SequenceBuilder<Map<Int, Command>>.() -> Unit) :
            this(tag, buildIterator(innerBuilder))
}

class CollisionError : Exception()

class DeadLockError : Exception()

class BotManager(val system: System) {
    private val botPool = ArrayList<Bot>(system.currentState.bots)

    private val taskPool = ArrayList<Task>()

    private var fullWaitCounter = 0

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
        val commands = TreeMap<Int, Command>()
        val done = ArrayList<Task>()
        for (task in taskPool) {
            if (!task.hasNext()) {
                done.add(task)
                continue
            }
            val taskCommands = task.next()
            for (bot in botPool) {
                if (bot.id in taskCommands)
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
            if (bot.id in commands)
                throw CollisionError()
//            ToDo(Mikhail) wtf??
//            if(botPool.size != system.numBots) commands[bot] = Wait
            commands[bot.id] = Wait
        }

        if (commands.values.all { it === Wait })
            ++fullWaitCounter
        else
            fullWaitCounter = 0
        if (fullWaitCounter == 3)
            throw DeadLockError()

        if (commands.isNotEmpty() /*&& commands.values.any { it is GFill || it is Fill } */) {
            val timeStamp = system.timeStamp()
            try {
                if (system.currentState.harmonics == Harmonics.HIGH && system.currentState.matrix.isEverybodyGrounded) {
                    system.timeStep(commands.map { if (it.key == commands.keys.first()) Flip else Wait })
                }
                system.timeStep(commands.values.toList())
            } catch (ex: GroundError) {
                system.rollBackTo(timeStamp)
                if (system.currentState.harmonics == Harmonics.LOW) {
                    system.timeStep(commands.map { if (it.key == commands.keys.first()) Flip else Wait })
                }
                system.timeStep(commands.values.toList())
            }
        }
    }

    fun apply() {
        while (taskPool.isNotEmpty())
            timeStep()
    }
}