@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package icfpc2018.solutions.regions

import icfpc2018.bot.algo.AStar
import icfpc2018.bot.commands.*
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

suspend fun <T> SequenceBuilder<T>.end(default: T) = yield(default)

suspend fun SequenceBuilder<Map<Bot, Command>>.end() = end(emptyMap())

object RectangleTask {
    operator fun invoke(rectangle: Rectangle, manager: BotManager): Task = buildSequence {
        val nothing = emptyMap<Bot, Command>()
        val bots = doWhileNotNull(nothing) { manager.reserve(4) }
        while(!manager.system.reserve(rectangle.points)) {
            yield(bots.map { it to Wait }.toMap())
        }
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
        end()
    }.iterator()
}

object SectionTask {
    operator fun invoke(section: Section, manager: BotManager): Task = buildSequence<Map<Bot, Command>> {
        val nothing = emptyMap<Bot, Command>()
        val (bot1, bot2) = doWhileNotNull(nothing) { manager.reserve(2) }
        while(!manager.system.reserve(section.points)) {
            yield(mapOf(bot1 to Wait, bot2 to Wait))
        }
        val diff = NearCoordDiff(0, -1, 0)
        val first = section.first + -diff // best code
        val second = section.second + -diff // best code
        val goto1 = GoTo(bot1, first, manager.system)
        val goto2 = GoTo(bot2, second, manager.system)
        val default1 = mapOf(bot1 to Wait)
        val default2 = mapOf(bot2 to Wait)
        goto1.zipWithDefault(default1, default2, goto2).asSequence()
                .map { (c1, c2) -> c1 + c2 }
                .forEach { yield(it) }
        val diff1 = (first - section.second).toFarCoordDiff()
        val diff2 = (second - section.first).toFarCoordDiff()
        yield(mapOf(bot1 to GFill(diff, diff1), bot2 to GFill(diff, diff2)))
        manager.release(listOf(bot1, bot2))
        end()
    }.iterator()
}

object VoxelTask {
    operator fun invoke(voxel: Voxel, manager: BotManager): Task = buildSequence {
        val nothing = emptyMap<Bot, Command>()
        val (bot) = doWhileNotNull(nothing) { manager.reserve(1) }
        while(!manager.system.reserve(voxel.points)) yield(mapOf(bot to Wait))
        val diff = NearCoordDiff(0, -1, 0)
        val goto = GoTo(bot, voxel.point + -diff, manager.system)
        goto.forEach { yield(it) }
        yield(mapOf(bot to Fill(diff)))
        manager.release(listOf(bot))
        end()
    }.iterator()
}

fun <T> List<T>.sepWhile(limit: Int = size, predicate: (T) -> Boolean): Pair<List<T>, List<T>> {
    val before = mutableListOf<T>()
    var j = 0
    for(i in 0..minOf(lastIndex, limit)) {
        j = i
        val it = get(i)
        if(predicate(it)) before += it
        else return before to this.subList(i, size)
    }
    if(before.size == size) return before to listOf()
    return before to subList(before.size, size)
}

object GoTo {
    fun buildTrace(from: Point, to: Point, system: System): List<Point>? {
        val algo = AStar<Point>(
                neighbours = { it.immediateNeighbours().filter { system.currentState.canMoveTo(it) } },
                heuristic = { (it - to).mlen.toLong() },
                goal = { it == to }
        )
        return algo.run(from)?.asList()
    }

    fun convertTrace(trace: List<Point>): List<Command> {
        val res = mutableListOf<CoordDiff>()
        val diffs = trace.windowed(2) { (a, b) -> (b - a).toLinear() }

        require(
                diffs.fold(trace.first()) { p, d -> p + d } == trace.last()
        ){"Small diffs messed up =("}

        var current = diffs

        var forceShort = false

        while(current.isNotEmpty()) {
            val pt = current.first()
            val (move, rest) = current.sepWhile(limit = 15) { it.axis == pt.axis }
            current = rest
            if (move.isEmpty()) continue
            if (move.size > 5 && !forceShort) {
                res += move.reduce<CoordDiff, CoordDiff> { a, b -> a + b }.toLong()
            }
            else {
                if(move.size > 5) {
                    res += move.take(5).reduce<CoordDiff, CoordDiff> { a, b -> a + b }.toShort()
                    res += move.drop(5).reduce<CoordDiff, CoordDiff> { a, b -> a + b }.toLong()
                } else {
                    res += move.reduce<CoordDiff, CoordDiff> { a, b -> a + b }.toShort()
                }
                forceShort = !forceShort
            }
        }

        require(
                res.fold(trace.first()) { p, d -> p + d } == trace.last()
        ){"Big diffs messed up =("}

        val it = res.iterator()
        val commands = mutableListOf<Command>()
        while(it.hasNext()) {
            val e = it.next()
            if(e is LongCoordDiff) commands += SMove(e)
            if(e is ShortCoordDiff) {
                if(it.hasNext()) {
                    val e2 = it.next() as? ShortCoordDiff ?: throw IllegalArgumentException()
                    commands += LMove(e, e2)
                } else {
                    commands += SMove(e.toLong())
                }
            }
        }
        return commands
    }

    operator fun invoke(bot: Bot, point: Point, system: System): Task = buildSequence {
        val wait = mapOf(bot to Wait)
        val trace = doWhileNotNull(wait) { buildTrace(bot.position, point, system) }
        if(!system.reserve(trace)) {
            throw IllegalStateException("Cannot reserve")
        }
        for (command in convertTrace(trace)) {
            yield(mapOf(bot to command))
        }
        system.release(trace)
        end()
    }.iterator()
}

private operator fun CoordDiff.plus(b: CoordDiff): CoordDiff =
        CoordDiff(dx + b.dx, dy + b.dy, dz + b.dz)

object GoToBase {
    operator fun invoke(system: System): Task = buildSequence {
        val bots = system.currentState.bots.toList()
        val points = Pair(Point.ZERO, Point(bots.size, 0, 0)).coords()
        val goto: List<Task> = bots.zip(points).map { (bot, p) -> GoTo(bot, p, system) }
        val default = mapOf(bots.first() to Wait)
        val defaults: List<Map<Bot, Command>> = bots.drop(1).map { mapOf(it to Wait) }
        goto.first().zipWithDefault(default, defaults, goto.drop(1)).asSequence()
                .map { it.fold(emptyMap<Bot, Command>()) { acc, m -> acc + m } }
                .forEach { yield(it) }
        end()
    }.iterator()
}
