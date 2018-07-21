package icfpc2018.bot.state

import icfpc2018.bot.commands.*

enum class Mode {
    DEBUG, PRODUCTION
}

class ExecutionError : Exception()

class CommandCheckError(msg: String) : Exception(msg)

class GroupCommandError : Exception()

data class TimeStamp(val commandStamp: Int, val stateStamp: Int)

open class System(var currentState: State, var mode: Mode = Mode.DEBUG) {

    val numBots: Int
        get() = currentState.bots.size

    var commandTrace = mutableListOf<Command>()

    var stateTrace = mutableListOf(currentState)

    fun timeStamp() = TimeStamp(commandTrace.size, stateTrace.size)

    fun rollBackTo(timeStamp: TimeStamp) {
        commandTrace = commandTrace.subList(0, timeStamp.commandStamp)
        stateTrace = stateTrace.subList(0, timeStamp.stateStamp)
        currentState = stateTrace.last()
    }

    open fun timeStep(commands: List<Command>): Boolean {
        if (currentState.bots.isEmpty()) throw ExecutionError()

        var energy = currentState.energy

        energy += when (currentState.harmonics) {
            Harmonics.LOW -> 3 * currentState.matrix.sizeCubed
            Harmonics.HIGH -> 30 * currentState.matrix.sizeCubed
        }

        energy += 20 * currentState.bots.size

        var execState = currentState.copy(energy = energy)

        val ungroupedCommands = mutableListOf<Pair<Bot, Command>>()

        // TODO: Check volatile coords for non-intersection

        for ((bot, cmd) in currentState.bots.zip(commands)) {
            when (cmd) {
                is FusionP, is FusionS -> {
                    ungroupedCommands.add(bot to cmd)
                }
                is SimpleCommand -> {
                    if (Mode.DEBUG == mode) if (!cmd.check(bot, execState)) throw CommandCheckError(
                            "For $cmd of $bot with $execState"
                    )
                    execState = cmd.apply(bot, execState)
                }
            }
            commandTrace.add(cmd)
        }

        val fusionPrimary = ungroupedCommands.filter { it.second is FusionP }
        val fusionSecondary = ungroupedCommands.filter { it.second is FusionS }

        if (fusionPrimary.size != fusionSecondary.size) throw GroupCommandError()

        val groupedCommands = mutableListOf<Pair<List<Bot>, GroupCommand>>()

        for ((botP, cmdP) in fusionPrimary) {
            for ((botS, cmdS) in fusionSecondary) {
                if (cmdP !is FusionP) throw GroupCommandError()
                if (cmdS !is FusionS) throw GroupCommandError()

                val posP = botP.position + cmdP.nd
                val posS = botS.position + cmdS.nd
                if (posP == botS.position && posS == botP.position) {
                    groupedCommands.add(listOf(botP, botS) to FusionT(cmdP, cmdS))
                }
            }
        }

        if (groupedCommands.size != fusionPrimary.size) throw GroupCommandError()

        for ((bots, cmd) in groupedCommands) {
            if (Mode.DEBUG == mode) if (!cmd.check(bots, execState)) throw CommandCheckError(
                    "For $cmd of $bots with $execState"
            )
            execState = cmd.apply(bots, execState)
        }

        stateTrace.add(execState)

        currentState = execState

        return true
    }
}
