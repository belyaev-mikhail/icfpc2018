package icfpc2018.bot.commands

import icfpc2018.bot.state.*
import icfpc2018.bot.state.Harmonics.HIGH
import icfpc2018.bot.state.Harmonics.LOW

interface Command {
    fun apply(bot: Bot, state: State): State

    fun volatileCoords(bot: Bot) = listOf(bot.position)

    fun check(bot: Bot, state: State) = true
}

object Halt : Command {
    override fun apply(bot: Bot, state: State) {
        // state.bots = emptyList()
    }

    override fun check(bot: Bot, state: State): Boolean {
        return when {
            bot.position != Point.ZERO -> false
            state.bots != listOf(bot) -> false
            state.harmonics != LOW -> false
            else -> true
        }
    }
}

object Wait : Command {
    override fun apply(bot: Bot, state: State) {}
}

object Flip : Command {
    override fun apply(bot: Bot, state: State) {
        state.harmonics = when (state.harmonics) {
            LOW -> HIGH
            HIGH -> LOW
        }
    }
}

data class SMove(val lld: LongCoordDiff) : Command {
    override fun apply(bot: Bot, state: State) {

    }

    override fun volatileCoords(bot: Bot): List<Point> =
            lld.affectedCoords(bot.position)

    override fun check(bot: Bot, state: State): Boolean {
        val newPos = bot.position + lld
        // validate new pos
        for (ac in lld.affectedCoords(bot.position)) {
            // if (state.matrix[])
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
