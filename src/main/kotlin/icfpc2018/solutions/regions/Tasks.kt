@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package icfpc2018.solutions.regions

import icfpc2018.bot.commands.Command
import icfpc2018.bot.commands.Fill
import icfpc2018.bot.commands.GFill
import icfpc2018.bot.commands.Wait
import icfpc2018.bot.state.*
import icfpc2018.solutions.BotManager
import icfpc2018.solutions.Task
import kotlin.coroutines.experimental.SequenceBuilder
import kotlin.coroutines.experimental.buildSequence

suspend fun <T, C> SequenceBuilder<C>.doWhileNotNull(default: C, foo: () -> T?): T {
    var result = foo()
    while (result == null) {
        yield(default)
        result = foo()
    }
    return result
}

fun <T, C> Iterator<T>.zipWithDefault(default1: T, default2: C, other: Iterator<C>): Iterator<Pair<T, C>> = buildSequence {
    while (hasNext() && other.hasNext()) {
        val v1 = next()
        val v2 = other.next()
        yield(Pair(v1, v2))
    }
    while (hasNext()) {
        val v1 = next()
        yield(Pair(v1, default2))
    }
    while (other.hasNext()) {
        val v2 = other.next()
        yield(Pair(default1, v2))
    }
}.iterator()


fun <T> Iterator<T>.zipWithDefault(default: T, defaults: List<T>, other: List<Iterator<T>>): Iterator<List<T>> = buildSequence {
    while (hasNext() && other.all { it.hasNext() }) {
        val v1 = next()
        val v2 = other.map { it.next() }
        yield(listOf(v1) + v2)
    }
    if (other.isEmpty()) return@buildSequence
    while (hasNext()) {
        val v1 = next()
        yield(listOf(v1) + defaults)
    }
    val zippedOther = other.first().zipWithDefault(defaults.first(), defaults.drop(1), other.drop(1))
    while (zippedOther.hasNext()) {
        val v2 = zippedOther.next()
        yield(listOf(default) + v2)
    }
}.iterator()

object RectangleTask {
    operator fun invoke(rectangle: Rectangle, manager: BotManager): Task = buildSequence {
        val nothing = emptyMap<Bot, Command>()
        val bots = doWhileNotNull(nothing) { manager.reserve(4) }
        val diff = NearCoordDiff(0, -1, 0)
        val p1 = rectangle.p1 + -diff // best code
        val p2 = rectangle.p2 + -diff // best code
        val p3 = rectangle.p3 + -diff // best code
        val p4 = rectangle.p4 + -diff // best code
        val points = listOf(p1, p2, p3, p4)
        val goto: List<Task> = bots.zip(points).map { (bot, p) -> GoTo(bot, p, manager.system) }
        val default = mapOf(bots.first() to Wait)
        val defaults: List<Map<Bot, Command>> = bots.drop(1).map { mapOf(it to Wait) }
        goto.first().zipWithDefault(default, defaults, goto.drop(1)).asSequence()
                .map { it.fold(emptyMap<Bot, Command>()) { acc, m -> acc + m } }
                .forEach { yield(it) }
        val diff1 = (p1 - rectangle.p3).toFarCoordDiff()
        val diff2 = (p2 - rectangle.p4).toFarCoordDiff()
        val diff3 = (p3 - rectangle.p1).toFarCoordDiff()
        val diff4 = (p4 - rectangle.p2).toFarCoordDiff()
        val diffs = listOf(diff1, diff2, diff3, diff4)
        val commands = bots.zip(diffs).map { (bot, d) -> bot to GFill(diff, d) }.toMap()
        yield(commands)
        manager.release(bots)
    }.iterator()
}

object SectionTask {
    operator fun invoke(section: Section, manager: BotManager): Task = buildSequence {
        val nothing = emptyMap<Bot, Command>()
        val (bot1, bot2) = doWhileNotNull(nothing) { manager.reserve(2) }
        val diff = NearCoordDiff(0, -1, 0)
        val first = section.first + -diff // best code
        val second = section.second + -diff // best code
        val goto1 = GoTo(bot1, first, manager.system)
        val goto2 = GoTo(bot2, second, manager.system)
        val default1 = mapOf(bot1 to Wait)
        val default2 = mapOf(bot2 to Wait)
        goto1.zipWithDefault(default1, default2, goto2).asSequence()
                .map { (c1, c2) -> mapOf(bot1 to c1, bot2 to c2) }
                .forEach { yield(it) }
        val diff1 = (first - section.second).toFarCoordDiff()
        val diff2 = (second - section.first).toFarCoordDiff()
        yield(mapOf(bot1 to GFill(diff, diff1), bot2 to GFill(diff, diff2)))
        manager.release(listOf(bot1, bot2))
    }.iterator()
}

object VoxelTask {
    operator fun invoke(voxel: Voxel, manager: BotManager): Task = buildSequence {
        val nothing = emptyMap<Bot, Command>()
        val (bot) = doWhileNotNull(nothing) { manager.reserve(1) }
        val goto = GoTo(bot, voxel.point, manager.system)
        goto.forEach { yield(it) }
        yield(mapOf(bot to Fill(NearCoordDiff(0, -1, 0))))
        manager.release(listOf(bot))
    }.iterator()
}

object GoTo {
    fun buildTrace(from: Point, to: Point, system: System): Set<Point>? = TODO()

    fun convertTrace(trace: Set<Point>): List<Command> = TODO()

    operator fun invoke(bot: Bot, point: Point, system: System): Task = buildSequence {
        val wait = mapOf(bot to Wait)
        val trace = doWhileNotNull(wait) { buildTrace(bot.position, point, system) }
        system.reserve(trace)
        for (command in convertTrace(trace)) {
            yield(mapOf(bot to command))
        }
        system.release(trace)
    }.iterator()
}