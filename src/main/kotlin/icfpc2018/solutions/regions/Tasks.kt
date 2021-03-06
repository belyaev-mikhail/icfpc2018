@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package icfpc2018.solutions.regions

import icfpc2018.bot.algo.AStar
import icfpc2018.bot.commands.*
import icfpc2018.bot.state.*
import icfpc2018.log
import icfpc2018.solutions.BotManager
import icfpc2018.solutions.TaggedTask
import icfpc2018.solutions.Task
import icfpc2018.solutions.botPairs
import org.organicdesign.fp.collections.PersistentTreeSet
import kotlin.coroutines.experimental.SequenceBuilder
import kotlin.coroutines.experimental.buildSequence
import kotlin.math.abs

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
        yieldAll(buildSequence {
            val v1 = next()
            val v2 = other.next()
            yield(Pair(v1, v2))
        })
    }
    while (hasNext()) {
        yieldAll(buildSequence {
            val v1 = next()
            yield(Pair(v1, default2))
        })
    }
    while (other.hasNext()) {
        yieldAll(buildSequence {
            val v2 = other.next()
            yield(Pair(default1, v2))
        })
    }
}.iterator()

fun <T> List<Iterator<T>>.zipWithDefaults(defaults: List<T>) = buildSequence {
    while(any { it.hasNext() }) {
        yield(
                zip(defaults) { it, def ->
                    if(it.hasNext()) it.next()
                    else def
                }
        )
    }
}

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
    operator fun invoke(rectangle: Rectangle, manager: BotManager): Task = TaggedTask("$rectangle") {
        val nothing = emptyMap<Int, Command>()
        val diff = NearCoordDiff(0, -1, 0)
        val p1 = rectangle.p1 + -diff // best code
        val p2 = rectangle.p2 + -diff // best code
        val p3 = rectangle.p3 + -diff // best code
        val p4 = rectangle.p4 + -diff // best code
        val points = listOf(p1, p2, p3, p4)
        val bots = doWhileNotNull(nothing) { manager.reserve(points) }
        val goto: List<Task> = bots.zip(points).map { (bot, p) -> GoTo(bot, p, manager) }
        val default = mapOf(bots.first() to Wait)
        val defaults = bots.drop(1).map { mapOf(it to Wait) }
        goto.first().zipWithDefault(default, defaults, goto.drop(1)).asSequence()
                .map { it.fold(emptyMap<Int, Command>()) { acc, m -> acc + m } }
                .forEach { yield(it) }
        val diff1 = (rectangle.p3 - rectangle.p1).toFarCoordDiff()
        val diff2 = (rectangle.p4 - rectangle.p2).toFarCoordDiff()
        val diff3 = (rectangle.p1 - rectangle.p3).toFarCoordDiff()
        val diff4 = (rectangle.p2 - rectangle.p4).toFarCoordDiff()
        val diffs = listOf(diff1, diff2, diff3, diff4)
        while (!manager.system.reserve(rectangle.points)) {
            yield(bots.map { it to Wait }.toMap())
        }
        val commands = bots.zip(diffs).map { (bot, d) -> bot to GFill(diff, d) }.toMap()
        yield(commands)
        manager.release(bots)
    }
}

object SectionTask {
    operator fun invoke(section: Section, manager: BotManager): Task = TaggedTask("$section") {
        val nothing = emptyMap<Int, Command>()
        val diff = NearCoordDiff(0, -1, 0)
        val first = section.first + -diff // best code
        val second = section.second + -diff // best code
        val (bot1, bot2) = doWhileNotNull(nothing) { manager.reserve(listOf(first, second)) }

        val goto1 = GoTo(bot1, first, manager)
        val goto2 = GoTo(bot2, second, manager)
        val default1 = mapOf(bot1 to Wait)
        val default2 = mapOf(bot2 to Wait)
        goto1.zipWithDefault(default1, default2, goto2).asSequence()
                .map { (c1, c2) -> c1 + c2 }
                .forEach { yield(it) }
        val diff1 = (section.second - section.first).toFarCoordDiff()
        val diff2 = (section.first - section.second).toFarCoordDiff()
        while (!manager.system.reserve(section.points)) {
            yield(mapOf(bot1 to Wait, bot2 to Wait))
        }
        val firstBot = manager.system.currentState.bots.find { it.id == bot1 }!!
        val secondBot = manager.system.currentState.bots.find { it.id == bot2 }!!
        if (first != firstBot.position)
            log.info("Expected $first, but got ${firstBot.position}")
        if (second != secondBot.position)
            log.info("Expected $second, but got ${secondBot.position}")
        if (first != firstBot.position || second != secondBot.position)
            log.info("$bot1 $first $bot2 $second $diff $diff1 $diff2")
        yield(mapOf(bot1 to GFill(diff, diff1), bot2 to GFill(diff, diff2)))

//        if(manager.system.currentState.canMoveTo(first + CoordDiff(0, 2, 0))
//         && manager.system.currentState.canMoveTo(second + CoordDiff(0, 2, 0))) {
//            val bail1 = GoTo(bot1, first + CoordDiff(0, 2, 0), manager)
//            val bail2 = GoTo(bot2, second + CoordDiff(0, 2, 0), manager)
//            bail1.zipWithDefault(default1, default2, bail2).asSequence()
//                    .map { (c1, c2) -> c1 + c2 }
//                    .forEach { yield(it) }
//        }

        manager.release(listOf(bot1, bot2))
    }.iterator()
}

object VoxelTask {
    operator fun invoke(voxel: Voxel, manager: BotManager): Task = TaggedTask("$voxel") {
        val nothing = emptyMap<Int, Command>()
        val (bot) = doWhileNotNull(nothing) { manager.reserve(1) }
        val diff = NearCoordDiff(0, -1, 0)
        val goto = GoTo(bot, voxel.point + -diff, manager)
        goto.forEach { yield(it) }
        while (!manager.system.reserve(voxel.points)) yield(mapOf(bot to Wait))
        yield(mapOf(bot to Fill(diff)))
        manager.release(listOf(bot))
    }.iterator()
}

fun <T> List<T>.sepWhile(limit: Int = size, predicate: (T) -> Boolean): Pair<List<T>, List<T>> {
    val before = mutableListOf<T>()
    var j = 0
    for (i in 0..minOf(lastIndex, limit - 1)) {
        j = i
        val it = get(i)
        if (predicate(it)) before += it
        else return before to this.subList(i, size)
    }
    if (before.size == size) return before to listOf()
    return before to subList(before.size, size)
}

object GoTo {
    fun buildTrace(from: Point, to: Point, system: System): List<Point>? {
        if(!system.currentState.canMoveTo(to)) return null
        val algo = AStar<Point>(
                neighbours = { it.immediateNeighbours().filter { system.currentState.canMoveTo(it) } },
                heuristic = {
                    val diff = it - to
                    maxOf(abs(diff.dz), abs(diff.dy) / 2, abs(diff.dx) / 2)
                },
                goal = { it == to }
        )
        return algo.run(from)?.asList()
    }

    fun convertTrace(trace: List<Point>): List<Command> {
        val res = mutableListOf<CoordDiff>()
        val diffs = trace.windowed(2) { (a, b) -> (b - a).toLinear() }

        require(
                diffs.fold(trace.first()) { p, d -> p + d } == trace.last()
        ) { "Small diffs messed up =(" }

        var current = diffs

        var forceShort = false

        while (current.isNotEmpty()) {
            val pt = current.first()
            val (move, rest) = current.sepWhile(limit = 15) { it.axis == pt.axis }
            current = rest
            if (move.isEmpty()) continue
            if (move.size > 5 && !forceShort) {
                res += move.reduce<CoordDiff, CoordDiff> { a, b -> a + b }.toLong()
            } else {
                if (move.size > 5) {
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
        ) { "Big diffs messed up =(" }

        var cur = trace.first()
        res.forEach {
            cur += it
        }

        val it = res.iterator()
        val commands = mutableListOf<Command>()
        while (it.hasNext()) {
            val e = it.next()
            if (e is LongCoordDiff) commands += SMove(e)
            if (e is ShortCoordDiff) {
                if (it.hasNext()) {
                    val e2 = it.next() as? ShortCoordDiff ?: throw IllegalArgumentException()
                    commands += LMove(e, e2)
                } else {
                    commands += SMove(e.toLong())
                }
            }
        }
        return commands
    }

    operator fun invoke(bid: Int, to: Point, manager: BotManager): Task = TaggedTask("Goto(Bot(id=$bid), $to)") {
        val from = manager.position(bid)
        if (from == to) return@TaggedTask
        val system = manager.system
        val wait = mapOf(bid to Wait)
        val trace = doWhileNotNull(wait) {
            val trace_ = buildTrace(from, to, system)
            trace_?.apply {
                if (!system.reserve(trace_, exclude = setOf(from)))
                    throw IllegalStateException("Cannot reserve")
            }
        }
        val previousCommands: MutableList<Command> = mutableListOf()
        val previousBotPositions: MutableList<Point> = mutableListOf()

        for (command in convertTrace(trace)) {
            previousBotPositions += manager.position(bid)
            yield(mapOf(bid to command))
            if(previousCommands.size > 2) {
                val it = previousCommands[previousCommands.lastIndex - 2]
                val pos = previousBotPositions[previousBotPositions.lastIndex - 3]
//                when {
//                    it is SMove -> require(pos + it.lld == previousBotPositions.last())
//                    it is LMove -> require(pos + it.sld1 + it.sld2 == previousBotPositions.last())
//                }
               // system.release((it as SimpleCommand).volatileCoords(Bot(id = bid, position = pos, seeds = PersistentTreeSet.empty())))
               // system.reserve((it as SimpleCommand).volatileCoords(Bot(id = bid, position = pos, seeds = PersistentTreeSet.empty())))
                //system.reserve(setOf(previousBotPositions[previousBotPositions.lastIndex - 2]))
            }
            previousCommands += command
        }
        system.release(trace)
    }
}

private operator fun CoordDiff.plus(b: CoordDiff): CoordDiff =
        CoordDiff(dx + b.dx, dy + b.dy, dz + b.dz)

object GoToBase {
    operator fun invoke(manager: BotManager): Task = TaggedTask("GotoBase()") {
        val numBots = manager.system.numBots
        val points = Pair(Point.ZERO, Point(numBots, 0, 0)).coords()
        val bots = manager.reserve(numBots)?.sorted() ?: throw IllegalStateException("WTF")
        val goto: List<Task> = bots.zip(points).map { (bot, p) -> GoTo(bot, p, manager) }
        val defaults = bots.map { mapOf(it to Wait) }
        val subs = goto.zipWithDefaults(defaults).map {
            it.fold(emptyMap<Int, Command>()){ a, b -> a + b }
        }
        yieldAll(subs)
        manager.release(bots)
    }.iterator()
}
