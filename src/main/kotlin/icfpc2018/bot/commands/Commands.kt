package icfpc2018.bot.commands

import icfpc2018.bot.state.*
import icfpc2018.bot.state.Harmonics.HIGH
import icfpc2018.bot.state.Harmonics.LOW
import org.pcollections.TreePVector

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
        for (ac in lld.affectedCoords(bot.position)) {
            if (state.matrix[ac]) return false
        }
        return true
    }
}

data class LMove(val sld1: ShortCoordDiff, val sld2: ShortCoordDiff) : Command {
    override fun apply(bot: Bot, state: State) = TODO()
}

data class Fission(val nd: NearCoordDiff, val m: Int) : Command {
    override fun apply(bot: Bot, state: State) = TODO()
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
