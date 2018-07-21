package icfpc2018.solutions.tripleSlices

import icfpc2018.Config
import icfpc2018.bot.commands.Command
import icfpc2018.bot.commands.Fission
import icfpc2018.bot.commands.SMove
import icfpc2018.bot.commands.Wait
import icfpc2018.bot.state.LinearCoordDiff.Axis.X
import icfpc2018.bot.state.LongCoordDiff
import icfpc2018.bot.state.NearCoordDiff
import icfpc2018.bot.state.System

fun initialLinearFission(bots: Int, system: System) {
    val commands = linearFission(bots)
    system.timeStep(listOf(
            SMove(LongCoordDiff.fromAxis(X, 1))
    ))
    for (stepCommands in commands) {
        system.timeStep(stepCommands)
    }
}

fun linearFission(bots: Int): List<List<Command>> {
    val trace = ArrayList<List<Command>>()
    val maxBotIndex = Integer.min(bots, Config.maxBots) - 1
    for (i in 0 until maxBotIndex) {
        val stepTrace = ArrayList<Command>()
        val moveTrace = ArrayList<Command>()
        for (j in 0 until i) {
            stepTrace.add(Wait)
        }
        for (j in 0 until i + 1) {
            moveTrace.add(Wait)
        }
        stepTrace.add(Fission(NearCoordDiff(1, 0, 0), maxBotIndex - i - 1))
        moveTrace.add(SMove(LongCoordDiff.fromAxis(X, 2)))
        trace.add(stepTrace)
        trace.add(moveTrace)
    }
    return trace
}
