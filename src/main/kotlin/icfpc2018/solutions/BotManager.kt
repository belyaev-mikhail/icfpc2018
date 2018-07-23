package icfpc2018.solutions

import icfpc2018.bot.algo.AStar
import icfpc2018.bot.commands.*
import icfpc2018.bot.state.*
import icfpc2018.solutions.regions.GoTo
import org.organicdesign.fp.collections.PersistentHashSet
import org.organicdesign.fp.collections.PersistentTreeSet
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
    private val botPool = ArrayList<Int>(system.currentState.bots.map { it.id }.toList())

    private val taskPool = ArrayList<Task>()

    private var fullWaitCounter = 0

    fun add(task: Task) = taskPool.add(task)

    fun position(bid: Int) = system.currentState.bots
            .find { it.id == bid }?.position
            ?: throw IllegalArgumentException("bot $bid ot found")

    fun positions(bids: Set<Int>): Map<Int, Point> = system.currentState.bots
            .filter { it.id in bids }
            .map { it.id to it.position }
            .toMap()

    fun reserveBot(nearestTo: Point): Int? {
        if (botPool.isEmpty()) return null
        val bid = botPool.minBy { (position(it) - nearestTo).clen } ?: return null
        botPool.remove(bid)
        return bid
    }

    fun reserve(nearestTo: List<Point>): List<Int>? {
        if (botPool.size < nearestTo.size) return null
        return nearestTo.mapNotNull { reserveBot(it) }
    }

    fun reserve(numBots: Int): List<Int>? {
        if (botPool.size < numBots) return null
        val bots = botPool.takeLast(numBots)
        botPool.subList(botPool.size - numBots, botPool.size).clear()
        return bots
    }

    fun release(bots: List<Int>) {
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

        val reservedForPool: MutableSet<Point> = mutableSetOf()
        for (bot in botPool) {
            if (bot in commands)
                throw CollisionError()
//            ToDo(Mikhail) wtf??
//            if(botPool.size != system.numBots) commands[bot] = Wait

            commands[bot] = Wait
            continue
            val trye = AStar<Point> (
                    neighbours = { it.immediateNeighbours().filter { system.currentState.canMoveTo(it) } },
                    heuristic = { it.z },
                    goal = { it.z == 0 }
            ).run(position(bot))?.asList()
            if(trye != null) {
                val move = GoTo.convertTrace(trye).firstOrNull() as? SimpleCommand ?: Wait
                val coords = move.volatileCoords(Bot(id = bot, position = position(bot), seeds = PersistentTreeSet.empty()))
                system.reserve(coords)
                reservedForPool.addAll(coords)
                commands[bot] = move
            }
        }
        system.release(reservedForPool)

        if (commands.values.all { it == Wait }) {
            ++fullWaitCounter
            return
        }
        else
            fullWaitCounter = 0
        if (fullWaitCounter == 5)
            throw DeadLockError()

        if (commands.isNotEmpty()) {
            val timeStamp = system.timeStamp()
            try {
                if (system.currentState.matrix.isEverybodyGrounded)
                    flipTo(Harmonics.LOW, commands)
                system.timeStep(commands.values.toList())
            } catch (ex: GroundError) {
                system.rollBackTo(timeStamp)
                flipTo(Harmonics.HIGH, commands)
                system.timeStep(commands.values.toList())
            }
            if (system.currentState.matrix.isEverybodyGrounded)
                flipTo(Harmonics.LOW, null)
        }
    }

    private fun flipTo(harmonics: Harmonics, commands: TreeMap<Int, Command>?): Boolean {
        if (system.currentState.harmonics == harmonics) return true

        val existing = commands?.entries?.find { entry -> entry.value == Wait }?.let { it.setValue(Flip)  }
        if(existing != null) return true

        val commands = system.currentState.bots.map { if (it.id == 1) Flip else Wait }.toList()
        system.timeStep(commands)
        return false
    }

    fun apply() {
        while (taskPool.isNotEmpty())
            timeStep()
    }
}