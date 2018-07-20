package icfpc2018.bot.commands

import icfpc2018.bot.state.*
import icfpc2018.bot.state.Harmonics.HIGH
import icfpc2018.bot.state.Harmonics.LOW
import org.pcollections.TreePVector
import java.util.*

interface Command {
    fun apply(bot: Bot, state: State): State = state

    fun volatileCoords(bot: Bot) = listOf(bot.position)

    fun check(bot: Bot, state: State) = true
}

object Halt : Command {
    override fun apply(bot: Bot, state: State): State =
            state.copy(bots = TreePVector.empty())

    override fun check(bot: Bot, state: State): Boolean {
        return when {
            bot.position != Point.ZERO -> false
            state.bots != listOf(bot) -> false
            state.harmonics != LOW -> false
            else -> true
        }
    }
}

object Wait : Command

object Flip : Command {
    override fun apply(bot: Bot, state: State): State {
        return state.copy(harmonics = when (state.harmonics) {
            LOW -> HIGH
            HIGH -> LOW
        })
    }
}

data class SMove(val lld: LongCoordDiff) : Command {
    override fun apply(bot: Bot, state: State) =
            state.copy(
                    energy = state.energy + 2 * lld.mlen,
                    bots = state.bots - bot + bot.copy(position = bot.position + lld)
            )

    override fun volatileCoords(bot: Bot): List<Point> =
            lld.affectedCoords(bot.position)

    override fun check(bot: Bot, state: State): Boolean {
        val newPos = bot.position + lld
        // TODO: validate new pos
        for (ac in volatileCoords(bot)) {
            if (state.matrix[ac]) return false
        }
        return true
    }
}

data class LMove(val sld1: ShortCoordDiff, val sld2: ShortCoordDiff) : Command {
    override fun apply(bot: Bot, state: State) =
            state.copy(
                    energy = state.energy + 2 * (sld1.mlen + 2 + sld2.mlen),
                    bots = state.bots - bot
                            + bot.copy(position = bot.position + sld1 + sld2)
            )

    override fun volatileCoords(bot: Bot): List<Point> =
            sld1.affectedCoords(bot.position) + sld2.affectedCoords(bot.position + sld1)

    override fun check(bot: Bot, state: State): Boolean {
        val newPos = bot.position + sld1 + sld2
        // TODO: validate new pos
        for (ac in volatileCoords(bot)) {
            if (state.matrix[ac]) return false
        }
        return true
    }
}

data class Fission(val nd: NearCoordDiff, val m: Int) : Command {
    override fun apply(bot: Bot, state: State): State {
        val bids = bot.seeds.withIndex().groupBy { (idx, _) ->
            when (idx) {
                0 -> 0
                in 1..m -> 1
                else -> 2
            }
        }
        return state.copy(
                energy = state.energy + 24,
                bots = state.bots
                        - bot
                        + bot.copy(seeds = TreeSet(bids[2]!!.map { it.value }))
                        + Bot(
                        id = bot.seeds.first(),
                        position = bot.position + nd,
                        seeds = TreeSet(bids[1]!!.map { it.value }))
        )
    }

    override fun volatileCoords(bot: Bot): List<Point> =
            listOf(bot.position, bot.position + nd)

    override fun check(bot: Bot, state: State): Boolean {
        if (bot.seeds.isEmpty()) return false
        val newPos = bot.position + nd
        // TODO: validate new pos
        if (state.matrix[newPos]) return false
        if (state.bots.size < m + 1) return false
        return true
    }
}

data class Fill(val nd: NearCoordDiff) : Command {
    override fun apply(bot: Bot, state: State) = TODO()
}

data class FusionP(val nd: NearCoordDiff) : Command {
    override fun apply(bot: Bot, state: State) = TODO()
}

data class FusionS(val nd: NearCoordDiff) : Command {
    override fun apply(bot: Bot, state: State) = TODO()
}
