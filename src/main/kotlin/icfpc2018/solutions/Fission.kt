package icfpc2018.solutions

import icfpc2018.bot.commands.Command
import icfpc2018.bot.commands.Fission
import icfpc2018.bot.commands.Wait
import icfpc2018.bot.state.NearCoordDiff
import icfpc2018.bot.state.System

fun initialLinearFission(bots: Int, system: System) {
    val commands = linearFission(bots)
    for (stepCommands in commands) {
        system.timeStep(stepCommands)
    }
}

fun linearFission(bots: Int): List<List<Command>> {
    val trace = ArrayList<List<Command>>()
    val maxBotIndex = Integer.min(bots, 20) - 1
    for (i in 0 until maxBotIndex) {
        val stepTrace = ArrayList<Command>()
        for (j in 0 until i) {
            stepTrace.add(Wait)
        }
        stepTrace.add(Fission(NearCoordDiff(1, 0, 0), maxBotIndex - i - 1))
        trace.add(stepTrace)
    }
    return trace
}