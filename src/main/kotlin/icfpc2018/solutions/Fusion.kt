package icfpc2018.solutions

import icfpc2018.bot.algo.AStar
import icfpc2018.bot.commands.allPossibleMoves
import icfpc2018.bot.state.*

fun botPairs(bots: Int, system: System): MutableList<Pair<Bot, Bot>> {
    val botList = system.currentState.bots.toMutableSet()
    val res: MutableList<Pair<Bot, Bot>> = mutableListOf()

    while(botList.isNotEmpty()) {
        val first = botList.first()
        botList -= first
        val second = botList.minBy { (first.position - it.position).mlen } ?: break
        botList -= second
        res += (first to second)
    }

    return res
}

inline operator fun Point.get(axis: LinearCoordDiff.Axis) = when(axis) {
    LinearCoordDiff.Axis.X -> x
    LinearCoordDiff.Axis.Y -> y
    LinearCoordDiff.Axis.Z -> z
}

fun meet(bot1: Bot, bot2: Bot, system: System) {
//
//    AStar<State>(
//            neighbours = { },
//            heuristic = { },
//            goal = {  }
//    )
}

fun comeTogether(bots: Pair<Bot, Bot>, system: SchedulingSystem) {



}
