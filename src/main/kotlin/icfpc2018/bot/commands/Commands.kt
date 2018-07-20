package icfpc2018.bot.commands

import icfpc2018.bot.state.*

interface Command {
    fun apply(bot: Bot, state: State): State

    fun volatileCoords(bot: Bot) = emptyList<Point>()

    fun check(bot: Bot, state: State) = true
}

object Halt : Command {
    override fun apply(bot: Bot, state: State) = TODO()
}

object Wait : Command {
    override fun apply(bot: Bot, state: State) = TODO()
}

object Flip : Command {
    override fun apply(bot: Bot, state: State) = TODO()
}

data class SMove(val lld: LongCoordDiff) : Command {
    override fun apply(bot: Bot, state: State) = TODO()
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
